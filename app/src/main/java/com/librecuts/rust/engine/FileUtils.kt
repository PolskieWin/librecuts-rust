package com.librecuts.rust.engine

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object FileUtils {
    private const val TAG = "FileUtils"

    suspend fun resolveMediaUriToLocalFile(context: Context, uri: Uri): Pair<String, Long> = withContext(Dispatchers.IO) {
        val scheme = uri.scheme
        var localFilePath: String
        var durationMs = 15_000L

        val mimeType = try { context.contentResolver.getType(uri) ?: "" } catch (e: Exception) { "" }

        // Determine proper extension from MIME or display name
        var resolvedExt = when {
            mimeType.contains("png") -> "png"
            mimeType.contains("jpeg") || mimeType.contains("jpg") -> "jpg"
            mimeType.contains("webp") -> "webp"
            mimeType.contains("gif") -> "gif"
            mimeType.contains("audio/mpeg") || mimeType.contains("mp3") -> "mp3"
            mimeType.contains("audio/wav") || mimeType.contains("wav") -> "wav"
            mimeType.contains("audio/m4a") || mimeType.contains("audio/mp4") -> "m4a"
            mimeType.contains("audio/ogg") -> "ogg"
            mimeType.contains("font") || mimeType.contains("ttf") -> "ttf"
            mimeType.contains("zip") -> "zip"
            mimeType.startsWith("image/") -> "png"
            mimeType.startsWith("audio/") -> "mp3"
            else -> ""
        }

        if (resolvedExt.isEmpty()) {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIdx >= 0 && cursor.moveToFirst()) {
                        val name = cursor.getString(nameIdx)
                        val dot = name.lastIndexOf('.')
                        if (dot != -1) {
                            resolvedExt = name.substring(dot + 1).lowercase()
                        }
                    }
                }
            } catch (e: Exception) {}
        }

        if (resolvedExt.isEmpty()) {
            resolvedExt = "mp4"
        }

        val isImage = mimeType.startsWith("image/") || resolvedExt in listOf("jpg", "jpeg", "png", "webp", "gif")

        if (scheme == "file") {
            localFilePath = uri.path ?: ""
        } else {
            // Android 14+ Content URI: copy to cache for direct NDK zero-overhead I/O
            val importsDir = File(context.cacheDir, "imported_media").apply { mkdirs() }
            val tempFile = File(importsDir, "clip_${System.nanoTime()}.$resolvedExt")

            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                localFilePath = tempFile.absolutePath
            } catch (e: Exception) {
                Log.e(TAG, "Failed to copy content uri to local file: ${e.message}")
                localFilePath = uri.toString()
            }
        }

        if (isImage) {
            durationMs = 3_000L
        } else {
            // Probe real duration using MediaMetadataRetriever
            try {
                MediaMetadataRetriever().use { retriever ->
                    retriever.setDataSource(localFilePath)
                    val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    durStr?.toLongOrNull()?.let { durationMs = it }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not extract duration with retriever: ${e.message}")
            }
        }

        Pair(localFilePath, durationMs)
    }
}

