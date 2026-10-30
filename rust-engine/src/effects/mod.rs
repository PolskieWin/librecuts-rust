pub mod chroma_key;
pub mod color_grading;
pub mod filters;
pub mod transform;

use crate::timeline::track::EffectConfig;

/// Applies a sequence of effects directly to an RGBA frame buffer
pub fn apply_effect_chain(rgba_buffer: &mut [u8], width: u32, height: u32, effects: &[EffectConfig]) {
    for effect in effects {
        match effect {
            EffectConfig::ChromaKey {
                target_r,
                target_g,
                target_b,
                similarity,
                smoothness,
                despill,
            } => {
                chroma_key::apply_chroma_key(
                    rgba_buffer,
                    width,
                    height,
                    *target_r,
                    *target_g,
                    *target_b,
                    *similarity,
                    *smoothness,
                    *despill,
                );
            }
            EffectConfig::ColorGrading {
                brightness,
                contrast,
                saturation,
                temperature,
            } => {
                color_grading::apply_color_grading(
                    rgba_buffer,
                    *brightness,
                    *contrast,
                    *saturation,
                    *temperature,
                );
            }
            EffectConfig::Filter {
                filter_type,
                intensity,
            } => {
                filters::apply_filter(rgba_buffer, width, height, filter_type, *intensity);
            }
            EffectConfig::Transform {
                flip_h,
                flip_v,
                ..
            } => {
                if *flip_h {
                    transform::flip_horizontal(rgba_buffer, width, height);
                }
                if *flip_v {
                    transform::flip_vertical(rgba_buffer, width, height);
                }
            }
        }
    }
}

