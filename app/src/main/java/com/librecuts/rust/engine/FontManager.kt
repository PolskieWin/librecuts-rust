package com.librecuts.rust.engine

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class FontOption(
    val name: String,
    val path: String?,
    val isCustom: Boolean = false
)

class FontManager(private val context: Context) {
    private val fontsDir: File
        get() = File(context.filesDir, "fonts").apply { mkdirs() }

    fun getAvailableFonts(): List<FontOption> {
        val builtIn = listOf(
            FontOption("System Default", null),
            FontOption("Sans-Serif", "sans-serif"),
            FontOption("Serif", "serif"),
            FontOption("Monospace", "monospace")
        )

        val customFiles = fontsDir.listFiles { _, name ->
            name.endsWith(".ttf", ignoreCase = true) || name.endsWith(".otf", ignoreCase = true)
        } ?: emptyArray()

        val customFonts = customFiles.map { file ->
            val cleanName = file.nameWithoutExtension.replace('_', ' ').replace('-', ' ')
            FontOption(cleanName, file.absolutePath, isCustom = true)
        }

        return builtIn + customFonts
    }

    suspend fun importFontFromUri(uri: Uri): FontOption? = withContext(Dispatchers.IO) {
        try {
            var fileName = "font_${System.currentTimeMillis()}.ttf"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    fileName = cursor.getString(nameIndex)
                }
            }

            val destFile = File(fontsDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            if (destFile.exists() && destFile.length() > 0) {
                val cleanName = destFile.nameWithoutExtension.replace('_', ' ').replace('-', ' ')
                FontOption(cleanName, destFile.absolutePath, isCustom = true)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getFontFamily(fontPath: String?): FontFamily {
        if (fontPath.isNullOrEmpty()) return FontFamily.Default
        return try {
            val file = File(fontPath)
            if (file.exists() && file.isFile) {
                FontFamily(Font(file))
            } else {
                when (fontPath.lowercase()) {
                    "sans-serif" -> FontFamily.SansSerif
                    "serif" -> FontFamily.Serif
                    "monospace" -> FontFamily.Monospace
                    else -> FontFamily.Default
                }
            }
        } catch (e: Exception) {
            FontFamily.Default
        }
    }
}
