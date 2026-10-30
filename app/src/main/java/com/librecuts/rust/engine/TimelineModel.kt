package com.librecuts.rust.engine

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class TrackType {
    @SerialName("Video") Video,
    @SerialName("Audio") Audio,
    @SerialName("Overlay") Overlay,
    @SerialName("Text") Text
}

@Serializable
data class Keyframe(
    val timeMs: Long,
    val x: Float = 0f,
    val y: Float = 0f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val opacity: Float = 1f
)

@Serializable
sealed class EffectConfig {
    @Serializable
    @SerialName("ChromaKey")
    data class ChromaKey(
        val target_r: Int = 0,
        val target_g: Int = 255,
        val target_b: Int = 0,
        val similarity: Float = 0.35f,
        val smoothness: Float = 0.1f,
        val despill: Float = 0.5f
    ) : EffectConfig()

    @Serializable
    @SerialName("ColorGrading")
    data class ColorGrading(
        val brightness: Float = 0f,
        val contrast: Float = 1f,
        val saturation: Float = 1f,
        val temperature: Float = 0f
    ) : EffectConfig()

    @Serializable
    @SerialName("Filter")
    data class Filter(
        val filter_type: String = "grayscale",
        val intensity: Float = 1.0f
    ) : EffectConfig()

    @Serializable
    @SerialName("Transform")
    data class Transform(
        val crop_left: Float = 0f,
        val crop_top: Float = 0f,
        val crop_right: Float = 0f,
        val crop_bottom: Float = 0f,
        val rotation_degrees: Float = 0f,
        val flip_h: Boolean = false,
        val flip_v: Boolean = false
    ) : EffectConfig()

    @Serializable
    @SerialName("TextOverlay")
    data class TextOverlay(
        val text: String = "LibreCut",
        val font_size: Float = 24f,
        val x: Float = 0.5f,
        val y: Float = 0.8f,
        val color_rgba: Long = 0xFFFFFFFFL
    ) : EffectConfig()
}

@Serializable
data class Clip(
    val id: String,
    val file_path: String,
    val source_start_ms: Long,
    val source_end_ms: Long,
    val timeline_start_ms: Long,
    val speed: Float = 1.0f,
    val volume: Float = 1.0f,
    val muted: Boolean = false,
    val keyframes: List<Keyframe> = emptyList(),
    val effects: List<EffectConfig> = emptyList()
) {
    val durationMs: Long
        get() {
            val raw = (source_end_ms - source_start_ms).coerceAtLeast(0)
            return if (speed > 0f) (raw / speed).toLong() else raw
        }
}

@Serializable
data class Track(
    val id: String,
    val track_type: TrackType,
    val muted: Boolean = false,
    val locked: Boolean = false,
    val clips: List<Clip> = emptyList()
)

@Serializable
data class Composition(
    val width: Int = 1920,
    val height: Int = 1080,
    val fps: Int = 30,
    val sample_rate: Int = 48000,
    val channels: Int = 2,
    val tracks: List<Track> = emptyList()
) {
    val totalDurationMs: Long
        get() = tracks.flatMap { it.clips }
            .maxOfOrNull { it.timeline_start_ms + it.durationMs } ?: 0L
}

@Serializable
data class ExportConfig(
    val output_path: String,
    val width: Int = 1920,
    val height: Int = 1080,
    val fps: Int = 30,
    val video_bitrate_bps: Int = 12_000_000,
    val audio_bitrate_bps: Int = 192_000,
    val video_codec: String = "h264",
    val audio_codec: String = "aac",
    val smart_render: Boolean = true
)

