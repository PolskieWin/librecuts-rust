use rayon::prelude::*;

/// Fast color grading adjustments: brightness, contrast, saturation, and temperature
pub fn apply_color_grading(
    rgba_buffer: &mut [u8],
    brightness: f32,  // -1.0 to 1.0 (0.0 is neutral)
    contrast: f32,    // 0.0 to 2.0 (1.0 is neutral)
    saturation: f32,  // 0.0 to 2.0 (1.0 is neutral)
    temperature: f32, // -1.0 (cool/blue) to 1.0 (warm/orange)
) {
    if brightness == 0.0 && contrast == 1.0 && saturation == 1.0 && temperature == 0.0 {
        return;
    }

    rgba_buffer.par_chunks_exact_mut(4).for_each(|pixel| {
        let mut r = pixel[0] as f32 / 255.0;
        let mut g = pixel[1] as f32 / 255.0;
        let mut b = pixel[2] as f32 / 255.0;

        // 1. Temperature / Tint
        if temperature > 0.0 {
            r += temperature * 0.15;
            b -= temperature * 0.15;
        } else if temperature < 0.0 {
            r -= temperature.abs() * 0.15;
            b += temperature.abs() * 0.15;
        }

        // 2. Brightness
        r += brightness;
        g += brightness;
        b += brightness;

        // 3. Contrast (around 0.5 midpoint)
        r = (r - 0.5) * contrast + 0.5;
        g = (g - 0.5) * contrast + 0.5;
        b = (b - 0.5) * contrast + 0.5;

        // 4. Saturation via Rec. 709 Luminance
        let luma = 0.2126 * r + 0.7152 * g + 0.0722 * b;
        r = luma + saturation * (r - luma);
        g = luma + saturation * (g - luma);
        b = luma + saturation * (b - luma);

        pixel[0] = (r * 255.0).clamp(0.0, 255.0) as u8;
        pixel[1] = (g * 255.0).clamp(0.0, 255.0) as u8;
        pixel[2] = (b * 255.0).clamp(0.0, 255.0) as u8;
    });
}

