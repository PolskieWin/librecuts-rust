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
    PURE_BLACK // True OLED 0 IRE Black (#000000) with Electric Blue accents
}

val LocalThemeMode = compositionLocalOf { AppThemeMode.PURE_BLACK }

// Crisp Electric Blue Light Color Scheme
private val FallbackLightColorScheme = lightColorScheme(
    primary = Color(0xFF1565C0),
    secondary = Color(0xFF0091EA),
    tertiary = Color(0xFF00B0FF),
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE2E8F0),
    onPrimary = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A)
)

// Electric Blue Dark Color Scheme
private val FallbackDarkColorScheme = darkColorScheme(
    primary = Color(0xFF2979FF),
    secondary = Color(0xFF00B0FF),
    tertiary = Color(0xFF82B1FF),
    background = Color(0xFF0A0E17),
    surface = Color(0xFF111827),
    surfaceVariant = Color(0xFF1F2937),
    onPrimary = Color.White,
    onBackground = Color(0xFFF3F4F6),
    onSurface = Color(0xFFF3F4F6)
)

fun colorFromHue(hue: Float, saturation: Float = 0.90f, value: Float = 1.0f): Color {
    val hsv = floatArrayOf(hue.coerceIn(0f, 360f), saturation.coerceIn(0f, 1f), value.coerceIn(0f, 1f))
    return Color(android.graphics.Color.HSVToColor(hsv))
}

@Composable
fun LibreCutRustTheme(
    themeMode: AppThemeMode = AppThemeMode.PURE_BLACK,
    customAccentHue: Float? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val supportsDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val baseScheme = when (themeMode) {
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
            // Material You accents over pure OLED 0 IRE black background
            val base = if (supportsDynamic) dynamicDarkColorScheme(context) else FallbackDarkColorScheme
            base.copy(
                primary = Color(0xFF2979FF),
                secondary = Color(0xFF00B0FF),
                background = Color(0xFF000000),
                surface = Color(0xFF080B10),
                surfaceVariant = Color(0xFF121824),
                onBackground = Color(0xFFFFFFFF),
                onSurface = Color(0xFFFFFFFF)
            )
        }
    }

    val colorScheme = if (customAccentHue != null) {
        val primaryCol = colorFromHue(customAccentHue, 0.95f, 1.0f)
        val primaryContainerCol = colorFromHue(customAccentHue, 0.85f, 0.38f)
        val secondaryCol = colorFromHue((customAccentHue + 25f) % 360f, 0.90f, 0.95f)
        val secondaryContainerCol = colorFromHue((customAccentHue + 25f) % 360f, 0.80f, 0.32f)
        val tertiaryCol = colorFromHue((customAccentHue + 50f) % 360f, 0.90f, 0.95f)
        val tertiaryContainerCol = colorFromHue((customAccentHue + 50f) % 360f, 0.80f, 0.32f)

        baseScheme.copy(
            primary = primaryCol,
            onPrimary = Color.White,
            primaryContainer = primaryContainerCol,
            onPrimaryContainer = Color.White,
            secondary = secondaryCol,
            onSecondary = Color.White,
            secondaryContainer = secondaryContainerCol,
            onSecondaryContainer = Color.White,
            tertiary = tertiaryCol,
            onTertiary = Color.White,
            tertiaryContainer = tertiaryContainerCol,
            onTertiaryContainer = Color.White,
            inversePrimary = colorFromHue(customAccentHue, 0.55f, 0.88f)
        )
    } else {
        baseScheme
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
