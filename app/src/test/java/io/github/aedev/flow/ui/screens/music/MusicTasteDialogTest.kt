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
import io.github.aedev.flow.data.music.PlaylistRepository
import io.github.aedev.flow.data.music.model.MusicTrack
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
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
        coEvery { repository.toggleFavorite(track) } coAnswers {
            favorites.value = listOf(track)
            true
        }
        val viewModel = MusicTasteViewModel(repository)
        viewModel.state.value = MusicTasteSearch(tracks = listOf(track), searched = true)
        var changed: Boolean? = null
        val context = ApplicationProvider.getApplicationContext<Application>()
        rule.setContent { MaterialTheme { MusicTasteDialog(onDismiss = { changed = it }, viewModel = viewModel) } }
        rule.onNodeWithContentDescription(track.title).performClick()
        rule.waitUntil { !viewModel.state.value.saving }
        coVerify(exactly = 1) { repository.toggleFavorite(track) }
        rule.onNodeWithText(context.getString(R.string.music_taste_done)).performClick()
        assertEquals(true, changed)
        assertEquals(false, viewModel.consumeChanges())
    }
}
