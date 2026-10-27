package io.github.aedev.flow.ui.screens.settings

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.aedev.flow.R
import io.github.aedev.flow.ui.screens.settings.about.DonationsScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h891dp")
class DonationsScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun unconfiguredDonationsShowNoPaymentOrCopyActions() {
        rule.setContent { MaterialTheme { DonationsScreen(onNavigateBack = {}) } }

        rule.onNodeWithText(context.getString(R.string.veylo_support_title)).assertExists()
        rule.onNodeWithText(context.getString(R.string.veylo_donations_unconfigured)).assertExists()
        rule.onNodeWithText(context.getString(R.string.veylo_support_open_collective)).assertDoesNotExist()
        rule.onNodeWithText(context.getString(R.string.veylo_support_patreon)).assertDoesNotExist()
        rule.onNodeWithText(context.getString(R.string.donations_cryptocurrency)).assertDoesNotExist()
        rule.onNodeWithContentDescription(context.getString(R.string.donation_address_clip_label)).assertDoesNotExist()
    }
}
