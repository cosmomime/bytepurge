package com.storagemaid.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storagemaid.desktop.engine.*
import com.storagemaid.desktop.ui.*
import com.storagemaid.desktop.ui.analyzer.DesktopAnalyzerView
import com.storagemaid.desktop.ui.duplicates.DesktopDuplicatesView
import com.storagemaid.desktop.ui.gamer.DesktopGamerJunkView
import com.storagemaid.desktop.ui.squeezer.DesktopMediaSqueezerView
import com.storagemaid.desktop.ui.startup.DesktopStartupView
import com.storagemaid.desktop.ui.sweep.DesktopSweepView
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

@OptIn(ExperimentalComposeUiApi::class)
object DesktopSnapshotRunner {

    fun captureView(
        tab: DesktopNavTab,
        width: Int,
        height: Int,
        outputFile: File
    ) {
        val junkScanner = WindowsJunkScanner()
        val gamerScanner = GamerJunkScanner()
        val startupManager = WindowsStartupManager()
        val squeezerEngine = MediaSqueezerEngine()
        val diskAnalyzer = DiskSpaceAnalyzer()
        val duplicateScanner = DesktopDuplicateScanner()
        val similarScanner = DesktopSimilarPhotoScanner()

        val scene = ImageComposeScene(
            width = width,
            height = height,
            density = Density(1f)
        ) {
            StorageMaidDesktopTheme {
                val isCompact = width < 920
                val sidebarWidth = if (isCompact) 72.dp else 230.dp

                Row(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
                    // Sidebar
                    Column(
                        modifier = Modifier
                            .width(sidebarWidth)
                            .fillMaxHeight()
                            .background(DarkSurface)
                            .padding(if (isCompact) 10.dp else 20.dp),
                        horizontalAlignment = if (isCompact) Alignment.CenterHorizontally else Alignment.Start,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = if (isCompact) Alignment.CenterHorizontally else Alignment.Start) {
                            if (isCompact) {
                                Icon(
                                    imageVector = Icons.Default.CleaningServices,
                                    contentDescription = "BytePurge",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CleaningServices,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("BytePurge", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        Text("v1.0.0 Desktop", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            DesktopNavTab.values().forEach { currentTab ->
                                val isSelected = currentTab == tab
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent
                                        )
                                        .padding(horizontal = if (isCompact) 10.dp else 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = if (isCompact) Arrangement.Center else Arrangement.Start
                                ) {
                                    Icon(
                                        imageVector = currentTab.icon,
                                        contentDescription = currentTab.label,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    if (!isCompact) {
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            currentTab.label,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 13.sp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }

                        // Footer
                        if (isCompact) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SafeGreen.copy(alpha = 0.12f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Shield, contentDescription = "Offline / Zero Telemetry", tint = SafeGreen, modifier = Modifier.size(18.dp))
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SafeGreen.copy(alpha = 0.12f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Shield, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "100% Offline\nZero Telemetry",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SafeGreen
                                    )
                                }
                            }
                        }
                    }

                    // Content View
                    Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                        when (tab) {
                            DesktopNavTab.SWEEP -> DesktopSweepView(junkScanner)
                            DesktopNavTab.GAMER -> DesktopGamerJunkView(gamerScanner)
                            DesktopNavTab.STARTUP -> DesktopStartupView(startupManager)
                            DesktopNavTab.SQUEEZER -> DesktopMediaSqueezerView(squeezerEngine)
                            DesktopNavTab.ANALYZER -> DesktopAnalyzerView(diskAnalyzer)
                            DesktopNavTab.DUPLICATES -> DesktopDuplicatesView(duplicateScanner, similarScanner)
                        }
                    }
                }
            }
        }

        // Render pass 1
        scene.render()
        // Wait a small moment for initial composition to settle, then render final frame
        Thread.sleep(500)
        val skiaImage = scene.render()
        val data = skiaImage.encodeToData(EncodedImageFormat.PNG) ?: return
        outputFile.parentFile?.mkdirs()
        outputFile.writeBytes(data.bytes)
        println("Saved screenshot: ${outputFile.absolutePath} (${width}x${height})")
        scene.close()
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val outDir = File("C:/Users/LENOVO/.gemini/antigravity/brain/f9375717-ad55-4c82-8d31-ba630feebdcd")

        // 1. Standard (1120x760)
        captureView(DesktopNavTab.GAMER, 1120, 760, File(outDir, "desktop_standard_gamer_1120x760.png"))
        captureView(DesktopNavTab.SWEEP, 1120, 760, File(outDir, "desktop_standard_sweep_1120x760.png"))
        captureView(DesktopNavTab.STARTUP, 1120, 760, File(outDir, "desktop_standard_startup_1120x760.png"))
        captureView(DesktopNavTab.ANALYZER, 1120, 760, File(outDir, "desktop_standard_analyzer_1120x760.png"))

        // 2. Compact / Small Laptop (800x600)
        captureView(DesktopNavTab.GAMER, 800, 600, File(outDir, "desktop_compact_gamer_800x600.png"))
        captureView(DesktopNavTab.SWEEP, 800, 600, File(outDir, "desktop_compact_sweep_800x600.png"))

        // 3. Wide / Large Display (1440x900)
        captureView(DesktopNavTab.GAMER, 1440, 900, File(outDir, "desktop_wide_gamer_1440x900.png"))
        captureView(DesktopNavTab.ANALYZER, 1440, 900, File(outDir, "desktop_wide_analyzer_1440x900.png"))
    }
}
