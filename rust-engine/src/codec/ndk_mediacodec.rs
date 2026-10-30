use log::{error, info, warn};
use ndk_sys::*;
use std::ffi::CString;

pub struct MediaProbeResult {
    pub duration_ms: u64,
    pub width: u32,
    pub height: u32,
    pub fps: f32,
    pub video_mime: String,
    pub audio_mime: String,
    pub bitrate: u32,
}

/// Probes media file information using native AMediaExtractor
pub fn probe_media(file_path: &str) -> Result<MediaProbeResult, String> {
    unsafe {
        let extractor = AMediaExtractor_new();
        if extractor.is_null() {
            return Err("Failed to allocate AMediaExtractor".into());
        }

        let c_path = CString::new(file_path).map_err(|e| e.to_string())?;
        if AMediaExtractor_setDataSource(extractor, c_path.as_ptr()) != media_status_t_AMEDIA_OK {
            AMediaExtractor_delete(extractor);
            return Err("Failed to read data source for probe".into());
        }

        let num_tracks = AMediaExtractor_getTrackCount(extractor);
        let mut width = 0;
        let mut height = 0;
        let mut duration_us = 0i64;
        let mut video_mime = String::new();
        let mut audio_mime = String::new();
        let mut fps = 30.0f32;
        let mut bitrate = 0;

        for i in 0..num_tracks {
            let format = AMediaExtractor_getTrackFormat(extractor, i);
            if format.is_null() {
                continue;
            }

            let mut mime_ptr: *const std::os::raw::c_char = std::ptr::null();
            if AMediaFormat_getString(format, AMEDIAFORMAT_KEY_MIME, &mut mime_ptr) {
                let mime = std::ffi::CStr::from_ptr(mime_ptr).to_string_lossy().into_owned();
                if mime.starts_with("video/") {
                    video_mime = mime;
                    let mut w = 0;
                    let mut h = 0;
                    let mut r = 0;
                    if AMediaFormat_getInt32(format, AMEDIAFORMAT_KEY_WIDTH, &mut w) {
                        width = w as u32;
                    }
                    if AMediaFormat_getInt32(format, AMEDIAFORMAT_KEY_HEIGHT, &mut h) {
                        height = h as u32;
                    }
                    if AMediaFormat_getInt32(format, AMEDIAFORMAT_KEY_FRAME_RATE, &mut r) {
                        fps = r as f32;
                    }
                    let mut d = 0i64;
                    if AMediaFormat_getInt64(format, AMEDIAFORMAT_KEY_DURATION, &mut d) {
                        duration_us = duration_us.max(d);
                    }
                    let mut b = 0;
                    if AMediaFormat_getInt32(format, AMEDIAFORMAT_KEY_BIT_RATE, &mut b) {
                        bitrate = b as u32;
                    }
                } else if mime.starts_with("audio/") {
                    audio_mime = mime;
                    let mut d = 0i64;
                    if AMediaFormat_getInt64(format, AMEDIAFORMAT_KEY_DURATION, &mut d) {
                        duration_us = duration_us.max(d);
                    }
                }
            }
            AMediaFormat_delete(format);
        }

        AMediaExtractor_delete(extractor);

        Ok(MediaProbeResult {
            duration_ms: (duration_us / 1000) as u64,
            width,
            height,
            fps,
            video_mime,
            audio_mime,
            bitrate,
        })
    }
}

