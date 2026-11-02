package com.storagemaid.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.Desktop
import java.net.URI
import com.storagemaid.desktop.engine.*
import com.storagemaid.desktop.ui.*
import com.storagemaid.desktop.ui.analyzer.DesktopAnalyzerView
import com.storagemaid.desktop.ui.duplicates.DesktopDuplicatesView
import com.storagemaid.desktop.ui.gamer.DesktopGamerJunkView
import com.storagemaid.desktop.ui.squeezer.DesktopMediaSqueezerView
import com.storagemaid.desktop.ui.home.DesktopHomeView
import com.storagemaid.desktop.ui.startup.DesktopStartupView
import com.storagemaid.desktop.ui.sweep.DesktopSweepView

enum class DesktopNavTab(val label: String, val icon: ImageVector) {
    HOME("Home Hub", Icons.Default.Home),
    SWEEP("Byte Sweep", Icons.Default.CleaningServices),
    GAMER("Gamer Hunter", Icons.Default.SportsEsports),
    STARTUP("Startup Boost", Icons.Default.RocketLaunch),
    SQUEEZER("Media Squeezer", Icons.Default.Compress),
    ANALYZER("Disk Analyzer", Icons.Default.PieChart),
    DUPLICATES("Duplicate Hunter", Icons.Default.ContentCopy)
}

fun main() {
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        System.err.println("BytePurge uncaught error in ${thread.name}: ${throwable.message}")
        throwable.printStackTrace()
    }
    application {
        val windowState = rememberWindowState(width = 1120.dp, height = 760.dp)

        val junkScanner = remember { WindowsJunkScanner() }
        val gamerScanner = remember { GamerJunkScanner() }
        val startupManager = remember { WindowsStartupManager() }
        val squeezerEngine = remember { MediaSqueezerEngine() }
        val diskAnalyzer = remember { DiskSpaceAnalyzer() }
        val duplicateScanner = remember { DesktopDuplicateScanner() }
        val similarScanner = remember { DesktopSimilarPhotoScanner() }

        var currentTab by remember { mutableStateOf(DesktopNavTab.HOME) }

        Window(
            onCloseRequest = {
                exitApplication()
                kotlin.system.exitProcess(0)
            },
            title = "BytePurge - Offline Storage Cleaner & Shader Hunter",
            state = windowState,
            icon = androidx.compose.ui.res.painterResource("icon.png")
        ) {
        StorageMaidDesktopTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = DarkBackground,
                contentColor = TextPrimary
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isCompact = maxWidth < 920.dp
                val sidebarWidth = if (isCompact) 72.dp else 230.dp

                Row(modifier = Modifier.fillMaxSize()) {
                    // Sidebar Navigation
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
                            // Branding Header
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

                            Spacer(modifier = Modifier.height(18.dp))

                            // Categorized Navigation Groups
                            val navGroups = remember {
                                listOf(
                                    "OVERVIEW" to listOf(DesktopNavTab.HOME),
                                    "CLEAN UP" to listOf(DesktopNavTab.SWEEP, DesktopNavTab.ANALYZER, DesktopNavTab.SQUEEZER),
                                    "PERFORMANCE" to listOf(DesktopNavTab.GAMER, DesktopNavTab.STARTUP),
                                    "PRIVACY" to listOf(DesktopNavTab.DUPLICATES)
                                )
                            }

                            navGroups.forEach { (sectionTitle, tabs) ->
                                if (!isCompact) {
                                    Text(
                                        text = sectionTitle,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary.copy(alpha = 0.5f),
                                        letterSpacing = 1.1.sp,
                                        modifier = Modifier.padding(start = 10.dp, top = 10.dp, bottom = 4.dp)
                                    )
                                }

                                tabs.forEach { tab ->
                                    val isSelected = currentTab == tab
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent
                                            )
                                            .clickable { currentTab = tab }
                                            .padding(horizontal = if (isCompact) 10.dp else 12.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = if (isCompact) Arrangement.Center else Arrangement.Start
                                    ) {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = tab.label,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(19.dp)
                                        )
                                        if (!isCompact) {
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                tab.label,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 13.sp,
                                                color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                }
                            }
                        }

                        // Footer
                        if (!isCompact) {
                            Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "BytePurge v1.0.0",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        " • GPL-3.0",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    "© 2026 CosmoMime • Free Software",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                )
                            }
                        }
                    }

                    // Main Content View
                    Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                        when (currentTab) {
                            DesktopNavTab.HOME -> DesktopHomeView(
                                onNavigate = { currentTab = it }
                            )
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
    }
}
}
}
