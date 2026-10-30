package com.librecuts.rust.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Extension
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import com.librecuts.rust.engine.ThumbnailCache
import com.librecuts.rust.engine.TtsHelper
import com.librecuts.rust.plugin.UiLayoutMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.librecuts.rust.engine.AnimationType
import com.librecuts.rust.engine.AudioRecorderHelper
import com.librecuts.rust.engine.AutoCaptionHelper
import com.librecuts.rust.engine.Clip
import com.librecuts.rust.engine.Composition
import com.librecuts.rust.engine.EffectConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.filled.AutoGraph
import com.librecuts.rust.engine.ExportManager
import com.librecuts.rust.engine.FileUtils
import com.librecuts.rust.engine.FontManager
import com.librecuts.rust.engine.Keyframe
import com.librecuts.rust.engine.Project
import com.librecuts.rust.engine.SoundEffectsManager
import com.librecuts.rust.engine.SpeedCurveType
import com.librecuts.rust.engine.SrtParser
import com.librecuts.rust.engine.Track
import com.librecuts.rust.engine.TrackType
import com.librecuts.rust.engine.TransitionType
import com.librecuts.rust.plugin.PluginManager
import com.librecuts.rust.ui.components.TimelineView
import com.librecuts.rust.ui.components.VideoPreviewPlayer
import com.librecuts.rust.ui.components.rememberHapticHelper
import com.librecuts.rust.ui.theme.AppThemeMode
import com.librecuts.rust.ui.theme.LocalAppStrings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    project: Project,
    currentTheme: AppThemeMode,
    onThemeChanged: (AppThemeMode) -> Unit,
    isTimeTrackerEnabled: Boolean = false,
    onSaveProject: (Project) -> Unit,
    onBackToProjects: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings = LocalAppStrings.current
    val haptics = rememberHapticHelper()
    val fontManager = remember { FontManager(context) }
    val pluginManager = remember { PluginManager(context) }
    val audioRecorder = remember { AudioRecorderHelper(context) }
    val autoCaptionHelper = remember { AutoCaptionHelper(context) }
    val ttsHelper = remember { TtsHelper(context) }
    DisposableEffect(Unit) {
        onDispose {
            ttsHelper.shutdown()
        }
    }
    val exportManager = remember { ExportManager(context) }
    val exportState by exportManager.exportState.collectAsState()

    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember(project.id) { mutableLongStateOf(project.lastPlayheadMs) }
    var selectedClipId by remember { mutableStateOf<String?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showCompressDialog by remember { mutableStateOf(false) }
    var showEffectsSheet by remember { mutableStateOf(false) }
    var showPluginManagerSheet by remember { mutableStateOf(false) }
    var showAudioSheet by remember { mutableStateOf(false) }
    var showSubtitleSheet by remember { mutableStateOf(false) }
    var showMediaImportSheet by remember { mutableStateOf(false) }
    var showTransitionsSheet by remember { mutableStateOf(false) }
    var showAnimationsSheet by remember { mutableStateOf(false) }
    var showJumpTimestampDialog by remember { mutableStateOf(false) }
    val activeUiLayout = pluginManager.getActiveUiLayout()

    var projectTimeSpentSeconds by remember(project.id) { mutableLongStateOf(project.timeSpentSeconds) }

    // Active project editing time tracker ticker
    LaunchedEffect(isTimeTrackerEnabled) {
        if (isTimeTrackerEnabled) {
            while (true) {
                delay(1000L)
                projectTimeSpentSeconds++
            }
        }
    }

    var isRecordingVoiceover by remember { mutableStateOf(false) }
    var recordElapsedSeconds by remember { mutableLongStateOf(0L) }

    var showTextEditorDialog by remember { mutableStateOf(false) }
    var textToEdit by remember { mutableStateOf("") }
    var editingTextClipId by remember { mutableStateOf<String?>(null) }
    var showCustomScriptDialog by remember { mutableStateOf(false) }
    var customScriptText by remember { mutableStateOf("") }
    var showTtsDialog by remember { mutableStateOf(false) }
    var ttsInitialText by remember { mutableStateOf("") }

    // Multi-track composition state loaded directly from the project
    var composition by remember(project.id) {
        mutableStateOf(project.composition)
    }

    val cutPoints = remember(composition) {
        val pts = mutableListOf(0L)
        composition.tracks.forEach { trk ->
            trk.clips.forEach { c ->
                pts.add(c.timeline_start_ms)
                pts.add(c.timeline_start_ms + c.durationMs)
            }
        }
        pts.distinct().sorted()
    }

    val undoStack = remember { mutableStateListOf<Composition>() }
    val redoStack = remember { mutableStateListOf<Composition>() }

    // Instant save when backgrounded so progress is NEVER lost
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                isPlaying = false
                onSaveProject(
                    project.copy(
                        composition = composition,
                        timeSpentSeconds = projectTimeSpentSeconds,
                        lastPlayheadMs = currentPositionMs
                    )
                )
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    fun updateCompositionWithUndo(newComp: Composition) {
        undoStack.add(composition)
        redoStack.clear()
        composition = newComp
        onSaveProject(
            project.copy(
                composition = newComp,
                timeSpentSeconds = projectTimeSpentSeconds,
                lastPlayheadMs = currentPositionMs
            )
        )
    }

    // Image Import Picker Contract (Spawns smaller than video on Overlay track)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            scope.launch {
                var timelineOffset = currentPositionMs
                val newClips = mutableListOf<Clip>()

                uris.forEach { uri ->
                    val (localPath, _) = FileUtils.resolveMediaUriToLocalFile(context, uri)
                    val imgDurationMs = 3000L
                    newClips.add(
                        Clip(
                            id = UUID.randomUUID().toString(),
                            file_path = localPath,
                            source_start_ms = 0L,
                            source_end_ms = imgDurationMs,
                            timeline_start_ms = timelineOffset,
                            keyframes = listOf(
                                Keyframe(timeMs = 0L, scale = 0.55f, x = 0f, y = 0f)
                            )
                        )
                    )
                    timelineOffset += imgDurationMs
                }

                val hasOverlayTrack = composition.tracks.any { it.track_type == TrackType.Overlay }
                val baseTracks = if (!hasOverlayTrack) {
                    composition.tracks + Track(id = "track_overlay", track_type = TrackType.Overlay)
                } else {
                    composition.tracks
                }

                val updatedTracks = baseTracks.map { track ->
                    if (track.track_type == TrackType.Overlay) {
                        track.copy(clips = track.clips + newClips)
                    } else track
                }
                updateCompositionWithUndo(composition.copy(tracks = updatedTracks))
                selectedClipId = newClips.firstOrNull()?.id
                haptics.split()
                Toast.makeText(context, "Added ${newClips.size} image(s) smaller than video!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Multiple Video Import Picker Contract
    val multipleVideoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            scope.launch {
                var timelineOffset = composition.totalDurationMs
                val newClips = mutableListOf<Clip>()

                uris.forEach { uri ->
                    val (localPath, realDurationMs) = FileUtils.resolveMediaUriToLocalFile(context, uri)
                    newClips.add(
                        Clip(
                            id = UUID.randomUUID().toString(),
                            file_path = localPath,
                            source_start_ms = 0L,
                            source_end_ms = realDurationMs,
                            timeline_start_ms = timelineOffset
                        )
                    )
                    timelineOffset += realDurationMs
                }

                val updatedTracks = composition.tracks.map { track ->
                    if (track.track_type == TrackType.Video) {
                        track.copy(clips = track.clips + newClips)
                    } else {
                        track
                    }
                }
                updateCompositionWithUndo(composition.copy(tracks = updatedTracks))
                selectedClipId = newClips.firstOrNull()?.id
                haptics.split()
                Toast.makeText(context, "Imported ${newClips.size} clips!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Audio / Song Picker
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val (localPath, realDurationMs) = FileUtils.resolveMediaUriToLocalFile(context, it)
                val audioClip = Clip(
                    id = UUID.randomUUID().toString(),
                    file_path = localPath,
                    source_start_ms = 0L,
                    source_end_ms = realDurationMs,
                    timeline_start_ms = currentPositionMs
                )
                val updatedTracks = composition.tracks.map { track ->
                    if (track.track_type == TrackType.Audio) {
                        track.copy(clips = track.clips + audioClip)
                    } else track
                }
                updateCompositionWithUndo(composition.copy(tracks = updatedTracks))
                haptics.split()
                showAudioSheet = false
                Toast.makeText(context, "Audio track added!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Subtitles SRT Picker
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

    // Auto-save project every 2 minutes for heavy editing security
    LaunchedEffect(composition) {
        while (true) {
            delay(120_000L)
            onSaveProject(project.copy(composition = composition, timeSpentSeconds = projectTimeSpentSeconds))
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
                            contentDescription = strings.back,
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
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${project.aspectRatio} • Rust Vulkan Engine",
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
                            contentDescription = strings.undo,
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
                            contentDescription = strings.redo,
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
                            contentDescription = strings.plugins,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        bottomBar = {
            // Adaptive Bottom Action Toolbar (Adapts dynamically to CapCut, Alight Motion, or Standard layout)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (activeUiLayout) {
                        UiLayoutMode.CAPCUT -> {
                            // CapCut Pro Streamlined Tool Ribbon
                            BottomToolbarItem(
                                icon = Icons.Default.CallSplit,
                                label = "Split",
                                onClick = {
                                    haptics.split()
                                    val target = selectedClip ?: composition.tracks.flatMap { it.clips }.find { currentPositionMs in it.timeline_start_ms until (it.timeline_start_ms + it.durationMs) }
                                    target?.let { clip ->
                                        val offset = currentPositionMs - clip.timeline_start_ms
                                        val srcOffset = (offset * clip.speed).toLong()
                                        if (offset in 50 until clip.durationMs) {
                                            val c1 = clip.copy(id = UUID.randomUUID().toString(), source_end_ms = clip.source_start_ms + srcOffset)
                                            val c2 = clip.copy(id = UUID.randomUUID().toString(), source_start_ms = clip.source_start_ms + srcOffset, timeline_start_ms = currentPositionMs)
                                            val trks = composition.tracks.map { trk -> trk.copy(clips = trk.clips.flatMap { if (it.id == clip.id) listOf(c1, c2) else listOf(it) }) }
                                            updateCompositionWithUndo(composition.copy(tracks = trks))
                                            selectedClipId = c2.id
                                        }
                                    }
                                }
                            )
                            BottomToolbarItem(
                                icon = Icons.Default.LibraryMusic,
                                label = "Audio / SFX",
                                onClick = { haptics.tick(); showAudioSheet = true }
                            )
                            BottomToolbarItem(
                                icon = Icons.Default.Subtitles,
                                label = "Text & Subs",
                                onClick = { haptics.tick(); showSubtitleSheet = true }
                            )
                            BottomToolbarItem(
                                icon = Icons.Default.Image,
                                label = "Overlay / PIP",
                                onClick = { haptics.tick(); imagePickerLauncher.launch("image/*") }
                            )
                            BottomToolbarItem(
                                icon = Icons.Default.AutoFixHigh,
                                label = "Effects",
                                onClick = {
                                    haptics.tick()
                                    if (selectedClip != null) showEffectsSheet = true else Toast.makeText(context, "Select clip first", Toast.LENGTH_SHORT).show()
                                }
                            )
                            BottomToolbarItem(
                                icon = Icons.Default.Animation,
                                label = "Transitions",
                                onClick = {
                                    haptics.tick()
                                    if (selectedClip != null) showTransitionsSheet = true else Toast.makeText(context, "Select clip first", Toast.LENGTH_SHORT).show()
                                }
                            )
                            BottomToolbarItem(
                                icon = Icons.Default.Speed,
                                label = "Curve Speed",
                                onClick = {
                                    haptics.tick()
                                    if (selectedClip != null) showEffectsSheet = true else Toast.makeText(context, "Select clip first", Toast.LENGTH_SHORT).show()
                                }
                            )
                            BottomToolbarItem(
                                icon = Icons.Default.Extension,
                                label = "Layout / UI",
                                onClick = { haptics.tick(); showPluginManagerSheet = true }
                            )
                            BottomToolbarItem(
                                icon = Icons.Default.FileUpload,
                                label = strings.export,
                                isHighlight = true,
                                onClick = { haptics.snap(); showExportDialog = true }
                            )
                        }
                        UiLayoutMode.ALIGHT_MOTION -> {
                            // Alight Motion Layer & Motion Design Ribbon
                            BottomToolbarItem(
                                icon = Icons.Default.VideoCall,
                                label = "Add Layer",
                                onClick = { haptics.tick(); showMediaImportSheet = true }
                            )
                            BottomToolbarItem(
                                icon = Icons.Default.Diamond,
                                label = "Keyframe",
                                onClick = {
                                    val target = selectedClip ?: composition.tracks.flatMap { it.clips }.find { currentPositionMs in it.timeline_start_ms until (it.timeline_start_ms + it.durationMs) }
                                    target?.let { clip ->
                                        val offset = (currentPositionMs - clip.timeline_start_ms).coerceIn(0L, clip.durationMs)
                                        val hasKf = clip.keyframes.any { Math.abs(it.timeMs - offset) < 150L }
                                        val newKfs = if (hasKf) clip.keyframes.filterNot { Math.abs(it.timeMs - offset) < 150L } else clip.keyframes + Keyframe(timeMs = offset)
                                        val trks = composition.tracks.map { trk -> trk.copy(clips = trk.clips.map { if (it.id == clip.id) it.copy(keyframes = newKfs) else it }) }
                                        updateCompositionWithUndo(composition.copy(tracks = trks))
                                        haptics.snap()
                                    }
                                }
                            )
                            BottomToolbarItem(
                                icon = Icons.Default.AutoGraph,
                                label = "Easing Graph",
                                onClick = {
                                    haptics.tick()
                                    if (selectedClip != null) showEffectsSheet = true else Toast.makeText(context, "Select clip first", Toast.LENGTH_SHORT).show()
                                }
                            )
                            BottomToolbarItem(
                                icon = Icons.Default.AutoAwesome,
                                label = "Animations",
                                onClick = {
                                    haptics.tick()
                                    if (selectedClip != null) showAnimationsSheet = true else Toast.makeText(context, "Select clip first", Toast.LENGTH_SHORT).show()
                                }
                            )
                            BottomToolbarItem(
                                icon = Icons.Default.LibraryMusic,
                                label = "Audio / SFX",
                                onClick = { haptics.tick(); showAudioSheet = true }
                            )
                            BottomToolbarItem(
                                icon = Icons.Default.Extension,
                                label = "Plugins",
                                onClick = { haptics.tick(); showPluginManagerSheet = true }
                            )
                            BottomToolbarItem(
                                icon = Icons.Default.FileUpload,
                                label = strings.export,
                                isHighlight = true,
                                onClick = { haptics.snap(); showExportDialog = true }
                            )
                        }
                        UiLayoutMode.STANDARD -> {
                            // Standard LibreCut RS Categorized Bottom Action Toolbar
                            BottomToolbarItem(
                                icon = Icons.Default.VideoCall,
                                label = strings.addClips,
                                onClick = {
                                    haptics.tick()
                                    showMediaImportSheet = true
                                }
                            )
                            BottomToolbarItem(
                                icon = Icons.Default.LibraryMusic,
                                label = strings.addAudio,
                                onClick = {
                                    haptics.tick()
                                    showAudioSheet = true
                                }
                            )
                            BottomToolbarItem(
                                icon = Icons.Default.Subtitles,
                                label = strings.addSubtitles,
                                onClick = {
                                    haptics.tick()
                                    showSubtitleSheet = true
                                }
                            )
                            BottomToolbarItem(
                                icon = Icons.Default.Animation,
                                label = strings.transitions,
                                onClick = {
                                    haptics.tick()
                                    if (selectedClip != null) {
                                        showTransitionsSheet = true
                                    } else {
                                        Toast.makeText(context, "Select a clip first to apply transitions", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            BottomToolbarItem(
                                icon = Icons.Default.AutoAwesome,
                                label = strings.animations,
                                onClick = {
                                    haptics.tick()
                                    if (selectedClip != null) {
                                        showAnimationsSheet = true
                                    } else {
                                        Toast.makeText(context, "Select a clip first to apply animations", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            BottomToolbarItem(
                                icon = Icons.Default.AutoFixHigh,
                                label = strings.effects,
                                onClick = {
                                    haptics.tick()
                                    if (selectedClip != null) {
                                        showEffectsSheet = true
                                    } else {
                                        Toast.makeText(context, "Select a clip first to adjust", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            BottomToolbarItem(
                                icon = Icons.Default.Extension,
                                label = strings.plugins,
                                onClick = {
                                    haptics.tick()
                                    showPluginManagerSheet = true
                                }
                            )
                            BottomToolbarItem(
                                icon = Icons.Default.Compress,
                                label = strings.compress,
                                onClick = {
                                    haptics.tick()
                                    showCompressDialog = true
                                }
                            )
                            BottomToolbarItem(
                                icon = Icons.Default.FileUpload,
                                label = strings.export,
                                isHighlight = true,
                                onClick = {
                                    haptics.snap()
                                    showExportDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Dynamic clip under playhead resolution so adjustments and cuts always work
            val clipAtPlayhead = selectedClip ?: composition.tracks
                .flatMap { it.clips }
                .find { currentPositionMs in it.timeline_start_ms until (it.timeline_start_ms + it.durationMs) }
                ?: composition.tracks
                    .find { it.track_type == TrackType.Video }
                    ?.clips
                    ?.firstOrNull()

            val currentMediaUri = clipAtPlayhead?.file_path

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.0f)
            ) {
                val audioClips = composition.tracks
                    .filter { it.track_type == TrackType.Audio }
                    .flatMap { it.clips }

                VideoPreviewPlayer(
                    videoUri = currentMediaUri,
                    activeClip = clipAtPlayhead,
                    audioClips = audioClips,
                    isPlaying = isPlaying,
                    currentPositionMs = currentPositionMs,
                    onPositionChanged = { currentPositionMs = it },
                    onPlaybackEnded = { isPlaying = false },
                    onKeyframeUpdated = { clipId, keyframe ->
                        val updatedTracks = composition.tracks.map { track ->
                            track.copy(clips = track.clips.map { c ->
                                if (c.id == clipId) {
                                    val filtered = c.keyframes.filterNot { Math.abs(it.timeMs - keyframe.timeMs) < 120L }
                                    c.copy(keyframes = (filtered + keyframe).sortedBy { it.timeMs })
                                } else c
                            })
                        }
                        updateCompositionWithUndo(composition.copy(tracks = updatedTracks))
                    },
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
                                text = "${strings.recordingVoiceover}: ${recordElapsedSeconds}s",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // Render Active Overlay Clips (Photos / Stickers / Graphics spawned smaller than video)
                val activeOverlayClip = composition.tracks
                    .filter { it.track_type == TrackType.Overlay }
                    .flatMap { it.clips }
                    .firstOrNull { clip ->
                        currentPositionMs in clip.timeline_start_ms..(clip.timeline_start_ms + clip.durationMs)
                    }

                activeOverlayClip?.let { ovClip ->
                    val bgRemoval = ovClip.effects.filterIsInstance<EffectConfig.BackgroundRemoval>().firstOrNull()
                    var ovBitmap by remember(ovClip.file_path, bgRemoval) { mutableStateOf<Bitmap?>(null) }

                    LaunchedEffect(ovClip.file_path, bgRemoval) {
                        withContext(Dispatchers.Default) {
                            val raw = ThumbnailCache.getThumbnail(ovClip.file_path, 800, 800)
                            if (raw != null && bgRemoval != null) {
                                ovBitmap = ThumbnailCache.removeBackgroundFromBitmap(raw, bgRemoval.threshold, bgRemoval.invert)
                            } else {
                                ovBitmap = raw
                            }
                        }
                    }

                    val clipOffsetMs = (currentPositionMs - ovClip.timeline_start_ms).coerceAtLeast(0L)
                    val sortedKfs = ovClip.keyframes.sortedBy { it.timeMs }
                    val currentKf = if (sortedKfs.isEmpty()) {
                        Keyframe(timeMs = 0L, scale = 0.55f, x = 0f, y = 0f)
                    } else if (clipOffsetMs <= sortedKfs.first().timeMs) {
                        sortedKfs.first()
                    } else if (clipOffsetMs >= sortedKfs.last().timeMs) {
                        sortedKfs.last()
                    } else {
                        val k1 = sortedKfs.last { it.timeMs <= clipOffsetMs }
                        val k2 = sortedKfs.first { it.timeMs > clipOffsetMs }
                        val factor = if (k2.timeMs == k1.timeMs) 0f else (clipOffsetMs - k1.timeMs).toFloat() / (k2.timeMs - k1.timeMs)
                        Keyframe(
                            timeMs = clipOffsetMs,
                            x = k1.x + (k2.x - k1.x) * factor,
                            y = k1.y + (k2.y - k1.y) * factor,
                            scale = k1.scale + (k2.scale - k1.scale) * factor,
                            rotation = k1.rotation + (k2.rotation - k1.rotation) * factor,
                            opacity = k1.opacity + (k2.opacity - k1.opacity) * factor
                        )
                    }

                    var ovPanX by remember(ovClip.id) { mutableFloatStateOf(0f) }
                    var ovPanY by remember(ovClip.id) { mutableFloatStateOf(0f) }
                    var ovScale by remember(ovClip.id) { mutableFloatStateOf(1f) }
                    var ovRot by remember(ovClip.id) { mutableFloatStateOf(0f) }

                    val ovRemainingMs = (((ovClip.timeline_start_ms) + ovClip.durationMs) - currentPositionMs).coerceAtLeast(0L)
                    val animDur = 600L
                    val ovProgIn = (clipOffsetMs.toFloat() / animDur).coerceIn(0f, 1f)
                    val ovProgOut = (ovRemainingMs.toFloat() / animDur).coerceIn(0f, 1f)

                    var ovAnimScale = 1.0f
                    var ovAnimAlpha = 1.0f
                    var ovAnimRot = 0.0f
                    var ovAnimSlideX = 0.0f

                    when (ovClip.animation) {
                        AnimationType.FadeIn -> ovAnimAlpha = ovProgIn
                        AnimationType.FadeOut -> ovAnimAlpha = ovProgOut
                        AnimationType.ZoomIn -> ovAnimScale = 0.35f + 0.65f * ovProgIn
                        AnimationType.ZoomOut -> ovAnimScale = 0.35f + 0.65f * ovProgOut
                        AnimationType.BounceIn -> {
                            val p = ovProgIn
                            ovAnimScale = if (p < 0.65f) 0.3f + 0.9f * (p / 0.65f) else 1.2f - 0.2f * ((p - 0.65f) / 0.35f)
                        }
                        AnimationType.SpinIn -> {
                            ovAnimRot = (1f - ovProgIn) * 360f
                            ovAnimScale = 0.3f + 0.7f * ovProgIn
                        }
                        AnimationType.SpinOut -> {
                            ovAnimRot = (1f - ovProgOut) * -360f
                            ovAnimScale = 0.3f + 0.7f * ovProgOut
                        }
                        AnimationType.SlideInLeft -> ovAnimSlideX = (1f - ovProgIn) * -600f
                        AnimationType.SlideInRight -> ovAnimSlideX = (1f - ovProgIn) * 600f
                        AnimationType.SlideOutLeft -> ovAnimSlideX = (1f - ovProgOut) * -600f
                        AnimationType.SlideOutRight -> ovAnimSlideX = (1f - ovProgOut) * 600f
                        AnimationType.Pulse -> ovAnimScale = 1.0f + 0.04f * kotlin.math.sin(clipOffsetMs / 120.0).toFloat()
                        AnimationType.Shake -> ovAnimSlideX = kotlin.math.sin(clipOffsetMs / 35.0).toFloat() * 25f
                        AnimationType.Glitch -> ovAnimSlideX = if ((clipOffsetMs / 60) % 2 == 0L) 18f else -18f
                        AnimationType.Pendulum -> ovAnimRot = kotlin.math.sin(clipOffsetMs / 180.0).toFloat() * 18f
                        AnimationType.Flash -> ovAnimAlpha = if ((clipOffsetMs / 90) % 2 == 0L) 1.0f else 0.15f
                        AnimationType.Swing -> {
                            ovAnimRot = kotlin.math.sin(clipOffsetMs / 250.0).toFloat() * 12f
                            ovAnimScale = 1.0f + 0.05f * kotlin.math.sin(clipOffsetMs / 250.0).toFloat()
                        }
                        else -> {}
                    }

                    ovBitmap?.let { bmp ->
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .pointerInput(ovClip.id, clipOffsetMs) {
                                    detectTransformGestures { _, pan, zoom, rotation ->
                                        ovPanX += pan.x
                                        ovPanY += pan.y
                                        ovScale = (ovScale * zoom).coerceIn(0.15f, 4.0f)
                                        ovRot += rotation

                                        // Auto-create/update keyframe at current playhead (CapCut style)
                                        val updatedKf = Keyframe(
                                            timeMs = clipOffsetMs,
                                            x = currentKf.x + ovPanX,
                                            y = currentKf.y + ovPanY,
                                            scale = (currentKf.scale * ovScale).coerceIn(0.1f, 4f),
                                            rotation = currentKf.rotation + ovRot,
                                            opacity = currentKf.opacity
                                        )
                                        val updatedKfs = (ovClip.keyframes.filterNot { Math.abs(it.timeMs - clipOffsetMs) < 120L } + updatedKf).sortedBy { it.timeMs }
                                        val newTracks = composition.tracks.map { trk ->
                                            if (trk.track_type == TrackType.Overlay) {
                                                trk.copy(clips = trk.clips.map { if (it.id == ovClip.id) it.copy(keyframes = updatedKfs) else it })
                                            } else trk
                                        }
                                        updateCompositionWithUndo(composition.copy(tracks = newTracks))
                                    }
                                }
                                .graphicsLayer {
                                    this.translationX = currentKf.x + ovPanX + ovAnimSlideX
                                    this.translationY = currentKf.y + ovPanY
                                    val totalScale = currentKf.scale * ovScale * ovAnimScale
                                    this.scaleX = totalScale
                                    this.scaleY = totalScale
                                    this.rotationZ = currentKf.rotation + ovRot + ovAnimRot
                                    this.alpha = (currentKf.opacity * ovAnimAlpha).coerceIn(0f, 1f)
                                }
                        ) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Image Overlay",
                                modifier = Modifier
                                    .size(240.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }
                    }
                }

                // Render Active Text Overlays with Movable & Resizable Gestures and Click-to-Edit
                val activeTextClip = composition.tracks
                    .flatMap { it.clips }
                    .firstOrNull { clip ->
                        currentPositionMs in clip.timeline_start_ms..(clip.timeline_start_ms + clip.durationMs) &&
                                clip.effects.any { it is EffectConfig.TextOverlay }
                    }

                val activeTextOverlay = activeTextClip?.effects
                    ?.filterIsInstance<EffectConfig.TextOverlay>()
                    ?.firstOrNull()

                activeTextOverlay?.let { textEff ->
                    val customFontFamily = remember(textEff.custom_font_path) {
                        fontManager.getFontFamily(textEff.custom_font_path)
                    }

                    var textOffsetX by remember(activeTextClip.id) { mutableFloatStateOf(0f) }
                    var textOffsetY by remember(activeTextClip.id) { mutableFloatStateOf(0f) }
                    var textScale by remember(activeTextClip.id) { mutableFloatStateOf(1f) }

                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset { androidx.compose.ui.unit.IntOffset(textOffsetX.toInt(), textOffsetY.toInt()) }
                            .pointerInput(activeTextClip.id) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    textOffsetX += pan.x
                                    textOffsetY += pan.y
                                    textScale = (textScale * zoom).coerceIn(0.5f, 3.5f)
                                }
                            }
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.72f))
                            .clickable {
                                textToEdit = textEff.text
                                editingTextClipId = activeTextClip.id
                                showTextEditorDialog = true
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = textEff.text,
                            fontFamily = customFontFamily,
                            fontSize = (textEff.font_size * textScale).sp,
                            color = Color(textEff.color_rgba),
                            textAlign = TextAlign.Center,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // CapCut Pro Layout: Floating Quick Action Pill above timeline
            if (activeUiLayout == UiLayoutMode.CAPCUT) {
                val targetForPill = selectedClip ?: composition.tracks
                    .flatMap { it.clips }
                    .find { currentPositionMs in it.timeline_start_ms until (it.timeline_start_ms + it.durationMs) }

                if (targetForPill != null) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = {
                                val splitTimelineOffset = currentPositionMs - targetForPill.timeline_start_ms
                                val splitSourceOffset = (splitTimelineOffset * targetForPill.speed).toLong()
                                if (splitTimelineOffset in 50 until targetForPill.durationMs) {
                                    haptics.split()
                                    val firstHalf = targetForPill.copy(id = UUID.randomUUID().toString(), source_end_ms = targetForPill.source_start_ms + splitSourceOffset)
                                    val secondHalf = targetForPill.copy(id = UUID.randomUUID().toString(), source_start_ms = targetForPill.source_start_ms + splitSourceOffset, timeline_start_ms = currentPositionMs)
                                    val updatedTracks = composition.tracks.map { track ->
                                        track.copy(clips = track.clips.flatMap { if (it.id == targetForPill.id) listOf(firstHalf, secondHalf) else listOf(it) })
                                    }
                                    updateCompositionWithUndo(composition.copy(tracks = updatedTracks))
                                    selectedClipId = secondHalf.id
                                }
                            }) {
                                Icon(Icons.Default.CallSplit, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(3.dp))
                                Text(strings.split, fontSize = 11.sp)
                            }
                            TextButton(onClick = { showEffectsSheet = true }) {
                                Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(3.dp))
                                Text(strings.speed, fontSize = 11.sp)
                            }
                            TextButton(onClick = {
                                val offsetMs = (currentPositionMs - targetForPill.timeline_start_ms).coerceIn(0L, targetForPill.durationMs)
                                val hasKeyframe = targetForPill.keyframes.any { Math.abs(it.timeMs - offsetMs) < 150L }
                                val updatedKeyframes = if (hasKeyframe) {
                                    targetForPill.keyframes.filterNot { Math.abs(it.timeMs - offsetMs) < 150L }
                                } else {
                                    targetForPill.keyframes + Keyframe(timeMs = offsetMs)
                                }
                                val updatedTracks = composition.tracks.map { track ->
                                    track.copy(clips = track.clips.map { if (it.id == targetForPill.id) it.copy(keyframes = updatedKeyframes) else it })
                                }
                                haptics.snap()
                                updateCompositionWithUndo(composition.copy(tracks = updatedTracks))
                            }) {
                                Icon(Icons.Default.Diamond, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(3.dp))
                                Text(strings.keyframe, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Timeline Action Controls Toolbar & High-Precision Timestamp Navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Timestamp Navigation Controls (Play/Pause, Cut Jumps, Frame Stepping, Timecode Badge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        haptics.tick()
                        isPlaying = !isPlaying
                    }) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) strings.pause else strings.play,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Jump to Previous Cut (|<<)
                    IconButton(
                        onClick = {
                            val prevCut = cutPoints.filter { it < currentPositionMs - 50L }.maxOrNull() ?: 0L
                            currentPositionMs = prevCut
                            haptics.snap()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = strings.previousCut,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Step 1 Frame Backwards (-33ms)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        modifier = Modifier
                            .clickable {
                                haptics.tick()
                                currentPositionMs = (currentPositionMs - 33L).coerceAtLeast(0L)
                            }
                    ) {
                        Text(
                            text = "< 1f",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(3.dp))

                    // Formatted Timecode Pill (Click to open Jump to Timestamp Dialog)
                    val curMin = currentPositionMs / 60000
                    val curSec = (currentPositionMs % 60000) / 1000
                    val curMs = currentPositionMs % 1000
                    val totMin = composition.totalDurationMs / 60000
                    val totSec = (composition.totalDurationMs % 60000) / 1000
                    val timecodeLabel = String.format("%02d:%02d.%03d / %02d:%02d", curMin, curSec, curMs, totMin, totSec)

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
                        modifier = Modifier
                            .clickable {
                                haptics.tick()
                                showJumpTimestampDialog = true
                            }
                    ) {
                        Text(
                            text = timecodeLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(3.dp))

                    // Step 1 Frame Forward (+33ms)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        modifier = Modifier
                            .clickable {
                                haptics.tick()
                                currentPositionMs = (currentPositionMs + 33L).coerceAtMost(composition.totalDurationMs)
                            }
                    ) {
                        Text(
                            text = "1f >",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp)
                        )
                    }

                    // Jump to Next Cut (>>|)
                    IconButton(
                        onClick = {
                            val nextCut = cutPoints.filter { it > currentPositionMs + 50L }.minOrNull() ?: composition.totalDurationMs
                            currentPositionMs = nextCut
                            haptics.snap()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = strings.nextCut,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Auto-detect clip under playhead if none explicitly tapped
                val activeTargetClip = selectedClip ?: composition.tracks
                    .flatMap { it.clips }
                    .find { currentPositionMs in it.timeline_start_ms until (it.timeline_start_ms + it.durationMs) }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Split at Playhead (Cut clip in two)
                    IconButton(
                        onClick = {
                            activeTargetClip?.let { clip ->
                                val splitTimelineOffset = currentPositionMs - clip.timeline_start_ms
                                val splitSourceOffset = (splitTimelineOffset * clip.speed).toLong()
                                if (splitTimelineOffset in 50 until clip.durationMs) {
                                    haptics.split()
                                    val firstHalf = clip.copy(
                                        id = UUID.randomUUID().toString(),
                                        source_end_ms = clip.source_start_ms + splitSourceOffset
                                    )
                                    val secondHalf = clip.copy(
                                        id = UUID.randomUUID().toString(),
                                        source_start_ms = clip.source_start_ms + splitSourceOffset,
                                        timeline_start_ms = currentPositionMs
                                    )

                                    val updatedTracks = composition.tracks.map { track ->
                                        track.copy(clips = track.clips.flatMap {
                                            if (it.id == clip.id) listOf(firstHalf, secondHalf) else listOf(it)
                                        })
                                    }
                                    updateCompositionWithUndo(composition.copy(tracks = updatedTracks))
                                    selectedClipId = secondHalf.id
                                    Toast.makeText(context, "Cut clip at ${(currentPositionMs / 1000f).let { String.format(java.util.Locale.US, "%.1fs", it) }}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = activeTargetClip != null
                    ) {
                        Icon(
                            Icons.Default.CallSplit,
                            contentDescription = strings.split,
                            tint = if (activeTargetClip != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }

                    // Trim Start (Cut Left - trim off beginning of clip)
                    IconButton(
                        onClick = {
                            activeTargetClip?.let { clip ->
                                val splitTimelineOffset = currentPositionMs - clip.timeline_start_ms
                                val splitSourceOffset = (splitTimelineOffset * clip.speed).toLong()
                                if (splitTimelineOffset in 50 until clip.durationMs) {
                                    haptics.split()
                                    val trimmed = clip.copy(
                                        source_start_ms = clip.source_start_ms + splitSourceOffset,
                                        timeline_start_ms = currentPositionMs
                                    )
                                    val updatedTracks = composition.tracks.map { track ->
                                        track.copy(clips = track.clips.map {
                                            if (it.id == clip.id) trimmed else it
                                        })
                                    }
                                    updateCompositionWithUndo(composition.copy(tracks = updatedTracks))
                                    selectedClipId = trimmed.id
                                    Toast.makeText(context, "Trimmed start of clip", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = activeTargetClip != null && (currentPositionMs > (activeTargetClip.timeline_start_ms + 50))
                    ) {
                        Icon(
                            Icons.Default.ContentCut,
                            contentDescription = "Cut Start",
                            tint = if (activeTargetClip != null && (currentPositionMs > (activeTargetClip.timeline_start_ms + 50))) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
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
                            contentDescription = strings.keyframe,
                            tint = if (selectedClip != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }

                    // Voiceover Mic Button
                    IconButton(
                        onClick = {
                            if (isRecordingVoiceover) {
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
                                    Toast.makeText(context, "Voiceover added!", Toast.LENGTH_SHORT).show()
                                }
                            } else {
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
                            contentDescription = strings.voiceover,
                            tint = if (isRecordingVoiceover) Color.Red else MaterialTheme.colorScheme.primary
                        )
                    }

                    // Effects / Speed / Transitions / Crop Sheet
                    IconButton(
                        onClick = {
                            haptics.tick()
                            showEffectsSheet = true
                        },
                        enabled = selectedClip != null
                    ) {
                        Icon(
                            Icons.Default.AutoFixHigh,
                            contentDescription = strings.effects,
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
                            contentDescription = strings.delete,
                            tint = if (selectedClip != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
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

    // Effects & Animations Bottom Sheet (Includes Speed Curves, Transitions, Crop, Masks, Custom Fonts)
    if (showEffectsSheet && selectedClip != null) {
        ModalBottomSheet(
            onDismissRequest = { showEffectsSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            EffectsSheet(
                clip = selectedClip,
                onUpdateClip = { newEffects, newSpeed, newSpeedCurve, newVolume, newTransition, newAnimation ->
                    val updatedTracks = composition.tracks.map { track ->
                        track.copy(clips = track.clips.map {
                            if (it.id == selectedClip.id) {
                                it.copy(
                                    effects = newEffects,
                                    speed = newSpeed,
                                    speedCurve = newSpeedCurve,
                                    volume = newVolume,
                                    transition = newTransition,
                                    animation = newAnimation
                                )
                            } else it
                        })
                    }
                    updateCompositionWithUndo(composition.copy(tracks = updatedTracks))
                }
            )
        }
    }

    // Subtitles & Auto Captions Bottom Sheet (CapCut style automatic speech-to-subtitles)
    if (showSubtitleSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSubtitleSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            SubtitleImportSheet(
                onOpenTts = {
                    showSubtitleSheet = false
                    ttsInitialText = "LibreCut RS"
                    showTtsDialog = true
                },
                onAutoCaptions = {
                    showSubtitleSheet = false
                    scope.launch {
                        val generatedClips = autoCaptionHelper.generateCaptionsForComposition(composition)
                        if (generatedClips.isNotEmpty()) {
                            val updatedTracks = composition.tracks.map { track ->
                                if (track.track_type == TrackType.Text) {
                                    track.copy(clips = track.clips + generatedClips)
                                } else track
                            }
                            updateCompositionWithUndo(composition.copy(tracks = updatedTracks))
                            haptics.split()
                            Toast.makeText(context, "Generated ${generatedClips.size} auto-captions!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "No spoken audio found to transcribe", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onImportSrt = {
                    showSubtitleSheet = false
                    srtPickerLauncher.launch("*/*")
                },
                onCustomScript = {
                    showSubtitleSheet = false
                    showCustomScriptDialog = true
                },
                onAddBlankText = {
                    showSubtitleSheet = false
                    val textClip = Clip(
                        id = UUID.randomUUID().toString(),
                        file_path = "text://${UUID.randomUUID()}",
                        source_start_ms = 0L,
                        source_end_ms = 3000L,
                        timeline_start_ms = currentPositionMs,
                        effects = listOf(
                            EffectConfig.TextOverlay(
                                text = "LibreCut RS",
                                font_size = 24f,
                                x = 0.5f,
                                y = 0.8f
                            )
                        )
                    )
                    val updatedTracks = composition.tracks.map { track ->
                        if (track.track_type == TrackType.Text) {
                            track.copy(clips = track.clips + textClip)
                        } else track
                    }
                    updateCompositionWithUndo(composition.copy(tracks = updatedTracks))
                    haptics.snap()
                    Toast.makeText(context, "Text clip added!", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    // Audio & Sound FX Sheet
    if (showAudioSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAudioSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            AudioImportSheet(
                onImportLocalFile = {
                    haptics.tick()
                    audioPickerLauncher.launch("audio/*")
                },
                onSelectEffect = { item ->
                    haptics.snap()
                    val audioClip = Clip(
                        id = UUID.randomUUID().toString(),
                        file_path = item.localFilePath,
                        source_start_ms = 0L,
                        source_end_ms = (item.durationSec * 1000).toLong(),
                        timeline_start_ms = currentPositionMs
                    )
                    val updatedTracks = composition.tracks.map { track ->
                        if (track.track_type == TrackType.Audio) track.copy(clips = track.clips + audioClip) else track
                    }
                    updateCompositionWithUndo(composition.copy(tracks = updatedTracks))
                    showAudioSheet = false
                    Toast.makeText(context, "${item.name} added!", Toast.LENGTH_SHORT).show()
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

    // Compress Dialog
    if (showCompressDialog) {
        CompressDialog(
            composition = composition,
            exportState = exportState,
            onStartCompress = { res, bitrate ->
                scope.launch {
                    exportManager.exportComposition(
                        composition = composition,
                        resolution = res.toDimensions(),
                        fps = 30,
                        bitrateMbps = bitrate,
                        smartRender = false
                    )
                }
            },
            onCancel = { exportManager.cancel() },
            onDismiss = { showCompressDialog = false }
        )
    }

    // Export Dialog
    if (showExportDialog) {
        ExportDialog(
            totalDurationMs = composition.totalDurationMs,
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

    // Media Import Bottom Sheet (Videos & Images)
    if (showMediaImportSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMediaImportSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            MediaImportSheet(
                onImportVideos = {
                    showMediaImportSheet = false
                    multipleVideoPickerLauncher.launch("video/*")
                },
                onImportImages = {
                    showMediaImportSheet = false
                    imagePickerLauncher.launch("image/*")
                }
            )
        }
    }

    // Direct Transitions Bottom Sheet
    if (showTransitionsSheet && selectedClip != null) {
        ModalBottomSheet(
            onDismissRequest = { showTransitionsSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            DirectTransitionsSheet(
                selectedTransition = selectedClip.transition,
                transitionDurationMs = selectedClip.transitionDurationMs,
                onSelectTransition = { newTransition, newDuration ->
                    val updatedTracks = composition.tracks.map { track ->
                        track.copy(clips = track.clips.map {
                            if (it.id == selectedClip.id) {
                                it.copy(transition = newTransition, transitionDurationMs = newDuration)
                            } else it
                        })
                    }
                    updateCompositionWithUndo(composition.copy(tracks = updatedTracks))
                    showTransitionsSheet = false
                    Toast.makeText(context, "Transition applied: ${newTransition.displayName}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    // Direct Animations Bottom Sheet
    if (showAnimationsSheet && selectedClip != null) {
        ModalBottomSheet(
            onDismissRequest = { showAnimationsSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            DirectAnimationsSheet(
                selectedAnimation = selectedClip.animation,
                onSelectAnimation = { newAnimation ->
                    val updatedTracks = composition.tracks.map { track ->
                        track.copy(clips = track.clips.map {
                            if (it.id == selectedClip.id) {
                                it.copy(animation = newAnimation)
                            } else it
                        })
                    }
                    updateCompositionWithUndo(composition.copy(tracks = updatedTracks))
                    showAnimationsSheet = false
                    Toast.makeText(context, "Animation applied: ${newAnimation.label}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    // Direct Subtitle / Text Editor Dialog
    if (showTextEditorDialog && editingTextClipId != null) {
        AlertDialog(
            onDismissRequest = { showTextEditorDialog = false },
            title = { Text(strings.editSubtitleTitle, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = textToEdit,
                        onValueChange = { textToEdit = it },
                        label = { Text(strings.subtitleTextLabel) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            ttsInitialText = textToEdit
                            showTtsDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.generateTtsForText, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val updatedTracks = composition.tracks.map { track ->
                        track.copy(clips = track.clips.map { clip ->
                            if (clip.id == editingTextClipId) {
                                val updatedEffects = clip.effects.map { eff ->
                                    if (eff is EffectConfig.TextOverlay) {
                                        eff.copy(text = textToEdit)
                                    } else eff
                                }
                                clip.copy(effects = updatedEffects)
                            } else clip
                        })
                    }
                    updateCompositionWithUndo(composition.copy(tracks = updatedTracks))
                    showTextEditorDialog = false
                    Toast.makeText(context, "Subtitle updated!", Toast.LENGTH_SHORT).show()
                }) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showTextEditorDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Custom Script / Lyrics to Subtitles Dialog
    if (showCustomScriptDialog) {
        AlertDialog(
            onDismissRequest = { showCustomScriptDialog = false },
            title = { Text(strings.pasteScriptTitle, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        strings.typePasteScriptDesc,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customScriptText,
                        onValueChange = { customScriptText = it },
                        placeholder = { Text(strings.pasteScriptPlaceholder) },
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        maxLines = 8
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val lines = customScriptText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                    if (lines.isNotEmpty()) {
                        var offset = currentPositionMs
                        val durationPerLine = 2500L
                        val newClips = lines.map { line ->
                            val c = Clip(
                                id = UUID.randomUUID().toString(),
                                file_path = "text://${UUID.randomUUID()}",
                                source_start_ms = 0L,
                                source_end_ms = durationPerLine,
                                timeline_start_ms = offset,
                                effects = listOf(
                                    EffectConfig.TextOverlay(
                                        text = line,
                                        font_size = 22f,
                                        x = 0.5f,
                                        y = 0.82f,
                                        color_rgba = 0xFFFFFFFFL
                                    )
                                )
                            )
                            offset += durationPerLine
                            c
                        }
                        val updatedTracks = composition.tracks.map { track ->
                            if (track.track_type == TrackType.Text) {
                                track.copy(clips = track.clips + newClips)
                            } else track
                        }
                        updateCompositionWithUndo(composition.copy(tracks = updatedTracks))
                        showCustomScriptDialog = false
                        Toast.makeText(context, "Added ${newClips.size} subtitle lines!", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text(strings.generateSubtitlesBtn)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCustomScriptDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Native Android Text-to-Speech (TTS) Voiceover Dialog (0 Byte)
    if (showTtsDialog) {
        TtsVoiceoverDialog(
            initialText = ttsInitialText,
            ttsHelper = ttsHelper,
            onAddVoiceoverClip = { newClip ->
                val alignedClip = newClip.copy(timeline_start_ms = currentPositionMs)
                val updatedTracks = composition.tracks.map { track ->
                    if (track.track_type == TrackType.Audio) {
                        track.copy(clips = track.clips + alignedClip)
                    } else track
                }
                updateCompositionWithUndo(composition.copy(tracks = updatedTracks))
            },
            onDismiss = { showTtsDialog = false }
        )
    }

    // Jump to Timestamp Dialog
    if (showJumpTimestampDialog) {
        JumpToTimestampDialog(
            currentMs = currentPositionMs,
            totalMs = composition.totalDurationMs,
            onJump = { currentPositionMs = it },
            onDismiss = { showJumpTimestampDialog = false }
        )
    }
}

@Composable
private fun JumpToTimestampDialog(
    currentMs: Long,
    totalMs: Long,
    onJump: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    val strings = LocalAppStrings.current
    val haptics = rememberHapticHelper()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.jumpToTimestamp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = "Enter time in seconds (e.g. 12.5) or MM:SS (e.g. 01:30):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("e.g. 00:05.500 or 10") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0L to "0s", 1000L to "1s", 5000L to "5s", 10000L to "10s").forEach { (ms, lbl) ->
                        OutlinedButton(
                            onClick = {
                                haptics.tick()
                                onJump(ms.coerceIn(0L, totalMs))
                                onDismiss()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(lbl, fontSize = 11.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    haptics.snap()
                    val parsedMs = parseTimeToMs(textInput, currentMs)
                    onJump(parsedMs.coerceIn(0L, totalMs))
                    onDismiss()
                }
            ) {
                Text(strings.jumpBtn)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

private fun parseTimeToMs(input: String, fallback: Long): Long {
    val clean = input.trim()
    if (clean.isEmpty()) return fallback
    return try {
        if (clean.contains(":")) {
            val parts = clean.split(":")
            val min = parts[0].toLong()
            val sec = parts.getOrNull(1)?.toDouble() ?: 0.0
            (min * 60_000 + (sec * 1000).toLong())
        } else {
            (clean.toDouble() * 1000).toLong()
        }
    } catch (e: Exception) {
        fallback
    }
}

@Composable
private fun TtsVoiceoverDialog(
    initialText: String,
    ttsHelper: TtsHelper,
    onAddVoiceoverClip: (Clip) -> Unit,
    onDismiss: () -> Unit
) {
    var textInput by remember { mutableStateOf(initialText) }
    var pitch by remember { mutableFloatStateOf(1.0f) }
    var speed by remember { mutableFloatStateOf(1.0f) }
    var isGenerating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptics = rememberHapticHelper()

    AlertDialog(
        onDismissRequest = {
            ttsHelper.stop()
            onDismiss()
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.ttsDialogTitle, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = strings.ttsVoiceoverDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text(strings.enterTextToSpeak) },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    maxLines = 4
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Voice Pitch Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${strings.voicePitch}: ${((pitch * 10).toInt() / 10f)}x", fontSize = 11.sp)
                    Slider(
                        value = pitch,
                        onValueChange = { pitch = it },
                        valueRange = 0.5f..2.0f,
                        modifier = Modifier.width(160.dp)
                    )
                }

                // Speech Speed Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${strings.voiceSpeed}: ${((speed * 10).toInt() / 10f)}x", fontSize = 11.sp)
                    Slider(
                        value = speed,
                        onValueChange = { speed = it },
                        valueRange = 0.5f..2.0f,
                        modifier = Modifier.width(160.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Audition Button
                OutlinedButton(
                    onClick = {
                        haptics.tick()
                        if (textInput.isNotBlank()) {
                            ttsHelper.speak(textInput, pitch, speed)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.auditionVoice, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = textInput.isNotBlank() && !isGenerating,
                onClick = {
                    haptics.snap()
                    isGenerating = true
                    scope.launch {
                        val result = ttsHelper.synthesizeVoiceoverToFile(textInput, pitch, speed)
                        isGenerating = false
                        if (result != null) {
                            val (file, durationMs) = result
                            val newClip = Clip(
                                id = UUID.randomUUID().toString(),
                                file_path = file.absolutePath,
                                source_start_ms = 0L,
                                source_end_ms = durationMs.coerceAtLeast(600L),
                                timeline_start_ms = 0L,
                                volume = 1.0f
                            )
                            onAddVoiceoverClip(newClip)
                            onDismiss()
                            Toast.makeText(context, "TTS Voiceover added to audio track!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Speech synthesis completed", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                    }
                }
            ) {
                Text(if (isGenerating) strings.synthesizingAudio else strings.addToTimeline)
            }
        },
        dismissButton = {
            TextButton(onClick = {
                ttsHelper.stop()
                onDismiss()
            }) {
                Text(strings.cancel)
            }
        }
    )
}

@Composable
private fun BottomToolbarItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isHighlight: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal,
            color = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
        )
    }
}

@Composable
private fun AudioImportSheet(
    onImportLocalFile: () -> Unit,
    onSelectEffect: (SoundEffectsManager.SoundEffectItem) -> Unit
) {
    val context = LocalContext.current
    val haptics = rememberHapticHelper()
    val sfxManager = remember { SoundEffectsManager(context) }
    var sfxList by remember { mutableStateOf<List<SoundEffectsManager.SoundEffectItem>>(emptyList()) }
    var auditioningFile by remember { mutableStateOf<String?>(null) }
    val auditionPlayer = remember { android.media.MediaPlayer() }

    DisposableEffect(Unit) {
        onDispose {
            try {
                if (auditionPlayer.isPlaying) auditionPlayer.stop()
                auditionPlayer.release()
            } catch (e: Exception) {}
        }
    }

    LaunchedEffect(Unit) {
        sfxList = sfxManager.getAvailableSoundEffects()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Text(
            text = "Audio & Sound Effects",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(14.dp))

        // Option 1: File Picker
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    try { if (auditionPlayer.isPlaying) auditionPlayer.stop() } catch (e: Exception) {}
                    onImportLocalFile()
                }
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(strings.importAudioSong, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(strings.importAudioSongDesc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(strings.builtinSfxTitle, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(8.dp))

        sfxList.forEach { item ->
            val isPlaying = auditioningFile == item.localFilePath
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            haptics.tick()
                            if (isPlaying) {
                                try {
                                    auditionPlayer.stop()
                                    auditionPlayer.reset()
                                } catch (e: Exception) {}
                                auditioningFile = null
                            } else {
                                try {
                                    auditionPlayer.reset()
                                    auditionPlayer.setDataSource(item.localFilePath)
                                    auditionPlayer.prepare()
                                    auditionPlayer.start()
                                    auditioningFile = item.localFilePath
                                    auditionPlayer.setOnCompletionListener {
                                        auditioningFile = null
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    auditioningFile = null
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = strings.auditionSound,
                            tint = if (isPlaying) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(modifier = Modifier.weight(1f).padding(horizontal = 6.dp)) {
                        Text(item.name, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                        Text(item.description, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
                    }

                    Button(
                        onClick = {
                            haptics.snap()
                            try { if (auditionPlayer.isPlaying) auditionPlayer.stop() } catch (e: Exception) {}
                            onSelectEffect(item)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(strings.addBtn, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SubtitleImportSheet(
    onOpenTts: () -> Unit,
    onAutoCaptions: () -> Unit,
    onCustomScript: () -> Unit,
    onImportSrt: () -> Unit,
    onAddBlankText: () -> Unit
) {
    val haptics = rememberHapticHelper()
    val strings = LocalAppStrings.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Text(
            text = strings.addSubtitles,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = strings.autoCaptionsDesc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Option 0: Android Native Text-to-Speech (TTS Voiceover - 0 Byte)
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    haptics.snap()
                    onOpenTts()
                }
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(strings.ttsVoiceoverTitle, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(strings.ttsVoiceoverDesc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Option 1: Auto Captions
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    haptics.snap()
                    onAutoCaptions()
                }
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(strings.autoCaptionsTitle, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(strings.autoCaptionsDesc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Option 2: Custom Script / Lyrics
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    haptics.snap()
                    onCustomScript()
                }
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.TextFields, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(strings.typePasteScriptTitle, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(strings.typePasteScriptDesc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Option 3: Import .SRT
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    haptics.tick()
                    onImportSrt()
                }
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Subtitles, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(strings.importSrtTitle, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(strings.importSrtDesc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Option 4: Manual Text
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    haptics.tick()
                    onAddBlankText()
                }
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(strings.addBlankTextTitle, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(strings.addBlankTextDesc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
                }
            }
        }
    }
}

@Composable
private fun MediaImportSheet(
    onImportVideos: () -> Unit,
    onImportImages: () -> Unit
) {
    val haptics = rememberHapticHelper()
    val strings = LocalAppStrings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Text(
            text = strings.importMedia,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = strings.importVideosDesc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    haptics.snap()
                    onImportVideos()
                }
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.VideoCall, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(strings.importVideosTitle, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(strings.importVideosDesc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    haptics.snap()
                    onImportImages()
                }
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(strings.importPhotosTitle, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(strings.importPhotosDesc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
                }
            }
        }
    }
}

@Composable
private fun DirectTransitionsSheet(
    selectedTransition: TransitionType,
    transitionDurationMs: Long,
    onSelectTransition: (TransitionType, Long) -> Unit
) {
    val haptics = rememberHapticHelper()
    val strings = LocalAppStrings.current
    var currentDuration by remember { mutableFloatStateOf(transitionDurationMs.toFloat()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Text(
            text = strings.transitions,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = strings.transitionDesc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${strings.transitionDurationLabel}: ${(currentDuration / 1000f).let { String.format(java.util.Locale.US, "%.1fs", it) }}", fontSize = 12.sp)
        }
        Slider(
            value = currentDuration,
            onValueChange = { currentDuration = it },
            valueRange = 200f..2000f,
            steps = 9
        )
        Spacer(modifier = Modifier.height(10.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp)
        ) {
            items(TransitionType.values()) { trans ->
                val isSelected = trans == selectedTransition
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .clickable {
                            haptics.snap()
                            onSelectTransition(trans, currentDuration.toLong())
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = trans.displayName,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DirectAnimationsSheet(
    selectedAnimation: AnimationType,
    onSelectAnimation: (AnimationType) -> Unit
) {
    val haptics = rememberHapticHelper()
    var selectedCategory by remember { mutableStateOf("In") }
    val categories = listOf("In", "Out", "Combo")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Text(
            text = "Clip Animations",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Apply dynamic entrance, exit, or loop animations to this clip",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                val isCatSelected = cat == selectedCategory
                Button(
                    onClick = {
                        haptics.tick()
                        selectedCategory = cat
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCatSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = cat,
                        fontSize = 12.sp,
                        fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCatSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val filteredAnimations = AnimationType.values().filter { it.category == selectedCategory || it == AnimationType.None }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp)
        ) {
            items(filteredAnimations) { anim ->
                val isSelected = anim == selectedAnimation
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .clickable {
                            haptics.snap()
                            onSelectAnimation(anim)
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = anim.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
