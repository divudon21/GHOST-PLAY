package com.ghost.video

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ghost.video.ui.screens.playerStateName
import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.net.URLEncoder

/**
 * The player URL travels through a navigation route, so it is encoded on the way
 * in. Decoding it twice used to corrupt stream URLs (a `%2B` became a space).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class RouteUrlTest {

    @Test
    fun `encoded content uri is decoded`() {
        val original = "content://media/external/video/media/42"
        val encoded = URLEncoder.encode(original, "UTF-8")
        assertEquals(original, normalizeRouteUrl(encoded))
    }

    @Test
    fun `already decoded uri is left alone`() {
        val original = "content://media/external/video/media/42"
        assertEquals(original, normalizeRouteUrl(original))
    }

    @Test
    fun `already decoded http url keeps its percent escapes`() {
        val original = "https://example.com/a%20b/video.mp4?x=1+2"
        assertEquals(original, normalizeRouteUrl(original))
    }

    @Test
    fun `encoded http url survives exactly one decode`() {
        val original = "https://example.com/a b/video.mp4?x=1+2"
        val encoded = URLEncoder.encode(original, "UTF-8")
        assertEquals(original, normalizeRouteUrl(encoded))
    }

    @Test
    fun `empty stays empty`() {
        assertEquals("", normalizeRouteUrl(""))
    }

    @Test
    fun `player state names are readable`() {
        assertEquals("IDLE", playerStateName(Player.STATE_IDLE))
        assertEquals("BUFFERING", playerStateName(Player.STATE_BUFFERING))
        assertEquals("READY", playerStateName(Player.STATE_READY))
        assertEquals("ENDED", playerStateName(Player.STATE_ENDED))
    }
}
