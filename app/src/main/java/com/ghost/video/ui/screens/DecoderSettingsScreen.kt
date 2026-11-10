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
import com.ghost.video.data.DecoderPriority
import com.ghost.video.ui.components.ListSectionTitle
import com.ghost.video.ui.components.NextIcons
import com.ghost.video.ui.components.NextTopAppBar
import com.ghost.video.ui.components.SingleSelectablePreference
import com.ghost.video.viewmodel.SettingsViewModel

/**
 * Decoder preferences, laid out with NextPlayer's settings structure
 * (section title + segmented single-select list).
 */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun DecoderSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val currentPriority by viewModel.decoderPriority.collectAsState()

    Scaffold(
        topBar = {
            NextTopAppBar(
                title = stringResource(id = R.string.decoder),
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
            ListSectionTitle(text = stringResource(id = R.string.decoder))
            Column(
                modifier = Modifier.selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                SingleSelectablePreference(
                    title = stringResource(id = R.string.prefer_device_decoders),
                    description = stringResource(id = R.string.prefer_device_decoders_desc),
                    selected = currentPriority == DecoderPriority.PREFER_DEVICE,
                    onClick = {
                        if (currentPriority != DecoderPriority.PREFER_DEVICE) {
                            viewModel.setDecoderPriority(DecoderPriority.PREFER_DEVICE)
                        }
                    },
                    isFirstItem = true,
                )
                SingleSelectablePreference(
                    title = stringResource(id = R.string.prefer_app_decoders),
                    description = stringResource(id = R.string.prefer_app_decoders_desc),
                    selected = currentPriority == DecoderPriority.PREFER_APP,
                    onClick = {
                        if (currentPriority != DecoderPriority.PREFER_APP) {
                            viewModel.setDecoderPriority(DecoderPriority.PREFER_APP)
                        }
                    },
                )
                SingleSelectablePreference(
                    title = stringResource(id = R.string.device_decoders_only),
                    description = stringResource(id = R.string.device_decoders_only_desc),
                    selected = currentPriority == DecoderPriority.DEVICE_ONLY,
                    onClick = {
                        if (currentPriority != DecoderPriority.DEVICE_ONLY) {
                            viewModel.setDecoderPriority(DecoderPriority.DEVICE_ONLY)
                        }
                    },
                    isLastItem = true,
                )
            }
        }
    }
}
