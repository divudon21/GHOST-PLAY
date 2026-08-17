package com.ghost.video.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Filter
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ghost.video.data.ThumbnailStrategy
import com.ghost.video.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThumbnailSettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = viewModel()) {
    val currentStrategy by viewModel.thumbnailStrategy.collectAsState()
    val currentPosition by viewModel.thumbnailPositionPercent.collectAsState()

    val strategies = listOf(
        ThumbnailOption(
            strategy = ThumbnailStrategy.FIRST_FRAME,
            title = "First frame",
            description = "Use the first frame of the video",
            icon = Icons.Rounded.Image
        ),
        ThumbnailOption(
            strategy = ThumbnailStrategy.FRAME_AT_POSITION,
            title = "Frame at position",
            description = "Use frame at specific position in the video",
            icon = Icons.Rounded.Filter
        ),
        ThumbnailOption(
            strategy = ThumbnailStrategy.HYBRID,
            title = "Hybrid (smart)",
            description = "First frame first, then smart fallback to selected position",
            icon = Icons.Rounded.AutoAwesome
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thumbnail generation") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
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
            CapsuleSwitcherCard(
                title = "Thumbnail Strategy",
                titleIcon = Icons.Rounded.Image,
                options = listOf(
                    CapsuleSwitcherOption("First", Icons.Rounded.Image),
                    CapsuleSwitcherOption("Position", Icons.Rounded.Filter),
                    CapsuleSwitcherOption("Hybrid", Icons.Rounded.AutoAwesome)
                ),
                selectedIndex = currentStrategy.ordinal,
                onSelected = { viewModel.setThumbnailStrategy(ThumbnailStrategy.entries[it]) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (currentStrategy == ThumbnailStrategy.FRAME_AT_POSITION || currentStrategy == ThumbnailStrategy.HYBRID) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Frame position",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Frame at ${currentPosition}% of the video duration",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = currentPosition.toFloat(),
                            onValueChange = { viewModel.setThumbnailPositionPercent(it.toInt()) },
                            valueRange = 1f..99f,
                            steps = 98,
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant,
                                activeTickColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.65f),
                                inactiveTickColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                }
            }
        }
    }

    }

@Composable
fun ThumbnailDialogOptionRow(
    option: ThumbnailOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        animationSpec = tween(180),
        label = "thumbnailContainer"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = option.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                )
            }
            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn(animationSpec = tween(160)),
                exit = fadeOut(animationSpec = tween(120))
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}



data class ThumbnailOption(
    val strategy: ThumbnailStrategy,
    val title: String,
    val description: String,
    val icon: ImageVector
)
