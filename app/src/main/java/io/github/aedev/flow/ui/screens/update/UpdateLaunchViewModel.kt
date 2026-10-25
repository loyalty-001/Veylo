package io.github.aedev.flow.ui.screens.update

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.aedev.flow.BuildConfig
import io.github.aedev.flow.R
import io.github.aedev.flow.data.update.AppRelease
import io.github.aedev.flow.data.update.UpdateAnnouncement
import io.github.aedev.flow.data.update.UpdateRepository
import io.github.aedev.flow.ui.components.shared.FlowAlertDialog
import io.github.aedev.flow.ui.startup.LaunchPrompts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Checks once per activity and holds a newer release until it can be shown as a prompt. */
@HiltViewModel
class UpdateLaunchViewModel
    @Inject
    constructor(
        private val updates: UpdateRepository,
        private val prompts: LaunchPrompts,
    ) : ViewModel() {
        private var checked = false
        private var shownVersion: String? = null
        private val _pendingRelease = MutableStateFlow<AppRelease?>(null)
        val pendingRelease: StateFlow<AppRelease?> = _pendingRelease.asStateFlow()

        fun check() {
            if (checked) return
            checked = true
            if (BuildConfig.DEBUG || !BuildConfig.UPDATER_ENABLED) {
                prompts.updateCheckFinished(shownUpdate = false)
                return
            }
            viewModelScope.launch {
                val release = updates.releaseToAnnounce(UpdateAnnouncement.LAUNCH_PAGE)
                prompts.updateCheckFinished(shownUpdate = release != null)
                _pendingRelease.value = release
            }
        }

        fun promptShown(version: String) {
            if (shownVersion == version) return
            shownVersion = version
            viewModelScope.launch { updates.markPrompted(version) }
        }

        fun dismiss() {
            _pendingRelease.value?.let { promptShown(it.version) }
            _pendingRelease.value = null
        }
    }

/** Offers the update after onboarding, when the app is ready to display a dialog. */
@Composable
fun UpdateLaunchEffect(
    needsOnboarding: Boolean?,
    onOpenUpdate: () -> Unit,
    enabled: Boolean = true,
) {
    val activity = LocalContext.current as? ComponentActivity ?: return
    val viewModel: UpdateLaunchViewModel = hiltViewModel(activity)
    val pendingRelease by viewModel.pendingRelease.collectAsStateWithLifecycle()
    LaunchedEffect(needsOnboarding, enabled) {
        if (needsOnboarding == false && enabled) viewModel.check()
    }
    val release = pendingRelease ?: return
    if (needsOnboarding != false || !enabled) return

    LaunchedEffect(release.version) { viewModel.promptShown(release.version) }
    FlowAlertDialog(
        onDismissRequest = viewModel::dismiss,
        title = { Text(stringResource(R.string.new_update_available)) },
        text = { Text(stringResource(R.string.update_title, release.version)) },
        confirmButton = {
            TextButton(onClick = {
                viewModel.dismiss()
                onOpenUpdate()
            }) { Text(stringResource(R.string.update_flow)) }
        },
        dismissButton = {
            TextButton(onClick = viewModel::dismiss) { Text(stringResource(R.string.update_not_now)) }
        },
    )
}
