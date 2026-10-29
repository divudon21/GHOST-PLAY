package com.ghost.video.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for the media-notification action indices. A single-track
 * playlist used to request compact action index 1 while only one action existed,
 * which made NotificationCompat throw IllegalArgumentException.
 */
class NotificationActionsTest {

    @Test
    fun `single item playlist exposes exactly one action`() {
        assertEquals(1, notificationActionCount(hasSiblings = false))
        assertEquals(listOf(0), compactActionIndices(hasSiblings = false).toList())
    }

    @Test
    fun `multi item playlist exposes previous play pause and next`() {
        assertEquals(3, notificationActionCount(hasSiblings = true))
        assertEquals(listOf(0, 1, 2), compactActionIndices(hasSiblings = true).toList())
    }

    @Test
    fun `compact indices are always inside the action range`() {
        listOf(true, false).forEach { siblings ->
            val count = notificationActionCount(siblings)
            compactActionIndices(siblings).forEach { index ->
                assertTrue(
                    "compact index $index must be < action count $count (siblings=$siblings)",
                    index in 0 until count
                )
            }
        }
    }
}
