package com.ghost.video.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ghost.video.data.AppColorPreference
import com.ghost.video.data.ThumbnailStrategy
import com.ghost.video.data.ThemePreference
import com.ghost.video.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onNavigateToAppearance: () -> Unit = {},
    onNavigateToPlayer: () -> Unit = {},
    onNavigateToThumbnail: () -> Unit = {},
    onNavigateToGestures: () -> Unit = {},
    onNavigateToDecoder: () -> Unit = {},
    onNavigateToAudio: () -> Unit = {},
    onNavigateToSubtitle: () -> Unit = {},
    onNavigateToGeneral: () -> Unit = {},
    onNavigateToBatterySaver: () -> Unit = {},
    onNavigateToAppUpdate: () -> Unit = {}
) {
    val currentTheme by viewModel.themePreference.collectAsState()
    val currentStrategy by viewModel.thumbnailStrategy.collectAsState()

    val strategyLabel = when (currentStrategy) {
        ThumbnailStrategy.FIRST_FRAME -> "First frame"
        ThumbnailStrategy.FRAME_AT_POSITION -> "Frame at position"
        ThumbnailStrategy.HYBRID -> "Hybrid (smart)"
    }

    val categories = listOf(
        SettingsCategory(
            title = "Appearance",
            subtitle = "Adjust the app to your liking",
            icon = Icons.Rounded.Palette,
            onClick = onNavigateToAppearance
        ),
        SettingsCategory(
            title = "Player",
            subtitle = "Player appearance, playback controls",
            icon = Icons.Rounded.PlayCircleOutline,
            onClick = onNavigateToPlayer
        ),
        SettingsCategory(
            title = "Gestures",
            subtitle = "Configure player touch gestures",
            icon = Icons.Rounded.Gesture,
            onClick = onNavigateToGestures
        ),
        SettingsCategory(
            title = "Decoder",
            subtitle = "Decoder priority, playback decoder options",
            icon = Icons.Rounded.Memory,
            onClick = onNavigateToDecoder
        ),
        SettingsCategory(
            title = "Audio",
            subtitle = "Audio playback options",
            icon = Icons.Rounded.Audiotrack,
            onClick = onNavigateToAudio
        ),
        SettingsCategory(
            title = "Subtitle",
            subtitle = "Subtitle appearance, playback options",
            icon = Icons.Rounded.ClosedCaption,
            onClick = onNavigateToSubtitle
        ),
        SettingsCategory(
            title = "Thumbnail generation",
            subtitle = strategyLabel,
            icon = Icons.Rounded.Image,
            onClick = onNavigateToThumbnail
        ),
        SettingsCategory(
            title = "General",
            subtitle = "User data options",
            icon = Icons.Rounded.Tune,
            onClick = onNavigateToGeneral
        ),
        SettingsCategory(
            title = "Battery saver",
            subtitle = "Reduce power usage",
            icon = Icons.Rounded.BatterySaver,
            onClick = onNavigateToBatterySaver
        ),
        SettingsCategory(
            title = "App update",
            subtitle = "Check for new versions",
            icon = Icons.Rounded.SystemUpdate,
            onClick = onNavigateToAppUpdate
        )
    )

    Scaffold(
        topBar = {
            // Compact title sitting higher: no top inset, no extra bottom padding.
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            items(categories.size) { index ->
                val category = categories[index]
                SettingsCategoryCard(
                    title = category.title,
                    subtitle = category.subtitle,
                    icon = category.icon,
                    onClick = category.onClick,
                    showTopDivider = index == 0
                )
            }
        }
    }
}

@Composable
fun SettingsCategoryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    showTopDivider: Boolean = false
) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    Column(modifier = Modifier.fillMaxWidth()) {
        if (showTopDivider) {
            HorizontalDivider(thickness = 0.6.dp, color = lineColor)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HorizontalDivider(thickness = 0.6.dp, color = lineColor)
    }
}

data class SettingsCategory(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun ColorCircle(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(color = color, shape = CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(color = MaterialTheme.colorScheme.surface, shape = CircleShape)
            )
        }
    }
}

@Composable
fun ThemeOptionCard(
    option: ThemeOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = option.title,
                style = MaterialTheme.typography.titleLarge,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

data class ThemeOption(
    val title: String,
    val icon: ImageVector,
    val preference: ThemePreference
)

data class ColorOption(
    val name: String,
    val colorValue: Color,
    val preference: AppColorPreference
)
