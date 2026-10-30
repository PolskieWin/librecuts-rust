package com.librecuts.rust.engine

import kotlinx.serialization.json.Json
import org.json.JSONObject

interface NativeProgressCallback {
    fun onProgress(progress: Float)
}

object NativeEngine {
    private val json = Json { ignoreUnknownKeys = true }

    init {
        try {
            System.loadLibrary("librecut_engine")
        } catch (e: UnsatisfiedLinkError) {
            System.err.println("NativeEngine: Library 'liblibrecut_engine.so' not found in apk yet: ${e.message}")
        }
    }

    external fun nativeInit(): Boolean
    external fun nativeGetVersion(): String
    external fun nativeProbeMedia(filePath: String): String
    external fun nativeFastTrim(
        inputPath: String,
        outputPath: String,
        startMs: Long,
        endMs: Long
    ): Boolean

    external fun nativeRenderComposition(
        compositionJson: String,
        exportConfigJson: String,
        callback: NativeProgressCallback?
    ): Boolean

    external fun nativeCancelRender()

    fun init(): Boolean = nativeInit()
    fun getVersion(): String = nativeGetVersion()

    fun probe(filePath: String): JSONObject {
        val resultStr = nativeProbeMedia(filePath)
        return JSONObject(resultStr)
    }

    fun fastTrim(input: String, output: String, startMs: Long, endMs: Long): Boolean {
        return nativeFastTrim(input, output, startMs, endMs)
    }

    fun render(
        composition: Composition,
        exportConfig: ExportConfig,
        onProgress: (Float) -> Unit
    ): Boolean {
        val compJson = json.encodeToString(Composition.serializer(), composition)
        val expJson = json.encodeToString(ExportConfig.serializer(), exportConfig)

        return nativeRenderComposition(
            compJson,
            expJson,
            object : NativeProgressCallback {
                override fun onProgress(progress: Float) {
                    onProgress(progress)
                }
            }
        )
    }

    fun cancel() = nativeCancelRender()
}

