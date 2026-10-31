use rayon::prelude::*;

/// Pre-built creative filters: Cinematic, Cyberpunk, Vintage, Film, Noir, Vivid, Grayscale, Sepia, Vignette, Invert
pub fn apply_filter(rgba_buffer: &mut [u8], width: u32, height: u32, filter_type: &str, intensity: f32) {
    let intensity = intensity.clamp(0.0, 1.0);
    if intensity == 0.0 {
        return;
    }

    match filter_type.to_lowercase().as_str() {
        "cinematic" => {
            // Teal & Orange Hollywood blockbuster aesthetic
            rgba_buffer.par_chunks_exact_mut(4).for_each(|pixel| {
                let r = pixel[0] as f32;
                let g = pixel[1] as f32;
                let b = pixel[2] as f32;

                // Push highlights toward warm orange, shadows toward cool teal
                let luma = 0.299 * r + 0.587 * g + 0.114 * b;
                let target_r = if luma > 128.0 { (r * 1.15).min(255.0) } else { (r * 0.85).max(0.0) };
                let target_g = g;
                let target_b = if luma > 128.0 { (b * 0.85).max(0.0) } else { (b * 1.2).min(255.0) };

                pixel[0] = (r * (1.0 - intensity) + target_r * intensity).clamp(0.0, 255.0) as u8;
                pixel[1] = (g * (1.0 - intensity) + target_g * intensity).clamp(0.0, 255.0) as u8;
                pixel[2] = (b * (1.0 - intensity) + target_b * intensity).clamp(0.0, 255.0) as u8;
            });
        }
        "cyberpunk" => {
            // Neon cyan and electric violet boost
            rgba_buffer.par_chunks_exact_mut(4).for_each(|pixel| {
                let r = pixel[0] as f32;
                let g = pixel[1] as f32;
                let b = pixel[2] as f32;

                let cr = (r * 1.25 + 20.0).min(255.0);
                let cg = (g * 0.9).max(0.0);
                let cb = (b * 1.35 + 25.0).min(255.0);

                pixel[0] = (r * (1.0 - intensity) + cr * intensity) as u8;
                pixel[1] = (g * (1.0 - intensity) + cg * intensity) as u8;
                pixel[2] = (b * (1.0 - intensity) + cb * intensity) as u8;
            });
        }
        "vintage" => {
            // Warm retro 70s faded film
            rgba_buffer.par_chunks_exact_mut(4).for_each(|pixel| {
                let r = pixel[0] as f32;
                let g = pixel[1] as f32;
                let b = pixel[2] as f32;

                let vr = (r * 1.1 + 15.0).min(255.0);
                let vg = (g * 1.05 + 10.0).min(255.0);
                let vb = (b * 0.85).max(0.0);

                pixel[0] = (r * (1.0 - intensity) + vr * intensity) as u8;
                pixel[1] = (g * (1.0 - intensity) + vg * intensity) as u8;
                pixel[2] = (b * (1.0 - intensity) + vb * intensity) as u8;
            });
        }
        "vivid" => {
            // High saturation color pop
            rgba_buffer.par_chunks_exact_mut(4).for_each(|pixel| {
                let r = pixel[0] as f32;
                let g = pixel[1] as f32;
                let b = pixel[2] as f32;
                let gray = 0.299 * r + 0.587 * g + 0.114 * b;

                let sat_factor = 1.0 + (0.5 * intensity);
                pixel[0] = (gray + (r - gray) * sat_factor).clamp(0.0, 255.0) as u8;
                pixel[1] = (gray + (g - gray) * sat_factor).clamp(0.0, 255.0) as u8;
                pixel[2] = (gray + (b - gray) * sat_factor).clamp(0.0, 255.0) as u8;
            });
        }
        "noir" => {
            // High contrast black & white
            rgba_buffer.par_chunks_exact_mut(4).for_each(|pixel| {
                let r = pixel[0] as f32;
                let g = pixel[1] as f32;
                let b = pixel[2] as f32;
                let gray = 0.299 * r + 0.587 * g + 0.114 * b;

                // S-curve contrast boost
                let normalized = gray / 255.0;
                let contrasted = if normalized < 0.5 {
                    2.0 * normalized * normalized
                } else {
                    1.0 - 2.0 * (1.0 - normalized) * (1.0 - normalized)
                } * 255.0;

                pixel[0] = (r * (1.0 - intensity) + contrasted * intensity).clamp(0.0, 255.0) as u8;
                pixel[1] = (g * (1.0 - intensity) + contrasted * intensity).clamp(0.0, 255.0) as u8;
                pixel[2] = (b * (1.0 - intensity) + contrasted * intensity).clamp(0.0, 255.0) as u8;
            });
        }
        "grayscale" => {
            rgba_buffer.par_chunks_exact_mut(4).for_each(|pixel| {
                let r = pixel[0] as f32;
                let g = pixel[1] as f32;
                let b = pixel[2] as f32;
                let gray = 0.299 * r + 0.587 * g + 0.114 * b;

                pixel[0] = (r * (1.0 - intensity) + gray * intensity) as u8;
                pixel[1] = (g * (1.0 - intensity) + gray * intensity) as u8;
                pixel[2] = (b * (1.0 - intensity) + gray * intensity) as u8;
            });
        }
        "sepia" => {
            rgba_buffer.par_chunks_exact_mut(4).for_each(|pixel| {
                let r = pixel[0] as f32;
                let g = pixel[1] as f32;
                let b = pixel[2] as f32;

                let sr = (r * 0.393 + g * 0.769 + b * 0.189).min(255.0);
                let sg = (r * 0.349 + g * 0.686 + b * 0.168).min(255.0);
                let sb = (r * 0.272 + g * 0.534 + b * 0.131).min(255.0);

                pixel[0] = (r * (1.0 - intensity) + sr * intensity) as u8;
                pixel[1] = (g * (1.0 - intensity) + sg * intensity) as u8;
                pixel[2] = (b * (1.0 - intensity) + sb * intensity) as u8;
            });
        }
        "invert" => {
            rgba_buffer.par_chunks_exact_mut(4).for_each(|pixel| {
                let inv_r = 255 - pixel[0];
                let inv_g = 255 - pixel[1];
                let inv_b = 255 - pixel[2];

                pixel[0] = (pixel[0] as f32 * (1.0 - intensity) + inv_r as f32 * intensity) as u8;
                pixel[1] = (pixel[1] as f32 * (1.0 - intensity) + inv_g as f32 * intensity) as u8;
                pixel[2] = (pixel[2] as f32 * (1.0 - intensity) + inv_b as f32 * intensity) as u8;
            });
        }
        "vignette" => {
            let cx = width as f32 / 2.0;
            let cy = height as f32 / 2.0;
            let max_dist = (cx * cx + cy * cy).sqrt();

            rgba_buffer
                .par_chunks_exact_mut(width as usize * 4)
                .enumerate()
                .for_each(|(y, row)| {
                    let dy = y as f32 - cy;
                    for x in 0..width as usize {
                        let dx = x as f32 - cx;
                        let dist = (dx * dx + dy * dy).sqrt() / max_dist;
                        let factor = 1.0 - (dist * dist * intensity).clamp(0.0, 0.85);

                        let idx = x * 4;
                        row[idx] = (row[idx] as f32 * factor) as u8;
                        row[idx + 1] = (row[idx + 1] as f32 * factor) as u8;
                        row[idx + 2] = (row[idx + 2] as f32 * factor) as u8;
                    }
                });
        }
        "motion_blur" | "blur" => {
            let radius = ((intensity * 12.0) as i32).max(1);
            let orig = rgba_buffer.to_vec();
            let w = width as i32;
            let _h = height as i32;

            rgba_buffer
                .par_chunks_exact_mut(width as usize * 4)
                .enumerate()
                .for_each(|(y, row)| {
                    let y_i = y as i32;
                    for x in 0..width as usize {
                        let x_i = x as i32;
                        let mut sum_r = 0u32;
                        let mut sum_g = 0u32;
                        let mut sum_b = 0u32;
                        let mut count = 0u32;

                        for k in -radius..=radius {
                            let sample_x = (x_i + k).clamp(0, w - 1);
                            let idx = ((y_i * w + sample_x) * 4) as usize;
                            sum_r += orig[idx] as u32;
                            sum_g += orig[idx + 1] as u32;
                            sum_b += orig[idx + 2] as u32;
                            count += 1;
                        }

                        let idx = x * 4;
                        let orig_r = row[idx] as f32;
                        let orig_g = row[idx + 1] as f32;
                        let orig_b = row[idx + 2] as f32;

                        let blurred_r = (sum_r / count) as f32;
                        let blurred_g = (sum_g / count) as f32;
                        let blurred_b = (sum_b / count) as f32;

                        row[idx] = (orig_r * (1.0 - intensity) + blurred_r * intensity).clamp(0.0, 255.0) as u8;
                        row[idx + 1] = (orig_g * (1.0 - intensity) + blurred_g * intensity).clamp(0.0, 255.0) as u8;
                        row[idx + 2] = (orig_b * (1.0 - intensity) + blurred_b * intensity).clamp(0.0, 255.0) as u8;
                    }
                });
        }
        _ => {}
    }
}
