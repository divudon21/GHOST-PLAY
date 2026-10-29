/*
 * GHOST PLAY — vendored from Convx (https://github.com/cosmictaserdev-creator/Convx, GPL-3.0).
 *
 * This file is a verbatim extraction of the parts of Convx `FloatingTabBar.kt`
 * that the folding navigation bar uses: the nested-scroll connection that folds
 * the bar on scroll, its inline behaviours and the tab scope. The rest of that
 * file (the older non-folding bar) is not used by Ghost.
 */

package com.ghost.video.navigation.convx.floatingtabbar

import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

internal const val DIAG_SLOWDOWN = 1

// The container morph outlasts its fades so the overshoot settles visibly.
internal const val CollapseMorphMs = 560 * DIAG_SLOWDOWN
internal const val ExpandMorphMs = 470 * DIAG_SLOWDOWN
internal fun barMorphMs(collapsing: Boolean) = if (collapsing) CollapseMorphMs else ExpandMorphMs

/** Keeps the backdrop frozen briefly after scrolling stops. */
internal const val ScrollFreezeTailNs = 120_000_000L

// EMA weight for the scroll-velocity estimate. Plain value, no recomposition.
internal const val MomentumVelocityEmaAlpha = 0.35f

/** Draw-phase flag that reuses glass captures during bar transitions, scrolling, and artwork morphs. */
internal val LocalTabBarBackdropFrozen = staticCompositionLocalOf<() -> Boolean> { { false } }

class FloatingTabBarScrollConnection(
    initialIsInline: Boolean = false,
    private val scrollThresholdPx: Float,
    private val expandThresholdPx: Float = scrollThresholdPx,
    private val inlineBehavior: FloatingTabBarInlineBehavior = FloatingTabBarInlineBehavior.OnScrollDown
) : NestedScrollConnection {
    var isInline by mutableStateOf(initialIsInline)
        private set

    private var accumulatedScroll = 0f

    // Plain arrays are read during drawing; snapshot state would invalidate every frame.
    private val scrollClockNs = longArrayOf(0L)

    // Keep scroll velocity outside snapshot state; only graphics and transition code reads it.
    private val smoothedVelocityPx = floatArrayOf(0f)
    private val lastDeltaClockNs = longArrayOf(0L)

    // Scroll velocity controls only the collapse/expand spring stiffness.
    fun scrollSpeed(): Float = abs(smoothedVelocityPx[0])

    /** True during scrolling and briefly afterward, so glass can reuse its last capture. */
    fun isScrolling(): Boolean {
        val last = scrollClockNs[0]
        return last != 0L && System.nanoTime() - last < ScrollFreezeTailNs
    }

    private fun markScrolled() {
        scrollClockNs[0] = System.nanoTime()
    }

    fun expand() {
        isInline = false
        accumulatedScroll = 0f
    }

    fun inline() {
        isInline = true
        accumulatedScroll = 0f
    }

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        // Even a fixed bar needs its backdrop frozen while content scrolls underneath.
        if (inlineBehavior == FloatingTabBarInlineBehavior.Never) {
            markScrolled()
            return Offset.Zero
        }

        markScrolled()
        // Smooth noisy per-frame deltas before using them to animate the bar.
        smoothedVelocityPx[0] = smoothedVelocityPx[0] * (1f - MomentumVelocityEmaAlpha) +
            available.y * MomentumVelocityEmaAlpha
        lastDeltaClockNs[0] = System.nanoTime()

        val scrollDelta = available.y

        if ((accumulatedScroll > 0 && scrollDelta < 0) || (accumulatedScroll < 0 && scrollDelta > 0)) {
            accumulatedScroll = 0f
        }

        accumulatedScroll += scrollDelta

        when (inlineBehavior) {
            FloatingTabBarInlineBehavior.OnScrollDown -> {
                // Collapse needs more hysteresis to avoid flicker; expansion should respond sooner.
                if (accumulatedScroll <= -scrollThresholdPx && !isInline) {
                    isInline = true
                    accumulatedScroll = 0f
                } else if (accumulatedScroll >= expandThresholdPx && isInline) {
                    isInline = false
                    accumulatedScroll = 0f
                }
            }

            FloatingTabBarInlineBehavior.OnScrollUp -> {
                if (accumulatedScroll >= scrollThresholdPx && !isInline) {
                    isInline = true
                    accumulatedScroll = 0f
                } else if (accumulatedScroll <= -scrollThresholdPx && isInline) {
                    isInline = false
                    accumulatedScroll = 0f
                }
            }

            FloatingTabBarInlineBehavior.ExpandedAtTopOnly -> {
                // Handle this after the content reports how much scroll it consumed.
            }

            FloatingTabBarInlineBehavior.Never -> {
            }
        }

        return Offset.Zero // Don't consume the scroll, let it pass through
    }

    /** Uses unconsumed upward scroll to detect the top of the content. */
    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        if (inlineBehavior != FloatingTabBarInlineBehavior.ExpandedAtTopOnly) return Offset.Zero

        if (available.y > AtTopSlopPx) {
            if (isInline) {
                isInline = false
                accumulatedScroll = 0f
            }
        } else if (abs(consumed.y) > AtTopSlopPx && !isInline) {
            isInline = true
            accumulatedScroll = 0f
        }
        return Offset.Zero
    }
}

/** Below this many pixels a scroll event is rounding noise, not a move. */
internal const val AtTopSlopPx = 0.5f

/**
 * Creates and remembers a [FloatingTabBarScrollConnection] instance.
 *
 * @param initialIsInline Initial state of the tab bar (inline or expanded). Default is false.
 * @param scrollThreshold The scroll distance required to collapse to inline. Default is 50.dp.
 * @param expandThreshold The scroll distance required to expand back. Default is 8.dp — small
 * enough that the bar returns immediately on scroll up, large enough that a shaky finger
 * mid-scroll cannot oscillate it.
 * @param inlineBehavior controls when the bar collapses to inline mode
 * @return A remembered [FloatingTabBarScrollConnection] instance.
 */
@Composable
fun rememberFloatingTabBarScrollConnection(
    initialIsInline: Boolean = false,
    scrollThreshold: Dp = 50.dp,
    expandThreshold: Dp = 8.dp,
    inlineBehavior: FloatingTabBarInlineBehavior = FloatingTabBarInlineBehavior.OnScrollDown
): FloatingTabBarScrollConnection = with(LocalDensity.current) {
    val scrollThresholdPx = scrollThreshold.toPx()
    val expandThresholdPx = expandThreshold.toPx()
    remember(scrollThresholdPx, expandThresholdPx, inlineBehavior, initialIsInline) {
        FloatingTabBarScrollConnection(
            initialIsInline,
            scrollThresholdPx,
            expandThresholdPx,
            inlineBehavior,
        )
    }
}

/**
 * Defines when the floating tab bar should transition to inline state.
 */
enum class FloatingTabBarInlineBehavior {
    /** Never transition to inline - it stays in expanded state */
    Never,

    /** Transition to inline when scrolling down */
    OnScrollDown,

    /** Transition to inline when scrolling up */
    OnScrollUp,

    /**
     * Expanded only while the content is at its top; inline the moment it scrolls away,
     * in either direction, and expanded again only on arriving back at the top.
     *
     * No thresholds and no accumulated distance: "at the top" is read from the scroll the
     * content could NOT consume upward, which is the only thing a nested-scroll connection
     * actually knows about the list's position.
     */
    ExpandedAtTopOnly
}

interface FloatingTabBarScope {
    /**
     * Adds a regular tab to the floating tab bar.
     *
     * @param key Unique identifier for the tab
     * @param title Composable content for the tab title
     * @param icon Composable content for the tab icon
     * @param onClick Callback invoked when the tab is clicked
     * @param indication Optional indication provider for touch feedback, defaults to LocalIndication.current
     * @param onLongClick Callback for a press-and-hold, handed the cell's own bounds in window
     * coordinates so the caller can anchor a context menu to the tab that was actually held.
     */
    fun tab(
        key: Any,
        title: @Composable () -> Unit,
        icon: @Composable () -> Unit,
        onClick: () -> Unit,
        indication: (@Composable () -> Indication)? = { LocalIndication.current },
        onLongClick: ((Rect) -> Unit)? = null
    )

    /**
     * Adds a standalone tab to the floating tab bar.
     *
     * Calling this method again replaces the previous standalone tab.
     *
     * @param key Unique identifier for the standalone tab
     * @param icon Composable content for the tab icon
     * @param onClick Callback invoked when the tab is clicked
     * @param indication Optional indication provider for touch feedback, defaults to LocalIndication.current
     * @param title Composable content for the tab title. Never actually shown —
     * the standalone tab is always an icon-only floating circle, in both inline
     * and expanded states (see InlineStandaloneTab / ExpandedStandaloneTab).
     */
    fun standaloneTab(
        key: Any,
        icon: @Composable () -> Unit,
        onClick: () -> Unit,
        indication: (@Composable () -> Indication)? = { LocalIndication.current },
        title: @Composable () -> Unit = {},
        onLongClick: ((Rect) -> Unit)? = null
    )
}
