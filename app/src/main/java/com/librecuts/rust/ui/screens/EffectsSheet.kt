package com.librecuts.rust.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.librecuts.rust.engine.Clip
import com.librecuts.rust.engine.EffectConfig
import com.librecuts.rust.ui.components.rememberHapticHelper

@Composable
fun EffectsSheet(
    clip: Clip,
    onUpdateEffects: (List<EffectConfig>, speed: Float, volume: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberHapticHelper()
    var speed by remember(clip.id) { mutableFloatStateOf(clip.speed) }
    var volume by remember(clip.id) { mutableFloatStateOf(clip.volume) }

    // Chroma key states
    var chromaEnabled by remember(clip.id) {
        mutableStateOf(clip.effects.any { it is EffectConfig.ChromaKey })
    }
    var similarity by remember(clip.id) { mutableFloatStateOf(0.35f) }
    var despill by remember(clip.id) { mutableFloatStateOf(0.5f) }

    // Shape Masking states
    val existingMask = clip.effects.find { it is EffectConfig.ShapeMask } as? EffectConfig.ShapeMask
    var maskEnabled by remember(clip.id) { mutableStateOf(existingMask != null) }
    var maskShape by remember(clip.id) { mutableStateOf(existingMask?.shape ?: "circle") }
    var maskSize by remember(clip.id) { mutableFloatStateOf(existingMask?.size_x ?: 0.4f) }
    var maskFeather by remember(clip.id) { mutableFloatStateOf(existingMask?.feather ?: 0.05f) }
    var maskInvert by remember(clip.id) { mutableStateOf(existingMask?.invert ?: false) }

    // Color grading states
    var brightness by remember(clip.id) { mutableFloatStateOf(0.0f) }
    var contrast by remember(clip.id) { mutableFloatStateOf(1.0f) }
    var saturation by remember(clip.id) { mutableFloatStateOf(1.0f) }

    // Creative Filter state
    val existingFilter = clip.effects.find { it is EffectConfig.Filter } as? EffectConfig.Filter
    var activeFilter by remember(clip.id) { mutableStateOf(existingFilter?.filter_type ?: "none") }

    // Text Overlay state
    val existingText = clip.effects.find { it is EffectConfig.TextOverlay } as? EffectConfig.TextOverlay
    var textOverlayEnabled by remember(clip.id) { mutableStateOf(existingText != null) }
    var overlayText by remember(clip.id) { mutableStateOf(existingText?.text ?: "Sample Text") }
    var overlayFontSize by remember(clip.id) { mutableFloatStateOf(existingText?.font_size ?: 24f) }

    fun syncAllEffects() {
        val newEffects = mutableListOf<EffectConfig>()
        if (chromaEnabled) {
            newEffects.add(EffectConfig.ChromaKey(similarity = similarity, despill = despill))
        }
        if (maskEnabled) {
            newEffects.add(
                EffectConfig.ShapeMask(
                    shape = maskShape,
                    center_x = 0.5f,
                    center_y = 0.5f,
                    size_x = maskSize,
                    size_y = maskSize,
                    feather = maskFeather,
                    invert = maskInvert
                )
            )
        }
        if (brightness != 0f || contrast != 1f || saturation != 1f) {
            newEffects.add(EffectConfig.ColorGrading(brightness = brightness, contrast = contrast, saturation = saturation))
        }
        if (activeFilter != "none") {
            newEffects.add(EffectConfig.Filter(filter_type = activeFilter, intensity = 1.0f))
        }
        if (textOverlayEnabled) {
            newEffects.add(EffectConfig.TextOverlay(text = overlayText, font_size = overlayFontSize))
        }
        onUpdateEffects(newEffects, speed, volume)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Clip Adjustments & FX (Rust Accelerated)",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(14.dp))

        // Playback Speed Slider
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Playback Speed: ${"%.1f".format(speed)}x", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        }
        Slider(
            value = speed,
            onValueChange = {
                speed = it
                syncAllEffects()
            },
            valueRange = 0.25f..4.0f,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )

        // Audio Volume Booster (up to 200%)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Audio Boost: ${(volume * 100).toInt()}% (Soft Knee Limiter Protected)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        }
        Slider(
            value = volume,
            onValueChange = {
                volume = it
                syncAllEffects()
            },
            valueRange = 0.0f..2.0f,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )

        Divider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 12.dp))

        // Shape Masking Section (Original LibreCuts Feature)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Shape Masking", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                Text("Circle, rectangle, heart & linear wipe mask", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            Switch(
                checked = maskEnabled,
                onCheckedChange = {
                    haptics.tick()
                    maskEnabled = it
                    syncAllEffects()
                }
            )
        }

        if (maskEnabled) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple("circle", "Circle", Icons.Default.RadioButtonUnchecked),
                    Triple("rectangle", "Rectangle", Icons.Default.CropSquare),
                    Triple("heart", "Heart", Icons.Default.Favorite),
                    Triple("linear", "Wipe", Icons.Default.LinearScale)
                ).forEach { (shapeKey, label, icon) ->
                    FilterChip(
                        selected = maskShape == shapeKey,
                        onClick = {
                            haptics.tick()
                            maskShape = shapeKey
                            syncAllEffects()
                        },
                        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text("Mask Radius: ${"%.2f".format(maskSize)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Slider(value = maskSize, onValueChange = { maskSize = it; syncAllEffects() }, valueRange = 0.1f..0.9f)

            Text("Edge Feather: ${"%.2f".format(maskFeather)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Slider(value = maskFeather, onValueChange = { maskFeather = it; syncAllEffects() }, valueRange = 0.0f..0.3f)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Invert Mask Alpha", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Switch(checked = maskInvert, onCheckedChange = { maskInvert = it; syncAllEffects() })
            }
        }

        Divider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 12.dp))

        // Chroma Key (Green Screen with Despill)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Chroma Key (Green Screen)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                Text("Zero-latency SIMD keying with despill color suppression", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            Switch(
                checked = chromaEnabled,
                onCheckedChange = {
                    haptics.tick()
                    chromaEnabled = it
                    syncAllEffects()
                }
            )
        }

        if (chromaEnabled) {
            Spacer(modifier = Modifier.height(6.dp))
            Text("Similarity Threshold: ${"%.2f".format(similarity)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Slider(value = similarity, onValueChange = { similarity = it; syncAllEffects() }, valueRange = 0.1f..0.8f)

            Text("Despill Suppressor: ${"%.2f".format(despill)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Slider(value = despill, onValueChange = { despill = it; syncAllEffects() }, valueRange = 0.0f..1.0f)
        }

        Divider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 12.dp))

        // Creative Filters
        Text("Creative Color Filters", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("none" to "Normal", "grayscale" to "B&W", "sepia" to "Sepia", "vignette" to "Vignette").forEach { (type, label) ->
                FilterChip(
                    selected = activeFilter == type,
                    onClick = {
                        haptics.tick()
                        activeFilter = type
                        syncAllEffects()
                    },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        Divider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 12.dp))

        // Color Grading (Vulkan / SIMD)
        Text("Color Grading (3-Way Lift/Gamma/Gain)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(6.dp))

        Text("Brightness: ${"%.2f".format(brightness)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Slider(value = brightness, onValueChange = { brightness = it; syncAllEffects() }, valueRange = -0.5f..0.5f)

        Text("Contrast: ${"%.2f".format(contrast)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Slider(value = contrast, onValueChange = { contrast = it; syncAllEffects() }, valueRange = 0.5f..2.0f)

        Text("Saturation: ${"%.2f".format(saturation)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Slider(value = saturation, onValueChange = { saturation = it; syncAllEffects() }, valueRange = 0.0f..2.0f)

        Divider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 12.dp))

        // Text & Subtitle Overlay
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Text / Subtitle Overlay", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                Text("Custom text with live preview rendering", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            Switch(
                checked = textOverlayEnabled,
                onCheckedChange = {
                    haptics.tick()
                    textOverlayEnabled = it
                    syncAllEffects()
                }
            )
        }

        if (textOverlayEnabled) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = overlayText,
                onValueChange = {
                    overlayText = it
                    syncAllEffects()
                },
                label = { Text("Overlay Text") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Font Size: ${overlayFontSize.toInt()}sp", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Slider(value = overlayFontSize, onValueChange = { overlayFontSize = it; syncAllEffects() }, valueRange = 14f..64f)
        }
    }
}
