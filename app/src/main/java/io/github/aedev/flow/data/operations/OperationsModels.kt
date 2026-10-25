package io.github.aedev.flow.data.operations

import kotlinx.serialization.Serializable

@Serializable
internal data class OperationsConfig(
    val serverTime: Long = 0,
    val analyticsEnabled: Boolean = false,
    val reportsEnabled: Boolean = false,
    val heldReleases: List<String> = emptyList(),
    val announcements: List<AppAnnouncement> = emptyList(),
)

@Serializable
internal data class AppAnnouncement(
    val id: String,
    val revision: Int,
    val title: String,
    val body: String,
    val kind: String,
    val startsAt: Long,
    val endsAt: Long,
    val minCode: Int,
    val maxCode: Int,
    val actionLabel: String = "",
    val actionUrl: String = "",
    val onceOnly: Boolean = true,
) {
    val dismissalKey: String get() = "$id:$revision"
}

internal fun AppAnnouncement.isEligible(
    code: Int,
    now: Long,
    dismissed: Set<String>,
): Boolean =
    code in minCode..maxCode && now in startsAt until endsAt && dismissalKey !in dismissed &&
        kind in setOf("banner", "popup") && title.length in 1..100 && body.length in 1..1500

internal fun allowedAnnouncementLink(value: String): Boolean =
    runCatching {
        val uri = java.net.URI(value)
        uri.scheme == "https" && uri.userInfo == null &&
            (uri.host == "veylo-lra9.onrender.com" || (uri.host == "github.com" && uri.path.startsWith("/loyalty-001/Veylo/")))
    }.getOrDefault(false)

@Serializable
internal data class ActivityEvent(
    val installId: String,
    val version: String,
    val code: Int,
    val category: String,
    val platform: String = "android",
)

@Serializable
internal data class ProblemReport(
    val category: String,
    val message: String,
    val version: String,
    val code: Int,
    val platform: String = "android",
)
