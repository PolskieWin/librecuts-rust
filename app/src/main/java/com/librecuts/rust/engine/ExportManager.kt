package com.librecuts.rust.engine

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class ExportState {
    object Idle : ExportState()
    data class Exporting(val progress: Float) : ExportState()
    data class Success(val outputPath: String) : ExportState()
    data class Error(val message: String) : ExportState()
}

class ExportManager(private val context: Context) {
    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

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
        val outputFile = File(outputDir, "LibreCut_$timeStamp.mp4")

        val config = ExportConfig(
            output_path = outputFile.absolutePath,
            width = resolution.first,
            height = resolution.second,
            fps = fps,
            video_bitrate_bps = bitrateMbps * 1_000_000,
            smart_render = smartRender
        )

        try {
            val success = NativeEngine.render(composition, config) { progress ->
                _exportState.value = ExportState.Exporting(progress)
            }

            if (success) {
                _exportState.value = ExportState.Success(outputFile.absolutePath)
                true
            } else {
                _exportState.value = ExportState.Error("Native rendering pipeline returned false")
                false
            }
        } catch (e: Exception) {
            _exportState.value = ExportState.Error("Export failed: ${e.message}")
            false
        }
    }

    fun cancel() {
        NativeEngine.cancel()
        _exportState.value = ExportState.Idle
    }
}

