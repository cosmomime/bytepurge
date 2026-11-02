package com.storagemaid.app

import com.storagemaid.app.core.model.JunkItem
import com.storagemaid.app.core.model.LargeFileThreshold
import com.storagemaid.app.core.preferences.DuplicateKeepStrategy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StorageMaidCoreUnitTest {

    @Test
    fun testFormatSize_ZeroAndBytes() {
        assertEquals("0 B", JunkItem.formatSize(0L))
        assertEquals("500 B", JunkItem.formatSize(500L))
    }

    @Test
    fun testFormatSize_KilobytesAndMegabytes() {
        assertEquals("1.0 KB", JunkItem.formatSize(1024L))
        assertEquals("1.5 KB", JunkItem.formatSize(1536L))
        assertEquals("1.0 MB", JunkItem.formatSize(1024L * 1024L))
        assertEquals("32.5 MB", JunkItem.formatSize((32.5 * 1024 * 1024).toLong()))
    }

    @Test
    fun testFormatSize_Gigabytes() {
        assertEquals("1.0 GB", JunkItem.formatSize(1024L * 1024L * 1024L))
        assertEquals("102.7 GB", JunkItem.formatSize((102.7 * 1024 * 1024 * 1024).toLong()))
    }

    @Test
    fun testLargeFileThresholds() {
        val bytes30MB = 30L * 1024L * 1024L
        val bytes75MB = 75L * 1024L * 1024L
        val bytes150MB = 150L * 1024L * 1024L

        assertTrue(bytes30MB >= LargeFileThreshold.MB_25.minBytes)
        assertFalse(bytes30MB >= LargeFileThreshold.MB_50.minBytes)

        assertTrue(bytes75MB >= LargeFileThreshold.MB_50.minBytes)
        assertFalse(bytes75MB >= LargeFileThreshold.MB_100.minBytes)

        assertTrue(bytes150MB >= LargeFileThreshold.MB_100.minBytes)
    }

    @Test
    fun testHammingDistance_Logic() {
        val hashA: Long = 0x5555555555555555L
        val hashB: Long = 0x5555555555555555L
        val distIdentical = java.lang.Long.bitCount(hashA xor hashB)
        assertEquals(0, distIdentical)

        val hashC: Long = hashA xor 1L
        val distOneBit = java.lang.Long.bitCount(hashA xor hashC)
        assertEquals(1, distOneBit)

        val hashD: Long = hashA.inv()
        val distInverted = java.lang.Long.bitCount(hashA xor hashD)
        assertEquals(64, distInverted)

        // Perceptual similarity threshold check (<= 8 bit differences)
        assertTrue(distIdentical <= 8)
        assertTrue(distOneBit <= 8)
        assertFalse(distInverted <= 8)
    }

    @Test
    fun testDuplicateKeepStrategy() {
        val strategies = DuplicateKeepStrategy.values()
        assertEquals(2, strategies.size)
        assertTrue(strategies.contains(DuplicateKeepStrategy.KEEP_OLDEST))
        assertTrue(strategies.contains(DuplicateKeepStrategy.KEEP_SHORTEST_PATH))
    }
}
