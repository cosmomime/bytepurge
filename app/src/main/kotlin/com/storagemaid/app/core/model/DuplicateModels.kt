package com.storagemaid.app.core.model

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DuplicateFileItem(
    val id: String,
    val file: File,
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val isOriginal: Boolean,
    var isSelected: Boolean = false
) {
    val formattedSize: String
        get() = JunkItem.formatSize(sizeBytes)

    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
            return sdf.format(Date(lastModified))
        }

    val parentFolder: String
        get() = file.parentFile?.name ?: "Storage"
}

data class DuplicateGroup(
    val hash: String,
    val sizeBytes: Long,
    val files: List<DuplicateFileItem>
) {
    val duplicateCount: Int
        get() = (files.size - 1).coerceAtLeast(0)

    val reclaimableBytes: Long
        get() = duplicateCount * sizeBytes

    val selectedCount: Int
        get() = files.count { it.isSelected }

    val selectedReclaimableBytes: Long
        get() = files.filter { it.isSelected }.sumOf { it.sizeBytes }

    val formattedReclaimable: String
        get() = JunkItem.formatSize(reclaimableBytes)
}

enum class DuplicateMode(val label: String) {
    EXACT("Exact Duplicates"),
    SIMILAR_PHOTOS("Similar & Burst Photos")
}

data class SimilarPhotoItem(
    val id: String,
    val file: File,
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val isBestShot: Boolean = false,
    var isSelected: Boolean = false
) {
    val formattedSize: String
        get() = JunkItem.formatSize(sizeBytes)

    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("MMM d, yyyy HH:mm:ss", Locale.getDefault())
            return sdf.format(Date(lastModified))
        }

    val parentFolder: String
        get() = file.parentFile?.name ?: "Photos"
}

data class SimilarPhotoGroup(
    val groupId: String,
    val photos: List<SimilarPhotoItem>
) {
    val reclaimableCount: Int
        get() = (photos.size - 1).coerceAtLeast(0)

    val reclaimableBytes: Long
        get() = photos.filter { !it.isBestShot }.sumOf { it.sizeBytes }

    val selectedCount: Int
        get() = photos.count { it.isSelected }

    val selectedReclaimableBytes: Long
        get() = photos.filter { it.isSelected }.sumOf { it.sizeBytes }

    val formattedReclaimable: String
        get() = JunkItem.formatSize(reclaimableBytes)
}
