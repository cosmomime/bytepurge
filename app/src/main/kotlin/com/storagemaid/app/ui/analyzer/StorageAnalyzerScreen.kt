package com.storagemaid.app.ui.analyzer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.storagemaid.app.core.analyzer.StorageBenchmarkResult
import com.storagemaid.app.core.model.DeviceHardwareStats
import com.storagemaid.app.core.model.JunkItem
import com.storagemaid.app.core.model.LargeFileItem
import com.storagemaid.app.core.model.LargeFileSort
import com.storagemaid.app.core.model.LargeFileThreshold
import com.storagemaid.app.core.model.LargeFileType
import com.storagemaid.app.core.model.StorageBreakdown
import com.storagemaid.app.ui.dashboard.DashboardViewModel
import com.storagemaid.app.ui.theme.CategoryApks
import com.storagemaid.app.ui.theme.CategoryArchives
import com.storagemaid.app.ui.theme.CategoryAudio
import com.storagemaid.app.ui.theme.CategoryDocuments
import com.storagemaid.app.ui.theme.CategoryImages
import com.storagemaid.app.ui.theme.CategoryOther
import com.storagemaid.app.ui.theme.CategoryVideo
import com.storagemaid.app.ui.theme.DangerRed
import com.storagemaid.app.ui.theme.DarkSurfaceVariant
import com.storagemaid.app.ui.theme.SafeGreen
import com.storagemaid.app.ui.theme.WarningAmber
import java.io.File

@Composable
fun StorageAnalyzerScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val breakdown by viewModel.storageBreakdown.collectAsState()
    val largeFiles by viewModel.filteredLargeFiles.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val statusText by viewModel.analyzerStatus.collectAsState()
    val selectedThreshold by viewModel.selectedThreshold.collectAsState()
    val selectedType by viewModel.selectedFilterType.collectAsState()
    val onlyStaleDownloads by viewModel.onlyStaleDownloads.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val deleteResult by viewModel.lastDeleteResult.collectAsState()
    val hardwareStats by viewModel.hardwareStats.collectAsState()
    val benchmarkResult by viewModel.benchmarkResult.collectAsState()
    val isRunningBenchmark by viewModel.isRunningBenchmark.collectAsState()
    val benchmarkStatus by viewModel.benchmarkStatus.collectAsState()
    val isFolderExplorerOpen by viewModel.isFolderExplorerOpen.collectAsState()

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var selectedFileToPreview by remember { mutableStateOf<LargeFileItem?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }

    val selectedCount = largeFiles.count { it.isSelected }
    val selectedBytes = largeFiles.filter { it.isSelected }.sumOf { it.sizeBytes }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Storage Breakdown Card
            item {
                StorageBreakdownCard(
                    breakdown = breakdown,
                    isAnalyzing = isAnalyzing,
                    onRefresh = { viewModel.runStorageAnalysis() },
                    onExploreFolders = { viewModel.openFolderExplorer() },
                    onSelectCategory = { viewModel.setFilterType(it) }
                )
            }

            // Device & RAM Hardware Health Card
            item {
                DeviceHardwareCard(
                    stats = hardwareStats,
                    benchmarkResult = benchmarkResult,
                    isRunningBenchmark = isRunningBenchmark,
                    benchmarkStatus = benchmarkStatus,
                    onRunBenchmark = { viewModel.runStorageBenchmark() }
                )
            }

            // Big File Hunter Header & Controls
            item {
                BigFileHunterControls(
                    totalCount = largeFiles.size,
                    totalBytes = largeFiles.sumOf { it.sizeBytes },
                    selectedThreshold = selectedThreshold,
                    onThresholdSelected = { viewModel.setThreshold(it) },
                    selectedType = selectedType,
                    onTypeSelected = { viewModel.setFilterType(it) },
                    onlyStaleDownloads = onlyStaleDownloads,
                    onToggleStaleDownloads = { viewModel.toggleOnlyStaleDownloads() },
                    sortOrder = sortOrder,
                    onSortOrderSelected = { viewModel.setSortOrder(it) },
                    allSelected = largeFiles.isNotEmpty() && largeFiles.all { it.isSelected },
                    onToggleSelectAll = { viewModel.selectAllLargeFiles(it) }
                )
            }

            // Status message during scanning
            if (isAnalyzing && statusText.isNotBlank()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(statusText, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Empty state
            if (!isAnalyzing && largeFiles.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = SafeGreen,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No large files found",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                "No files match the ${selectedThreshold.label} threshold.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Large File Items
            items(largeFiles, key = { it.id }) { item ->
                LargeFileRowItem(
                    item = item,
                    onToggle = { viewModel.toggleLargeFileSelection(item.id) },
                    onClick = {
                        openFile(context, item.file)
                    }
                )
            }
        }

        // Floating Batch Action Bar (when 1 or more files are selected)
        AnimatedVisibility(
            visible = selectedCount > 0,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "$selectedCount selected",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = JunkItem.formatSize(selectedBytes),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = {
                                val selectedFiles = largeFiles.filter { it.isSelected }.map { it.file }
                                shareFiles(context, selectedFiles)
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { showDeleteConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = DangerRed) },
            title = { Text("Delete $selectedCount Big Files?") },
            text = {
                Text(
                    "You are about to permanently delete $selectedCount file(s) freeing ${JunkItem.formatSize(selectedBytes)}. This action cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteSelectedLargeFiles()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Delete Permanently", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Success Toast / Alert
    deleteResult?.let { (count, freed) ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteResult() },
            title = { Text("Files Deleted") },
            text = {
                Text("Successfully deleted $count file(s) and reclaimed ${JunkItem.formatSize(freed)}.")
            },
            confirmButton = {
                Button(onClick = { viewModel.dismissDeleteResult() }) {
                    Text("OK")
                }
            }
        )
    }

    if (isFolderExplorerOpen) {
        FolderExplorerSheet(
            viewModel = viewModel,
            onDismiss = { viewModel.closeFolderExplorer() }
        )
    }
}

@Composable
fun StorageBreakdownCard(
    breakdown: StorageBreakdown?,
    isAnalyzing: Boolean,
    onRefresh: () -> Unit,
    onExploreFolders: () -> Unit,
    onSelectCategory: (LargeFileType) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Storage Breakdown",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = if (breakdown != null) {
                            "Used ${JunkItem.formatSize(breakdown.usedBytes)} of ${JunkItem.formatSize(breakdown.totalBytes)}"
                        } else {
                            "Calculating storage distribution…"
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    enabled = !isAnalyzing
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Multi-segment Segmented Bar
            MultiSegmentStorageBar(breakdown = breakdown)

            Spacer(modifier = Modifier.height(18.dp))

            // Category Legend Grid
            if (breakdown != null) {
                val categories = listOf(
                    CategoryItem("Videos", breakdown.videosBytes, CategoryVideo, LargeFileType.VIDEO),
                    CategoryItem("Images", breakdown.imagesBytes, CategoryImages, null),
                    CategoryItem("Audio", breakdown.audioBytes, CategoryAudio, LargeFileType.AUDIO),
                    CategoryItem("Docs", breakdown.documentsBytes, CategoryDocuments, LargeFileType.DOCUMENT),
                    CategoryItem("Archives", breakdown.archivesBytes, CategoryArchives, LargeFileType.ARCHIVE),
                    CategoryItem("APKs", breakdown.apksBytes, CategoryApks, LargeFileType.APK),
                    CategoryItem("Other/System", breakdown.otherBytes, CategoryOther, null),
                    CategoryItem("Free Space", breakdown.freeBytes, MaterialTheme.colorScheme.surfaceVariant, null)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in categories.indices step 2) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            CategoryLegendChip(
                                item = categories[i],
                                totalBytes = breakdown.totalBytes,
                                onClick = categories[i].type?.let { t -> { onSelectCategory(t) } },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            if (i + 1 < categories.size) {
                                CategoryLegendChip(
                                    item = categories[i + 1],
                                    totalBytes = breakdown.totalBytes,
                                    onClick = categories[i + 1].type?.let { t -> { onSelectCategory(t) } },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = onExploreFolders,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Explore Largest Folders & Heatmap",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

private data class CategoryItem(
    val name: String,
    val bytes: Long,
    val color: Color,
    val type: LargeFileType? = null
)

@Composable
private fun CategoryLegendChip(
    item: CategoryItem,
    totalBytes: Long,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val pct = if (totalBytes > 0) ((item.bytes.toFloat() / totalBytes) * 100).toInt() else 0
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .then(
                if (onClick != null) {
                    Modifier
                        .clickable(onClick = onClick)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                } else Modifier
            )
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(item.color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            item.name,
            fontSize = 12.sp,
            color = if (onClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            JunkItem.formatSize(item.bytes),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun MultiSegmentStorageBar(
    breakdown: StorageBreakdown?,
    modifier: Modifier = Modifier
) {
    val height = 14.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (breakdown != null && breakdown.totalBytes > 0) {
            Row(modifier = Modifier.fillMaxSize()) {
                val segments = listOf(
                    breakdown.videosBytes to CategoryVideo,
                    breakdown.imagesBytes to CategoryImages,
                    breakdown.audioBytes to CategoryAudio,
                    breakdown.documentsBytes to CategoryDocuments,
                    breakdown.archivesBytes to CategoryArchives,
                    breakdown.apksBytes to CategoryApks,
                    breakdown.otherBytes to CategoryOther
                )

                for ((bytes, color) in segments) {
                    val weight = (bytes.toFloat() / breakdown.totalBytes).coerceAtLeast(0.001f)
                    if (bytes > 0) {
                        Box(
                            modifier = Modifier
                                .weight(weight)
                                .height(height)
                                .background(color)
                        )
                    }
                }

                // Remaining free space
                val freeWeight = (breakdown.freeBytes.toFloat() / breakdown.totalBytes).coerceAtLeast(0.001f)
                Box(
                    modifier = Modifier
                        .weight(freeWeight)
                        .height(height)
                        .background(Color.Transparent)
                )
            }
        }
    }
}

@Composable
fun BigFileHunterControls(
    totalCount: Int,
    totalBytes: Long,
    selectedThreshold: LargeFileThreshold,
    onThresholdSelected: (LargeFileThreshold) -> Unit,
    selectedType: LargeFileType?,
    onTypeSelected: (LargeFileType?) -> Unit,
    onlyStaleDownloads: Boolean,
    onToggleStaleDownloads: () -> Unit,
    sortOrder: LargeFileSort,
    onSortOrderSelected: (LargeFileSort) -> Unit,
    allSelected: Boolean,
    onToggleSelectAll: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Big File Hunter",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Text(
                    text = "$totalCount files • ${JunkItem.formatSize(totalBytes)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Select all checkbox
                Checkbox(
                    checked = allSelected,
                    onCheckedChange = onToggleSelectAll,
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                )
                Text("Select All", fontSize = 12.sp)
            }
        }

        // Threshold Selector Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LargeFileThreshold.values().forEach { threshold ->
                FilterChip(
                    selected = selectedThreshold == threshold,
                    onClick = { onThresholdSelected(threshold) },
                    label = { Text(threshold.label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        // File Type Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = onlyStaleDownloads,
                onClick = onToggleStaleDownloads,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                },
                label = { Text("Old Downloads (>30d)", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = WarningAmber.copy(alpha = 0.2f),
                    selectedLabelColor = WarningAmber
                )
            )

            FilterChip(
                selected = selectedType == null && !onlyStaleDownloads,
                onClick = { onTypeSelected(null) },
                label = { Text("All Types", fontSize = 12.sp) }
            )

            LargeFileType.values().forEach { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { onTypeSelected(type) },
                    label = { Text(type.displayName, fontSize = 12.sp) }
                )
            }
        }
    }
}

@Composable
fun LargeFileRowItem(
    item: LargeFileItem,
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // File Type Icon
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(getTypeColor(item.type).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getTypeIcon(item.type),
                    contentDescription = null,
                    tint = getTypeColor(item.type),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // File Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.formattedSize,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = getTypeColor(item.type)
                    )
                    Text(" • ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = item.parentFolder,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(" • ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = item.formattedDate,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (item.isStaleDownload) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(WarningAmber.copy(alpha = 0.2f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "Old Download",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarningAmber
                            )
                        }
                    }
                }
            }

            Checkbox(
                checked = item.isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
            )
        }
    }
}

private fun getTypeIcon(type: LargeFileType): ImageVector {
    return when (type) {
        LargeFileType.VIDEO -> Icons.Default.Videocam
        LargeFileType.ARCHIVE -> Icons.Default.Archive
        LargeFileType.APK -> Icons.Default.FolderOpen
        LargeFileType.AUDIO -> Icons.Default.AudioFile
        LargeFileType.DOCUMENT -> Icons.Default.Description
        LargeFileType.OTHER -> Icons.AutoMirrored.Filled.InsertDriveFile
    }
}

private fun getTypeColor(type: LargeFileType): Color {
    return when (type) {
        LargeFileType.VIDEO -> CategoryVideo
        LargeFileType.ARCHIVE -> CategoryArchives
        LargeFileType.APK -> CategoryApks
        LargeFileType.AUDIO -> CategoryAudio
        LargeFileType.DOCUMENT -> CategoryDocuments
        LargeFileType.OTHER -> CategoryOther
    }
}

private fun openFile(context: Context, file: File) {
    try {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val ext = file.extension.lowercase()
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot open file: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun shareFiles(context: Context, files: List<File>) {
    if (files.isEmpty()) return
    try {
        if (files.size == 1) {
            val file = files[0]
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val ext = file.extension.lowercase()
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Share file"))
        } else {
            val uris = ArrayList<Uri>()
            for (file in files) {
                uris.add(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
            }
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Share ${files.size} files"))
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot share files: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun DeviceHardwareCard(
    stats: DeviceHardwareStats,
    benchmarkResult: StorageBenchmarkResult?,
    isRunningBenchmark: Boolean,
    benchmarkStatus: String,
    onRunBenchmark: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Device & RAM Health",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "${stats.deviceModel} • Android ${stats.androidVersion}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (stats.isLowMemory) WarningAmber.copy(alpha = 0.15f)
                            else SafeGreen.copy(alpha = 0.15f)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (stats.isLowMemory) "High Pressure" else "RAM Optimal",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (stats.isLowMemory) WarningAmber else SafeGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // RAM Bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Memory Usage (${(stats.ramUsagePercentage * 100).toInt()}%)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${stats.formattedUsedRam} / ${stats.formattedTotalRam}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { stats.ramUsagePercentage },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (stats.isLowMemory) WarningAmber else MaterialTheme.colorScheme.primary,
                    trackColor = DarkSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Available: ${stats.formattedAvailRam}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "API ${stats.apiLevel}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            )

            // Flash Storage Speed Benchmark Section
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Storage I/O Speed",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    if (benchmarkResult != null && !isRunningBenchmark) {
                        TextButton(
                            onClick = onRunBenchmark,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Retest", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (isRunningBenchmark) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = benchmarkStatus.ifBlank { "Testing read/write speed…" },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (benchmarkResult != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Write: ${benchmarkResult.formattedWriteSpeed} • Read: ${benchmarkResult.formattedReadSpeed}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Grade: ${benchmarkResult.grade}",
                                fontSize = 11.sp,
                                color = SafeGreen
                            )
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = onRunBenchmark,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Benchmark Flash Speed", fontSize = 12.sp)
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            )

            // Battery, Thermal & System Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Battery & Thermal
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val batteryColor = when {
                        stats.batteryTempCelsius > 42f -> DangerRed
                        stats.batteryTempCelsius > 38f -> WarningAmber
                        else -> SafeGreen
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(batteryColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (stats.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                            contentDescription = null,
                            tint = batteryColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (stats.batteryPct >= 0) "${stats.batteryPct}% • ${stats.batteryTempCelsius}°C" else "Battery ${stats.batteryTempCelsius}°C",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (stats.isCharging) "Charging • ${if (stats.batteryTempCelsius > 38f) "Warm" else "Cool"}" else "Discharging • ${if (stats.batteryTempCelsius > 38f) "Warm" else "Normal"}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // CPU & Uptime
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${stats.cpuCores} CPU Cores",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Uptime: ${stats.formattedUptime}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
