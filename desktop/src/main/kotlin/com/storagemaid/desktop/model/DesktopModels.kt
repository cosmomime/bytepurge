package com.storagemaid.desktop.model

import java.io.File
import java.util.Locale

enum class DesktopJunkCategory(val title: String, val description: String, val iconName: String) {
    WINDOWS_TEMP("Windows & User Temp", "Leftover installer caches, %TEMP%, and system logs", "FolderDelete"),
    BROWSER_CACHE("Browser Caches", "Cached media from Google Chrome, Edge, and Brave", "Language"),
    APP_CACHE("Desktop App Cache", "Cached data from Discord, Spotify, and Steam", "Apps"),
    WINDOWS_UPDATE("Windows Update Download Cache", "Leftover downloaded patches in SoftwareDistribution", "SystemUpdate"),
    CRASH_DUMPS("Crash Dumps & Error Reports", "Windows minidumps and crash diagnostic traces", "BugReport"),
    RECYCLE_BIN("Recycle Bin Items", "Deleted files occupying drive storage", "Delete")
}

data class DesktopJunkItem(
    val id: String,
    val file: File,
    val name: String,
    val sizeBytes: Long,
    val category: DesktopJunkCategory,
    var isSelected: Boolean = true
) {
    val formattedSize: String get() = formatSize(sizeBytes)

    companion object {
        fun formatSize(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            if (bytes < 1024) return "$bytes B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
            val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
            return String.format(Locale.US, "%.1f %s", value, units[digitGroups.coerceIn(0, units.size - 1)])
        }
    }
}

data class DriveOverview(
    val rootPath: String,
    val totalBytes: Long,
    val freeBytes: Long,
    val usedBytes: Long,
    val label: String
) {
    val usedPercentage: Float
        get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes) else 0f
    val formattedTotal: String get() = DesktopJunkItem.formatSize(totalBytes)
    val formattedFree: String get() = DesktopJunkItem.formatSize(freeBytes)
    val formattedUsed: String get() = DesktopJunkItem.formatSize(usedBytes)
}

data class DesktopLargeFile(
    val file: File,
    val name: String,
    val sizeBytes: Long,
    val extension: String,
    val lastModified: Long
) {
    val formattedSize: String get() = DesktopJunkItem.formatSize(sizeBytes)
}

data class DesktopFolderHeatmapItem(
    val file: File,
    val name: String,
    val totalBytes: Long,
    val parentPercentage: Float
) {
    val formattedSize: String get() = DesktopJunkItem.formatSize(totalBytes)
}

data class DesktopDuplicateGroup(
    val hash: String,
    val sizeBytes: Long,
    val files: List<File>,
    val selectedFiles: MutableSet<File> = mutableSetOf()
) {
    val formattedSize: String get() = DesktopJunkItem.formatSize(sizeBytes)
    val reclaimableBytes: Long get() = sizeBytes * selectedFiles.size
}

data class DesktopSimilarPhoto(
    val file: File,
    val sizeBytes: Long,
    val hash: Long,
    val isBestShot: Boolean,
    var isSelected: Boolean
)

data class DesktopSimilarPhotoGroup(
    val groupId: String,
    val photos: List<DesktopSimilarPhoto>
) {
    val reclaimableBytes: Long
        get() = photos.filter { it.isSelected && !it.isBestShot }.sumOf { it.sizeBytes }
}

enum class GamerCacheCategory(val title: String, val description: String) {
    REPLAY_TMP("Game Match Replays & Buffers", "Temporary game recordings, match dumps, and emulator buffers"),
    SHADER_CACHE("DirectX & GPU Shader Pipeline", "Pre-compiled shader blobs from DirectX and NVIDIA/AMD"),
    LAUNCHER_CACHE("Game Launcher & Web Caches", "Embedded web caches from Steam, Epic Games, EA, and Battle.net"),
    EMULATOR_BLOBS("Emulated Cloud & Sync Data", "Local offline sync dumps and CDN downloads from game emulators")
}

data class GamerCacheItem(
    val id: String,
    val file: File,
    val name: String,
    val sizeBytes: Long,
    val category: GamerCacheCategory,
    val gameTitle: String,
    val isSelected: Boolean = true
) {
    val formattedSize: String get() = DesktopJunkItem.formatSize(sizeBytes)
}

enum class StartupImpact {
    HIGH, MEDIUM, LOW, UNKNOWN
}

data class DesktopStartupItem(
    val id: String,
    val name: String,
    val command: String,
    val sourceLocation: String,
    val isEnabled: Boolean,
    val impact: StartupImpact,
    val targetFile: File? = null
)

data class SqueezeTarget(
    val file: File,
    val name: String,
    val originalSizeBytes: Long,
    val compressedSizeBytes: Long = 0L,
    val status: String = "Ready"
) {
    val formattedOriginal: String get() = DesktopJunkItem.formatSize(originalSizeBytes)
    val formattedCompressed: String get() = DesktopJunkItem.formatSize(compressedSizeBytes)
    val savingsPercentage: Int get() = if (originalSizeBytes > 0 && compressedSizeBytes > 0) {
        ((1.0 - (compressedSizeBytes.toDouble() / originalSizeBytes.toDouble())) * 100).toInt().coerceAtLeast(0)
    } else 0
}
