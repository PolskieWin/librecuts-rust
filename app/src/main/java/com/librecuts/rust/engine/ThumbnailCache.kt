package com.librecuts.rust.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * High-performance Memory & Disk Thumbnail Cache for LibreCut RS.
 * Ensures video scrubbing, project cards, and clip representations render smoothly without UI stutter.
 */
object ThumbnailCache {
    // 32MB Memory Cache for instant frame retrieval
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8 // Use 1/8th of available memory

    private val memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    /**
     * Retrieves or asynchronously extracts a thumbnail bitmap for any media file (video or image).
     */
    suspend fun getThumbnail(filePath: String, targetWidth: Int = 320, targetHeight: Int = 180): Bitmap? = withContext(Dispatchers.IO) {
        if (filePath.isBlank()) return@withContext null

        val cacheKey = "$filePath@${targetWidth}x$targetHeight"
        memoryCache.get(cacheKey)?.let { return@withContext it }

        val file = File(filePath)
        if (!file.exists()) return@withContext null

        val isImage = filePath.endsWith(".jpg", ignoreCase = true) ||
                filePath.endsWith(".jpeg", ignoreCase = true) ||
                filePath.endsWith(".png", ignoreCase = true) ||
                filePath.endsWith(".webp", ignoreCase = true)

        val bitmap = if (isImage) {
            decodeSampledBitmapFromFile(file.absolutePath, targetWidth, targetHeight)
        } else {
            extractVideoFrame(file.absolutePath, targetWidth, targetHeight)
        }

        bitmap?.let {
            memoryCache.put(cacheKey, it)
        }

        bitmap
    }

    private fun extractVideoFrame(path: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            // Retrieve frame at 500ms
            val raw = retriever.getFrameAtTime(500_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            raw?.let {
                Bitmap.createScaledBitmap(it, reqWidth, reqHeight, true)
            }
        } catch (e: Exception) {
            null
        } finally {
            try { retriever.release() } catch (ignored: Exception) {}
        }
    }

    private fun decodeSampledBitmapFromFile(path: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(path, options)

        var inSampleSize = 1
        if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
            val halfHeight = options.outHeight / 2
            val halfWidth = options.outWidth / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        options.inJustDecodeBounds = false
        options.inSampleSize = inSampleSize
        return try {
            val decoded = BitmapFactory.decodeFile(path, options)
            decoded?.let {
                Bitmap.createScaledBitmap(it, reqWidth, reqHeight, true)
            }
        } catch (e: Exception) {
            null
        }
    }
}

