package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CedarOrangePrimary,
    onPrimary = Color.Black,
    primaryContainer = CedarOrangeDark,
    onPrimaryContainer = Color.White,
    secondary = SunsetCoral,
    onSecondary = Color.Black,
    secondaryContainer = Color(0x33FF6B35),
    onSecondaryContainer = SunsetCoral,
    tertiary = AmberGlow,
    onTertiary = Color.Black,
    background = ObsidianBackground,
    onBackground = TextPrimaryDark,
    surface = LuxurySurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = LuxurySurfaceElevated,
    onSurfaceVariant = TextSecondaryDark,
    outline = LuxuryCardBorder,
    error = StatusRed,
    onError = Color.White
)

@Composable
fun CedarWattTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
