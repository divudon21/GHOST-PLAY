package com.ghost.video.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
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
import com.ghost.video.data.ThumbnailStrategy
import com.ghost.video.ui.components.ListSectionTitle
import com.ghost.video.ui.components.NextIcons
import com.ghost.video.ui.components.NextTopAppBar
import com.ghost.video.ui.components.PreferenceSlider
import com.ghost.video.ui.components.SingleSelectablePreference
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

            Column(
                modifier = Modifier.selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                SingleSelectablePreference(
                    title = stringResource(id = R.string.first_frame),
                    description = stringResource(id = R.string.first_frame_desc),
                    selected = currentStrategy == ThumbnailStrategy.FIRST_FRAME,
                    onClick = {
                        if (currentStrategy != ThumbnailStrategy.FIRST_FRAME) {
                            viewModel.setThumbnailStrategy(ThumbnailStrategy.FIRST_FRAME)
                        }
                    },
                    isFirstItem = true,
                )
                SingleSelectablePreference(
                    title = stringResource(id = R.string.frame_at_position),
                    description = stringResource(id = R.string.frame_at_position_desc),
                    selected = currentStrategy == ThumbnailStrategy.FRAME_AT_POSITION,
                    onClick = {
                        if (currentStrategy != ThumbnailStrategy.FRAME_AT_POSITION) {
                            viewModel.setThumbnailStrategy(ThumbnailStrategy.FRAME_AT_POSITION)
                        }
                    },
                )
                SingleSelectablePreference(
                    title = stringResource(id = R.string.hybrid),
                    description = stringResource(id = R.string.hybrid_desc),
                    selected = currentStrategy == ThumbnailStrategy.HYBRID,
                    onClick = {
                        if (currentStrategy != ThumbnailStrategy.HYBRID) {
                            viewModel.setThumbnailStrategy(ThumbnailStrategy.HYBRID)
                        }
                    },
                    isLastItem = true,
                )
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
