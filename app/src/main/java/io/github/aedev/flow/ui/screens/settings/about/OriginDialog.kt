package io.github.aedev.flow.ui.screens.settings.about

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import io.github.aedev.flow.R
import io.github.aedev.flow.ui.components.shared.FlowAlertDialog

@Composable
internal fun OriginDialog(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    FlowAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.veylo_original_project)) },
        text = { Text(stringResource(R.string.veylo_origin_notice)) },
        confirmButton = {
            TextButton(onClick = { runCatching { uriHandler.openUri("https://github.com/Flow-Tube/Flow") } }) {
                Text(stringResource(R.string.veylo_original_source))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}
