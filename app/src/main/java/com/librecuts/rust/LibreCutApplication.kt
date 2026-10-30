package com.librecuts.rust

import android.app.Application
import android.util.Log
import com.librecuts.rust.engine.NativeEngine

class LibreCutApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            NativeEngine.init()
            Log.i("LibreCut", "Native Rust Engine loaded: ${NativeEngine.getVersion()}")
        } catch (e: UnsatisfiedLinkError) {
            Log.w("LibreCut", "Native library not yet compiled into APK: ${e.message}")
        }
    }
}

