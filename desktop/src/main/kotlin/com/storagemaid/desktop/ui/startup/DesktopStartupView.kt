package com.storagemaid.desktop.ui.startup

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storagemaid.desktop.engine.WindowsStartupManager
import com.storagemaid.desktop.model.DesktopStartupItem
import com.storagemaid.desktop.model.StartupImpact
import com.storagemaid.desktop.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DesktopStartupView(manager: WindowsStartupManager) {
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var startupItems by remember { mutableStateOf<List<DesktopStartupItem>>(emptyList()) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        coroutineScope.launch {
            isLoading = true
            val items = withContext(Dispatchers.IO) { manager.listStartupItems() }
            startupItems = items
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    val enabledCount = startupItems.count { it.isEnabled }
    val highImpactCount = startupItems.count { it.isEnabled && it.impact == StartupImpact.HIGH }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.RocketLaunch,
                        contentDescription = null,
                        tint = SafeGreen,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Windows Startup Boot Manager",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Turn off unwanted background apps that launch on boot to accelerate Windows startup time.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = { refresh() },
                enabled = !isLoading,
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Refresh")
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Stat Overview Cards
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkSurface,
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Detected Apps", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${startupItems.size}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkSurface,
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Active at Boot", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "$enabledCount Enabled",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (enabledCount > 5) DangerRed else SafeGreen
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkSurface,
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("High Boot Impact", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "$highImpactCount Apps",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (highImpactCount > 0) WarningAmber else SafeGreen
                    )
                }
            }
        }

        statusMessage?.let { msg ->
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SafeGreen.copy(alpha = 0.15f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(msg, color = SafeGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = { statusMessage = null }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (startupItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No startup programs detected in current registry or startup folders.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(startupItems, key = { it.id }) { item ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = if (item.isEnabled) Icons.Default.PlayCircle else Icons.Default.PauseCircle,
                                    contentDescription = null,
                                    tint = if (item.isEnabled) SafeGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(item.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(8.dp))

                                        // Impact badge
                                        val impactColor: Color = when (item.impact) {
                                            StartupImpact.HIGH -> DangerRed
                                            StartupImpact.MEDIUM -> WarningAmber
                                            StartupImpact.LOW -> SafeGreen
                                            StartupImpact.UNKNOWN -> Color.Gray
                                        }
                                        val impactText: String = when (item.impact) {
                                            StartupImpact.HIGH -> "High Impact"
                                            StartupImpact.MEDIUM -> "Medium Impact"
                                            StartupImpact.LOW -> "Low Impact"
                                            StartupImpact.UNKNOWN -> "Normal"
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = impactColor.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                impactText,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = impactColor,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = DarkSurfaceVariant
                                        ) {
                                            Text(
                                                if (item.sourceLocation.contains("HKCU")) "User Registry" else "System",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        item.command,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        maxLines = 1
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (item.isEnabled) "Enabled" else "Disabled",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (item.isEnabled) SafeGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Switch(
                                    checked = item.isEnabled,
                                    onCheckedChange = {
                                        coroutineScope.launch {
                                            val ok = withContext(Dispatchers.IO) {
                                                manager.toggleStartupItem(item)
                                            }
                                            if (ok) {
                                                statusMessage = "Toggled ${item.name} ${if (item.isEnabled) "Disabled" else "Enabled"}"
                                                refresh()
                                            } else {
                                                statusMessage = "Administrator rights required to modify system-wide (HKLM) startup items."
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
