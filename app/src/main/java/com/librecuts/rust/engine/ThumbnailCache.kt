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
    private val cacheSize = (maxMemory / 8).coerceAtLeast(1024)

    private val memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return (bitmap.byteCount / 1024).coerceAtLeast(1)
        }
    }

    /**
     * Retrieves or asynchronously extracts a thumbnail bitmap for any media file (video or image).
     */
    suspend fun getThumbnail(filePath: String, targetWidth: Int = 320, targetHeight: Int = 180, context: Context? = null): Bitmap? = withContext(Dispatchers.IO) {
        if (filePath.isBlank()) return@withContext null

        val cacheKey = "$filePath@${targetWidth}x$targetHeight"
        memoryCache.get(cacheKey)?.let { return@withContext it }

        val isContentUri = filePath.startsWith("content://") || filePath.startsWith("file://")
        val file = if (!isContentUri) {
            try { File(filePath) } catch (t: Throwable) { null }
        } else null
        if (file != null) {
            val exists = try { file.exists() } catch (t: Throwable) { false }
            if (!exists) return@withContext null
        }

        val isImage = filePath.endsWith(".jpg", ignoreCase = true) ||
                filePath.endsWith(".jpeg", ignoreCase = true) ||
                filePath.endsWith(".png", ignoreCase = true) ||
                filePath.endsWith(".webp", ignoreCase = true) ||
                (isContentUri && context != null && try { context.contentResolver.getType(android.net.Uri.parse(filePath))?.startsWith("image/") == true } catch (e: Exception) { false })

        val bitmap = try {
            if (isImage) {
                decodeSampledBitmap(filePath, targetWidth, targetHeight, context)
            } else {
                extractVideoFrame(filePath, targetWidth, targetHeight, context)
            }
        } catch (t: Throwable) {
            null
        }

        bitmap?.let {
            memoryCache.put(cacheKey, it)
        }

        bitmap
    }

    private fun scalePreservingAspectRatio(bitmap: Bitmap, maxDim: Int = 360): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 0 || h <= 0) return bitmap
        val aspect = w.toFloat() / h.toFloat()
        val (targetW, targetH) = if (w >= h) {
            maxDim to (maxDim / aspect).toInt().coerceAtLeast(1)
        } else {
            (maxDim * aspect).toInt().coerceAtLeast(1) to maxDim
        }
        return if (targetW == w && targetH == h) bitmap else Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }

    private fun extractVideoFrame(path: String, reqWidth: Int, reqHeight: Int, context: Context? = null): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            if (path.startsWith("content://") && context != null) {
                retriever.setDataSource(context, android.net.Uri.parse(path))
            } else {
                retriever.setDataSource(path)
            }
            // Retrieve frame at 500ms or fallback to first available frame
            val raw = retriever.getFrameAtTime(500_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime
            raw?.let {
                scalePreservingAspectRatio(it, maxOf(reqWidth, reqHeight))
            }
        } catch (t: Throwable) {
            null
        } finally {
            try { retriever.release() } catch (ignored: Throwable) {}
        }
    }

    private fun decodeSampledBitmap(path: String, reqWidth: Int, reqHeight: Int, context: Context? = null): Bitmap? {
        return try {
            val isContent = path.startsWith("content://") && context != null
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            if (isContent) {
                context!!.contentResolver.openInputStream(android.net.Uri.parse(path))?.use {
                    BitmapFactory.decodeStream(it, null, options)
                }
            } else {
                BitmapFactory.decodeFile(path, options)
            }

            var inSampleSize = 1
            val maxReq = maxOf(reqWidth, reqHeight)
            if (options.outHeight > maxReq || options.outWidth > maxReq) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while ((halfHeight / inSampleSize) >= maxReq && (halfWidth / inSampleSize) >= maxReq) {
                    inSampleSize *= 2
                }
            }

            options.inJustDecodeBounds = false
            options.inSampleSize = inSampleSize
            val decoded = if (isContent) {
                context!!.contentResolver.openInputStream(android.net.Uri.parse(path))?.use {
                    BitmapFactory.decodeStream(it, null, options)
                }
            } else {
                BitmapFactory.decodeFile(path, options)
            }
            decoded?.let {
                scalePreservingAspectRatio(it, maxReq)
            }
        } catch (t: Throwable) {
            null
        }
    }

    /**
     * 100% Offline On-Device Image Background Removal / Cutout
     */
    fun removeBackgroundFromBitmap(source: Bitmap, threshold: Float = 0.45f, invert: Boolean = false): Bitmap {
        val width = source.width
        val height = source.height
        val result = source.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(width * height)
        result.getPixels(pixels, 0, width, 0, 0, width, height)

        val c1 = pixels[0]
        val c2 = pixels[width - 1]
        val c3 = pixels[(height - 1) * width]
        val c4 = pixels[width * height - 1]

        val refR = ((c1 shr 16 and 0xFF) + (c2 shr 16 and 0xFF) + (c3 shr 16 and 0xFF) + (c4 shr 16 and 0xFF)) / 4
        val refG = ((c1 shr 8 and 0xFF) + (c2 shr 8 and 0xFF) + (c3 shr 8 and 0xFF) + (c4 shr 8 and 0xFF)) / 4
        val refB = ((c1 and 0xFF) + (c2 and 0xFF) + (c3 and 0xFF) + (c4 and 0xFF)) / 4

        val threshSq = (threshold * 441.67f) * (threshold * 441.67f)

        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF

            val distSq = (r - refR) * (r - refR) + (g - refG) * (g - refG) + (b - refB) * (b - refB)
            val isBg = if (invert) distSq > threshSq else distSq <= threshSq

            if (isBg) {
                pixels[i] = 0x00000000
            }
        }

        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }
}
