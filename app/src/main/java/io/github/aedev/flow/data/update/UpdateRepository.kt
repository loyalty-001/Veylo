package io.github.aedev.flow.data.update

import android.os.Build
import io.github.aedev.flow.BuildConfig
import io.github.aedev.flow.data.local.LocalDataManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val STABLE_RELEASE_API = "https://api.github.com/repos/loyalty-001/Veylo/releases/latest"
private const val NIGHTLY_RELEASE_API = "https://api.github.com/repos/loyalty-001/Veylo/releases/tags/nightly"
private const val NIGHTLY_CHANNEL = "nightly"
private const val GITHUB_JSON = "application/vnd.github+json"

/** Where an automatic check may surface a release; each announces a version once. */
enum class UpdateAnnouncement { LAUNCH_PAGE, NOTIFICATION }

/** How the automatic checks space themselves out and which releases they may show. */
internal object UpdateSchedule {
    val COOLDOWN_MS: Long = TimeUnit.HOURS.toMillis(12)

    fun isDue(
        lastCheckMs: Long,
        nowMs: Long,
    ): Boolean = nowMs < lastCheckMs || nowMs - lastCheckMs >= COOLDOWN_MS

    fun shouldFetch(
        announcement: UpdateAnnouncement,
        lastCheckMs: Long,
        nowMs: Long,
    ): Boolean = announcement == UpdateAnnouncement.LAUNCH_PAGE || isDue(lastCheckMs, nowMs)

    fun mayAnnounce(
        version: String,
        skippedVersion: String?,
        announcedVersion: String?,
    ): Boolean = version != skippedVersion && version != announcedVersion
}

/**
 * Shares release selection and skipped versions between the startup prompt, background checks
 * and the Settings button.
 */
@Singleton
class UpdateRepository
    @Inject
    constructor(
        private val client: OkHttpClient,
        private val dataManager: LocalDataManager,
    ) {
        private val mutex = Mutex()
        private val isNightly = BuildConfig.UPDATE_CHANNEL == NIGHTLY_CHANNEL
        private val _latest = MutableStateFlow<AppRelease?>(null)

        /** The newest release found this process, or null when none is known or this build is current. */
        val latest: StateFlow<AppRelease?> = _latest.asStateFlow()

        /** Asks GitHub now. Throws when the request fails, so a check the user started can say so. */
        suspend fun fetch(): AppRelease? = mutex.withLock { fetchLocked() }

        /**
         * Background checks share a cooldown. A launch checks once per activity even when a worker
         * checked recently, and returns only a version this surface has not shown or skipped.
         */
        suspend fun releaseToAnnounce(
            announcement: UpdateAnnouncement,
            nowMs: Long = System.currentTimeMillis(),
        ): AppRelease? =
            mutex.withLock {
                if (!UpdateSchedule.shouldFetch(announcement, dataManager.lastUpdateCheck.first(), nowMs)) return@withLock null
                val release = runCatching { fetchLocked() }.getOrNull() ?: return@withLock null
                val announced =
                    when (announcement) {
                        UpdateAnnouncement.LAUNCH_PAGE -> dataManager.promptedUpdateVersion.first()
                        UpdateAnnouncement.NOTIFICATION -> dataManager.notifiedUpdateVersion.first()
                    }
                if (!UpdateSchedule.mayAnnounce(release.version, dataManager.skippedUpdateVersion.first(), announced)) {
                    return@withLock null
                }
                // Launch prompts are recorded by the UI only when visible.
                if (announcement == UpdateAnnouncement.NOTIFICATION) {
                    dataManager.setNotifiedUpdateVersion(release.version)
                }
                release
            }

        suspend fun markPrompted(version: String) = dataManager.setPromptedUpdateVersion(version)

        suspend fun skip(version: String) = dataManager.setSkippedUpdateVersion(version)

        private suspend fun fetchLocked(): AppRelease? {
            val body =
                withContext(Dispatchers.IO) {
                    val request =
                        Request
                            .Builder()
                            .url(if (isNightly) NIGHTLY_RELEASE_API else STABLE_RELEASE_API)
                            .header("Accept", GITHUB_JSON)
                            .cacheControl(CacheControl.FORCE_NETWORK)
                            .build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw IOException("Update check failed: HTTP ${response.code}")
                        response.body.string()
                    }
                }
            val parsed = parseGitHubRelease(body)
            val release =
                if (isNightly) {
                    parsed.toNightlyReleaseIfNewer(BuildConfig.NIGHTLY_RUN)
                } else {
                    parsed.toAppReleaseIfNewer(BuildConfig.VERSION_NAME, Build.SUPPORTED_ABIS.toList())
                }
            dataManager.setLastUpdateCheck(System.currentTimeMillis())
            _latest.value = release
            return release
        }
    }
