package io.github.aedev.flow.data.operations

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import java.util.Collections

class AnalyticsPreferencesTest {
    @get:Rule val folder = TemporaryFolder()

    @Test
    fun noticeBlocksDefaultCollectionAndExplicitOptOutSurvivesUpgrade() =
        runBlocking {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val store = PreferenceDataStoreFactory.create(scope = scope) { folder.root.resolve("operations.preferences_pb") }
            val requests = Collections.synchronizedList(mutableListOf<String>())
            var failDeletion = true
            var failNextEvent = false
            val client =
                OkHttpClient
                    .Builder()
                    .addInterceptor { chain ->
                        val request = chain.request()
                        requests.add(request.url.encodedPath)
                        val failEvent = request.url.encodedPath.endsWith("events") && failNextEvent
                        if (failEvent) failNextEvent = false
                        val config = """{"analyticsEnabled":true,"reportsEnabled":true}"""
                        Response
                            .Builder()
                            .request(request)
                            .protocol(Protocol.HTTP_1_1)
                            .code(if ((request.url.encodedPath.endsWith("forget") && failDeletion) || failEvent) 503 else 200)
                            .message("test")
                            .body((if (request.url.encodedPath.endsWith("config")) config else "{}").toResponseBody())
                            .build()
                    }.build()
            try {
                val repository = OperationsRepository(store, client, reportingSupported = true)
                assertTrue(repository.analyticsEnabled.first())
                assertTrue(repository.analyticsNoticeNeeded.first())
                repository.configuration()
                repository.reportActivity()
                assertFalse(requests.any { it.endsWith("events") })
                repository.setAnalyticsEnabled(true)
                assertEquals(1, requests.count { it.endsWith("events") })
                assertEquals(AnalyticsDelivery.SENT, repository.analyticsDelivery.value)
                assertFalse(repository.analyticsNoticeNeeded.first())
                repository.reportActivity()
                assertEquals(1, requests.count { it.endsWith("events") })
                val idKey =
                    androidx.datastore.preferences.core
                        .stringPreferencesKey("installation_id")
                val originalId = store.data.first()[idKey]
                failNextEvent = true
                assertTrue(runCatching { repository.reportActivity("playback_error") }.exceptionOrNull() is IOException)
                assertEquals(AnalyticsDelivery.FAILED, repository.analyticsDelivery.value)
                repository.retryActivity()
                assertEquals(AnalyticsDelivery.SENT, repository.analyticsDelivery.value)
                assertEquals(originalId, store.data.first()[idKey])
                assertEquals(3, requests.count { it.endsWith("events") })
                repository.setAnalyticsEnabled(false)
                assertEquals(AnalyticsDelivery.DELETION_PENDING, repository.analyticsDelivery.value)
                val upgraded = OperationsRepository(store, client, reportingSupported = true)
                assertFalse(upgraded.analyticsEnabled.first())
                assertFalse(upgraded.analyticsNoticeNeeded.first())
                failDeletion = false
                upgraded.configuration(force = true)
                upgraded.reportActivity()
                assertEquals(AnalyticsDelivery.OFF, upgraded.analyticsDelivery.value)
                assertEquals(3, requests.count { it.endsWith("events") })
                assertTrue(
                    store.data.first()[
                        androidx.datastore.preferences.core
                            .stringSetPreferencesKey("pending_forget_ids"),
                    ].isNullOrEmpty(),
                )
                store.edit { it[booleanPreferencesKey("analytics_enabled")] = true }
                assertTrue(OperationsRepository(store, client, reportingSupported = true).analyticsEnabled.first())
                val unsupported = OperationsRepository(store, client, reportingSupported = false)
                unsupported.configuration(force = true)
                unsupported.reportActivity()
                assertEquals(3, requests.count { it.endsWith("events") })
            } finally {
                scope.cancel()
            }
        }
}
