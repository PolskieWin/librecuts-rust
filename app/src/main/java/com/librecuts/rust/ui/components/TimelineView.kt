package com.librecuts.rust.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.librecuts.rust.engine.AnimationType
import com.librecuts.rust.engine.Clip
import com.librecuts.rust.engine.Composition
import com.librecuts.rust.engine.Track
import com.librecuts.rust.engine.TrackType
import com.librecuts.rust.ui.theme.LocalAppStrings
import com.librecuts.rust.ui.theme.getAnimationDisplayName
import com.librecuts.rust.ui.theme.getTransitionDisplayName
import com.librecuts.rust.engine.TransitionType
import com.librecuts.rust.ui.theme.TrackAudioColor
import com.librecuts.rust.ui.theme.TrackOverlayColor
import com.librecuts.rust.ui.theme.TrackVideoColor

@Composable
fun TimelineView(
    composition: Composition,
    currentPositionMs: Long,
    selectedClipId: String?,
    onClipSelected: (Clip) -> Unit,
    onSeekPosition: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberHapticHelper()
    val strings = LocalAppStrings.current
    val density = LocalDensity.current

    val totalDurationMs = composition.totalDurationMs.coerceAtLeast(10_000L)
    val defaultScale = if (totalDurationMs > 600_000L) (15_000f / totalDurationMs).coerceIn(0.005f, 0.05f) else 0.05f
    var zoomScale by remember(totalDurationMs) { mutableFloatStateOf(defaultScale) } // dp per millisecond
    val scrollState = rememberScrollState()

    val pxPerMs = with(density) { zoomScale.dp.toPx() }
    val totalWidthDp = ((totalDurationMs * zoomScale)).coerceIn(300f, 20_000f).dp

    // Magnetic Snapping Points: 0, and clip starts/ends
    val snapPoints = remember(composition) {
        val points = mutableListOf(0L)
        composition.tracks.forEach { track ->
            track.clips.forEach { clip ->
                points.add(clip.timeline_start_ms)
                points.add(clip.timeline_start_ms + clip.durationMs)
            }
        }
        points.distinct()
    }

    var lastSnappedMs by remember { mutableStateOf<Long?>(null) }

    fun applySnap(rawMs: Long): Long {
        val thresholdMs = 120L
        val snapCandidate = snapPoints.find { Math.abs(it - rawMs) <= thresholdMs }
        return if (snapCandidate != null) {
            if (lastSnappedMs != snapCandidate) {
                haptics.snap()
                lastSnappedMs = snapCandidate
            }
            snapCandidate
        } else {
            lastSnappedMs = null
            rawMs.coerceIn(0L, totalDurationMs)
        }
    }

    val minScale = (600f / totalDurationMs).coerceIn(0.001f, 0.05f)
    val transformState = rememberTransformableState { zoomChange, _, _ ->
        zoomScale = (zoomScale * zoomChange).coerceIn(minScale, 0.35f)
    }

    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .fillMaxSize()
    ) {
        // Left Space Sidebar: Track Headers & 3-Lines Timestamp Scrubbing Drag Handle
        Column(
            modifier = Modifier
                .width(48.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
        ) {
            // Three-Lines Jog Scrubber Handle by the Left Space
            val currentPosSnapState by rememberUpdatedState(currentPositionMs)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                    .pointerInput(totalDurationMs) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaMs = (dragAmount.x * 20L + dragAmount.y * 10L).toLong()
                            val newMs = (currentPosSnapState + deltaMs).coerceIn(0L, totalDurationMs)
                            onSeekPosition(applySnap(newMs))
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Scrub Timestamps Handle",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Track Headers with 3-Lines Grip and Icons
            composition.tracks.forEach { track ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(vertical = 3.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = "Track Reorder Handle",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(14.dp)
                        )
                        Icon(
                            imageVector = when (track.track_type) {
                                TrackType.Video -> Icons.Default.Videocam
                                TrackType.Audio -> Icons.Default.GraphicEq
                                TrackType.Overlay -> Icons.Default.Layers
                                TrackType.Text -> Icons.Default.TextFields
                            },
                            contentDescription = null,
                            tint = when (track.track_type) {
                                TrackType.Video -> TrackVideoColor
                                TrackType.Audio -> TrackAudioColor
                                TrackType.Overlay -> TrackOverlayColor
                                TrackType.Text -> MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Down Left Space: Dedicated timestamp movement handle higher than bottom buttons
            val leftDownPosState by rememberUpdatedState(currentPositionMs)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .heightIn(min = 68.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(topStart = 8.dp)
                    )
                    .pointerInput(totalDurationMs) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaMs = (dragAmount.x * 25L + dragAmount.y * 15L).toLong()
                            val newMs = (leftDownPosState + deltaMs).coerceIn(0L, totalDurationMs)
                            onSeekPosition(applySnap(newMs))
                        }
                    }
                    .pointerInput(totalDurationMs) {
                        detectTapGestures {
                            haptics.tick()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Move Timeline",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Main Scrollable Timeline Tracks
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .transformable(state = transformState)
                .horizontalScroll(scrollState)
                .pointerInput(totalDurationMs, pxPerMs) {
                    detectTapGestures { offset ->
                        if (pxPerMs > 0f) {
                            val clickedTimeMs = (offset.x / pxPerMs).toLong().coerceIn(0L, totalDurationMs)
                            onSeekPosition(applySnap(clickedTimeMs))
                        }
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .width(totalWidthDp)
                    .fillMaxSize()
            ) {
                // High-performance Viewport-Clipped Time Ruler
                TimeRuler(
                    durationMs = totalDurationMs,
                    scale = zoomScale,
                    scrollOffsetPx = scrollState.value,
                    onSeek = { ms -> onSeekPosition(applySnap(ms)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )

                // Tracks
                composition.tracks.forEach { track ->
                    TrackRow(
                        track = track,
                        scale = zoomScale,
                        selectedClipId = selectedClipId,
                        onClipSelected = { clip ->
                            haptics.tick()
                            onClipSelected(clip)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(vertical = 3.dp)
                    )
                }

                // Down Free Space: Move Through Timestamps by dragging in bottom free space (Elevated higher than buttons)
                val currentPosState by rememberUpdatedState(currentPositionMs)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .heightIn(min = 68.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .pointerInput(totalDurationMs, pxPerMs) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                if (pxPerMs > 0f) {
                                    val deltaMs = (dragAmount.x / pxPerMs).toLong()
                                    val newPos = (currentPosState + deltaMs).coerceIn(0L, totalDurationMs)
                                    onSeekPosition(applySnap(newPos))
                                }
                            }
                        }
                        .pointerInput(totalDurationMs, pxPerMs) {
                            detectTapGestures { offset ->
                                if (pxPerMs > 0f) {
                                    val clickedTimeMs = ((offset.x + scrollState.value) / pxPerMs).toLong().coerceIn(0L, totalDurationMs)
                                    onSeekPosition(applySnap(clickedTimeMs))
                                }
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.alpha(0.6f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.scrubTimestamps,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Playhead Scrubber Needle with Real-Time Floating Timecode Badge
            val playheadOffsetDp = (currentPositionMs * zoomScale).dp
            val min = currentPositionMs / 60000
            val sec = (currentPositionMs % 60000) / 1000
            val ms = currentPositionMs % 1000
            val timecodeStr = String.format("%02d:%02d.%03d", min, sec, ms)

            val playheadPosState by rememberUpdatedState(currentPositionMs)
            Box(
                modifier = Modifier
                    .offset(x = playheadOffsetDp - 28.dp)
                    .width(56.dp)
                    .fillMaxHeight()
                    .pointerInput(totalDurationMs, pxPerMs) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            if (pxPerMs > 0f) {
                                val deltaMs = (dragAmount.x / pxPerMs).toLong()
                                val newPos = (playheadPosState + deltaMs).coerceIn(0L, totalDurationMs)
                                onSeekPosition(applySnap(newPos))
                            }
                        }
                    }
            ) {
                // Floating timestamp bubble above playhead
                val playheadColor = MaterialTheme.colorScheme.primary
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = playheadColor,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = 2.dp)
                ) {
                    Text(
                        text = timecodeStr,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }

                // Playhead Needle Line
                Canvas(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = 18.dp)
                        .width(4.dp)
                        .fillMaxHeight()
                ) {
                    // Playhead Flag
                    val headPath = Path().apply {
                        moveTo(-6f, 0f)
                        lineTo(6f, 0f)
                        lineTo(0f, 10f)
                        close()
                    }
                    drawPath(path = headPath, color = playheadColor)

                    // Playhead Vertical Line
                    drawLine(
                        color = playheadColor,
                        start = Offset(0f, 0f),
                        end = Offset(0f, size.height),
                        strokeWidth = 2.5f
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeRuler(
    durationMs: Long,
    scale: Float,
    scrollOffsetPx: Int,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val tickColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    val subTickColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
    val density = LocalDensity.current
    val pxPerMs = with(density) { scale.dp.toPx() }

    Canvas(
        modifier = modifier
            .pointerInput(durationMs, pxPerMs) {
                detectTapGestures { offset ->
                    if (pxPerMs > 0f) {
                        val clickedMs = (offset.x / pxPerMs).toLong().coerceIn(0L, durationMs)
                        onSeek(clickedMs)
                    }
                }
            }
            .pointerInput(durationMs, pxPerMs) {
                detectDragGestures { change, _ ->
                    change.consume()
                    if (pxPerMs > 0f) {
                        val draggedMs = (change.position.x / pxPerMs).toLong().coerceIn(0L, durationMs)
                        onSeek(draggedMs)
                    }
                }
            }
    ) {
        if (pxPerMs <= 0f) return@Canvas

        val stepMs = when {
            pxPerMs >= 0.2f -> 500L
            pxPerMs >= 0.08f -> 1000L
            pxPerMs >= 0.03f -> 2000L
            pxPerMs >= 0.01f -> 5000L
            else -> 10000L
        }

        // Clip tick drawing strictly to the visible window on screen (+ safe buffer)
        val viewStartPx = (scrollOffsetPx - 150f).coerceAtLeast(0f)
        val viewEndPx = scrollOffsetPx + 4000f

        val startMs = ((viewStartPx / pxPerMs).toLong() / stepMs * stepMs).coerceAtLeast(0L)
        val endMs = (((viewEndPx / pxPerMs).toLong() / stepMs + 1) * stepMs).coerceAtMost(durationMs)

        var currentMs = startMs
        while (currentMs <= endMs) {
            val x = currentMs * pxPerMs
            val isMajor = currentMs % (stepMs * 5) == 0L
            val tickHeight = if (isMajor) 14.dp.toPx() else 7.dp.toPx()

            drawLine(
                color = if (isMajor) tickColor else subTickColor,
                start = Offset(x, size.height - tickHeight),
                end = Offset(x, size.height),
                strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()
            )
            currentMs += stepMs
        }
    }
}

@Composable
private fun TrackRow(
    track: Track,
    scale: Float,
    selectedClipId: String?,
    onClipSelected: (Clip) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        track.clips.forEach { clip ->
            val startDp = (clip.timeline_start_ms * scale).dp
            val widthDp = (clip.durationMs * scale).coerceAtLeast(36f).dp
            val isSelected = clip.id == selectedClipId

            val clipColor = when (track.track_type) {
                TrackType.Video -> TrackVideoColor
                TrackType.Audio -> TrackAudioColor
                TrackType.Overlay -> TrackOverlayColor
                TrackType.Text -> Color(0xFFE65100)
            }

            Box(
                modifier = Modifier
                    .offset(x = startDp)
                    .width(widthDp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) clipColor else clipColor.copy(alpha = 0.75f))
                    .pointerInput(clip.id) {
                        detectTapGestures {
                            onClipSelected(clip)
                        }
                    }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (track.track_type) {
                            TrackType.Video -> Icons.Default.Videocam
                            TrackType.Audio -> Icons.Default.GraphicEq
                            TrackType.Overlay -> Icons.Default.Layers
                            TrackType.Text -> Icons.Default.TextFields
                        },
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = clip.id.take(8),
                        color = Color.White,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }

                // Render Keyframe Diamond Markers on Clip
                clip.keyframes.forEach { kf ->
                    val kfOffsetDp = (kf.timeMs * scale).dp
                    Icon(
                        imageVector = Icons.Default.Diamond,
                        contentDescription = "Keyframe",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .offset(x = kfOffsetDp)
                            .align(Alignment.BottomStart)
                            .size(11.dp)
                    )
                }

                // Show Transition Badge at start timestamp (clamped to clip duration, smaller but not bigger)
                if (clip.transition != TransitionType.None) {
                    val clipWidthDp = (clip.durationMs * scale).dp
                    val transDur = clip.transitionDurationMs.coerceIn(100L, clip.durationMs)
                    val transWidthDp = (transDur * scale).dp.coerceAtLeast(8.dp).coerceAtMost(clipWidthDp)
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .width(transWidthDp)
                            .fillMaxHeight()
                            .background(Color.White.copy(alpha = 0.25f))
                            .border(1.dp, Color.White.copy(alpha = 0.65f), RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp)),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Transform,
                                contentDescription = "Transition: ${getTransitionDisplayName(clip.transition, strings)}",
                                tint = Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                            if (transWidthDp > 36.dp) {
                                Text(
                                    text = getTransitionDisplayName(clip.transition, strings).take(5),
                                    fontSize = 8.sp,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Show Animation Badge at timestamp (clamped to clip duration, smaller but not bigger)
                if (clip.animation != AnimationType.None) {
                    val clipWidthDp = (clip.durationMs * scale).dp
                    val animDur = clip.animationDurationMs.coerceIn(100L, clip.durationMs)
                    val animWidthDp = (animDur * scale).dp.coerceAtLeast(8.dp).coerceAtMost(clipWidthDp)
                    val isOutAnim = clip.animation.category == "Out"
                    Box(
                        modifier = Modifier
                            .align(if (isOutAnim) Alignment.CenterEnd else Alignment.CenterStart)
                            .width(animWidthDp)
                            .height(13.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.65f), RoundedCornerShape(3.dp))
                            .padding(horizontal = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = getAnimationDisplayName(clip.animation, strings).take(6),
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
