use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum TrackType {
    Video,
    Audio,
    Overlay,
    Text,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Keyframe {
    #[serde(alias = "timeMs", alias = "time_ms", default)]
    pub time_ms: u64,
    #[serde(default)]
    pub x: f32,
    #[serde(default)]
    pub y: f32,
    #[serde(default = "default_scale")]
    pub scale: f32,
    #[serde(default)]
    pub rotation: f32,
    #[serde(default = "default_scale")]
    pub opacity: f32,
    #[serde(default)]
    pub easing: Option<String>,
}

fn default_scale() -> f32 { 1.0 }

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(tag = "type")]
pub enum EffectConfig {
    #[serde(alias = "ChromaKey")]
    ChromaKey {
        #[serde(default)]
        target_r: u8,
        #[serde(default)]
        target_g: u8,
        #[serde(default)]
        target_b: u8,
        #[serde(default = "default_similarity")]
        similarity: f32,
        #[serde(default = "default_smoothness")]
        smoothness: f32,
        #[serde(default = "default_despill")]
        despill: f32,
    },
    #[serde(alias = "ColorGrading")]
    ColorGrading {
        #[serde(default)]
        brightness: f32,  // -1.0 to 1.0
        #[serde(default = "default_scale")]
        contrast: f32,    // 0.0 to 2.0
        #[serde(default = "default_scale")]
        saturation: f32,  // 0.0 to 2.0
        #[serde(default)]
        temperature: f32, // -1.0 to 1.0
    },
    #[serde(alias = "Filter")]
    Filter {
        #[serde(default)]
        filter_type: String, // "sepia", "grayscale", "blur", "sharpen", "vignette"
        #[serde(default = "default_scale")]
        intensity: f32,
    },
    #[serde(alias = "Transform")]
    Transform {
        #[serde(default)]
        crop_left: f32,
        #[serde(default)]
        crop_top: f32,
        #[serde(default)]
        crop_right: f32,
        #[serde(default)]
        crop_bottom: f32,
        #[serde(default)]
        rotation_degrees: f32,
        #[serde(default)]
        flip_h: bool,
        #[serde(default)]
        flip_v: bool,
        #[serde(default = "default_scale")]
        scale: f32,
        #[serde(default)]
        crop_aspect_ratio: Option<String>,
    },
    #[serde(alias = "TextOverlay")]
    TextOverlay {
        #[serde(default)]
        text: String,
        #[serde(default = "default_font_size")]
        font_size: f32,
        #[serde(default)]
        x: f32,
        #[serde(default)]
        y: f32,
        #[serde(default = "default_color_rgba")]
        color_rgba: u64,
        #[serde(default)]
        custom_font_path: Option<String>,
        #[serde(default)]
        stroke_width: f32,
        #[serde(default = "default_stroke_color_rgba")]
        stroke_color_rgba: u64,
    },
    #[serde(alias = "ShapeMask")]
    ShapeMask {
        #[serde(default)]
        shape: String,
        #[serde(default)]
        center_x: f32,
        #[serde(default)]
        center_y: f32,
        #[serde(default)]
        size_x: f32,
        #[serde(default)]
        size_y: f32,
        #[serde(default)]
        feather: f32,
        #[serde(default)]
        invert: bool,
    },
    #[serde(alias = "BackgroundRemoval")]
    BackgroundRemoval {
        #[serde(default)]
        threshold: f32,
        #[serde(default)]
        feather: f32,
        #[serde(default)]
        invert: bool,
    },
}

fn default_similarity() -> f32 { 0.35 }
fn default_smoothness() -> f32 { 0.1 }
fn default_despill() -> f32 { 0.5 }
fn default_scale() -> f32 { 1.0 }
fn default_font_size() -> f32 { 24.0 }
fn default_color_rgba() -> u64 { 0xFFFFFFFF }
fn default_stroke_color_rgba() -> u64 { 0xFF000000 }

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Clip {
    #[serde(default)]
    pub id: String,
    #[serde(alias = "filePath", alias = "file_path", default)]
    pub file_path: String,
    #[serde(alias = "sourceStartMs", alias = "source_start_ms", default)]
    pub source_start_ms: u64,
    #[serde(alias = "sourceEndMs", alias = "source_end_ms", default)]
    pub source_end_ms: u64,
    #[serde(alias = "timelineStartMs", alias = "timeline_start_ms", default)]
    pub timeline_start_ms: u64,
    #[serde(default = "default_speed")]
    pub speed: f32,
    #[serde(alias = "speed_curve", alias = "speedCurve", default)]
    pub speedCurve: Option<String>,
    #[serde(default = "default_volume")]
    pub volume: f32,
    #[serde(default)]
    pub muted: bool,
    #[serde(default)]
    pub transition: Option<String>,
    #[serde(alias = "transition_duration_ms", alias = "transitionDurationMs", default)]
    pub transitionDurationMs: Option<u64>,
    #[serde(default)]
    pub animation: Option<String>,
    #[serde(alias = "animation_duration_ms", alias = "animationDurationMs", default)]
    pub animationDurationMs: Option<u64>,
    #[serde(default)]
    pub keyframes: Vec<Keyframe>,
    #[serde(default)]
    pub effects: Vec<EffectConfig>,
}

fn default_speed() -> f32 { 1.0 }
fn default_volume() -> f32 { 1.0 }

impl Clip {
    pub fn duration_ms(&self) -> u64 {
        let raw_duration = self.source_end_ms.saturating_sub(self.source_start_ms);
        if self.speed > 0.0 {
            (raw_duration as f32 / self.speed) as u64
        } else {
            raw_duration
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Track {
    #[serde(default)]
    pub id: String,
    #[serde(alias = "trackType", alias = "track_type")]
    pub track_type: TrackType,
    #[serde(default)]
    pub muted: bool,
    #[serde(default)]
    pub locked: bool,
    #[serde(default)]
    pub clips: Vec<Clip>,
}

