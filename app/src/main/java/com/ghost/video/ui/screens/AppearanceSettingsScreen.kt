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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ghost.video.R
import com.ghost.video.data.AppColor
import com.ghost.video.data.AppTextStyle
import com.ghost.video.data.ThemePreference
import com.ghost.video.ui.components.tabBarClearance
import com.ghost.video.ui.components.InlineOptionGroup
import com.ghost.video.ui.components.ListSectionTitle
import com.ghost.video.ui.components.NextIcons
import com.ghost.video.ui.components.NextTopAppBar
import com.ghost.video.ui.components.PreferenceItem
import com.ghost.video.ui.components.PreferenceSwitch
import com.ghost.video.ui.components.PreferenceSwitchWithDivider
import com.ghost.video.ui.components.RadioTextButton
import com.ghost.video.ui.theme.appTextStyleFontFamily
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
    val appTextStyle by viewModel.appTextStyle.collectAsState()

    AppearanceSettingsScreenContent(
        state = AppearanceSettingsUiState(
            themePreference = themePreference,
            useHighContrastDarkTheme = highContrastDark,
            appColor = appColor,
            appTextStyle = appTextStyle,
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

                is AppearanceSettingsEvent.UpdateAppTextStyle ->
                    viewModel.setAppTextStyle(action.textStyle)

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
    val textExpanded = expandedSection == AppearanceSection.Text

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
                .padding(horizontal = 16.dp)
                .padding(bottom = tabBarClearance()),
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

                PreferenceItem(
                    title = stringResource(id = R.string.app_text),
                    description = state.appTextStyle.appName(),
                    icon = NextIcons.TextFields,
                    enabled = true,
                    onClick = {
                        expandedSection = if (textExpanded) null else AppearanceSection.Text
                    },
                )

                AnimatedVisibility(visible = textExpanded) {
                    InlineOptionGroup {
                        AppTextStyleOptions.forEach { style ->
                            AppTextOption(
                                name = style.appName(),
                                description = style.appDescription(),
                                fontFamily = appTextStyleFontFamily(style),
                                selected = style == state.appTextStyle,
                                onClick = {
                                    onAction(AppearanceSettingsEvent.UpdateAppTextStyle(style))
                                    expandedSection = null
                                },
                            )
                        }
                    }
                }

                // The high contrast variant only exists for the dark scheme, so the
                // switch is disabled (and shown as off) while the app is in light mode
                // instead of letting it be toggled with no visible effect.
                val themeIsDark = when (state.themePreference) {
                    ThemePreference.DARK -> true
                    ThemePreference.LIGHT -> false
                    ThemePreference.SYSTEM -> isSystemInDarkTheme()
                }
                PreferenceSwitch(
                    title = stringResource(R.string.high_contrast_dark_theme),
                    description = stringResource(R.string.high_contrast_dark_theme_desc),
                    icon = NextIcons.Contrast,
                    enabled = themeIsDark,
                    isChecked = themeIsDark && state.useHighContrastDarkTheme,
                    onClick = {
                        if (themeIsDark) {
                            onAction(AppearanceSettingsEvent.ToggleUseHighContrastDarkTheme)
                        }
                    },
                    isLastItem = true,
                )
            }
        }
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

private enum class AppearanceSection { Theme, Color, Text }

data class AppearanceSettingsUiState(
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val useHighContrastDarkTheme: Boolean = false,
    val appColor: AppColor = AppColor.PURPLE,
    val appTextStyle: AppTextStyle = AppTextStyle.DEFAULT,
)

sealed interface AppearanceSettingsEvent {
    data object NavigateUp : AppearanceSettingsEvent
    data object ToggleDarkTheme : AppearanceSettingsEvent
    data class UpdateThemeConfig(val themePreference: ThemePreference) : AppearanceSettingsEvent
    data class UpdateAppColor(val appColor: AppColor) : AppearanceSettingsEvent
    data class UpdateAppTextStyle(val textStyle: AppTextStyle) : AppearanceSettingsEvent
    data object ToggleUseHighContrastDarkTheme : AppearanceSettingsEvent
}

/**
 * The app text options in DownList's order, with DownList's copy. Each row is
 * rendered in the font it selects so the choice is visible before picking it.
 */
private val AppTextStyleOptions = listOf(
    AppTextStyle.DEFAULT,
    AppTextStyle.INTER,
    AppTextStyle.NUNITO,
    AppTextStyle.LATO,
    AppTextStyle.JETBRAINS_MONO,
    AppTextStyle.MANROPE,
)

@Composable
private fun AppTextOption(
    name: String,
    description: String,
    fontFamily: FontFamily?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val nameStyle = if (fontFamily != null) TextStyle(fontFamily = fontFamily) else TextStyle.Default
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge.merge(nameStyle),
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.merge(nameStyle),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AppTextStyle.appName(): String = stringResource(
    id = when (this) {
        AppTextStyle.DEFAULT -> R.string.font_system_default
        AppTextStyle.INTER -> R.string.font_inter
        AppTextStyle.NUNITO -> R.string.font_nunito
        AppTextStyle.LATO -> R.string.font_lato
        AppTextStyle.JETBRAINS_MONO -> R.string.font_jetbrains_mono
        AppTextStyle.MANROPE -> R.string.font_manrope
        else -> R.string.font_system_default
    },
)

@Composable
private fun AppTextStyle.appDescription(): String = stringResource(
    id = when (this) {
        AppTextStyle.DEFAULT -> R.string.font_system_default_desc
        AppTextStyle.INTER -> R.string.font_inter_desc
        AppTextStyle.NUNITO -> R.string.font_nunito_desc
        AppTextStyle.LATO -> R.string.font_lato_desc
        AppTextStyle.JETBRAINS_MONO -> R.string.font_jetbrains_mono_desc
        AppTextStyle.MANROPE -> R.string.font_manrope_desc
        else -> R.string.font_system_default_desc
    },
)
