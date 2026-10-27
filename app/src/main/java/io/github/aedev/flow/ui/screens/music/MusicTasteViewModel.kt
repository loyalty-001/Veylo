package io.github.aedev.flow.ui.screens.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.aedev.flow.data.music.PlaylistRepository
import io.github.aedev.flow.data.music.model.MusicTrack
import io.github.aedev.flow.data.music.model.audioMusicOnly
import io.github.aedev.flow.data.newmusic.InnertubeMusicService
import io.github.aedev.flow.innertube.YouTube
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal data class MusicTasteSearch(
    val tracks: List<MusicTrack> = emptyList(),
    val loading: Boolean = false,
    val searched: Boolean = false,
    val failed: Boolean = false,
    val saving: Boolean = false,
)

@HiltViewModel
internal class MusicTasteViewModel
    @Inject
    constructor(
        private val playlists: PlaylistRepository,
    ) : ViewModel() {
        val state = MutableStateFlow(MusicTasteSearch())
        val favorites =
            playlists.favorites
                .map { tracks -> tracks.map { it.videoId }.toSet() }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
        private var changed = false

        fun consumeChanges(): Boolean = changed.also { changed = false }

        private var searchJob: Job? = null

        fun search(query: String) {
            val term = query.trim().take(100)
            if (term.length < 2 || state.value.saving) return
            searchJob?.cancel()
            searchJob =
                viewModelScope.launch {
                    state.value = MusicTasteSearch(loading = true)
                    try {
                        val tracks =
                            withContext(Dispatchers.IO) {
                                YouTube
                                    .search(term, YouTube.SearchFilter.FILTER_SONG)
                                    .getOrThrow()
                                    .items
                                    .mapNotNull(InnertubeMusicService::convertToMusicTrack)
                                    .audioMusicOnly()
                                    .distinctBy { it.videoId }
                                    .take(12)
                            }
                        state.value = MusicTasteSearch(tracks = tracks, searched = true)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        state.value = MusicTasteSearch(searched = true, failed = true)
                    }
                }
        }

        fun toggle(track: MusicTrack) {
            if (state.value.saving || state.value.loading) return
            state.value = state.value.copy(saving = true, failed = false)
            viewModelScope.launch {
                try {
                    playlists.toggleFavorite(track)
                    changed = true
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    state.value = state.value.copy(failed = true)
                } finally {
                    state.value = state.value.copy(saving = false)
                }
            }
        }

        fun close() {
            searchJob?.cancel()
            state.value = state.value.copy(loading = false)
        }
    }
