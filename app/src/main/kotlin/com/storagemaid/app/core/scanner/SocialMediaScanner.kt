package com.storagemaid.app.core.scanner

import android.content.Context
import com.storagemaid.app.core.preferences.StoragePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class SocialCategory(val label: String) {
    VOICE_NOTES("Voice Notes"),
    SENT_MEDIA("Sent Copies"),
    STICKERS("Stickers"),
    TELEGRAM("Telegram")
}

data class SocialMediaFileItem(
    val id: String,
    val file: File,
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val category: SocialCategory,
    val isAudio: Boolean = false,
    val isImage: Boolean = false,
    val isVideo: Boolean = false,
    var isSelected: Boolean = false
) {
    val formattedSize: String
        get() = com.storagemaid.app.core.model.JunkItem.formatSize(sizeBytes)

    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            return sdf.format(Date(lastModified))
        }
}

data class SocialMediaSummary(
    val voiceNotesCount: Int = 0,
    val voiceNotesBytes: Long = 0L,
    val sentMediaCount: Int = 0,
    val sentMediaBytes: Long = 0L,
    val stickersCount: Int = 0,
    val stickersBytes: Long = 0L,
    val telegramCount: Int = 0,
    val telegramBytes: Long = 0L
) {
    val totalFiles: Int
        get() = voiceNotesCount + sentMediaCount + stickersCount + telegramCount

    val totalBytes: Long
        get() = voiceNotesBytes + sentMediaBytes + stickersBytes + telegramBytes

    val formattedTotalSize: String
        get() = com.storagemaid.app.core.model.JunkItem.formatSize(totalBytes)
}

class SocialMediaScanner(
    private val context: Context,
    private val preferences: StoragePreferences = StoragePreferences(context)
) {

    suspend fun scanSocialMedia(roots: List<File>): Map<SocialCategory, List<SocialMediaFileItem>> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<SocialCategory, MutableList<SocialMediaFileItem>>(
            SocialCategory.VOICE_NOTES to mutableListOf(),
            SocialCategory.SENT_MEDIA to mutableListOf(),
            SocialCategory.STICKERS to mutableListOf(),
            SocialCategory.TELEGRAM to mutableListOf()
        )

        for (root in roots) {
            val whatsappPaths = listOf(
                File(root, "Android/media/com.whatsapp/WhatsApp/Media"),
                File(root, "WhatsApp/Media")
            )

            for (mediaRoot in whatsappPaths) {
                if (!mediaRoot.exists() || !mediaRoot.isDirectory) continue

                val subDirs = mediaRoot.listFiles() ?: continue
                for (subDir in subDirs) {
                    val name = subDir.name

                    // 1. WhatsApp Voice Notes
                    if (name.equals("WhatsApp Voice Notes", ignoreCase = true)) {
                        subDir.walkTopDown().maxDepth(4).filter { it.isFile && !preferences.isPathWhitelisted(it.absolutePath) }.forEach { file ->
                            val ext = file.extension.lowercase()
                            if (ext == "opus" || ext == "ogg" || ext == "m4a") {
                                result[SocialCategory.VOICE_NOTES]?.add(
                                    SocialMediaFileItem(
                                        id = UUID.nameUUIDFromBytes(file.absolutePath.toByteArray()).toString(),
                                        file = file,
                                        name = file.name,
                                        path = file.absolutePath,
                                        sizeBytes = file.length(),
                                        lastModified = file.lastModified(),
                                        category = SocialCategory.VOICE_NOTES,
                                        isAudio = true,
                                        isSelected = false
                                    )
                                )
                            }
                        }
                    }

                    // 2. Sent Folders in Images / Videos
                    val sentFolder = File(subDir, "Sent")
                    if (sentFolder.exists() && sentFolder.isDirectory) {
                        sentFolder.walkTopDown().maxDepth(3).filter { it.isFile && !preferences.isPathWhitelisted(it.absolutePath) }.forEach { file ->
                            val ext = file.extension.lowercase()
                            val isImg = ext in setOf("jpg", "jpeg", "png", "webp")
                            val isVid = ext in setOf("mp4", "mkv", "3gp", "mov")
                            if (isImg || isVid) {
                                result[SocialCategory.SENT_MEDIA]?.add(
                                    SocialMediaFileItem(
                                        id = UUID.nameUUIDFromBytes(file.absolutePath.toByteArray()).toString(),
                                        file = file,
                                        name = file.name,
                                        path = file.absolutePath,
                                        sizeBytes = file.length(),
                                        lastModified = file.lastModified(),
                                        category = SocialCategory.SENT_MEDIA,
                                        isImage = isImg,
                                        isVideo = isVid,
                                        isSelected = false
                                    )
                                )
                            }
                        }
                    }

                    // 3. Stickers
                    if (name.equals("WhatsApp Stickers", ignoreCase = true)) {
                        subDir.walkTopDown().maxDepth(2).filter { it.isFile && !preferences.isPathWhitelisted(it.absolutePath) }.forEach { file ->
                            if (file.extension.equals("webp", ignoreCase = true)) {
                                result[SocialCategory.STICKERS]?.add(
                                    SocialMediaFileItem(
                                        id = UUID.nameUUIDFromBytes(file.absolutePath.toByteArray()).toString(),
                                        file = file,
                                        name = file.name,
                                        path = file.absolutePath,
                                        sizeBytes = file.length(),
                                        lastModified = file.lastModified(),
                                        category = SocialCategory.STICKERS,
                                        isImage = true,
                                        isSelected = false
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Telegram Files
            val telegramPaths = listOf(
                File(root, "Telegram"),
                File(root, "Android/media/org.telegram.messenger/Telegram"),
                File(root, "Android/data/org.telegram.messenger/cache")
            )
            for (tgPath in telegramPaths) {
                if (!tgPath.exists() || !tgPath.isDirectory) continue
                tgPath.walkTopDown().maxDepth(4).filter { it.isFile && !preferences.isPathWhitelisted(it.absolutePath) }.forEach { file ->
                    val ext = file.extension.lowercase()
                    val isImg = ext in setOf("jpg", "jpeg", "png", "webp")
                    val isVid = ext in setOf("mp4", "mkv", "mov")
                    val isAud = ext in setOf("ogg", "opus", "mp3")
                    result[SocialCategory.TELEGRAM]?.add(
                        SocialMediaFileItem(
                            id = UUID.nameUUIDFromBytes(file.absolutePath.toByteArray()).toString(),
                            file = file,
                            name = file.name,
                            path = file.absolutePath,
                            sizeBytes = file.length(),
                            lastModified = file.lastModified(),
                            category = SocialCategory.TELEGRAM,
                            isImage = isImg,
                            isVideo = isVid,
                            isAudio = isAud,
                            isSelected = false
                        )
                    )
                }
            }
        }

        // Sort each category by lastModified descending (newest first)
        result.mapValues { (_, list) -> list.sortedByDescending { it.lastModified } }
    }

    suspend fun deleteSocialFiles(items: List<SocialMediaFileItem>): Pair<Int, Long> = withContext(Dispatchers.IO) {
        var count = 0
        var freed = 0L
        for (item in items) {
            if (preferences.isPathWhitelisted(item.path)) continue
            try {
                val f = item.file
                val len = item.sizeBytes
                var deleted = false
                if (f.exists()) {
                    deleted = f.delete()
                }
                if (!deleted) {
                    try {
                        val uri = android.provider.MediaStore.Files.getContentUri("external")
                        val rows = context.contentResolver.delete(
                            uri,
                            "${android.provider.MediaStore.MediaColumns.DATA} = ?",
                            arrayOf(item.path)
                        )
                        deleted = rows > 0
                    } catch (_: Exception) {}
                }
                if (deleted) {
                    count++
                    freed += len
                }
            } catch (_: Exception) {}
        }
        count to freed
    }
}
