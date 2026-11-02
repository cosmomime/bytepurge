package com.storagemaid.app.ui.apps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storagemaid.app.core.manager.AppManager
import com.storagemaid.app.core.model.AppFilter
import com.storagemaid.app.core.model.AppInfoItem
import com.storagemaid.app.core.model.AppManagerSummary
import com.storagemaid.app.core.model.AppSortOrder
import com.storagemaid.app.core.shizuku.ShizukuManager
import com.storagemaid.app.core.shizuku.ShizukuStatus
import com.storagemaid.app.core.sound.RetroSoundManager
import com.storagemaid.app.ui.dashboard.DashboardViewModel
import com.storagemaid.app.ui.theme.DarkSurfaceVariant
import com.storagemaid.app.ui.theme.SafeGreen
import com.storagemaid.app.ui.theme.WarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppManagerScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appManager = remember { AppManager(context) }

    val apps by viewModel.filteredApps.collectAsState()
    val summary by viewModel.appSummary.collectAsState()
    val isLoading by viewModel.isLoadingApps.collectAsState()
    val loadStatus by viewModel.appLoadStatus.collectAsState()
    val selectedFilter by viewModel.selectedAppFilter.collectAsState()
    val selectedSort by viewModel.selectedAppSort.collectAsState()
    val searchQuery by viewModel.appSearchQuery.collectAsState()

    var showSortMenu by remember { mutableStateOf(false) }
    val shizukuManager = remember { ShizukuManager() }
    var shizukuStatus by remember { mutableStateOf(shizukuManager.checkStatus()) }
    var isTrimmingShizuku by remember { mutableStateOf(false) }
    var shizukuMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val hasUsagePermission = remember { viewModel.hasUsageStatsPermission() }

    // Uninstaller launcher to refresh immediately on return
    val uninstallLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.pruneUninstalledApps(context.packageManager)
    }

    // Real-time receiver: catches package uninstalls instantly
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val action = intent?.action
                if (action == Intent.ACTION_PACKAGE_REMOVED || action == Intent.ACTION_PACKAGE_FULLY_REMOVED) {
                    val pkgName = intent.data?.schemeSpecificPart
                    if (!pkgName.isNullOrEmpty()) {
                        viewModel.onPackageRemoved(pkgName)
                    } else {
                        viewModel.pruneUninstalledApps(context.packageManager)
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED)
            addDataScheme("package")
        }
        context.registerReceiver(receiver, filter)
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(6.dp))

        // Compact Sticky Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setAppSearchQuery(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search installed applications…", fontSize = 13.sp) },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setAppSearchQuery("") }) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear search",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Sticky Filter & Sort Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AppFilter.values().forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { viewModel.setAppFilter(filter) },
                        label = {
                            Text(
                                text = filter.label,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.height(30.dp)
                    )
                }
            }

            Box {
                IconButton(
                    onClick = { showSortMenu = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = "Sort apps",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    AppSortOrder.values().forEach { sort ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = sort.label,
                                    fontWeight = if (selectedSort == sort) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedSort == sort) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                viewModel.setAppSort(sort)
                                showSortMenu = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Full Screen LazyColumn: Headers scroll away naturally!
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Summary and banner cards scroll with content, disappearing as user browses apps
            if (searchQuery.isEmpty()) {
                item(key = "header_summary") {
                    AppSummaryCard(
                        summary = summary,
                        isLoading = isLoading,
                        onRefresh = { viewModel.loadInstalledApps() }
                    )
                }

                if (!hasUsagePermission) {
                    item(key = "header_usage_banner") {
                        UsageStatsBanner(onGrant = { viewModel.openUsageAccessSettings() })
                    }
                }

                item(key = "header_shizuku_banner") {
                    ShizukuBanner(
                        shizukuStatus = shizukuStatus,
                        isTrimming = isTrimmingShizuku,
                        shizukuMessage = shizukuMessage,
                        onTrimAll = {
                            isTrimmingShizuku = true
                            coroutineScope.launch {
                                val (ok, msg) = shizukuManager.trimAllSystemCaches()
                                isTrimmingShizuku = false
                                shizukuMessage = msg
                                viewModel.loadInstalledApps()
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        },
                        onAuthorize = {
                            shizukuManager.requestPermission()
                            shizukuStatus = shizukuManager.checkStatus()
                        },
                        onCheck = {
                            shizukuStatus = shizukuManager.checkStatus()
                            if (shizukuStatus == ShizukuStatus.NOT_INSTALLED_OR_RUNNING) {
                                Toast.makeText(context, "Shizuku is not running. Launch Shizuku app via Wireless ADB first.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            // App list loading / empty / populated state
            if (isLoading) {
                item(key = "loading_state") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (loadStatus.isNotEmpty()) loadStatus else "Inspecting installed applications…",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            } else if (apps.isEmpty()) {
                item(key = "empty_state") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.Apps,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "No applications match \"$searchQuery\"" else "No applications found",
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(apps, key = { it.packageName }) { appItem ->
                    AppItemCard(
                        app = appItem,
                        appManager = appManager,
                        onOpen = {
                            val opened = appManager.launchApp(appItem.packageName)
                            if (!opened) {
                                appManager.openAppDetails(appItem.packageName)
                            }
                        },
                        onDetails = {
                            appManager.openAppDetails(appItem.packageName)
                        },
                        onExtractApk = {
                            viewModel.extractApk(appItem) { success, msg ->
                                val text = if (success) "Exported APK to: $msg" else "Export failed: $msg"
                                Toast.makeText(context, text, Toast.LENGTH_LONG).show()
                            }
                        },
                        onShareApk = {
                            viewModel.shareApk(appItem)
                        },
                        onUninstall = {
                            RetroSoundManager.playDeleteTrash()
                            try {
                                val intent = Intent(Intent.ACTION_DELETE).apply {
                                    data = Uri.parse("package:${appItem.packageName}")
                                }
                                uninstallLauncher.launch(intent)
                            } catch (_: Exception) {
                                appManager.requestUninstall(appItem.packageName)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun UsageStatsBanner(onGrant: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Unlock Cache & Data Breakdown",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "Grant Usage Access to see exactly how much cache TikTok, Chrome, etc. consume.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onGrant,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(30.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Grant", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ShizukuBanner(
    shizukuStatus: ShizukuStatus,
    isTrimming: Boolean,
    shizukuMessage: String?,
    onTrimAll: () -> Unit,
    onAuthorize: () -> Unit,
    onCheck: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (shizukuStatus) {
                ShizukuStatus.READY -> SafeGreen.copy(alpha = 0.15f)
                ShizukuStatus.PERMISSION_NEEDED -> WarningAmber.copy(alpha = 0.15f)
                ShizukuStatus.NOT_INSTALLED_OR_RUNNING -> DarkSurfaceVariant
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = when (shizukuStatus) {
                            ShizukuStatus.READY -> SafeGreen
                            ShizukuStatus.PERMISSION_NEEDED -> WarningAmber
                            ShizukuStatus.NOT_INSTALLED_OR_RUNNING -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = when (shizukuStatus) {
                                ShizukuStatus.READY -> "Rootless Turbo Cache Trimmer (Active)"
                                ShizukuStatus.PERMISSION_NEEDED -> "Authorize Shizuku Wireless ADB"
                                ShizukuStatus.NOT_INSTALLED_OR_RUNNING -> "Shizuku Rootless Trimmer Available"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = when (shizukuStatus) {
                                ShizukuStatus.READY -> "1-Tap 'pm trim-caches' to safely purge all system app caches."
                                ShizukuStatus.PERMISSION_NEEDED -> "Permission needed to trim caches across all apps."
                                ShizukuStatus.NOT_INSTALLED_OR_RUNNING -> "Pair once with Wireless ADB to trim app caches without root."
                            },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                when (shizukuStatus) {
                    ShizukuStatus.READY -> {
                        Button(
                            onClick = onTrimAll,
                            enabled = !isTrimming,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SafeGreen)
                        ) {
                            Text(if (isTrimming) "Trimming..." else "Trim All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    ShizukuStatus.PERMISSION_NEEDED -> {
                        Button(
                            onClick = onAuthorize,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WarningAmber)
                        ) {
                            Text("Authorize", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    ShizukuStatus.NOT_INSTALLED_OR_RUNNING -> {
                        OutlinedButton(
                            onClick = onCheck,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Check", fontSize = 11.sp)
                        }
                    }
                }
            }

            shizukuMessage?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(it, fontSize = 11.sp, color = SafeGreen, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun AppSummaryCard(
    summary: AppManagerSummary,
    isLoading: Boolean,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Installed Apps Overview",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "${summary.totalApps} packages installed",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    enabled = !isLoading,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh apps",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryStatBox(
                    title = "User Apps",
                    value = "${summary.userApps}",
                    subtitle = summary.formattedUserSize,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                SummaryStatBox(
                    title = "System Apps",
                    value = "${summary.systemApps}",
                    subtitle = "Protected",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SummaryStatBox(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceVariant)
            .padding(10.dp)
    ) {
        Column {
            Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AppItemCard(
    app: AppInfoItem,
    appManager: AppManager,
    onOpen: () -> Unit,
    onDetails: () -> Unit,
    onExtractApk: () -> Unit,
    onShareApk: () -> Unit,
    onUninstall: () -> Unit
) {
    val iconBitmap = remember(app.packageName) {
        val drawable = appManager.getAppIcon(app.packageName)
        drawableToBitmap(drawable)
    }
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon (42dp)
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap.asImageBitmap(),
                    contentDescription = app.appName,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Android,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // App Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.appName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (app.isSystemApp) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "SYSTEM",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = app.packageName,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (app.hasUsageStats) app.formattedTotalStorage else app.formattedApkSize,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (app.hasUsageStats && app.formattedCacheSize != "0 B") {
                        Text(
                            text = "• Cache: ${app.formattedCacheSize}",
                            fontSize = 11.sp,
                            color = if (app.isLargeCache) WarningAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (app.isLargeCache) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Quick Actions: Direct Uninstall button & 3-dots Menu
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!app.isSystemApp) {
                    Button(
                        onClick = onUninstall,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Uninstall", modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Uninstall", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedButton(
                        onClick = onDetails,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Info", fontSize = 11.sp)
                    }
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More actions",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Open App") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Launch, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            onClick = {
                                showMenu = false
                                onOpen()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("App Details (Settings)") },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            onClick = {
                                showMenu = false
                                onDetails()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Extract APK to Storage") },
                            leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            onClick = {
                                showMenu = false
                                onExtractApk()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Share APK") },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            onClick = {
                                showMenu = false
                                onShareApk()
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun drawableToBitmap(drawable: Drawable?): Bitmap? {
    if (drawable == null) return null
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
        return drawable.bitmap
    }
    return try {
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
        val bitmap = Bitmap.createBitmap(
            width.coerceIn(48, 144),
            height.coerceIn(48, 144),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bitmap
    } catch (e: Exception) {
        null
    }
}
