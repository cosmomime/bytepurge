package com.storagemaid.app.core.model

enum class JunkCategory(val displayName: String, val description: String) {
    CORPSES(
        "Orphan App Files",
        "Residual app data left behind after uninstallation"
    ),
    SYSTEM_TEMP(
        "System Scrubber",
        "Temporary files (.tmp), crash logs, and cache dumps"
    ),
    EMPTY_FOLDERS(
        "Empty Directories",
        "Abandoned directory trees containing zero files"
    ),
    SOCIAL_JUNK(
        "Social Media Duplicates",
        "Redundant sent media copies and expired cache assets"
    )
}

enum class RiskLevel {
    SAFE,       // 100% safe to delete automatically (empty dirs, stale temp files, uninstalled app folders)
    REVIEW      // Safe, but user might want to review (large files, sent chat media)
}

data class JunkItem(
    val id: String,
    val path: String,
    val name: String,
    val sizeBytes: Long,
    val category: JunkCategory,
    val riskLevel: RiskLevel = RiskLevel.SAFE,
    val isDirectory: Boolean = false,
    val fileCount: Int = 1,
    var isSelected: Boolean = true
) {
    val formattedSize: String
        get() = formatSize(sizeBytes)

    companion object {
        fun formatSize(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            if (bytes < 1024) return "$bytes B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
            val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
            return String.format(java.util.Locale.US, "%.1f %s", value, units[digitGroups.coerceIn(0, units.size - 1)])
        }
    }
}

data class ScanProgress(
    val currentPath: String = "",
    val itemsScanned: Int = 0,
    val junkItemsFound: Int = 0,
    val totalReclaimableBytes: Long = 0L,
    val isScanning: Boolean = false,
    val isFinished: Boolean = false
)

data class StorageOverview(
    val totalBytes: Long = 0L,
    val freeBytes: Long = 0L,
    val usedBytes: Long = 0L,
    val reclaimableBytes: Long = 0L
) {
    val usedPercentage: Float
        get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes) else 0f

    val reclaimablePercentage: Float
        get() = if (totalBytes > 0) (reclaimableBytes.toFloat() / totalBytes) else 0f
}
