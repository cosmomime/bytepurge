package com.storagemaid.app.core.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AppInfoItem(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val isSystemApp: Boolean,
    val apkSizeBytes: Long,
    val installTimeMillis: Long,
    val lastUpdateTimeMillis: Long,
    val targetSdkVersion: Int,
    val sourceDir: String,
    val dataSizeBytes: Long = 0L,
    val cacheSizeBytes: Long = 0L,
    val hasUsageStats: Boolean = false
) {
    val totalStorageBytes: Long
        get() = if (hasUsageStats) (apkSizeBytes + dataSizeBytes + cacheSizeBytes) else apkSizeBytes

    val formattedApkSize: String
        get() = JunkItem.formatSize(apkSizeBytes)

    val formattedDataSize: String
        get() = JunkItem.formatSize(dataSizeBytes)

    val formattedCacheSize: String
        get() = JunkItem.formatSize(cacheSizeBytes)

    val formattedTotalStorage: String
        get() = JunkItem.formatSize(totalStorageBytes)

    val isLargeCache: Boolean
        get() = cacheSizeBytes >= 500L * 1024 * 1024

    val formattedInstallDate: String
        get() {
            val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            return sdf.format(Date(installTimeMillis))
        }

    val formattedUpdateDate: String
        get() {
            val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            return sdf.format(Date(lastUpdateTimeMillis))
        }
}

enum class AppFilter(val label: String) {
    USER_ONLY("User Apps"),
    SYSTEM_ONLY("System Apps"),
    LARGE_ONLY("Large (>100MB)"),
    LARGE_CACHE("Heavy Cache (>500MB)"),
    ALL("All Apps")
}

enum class AppSortOrder(val label: String) {
    SIZE_DESC("Largest First"),
    NAME_ASC("A - Z"),
    DATE_DESC("Recently Installed"),
    DATE_ASC("Oldest Installed")
}

data class AppManagerSummary(
    val totalApps: Int = 0,
    val userApps: Int = 0,
    val systemApps: Int = 0,
    val totalUserSizeBytes: Long = 0L
) {
    val formattedUserSize: String
        get() = JunkItem.formatSize(totalUserSizeBytes)
}
