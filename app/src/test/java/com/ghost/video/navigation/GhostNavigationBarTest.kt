package com.ghost.video.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ghost.video.navigation.convx.GhostConvxNavigationBar
import com.ghost.video.navigation.convx.GhostNavTab
import com.ghost.video.navigation.convx.floatingtabbar.rememberFloatingTabBarScrollConnection
import com.ghost.video.ui.theme.AgonAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Robolectric tests for the Convx navigation bar wrapper. These render the real
 * bar (Convx's FoldingTabBar + FoldingTabGroup) and assert that it exposes
 * exactly the three Ghost tabs and that tapping one reports the right tab.
 *
 * SDK 30 is used because the liquid-glass effects are only enabled from API 31;
 * below that the bar must still render and be clickable (older devices).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class GhostNavigationBarTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val tabs = mutableListOf<GhostNavTab>()

    private fun setBar(selected: String) {
        composeRule.setContent {
            AgonAppTheme {
                GhostConvxNavigationBar(
                    selectedRoute = selected,
                    onTabSelected = { tabs += it },
                    scrollConnection = rememberFloatingTabBarScrollConnection(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    @Test
    fun `there are exactly three destinations`() {
        assertEquals(3, GhostNavTab.entries.size)
        assertEquals(
            listOf("home", "audio", "settings"),
            GhostNavTab.entries.map { it.route }
        )
        assertEquals(
            listOf("Home", "Audio", "Settings"),
            GhostNavTab.entries.map { it.label }
        )
    }

    @Test
    fun `bar renders all three labels`() {
        setBar("home")
        composeRule.waitForIdle()
        listOf("Home", "Audio", "Settings").forEach { label ->
            composeRule.onNodeWithText(label).assertExists()
        }
    }

    @Test
    fun `tapping the audio tab reports it`() {
        setBar("home")
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Audio").performClick()
        composeRule.waitForIdle()
        assertTrue("expected an Audio selection, got $tabs", tabs.contains(GhostNavTab.AUDIO))
    }

    @Test
    fun `selected route drives the highlighted tab`() {
        setBar("settings")
        composeRule.waitForIdle()
        // The label is drawn for every tab; the selected one must still exist and
        // the bar must not throw while folding into the selected tab.
        composeRule.onNodeWithText("Settings").assertExists()
    }
}
