package com.librecuts.rust.engine

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Serializable
data class Project(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Untitled Project",
    val createdAt: Long = System.currentTimeMillis(),
    val lastModifiedAt: Long = System.currentTimeMillis(),
    val aspectRatio: String = "16:9", // "16:9", "9:16", "1:1", "4:5"
    val composition: Composition
) {
    val formattedLastModified: String
        get() {
            val sdf = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
            return sdf.format(Date(lastModifiedAt))
        }

    val totalDurationSeconds: Long
        get() = composition.totalDurationMs / 1000

    val totalClipCount: Int
        get() = composition.tracks.sumOf { it.clips.size }

    val firstClipUri: String?
        get() = composition.tracks
            .find { it.track_type == TrackType.Video }
            ?.clips
            ?.firstOrNull()
            ?.file_path
}

class ProjectRepository(private val context: Context) {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        prettyPrint = false
        isLenient = true
    }

    private val projectsDir: File
        get() = File(context.filesDir, "projects").apply { mkdirs() }

    suspend fun getAllProjects(): List<Project> = withContext(Dispatchers.IO) {
        val files = projectsDir.listFiles { _, name -> name.endsWith(".json") } ?: emptyArray()
        files.mapNotNull { file ->
            try {
                val content = file.readText()
                json.decodeFromString(Project.serializer(), content)
            } catch (e: Exception) {
                null
            }
        }.sortedByDescending { it.lastModifiedAt }
    }

    suspend fun getProject(id: String): Project? = withContext(Dispatchers.IO) {
        val file = File(projectsDir, "$id.json")
        if (!file.exists()) return@withContext null
        try {
            json.decodeFromString(Project.serializer(), file.readText())
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveProject(project: Project) = withContext(Dispatchers.IO) {
        val updated = project.copy(lastModifiedAt = System.currentTimeMillis())
        val file = File(projectsDir, "${updated.id}.json")
        file.writeText(json.encodeToString(Project.serializer(), updated))
    }

    suspend fun duplicateProject(id: String): Project? = withContext(Dispatchers.IO) {
        val original = getProject(id) ?: return@withContext null
        val copy = original.copy(
            id = UUID.randomUUID().toString(),
            title = "${original.title} (Copy)",
            createdAt = System.currentTimeMillis(),
            lastModifiedAt = System.currentTimeMillis()
        )
        saveProject(copy)
        copy
    }

    suspend fun renameProject(id: String, newTitle: String) = withContext(Dispatchers.IO) {
        val original = getProject(id) ?: return@withContext
        val updated = original.copy(
            title = newTitle,
            lastModifiedAt = System.currentTimeMillis()
        )
        saveProject(updated)
    }

    suspend fun deleteProject(id: String) = withContext(Dispatchers.IO) {
        val file = File(projectsDir, "$id.json")
        if (file.exists()) {
            file.delete()
        }
    }

    fun createNewProject(title: String = "New Project", aspectRatio: String = "16:9"): Project {
        val (width, height) = when (aspectRatio) {
            "9:16" -> Pair(1080, 1920) // Vertical (TikTok / Shorts)
            "1:1" -> Pair(1080, 1080)  // Square
            "4:5" -> Pair(1080, 1350)  // Portrait
            else -> Pair(1920, 1080)   // Landscape (16:9)
        }

        return Project(
            id = UUID.randomUUID().toString(),
            title = title,
            aspectRatio = aspectRatio,
            composition = Composition(
                width = width,
                height = height,
                tracks = listOf(
                    Track(id = "track_video", track_type = TrackType.Video),
                    Track(id = "track_audio", track_type = TrackType.Audio),
                    Track(id = "track_overlay", track_type = TrackType.Overlay)
                )
            )
        )
    }
}

