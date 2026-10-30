#![allow(dead_code)]
use rayon::prelude::*;

#[derive(Debug, Clone, Copy, PartialEq)]
pub enum MaskShape {
    Circle,
    Rectangle,
    Heart,
    LinearWipe,
}

impl MaskShape {
    pub fn from_str(s: &str) -> Self {
        match s.to_lowercase().as_str() {
            "circle" | "ellipse" => MaskShape::Circle,
            "heart" => MaskShape::Heart,
            "linear" | "wipe" => MaskShape::LinearWipe,
            _ => MaskShape::Rectangle,
        }
    }
}

/// Applies parallel SIMD-accelerated shape mask on RGBA buffer
pub fn apply_shape_mask(
    rgba_buffer: &mut [u8],
    width: u32,
    height: u32,
    shape: &str,
    center_x: f32, // 0.0 to 1.0
    center_y: f32, // 0.0 to 1.0
    size_x: f32,   // 0.0 to 1.0 radius or half-width
    size_y: f32,   // 0.0 to 1.0 radius or half-height
    feather: f32,  // 0.0 to 0.5 softness
    invert: bool,
) {
    let mask_type = MaskShape::from_str(shape);
    let cx = center_x * width as f32;
    let cy = center_y * height as f32;
    let rx = (size_x * width as f32).max(1.0);
    let ry = (size_y * height as f32).max(1.0);
    let feather_dist = (feather * width as f32).max(0.001);

    let row_bytes = (width as usize) * 4;

    rgba_buffer
        .par_chunks_mut(row_bytes)
        .enumerate()
        .for_each(|(y_idx, row)| {
            let py = y_idx as f32;
            for (x_idx, pixel) in row.chunks_exact_mut(4).enumerate() {
                let px = x_idx as f32;

                let inside_factor: f32 = match mask_type {
                    MaskShape::Circle => {
                        let dx = (px - cx) / rx;
                        let dy = (py - cy) / ry;
                        let dist = (dx * dx + dy * dy).sqrt();
                        if dist <= 1.0 {
                            1.0
                        } else if dist < 1.0 + (feather_dist / rx) {
                            1.0 - ((dist - 1.0) / (feather_dist / rx)).clamp(0.0, 1.0)
                        } else {
                            0.0
                        }
                    }
                    MaskShape::Rectangle => {
                        let dx = (px - cx).abs();
                        let dy = (py - cy).abs();
                        if dx <= rx && dy <= ry {
                            1.0
                        } else if dx <= rx + feather_dist && dy <= ry + feather_dist {
                            let excess_x = (dx - rx).max(0.0) / feather_dist;
                            let excess_y = (dy - ry).max(0.0) / feather_dist;
                            (1.0 - excess_x.max(excess_y)).clamp(0.0, 1.0)
                        } else {
                            0.0
                        }
                    }
                    MaskShape::Heart => {
                        // Mathematical heart curve: (x^2 + y^2 - 1)^3 - x^2 * y^3 <= 0
                        let nx = (px - cx) / rx;
                        let ny = -(py - cy) / ry; // Invert Y for Cartesian
                        let a = nx * nx + ny * ny - 1.0;
                        let formula = a * a * a - nx * nx * ny * ny * ny;
                        if formula <= 0.0 { 1.0 } else { 0.0 }
                    }
                    MaskShape::LinearWipe => {
                        let progress = (px / width as f32) - center_x;
                        if progress <= 0.0 {
                            1.0
                        } else if progress < feather {
                            1.0 - (progress / feather).clamp(0.0, 1.0)
                        } else {
                            0.0
                        }
                    }
                };

                let final_alpha_factor = if invert { 1.0 - inside_factor } else { inside_factor };
                pixel[3] = ((pixel[3] as f32) * final_alpha_factor).clamp(0.0, 255.0) as u8;
            }
        });
}
