package com.librecuts.rust.engine

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object FileUtils {
    private const val TAG = "FileUtils"

    suspend fun resolveMediaUriToLocalFile(context: Context, uri: Uri): Pair<String, Long> = withContext(Dispatchers.IO) {
        val scheme = uri.scheme

        // 1. Direct file:// URI
        if (scheme == "file") {
            val path = uri.path ?: ""
            val durationMs = probeDuration(context, uri, path)
            return@withContext Pair(path, durationMs)
        }

        // 2. Persistable URI permission (surrounded by try-catch for third-party file managers)
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (ignored: Exception) {}

        // 3. Fast MediaStore DATA query for zero-overhead instant access
        try {
            val projection = arrayOf(MediaStore.MediaColumns.DATA)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val colIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                    if (colIdx >= 0) {
                        val directPath = cursor.getString(colIdx)
                        if (!directPath.isNullOrBlank() && File(directPath).exists()) {
                            val durationMs = probeDuration(context, uri, directPath)
                            return@withContext Pair(directPath, durationMs)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "MediaStore DATA resolution fallback: ${e.message}")
        }

        // 4. Determine MIME type and extension
        val mimeType = try { context.contentResolver.getType(uri) ?: "" } catch (e: Exception) { "" }
        var resolvedExt = when {
            mimeType.contains("png") -> "png"
            mimeType.contains("jpeg") || mimeType.contains("jpg") -> "jpg"
            mimeType.contains("webp") -> "webp"
            mimeType.contains("gif") -> "gif"
            mimeType.contains("audio/mpeg") || mimeType.contains("mp3") -> "mp3"
            mimeType.contains("audio/wav") || mimeType.contains("wav") -> "wav"
            mimeType.contains("audio/m4a") || mimeType.contains("audio/mp4") -> "m4a"
            mimeType.contains("audio/ogg") -> "ogg"
            mimeType.startsWith("image/") -> "png"
            mimeType.startsWith("audio/") -> "mp3"
            else -> ""
        }

        if (resolvedExt.isEmpty()) {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx >= 0 && cursor.moveToFirst()) {
                        val name = cursor.getString(nameIdx)
                        val dot = name.lastIndexOf('.')
                        if (dot != -1) {
                            resolvedExt = name.substring(dot + 1).lowercase()
                        }
                    }
                }
            } catch (ignored: Exception) {}
        }

        if (resolvedExt.isEmpty()) {
            resolvedExt = "mp4"
        }

        val isImage = mimeType.startsWith("image/") || resolvedExt in listOf("jpg", "jpeg", "png", "webp", "gif")

        // 5. For Images: copy small image file (<3MB) to cache so Bitmap decoders have local file fallback
        if (isImage) {
            val importsDir = File(context.cacheDir, "imported_media").apply { mkdirs() }
            val tempFile = File(importsDir, "img_${System.currentTimeMillis()}.$resolvedExt")
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                return@withContext Pair(tempFile.absolutePath, 3000L)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to copy image URI: ${e.message}")
                return@withContext Pair(uri.toString(), 3000L)
            }
        }

        // 6. For Videos / Audio: NEVER block main thread copying multi-gigabyte video files!
        // ExoPlayer & MediaExtractor natively play content:// URIs in 0ms with zero copy!
        val mediaUriString = uri.toString()
        val durationMs = probeDuration(context, uri, mediaUriString)
        Pair(mediaUriString, durationMs)
    }

    private fun probeDuration(context: Context, uri: Uri, path: String): Long {
        var durationMs = 15_000L
        val retriever = MediaMetadataRetriever()
        try {
            if (path.startsWith("content://")) {
                retriever.setDataSource(context, uri)
            } else {
                retriever.setDataSource(path)
            }
            val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durStr?.toLongOrNull()?.let { durationMs = it }
        } catch (e: Exception) {
            Log.w(TAG, "Could not extract duration with retriever: ${e.message}")
        } finally {
            try { retriever.release() } catch (ignored: Exception) {}
        }
        return durationMs
    }
}
