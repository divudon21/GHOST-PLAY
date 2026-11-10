package com.ghost.video.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ghost.video.R
import com.ghost.video.ui.components.ClickablePreferenceItem
import com.ghost.video.ui.components.ListSectionTitle
import com.ghost.video.ui.components.NextIcons
import com.ghost.video.ui.components.NextTopAppBar
import com.ghost.video.viewmodel.SettingsViewModel
import java.io.File

/**
 * General preferences.
 *
 * Ported from NextPlayer (GPL-3.0):
 * https://github.com/anilbeesetti/nextplayer/blob/main/feature/settings/src/main/java/dev/anilbeesetti/nextplayer/settings/screens/general/GeneralPreferencesScreen.kt
 */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun GeneralSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    var showDeleteCacheDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            NextTopAppBar(
                title = stringResource(id = R.string.general_name),
                navigationIcon = {
                    FilledTonalIconButton(onClick = onBack) {
                        Icon(
                            imageVector = NextIcons.ArrowBack,
                            contentDescription = stringResource(id = R.string.navigate_up),
                        )
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(state = rememberScrollState())
                .padding(paddingValues = innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            ListSectionTitle(text = stringResource(id = R.string.user_data))
            Column(
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                ClickablePreferenceItem(
                    title = stringResource(id = R.string.delete_thumbnail_cache),
                    description = stringResource(id = R.string.delete_thumbnail_cache_description),
                    icon = NextIcons.DeleteSweep,
                    onClick = { showDeleteCacheDialog = true },
                    isFirstItem = true,
                )
                ClickablePreferenceItem(
                    title = stringResource(id = R.string.reset_settings),
                    description = stringResource(id = R.string.reset_settings_description),
                    icon = NextIcons.Replay,
                    onClick = { showResetDialog = true },
                    isLastItem = true,
                )
            }
        }
    }

    if (showDeleteCacheDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteCacheDialog = false },
            title = { Text(text = stringResource(id = R.string.delete_thumbnail_cache)) },
            text = { Text(text = stringResource(id = R.string.delete_thumbnail_cache_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteThumbnailCache(context)
                        showDeleteCacheDialog = false
                        Toast.makeText(context, "Thumbnail cache cleared", Toast.LENGTH_SHORT).show()
                    },
                ) { Text(text = stringResource(id = R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteCacheDialog = false }) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(text = stringResource(id = R.string.reset_settings)) },
            text = { Text(text = stringResource(id = R.string.reset_settings_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetAllSettings()
                        showResetDialog = false
                        Toast.makeText(context, "Settings reset to default", Toast.LENGTH_SHORT).show()
                    },
                ) { Text(text = stringResource(id = R.string.reset)) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
        )
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
