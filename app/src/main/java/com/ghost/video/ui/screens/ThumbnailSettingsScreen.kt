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
import com.ghost.video.data.ThumbnailStrategy
import com.ghost.video.ui.components.InlineOptionGroup
import com.ghost.video.ui.components.ListSectionTitle
import com.ghost.video.ui.components.NextIcons
import com.ghost.video.ui.components.NextTopAppBar
import com.ghost.video.ui.components.PreferenceItem
import com.ghost.video.ui.components.PreferenceSlider
import com.ghost.video.ui.components.RadioTextButton
import com.ghost.video.viewmodel.SettingsViewModel

/**
 * Thumbnail generation preferences.
 *
 * Ported from NextPlayer (GPL-3.0):
 * https://github.com/anilbeesetti/nextplayer/blob/main/feature/settings/src/main/java/dev/anilbeesetti/nextplayer/settings/screens/thumbnail/ThumbnailPreferencesScreen.kt
 */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun ThumbnailSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val currentStrategy by viewModel.thumbnailStrategy.collectAsState()
    val currentPosition by viewModel.thumbnailPositionPercent.collectAsState()
    var strategyExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            NextTopAppBar(
                title = stringResource(id = R.string.thumbnail_generation),
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
            ListSectionTitle(text = stringResource(id = R.string.thumbnail_generation_strategy))

            // The strategy is picked from a single row that unfolds its options
            // inline, matching the decoder / orientation / subtitle font rows.
            PreferenceItem(
                title = stringResource(id = R.string.thumbnail_generation_strategy),
                description = currentStrategy.label(),
                icon = NextIcons.Image,
                enabled = true,
                onClick = { strategyExpanded = !strategyExpanded },
                isFirstItem = true,
                isLastItem = true,
            )

            AnimatedVisibility(visible = strategyExpanded) {
                InlineOptionGroup {
                    ThumbnailStrategy.entries.forEach { strategy ->
                        RadioTextButton(
                            text = strategy.label(),
                            description = strategy.description(),
                            selected = strategy == currentStrategy,
                            onClick = {
                                viewModel.setThumbnailStrategy(strategy)
                                strategyExpanded = false
                            },
                        )
                    }
                }
            }

            PreferenceSlider(
                enabled = currentStrategy != ThumbnailStrategy.FIRST_FRAME,
                modifier = Modifier.padding(vertical = 16.dp),
                title = stringResource(R.string.frame_position),
                description = stringResource(
                    R.string.frame_position_value,
                    currentPosition,
                ),
                icon = NextIcons.Frame,
                value = currentPosition.toFloat(),
                valueRange = 0f..100f,
                isFirstItem = true,
                isLastItem = true,
                onValueChange = { viewModel.setThumbnailPositionPercent(it.toInt()) },
            )
        }
    }
}

@Composable
private fun ThumbnailStrategy.label(): String = stringResource(
    id = when (this) {
        ThumbnailStrategy.FIRST_FRAME -> R.string.first_frame
        ThumbnailStrategy.FRAME_AT_POSITION -> R.string.frame_at_position
        ThumbnailStrategy.HYBRID -> R.string.hybrid
    },
)

@Composable
private fun ThumbnailStrategy.description(): String = stringResource(
    id = when (this) {
        ThumbnailStrategy.FIRST_FRAME -> R.string.first_frame_desc
        ThumbnailStrategy.FRAME_AT_POSITION -> R.string.frame_at_position_desc
        ThumbnailStrategy.HYBRID -> R.string.hybrid_desc
    },
)
