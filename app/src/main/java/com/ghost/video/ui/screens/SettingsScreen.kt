package com.ghost.video.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Tune
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
import com.ghost.video.data.ThumbnailStrategy
import com.ghost.video.data.ThemePreference
import com.ghost.video.ui.components.tabSwipe
import com.ghost.video.viewmodel.SettingsViewModel

/**
 * Settings list, styled after NextPlayer's settings screen
 * (https://github.com/anilbeesetti/nextplayer): a top app bar with a tonal
 * back button and title, followed by segmented card rows — leading icon,
 * bold title and a smaller description, with no trailing chevron. Each row
 * is its own rounded card separated by a small gap, instead of the old
 * divider-separated list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onBack: () -> Unit = {},
    onNavigateToAppearance: () -> Unit = {},
    onNavigateToPlayer: () -> Unit = {},
    onNavigateToThumbnail: () -> Unit = {},
    onNavigateToGestures: () -> Unit = {},
    onNavigateToDecoder: () -> Unit = {},
    onNavigateToAudio: () -> Unit = {},
    onNavigateToSubtitle: () -> Unit = {},
    onNavigateToGeneral: () -> Unit = {},
    onNavigateToBatterySaver: () -> Unit = {},
    onNavigateToAppUpdate: () -> Unit = {},
    onSwipeNext: () -> Unit = {},
    onSwipePrevious: () -> Unit = {}
) {
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
            icon = Icons.Rounded.PlayCircle,
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
            onClick = onNavigateToThumbnail,
            highlightSubtitle = true
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
        modifier = Modifier.tabSwipe(onSwipeNext, onSwipePrevious),
        // The outer NavHost already pads the tab screens by the status/nav bar
        // insets, so tell this inner Scaffold not to add them a second time
        // (otherwise the list gets an extra gap under the title and at the bottom).
        contentWindowInsets = WindowInsets(0.dp),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    FilledTonalIconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(state = rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp)
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                SettingsCategoryCard(
                    title = category.title,
                    subtitle = category.subtitle,
                    icon = category.icon,
                    onClick = category.onClick,
                    highlightSubtitle = category.highlightSubtitle
                )
            }
        }
    }
}

/**
 * One segmented settings row: a rounded card with a leading icon, a bold
 * title and a smaller description underneath. No trailing chevron, matching
 * NextPlayer's preference items.
 */
@Composable
fun SettingsCategoryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    highlightSubtitle: Boolean = false
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
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
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (highlightSubtitle) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (highlightSubtitle) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

data class SettingsCategory(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val highlightSubtitle: Boolean = false
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
