package com.librecuts.rust

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.librecuts.rust.ui.theme.AppThemeMode
import com.librecuts.rust.ui.theme.LibreCutRustTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var themeMode by remember { mutableStateOf(AppThemeMode.PURE_BLACK) }
            var activeProject by remember { mutableStateOf<Project?>(null) }
            val projectRepo = remember { ProjectRepository(this) }
            val scope = rememberCoroutineScope()

            LibreCutRustTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val currentProject = activeProject
                    if (currentProject == null) {
                        ProjectsScreen(
                            currentTheme = themeMode,
                            onThemeChanged = { themeMode = it },
                            onOpenProject = { project -> activeProject = project }
                        )
                    } else {
                        BackHandler {
                            activeProject = null
                        }

                        EditorScreen(
                            project = currentProject,
                            currentTheme = themeMode,
                            onThemeChanged = { themeMode = it },
                            onSaveProject = { updatedProject ->
                                scope.launch {
                                    projectRepo.saveProject(updatedProject)
                                    activeProject = updatedProject
                                }
                            },
                            onBackToProjects = {
                                activeProject = null
                            }
                        )
                    }
                }
            }
        }
    }
}
