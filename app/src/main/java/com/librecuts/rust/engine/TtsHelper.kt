package com.librecuts.rust.engine

import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.UUID

/**
 * 100% Offline, Zero-Byte Android Native Text-to-Speech (TTS) Voiceover Generator.
 * Uses Android system speech synthesizer (`android.speech.tts.TextToSpeech`).
 * Adds 0 bytes to APK size and requires zero internet permissions.
 */
class TtsHelper(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                tts?.language = Locale.getDefault()
            }
        }
    }

    fun isReady(): Boolean = isInitialized && tts != null

    fun speak(
        text: String,
        pitch: Float = 1.0f,
        speechRate: Float = 1.0f,
        locale: Locale = Locale.getDefault()
    ) {
        tts?.let { engine ->
            engine.language = locale
            engine.setPitch(pitch.coerceIn(0.5f, 2.0f))
            engine.setSpeechRate(speechRate.coerceIn(0.5f, 2.0f))
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_audition_${UUID.randomUUID()}")
        }
    }

    fun stop() {
        tts?.stop()
    }

    suspend fun synthesizeVoiceoverToFile(
        text: String,
        pitch: Float = 1.0f,
        speechRate: Float = 1.0f,
        locale: Locale = Locale.getDefault()
    ): Pair<File, Long>? = withContext(Dispatchers.IO) {
        val engine = tts ?: return@withContext null
        if (!isInitialized) return@withContext null

        val ttsDir = File(context.filesDir, "tts").apply { mkdirs() }
        val outputFile = File(ttsDir, "voiceover_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.wav")
        val utteranceId = "tts_export_${UUID.randomUUID()}"

        engine.language = locale
        engine.setPitch(pitch.coerceIn(0.5f, 2.0f))
        engine.setSpeechRate(speechRate.coerceIn(0.5f, 2.0f))

        val lock = Object()
        var completed = false
        var success = false

        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) {
                if (id == utteranceId) {
                    synchronized(lock) {
                        completed = true
                        success = true
                        lock.notifyAll()
                    }
                }
            }
            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) {
                if (id == utteranceId) {
                    synchronized(lock) {
                        completed = true
                        success = false
                        lock.notifyAll()
                    }
                }
            }
        })

        val params = Bundle()
        val result = engine.synthesizeToFile(text, params, outputFile, utteranceId)
        if (result != TextToSpeech.SUCCESS) {
            return@withContext null
        }

        synchronized(lock) {
            if (!completed) {
                try {
                    lock.wait(6000L) // Wait up to 6 seconds for speech synthesis
                } catch (e: Exception) {}
            }
        }

        if (success && outputFile.exists() && outputFile.length() > 44) {
            val durationMs = getAudioDurationMs(outputFile)
            Pair(outputFile, durationMs)
        } else {
            null
        }
    }

    private fun getAudioDurationMs(file: File): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            time?.toLong() ?: 2000L
        } catch (e: Exception) {
            // Estimate based on file length (44.1kHz 16-bit mono = ~88200 bytes/sec)
            val pcmDataLength = (file.length() - 44).coerceAtLeast(0)
            (pcmDataLength * 1000L) / 88200L
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {}
    }
}
