package io.github.aedev.flow.player.googlecast

data class GoogleCastPlaybackState(
    val device: String? = null,
    val videoId: String? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
)
