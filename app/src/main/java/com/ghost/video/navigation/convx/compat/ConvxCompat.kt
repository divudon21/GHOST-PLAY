/*
 * GHOST PLAY — Convx navigation port.
 *
 * The files in com.ghost.video.navigation.convx.* are copied verbatim from the
 * Convx project (https://github.com/cosmictaserdev-creator/Convx, GPL-3.0) so the
 * navigation bar keeps exactly the original behaviour, geometry and motion.
 * Nothing in those files was redesigned; the only change made while vendoring is
 * the package rename plus the small set of bridges below, which map the handful
 * of Convx-wide symbols the bar reads onto this app's Material theme.
 */

package com.ghost.video.navigation.convx.compat

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.ghost.video.navigation.convx.backdrop.catalog.utils.InteractiveHighlight
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh
import androidx.compose.ui.unit.dp

/**
 * Bridge for Convx's global `AppTheme.colorScheme`. It is only read for default
 * arguments of the vendored bar; the app keeps it in sync with the live Material
 * theme from [com.ghost.video.navigation.convx.GhostConvxNavigationBar].
 */
object AppTheme {
    @Volatile
    var colorScheme: ColorScheme = darkColorScheme()
}

/**
 * Convx decides the liquid chrome's light/dark appearance from the theme it is
 * running under. Ghost has user selectable themes, so the bridged scheme is the
 * accurate source instead of the system night mask.
 */
fun glassUsesDarkAppearance(): Boolean = AppTheme.colorScheme.surface.luminance() < 0.5f

/**
 * Bridge for Convx's player-morph driving values. Ghost has no shared player
 * morph, so the bar's landing echo and pill shadow stay at rest.
 */
object PlayerMorph {
    fun landingDipPx(): Float = 0f

    val pillShadow = mutableFloatStateOf(0f)
}

/** Convx freezes its backdrop after a push; Ghost has no recorded app backdrop. */
fun lastNavWasPop(): Boolean = false

/**
 * Copied verbatim from Convx `GlassCircleButton.kt`: the tanh-damped press/drag
 * stretch the bar's pills use through [InteractiveHighlight].
 */
internal fun elasticStretchLayerBlock(
    interactiveHighlight: InteractiveHighlight,
    // Bar pills pass 2x: same feel, larger travel so the stretch reads at pill size.
    magnitude: Float = 1f,
): GraphicsLayerScope.() -> Unit = {
    val width = size.width
    val height = size.height

    val progress = interactiveHighlight.pressProgress
    val scale = lerp(1f, 1f + magnitude * 4f.dp.toPx() / size.height, progress)

    val maxOffset = size.minDimension
    val initialDerivative = 0.05f
    val offset = interactiveHighlight.offset
    translationX = magnitude * maxOffset * tanh(initialDerivative * offset.x / maxOffset)
    translationY = magnitude * maxOffset * tanh(initialDerivative * offset.y / maxOffset)

    val maxDragScale = magnitude * 4f.dp.toPx() / size.height
    val offsetAngle = atan2(offset.y, offset.x)
    scaleX = scale +
        maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
        (width / height).fastCoerceAtMost(1f)
    scaleY = scale +
        maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
        (height / width).fastCoerceAtMost(1f)
}
