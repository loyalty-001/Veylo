package io.github.aedev.flow.ui.components.operations

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.aedev.flow.R
import io.github.aedev.flow.ui.components.shared.FlowAlertDialog

@Composable
internal fun AnalyticsNotice(
    onContinue: () -> Unit,
    onDisable: () -> Unit,
) {
    FlowAlertDialog(
        onDismissRequest = onDisable,
        title = { Text(stringResource(R.string.operations_analytics_notice_title)) },
        text = { Text(stringResource(R.string.operations_analytics_notice)) },
        confirmButton = { TextButton(onClick = onContinue) { Text(stringResource(R.string.operations_continue)) } },
        dismissButton = { TextButton(onClick = onDisable) { Text(stringResource(R.string.operations_turn_off)) } },
    )
}
