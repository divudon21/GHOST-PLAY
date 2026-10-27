/*
 * GHOST PLAY — navigation, ported from Convx
 * (https://github.com/cosmictaserdev-creator/Convx, GPL-3.0).
 *
 * The bar itself is Convx's `FoldingTabBar` + `FoldingTabGroup`, copied verbatim
 * into `navigation/convx/floatingtabbar` together with its backdrop/shape modules.
 * This file is the thin call site, the same one Convx uses in `FloatingNavBar.kt`,
 * reduced to Ghost's three destinations: Home, Audio, Settings.
 */

package com.ghost.video.navigation.convx

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghost.video.R
import com.ghost.video.navigation.convx.compat.AppTheme
import com.ghost.video.navigation.convx.floatingtabbar.FloatingTabBarDefaults
import com.ghost.video.navigation.convx.floatingtabbar.FloatingTabBarScrollConnection
import com.ghost.video.navigation.convx.floatingtabbar.FoldingTabBar
import com.ghost.video.navigation.convx.floatingtabbar.FoldingTabCell
import com.ghost.video.navigation.convx.floatingtabbar.FoldingTabGroup
import com.ghost.video.navigation.convx.shapes.NavContinuousCapsule

/** The only destinations the Ghost navigation bar shows. */
enum class GhostNavTab(
    val route: String,
    @DrawableRes val iconRes: Int,
    val label: String,
) {
    HOME("home", R.drawable.ic_home, "Home"),
    AUDIO("audio", R.drawable.ic_music, "Audio"),
    SETTINGS("settings", R.drawable.ic_settings, "Settings"),
}

/** Convx's expanded tab row height, unchanged. */
private val NavBarRowHeight = 68.dp

/** Confirmed Convx glyph size for tabs that fill their viewbox. */
private val NavGlyphSize = 32.dp

/**
 * Convx floating navigation bar for Ghost: the folding capsule, the draggable
 * glass slot and the scroll-driven collapse all come from the vendored sources.
 */
@Composable
fun GhostConvxNavigationBar(
    selectedRoute: String?,
    onTabSelected: (GhostNavTab) -> Unit,
    scrollConnection: FloatingTabBarScrollConnection,
    modifier: Modifier = Modifier,
) {
    // Keep the bridged scheme aligned with the live app theme (used by the vendored defaults).
    // The theme is read here, in composition; SideEffect's lambda is not composable.
    val appColorScheme = MaterialTheme.colorScheme
    SideEffect { AppTheme.colorScheme = appColorScheme }

    val backgroundColor = appColorScheme.surfaceContainerHigh
    val selectedIconColor = appColorScheme.primary
    val unselectedIconColor = appColorScheme.onSurface.copy(alpha = 0.82f)

    // Detail routes keep the indicator on the last tab (Convx's sticky route).
    var stickyRoute by rememberSaveable {
        mutableStateOf(GhostNavTab.entries.firstOrNull { it.route == selectedRoute }?.route ?: GhostNavTab.HOME.route)
    }
    LaunchedEffect(selectedRoute) {
        GhostNavTab.entries.firstOrNull { it.route == selectedRoute }?.let { stickyRoute = it.route }
    }
    val selectedKey = stickyRoute

    FoldingTabBar(
        selectedTabKey = selectedKey,
        scrollConnection = scrollConnection,
        rowHeight = NavBarRowHeight,
        tabBarContentModifier = Modifier,
        colors = FloatingTabBarDefaults.colors(
            backgroundColor = backgroundColor,
            accessoryBackgroundColor = backgroundColor,
        ),
        shapes = FloatingTabBarDefaults.shapes(
            tabBarShape = NavContinuousCapsule,
            accessoryShape = NavContinuousCapsule,
        ),
        sizes = FloatingTabBarDefaults.sizes(),
        elevations = FloatingTabBarDefaults.elevations(),
        modifier = modifier,
        accessory = null,
        // Ghost has no search/standalone destination: the third circle Convx folds
        // out for search would be an empty ring here, so it is switched off.
        detachedTabEnabled = false,
        expandedTabs = { tabs, surfaceBackdrop ->
            val selectedIndex = tabs.indexOfFirst { it.key == selectedKey }.coerceAtLeast(0)
            val selectedIndexState = rememberUpdatedState(selectedIndex)
            FoldingTabGroup(
                selectedTabIndex = { selectedIndexState.value },
                onTabSelected = { index -> tabs.getOrNull(index)?.onClick?.invoke() },
                backdrop = surfaceBackdrop,
                surfaceBackdrop = surfaceBackdrop,
                tabsCount = tabs.size,
                accentColor = selectedIconColor,
                containerColor = Color.Transparent,
                height = NavBarRowHeight,
                indicatorVisible = tabs.any { it.key == selectedKey },
            ) {
                tabs.forEach { tab ->
                    FoldingTabCell(onClick = tab.onClick, onLongClick = tab.onLongClick) {
                        Box(
                            Modifier.size(NavGlyphSize),
                            contentAlignment = Alignment.Center,
                        ) {
                            tab.icon()
                        }
                        tab.title()
                    }
                }
            }
        },
    ) {
        GhostNavTab.entries.forEach { screen ->
            val isSelected = screen.route == selectedKey
            tab(
                key = screen.route,
                title = {
                    Text(
                        text = screen.label,
                        color = if (isSelected) selectedIconColor else unselectedIconColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    )
                },
                icon = {
                    Icon(
                        painter = painterResource(screen.iconRes),
                        contentDescription = screen.label,
                        tint = if (isSelected) selectedIconColor else unselectedIconColor,
                        modifier = Modifier.size(NavGlyphSize),
                    )
                },
                onClick = { onTabSelected(screen) },
                onLongClick = null,
            )
        }
    }
}
