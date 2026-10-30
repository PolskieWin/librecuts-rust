package com.librecuts.rust.engine

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipInputStream

data class FontOption(
    val name: String,
    val path: String?,
    val isCustom: Boolean = false
)

class FontManager(private val context: Context) {
    private val fontsDir: File
        get() = File(context.filesDir, "fonts").apply { mkdirs() }

    private val fontFamilyCache = ConcurrentHashMap<String, FontFamily>()

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
        }.sortedBy { it.name }

        return builtIn + customFonts
    }

    /**
     * Imports fonts from URI. Handles single TTF/OTF files and .ZIP font archives (e.g. DaFont, Google Fonts).
     * Automatically extracts all .ttf and .otf font files contained within the ZIP.
     */
    suspend fun importFontFromUri(uri: Uri): FontOption? = withContext(Dispatchers.IO) {
        try {
            var fileName = "font_${System.currentTimeMillis()}"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    fileName = cursor.getString(nameIndex)
                }
            }

            val isZip = fileName.endsWith(".zip", ignoreCase = true) ||
                    context.contentResolver.getType(uri)?.contains("zip", ignoreCase = true) == true

            if (isZip) {
                // Extract ZIP Archive
                var firstImported: FontOption? = null
                context.contentResolver.openInputStream(uri)?.use { rawInput ->
                    ZipInputStream(rawInput).use { zipInput ->
                        var entry = zipInput.nextEntry
                        while (entry != null) {
                            val entryName = entry.name
                            val isFontFile = !entry.isDirectory && (
                                    entryName.endsWith(".ttf", ignoreCase = true) ||
                                            entryName.endsWith(".otf", ignoreCase = true)
                                    )

                            if (isFontFile) {
                                // Prevent zip slip vulnerability
                                val safeName = File(entryName).name
                                val destFile = File(fontsDir, safeName)
                                destFile.outputStream().use { out ->
                                    zipInput.copyTo(out)
                                }
                                if (destFile.exists() && destFile.length() > 0) {
                                    val cleanName = destFile.nameWithoutExtension.replace('_', ' ').replace('-', ' ')
                                    val opt = FontOption(cleanName, destFile.absolutePath, isCustom = true)
                                    if (firstImported == null) {
                                        firstImported = opt
                                    }
                                }
                            }
                            zipInput.closeEntry()
                            entry = zipInput.nextEntry
                        }
                    }
                }
                firstImported
            } else {
                // Single TTF/OTF File
                val ext = if (fileName.endsWith(".otf", ignoreCase = true)) ".otf" else ".ttf"
                val destFile = if (fileName.endsWith(".ttf", ignoreCase = true) || fileName.endsWith(".otf", ignoreCase = true)) {
                    File(fontsDir, fileName)
                } else {
                    File(fontsDir, "$fileName$ext")
                }

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
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Resolves a custom or system font family with memory caching.
     */
    fun getFontFamily(fontPath: String?): FontFamily {
        if (fontPath.isNullOrEmpty()) return FontFamily.Default

        return fontFamilyCache.getOrPut(fontPath) {
            try {
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
}
