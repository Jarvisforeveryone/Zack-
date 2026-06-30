package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Premium flagship AIRA Accent Color
val AiraAccent = Color(0xFF4A9EFF)
val TextWhite87 = Color(0xDEFFFFFF) // White with 87% opacity

// 1. Deep Black (#0A0A0A)
val DeepBlackColorScheme = darkColorScheme(
    primary = AiraAccent,
    onPrimary = Color(0xFF0A0A0A),
    secondary = AiraAccent,
    onSecondary = Color.White,
    tertiary = AiraAccent,
    background = Color(0xFF0A0A0A),
    onBackground = TextWhite87,
    surface = Color(0xFF121212),
    onSurface = TextWhite87,
    surfaceVariant = Color(0xFF1C1C1C),
    onSurfaceVariant = Color.White
)

// 2. Ink Black (#0D0F14)
val InkBlackColorScheme = darkColorScheme(
    primary = AiraAccent,
    onPrimary = Color(0xFF0D0F14),
    secondary = AiraAccent,
    onSecondary = Color.White,
    tertiary = AiraAccent,
    background = Color(0xFF0D0F14),
    onBackground = TextWhite87,
    surface = Color(0xFF161920),
    onSurface = TextWhite87,
    surfaceVariant = Color(0xFF222630),
    onSurfaceVariant = Color.White
)

// 3. Charcoal (#111318)
val CharcoalColorScheme = darkColorScheme(
    primary = AiraAccent,
    onPrimary = Color(0xFF111318),
    secondary = AiraAccent,
    onSecondary = Color.White,
    tertiary = AiraAccent,
    background = Color(0xFF111318),
    onBackground = TextWhite87,
    surface = Color(0xFF1B1D24),
    onSurface = TextWhite87,
    surfaceVariant = Color(0xFF242730),
    onSurfaceVariant = Color.White
)

// 4. Slate Dark (#15161A)
val SlateDarkColorScheme = darkColorScheme(
    primary = AiraAccent,
    onPrimary = Color(0xFF15161A),
    secondary = AiraAccent,
    onSecondary = Color.White,
    tertiary = AiraAccent,
    background = Color(0xFF15161A),
    onBackground = TextWhite87,
    surface = Color(0xFF202126),
    onSurface = TextWhite87,
    surfaceVariant = Color(0xFF2B2C33),
    onSurfaceVariant = Color.White
)

// 5. Graphite (#1A1B1F)
val GraphiteColorScheme = darkColorScheme(
    primary = AiraAccent,
    onPrimary = Color(0xFF1A1B1F),
    secondary = AiraAccent,
    onSecondary = Color.White,
    tertiary = AiraAccent,
    background = Color(0xFF1A1B1F),
    onBackground = TextWhite87,
    surface = Color(0xFF26272C),
    onSurface = TextWhite87,
    surfaceVariant = Color(0xFF31333B),
    onSurfaceVariant = Color.White
)

// 6. Navy Dark (#0B1020)
val NavyDarkColorScheme = darkColorScheme(
    primary = AiraAccent,
    onPrimary = Color(0xFF0B1020),
    secondary = AiraAccent,
    onSecondary = Color.White,
    tertiary = AiraAccent,
    background = Color(0xFF0B1020),
    onBackground = TextWhite87,
    surface = Color(0xFF141A2D),
    onSurface = TextWhite87,
    surfaceVariant = Color(0xFF1D243B),
    onSurfaceVariant = Color.White
)

// 7. Steel Blue (#0F141D)
val SteelBlueColorScheme = darkColorScheme(
    primary = AiraAccent,
    onPrimary = Color(0xFF0F141D),
    secondary = AiraAccent,
    onSecondary = Color.White,
    tertiary = AiraAccent,
    background = Color(0xFF0F141D),
    onBackground = TextWhite87,
    surface = Color(0xFF1A212E),
    onSurface = TextWhite87,
    surfaceVariant = Color(0xFF242D3E),
    onSurfaceVariant = Color.White
)

// 8. Forest Dark (#0E120F)
val ForestDarkColorScheme = darkColorScheme(
    primary = AiraAccent,
    onPrimary = Color(0xFF0E120F),
    secondary = AiraAccent,
    onSecondary = Color.White,
    tertiary = AiraAccent,
    background = Color(0xFF0E120F),
    onBackground = TextWhite87,
    surface = Color(0xFF181F1A),
    onSurface = TextWhite87,
    surfaceVariant = Color(0xFF222C24),
    onSurfaceVariant = Color.White
)

// 9. Cocoa Dark (#1A1410)
val CocoaDarkColorScheme = darkColorScheme(
    primary = AiraAccent,
    onPrimary = Color(0xFF1A1410),
    secondary = AiraAccent,
    onSecondary = Color.White,
    tertiary = AiraAccent,
    background = Color(0xFF1A1410),
    onBackground = TextWhite87,
    surface = Color(0xFF271F19),
    onSurface = TextWhite87,
    surfaceVariant = Color(0xFF342B22),
    onSurfaceVariant = Color.White
)

// 10. Stone Dark (#222325)
val StoneDarkColorScheme = darkColorScheme(
    primary = AiraAccent,
    onPrimary = Color(0xFF222325),
    secondary = AiraAccent,
    onSecondary = Color.White,
    tertiary = AiraAccent,
    background = Color(0xFF222325),
    onBackground = TextWhite87,
    surface = Color(0xFF2F3033),
    onSurface = TextWhite87,
    surfaceVariant = Color(0xFF3D3F42),
    onSurfaceVariant = Color.White
)

@Composable
fun AiraTheme(
    themeIndex: Int = 0,
    customColorHex: String = "#00FFFF",
    content: @Composable () -> Unit
) {
    val airaColorScheme = when (themeIndex) {
        0 -> DeepBlackColorScheme
        1 -> InkBlackColorScheme
        2 -> CharcoalColorScheme
        3 -> SlateDarkColorScheme
        4 -> GraphiteColorScheme
        5 -> NavyDarkColorScheme
        6 -> SteelBlueColorScheme
        7 -> ForestDarkColorScheme
        8 -> CocoaDarkColorScheme
        9 -> StoneDarkColorScheme
        else -> DeepBlackColorScheme
    }

    MaterialTheme(
        colorScheme = airaColorScheme,
        typography = Typography,
        content = content
    )
}
