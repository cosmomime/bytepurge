package com.storagemaid.desktop.ui.gamer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storagemaid.desktop.engine.GamerJunkScanner
import com.storagemaid.desktop.model.DesktopJunkItem
import com.storagemaid.desktop.model.GamerCacheCategory
import com.storagemaid.desktop.model.GamerCacheItem
import com.storagemaid.desktop.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.io.File

@Composable
fun DesktopGamerJunkView(scanner: GamerJunkScanner) {
    val coroutineScope = rememberCoroutineScope()
    var isScanning by remember { mutableStateOf(false) }
    var scanResults by remember { mutableStateOf<Map<GamerCacheCategory, List<GamerCacheItem>>>(emptyMap()) }
    var expandedCategories by remember { mutableStateOf<Set<GamerCacheCategory>>(emptySet()) }
    var lastPurgedBytes by remember { mutableStateOf(0L) }
    var showPurgeSuccess by remember { mutableStateOf(false) }

    fun refreshScan() {
        coroutineScope.launch {
            isScanning = true
            val results = withContext(Dispatchers.IO) { scanner.scanGamerJunk() }
            scanResults = results
            // Keep collapsed by default so all categories and sizes fit on one screen
            expandedCategories = emptySet()
            isScanning = false
        }
    }

    LaunchedEffect(Unit) {
        refreshScan()
    }

    val allItems = scanResults.values.flatten()
    val totalFoundBytes = allItems.sumOf { it.sizeBytes }
    val selectedItems = allItems.filter { it.isSelected }
    val selectedReclaimBytes = selectedItems.sumOf { it.sizeBytes }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SportsEsports,
                        contentDescription = null,
                        tint = Color(0xFFA855F7), // Neon Purple
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Gamer Ghost Cache & Shader Hunter",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Hunt hidden match replays, Steam emulator sync blobs, DirectX shader pools, and launcher caches.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { refreshScan() },
                    enabled = !isScanning,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Re-Scan")
                }

                Button(
                    onClick = {
                        coroutineScope.launch {
                            val itemsToPurge = selectedItems
                            val (_, freed) = withContext(Dispatchers.IO) {
                                scanner.purgeSelected(itemsToPurge)
                            }
                            lastPurgedBytes = freed
                            showPurgeSuccess = true
                            refreshScan()
                        }
                    },
                    enabled = !isScanning && selectedItems.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Purge (${DesktopJunkItem.formatSize(selectedReclaimBytes)})",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Banner Stats
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = DarkSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total Game & Shader Bloat Found", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        DesktopJunkItem.formatSize(totalFoundBytes),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFA855F7)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${allItems.size} Junk Files", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SafeGreen.copy(alpha = 0.15f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Selected: ${DesktopJunkItem.formatSize(selectedReclaimBytes)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SafeGreen
                            )
                        }
                    }
                }
            }
        }

        if (showPurgeSuccess) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SafeGreen.copy(alpha = 0.15f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SafeGreen)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Purged ${DesktopJunkItem.formatSize(lastPurgedBytes)} of gamer & shader bloat safely!",
                            fontWeight = FontWeight.SemiBold,
                            color = SafeGreen
                        )
                    }
                    IconButton(onClick = { showPurgeSuccess = false }) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Category Breakdown Strip & View Controls
        val sortedCategories = remember(scanResults) {
            GamerCacheCategory.values().sortedByDescending { cat ->
                scanResults[cat]?.sumOf { it.sizeBytes } ?: 0L
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sortedCategories.forEach { cat ->
                    val items = scanResults[cat] ?: emptyList()
                    if (items.isNotEmpty()) {
                        val size = items.sumOf { it.sizeBytes }
                        val isExpanded = expandedCategories.contains(cat)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isExpanded) Color(0xFFA855F7).copy(alpha = 0.18f) else DarkSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isExpanded) Color(0xFFA855F7).copy(alpha = 0.6f) else DarkSurfaceVariant
                            ),
                            modifier = Modifier.clickable {
                                expandedCategories = if (isExpanded) {
                                    expandedCategories - cat
                                } else {
                                    expandedCategories + cat
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(cat.title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    DesktopJunkItem.formatSize(size),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFA855F7)
                                )
                            }
                        }
                    }
                }
            }

            if (allItems.isNotEmpty()) {
                val allExpanded = expandedCategories.size == sortedCategories.count { (scanResults[it]?.size ?: 0) > 0 }
                TextButton(
                    onClick = {
                        expandedCategories = if (allExpanded) {
                            emptySet()
                        } else {
                            sortedCategories.filter { (scanResults[it]?.size ?: 0) > 0 }.toSet()
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        if (allExpanded) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (allExpanded) "Collapse All" else "Expand All",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isScanning) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFFA855F7))
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Scanning Game Buffers, Replays & Shader Pipelines...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else if (allItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.DoneAll,
                        contentDescription = null,
                        tint = SafeGreen,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Clean & Peak Gaming Condition!", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "No leftover game replays, emulator sync dumps, or shader bloat found.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                sortedCategories.forEach { category ->
                    val categoryItems = scanResults[category] ?: emptyList()
                    if (categoryItems.isNotEmpty()) {
                        item(key = category.name) {
                            val isExpanded = expandedCategories.contains(category)
                            val categoryBytes = categoryItems.sumOf { it.sizeBytes }
                            val allSelected = categoryItems.all { it.isSelected }

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // Category Header Row
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                expandedCategories = if (isExpanded) {
                                                    expandedCategories - category
                                                } else {
                                                    expandedCategories + category
                                                }
                                            }
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Checkbox(
                                                checked = allSelected,
                                                onCheckedChange = { checkState ->
                                                    scanResults = scanResults.mapValues { (cat, list) ->
                                                        if (cat == category) list.map { it.copy(isSelected = checkState) } else list
                                                    }
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(category.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                Text(
                                                    "${categoryItems.size} items • ${category.description}",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                DesktopJunkItem.formatSize(categoryBytes),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = Color(0xFFA855F7)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Icon(
                                                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Item List
                                    AnimatedVisibility(visible = isExpanded) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            HorizontalDivider(color = DarkSurfaceVariant)
                                            categoryItems.forEach { item ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(DarkSurfaceVariant.copy(alpha = 0.5f))
                                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                        Checkbox(
                                                            checked = item.isSelected,
                                                            onCheckedChange = { checked ->
                                                                scanResults = scanResults.mapValues { (cat, list) ->
                                                                    if (cat == category) {
                                                                        list.map { if (it.id == item.id) it.copy(isSelected = checked) else it }
                                                                    } else list
                                                                }
                                                            }
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Icon(
                                                            Icons.AutoMirrored.Filled.InsertDriveFile,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Column {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Text(item.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                                                Spacer(modifier = Modifier.width(8.dp))
                                                                Surface(
                                                                    shape = RoundedCornerShape(4.dp),
                                                                    color = Color(0xFFA855F7).copy(alpha = 0.2f)
                                                                ) {
                                                                    Text(
                                                                        item.gameTitle,
                                                                        fontSize = 10.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = Color(0xFFA855F7),
                                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                    )
                                                                }
                                                            }
                                                            Text(
                                                                item.file.absolutePath,
                                                                fontSize = 11.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                                maxLines = 1
                                                            )
                                                        }
                                                    }

                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(item.formattedSize, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        IconButton(
                                                            onClick = {
                                                                try {
                                                                    Desktop.getDesktop().open(item.file.parentFile)
                                                                } catch (_: Exception) {}
                                                            },
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(Icons.Default.FolderOpen, contentDescription = "Open Folder", modifier = Modifier.size(16.dp))
                                                        }
                                                        IconButton(
                                                            onClick = {
                                                                coroutineScope.launch {
                                                                    withContext(Dispatchers.IO) { item.file.delete() }
                                                                    refreshScan()
                                                                }
                                                            },
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = DangerRed, modifier = Modifier.size(16.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
