package io.github.aedev.flow.ui.theme

import androidx.compose.ui.graphics.Color

val VeyloPalette =
    PalettePair(
        id = "veylo",
        light =
            PaletteSeed(
                primary = Color(0xFF007D6B),
                onPrimary = Color(0xFFFFFFFF),
                secondary = Color(0xFF49645E),
                background = Color(0xFFFFFFFF),
                surface = Color(0xFFF2F6F4),
                onSurface = Color(0xFF101413),
                onSurfaceVariant = Color(0xFF4F5B57),
                outline = Color(0xFFCDD8D3),
                error = Color(0xFFBA1A1A),
            ),
        dark =
            PaletteSeed(
                primary = Color(0xFF00D5B5),
                onPrimary = Color(0xFF002019),
                secondary = Color(0xFF9ACFC1),
                background = Color(0xFF000000),
                surface = Color(0xFF101513),
                onSurface = Color(0xFFF2F7F4),
                onSurfaceVariant = Color(0xFFB4C6BE),
                outline = Color(0xFF34433C),
                error = Color(0xFFFFB4AB),
            ),
    )
