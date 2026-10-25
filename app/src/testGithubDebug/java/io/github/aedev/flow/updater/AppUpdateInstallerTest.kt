package io.github.aedev.flow.updater

import android.content.Context
import com.google.common.truth.Truth.assertThat
import io.github.aedev.flow.BuildConfig
import io.github.aedev.flow.data.update.AppRelease
import io.github.aedev.flow.data.update.ReleaseApk
import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AppUpdateInstallerTest {
    private val context = mockk<Context>()
    private val installer = AppUpdateInstaller(context)

    @Test
    fun `debug downloads cannot enqueue official release installation`() {
        assertThat(BuildConfig.DEBUG).isTrue()
        val release =
            AppRelease(
                version = "3.0.1",
                tag = "v3.0.1",
                notes = "",
                publishedAt = null,
                pageUrl = "https://github.com/nosakhare-ezomo/Veylo/releases/tag/v3.0.1",
                apk = ReleaseApk("Veylo.apk", "https://example.com/Veylo.apk", 1, null),
            )

        installer.download(release)

        assertThat(installer.isAvailable).isFalse()
        assertThat(installer.canInstall()).isFalse()
        verify { context wasNot Called }
    }

    @Test
    fun `debug install attempts never reach the package installer`() =
        runTest {
            assertThat(installer.install("3.0.1")).isFalse()
            verify { context wasNot Called }
        }
}
