package com.ghost.video.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ghost.video.R
import com.ghost.video.data.AudioLocales
import com.ghost.video.ui.components.InlineOptionGroup
import com.ghost.video.ui.components.ListSectionTitle
import com.ghost.video.ui.components.NextIcons
import com.ghost.video.ui.components.NextTopAppBar
import com.ghost.video.ui.components.PreferenceItem
import com.ghost.video.ui.components.PreferenceSwitch
import com.ghost.video.ui.components.RadioTextButton
import com.ghost.video.viewmodel.SettingsViewModel

/**
 * Audio preferences.
 *
 * Ported from NextPlayer (GPL-3.0):
 * https://github.com/anilbeesetti/nextplayer/blob/main/feature/settings/src/main/java/dev/anilbeesetti/nextplayer/settings/screens/audio/AudioPreferencesScreen.kt
 *
 * Same rows, same order, same copy and same defaults as upstream:
 *   Preferred audio language · Require audio focus · Pause on disconnect ·
 *   System volume panel · Volume boost
 *
 * The only structural difference is the language picker: this app expands option
 * pickers inline instead of opening NextPlayer's `OptionsDialog`, per the app's
 * own settings-shell rule.
 */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun AudioSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val preferredAudioLanguage by viewModel.preferredAudioLanguage.collectAsState()
    val requireAudioFocus by viewModel.requireAudioFocus.collectAsState()
    val pauseOnHeadsetDisconnect by viewModel.pauseOnHeadsetDisconnect.collectAsState()
    val showSystemVolumePanel by viewModel.showSystemVolumePanel.collectAsState()
    val volumeBoost by viewModel.volumeBoostEnabled.collectAsState()

    var showLanguageOptions by rememberSaveable { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val noneLabel = stringResource(id = R.string.audio_language_none)
    // Building the locale list is not free, so it is computed once per screen.
    val languages = remember(noneLabel) {
        listOf(noneLabel to "") + AudioLocales.available()
    }
    val languageLabel = AudioLocales.displayName(preferredAudioLanguage)
        .takeIf { it.isNotBlank() }
        ?: stringResource(id = R.string.preferred_audio_lang_description)

    Scaffold(
        topBar = {
            NextTopAppBar(
                title = stringResource(id = R.string.audio),
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
                .verticalScroll(state = scrollState)
                .padding(paddingValues = innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            ListSectionTitle(text = stringResource(id = R.string.playback))
            // The first group, which is the language row and its picker: the
            // picker belongs to that row, so it opens between it and the switch
            // below rather than several rows further down.
            Column(
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                PreferenceItem(
                    title = stringResource(id = R.string.preferred_audio_lang),
                    description = languageLabel,
                    icon = NextIcons.Language,
                    enabled = true,
                    onClick = { showLanguageOptions = !showLanguageOptions },
                    isFirstItem = true,
                    // The chevron states what the row does and which way it is
                    // pointing while the picker is open.
                    trailingContent = {
                        Icon(
                            imageVector = NextIcons.ChevronDown,
                            contentDescription = null,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer {
                                    rotationZ = if (showLanguageOptions) 180f else 0f
                                },
                        )
                    },
                )

                // The picker, directly under the row that owns it — this app
                // never opens a dialog for a preference, and a list of languages
                // parked under the last switch reads as a setting of its own.
                AnimatedVisibility(
                    visible = showLanguageOptions,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    InlineOptionGroup {
                        LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                            items(languages) { (label, code) ->
                                RadioTextButton(
                                    text = label,
                                    selected = code == preferredAudioLanguage,
                                    onClick = {
                                        viewModel.setPreferredAudioLanguage(code)
                                        showLanguageOptions = false
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // The switches, in their own group — which is what the hairline
            // between the two groups is for.
            Column(
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                PreferenceSwitch(
                    isFirstItem = true,
                    title = stringResource(id = R.string.require_audio_focus),
                    description = stringResource(id = R.string.require_audio_focus_desc),
                    icon = NextIcons.Focus,
                    isChecked = requireAudioFocus,
                    onClick = { viewModel.setRequireAudioFocus(!requireAudioFocus) },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.pause_on_headset_disconnect),
                    description = stringResource(id = R.string.pause_on_headset_disconnect_desc),
                    icon = NextIcons.HeadsetOff,
                    isChecked = pauseOnHeadsetDisconnect,
                    onClick = { viewModel.setPauseOnHeadsetDisconnect(!pauseOnHeadsetDisconnect) },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.system_volume_panel),
                    description = stringResource(id = R.string.system_volume_panel_desc),
                    icon = NextIcons.Headset,
                    isChecked = showSystemVolumePanel,
                    onClick = { viewModel.setShowSystemVolumePanel(!showSystemVolumePanel) },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.volume_boost),
                    description = stringResource(id = R.string.volume_boost_desc),
                    icon = NextIcons.VolumeUp,
                    isChecked = volumeBoost,
                    onClick = { viewModel.setVolumeBoostEnabled(!volumeBoost) },
                    isFirstItem = true,
                    isLastItem = true,
                )
            }
        }
    }
}
