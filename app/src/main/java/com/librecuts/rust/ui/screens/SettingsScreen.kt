package com.librecuts.rust.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.librecuts.rust.ui.components.rememberHapticHelper
import com.librecuts.rust.ui.theme.AppLanguage
import com.librecuts.rust.ui.theme.AppThemeMode
import com.librecuts.rust.ui.theme.LocalAppLanguage
import com.librecuts.rust.ui.theme.LocalAppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentTheme: AppThemeMode,
    onThemeChanged: (AppThemeMode) -> Unit,
    currentLanguage: AppLanguage,
    onLanguageChanged: (AppLanguage) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val haptics = rememberHapticHelper()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        haptics.tick()
                        onBack()
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = strings.back,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                title = {
                    Text(
                        text = strings.settingsTitle,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Section 1: Appearance & Theme
            SectionHeader(title = strings.appearanceSection, icon = Icons.Default.Palette)

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    ThemeOptionRow(
                        title = strings.themeMaterialYou,
                        subtitle = "Adaptive colors based on system wallpaper",
                        selected = currentTheme == AppThemeMode.MATERIAL_YOU_AUTO,
                        onSelect = {
                            haptics.tick()
                            onThemeChanged(AppThemeMode.MATERIAL_YOU_AUTO)
                        }
                    )
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
                    ThemeOptionRow(
                        title = strings.themeLight,
                        subtitle = "Clean high-contrast daytime interface",
                        selected = currentTheme == AppThemeMode.LIGHT,
                        onSelect = {
                            haptics.tick()
                            onThemeChanged(AppThemeMode.LIGHT)
                        }
                    )
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
                    ThemeOptionRow(
                        title = strings.themeDark,
                        subtitle = "Standard dark grey theme",
                        selected = currentTheme == AppThemeMode.DARK,
                        onSelect = {
                            haptics.tick()
                            onThemeChanged(AppThemeMode.DARK)
                        }
                    )
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
                    ThemeOptionRow(
                        title = strings.themePureBlack,
                        subtitle = "True 0 IRE black for AMOLED battery savings",
                        selected = currentTheme == AppThemeMode.PURE_BLACK,
                        onSelect = {
                            haptics.tick()
                            onThemeChanged(AppThemeMode.PURE_BLACK)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 2: Language Switcher (Only English, Spanish, Polish)
            SectionHeader(title = strings.languageSection, icon = Icons.Default.Language)

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    AppLanguage.values().forEachIndexed { index, lang ->
                        LanguageOptionRow(
                            language = lang,
                            selected = currentLanguage == lang,
                            onSelect = {
                                haptics.tick()
                                onLanguageChanged(lang)
                            }
                        )
                        if (index < AppLanguage.values().size - 1) {
                            Divider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 3: About the Project
            SectionHeader(title = strings.aboutSection, icon = Icons.Default.Info)

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = strings.aboutTitle,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = strings.versionLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = strings.aboutDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            haptics.tick()
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/PolskieWin/librecuts-rust"))
                            context.startActivity(browserIntent)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.openGitHub)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = strings.madeBy,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = strings.builtWithAi,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ThemeOptionRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        RadioButton(
            selected = selected,
            onClick = onSelect
        )
    }
}

@Composable
private fun LanguageOptionRow(
    language: AppLanguage,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = language.displayName,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = language.code.uppercase(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        RadioButton(
            selected = selected,
            onClick = onSelect
        )
    }
}
