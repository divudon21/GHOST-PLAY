package com.ghost.video.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ghost.video.R
import com.ghost.video.data.ReleaseInfo
import com.ghost.video.data.UpdateChecker
import com.ghost.video.data.isNewer
import com.ghost.video.ui.components.AppLoadingIndicator
import com.ghost.video.viewmodel.SettingsViewModel

private const val CURRENT_VERSION = "1.0"
private const val VERSION_CODE = 1
private const val REPO_AUTHOR = "divudon21"
private const val REPO_URL = "https://github.com/divudon21/GHOST-PLAY"

private enum class CheckState { CHECKING, UPDATE, LATEST, ERROR }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUpdateScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val updateNotifications by viewModel.updateNotifications.collectAsState()
    val loadingIndicatorStyle by viewModel.loadingIndicatorStyle.collectAsState()

    var state by remember { mutableStateOf(CheckState.CHECKING) }
    var release by remember { mutableStateOf<ReleaseInfo?>(null) }
    var reloadKey by remember { mutableStateOf(0) }

    // Ask for notification permission (Android 13+) when the user turns updates on.
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Persist the toggle only if permission is available (or not required).
        viewModel.setUpdateNotifications(granted)
        if (granted) com.ghost.video.data.UpdateWorker.schedule(context)
    }

    fun enableUpdateNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val alreadyGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (alreadyGranted) {
                viewModel.setUpdateNotifications(true)
                com.ghost.video.data.UpdateWorker.schedule(context)
            } else {
                // Permission missing → request it. If already granted we skip asking.
                notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            // Below Android 13 no runtime permission is needed.
            viewModel.setUpdateNotifications(true)
            com.ghost.video.data.UpdateWorker.schedule(context)
        }
    }

    LaunchedEffect(reloadKey) {
        state = CheckState.CHECKING
        val info = UpdateChecker.fetchLatestRelease()
        release = info
        state = when {
            info == null -> CheckState.ERROR
            isNewer(info.tag, CURRENT_VERSION) -> CheckState.UPDATE
            else -> CheckState.LATEST
        }
        // Remember the newest release we've shown so the background notification
        // won't nag the user about a release they already saw here.
        if (info != null) viewModel.setLastSeenRelease(info.tag)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // ── Beautiful app header card (icon, name, version, GitHub) ──
            item {
                AppHeaderCard(
                    onOpenGithub = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(REPO_URL)))
                        }
                    }
                )
            }

            // ── Status card ──
            item {
                StatusCard(
                    state = state,
                    release = release,
                    onCheckAgain = { reloadKey++ },
                    onDownload = {
                        release?.htmlUrl?.let { url ->
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                        }
                    },
                    loadingIndicatorStyle = loadingIndicatorStyle
                )
            }

            // ── Update notifications toggle ──
            item {
                UpdateNotificationRow(
                    checked = updateNotifications,
                    onCheckedChange = { want ->
                        if (want) {
                            enableUpdateNotifications()
                        } else {
                            viewModel.setUpdateNotifications(false)
                            com.ghost.video.data.UpdateWorker.cancel(context)
                        }
                    }
                )
            }

            // ── Installed version ──
            item {
                Column {
                    HorizontalDivider(
                        thickness = 0.6.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SystemUpdate,
                            contentDescription = null,
                            modifier = Modifier.size(26.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Installed version",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Ghost Play v$CURRENT_VERSION",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                            )
                        }
                    }
                    HorizontalDivider(
                        thickness = 0.6.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

/**
 * Premium gradient header card: circular app icon, app name, version + author,
 * and a GitHub button. Matches a clean "About" layout.
 */
@Composable
private fun AppHeaderCard(onOpenGithub: () -> Unit) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.75f)
        )
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(20.dp)
        ) {
            // ── Top: icon + name/version ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.28f)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_fireplay),
                        contentDescription = "Ghost Play",
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "GHOST PLAY",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$CURRENT_VERSION ($VERSION_CODE) by $REPO_AUTHOR",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── GitHub button ──
            Surface(
                onClick = onOpenGithub,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.92f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_github),
                        contentDescription = null,
                        tint = Color(0xFF1B1B1F),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "GitHub",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B1B1F)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusCard(
    state: CheckState,
    release: ReleaseInfo?,
    onCheckAgain: () -> Unit,
    onDownload: () -> Unit,
    loadingIndicatorStyle: com.ghost.video.data.LoadingIndicatorStyle
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (state) {
                CheckState.CHECKING -> {
                    AppLoadingIndicator(style = loadingIndicatorStyle, size = 56.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Checking for updates…",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                CheckState.LATEST -> {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "You're up to date",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Ghost Play v$CURRENT_VERSION is the latest version.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = onCheckAgain) { Text("Check again") }
                }

                CheckState.UPDATE -> {
                    Icon(
                        imageVector = Icons.Rounded.SystemUpdate,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Update available",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${release?.name ?: release?.tag}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    val notes = release?.notes?.trim().orEmpty()
                    if (notes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = notes.take(600),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp)
                                .verticalScroll(rememberScrollState())
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = onDownload,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download update")
                    }
                }

                CheckState.ERROR -> {
                    Text(
                        "Couldn't check for updates",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Check your internet connection and try again.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = onCheckAgain) { Text("Retry") }
                }
            }
        }
    }
}

@Composable
private fun UpdateNotificationRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(thickness = 0.6.dp, color = lineColor)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.NotificationsActive,
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Update notifications",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Get notified when a new version is released",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                )
            }
            com.ghost.video.ui.components.SmoothSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}
