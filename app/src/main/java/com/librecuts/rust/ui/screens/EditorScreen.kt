package com.librecuts.rust.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.librecuts.rust.engine.Clip
import com.librecuts.rust.engine.Composition
import com.librecuts.rust.engine.ExportManager
import com.librecuts.rust.engine.Track
import com.librecuts.rust.engine.TrackType
import com.librecuts.rust.ui.components.TimelineView
import com.librecuts.rust.ui.components.VideoPreviewPlayer
import com.librecuts.rust.ui.theme.AccentAmber
import com.librecuts.rust.ui.theme.DarkBackground
import com.librecuts.rust.ui.theme.DarkSurface
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exportManager = remember { ExportManager(context) }
    val exportState by exportManager.exportState.collectAsState()

    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var selectedClipId by remember { mutableStateOf<String?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showEffectsSheet by remember { mutableStateOf(false) }

    // Multi-track composition state
    var composition by remember {
        mutableStateOf(
            Composition(
                tracks = listOf(
                    Track(id = "track_video", track_type = TrackType.Video),
                    Track(id = "track_audio", track_type = TrackType.Audio),
                    Track(id = "track_overlay", track_type = TrackType.Overlay)
                )
            )
        )
    }

    // Modern Android 14+ Photo & Video Picker contract
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val clipId = UUID.randomUUID().toString()
            val newClip = Clip(
                id = clipId,
                file_path = it.toString(),
                source_start_ms = 0L,
                source_end_ms = 15_000L, // default 15s window, auto-updated by probe
                timeline_start_ms = composition.totalDurationMs
            )

            // Append to main video track
            val updatedTracks = composition.tracks.map { track ->
                if (track.track_type == TrackType.Video) {
                    track.copy(clips = track.clips + newClip)
                } else {
                    track
                }
            }
            composition = composition.copy(tracks = updatedTracks)
            selectedClipId = clipId
        }
    }

    val selectedClip = composition.tracks
        .flatMap { it.clips }
        .find { it.id == selectedClipId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LibreCut RS (Android 14+)", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface),
                actions = {
                    Button(
                        onClick = { videoPickerLauncher.launch("video/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurface)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Clip", tint = AccentAmber)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import", color = Color.White)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { showExportDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentAmber)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = "Export", tint = Color.Black)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export", color = Color.Black)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBackground)
        ) {
            // Live Preview Player (Upper half)
            val currentVideoUri = composition.tracks
                .find { it.track_type == TrackType.Video }
                ?.clips
                ?.firstOrNull()
                ?.file_path

            VideoPreviewPlayer(
                videoUri = currentVideoUri,
                isPlaying = isPlaying,
                currentPositionMs = currentPositionMs,
                onPositionChanged = { currentPositionMs = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.0f)
            )

            // Timeline Action Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(DarkSurface)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Play / Pause
                IconButton(onClick = { isPlaying = !isPlaying }) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = AccentAmber
                    )
                }

                Row {
                    // Split at Playhead
                    IconButton(
                        onClick = {
                            selectedClip?.let { clip ->
                                val splitOffset = currentPositionMs - clip.timeline_start_ms
                                if (splitOffset in 100 until clip.durationMs) {
                                    val firstHalf = clip.copy(
                                        id = UUID.randomUUID().toString(),
                                        source_end_ms = clip.source_start_ms + splitOffset
                                    )
                                    val secondHalf = clip.copy(
                                        id = UUID.randomUUID().toString(),
                                        source_start_ms = clip.source_start_ms + splitOffset,
                                        timeline_start_ms = currentPositionMs
                                    )

                                    val updatedTracks = composition.tracks.map { track ->
                                        track.copy(clips = track.clips.flatMap {
                                            if (it.id == clip.id) listOf(firstHalf, secondHalf) else listOf(it)
                                        })
                                    }
                                    composition = composition.copy(tracks = updatedTracks)
                                    selectedClipId = secondHalf.id
                                }
                            }
                        },
                        enabled = selectedClip != null
                    ) {
                        Icon(Icons.Default.CallSplit, contentDescription = "Split", tint = Color.White)
                    }

                    // Effects / Filters / Chroma Key
                    IconButton(
                        onClick = { showEffectsSheet = true },
                        enabled = selectedClip != null
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = "Effects", tint = AccentAmber)
                    }

                    // Delete Clip
                    IconButton(
                        onClick = {
                            selectedClipId?.let { id ->
                                val updatedTracks = composition.tracks.map { track ->
                                    track.copy(clips = track.clips.filterNot { it.id == id })
                                }
                                composition = composition.copy(tracks = updatedTracks)
                                selectedClipId = null
                            }
                        },
                        enabled = selectedClip != null
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                    }
                }
            }

            // Multi-Track Timeline (Lower half)
            TimelineView(
                composition = composition,
                currentPositionMs = currentPositionMs,
                selectedClipId = selectedClipId,
                onClipSelected = { clip -> selectedClipId = clip.id },
                onSeekPosition = { newPos -> currentPositionMs = newPos },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.9f)
            )
        }
    }

    // Effects Bottom Sheet
    if (showEffectsSheet && selectedClip != null) {
        ModalBottomSheet(
            onDismissRequest = { showEffectsSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            EffectsSheet(
                clip = selectedClip,
                onUpdateEffects = { newEffects, newSpeed, newVolume ->
                    val updatedTracks = composition.tracks.map { track ->
                        track.copy(clips = track.clips.map {
                            if (it.id == selectedClip.id) {
                                it.copy(effects = newEffects, speed = newSpeed, volume = newVolume)
                            } else it
                        })
                    }
                    composition = composition.copy(tracks = updatedTracks)
                }
            )
        }
    }

    // Export Dialog
    if (showExportDialog) {
        ExportDialog(
            exportState = exportState,
            onStartExport = { res, fps, bitrate, smartRender ->
                scope.launch {
                    exportManager.exportComposition(
                        composition = composition,
                        resolution = res,
                        fps = fps,
                        bitrateMbps = bitrate,
                        smartRender = smartRender
                    )
                }
            },
            onCancel = { exportManager.cancel() },
            onDismiss = { showExportDialog = false }
        )
    }
}

