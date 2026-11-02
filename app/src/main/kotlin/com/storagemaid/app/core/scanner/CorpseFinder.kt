package com.storagemaid.app.core.scanner

import android.content.Context
import android.content.pm.PackageManager
import com.storagemaid.app.core.model.JunkCategory
import com.storagemaid.app.core.model.JunkItem
import com.storagemaid.app.core.model.RiskLevel
import java.io.File

class CorpseFinder(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    fun getInstalledPackageNames(): Set<String> {
        return try {
            val flags = 0
            val packages = packageManager.getInstalledPackages(flags)
            packages.map { it.packageName }.toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    fun scan(
        storageRoots: List<File>,
        installedPackages: Set<String>,
        onProgress: (File) -> Unit
    ): List<JunkItem> {
        val corpses = mutableListOf<JunkItem>()

        for (root in storageRoots) {
            val androidDir = File(root, "Android")
            if (!androidDir.exists() || !androidDir.isDirectory) continue

            val targets = listOf(
                File(androidDir, "data"),
                File(androidDir, "media"),
                File(androidDir, "obb")
            )

            for (target in targets) {
                if (!target.exists() || !target.isDirectory) continue
                val subFolders = target.listFiles() ?: continue

                for (folder in subFolders) {
                    if (!folder.isDirectory) continue
                    val folderName = folder.name
                    onProgress(folder)

                    // Must look like a package name (contains at least one dot, e.g. com.example.app)
                    if (folderName.contains(".") && !folderName.startsWith(".")) {
                        if (!installedPackages.contains(folderName)) {
                            // Ghost folder of uninstalled app!
                            val (size, count) = calculateDirectoryStats(folder)
                            corpses.add(
                                JunkItem(
                                    id = "corpse_${folder.absolutePath.hashCode()}",
                                    path = folder.absolutePath,
                                    name = formatCorpseName(folderName),
                                    sizeBytes = size,
                                    category = JunkCategory.CORPSES,
                                    riskLevel = RiskLevel.SAFE,
                                    isDirectory = true,
                                    fileCount = count,
                                    isSelected = true
                                )
                            )
                        }
                    }
                }
            }
        }

        return corpses
    }

    private fun calculateDirectoryStats(dir: File): Pair<Long, Int> {
        var totalSize = 0L
        var fileCount = 0
        try {
            val files = dir.walkTopDown().maxDepth(15)
            for (file in files) {
                if (file.isFile) {
                    totalSize += file.length()
                    fileCount++
                }
            }
        } catch (_: Exception) {}
        return totalSize to fileCount
    }

    private fun formatCorpseName(packageName: String): String {
        val parts = packageName.split(".").filter { it.isNotBlank() }
        if (parts.size >= 2) {
            val meaningfulParts = parts.filter { it.lowercase() !in setOf("com", "org", "net", "io", "android", "app") }
            val words = meaningfulParts.takeLast(2).joinToString(" ") { part ->
                part.split(Regex("(?<=[a-z])(?=[A-Z])|_|-"))
                    .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            }
            if (words.isNotBlank()) {
                return "$words (Leftovers)"
            }
        }
        return "$packageName (Leftovers)"
    }
}
