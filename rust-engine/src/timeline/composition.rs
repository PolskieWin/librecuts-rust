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
    #[serde(alias = "sampleRate", alias = "sample_rate", default = "default_sample_rate")]
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
    #[serde(alias = "outputPath", alias = "output_path")]
    pub output_path: String,
    #[serde(default = "default_width")]
    pub width: u32,
    #[serde(default = "default_height")]
    pub height: u32,
    #[serde(default = "default_fps")]
    pub fps: u32,
    #[serde(alias = "videoBitrateBps", alias = "video_bitrate_bps", default = "default_video_bitrate")]
    pub video_bitrate_bps: u32,
    #[serde(alias = "audioBitrateBps", alias = "audio_bitrate_bps", default = "default_audio_bitrate")]
    pub audio_bitrate_bps: u32,
    #[serde(alias = "videoCodec", alias = "video_codec", default = "default_video_codec")]
    pub video_codec: String, // "h264", "hevc", "av1"
    #[serde(alias = "audioCodec", alias = "audio_codec", default = "default_audio_codec")]
    pub audio_codec: String, // "aac"
    #[serde(alias = "smartRender", alias = "smart_render", default = "default_smart_render")]
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

    /// Checks if this composition qualifies for Smart Render (single video clip without filters/transitions)
    pub fn is_smart_render_eligible(&self) -> Option<&Clip> {
        let active_tracks: Vec<&Track> = self
            .tracks
            .iter()
            .filter(|t| !t.clips.is_empty())
            .collect();

        if active_tracks.len() == 1 && active_tracks[0].track_type == TrackType::Video && active_tracks[0].clips.len() == 1 {
            let clip = &active_tracks[0].clips[0];
            // If speed is normal and no frame alteration effects or transitions are active
            if (clip.speed - 1.0).abs() < f32::EPSILON && clip.effects.is_empty() && clip.keyframes.is_empty() {
                return Some(clip);
            }
        }
        None
    }
}

