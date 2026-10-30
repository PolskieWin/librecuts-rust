/// Applies volume boost (e.g., 0.0 to 2.0 = 200%) with a soft-knee limiter (tanh)
/// to prevent ugly digital clipping.
pub fn apply_volume_with_limiter(samples: &mut [f32], volume_factor: f32) {
    if (volume_factor - 1.0).abs() < f32::EPSILON {
        return;
    }

    for sample in samples.iter_mut() {
        let boosted = *sample * volume_factor;
        // Soft clipping using tanh limiter when signal exceeds 0.95 threshold
        if boosted.abs() > 0.95 {
            *sample = boosted.signum() * (0.95 + 0.05 * ((boosted.abs() - 0.95) / 0.05).tanh());
        } else {
            *sample = boosted;
        }
    }
}

/// Applies linear fade-in and fade-out to audio samples
pub fn apply_fades(
    samples: &mut [f32],
    sample_rate: u32,
    channels: u32,
    fade_in_ms: u64,
    fade_out_ms: u64,
) {
    let total_frames = samples.len() / channels as usize;
    let fade_in_frames = ((fade_in_ms as f64 / 1000.0) * sample_rate as f64) as usize;
    let fade_out_frames = ((fade_out_ms as f64 / 1000.0) * sample_rate as f64) as usize;

    let ch = channels as usize;

    // Fade In
    if fade_in_frames > 0 {
        let limit = fade_in_frames.min(total_frames);
        for frame in 0..limit {
            let gain = frame as f32 / limit as f32;
            for c in 0..ch {
                samples[frame * ch + c] *= gain;
            }
        }
    }

    // Fade Out
    if fade_out_frames > 0 && total_frames > fade_out_frames {
        let start_frame = total_frames - fade_out_frames;
        for frame in start_frame..total_frames {
            let gain = (total_frames - frame) as f32 / fade_out_frames as f32;
            for c in 0..ch {
                samples[frame * ch + c] *= gain;
            }
        }
    }
}

