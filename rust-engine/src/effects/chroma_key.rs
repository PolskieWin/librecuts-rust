use rayon::prelude::*;

/// Highly optimized chroma key (green screen) algorithm with Rayon multi-threading.
/// Processes an RGBA byte slice in-place.
pub fn apply_chroma_key(
    rgba_buffer: &mut [u8],
    _width: u32,
    _height: u32,
    target_r: u8,
    target_g: u8,
    target_b: u8,
    similarity: f32, // typically 0.2 to 0.5
    smoothness: f32, // typically 0.05 to 0.2
    despill: f32,    // 0.0 to 1.0 (removes green bounce on edges)
) {
    let key_r = target_r as f32 / 255.0;
    let key_g = target_g as f32 / 255.0;
    let key_b = target_b as f32 / 255.0;

    // Process 4 bytes (RGBA) per pixel in parallel across CPU cores
    rgba_buffer
        .par_chunks_exact_mut(4)
        .for_each(|pixel| {
            let r = pixel[0] as f32 / 255.0;
            let g = pixel[1] as f32 / 255.0;
            let b = pixel[2] as f32 / 255.0;

            // Normalized Euclidean distance in RGB color space
            let dr = r - key_r;
            let dg = g - key_g;
            let db = b - key_b;
            let dist = (dr * dr + dg * dg + db * db).sqrt();

            let alpha = if dist < similarity {
                0.0
            } else if dist < similarity + smoothness {
                // Smooth transition edge
                (dist - similarity) / smoothness
            } else {
                1.0
            };

            // Despill filter to suppress green spill on hair/edges
            let mut final_g = g;
            if despill > 0.0 && alpha < 1.0 {
                let spill = (g - (r + b) * 0.5).max(0.0);
                final_g = (g - spill * despill).max(0.0);
            }

            pixel[1] = (final_g * 255.0).clamp(0.0, 255.0) as u8;
            pixel[3] = ((pixel[3] as f32 * alpha)).clamp(0.0, 255.0) as u8;
        });
}

