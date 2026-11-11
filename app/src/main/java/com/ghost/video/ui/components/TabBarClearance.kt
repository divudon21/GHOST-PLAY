package com.ghost.video.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * How much room a scrolling tab page has to leave at its bottom edge.
 *
 * The tab bar is drawn *over* the pages rather than laid out below them — it is
 * one of the two floating pills in MainActivity, so nothing claims a strip of the
 * screen for it. That is what the mini player needs (no band above or below it),
 * but it also means a list's last rows scroll underneath the bar and stop there,
 * half covered, and on a page whose content only just overflows they cannot be
 * brought up at all: Settings ended at "General", with "Updates" behind the bar
 * and no way to reach it.
 *
 * A Material 3 [androidx.compose.material3.NavigationBar] is [NAV_BAR_HEIGHT]
 * tall and carries the navigation-bar inset itself, so the strip it covers runs
 * from its top edge to the bottom of the screen. Measured per composition rather
 * than guessed: that inset is ~24dp with gesture navigation, ~48dp with three
 * buttons, and zero on a device without either.
 *
 * Not [androidx.compose.foundation.layout.navigationBarsPadding] at the call
 * sites: this is padding added to content that already runs to the bottom edge,
 * and it must be exactly the covered height, no more.
 */
@Composable
fun tabBarClearance(): Dp =
    NAV_BAR_HEIGHT +
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
        if (LocalMiniPlayerVisible.current) MINI_PLAYER_CLEARANCE else 0.dp

/**
 * Whether the mini player is on screen.
 *
 * Provided by MainActivity, which is the only place that knows: playback
 * outlives any one screen, so the pill is drawn over the pages rather than by
 * them. While it is up it rides *on* the tab bar, and the last rows have to
 * clear it as well as the bar — otherwise starting a track buried the end of
 * every list a second time, by exactly one pill.
 */
val LocalMiniPlayerVisible = staticCompositionLocalOf { false }

/** The pill's 56dp body plus the gap above it. */
private val MINI_PLAYER_CLEARANCE = 64.dp

/** The Material 3 navigation bar's own height, without any window inset. */
private val NAV_BAR_HEIGHT = 80.dp
