use rayon::prelude::*;

/// Pre-built creative filters: Grayscale, Sepia, Vignette, Invert, Sharpen
pub fn apply_filter(rgba_buffer: &mut [u8], width: u32, height: u32, filter_type: &str, intensity: f32) {
    let intensity = intensity.clamp(0.0, 1.0);
    if intensity == 0.0 {
        return;
    }

    match filter_type {
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
        _ => {}
    }
}

