package com.storagemaid.app.core.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import com.storagemaid.app.core.model.SimilarPhotoGroup
import com.storagemaid.app.core.model.SimilarPhotoItem
import com.storagemaid.app.core.preferences.StoragePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class SimilarPhotoScanner(
    private val context: Context,
    private val preferences: StoragePreferences = StoragePreferences(context)
) {

    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "heic")

    private val targetFolderNames = setOf(
        "camera", "dcim", "pictures", "screenshots", "photos", "100andro"
    )

    private data class CandidatePhoto(
        val file: File,
        val sizeBytes: Long,
        val lastModified: Long,
        var dHash: Long? = null
    )

    suspend fun scanSimilarPhotos(
        roots: List<File>,
        onProgress: (Int, String) -> Unit = { _, _ -> }
    ): List<SimilarPhotoGroup> = withContext(Dispatchers.IO) {
        val candidates = mutableListOf<CandidatePhoto>()
        val seenPaths = HashSet<String>()

        for (root in roots) {
            if (!root.exists()) continue

            root.walkTopDown()
                .onEnter { dir ->
                    val name = dir.name.lowercase()
                    !name.startsWith(".") &&
                            !name.contains("cache") &&
                            !preferences.isPathWhitelisted(dir.absolutePath)
                }
                .filter { it.isFile }
                .forEach { file ->
                    val ext = file.extension.lowercase()
                    if (imageExtensions.contains(ext)) {
                        val parentLower = (file.parentFile?.name ?: "").lowercase()
                        val grandParentLower = (file.parentFile?.parentFile?.name ?: "").lowercase()
                        val isPhotoFolder = targetFolderNames.any { parentLower.contains(it) || grandParentLower.contains(it) }

                        val len = file.length()
                        if (isPhotoFolder && len >= 50 * 1024 && !preferences.isPathWhitelisted(file.absolutePath)) {
                            val canonical = file.canonicalPath
                            if (seenPaths.add(canonical)) {
                                candidates.add(CandidatePhoto(file, len, file.lastModified()))
                            }
                        }
                    }
                }
        }

        if (candidates.size < 2) return@withContext emptyList()

        // Sort candidates by last modified for temporal clustering
        candidates.sortBy { it.lastModified }

        val groups = mutableListOf<SimilarPhotoGroup>()
        val groupedIndices = HashSet<Int>()

        // Limit computation to reasonable set to maintain smooth performance
        val maxToCompute = candidates.size.coerceAtMost(600)

        for (i in 0 until maxToCompute) {
            if (groupedIndices.contains(i)) continue

            val current = candidates[i]
            onProgress(i, current.file.name)

            if (current.dHash == null) {
                current.dHash = calculateDHash(current.file)
            }
            val currentHash = current.dHash ?: continue

            val currentGroupMembers = mutableListOf(current)
            groupedIndices.add(i)

            // Look forward up to 30 items or within 60 seconds
            val lookAheadLimit = (i + 30).coerceAtMost(maxToCompute)
            for (j in (i + 1) until lookAheadLimit) {
                if (groupedIndices.contains(j)) continue

                val target = candidates[j]
                val timeDiff = Math.abs(target.lastModified - current.lastModified)

                // If taken within 30 seconds of each other or similar visual hash
                if (timeDiff <= 30_000L) {
                    if (target.dHash == null) {
                        target.dHash = calculateDHash(target.file)
                    }
                    val targetHash = target.dHash
                    if (targetHash != null) {
                        val distance = java.lang.Long.bitCount(currentHash xor targetHash)
                        if (distance <= 8) { // 88%+ visual match
                            currentGroupMembers.add(target)
                            groupedIndices.add(j)
                        }
                    }
                }
            }

            if (currentGroupMembers.size >= 2) {
                // Determine Best Shot (largest file size / highest resolution)
                val sortedByQuality = currentGroupMembers.sortedByDescending { it.sizeBytes }
                val bestShotFile = sortedByQuality.first().file

                val photoItems = currentGroupMembers.map { member ->
                    val isBest = member.file.canonicalPath == bestShotFile.canonicalPath
                    SimilarPhotoItem(
                        id = UUID.nameUUIDFromBytes(member.file.canonicalPath.toByteArray()).toString(),
                        file = member.file,
                        name = member.file.name,
                        path = member.file.canonicalPath,
                        sizeBytes = member.sizeBytes,
                        lastModified = member.lastModified,
                        isBestShot = isBest,
                        isSelected = !isBest // Auto-select duplicates, preserve the best shot!
                    )
                }

                groups.add(
                    SimilarPhotoGroup(
                        groupId = UUID.randomUUID().toString(),
                        photos = photoItems
                    )
                )
            }
        }

        groups.sortedByDescending { it.reclaimableBytes }
    }

    private fun calculateDHash(file: File): Long? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, options)
            if (options.outWidth <= 0 || options.outHeight <= 0) return null

            val sampleSize = (options.outWidth / 32).coerceAtLeast(options.outHeight / 32).coerceAtLeast(1)
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val tempBmp = BitmapFactory.decodeFile(file.absolutePath, decodeOptions) ?: return null
            val scaled = Bitmap.createScaledBitmap(tempBmp, 9, 8, true)
            if (scaled != tempBmp) tempBmp.recycle()

            var hash = 0L
            for (y in 0 until 8) {
                for (x in 0 until 8) {
                    val leftPixel = scaled.getPixel(x, y)
                    val rightPixel = scaled.getPixel(x + 1, y)
                    val leftLum = (Color.red(leftPixel) * 299 + Color.green(leftPixel) * 587 + Color.blue(leftPixel) * 114) / 1000
                    val rightLum = (Color.red(rightPixel) * 299 + Color.green(rightPixel) * 587 + Color.blue(rightPixel) * 114) / 1000
                    if (leftLum > rightLum) {
                        hash = hash or (1L shl (y * 8 + x))
                    }
                }
            }
            scaled.recycle()
            hash
        } catch (_: Exception) {
            null
        }
    }

    suspend fun deleteSimilarPhotos(items: List<SimilarPhotoItem>): Pair<Int, Long> = withContext(Dispatchers.IO) {
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
                        val uri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
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
