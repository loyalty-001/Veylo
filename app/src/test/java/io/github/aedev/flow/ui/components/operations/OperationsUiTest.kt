package io.github.aedev.flow.ui.components.operations

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.aedev.flow.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h891dp")
class OperationsUiTest {
    @get:Rule val rule = createComposeRule()
    private val context = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun noticeOffersWorkingContinueAndTurnOffActions() {
        var continued = 0
        var disabled = 0
        rule.setContent { MaterialTheme { AnalyticsNotice(onContinue = { continued++ }, onDisable = { disabled++ }) } }
        rule.onNodeWithText(context.getString(R.string.operations_continue)).performClick()
        rule.onNodeWithText(context.getString(R.string.operations_turn_off)).performClick()
        assertEquals(1, continued)
        assertEquals(1, disabled)
    }

    @Test
    fun guideCanScrollToCastingAndClose() {
        var closed = false
        rule.setContent { MaterialTheme { GettingStartedDialog(onClose = { closed = true }) } }
        rule.onNodeWithText(context.getString(R.string.veylo_guide_casting)).performScrollTo().assertExists()
        rule.onNodeWithText(context.getString(R.string.operations_close)).performClick()
        assertEquals(true, closed)
    }
}
