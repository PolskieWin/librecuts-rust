package com.librecuts.rust.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.librecuts.rust.plugin.LibreCutPlugin
import com.librecuts.rust.plugin.ParamType
import com.librecuts.rust.plugin.PluginCategory
import com.librecuts.rust.plugin.PluginManager
import com.librecuts.rust.ui.components.rememberHapticHelper
import com.librecuts.rust.ui.theme.LocalAppStrings
import kotlinx.coroutines.launch

@Composable
fun PluginManagerSheet(
    pluginManager: PluginManager,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = rememberHapticHelper()
    val strings = LocalAppStrings.current
    val plugins by pluginManager.plugins.collectAsState()

    var selectedCategory by remember { mutableStateOf<PluginCategory?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val success = pluginManager.importPluginFromUri(it)
                if (success) {
                    haptics.split()
                    Toast.makeText(context, "Plugin imported successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Invalid .lcplugin file format", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val filteredPlugins = plugins.filter {
        selectedCategory == null || it.category == selectedCategory
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Extension,
                            contentDescription = "Plugins",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = strings.communityPluginsTitle,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = strings.communityPluginsSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Action Row: Import Button & Category Filter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    haptics.tick()
                    filePicker.launch("*/*")
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = strings.importPlugin, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.importPlugin, fontSize = 13.sp)
            }

            Text(
                text = "${filteredPlugins.count { it.enabled }} / ${filteredPlugins.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Category Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = {
                    haptics.tick()
                    selectedCategory = null
                },
                label = { Text("${strings.pluginCatAll} (${plugins.size})", fontSize = 12.sp) }
            )
            FilterChip(
                selected = selectedCategory == PluginCategory.EFFECT,
                onClick = {
                    haptics.tick()
                    selectedCategory = PluginCategory.EFFECT
                },
                leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp)) },
                label = { Text(strings.pluginCatEffects, fontSize = 12.sp) }
            )
            FilterChip(
                selected = selectedCategory == PluginCategory.FILTER,
                onClick = {
                    haptics.tick()
                    selectedCategory = PluginCategory.FILTER
                },
                leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(14.dp)) },
                label = { Text(strings.pluginCatFilters, fontSize = 12.sp) }
            )
            FilterChip(
                selected = selectedCategory == PluginCategory.AUDIO,
                onClick = {
                    haptics.tick()
                    selectedCategory = PluginCategory.AUDIO
                },
                leadingIcon = { Icon(Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(14.dp)) },
                label = { Text(strings.pluginCatAudio, fontSize = 12.sp) }
            )
            FilterChip(
                selected = selectedCategory == PluginCategory.UI_LAYOUT,
                onClick = {
                    haptics.tick()
                    selectedCategory = PluginCategory.UI_LAYOUT
                },
                leadingIcon = { Icon(Icons.Default.Dashboard, contentDescription = null, modifier = Modifier.size(14.dp)) },
                label = { Text(strings.pluginCatUiLayouts, fontSize = 12.sp) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Divider(color = MaterialTheme.colorScheme.surfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))

        // Plugin Items List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .height(380.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredPlugins, key = { it.id }) { plugin ->
                PluginItemCard(
                    plugin = plugin,
                    onToggle = { enabled ->
                        haptics.snap()
                        pluginManager.togglePlugin(plugin.id, enabled)
                    }
                )
            }
        }
    }
}

@Composable
private fun PluginItemCard(
    plugin: LibreCutPlugin,
    onToggle: (Boolean) -> Unit
) {
    val strings = LocalAppStrings.current
    val haptics = rememberHapticHelper()
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (plugin.enabled) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(if (plugin.enabled) Color(0xFF4CAF50) else Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = plugin.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "v${plugin.version}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "by ${plugin.author} • ${plugin.category.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Switch(
                    checked = plugin.enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = plugin.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )

            // Plugin Parameters (Tweakable Sliders)
            if (plugin.enabled && plugin.parameters.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Parameters",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = strings.pluginParameters,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                plugin.parameters.forEach { param ->
                    var paramVal by remember(param.id) { mutableStateOf(param.currentValue) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = param.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                        )
                        Text(
                            text = if (param.type == ParamType.TOGGLE) {
                                if (paramVal > 0.5f) "ON" else "OFF"
                            } else {
                                "%.2f".format(paramVal)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (param.type == ParamType.SLIDER) {
                        Slider(
                            value = paramVal,
                            onValueChange = {
                                paramVal = it
                                param.currentValue = it
                            },
                            valueRange = param.minValue..param.maxValue,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    } else if (param.type == ParamType.TOGGLE) {
                        Switch(
                            checked = paramVal > 0.5f,
                            onCheckedChange = {
                                val newVal = if (it) 1f else 0f
                                paramVal = newVal
                                param.currentValue = newVal
                                haptics.tick()
                            }
                        )
                    }
                }
            }
        }
    }
}

