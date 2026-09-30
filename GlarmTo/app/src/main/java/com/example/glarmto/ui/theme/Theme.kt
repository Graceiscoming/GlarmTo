package com.example.glarmto.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ClassicColorScheme = darkColorScheme(
    primary = BloodRed,
    secondary = DarkRed,
    tertiary = LightRed,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = Color(0xFF2C2C2C),
    onPrimary = LightText,
    onSecondary = LightText,
    onTertiary = DarkBackground,
    onBackground = LightText,
    onSurface = LightText,
    onSurfaceVariant = SecondaryText
)

private val OceanColorScheme = darkColorScheme(
    primary = OceanPrimary,
    secondary = OceanSecondary,
    tertiary = OceanPrimary,
    background = OceanBackground,
    surface = OceanSurface,
    surfaceVariant = Color(0xFF033A56),
    onPrimary = LightText,
    onSecondary = DarkBackground,
    onBackground = LightText,
    onSurface = LightText,
    onSurfaceVariant = Color(0xFFB0D0E6)
)

private val NeonColorScheme = darkColorScheme(
    primary = NeonPrimary,
    secondary = NeonSecondary,
    tertiary = NeonPrimary,
    background = NeonBackground,
    surface = NeonSurface,
    surfaceVariant = Color(0xFF242436),
    onPrimary = DarkBackground,
    onSecondary = LightText,
    onBackground = LightText,
    onSurface = LightText,
    onSurfaceVariant = Color(0xFFC0C0D4)
)

private val ForestColorScheme = darkColorScheme(
    primary = ForestPrimary,
    secondary = ForestSecondary,
    tertiary = ForestPrimary,
    background = ForestBackground,
    surface = ForestSurface,
    surfaceVariant = Color(0xFF384A41),
    onPrimary = LightText,
    onSecondary = DarkBackground,
    onBackground = LightText,
    onSurface = LightText,
    onSurfaceVariant = Color(0xFFB0C4BC)
)

private val AuraColorScheme = darkColorScheme(
    primary = AuraPrimary,
    secondary = AuraSecondary,
    tertiary = AuraPrimary,
    background = AuraBackground,
    surface = AuraSurface,
    surfaceVariant = Color(0x331E152A),
    onPrimary = LightText,
    onSecondary = DarkBackground,
    onBackground = LightText,
    onSurface = LightText,
    onSurfaceVariant = Color(0xFFD8C4FF)
)

@Composable
fun GlarmToTheme(
    themeName: String = com.example.glarmto.data.preferences.ThemeManager.THEME_CLASSIC,
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeName) {
        com.example.glarmto.data.preferences.ThemeManager.THEME_OCEAN -> OceanColorScheme
        com.example.glarmto.data.preferences.ThemeManager.THEME_NEON -> NeonColorScheme
        com.example.glarmto.data.preferences.ThemeManager.THEME_FOREST -> ForestColorScheme
        com.example.glarmto.data.preferences.ThemeManager.THEME_AURA -> AuraColorScheme
        else -> ClassicColorScheme // Default
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}