package com.ghost.video.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ghost.video.R
import com.ghost.video.ui.components.ListSectionTitle
import com.ghost.video.ui.components.NextIcons
import com.ghost.video.ui.components.NextTopAppBar
import com.ghost.video.ui.components.PreferenceSlider
import com.ghost.video.ui.components.PreferenceSwitch
import com.ghost.video.viewmodel.SettingsViewModel

/**
 * Gesture preferences.
 *
 * Ported from NextPlayer (GPL-3.0):
 * https://github.com/anilbeesetti/nextplayer/blob/main/feature/settings/src/main/java/dev/anilbeesetti/nextplayer/settings/screens/gesture/GesturePreferencesScreen.kt
 */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun GesturesSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
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
            NextTopAppBar(
                title = stringResource(id = R.string.gestures),
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
            ListSectionTitle(text = stringResource(id = R.string.gestures))
            Column(
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                PreferenceSwitch(
                    title = stringResource(id = R.string.seek_gesture),
                    description = stringResource(id = R.string.seek_gesture_description),
                    icon = NextIcons.SwipeHorizontal,
                    isChecked = seekEnabled,
                    onClick = { viewModel.setGestureSeekEnabled(!seekEnabled) },
                    isFirstItem = true,
                )
                PreferenceSlider(
                    title = stringResource(id = R.string.seek_gesture_sensitivity),
                    description = "%.1f×".format(seekSensitivity),
                    icon = NextIcons.Sensitivity,
                    enabled = seekEnabled,
                    value = seekSensitivity,
                    valueRange = 0.1f..2.0f,
                    onValueChange = { viewModel.setGestureSeekSensitivity(it) },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.brightness_gesture),
                    description = stringResource(id = R.string.brightness_gesture_description),
                    icon = NextIcons.SwipeVertical,
                    isChecked = brightnessEnabled,
                    onClick = { viewModel.setGestureBrightnessEnabled(!brightnessEnabled) },
                )
                PreferenceSlider(
                    title = stringResource(id = R.string.brightness_gesture_sensitivity),
                    description = "%.1f×".format(brightnessSensitivity),
                    icon = NextIcons.Brightness,
                    enabled = brightnessEnabled,
                    value = brightnessSensitivity,
                    valueRange = 0.1f..2.0f,
                    onValueChange = { viewModel.setGestureBrightnessSensitivity(it) },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.volume_gesture),
                    description = stringResource(id = R.string.volume_gesture_description),
                    icon = NextIcons.VolumeUp,
                    isChecked = volumeEnabled,
                    onClick = { viewModel.setGestureVolumeEnabled(!volumeEnabled) },
                )
                PreferenceSlider(
                    title = stringResource(id = R.string.volume_gesture_sensitivity),
                    description = "%.1f×".format(volumeSensitivity),
                    icon = NextIcons.Sensitivity,
                    enabled = volumeEnabled,
                    value = volumeSensitivity,
                    valueRange = 0.1f..2.0f,
                    onValueChange = { viewModel.setGestureVolumeSensitivity(it) },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.zoom_gesture),
                    description = stringResource(id = R.string.zoom_gesture_description),
                    icon = NextIcons.Pinch,
                    isChecked = zoomEnabled,
                    onClick = { viewModel.setGestureZoomEnabled(!zoomEnabled) },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.pan_gesture),
                    description = stringResource(id = R.string.pan_gesture_description),
                    icon = NextIcons.Pan,
                    isChecked = panEnabled,
                    onClick = { viewModel.setGesturePanEnabled(!panEnabled) },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.double_tap),
                    description = stringResource(id = R.string.double_tap_description),
                    icon = NextIcons.DoubleTap,
                    isChecked = doubleTapEnabled,
                    onClick = { viewModel.setGestureDoubleTapEnabled(!doubleTapEnabled) },
                    isLastItem = true,
                )
            }
        }
    }
}
