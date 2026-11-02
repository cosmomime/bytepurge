package com.storagemaid.app.core.scanner

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.storagemaid.app.core.model.DuplicateFileItem
import com.storagemaid.app.core.model.DuplicateGroup
import com.storagemaid.app.core.preferences.DuplicateKeepStrategy
import com.storagemaid.app.core.preferences.StoragePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.UUID

class DuplicateScanner(
    private val context: Context,
    private val preferences: StoragePreferences = StoragePreferences(context)
) {

    private val excludedDirs = setOf(
        ".git", ".gradle", ".idea", "node_modules", "proc", "sys", "dev"
    )

    suspend fun scanDuplicates(
        roots: List<File>,
        minSizeBytes: Long = 50 * 1024L, // 50 KB minimum
        onProgress: ((status: String) -> Unit)? = null
    ): List<DuplicateGroup> = withContext(Dispatchers.IO) {
        onProgress?.invoke("Scanning files across storage…")

        // 1. Group files by exact byte size
        val sizeBuckets = HashMap<Long, MutableList<File>>()
        var totalFilesFound = 0

        for (root in roots) {
            if (!root.exists()) continue

            root.walkTopDown()
                .onEnter { dir ->
                    val name = dir.name
                    !name.startsWith(".") && !excludedDirs.contains(name) && !preferences.isPathWhitelisted(dir.absolutePath)
                }
                .filter { it.isFile && !it.name.startsWith(".") }
                .forEach { file ->
                    totalFilesFound++
                    if (totalFilesFound % 500 == 0) {
                        onProgress?.invoke("Indexed $totalFilesFound files…")
                    }

                    try {
                        val len = file.length()
                        if (len >= minSizeBytes && !preferences.isPathWhitelisted(file.absolutePath)) {
                            val bucket = sizeBuckets.getOrPut(len) { ArrayList(2) }
                            bucket.add(file)
                        }
                    } catch (_: Exception) {}
                }
        }

        // Filter to only sizes with 2 or more files
        val sizeCandidates = sizeBuckets.filter { it.value.size >= 2 }
        val candidateCount = sizeCandidates.values.sumOf { it.size }
        onProgress?.invoke("Comparing $candidateCount candidate files…")

        // 2. Filter by fast partial hash (head + middle + tail samples)
        val partialHashBuckets = HashMap<String, MutableList<File>>()
        var partialChecked = 0

        for ((size, files) in sizeCandidates) {
            for (file in files) {
                partialChecked++
                if (partialChecked % 50 == 0) {
                    onProgress?.invoke("Fast sampling: $partialChecked / $candidateCount…")
                }

                val pHash = computePartialHash(file, size) ?: continue
                val key = "$size:$pHash"
                val bucket = partialHashBuckets.getOrPut(key) { ArrayList(2) }
                bucket.add(file)
            }
        }

        val fullHashCandidates = partialHashBuckets.filter { it.value.size >= 2 }
        val fullCheckCount = fullHashCandidates.values.sumOf { it.size }
        onProgress?.invoke("Verifying $fullCheckCount identical matches…")

        // 3. Compute full MD5 hash for exact confirmation
        val confirmedGroups = HashMap<String, MutableList<File>>()
        var fullChecked = 0

        for ((_, files) in fullHashCandidates) {
            for (file in files) {
                fullChecked++
                if (fullChecked % 20 == 0) {
                    onProgress?.invoke("Full verification: $fullChecked / $fullCheckCount…")
                }

                val fHash = computeFullHash(file) ?: continue
                val bucket = confirmedGroups.getOrPut(fHash) { ArrayList(2) }
                bucket.add(file)
            }
        }

        // 4. Construct DuplicateGroup models
        val resultGroups = mutableListOf<DuplicateGroup>()

        for ((hash, files) in confirmedGroups) {
            if (files.size < 2) continue

            // Sort files based on user preference strategy (canonical original)
            val strategy = preferences.duplicateStrategy.value
            val sorted = when (strategy) {
                DuplicateKeepStrategy.KEEP_SHORTEST_PATH -> {
                    files.sortedWith(
                        compareBy<File> { it.path.length }
                            .thenBy { it.lastModified() }
                    )
                }
                DuplicateKeepStrategy.KEEP_OLDEST -> {
                    files.sortedWith(
                        compareBy<File> { it.lastModified() }
                            .thenBy { it.path.length }
                    )
                }
            }

            val size = sorted.first().length()
            val items = sorted.mapIndexed { index, file ->
                val isOriginal = (index == 0)
                DuplicateFileItem(
                    id = UUID.nameUUIDFromBytes(file.canonicalPath.toByteArray()).toString(),
                    file = file,
                    name = file.name,
                    path = file.canonicalPath,
                    sizeBytes = size,
                    lastModified = file.lastModified(),
                    isOriginal = isOriginal,
                    isSelected = !isOriginal // Auto-select duplicates, keep original safe!
                )
            }

            resultGroups.add(
                DuplicateGroup(
                    hash = hash,
                    sizeBytes = size,
                    files = items
                )
            )
        }

        // Return largest reclaimable groups first
        resultGroups.sortedByDescending { it.reclaimableBytes }
    }

    private fun computePartialHash(file: File, fileSize: Long): String? {
        return try {
            val digest = MessageDigest.getInstance("MD5")
            val sampleSize = 4096

            RandomAccessFile(file, "r").use { raf ->
                val buffer = ByteArray(sampleSize)

                // 1. Head
                val headRead = raf.read(buffer, 0, sampleSize.coerceAtMost(fileSize.toInt()))
                if (headRead > 0) digest.update(buffer, 0, headRead)

                // 2. Middle
                if (fileSize > sampleSize * 3) {
                    val midPos = (fileSize / 2) - (sampleSize / 2)
                    raf.seek(midPos)
                    val midRead = raf.read(buffer, 0, sampleSize)
                    if (midRead > 0) digest.update(buffer, 0, midRead)
                }

                // 3. Tail
                if (fileSize > sampleSize * 2) {
                    val tailPos = fileSize - sampleSize
                    raf.seek(tailPos)
                    val tailRead = raf.read(buffer, 0, sampleSize)
                    if (tailRead > 0) digest.update(buffer, 0, tailRead)
                }
            }

            bytesToHex(digest.digest())
        } catch (_: Exception) {
            null
        }
    }

    private fun computeFullHash(file: File): String? {
        return try {
            val digest = MessageDigest.getInstance("MD5")
            val buffer = ByteArray(65536) // 64 KB chunks

            FileInputStream(file).use { fis ->
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }

            bytesToHex(digest.digest())
        } catch (_: Exception) {
            null
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    suspend fun deleteSelectedDuplicates(items: List<DuplicateFileItem>): Pair<Int, Long> = withContext(Dispatchers.IO) {
        var count = 0
        var freed = 0L

        for (item in items) {
            if (item.isOriginal) continue // Never delete the designated original file!

            try {
                var deleted = false
                if (item.file.exists()) {
                    deleted = item.file.delete()
                }

                if (!deleted) {
                    try {
                        val uri = MediaStore.Files.getContentUri("external")
                        val rows = context.contentResolver.delete(
                            uri,
                            "${MediaStore.MediaColumns.DATA} = ?",
                            arrayOf(item.path)
                        )
                        deleted = rows > 0
                    } catch (_: Exception) {}
                }

                if (deleted) {
                    count++
                    freed += item.sizeBytes
                }
            } catch (_: Exception) {}
        }

        count to freed
    }
}
