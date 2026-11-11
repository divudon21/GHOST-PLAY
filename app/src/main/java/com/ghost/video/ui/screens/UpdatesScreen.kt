package com.ghost.video.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ghost.video.R
import com.ghost.video.data.UpdateOption
import com.ghost.video.data.UpdateRelease
import com.ghost.video.data.UpdatesState
import com.ghost.video.ui.components.ListSectionTitle
import com.ghost.video.ui.components.NextIcons
import com.ghost.video.ui.components.NextTopAppBar
import com.ghost.video.ui.components.PreferenceItem
import com.ghost.video.ui.components.PreferenceSwitch
import com.ghost.video.viewmodel.UpdatesViewModel

/**
 * Updates settings screen.
 *
 * Ported 1:1 from DownList (`lib/screens/settings/update_settings_screen.dart`):
 * one "Check for updates" action, the result card (error / up to date / update
 * available) and the four preferences (auto check, notifications, beta, pop-up)
 * with the same copy and enabled states.
 */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun UpdatesScreen(
    onBack: () -> Unit,
    viewModel: UpdatesViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    // DownList requests the notification permission when the toggle is enabled.
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { /* Preference is persisted either way, exactly like DownList. */ }

    fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun setOption(key: String, value: Boolean) {
        viewModel.setOption(key, value)
        if (key == UpdateOption.NOTIFICATIONS && value) requestNotificationPermissionIfNeeded()
    }

    Scaffold(
        topBar = {
            NextTopAppBar(
                title = stringResource(id = R.string.updates),
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
            ListSectionTitle(text = stringResource(id = R.string.updates_section_app_updates))
            Column(
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                PreferenceItem(
                    title = stringResource(id = R.string.check_for_updates),
                    description = if (state.checking) {
                        stringResource(id = R.string.checking)
                    } else {
                        stringResource(id = R.string.check_for_updates_description)
                    },
                    icon = NextIcons.Refresh,
                    enabled = true,
                    onClick = {
                        if (!state.checking && state.ready) viewModel.check()
                    },
                    isFirstItem = true,
                    isLastItem = true,
                    trailingContent = {
                        if (state.checking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                            )
                        }
                    },
                )
            }

            UpdateResult(state = state, onUpdate = viewModel::openRelease)

            ListSectionTitle(text = stringResource(id = R.string.updates_section_preferences))
            val preferencesEnabled = state.ready && !state.checking
            Column(
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                PreferenceSwitch(
                    title = stringResource(id = R.string.updates_auto_check),
                    description = stringResource(id = R.string.updates_auto_check_desc),
                    icon = NextIcons.Update,
                    enabled = preferencesEnabled,
                    isChecked = state.autoCheck,
                    onClick = { setOption(UpdateOption.AUTO_CHECK, !state.autoCheck) },
                    isFirstItem = true,
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.updates_notifications),
                    description = stringResource(id = R.string.updates_notifications_desc),
                    icon = NextIcons.Notifications,
                    enabled = preferencesEnabled,
                    isChecked = state.notifications,
                    onClick = { setOption(UpdateOption.NOTIFICATIONS, !state.notifications) },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.updates_include_beta),
                    description = stringResource(id = R.string.updates_include_beta_desc),
                    icon = NextIcons.Science,
                    enabled = preferencesEnabled,
                    isChecked = state.includeBeta,
                    onClick = { setOption(UpdateOption.INCLUDE_BETA, !state.includeBeta) },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.updates_popup),
                    description = stringResource(id = R.string.updates_popup_desc),
                    icon = NextIcons.OpenInNew,
                    enabled = preferencesEnabled,
                    isChecked = state.popup,
                    onClick = { setOption(UpdateOption.POPUP, !state.popup) },
                    isLastItem = true,
                )
            }

            Text(
                text = stringResource(id = R.string.updates_footer_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 14.dp, bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun UpdateResult(state: UpdatesState, onUpdate: (UpdateRelease) -> Unit) {
    val release = state.available
    val build = state.installed
    val error = state.error

    AnimatedVisibility(visible = error != null || (state.checked && release == null) || release != null) {
        Column {
            if (error != null) {
                UpdateMessage(
                    icon = NextIcons.Error,
                    title = stringResource(id = R.string.updates_error_title),
                    details = error,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            } else if (state.checked && release == null) {
                UpdateMessage(
                    icon = NextIcons.CheckCircle,
                    title = stringResource(id = R.string.updates_up_to_date),
                    details = build?.let {
                        "Installed: ${it.versionName} (build ${it.versionCode})"
                    } ?: "",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            } else if (release != null) {
                UpdateAvailableCard(
                    release = release,
                    compatible = build?.let { release.isCompatibleWith(it) } ?: true,
                    onUpdate = onUpdate,
                )
            }
        }
    }
}

@Composable
private fun UpdateMessage(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    details: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Icon(imageVector = icon, contentDescription = null)
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(text = title, fontWeight = FontWeight.Bold)
                if (details.isNotEmpty()) {
                    Text(text = details, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun UpdateAvailableCard(
    release: UpdateRelease,
    compatible: Boolean,
    onUpdate: (UpdateRelease) -> Unit,
) {
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = contentColor,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(id = R.string.updates_available),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(text = release.label, modifier = Modifier.padding(top = 4.dp))
            if (release.notes.isNotBlank()) {
                Text(
                    text = stringResource(id = R.string.updates_whats_new),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(
                    text = release.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.85f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (!compatible) {
                Text(
                    text = stringResource(id = R.string.updates_signing_key_warning),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            FilledTonalButton(
                onClick = { onUpdate(release) },
                modifier = Modifier
                    .padding(top = 12.dp)
                    .height(40.dp),
            ) {
                Icon(
                    imageVector = NextIcons.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(id = R.string.updates_update),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}
