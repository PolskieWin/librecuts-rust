package com.librecuts.rust.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.librecuts.rust.engine.AnimationType
import com.librecuts.rust.engine.Clip
import com.librecuts.rust.engine.EffectConfig
import com.librecuts.rust.engine.ThumbnailCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(UnstableApi::class)
@Composable
fun VideoPreviewPlayer(
    videoUri: String?,
    activeClip: Clip? = null,
    isPlaying: Boolean,
    currentPositionMs: Long,
    onPositionChanged: (Long) -> Unit,
    onPlaybackEnded: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val isImageMedia = remember(videoUri) {
        val path = videoUri ?: ""
        path.endsWith(".jpg", ignoreCase = true) ||
                path.endsWith(".jpeg", ignoreCase = true) ||
                path.endsWith(".png", ignoreCase = true) ||
                path.endsWith(".webp", ignoreCase = true)
    }

    val bgRemoval = activeClip?.effects?.filterIsInstance<EffectConfig.BackgroundRemoval>()?.firstOrNull()
    var imageBitmap by remember(videoUri, bgRemoval) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(videoUri, bgRemoval) {
        if (isImageMedia && !videoUri.isNullOrEmpty()) {
            val rawBitmap = ThumbnailCache.getThumbnail(videoUri, 1280, 720)
            if (rawBitmap != null && bgRemoval != null) {
                withContext(Dispatchers.Default) {
                    imageBitmap = removeBackgroundFromBitmap(rawBitmap, bgRemoval.threshold, bgRemoval.invert)
                }
            } else {
                imageBitmap = rawBitmap
            }
        } else {
            imageBitmap = null
        }
    }

    // ExoPlayer for Video Clips
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    val loudnessEnhancer = remember(exoPlayer) {
        try {
            LoudnessEnhancer(exoPlayer.audioSessionId).apply { enabled = true }
        } catch (e: Exception) {
            null
        }
    }

    DisposableEffect(loudnessEnhancer) {
        onDispose {
            try {
                loudnessEnhancer?.release()
            } catch (e: Exception) {}
        }
    }

    // Dynamic Volume & Audio Boost Control (0% to 200%)
    LaunchedEffect(activeClip?.volume, activeClip?.muted, exoPlayer) {
        val vol = if (activeClip?.muted == true) 0f else (activeClip?.volume ?: 1.0f)
        exoPlayer.volume = vol.coerceIn(0f, 1.0f)
        if (vol > 1.0f) {
            val gainMb = ((vol - 1.0f) * 1000).toInt().coerceIn(0, 1500)
            try {
                loudnessEnhancer?.setTargetGain(gainMb)
                loudnessEnhancer?.enabled = true
            } catch (e: Exception) {}
        } else {
            try {
                loudnessEnhancer?.setTargetGain(0)
            } catch (e: Exception) {}
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    onPlaybackEnded()
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(videoUri, isImageMedia) {
        if (!isImageMedia && !videoUri.isNullOrEmpty()) {
            val mediaItem = MediaItem.fromUri(Uri.parse(videoUri))
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
        }
    }

    // Playback loop
    LaunchedEffect(isPlaying, isImageMedia) {
        if (isPlaying) {
            if (!isImageMedia) exoPlayer.play()
            while (isActive) {
                if (!isImageMedia) {
                    onPositionChanged(exoPlayer.currentPosition)
                } else {
                    onPositionChanged(currentPositionMs + 40L)
                }
                delay(40)
            }
        } else {
            if (!isImageMedia) exoPlayer.pause()
        }
    }

    // Scrubbing synchronization
    LaunchedEffect(currentPositionMs, isImageMedia) {
        if (!isImageMedia && Math.abs(exoPlayer.currentPosition - currentPositionMs) > 120) {
            exoPlayer.seekTo(currentPositionMs)
        }
    }

    // Dynamic Color Grading & Filter Matrix for Real-Time Clip Adjustments
    val colorMatrix = remember(activeClip?.effects) {
        val grading = activeClip?.effects?.filterIsInstance<EffectConfig.ColorGrading>()?.firstOrNull()
        val filter = activeClip?.effects?.filterIsInstance<EffectConfig.Filter>()?.firstOrNull()
        val matrix = ColorMatrix()

        // Saturation
        val sat = grading?.saturation ?: 1.0f
        matrix.setToSaturation(sat)

        // Brightness & Contrast
        val b = (grading?.brightness ?: 0f) * 255f
        val c = grading?.contrast ?: 1.0f
        val translate = 128f * (1f - c) + b
        val contrastMatrix = ColorMatrix(floatArrayOf(
            c, 0f, 0f, 0f, translate,
            0f, c, 0f, 0f, translate,
            0f, 0f, c, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))
        matrix.timesAssign(contrastMatrix)

        // Creative Filters
        filter?.let {
            when (it.filter_type.lowercase()) {
                "grayscale", "noir" -> {
                    val grayMatrix = ColorMatrix().apply { setToSaturation(0f) }
                    matrix.timesAssign(grayMatrix)
                }
                "sepia" -> {
                    val sepiaMatrix = ColorMatrix(floatArrayOf(
                        0.393f, 0.769f, 0.189f, 0f, 0f,
                        0.349f, 0.686f, 0.168f, 0f, 0f,
                        0.272f, 0.534f, 0.131f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    ))
                    matrix.timesAssign(sepiaMatrix)
                }
                "cinematic" -> {
                    val cineMatrix = ColorMatrix(floatArrayOf(
                        1.15f, 0f, 0f, 0f, 10f,
                        0f, 1.0f, 0f, 0f, 0f,
                        0f, 0f, 1.25f, 0f, -10f,
                        0f, 0f, 0f, 1f, 0f
                    ))
                    matrix.timesAssign(cineMatrix)
                }
                "cyberpunk" -> {
                    val cyberMatrix = ColorMatrix(floatArrayOf(
                        1.25f, 0f, 0f, 0f, 25f,
                        0f, 0.85f, 0f, 0f, 0f,
                        0f, 0f, 1.35f, 0f, 30f,
                        0f, 0f, 0f, 1f, 0f
                    ))
                    matrix.timesAssign(cyberMatrix)
                }
                "vintage" -> {
                    val vintageMatrix = ColorMatrix(floatArrayOf(
                        1.1f, 0f, 0f, 0f, 15f,
                        0f, 1.05f, 0f, 0f, 10f,
                        0f, 0f, 0.85f, 0f, -5f,
                        0f, 0f, 0f, 1f, 0f
                    ))
                    matrix.timesAssign(vintageMatrix)
                }
                "invert" -> {
                    val invertMatrix = ColorMatrix(floatArrayOf(
                        -1f, 0f, 0f, 0f, 255f,
                        0f, -1f, 0f, 0f, 255f,
                        0f, 0f, -1f, 0f, 255f,
                        0f, 0f, 0f, 1f, 0f
                    ))
                    matrix.timesAssign(invertMatrix)
                }
                else -> {}
            }
        }
        matrix
    }

    // Transform, Crop & Animation offsets
    val transform = activeClip?.effects?.filterIsInstance<EffectConfig.Transform>()?.firstOrNull()
    val shapeMask = activeClip?.effects?.filterIsInstance<EffectConfig.ShapeMask>()?.firstOrNull()

    val scaleX = if (transform?.flip_h == true) -1f else 1f
    val scaleY = if (transform?.flip_v == true) -1f else 1f

    val clipShape = remember(shapeMask?.shape) {
        when (shapeMask?.shape?.lowercase()) {
            "circle" -> CircleShape
            "rectangle" -> RoundedCornerShape(16.dp)
            else -> null
        }
    }

    // Dynamic In/Out/Combo Animations
    val animScale by animateFloatAsState(
        targetValue = when (activeClip?.animation) {
            AnimationType.ZoomIn -> 1.05f
            AnimationType.BounceIn -> 1.03f
            AnimationType.Pulse -> 1.04f
            else -> 1.0f
        },
        animationSpec = tween(durationMillis = 400),
        label = "animScale"
    )

    // Interactive pan (moving) and pinch-to-zoom (resizing) for photos and video
    var panX by remember(activeClip?.id) { mutableFloatStateOf(0f) }
    var panY by remember(activeClip?.id) { mutableFloatStateOf(0f) }
    var zoomScale by remember(activeClip?.id) { mutableFloatStateOf(1f) }
    var rotationAngle by remember(activeClip?.id) { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .background(Color.Black)
            .pointerInput(activeClip?.id) {
                detectTransformGestures { _, pan, zoom, rotation ->
                    panX += pan.x
                    panY += pan.y
                    zoomScale = (zoomScale * zoom).coerceIn(0.2f, 5.0f)
                    rotationAngle += rotation
                }
            }
            .graphicsLayer {
                this.translationX = panX
                this.translationY = panY
                this.scaleX = scaleX * animScale * zoomScale
                this.scaleY = scaleY * animScale * zoomScale
                this.rotationZ = rotationAngle + (if (activeClip?.animation == AnimationType.SpinIn) 360f * animScale else 0f)
            }
            .then(if (clipShape != null) Modifier.clip(clipShape) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (isImageMedia && imageBitmap != null) {
            Image(
                bitmap = imageBitmap!!.asImageBitmap(),
                contentDescription = null,
                colorFilter = ColorFilter.colorMatrix(colorMatrix),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val grading = activeClip?.effects?.filterIsInstance<EffectConfig.ColorGrading>()?.firstOrNull()
            val filter = activeClip?.effects?.filterIsInstance<EffectConfig.Filter>()?.firstOrNull()

            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Color grading & creative filter visual adjustment overlay layer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        drawContent()
                        // Creative Color Filter Blend Overlay
                        filter?.let { f ->
                            when (f.filter_type.lowercase()) {
                                "sepia" -> drawRect(Color(0xFF704214), blendMode = BlendMode.Color, alpha = 0.55f * f.intensity)
                                "cinematic" -> drawRect(Color(0xFF005B94), blendMode = BlendMode.Overlay, alpha = 0.45f * f.intensity)
                                "cyberpunk" -> drawRect(Color(0xFFFF007F), blendMode = BlendMode.Screen, alpha = 0.35f * f.intensity)
                                "vintage" -> drawRect(Color(0xFFD4A373), blendMode = BlendMode.Multiply, alpha = 0.40f * f.intensity)
                                "noir", "grayscale" -> drawRect(Color.Black, blendMode = BlendMode.Saturation, alpha = 1.0f * f.intensity)
                                "invert" -> drawRect(Color.White, blendMode = BlendMode.Difference, alpha = 1.0f * f.intensity)
                                else -> {}
                            }
                        }
                        // Brightness & Contrast Adjustment Overlay
                        grading?.let { g ->
                            if (g.brightness > 0f) {
                                drawRect(Color.White, blendMode = BlendMode.Plus, alpha = (g.brightness * 0.8f).coerceIn(0f, 0.8f))
                            } else if (g.brightness < 0f) {
                                drawRect(Color.Black, blendMode = BlendMode.Multiply, alpha = (-g.brightness * 0.8f).coerceIn(0f, 0.8f))
                            }
                        }
                    }
            )
        }
    }
}

/**
 * 100% Offline On-Device Image Background Removal / Cutout
 * Samples corner pixels to detect dominant background, calculates Euclidean color distance,
 * and sets background pixels to transparent alpha based on threshold and invert settings.
 */
private fun removeBackgroundFromBitmap(source: Bitmap, threshold: Float = 0.45f, invert: Boolean = false): Bitmap {
    val width = source.width
    val height = source.height
    val result = source.copy(Bitmap.Config.ARGB_8888, true)
    val pixels = IntArray(width * height)
    result.getPixels(pixels, 0, width, 0, 0, width, height)

    // Sample corner pixels to detect dominant background color
    val corners = listOf(
        pixels[0],
        pixels[width - 1],
        pixels[(height - 1) * width],
        pixels[width * height - 1]
    )
    var bgR = 0; var bgG = 0; var bgB = 0
    corners.forEach { c ->
        bgR += (c shr 16) and 0xFF
        bgG += (c shr 8) and 0xFF
        bgB += c and 0xFF
    }
    bgR /= 4; bgG /= 4; bgB /= 4

    val maxDist = 441.67f // sqrt(255^2 + 255^2 + 255^2)

    for (i in pixels.indices) {
        val p = pixels[i]
        val a = (p shr 24) and 0xFF
        if (a == 0) continue
        val r = (p shr 16) and 0xFF
        val g = (p shr 8) and 0xFF
        val b = p and 0xFF

        val dist = Math.sqrt(((r - bgR) * (r - bgR) + (g - bgG) * (g - bgG) + (b - bgB) * (b - bgB)).toDouble()).toFloat() / maxDist
        val isBg = dist < threshold
        if (if (invert) !isBg else isBg) {
            pixels[i] = 0 // Transparent alpha
        }
    }
    result.setPixels(pixels, 0, width, 0, 0, width, height)
    return result
}
