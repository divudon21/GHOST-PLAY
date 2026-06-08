package com.ghost.io.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import com.ghost.io.viewmodel.SettingsViewModel

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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Gestures",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Seek gesture
            GestureToggleItem(
                title = "Seek gesture",
                subtitle = "Swipe horizontally to seek forwards or backwards",
                icon = Icons.Default.Swipe,
                checked = seekEnabled,
                onCheckedChange = { viewModel.setGestureSeekEnabled(it) }
            )

            // Seek sensitivity
            GestureSensitivityItem(
                title = "Seek gesture sensitivity",
                value = seekSensitivity,
                enabled = seekEnabled,
                onValueChange = { viewModel.setGestureSeekSensitivity(it) }
            )

            // Brightness gesture
            GestureToggleItem(
                title = "Brightness gesture",
                subtitle = "Adjust brightness by swiping vertically",
                icon = Icons.Default.WbSunny,
                checked = brightnessEnabled,
                onCheckedChange = { viewModel.setGestureBrightnessEnabled(it) }
            )

            // Brightness sensitivity
            GestureSensitivityItem(
                title = "Brightness gesture sensitivity",
                value = brightnessSensitivity,
                enabled = brightnessEnabled,
                onValueChange = { viewModel.setGestureBrightnessSensitivity(it) }
            )

            // Volume gesture
            GestureToggleItem(
                title = "Volume gesture",
                subtitle = "Adjust volume by swiping vertically",
                icon = Icons.Default.VolumeUp,
                checked = volumeEnabled,
                onCheckedChange = { viewModel.setGestureVolumeEnabled(it) }
            )

            // Volume sensitivity
            GestureSensitivityItem(
                title = "Volume gesture sensitivity",
                value = volumeSensitivity,
                enabled = volumeEnabled,
                onValueChange = { viewModel.setGestureVolumeSensitivity(it) }
            )

            // Zoom gesture
            GestureToggleItem(
                title = "Zoom gesture",
                subtitle = "Pinch to zoom the video",
                icon = Icons.Default.ZoomIn,
                checked = zoomEnabled,
                onCheckedChange = { viewModel.setGestureZoomEnabled(it) }
            )

            // Pan gesture
            GestureToggleItem(
                title = "Pan gesture",
                subtitle = "Zoom and pan the video",
                icon = Icons.Default.PanTool,
                checked = panEnabled,
                onCheckedChange = { viewModel.setGesturePanEnabled(it) }
            )

            // Double tap gesture
            GestureToggleItem(
                title = "Double tap gesture",
                subtitle = "Select double tap gesture action",
                icon = Icons.Default.TouchApp,
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
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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
fun GestureSensitivityItem(
    title: String,
    value: Float,
    enabled: Boolean,
    onValueChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Text(
                        text = String.format("%.2f", value),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = 0.1f..2.0f,
                steps = 18,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
