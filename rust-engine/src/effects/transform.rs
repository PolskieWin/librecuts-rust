use rayon::prelude::*;

/// Fast horizontal flip for RGBA buffer
pub fn flip_horizontal(rgba_buffer: &mut [u8], width: u32, height: u32) {
    let row_stride = width as usize * 4;
    rgba_buffer
        .par_chunks_exact_mut(row_stride)
        .for_each(|row| {
            for x in 0..(width as usize / 2) {
                let left_idx = x * 4;
                let right_idx = (width as usize - 1 - x) * 4;
                for i in 0..4 {
                    row.swap(left_idx + i, right_idx + i);
                }
            }
        });
}

/// Fast vertical flip for RGBA buffer
pub fn flip_vertical(rgba_buffer: &mut [u8], width: u32, height: u32) {
    let row_stride = width as usize * 4;
    let half_height = height as usize / 2;

    for y in 0..half_height {
        let top_start = y * row_stride;
        let bottom_start = (height as usize - 1 - y) * row_stride;

        for i in 0..row_stride {
            rgba_buffer.swap(top_start + i, bottom_start + i);
        }
    }
}

