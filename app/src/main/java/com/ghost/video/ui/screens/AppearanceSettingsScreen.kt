package com.ghost.video.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ghost.video.R
import com.ghost.video.data.ThemePreference
import com.ghost.video.ui.components.ListSectionTitle
import com.ghost.video.ui.components.NextIcons
import com.ghost.video.ui.components.NextTopAppBar
import com.ghost.video.ui.components.OptionsDialog
import com.ghost.video.ui.components.PreferenceSwitch
import com.ghost.video.ui.components.PreferenceSwitchWithDivider
import com.ghost.video.ui.components.RadioTextButton
import com.ghost.video.viewmodel.SettingsViewModel

/**
 * Appearance preferences.
 *
 * Ported from NextPlayer (GPL-3.0):
 * https://github.com/anilbeesetti/nextplayer/blob/main/feature/settings/src/main/java/dev/anilbeesetti/nextplayer/settings/screens/appearance/AppearancePreferencesScreen.kt
 *
 * The screen structure, components and interactions are NextPlayer's: a section
 * title, a segmented list (dark theme switch with divider + theme dialog, high
 * contrast dark theme, dynamic color theme) and the radio options dialog.
 * Ghost Play's extra appearance preferences will be layered back in later.
 */
@Composable
fun AppearanceSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val themePreference by viewModel.themePreference.collectAsState()
    val highContrastDark by viewModel.highContrastDark.collectAsState()
    val useDynamicColors by viewModel.useDynamicColors.collectAsState()

    AppearanceSettingsScreenContent(
        state = AppearanceSettingsUiState(
            themePreference = themePreference,
            useHighContrastDarkTheme = highContrastDark,
            useDynamicColors = useDynamicColors,
        ),
        onAction = { action ->
            when (action) {
                is AppearanceSettingsEvent.NavigateUp -> onBack()
                is AppearanceSettingsEvent.ShowDialog -> Unit
                is AppearanceSettingsEvent.ToggleDarkTheme -> viewModel.setTheme(
                    if (themePreference == ThemePreference.DARK) ThemePreference.LIGHT
                    else ThemePreference.DARK
                )

                is AppearanceSettingsEvent.UpdateThemeConfig ->
                    viewModel.setTheme(action.themePreference)

                is AppearanceSettingsEvent.ToggleUseDynamicColors ->
                    viewModel.setUseDynamicColors(!useDynamicColors)

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
    var showDialog by remember { mutableStateOf<AppearanceSettingsDialog?>(null) }

    Scaffold(
        topBar = {
            NextTopAppBar(
                title = stringResource(id = R.string.appearance_name),
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = {
                            onAction(AppearanceSettingsEvent.NavigateUp)
                        },
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
                        onAction(
                            AppearanceSettingsEvent.ShowDialog(
                                value = AppearanceSettingsDialog.Theme,
                            ),
                        )
                        showDialog = AppearanceSettingsDialog.Theme
                    },
                    isFirstItem = true,
                )
                PreferenceSwitch(
                    title = stringResource(R.string.high_contrast_dark_theme),
                    description = stringResource(R.string.high_contrast_dark_theme_desc),
                    icon = NextIcons.Contrast,
                    isChecked = state.useHighContrastDarkTheme,
                    onClick = {
                        onAction(AppearanceSettingsEvent.ToggleUseHighContrastDarkTheme)
                    },
                    isLastItem = !supportsDynamicTheming(),
                )
                if (supportsDynamicTheming()) {
                    PreferenceSwitch(
                        title = stringResource(id = R.string.dynamic_theme),
                        description = stringResource(id = R.string.dynamic_theme_description),
                        icon = NextIcons.Appearance,
                        isChecked = state.useDynamicColors,
                        onClick = { onAction(AppearanceSettingsEvent.ToggleUseDynamicColors) },
                        isLastItem = true,
                    )
                }
            }
        }

        showDialog?.let { dialog ->
            when (dialog) {
                AppearanceSettingsDialog.Theme -> {
                    OptionsDialog(
                        text = stringResource(id = R.string.dark_theme),
                        onDismissClick = {
                            showDialog = null
                            onAction(AppearanceSettingsEvent.ShowDialog(value = null))
                        },
                    ) {
                        items(ThemePreference.entries.toTypedArray()) {
                            RadioTextButton(
                                text = it.name(),
                                selected = (it == state.themePreference),
                                onClick = {
                                    onAction(
                                        AppearanceSettingsEvent.UpdateThemeConfig(
                                            themePreference = it,
                                        ),
                                    )
                                    showDialog = null
                                    onAction(AppearanceSettingsEvent.ShowDialog(value = null))
                                },
                            )
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

private fun supportsDynamicTheming(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

data class AppearanceSettingsUiState(
    val showDialog: AppearanceSettingsDialog? = null,
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val useHighContrastDarkTheme: Boolean = false,
    val useDynamicColors: Boolean = false,
)

sealed interface AppearanceSettingsEvent {
    data object NavigateUp : AppearanceSettingsEvent

    data class ShowDialog(val value: AppearanceSettingsDialog?) : AppearanceSettingsEvent
    data object ToggleDarkTheme : AppearanceSettingsEvent
    data class UpdateThemeConfig(val themePreference: ThemePreference) : AppearanceSettingsEvent
    data object ToggleUseDynamicColors : AppearanceSettingsEvent
    data object ToggleUseHighContrastDarkTheme : AppearanceSettingsEvent
}

sealed interface AppearanceSettingsDialog {
    data object Theme : AppearanceSettingsDialog
}
