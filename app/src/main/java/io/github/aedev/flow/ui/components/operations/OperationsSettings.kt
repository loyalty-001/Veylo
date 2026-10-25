package io.github.aedev.flow.ui.components.operations

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.aedev.flow.BuildConfig
import io.github.aedev.flow.R
import io.github.aedev.flow.ui.components.shared.FlowAlertDialog
import io.github.aedev.flow.ui.components.shared.FlowSwitchRow
import io.github.aedev.flow.ui.screens.operations.OperationsViewModel

@Composable
internal fun OperationsSettings() {
    if (!BuildConfig.UPDATER_ENABLED) return
    val activity = LocalContext.current as? ComponentActivity ?: return
    val viewModel: OperationsViewModel = hiltViewModel(activity)
    val enabled by viewModel.analyticsEnabled.collectAsStateWithLifecycle()
    val report by viewModel.reportState.collectAsStateWithLifecycle()
    var open by rememberSaveable { mutableStateOf(false) }
    var text by rememberSaveable { mutableStateOf("") }
    TextButton(onClick = {
        open = true
        viewModel.resetReport()
    }) { Text(stringResource(R.string.operations_settings)) }
    if (!open) return
    FlowAlertDialog(
        onDismissRequest = { if (!report.sending) open = false },
        title = { Text(stringResource(R.string.operations_settings)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                FlowSwitchRow(
                    title = stringResource(R.string.operations_analytics),
                    supportingText = stringResource(R.string.operations_privacy),
                    checked = enabled,
                    onCheckedChange = viewModel::analytics,
                )
                Text(stringResource(R.string.operations_report_notice))
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= 2000) text = it },
                    label = { Text(stringResource(R.string.operations_report_message)) },
                    minLines = 4,
                    enabled = !report.sending,
                )
                when {
                    report.sent -> Text(stringResource(R.string.operations_report_sent))
                    report.failed -> Text(stringResource(R.string.operations_report_failed))
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = text.trim().length >= 10 && !report.sending && !report.sent,
                onClick = { viewModel.sendReport("other", text) },
            ) { Text(stringResource(R.string.operations_send_report)) }
        },
        dismissButton = {
            TextButton(enabled = !report.sending, onClick = { open = false }) { Text(stringResource(R.string.operations_close)) }
        },
    )
}
