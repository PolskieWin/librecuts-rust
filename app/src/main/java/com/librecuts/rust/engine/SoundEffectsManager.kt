package com.librecuts.rust.engine

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * 100% Offline Sound Effects Manager
 * Generates and caches real, broadcast-quality 16-bit 44.1kHz PCM WAV audio files
 * on local device storage so ExoPlayer and export engines can natively play and mix real audio.
 */
class SoundEffectsManager(private val context: Context) {

    private val sfxDir: File
        get() = File(context.filesDir, "sfx").apply { mkdirs() }

    data class SoundEffectItem(
        val name: String,
        val filename: String,
        val durationSec: Double,
        val description: String,
        val localFilePath: String
    )

    suspend fun getAvailableSoundEffects(): List<SoundEffectItem> = withContext(Dispatchers.IO) {
        val effects = listOf(
            Triple("Cinematic Boom Impact", "cinematic_boom.wav", "Deep sub-bass impact hit with exponential decay"),
            Triple("Fast Air Whoosh", "fast_whoosh.wav", "Dynamic aerodynamic cut transition swish"),
            Triple("UI Tactile Pop Click", "tactile_pop.wav", "Crisp mechanical pop click for UI and snappy cuts"),
            Triple("Tension Riser", "tension_riser.wav", "Ascending pitch suspense builder for hype moments"),
            Triple("Vinyl Record Scratch", "vinyl_scratch.wav", "Retro analog turntable slip scratch")
        )

        effects.map { (name, filename, desc) ->
            val file = File(sfxDir, filename)
            val duration = when (filename) {
                "cinematic_boom.wav" -> 2.5
                "fast_whoosh.wav" -> 0.8
                "tactile_pop.wav" -> 0.3
                "tension_riser.wav" -> 3.0
                "vinyl_scratch.wav" -> 1.0
                else -> 1.0
            }

            if (!file.exists() || file.length() < 1024) {
                generateSfxFile(filename, file, duration)
            }

            SoundEffectItem(
                name = name,
                filename = filename,
                durationSec = duration,
                description = desc,
                localFilePath = file.absolutePath
            )
        }
    }

    private fun generateSfxFile(filename: String, targetFile: File, durationSec: Double) {
        val sampleRate = 44100
        val totalSamples = (sampleRate * durationSec).toInt()
        val samples = ShortArray(totalSamples)

        when (filename) {
            "cinematic_boom.wav" -> {
                // Deep sub-bass boom (starts at 80Hz, glides to 35Hz with exponential decay and saturation)
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    val freq = 80.0 - 45.0 * (t / durationSec)
                    val envelope = exp(-2.0 * t)
                    val raw = sin(2.0 * PI * freq * t) * envelope
                    // Soft-clip saturation
                    val saturated = Math.tanh(raw * 1.8)
                    samples[i] = (saturated * 32000).toInt().coerceIn(-32768, 32767).toShort()
                }
            }
            "fast_whoosh.wav" -> {
                // Swept aerodynamic whoosh: filtered noise and frequency rise/fall
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / durationSec
                    val freq = 300.0 + 1600.0 * sin(PI * t)
                    val envelope = sin(PI * t)
                    val noise = (Random.nextDouble() * 2.0 - 1.0) * 0.4
                    val tone = sin(2.0 * PI * freq * (i.toDouble() / sampleRate)) * 0.6
                    val combined = (noise + tone) * envelope
                    samples[i] = (combined * 28000).toInt().coerceIn(-32768, 32767).toShort()
                }
            }
            "tactile_pop.wav" -> {
                // Snappy mechanical click (1200Hz burst with rapid 50ms exponential decay)
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    val envelope = exp(-45.0 * t)
                    val raw = sin(2.0 * PI * 1200.0 * t) * envelope
                    samples[i] = (raw * 30000).toInt().coerceIn(-32768, 32767).toShort()
                }
            }
            "tension_riser.wav" -> {
                // Ascending tension pitch riser (120Hz climbing to 950Hz with building intensity)
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / durationSec
                    val freq = 120.0 + 830.0 * (t * t)
                    val envelope = 0.2 + 0.8 * t
                    val raw = sin(2.0 * PI * freq * (i.toDouble() / sampleRate)) * envelope
                    val tremolo = 0.9 + 0.1 * sin(2.0 * PI * 8.0 * (i.toDouble() / sampleRate))
                    samples[i] = (raw * tremolo * 28000).toInt().coerceIn(-32768, 32767).toShort()
                }
            }
            "vinyl_scratch.wav" -> {
                // Turntable vinyl record slip scratch
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    val freqMod = 600.0 + 400.0 * sin(2.0 * PI * 14.0 * t)
                    val envelope = exp(-3.0 * t)
                    val noise = (Random.nextDouble() * 2.0 - 1.0) * 0.6
                    val tone = sin(2.0 * PI * freqMod * t) * 0.4
                    val combined = (noise + tone) * envelope
                    samples[i] = (combined * 26000).toInt().coerceIn(-32768, 32767).toShort()
                }
            }
            else -> {
                for (i in 0 until totalSamples) {
                    samples[i] = 0
                }
            }
        }

        writeWavFile(targetFile, samples, sampleRate)
    }

    private fun writeWavFile(file: File, samples: ShortArray, sampleRate: Int) {
        val numChannels = 1
        val byteRate = sampleRate * numChannels * 2
        val dataSize = samples.size * 2
        val totalSize = 36 + dataSize

        val header = ByteBuffer.allocate(44).apply {
            order(ByteOrder.LITTLE_ENDIAN)
            put('R'.code.toByte())
            put('I'.code.toByte())
            put('F'.code.toByte())
            put('F'.code.toByte())
            putInt(totalSize)
            put('W'.code.toByte())
            put('A'.code.toByte())
            put('V'.code.toByte())
            put('E'.code.toByte())
            put('f'.code.toByte())
            put('m'.code.toByte())
            put('t'.code.toByte())
            put(' '.code.toByte())
            putInt(16) // Subchunk1Size for PCM
            putShort(1) // AudioFormat 1 = PCM
            putShort(numChannels.toShort())
            putInt(sampleRate)
            putInt(byteRate)
            putShort((numChannels * 2).toShort()) // BlockAlign
            putShort(16) // BitsPerSample
            put('d'.code.toByte())
            put('a'.code.toByte())
            put('t'.code.toByte())
            put('a'.code.toByte())
            putInt(dataSize)
        }

        FileOutputStream(file).use { fos ->
            fos.write(header.array())
            val pcmBuffer = ByteBuffer.allocate(samples.size * 2).apply {
                order(ByteOrder.LITTLE_ENDIAN)
                for (s in samples) {
                    putShort(s)
                }
            }
            fos.write(pcmBuffer.array())
            fos.flush()
        }
    }
}

