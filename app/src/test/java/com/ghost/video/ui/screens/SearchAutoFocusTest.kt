package com.ghost.video.ui.screens

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class SearchAutoFocusTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun showField(enabled: MutableStateFlow<Boolean?>) {
        composeRule.setContent {
            val requester = remember { FocusRequester() }
            SearchAutoFocus(enabled, requester)
            BasicTextField(
                value = "", onValueChange = {},
                modifier = Modifier.testTag("search").focusRequester(requester),
            )
        }
    }

    @Test
    fun waitsForSavedOffSettingAndAllowsManualFocus() {
        val enabled = MutableStateFlow<Boolean?>(null)
        showField(enabled)
        composeRule.onNodeWithTag("search").assertIsNotFocused()
        composeRule.runOnIdle { enabled.value = false }
        composeRule.onNodeWithTag("search").assertIsNotFocused()
        composeRule.onNodeWithTag("search").performClick().assertIsFocused()
    }

    @Test
    fun savedOnSettingFocusesOnEntry() {
        val enabled = MutableStateFlow<Boolean?>(null)
        showField(enabled)
        composeRule.onNodeWithTag("search").assertIsNotFocused()
        composeRule.runOnIdle { enabled.value = true }
        composeRule.onNodeWithTag("search").assertIsFocused()
    }
}
