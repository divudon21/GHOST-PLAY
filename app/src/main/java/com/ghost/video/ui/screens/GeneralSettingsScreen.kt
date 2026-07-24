package com.ghost.video.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ghost.video.viewmodel.SettingsViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    var showResetDialog by remember { mutableStateOf(false) }
    var showDeleteCacheDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("General") },
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
                .padding(horizontal = 16.dp)
        ) {
            item {
                GeneralActionCard(
                    title = "Delete thumbnail cache",
                    subtitle = "Clear all cached thumbnails",
                    icon = Icons.Rounded.DeleteOutline,
                    onClick = { showDeleteCacheDialog = true },
                    showTopDivider = true
                )
            }

            item {
                GeneralActionCard(
                    title = "Reset settings",
                    subtitle = "Reset all settings to default",
                    icon = Icons.Rounded.RestartAlt,
                    onClick = { showResetDialog = true }
                )
            }
        }
    }

    if (showDeleteCacheDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteCacheDialog = false },
            title = { Text("Delete thumbnail cache") },
            text = { Text("This will delete all cached thumbnails. They will be regenerated when you open the media list.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteThumbnailCache(context)
                        Toast.makeText(context, "Thumbnail cache deleted", Toast.LENGTH_SHORT).show()
                        showDeleteCacheDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteCacheDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset settings") },
            text = { Text("Are you sure you want to reset all settings to their default values?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetAllSettings()
                        Toast.makeText(context, "Settings reset to default", Toast.LENGTH_SHORT).show()
                        showResetDialog = false
                    }
                ) {
                    Text("Reset", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun GeneralActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    showTopDivider: Boolean = false
) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    Column(modifier = Modifier.fillMaxWidth()) {
        if (showTopDivider) {
            HorizontalDivider(thickness = 0.6.dp, color = lineColor)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                )
            }
        }
        HorizontalDivider(thickness = 0.6.dp, color = lineColor)
    }
}

private fun deleteThumbnailCache(context: Context) {
    val cacheDir = context.cacheDir
    val thumbnailDir = File(cacheDir, "thumbnails")
    if (thumbnailDir.exists()) {
        thumbnailDir.listFiles()?.forEach { it.delete() }
    }
    // Also clear the in-memory thumbnail/album-art cache.
    com.ghost.video.data.ThumbnailCache.clear()
    try {
        coil3.SingletonImageLoader.get(context).diskCache?.clear()
    } catch (_: Exception) {}
}
