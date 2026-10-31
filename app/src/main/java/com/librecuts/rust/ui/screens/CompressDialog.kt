package com.librecuts.rust.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.DataSaverOn
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.librecuts.rust.engine.Composition
import com.librecuts.rust.engine.ExportConfig
import com.librecuts.rust.engine.ExportResolution
import com.librecuts.rust.engine.ExportState
import com.librecuts.rust.ui.components.rememberHapticHelper
import com.librecuts.rust.ui.theme.LocalAppStrings

enum class CompressionPreset(val label: String, val resolution: ExportResolution, val bitrateMbps: Int) {
    SMALL("Small (Discord / WhatsApp)", ExportResolution.R720P, 1),
    MEDIUM("Medium (Web / Social)", ExportResolution.R1080P, 3),
    HIGH("High Quality", ExportResolution.R1080P, 8)
}

@Composable
fun CompressDialog(
    composition: Composition,
    exportState: ExportState,
    onStartCompress: (resolution: ExportResolution, bitrateMbps: Int) -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val haptics = rememberHapticHelper()
    var selectedPreset by remember { mutableStateOf(CompressionPreset.SMALL) }

    val estimatedBytes = remember(selectedPreset, composition.totalDurationMs) {
        ExportConfig.estimateFileSizeBytes(
            durationMs = composition.totalDurationMs,
            videoBitrateBps = selectedPreset.bitrateMbps * 1_000_000,
            audioBitrateBps = 128_000
        )
    }

    AlertDialog(
        onDismissRequest = {
            if (exportState !is ExportState.Exporting) onDismiss()
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Compress, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = strings.compressVideo,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                when (exportState) {
                    is ExportState.Idle, is ExportState.Cancelled -> {
                        Text(
                            text = strings.chooseCompressTarget,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        CompressionPreset.values().forEach { preset ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedPreset == preset) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        haptics.tick()
                                        selectedPreset = preset
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        val presetLabel = when (preset) {
                                            CompressionPreset.SMALL -> strings.compressionPresetDiscord
                                            CompressionPreset.MEDIUM -> strings.compressionPresetWeb
                                            CompressionPreset.HIGH -> strings.compressionPresetHigh
                                        }
                                        Text(
                                            text = presetLabel,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${preset.resolution.label} • ${preset.bitrateMbps} Mbps",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                    RadioButton(
                                        selected = selectedPreset == preset,
                                        onClick = {
                                            haptics.tick()
                                            selectedPreset = preset
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Estimated File Size Card
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = strings.estimatedSize,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "~${ExportConfig.formatFileSize(estimatedBytes)}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    is ExportState.Exporting -> {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = strings.compressingProgress,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            LinearProgressIndicator(
                                progress = { exportState.progress },
                                modifier = Modifier.fillMaxWidth().height(8.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "${(exportState.progress * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    is ExportState.Success -> {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = strings.compressionFinished,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = exportState.outputPath,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                maxLines = 2
                            )
                        }
                    }

                    is ExportState.Error -> {
                        Text(
                            text = "${strings.compressionFailed}: ${exportState.message}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            when (exportState) {
                is ExportState.Idle, is ExportState.Cancelled -> {
                    Button(
                        onClick = {
                            haptics.snap()
                            onStartCompress(selectedPreset.resolution, selectedPreset.bitrateMbps)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(strings.compress, fontSize = 12.sp, maxLines = 1, softWrap = false)
                    }
                }
                is ExportState.Exporting -> {
                    OutlinedButton(
                        onClick = onCancel,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(strings.cancel, fontSize = 12.sp, maxLines = 1, softWrap = false)
                    }
                }
                is ExportState.Success, is ExportState.Error -> {
                    Button(
                        onClick = onDismiss,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(strings.done, fontSize = 12.sp, maxLines = 1, softWrap = false)
                    }
                }
            }
        },
        dismissButton = {
            if (exportState is ExportState.Idle || exportState is ExportState.Cancelled) {
                OutlinedButton(
                    onClick = onDismiss,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(strings.cancel, fontSize = 12.sp, maxLines = 1, softWrap = false)
                }
            }
        }
    )
}

