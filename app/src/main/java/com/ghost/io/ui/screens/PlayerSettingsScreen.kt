package com.ghost.io.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ghost.io.data.OrientationPreference
import com.ghost.io.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val resumePlayback by viewModel.resumePlayback.collectAsState()
    val playbackSpeed by viewModel.defaultPlaybackSpeed.collectAsState()
    val autoplay by viewModel.autoplay.collectAsState()
    val pipMode by viewModel.pipMode.collectAsState()
    val backgroundPlay by viewModel.backgroundPlay.collectAsState()
    val rememberBrightness by viewModel.rememberBrightness.collectAsState()
    val rememberSelections by viewModel.rememberSelections.collectAsState()
    val orientation by viewModel.playerOrientation.collectAsState()

    var showOrientationDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Player") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Playback",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            item {
                ToggleCard(
                    title = "Resume",
                    subtitle = "Select whether to resume played videos from the point where you stopped",
                    icon = Icons.Default.RestartAlt,
                    checked = resumePlayback,
                    onCheckedChange = { viewModel.setResumePlayback(it) }
                )
            }

            item {
                SpeedCard(
                    title = "Default playback speed",
                    currentSpeed = playbackSpeed,
                    icon = Icons.Default.Speed,
                    onSpeedChange = { viewModel.setDefaultPlaybackSpeed(it) }
                )
            }

            item {
                ToggleCard(
                    title = "Autoplay",
                    subtitle = "Automatically play the next video in the current folder",
                    icon = Icons.Default.PlayArrow,
                    checked = autoplay,
                    onCheckedChange = { viewModel.setAutoplay(it) }
                )
            }

            item {
                ToggleCard(
                    title = "Picture in Picture Mode",
                    subtitle = "Automatically switch to miniature player when tapping Home Button",
                    icon = Icons.Default.PictureInPicture,
                    checked = pipMode,
                    onCheckedChange = { viewModel.setPipMode(it) }
                )
            }

            item {
                ToggleCard(
                    title = "Background play",
                    subtitle = "Play in background when tapped on home or screen is locked.",
                    icon = Icons.Default.Headphones,
                    checked = backgroundPlay,
                    onCheckedChange = { viewModel.setBackgroundPlay(it) }
                )
            }

            item {
                ToggleCard(
                    title = "Remember brightness level",
                    subtitle = "Remember brightness level between playback sessions",
                    icon = Icons.Default.BrightnessMedium,
                    checked = rememberBrightness,
                    onCheckedChange = { viewModel.setRememberBrightness(it) }
                )
            }

            item {
                ToggleCard(
                    title = "Remember selections",
                    subtitle = "Remember selections for each file like audio track, subtitle track, etc.",
                    icon = Icons.Default.Checklist,
                    checked = rememberSelections,
                    onCheckedChange = { viewModel.setRememberSelections(it) }
                )
            }

            item {
                ClickableCard(
                    title = "Player screen orientation",
                    subtitle = orientationLabel(orientation),
                    icon = Icons.Default.ScreenRotation,
                    onClick = { showOrientationDialog = true }
                )
            }
        }
    }

    if (showOrientationDialog) {
        OrientationDialog(
            currentOrientation = orientation,
            onOrientationSelected = {
                viewModel.setPlayerOrientation(it)
                showOrientationDialog = false
            },
            onDismiss = { showOrientationDialog = false }
        )
    }
}

@Composable
fun ToggleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
fun SpeedCard(
    title: String,
    currentSpeed: Float,
    icon: ImageVector,
    onSpeedChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "%.1f".format(currentSpeed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Slider(
                value = currentSpeed,
                onValueChange = onSpeedChange,
                valueRange = 0.25f..3.0f,
                steps = 10,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
            )
        }
    }
}

@Composable
fun ClickableCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun OrientationDialog(
    currentOrientation: OrientationPreference,
    onOrientationSelected: (OrientationPreference) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        OrientationPreference.AUTO to "Auto",
        OrientationPreference.LANDSCAPE to "Landscape",
        OrientationPreference.PORTRAIT to "Portrait",
        OrientationPreference.SENSOR_LANDSCAPE to "Sensor Landscape"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Player screen orientation") },
        text = {
            Column {
                options.forEach { (pref, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOrientationSelected(pref) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentOrientation == pref,
                            onClick = { onOrientationSelected(pref) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun orientationLabel(orientation: OrientationPreference): String = when (orientation) {
    OrientationPreference.AUTO -> "Video Orientation"
    OrientationPreference.LANDSCAPE -> "Landscape"
    OrientationPreference.PORTRAIT -> "Portrait"
    OrientationPreference.SENSOR_LANDSCAPE -> "Sensor Landscape"
}
