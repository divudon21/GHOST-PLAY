package com.ghost.video.ui.components

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Ported from NextPlayer v0.17.5 (GPL-3.0):
 * https://github.com/anilbeesetti/nextplayer/blob/v0.17.5/core/ui/src/main/java/dev/anilbeesetti/nextplayer/core/ui/components/TvFocusRing.kt
 * https://github.com/anilbeesetti/nextplayer/blob/v0.17.5/core/common/src/main/java/dev/anilbeesetti/nextplayer/core/common/extensions/Context.kt
 */

/** True when the app runs on a television (leanback) device. */
internal val Context.isTelevision: Boolean
    get() {
        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        if (uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) return true
        return packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    }

private fun Modifier.thenIf(condition: Boolean, other: Modifier.() -> Modifier): Modifier =
    if (condition) other() else this

/**
 * Draws a ring around a component while it (or a child) holds D-pad focus, so the
 * focus state is clearly visible on Android TV. No-op on touch devices.
 */
@Composable
fun Modifier.tvFocusRing(
    isTv: Boolean,
    shape: Shape = CircleShape,
    color: Color = MaterialTheme.colorScheme.primary,
    width: Dp = 3.dp,
): Modifier {
    if (!isTv) return this
    var focused by remember { mutableStateOf(false) }
    return this
        .onFocusChanged { focused = it.hasFocus }
        .thenIf(focused) { border(width = width, color = color, shape = shape) }
}

/** Convenience overload that detects Android TV itself. */
@Composable
fun Modifier.tvFocusRing(
    shape: Shape = CircleShape,
    color: Color = MaterialTheme.colorScheme.primary,
    width: Dp = 3.dp,
): Modifier {
    val context = LocalContext.current
    val isTv = remember { context.isTelevision }
    return tvFocusRing(isTv = isTv, shape = shape, color = color, width = width)
}
