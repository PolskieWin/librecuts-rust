package com.librecuts.rust.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Subtitles
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.librecuts.rust.engine.AudioRecorderHelper
import com.librecuts.rust.engine.Clip
import com.librecuts.rust.engine.Composition
import com.librecuts.rust.engine.EffectConfig
import com.librecuts.rust.engine.ExportManager
import com.librecuts.rust.engine.FileUtils
import com.librecuts.rust.engine.Keyframe
import com.librecuts.rust.engine.Project
import com.librecuts.rust.engine.SrtParser
import com.librecuts.rust.engine.Track
import com.librecuts.rust.engine.TrackType
import com.librecuts.rust.plugin.PluginManager
import com.librecuts.rust.ui.components.TimelineView
import com.librecuts.rust.ui.components.VideoPreviewPlayer
import com.librecuts.rust.ui.components.rememberHapticHelper
import com.librecuts.rust.ui.theme.AppThemeMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    project: Project,
    currentTheme: AppThemeMode,
    onThemeChanged: (AppThemeMode) -> Unit,
    onSaveProject: (Project) -> Unit,
    onBackToProjects: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = rememberHapticHelper()
    val pluginManager = remember { PluginManager(context) }
    val audioRecorder = remember { AudioRecorderHelper(context) }
    val exportManager = remember { ExportManager(context) }
    val exportState by exportManager.exportState.collectAsState()

    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var selectedClipId by remember { mutableStateOf<String?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showEffectsSheet by remember { mutableStateOf(false) }
    var showPluginManagerSheet by remember { mutableStateOf(false) }
    var showThemeMenu by remember { mutableStateOf(false) }
    var showImportMenu by remember { mutableStateOf(false) }

    var isRecordingVoiceover by remember { mutableStateOf(false) }
    var recordElapsedSeconds by remember { mutableLongStateOf(0L) }

    // Multi-track composition state loaded directly from the project
    var composition by remember(project.id) {
        mutableStateOf(project.composition)
    }

    val undoStack = remember { mutableStateListOf<Composition>() }
    val redoStack = remember { mutableStateListOf<Composition>() }

    fun updateCompositionWithUndo(newComp: Composition) {
        undoStack.add(composition)
        redoStack.clear()
        composition = newComp
        onSaveProject(project.copy(composition = newComp))
    }

    // Modern Android 14+ Media Pickers
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
                haptics.split()
            }
        }
    }

    val srtPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    context.contentResolver.openInputStream(it)?.use { stream ->
                        val entries = SrtParser.parse(stream)
                        val textClips = SrtParser.toClips(entries)
                        if (textClips.isNotEmpty()) {
                            val updatedTracks = composition.tracks.map { track ->
                                if (track.track_type == TrackType.Text) {
                                    track.copy(clips = track.clips + textClips)
                                } else {
                                    track
                                }
                            }
                            updateCompositionWithUndo(composition.copy(tracks = updatedTracks))
                            haptics.split()
                            Toast.makeText(context, "Imported ${textClips.size} subtitles!", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to parse .srt file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        if (granted) {
            val started = audioRecorder.startRecording()
            if (started) {
                isRecordingVoiceover = true
                recordElapsedSeconds = 0L
                haptics.snap()
            }
        } else {
            Toast.makeText(context, "Microphone permission required for voiceover", Toast.LENGTH_SHORT).show()
        }
    }

    // Voiceover timer ticker
    LaunchedEffect(isRecordingVoiceover) {
        if (isRecordingVoiceover) {
            while (isRecordingVoiceover) {
                delay(1000L)
                recordElapsedSeconds++
            }
        }
    }

    val selectedClip = composition.tracks
        .flatMap { it.clips }
        .find { it.id == selectedClipId }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        haptics.tick()
                        onBackToProjects()
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back to Projects",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = project.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Text(
                            text = "${project.aspectRatio} • Rust Vulkan Hardware Accel",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    // Undo Button
                    IconButton(
                        onClick = {
                            if (undoStack.isNotEmpty()) {
                                haptics.tick()
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
                                haptics.tick()
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

                    // Plugins Framework Button
                    IconButton(onClick = {
                        haptics.tick()
                        showPluginManagerSheet = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.Extension,
                            contentDescription = "Community Plugins",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Theme Switcher Menu
                    Box {
                        IconButton(onClick = {
                            haptics.tick()
                            showThemeMenu = true
                        }) {
                            Icon(
                                imageVector = when (currentTheme) {
                                    AppThemeMode.MATERIAL_YOU_AUTO -> Icons.Default.ColorLens
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
                                text = { Text("✨ Material You (Auto)") },
                                onClick = {
                                    onThemeChanged(AppThemeMode.MATERIAL_YOU_AUTO)
                                    showThemeMenu = false
                                }
                            )
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

                    // Import Media Menu
                    Box {
                        Button(
                            onClick = {
                                haptics.tick()
                                showImportMenu = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Import", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add", fontSize = 13.sp)
                        }

                        DropdownMenu(
                            expanded = showImportMenu,
                            onDismissRequest = { showImportMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("🎥 Video / Image Clip") },
                                onClick = {
                                    showImportMenu = false
                                    videoPickerLauncher.launch("video/*")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("📄 Subtitles (.srt)") },
                                onClick = {
                                    showImportMenu = false
                                    srtPickerLauncher.launch("*/*")
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Export Button
                    Button(
                        onClick = {
                            haptics.snap()
                            showExportDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = "Export", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export", fontSize = 13.sp)
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

                // Voiceover Recording Status Banner
                if (isRecordingVoiceover) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Red.copy(alpha = 0.85f),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Recording Voiceover: ${recordElapsedSeconds}s",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

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
                            .background(Color.Black.copy(alpha = 0.7f))
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

            // Timeline Action Controls Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Play / Pause Toggle
                IconButton(onClick = {
                    haptics.tick()
                    isPlaying = !isPlaying
                }) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Split at Playhead
                    IconButton(
                        onClick = {
                            selectedClip?.let { clip ->
                                val splitOffset = currentPositionMs - clip.timeline_start_ms
                                if (splitOffset in 100 until clip.durationMs) {
                                    haptics.split()
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

                    // Keyframe Button (Diamond)
                    IconButton(
                        onClick = {
                            selectedClip?.let { clip ->
                                val offsetMs = (currentPositionMs - clip.timeline_start_ms).coerceIn(0L, clip.durationMs)
                                val hasKeyframe = clip.keyframes.any { Math.abs(it.timeMs - offsetMs) < 150L }
                                val updatedKeyframes = if (hasKeyframe) {
                                    clip.keyframes.filterNot { Math.abs(it.timeMs - offsetMs) < 150L }
                                } else {
                                    clip.keyframes + Keyframe(timeMs = offsetMs)
                                }
                                val updatedTracks = composition.tracks.map { track ->
                                    track.copy(clips = track.clips.map {
                                        if (it.id == clip.id) it.copy(keyframes = updatedKeyframes) else it
                                    })
                                }
                                haptics.snap()
                                updateCompositionWithUndo(composition.copy(tracks = updatedTracks))
                            }
                        },
                        enabled = selectedClip != null
                    ) {
                        Icon(
                            Icons.Default.Diamond,
                            contentDescription = "Keyframe",
                            tint = if (selectedClip != null) Color(0xFFFFEB3B) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }

                    // Voiceover Mic Button
                    IconButton(
                        onClick = {
                            if (isRecordingVoiceover) {
                                // Stop voiceover
                                val result = audioRecorder.stopRecording()
                                isRecordingVoiceover = false
                                if (result != null) {
                                    haptics.split()
                                    val audioClip = Clip(
                                        id = UUID.randomUUID().toString(),
                                        file_path = result.filePath,
                                        source_start_ms = 0L,
                                        source_end_ms = result.durationMs,
                                        timeline_start_ms = currentPositionMs
                                    )
                                    val updatedTracks = composition.tracks.map { track ->
                                        if (track.track_type == TrackType.Audio) {
                                            track.copy(clips = track.clips + audioClip)
                                        } else track
                                    }
                                    updateCompositionWithUndo(composition.copy(tracks = updatedTracks))
                                    Toast.makeText(context, "Voiceover added to audio track!", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                // Start recording
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasPermission) {
                                    if (audioRecorder.startRecording()) {
                                        isRecordingVoiceover = true
                                        recordElapsedSeconds = 0L
                                        haptics.snap()
                                    }
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isRecordingVoiceover) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Voiceover",
                            tint = if (isRecordingVoiceover) Color.Red else MaterialTheme.colorScheme.primary
                        )
                    }

                    // Effects / Filters / Chroma Key / Masking Sheet
                    IconButton(
                        onClick = {
                            haptics.tick()
                            showEffectsSheet = true
                        },
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
                                haptics.heavy()
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

    // Community Plugins Bottom Sheet
    if (showPluginManagerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPluginManagerSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            PluginManagerSheet(
                pluginManager = pluginManager,
                onDismiss = { showPluginManagerSheet = false }
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
