package com.storagemaid.app.ui.duplicates

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterFrames
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.storagemaid.app.core.model.DuplicateFileItem
import com.storagemaid.app.core.model.DuplicateGroup
import com.storagemaid.app.core.model.DuplicateMode
import com.storagemaid.app.core.model.JunkItem
import com.storagemaid.app.core.model.SimilarPhotoGroup
import com.storagemaid.app.core.model.SimilarPhotoItem
import com.storagemaid.app.ui.dashboard.DashboardViewModel
import com.storagemaid.app.ui.theme.DangerRed
import com.storagemaid.app.ui.theme.DarkSurfaceVariant
import com.storagemaid.app.ui.theme.SafeGreen
import com.storagemaid.app.ui.theme.WarningAmber
import java.io.File

@Composable
fun DuplicateFinderScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentMode by viewModel.duplicateMode.collectAsState()

    // Exact Duplicates State
    val duplicateGroups by viewModel.duplicateGroups.collectAsState()
    val isScanningExact by viewModel.isScanningDuplicates.collectAsState()
    val scanStatusExact by viewModel.duplicateScanStatus.collectAsState()
    val deleteResultExact by viewModel.duplicateDeleteResult.collectAsState()

    // Similar Photos State
    val similarGroups by viewModel.similarPhotoGroups.collectAsState()
    val isScanningSimilar by viewModel.isScanningSimilar.collectAsState()
    val scanStatusSimilar by viewModel.similarScanStatus.collectAsState()
    val deleteResultSimilar by viewModel.similarDeleteResult.collectAsState()

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val allFilesExact = duplicateGroups.flatMap { it.files }
    val totalDuplicateFilesExact = duplicateGroups.sumOf { it.duplicateCount }
    val totalReclaimableExact = duplicateGroups.sumOf { it.reclaimableBytes }
    val selectedFilesExact = allFilesExact.filter { it.isSelected }
    val selectedBytesExact = selectedFilesExact.sumOf { it.sizeBytes }

    val allPhotosSimilar = similarGroups.flatMap { it.photos }
    val totalSimilarPhotos = similarGroups.sumOf { it.reclaimableCount }
    val totalReclaimableSimilar = similarGroups.sumOf { it.reclaimableBytes }
    val selectedPhotosSimilar = allPhotosSimilar.filter { it.isSelected }
    val selectedBytesSimilar = selectedPhotosSimilar.sumOf { it.sizeBytes }

    Column(modifier = modifier.fillMaxSize()) {
        // Tab Row Switcher (Exact Duplicates vs Similar Photos)
        TabRow(
            selectedTabIndex = currentMode.ordinal,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[currentMode.ordinal]),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            DuplicateMode.values().forEach { mode ->
                Tab(
                    selected = currentMode == mode,
                    onClick = { viewModel.setDuplicateMode(mode) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (mode == DuplicateMode.EXACT) Icons.Default.ContentCopy else Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(mode.label, fontWeight = if (currentMode == mode) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp)
                        }
                    }
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (currentMode == DuplicateMode.EXACT) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        DuplicateOverviewCard(
                            groupCount = duplicateGroups.size,
                            duplicateFileCount = totalDuplicateFilesExact,
                            reclaimableBytes = totalReclaimableExact,
                            isScanning = isScanningExact,
                            onStartScan = { viewModel.scanDuplicates() },
                            onAutoSelect = { viewModel.autoSelectDuplicates() },
                            onDeselectAll = { viewModel.deselectAllDuplicates() }
                        )
                    }

                    if (isScanningExact && scanStatusExact.isNotBlank()) {
                        item {
                            ScanningCard(status = scanStatusExact)
                        }
                    }

                    if (!isScanningExact && duplicateGroups.isEmpty()) {
                        item {
                            EmptyDuplicateCard(title = "No duplicate files found", subtitle = "Your storage is clean of exact identical files.")
                        }
                    }

                    items(duplicateGroups, key = { it.hash }) { group ->
                        DuplicateGroupCard(
                            group = group,
                            onToggleFile = { fileId -> viewModel.toggleDuplicateSelection(group.hash, fileId) },
                            onOpenFile = { file -> openFile(context, file) }
                        )
                    }
                }
            } else {
                // Similar Photos List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        SimilarPhotoOverviewCard(
                            groupCount = similarGroups.size,
                            photoCount = totalSimilarPhotos,
                            reclaimableBytes = totalReclaimableSimilar,
                            isScanning = isScanningSimilar,
                            onStartScan = { viewModel.scanSimilarPhotos() },
                            onAutoSelect = { viewModel.autoSelectSimilarPhotos() },
                            onDeselectAll = { viewModel.deselectAllSimilarPhotos() }
                        )
                    }

                    if (isScanningSimilar && scanStatusSimilar.isNotBlank()) {
                        item {
                            ScanningCard(status = scanStatusSimilar)
                        }
                    }

                    if (!isScanningSimilar && similarGroups.isEmpty()) {
                        item {
                            EmptyDuplicateCard(title = "No similar or burst photos found", subtitle = "No burst shots or near-identical photos detected in Camera & Pictures.")
                        }
                    }

                    items(similarGroups, key = { it.groupId }) { group ->
                        SimilarPhotoGroupCard(
                            group = group,
                            onTogglePhoto = { photoId -> viewModel.toggleSimilarPhotoSelection(group.groupId, photoId) },
                            onSetBestShot = { photoId -> viewModel.setBestShot(group.groupId, photoId) },
                            onOpenPhoto = { file -> openFile(context, file) }
                        )
                    }
                }
            }

            // Floating Batch Action Bar
            val hasSelection = if (currentMode == DuplicateMode.EXACT) selectedFilesExact.isNotEmpty() else selectedPhotosSimilar.isNotEmpty()
            val selectedCount = if (currentMode == DuplicateMode.EXACT) selectedFilesExact.size else selectedPhotosSimilar.size
            val selectedBytes = if (currentMode == DuplicateMode.EXACT) selectedBytesExact else selectedBytesSimilar

            androidx.compose.animation.AnimatedVisibility(
                visible = hasSelection,
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
                                text = "Free up ${JunkItem.formatSize(selectedBytes)}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { showDeleteConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clean", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialog
    if (showDeleteConfirmDialog) {
        val count = if (currentMode == DuplicateMode.EXACT) selectedFilesExact.size else selectedPhotosSimilar.size
        val bytes = if (currentMode == DuplicateMode.EXACT) selectedBytesExact else selectedBytesSimilar
        val targetLabel = if (currentMode == DuplicateMode.EXACT) "duplicate file(s)" else "burst / similar photo(s)"

        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = DangerRed) },
            title = { Text("Delete $count $targetLabel?") },
            text = {
                Text(
                    "You are about to delete $count $targetLabel freeing ${JunkItem.formatSize(bytes)}. The original best shot will remain safe. Continue?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        if (currentMode == DuplicateMode.EXACT) {
                            viewModel.deleteSelectedDuplicates()
                        } else {
                            viewModel.deleteSelectedSimilarPhotos()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Deletion Result Dialogs
    deleteResultExact?.let { (count, freed) ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissDuplicateDeleteResult() },
            title = { Text("Duplicates Cleaned!") },
            text = { Text("Successfully removed $count duplicate file(s) and reclaimed ${JunkItem.formatSize(freed)}.") },
            confirmButton = {
                Button(onClick = { viewModel.dismissDuplicateDeleteResult() }) {
                    Text("OK")
                }
            }
        )
    }

    deleteResultSimilar?.let { (count, freed) ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissSimilarPhotoDeleteResult() },
            title = { Text("Similar Photos Cleaned!") },
            text = { Text("Successfully removed $count similar photo(s) and reclaimed ${JunkItem.formatSize(freed)}.") },
            confirmButton = {
                Button(onClick = { viewModel.dismissSimilarPhotoDeleteResult() }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun ScanningCard(status: String) {
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
            Text(status, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EmptyDuplicateCard(title: String, subtitle: String) {
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
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun DuplicateOverviewCard(
    groupCount: Int,
    duplicateFileCount: Int,
    reclaimableBytes: Long,
    isScanning: Boolean,
    onStartScan: () -> Unit,
    onAutoSelect: () -> Unit,
    onDeselectAll: () -> Unit
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Exact Duplicate Finder",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (duplicateFileCount > 0) {
                                "$duplicateFileCount redundant files in $groupCount groups"
                            } else {
                                "Byte-for-byte exact SHA-256 deduplication"
                            },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onStartScan,
                    enabled = !isScanning
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Scan",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("RECLAIMABLE SPACE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SafeGreen)
                    Text(JunkItem.formatSize(reclaimableBytes), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = SafeGreen)
                }
                Box(
                    modifier = Modifier
                        .background(SafeGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("$duplicateFileCount duplicates", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SafeGreen)
                }
            }

            if (groupCount > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onAutoSelect,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Auto-Select", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onDeselectAll,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Clear Selection", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SimilarPhotoOverviewCard(
    groupCount: Int,
    photoCount: Int,
    reclaimableBytes: Long,
    isScanning: Boolean,
    onStartScan: () -> Unit,
    onAutoSelect: () -> Unit,
    onDeselectAll: () -> Unit
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Similar & Burst Cleaner",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (photoCount > 0) {
                                "$photoCount redundant photos across $groupCount scenes"
                            } else {
                                "Perceptual dHash visual detection"
                            },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onStartScan,
                    enabled = !isScanning
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Scan",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("RECLAIMABLE BURST SPACE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SafeGreen)
                    Text(JunkItem.formatSize(reclaimableBytes), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = SafeGreen)
                }
                Box(
                    modifier = Modifier
                        .background(SafeGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("$photoCount burst shots", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SafeGreen)
                }
            }

            if (groupCount > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onAutoSelect,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Auto-Select", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onDeselectAll,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Clear Selection", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun DuplicateGroupCard(
    group: DuplicateGroup,
    onToggleFile: (String) -> Unit,
    onOpenFile: (File) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.files.firstOrNull()?.name ?: "Duplicate Group",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${group.files.size} identical files • ${group.files.firstOrNull()?.formattedSize ?: ""} each",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .background(
                            color = WarningAmber.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${group.duplicateCount} duplicate(s)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = WarningAmber
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                group.files.forEach { fileItem ->
                    DuplicateFileRow(
                        item = fileItem,
                        onToggle = { onToggleFile(fileItem.id) },
                        onOpen = { onOpenFile(fileItem.file) }
                    )
                }
            }
        }
    }
}

@Composable
fun DuplicateFileRow(
    item: DuplicateFileItem,
    onToggle: () -> Unit,
    onOpen: () -> Unit
) {
    val isImage = remember(item.file) {
        val ext = item.file.extension.lowercase()
        ext in setOf("jpg", "jpeg", "png", "webp", "bmp")
    }

    val thumbnailBitmap = remember(item.file) {
        if (isImage && item.file.exists()) {
            try {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(item.file.absolutePath, bounds)
                val targetDim = 96
                var sample = 1
                if (bounds.outHeight > targetDim || bounds.outWidth > targetDim) {
                    val halfHeight = bounds.outHeight / 2
                    val halfWidth = bounds.outWidth / 2
                    while ((halfHeight / sample) >= targetDim && (halfWidth / sample) >= targetDim) {
                        sample *= 2
                    }
                }
                val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                BitmapFactory.decodeFile(item.file.absolutePath, opts)
            } catch (_: Exception) { null }
        } else null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onOpen)
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (thumbnailBitmap != null) {
            Image(
                bitmap = thumbnailBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(DarkSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = if (item.isOriginal) SafeGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.isOriginal) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = SafeGreen.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "ORIGINAL",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafeGreen
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = item.parentFolder,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = item.path,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (!item.isOriginal) {
            Checkbox(
                checked = item.isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
            )
        } else {
            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Text("Safe", fontSize = 10.sp, color = SafeGreen, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SimilarPhotoGroupCard(
    group: SimilarPhotoGroup,
    onTogglePhoto: (String) -> Unit,
    onSetBestShot: (String) -> Unit,
    onOpenPhoto: (File) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Similar Scene (${group.photos.size} photos)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${group.photos.firstOrNull()?.parentFolder ?: "DCIM"} • Can save ${group.formattedReclaimable}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .background(
                            color = WarningAmber.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${group.reclaimableCount} to clean",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = WarningAmber
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                group.photos.forEach { photoItem ->
                    SimilarPhotoRow(
                        item = photoItem,
                        onToggle = { onTogglePhoto(photoItem.id) },
                        onSetBest = { onSetBestShot(photoItem.id) },
                        onOpen = { onOpenPhoto(photoItem.file) }
                    )
                }
            }
        }
    }
}

@Composable
fun SimilarPhotoRow(
    item: SimilarPhotoItem,
    onToggle: () -> Unit,
    onSetBest: () -> Unit,
    onOpen: () -> Unit
) {
    val thumbnailBitmap = remember(item.file) {
        if (item.file.exists()) {
            try {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(item.file.absolutePath, bounds)
                val targetDim = 120
                var sample = 1
                if (bounds.outHeight > targetDim || bounds.outWidth > targetDim) {
                    val halfHeight = bounds.outHeight / 2
                    val halfWidth = bounds.outWidth / 2
                    while ((halfHeight / sample) >= targetDim && (halfWidth / sample) >= targetDim) {
                        sample *= 2
                    }
                }
                val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                BitmapFactory.decodeFile(item.file.absolutePath, opts)
            } catch (_: Exception) { null }
        } else null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onOpen)
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (thumbnailBitmap != null) {
            Image(
                bitmap = thumbnailBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(DarkSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FilterFrames,
                    contentDescription = null,
                    tint = if (item.isBestShot) SafeGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.isBestShot) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = SafeGreen.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "BEST SHOT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafeGreen
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = item.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "${item.formattedSize} • ${item.formattedDate}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!item.isBestShot) {
            TextButton(
                onClick = onSetBest,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("Keep this", fontSize = 10.sp)
            }
            Checkbox(
                checked = item.isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
            )
        } else {
            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Star, contentDescription = "Best Shot", tint = SafeGreen, modifier = Modifier.size(18.dp))
            }
        }
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
