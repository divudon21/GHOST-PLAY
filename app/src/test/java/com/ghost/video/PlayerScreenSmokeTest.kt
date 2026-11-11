package com.ghost.video

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.assertCountEquals
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ghost.video.ui.screens.PlayerScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Smoke test: the video screen must compose without throwing. A crash here is
 * exactly what "video kholne pe kuch nahi dikhta" looks like on a device.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class PlayerScreenSmokeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `player screen composes`() {
        composeRule.setContent {
            com.ghost.video.ui.theme.AgonAppTheme {
                PlayerScreen(url = "content://media/external/video/media/42")
            }
        }
        composeRule.waitForIdle()
        // Composition must not throw. A real play attempt is not possible here
        // (Robolectric has no codecs), but the screen has to render rather than
        // crash - and the failure panel must be able to describe an error.
        //
        // The player's control row holds a ComposeView for the mute button, and a
        // ComposeView is a composition root of its own, so there are two roots
        // here by design: the screen's, and the mute button's. One root was the
        // expectation before that button became a composable.
        composeRule.onAllNodes(isRoot()).assertCountEquals(2)
        composeRule.onAllNodes(isRoot())[0].assertExists()
    }
}
