package io.github.aedev.flow.ui.screens.music

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.aedev.flow.R
import io.github.aedev.flow.data.local.LikedVideoInfo
import io.github.aedev.flow.data.local.LikedVideosRepository
import io.github.aedev.flow.data.music.PlaylistRepository
import io.github.aedev.flow.data.music.model.MusicTrack
import io.github.aedev.flow.data.recommendation.music.MusicBrainEngine
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h891dp")
class MusicTasteDialogTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun selectingASongUsesFavoritesAndRefreshesOnlyAfterAChange() {
        val track = MusicTrack("song", "Chosen song", "Chosen artist", "", 180)
        val repository = mockk<PlaylistRepository>()
        val favorites = MutableStateFlow<List<MusicTrack>>(emptyList())
        every { repository.favorites } returns favorites
        val likes = mockk<LikedVideosRepository>()
        val brain = mockk<MusicBrainEngine>()
        every { likes.getLikedMusicFlow() } returns flowOf(emptyList())
        every { likes.getLikeState(track.videoId) } returns flowOf(null)
        coEvery { repository.isFavorite(track.videoId) } returns false
        coEvery { repository.addToFavorites(track) } coAnswers { favorites.value = listOf(track) }
        coEvery { likes.likeVideo(any()) } returns Unit
        coEvery { brain.onExplicitLike(track) } returns Unit
        val viewModel = MusicTasteViewModel(repository, likes, brain)
        viewModel.state.value = MusicTasteSearch(tracks = listOf(track), searched = true)
        var changed: Boolean? = null
        val context = ApplicationProvider.getApplicationContext<Application>()
        rule.setContent { MaterialTheme { MusicTasteDialog(onDismiss = { changed = it }, viewModel = viewModel) } }
        rule.onNodeWithContentDescription(track.title).performClick()
        rule.waitUntil { !viewModel.state.value.saving }
        coVerify(exactly = 1) { repository.addToFavorites(track) }
        coVerify(exactly = 1) { likes.likeVideo(match { it.videoId == track.videoId && it.isMusic }) }
        coVerify(exactly = 1) { brain.onExplicitLike(track) }
        rule.onNodeWithText(context.getString(R.string.music_taste_done)).performClick()
        assertEquals(true, changed)
        assertEquals(false, viewModel.consumeChanges())
    }

    @Test
    fun unselectingAnExistingLibraryLikeRemovesBothLikeAndSeed() {
        val track = MusicTrack("saved", "Saved song", "Artist", "", 180)
        val repository = mockk<PlaylistRepository>()
        val likes = mockk<LikedVideosRepository>()
        val brain = mockk<MusicBrainEngine>(relaxed = true)
        val saved = MutableStateFlow(listOf(LikedVideoInfo(track.videoId, track.title, "", track.artist, isMusic = true)))
        every { repository.favorites } returns flowOf(emptyList())
        every { likes.getLikedMusicFlow() } returns saved
        every { likes.getLikeState(track.videoId) } returns flowOf("LIKED")
        coEvery { repository.isFavorite(track.videoId) } returns false
        coEvery { repository.removeFromFavorites(track.videoId) } returns Unit
        coEvery { likes.removeLikeState(track.videoId) } coAnswers { saved.value = emptyList() }
        val viewModel = MusicTasteViewModel(repository, likes, brain)
        viewModel.state.value = MusicTasteSearch(tracks = listOf(track), searched = true)
        rule.setContent { MaterialTheme { MusicTasteDialog(onDismiss = {}, viewModel = viewModel) } }
        rule.waitUntil { track.videoId in viewModel.favorites.value }
        rule.onNodeWithContentDescription(track.title).performClick()
        rule.waitUntil { !viewModel.state.value.saving }
        coVerify(exactly = 1) { repository.removeFromFavorites(track.videoId) }
        coVerify(exactly = 1) { likes.removeLikeState(track.videoId) }
        coVerify(exactly = 0) { brain.onExplicitLike(any()) }
    }
}
