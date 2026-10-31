package com.librecuts.rust.engine

import android.content.Context
import android.net.Uri
import androidx.compose.ui.text.font.FontFamily
import java.util.Locale

data class FontOption(
    val name: String,
    val path: String?,
    val isCustom: Boolean = false
)

/**
 * Built-in System Font Manager for LibreCut RS.
 * Provides instant, zero-overhead, crash-free system fonts (Default, Sans-Serif, Serif, Monospace).
 */
class FontManager(private val context: Context) {

    fun getAvailableFonts(): List<FontOption> {
        return listOf(
            FontOption("System Default", null),
            FontOption("Sans-Serif", "sans-serif"),
            FontOption("Serif", "serif"),
            FontOption("Monospace", "monospace")
        )
    }

    suspend fun importFontFromUri(uri: Uri): FontOption? {
        return null
    }

    fun getFontFamily(fontPath: String?): FontFamily {
        if (fontPath.isNullOrEmpty()) return FontFamily.Default
        return when (fontPath.lowercase(Locale.ROOT)) {
            "sans-serif" -> FontFamily.SansSerif
            "serif" -> FontFamily.Serif
            "monospace" -> FontFamily.Monospace
            else -> FontFamily.Default
        }
    }
}
