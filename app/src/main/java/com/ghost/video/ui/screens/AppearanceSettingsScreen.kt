package com.ghost.video.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ghost.video.R
import com.ghost.video.data.AppColor
import com.ghost.video.data.ThemePreference
import com.ghost.video.ui.components.ListSectionTitle
import com.ghost.video.ui.components.NextIcons
import com.ghost.video.ui.components.NextTopAppBar
import com.ghost.video.ui.components.PreferenceItem
import com.ghost.video.ui.components.PreferenceSwitch
import com.ghost.video.ui.components.PreferenceSwitchWithDivider
import com.ghost.video.ui.components.RadioTextButton
import com.ghost.video.viewmodel.SettingsViewModel

/**
 * Appearance preferences.
 *
 * Structure ported from NextPlayer (GPL-3.0) AppearancePreferencesScreen; the
 * theme options expand inline (no pop-up) and Ghost Play's app colour picker is
 * a segmented section with twelve seeds.
 */
@Composable
fun AppearanceSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val themePreference by viewModel.themePreference.collectAsState()
    val highContrastDark by viewModel.highContrastDark.collectAsState()
    val appColor by viewModel.appColor.collectAsState()

    AppearanceSettingsScreenContent(
        state = AppearanceSettingsUiState(
            themePreference = themePreference,
            useHighContrastDarkTheme = highContrastDark,
            appColor = appColor,
        ),
        onAction = { action ->
            when (action) {
                is AppearanceSettingsEvent.NavigateUp -> onBack()
                is AppearanceSettingsEvent.ToggleDarkTheme -> viewModel.setTheme(
                    if (themePreference == ThemePreference.DARK) ThemePreference.LIGHT
                    else ThemePreference.DARK
                )

                is AppearanceSettingsEvent.UpdateThemeConfig ->
                    viewModel.setTheme(action.themePreference)

                is AppearanceSettingsEvent.UpdateAppColor ->
                    viewModel.setAppColor(action.appColor)

                is AppearanceSettingsEvent.ToggleUseHighContrastDarkTheme ->
                    viewModel.setHighContrastDark(!highContrastDark)
            }
        },
    )
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
private fun AppearanceSettingsScreenContent(
    state: AppearanceSettingsUiState,
    onAction: (AppearanceSettingsEvent) -> Unit,
) {
    // Only one section is expanded at a time; the options unfold inline instead
    // of opening a dialog.
    var expandedSection by remember { mutableStateOf<AppearanceSection?>(null) }
    val themeExpanded = expandedSection == AppearanceSection.Theme
    val colorExpanded = expandedSection == AppearanceSection.Color

    Scaffold(
        topBar = {
            NextTopAppBar(
                title = stringResource(id = R.string.appearance_name),
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = { onAction(AppearanceSettingsEvent.NavigateUp) },
                    ) {
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
                PreferenceSwitchWithDivider(
                    title = stringResource(id = R.string.dark_theme),
                    description = state.themePreference.name(),
                    isChecked = state.themePreference == ThemePreference.DARK,
                    onChecked = { onAction(AppearanceSettingsEvent.ToggleDarkTheme) },
                    icon = NextIcons.DarkMode,
                    onClick = {
                        expandedSection = if (themeExpanded) null else AppearanceSection.Theme
                    },
                    isFirstItem = true,
                )

                AnimatedVisibility(visible = themeExpanded) {
                    InlineOptionGroup {
                        ThemePreference.entries.forEach { option ->
                            RadioTextButton(
                                text = option.name(),
                                selected = option == state.themePreference,
                                onClick = {
                                    onAction(AppearanceSettingsEvent.UpdateThemeConfig(option))
                                    expandedSection = null
                                },
                            )
                        }
                    }
                }

                PreferenceItem(
                    title = stringResource(id = R.string.app_color),
                    description = state.appColor.label,
                    icon = NextIcons.Appearance,
                    enabled = true,
                    onClick = {
                        expandedSection = if (colorExpanded) null else AppearanceSection.Color
                    },
                    trailingContent = {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color(state.appColor.seed))
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    shape = CircleShape,
                                )
                        )
                    },
                )

                AnimatedVisibility(visible = colorExpanded) {
                    AppColorPicker(
                        selected = state.appColor,
                        onSelect = { onAction(AppearanceSettingsEvent.UpdateAppColor(it)) },
                    )
                }

                PreferenceSwitch(
                    title = stringResource(R.string.high_contrast_dark_theme),
                    description = stringResource(R.string.high_contrast_dark_theme_desc),
                    icon = NextIcons.Contrast,
                    isChecked = state.useHighContrastDarkTheme,
                    onClick = {
                        onAction(AppearanceSettingsEvent.ToggleUseHighContrastDarkTheme)
                    },
                    isLastItem = true,
                )
            }
        }
    }
}

/** Container that makes an unfolded option list read like part of the segmented list. */
@Composable
private fun InlineOptionGroup(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) { content() }
    }
}

/** Twelve app colours laid out as two rows of six swatches. */
@Composable
private fun AppColorPicker(
    selected: AppColor,
    onSelect: (AppColor) -> Unit,
) {
    val swatchBackground = MaterialTheme.colorScheme.surfaceContainerHigh
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        color = swatchBackground,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AppColor.entries.chunked(6).forEach { rowColors ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    rowColors.forEach { color ->
                        val isSelected = color == selected
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(color.seed))
                                .then(
                                    if (isSelected) {
                                        Modifier.border(
                                            width = 3.dp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            shape = CircleShape,
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable { onSelect(color) },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = NextIcons.Check,
                                    contentDescription = color.label,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemePreference.name(): String {
    val stringRes = when (this) {
        ThemePreference.SYSTEM -> R.string.system_default
        ThemePreference.LIGHT -> R.string.off
        ThemePreference.DARK -> R.string.on
    }

    return stringResource(id = stringRes)
}

private enum class AppearanceSection { Theme, Color }

data class AppearanceSettingsUiState(
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val useHighContrastDarkTheme: Boolean = false,
    val appColor: AppColor = AppColor.PURPLE,
)

sealed interface AppearanceSettingsEvent {
    data object NavigateUp : AppearanceSettingsEvent
    data object ToggleDarkTheme : AppearanceSettingsEvent
    data class UpdateThemeConfig(val themePreference: ThemePreference) : AppearanceSettingsEvent
    data class UpdateAppColor(val appColor: AppColor) : AppearanceSettingsEvent
    data object ToggleUseHighContrastDarkTheme : AppearanceSettingsEvent
}
