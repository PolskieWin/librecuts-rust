package com.librecuts.rust.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class AppThemeMode {
    LIGHT,
    DARK,
    PURE_BLACK // OLED True Black Mode (#000000)
}

val LocalThemeMode = compositionLocalOf { AppThemeMode.PURE_BLACK }

private val LightColorScheme = lightColorScheme(
    primary = LightAccentAmber,
    secondary = AccentOrange,
    tertiary = AccentGreen,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceElevated,
    onPrimary = LightSurface,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary
)

private val DarkColorScheme = darkColorScheme(
    primary = AccentAmber,
    secondary = AccentOrange,
    tertiary = AccentGreen,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceElevated,
    onPrimary = DarkBackground,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary
)

private val PureBlackColorScheme = darkColorScheme(
    primary = AccentAmber,
    secondary = AccentOrange,
    tertiary = AccentGreen,
    background = OledBackground,
    surface = OledSurface,
    surfaceVariant = OledSurfaceElevated,
    onPrimary = OledBackground,
    onBackground = OledTextPrimary,
    onSurface = OledTextPrimary
)

@Composable
fun LibreCutRustTheme(
    themeMode: AppThemeMode = AppThemeMode.PURE_BLACK,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        AppThemeMode.LIGHT -> LightColorScheme
        AppThemeMode.DARK -> DarkColorScheme
        AppThemeMode.PURE_BLACK -> PureBlackColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            val insets = WindowCompat.getInsetsController(window, view)
            insets.isAppearanceLightStatusBars = (themeMode == AppThemeMode.LIGHT)
            insets.isAppearanceLightNavigationBars = (themeMode == AppThemeMode.LIGHT)
        }
    }

    CompositionLocalProvider(LocalThemeMode provides themeMode) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
