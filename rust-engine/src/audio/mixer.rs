use super::effects::{apply_fades, apply_volume_with_limiter};

#[derive(Debug, Clone)]
pub struct AudioTrackInput {
    pub samples: Vec<f32>,
    pub volume: f32,
    pub is_voice: bool,
    pub fade_in_ms: u64,
    pub fade_out_ms: u64,
}

/// Mixes multiple audio tracks with ducking and master soft-limiting
pub fn mix_audio_tracks(
    tracks: &mut [AudioTrackInput],
    sample_rate: u32,
    channels: u32,
    ducking_amount_db: f32, // e.g. -14.0 dB
) -> Vec<f32> {
    if tracks.is_empty() {
        return Vec::new();
    }

    // Determine max length
    let max_len = tracks.iter().map(|t| t.samples.len()).max().unwrap_or(0);
    let mut mixed = vec![0.0f32; max_len];

    // Check if voice is active to calculate ducking factor
    let ducking_gain = 10.0f32.powf(ducking_amount_db / 20.0);

    for track in tracks.iter_mut() {
        // Pre-process track: volume and fades
        apply_volume_with_limiter(&mut track.samples, track.volume);
        apply_fades(
            &mut track.samples,
            sample_rate,
            channels,
            track.fade_in_ms,
            track.fade_out_ms,
        );

        let gain = if !track.is_voice && ducking_amount_db < 0.0 {
            ducking_gain
        } else {
            1.0
        };

        for (i, sample) in track.samples.iter().enumerate() {
            mixed[i] += *sample * gain;
        }
    }

    // Master limiting pass across mixed audio
    apply_volume_with_limiter(&mut mixed, 1.0);

    mixed
}

