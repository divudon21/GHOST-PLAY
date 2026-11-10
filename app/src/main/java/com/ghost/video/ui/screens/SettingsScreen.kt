package com.ghost.video.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ghost.video.data.ThumbnailStrategy
import com.ghost.video.data.ThemePreference
import com.ghost.video.ui.components.ClickablePreferenceItem
import com.ghost.video.ui.components.NextIcons
import com.ghost.video.ui.components.NextTopAppBar
import com.ghost.video.ui.components.tabSwipe
import com.ghost.video.viewmodel.SettingsViewModel

/**
 * Settings list, ported from NextPlayer (GPL-3.0):
 * https://github.com/anilbeesetti/nextplayer/blob/main/feature/settings/src/main/java/dev/anilbeesetti/nextplayer/settings/SettingsScreen.kt
 *
 * Same structure as NextPlayer: a [NextTopAppBar] with a tonal back button
 * and title, then segmented preference cards ([ClickablePreferenceItem])
 * separated by [ListItemDefaults.SegmentedGap] on a surfaceContainer
 * background. Ghost keeps its own categories and navigation callbacks; the
 * TV focus modifiers from NextPlayer are omitted (no-op on touch devices).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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
            icon = NextIcons.Appearance,
            onClick = onNavigateToAppearance
        ),
        SettingsCategory(
            title = "Player",
            subtitle = "Player appearance, playback controls",
            icon = NextIcons.Player,
            onClick = onNavigateToPlayer
        ),
        SettingsCategory(
            title = "Gestures",
            subtitle = "Configure player touch gestures",
            icon = NextIcons.SwipeHorizontal,
            onClick = onNavigateToGestures
        ),
        SettingsCategory(
            title = "Decoder",
            subtitle = "Decoder priority, playback decoder options",
            icon = NextIcons.Decoder,
            onClick = onNavigateToDecoder
        ),
        SettingsCategory(
            title = "Audio",
            subtitle = "Audio playback options",
            icon = NextIcons.Audio,
            onClick = onNavigateToAudio
        ),
        SettingsCategory(
            title = "Subtitle",
            subtitle = "Subtitle appearance, playback options",
            icon = NextIcons.Subtitle,
            onClick = onNavigateToSubtitle
        ),
        SettingsCategory(
            title = "Thumbnail generation",
            subtitle = strategyLabel,
            icon = NextIcons.Image,
            onClick = onNavigateToThumbnail
        ),
        SettingsCategory(
            title = "General",
            subtitle = "User data options",
            icon = NextIcons.ExtraSettings,
            onClick = onNavigateToGeneral
        ),
        SettingsCategory(
            title = "Battery saver",
            subtitle = "Reduce power usage",
            icon = androidx.compose.material.icons.Icons.Rounded.BatterySaver,
            onClick = onNavigateToBatterySaver
        ),
        SettingsCategory(
            title = "App update",
            subtitle = "Check for new versions",
            icon = NextIcons.Update,
            onClick = onNavigateToAppUpdate
        )
    )

    Scaffold(
        modifier = Modifier.tabSwipe(onSwipeNext, onSwipePrevious),
        // The outer NavHost already pads the tab screens by the status/nav bar
        // insets, so tell this inner Scaffold not to add them a second time
        // (otherwise the list gets an extra gap under the title and at the bottom).
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            NextTopAppBar(
                title = "Settings",
                fontWeight = FontWeight.Bold,
                navigationIcon = {
                    FilledTonalIconButton(onClick = onBack) {
                        Icon(
                            imageVector = NextIcons.ArrowBack,
                            contentDescription = "Back"
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
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            categories.forEachIndexed { index, category ->
                key(category.title) {
                    ClickablePreferenceItem(
                        title = category.title,
                        description = category.subtitle,
                        icon = category.icon,
                        onClick = category.onClick,
                        isFirstItem = index == 0,
                        isLastItem = index == categories.lastIndex,
                    )
                }
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
