package com.ghost.video.navigation

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.animateBounds
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.ghost.video.R
import com.ghost.video.ui.components.NextIcons

/**
 * Top-level destinations of the bottom bar.
 *
 * Icon set and animation come from NextPlayer's navigation source (GPL-3.0):
 * https://github.com/anilbeesetti/nextplayer/blob/main/app/src/main/java/dev/anilbeesetti/nextplayer/navigation/TopLevelNavigation.kt
 *
 * The only change from the original list is the third slot: NextPlayer's
 * "Network" destination is Ghost Play's music library.
 */
enum class GhostTopLevelDestination(
    val route: String,
    val icon: ImageVector,
    val labelRes: Int,
) {
    HOME("home", NextIcons.Home, R.string.nav_home),
    SEARCH("search", NextIcons.Search, R.string.nav_search),
    MUSIC("audio", NextIcons.Audio, R.string.nav_music),
    SETTINGS("settings", NextIcons.Settings, R.string.nav_settings),
}

/**
 * Floating capsule navigation bar: transparent-to-background gradient, cutout and
 * navigation bar insets and the pill that slides between the selected tabs.
 */
@Composable
fun GhostNavigationBar(
    modifier: Modifier = Modifier,
    destinations: List<GhostTopLevelDestination> = GhostTopLevelDestination.entries,
    isSelected: (GhostTopLevelDestination) -> Boolean,
    onDestinationClick: (GhostTopLevelDestination) -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            )
            .navigationBarsPadding()
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
            .padding(bottom = 12.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        NavigationView(
            destinations = destinations,
            isSelected = isSelected,
            onDestinationClick = onDestinationClick,
        )
    }
}

/**
 * The pill itself. The selected highlight is a single movable composable that
 * animates its bounds between tabs (LookaheadScope + animateBounds with the
 * Material motion scheme), exactly as in NextPlayer.
 */
@Composable
fun NavigationView(
    modifier: Modifier = Modifier,
    destinations: List<GhostTopLevelDestination>,
    isSelected: (GhostTopLevelDestination) -> Boolean,
    onDestinationClick: (GhostTopLevelDestination) -> Unit,
) {
    val animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Rect>()
    val boundsTransform = remember { BoundsTransform { _, _ -> animationSpec } }

    Box(
        modifier = modifier
            .graphicsLayer(
                shadowElevation = with(LocalDensity.current) { 6.dp.toPx() },
                shape = CircleShape,
            )
            .clip(shape = CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(4.dp),
    ) {
        LookaheadScope {
            val pill = remember {
                movableContentOf<BoxScope> { scope ->
                    with(receiver = scope) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .animateBounds(
                                    lookaheadScope = this@LookaheadScope,
                                    boundsTransform = boundsTransform,
                                )
                                .clip(shape = CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                destinations.forEach { destination ->
                    val selected = isSelected(destination)
                    Box(
                        modifier = Modifier.zIndex(zIndex = if (selected) 0f else 1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            pill(this)
                        }
                        Column(
                            Modifier
                                .clip(shape = CircleShape)
                                .selectable(
                                    selected = selected,
                                    onClick = { onDestinationClick(destination) },
                                    role = Role.Tab,
                                    indication = null,
                                    interactionSource = null,
                                )
                                .padding(
                                    horizontal = 18.dp,
                                    vertical = 12.dp,
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            CompositionLocalProvider(
                                value = LocalContentColor provides if (selected) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            ) {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = stringResource(destination.labelRes),
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Kept for call sites that only need the tab shape. */
internal val GhostNavItemShape = RoundedCornerShape(99.dp)
