package io.github.aedev.flow.ui.components.operations

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.aedev.flow.BuildConfig
import io.github.aedev.flow.R
import io.github.aedev.flow.data.operations.AnalyticsDelivery
import io.github.aedev.flow.ui.components.shared.FlowAlertDialog
import io.github.aedev.flow.ui.components.shared.FlowFilterChip
import io.github.aedev.flow.ui.components.shared.FlowSwitchRow
import io.github.aedev.flow.ui.screens.operations.OperationsViewModel

@Composable
internal fun OperationsSettings() {
    if (!BuildConfig.UPDATER_ENABLED) return
    val activity = LocalContext.current as? ComponentActivity ?: return
    val viewModel: OperationsViewModel = hiltViewModel(activity)
    val enabled by viewModel.analyticsEnabled.collectAsStateWithLifecycle()
    val delivery by viewModel.analyticsDelivery.collectAsStateWithLifecycle()
    val report by viewModel.reportState.collectAsStateWithLifecycle()
    var open by rememberSaveable { mutableStateOf(false) }
    var text by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("other") }
    var errorCode by rememberSaveable { mutableStateOf("") }
    var details by rememberSaveable { mutableStateOf(false) }
    var preview by rememberSaveable { mutableStateOf(false) }
    val categories =
        remember {
            listOf(
                "playback" to R.string.operations_category_playback,
                "casting" to R.string.operations_category_casting,
                "downloads" to R.string.operations_category_downloads,
                "updates" to R.string.operations_category_updates,
                "other" to R.string.operations_category_other,
            )
        }
    val reportMessage =
        listOfNotNull(
            text.trim(),
            errorCode.trim().takeIf { it.isNotEmpty() }?.let { stringResource(R.string.operations_error_code_value, it) },
        ).joinToString("\n\n")
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
                    supportingText = stringResource(R.string.operations_analytics_summary),
                    checked = enabled,
                    onCheckedChange = viewModel::analytics,
                )
                TextButton(onClick = { details = !details }) { Text(stringResource(R.string.operations_privacy_details)) }
                if (details) Text(stringResource(R.string.operations_privacy))
                Text(
                    stringResource(
                        when {
                            !enabled && delivery != AnalyticsDelivery.DELETION_PENDING -> R.string.operations_status_off
                            delivery == AnalyticsDelivery.DELETION_PENDING -> R.string.operations_status_deletion
                            delivery == AnalyticsDelivery.SENT -> R.string.operations_status_sent
                            delivery == AnalyticsDelivery.FAILED -> R.string.operations_status_failed
                            delivery == AnalyticsDelivery.PAUSED -> R.string.operations_status_paused
                            else -> R.string.operations_status_idle
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
                if ((enabled && delivery in listOf(AnalyticsDelivery.FAILED, AnalyticsDelivery.IDLE, AnalyticsDelivery.PAUSED)) ||
                    delivery == AnalyticsDelivery.DELETION_PENDING
                ) {
                    TextButton(onClick = viewModel::retryAnalytics) { Text(stringResource(R.string.retry)) }
                }
                Text(stringResource(R.string.operations_report_notice))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { (key, label) ->
                        FlowFilterChip(label = stringResource(label), selected = category == key, onClick = {
                            category = key
                            preview = false
                        })
                    }
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        if (it.length <= 1850) text = it
                        preview = false
                    },
                    label = { Text(stringResource(R.string.operations_report_message)) },
                    minLines = 4,
                    enabled = !report.sending,
                )
                OutlinedTextField(
                    value = errorCode,
                    onValueChange = {
                        if (it.length <= 80 &&
                            it.all { c -> c.isLetterOrDigit() || c in " _.-" }
                        ) {
                            errorCode = it
                        }
                        preview = false
                    },
                    label = { Text(stringResource(R.string.operations_error_code)) },
                    singleLine = true,
                    enabled = !report.sending,
                )
                if (preview) {
                    Text(stringResource(R.string.operations_preview_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE))
                    Text(reportMessage)
                }
                when {
                    report.sent -> Text(stringResource(R.string.operations_report_sent))
                    report.failed -> Text(stringResource(R.string.operations_report_failed))
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = text.trim().length >= 10 && !report.sending && !report.sent,
                onClick = { if (preview) viewModel.sendReport(category, reportMessage) else preview = true },
            ) { Text(stringResource(if (preview) R.string.operations_send_report else R.string.operations_review_report)) }
        },
        dismissButton = {
            TextButton(enabled = !report.sending, onClick = { open = false }) { Text(stringResource(R.string.operations_close)) }
        },
    )
}
