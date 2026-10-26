package io.github.aedev.flow.data.operations

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.aedev.flow.BuildConfig
import io.github.aedev.flow.data.local.safePreferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private val Context.operationsStore by safePreferencesDataStore(name = "veylo_operations")
private const val API = "https://veylo-operations.scrde.workers.dev/v1"

@Singleton
class OperationsRepository
    @Inject
    constructor(
        @ApplicationContext context: Context,
        client: OkHttpClient,
    ) {
        private val store = context.operationsStore
        private val http = client.newBuilder().callTimeout(6, TimeUnit.SECONDS).build()
        private val json = OperationsJson
        private val consentKey = booleanPreferencesKey("analytics_enabled")
        private val idKey = stringPreferencesKey("installation_id")
        private val firstReportedKey = booleanPreferencesKey("first_launch_reported")
        private val forgetKey = stringSetPreferencesKey("pending_forget_ids")
        private val dismissedKey = stringSetPreferencesKey("dismissed_announcements")
        private val mutex = Mutex()
        private var lastActivityDay: String? = null
        private var lastConfigAt = 0L
        private var cachedConfig = OperationsConfig()
        private val healthSent = mutableSetOf<String>()
        val analyticsEnabled = store.data.map { it[consentKey] ?: false }
        val dismissed = store.data.map { it[dismissedKey] ?: emptySet() }

        internal suspend fun configuration(force: Boolean = false): OperationsConfig =
            mutex.withLock {
                flushForget()
                if (!force && System.currentTimeMillis() - lastConfigAt < 60_000) return@withLock cachedConfig
                val body = request("/config?code=${BuildConfig.VERSION_CODE}")
                cachedConfig = json.decodeFromString<OperationsConfig>(body)
                lastConfigAt = System.currentTimeMillis()
                cachedConfig
            }

        suspend fun setAnalyticsEnabled(enabled: Boolean) {
            mutex.withLock {
                store.edit {
                    it[consentKey] = enabled
                    if (!enabled) {
                        it[idKey]?.let { old -> it[forgetKey] = (it[forgetKey] ?: emptySet()) + old }
                        it.remove(idKey)
                        it.remove(firstReportedKey)
                    }
                }
                lastActivityDay = null
                healthSent.clear()
                flushForget()
            }
            if (enabled) reportActivity()
        }

        private suspend fun flushForget() {
            val pending = store.data.first()[forgetKey] ?: return
            for (id in pending.take(10)) {
                try {
                    request("/forget", json.encodeToString(mapOf("installId" to id)))
                    store.edit { it[forgetKey] = (it[forgetKey] ?: emptySet()) - id }
                } catch (_: IOException) {
                    // Keep failed deletions for the next foreground refresh.
                    return
                }
            }
        }

        suspend fun reportActivity(category: String = "launch") {
            if (!BuildConfig.UPDATER_ENABLED || !analyticsEnabled.first()) return
            mutex.withLock {
                if (!cachedConfig.analyticsEnabled || !analyticsEnabled.first()) return@withLock
                val day =
                    java.time.LocalDate
                        .now(java.time.ZoneOffset.UTC)
                        .toString()
                if (category == "launch" && lastActivityDay == day) return@withLock
                if (category != "launch" &&
                    (category !in setOf("playback_error", "cast_error", "update_error") || category in healthSent)
                ) {
                    return@withLock
                }
                val id =
                    store.data.first()[idKey] ?: UUID.randomUUID().toString().also { newId ->
                        store.edit { it[idKey] = newId }
                    }
                val eventCategory = if (category == "launch" && store.data.first()[firstReportedKey] != true) "first_launch" else category
                request(
                    "/events",
                    json.encodeToString(ActivityEvent(id, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, eventCategory)),
                )
                if (eventCategory == "first_launch") store.edit { it[firstReportedKey] = true }
                if (category == "launch") lastActivityDay = day else healthSent.add(category)
            }
        }

        internal suspend fun dismiss(announcement: AppAnnouncement) {
            store.edit { it[dismissedKey] = ((it[dismissedKey] ?: emptySet()) + announcement.dismissalKey).toList().takeLast(200).toSet() }
        }

        suspend fun sendReport(
            category: String,
            message: String,
        ) {
            require(message.trim().length in 10..2000)
            require(category in setOf("playback", "casting", "downloads", "updates", "other"))
            request(
                "/reports",
                json.encodeToString(ProblemReport(category, message.trim(), BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)),
            )
        }

        private suspend fun request(
            path: String,
            body: String? = null,
        ): String =
            withContext(Dispatchers.IO) {
                val request = Request.Builder().url(API + path).header("Accept", "application/json")
                if (body != null) request.post(body.toRequestBody("application/json".toMediaType()))
                http.newCall(request.build()).execute().use {
                    if (!it.isSuccessful) throw IOException("Operations request failed: ${it.code}")
                    if (it.body.contentLength() > 65536) throw IOException("Response too large")
                    val source = it.body.source()
                    if (source.request(65_537)) throw IOException("Response too large")
                    source.readUtf8()
                }
            }
    }
