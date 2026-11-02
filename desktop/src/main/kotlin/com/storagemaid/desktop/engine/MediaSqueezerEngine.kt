package com.storagemaid.desktop.engine

import com.storagemaid.desktop.model.SqueezeTarget
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.ImageWriter

class MediaSqueezerEngine {

    private val supportedPhotoExts = setOf("png", "jpg", "jpeg", "bmp")

    fun scanHeavyPhotos(minSizeBytes: Long = 2 * 1024 * 1024): List<SqueezeTarget> {
        val targets = mutableListOf<SqueezeTarget>()
        val userHome = System.getProperty("user.home") ?: return targets

        val searchDirs = listOf(
            File(userHome, "Pictures"),
            File(userHome, "Downloads"),
            File(userHome, "Desktop")
        )

        for (dir in searchDirs) {
            if (dir.exists() && dir.isDirectory) {
                scanDirForMedia(dir, targets, minSizeBytes, currentDepth = 0, maxDepth = 2)
            }
        }

        return targets.sortedByDescending { it.originalSizeBytes }
    }

    private fun scanDirForMedia(
        dir: File,
        results: MutableList<SqueezeTarget>,
        minSizeBytes: Long,
        currentDepth: Int,
        maxDepth: Int
    ) {
        if (!dir.exists() || currentDepth > maxDepth) return
        val files = dir.listFiles() ?: return

        for (file in files) {
            try {
                if (file.isFile) {
                    val ext = file.extension.lowercase(Locale.US)
                    if (supportedPhotoExts.contains(ext) && file.length() >= minSizeBytes && !file.name.contains("_squeezed")) {
                        results.add(
                            SqueezeTarget(
                                file = file,
                                name = file.name,
                                originalSizeBytes = file.length()
                            )
                        )
                    }
                } else if (file.isDirectory) {
                    scanDirForMedia(file, results, minSizeBytes, currentDepth + 1, maxDepth)
                }
            } catch (_: Exception) {}
        }
    }

    fun squeezePhoto(target: SqueezeTarget, quality: Float = 0.80f): SqueezeTarget {
        try {
            val original = ImageIO.read(target.file) ?: return target.copy(status = "Failed")

            // Convert ARGB to RGB if needed for JPEG compression
            val rgbImage = BufferedImage(original.width, original.height, BufferedImage.TYPE_INT_RGB)
            val g2d = rgbImage.createGraphics()
            g2d.drawImage(original, 0, 0, java.awt.Color.WHITE, null)
            g2d.dispose()

            val outputFile = File(target.file.parentFile, "${target.file.nameWithoutExtension}_squeezed.jpg")
            val writers = ImageIO.getImageWritersByFormatName("jpeg")
            if (!writers.hasNext()) return target.copy(status = "No JPEG encoder")

            val writer: ImageWriter = writers.next()
            val outputStream = FileOutputStream(outputFile)
            val ios = ImageIO.createImageOutputStream(outputStream)
            writer.output = ios

            val params = writer.defaultWriteParam
            if (params.canWriteCompressed()) {
                params.compressionMode = ImageWriteParam.MODE_EXPLICIT
                params.compressionQuality = quality
            }

            writer.write(null, IIOImage(rgbImage, null, null), params)
            ios.close()
            outputStream.close()
            writer.dispose()

            val newSize = outputFile.length()
            return target.copy(
                compressedSizeBytes = newSize,
                status = "Compressed"
            )
        } catch (e: Exception) {
            return target.copy(status = "Error: ${e.message}")
        }
    }
}
