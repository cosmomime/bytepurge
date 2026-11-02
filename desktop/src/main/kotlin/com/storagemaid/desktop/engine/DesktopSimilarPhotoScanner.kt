package com.storagemaid.desktop.engine

import com.storagemaid.desktop.model.DesktopSimilarPhoto
import com.storagemaid.desktop.model.DesktopSimilarPhotoGroup
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.File
import java.util.UUID
import javax.imageio.ImageIO

class DesktopSimilarPhotoScanner {

    fun scanSimilarPhotos(directory: File): List<DesktopSimilarPhotoGroup> {
        val photos = mutableListOf<File>()
        walkPhotos(directory, photos, 0)

        val hashes = mutableListOf<Pair<File, Long>>()
        for (p in photos) {
            val h = computeDHash(p)
            if (h != null) {
                hashes.add(p to h)
            }
        }

        val visited = mutableSetOf<File>()
        val groups = mutableListOf<DesktopSimilarPhotoGroup>()

        for (i in hashes.indices) {
            val (fileA, hashA) = hashes[i]
            if (visited.contains(fileA)) continue

            val cluster = mutableListOf(fileA to hashA)
            for (j in i + 1 until hashes.size) {
                val (fileB, hashB) = hashes[j]
                if (visited.contains(fileB)) continue

                val dist = java.lang.Long.bitCount(hashA xor hashB)
                if (dist <= 8) {
                    cluster.add(fileB to hashB)
                    visited.add(fileB)
                }
            }

            if (cluster.size > 1) {
                visited.add(fileA)
                // Pick largest file as default Best Shot
                val sorted = cluster.sortedByDescending { it.first.length() }
                val bestFile = sorted.first().first

                val groupItems = sorted.map { (f, h) ->
                    val isBest = (f == bestFile)
                    DesktopSimilarPhoto(
                        file = f,
                        sizeBytes = f.length(),
                        hash = h,
                        isBestShot = isBest,
                        isSelected = !isBest // Auto-select redundant photos
                    )
                }

                groups.add(
                    DesktopSimilarPhotoGroup(
                        groupId = UUID.randomUUID().toString(),
                        photos = groupItems
                    )
                )
            }
        }

        return groups.sortedByDescending { it.reclaimableBytes }
    }

    private fun walkPhotos(dir: File, list: MutableList<File>, depth: Int) {
        if (depth > 4 || !dir.exists() || !dir.canRead()) return
        val files = dir.listFiles() ?: return

        for (f in files) {
            try {
                if (f.isFile) {
                    val ext = f.extension.lowercase()
                    if (ext in setOf("jpg", "jpeg", "png", "webp", "bmp")) {
                        list.add(f)
                    }
                } else if (f.isDirectory && !f.name.startsWith(".")) {
                    walkPhotos(f, list, depth + 1)
                }
            } catch (_: Exception) {}
        }
    }

    private fun computeDHash(file: File): Long? {
        return try {
            val img = ImageIO.read(file) ?: return null
            val scaled = BufferedImage(9, 8, BufferedImage.TYPE_BYTE_GRAY)
            val g = scaled.createGraphics()
            g.drawImage(img.getScaledInstance(9, 8, Image.SCALE_SMOOTH), 0, 0, null)
            g.dispose()

            var hash = 0L
            for (y in 0 until 8) {
                for (x in 0 until 8) {
                    val left = scaled.raster.getSample(x, y, 0)
                    val right = scaled.raster.getSample(x + 1, y, 0)
                    if (left > right) {
                        hash = hash or (1L shl (y * 8 + x))
                    }
                }
            }
            hash
        } catch (_: Exception) {
            null
        }
    }
}
