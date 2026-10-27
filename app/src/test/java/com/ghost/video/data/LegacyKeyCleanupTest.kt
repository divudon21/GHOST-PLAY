package com.ghost.video.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The one-time legacy-key cleanup must keep the JSON maps the app writes today.
 * It used to match `audio_` / `text_` prefixes only, so `audio_tracks_map` and
 * `text_tracks_map` were deleted on every launch and "remember track selection"
 * never survived a restart.
 */
class LegacyKeyCleanupTest {

    private val current = setOf(
        "playback_positions_map",
        "audio_tracks_map",
        "text_tracks_map",
    )

    @Test
    fun `current keys are never removed`() {
        val removed = legacyKeysToRemove(current, current)
        assertTrue("current keys must survive cleanup, got $removed", removed.isEmpty())
    }

    @Test
    fun `legacy per url keys are removed`() {
        val all = current + listOf(
            "pos_content://media/1",
            "audio_content://media/1",
            "text_content://media/1",
        )
        val removed = legacyKeysToRemove(all, current)
        assertEquals(
            setOf("pos_content://media/1", "audio_content://media/1", "text_content://media/1"),
            removed.toSet()
        )
    }

    @Test
    fun `unrelated settings are untouched`() {
        val all = current + listOf("theme_preference", "glow_effect", "position_marker")
        assertTrue(legacyKeysToRemove(all, current).isEmpty())
    }
}
