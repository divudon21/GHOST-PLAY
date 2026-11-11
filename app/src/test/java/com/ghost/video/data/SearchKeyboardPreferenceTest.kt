package com.ghost.video.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class SearchKeyboardPreferenceTest {
    @Test
    fun defaultPersistenceAndReset() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = SettingsRepository(context)
        repository.resetAllSettings()
        assertTrue(repository.searchAutoKeyboard.first())
        repository.setSearchAutoKeyboard(false)
        assertFalse(SettingsRepository(context).searchAutoKeyboard.first())
        repository.setSearchAutoKeyboard(true)
        assertTrue(SettingsRepository(context).searchAutoKeyboard.first())
        repository.setSearchAutoKeyboard(false)
        repository.resetAllSettings()
        assertTrue(repository.searchAutoKeyboard.first())
    }
}
