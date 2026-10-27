package io.github.aedev.flow.ui.screens.settings.about

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// Ported from Mihon.
internal val IconPatreon: ImageVector by lazy {
    ImageVector
        .Builder(
            name = "Patreon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(22.957f, 7.21f)
                curveToRelative(-0.004f, -3.064f, -2.391f, -5.576f, -5.191f, -6.482f)
                curveToRelative(-3.478f, -1.125f, -8.064f, -0.962f, -11.384f, 0.604f)
                curveTo(2.357f, 3.231f, 1.093f, 7.391f, 1.046f, 11.54f)
                curveToRelative(-0.039f, 3.411f, 0.302f, 12.396f, 5.369f, 12.46f)
                curveToRelative(3.765f, 0.047f, 4.326f, -4.804f, 6.068f, -7.141f)
                curveToRelative(1.24f, -1.662f, 2.836f, -2.132f, 4.801f, -2.618f)
                curveToRelative(3.376f, -0.836f, 5.678f, -3.501f, 5.673f, -7.031f)
                close()
            }
        }.build()
}

// Simple Icons' Open Collective mark.
internal val IconOpenCollective: ImageVector by lazy {
    ImageVector
        .Builder(
            name = "OpenCollective",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 0f)
                curveTo(5.373f, 0f, 0f, 5.373f, 0f, 12f)
                reflectiveCurveToRelative(5.373f, 12f, 12f, 12f)
                curveToRelative(2.54f, 0f, 4.894f, -0.79f, 6.834f, -2.135f)
                lineToRelative(-3.107f, -3.109f)
                arcToRelative(7.715f, 7.715f, 0f, isMoreThanHalf = true, isPositiveArc = true, 0f, -13.512f)
                lineToRelative(3.107f, -3.109f)
                arcTo(11.943f, 11.943f, 0f, isMoreThanHalf = false, isPositiveArc = false, 12f, 0f)
                close()
                moveToRelative(9.865f, 5.166f)
                lineToRelative(-3.109f, 3.107f)
                arcTo(7.67f, 7.67f, 0f, isMoreThanHalf = false, isPositiveArc = true, 19.715f, 12f)
                arcToRelative(7.682f, 7.682f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.959f, 3.727f)
                lineToRelative(3.109f, 3.107f)
                arcTo(11.943f, 11.943f, 0f, isMoreThanHalf = false, isPositiveArc = false, 24f, 12f)
                curveToRelative(0f, -2.54f, -0.79f, -4.894f, -2.135f, -6.834f)
                close()
            }
        }.build()
}
