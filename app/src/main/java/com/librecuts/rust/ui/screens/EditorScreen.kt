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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.librecuts.rust.engine.Clip
import com.librecuts.rust.engine.Composition
import com.librecuts.rust.engine.EffectConfig
import com.librecuts.rust.engine.ExportManager
import com.librecuts.rust.engine.FileUtils
import com.librecuts.rust.engine.Track
import com.librecuts.rust.engine.TrackType
import com.librecuts.rust.ui.components.TimelineView
import com.librecuts.rust.ui.components.VideoPreviewPlayer
import com.librecuts.rust.ui.theme.AppThemeMode
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    currentTheme: AppThemeMode,
    onThemeChanged: (AppThemeMode) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exportManager = remember { ExportManager(context) }
    val exportState by exportManager.exportState.collectAsState()

    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var selectedClipId by remember { mutableStateOf<String?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showEffectsSheet by remember { mutableStateOf(false) }
    var showThemeMenu by remember { mutableStateOf(false) }

    // Multi-track composition state with Undo / Redo history
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

    val undoStack = remember { mutableStateListOf<Composition>() }
    val redoStack = remember { mutableStateListOf<Composition>() }

    fun updateCompositionWithUndo(newComp: Composition) {
        undoStack.add(composition)
        redoStack.clear()
        composition = newComp
    }

    // Modern Android 14+ Photo & Video Picker contract with local cached path resolution
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val (localPath, realDurationMs) = FileUtils.resolveMediaUriToLocalFile(context, it)
                val clipId = UUID.randomUUID().toString()
                val newClip = Clip(
                    id = clipId,
                    file_path = localPath,
                    source_start_ms = 0L,
                    source_end_ms = realDurationMs,
                    timeline_start_ms = composition.totalDurationMs
                )

                val updatedTracks = composition.tracks.map { track ->
                    if (track.track_type == TrackType.Video) {
                        track.copy(clips = track.clips + newClip)
                    } else {
                        track
                    }
                }
                updateCompositionWithUndo(composition.copy(tracks = updatedTracks))
                selectedClipId = clipId
            }
        }
    }

    val selectedClip = composition.tracks
        .flatMap { it.clips }
        .find { it.id == selectedClipId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "LibreCut RS",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    // Undo Button
                    IconButton(
                        onClick = {
                            if (undoStack.isNotEmpty()) {
                                val prev = undoStack.removeAt(undoStack.lastIndex)
                                redoStack.add(composition)
                                composition = prev
                            }
                        },
                        enabled = undoStack.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Undo,
                            contentDescription = "Undo",
                            tint = if (undoStack.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }

                    // Redo Button
                    IconButton(
                        onClick = {
                            if (redoStack.isNotEmpty()) {
                                val next = redoStack.removeAt(redoStack.lastIndex)
                                undoStack.add(composition)
                                composition = next
                            }
                        },
                        enabled = redoStack.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Redo,
                            contentDescription = "Redo",
                            tint = if (redoStack.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }

                    // Theme Switcher Menu
                    Box {
                        IconButton(onClick = { showThemeMenu = true }) {
                            Icon(
                                imageVector = when (currentTheme) {
                                    AppThemeMode.LIGHT -> Icons.Default.LightMode
                                    AppThemeMode.DARK -> Icons.Default.DarkMode
                                    AppThemeMode.PURE_BLACK -> Icons.Default.ColorLens
                                },
                                contentDescription = "Switch Theme",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        DropdownMenu(
                            expanded = showThemeMenu,
                            onDismissRequest = { showThemeMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("☀️ Light Mode") },
                                onClick = {
                                    onThemeChanged(AppThemeMode.LIGHT)
                                    showThemeMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("🌙 Dark Mode") },
                                onClick = {
                                    onThemeChanged(AppThemeMode.DARK)
                                    showThemeMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("⬛ Pure Black (OLED)") },
                                onClick = {
                                    onThemeChanged(AppThemeMode.PURE_BLACK)
                                    showThemeMenu = false
                                }
                            )
                        }
                    }

                    // Import Video Button
                    Button(
                        onClick = { videoPickerLauncher.launch("video/*") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Import", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import")
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Export Button
                    Button(
                        onClick = { showExportDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = "Export")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Live Preview Player Box with Text Overlay (Upper half)
            val currentVideoUri = composition.tracks
                .find { it.track_type == TrackType.Video }
                ?.clips
                ?.firstOrNull()
                ?.file_path

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.0f)
            ) {
                VideoPreviewPlayer(
                    videoUri = currentVideoUri,
                    isPlaying = isPlaying,
                    currentPositionMs = currentPositionMs,
                    onPositionChanged = { currentPositionMs = it },
                    onPlaybackEnded = { isPlaying = false },
                    modifier = Modifier.fillMaxSize()
                )

                // Render Active Text Overlays if present on active clip
                val activeTextOverlay = composition.tracks
                    .flatMap { it.clips }
                    .filter { currentPositionMs in it.timeline_start_ms..(it.timeline_start_ms + it.durationMs) }
                    .flatMap { it.effects }
                    .filterIsInstance<EffectConfig.TextOverlay>()
                    .firstOrNull()

                activeTextOverlay?.let { textEff ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.65f))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = textEff.text,
                            fontSize = textEff.font_size.sp,
                            color = Color.White
                        )
                    }
                }
            }

            // Timeline Action Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Play / Pause Toggle
                IconButton(onClick = { isPlaying = !isPlaying }) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = MaterialTheme.colorScheme.primary
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
                                    updateCompositionWithUndo(composition.copy(tracks = updatedTracks))
                                    selectedClipId = secondHalf.id
                                }
                            }
                        },
                        enabled = selectedClip != null
                    ) {
                        Icon(
                            Icons.Default.CallSplit,
                            contentDescription = "Split",
                            tint = if (selectedClip != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }

                    // Effects / Filters / Chroma Key / Text Sheet
                    IconButton(
                        onClick = { showEffectsSheet = true },
                        enabled = selectedClip != null
                    ) {
                        Icon(
                            Icons.Default.AutoFixHigh,
                            contentDescription = "Effects",
                            tint = if (selectedClip != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }

                    // Delete Clip
                    IconButton(
                        onClick = {
                            selectedClipId?.let { id ->
                                val updatedTracks = composition.tracks.map { track ->
                                    track.copy(clips = track.clips.filterNot { it.id == id })
                                }
                                updateCompositionWithUndo(composition.copy(tracks = updatedTracks))
                                selectedClipId = null
                            }
                        },
                        enabled = selectedClip != null
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = if (selectedClip != null) Color.Red else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
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

    // Effects & Text Bottom Sheet
    if (showEffectsSheet && selectedClip != null) {
        ModalBottomSheet(
            onDismissRequest = { showEffectsSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surface
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
                    updateCompositionWithUndo(composition.copy(tracks = updatedTracks))
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
