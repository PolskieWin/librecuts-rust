package com.librecuts.rust.engine

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.io.IOException

class AudioRecorderHelper(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var recordStartTimeMs: Long = 0L

    var isRecording: Boolean = false
        private set

    fun startRecording(): Boolean {
        if (isRecording) return false

        val voiceoverDir = File(context.cacheDir, "voiceovers").apply { mkdirs() }
        val outputFile = File(voiceoverDir, "voiceover_${System.currentTimeMillis()}.m4a")
        currentOutputFile = outputFile

        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        return try {
            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(48000)
                setAudioEncodingBitRate(192000)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            recordStartTimeMs = System.currentTimeMillis()
            isRecording = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            recorder.release()
            mediaRecorder = null
            false
        }
    }

    data class RecordingResult(
        val filePath: String,
        val durationMs: Long
    )

    fun stopRecording(): RecordingResult? {
        if (!isRecording) return null
        val durationMs = (System.currentTimeMillis() - recordStartTimeMs).coerceAtLeast(100L)
        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
        }

        val file = currentOutputFile
        return if (file != null && file.exists() && file.length() > 0) {
            RecordingResult(file.absolutePath, durationMs)
        } else {
            null
        }
    }
}

