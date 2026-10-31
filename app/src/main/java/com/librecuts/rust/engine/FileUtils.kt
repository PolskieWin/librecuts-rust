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
import java.util.UUID

object FileUtils {
    private const val TAG = "FileUtils"

    suspend fun resolveMediaUriToLocalFile(context: Context, uri: Uri): Pair<String, Long> = withContext(Dispatchers.IO) {
        val scheme = uri.scheme

        // 1. Direct file:// URI or raw path
        if (scheme == "file") {
            val path = uri.path ?: ""
            val isImg = isImagePath(path)
            val durationMs = if (isImg) 3000L else probeDuration(context, uri, path)
            return@withContext Pair(path, durationMs)
        }

        // 2. Persistable URI permission (surrounded by try-catch for third-party file managers)
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (ignored: Throwable) {}

        // 3. Inspect MIME type & Display name safely
        val mimeType = try { context.contentResolver.getType(uri)?.lowercase() ?: "" } catch (t: Throwable) { "" }
        var displayName = ""
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        displayName = cursor.getString(idx) ?: ""
                    }
                }
            }
        } catch (t: Throwable) {}

        val nameExt = if (displayName.contains('.')) displayName.substringAfterLast('.').lowercase() else ""
        val isImage = mimeType.startsWith("image/") ||
                nameExt in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic") ||
                uri.toString().contains("image", ignoreCase = true)

        // 4. For Images: ALWAYS safely copy to internal cache.
        // Images are lightweight (<3MB) and copying guarantees 100% offline POSIX file access for Bitmap decoders and NativeEngine.
        // It also prevents MediaMetadataRetriever crashes on static images.
        if (isImage) {
            val resolvedExt = when {
                nameExt.isNotBlank() -> nameExt
                mimeType.contains("png") -> "png"
                mimeType.contains("webp") -> "webp"
                mimeType.contains("gif") -> "gif"
                else -> "jpg"
            }
            try {
                val importsDir = File(context.cacheDir, "imported_media").apply { mkdirs() }
                val tempFile = File(importsDir, "img_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.$resolvedExt")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                if (tempFile.exists() && tempFile.length() > 0L) {
                    return@withContext Pair(tempFile.absolutePath, 3000L)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to copy image URI to cache: ${t.message}", t)
            }
            // Fallback: return URI string with 3000ms duration
            return@withContext Pair(uri.toString(), 3000L)
        }

        // 5. Check if MediaStore DATA column yields a real, readable POSIX file
        // Crucial: check f.canRead() to avoid EACCES permission denials under scoped storage!
        try {
            val projection = arrayOf(MediaStore.MediaColumns.DATA)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val colIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                    if (colIdx >= 0) {
                        val directPath = cursor.getString(colIdx)
                        if (!directPath.isNullOrBlank()) {
                            val f = File(directPath)
                            if (f.exists() && f.canRead() && f.length() > 0L) {
                                val durationMs = probeDuration(context, uri, directPath)
                                return@withContext Pair(directPath, durationMs)
                            }
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "MediaStore DATA resolution fallback: ${t.message}")
        }

        // 6. For Videos / Audio without direct POSIX file access:
        // ExoPlayer & MediaExtractor natively stream content:// URIs with zero copy overhead.
        val mediaUriString = uri.toString()
        val durationMs = probeDuration(context, uri, mediaUriString)
        Pair(mediaUriString, durationMs)
    }

    suspend fun ensureLocalCacheFile(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        if (uri.scheme == "file") {
            return@withContext uri.path
        }
        val pathStr = uri.toString()
        if (!pathStr.startsWith("content://")) {
            val f = File(pathStr)
            if (f.exists() && f.canRead()) return@withContext f.absolutePath
        }
        try {
            val exportTempDir = File(context.cacheDir, "export_temp").apply { mkdirs() }
            val tempFile = File(exportTempDir, "export_input_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.mp4")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (tempFile.exists() && tempFile.length() > 0L) {
                return@withContext tempFile.absolutePath
            }
        } catch (t: Throwable) {
            Log.w(TAG, "ensureLocalCacheFile failed: ${t.message}")
        }
        null
    }

    private fun isImagePath(path: String): Boolean {
        val lower = path.lowercase()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") ||
                lower.endsWith(".webp") || lower.endsWith(".gif") || lower.endsWith(".bmp") || lower.endsWith(".heic")
    }

    private fun probeDuration(context: Context, uri: Uri, path: String): Long {
        var durationMs = 10_000L
        val retriever = MediaMetadataRetriever()
        try {
            if (path.startsWith("content://") || uri.scheme == "content") {
                retriever.setDataSource(context, uri)
            } else {
                retriever.setDataSource(path)
            }
            val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durStr?.toLongOrNull()?.let {
                if (it > 0L) durationMs = it
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Could not extract duration with retriever: ${t.message}")
        } finally {
            try { retriever.release() } catch (ignored: Throwable) {}
        }
        return durationMs
    }
}
