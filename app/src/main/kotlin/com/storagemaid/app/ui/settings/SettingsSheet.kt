package com.storagemaid.app.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storagemaid.app.core.model.LargeFileThreshold
import com.storagemaid.app.core.preferences.DuplicateKeepStrategy
import com.storagemaid.app.core.sound.RetroSoundManager
import com.storagemaid.app.ui.dashboard.DashboardViewModel
import com.storagemaid.app.ui.theme.DarkSurfaceVariant
import com.storagemaid.app.ui.theme.SafeGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    viewModel: DashboardViewModel,
    onDismiss: () -> Unit
) {
    val isSoundEnabled by viewModel.isSoundEnabled.collectAsState()
    val isSentinelEnabled by viewModel.isSentinelEnabled.collectAsState()
    val defaultThreshold by viewModel.defaultLargeFileThreshold.collectAsState()
    val duplicateStrategy by viewModel.duplicateStrategy.collectAsState()
    val whitelist by viewModel.whitelistPaths.collectAsState()

    var newCustomPath by remember { mutableStateOf("") }
    var showAddPathDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Settings & Whitelist",
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Storage Sentinel (Background Monitor)
                item {
                    SettingsCard(title = "Storage Sentinel (Background Monitor)") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Storage Full & Junk Alerts", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(
                                    "Periodic background checks every 24h that alert you when storage >= 85% or safe junk exceeds 500 MB.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isSentinelEnabled,
                                onCheckedChange = { viewModel.setSentinelEnabled(it) }
                            )
                        }
                    }
                }

                // Section 3: Scanning & Deduplication Rules
                item {
                    SettingsCard(title = "Cleaning & Scanning Rules") {
                        // Default Big File Threshold
                        Text("Default Big File Hunter Threshold", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            LargeFileThreshold.values().forEach { threshold ->
                                FilterChip(
                                    selected = defaultThreshold == threshold,
                                    onClick = { viewModel.setDefaultThresholdPreference(threshold) },
                                    label = { Text(threshold.label, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        // Duplicate Keep Strategy
                        Text("Duplicate Auto-Select Strategy", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        DuplicateKeepStrategy.values().forEach { strategy ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.setDuplicateStrategy(strategy) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = duplicateStrategy == strategy,
                                    onClick = { viewModel.setDuplicateStrategy(strategy) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(strategy.label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text(strategy.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                // Section 3: Protected Folders Whitelist
                item {
                    SettingsCard(title = "Protected Folders (Whitelist)") {
                        Text(
                            text = "Files inside these directories are strictly protected and never suggested for deletion or duplicate purging.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Current Whitelist items
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            whitelist.forEach { path ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DarkSurfaceVariant)
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = null,
                                            tint = SafeGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = path,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.removeWhitelistPath(path) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Remove",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick Add suggestions
                        Text("Quick Add Suggestions:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val suggestions = listOf("DCIM/Camera", "Documents", "KeepSafe", "Music", "Download/Work")
                            items(suggestions) { folder ->
                                val fullPath = "/$folder"
                                if (!whitelist.contains(fullPath)) {
                                    SuggestionChip(
                                        onClick = { viewModel.addWhitelistPath(fullPath) },
                                        label = { Text("+$folder", fontSize = 11.sp) }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Add Custom Path Button
                        OutlinedButton(
                            onClick = { showAddPathDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Custom Excluded Path", fontSize = 12.sp)
                        }
                    }
                }

                // Section 4: Support Open Source
                item {
                    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                    SettingsCard(title = "Support Open Source Development") {
                        Text(
                            text = "BytePurge is 100% free, offline, and open-source under GPLv3. If it helped reclaim your storage, consider sponsoring to keep it independent and ad-free!",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    try { uriHandler.openUri("https://github.com/sponsors/cosmomime") } catch (_: Exception) {}
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA4AAA)), // GitHub Sponsor Pink
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sponsor", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    try { uriHandler.openUri("https://ko-fi.com/cosmomime") } catch (_: Exception) {}
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Coffee, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ko-fi / Tip", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = {
                                try { uriHandler.openUri("https://github.com/cosmomime/bytepurge") } catch (_: Exception) {}
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("View Source Code (GPLv3 on GitHub)", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // Section 5: App Information
                item {
                    SettingsCard(title = "About BytePurge") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "BytePurge v1.2.0 (Build 3)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "GPLv3 Free Software • 100% Offline & Private",
                                    fontSize = 11.sp,
                                    color = SafeGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SafeGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    "GPL-3.0",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SafeGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Zero telemetry, zero trackers, and no background analytics.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showAddPathDialog) {
        AlertDialog(
            onDismissRequest = { showAddPathDialog = false },
            icon = { Icon(Icons.Default.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Add Protected Path") },
            text = {
                Column {
                    Text("Enter a folder name or subpath to exclude from all scans and deletions:", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newCustomPath,
                        onValueChange = { newCustomPath = it },
                        placeholder = { Text("e.g. /KeepSafe or /PrivateDocs") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCustomPath.isNotBlank()) {
                            viewModel.addWhitelistPath(newCustomPath.trim())
                            newCustomPath = ""
                            showAddPathDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPathDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}
