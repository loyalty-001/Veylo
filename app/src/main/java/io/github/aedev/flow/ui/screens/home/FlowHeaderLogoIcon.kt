package io.github.aedev.flow.ui.screens.home

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import io.github.aedev.flow.R

/** Long-press on the Home logo or on the wordmark beside it switches Deep Flow. */
@OptIn(ExperimentalFoundationApi::class)
internal fun Modifier.deepFlowLongPress(
    label: String,
    onLongPress: () -> Unit,
): Modifier = combinedClickable(onLongClickLabel = label, onClick = {}, onLongClick = onLongPress)

@Composable
internal fun FlowHeaderLogoIcon(
    isDeepFlowActive: Boolean,
    modifier: Modifier = Modifier,
) {
    Crossfade(
        targetState = isDeepFlowActive,
        modifier = modifier,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "veylo_header_logo",
    ) { privateMode ->
        Icon(
            imageVector = if (privateMode) Icons.Outlined.VisibilityOff else ImageVector.vectorResource(R.drawable.ic_flow_logo),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}
