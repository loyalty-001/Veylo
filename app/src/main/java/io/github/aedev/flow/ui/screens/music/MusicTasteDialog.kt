package io.github.aedev.flow.ui.screens.music

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.aedev.flow.R
import io.github.aedev.flow.ui.components.shared.FlowAlertDialog
import io.github.aedev.flow.ui.components.shared.FlowSearchField

@Composable
internal fun MusicTasteDialog(
    onDismiss: (Boolean) -> Unit,
    viewModel: MusicTasteViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val query = rememberTextFieldState()
    val close = {
        viewModel.close()
        onDismiss(viewModel.consumeChanges())
    }
    FlowAlertDialog(
        onDismissRequest = { if (!state.saving) close() },
        title = { Text(stringResource(R.string.music_taste_setup)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.music_taste_setup_summary))
                FlowSearchField(
                    state = query,
                    placeholder = stringResource(R.string.music_taste_search),
                    onSearch = viewModel::search,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(
                    onClick = { viewModel.search(query.text.toString()) },
                    enabled = query.text.trim().length >= 2 && !state.saving,
                ) {
                    Text(stringResource(if (state.loading) R.string.music_taste_searching else R.string.search))
                }
                if (state.failed) Text(stringResource(R.string.music_taste_failed))
                if (state.searched && !state.failed && state.tracks.isEmpty()) Text(stringResource(R.string.music_taste_empty))
                state.tracks.forEach { track ->
                    ListItem(
                        headlineContent = { Text(track.title) },
                        supportingContent = { Text(track.artist) },
                        trailingContent = {
                            Checkbox(
                                checked = track.videoId in favorites,
                                onCheckedChange = { viewModel.toggle(track) },
                                enabled = !state.saving,
                                modifier = Modifier.semantics { contentDescription = track.title },
                            )
                        },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = close, enabled = !state.saving) { Text(stringResource(R.string.music_taste_done)) }
        },
    )
}
