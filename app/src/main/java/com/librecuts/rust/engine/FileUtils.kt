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
        val localFilePath: String
        var durationMs = 15_000L

        if (scheme == "file") {
            localFilePath = uri.path ?: ""
        } else {
            // Android 14+ Content URI: copy to cache for direct NDK zero-overhead I/O
            val importsDir = File(context.cacheDir, "imported_media").apply { mkdirs() }
            val tempFile = File(importsDir, "clip_${System.currentTimeMillis()}.mp4")

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

        Pair(localFilePath, durationMs)
    }
}

