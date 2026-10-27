package io.github.aedev.flow.ui.screens.settings

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.aedev.flow.R
import io.github.aedev.flow.ui.screens.settings.about.AboutHeader
import io.github.aedev.flow.ui.screens.settings.about.OriginDialog
import io.github.aedev.flow.ui.screens.settings.index.AboutIndex
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h891dp")
class AboutBrandingTest {
    @get:Rule
    val rule = createComposeRule()
    private val context = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun headerShowsVeyloAndKeepsOriginCreditInLegal() {
        rule.setContent { MaterialTheme { AboutHeader() } }
        rule.onNodeWithText(context.getString(R.string.app_name)).assertExists()
        rule.onNodeWithText(context.getString(R.string.veylo_origin_notice)).assertDoesNotExist()
        assertEquals(R.string.section_legal, AboutIndex.creator.section)
        assertEquals("Source, licenses, and contributor credits", context.getString(R.string.veylo_original_project_summary))
    }

    @Test
    fun originDialogRetainsAttributionAndOriginalSourceLink() {
        rule.setContent { MaterialTheme { OriginDialog(onDismiss = {}) } }
        rule.onNodeWithText(context.getString(R.string.veylo_origin_notice)).assertExists()
        rule.onNodeWithText(context.getString(R.string.veylo_original_source)).assertExists()
    }
}
