package io.github.aedev.flow.player.googlecast

import android.content.Context
import android.view.ContextThemeWrapper
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.mediarouter.app.MediaRouteChooserDialog
import androidx.mediarouter.app.MediaRouteControllerDialog
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaSeekOptions
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import io.github.aedev.flow.R
import io.github.aedev.flow.player.EnhancedPlayerManager
import io.github.aedev.flow.player.dlna.DlnaCastManager
import io.github.aedev.flow.player.dlna.StreamProxyServer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Google owns discovery, receiver sessions and remote controls; the phone relays media bytes. */
object GoogleCastManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _connectedDevice = MutableStateFlow<String?>(null)
    val connectedDevice: StateFlow<String?> = _connectedDevice.asStateFlow()
    private val _playback = MutableStateFlow(GoogleCastPlaybackState())
    val playback: StateFlow<GoogleCastPlaybackState> = _playback.asStateFlow()
    private var remote: RemoteMediaClient? = null
    private var framework: CastContext? = null
    private var application: Context? = null
    private var pending: GoogleCastRequest? = null
    private var loadedVideoId: String? = null
    private var loadJob: Job? = null

    fun initialize(context: Context) {
        if (!runCatching { isAvailable(context) }.getOrDefault(false)) return
        application = context.applicationContext
        runCatching {
            CastContext
                .getSharedInstance(context, ContextCompat.getMainExecutor(context))
                .addOnSuccessListener { attach(it) }
        }
    }

    private fun attach(cast: CastContext) {
        if (framework === cast) return
        framework = cast
        cast.sessionManager.addSessionManagerListener(listener, CastSession::class.java)
        cast.sessionManager.currentCastSession?.takeIf { it.isConnected }?.let { session ->
            _connectedDevice.value = session.castDevice?.friendlyName
            bindRemote(session)
        }
    }

    fun isAvailable(context: Context): Boolean =
        GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS

    fun openPicker(
        context: Context,
        request: GoogleCastRequest,
    ) {
        application = context.applicationContext
        pending = request
        CastContext
            .getSharedInstance(context, ContextCompat.getMainExecutor(context))
            .addOnSuccessListener { cast ->
                attach(cast)
                val theme = ContextThemeWrapper(context, androidx.appcompat.R.style.Theme_AppCompat_DayNight_Dialog)
                runCatching {
                    val session = cast.sessionManager.currentCastSession
                    if (session?.isConnected == true) {
                        _connectedDevice.value = session.castDevice?.friendlyName
                        bindRemote(session)
                        if (loadedVideoId != request.videoId) load(session)
                        MediaRouteControllerDialog(theme).show()
                    } else {
                        MediaRouteChooserDialog(theme).apply { routeSelector = cast.mergedSelector }.show()
                    }
                }.onFailure { failed() }
            }.addOnFailureListener { failed() }
    }

    fun togglePlayback() {
        remote?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun seekTo(positionMs: Long) {
        remote?.seek(MediaSeekOptions.Builder().setPosition(positionMs.coerceAtLeast(0)).build())
    }

    private fun refreshPlayback() {
        val client = remote ?: return
        _playback.value =
            if (client.hasMediaSession()) {
                GoogleCastPlaybackState(
                    device = _connectedDevice.value,
                    videoId = loadedVideoId ?: pending?.videoId,
                    isPlaying = client.isPlaying,
                    isBuffering = client.isBuffering,
                    positionMs = client.approximateStreamPosition.coerceAtLeast(0),
                    durationMs = client.streamDuration.coerceAtLeast(0),
                )
            } else {
                GoogleCastPlaybackState()
            }
        val manager = EnhancedPlayerManager.getInstance()
        val castVideoId = loadedVideoId ?: pending?.videoId
        if (client.isPlaying && castVideoId != null && manager.getPlayer()?.currentMediaItem?.mediaId == castVideoId) manager.pause()
    }

    private val statusCallback =
        object : RemoteMediaClient.Callback() {
            override fun onStatusUpdated() {
                refreshPlayback()
            }
        }
    private val progressListener =
        RemoteMediaClient.ProgressListener { position, duration ->
            _playback.value = _playback.value.copy(positionMs = position.coerceAtLeast(0), durationMs = duration.coerceAtLeast(0))
        }

    private fun bindRemote(session: CastSession?) {
        remote?.unregisterCallback(statusCallback)
        remote?.removeProgressListener(progressListener)
        remote = session?.remoteMediaClient
        remote?.registerCallback(statusCallback)
        remote?.addProgressListener(progressListener, 1_000L)
        if (session == null) _playback.value = GoogleCastPlaybackState() else refreshPlayback()
    }

    private fun load(session: CastSession) {
        val request = pending ?: return
        val context = application ?: return
        loadJob?.cancel()
        loadJob =
            scope.launch {
                val media =
                    runCatching {
                        withContext(Dispatchers.IO) {
                            GoogleCastMedia.prepare(context, request)
                        }
                    }.getOrElse { if (it is CancellationException) throw it else null }
                if (media == null) {
                    failed(R.string.google_cast_format_unavailable)
                    return@launch
                }
                if (framework?.sessionManager?.currentCastSession !== session || !session.isConnected) return@launch
                val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE).apply { putString(MediaMetadata.KEY_TITLE, request.title) }
                val info =
                    MediaInfo
                        .Builder(media.url)
                        .setContentType(media.contentType)
                        .setStreamType(if (media.live) MediaInfo.STREAM_TYPE_LIVE else MediaInfo.STREAM_TYPE_BUFFERED)
                        .setMetadata(metadata)
                        .apply { if (!media.live && request.durationMs > 0) setStreamDuration(request.durationMs) }
                        .build()
                val load =
                    MediaLoadRequestData
                        .Builder()
                        .setMediaInfo(info)
                        .setAutoplay(true)
                        .setCurrentTime(if (media.live) 0 else request.positionMs.coerceAtLeast(0))
                        .build()
                session.remoteMediaClient?.load(load)?.setResultCallback { result ->
                    if (pending?.videoId != request.videoId) return@setResultCallback
                    if (result.status.isSuccess) {
                        loadedVideoId = request.videoId
                        pending = null
                        refreshPlayback()
                    } else {
                        failed()
                    }
                } ?: failed()
            }
    }

    private fun failed(message: Int = R.string.google_cast_connection_failed) {
        application?.let { Toast.makeText(it, message, Toast.LENGTH_LONG).show() }
    }

    private fun clearSession() {
        loadJob?.cancel()
        pending = null
        loadedVideoId = null
        _connectedDevice.value = null
        bindRemote(null)
        if (!DlnaCastManager.isCasting) StreamProxyServer.getInstance().stop()
    }

    private val listener =
        object : SessionManagerListener<CastSession> {
            override fun onSessionStarted(
                session: CastSession,
                sessionId: String,
            ) {
                _connectedDevice.value = session.castDevice?.friendlyName
                bindRemote(session)
                load(session)
            }

            override fun onSessionResumed(
                session: CastSession,
                wasSuspended: Boolean,
            ) {
                _connectedDevice.value = session.castDevice?.friendlyName
                bindRemote(session)
                if (pending != null) load(session)
            }

            override fun onSessionEnded(
                session: CastSession,
                error: Int,
            ) {
                clearSession()
            }

            override fun onSessionStartFailed(
                session: CastSession,
                error: Int,
            ) {
                clearSession()
                failed()
            }

            override fun onSessionResumeFailed(
                session: CastSession,
                error: Int,
            ) {
                clearSession()
                failed()
            }

            override fun onSessionStarting(session: CastSession) = Unit

            override fun onSessionEnding(session: CastSession) = Unit

            override fun onSessionResuming(
                session: CastSession,
                sessionId: String,
            ) = Unit

            override fun onSessionSuspended(
                session: CastSession,
                reason: Int,
            ) = Unit
        }
}
