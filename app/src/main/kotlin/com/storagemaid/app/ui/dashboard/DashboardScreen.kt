package com.storagemaid.app.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storagemaid.app.core.model.AppTab
import com.storagemaid.app.core.model.JunkCategory
import com.storagemaid.app.core.model.JunkItem
import com.storagemaid.app.core.model.RiskLevel
import com.storagemaid.app.core.scanner.SocialMediaScanner
import com.storagemaid.app.ui.analyzer.StorageAnalyzerScreen
import com.storagemaid.app.ui.apps.AppManagerScreen
import com.storagemaid.app.ui.components.CleanCelebrationDialog
import com.storagemaid.app.ui.duplicates.DuplicateFinderScreen
import com.storagemaid.app.ui.settings.SettingsBottomSheet
import com.storagemaid.app.ui.theme.*

private data class JunkCategoryMetadata(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color
)

private fun getCategoryMetadata(category: JunkCategory): JunkCategoryMetadata {
    return when (category) {
        JunkCategory.CORPSES -> JunkCategoryMetadata(
            title = "Orphan App Leftovers",
            subtitle = "Residual folders from uninstalled apps",
            icon = Icons.Default.PersonRemove,
            accentColor = Color(0xFFA855F7)
        )
        JunkCategory.SYSTEM_TEMP -> JunkCategoryMetadata(
            title = "System & App Caches",
            subtitle = "Temporary files, crash dumps & cache logs",
            icon = Icons.Default.Description,
            accentColor = WarningAmber
        )
        JunkCategory.EMPTY_FOLDERS -> JunkCategoryMetadata(
            title = "Empty Ghost Folders",
            subtitle = "Abandoned 0-byte directory hierarchies",
            icon = Icons.Default.FolderOpen,
            accentColor = Color(0xFF00D2D3)
        )
        JunkCategory.SOCIAL_JUNK -> JunkCategoryMetadata(
            title = "Social Media Purger",
            subtitle = "WhatsApp & Telegram sent media & voice notes",
            icon = Icons.Default.GraphicEq,
            accentColor = SafeGreen
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    hasStoragePermission: Boolean,
    onRequestStoragePermission: () -> Unit
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val storageOverview by viewModel.storageOverview.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    val junkByCategory by viewModel.junkByCategory.collectAsState()
    val isCleaning by viewModel.isCleaning.collectAsState()
    val cleanProgress by viewModel.cleanProgress.collectAsState()
    val lastCleanResult by viewModel.lastCleanResult.collectAsState()

    var showCleanConfirmDialog by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var showSocialCleanerSheet by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val socialScanner = remember { SocialMediaScanner(context, viewModel.preferences) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val (topIcon, topTitle) = when (currentTab) {
                            AppTab.SWEEP -> Icons.Default.CleaningServices to "BytePurge"
                            AppTab.ANALYZER -> Icons.Default.PieChart to "Storage Analyzer"
                            AppTab.DUPLICATES -> Icons.Default.ContentCopy to "Duplicate Finder"
                            AppTab.APPS -> Icons.Default.Apps to "App Manager"
                        }
                        Icon(
                            imageVector = topIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = topTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        when (currentTab) {
                            AppTab.SWEEP -> viewModel.refreshStorageOverview()
                            AppTab.ANALYZER -> viewModel.runStorageAnalysis()
                            AppTab.DUPLICATES -> viewModel.scanDuplicates()
                            AppTab.APPS -> viewModel.loadInstalledApps()
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { showHistorySheet = true }) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Cleaning Savings Log",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showSettingsSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == AppTab.SWEEP,
                    onClick = { viewModel.selectTab(AppTab.SWEEP) },
                    icon = { Icon(Icons.Default.CleaningServices, contentDescription = "Byte Sweep") },
                    label = { Text("Byte Sweep", fontSize = 11.sp, fontWeight = if (currentTab == AppTab.SWEEP) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    )
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.ANALYZER,
                    onClick = { viewModel.selectTab(AppTab.ANALYZER) },
                    icon = { Icon(Icons.Default.PieChart, contentDescription = "Storage Analyzer") },
                    label = { Text("Analyzer", fontSize = 11.sp, fontWeight = if (currentTab == AppTab.ANALYZER) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    )
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.DUPLICATES,
                    onClick = { viewModel.selectTab(AppTab.DUPLICATES) },
                    icon = { Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate Finder") },
                    label = { Text("Duplicates", fontSize = 11.sp, fontWeight = if (currentTab == AppTab.DUPLICATES) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    )
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.APPS,
                    onClick = { viewModel.selectTab(AppTab.APPS) },
                    icon = { Icon(Icons.Default.Apps, contentDescription = "App Manager") },
                    label = { Text("Apps", fontSize = 11.sp, fontWeight = if (currentTab == AppTab.APPS) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    ) { innerPadding ->
        when (currentTab) {
            AppTab.ANALYZER -> {
                StorageAnalyzerScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            AppTab.DUPLICATES -> {
                DuplicateFinderScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            AppTab.APPS -> {
                AppManagerScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            AppTab.SWEEP -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    // Permission Banner (if MANAGE_EXTERNAL_STORAGE is not granted)
                    if (!hasStoragePermission) {
                        item {
                            PermissionWarningCard(onRequestPermission = onRequestStoragePermission)
                        }
                    }

                    // Compact Hero Storage Gauge
                    item {
                        StorageMeterCard(
                            usedPercentage = storageOverview.usedPercentage,
                            reclaimableBytes = storageOverview.reclaimableBytes,
                            freeBytes = storageOverview.freeBytes,
                            totalBytes = storageOverview.totalBytes,
                            isScanning = scanProgress.isScanning,
                            isCleaning = isCleaning
                        )
                    }

                    // Action Buttons (Scan / Clean)
                    item {
                        ActionButtons(
                            isScanning = scanProgress.isScanning,
                            isCleaning = isCleaning,
                            reclaimableBytes = storageOverview.reclaimableBytes,
                            onStartScan = { viewModel.startScan() },
                            onStartClean = { showCleanConfirmDialog = true }
                        )
                    }

                    // Active Scanning / Cleaning Progress Bar
                    if (scanProgress.isScanning || isCleaning) {
                        item {
                            ScanningStatusCard(
                                isScanning = scanProgress.isScanning,
                                isCleaning = isCleaning,
                                scanProgress = scanProgress,
                                cleanProgress = cleanProgress
                            )
                        }
                    }

                    // Category Header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Clean Suggestions",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            if (storageOverview.reclaimableBytes > 0) {
                                Text(
                                    "Ready to clean",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SafeGreen
                                )
                            }
                        }
                    }

                    // Category Cards
                    items(JunkCategory.values()) { category ->
                        val items = junkByCategory[category].orEmpty()
                        CategoryCard(
                            category = category,
                            items = items,
                            onToggleItem = { item -> viewModel.toggleItemSelection(category, item) },
                            onToggleAll = { selectAll -> viewModel.toggleCategorySelection(category, selectAll) },
                            onOpenSocialPurger = if (category == JunkCategory.SOCIAL_JUNK) {
                                { showSocialCleanerSheet = true }
                            } else null
                        )
                    }

                    // Deep Clean Tools Section
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Quick Tools & Deep Cleaning",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Quick Tool 1: Social Media Purger
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f).clickable { showSocialCleanerSheet = true }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Surface(
                                        color = SafeGreen.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Social Media", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                                    Text("Voice & sent media", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                }
                            }

                            // Quick Tool 2: Duplicate Hunter
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f).clickable { viewModel.selectTab(AppTab.DUPLICATES) }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Surface(
                                        color = Color(0xFFA855F7).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFFA855F7), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Duplicates", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                                    Text("SHA-256 detection", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                }
                            }

                            // Quick Tool 3: Storage Radar
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f).clickable { viewModel.selectTab(AppTab.ANALYZER) }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Surface(
                                        color = Color(0xFF00D2D3).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.PieChart, contentDescription = null, tint = Color(0xFF00D2D3), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Big Files", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                                    Text("Radar & heatmaps", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }

    // Clean Confirmation Dialog
    if (showCleanConfirmDialog) {
        val totalSelected = junkByCategory.values.flatten().count { it.isSelected }
        val reclaimableFormatted = JunkItem.formatSize(storageOverview.reclaimableBytes)

        AlertDialog(
            onDismissRequest = { showCleanConfirmDialog = false },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = SafeGreen) },
            title = { Text("Safe Clean Confirmation") },
            text = {
                Text(
                    "You are about to safely remove $totalSelected junk items and reclaim $reclaimableFormatted of storage. Continue?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCleanConfirmDialog = false
                        viewModel.cleanSelected()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SafeGreen)
                ) {
                    Text("Clean Now", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCleanConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clean Completed Celebration Dialog
    lastCleanResult?.let { result ->
        CleanCelebrationDialog(
            result = result,
            onDismiss = { viewModel.dismissCleanResult() }
        )
    }

    // Settings & Whitelist Bottom Sheet
    if (showSettingsSheet) {
        SettingsBottomSheet(
            viewModel = viewModel,
            onDismiss = { showSettingsSheet = false }
        )
    }

    // Cleaning History & Savings Log Sheet
    if (showHistorySheet) {
        CleaningHistorySheet(
            preferences = viewModel.preferences,
            onDismiss = { showHistorySheet = false }
        )
    }

    // Interactive Social Media Purger Sheet
    if (showSocialCleanerSheet) {
        val rootDir = remember { android.os.Environment.getExternalStorageDirectory() }
        SocialMediaCleanerSheet(
            scanner = socialScanner,
            roots = listOf(rootDir),
            onRecordClean = { freed, count ->
                viewModel.preferences.recordClean(freed, count, "Social Media Purger")
                viewModel.refreshStorageOverview()
            },
            onDismiss = { showSocialCleanerSheet = false }
        )
    }
}

@Composable
fun PermissionWarningCard(onRequestPermission: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = WarningAmber.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = WarningAmber)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Storage Access Required",
                    fontWeight = FontWeight.Bold,
                    color = WarningAmber,
                    fontSize = 15.sp
                )
                Text(
                    "Grant All Files access to allow scanning orphan folders and junk files.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Grant", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun StorageMeterCard(
    usedPercentage: Float,
    reclaimableBytes: Long,
    freeBytes: Long,
    totalBytes: Long,
    isScanning: Boolean,
    isCleaning: Boolean
) {
    val animatedUsed by animateFloatAsState(
        targetValue = usedPercentage.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800),
        label = "usedProgress"
    )

    val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row: Free space big metric + Health Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = JunkItem.formatSize(freeBytes),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "free",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${(usedPercentage * 100).toInt()}% used of ${JunkItem.formatSize(totalBytes)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = when {
                        reclaimableBytes > 0 -> SafeGreen.copy(alpha = 0.15f)
                        usedPercentage > 0.90f -> DangerRed.copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when {
                                reclaimableBytes > 0 -> Icons.Default.DeleteSweep
                                usedPercentage > 0.90f -> Icons.Default.Warning
                                else -> Icons.Default.CheckCircle
                            },
                            contentDescription = null,
                            tint = when {
                                reclaimableBytes > 0 -> SafeGreen
                                usedPercentage > 0.90f -> DangerRed
                                else -> MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                reclaimableBytes > 0 -> "${JunkItem.formatSize(reclaimableBytes)} Junk"
                                usedPercentage > 0.90f -> "Low Space"
                                else -> "Storage Clean"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                reclaimableBytes > 0 -> SafeGreen
                                usedPercentage > 0.90f -> DangerRed
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Multi-segment storage heatbar
            val usedRatio = animatedUsed
            val junkRatio = if (totalBytes > 0) (reclaimableBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
            val cleanUsedRatio = (usedRatio - junkRatio).coerceAtLeast(0.01f)
            val freeRatio = (1f - usedRatio).coerceAtLeast(0.01f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(DarkSurface)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(cleanUsedRatio)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    if (junkRatio > 0.001f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(junkRatio.coerceAtLeast(0.02f))
                                .background(SafeGreen)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(freeRatio)
                            .background(DarkSurfaceVariant.copy(alpha = 0.8f))
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Legend / Stats summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Used: ${JunkItem.formatSize(usedBytes)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                if (reclaimableBytes > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(SafeGreen))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Junk: ${JunkItem.formatSize(reclaimableBytes)}", fontSize = 11.sp, color = SafeGreen, fontWeight = FontWeight.SemiBold)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(DarkSurfaceVariant))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Free: ${JunkItem.formatSize(freeBytes)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun ActionButtons(
    isScanning: Boolean,
    isCleaning: Boolean,
    reclaimableBytes: Long,
    onStartScan: () -> Unit,
    onStartClean: () -> Unit
) {
    if (reclaimableBytes > 0 && !isScanning && !isCleaning) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onStartScan,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Re-scan", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }

            Button(
                onClick = onStartClean,
                modifier = Modifier.weight(2f).height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SafeGreen)
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.Black, modifier = Modifier.size(19.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Clean ${JunkItem.formatSize(reclaimableBytes)}",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    } else {
        Button(
            onClick = onStartScan,
            enabled = !isScanning && !isCleaning,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            if (isScanning) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Scanning Storage…", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            } else if (isCleaning) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Purging Selected Junk…", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            } else {
                Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Scan Storage for Junk", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
fun ScanningStatusCard(
    isScanning: Boolean,
    isCleaning: Boolean,
    scanProgress: com.storagemaid.app.core.model.ScanProgress,
    cleanProgress: Pair<Int, Int>?
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isCleaning) {
                        "Cleaning: ${cleanProgress?.first ?: 0} / ${cleanProgress?.second ?: 0}"
                    } else {
                        "Checked ${scanProgress.itemsScanned} items (${scanProgress.junkItemsFound} junk found)"
                    },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = scanProgress.currentPath,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CategoryCard(
    category: JunkCategory,
    items: List<JunkItem>,
    onToggleItem: (JunkItem) -> Unit,
    onToggleAll: (Boolean) -> Unit,
    onOpenSocialPurger: (() -> Unit)? = null
) {
    var isExpanded by remember { mutableStateOf(false) }
    val totalSize = items.sumOf { it.sizeBytes }
    val allSelected = items.isNotEmpty() && items.all { it.isSelected }
    val meta = getCategoryMetadata(category)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
        border = BorderStroke(
            1.dp,
            if (items.isNotEmpty()) meta.accentColor.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Colored Category Icon Box
                Surface(
                    color = meta.accentColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = meta.icon,
                            contentDescription = null,
                            tint = meta.accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = meta.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (items.isNotEmpty()) {
                            "${items.size} items • ${JunkItem.formatSize(totalSize)}"
                        } else {
                            meta.subtitle
                        },
                        fontSize = 11.sp,
                        color = if (items.isNotEmpty()) meta.accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (items.isNotEmpty()) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (items.isNotEmpty()) {
                    Checkbox(
                        checked = allSelected,
                        onCheckedChange = { onToggleAll(!allSelected) },
                        colors = CheckboxDefaults.colors(checkedColor = meta.accentColor)
                    )
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Clean",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (category == JunkCategory.SOCIAL_JUNK && onOpenSocialPurger != null) {
                OutlinedButton(
                    onClick = onOpenSocialPurger,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(16.dp), tint = SafeGreen)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Visual Purger (Voice Notes, Sent Media)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            AnimatedVisibility(visible = isExpanded && items.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(2.dp))
                    items.forEach { item ->
                        JunkItemRow(item = item, onToggle = { onToggleItem(item) })
                    }
                }
            }
        }
    }
}

@Composable
fun JunkItemRow(
    item: JunkItem,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onToggle() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = item.isSelected,
            onCheckedChange = { onToggle() },
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = TextPrimary
            )
            Text(
                text = item.path,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = item.formattedSize,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (item.riskLevel == RiskLevel.SAFE) SafeGreen else WarningAmber
        )
    }
}
