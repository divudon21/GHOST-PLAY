package com.ghost.video.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ghost.video.R
import com.ghost.video.data.OrientationPreference
import com.ghost.video.ui.components.ExpandingPreferenceItem
import com.ghost.video.ui.components.InlineOptionGroup
import com.ghost.video.ui.components.ListSectionTitle
import com.ghost.video.ui.components.NextIcons
import com.ghost.video.ui.components.NextTopAppBar
import com.ghost.video.ui.components.PreferenceItem
import com.ghost.video.ui.components.PreferenceSlider
import com.ghost.video.ui.components.PreferenceSwitch
import com.ghost.video.ui.components.RadioTextButton
import com.ghost.video.viewmodel.SettingsViewModel

/**
 * Player preferences.
 *
 * Ported from NextPlayer (GPL-3.0):
 * https://github.com/anilbeesetti/nextplayer/blob/main/feature/settings/src/main/java/dev/anilbeesetti/nextplayer/settings/screens/player/PlayerPreferencesScreen.kt
 */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
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

    var orientationExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            NextTopAppBar(
                title = stringResource(id = R.string.player_name),
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
            ListSectionTitle(text = stringResource(id = R.string.interface_name))
            Column(
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                ExpandingPreferenceItem(
                    title = stringResource(id = R.string.player_screen_orientation),
                    description = orientation.label(),
                    icon = NextIcons.Rotation,
                    expanded = orientationExpanded,
                    onToggle = { orientationExpanded = !orientationExpanded },
                    isFirstItem = true,
                    isLastItem = true,
                ) {
                    OrientationPreference.entries.forEach { option ->
                        RadioTextButton(
                            text = option.label(),
                            selected = option == orientation,
                            onClick = {
                                viewModel.setPlayerOrientation(option)
                                orientationExpanded = false
                            },
                        )
                    }
                }
            }

            ListSectionTitle(text = stringResource(id = R.string.playback))
            Column(
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                PreferenceSwitch(
                    title = stringResource(id = R.string.resume),
                    description = stringResource(id = R.string.resume_description),
                    icon = NextIcons.Resume,
                    isChecked = resumePlayback,
                    onClick = { viewModel.setResumePlayback(!resumePlayback) },
                    isFirstItem = true,
                )
                PreferenceSlider(
                    title = stringResource(id = R.string.default_playback_speed),
                    description = "%.2f×".format(playbackSpeed),
                    icon = NextIcons.Speed,
                    value = playbackSpeed,
                    valueRange = 0.2f..4.0f,
                    onValueChange = { viewModel.setDefaultPlaybackSpeed(it) },
                    trailingContent = {
                        FilledIconButton(onClick = { viewModel.setDefaultPlaybackSpeed(1f) }) {
                            Icon(
                                imageVector = NextIcons.History,
                                contentDescription = stringResource(
                                    id = R.string.reset_default_playback_speed,
                                ),
                            )
                        }
                    },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.autoplay_settings),
                    description = stringResource(id = R.string.autoplay_settings_description),
                    icon = NextIcons.Player,
                    isChecked = autoplay,
                    onClick = { viewModel.setAutoplay(!autoplay) },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.background_play),
                    description = stringResource(id = R.string.background_play_description),
                    icon = NextIcons.Headset,
                    isChecked = backgroundPlay,
                    onClick = { viewModel.setBackgroundPlay(!backgroundPlay) },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.pip_settings),
                    description = stringResource(id = R.string.pip_settings_description),
                    icon = NextIcons.Pip,
                    isChecked = autoPipMode,
                    onClick = { viewModel.setAutoPipMode(!autoPipMode) },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.pip_button),
                    description = stringResource(id = R.string.pip_button_description),
                    icon = NextIcons.Pip,
                    isChecked = pipMode,
                    onClick = { viewModel.setPipMode(!pipMode) },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.remember_brightness_level),
                    description = stringResource(
                        id = R.string.remember_brightness_level_description,
                    ),
                    icon = NextIcons.Brightness,
                    isChecked = rememberBrightness,
                    onClick = { viewModel.setRememberBrightness(!rememberBrightness) },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.remember_selections),
                    description = stringResource(id = R.string.remember_selections_description),
                    icon = NextIcons.Selection,
                    isChecked = rememberSelections,
                    onClick = { viewModel.setRememberSelections(!rememberSelections) },
                    isLastItem = true,
                )
            }
        }
    }
}

private fun OrientationPreference.label(): String = when (this) {
    OrientationPreference.AUTO -> "Auto"
    OrientationPreference.LANDSCAPE -> "Landscape"
    OrientationPreference.PORTRAIT -> "Portrait"
    OrientationPreference.SENSOR_LANDSCAPE -> "Sensor landscape"
}
