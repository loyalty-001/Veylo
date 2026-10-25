package io.github.aedev.flow.ui.screens.operations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.aedev.flow.BuildConfig
import io.github.aedev.flow.data.operations.AppAnnouncement
import io.github.aedev.flow.data.operations.OperationsRepository
import io.github.aedev.flow.data.operations.isEligible
import io.github.aedev.flow.ui.startup.LaunchPrompts
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class OperationsUiState(
    val sending: Boolean = false,
    val sent: Boolean = false,
    val failed: Boolean = false,
)

@HiltViewModel
internal class OperationsViewModel
    @Inject
    constructor(
        private val operations: OperationsRepository,
        private val prompts: LaunchPrompts,
    ) : ViewModel() {
        private val notes = MutableStateFlow<List<AppAnnouncement>>(emptyList())
        private val dismissedThisSession = MutableStateFlow<Set<String>>(emptySet())
        private val serverOffset = MutableStateFlow(0L)
        val reportState = MutableStateFlow(OperationsUiState())
        val analyticsEnabled = operations.analyticsEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
        val pending =
            combine(notes, operations.dismissed, dismissedThisSession, serverOffset) { all, saved, session, offset ->
                all.firstOrNull { it.isEligible(BuildConfig.VERSION_CODE, System.currentTimeMillis() + offset, saved + session) }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        suspend fun refresh() {
            if (!BuildConfig.UPDATER_ENABLED) {
                prompts.operationsCheckFinished(false)
                return
            }
            try {
                val config = operations.configuration(force = true)
                serverOffset.value = config.serverTime - System.currentTimeMillis()
                notes.value = config.announcements
                prompts.operationsCheckFinished(config.announcements.isNotEmpty())
                operations.reportActivity()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Expired or unpublished content must not survive a failed refresh.
                notes.value = emptyList()
                prompts.operationsCheckFinished(false)
            }
        }

        fun dismiss(note: AppAnnouncement) {
            dismissedThisSession.value += note.dismissalKey
            if (note.onceOnly) viewModelScope.launch { operations.dismiss(note) }
        }

        fun analytics(enabled: Boolean) {
            viewModelScope.launch {
                try {
                    operations.setAnalyticsEnabled(enabled)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                }
            }
        }

        fun health(category: String) {
            viewModelScope.launch {
                try {
                    operations.reportActivity(category)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                }
            }
        }

        fun sendReport(
            category: String,
            message: String,
        ) {
            if (reportState.value.sending) return
            reportState.value = OperationsUiState(sending = true)
            viewModelScope.launch {
                try {
                    operations.sendReport(category, message)
                    reportState.value = OperationsUiState(sent = true)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    reportState.value = OperationsUiState(failed = true)
                }
            }
        }

        fun resetReport() {
            reportState.value = OperationsUiState()
        }
    }
