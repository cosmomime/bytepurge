package com.storagemaid.app.core.scanner

import com.storagemaid.app.core.model.JunkCategory
import com.storagemaid.app.core.model.JunkItem
import com.storagemaid.app.core.model.RiskLevel
import java.io.File

class MediaPurger {

    private val sentFolderNames = setOf(
        "Sent", "sent"
    )

    fun scan(
        storageRoots: List<File>,
        onProgress: (File) -> Unit
    ): List<JunkItem> {
        val mediaJunk = mutableListOf<JunkItem>()

        for (root in storageRoots) {
            // Check Android/media/com.whatsapp/WhatsApp/Media
            val whatsappPaths = listOf(
                File(root, "Android/media/com.whatsapp/WhatsApp/Media"),
                File(root, "WhatsApp/Media")
            )

            for (mediaRoot in whatsappPaths) {
                if (!mediaRoot.exists() || !mediaRoot.isDirectory) continue

                val subMediaDirs = mediaRoot.listFiles() ?: continue
                for (subDir in subMediaDirs) {
                    onProgress(subDir)

                    // 1. WhatsApp .Statuses cache
                    if (subDir.name.equals(".Statuses", ignoreCase = true)) {
                        val (size, count) = calculateDirectoryStats(subDir)
                        if (count > 0) {
                            mediaJunk.add(
                                JunkItem(
                                    id = "wa_status_${subDir.absolutePath.hashCode()}",
                                    path = subDir.absolutePath,
                                    name = "WhatsApp Statuses Cache",
                                    sizeBytes = size,
                                    category = JunkCategory.SOCIAL_JUNK,
                                    riskLevel = RiskLevel.SAFE,
                                    isDirectory = true,
                                    fileCount = count,
                                    isSelected = true
                                )
                            )
                        }
                    }

                    // 2. Sent folders in WhatsApp Images / Video / Audio / Docs
                    val sentFolder = File(subDir, "Sent")
                    if (sentFolder.exists() && sentFolder.isDirectory) {
                        val (size, count) = calculateDirectoryStats(sentFolder)
                        if (count > 0) {
                            mediaJunk.add(
                                JunkItem(
                                    id = "wa_sent_${sentFolder.absolutePath.hashCode()}",
                                    path = sentFolder.absolutePath,
                                    name = "${subDir.name} (Sent Copies)",
                                    sizeBytes = size,
                                    category = JunkCategory.SOCIAL_JUNK,
                                    riskLevel = RiskLevel.REVIEW, // Review recommended so user knows they are sent duplicates
                                    isDirectory = true,
                                    fileCount = count,
                                    isSelected = false // Default unselected for user safety
                                )
                            )
                        }
                    }

                    // 3. WhatsApp Voice Notes
                    if (subDir.name.equals("WhatsApp Voice Notes", ignoreCase = true)) {
                        val (size, count) = calculateDirectoryStats(subDir)
                        if (count > 0) {
                            mediaJunk.add(
                                JunkItem(
                                    id = "wa_voice_${subDir.absolutePath.hashCode()}",
                                    path = subDir.absolutePath,
                                    name = "WhatsApp Voice Notes ($count notes)",
                                    sizeBytes = size,
                                    category = JunkCategory.SOCIAL_JUNK,
                                    riskLevel = RiskLevel.REVIEW,
                                    isDirectory = true,
                                    fileCount = count,
                                    isSelected = false
                                )
                            )
                        }
                    }

                    // 4. WhatsApp Stickers
                    if (subDir.name.equals("WhatsApp Stickers", ignoreCase = true)) {
                        val (size, count) = calculateDirectoryStats(subDir)
                        if (count > 0) {
                            mediaJunk.add(
                                JunkItem(
                                    id = "wa_stickers_${subDir.absolutePath.hashCode()}",
                                    path = subDir.absolutePath,
                                    name = "WhatsApp Cached Stickers",
                                    sizeBytes = size,
                                    category = JunkCategory.SOCIAL_JUNK,
                                    riskLevel = RiskLevel.REVIEW,
                                    isDirectory = true,
                                    fileCount = count,
                                    isSelected = false
                                )
                            )
                        }
                    }
                }
            }

            // Check Telegram Media Directories
            val telegramPaths = listOf(
                File(root, "Telegram"),
                File(root, "Android/media/org.telegram.messenger/Telegram"),
                File(root, "Android/data/org.telegram.messenger/cache")
            )
            for (tgPath in telegramPaths) {
                if (!tgPath.exists() || !tgPath.isDirectory) continue
                val tgSubDirs = tgPath.listFiles() ?: continue
                for (tgSub in tgSubDirs) {
                    onProgress(tgSub)
                    if (tgSub.isDirectory) {
                        val (size, count) = calculateDirectoryStats(tgSub)
                        if (count > 0 && size > 1024 * 1024) { // Only if > 1MB
                            mediaJunk.add(
                                JunkItem(
                                    id = "tg_${tgSub.absolutePath.hashCode()}",
                                    path = tgSub.absolutePath,
                                    name = "Telegram ${tgSub.name}",
                                    sizeBytes = size,
                                    category = JunkCategory.SOCIAL_JUNK,
                                    riskLevel = RiskLevel.REVIEW,
                                    isDirectory = true,
                                    fileCount = count,
                                    isSelected = false
                                )
                            )
                        }
                    }
                }
            }
        }

        return mediaJunk
    }

    private fun calculateDirectoryStats(dir: File): Pair<Long, Int> {
        var totalSize = 0L
        var fileCount = 0
        try {
            val files = dir.walkTopDown().maxDepth(5)
            for (file in files) {
                if (file.isFile) {
                    totalSize += file.length()
                    fileCount++
                }
            }
        } catch (_: Exception) {}
        return totalSize to fileCount
    }
}
