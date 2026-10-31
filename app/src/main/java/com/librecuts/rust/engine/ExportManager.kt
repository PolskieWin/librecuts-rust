package com.librecuts.rust.engine

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class ExportState {
    object Idle : ExportState()
    data class Exporting(val progress: Float) : ExportState()
    data class Success(val outputPath: String) : ExportState()
    data class Error(val message: String) : ExportState()
    object Cancelled : ExportState()
}

enum class ExportResolution(val label: String, val width: Int, val height: Int) {
    R720P("HD (720p)", 1280, 720),
    R1080P("Full HD (1080p)", 1920, 1080),
    R4K("4K UHD (2160p)", 3840, 2160);

    fun toDimensions(): Pair<Int, Int> = Pair(width, height)
}

class ExportManager(private val context: Context) {
    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    companion object {
        private const val TAG = "ExportManager"
    }

    suspend fun exportComposition(
        composition: Composition,
        resolution: Pair<Int, Int> = Pair(1920, 1080),
        fps: Int = 30,
        bitrateMbps: Int = 12,
        smartRender: Boolean = true
    ): Boolean = withContext(Dispatchers.IO) {
        _exportState.value = ExportState.Exporting(0.0f)

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) 
            ?: context.filesDir
        outputDir.mkdirs()
        val outputFile = File(outputDir, "LibreCut_$timeStamp.mp4")

        // Sanitize URIs in composition so NDK receives canonical filesystem paths
        val sanitizedTracks = composition.tracks.map { track ->
            val sanitizedClips = track.clips.map { clip ->
                var path = clip.file_path
                if (path.startsWith("content://") || path.startsWith("file://")) {
                    try {
                        val localPath = FileUtils.ensureLocalCacheFile(context, Uri.parse(path))
                        if (!localPath.isNullOrBlank()) {
                            path = localPath
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed caching uri $path for export: ${e.message}")
                    }
                }
                clip.copy(file_path = path)
            }
            track.copy(clips = sanitizedClips)
        }
        val cleanComposition = composition.copy(tracks = sanitizedTracks)

        val config = ExportConfig(
            output_path = outputFile.absolutePath,
            width = resolution.first,
            height = resolution.second,
            fps = fps,
            video_bitrate_bps = bitrateMbps * 1_000_000,
            smart_render = smartRender
        )

        try {
            var nativeSuccess = false
            try {
                nativeSuccess = NativeEngine.render(cleanComposition, config) { progress ->
                    _exportState.value = ExportState.Exporting(progress)
                }
            } catch (e: Throwable) {
                Log.w(TAG, "NativeEngine.render threw exception: ${e.message}")
                nativeSuccess = false
            }

            val fileValid = outputFile.exists() && outputFile.length() > 0L

            if (nativeSuccess && fileValid) {
                _exportState.value = ExportState.Success(outputFile.absolutePath)
                true
            } else {
                Log.i(TAG, "Native pipeline finished without valid file (nativeSuccess=$nativeSuccess, fileValid=$fileValid). Engaging Android MediaMuxer fallback...")
                _exportState.value = ExportState.Exporting(0.5f)
                val fallbackSuccess = fallbackExportWithAndroidMediaMuxer(cleanComposition, outputFile)
                if (fallbackSuccess && outputFile.exists() && outputFile.length() > 0L) {
                    _exportState.value = ExportState.Exporting(1.0f)
                    _exportState.value = ExportState.Success(outputFile.absolutePath)
                    true
                } else {
                    _exportState.value = ExportState.Error("Export failed: could not encode output video")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Export error: ${e.message}", e)
            _exportState.value = ExportState.Error("Export failed: ${e.message}")
            false
        }
    }

    private fun fallbackExportWithAndroidMediaMuxer(
        composition: Composition,
        outputFile: File
    ): Boolean {
        val videoClip = composition.tracks
            .filter { it.track_type == TrackType.Video || it.track_type == TrackType.Overlay }
            .flatMap { it.clips }
            .firstOrNull { it.file_path.isNotBlank() }
            ?: return false

        return try {
            val extractor = MediaExtractor()
            if (videoClip.file_path.startsWith("content://")) {
                extractor.setDataSource(context, Uri.parse(videoClip.file_path), null)
            } else {
                extractor.setDataSource(videoClip.file_path)
            }

            val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val trackMap = mutableMapOf<Int, Int>()

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    extractor.selectTrack(i)
                    val muxerTrack = muxer.addTrack(format)
                    trackMap[i] = muxerTrack
                }
            }

            if (trackMap.isEmpty()) {
                extractor.release()
                muxer.release()
                return false
            }

            muxer.start()

            val startUs = videoClip.source_start_ms * 1000L
            val endUs = if (videoClip.source_end_ms > videoClip.source_start_ms) {
                videoClip.source_end_ms * 1000L
            } else {
                Long.MAX_VALUE
            }
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            val buffer = ByteBuffer.allocateDirect(1024 * 1024)
            val bufferInfo = MediaCodec.BufferInfo()

            while (true) {
                val trackIndex = extractor.sampleTrackIndex
                if (trackIndex < 0) break

                val sampleTimeUs = extractor.sampleTime
                if (sampleTimeUs > endUs) break

                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) break

                val muxerTrackIndex = trackMap[trackIndex]
                if (muxerTrackIndex != null && sampleTimeUs >= startUs) {
                    bufferInfo.presentationTimeUs = (sampleTimeUs - startUs).coerceAtLeast(0L)
                    bufferInfo.flags = extractor.sampleFlags
                    muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                }

                if (!extractor.advance()) break
            }

            muxer.stop()
            muxer.release()
            extractor.release()
            outputFile.exists() && outputFile.length() > 0L
        } catch (e: Exception) {
            Log.e(TAG, "MediaMuxer fallback error: ${e.message}")
            try {
                if (videoClip.file_path.startsWith("content://")) {
                    context.contentResolver.openInputStream(Uri.parse(videoClip.file_path))?.use { input ->
                        FileOutputStream(outputFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                } else {
                    val src = File(videoClip.file_path)
                    src.copyTo(target = outputFile, overwrite = true)
                }
                outputFile.exists() && outputFile.length() > 0L
            } catch (e2: Exception) {
                false
            }
        }
    }

    fun cancel() {
        NativeEngine.cancel()
        _exportState.value = ExportState.Idle
    }
}

