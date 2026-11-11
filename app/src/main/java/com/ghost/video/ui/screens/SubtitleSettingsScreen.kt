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
import com.ghost.video.data.MAX_SUBTITLE_DELAY
import com.ghost.video.data.MAX_SUBTITLE_SPEED
import com.ghost.video.data.MIN_SUBTITLE_DELAY
import com.ghost.video.data.MIN_SUBTITLE_SPEED
import com.ghost.video.data.SubtitleFont
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
 * Subtitle preferences.
 *
 * Ported from NextPlayer (GPL-3.0):
 * https://github.com/anilbeesetti/nextplayer/blob/main/feature/settings/src/main/java/dev/anilbeesetti/nextplayer/settings/screens/subtitle/SubtitlePreferencesScreen.kt
 */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun SubtitleSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val currentFont by viewModel.subtitleFont.collectAsState()
    val subtitleBold by viewModel.subtitleBold.collectAsState()
    val subtitleSize by viewModel.subtitleSize.collectAsState()
    val subtitleBackground by viewModel.subtitleBackground.collectAsState()
    val subtitleEmbeddedStyles by viewModel.subtitleEmbeddedStyles.collectAsState()
    val subtitleDelay by viewModel.subtitleDelay.collectAsState()
    val subtitleSpeed by viewModel.subtitleSpeed.collectAsState()
    val systemCaptionStyle by viewModel.systemCaptionStyle.collectAsState()

    var fontExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            NextTopAppBar(
                title = stringResource(id = R.string.subtitle),
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
            ListSectionTitle(text = stringResource(id = R.string.appearance_name))
            Column(
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                PreferenceSwitch(
                    title = stringResource(id = R.string.system_caption_style),
                    description = stringResource(id = R.string.system_caption_style_desc),
                    icon = NextIcons.Settings,
                    isChecked = systemCaptionStyle,
                    onClick = { viewModel.setSystemCaptionStyle(!systemCaptionStyle) },
                    isFirstItem = true,
                )
                ExpandingPreferenceItem(
                    title = stringResource(id = R.string.subtitle_font),
                    description = currentFont.label(),
                    icon = NextIcons.Font,
                    expanded = fontExpanded,
                    onToggle = { fontExpanded = !fontExpanded },
                ) {
                    SubtitleFont.entries.forEach { font ->
                        RadioTextButton(
                            text = font.label(),
                            selected = font == currentFont,
                            onClick = {
                                viewModel.setSubtitleFont(font)
                                fontExpanded = false
                            },
                        )
                    }
                }
                PreferenceSwitch(
                    title = stringResource(id = R.string.subtitle_text_bold),
                    description = stringResource(id = R.string.subtitle_text_bold_desc),
                    icon = NextIcons.Bold,
                    isChecked = subtitleBold,
                    onClick = { viewModel.setSubtitleBold(!subtitleBold) },
                )
                PreferenceSlider(
                    title = stringResource(id = R.string.subtitle_text_size),
                    description = subtitleSize.toString(),
                    icon = NextIcons.FontSize,
                    value = subtitleSize.toFloat(),
                    valueRange = 10f..40f,
                    onValueChange = { viewModel.setSubtitleSize(it.toInt()) },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.subtitle_background),
                    description = stringResource(id = R.string.subtitle_background_desc),
                    icon = NextIcons.Caption,
                    isChecked = subtitleBackground,
                    onClick = { viewModel.setSubtitleBackground(!subtitleBackground) },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.embedded_styles),
                    description = stringResource(id = R.string.embedded_styles_desc),
                    icon = NextIcons.Style,
                    isChecked = subtitleEmbeddedStyles,
                    onClick = { viewModel.setSubtitleEmbeddedStyles(!subtitleEmbeddedStyles) },
                )
                // Timing correction for subtitles that do not match the video
                // (typical for sidecar files and re-encoded releases).
                PreferenceSlider(
                    title = stringResource(id = R.string.subtitle_delay),
                    description = when {
                        subtitleDelay == 0 -> stringResource(id = R.string.subtitle_delay_off)
                        subtitleDelay > 0 -> "+%.1fs".format(subtitleDelay / 1000f)
                        else -> "-%.1fs".format(-subtitleDelay / 1000f)
                    },
                    icon = NextIcons.Timer,
                    value = subtitleDelay.toFloat(),
                    valueRange = MIN_SUBTITLE_DELAY.toFloat()..MAX_SUBTITLE_DELAY.toFloat(),
                    steps = 39,
                    // Snap to 0.5 s steps and only persist real changes, otherwise a
                    // single drag would write hundreds of DataStore edits.
                    onValueChange = { raw ->
                        val snapped = (raw / 500f).toInt() * 500
                        if (snapped != subtitleDelay) viewModel.setSubtitleDelay(snapped)
                        Unit
                    },
                )
                PreferenceSlider(
                    title = stringResource(id = R.string.subtitle_speed),
                    description = "%.3f×".format(subtitleSpeed),
                    icon = NextIcons.Speed,
                    value = subtitleSpeed,
                    valueRange = MIN_SUBTITLE_SPEED..MAX_SUBTITLE_SPEED,
                    steps = 19,
                    onValueChange = { raw ->
                        val snapped = (raw * 100f).toInt() / 100f
                        if (snapped != subtitleSpeed) viewModel.setSubtitleSpeed(snapped)
                        Unit
                    },
                    isLastItem = true,
                )
            }
        }
    }
}

private fun SubtitleFont.label(): String = when (this) {
    SubtitleFont.DEFAULT -> "Default"
    SubtitleFont.LORA -> "Lora"
    SubtitleFont.JETBRAINS_MONO -> "JetBrains Mono"
    SubtitleFont.NUNITO -> "Nunito"
}
