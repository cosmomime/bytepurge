package com.storagemaid.app.core.manager

import android.app.AppOpsManager
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Process
import android.provider.Settings
import com.storagemaid.app.core.model.AppInfoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AppManager(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    suspend fun getInstalledApps(
        onProgress: (Int, String) -> Unit = { _, _ -> }
    ): List<AppInfoItem> = withContext(Dispatchers.IO) {
        val packages: List<PackageInfo> = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstalledPackages(0)
            }
        } catch (e: Exception) {
            emptyList()
        }

        val hasUsagePermission = hasUsageStatsPermission()
        val storageStatsManager = if (hasUsagePermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(Context.STORAGE_STATS_SERVICE) as? StorageStatsManager
        } else null
        val currentUserHandle = Process.myUserHandle()

        val appList = mutableListOf<AppInfoItem>()
        var count = 0

        for (pkg in packages) {
            val appInfo = pkg.applicationInfo ?: continue
            val packageName = pkg.packageName

            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                    (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

            val appName = try {
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                packageName
            }

            count++
            if (count % 15 == 0) {
                onProgress(count, appName)
            }

            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkg.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pkg.versionCode.toLong()
            }

            val versionName = pkg.versionName ?: "1.0"
            val targetSdkVersion = appInfo.targetSdkVersion

            // Calculate APK Size (Base + Split APKs)
            val sourceDir = appInfo.publicSourceDir ?: appInfo.sourceDir ?: ""
            var totalApkSize = 0L
            if (sourceDir.isNotEmpty()) {
                val baseApk = File(sourceDir)
                if (baseApk.exists()) {
                    totalApkSize += baseApk.length()
                }
            }

            appInfo.splitPublicSourceDirs?.forEach { splitPath ->
                val splitFile = File(splitPath)
                if (splitFile.exists()) {
                    totalApkSize += splitFile.length()
                }
            }

            // Query App Data and Cache via StorageStatsManager
            var dataBytes = 0L
            var cacheBytes = 0L
            var hasStatsForThisApp = false

            if (storageStatsManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    val stats = storageStatsManager.queryStatsForPackage(appInfo.storageUuid, packageName, currentUserHandle)
                    dataBytes = stats.dataBytes
                    cacheBytes = stats.cacheBytes
                    hasStatsForThisApp = true
                } catch (_: Exception) {}
            }

            appList.add(
                AppInfoItem(
                    packageName = packageName,
                    appName = appName,
                    versionName = versionName,
                    versionCode = versionCode,
                    isSystemApp = isSystem,
                    apkSizeBytes = totalApkSize,
                    installTimeMillis = pkg.firstInstallTime,
                    lastUpdateTimeMillis = pkg.lastUpdateTime,
                    targetSdkVersion = targetSdkVersion,
                    sourceDir = sourceDir,
                    dataSizeBytes = dataBytes,
                    cacheSizeBytes = cacheBytes,
                    hasUsageStats = hasStatsForThisApp
                )
            )
        }

        appList
    }

    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        @Suppress("DEPRECATION")
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openUsageAccessSettings() {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun getAppIcon(packageName: String): Drawable? {
        return try {
            packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            null
        }
    }

    fun launchApp(packageName: String): Boolean {
        return try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun openAppDetails(packageName: String): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun requestUninstall(packageName: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun extractApk(app: AppInfoItem): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(app.sourceDir)
            if (!sourceFile.exists()) {
                return@withContext false to "APK file not found on device"
            }

            val sanitizedAppName = app.appName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val targetDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "BytePurge/APKs"
            )
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }

            val targetFile = File(targetDir, "${sanitizedAppName}_v${app.versionName}.apk")
            sourceFile.copyTo(targetFile, overwrite = true)
            true to targetFile.absolutePath
        } catch (e: Exception) {
            false to (e.message ?: "Extraction failed")
        }
    }

    fun shareApk(app: AppInfoItem) {
        try {
            val sourceFile = File(app.sourceDir)
            if (!sourceFile.exists()) return

            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                sourceFile
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "${app.appName} APK")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Share ${app.appName} APK"))
        } catch (_: Exception) {}
    }
}
