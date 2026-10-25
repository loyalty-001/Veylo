package io.github.aedev.flow.player.googlecast

import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.VideoStream

/** A snapshot of the media the user explicitly chose to send to a Cast receiver. */
data class GoogleCastRequest(
    val videoId: String,
    val title: String,
    val positionMs: Long,
    val durationMs: Long,
    val videoStreams: List<VideoStream>,
    val audioStreams: List<AudioStream>,
    val liveUrl: String? = null,
)
