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

        let c_input = CString::new(input_path).map_err(|e| e.to_string())?;
        let set_res = AMediaExtractor_setDataSource(extractor, c_input.as_ptr());
        if set_res != media_status_t_AMEDIA_OK {
            AMediaExtractor_delete(extractor);
            return Err(format!("AMediaExtractor_setDataSource failed with code {}", set_res));
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

        let muxer = AMediaMuxer_new(
            output_fd,
            media_status_t_AMEDIA_MUXER_OUTPUT_FORMAT_MPEG_4 as u32,
        );
        if muxer.is_null() {
            libc::close(output_fd);
            AMediaExtractor_delete(extractor);
            return Err("Failed to create AMediaMuxer".into());
        }

        let num_tracks = AMediaExtractor_getTrackCount(extractor);
        let mut track_map: Vec<isize> = Vec::with_capacity(num_tracks);

        for i in 0..num_tracks {
            let format = AMediaExtractor_getTrackFormat(extractor, i);
            if format.is_null() {
                track_map.push(-1);
                continue;
            }

            AMediaExtractor_selectTrack(extractor, i);
            let muxer_track = AMediaMuxer_addTrack(muxer, format);
            track_map.push(muxer_track as isize);
            AMediaFormat_delete(format);
        }

        let mux_start_res = AMediaMuxer_start(muxer);
        if mux_start_res != media_status_t_AMEDIA_OK {
            AMediaMuxer_delete(muxer);
            libc::close(output_fd);
            AMediaExtractor_delete(extractor);
            return Err("AMediaMuxer_start failed".into());
        }

        // Seek to start position (in microseconds)
        let seek_us = (start_ms * 1000) as i64;
        let end_us = (end_ms * 1000) as i64;
        AMediaExtractor_seekTo(
            extractor,
            seek_us,
            AMediaExtractorSeekMode_AMEDIAEXTRACTOR_SEEK_PREVIOUS_SYNC,
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
        loop {
            let track_idx = AMediaExtractor_getSampleTrackIndex(extractor);
            if track_idx < 0 {
                break; // End of stream
            }

            let sample_time_us = AMediaExtractor_getSampleTime(extractor);
            if sample_time_us > end_us {
                break; // Reached end of cut window
            }

            let sample_size = AMediaExtractor_readSampleData(
                extractor,
                buffer.as_mut_ptr(),
                BUF_SIZE,
            );

            if sample_size <= 0 {
                break;
            }

            let mux_track_id = track_map[track_idx as usize];
            if mux_track_id >= 0 && sample_time_us >= seek_us {
                info_struct.offset = 0;
                info_struct.size = sample_size as i32;
                // Offset presentation timestamp relative to trim start
                info_struct.presentationTimeUs = sample_time_us - seek_us;
                info_struct.flags = AMediaExtractor_getSampleFlags(extractor);

                AMediaMuxer_writeSampleData(
                    muxer,
                    mux_track_id as usize,
                    buffer.as_ptr(),
                    &info_struct,
                );
                copied_packets += 1;
            }

            AMediaExtractor_advance(extractor);
        }

        info!("SmartRender: successfully copied {} packets directly.", copied_packets);

        AMediaMuxer_stop(muxer);
        AMediaMuxer_delete(muxer);
        libc::close(output_fd);
        AMediaExtractor_delete(extractor);

        Ok(())
    }
}

