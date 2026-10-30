use super::track::{Clip, Track, TrackType};
use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Composition {
    pub width: u32,
    pub height: u32,
    pub fps: u32,
    pub sample_rate: u32,
    pub channels: u32,
    pub tracks: Vec<Track>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ExportConfig {
    pub output_path: String,
    pub width: u32,
    pub height: u32,
    pub fps: u32,
    pub video_bitrate_bps: u32,
    pub audio_bitrate_bps: u32,
    pub video_codec: String, // "h264", "hevc", "av1"
    pub audio_codec: String, // "aac"
    pub smart_render: bool,  // attempt direct stream copy if single video trimmed without effects
}

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

