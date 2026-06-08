package com.ghost.io.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subtitles
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
import com.ghost.io.data.AppColorPreference
import com.ghost.io.data.ThumbnailStrategy
import com.ghost.io.data.ThemePreference
import com.ghost.io.viewmodel.SettingsViewModel

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
    onNavigateToGeneral: () -> Unit = {}
) {
    val currentTheme by viewModel.themePreference.collectAsState()
    val currentColor by viewModel.colorPreference.collectAsState()
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
            icon = Icons.Default.Palette,
            onClick = onNavigateToAppearance
        ),
        SettingsCategory(
            title = "Player",
            subtitle = "Player appearance, playback controls",
            icon = Icons.Default.PlayCircle,
            onClick = onNavigateToPlayer
        ),
        SettingsCategory(
            title = "Gestures",
            subtitle = "Configure player touch gestures",
            icon = Icons.Default.Gesture,
            onClick = onNavigateToGestures
        ),
        SettingsCategory(
            title = "Decoder",
            subtitle = "Decoder priority, playback decoder options",
            icon = Icons.Default.Memory,
            onClick = onNavigateToDecoder
        ),
        SettingsCategory(
            title = "Audio",
            subtitle = "Audio playback options",
            icon = Icons.Default.Audiotrack,
            onClick = onNavigateToAudio
        ),
        SettingsCategory(
            title = "Subtitle",
            subtitle = "Subtitle appearance, playback options",
            icon = Icons.Default.Subtitles,
            onClick = onNavigateToSubtitle
        ),
        SettingsCategory(
            title = "Thumbnail generation",
            subtitle = strategyLabel,
            icon = Icons.Default.Image,
            onClick = onNavigateToThumbnail
        ),
        SettingsCategory(
            title = "General",
            subtitle = "User data options",
            icon = Icons.Default.Settings,
            onClick = onNavigateToGeneral
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(categories.size) { index ->
                val category = categories[index]
                SettingsCategoryCard(
                    title = category.title,
                    subtitle = category.subtitle,
                    icon = category.icon,
                    onClick = category.onClick
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
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
