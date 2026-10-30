package com.librecuts.rust.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import com.librecuts.rust.engine.Clip
import com.librecuts.rust.engine.EffectConfig

@Composable
fun EffectsSheet(
    clip: Clip,
    onUpdateEffects: (List<EffectConfig>, speed: Float, volume: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var speed by remember(clip.id) { mutableFloatStateOf(clip.speed) }
    var volume by remember(clip.id) { mutableFloatStateOf(clip.volume) }

    // Chroma key states
    var chromaEnabled by remember(clip.id) {
        mutableStateOf(clip.effects.any { it is EffectConfig.ChromaKey })
    }
    var similarity by remember(clip.id) { mutableFloatStateOf(0.35f) }
    var despill by remember(clip.id) { mutableFloatStateOf(0.5f) }

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
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Clip Adjustments (Rust SIMD Powered)",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Playback Speed Slider
        Text("Playback Speed: ${"%.1f".format(speed)}x", color = MaterialTheme.colorScheme.onSurface)
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
        Text("Audio Volume: ${(volume * 100).toInt()}% (Soft Limiter Protected)", color = MaterialTheme.colorScheme.onSurface)
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

        // Chroma Key (Green Screen)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Chroma Key (Green Screen)", color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Switch(
                checked = chromaEnabled,
                onCheckedChange = {
                    chromaEnabled = it
                    syncAllEffects()
                }
            )
        }

        if (chromaEnabled) {
            Text("Similarity: ${"%.2f".format(similarity)}", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Slider(
                value = similarity,
                onValueChange = {
                    similarity = it
                    syncAllEffects()
                },
                valueRange = 0.1f..0.8f
            )

            Text("Despill Suppressor: ${"%.2f".format(despill)}", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Slider(
                value = despill,
                onValueChange = {
                    despill = it
                    syncAllEffects()
                },
                valueRange = 0.0f..1.0f
            )
        }

        Divider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 12.dp))

        // Creative Filters
        Text("Creative Filters", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("none" to "Normal", "grayscale" to "B&W", "sepia" to "Sepia", "vignette" to "Vignette").forEach { (type, label) ->
                FilterChip(
                    selected = activeFilter == type,
                    onClick = {
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

        // Color Grading
        Text("Color Grading", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

        Text("Brightness: ${"%.2f".format(brightness)}", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Slider(
            value = brightness,
            onValueChange = {
                brightness = it
                syncAllEffects()
            },
            valueRange = -0.5f..0.5f
        )

        Text("Contrast: ${"%.2f".format(contrast)}", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Slider(
            value = contrast,
            onValueChange = {
                contrast = it
                syncAllEffects()
            },
            valueRange = 0.5f..2.0f
        )

        Text("Saturation: ${"%.2f".format(saturation)}", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Slider(
            value = saturation,
            onValueChange = {
                saturation = it
                syncAllEffects()
            },
            valueRange = 0.0f..2.0f
        )

        Divider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 12.dp))

        // Text & Subtitle Overlay
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Text / Subtitle Overlay", color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Switch(
                checked = textOverlayEnabled,
                onCheckedChange = {
                    textOverlayEnabled = it
                    syncAllEffects()
                }
            )
        }

        if (textOverlayEnabled) {
            OutlinedTextField(
                value = overlayText,
                onValueChange = {
                    overlayText = it
                    syncAllEffects()
                },
                label = { Text("Overlay Text") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text("Font Size: ${overlayFontSize.toInt()}sp", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Slider(
                value = overlayFontSize,
                onValueChange = {
                    overlayFontSize = it
                    syncAllEffects()
                },
                valueRange = 14f..64f
            )
        }
    }
}
