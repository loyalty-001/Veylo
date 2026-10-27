package io.github.aedev.flow.ui.components.operations

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import io.github.aedev.flow.BuildConfig
import io.github.aedev.flow.MainActivity
import io.github.aedev.flow.R
import io.github.aedev.flow.data.operations.allowedAnnouncementLink
import io.github.aedev.flow.player.googlecast.GoogleCastManager
import io.github.aedev.flow.ui.components.shared.FlowAlertDialog
import io.github.aedev.flow.ui.screens.operations.OperationsViewModel
import io.github.aedev.flow.ui.screens.update.UpdateLaunchViewModel
import kotlinx.coroutines.delay

@Composable
internal fun OperationsHost(
    enabled: Boolean,
    hasPlaybackError: Boolean,
) {
    val activity = LocalContext.current as? ComponentActivity ?: return
    if (activity.intent?.getBooleanExtra(MainActivity.EXTRA_BENCHMARK_BYPASS_ONBOARDING, false) == true) return
    val viewModel: OperationsViewModel = hiltViewModel(activity)
    val updates: UpdateLaunchViewModel = hiltViewModel(activity)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val note by viewModel.pending.collectAsStateWithLifecycle()
    val update by updates.pendingRelease.collectAsStateWithLifecycle()
    val castFailures by GoogleCastManager.failureCount.collectAsStateWithLifecycle()
    LaunchedEffect(castFailures) { if (castFailures > 0) viewModel.health("cast_error") }
    LaunchedEffect(lifecycle, viewModel, enabled) {
        if (!enabled) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                viewModel.refresh()
                delay(60_000)
            }
        }
    }
    LaunchedEffect(hasPlaybackError) { if (hasPlaybackError) viewModel.health("playback_error") }
    val guideNeeded by viewModel.guideNeeded.collectAsStateWithLifecycle()
    val noticeNeeded by viewModel.analyticsNoticeNeeded.collectAsStateWithLifecycle()
    if (!enabled || update != null || !BuildConfig.UPDATER_ENABLED) return
    if (noticeNeeded == null) return
    if (noticeNeeded == true) {
        AnalyticsNotice(onContinue = { viewModel.analytics(true) }, onDisable = { viewModel.analytics(false) })
        return
    }
    if (guideNeeded) {
        GettingStartedDialog(onClose = viewModel::completeGuide)
        return
    }
    val announcement = note ?: return
    val uri = LocalUriHandler.current
    if (announcement.kind == "banner") {
        AnnouncementBanner(announcement, onDismiss = { viewModel.dismiss(announcement) }, onAction = {
            if (allowedAnnouncementLink(announcement.actionUrl)) uri.openUri(announcement.actionUrl)
            viewModel.dismiss(announcement)
        })
    } else {
        FlowAlertDialog(
            onDismissRequest = { viewModel.dismiss(announcement) },
            title = { Text(announcement.title) },
            text = { Text(announcement.body) },
            confirmButton = {
                if (allowedAnnouncementLink(announcement.actionUrl)) {
                    TextButton(onClick = {
                        uri.openUri(announcement.actionUrl)
                        viewModel.dismiss(announcement)
                    }) { Text(announcement.actionLabel) }
                } else {
                    TextButton(onClick = { viewModel.dismiss(announcement) }) { Text(stringResource(R.string.operations_dismiss)) }
                }
            },
            dismissButton = {
                if (allowedAnnouncementLink(announcement.actionUrl)) {
                    TextButton(onClick = { viewModel.dismiss(announcement) }) { Text(stringResource(R.string.operations_dismiss)) }
                }
            },
        )
    }
}
