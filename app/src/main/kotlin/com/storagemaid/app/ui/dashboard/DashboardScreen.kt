package com.storagemaid.app.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.StrokeCap
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
import com.storagemaid.app.ui.theme.DarkSurfaceVariant
import com.storagemaid.app.ui.theme.SafeGreen
import com.storagemaid.app.ui.theme.WarningAmber

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
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = topTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
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
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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
                    label = { Text("Byte Sweep", fontSize = 11.sp) },
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
                    label = { Text("Analyzer", fontSize = 11.sp) },
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
                    label = { Text("Duplicates", fontSize = 11.sp) },
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
                    label = { Text("Apps", fontSize = 11.sp) },
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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Permission Banner (if MANAGE_EXTERNAL_STORAGE is not granted)
                    if (!hasStoragePermission) {
                        item {
                            PermissionWarningCard(onRequestPermission = onRequestStoragePermission)
                        }
                    }

                    // Storage Gauge Header
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

                    // Action Button (Scan / Clean)
                    item {
                        ActionButtons(
                            isScanning = scanProgress.isScanning,
                            isCleaning = isCleaning,
                            reclaimableBytes = storageOverview.reclaimableBytes,
                            onStartScan = { viewModel.startScan() },
                            onStartClean = { showCleanConfirmDialog = true }
                        )
                    }

                    // Scanning / Cleaning Progress Bar
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
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
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
    val animatedProgress by animateFloatAsState(
        targetValue = usedPercentage,
        animationSpec = tween(durationMillis = 800),
        label = "storageProgress"
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(216.dp)
            ) {
                // Background Track
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 12.dp
                )

                // Used Storage Indicator
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 12.dp,
                    strokeCap = StrokeCap.Round
                )

                // Center Content: Modern Icon Badge + Size Text
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    val statusIcon = when {
                        isCleaning -> Icons.Default.CleaningServices
                        isScanning -> Icons.Default.Search
                        reclaimableBytes > 0 -> Icons.Default.DeleteSweep
                        else -> Icons.Default.CheckCircle
                    }
                    val statusColor = when {
                        isCleaning || isScanning -> MaterialTheme.colorScheme.primary
                        else -> SafeGreen
                    }

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (reclaimableBytes > 0) JunkItem.formatSize(reclaimableBytes) else "Clean",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (reclaimableBytes > 0) SafeGreen else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (reclaimableBytes > 0) "Reclaimable Junk" else "Storage Optimized",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Storage Details Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Free Space", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(JunkItem.formatSize(freeBytes), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total Storage", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(JunkItem.formatSize(totalBytes), fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Scan")
            }

            Button(
                onClick = onStartClean,
                modifier = Modifier.weight(2f),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SafeGreen)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Clean ${JunkItem.formatSize(reclaimableBytes)}",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    } else {
        Button(
            onClick = onStartScan,
            enabled = !isScanning && !isCleaning,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Search, contentDescription = null)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                if (isScanning) "Scanning Storage…" else if (isCleaning) "Cleaning…" else "Scan Storage",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.5.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (isCleaning) {
                        "Cleaning: ${cleanProgress?.first ?: 0} / ${cleanProgress?.second ?: 0}"
                    } else {
                        "Checked ${scanProgress.itemsScanned} items (${scanProgress.junkItemsFound} junk found)"
                    },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = scanProgress.currentPath,
                fontSize = 12.sp,
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

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Icon
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (category) {
                            JunkCategory.CORPSES -> Icons.Default.PersonRemove
                            JunkCategory.SYSTEM_TEMP -> Icons.Default.Description
                            JunkCategory.EMPTY_FOLDERS -> Icons.Default.FolderOpen
                            JunkCategory.SOCIAL_JUNK -> Icons.Default.ChatBubbleOutline
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "${items.size} items • ${JunkItem.formatSize(totalSize)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (items.isNotEmpty()) {
                    Checkbox(
                        checked = allSelected,
                        onCheckedChange = { onToggleAll(!allSelected) }
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (category == JunkCategory.SOCIAL_JUNK && onOpenSocialPurger != null) {
                OutlinedButton(
                    onClick = onOpenSocialPurger,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Visual Purger (Voice Notes, Sent Media)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            AnimatedVisibility(visible = isExpanded && items.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
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
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.path,
                fontSize = 11.sp,
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
