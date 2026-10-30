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
            Triple("Vine Boom", "vine_boom.wav", "Legendary deep bass drop with 808 sub rumble"),
            Triple("Bruh Sound Effect", "bruh.wav", "Classic vocal tone descent meme sound"),
            Triple("Airhorn Blare", "air_horn.wav", "Iconic triple-tone triumphant horn blast"),
            Triple("Fast Air Whoosh", "fast_whoosh.wav", "Dynamic aerodynamic cut transition swish"),
            Triple("Bubble Pop", "bubble_pop.wav", "Crisp satisfying mouth pop click"),
            Triple("Camera Shutter", "camera_shutter.wav", "Dual mechanical DSLR snapshot click"),
            Triple("Fail Buzzer", "fail_buzzer.wav", "Harsh dissonant wrong answer double buzzer"),
            Triple("Victory Bell Ding", "ding_bell.wav", "Pure resonant metallic chime with long tail"),
            Triple("Vinyl Record Scratch", "vinyl_scratch.wav", "Retro analog turntable slip scratch")
        )

        effects.map { (name, filename, desc) ->
            val file = File(sfxDir, filename)
            val duration = when (filename) {
                "vine_boom.wav" -> 2.2
                "bruh.wav" -> 1.2
                "air_horn.wav" -> 0.9
                "fast_whoosh.wav" -> 0.7
                "bubble_pop.wav" -> 0.35
                "camera_shutter.wav" -> 0.5
                "fail_buzzer.wav" -> 1.0
                "ding_bell.wav" -> 2.0
                "vinyl_scratch.wav" -> 0.8
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
            "vine_boom.wav" -> {
                // The legendary Vine Boom: hard sub-bass drop (145Hz down to 38Hz) with heavy saturation & sub rumble
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    val progress = t / durationSec
                    val freq = 145.0 * exp(-3.2 * progress) + 38.0
                    val subFreq = freq * 0.5

                    val env = exp(-1.8 * t)
                    val primary = sin(2.0 * PI * freq * t)
                    val sub = sin(2.0 * PI * subFreq * t) * 0.65
                    val initialPunchNoise = if (t < 0.08) (Random.nextDouble() * 2.0 - 1.0) * (1.0 - t / 0.08) * 0.4 else 0.0

                    val raw = (primary + sub + initialPunchNoise) * env
                    // Heavy soft-clip saturation for the signature bass crunch
                    val saturated = Math.tanh(raw * 2.2)
                    samples[i] = (saturated * 32200).toInt().coerceIn(-32768, 32767).toShort()
                }
            }
            "bruh.wav" -> {
                // Bruh vocal pitch glide: starts at 210Hz and glides down to 135Hz with formant warmth
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    val progress = (t / durationSec).coerceIn(0.0, 1.0)
                    val f0 = 210.0 - 75.0 * (progress * progress)
                    val env = sin(PI * progress) * exp(-0.8 * progress)

                    // Harmonics simulating vocal tract
                    val h1 = sin(2.0 * PI * f0 * t)
                    val h2 = sin(2.0 * PI * (f0 * 2.0) * t) * 0.55
                    val h3 = sin(2.0 * PI * (f0 * 3.0) * t) * 0.35
                    val vocal = (h1 + h2 + h3) * env
                    samples[i] = (vocal * 29000).toInt().coerceIn(-32768, 32767).toShort()
                }
            }
            "air_horn.wav" -> {
                // Triple-tone MLG horn blare (Bb4: 466Hz, Eb5: 622Hz, Bb5: 932Hz) with buzzy harmonics
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    val env = if (t < 0.05) t / 0.05 else exp(-1.5 * (t - 0.05))
                    val t1 = Math.signum(sin(2.0 * PI * 466.16 * t)) * 0.4
                    val t2 = Math.signum(sin(2.0 * PI * 622.25 * t)) * 0.35
                    val t3 = Math.signum(sin(2.0 * PI * 932.33 * t)) * 0.25
                    val raw = (t1 + t2 + t3) * env
                    samples[i] = (raw * 27000).toInt().coerceIn(-32768, 32767).toShort()
                }
            }
            "fast_whoosh.wav" -> {
                // Swept aerodynamic whoosh: filtered noise and frequency rise/fall
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / durationSec
                    val freq = 300.0 + 1600.0 * sin(PI * t)
                    val envelope = sin(PI * t)
                    val noise = (Random.nextDouble() * 2.0 - 1.0) * 0.45
                    val tone = sin(2.0 * PI * freq * (i.toDouble() / sampleRate)) * 0.55
                    val combined = (noise + tone) * envelope
                    samples[i] = (combined * 28000).toInt().coerceIn(-32768, 32767).toShort()
                }
            }
            "bubble_pop.wav" -> {
                // Satisfying bubble mouth pop (600Hz upward chirp to 1600Hz with fast decay)
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    val freq = 600.0 + 1000.0 * (t / 0.08).coerceIn(0.0, 1.0)
                    val env = exp(-35.0 * t)
                    val raw = sin(2.0 * PI * freq * t) * env
                    samples[i] = (raw * 30000).toInt().coerceIn(-32768, 32767).toShort()
                }
            }
            "camera_shutter.wav" -> {
                // Camera shutter: double mechanical click (click 1 at 0ms, click 2 at 110ms)
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    val click1 = if (t < 0.06) (Random.nextDouble() * 2.0 - 1.0) * exp(-50.0 * t) else 0.0
                    val t2 = t - 0.11
                    val click2 = if (t2 in 0.0..0.08) (Random.nextDouble() * 2.0 - 1.0) * exp(-45.0 * t2) else 0.0
                    val combined = (click1 + click2 * 1.2).coerceIn(-1.0, 1.0)
                    samples[i] = (combined * 31000).toInt().coerceIn(-32768, 32767).toShort()
                }
            }
            "fail_buzzer.wav" -> {
                // Harsh dissonant wrong answer error buzzer (150Hz + 158Hz beat frequency)
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    val env = if (t in 0.45..0.55) 0.0 else exp(-0.8 * (t % 0.5))
                    val b1 = Math.signum(sin(2.0 * PI * 150.0 * t)) * 0.5
                    val b2 = Math.signum(sin(2.0 * PI * 158.0 * t)) * 0.5
                    val raw = (b1 + b2) * env
                    samples[i] = (raw * 28000).toInt().coerceIn(-32768, 32767).toShort()
                }
            }
            "ding_bell.wav" -> {
                // Pure resonant crystal bell chime (2400Hz + 4800Hz overtone with long exponential decay)
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    val env = exp(-2.2 * t)
                    val fundamental = sin(2.0 * PI * 2400.0 * t) * 0.75
                    val overtone = sin(2.0 * PI * 4800.0 * t) * 0.25 * exp(-4.0 * t)
                    val raw = (fundamental + overtone) * env
                    samples[i] = (raw * 31000).toInt().coerceIn(-32768, 32767).toShort()
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
