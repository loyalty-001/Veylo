package io.github.aedev.flow.ui.components.operations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.R
import io.github.aedev.flow.ui.components.shared.FlowAlertDialog

@Composable
internal fun GettingStarted() {
    var open by rememberSaveable { mutableStateOf(false) }
    TextButton(onClick = { open = true }) { Text(stringResource(R.string.veylo_getting_started)) }
    if (open) GettingStartedDialog(onClose = { open = false })
}

@Composable
internal fun GettingStartedDialog(onClose: () -> Unit) {
    FlowAlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.veylo_getting_started)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(
                    R.string.veylo_guide_music_title to R.string.veylo_guide_music,
                    R.string.veylo_guide_downloads_title to R.string.veylo_guide_downloads,
                    R.string.veylo_guide_background_title to R.string.veylo_guide_background,
                    R.string.veylo_guide_playlists_title to R.string.veylo_guide_playlists,
                    R.string.veylo_guide_casting_title to R.string.veylo_guide_casting,
                ).forEach { (title, body) ->
                    Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(body))
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.operations_close)) } },
    )
}
