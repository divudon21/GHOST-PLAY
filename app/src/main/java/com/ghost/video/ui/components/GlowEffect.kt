package com.ghost.video.ui.components

import androidx.compose.runtime.compositionLocalOf

/**
 * App-wide "glow effect" flag. When true, toggles (SmoothSwitch and friends)
 * render a soft glow behind their active state. Provided once at the top of the
 * UI tree so switches read it without each screen creating its own DataStore
 * collector — keeps it cheap.
 */
val LocalGlowEffect = compositionLocalOf { false }
