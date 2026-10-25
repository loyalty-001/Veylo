package io.github.aedev.flow.player.googlecast

import android.content.Context
import io.github.aedev.flow.data.shorts.ShortsDashManifest
import io.github.aedev.flow.player.dlna.StreamProxyServer

internal data class PreparedCastMedia(
    val url: String,
    val contentType: String,
    val live: Boolean = false,
)

/** Uses the existing phone relay so the TV does not need the phone's YouTube request identity. */
internal object GoogleCastMedia {
    suspend fun prepare(
        context: Context,
        request: GoogleCastRequest,
    ): PreparedCastMedia? {
        request.liveUrl?.takeIf { it.startsWith("https://") }?.let {
            return PreparedCastMedia(it, "application/x-mpegURL", live = true)
        }
        val video =
            request.videoStreams
                .filter { it.format?.mimeType == "video/mp4" }
                .filter { it.itagItem?.codec?.startsWith("avc") == true }
                .filter { it.height in 1..1080 }
                .maxByOrNull { it.height } ?: return null
        val proxy = StreamProxyServer.getInstance()
        proxy.start(context)
        if (!proxy.awaitReady()) return null
        if (!video.isVideoOnly) {
            val url = video.content?.takeIf { it.isNotBlank() } ?: return null
            return PreparedCastMedia(proxy.registerStream(url, "video/mp4"), "video/mp4")
        }
        val audio =
            request.audioStreams.firstOrNull {
                it.format?.mimeType in setOf("audio/mp4", "audio/m4a") && it.itagItem?.codec?.startsWith("mp4a") == true
            } ?: return null
        val videoMpd = ShortsDashManifest.forStream(video, request.durationMs) ?: return null
        val audioMpd = ShortsDashManifest.forStream(audio, request.durationMs) ?: return null
        val manifest = CastDashManifest.combine(videoMpd, audioMpd) { url, mime -> proxy.registerStream(url, mime) }
        return PreparedCastMedia(proxy.registerDashManifest(manifest), "application/dash+xml")
    }
}
