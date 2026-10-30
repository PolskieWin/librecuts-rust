package com.librecuts.rust.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.librecuts.rust.engine.Clip
import com.librecuts.rust.engine.Composition
import com.librecuts.rust.engine.Track
import com.librecuts.rust.engine.TrackType
import com.librecuts.rust.ui.theme.DarkSurface
import com.librecuts.rust.ui.theme.DarkSurfaceElevated
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
    var zoomScale by remember { mutableFloatStateOf(0.05f) } // pixels per millisecond
    val scrollState = rememberScrollState()

    val totalDurationMs = composition.totalDurationMs.coerceAtLeast(10_000L)
    val totalWidthDp = ((totalDurationMs * zoomScale)).coerceAtLeast(300f).dp

    val transformState = rememberTransformableState { zoomChange, _, _ ->
        zoomScale = (zoomScale * zoomChange).coerceIn(0.01f, 0.5f)
    }

    Box(
        modifier = modifier
            .background(DarkSurface)
            .transformable(state = transformState)
            .horizontalScroll(scrollState)
            .pointerInput(totalDurationMs) {
                detectTapGestures { offset ->
                    val clickedTimeMs = (offset.x / zoomScale).toLong()
                    onSeekPosition(clickedTimeMs.coerceIn(0L, totalDurationMs))
                }
            }
    ) {
        Column(
            modifier = Modifier
                .width(totalWidthDp)
                .fillMaxSize()
        ) {
            // Time Ruler
            TimeRuler(
                durationMs = totalDurationMs,
                scale = zoomScale,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(DarkSurfaceElevated)
            )

            // Tracks
            composition.tracks.forEach { track ->
                TrackRow(
                    track = track,
                    scale = zoomScale,
                    selectedClipId = selectedClipId,
                    onClipSelected = onClipSelected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(vertical = 3.dp)
                )
            }
        }

        // Playhead Scrubber Needle
        val playheadOffsetDp = (currentPositionMs * zoomScale).dp
        Canvas(
            modifier = Modifier
                .offset(x = playheadOffsetDp)
                .width(2.dp)
                .fillMaxSize()
        ) {
            drawLine(
                color = Color.Red,
                start = Offset(0f, 0f),
                end = Offset(0f, size.height),
                strokeWidth = 4f
            )
        }
    }
}

@Composable
private fun TimeRuler(
    durationMs: Long,
    scale: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val stepMs = 1000L
        var currentMs = 0L

        while (currentMs <= durationMs) {
            val x = currentMs * scale
            val isMajor = currentMs % 5000L == 0L
            val tickHeight = if (isMajor) 14f else 7f

            drawLine(
                color = if (isMajor) Color.LightGray else Color.DarkGray,
                start = Offset(x, size.height - tickHeight),
                end = Offset(x, size.height),
                strokeWidth = 2f
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
    Box(
        modifier = modifier
            .background(DarkSurfaceElevated.copy(alpha = 0.5f))
    ) {
        track.clips.forEach { clip ->
            val startDp = (clip.timeline_start_ms * scale).dp
            val widthDp = (clip.durationMs * scale).coerceAtLeast(30f).dp
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
                    .fillMaxSize()
                    .padding(horizontal = 1.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isSelected) Color.White else clipColor)
                    .pointerInput(clip.id) {
                        detectTapGestures { onClipSelected(clip) }
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "${track.track_type.name}: ${clip.durationMs / 1000}s",
                    fontSize = 11.sp,
                    color = if (isSelected) Color.Black else Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp),
                    maxLines = 1
                )
            }
        }
    }
}

