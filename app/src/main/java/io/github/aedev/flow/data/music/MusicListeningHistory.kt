package io.github.aedev.flow.data.music

import io.github.aedev.flow.data.local.AppDatabase
import io.github.aedev.flow.data.local.entity.WatchHistoryEntity
import io.github.aedev.flow.data.music.model.MusicArtist
import io.github.aedev.flow.data.music.model.MusicTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest

internal const val MUSIC_HISTORY_LIMIT = 100

@OptIn(ExperimentalCoroutinesApi::class)
internal fun musicListeningHistory(
    database: AppDatabase,
    metadata: Flow<List<MusicTrack>>,
): Flow<List<MusicTrack>> {
    val recent =
        database.invalidationTracker.createFlow("watch_history").mapLatest {
            database.watchHistoryDao().getHistoryPage(
                isMusic = 1,
                isLocal = 0,
                beforeTimestamp = Long.MAX_VALUE,
                afterVideoId = "",
                limit = MUSIC_HISTORY_LIMIT,
            )
        }
    return combine(recent, metadata, ::restoreMusicHistory)
        .distinctUntilChanged()
        .flowOn(Dispatchers.IO)
}

// Room owns membership and recency; legacy metadata must never resurrect cleared history.
internal fun restoreMusicHistory(
    rows: List<WatchHistoryEntity>,
    metadata: List<MusicTrack>,
): List<MusicTrack> {
    val known = metadata.associateBy { it.videoId }
    return rows.map { row ->
        val previous = known[row.videoId]
        val artist = row.channelName.ifBlank { previous?.artist.orEmpty() }
        val channel = row.channelId.ifBlank { previous?.channelId.orEmpty() }
        (previous ?: MusicTrack(row.videoId, row.title, artist, row.thumbnailUrl, 0)).copy(
            title = row.title.ifBlank { previous?.title.orEmpty() },
            artist = artist,
            thumbnailUrl = row.thumbnailUrl.ifBlank { previous?.thumbnailUrl.orEmpty() },
            duration =
                if (row.duration >
                    0
                ) {
                    (row.duration / 1000).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                } else {
                    previous?.duration ?: 0
                },
            channelId = channel,
            artists =
                previous?.artists?.takeIf { it.isNotEmpty() }
                    ?: listOf(MusicArtist(artist, channel.takeIf { it.isNotBlank() })),
        )
    }
}
