package io.github.aedev.flow.player.googlecast

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Google Cast belongs to the GitHub flavor; FOSS keeps the existing DLNA path. */
object GoogleCastManager {
    val connectedDevice: StateFlow<String?> = MutableStateFlow(null)
    val playback: StateFlow<GoogleCastPlaybackState> = MutableStateFlow(GoogleCastPlaybackState())

    fun togglePlayback() = Unit

    fun seekTo(positionMs: Long) = Unit

    fun isAvailable(context: Context): Boolean = false

    fun openPicker(
        context: Context,
        request: GoogleCastRequest,
    ) = Unit
}
