package io.github.aedev.flow.ui.components.shorts

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
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
class ShortsActionRailTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun shareKeepsAnAccessibleTargetAndCallsTheExistingAction() {
        var shares = 0
        val context = ApplicationProvider.getApplicationContext<Application>()
        rule.setContent {
            MaterialTheme {
                ShortsActionRail(
                    isLiked = false,
                    likeLabel = "10",
                    onLikeClick = {},
                    commentLabel = "2",
                    onCommentsClick = {},
                    isSaved = false,
                    saveLabel = "Save",
                    onSaveClick = {},
                    shareLabel = "Share",
                    onShareClick = { shares++ },
                    moreLabel = "More",
                    onMoreClick = {},
                    channelAvatarUrl = "",
                    channelName = "Artist",
                    isDiscSpinning = false,
                )
            }
        }
        val share = rule.onNodeWithContentDescription(context.getString(R.string.action_share))
        val like = rule.onNodeWithContentDescription(context.getString(R.string.action_like))
        assertEquals(48.dp, share.getUnclippedBoundsInRoot().height)
        assertEquals(like.getUnclippedBoundsInRoot().width, share.getUnclippedBoundsInRoot().width)
        share.performClick()
        assertEquals(1, shares)
    }
}
