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

enum class ViewLayout {
    LIST, GRID
}

enum class DecoderPriority {
    PREFER_DEVICE, PREFER_APP, DEVICE_ONLY
}

enum class SubtitleFont {
    DEFAULT, MONOSPACE, SANS_SERIF, SERIF
}

enum class OrientationPreference {
    AUTO, LANDSCAPE, PORTRAIT, SENSOR_LANDSCAPE
}

enum class DialogThemePreference {
    FOLLOW_SYSTEM, DARK, LIGHT, CUSTOM
}

class SettingsRepository(private val context: Context) {
    private val THEME_KEY = intPreferencesKey("theme_preference")
    private val COLOR_KEY = intPreferencesKey("color_preference")
    private val THUMBNAIL_STRATEGY_KEY = intPreferencesKey("thumbnail_strategy")
    private val THUMBNAIL_POSITION_KEY = intPreferencesKey("thumbnail_position_percent")
    private val VIEW_LAYOUT_KEY = intPreferencesKey("view_layout")
    private val DECODER_PRIORITY_KEY = intPreferencesKey("decoder_priority")
    private val GESTURE_SEEK_ENABLED_KEY = booleanPreferencesKey("gesture_seek_enabled")
    private val GESTURE_SEEK_SENSITIVITY_KEY = floatPreferencesKey("gesture_seek_sensitivity")
    private val GESTURE_BRIGHTNESS_ENABLED_KEY = booleanPreferencesKey("gesture_brightness_enabled")
    private val GESTURE_BRIGHTNESS_SENSITIVITY_KEY = floatPreferencesKey("gesture_brightness_sensitivity")
    private val GESTURE_VOLUME_ENABLED_KEY = booleanPreferencesKey("gesture_volume_enabled")
    private val GESTURE_VOLUME_SENSITIVITY_KEY = floatPreferencesKey("gesture_volume_sensitivity")
    private val GESTURE_ZOOM_ENABLED_KEY = booleanPreferencesKey("gesture_zoom_enabled")
    private val GESTURE_PAN_ENABLED_KEY = booleanPreferencesKey("gesture_pan_enabled")
    private val GESTURE_DOUBLE_TAP_ENABLED_KEY = booleanPreferencesKey("gesture_double_tap_enabled")
    private val SUBTITLE_FONT_KEY = intPreferencesKey("subtitle_font")
    private val SUBTITLE_BOLD_KEY = booleanPreferencesKey("subtitle_bold")
    private val SUBTITLE_SIZE_KEY = intPreferencesKey("subtitle_size")
    private val SUBTITLE_BACKGROUND_KEY = booleanPreferencesKey("subtitle_background")
    private val SUBTITLE_COLOR_KEY = intPreferencesKey("subtitle_color")
    private val SUBTITLE_EMBEDDED_STYLES_KEY = booleanPreferencesKey("subtitle_embedded_styles")
    private val ORIENTATION_KEY = intPreferencesKey("orientation_preference")
    private val RESUME_PLAYBACK_KEY = booleanPreferencesKey("resume_playback")
    private val DEFAULT_SPEED_KEY = floatPreferencesKey("default_playback_speed")
    private val AUTOPLAY_KEY = booleanPreferencesKey("autoplay")
    private val PIP_MODE_KEY = booleanPreferencesKey("pip_mode")
    private val BACKGROUND_PLAY_KEY = booleanPreferencesKey("background_play")
    private val REMEMBER_BRIGHTNESS_KEY = booleanPreferencesKey("remember_brightness")
    private val REMEMBER_SELECTIONS_KEY = booleanPreferencesKey("remember_selections")
    private val PLAYER_ORIENTATION_KEY = intPreferencesKey("player_orientation")
    private val SYSTEM_CAPTION_STYLE_KEY = booleanPreferencesKey("system_caption_style")
    private val DIALOG_THEME_KEY = intPreferencesKey("dialog_theme_preference")

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

    val viewLayout: Flow<ViewLayout> = context.dataStore.data
        .map { preferences ->
            val value = preferences[VIEW_LAYOUT_KEY] ?: ViewLayout.GRID.ordinal
            ViewLayout.values()[value]
        }

    val decoderPriority: Flow<DecoderPriority> = context.dataStore.data
        .map { preferences ->
            val value = preferences[DECODER_PRIORITY_KEY] ?: DecoderPriority.PREFER_DEVICE.ordinal
            DecoderPriority.values()[value]
        }

    val gestureSeekEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[GESTURE_SEEK_ENABLED_KEY] ?: true }

    val gestureSeekSensitivity: Flow<Float> = context.dataStore.data
        .map { it[GESTURE_SEEK_SENSITIVITY_KEY] ?: 0.5f }

    val gestureBrightnessEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[GESTURE_BRIGHTNESS_ENABLED_KEY] ?: true }

    val gestureBrightnessSensitivity: Flow<Float> = context.dataStore.data
        .map { it[GESTURE_BRIGHTNESS_SENSITIVITY_KEY] ?: 0.5f }

    val gestureVolumeEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[GESTURE_VOLUME_ENABLED_KEY] ?: true }

    val gestureVolumeSensitivity: Flow<Float> = context.dataStore.data
        .map { it[GESTURE_VOLUME_SENSITIVITY_KEY] ?: 0.5f }

    val gestureZoomEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[GESTURE_ZOOM_ENABLED_KEY] ?: true }

    val gesturePanEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[GESTURE_PAN_ENABLED_KEY] ?: false }

    val gestureDoubleTapEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[GESTURE_DOUBLE_TAP_ENABLED_KEY] ?: true }

    val subtitleFont: Flow<SubtitleFont> = context.dataStore.data
        .map { preferences ->
            val value = preferences[SUBTITLE_FONT_KEY] ?: SubtitleFont.DEFAULT.ordinal
            SubtitleFont.values()[value]
        }

    val subtitleBold: Flow<Boolean> = context.dataStore.data
        .map { it[SUBTITLE_BOLD_KEY] ?: true }

    val subtitleSize: Flow<Int> = context.dataStore.data
        .map { it[SUBTITLE_SIZE_KEY] ?: 20 }

    val subtitleBackground: Flow<Boolean> = context.dataStore.data
        .map { it[SUBTITLE_BACKGROUND_KEY] ?: false }

    val subtitleColor: Flow<Int> = context.dataStore.data
        .map { it[SUBTITLE_COLOR_KEY] ?: android.graphics.Color.WHITE }

    val subtitleEmbeddedStyles: Flow<Boolean> = context.dataStore.data
        .map { it[SUBTITLE_EMBEDDED_STYLES_KEY] ?: true }

    val orientationPreference: Flow<OrientationPreference> = context.dataStore.data
        .map { preferences ->
            val value = preferences[ORIENTATION_KEY] ?: OrientationPreference.AUTO.ordinal
            OrientationPreference.values()[value]
        }

    val resumePlayback: Flow<Boolean> = context.dataStore.data
        .map { it[RESUME_PLAYBACK_KEY] ?: true }

    val defaultPlaybackSpeed: Flow<Float> = context.dataStore.data
        .map { it[DEFAULT_SPEED_KEY] ?: 1.0f }

    val autoplay: Flow<Boolean> = context.dataStore.data
        .map { it[AUTOPLAY_KEY] ?: true }

    val pipMode: Flow<Boolean> = context.dataStore.data
        .map { it[PIP_MODE_KEY] ?: true }

    val backgroundPlay: Flow<Boolean> = context.dataStore.data
        .map { it[BACKGROUND_PLAY_KEY] ?: false }

    val rememberBrightness: Flow<Boolean> = context.dataStore.data
        .map { it[REMEMBER_BRIGHTNESS_KEY] ?: true }

    val rememberSelections: Flow<Boolean> = context.dataStore.data
        .map { it[REMEMBER_SELECTIONS_KEY] ?: true }

    val playerOrientation: Flow<OrientationPreference> = context.dataStore.data
        .map { preferences ->
            val value = preferences[PLAYER_ORIENTATION_KEY] ?: OrientationPreference.AUTO.ordinal
            OrientationPreference.values()[value]
        }

    val systemCaptionStyle: Flow<Boolean> = context.dataStore.data
        .map { it[SYSTEM_CAPTION_STYLE_KEY] ?: false }

    val dialogThemePreference: Flow<DialogThemePreference> = context.dataStore.data
        .map { preferences ->
            val value = preferences[DIALOG_THEME_KEY] ?: DialogThemePreference.FOLLOW_SYSTEM.ordinal
            DialogThemePreference.values()[value]
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

    suspend fun setThumbnailStrategy(strategy: ThumbnailStrategy) {
        context.dataStore.edit { preferences ->
            preferences[THUMBNAIL_STRATEGY_KEY] = strategy.ordinal
        }
    }

    suspend fun setThumbnailPositionPercent(percent: Int) {
        context.dataStore.edit { preferences ->
            preferences[THUMBNAIL_POSITION_KEY] = percent
        }
    }

    suspend fun setViewLayout(layout: ViewLayout) {
        context.dataStore.edit { preferences ->
            preferences[VIEW_LAYOUT_KEY] = layout.ordinal
        }
    }

    suspend fun setDecoderPriority(priority: DecoderPriority) {
        context.dataStore.edit { preferences ->
            preferences[DECODER_PRIORITY_KEY] = priority.ordinal
        }
    }

    suspend fun setGestureSeekEnabled(enabled: Boolean) {
        context.dataStore.edit { it[GESTURE_SEEK_ENABLED_KEY] = enabled }
    }

    suspend fun setGestureSeekSensitivity(sensitivity: Float) {
        context.dataStore.edit { it[GESTURE_SEEK_SENSITIVITY_KEY] = sensitivity }
    }

    suspend fun setGestureBrightnessEnabled(enabled: Boolean) {
        context.dataStore.edit { it[GESTURE_BRIGHTNESS_ENABLED_KEY] = enabled }
    }

    suspend fun setGestureBrightnessSensitivity(sensitivity: Float) {
        context.dataStore.edit { it[GESTURE_BRIGHTNESS_SENSITIVITY_KEY] = sensitivity }
    }

    suspend fun setGestureVolumeEnabled(enabled: Boolean) {
        context.dataStore.edit { it[GESTURE_VOLUME_ENABLED_KEY] = enabled }
    }

    suspend fun setGestureVolumeSensitivity(sensitivity: Float) {
        context.dataStore.edit { it[GESTURE_VOLUME_SENSITIVITY_KEY] = sensitivity }
    }

    suspend fun setGestureZoomEnabled(enabled: Boolean) {
        context.dataStore.edit { it[GESTURE_ZOOM_ENABLED_KEY] = enabled }
    }

    suspend fun setGesturePanEnabled(enabled: Boolean) {
        context.dataStore.edit { it[GESTURE_PAN_ENABLED_KEY] = enabled }
    }

    suspend fun setGestureDoubleTapEnabled(enabled: Boolean) {
        context.dataStore.edit { it[GESTURE_DOUBLE_TAP_ENABLED_KEY] = enabled }
    }

    suspend fun setSubtitleFont(font: SubtitleFont) {
        context.dataStore.edit { it[SUBTITLE_FONT_KEY] = font.ordinal }
    }

    suspend fun setSubtitleBold(bold: Boolean) {
        context.dataStore.edit { it[SUBTITLE_BOLD_KEY] = bold }
    }

    suspend fun setSubtitleSize(size: Int) {
        context.dataStore.edit { it[SUBTITLE_SIZE_KEY] = size }
    }

    suspend fun setSubtitleBackground(background: Boolean) {
        context.dataStore.edit { it[SUBTITLE_BACKGROUND_KEY] = background }
    }

    suspend fun setSubtitleColor(color: Int) {
        context.dataStore.edit { it[SUBTITLE_COLOR_KEY] = color }
    }

    suspend fun setSubtitleEmbeddedStyles(enabled: Boolean) {
        context.dataStore.edit { it[SUBTITLE_EMBEDDED_STYLES_KEY] = enabled }
    }

    suspend fun setOrientationPreference(orientation: OrientationPreference) {
        context.dataStore.edit { it[ORIENTATION_KEY] = orientation.ordinal }
    }

    suspend fun setResumePlayback(enabled: Boolean) {
        context.dataStore.edit { it[RESUME_PLAYBACK_KEY] = enabled }
    }

    suspend fun setDefaultPlaybackSpeed(speed: Float) {
        context.dataStore.edit { it[DEFAULT_SPEED_KEY] = speed }
    }

    suspend fun setAutoplay(enabled: Boolean) {
        context.dataStore.edit { it[AUTOPLAY_KEY] = enabled }
    }

    suspend fun setPipMode(enabled: Boolean) {
        context.dataStore.edit { it[PIP_MODE_KEY] = enabled }
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
        context.dataStore.edit { it[PLAYER_ORIENTATION_KEY] = orientation.ordinal }
    }

    suspend fun setSystemCaptionStyle(enabled: Boolean) {
        context.dataStore.edit { it[SYSTEM_CAPTION_STYLE_KEY] = enabled }
    }

    suspend fun setDialogTheme(theme: DialogThemePreference) {
        context.dataStore.edit { it[DIALOG_THEME_KEY] = theme.ordinal }
    }

    suspend fun resetAllSettings() {
        context.dataStore.edit { it.clear() }
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
