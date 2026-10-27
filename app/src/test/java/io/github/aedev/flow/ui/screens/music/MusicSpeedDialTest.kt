package io.github.aedev.flow.ui.screens.music

import io.github.aedev.flow.data.music.model.MusicTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicSpeedDialTest {
    @Test
    fun suggestionsCannotPopulateSpeedDialWithoutListeningHistory() {
        assertTrue(listenedSpeedDial(emptyList(), listOf(track("suggested"))).isEmpty())
    }

    @Test
    fun rankingOnlyReordersPlayedTracksAndKeepsNewListens() {
        val history = listOf(track("new"), track("repeat"), track("older"))
        val ranked = listOf(track("unplayed"), track("repeat"), track("repeat"))
        assertEquals(listOf("repeat", "new", "older"), listenedSpeedDial(history, ranked).map { it.videoId })
        assertTrue(listenedSpeedDial(emptyList(), ranked).isEmpty())
    }

    private fun track(id: String) = MusicTrack(id, id, "Artist", "", 180)
}
