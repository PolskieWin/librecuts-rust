package com.librecuts.rust.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.librecuts.rust.engine.AnimationType
import com.librecuts.rust.engine.AudioRecorderHelper
import com.librecuts.rust.engine.AutoCaptionHelper
import com.librecuts.rust.engine.Clip
import com.librecuts.rust.engine.Composition
import com.librecuts.rust.engine.EffectConfig
import com.librecuts.rust.engine.ExportManager
import com.librecuts.rust.engine.FileUtils
import com.librecuts.rust.engine.FontManager
import com.librecuts.rust.engine.Keyframe
import com.librecuts.rust.engine.Project
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
    val exportManager = remember { ExportManager(context) }
    val exportState by exportManager.exportState.collectAsState()

    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var selectedClipId by remember { mutableStateOf<String?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showCompressDialog by remember { mutableStateOf(false) }
    var showEffectsSheet by remember { mutableStateOf(false) }
    var showPluginManagerSheet by remember { mutableStateOf(false) }
    var showAudioSheet by remember { mutableStateOf(false) }
    var showSubtitleSheet by remember { mutableStateOf(false) }

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
            onSaveProject(project.copy(composition = composition))
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
            // Compact Bottom Toolbar for Import, Audio, Subtitles, Compress & Export as requested
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Add Clips
                    BottomToolbarItem(
                        icon = Icons.Default.VideoCall,
                        label = strings.addClips,
                        onClick = {
                            haptics.tick()
                            multipleVideoPickerLauncher.launch("video/*")
                        }
                    )

                    // 2. Audio & SFX
                    BottomToolbarItem(
                        icon = Icons.Default.LibraryMusic,
                        label = strings.addAudio,
                        onClick = {
                            haptics.tick()
                            showAudioSheet = true
                        }
                    )

                    // 3. Subtitles & Auto Captions
                    BottomToolbarItem(
                        icon = Icons.Default.Subtitles,
                        label = strings.addSubtitles,
                        onClick = {
                            haptics.tick()
                            showSubtitleSheet = true
                        }
                    )

                    // 4. Compress
                    BottomToolbarItem(
                        icon = Icons.Default.Compress,
                        label = strings.compress,
                        onClick = {
                            haptics.tick()
                            showCompressDialog = true
                        }
                    )

                    // 5. Export
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
                                text = "${strings.recordingVoiceover}: ${recordElapsedSeconds}s",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // Render Active Text Overlays with Custom Font Support and NO Stretch Bug
                val activeTextOverlay = composition.tracks
                    .flatMap { it.clips }
                    .filter { currentPositionMs in it.timeline_start_ms..(it.timeline_start_ms + it.durationMs) }
                    .flatMap { it.effects }
                    .filterIsInstance<EffectConfig.TextOverlay>()
                    .firstOrNull()

                activeTextOverlay?.let { textEff ->
                    val customFontFamily = remember(textEff.custom_font_path) {
                        fontManager.getFontFamily(textEff.custom_font_path)
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 24.dp, vertical = 20.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.72f))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = textEff.text,
                            fontFamily = customFontFamily,
                            fontSize = textEff.font_size.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Timeline Action Controls Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
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
                        contentDescription = if (isPlaying) strings.pause else strings.play,
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
                            contentDescription = strings.split,
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
                            contentDescription = strings.keyframe,
                            tint = if (selectedClip != null) Color(0xFFFFEB3B) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
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
                onSelectPreset = { name, durationSec ->
                    haptics.snap()
                    val audioClip = Clip(
                        id = UUID.randomUUID().toString(),
                        file_path = "sfx://${name.lowercase().replace(" ", "_")}",
                        source_start_ms = 0L,
                        source_end_ms = (durationSec * 1000).toLong(),
                        timeline_start_ms = currentPositionMs
                    )
                    val updatedTracks = composition.tracks.map { track ->
                        if (track.track_type == TrackType.Audio) track.copy(clips = track.clips + audioClip) else track
                    }
                    updateCompositionWithUndo(composition.copy(tracks = updatedTracks))
                    showAudioSheet = false
                    Toast.makeText(context, "$name added!", Toast.LENGTH_SHORT).show()
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
    onSelectPreset: (name: String, durationSec: Double) -> Unit
) {
    val haptics = rememberHapticHelper()

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
                .clickable { onImportLocalFile() }
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Import Audio Song / Voice (.mp3, .wav, .m4a)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("Select any audio file from device storage", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Built-in Sound Effects", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(8.dp))

        val presets = listOf(
            Triple("Cinematic Boom Impact", 2.5, "Deep low-end impact hit"),
            Triple("Fast Air Whoosh", 0.8, "Quick transition swish"),
            Triple("UI Tactile Pop Click", 0.3, "Subtle mechanical click"),
            Triple("Tension Riser", 3.0, "Ascending pitch suspense builder"),
            Triple("Vinyl Record Scratch", 1.2, "Retro turntable tape scratch")
        )

        presets.forEach { (name, duration, desc) ->
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .clickable { onSelectPreset(name, duration) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(name, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                        Text(desc, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Text("${duration}s", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun SubtitleImportSheet(
    onAutoCaptions: () -> Unit,
    onImportSrt: () -> Unit,
    onAddBlankText: () -> Unit
) {
    val haptics = rememberHapticHelper()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Text(
            text = "Subtitles & Auto Captions",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Generate on-device captions or import external subtitle files",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Option 1: Auto Captions (CapCut Style)
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
                    Text("Auto Captions (Speech to Subtitles)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("Detect speech intervals & automatically generate synced text overlays", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Option 2: Import .SRT
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
                    Text("Import .SRT Subtitle File", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("Parse external timestamped subtitles from storage", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Option 3: Manual Text
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
                Icon(Icons.Default.TextFields, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Add Blank Title / Text Clip", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("Insert a customizable text overlay at current playhead", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
                }
            }
        }
    }
}
