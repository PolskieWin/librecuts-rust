use log::{info, warn, error};
use std::ffi::CString;
use std::ptr;
use ndk_sys::*;

/// Smart Render / Lossless Fast Trim:
/// Direct bitstream packet copying from AMediaExtractor to AMediaMuxer without re-encoding.
/// Runs in under 1 second even for 4K 60fps files.
pub fn fast_trim_video(
    input_path: &str,
    output_path: &str,
    start_ms: u64,
    end_ms: u64,
) -> Result<(), String> {
    info!(
        "SmartRender: Starting fast lossless trim '{}' -> '{}' [{}ms to {}ms]",
        input_path, output_path, start_ms, end_ms
    );

    unsafe {
        let extractor = AMediaExtractor_new();
        if extractor.is_null() {
            return Err("Failed to create AMediaExtractor".into());
        }

        // Ensure target directory exists
        if let Some(parent) = std::path::Path::new(output_path).parent() {
            let _ = std::fs::create_dir_all(parent);
        }

        let c_input = CString::new(input_path).map_err(|e| e.to_string())?;
        let set_res = AMediaExtractor_setDataSource(extractor, c_input.as_ptr());
        let is_set_err: i32 = std::mem::transmute(set_res);
        if is_set_err != 0 {
            AMediaExtractor_delete(extractor);
            return Err(format!("AMediaExtractor_setDataSource failed with code {}", is_set_err));
        }

        // Open Output File Descriptor for AMediaMuxer
        let c_output = CString::new(output_path).map_err(|e| e.to_string())?;
        let output_fd = libc::open(
            c_output.as_ptr(),
            libc::O_CREAT | libc::O_WRONLY | libc::O_TRUNC,
            0o666,
        );
        if output_fd < 0 {
            AMediaExtractor_delete(extractor);
            return Err("Failed to open output file for muxing".into());
        }

        // 0 = AMEDIAMUXER_OUTPUT_FORMAT_MPEG_4
        let muxer = AMediaMuxer_new(
            output_fd,
            std::mem::transmute(0),
        );
        if muxer.is_null() {
            libc::close(output_fd);
            AMediaExtractor_delete(extractor);
            return Err("Failed to create AMediaMuxer".into());
        }

        let num_tracks = AMediaExtractor_getTrackCount(extractor);
        let mut track_map: Vec<isize> = Vec::with_capacity(num_tracks);
        let mut added_any_track = false;

        for i in 0..num_tracks {
            let format = AMediaExtractor_getTrackFormat(extractor, i);
            if format.is_null() {
                track_map.push(-1);
                continue;
            }

            let muxer_track = AMediaMuxer_addTrack(muxer, format);
            if muxer_track >= 0 {
                AMediaExtractor_selectTrack(extractor, i);
                track_map.push(muxer_track as isize);
                added_any_track = true;
            } else {
                track_map.push(-1);
            }
            AMediaFormat_delete(format);
        }

        if !added_any_track {
            AMediaMuxer_delete(muxer);
            libc::close(output_fd);
            AMediaExtractor_delete(extractor);
            return Err("No compatible tracks found to mux".into());
        }

        let mux_start_res = AMediaMuxer_start(muxer);
        let is_mux_err: i32 = std::mem::transmute(mux_start_res);
        if is_mux_err != 0 {
            AMediaMuxer_delete(muxer);
            libc::close(output_fd);
            AMediaExtractor_delete(extractor);
            return Err("AMediaMuxer_start failed".into());
        }

        // Seek to start position (in microseconds)
        let seek_us = (start_ms * 1000) as i64;
        let end_us = if end_ms > start_ms { (end_ms * 1000) as i64 } else { i64::MAX };
        AMediaExtractor_seekTo(
            extractor,
            seek_us,
            std::mem::transmute(0),
        );

        // Allocate copy buffer (1MB chunks)
        const BUF_SIZE: usize = 1024 * 1024;
        let mut buffer = vec![0u8; BUF_SIZE];
        let mut info_struct = AMediaCodecBufferInfo {
            offset: 0,
            size: 0,
            presentationTimeUs: 0,
            flags: 0,
        };

        let mut copied_packets = 0;
        let mut eos_count = 0;
        loop {
            let track_idx = AMediaExtractor_getSampleTrackIndex(extractor);
            if track_idx < 0 {
                break; // End of stream
            }

            let sample_time_us = AMediaExtractor_getSampleTime(extractor);
            let mux_track_id = track_map.get(track_idx as usize).copied().unwrap_or(-1);

            if sample_time_us > end_us {
                eos_count += 1;
                if eos_count > 100 {
                    break;
                }
                if !AMediaExtractor_advance(extractor) {
                    break;
                }
                continue;
            }

            let sample_size = AMediaExtractor_readSampleData(
                extractor,
                buffer.as_mut_ptr(),
                BUF_SIZE,
            );

            if sample_size <= 0 {
                break;
            }

            if mux_track_id >= 0 && sample_time_us >= seek_us {
                info_struct.offset = 0;
                info_struct.size = sample_size as i32;
                info_struct.presentationTimeUs = sample_time_us.saturating_sub(seek_us);
                info_struct.flags = AMediaExtractor_getSampleFlags(extractor);

                AMediaMuxer_writeSampleData(
                    muxer,
                    mux_track_id as usize,
                    buffer.as_ptr(),
                    &info_struct,
                );
                copied_packets += 1;
            }

            if !AMediaExtractor_advance(extractor) {
                break;
            }
        }

        info!("SmartRender: successfully copied {} packets directly.", copied_packets);

        AMediaMuxer_stop(muxer);
        AMediaMuxer_delete(muxer);
        libc::close(output_fd);
        AMediaExtractor_delete(extractor);

        Ok(())
    }
}

