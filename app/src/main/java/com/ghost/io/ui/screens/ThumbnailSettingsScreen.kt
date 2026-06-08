package com.ghost.io.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ghost.io.data.ThumbnailStrategy
import com.ghost.io.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThumbnailSettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = viewModel()) {
    val currentStrategy by viewModel.thumbnailStrategy.collectAsState()
    val currentPosition by viewModel.thumbnailPositionPercent.collectAsState()

    val strategies = listOf(
        ThumbnailOption(
            strategy = ThumbnailStrategy.FIRST_FRAME,
            title = "First frame",
            description = "Use the first frame of the video"
        ),
        ThumbnailOption(
            strategy = ThumbnailStrategy.FRAME_AT_POSITION,
            title = "Frame at position",
            description = "Use frame at specific position in the video"
        ),
        ThumbnailOption(
            strategy = ThumbnailStrategy.HYBRID,
            title = "Hybrid (smart)",
            description = "Attempts to use the first frame. If it's mostly a solid color, uses a frame from at specific position in the video instead"
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thumbnail generation") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Strategy options
            Column(modifier = Modifier.selectableGroup()) {
                strategies.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = currentStrategy == option.strategy,
                                onClick = { viewModel.setThumbnailStrategy(option.strategy) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        RadioButton(
                            selected = currentStrategy == option.strategy,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = option.title,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = option.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (option != strategies.last()) {
                        HorizontalDivider()
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Frame position slider (shown for FRAME_AT_POSITION and HYBRID)
            if (currentStrategy == ThumbnailStrategy.FRAME_AT_POSITION || currentStrategy == ThumbnailStrategy.HYBRID) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Frame position",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Frame at ${currentPosition}% of the video duration",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = currentPosition.toFloat(),
                            onValueChange = { viewModel.setThumbnailPositionPercent(it.toInt()) },
                            valueRange = 1f..99f,
                            steps = 98
                        )
                    }
                }
            }
        }
    }
}

data class ThumbnailOption(
    val strategy: ThumbnailStrategy,
    val title: String,
    val description: String
)
