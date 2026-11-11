package com.ghost.video.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlin.math.abs

/**
 * Telegram-style horizontal swipe between bottom-nav tabs.
 *
 * Swipe LEFT -> [onNext], swipe RIGHT -> [onPrevious].
 *
 * The gesture is only claimed once it is unambiguously horizontal: past the
 * touch slop **and** travelling at least [HORIZONTAL_BIAS] times further
 * sideways than up or down. Until then the pointer is left alone, so a list
 * under this modifier keeps its vertical scroll — which it did not, with a
 * plain horizontal-drag detector on the page: that detector takes the pointer
 * at the slop for any drag whose horizontal part merely matches its vertical
 * part, and a slow vertical scroll easily does.
 *
 * Claiming is decided once, at the slop, and a gesture that has been decided
 * vertical is never stolen half way through it.
 */
fun Modifier.tabSwipe(
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    thresholdPx: Float = 120f
): Modifier = pointerInput(onNext, onPrevious) {
    val threshold = thresholdPx
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val pointer = down.id
        var start: androidx.compose.ui.input.pointer.PointerInputChange? = null
        while (start == null) {
            val change = awaitPointerEvent().changes.firstOrNull { it.id == pointer }
                ?: return@awaitEachGesture
            // Lifted, or already spoken for by something under the finger (a list
            // scrolling, a row's tap): nothing here to decide.
            if (!change.pressed || change.isConsumed) return@awaitEachGesture
            val moved = change.position - down.position
            if (moved.getDistance() < viewConfiguration.touchSlop) continue
            // Sideways by a clear margin, or this is somebody's scroll.
            if (abs(moved.x) < abs(moved.y) * HORIZONTAL_BIAS) return@awaitEachGesture
            change.consume()
            start = change
        }
        val slopped = start ?: return@awaitEachGesture

        var dragged = slopped.position.x - down.position.x
        // Read before consuming: a consumed change reports no movement.
        val finished = drag(pointer) { change ->
            dragged += change.positionChange().x
            change.consume()
        }
        if (!finished) return@awaitEachGesture
        when {
            dragged > threshold -> onPrevious()
            dragged < -threshold -> onNext()
        }
    }
}

/**
 * How much further sideways than up or down a drag has to be before it is a tab
 * swipe rather than a scroll. Half again as far is the point where nobody
 * scrolling a list has ever meant to change tab.
 */
private const val HORIZONTAL_BIAS = 1.5f
