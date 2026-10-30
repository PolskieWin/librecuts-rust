use super::track::{Clip, Track, TrackType};
use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Composition {
    #[serde(default = "default_width")]
    pub width: u32,
    #[serde(default = "default_height")]
    pub height: u32,
    #[serde(default = "default_fps")]
    pub fps: u32,
    #[serde(default = "default_sample_rate")]
    pub sample_rate: u32,
    #[serde(default = "default_channels")]
    pub channels: u32,
    #[serde(default)]
    pub tracks: Vec<Track>,
}

fn default_width() -> u32 { 1920 }
fn default_height() -> u32 { 1080 }
fn default_fps() -> u32 { 30 }
fn default_sample_rate() -> u32 { 48000 }
fn default_channels() -> u32 { 2 }

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ExportConfig {
    pub output_path: String,
    #[serde(default = "default_width")]
    pub width: u32,
    #[serde(default = "default_height")]
    pub height: u32,
    #[serde(default = "default_fps")]
    pub fps: u32,
    #[serde(default = "default_video_bitrate")]
    pub video_bitrate_bps: u32,
    #[serde(default = "default_audio_bitrate")]
    pub audio_bitrate_bps: u32,
    #[serde(default = "default_video_codec")]
    pub video_codec: String, // "h264", "hevc", "av1"
    #[serde(default = "default_audio_codec")]
    pub audio_codec: String, // "aac"
    #[serde(default = "default_smart_render")]
    pub smart_render: bool,  // attempt direct stream copy if single video trimmed without effects
}

fn default_video_bitrate() -> u32 { 12_000_000 }
fn default_audio_bitrate() -> u32 { 192_000 }
fn default_video_codec() -> String { "h264".into() }
fn default_audio_codec() -> String { "aac".into() }
fn default_smart_render() -> bool { true }

impl Composition {
    pub fn total_duration_ms(&self) -> u64 {
        let mut max_end = 0;
        for track in &self.tracks {
            for clip in &track.clips {
                let clip_end = clip.timeline_start_ms + clip.duration_ms();
                if clip_end > max_end {
                    max_end = clip_end;
                }
            }
        }
        max_end
    }

    /// Checks if this composition is a single-track trim that qualifies for Smart Render
    pub fn is_smart_render_eligible(&self) -> Option<&Clip> {
        let video_tracks: Vec<&Track> = self
            .tracks
            .iter()
            .filter(|t| t.track_type == TrackType::Video)
            .collect();

        if video_tracks.len() == 1 && video_tracks[0].clips.len() == 1 {
            let clip = &video_tracks[0].clips[0];
            // If speed is normal and no frame alteration effects are active
            if (clip.speed - 1.0).abs() < f32::EPSILON && clip.effects.is_empty() {
                return Some(clip);
            }
        }
        None
    }
}

