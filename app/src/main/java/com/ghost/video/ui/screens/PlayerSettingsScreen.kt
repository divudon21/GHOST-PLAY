package com.ghost.video.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlaylistAddCheck
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.StayCurrentLandscape
import androidx.compose.material.icons.rounded.StayCurrentPortrait
import androidx.compose.material.icons.rounded.ScreenLockRotation
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ghost.video.data.OrientationPreference
import com.ghost.video.viewmodel.SettingsViewModel

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
    val autoPipMode by viewModel.autoPipMode.collectAsState()
    val backgroundPlay by viewModel.backgroundPlay.collectAsState()
    val rememberBrightness by viewModel.rememberBrightness.collectAsState()
    val rememberSelections by viewModel.rememberSelections.collectAsState()
    val orientation by viewModel.playerOrientation.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Player") },
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
                ToggleCard(
                    title = "Resume",
                    subtitle = "Resume videos from where you stopped",
                    icon = Icons.Rounded.Replay,
                    checked = resumePlayback,
                    onCheckedChange = { viewModel.setResumePlayback(it) },
                    showTopDivider = true
                )
            }

            item {
                SpeedCard(
                    title = "Default playback speed",
                    currentSpeed = playbackSpeed,
                    icon = Icons.Rounded.Speed,
                    onSpeedChange = { viewModel.setDefaultPlaybackSpeed(it) }
                )
            }

            item {
                ToggleCard(
                    title = "Autoplay",
                    subtitle = "Play the next video automatically",
                    icon = Icons.Rounded.PlayCircleOutline,
                    checked = autoplay,
                    onCheckedChange = { viewModel.setAutoplay(it) }
                )
            }

            item {
                ToggleCard(
                    title = "Picture in Picture Mode",
                    subtitle = "Show the Picture-in-Picture button in the player",
                    icon = Icons.Rounded.PictureInPictureAlt,
                    checked = pipMode,
                    onCheckedChange = { viewModel.setPipMode(it) }
                )

                ToggleCard(
                    title = "Auto Picture in Picture Mode",
                    subtitle = "Automatically enter Picture-in-Picture when you leave the app",
                    icon = Icons.Rounded.PictureInPictureAlt,
                    checked = autoPipMode,
                    onCheckedChange = { viewModel.setAutoPipMode(it) }
                )
            }

            item {
                ToggleCard(
                    title = "Background play",
                    subtitle = "Keep playing when screen is off",
                    icon = Icons.Rounded.Headphones,
                    checked = backgroundPlay,
                    onCheckedChange = { viewModel.setBackgroundPlay(it) }
                )
            }

            item {
                ToggleCard(
                    title = "Remember brightness level",
                    subtitle = "Keep brightness between sessions",
                    icon = Icons.Rounded.Brightness6,
                    checked = rememberBrightness,
                    onCheckedChange = { viewModel.setRememberBrightness(it) }
                )
            }

            item {
                ToggleCard(
                    title = "Remember selections",
                    subtitle = "Keep audio & subtitle track choices",
                    icon = Icons.Rounded.PlaylistAddCheck,
                    checked = rememberSelections,
                    onCheckedChange = { viewModel.setRememberSelections(it) }
                )
            }

            item {
                CapsuleSwitcherCard(
                    title = "Screen Orientation",
                    titleIcon = Icons.Rounded.ScreenRotation,
                    options = listOf(
                        CapsuleSwitcherOption("Auto", Icons.Rounded.ScreenRotation),
                        CapsuleSwitcherOption("Land", Icons.Rounded.StayCurrentLandscape),
                        CapsuleSwitcherOption("Port", Icons.Rounded.StayCurrentPortrait),
                        CapsuleSwitcherOption("Sensor", Icons.Rounded.ScreenLockRotation)
                    ),
                    selectedIndex = orientation.ordinal,
                    onSelected = { viewModel.setPlayerOrientation(OrientationPreference.entries[it]) }
                )
            }
        }
    }

    }

/**
 * Shared minimal player-setting row — plain leading icon (no box), title, subtitle
 * and trailing content. A thin line below each row (and above the first) keeps
 * options cleanly separated, matching the Appearance settings style.
 */
@Composable
fun MinimalPlayerRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: (() -> Unit)? = null,
    showTopDivider: Boolean = false,
    trailing: @Composable (() -> Unit)? = null
) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    Column(modifier = Modifier.fillMaxWidth()) {
        if (showTopDivider) {
            HorizontalDivider(thickness = 0.6.dp, color = lineColor)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
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
            trailing?.invoke()
        }
        HorizontalDivider(thickness = 0.6.dp, color = lineColor)
    }
}

@Composable
fun ToggleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showTopDivider: Boolean = false
) {
    MinimalPlayerRow(
        title = title,
        subtitle = subtitle,
        icon = icon,
        showTopDivider = showTopDivider,
        trailing = {
            com.ghost.video.ui.components.SmoothSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    )
}

@Composable
fun SpeedCard(
    title: String,
    currentSpeed: Float,
    icon: ImageVector,
    onSpeedChange: (Float) -> Unit
) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    // Only persist when the user releases the slider, so a casual drag can't
    // accidentally leave playback at an unwanted speed (e.g. 0.5x).
    var isDragging by remember { mutableStateOf(false) }
    var pendingSpeed by remember { mutableFloatStateOf(currentSpeed) }
    val displayedSpeed = if (isDragging) pendingSpeed else currentSpeed

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 4.dp),
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
                    text = "%.1f×".format(displayedSpeed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                )
            }
            // One-tap reset back to normal 1x speed.
            IconButton(onClick = { onSpeedChange(1.0f) }) {
                Icon(
                    imageVector = Icons.Rounded.RestartAlt,
                    contentDescription = "Reset to 1x speed",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Slider(
            value = displayedSpeed,
            onValueChange = {
                isDragging = true
                pendingSpeed = it
            },
            onValueChangeFinished = {
                onSpeedChange(pendingSpeed)
                isDragging = false
            },
            valueRange = 0.25f..3.0f,
            steps = 10,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant,
                activeTickColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.65f),
                inactiveTickColor = MaterialTheme.colorScheme.outline
            )
        )
        HorizontalDivider(thickness = 0.6.dp, color = lineColor)
    }
}

@Composable
fun ClickableCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    MinimalPlayerRow(
        title = title,
        subtitle = subtitle,
        icon = icon,
        onClick = onClick,
        trailing = {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

@Composable
fun OrientationOptionRow(
    label: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        animationSpec = tween(180),
        label = "orientationContainer"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.ScreenRotation,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                )
            }
            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn(animationSpec = tween(160)),
                exit = fadeOut(animationSpec = tween(120))
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

fun orientationDescription(orientation: OrientationPreference): String = when (orientation) {
    OrientationPreference.AUTO -> "Follow the video's natural orientation"
    OrientationPreference.LANDSCAPE -> "Always open player in landscape"
    OrientationPreference.PORTRAIT -> "Always open player in portrait"
    OrientationPreference.SENSOR_LANDSCAPE -> "Landscape with sensor-based rotation"
}

fun orientationLabel(orientation: OrientationPreference): String = when (orientation) {
    OrientationPreference.AUTO -> "Video Orientation"
    OrientationPreference.LANDSCAPE -> "Landscape"
    OrientationPreference.PORTRAIT -> "Portrait"
    OrientationPreference.SENSOR_LANDSCAPE -> "Sensor Landscape"
}
