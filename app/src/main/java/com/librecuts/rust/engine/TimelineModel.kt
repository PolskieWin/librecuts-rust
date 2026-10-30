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
enum class TransitionType(val displayName: String) {
    @SerialName("None") None("None"),
    @SerialName("CrossDissolve") CrossDissolve("Cross Dissolve"),
    @SerialName("FadeToBlack") FadeToBlack("Dip to Black"),
    @SerialName("DipToWhite") DipToWhite("Dip to White"),
    @SerialName("SlideLeft") SlideLeft("Slide Left"),
    @SerialName("SlideRight") SlideRight("Slide Right"),
    @SerialName("ZoomBlur") ZoomBlur("Zoom Blur"),
    @SerialName("WipeLeft") WipeLeft("Wipe Left"),
    @SerialName("FlashWhite") FlashWhite("Flash Cut")
}

@Serializable
enum class AnimationType(val label: String, val category: String) {
    @SerialName("None") None("None", "None"),

    // IN Animations
    @SerialName("FadeIn") FadeIn("Fade In", "In"),
    @SerialName("ZoomIn") ZoomIn("Zoom In", "In"),
    @SerialName("SlideInLeft") SlideInLeft("Slide In Left", "In"),
    @SerialName("SlideInRight") SlideInRight("Slide In Right", "In"),
    @SerialName("BounceIn") BounceIn("Bounce In", "In"),
    @SerialName("SpinIn") SpinIn("Spin In", "In"),

    // OUT Animations
    @SerialName("FadeOut") FadeOut("Fade Out", "Out"),
    @SerialName("ZoomOut") ZoomOut("Zoom Out", "Out"),
    @SerialName("SlideOutLeft") SlideOutLeft("Slide Out Left", "Out"),
    @SerialName("SlideOutRight") SlideOutRight("Slide Out Right", "Out"),
    @SerialName("SpinOut") SpinOut("Spin Out", "Out"),

    // COMBO Animations
    @SerialName("Shake") Shake("Dynamic Shake", "Combo"),
    @SerialName("Glitch") Glitch("RGB Glitch", "Combo"),
    @SerialName("Pendulum") Pendulum("Pendulum Swing", "Combo"),
    @SerialName("Flash") Flash("Strobe Flash", "Combo"),
    @SerialName("Pulse") Pulse("Heartbeat Pulse", "Combo"),
    @SerialName("Swing") Swing("Gentle Swing", "Combo")
}

@Serializable
enum class SpeedCurveType(val label: String) {
    NORMAL("Normal"),
    MONTAGE("Montage"),
    HERO_MOMENT("Hero Moment"),
    BULLET_TIME("Bullet Time"),
    FLASH_IN("Flash In")
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
    @SerialName("BackgroundRemoval")
    data class BackgroundRemoval(
        val threshold: Float = 0.45f,
        val feather: Float = 0.08f,
        val invert: Boolean = false
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
        val flip_v: Boolean = false,
        val crop_aspect_ratio: String = "Freeform"
    ) : EffectConfig()

    @Serializable
    @SerialName("TextOverlay")
    data class TextOverlay(
        val text: String = "LibreCut RS",
        val font_size: Float = 24f,
        val x: Float = 0.5f,
        val y: Float = 0.8f,
        val color_rgba: Long = 0xFFFFFFFFL,
        val custom_font_path: String? = null
    ) : EffectConfig()

    @Serializable
    @SerialName("ShapeMask")
    data class ShapeMask(
        val shape: String = "rectangle", // "rectangle", "circle", "heart", "linear"
        val center_x: Float = 0.5f,
        val center_y: Float = 0.5f,
        val size_x: Float = 0.4f,
        val size_y: Float = 0.4f,
        val feather: Float = 0.05f,
        val invert: Boolean = false
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
    val speedCurve: SpeedCurveType = SpeedCurveType.NORMAL,
    val volume: Float = 1.0f,
    val muted: Boolean = false,
    val transition: TransitionType = TransitionType.None,
    val transitionDurationMs: Long = 500L,
    val animation: AnimationType = AnimationType.None,
    val animationDurationMs: Long = 600L,
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
) {
    companion object {
        // 4GB Memory Ceiling for Heavy Editing (4K 60fps compositions)
        const val MAX_EDITING_BUFFER_BYTES = 4096L * 1024L * 1024L

        fun estimateFileSizeBytes(durationMs: Long, videoBitrateBps: Int, audioBitrateBps: Int): Long {
            val totalBps = (videoBitrateBps + audioBitrateBps).toLong()
            val durationSec = durationMs.toDouble() / 1000.0
            return ((totalBps * durationSec) / 8.0).toLong().coerceAtLeast(1024L)
        }

        fun formatFileSize(bytes: Long): String {
            val mb = bytes.toDouble() / (1024.0 * 1024.0)
            return if (mb >= 1000) {
                "%.2f GB".format(mb / 1024.0)
            } else {
                "%.1f MB".format(mb)
            }
        }
    }
}
