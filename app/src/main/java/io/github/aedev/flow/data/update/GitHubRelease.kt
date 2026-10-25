package io.github.aedev.flow.data.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant

private val ReleaseJson = Json { ignoreUnknownKeys = true }
private const val SHA256_PREFIX = "sha256:"
private const val UNIVERSAL_APK = "Veylo.apk"
private val StableSplitApk = Regex("""veylo-(armeabi-v7a|arm64-v8a|x86|x86_64)\.apk""", RegexOption.IGNORE_CASE)
private val NightlyApk = Regex("""veylo-nightly-(\d+)\.apk""", RegexOption.IGNORE_CASE)

@Serializable
internal data class GitHubRelease(
    @SerialName("tag_name") val tagName: String = "",
    val body: String? = null,
    @SerialName("html_url") val htmlUrl: String = "",
    @SerialName("published_at") val publishedAt: String? = null,
    val assets: List<GitHubAsset> = emptyList(),
)

@Serializable
internal data class GitHubAsset(
    val name: String = "",
    @SerialName("browser_download_url") val downloadUrl: String = "",
    val size: Long = 0,
    val digest: String? = null,
)

internal fun parseGitHubRelease(json: String): GitHubRelease = ReleaseJson.decodeFromString(GitHubRelease.serializer(), json)

/** [release] as an [AppRelease] when it is newer than [currentVersion], otherwise null. */
internal fun GitHubRelease.toAppReleaseIfNewer(
    currentVersion: String,
    supportedAbis: List<String>,
): AppRelease? {
    if (tagName.isBlank() || !AppVersions.isNewer(tagName, currentVersion)) return null
    return AppRelease(
        version = AppVersions.normalize(tagName),
        tag = tagName,
        notes = body.orEmpty(),
        publishedAt = publishedAt?.let { runCatching { Instant.parse(it) }.getOrNull() },
        pageUrl = htmlUrl,
        apk = selectApk(assets, supportedAbis)?.toReleaseApk(),
    )
}

/**
 * The rolling nightly release as an [AppRelease] when its build is newer than [currentRun]. Nightly
 * builds are told apart by their CI run number, which the APK's name carries, so a local build
 * (run 0) never offers one.
 */
internal fun GitHubRelease.toNightlyReleaseIfNewer(currentRun: Int): AppRelease? {
    if (currentRun <= 0) return null
    val (apk, run) =
        assets
            .firstNotNullOfOrNull { asset -> NightlyApk.matchEntire(asset.name)?.let { asset to it.groupValues[1].toInt() } }
            ?: return null
    if (run <= currentRun) return null
    return AppRelease(
        version = "nightly.$run",
        tag = tagName,
        notes = body.orEmpty(),
        publishedAt = publishedAt?.let { runCatching { Instant.parse(it) }.getOrNull() },
        pageUrl = htmlUrl,
        apk = apk.toReleaseApk(),
    )
}

/**
 * The github-flavour APK for this device: the split build for its preferred ABI, else the universal
 * `Veylo.apk`. Foss, nightly and unrelated APKs are never picked.
 */
internal fun selectApk(
    assets: List<GitHubAsset>,
    supportedAbis: List<String>,
): GitHubAsset? {
    val github =
        assets.filter {
            it.name.equals(UNIVERSAL_APK, ignoreCase = true) || StableSplitApk.matches(it.name)
        }
    val splits = github.filter { asset -> supportedAbis.any { asset.name.equals("veylo-$it.apk", ignoreCase = true) } }
    if (splits.isNotEmpty()) {
        return supportedAbis.firstNotNullOfOrNull { abi -> splits.firstOrNull { it.name.equals("veylo-$abi.apk", ignoreCase = true) } }
    }
    val publishesSplits = github.any { it.name.startsWith("veylo-", ignoreCase = true) }
    if (publishesSplits && github.none { it.name.equals(UNIVERSAL_APK, ignoreCase = true) }) return null
    return github.firstOrNull { it.name.equals(UNIVERSAL_APK, ignoreCase = true) } ?: github.firstOrNull()
}

private fun GitHubAsset.toReleaseApk(): ReleaseApk =
    ReleaseApk(
        name = name,
        url = downloadUrl,
        sizeBytes = size,
        sha256 = digest?.takeIf { it.startsWith(SHA256_PREFIX) }?.removePrefix(SHA256_PREFIX)?.lowercase(),
    )
