package com.ghost.video.ui.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Telegram-style horizontal swipe between bottom-nav tabs.
 *
 * Swipe LEFT -> [onNext], swipe RIGHT -> [onPrevious]. Only horizontal-dominant
 * drags past [thresholdPx] trigger a switch, so vertical list scrolling is
 * untouched. Purely gesture-driven: no full-screen slide animation, no layout
 * work — the target screen just fades in via the NavHost transition.
 */
fun Modifier.tabSwipe(
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    thresholdPx: Float = 120f
): Modifier = pointerInput(onNext, onPrevious) {
    var accumulated = 0f
    detectHorizontalDragGestures(
        onDragStart = { accumulated = 0f },
        onHorizontalDrag = { change, dragAmount ->
            change.consume()
            accumulated += dragAmount
        },
        onDragEnd = {
            when {
                accumulated > thresholdPx -> onPrevious()
                accumulated < -thresholdPx -> onNext()
            }
        },
        onDragCancel = { accumulated = 0f }
    )
}
