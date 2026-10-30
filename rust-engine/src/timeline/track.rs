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
    pub time_ms: u64,
    pub x: f32,
    pub y: f32,
    pub scale: f32,
    pub rotation: f32,
    pub opacity: f32,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(tag = "type")]
pub enum EffectConfig {
    ChromaKey {
        target_r: u8,
        target_g: u8,
        target_b: u8,
        similarity: f32,
        smoothness: f32,
        despill: f32,
    },
    ColorGrading {
        brightness: f32,  // -1.0 to 1.0
        contrast: f32,    // 0.0 to 2.0
        saturation: f32,  // 0.0 to 2.0
        temperature: f32, // -1.0 to 1.0
    },
    Filter {
        filter_type: String, // "sepia", "grayscale", "blur", "sharpen", "vignette"
        intensity: f32,
    },
    Transform {
        crop_left: f32,
        crop_top: f32,
        crop_right: f32,
        crop_bottom: f32,
        rotation_degrees: f32,
        flip_h: bool,
        flip_v: bool,
    },
    TextOverlay {
        text: String,
        font_size: f32,
        x: f32,
        y: f32,
        color_rgba: u32,
    },
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Clip {
    pub id: String,
    pub file_path: String,
    pub source_start_ms: u64,
    pub source_end_ms: u64,
    pub timeline_start_ms: u64,
    #[serde(default = "default_speed")]
    pub speed: f32,
    #[serde(default = "default_volume")]
    pub volume: f32,
    #[serde(default)]
    pub muted: bool,
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
    pub id: String,
    pub track_type: TrackType,
    pub muted: bool,
    pub locked: bool,
    pub clips: Vec<Clip>,
}

