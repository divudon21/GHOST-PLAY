package com.ghost.video.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.SwipeRight
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.ZoomOutMap
import androidx.compose.material.icons.rounded.PanToolAlt
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ghost.video.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GesturesSettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val seekEnabled by viewModel.gestureSeekEnabled.collectAsState()
    val seekSensitivity by viewModel.gestureSeekSensitivity.collectAsState()
    val brightnessEnabled by viewModel.gestureBrightnessEnabled.collectAsState()
    val brightnessSensitivity by viewModel.gestureBrightnessSensitivity.collectAsState()
    val volumeEnabled by viewModel.gestureVolumeEnabled.collectAsState()
    val volumeSensitivity by viewModel.gestureVolumeSensitivity.collectAsState()
    val zoomEnabled by viewModel.gestureZoomEnabled.collectAsState()
    val panEnabled by viewModel.gesturePanEnabled.collectAsState()
    val doubleTapEnabled by viewModel.gestureDoubleTapEnabled.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestures") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // Seek gesture
            GestureToggleItem(
                title = "Seek gesture",
                subtitle = "Swipe horizontally to seek",
                icon = Icons.Rounded.SwipeRight,
                checked = seekEnabled,
                onCheckedChange = { viewModel.setGestureSeekEnabled(it) },
                showTopDivider = true
            )

            // Seek sensitivity — collapses when the gesture is off.
            AnimatedVisibility(visible = seekEnabled) {
                GestureSensitivityItem(
                    title = "Seek gesture sensitivity",
                    value = seekSensitivity,
                    enabled = true,
                    onValueChange = { viewModel.setGestureSeekSensitivity(it) }
                )
            }

            // Brightness gesture
            GestureToggleItem(
                title = "Brightness gesture",
                subtitle = "Adjust brightness by swiping vertically",
                icon = Icons.Rounded.WbSunny,
                checked = brightnessEnabled,
                onCheckedChange = { viewModel.setGestureBrightnessEnabled(it) }
            )

            // Brightness sensitivity — collapses when the gesture is off.
            AnimatedVisibility(visible = brightnessEnabled) {
                GestureSensitivityItem(
                    title = "Brightness gesture sensitivity",
                    value = brightnessSensitivity,
                    enabled = true,
                    onValueChange = { viewModel.setGestureBrightnessSensitivity(it) }
                )
            }

            // Volume gesture
            GestureToggleItem(
                title = "Volume gesture",
                subtitle = "Adjust volume by swiping vertically",
                icon = Icons.Rounded.VolumeUp,
                checked = volumeEnabled,
                onCheckedChange = { viewModel.setGestureVolumeEnabled(it) }
            )

            // Volume sensitivity — collapses when the gesture is off.
            AnimatedVisibility(visible = volumeEnabled) {
                GestureSensitivityItem(
                    title = "Volume gesture sensitivity",
                    value = volumeSensitivity,
                    enabled = true,
                    onValueChange = { viewModel.setGestureVolumeSensitivity(it) }
                )
            }

            // Zoom gesture
            GestureToggleItem(
                title = "Zoom gesture",
                subtitle = "Pinch to zoom the video",
                icon = Icons.Rounded.ZoomOutMap,
                checked = zoomEnabled,
                onCheckedChange = { viewModel.setGestureZoomEnabled(it) }
            )

            // Pan gesture
            GestureToggleItem(
                title = "Pan gesture",
                subtitle = "Zoom and pan the video",
                icon = Icons.Rounded.PanToolAlt,
                checked = panEnabled,
                onCheckedChange = { viewModel.setGesturePanEnabled(it) }
            )

            // Double tap gesture
            GestureToggleItem(
                title = "Double tap gesture",
                subtitle = "Select double tap gesture action",
                icon = Icons.Rounded.TouchApp,
                checked = doubleTapEnabled,
                onCheckedChange = { viewModel.setGestureDoubleTapEnabled(it) }
            )
        }
    }
}

@Composable
fun GestureToggleItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
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
            com.ghost.video.ui.components.SmoothSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
        HorizontalDivider(thickness = 0.6.dp, color = lineColor)
    }
}

@Composable
fun GestureSensitivityItem(
    title: String,
    value: Float,
    enabled: Boolean,
    onValueChange: (Float) -> Unit
) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Tune,
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                tint = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                Text(
                    text = String.format("%.2f", value),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
        Box(modifier = Modifier.padding(bottom = 12.dp)) {
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = 0.1f..2.0f,
                steps = 18,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant,
                    activeTickColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.65f),
                    inactiveTickColor = MaterialTheme.colorScheme.outline,
                    disabledThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    disabledActiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f),
                    disabledInactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )
            )
        }
        HorizontalDivider(thickness = 0.6.dp, color = lineColor)
    }
}
