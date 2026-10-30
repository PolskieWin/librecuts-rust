package com.librecuts.rust.engine

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipInputStream

data class FontOption(
    val name: String,
    val path: String?,
    val isCustom: Boolean = false
)

class FontManager(private val context: Context) {
    companion object {
        private const val TAG = "FontManager"
    }

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
            val cleanName = file.nameWithoutExtension.replace('_', ' ').replace('-', ' ').trim()
            FontOption(cleanName, file.absolutePath, isCustom = true)
        }.sortedBy { it.name }

        return builtIn + customFonts
    }

    /**
     * Imports fonts from URI. Handles single TTF/OTF files and .ZIP font archives (e.g. DaFont, Google Fonts).
     * Extracts all .ttf and .otf font files contained within the ZIP.
     */
    suspend fun importFontFromUri(uri: Uri): FontOption? = withContext(Dispatchers.IO) {
        try {
            var displayName = "font_${System.currentTimeMillis()}"
            try {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        val queried = cursor.getString(nameIndex)
                        if (!queried.isNullOrBlank()) {
                            displayName = queried
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not query display name: ${e.message}")
            }

            // Copy to temporary file for safe stream reading and ZIP magic inspection
            val tempImportFile = File(context.cacheDir, "import_temp_${System.currentTimeMillis()}.bin")
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempImportFile.outputStream().use { output ->
                    input.copyTo(output)
                    output.flush()
                }
            }

            if (!tempImportFile.exists() || tempImportFile.length() < 4) {
                return@withContext null
            }

            // Check if file starts with ZIP magic header (0x50, 0x4B, 0x03, 0x04)
            val isZip = try {
                tempImportFile.inputStream().use { stream ->
                    val header = ByteArray(4)
                    val read = stream.read(header)
                    read >= 2 && header[0] == 0x50.toByte() && header[1] == 0x4B.toByte()
                }
            } catch (e: Exception) {
                false
            } || displayName.endsWith(".zip", ignoreCase = true) ||
                    context.contentResolver.getType(uri)?.contains("zip", ignoreCase = true) == true

            if (isZip) {
                // Extract ZIP Archive
                var firstImported: FontOption? = null
                tempImportFile.inputStream().use { rawInput ->
                    ZipInputStream(rawInput).use { zipInput ->
                        var entry = zipInput.nextEntry
                        while (entry != null) {
                            val entryName = entry.name
                            val lower = entryName.lowercase(Locale.ROOT)
                            val isFontFile = !entry.isDirectory &&
                                    !lower.contains("__macosx") &&
                                    !File(entryName).name.startsWith(".") &&
                                    (lower.endsWith(".ttf") || lower.endsWith(".otf"))

                            if (isFontFile) {
                                val safeName = File(entryName).name.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
                                val destFile = File(fontsDir, safeName)
                                destFile.outputStream().use { out ->
                                    zipInput.copyTo(out)
                                    out.flush()
                                }
                                if (destFile.exists() && destFile.length() > 0) {
                                    val cleanName = destFile.nameWithoutExtension.replace('_', ' ').replace('-', ' ').trim()
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
                tempImportFile.delete()
                firstImported
            } else {
                // Single TTF/OTF File
                val ext = if (displayName.endsWith(".otf", ignoreCase = true)) ".otf" else ".ttf"
                val safeName = if (displayName.endsWith(".ttf", ignoreCase = true) || displayName.endsWith(".otf", ignoreCase = true)) {
                    File(displayName).name.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
                } else {
                    "font_${System.currentTimeMillis()}$ext"
                }

                val destFile = File(fontsDir, safeName)
                tempImportFile.copyTo(destFile, overwrite = true)
                tempImportFile.delete()

                if (destFile.exists() && destFile.length() > 0) {
                    val cleanName = destFile.nameWithoutExtension.replace('_', ' ').replace('-', ' ').trim()
                    FontOption(cleanName, destFile.absolutePath, isCustom = true)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error importing font: ${e.message}", e)
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
                if (file.exists() && file.isFile && file.length() > 0) {
                    val typeface = Typeface.createFromFile(file)
                    FontFamily(typeface)
                } else {
                    when (fontPath.lowercase(Locale.ROOT)) {
                        "sans-serif" -> FontFamily.SansSerif
                        "serif" -> FontFamily.Serif
                        "monospace" -> FontFamily.Monospace
                        else -> FontFamily.Default
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not load font from $fontPath: ${e.message}")
                FontFamily.Default
            }
        }
    }
}
