package com.librecuts.rust.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class AppThemeMode {
    MATERIAL_YOU_AUTO, // Adapts to system Light/Dark with Material You dynamic color
    LIGHT,
    DARK,
    PURE_BLACK // True OLED 0 IRE Black (#000000) with Material You accents
}

val LocalThemeMode = compositionLocalOf { AppThemeMode.PURE_BLACK }

private val FallbackLightColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),
    secondary = Color(0xFF625B71),
    tertiary = Color(0xFF7D5260),
    background = Color(0xFFFEF7FF),
    surface = Color(0xFFFEF7FF),
    surfaceVariant = Color(0xFFE7E0EC),
    onPrimary = Color.White,
    onBackground = Color(0xFF1D1B20),
    onSurface = Color(0xFF1D1B20)
)

private val FallbackDarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    secondary = Color(0xFFCCC2DC),
    tertiary = Color(0xFFEFB8C8),
    background = Color(0xFF141218),
    surface = Color(0xFF141218),
    surfaceVariant = Color(0xFF49454F),
    onPrimary = Color(0xFF381E72),
    onBackground = Color(0xFFE6E0E9),
    onSurface = Color(0xFFE6E0E9)
)

@Composable
fun LibreCutRustTheme(
    themeMode: AppThemeMode = AppThemeMode.PURE_BLACK,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val supportsDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = when (themeMode) {
        AppThemeMode.MATERIAL_YOU_AUTO -> {
            if (supportsDynamic) dynamicDarkColorScheme(context) else FallbackDarkColorScheme
        }
        AppThemeMode.LIGHT -> {
            if (supportsDynamic) dynamicLightColorScheme(context) else FallbackLightColorScheme
        }
        AppThemeMode.DARK -> {
            if (supportsDynamic) dynamicDarkColorScheme(context) else FallbackDarkColorScheme
        }
        AppThemeMode.PURE_BLACK -> {
            // Material You accents over pure OLED black background
            val base = if (supportsDynamic) dynamicDarkColorScheme(context) else FallbackDarkColorScheme
            base.copy(
                background = Color(0xFF000000),
                surface = Color(0xFF070707),
                surfaceVariant = Color(0xFF141414),
                onBackground = Color(0xFFFFFFFF),
                onSurface = Color(0xFFFFFFFF)
            )
        }
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
