package com.librecuts.rust.engine

import java.io.InputStream
import java.util.UUID

object SrtParser {

    data class SubtitleEntry(
        val index: Int,
        val startMs: Long,
        val endMs: Long,
        val text: String
    )

    fun parse(inputStream: InputStream): List<SubtitleEntry> {
        val entries = mutableListOf<SubtitleEntry>()
        val lines = inputStream.bufferedReader().readLines()

        var currentIndex = 0
        var currentStartMs = 0L
        var currentEndMs = 0L
        val currentText = StringBuilder()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                if (currentText.isNotEmpty() && currentEndMs > currentStartMs) {
                    entries.add(
                        SubtitleEntry(
                            index = currentIndex,
                            startMs = currentStartMs,
                            endMs = currentEndMs,
                            text = currentText.toString().trim()
                        )
                    )
                    currentText.clear()
                }
                continue
            }

            if (trimmed.matches(Regex("^\\d+$"))) {
                currentIndex = trimmed.toIntOrNull() ?: 0
            } else if (trimmed.contains("-->")) {
                val parts = trimmed.split("-->").map { it.trim() }
                if (parts.size == 2) {
                    currentStartMs = parseTimestampToMs(parts[0])
                    currentEndMs = parseTimestampToMs(parts[1])
                }
            } else {
                if (currentText.isNotEmpty()) currentText.append(" ")
                currentText.append(trimmed)
            }
        }

        // Catch the last entry if no trailing blank line
        if (currentText.isNotEmpty() && currentEndMs > currentStartMs) {
            entries.add(
                SubtitleEntry(
                    index = currentIndex,
                    startMs = currentStartMs,
                    endMs = currentEndMs,
                    text = currentText.toString().trim()
                )
            )
        }

        return entries
    }

    fun parseTimestampToMs(timestamp: String): Long {
        // Format: 00:01:23,456 or 00:01:23.456
        val clean = timestamp.replace(',', '.')
        val parts = clean.split(":")
        if (parts.size == 3) {
            val hours = parts[0].toLongOrNull() ?: 0L
            val minutes = parts[1].toLongOrNull() ?: 0L
            val secParts = parts[2].split(".")
            val seconds = secParts.getOrNull(0)?.toLongOrNull() ?: 0L
            val millis = secParts.getOrNull(1)?.padEnd(3, '0')?.take(3)?.toLongOrNull() ?: 0L

            return (hours * 3600_000L) + (minutes * 60_000L) + (seconds * 1000L) + millis
        }
        return 0L
    }

    fun toClips(entries: List<SubtitleEntry>): List<Clip> {
        return entries.map { entry ->
            Clip(
                id = UUID.randomUUID().toString(),
                file_path = "subtitle://${entry.index}",
                source_start_ms = 0L,
                source_end_ms = entry.endMs - entry.startMs,
                timeline_start_ms = entry.startMs,
                effects = listOf(
                    EffectConfig.TextOverlay(
                        text = entry.text,
                        font_size = 22f,
                        x = 0.5f,
                        y = 0.85f,
                        color_rgba = 0xFFFFFFFFL
                    )
                )
            )
        }
    }
}
