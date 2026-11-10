package com.ghost.video.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.ghost.video.R
import com.ghost.video.ui.components.NextIcons
import com.ghost.video.ui.components.tvFocusRing

/**
 * Top-level destinations shown in the bottom navigation bar.
 *
 * Ported from NextPlayer v0.17.5 (GPL-3.0):
 * https://github.com/anilbeesetti/nextplayer/blob/v0.17.5/app/src/main/java/dev/anilbeesetti/nextplayer/navigation/TopLevelNavigation.kt
 *
 * The destinations and their icons are the original ones; the single change is
 * that NextPlayer's NETWORK slot carries Ghost Play's music library, so it uses
 * the music icon.
 */
enum class GhostTopLevelDestination(
    val route: String,
    val icon: ImageVector,
    @StringRes val labelRes: Int,
) {
    MEDIA("home", NextIcons.Home, R.string.nav_home),
    SEARCH("search", NextIcons.Search, R.string.nav_search),
    MUSIC("audio", NextIcons.Audio, R.string.nav_music),
    SETTINGS("settings", NextIcons.Settings, R.string.nav_settings),
}

/**
 * The bottom navigation bar, exactly as in NextPlayer: a Material 3
 * [NavigationBar] with one [NavigationBarItem] per destination (icon + label) and
 * the television focus ring.
 */
@Composable
fun NextNavigationBar(
    destinations: List<GhostTopLevelDestination> = GhostTopLevelDestination.entries,
    isSelected: (GhostTopLevelDestination) -> Boolean,
    onDestinationClick: (GhostTopLevelDestination) -> Unit,
) {
    NavigationBar {
        destinations.forEach { destination ->
            NavigationBarItem(
                selected = isSelected(destination),
                onClick = { onDestinationClick(destination) },
                icon = { Icon(imageVector = destination.icon, contentDescription = null) },
                label = { Text(text = stringResource(destination.labelRes)) },
                modifier = Modifier.tvFocusRing(shape = RoundedCornerShape(24.dp)),
            )
        }
    }
}
