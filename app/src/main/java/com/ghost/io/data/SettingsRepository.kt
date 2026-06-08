package com.ghost.io.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
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

enum class ThumbnailStrategy {
    FIRST_FRAME, FRAME_AT_POSITION, HYBRID
}

enum class OrientationPreference {
    AUTO, LANDSCAPE, PORTRAIT, SENSOR_LANDSCAPE
}

class SettingsRepository(private val context: Context) {
    private val THEME_KEY = intPreferencesKey("theme_preference")
    private val COLOR_KEY = intPreferencesKey("color_preference")
    private val THUMBNAIL_STRATEGY_KEY = intPreferencesKey("thumbnail_strategy")
    private val THUMBNAIL_POSITION_KEY = intPreferencesKey("thumbnail_position_percent")

    // Player settings keys
    private val RESUME_KEY = booleanPreferencesKey("player_resume")
    private val PLAYBACK_SPEED_KEY = floatPreferencesKey("player_playback_speed")
    private val AUTOPLAY_KEY = booleanPreferencesKey("player_autoplay")
    private val PIP_KEY = booleanPreferencesKey("player_pip")
    private val BACKGROUND_PLAY_KEY = booleanPreferencesKey("player_background_play")
    private val REMEMBER_BRIGHTNESS_KEY = booleanPreferencesKey("player_remember_brightness")
    private val REMEMBER_SELECTIONS_KEY = booleanPreferencesKey("player_remember_selections")
    private val ORIENTATION_KEY = intPreferencesKey("player_orientation")

    // Gesture settings keys
    private val GESTURE_SEEK_KEY = booleanPreferencesKey("gesture_seek")
    private val GESTURE_SEEK_SENSITIVITY_KEY = floatPreferencesKey("gesture_seek_sensitivity")
    private val GESTURE_BRIGHTNESS_KEY = booleanPreferencesKey("gesture_brightness")
    private val GESTURE_BRIGHTNESS_SENSITIVITY_KEY = floatPreferencesKey("gesture_brightness_sensitivity")
    private val GESTURE_VOLUME_KEY = booleanPreferencesKey("gesture_volume")
    private val GESTURE_VOLUME_SENSITIVITY_KEY = floatPreferencesKey("gesture_volume_sensitivity")
    private val GESTURE_ZOOM_KEY = booleanPreferencesKey("gesture_zoom")
    private val GESTURE_PAN_KEY = booleanPreferencesKey("gesture_pan")
    private val GESTURE_DOUBLE_TAP_KEY = booleanPreferencesKey("gesture_double_tap")

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

    val thumbnailStrategy: Flow<ThumbnailStrategy> = context.dataStore.data
        .map { preferences ->
            val value = preferences[THUMBNAIL_STRATEGY_KEY] ?: ThumbnailStrategy.HYBRID.ordinal
            ThumbnailStrategy.values()[value]
        }

    val thumbnailPositionPercent: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[THUMBNAIL_POSITION_KEY] ?: 33
        }

    // Player settings flows
    val resumePlayback: Flow<Boolean> = context.dataStore.data
        .map { it[RESUME_KEY] ?: true }

    val defaultPlaybackSpeed: Flow<Float> = context.dataStore.data
        .map { it[PLAYBACK_SPEED_KEY] ?: 1.0f }

    val autoplay: Flow<Boolean> = context.dataStore.data
        .map { it[AUTOPLAY_KEY] ?: true }

    val pipMode: Flow<Boolean> = context.dataStore.data
        .map { it[PIP_KEY] ?: true }

    val backgroundPlay: Flow<Boolean> = context.dataStore.data
        .map { it[BACKGROUND_PLAY_KEY] ?: true }

    val rememberBrightness: Flow<Boolean> = context.dataStore.data
        .map { it[REMEMBER_BRIGHTNESS_KEY] ?: true }

    val rememberSelections: Flow<Boolean> = context.dataStore.data
        .map { it[REMEMBER_SELECTIONS_KEY] ?: true }

    val playerOrientation: Flow<OrientationPreference> = context.dataStore.data
        .map { preferences ->
            val value = preferences[ORIENTATION_KEY] ?: OrientationPreference.AUTO.ordinal
            OrientationPreference.values()[value]
        }

    // Gesture settings flows
    val gestureSeekEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[GESTURE_SEEK_KEY] ?: true }

    val gestureSeekSensitivity: Flow<Float> = context.dataStore.data
        .map { it[GESTURE_SEEK_SENSITIVITY_KEY] ?: 0.5f }

    val gestureBrightnessEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[GESTURE_BRIGHTNESS_KEY] ?: true }

    val gestureBrightnessSensitivity: Flow<Float> = context.dataStore.data
        .map { it[GESTURE_BRIGHTNESS_SENSITIVITY_KEY] ?: 0.5f }

    val gestureVolumeEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[GESTURE_VOLUME_KEY] ?: true }

    val gestureVolumeSensitivity: Flow<Float> = context.dataStore.data
        .map { it[GESTURE_VOLUME_SENSITIVITY_KEY] ?: 0.5f }

    val gestureZoomEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[GESTURE_ZOOM_KEY] ?: true }

    val gesturePanEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[GESTURE_PAN_KEY] ?: false }

    val gestureDoubleTapEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[GESTURE_DOUBLE_TAP_KEY] ?: true }

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

    suspend fun setThumbnailStrategy(strategy: ThumbnailStrategy) {
        context.dataStore.edit { preferences ->
            preferences[THUMBNAIL_STRATEGY_KEY] = strategy.ordinal
        }
    }

    suspend fun setThumbnailPositionPercent(percent: Int) {
        context.dataStore.edit { preferences ->
            preferences[THUMBNAIL_POSITION_KEY] = percent.coerceIn(0, 100)
        }
    }

    // Player settings setters
    suspend fun setResumePlayback(enabled: Boolean) {
        context.dataStore.edit { it[RESUME_KEY] = enabled }
    }

    suspend fun setDefaultPlaybackSpeed(speed: Float) {
        context.dataStore.edit { it[PLAYBACK_SPEED_KEY] = speed.coerceIn(0.25f, 3.0f) }
    }

    suspend fun setAutoplay(enabled: Boolean) {
        context.dataStore.edit { it[AUTOPLAY_KEY] = enabled }
    }

    suspend fun setPipMode(enabled: Boolean) {
        context.dataStore.edit { it[PIP_KEY] = enabled }
    }

    suspend fun setBackgroundPlay(enabled: Boolean) {
        context.dataStore.edit { it[BACKGROUND_PLAY_KEY] = enabled }
    }

    suspend fun setRememberBrightness(enabled: Boolean) {
        context.dataStore.edit { it[REMEMBER_BRIGHTNESS_KEY] = enabled }
    }

    suspend fun setRememberSelections(enabled: Boolean) {
        context.dataStore.edit { it[REMEMBER_SELECTIONS_KEY] = enabled }
    }

    suspend fun setPlayerOrientation(orientation: OrientationPreference) {
        context.dataStore.edit { preferences ->
            preferences[ORIENTATION_KEY] = orientation.ordinal
        }
    }

    // Gesture settings setters
    suspend fun setGestureSeekEnabled(enabled: Boolean) {
        context.dataStore.edit { it[GESTURE_SEEK_KEY] = enabled }
    }

    suspend fun setGestureSeekSensitivity(sensitivity: Float) {
        context.dataStore.edit { it[GESTURE_SEEK_SENSITIVITY_KEY] = sensitivity.coerceIn(0.1f, 2.0f) }
    }

    suspend fun setGestureBrightnessEnabled(enabled: Boolean) {
        context.dataStore.edit { it[GESTURE_BRIGHTNESS_KEY] = enabled }
    }

    suspend fun setGestureBrightnessSensitivity(sensitivity: Float) {
        context.dataStore.edit { it[GESTURE_BRIGHTNESS_SENSITIVITY_KEY] = sensitivity.coerceIn(0.1f, 2.0f) }
    }

    suspend fun setGestureVolumeEnabled(enabled: Boolean) {
        context.dataStore.edit { it[GESTURE_VOLUME_KEY] = enabled }
    }

    suspend fun setGestureVolumeSensitivity(sensitivity: Float) {
        context.dataStore.edit { it[GESTURE_VOLUME_SENSITIVITY_KEY] = sensitivity.coerceIn(0.1f, 2.0f) }
    }

    suspend fun setGestureZoomEnabled(enabled: Boolean) {
        context.dataStore.edit { it[GESTURE_ZOOM_KEY] = enabled }
    }

    suspend fun setGesturePanEnabled(enabled: Boolean) {
        context.dataStore.edit { it[GESTURE_PAN_KEY] = enabled }
    }

    suspend fun setGestureDoubleTapEnabled(enabled: Boolean) {
        context.dataStore.edit { it[GESTURE_DOUBLE_TAP_KEY] = enabled }
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
