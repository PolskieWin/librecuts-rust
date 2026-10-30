package com.librecuts.rust.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.librecuts.rust.engine.ExportState

@Composable
fun ExportDialog(
    exportState: ExportState,
    onStartExport: (resolution: Pair<Int, Int>, fps: Int, bitrateMbps: Int, smartRender: Boolean) -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedResIndex by remember { mutableIntStateOf(1) } // Default 1080p
    var selectedFps by remember { mutableIntStateOf(30) }
    var smartRender by remember { mutableStateOf(true) }

    val resolutions = listOf(
        Pair(3840, 2160) to "4K UHD (2160p)",
        Pair(1920, 1080) to "Full HD (1080p)",
        Pair(1280, 720) to "HD (720p)"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Export Fast Video (Rust NDK)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))

                when (exportState) {
                    is ExportState.Exporting -> {
                        Text(
                            text = "Rendering: ${(exportState.progress * 100).toInt()}%",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { exportState.progress },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onCancel,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Cancel Export")
                        }
                    }
                    is ExportState.Success -> {
                        Text(
                            text = "Export Complete!\nSaved to Movies/LibreCut",
                            color = Color(0xFF4CAF50)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Done")
                        }
                    }
                    is ExportState.Error -> {
                        Text(
                            text = "Error: ${exportState.message}",
                            color = Color.Red
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Close")
                        }
                    }
                    ExportState.Idle -> {
                        // Settings Selection
                        Text("Target Resolution:", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        resolutions.forEachIndexed { idx, pair ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(pair.second, color = MaterialTheme.colorScheme.onSurface)
                                OutlinedButton(
                                    onClick = { selectedResIndex = idx },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = if (selectedResIndex == idx) MaterialTheme.colorScheme.primary else Color.Gray
                                    )
                                ) {
                                    Text(if (selectedResIndex == idx) "Selected" else "Select")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Smart Render Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Smart Render (Lossless)", color = MaterialTheme.colorScheme.onSurface)
                                Text("Zero-reencode stream copy when possible", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(
                                checked = smartRender,
                                onCheckedChange = { smartRender = it }
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(onClick = onDismiss) {
                                Text("Cancel")
                            }
                            Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                            Button(
                                onClick = {
                                    val res = resolutions[selectedResIndex].first
                                    onStartExport(res, selectedFps, 14, smartRender)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text("Start Export")
                            }
                        }
                    }
                }
            }
        }
    }
}
