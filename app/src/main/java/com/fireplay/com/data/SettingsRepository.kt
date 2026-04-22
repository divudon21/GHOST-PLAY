package com.fireplay.com.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class ThemePreference {
    SYSTEM, LIGHT, DARK, AMOLED
}

enum class AppColorPreference {
    PURPLE, BLUE, GREEN, ORANGE, RED, 
    PINK, TEAL, YELLOW, CYAN, INDIGO
}

class SettingsRepository(private val context: Context) {
    private val THEME_KEY = intPreferencesKey("theme_preference")
    private val COLOR_KEY = intPreferencesKey("color_preference")

    val themePreference: Flow<ThemePreference> = context.dataStore.data
        .map { preferences ->
            val value = preferences[THEME_KEY] ?: ThemePreference.SYSTEM.ordinal
            ThemePreference.values()[value]
        }

    val colorPreference: Flow<AppColorPreference> = context.dataStore.data
        .map { preferences ->
            val value = preferences[COLOR_KEY] ?: AppColorPreference.PURPLE.ordinal
            AppColorPreference.values()[value]
        }

    suspend fun setThemePreference(preference: ThemePreference) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = preference.ordinal
        }
    }

    suspend fun setColorPreference(preference: AppColorPreference) {
        context.dataStore.edit { preferences ->
            preferences[COLOR_KEY] = preference.ordinal
        }
    }

    suspend fun savePlaybackPosition(url: String, position: Long) {
        val key = androidx.datastore.preferences.core.longPreferencesKey("pos_$url")
        context.dataStore.edit { preferences ->
            preferences[key] = position
        }
    }

    fun getPlaybackPosition(url: String): Flow<Long> {
        val key = androidx.datastore.preferences.core.longPreferencesKey("pos_$url")
        return context.dataStore.data.map { preferences ->
            preferences[key] ?: 0L
        }
    }

    suspend fun saveAudioTrack(url: String, id: String) {
        val key = androidx.datastore.preferences.core.stringPreferencesKey("audio_$url")
        context.dataStore.edit { preferences ->
            preferences[key] = id
        }
    }

    fun getAudioTrack(url: String): Flow<String> {
        val key = androidx.datastore.preferences.core.stringPreferencesKey("audio_$url")
        return context.dataStore.data.map { preferences ->
            preferences[key] ?: ""
        }
    }

    suspend fun saveTextTrack(url: String, id: String) {
        val key = androidx.datastore.preferences.core.stringPreferencesKey("text_$url")
        context.dataStore.edit { preferences ->
            preferences[key] = id
        }
    }

    fun getTextTrack(url: String): Flow<String> {
        val key = androidx.datastore.preferences.core.stringPreferencesKey("text_$url")
        return context.dataStore.data.map { preferences ->
            preferences[key] ?: ""
        }
    }
}