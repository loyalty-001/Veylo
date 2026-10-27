package io.github.aedev.flow.data.music

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.aedev.flow.data.local.AppDatabase
import io.github.aedev.flow.data.local.entity.WatchHistoryEntity
import io.github.aedev.flow.data.music.model.MusicArtist
import io.github.aedev.flow.data.music.model.MusicTrack
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
@ConscryptMode(ConscryptMode.Mode.OFF)
class MusicListeningHistoryTest {
    @Test
    fun roomPlaybackUpdatesAndClearsRecommendationSeeds() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Application>()
            val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
            val emissions = Channel<List<MusicTrack>>(Channel.UNLIMITED)
            val legacy = track("stale")
            val collector =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    musicListeningHistory(database, flowOf(listOf(legacy))).collect { emissions.send(it) }
                }
            try {
                assertTrue(withTimeout(5_000) { emissions.receive() }.isEmpty())
                withContext(Dispatchers.IO) { database.watchHistoryDao().upsert(row("heard", 1)) }
                assertEquals(listOf("heard"), withTimeout(5_000) { emissions.receive() }.map { it.videoId })
                withContext(Dispatchers.IO) { database.watchHistoryDao().upsert(row("newer", 2)) }
                assertEquals(listOf("newer", "heard"), withTimeout(5_000) { emissions.receive() }.map { it.videoId })
                withContext(Dispatchers.IO) { database.watchHistoryDao().clearAll() }
                assertTrue(withTimeout(5_000) { emissions.receive() }.isEmpty())
            } finally {
                collector.cancel()
                collector.join()
                database.close()
            }
        }

    @Test
    fun seedQueryIsBoundedAndExcludesVideosAndDeviceFiles() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Application>()
            val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
            try {
                withContext(Dispatchers.IO) {
                    database.watchHistoryDao().upsertAll(
                        (1..105).map { row("song-$it", it.toLong()) } +
                            row("video", 110).copy(isMusic = false) +
                            row("local", 111).copy(isLocal = true),
                    )
                }
                val history = withTimeout(5_000) { musicListeningHistory(database, flowOf(emptyList())).first() }
                assertEquals(MUSIC_HISTORY_LIMIT, history.size)
                assertEquals("song-105", history.first().videoId)
                assertEquals("song-6", history.last().videoId)
                assertTrue(history.none { it.videoId == "video" || it.videoId == "local" })
            } finally {
                database.close()
            }
        }

    @Test
    fun legacyMetadataEnrichesExistingRowsWithoutAddingUnplayedTracks() {
        val metadata = track("heard").copy(albumId = "album", artists = listOf(MusicArtist("Artist", "UCartist")))
        val restored = restoreMusicHistory(listOf(row("heard", 1)), listOf(track("stale"), metadata)).single()
        assertEquals("heard", restored.videoId)
        assertEquals(180, restored.duration)
        assertEquals("UCartist", restored.artists.single().id)
        assertEquals("album", restored.albumId)
    }

    private fun row(
        id: String,
        time: Long,
    ) = WatchHistoryEntity(
        videoId = id,
        position = 0,
        duration = 180_000,
        timestamp = time,
        title = "Song $id",
        thumbnailUrl = "https://example.com/$id.jpg",
        channelName = "Artist",
        channelId = "UCartist",
        isMusic = true,
    )

    private fun track(id: String) = MusicTrack(id, "Song $id", "Artist", "", 180)
}
