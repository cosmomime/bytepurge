package com.storagemaid.desktop.ui.sweep

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storagemaid.desktop.engine.WindowsJunkScanner
import com.storagemaid.desktop.model.DesktopJunkCategory
import com.storagemaid.desktop.model.DesktopJunkItem
import com.storagemaid.desktop.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DesktopSweepView(scanner: WindowsJunkScanner) {
    val coroutineScope = rememberCoroutineScope()
    var junkMap by remember { mutableStateOf<Map<DesktopJunkCategory, List<DesktopJunkItem>>>(emptyMap()) }
    var expandedCategories by remember { mutableStateOf<Set<DesktopJunkCategory>>(emptySet()) }
    var isScanning by remember { mutableStateOf(false) }
    var isCleaning by remember { mutableStateOf(false) }
    var lastCleanedSummary by remember { mutableStateOf<String?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    val totalSelectedItems = junkMap.values.flatten().filter { it.isSelected }
    val totalReclaimableBytes = totalSelectedItems.sumOf { it.sizeBytes }

    fun runScan() {
        if (isScanning || isCleaning) return
        isScanning = true
        lastCleanedSummary = null
        coroutineScope.launch {
            val res = withContext(Dispatchers.IO) { scanner.scanWindowsJunk() }
            junkMap = res
            isScanning = false
        }
    }

    fun executeClean() {
        if (totalSelectedItems.isEmpty() || isCleaning) return
        isCleaning = true
        coroutineScope.launch {
            val (count, freed) = withContext(Dispatchers.IO) {
                scanner.cleanSelectedItems(totalSelectedItems)
            }
            lastCleanedSummary = "Reclaimed ${DesktopJunkItem.formatSize(freed)} across $count files!"
            val res = withContext(Dispatchers.IO) { scanner.scanWindowsJunk() }
            junkMap = res
            isCleaning = false
        }
    }

    LaunchedEffect(Unit) {
        runScan()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text("Windows Byte Sweep", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(
                    "Deep clean %TEMP%, Windows Update downloads, browser caches, and minidumps",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { runScan() },
                    enabled = !isScanning && !isCleaning,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Re-Scan")
                }

                Button(
                    onClick = { showConfirmDialog = true },
                    enabled = totalSelectedItems.isNotEmpty() && !isScanning && !isCleaning,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isCleaning) "Cleaning..." else "Clean ${DesktopJunkItem.formatSize(totalReclaimableBytes)}"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Status Banner
        if (lastCleanedSummary != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SafeGreen.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SafeGreen)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(lastCleanedSummary!!, fontWeight = FontWeight.Medium, color = SafeGreen)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (isScanning) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Scanning system drives, caches, and temp directories...", fontSize = 13.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Categories List with expand/collapse & individual checkboxes
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(DesktopJunkCategory.values()) { category ->
                val items = junkMap[category].orEmpty()
                val categorySize = items.filter { it.isSelected }.sumOf { it.sizeBytes }
                val allSelected = items.isNotEmpty() && items.all { it.isSelected }
                val isExpanded = expandedCategories.contains(category)

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Checkbox(
                                    checked = allSelected,
                                    onCheckedChange = { checked ->
                                        val updated = items.map { it.copy(isSelected = checked) }
                                        junkMap = junkMap.toMutableMap().apply { put(category, updated) }
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(category.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                    Text(category.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        DesktopJunkItem.formatSize(categorySize),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (categorySize > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text("${items.count { it.isSelected }}/${items.size} files", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                IconButton(
                                    onClick = {
                                        expandedCategories = if (isExpanded) {
                                            expandedCategories - category
                                        } else {
                                            expandedCategories + category
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = if (isExpanded) "Collapse" else "Expand"
                                    )
                                }
                            }
                        }

                        // Expanded Individual Files List
                        AnimatedVisibility(visible = isExpanded && items.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                            ) {
                                HorizontalDivider(color = DarkSurfaceVariant)
                                Spacer(modifier = Modifier.height(8.dp))

                                items.take(30).forEach { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable {
                                                val updated = items.map {
                                                    if (it.id == item.id) it.copy(isSelected = !it.isSelected) else it
                                                }
                                                junkMap = junkMap.toMutableMap().apply { put(category, updated) }
                                            }
                                            .padding(vertical = 4.dp, horizontal = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Checkbox(
                                                checked = item.isSelected,
                                                onCheckedChange = { checked ->
                                                    val updated = items.map {
                                                        if (it.id == item.id) it.copy(isSelected = checked) else it
                                                    }
                                                    junkMap = junkMap.toMutableMap().apply { put(category, updated) }
                                                },
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(item.name, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, color = TextPrimary)
                                                Text(
                                                    item.file.parent ?: "",
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                        Text(
                                            item.formattedSize,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (items.size > 30) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "... and ${items.size - 30} more items",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            icon = { Icon(Icons.Default.CleaningServices, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Confirm Cleanup") },
            text = {
                Text(
                    "You are about to delete ${totalSelectedItems.size} temporary files and reclaim ${DesktopJunkItem.formatSize(totalReclaimableBytes)}. Proceed?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        executeClean()
                    }
                ) {
                    Text("Clean Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
