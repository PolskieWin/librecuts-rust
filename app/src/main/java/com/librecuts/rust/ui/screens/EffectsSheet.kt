package com.librecuts.rust.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
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
import com.librecuts.rust.ui.theme.AccentAmber
import com.librecuts.rust.ui.theme.DarkSurface

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

    Column(
        modifier = modifier
            .background(DarkSurface)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Clip Adjustments (Rust SIMD Powered)", style = MaterialTheme.typography.titleMedium, color = AccentAmber)
        Spacer(modifier = Modifier.height(12.dp))

        // Speed Slider
        Text("Playback Speed: ${"%.1f".format(speed)}x", color = Color.White)
        Slider(
            value = speed,
            onValueChange = {
                speed = it
                onUpdateEffects(clip.effects, speed, volume)
            },
            valueRange = 0.25f..4.0f,
            colors = SliderDefaults.colors(thumbColor = AccentAmber, activeTrackColor = AccentAmber)
        )

        // Audio Volume Booster (up to 200%)
        Text("Audio Volume: ${(volume * 100).toInt()}% (Soft Limiter Protected)", color = Color.White)
        Slider(
            value = volume,
            onValueChange = {
                volume = it
                onUpdateEffects(clip.effects, speed, volume)
            },
            valueRange = 0.0f..2.0f,
            colors = SliderDefaults.colors(thumbColor = AccentAmber, activeTrackColor = AccentAmber)
        )

        Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 12.dp))

        // Chroma Key (Green Screen)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Chroma Key (Green Screen)", color = Color.White, modifier = Modifier.weight(1f))
            Switch(
                checked = chromaEnabled,
                onCheckedChange = {
                    chromaEnabled = it
                    val newEffects = clip.effects.filterNot { it is EffectConfig.ChromaKey }.toMutableList()
                    if (it) {
                        newEffects.add(EffectConfig.ChromaKey(similarity = similarity, despill = despill))
                    }
                    onUpdateEffects(newEffects, speed, volume)
                }
            )
        }

        if (chromaEnabled) {
            Text("Similarity: ${"%.2f".format(similarity)}", color = Color.LightGray)
            Slider(
                value = similarity,
                onValueChange = {
                    similarity = it
                    val newEffects = clip.effects.filterNot { it is EffectConfig.ChromaKey } +
                        EffectConfig.ChromaKey(similarity = similarity, despill = despill)
                    onUpdateEffects(newEffects, speed, volume)
                },
                valueRange = 0.1f..0.8f
            )

            Text("Despill Suppressor: ${"%.2f".format(despill)}", color = Color.LightGray)
            Slider(
                value = despill,
                onValueChange = {
                    despill = it
                    val newEffects = clip.effects.filterNot { it is EffectConfig.ChromaKey } +
                        EffectConfig.ChromaKey(similarity = similarity, despill = despill)
                    onUpdateEffects(newEffects, speed, volume)
                },
                valueRange = 0.0f..1.0f
            )
        }

        Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 12.dp))

        // Color Grading
        Text("Color Grading", style = MaterialTheme.typography.titleSmall, color = AccentAmber)

        Text("Brightness: ${"%.2f".format(brightness)}", color = Color.LightGray)
        Slider(
            value = brightness,
            onValueChange = {
                brightness = it
                val newEffects = clip.effects.filterNot { it is EffectConfig.ColorGrading } +
                    EffectConfig.ColorGrading(brightness = brightness, contrast = contrast, saturation = saturation)
                onUpdateEffects(newEffects, speed, volume)
            },
            valueRange = -0.5f..0.5f
        )

        Text("Contrast: ${"%.2f".format(contrast)}", color = Color.LightGray)
        Slider(
            value = contrast,
            onValueChange = {
                contrast = it
                val newEffects = clip.effects.filterNot { it is EffectConfig.ColorGrading } +
                    EffectConfig.ColorGrading(brightness = brightness, contrast = contrast, saturation = saturation)
                onUpdateEffects(newEffects, speed, volume)
            },
            valueRange = 0.5f..2.0f
        )

        Text("Saturation: ${"%.2f".format(saturation)}", color = Color.LightGray)
        Slider(
            value = saturation,
            onValueChange = {
                saturation = it
                val newEffects = clip.effects.filterNot { it is EffectConfig.ColorGrading } +
                    EffectConfig.ColorGrading(brightness = brightness, contrast = contrast, saturation = saturation)
                onUpdateEffects(newEffects, speed, volume)
            },
            valueRange = 0.0f..2.0f
        )
    }
}

