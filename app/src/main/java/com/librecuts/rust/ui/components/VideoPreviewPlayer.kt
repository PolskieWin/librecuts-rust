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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.text.font.FontWeight
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
import com.librecuts.rust.engine.Keyframe
import com.librecuts.rust.engine.ThumbnailCache
import com.librecuts.rust.engine.TransitionType
import com.librecuts.rust.ui.theme.LocalAppStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.sin

@OptIn(UnstableApi::class)
@Composable
fun VideoPreviewPlayer(
    videoUri: String?,
    activeClip: Clip? = null,
    audioClips: List<Clip> = emptyList(),
    isPlaying: Boolean,
    currentPositionMs: Long,
    onPositionChanged: (Long) -> Unit,
    onPlaybackEnded: () -> Unit = {},
    onKeyframeUpdated: ((clipId: String, keyframe: Keyframe) -> Unit)? = null,
    onAddMediaClicked: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current

    val isImageMedia = remember(videoUri) {
        val path = videoUri ?: ""
        if (path.startsWith("content://")) {
            try {
                val mime = context.contentResolver.getType(Uri.parse(path)) ?: ""
                mime.startsWith("image/")
            } catch (e: Exception) {
                false
            }
        } else {
            path.endsWith(".jpg", ignoreCase = true) ||
                    path.endsWith(".jpeg", ignoreCase = true) ||
                    path.endsWith(".png", ignoreCase = true) ||
                    path.endsWith(".webp", ignoreCase = true)
        }
    }

    val bgRemoval = activeClip?.effects?.filterIsInstance<EffectConfig.BackgroundRemoval>()?.firstOrNull()
    var imageBitmap by remember(videoUri, bgRemoval) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(videoUri, bgRemoval) {
        if (isImageMedia && !videoUri.isNullOrEmpty()) {
            val rawBitmap = ThumbnailCache.getThumbnail(videoUri, 1280, 720, context = context)
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

    // Secondary Audio Player for synchronized Sound Effects and Music Tracks
    val audioPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    // Helper to safely convert file paths (with or without scheme) into valid ExoPlayer URIs
    fun safeUri(path: String?): Uri {
        if (path.isNullOrEmpty()) return Uri.EMPTY
        return try {
            if (path.startsWith("content://") || path.startsWith("file://")) {
                Uri.parse(path)
            } else {
                Uri.fromFile(File(path))
            }
        } catch (t: Throwable) {
            Uri.EMPTY
        }
    }

    var audioSessionId by remember { mutableIntStateOf(0) }
    var loudnessEnhancer by remember { mutableStateOf<LoudnessEnhancer?>(null) }

    DisposableEffect(exoPlayer, audioPlayer) {
        val playerListener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    onPlaybackEnded()
                }
            }
        }
        val analyticsListener = object : androidx.media3.exoplayer.analytics.AnalyticsListener {
            override fun onAudioSessionIdChanged(
                eventTime: androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime,
                sessionId: Int
            ) {
                audioSessionId = sessionId
                if (sessionId != 0 && sessionId != androidx.media3.common.C.AUDIO_SESSION_ID_UNSET) {
                    try {
                        loudnessEnhancer?.release()
                        loudnessEnhancer = LoudnessEnhancer(sessionId).apply { enabled = true }
                    } catch (e: Exception) {
                        loudnessEnhancer = null
                    }
                }
            }
        }
        exoPlayer.addListener(playerListener)
        exoPlayer.addAnalyticsListener(analyticsListener)

        val initSessionId = try { exoPlayer.audioSessionId } catch (e: Exception) { 0 }
        if (initSessionId != 0 && initSessionId != androidx.media3.common.C.AUDIO_SESSION_ID_UNSET) {
            audioSessionId = initSessionId
            try {
                loudnessEnhancer = LoudnessEnhancer(initSessionId).apply { enabled = true }
            } catch (e: Exception) {}
        }

        onDispose {
            exoPlayer.removeListener(playerListener)
            exoPlayer.removeAnalyticsListener(analyticsListener)
            try { loudnessEnhancer?.release() } catch (e: Exception) {}
            exoPlayer.release()
            audioPlayer.release()
        }
    }

    // Dynamic Volume & Audio Boost Control (0% to 200%)
    LaunchedEffect(activeClip?.volume, activeClip?.muted, loudnessEnhancer, audioSessionId) {
        val vol = if (activeClip?.muted == true) 0f else (activeClip?.volume ?: 1.0f)
        exoPlayer.volume = vol.coerceIn(0f, 1.0f)
        if (vol > 1.0f) {
            val gainMb = ((vol - 1.0f) * 1200).toInt().coerceIn(0, 2000)
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

    val clipStartMs = activeClip?.timeline_start_ms ?: 0L
    val clipSourceStartMs = activeClip?.source_start_ms ?: 0L

    LaunchedEffect(videoUri, isImageMedia) {
        if (!isImageMedia && !videoUri.isNullOrEmpty()) {
            try {
                val uri = safeUri(videoUri)
                if (uri != Uri.EMPTY) {
                    val currentUri = try { exoPlayer.currentMediaItem?.localConfiguration?.uri } catch (e: Exception) { null }
                    if (currentUri != uri) {
                        val mediaItem = MediaItem.fromUri(uri)
                        exoPlayer.setMediaItem(mediaItem)
                        exoPlayer.prepare()
                        val targetMediaPos = (currentPositionMs - clipStartMs + clipSourceStartMs).coerceAtLeast(0L)
                        exoPlayer.seekTo(targetMediaPos)
                        if (isPlaying) {
                            exoPlayer.play()
                        }
                    }
                }
            } catch (t: Throwable) {
                android.util.Log.w("VideoPreviewPlayer", "ExoPlayer failed to load videoUri $videoUri: ${t.message}")
            }
        } else if (isImageMedia) {
            try {
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
            } catch (t: Throwable) {}
        }
    }

    // Synchronize Audio Track / Sound Effects Playback
    val activeAudioClip = remember(currentPositionMs, audioClips) {
        audioClips.firstOrNull { clip ->
            currentPositionMs in clip.timeline_start_ms..(clip.timeline_start_ms + clip.durationMs)
        }
    }

    var currentAudioUri by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(activeAudioClip?.file_path) {
        val path = activeAudioClip?.file_path
        if (!path.isNullOrEmpty()) {
            if (path != currentAudioUri) {
                currentAudioUri = path
                try {
                    audioPlayer.setMediaItem(MediaItem.fromUri(safeUri(path)))
                    audioPlayer.prepare()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            currentAudioUri = null
            audioPlayer.pause()
        }
    }

    LaunchedEffect(isPlaying, activeAudioClip?.id, activeAudioClip?.volume, activeAudioClip?.muted) {
        if (isPlaying && activeAudioClip != null) {
            val offset = (currentPositionMs - activeAudioClip.timeline_start_ms + activeAudioClip.source_start_ms).coerceAtLeast(0L)
            audioPlayer.seekTo(offset)
            val vol = if (activeAudioClip.muted) 0f else (activeAudioClip.volume * 1.5f).coerceIn(0f, 1.0f)
            audioPlayer.volume = vol
            audioPlayer.play()
        } else {
            audioPlayer.pause()
        }
    }

    // Video Playback loop with Smooth Image Progress
    LaunchedEffect(isPlaying, isImageMedia, activeClip?.id) {
        if (isPlaying) {
            if (!isImageMedia && activeClip != null) {
                val startSeek = (currentPositionMs - clipStartMs + clipSourceStartMs).coerceAtLeast(0L)
                exoPlayer.seekTo(startSeek)
                exoPlayer.play()
            }
            var simulatedPos = currentPositionMs
            while (isActive) {
                if (!isImageMedia && activeClip != null) {
                    val newTimelinePos = (exoPlayer.currentPosition - clipSourceStartMs + clipStartMs).coerceAtLeast(0L)
                    onPositionChanged(newTimelinePos)
                } else {
                    simulatedPos += 33L
                    onPositionChanged(simulatedPos)
                }
                delay(33L)
            }
        } else {
            if (!isImageMedia) exoPlayer.pause()
        }
    }

    // Scrubbing synchronization - ONLY seek when paused to avoid starving playback thread
    LaunchedEffect(currentPositionMs, isImageMedia, isPlaying, activeClip?.id) {
        if (!isPlaying && !isImageMedia && activeClip != null) {
            val targetMediaPos = (currentPositionMs - clipStartMs + clipSourceStartMs).coerceAtLeast(0L)
            if (Math.abs(exoPlayer.currentPosition - targetMediaPos) > 60) {
                exoPlayer.seekTo(targetMediaPos)
            }
        }
    }

    // Dynamic Speed & Speed Ramp Control (Clip Adjustments)
    val clipElapsedMs = (currentPositionMs - (activeClip?.timeline_start_ms ?: 0L)).coerceAtLeast(0L)
    val hasActiveSpeedCurve = activeClip?.speedCurve != null && activeClip.speedCurve != com.librecuts.rust.engine.SpeedCurveType.NORMAL
    LaunchedEffect(
        activeClip?.speed,
        activeClip?.speedCurve,
        if (hasActiveSpeedCurve) currentPositionMs else Unit
    ) {
        if (!isImageMedia) {
            val baseSpeed = (activeClip?.speed ?: 1.0f).coerceIn(0.25f, 4.0f)
            val effectiveSpeed = if (hasActiveSpeedCurve && (activeClip?.durationMs ?: 0L) > 0) {
                val clipProgress = (clipElapsedMs.toFloat() / (activeClip?.durationMs ?: 1L)).coerceIn(0f, 1f)
                when (activeClip?.speedCurve) {
                    com.librecuts.rust.engine.SpeedCurveType.MONTAGE -> {
                        if (clipProgress in 0.35f..0.65f) 0.5f else 2.0f
                    }
                    com.librecuts.rust.engine.SpeedCurveType.HERO_MOMENT -> {
                        if (clipProgress in 0.35f..0.7f) 0.35f else 1.0f
                    }
                    com.librecuts.rust.engine.SpeedCurveType.BULLET_TIME -> {
                        if (clipProgress in 0.25f..0.75f) 0.25f else 1.0f
                    }
                    com.librecuts.rust.engine.SpeedCurveType.FLASH_IN -> {
                        if (clipProgress < 0.3f) 3.0f else 1.0f
                    }
                    else -> baseSpeed
                }
            } else {
                baseSpeed
            }

            try {
                if (Math.abs(exoPlayer.playbackParameters.speed - effectiveSpeed) > 0.05f) {
                    exoPlayer.setPlaybackSpeed(effectiveSpeed)
                }
            } catch (e: Exception) {}
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

    // Transform, Crop & Mask offsets
    val transform = activeClip?.effects?.filterIsInstance<EffectConfig.Transform>()?.firstOrNull()
    val shapeMask = activeClip?.effects?.filterIsInstance<EffectConfig.ShapeMask>()?.firstOrNull()

    val baseScaleX = if (transform?.flip_h == true) -1f else 1f
    val baseScaleY = if (transform?.flip_v == true) -1f else 1f

    val clipShape = remember(shapeMask?.shape) {
        when (shapeMask?.shape?.lowercase()) {
            "circle" -> CircleShape
            "rectangle" -> RoundedCornerShape(16.dp)
            else -> null
        }
    }

    // Real-Time Live In/Out/Combo Animations & Transitions based on currentPositionMs
    val clipRemainingMs = (((activeClip?.timeline_start_ms ?: 0L) + (activeClip?.durationMs ?: 1L)) - currentPositionMs).coerceAtLeast(0L)
    val animDuration = 600L

    val animProgressIn = (clipElapsedMs.toFloat() / animDuration).coerceIn(0f, 1f)
    val animProgressOut = (clipRemainingMs.toFloat() / animDuration).coerceIn(0f, 1f)

    var liveAnimScale = 1.0f
    var liveAnimAlpha = 1.0f
    var liveAnimRot = 0.0f
    var liveAnimSlideX = 0.0f

    when (activeClip?.animation) {
        AnimationType.FadeIn -> {
            liveAnimAlpha = animProgressIn
        }
        AnimationType.FadeOut -> {
            liveAnimAlpha = animProgressOut
        }
        AnimationType.ZoomIn -> {
            liveAnimScale = 0.35f + 0.65f * animProgressIn
        }
        AnimationType.ZoomOut -> {
            liveAnimScale = 0.35f + 0.65f * animProgressOut
        }
        AnimationType.BounceIn -> {
            val p = animProgressIn
            liveAnimScale = if (p < 0.65f) 0.3f + 0.9f * (p / 0.65f) else 1.2f - 0.2f * ((p - 0.65f) / 0.35f)
        }
        AnimationType.SpinIn -> {
            liveAnimRot = (1f - animProgressIn) * 360f
            liveAnimScale = 0.3f + 0.7f * animProgressIn
        }
        AnimationType.SlideInLeft -> {
            liveAnimSlideX = (1f - animProgressIn) * -600f
        }
        AnimationType.SlideInRight -> {
            liveAnimSlideX = (1f - animProgressIn) * 600f
        }
        AnimationType.SlideOutLeft -> {
            liveAnimSlideX = (1f - animProgressOut) * -600f
        }
        AnimationType.SlideOutRight -> {
            liveAnimSlideX = (1f - animProgressOut) * 600f
        }
        AnimationType.SpinOut -> {
            liveAnimRot = (1f - animProgressOut) * -360f
            liveAnimScale = 0.3f + 0.7f * animProgressOut
        }
        AnimationType.Pulse -> {
            liveAnimScale = 1.0f + 0.04f * sin(clipElapsedMs / 120.0).toFloat()
        }
        AnimationType.Shake -> {
            liveAnimSlideX = sin(clipElapsedMs / 35.0).toFloat() * 25f
        }
        AnimationType.Glitch -> {
            liveAnimSlideX = if ((clipElapsedMs / 60) % 2 == 0L) 18f else -18f
            liveAnimAlpha = if ((clipElapsedMs / 70) % 3 == 0L) 0.6f else 1.0f
        }
        AnimationType.Pendulum -> {
            liveAnimRot = sin(clipElapsedMs / 180.0).toFloat() * 18f
        }
        AnimationType.Flash -> {
            liveAnimAlpha = if ((clipElapsedMs / 90) % 2 == 0L) 1.0f else 0.15f
        }
        AnimationType.Swing -> {
            liveAnimRot = sin(clipElapsedMs / 250.0).toFloat() * 12f
            liveAnimScale = 1.0f + 0.05f * sin(clipElapsedMs / 250.0).toFloat()
        }
        else -> {}
    }

    // Transitions (CrossDissolve, Wipe, Slide, etc.)
    val transDuration = activeClip?.transitionDurationMs ?: 500L
    val transProgress = (clipElapsedMs.toFloat() / transDuration.coerceAtLeast(200L)).coerceIn(0f, 1f)
    when (activeClip?.transition) {
        TransitionType.CrossDissolve, TransitionType.FadeToBlack -> {
            liveAnimAlpha *= transProgress
        }
        TransitionType.SlideLeft -> {
            liveAnimSlideX += (1f - transProgress) * 400f
        }
        TransitionType.SlideRight -> {
            liveAnimSlideX -= (1f - transProgress) * 400f
        }
        TransitionType.ZoomBlur -> {
            liveAnimScale *= (1.4f - 0.4f * transProgress)
        }
        TransitionType.WipeLeft -> {
            liveAnimSlideX += (1f - transProgress) * 600f
        }
        else -> {}
    }

    // Dynamic Keyframe Interpolation & Auto-Creation (CapCut style)
    val sortedKeyframes = remember(activeClip?.keyframes) {
        activeClip?.keyframes?.sortedBy { it.timeMs } ?: emptyList()
    }

    // Interpolate current keyframe values
    val currentKfTransform = remember(clipElapsedMs, sortedKeyframes) {
        if (sortedKeyframes.isEmpty()) {
            Keyframe(timeMs = clipElapsedMs, x = 0f, y = 0f, scale = 1f, rotation = 0f, opacity = 1f)
        } else if (clipElapsedMs <= sortedKeyframes.first().timeMs) {
            sortedKeyframes.first()
        } else if (clipElapsedMs >= sortedKeyframes.last().timeMs) {
            sortedKeyframes.last()
        } else {
            val k1 = sortedKeyframes.last { it.timeMs <= clipElapsedMs }
            val k2 = sortedKeyframes.first { it.timeMs > clipElapsedMs }
            val factor = if (k2.timeMs == k1.timeMs) 0f else (clipElapsedMs - k1.timeMs).toFloat() / (k2.timeMs - k1.timeMs)
            Keyframe(
                timeMs = clipElapsedMs,
                x = k1.x + (k2.x - k1.x) * factor,
                y = k1.y + (k2.y - k1.y) * factor,
                scale = k1.scale + (k2.scale - k1.scale) * factor,
                rotation = k1.rotation + (k2.rotation - k1.rotation) * factor,
                opacity = k1.opacity + (k2.opacity - k1.opacity) * factor
            )
        }
    }

    var livePanX by remember(activeClip?.id) { mutableFloatStateOf(0f) }
    var livePanY by remember(activeClip?.id) { mutableFloatStateOf(0f) }
    var liveZoom by remember(activeClip?.id) { mutableFloatStateOf(1f) }
    var liveRotation by remember(activeClip?.id) { mutableFloatStateOf(0f) }

    if (videoUri.isNullOrEmpty() && activeClip == null) {
        Box(
            modifier = modifier
                .background(Color(0xFF0F172A))
                .fillMaxSize()
                .clickable { onAddMediaClicked?.invoke() },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    modifier = Modifier
                        .size(64.dp)
                        .clickable { onAddMediaClicked?.invoke() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = strings.importMedia,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = strings.noMediaAdded,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = strings.tapToAddMediaHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
        return
    }

    val currentKfTransformState by rememberUpdatedState(currentKfTransform)
    val currentClipElapsedMsState by rememberUpdatedState(clipElapsedMs)

    Box(
        modifier = modifier
            .background(Color.Black)
            .pointerInput(activeClip?.id) {
                detectTransformGestures { _, pan, zoom, rotation ->
                    val kf = currentKfTransformState
                    val elapsedMs = currentClipElapsedMsState
                    val newX = kf.x + pan.x
                    val newY = kf.y + pan.y
                    val newScale = (kf.scale * zoom).coerceIn(0.2f, 5.0f)
                    val newRot = kf.rotation + rotation

                    // CapCut behavior: automatically create or update keyframe at current playhead timestamp!
                    activeClip?.let { clip ->
                        onKeyframeUpdated?.invoke(
                            clip.id,
                            Keyframe(
                                timeMs = elapsedMs,
                                x = newX,
                                y = newY,
                                scale = newScale,
                                rotation = newRot,
                                opacity = kf.opacity
                            )
                        )
                    }
                }
            }
            .graphicsLayer {
                val totalX = currentKfTransform.x + liveAnimSlideX
                val totalY = currentKfTransform.y
                val totalScale = currentKfTransform.scale * liveAnimScale
                val totalRot = currentKfTransform.rotation + liveAnimRot
                val totalAlpha = (currentKfTransform.opacity * liveAnimAlpha).coerceIn(0f, 1f)
                val mediaScale = transform?.scale ?: 1.0f

                this.translationX = totalX
                this.translationY = totalY
                this.scaleX = baseScaleX * totalScale * mediaScale
                this.scaleY = baseScaleY * totalScale * mediaScale
                this.rotationZ = totalRot
                this.alpha = totalAlpha
            }
            .then(if (clipShape != null) Modifier.clip(clipShape) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (isImageMedia) {
            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap!!.asImageBitmap(),
                    contentDescription = null,
                    colorFilter = ColorFilter.colorMatrix(colorMatrix),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(modifier = Modifier.fillMaxSize())
            }
        } else {
            val grading = activeClip?.effects?.filterIsInstance<EffectConfig.ColorGrading>()?.firstOrNull()
            val filter = activeClip?.effects?.filterIsInstance<EffectConfig.Filter>()?.firstOrNull()

            AndroidView(
                factory = { ctx ->
                    val pv = try {
                        android.view.LayoutInflater.from(ctx).inflate(com.librecuts.rust.R.layout.exo_player_view, null) as PlayerView
                    } catch (e: Exception) {
                        PlayerView(ctx)
                    }
                    pv.apply {
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
                                "motion_blur", "blur" -> {
                                    drawRect(Color.White.copy(alpha = 0.10f * f.intensity), blendMode = BlendMode.Screen)
                                    drawRect(Color.Black.copy(alpha = 0.12f * f.intensity), blendMode = BlendMode.Multiply)
                                }
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
 */
private fun removeBackgroundFromBitmap(source: Bitmap, threshold: Float = 0.45f, invert: Boolean = false): Bitmap {
    val width = source.width
    val height = source.height
    val result = source.copy(Bitmap.Config.ARGB_8888, true)
    val pixels = IntArray(width * height)
    result.getPixels(pixels, 0, width, 0, 0, width, height)

    // Corner reference color sampling
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
            pixels[i] = 0x00000000 // Alpha zero transparent
        }
    }

    result.setPixels(pixels, 0, width, 0, 0, width, height)
    return result
}
