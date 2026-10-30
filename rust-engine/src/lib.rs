pub mod audio;
pub mod codec;
pub mod effects;
pub mod engine;
pub mod timeline;

use std::sync::Mutex;
use jni::objects::{JClass, JObject, JString};
use jni::sys::{jboolean, jint, jlong, jstring, JNI_FALSE, JNI_TRUE};
use jni::JNIEnv;
use log::{error, info, LevelFilter};
use once_cell::sync::Lazy;

use crate::codec::smart_render::fast_trim_video;
use crate::codec::ndk_mediacodec::probe_media;
use crate::engine::VideoEditorEngine;
use crate::timeline::{Composition, ExportConfig};

static GLOBAL_ENGINE: Lazy<Mutex<VideoEditorEngine>> =
    Lazy::new(|| Mutex::new(VideoEditorEngine::new()));

#[no_mangle]
pub extern "system" fn Java_com_librecuts_rust_engine_NativeEngine_nativeInit(
    _env: JNIEnv,
    _class: JClass,
) -> jboolean {
    #[cfg(target_os = "android")]
    let _ = android_logger::init_once(
        android_logger::Config::default()
            .with_max_level(LevelFilter::Debug)
            .with_tag("LibreCutNative"),
    );

    info!("LibreCut Rust Native Engine v2.0 initialized with 16KB page alignment");
    JNI_TRUE
}

#[no_mangle]
pub extern "system" fn Java_com_librecuts_rust_engine_NativeEngine_nativeGetVersion(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    let version = "LibreCut-Rust 2.0.0 (SIMD + NDK Hardware Acceleration)";
    let output = env.new_string(version).expect("Couldn't create java string");
    output.into_raw()
}

#[no_mangle]
pub extern "system" fn Java_com_librecuts_rust_engine_NativeEngine_nativeProbeMedia(
    mut env: JNIEnv,
    _class: JClass,
    j_path: JString,
) -> jstring {
    let path_str: String = match env.get_string(&j_path) {
        Ok(s) => s.into(),
        Err(_) => return env.new_string("{}").unwrap().into_raw(),
    };

    match probe_media(&path_str) {
        Ok(info) => {
            let json = serde_json::json!({
                "durationMs": info.duration_ms,
                "width": info.width,
                "height": info.height,
                "fps": info.fps,
                "videoMime": info.video_mime,
                "audioMime": info.audio_mime,
                "bitrate": info.bitrate,
            });
            env.new_string(json.to_string()).unwrap().into_raw()
        }
        Err(e) => {
            error!("Probe error: {}", e);
            env.new_string(format!("{{\"error\":\"{}\"}}", e)).unwrap().into_raw()
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_com_librecuts_rust_engine_NativeEngine_nativeFastTrim(
    mut env: JNIEnv,
    _class: JClass,
    j_input: JString,
    j_output: JString,
    start_ms: jlong,
    end_ms: jlong,
) -> jboolean {
    let input: String = match env.get_string(&j_input) {
        Ok(s) => s.into(),
        Err(_) => return JNI_FALSE,
    };
    let output: String = match env.get_string(&j_output) {
        Ok(s) => s.into(),
        Err(_) => return JNI_FALSE,
    };

    match fast_trim_video(&input, &output, start_ms as u64, end_ms as u64) {
        Ok(_) => JNI_TRUE,
        Err(e) => {
            error!("Fast trim failed: {}", e);
            JNI_FALSE
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_com_librecuts_rust_engine_NativeEngine_nativeRenderComposition(
    mut env: JNIEnv,
    _class: JClass,
    j_composition_json: JString,
    j_export_config_json: JString,
    callback: JObject,
) -> jboolean {
    let comp_json: String = match env.get_string(&j_composition_json) {
        Ok(s) => s.into(),
        Err(_) => return JNI_FALSE,
    };
    let exp_json: String = match env.get_string(&j_export_config_json) {
        Ok(s) => s.into(),
        Err(_) => return JNI_FALSE,
    };

    let composition: Composition = match serde_json::from_str(&comp_json) {
        Ok(c) => c,
        Err(e) => {
            error!("Failed to parse composition JSON: {}", e);
            return JNI_FALSE;
        }
    };

    let export_config: ExportConfig = match serde_json::from_str(&exp_json) {
        Ok(cfg) => cfg,
        Err(e) => {
            error!("Failed to parse export config JSON: {}", e);
            return JNI_FALSE;
        }
    };

    let engine = GLOBAL_ENGINE.lock().unwrap();
    let res = engine.render_composition(&composition, &export_config, |progress| {
        // Invoke Java callback method `onProgress(float progress)` if provided
        if !callback.is_null() {
            let _ = env.call_method(
                &callback,
                "onProgress",
                "(F)V",
                &[jni::objects::JValue::Float(progress)],
            );
        }
    });

    match res {
        Ok(_) => JNI_TRUE,
        Err(e) => {
            error!("Render error: {}", e);
            JNI_FALSE
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_com_librecuts_rust_engine_NativeEngine_nativeCancelRender(
    _env: JNIEnv,
    _class: JClass,
) {
    let engine = GLOBAL_ENGINE.lock().unwrap();
    engine.cancel();
}

