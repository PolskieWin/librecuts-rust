package com.librecuts.rust.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.librecuts.rust.engine.AnimationType
import com.librecuts.rust.engine.Clip
import com.librecuts.rust.engine.EffectConfig
import com.librecuts.rust.engine.FontManager
import com.librecuts.rust.engine.SpeedCurveType
import com.librecuts.rust.engine.TransitionType
import com.librecuts.rust.ui.components.rememberHapticHelper
import com.librecuts.rust.ui.theme.LocalAppStrings
import kotlinx.coroutines.launch

@Composable
fun EffectsSheet(
    clip: Clip,
    onUpdateClip: (
        newEffects: List<EffectConfig>,
        speed: Float,
        speedCurve: SpeedCurveType,
        volume: Float,
        transition: TransitionType,
        animation: AnimationType
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings = LocalAppStrings.current
    val haptics = rememberHapticHelper()
    val fontManager = remember { FontManager(context) }

    var selectedTab by remember(clip.id) {
        mutableIntStateOf(if (clip.effects.any { it is EffectConfig.TextOverlay }) 6 else 0)
    }
    val tabTitles = listOf("Adjust", "Animations", "Transitions", "Speed & Curve", "Crop", "Mask", "Text & Fonts")

    // Playback Speed & Speed Curve
    var speed by remember(clip.id) { mutableFloatStateOf(clip.speed) }
    var speedCurve by remember(clip.id) { mutableStateOf(clip.speedCurve) }
    var volume by remember(clip.id) { mutableFloatStateOf(clip.volume) }
    var transition by remember(clip.id) { mutableStateOf(clip.transition) }
    var animation by remember(clip.id) { mutableStateOf(clip.animation) }

    // Background Removal (Auto Cutout without Green Screen)
    val existingBgRemoval = clip.effects.find { it is EffectConfig.BackgroundRemoval } as? EffectConfig.BackgroundRemoval
    var bgRemovalEnabled by remember(clip.id) { mutableStateOf(existingBgRemoval != null) }
    var bgRemovalThreshold by remember(clip.id) { mutableFloatStateOf(existingBgRemoval?.threshold ?: 0.45f) }
    var bgRemovalFeather by remember(clip.id) { mutableFloatStateOf(existingBgRemoval?.feather ?: 0.08f) }
    var bgRemovalInvert by remember(clip.id) { mutableStateOf(existingBgRemoval?.invert ?: false) }

    // Chroma key states
    val existingChroma = clip.effects.find { it is EffectConfig.ChromaKey } as? EffectConfig.ChromaKey
    var chromaEnabled by remember(clip.id) {
        mutableStateOf(existingChroma != null)
    }
    var similarity by remember(clip.id) { mutableFloatStateOf(existingChroma?.similarity ?: 0.35f) }
    var despill by remember(clip.id) { mutableFloatStateOf(existingChroma?.despill ?: 0.5f) }

    // Shape Masking states
    val existingMask = clip.effects.find { it is EffectConfig.ShapeMask } as? EffectConfig.ShapeMask
    var maskEnabled by remember(clip.id) { mutableStateOf(existingMask != null) }
    var maskShape by remember(clip.id) { mutableStateOf(existingMask?.shape ?: "circle") }
    var maskSize by remember(clip.id) { mutableFloatStateOf(existingMask?.size_x ?: 0.4f) }
    var maskFeather by remember(clip.id) { mutableFloatStateOf(existingMask?.feather ?: 0.05f) }
    var maskInvert by remember(clip.id) { mutableStateOf(existingMask?.invert ?: false) }

    // Color grading states
    val existingColor = clip.effects.find { it is EffectConfig.ColorGrading } as? EffectConfig.ColorGrading
    var brightness by remember(clip.id) { mutableFloatStateOf(existingColor?.brightness ?: 0.0f) }
    var contrast by remember(clip.id) { mutableFloatStateOf(existingColor?.contrast ?: 1.0f) }
    var saturation by remember(clip.id) { mutableFloatStateOf(existingColor?.saturation ?: 1.0f) }

    // Creative Filter state
    val existingFilter = clip.effects.find { it is EffectConfig.Filter } as? EffectConfig.Filter
    var activeFilter by remember(clip.id) { mutableStateOf(existingFilter?.filter_type ?: "none") }
    var filterIntensity by remember(clip.id) { mutableFloatStateOf(existingFilter?.intensity ?: 1.0f) }

    // Transform & Crop states
    val existingTransform = clip.effects.find { it is EffectConfig.Transform } as? EffectConfig.Transform
    var cropTop by remember(clip.id) { mutableFloatStateOf(existingTransform?.crop_top ?: 0f) }
    var cropBottom by remember(clip.id) { mutableFloatStateOf(existingTransform?.crop_bottom ?: 0f) }
    var cropLeft by remember(clip.id) { mutableFloatStateOf(existingTransform?.crop_left ?: 0f) }
    var cropRight by remember(clip.id) { mutableFloatStateOf(existingTransform?.crop_right ?: 0f) }
    var flipH by remember(clip.id) { mutableStateOf(existingTransform?.flip_h ?: false) }
    var flipV by remember(clip.id) { mutableStateOf(existingTransform?.flip_v ?: false) }
    var mediaScale by remember(clip.id) { mutableFloatStateOf(existingTransform?.scale ?: 1.0f) }

    // Text & Custom Font states
    val existingText = clip.effects.find { it is EffectConfig.TextOverlay } as? EffectConfig.TextOverlay
    var textOverlayEnabled by remember(clip.id) { mutableStateOf(existingText != null) }
    var overlayText by remember(clip.id) { mutableStateOf(existingText?.text ?: "Subtitles") }
    var overlayFontSize by remember(clip.id) { mutableFloatStateOf(existingText?.font_size ?: 24f) }
    var selectedFontPath by remember(clip.id) { mutableStateOf(existingText?.custom_font_path) }
    var textColorRgba by remember(clip.id) { mutableLongStateOf(existingText?.color_rgba ?: 0xFFFFFFFFL) }
    var textStrokeWidth by remember(clip.id) { mutableFloatStateOf(existingText?.stroke_width ?: 0f) }
    var textStrokeColorRgba by remember(clip.id) { mutableLongStateOf(existingText?.stroke_color_rgba ?: 0xFF000000L) }
    var availableFonts by remember { mutableStateOf(fontManager.getAvailableFonts()) }

    val fontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val opt = fontManager.importFontFromUri(it)
                if (opt != null) {
                    availableFonts = fontManager.getAvailableFonts()
                    selectedFontPath = opt.path
                    syncAll()
                    haptics.snap()
                    Toast.makeText(context, "Font ${opt.name} imported!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to import font", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun syncAll() {
        val newEffects = mutableListOf<EffectConfig>()
        if (bgRemovalEnabled) {
            newEffects.add(
                EffectConfig.BackgroundRemoval(
                    threshold = bgRemovalThreshold,
                    feather = bgRemovalFeather,
                    invert = bgRemovalInvert
                )
            )
        }
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
        if (cropTop > 0f || cropBottom > 0f || cropLeft > 0f || cropRight > 0f || flipH || flipV || mediaScale != 1.0f) {
            newEffects.add(
                EffectConfig.Transform(
                    crop_left = cropLeft,
                    crop_top = cropTop,
                    crop_right = cropRight,
                    crop_bottom = cropBottom,
                    flip_h = flipH,
                    flip_v = flipV,
                    scale = mediaScale
                )
            )
        }
        if (brightness != 0f || contrast != 1f || saturation != 1f) {
            newEffects.add(EffectConfig.ColorGrading(brightness = brightness, contrast = contrast, saturation = saturation))
        }
        if (activeFilter != "none") {
            newEffects.add(EffectConfig.Filter(filter_type = activeFilter, intensity = filterIntensity))
        }
        if (textOverlayEnabled) {
            newEffects.add(
                EffectConfig.TextOverlay(
                    text = overlayText,
                    font_size = overlayFontSize,
                    color_rgba = textColorRgba,
                    custom_font_path = selectedFontPath,
                    stroke_width = textStrokeWidth,
                    stroke_color_rgba = textStrokeColorRgba
                )
            )
        }
        onUpdateClip(newEffects, speed, speedCurve, volume, transition, animation)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = strings.clipAdjustments,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Organized Scrollable Tab Row
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 8.dp
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = {
                        haptics.tick()
                        selectedTab = index
                    },
                    text = { Text(title, fontSize = 11.sp, maxLines = 1, softWrap = false) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            // TAB 0: Adjust (Audio Boost, Cutout/BG Removal, Chroma Key, Color Grading, Creative Filters)
            0 -> {
                // Audio Volume Booster
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${strings.audioBoost}: ${(volume * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                }
                Slider(
                    value = volume,
                    onValueChange = { volume = it; syncAll() },
                    valueRange = 0.0f..2.0f,
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                )

                Divider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 10.dp))

                // Background Removal (Auto Cutout without green screen)
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.backgroundRemoval, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.primary)
                        Text(strings.autoCutoutDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Switch(checked = bgRemovalEnabled, onCheckedChange = { haptics.tick(); bgRemovalEnabled = it; syncAll() })
                }
                if (bgRemovalEnabled) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("${strings.cutoutSensitivity}: ${(bgRemovalThreshold * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Slider(value = bgRemovalThreshold, onValueChange = { bgRemovalThreshold = it; syncAll() }, valueRange = 0.1f..0.9f)

                    Text("${strings.edgeFeather}: ${(bgRemovalFeather * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Slider(value = bgRemovalFeather, onValueChange = { bgRemovalFeather = it; syncAll() }, valueRange = 0.01f..0.25f)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(strings.invertCutout, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        Switch(checked = bgRemovalInvert, onCheckedChange = { bgRemovalInvert = it; syncAll() })
                    }
                }

                Divider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 10.dp))

                // Chroma Key
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.chromaKey, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                        Text(strings.simdChromaDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Switch(checked = chromaEnabled, onCheckedChange = { haptics.tick(); chromaEnabled = it; syncAll() })
                }
                if (chromaEnabled) {
                    Text("${strings.similarity}: ${"%.2f".format(similarity)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Slider(value = similarity, onValueChange = { similarity = it; syncAll() }, valueRange = 0.1f..0.8f)

                    Text("${strings.despill}: ${"%.2f".format(despill)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Slider(value = despill, onValueChange = { despill = it; syncAll() }, valueRange = 0.0f..1.0f)
                }

                Divider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 10.dp))

                // Creative Filters
                Text(strings.filters, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(6.dp))
                val filterList = listOf(
                    "none" to "Normal",
                    "motion_blur" to "Motion Blur",
                    "cinematic" to "Cinematic",
                    "cyberpunk" to "Cyberpunk",
                    "vintage" to "Vintage",
                    "vivid" to "Vivid",
                    "noir" to "Noir B&W",
                    "grayscale" to "Grayscale",
                    "sepia" to "Sepia",
                    "vignette" to "Vignette"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    filterList.take(5).forEach { (type, label) ->
                        FilterChip(
                            selected = activeFilter == type,
                            onClick = { haptics.tick(); activeFilter = type; syncAll() },
                            label = { Text(label, fontSize = 11.sp, maxLines = 1, softWrap = false) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    filterList.drop(5).forEach { (type, label) ->
                        FilterChip(
                            selected = activeFilter == type,
                            onClick = { haptics.tick(); activeFilter = type; syncAll() },
                            label = { Text(label, fontSize = 11.sp, maxLines = 1, softWrap = false) }
                        )
                    }
                }
                if (activeFilter != "none") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Filter Intensity: ${(filterIntensity * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Slider(value = filterIntensity, onValueChange = { filterIntensity = it; syncAll() }, valueRange = 0.1f..1.0f)
                }

                Divider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 10.dp))

                // Color Grading
                Text(strings.colorGrading, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                Text("${strings.brightness}: ${"%.2f".format(brightness)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Slider(value = brightness, onValueChange = { brightness = it; syncAll() }, valueRange = -0.5f..0.5f)

                Text("${strings.contrast}: ${"%.2f".format(contrast)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Slider(value = contrast, onValueChange = { contrast = it; syncAll() }, valueRange = 0.5f..2.0f)

                Text("${strings.saturation}: ${"%.2f".format(saturation)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Slider(value = saturation, onValueChange = { saturation = it; syncAll() }, valueRange = 0.0f..2.0f)
            }

            // TAB 1: Animations (In, Out, Combo - CapCut style)
            1 -> {
                Text(strings.animations, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(4.dp))
                Text(strings.animationDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(12.dp))

                listOf("In", "Out", "Combo").forEach { cat ->
                    Text(
                        text = "$cat ${strings.animations}",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val catAnims = AnimationType.values().filter { it.category == cat }
                    catAnims.forEach { anim ->
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (animation == anim) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(anim.label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Button(
                                    onClick = {
                                        haptics.snap()
                                        animation = if (animation == anim) AnimationType.None else anim
                                        syncAll()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (animation == anim) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (animation == anim) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (animation == anim) "Active" else "Select",
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // TAB 2: Pre-made Transitions
            2 -> {
                Text(strings.transitions, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(4.dp))
                Text(strings.transitionDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(12.dp))

                TransitionType.values().forEach { trans ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (transition == trans) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Animation, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(trans.displayName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface)
                            }
                            Button(
                                onClick = {
                                    haptics.snap()
                                    transition = trans
                                    syncAll()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (transition == trans) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (transition == trans) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (transition == trans) "Active" else "Select",
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            // TAB 3: Speed & Speed Curves
            3 -> {
                Text(strings.playbackSpeed, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(8.dp))

                // Normal Speed Slider
                Text("${strings.speedNormal}: ${"%.2f".format(speed)}x", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                Slider(
                    value = speed,
                    onValueChange = {
                        speed = it
                        speedCurve = SpeedCurveType.NORMAL
                        syncAll()
                    },
                    valueRange = 0.25f..4.0f,
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                )

                Spacer(modifier = Modifier.height(14.dp))
                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(modifier = Modifier.height(14.dp))

                // Speed Curves (Speed Ramping)
                Text(strings.speedCurve, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(strings.speedCurveDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(8.dp))

                // Visual Speed Curve & Keyframe Easing Graph
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F172A))
                        .padding(8.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // Grid lines
                        val gridColor = Color.White.copy(alpha = 0.08f)
                        drawLine(gridColor, Offset(0f, h * 0.25f), Offset(w, h * 0.25f), 1f)
                        drawLine(gridColor, Offset(0f, h * 0.5f), Offset(w, h * 0.5f), 1f)
                        drawLine(gridColor, Offset(0f, h * 0.75f), Offset(w, h * 0.75f), 1f)
                        drawLine(gridColor, Offset(w * 0.5f, 0f), Offset(w * 0.5f, h), 1f)

                        val path = Path()
                        when (speedCurve) {
                            SpeedCurveType.NORMAL -> {
                                path.moveTo(0f, h * 0.5f)
                                path.lineTo(w, h * 0.5f)
                            }
                            SpeedCurveType.MONTAGE -> {
                                path.moveTo(0f, h * 0.2f)
                                path.cubicTo(w * 0.3f, h * 0.2f, w * 0.35f, h * 0.85f, w * 0.5f, h * 0.85f)
                                path.cubicTo(w * 0.65f, h * 0.85f, w * 0.7f, h * 0.2f, w, h * 0.2f)
                            }
                            SpeedCurveType.HERO_MOMENT -> {
                                path.moveTo(0f, h * 0.65f)
                                path.cubicTo(w * 0.2f, h * 0.15f, w * 0.35f, h * 0.15f, w * 0.5f, h * 0.9f)
                                path.cubicTo(w * 0.65f, h * 0.9f, w * 0.8f, h * 0.65f, w, h * 0.65f)
                            }
                            SpeedCurveType.BULLET_TIME -> {
                                path.moveTo(0f, h * 0.5f)
                                path.lineTo(w * 0.25f, h * 0.5f)
                                path.lineTo(w * 0.3f, h * 0.92f)
                                path.lineTo(w * 0.7f, h * 0.92f)
                                path.lineTo(w * 0.75f, h * 0.5f)
                                path.lineTo(w, h * 0.5f)
                            }
                            SpeedCurveType.FLASH_IN -> {
                                path.moveTo(0f, h * 0.1f)
                                path.cubicTo(w * 0.15f, h * 0.15f, w * 0.35f, h * 0.5f, w, h * 0.5f)
                            }
                        }
                        drawPath(path, color = Color(0xFF38BDF8), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f))
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                SpeedCurveType.values().forEach { curve ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (speedCurve == curve) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(curve.label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface)
                                val desc = when (curve) {
                                    SpeedCurveType.NORMAL -> "Constant playback speed"
                                    SpeedCurveType.MONTAGE -> "Fast start, 0.4x slow-mo middle, fast finish"
                                    SpeedCurveType.HERO_MOMENT -> "2x entry ramp into 0.25x bullet impact"
                                    SpeedCurveType.BULLET_TIME -> "Instant 0.2x frozen motion"
                                    SpeedCurveType.FLASH_IN -> "4x rapid acceleration into normal speed"
                                }
                                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                            Button(
                                onClick = {
                                    haptics.snap()
                                    speedCurve = curve
                                    syncAll()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (speedCurve == curve) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (speedCurve == curve) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (speedCurve == curve) "Active" else "Apply",
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            // TAB 4: Crop & Transform
            4 -> {
                Text(strings.crop, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(8.dp))

                // Aspect Presets
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Freeform", "16:9", "9:16", "1:1", "4:5").forEach { preset ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                haptics.tick()
                                when (preset) {
                                    "16:9" -> { cropTop = 0.05f; cropBottom = 0.05f; cropLeft = 0f; cropRight = 0f }
                                    "9:16" -> { cropTop = 0f; cropBottom = 0f; cropLeft = 0.2f; cropRight = 0.2f }
                                    "1:1" -> { cropTop = 0f; cropBottom = 0f; cropLeft = 0.15f; cropRight = 0.15f }
                                    "4:5" -> { cropTop = 0f; cropBottom = 0f; cropLeft = 0.1f; cropRight = 0.1f }
                                    else -> { cropTop = 0f; cropBottom = 0f; cropLeft = 0f; cropRight = 0f }
                                }
                                syncAll()
                            },
                            label = { Text(preset, fontSize = 11.sp, maxLines = 1, softWrap = false) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("${strings.cropTop}: ${(cropTop * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Slider(value = cropTop, onValueChange = { cropTop = it; syncAll() }, valueRange = 0.0f..0.4f)

                Text("${strings.cropBottom}: ${(cropBottom * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Slider(value = cropBottom, onValueChange = { cropBottom = it; syncAll() }, valueRange = 0.0f..0.4f)

                Text("${strings.cropLeft}: ${(cropLeft * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Slider(value = cropLeft, onValueChange = { cropLeft = it; syncAll() }, valueRange = 0.0f..0.4f)

                Text("${strings.cropRight}: ${(cropRight * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Slider(value = cropRight, onValueChange = { cropRight = it; syncAll() }, valueRange = 0.0f..0.4f)

                Spacer(modifier = Modifier.height(10.dp))
                Text("Resize Scale: ${String.format(java.util.Locale.US, "%.2fx", mediaScale)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                Slider(value = mediaScale, onValueChange = { mediaScale = it; syncAll() }, valueRange = 0.2f..3.0f)

                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(strings.flipHorizontal, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    Switch(checked = flipH, onCheckedChange = { flipH = it; syncAll() })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(strings.flipVertical, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    Switch(checked = flipV, onCheckedChange = { flipV = it; syncAll() })
                }
            }

            // TAB 5: Mask (Shape Masking)
            5 -> {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.shapeMask, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                        Text(strings.shapeMaskDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Switch(checked = maskEnabled, onCheckedChange = { haptics.tick(); maskEnabled = it; syncAll() })
                }

                if (maskEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            Triple("circle", "Circle", Icons.Default.RadioButtonUnchecked),
                            Triple("rectangle", "Rectangle", Icons.Default.CropSquare),
                            Triple("heart", "Heart", Icons.Default.Favorite),
                            Triple("linear", "Wipe", Icons.Default.LinearScale)
                        ).forEach { (shapeKey, label, icon) ->
                            FilterChip(
                                selected = maskShape == shapeKey,
                                onClick = { haptics.tick(); maskShape = shapeKey; syncAll() },
                                leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                label = { Text(label, fontSize = 11.sp, maxLines = 1, softWrap = false) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${strings.maskSize}: ${"%.2f".format(maskSize)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Slider(value = maskSize, onValueChange = { maskSize = it; syncAll() }, valueRange = 0.1f..0.9f)

                    Text("${strings.maskFeather}: ${"%.2f".format(maskFeather)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Slider(value = maskFeather, onValueChange = { maskFeather = it; syncAll() }, valueRange = 0.0f..0.3f)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(strings.maskInvert, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        Switch(checked = maskInvert, onCheckedChange = { maskInvert = it; syncAll() })
                    }
                }
            }

            // TAB 6: Text & Custom Font Import
            6 -> {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.textOverlay, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                        Text(strings.textOverlayDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Switch(checked = textOverlayEnabled, onCheckedChange = { haptics.tick(); textOverlayEnabled = it; syncAll() })
                }

                if (textOverlayEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = overlayText,
                        onValueChange = { overlayText = it; syncAll() },
                        label = { Text(strings.overlayTextLabel) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${strings.fontSizeLabel}: ${overlayFontSize.toInt()}sp", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Slider(value = overlayFontSize, onValueChange = { overlayFontSize = it; syncAll() }, valueRange = 14f..64f)

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Text Color", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))
                    val colorPalette = listOf(
                        0xFFFFFFFFL to "White",
                        0xFFFFEB3BL to "Yellow",
                        0xFFFF5722L to "Orange",
                        0xFFF44336L to "Red",
                        0xFF4CAF50L to "Green",
                        0xFF00E5FFL to "Cyan",
                        0xFFE91E63L to "Pink",
                        0xFF0F172AL to "Dark"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        colorPalette.forEach { (cVal, _) ->
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(cVal))
                                    .border(
                                        width = if (textColorRgba == cVal) 3.dp else 1.dp,
                                        color = if (textColorRgba == cVal) MaterialTheme.colorScheme.primary else Color.Gray,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        haptics.tick()
                                        textColorRgba = cVal
                                        syncAll()
                                    }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Text Outline: ${textStrokeWidth.toInt()}px", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.primary)
                    Slider(value = textStrokeWidth, onValueChange = { textStrokeWidth = it; syncAll() }, valueRange = 0f..12f)
                    if (textStrokeWidth > 0f) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Outline Color", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.height(4.dp))
                        val outlinePalette = listOf(
                            0xFF000000L to "Black",
                            0xFFFFFFFFL to "White",
                            0xFFFF0000L to "Red",
                            0xFF0284C7L to "Blue",
                            0xFFFFD700L to "Gold"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            outlinePalette.forEach { (cVal, _) ->
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(Color(cVal))
                                        .border(
                                            width = if (textStrokeColorRgba == cVal) 3.dp else 1.dp,
                                            color = if (textStrokeColorRgba == cVal) MaterialTheme.colorScheme.primary else Color.Gray,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            haptics.tick()
                                            textStrokeColorRgba = cVal
                                            syncAll()
                                        }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.fontLabel, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.primary)
                        Button(
                            onClick = {
                                haptics.tick()
                                fontPickerLauncher.launch("*/*")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.FontDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.importFont, fontSize = 11.sp, maxLines = 1, softWrap = false)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Font Selection Chips
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        availableFonts.forEach { fontOpt ->
                            FilterChip(
                                selected = selectedFontPath == fontOpt.path,
                                onClick = {
                                    haptics.tick()
                                    selectedFontPath = fontOpt.path
                                    syncAll()
                                },
                                label = { Text(fontOpt.name, fontSize = 11.sp, maxLines = 1, softWrap = false) }
                            )
                        }
                    }
                                    syncAll()
                                },
                                label = { Text(fontOpt.name, fontSize = 11.sp, maxLines = 1, softWrap = false) }
                            )
                        }
                    }
                }
            }
        }
    }
}
