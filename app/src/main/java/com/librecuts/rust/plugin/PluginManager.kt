package com.librecuts.rust.plugin

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

class PluginManager(private val context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    private val pluginsDir: File
        get() = File(context.filesDir, "plugins").apply { mkdirs() }

    private val _plugins = MutableStateFlow<List<LibreCutPlugin>>(emptyList())
    val plugins: StateFlow<List<LibreCutPlugin>> = _plugins.asStateFlow()

    init {
        loadPlugins()
    }

    fun loadPlugins() {
        val files = pluginsDir.listFiles { _, name -> name.endsWith(".json") || name.endsWith(".lcplugin") }
        if (files.isNullOrEmpty()) {
            installDefaultCommunityPlugins()
        } else {
            val list = files.mapNotNull { file ->
                try {
                    json.decodeFromString(LibreCutPlugin.serializer(), file.readText())
                } catch (e: Exception) {
                    null
                }
            }
            _plugins.value = list
        }
    }

    suspend fun importPluginFromUri(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val content = context.contentResolver.openInputStream(uri)?.use {
                it.bufferedReader().readText()
            } ?: return@withContext false

            val plugin = json.decodeFromString(LibreCutPlugin.serializer(), content)
            val destFile = File(pluginsDir, "${plugin.id}.lcplugin")
            destFile.writeText(content)
            loadPlugins()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun togglePlugin(pluginId: String, enabled: Boolean) {
        val updated = _plugins.value.map {
            if (it.id == pluginId) it.copy(enabled = enabled) else it
        }
        _plugins.value = updated
        // Persist change
        updated.find { it.id == pluginId }?.let { plugin ->
            val file = File(pluginsDir, "${plugin.id}.json")
            file.writeText(json.encodeToString(LibreCutPlugin.serializer(), plugin))
        }
    }

    private fun installDefaultCommunityPlugins() {
        val defaultPlugins = listOf(
            LibreCutPlugin(
                id = "org.community.glitch_fx",
                name = "Chromatic Glitch FX",
                version = "1.2.0",
                author = "CyberShader Devs",
                description = "Vulkan-accelerated RGB split distortion, scanline jitter, and digital noise simulation.",
                category = PluginCategory.EFFECT,
                enabled = true,
                parameters = listOf(
                    PluginParameter("intensity", "Glitch Intensity", ParamType.SLIDER, 0.4f, 0f, 1f),
                    PluginParameter("chroma_spread", "RGB Separation", ParamType.SLIDER, 0.6f, 0f, 1f)
                )
            ),
            LibreCutPlugin(
                id = "org.community.vhs_retro",
                name = "VHS 1986 Retro Cam",
                version = "2.0.1",
                author = "RetroMedia FOSS",
                description = "Authentic magnetic tape degradation, warm color bleeding, and phosphor TV scanlines.",
                category = PluginCategory.FILTER,
                enabled = true,
                parameters = listOf(
                    PluginParameter("tape_wear", "Tape Wear", ParamType.SLIDER, 0.5f, 0f, 1f),
                    PluginParameter("color_bleed", "Color Bleed", ParamType.SLIDER, 0.35f, 0f, 1f)
                )
            ),
            LibreCutPlugin(
                id = "org.community.anamorphic_flare",
                name = "Anamorphic Blue Flare",
                version = "1.0.0",
                author = "CinemaLens Lab",
                description = "Horizontal cinematic streaks and anamorphic bokeh flare simulation.",
                category = PluginCategory.EFFECT,
                enabled = false,
                parameters = listOf(
                    PluginParameter("brightness", "Flare Brightness", ParamType.SLIDER, 0.7f, 0f, 1f)
                )
            ),
            LibreCutPlugin(
                id = "org.community.vocal_clarity",
                name = "Vocal Clarity Studio Pro",
                version = "1.1.0",
                author = "DSP Audio Team",
                description = "Multi-band voice speech clarity enhancer with background rumble suppression.",
                category = PluginCategory.AUDIO,
                enabled = true,
                parameters = listOf(
                    PluginParameter("clarity_boost", "Presence Boost", ParamType.SLIDER, 0.5f, 0f, 1f),
                    PluginParameter("de_ess", "De-Esser", ParamType.TOGGLE, 1.0f, 0f, 1f)
                )
            )
        )

        defaultPlugins.forEach { plugin ->
            val file = File(pluginsDir, "${plugin.id}.json")
            file.writeText(json.encodeToString(LibreCutPlugin.serializer(), plugin))
        }
        _plugins.value = defaultPlugins
    }
}
