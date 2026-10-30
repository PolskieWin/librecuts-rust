package com.librecuts.rust.plugin

import kotlinx.serialization.Serializable

@Serializable
enum class PluginCategory {
    EFFECT,
    FILTER,
    AUDIO,
    TRANSITION,
    UTILITY
}

@Serializable
enum class ParamType {
    SLIDER,
    COLOR,
    TOGGLE
}

@Serializable
data class PluginParameter(
    val id: String,
    val name: String,
    val type: ParamType,
    val defaultValue: Float = 0f,
    val minValue: Float = 0f,
    val maxValue: Float = 1f,
    var currentValue: Float = defaultValue
)

@Serializable
data class LibreCutPlugin(
    val id: String,
    val name: String,
    val version: String,
    val author: String,
    val description: String,
    val category: PluginCategory,
    var enabled: Boolean = true,
    val shaderCode: String? = null,
    val parameters: List<PluginParameter> = emptyList()
)
