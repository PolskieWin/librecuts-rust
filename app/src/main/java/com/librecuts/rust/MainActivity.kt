package com.librecuts.rust

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.librecuts.rust.engine.Project
import com.librecuts.rust.engine.ProjectRepository
import com.librecuts.rust.ui.screens.EditorScreen
import com.librecuts.rust.ui.screens.ProjectsScreen
import com.librecuts.rust.ui.screens.SettingsScreen
import com.librecuts.rust.ui.theme.AppLanguage
import com.librecuts.rust.ui.theme.AppThemeMode
import com.librecuts.rust.ui.theme.LibreCutRustTheme
import com.librecuts.rust.ui.theme.LocalAppLanguage
import com.librecuts.rust.ui.theme.LocalAppStrings
import com.librecuts.rust.ui.theme.getAppStrings
import kotlinx.coroutines.launch

enum class MainScreen {
    PROJECTS,
    SETTINGS,
    EDITOR
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var currentScreen by remember { mutableStateOf(MainScreen.PROJECTS) }
            var themeMode by remember { mutableStateOf(AppThemeMode.PURE_BLACK) }
            var appLanguage by remember { mutableStateOf(AppLanguage.ENGLISH) }
            var activeProject by remember { mutableStateOf<Project?>(null) }
            val projectRepo = remember { ProjectRepository(this) }
            val scope = rememberCoroutineScope()

            val currentStrings = remember(appLanguage) { getAppStrings(appLanguage) }

            CompositionLocalProvider(
                LocalAppLanguage provides appLanguage,
                LocalAppStrings provides currentStrings
            ) {
                LibreCutRustTheme(themeMode = themeMode) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        when (currentScreen) {
                            MainScreen.SETTINGS -> {
                                BackHandler {
                                    currentScreen = MainScreen.PROJECTS
                                }
                                SettingsScreen(
                                    currentTheme = themeMode,
                                    onThemeChanged = { themeMode = it },
                                    currentLanguage = appLanguage,
                                    onLanguageChanged = { appLanguage = it },
                                    onBack = { currentScreen = MainScreen.PROJECTS }
                                )
                            }
                            MainScreen.PROJECTS -> {
                                ProjectsScreen(
                                    currentTheme = themeMode,
                                    onThemeChanged = { themeMode = it },
                                    onOpenProject = { project ->
                                        activeProject = project
                                        currentScreen = MainScreen.EDITOR
                                    },
                                    onOpenSettings = {
                                        currentScreen = MainScreen.SETTINGS
                                    }
                                )
                            }
                            MainScreen.EDITOR -> {
                                val project = activeProject
                                if (project != null) {
                                    BackHandler {
                                        currentScreen = MainScreen.PROJECTS
                                    }
                                    EditorScreen(
                                        project = project,
                                        currentTheme = themeMode,
                                        onThemeChanged = { themeMode = it },
                                        onSaveProject = { updatedProject ->
                                            scope.launch {
                                                projectRepo.saveProject(updatedProject)
                                                activeProject = updatedProject
                                            }
                                        },
                                        onBackToProjects = {
                                            currentScreen = MainScreen.PROJECTS
                                        }
                                    )
                                } else {
                                    currentScreen = MainScreen.PROJECTS
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
