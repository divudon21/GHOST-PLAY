package com.ghost.video.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import org.json.JSONObject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class ThemePreference {
    SYSTEM, LIGHT, DARK
}

/**
 * Curated Material 3 palettes. Each palette is a full multi-hue colour
 * combination (primary + secondary + tertiary), not a single static colour.
 */
/**
 * The twelve app colours. Each one is a single seed colour; the whole Material 3
 * scheme (primary, secondary, tertiary and every surface role) is generated from
 * it in ui/theme/Theme.kt.
 */
enum class AppColor(val seed: Long, val label: String) {
    PURPLE(0xFF6750A4, "Purple"),
    BLUE(0xFF1A73E8, "Blue"),
    INDIGO(0xFF3949AB, "Indigo"),
    CYAN(0xFF0097A7, "Cyan"),
    TEAL(0xFF00897B, "Teal"),
    GREEN(0xFF2E7D32, "Green"),
    LIME(0xFF7CB342, "Lime"),
    YELLOW(0xFFF9A825, "Yellow"),
    ORANGE(0xFFEF6C00, "Orange"),
    RED(0xFFD32F2F, "Red"),
    PINK(0xFFD81B60, "Pink"),
    SLATE(0xFF607D8B, "Slate"),
}

enum class AppTextStyle {
    // Keep the original four entries first so existing stored ordinals remain valid.
    MANROPE, NUNITO, LORA, JETBRAINS_MONO, DEFAULT, INTER, CABARET, GRAZING_MACE, ABRAHAM_STAMP,
    LATO
}

/** Critical appearance values loaded together before the first app frame. */
data class ThemeSettings(
    val theme: ThemePreference,
    val appColor: AppColor,
    val textStyle: AppTextStyle,
    val boldText: Boolean,
    val highContrastDark: Boolean
)

/** Update options persisted by the Updates settings screen. */
data class UpdatesPreferences(
    val autoCheck: Boolean = true,
    val notifications: Boolean = true,
    val includeBeta: Boolean = false,
    val popup: Boolean = true,
)

enum class ThumbnailStrategy {
    FIRST_FRAME, FRAME_AT_POSITION, HYBRID
}

enum class ViewLayout {
    LIST, GRID, COMPACT_GRID, CINEMA
}

enum class DecoderPriority {
    PREFER_DEVICE, PREFER_APP, DEVICE_ONLY
}

enum class SubtitleFont {
    DEFAULT, LORA, JETBRAINS_MONO, NUNITO
}

enum class OrientationPreference {
    AUTO, LANDSCAPE, PORTRAIT, SENSOR_LANDSCAPE
}

enum class DialogThemePreference {
    FOLLOW_SYSTEM, DARK, LIGHT, CUSTOM
}

/** Only the official Material 3 RoundedPolygon loading indicator is supported. */
enum class LoadingIndicatorStyle {
    ROUNDED_POLYGON
}

class SettingsRepository(private val context: Context) {
    private val THEME_KEY = intPreferencesKey("theme_preference")
    private val APP_COLOR_KEY = intPreferencesKey("app_color")
    private val TEXT_STYLE_KEY = intPreferencesKey("app_text_style")
    private val BOLD_TEXT_KEY = booleanPreferencesKey("app_bold_text")
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
    private val SUBTITLE_EMBEDDED_STYLES_KEY = booleanPreferencesKey("subtitle_embedded_styles")
    private val RESUME_PLAYBACK_KEY = booleanPreferencesKey("resume_playback")
    private val DEFAULT_SPEED_KEY = floatPreferencesKey("default_playback_speed")
    private val AUTOPLAY_KEY = booleanPreferencesKey("autoplay")
    private val PIP_MODE_KEY = booleanPreferencesKey("pip_mode")
    private val AUTO_PIP_MODE_KEY = booleanPreferencesKey("auto_pip_mode")
    private val BACKGROUND_PLAY_KEY = booleanPreferencesKey("background_play")
    private val REMEMBER_BRIGHTNESS_KEY = booleanPreferencesKey("remember_brightness")
    private val REMEMBER_SELECTIONS_KEY = booleanPreferencesKey("remember_selections")
    private val PLAYER_ORIENTATION_KEY = intPreferencesKey("player_orientation")
    private val SYSTEM_CAPTION_STYLE_KEY = booleanPreferencesKey("system_caption_style")
    private val DIALOG_THEME_KEY = intPreferencesKey("dialog_theme_preference")
    private val VOLUME_BOOST_KEY = booleanPreferencesKey("volume_boost_enabled")
    private val SEARCH_AUTO_KEYBOARD_KEY = booleanPreferencesKey("search_auto_keyboard")
    private val HIGH_CONTRAST_DARK_KEY = booleanPreferencesKey("high_contrast_dark")
    private val UPDATES_AUTO_CHECK_KEY = booleanPreferencesKey("updates_auto_check")
    private val UPDATES_NOTIFICATIONS_KEY = booleanPreferencesKey("updates_notifications")
    private val UPDATES_INCLUDE_BETA_KEY = booleanPreferencesKey("updates_include_beta")
    private val UPDATES_POPUP_KEY = booleanPreferencesKey("updates_popup")
    private val UPDATES_NOTIFIED_BUILD_KEY = intPreferencesKey("updates_notified_build")
    private val LOADING_INDICATOR_STYLE_KEY = intPreferencesKey("loading_indicator_style")
    private val PLAYBACK_POSITIONS_KEY = stringPreferencesKey("playback_positions_map")
    private val AUDIO_TRACKS_KEY = stringPreferencesKey("audio_tracks_map")
    private val TEXT_TRACKS_KEY = stringPreferencesKey("text_tracks_map")
    private val BRIGHTNESS_KEY = floatPreferencesKey("player_brightness")

    /**
     * One atomic snapshot for startup theming. Unlike separate StateFlows with
     * default values, this emits only after DataStore has supplied the persisted
     * values, so the first rendered frame never flashes the default palette.
     */
    val themeSettings: Flow<ThemeSettings> = context.dataStore.data
        .catch { error ->
            if (error is CancellationException) throw error
            emit(emptyPreferences())
        }
        .map { preferences ->
            ThemeSettings(
                theme = ThemePreference.entries.getOrElse(
                    preferences[THEME_KEY] ?: ThemePreference.SYSTEM.ordinal
                ) { ThemePreference.SYSTEM },
                appColor = AppColor.entries.getOrElse(
                    preferences[APP_COLOR_KEY] ?: AppColor.PURPLE.ordinal
                ) { AppColor.PURPLE },
                textStyle = AppTextStyle.entries.getOrElse(
                    preferences[TEXT_STYLE_KEY] ?: AppTextStyle.DEFAULT.ordinal
                ) { AppTextStyle.DEFAULT },
                boldText = preferences[BOLD_TEXT_KEY] ?: false,
                highContrastDark = preferences[HIGH_CONTRAST_DARK_KEY] ?: false
            )
        }
        .distinctUntilChanged()

    val themePreference: Flow<ThemePreference> = context.dataStore.data
        .map { preferences ->
            val value = preferences[THEME_KEY] ?: ThemePreference.SYSTEM.ordinal
            ThemePreference.values().getOrElse(value) { ThemePreference.SYSTEM }
        }

    val appColor: Flow<AppColor> = context.dataStore.data
        .map { preferences ->
            val value = preferences[APP_COLOR_KEY] ?: AppColor.PURPLE.ordinal
            AppColor.entries.getOrElse(value) { AppColor.PURPLE }
        }

    val appTextStyle: Flow<AppTextStyle> = context.dataStore.data
        .map { preferences ->
            val value = preferences[TEXT_STYLE_KEY] ?: AppTextStyle.DEFAULT.ordinal
            AppTextStyle.entries.getOrElse(value) { AppTextStyle.DEFAULT }
        }
        .distinctUntilChanged()

    val boldText: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[BOLD_TEXT_KEY] ?: false }
        .distinctUntilChanged()

    val thumbnailStrategy: Flow<ThumbnailStrategy> = context.dataStore.data
        .map { preferences ->
            val value = preferences[THUMBNAIL_STRATEGY_KEY] ?: ThumbnailStrategy.HYBRID.ordinal
            ThumbnailStrategy.values().getOrElse(value) { ThumbnailStrategy.HYBRID }
        }

    val thumbnailPositionPercent: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[THUMBNAIL_POSITION_KEY] ?: 33
        }

    val viewLayout: Flow<ViewLayout> = context.dataStore.data
        .map { preferences ->
            val value = preferences[VIEW_LAYOUT_KEY] ?: ViewLayout.GRID.ordinal
            ViewLayout.values().getOrElse(value) { ViewLayout.GRID }
        }

    val decoderPriority: Flow<DecoderPriority> = context.dataStore.data
        .map { preferences ->
            val value = preferences[DECODER_PRIORITY_KEY] ?: DecoderPriority.PREFER_DEVICE.ordinal
            DecoderPriority.values().getOrElse(value) { DecoderPriority.PREFER_DEVICE }
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
            SubtitleFont.values().getOrElse(value) { SubtitleFont.DEFAULT }
        }

    val subtitleBold: Flow<Boolean> = context.dataStore.data
        .map { it[SUBTITLE_BOLD_KEY] ?: true }

    val subtitleSize: Flow<Int> = context.dataStore.data
        .map { it[SUBTITLE_SIZE_KEY] ?: 20 }

    val subtitleBackground: Flow<Boolean> = context.dataStore.data
        .map { it[SUBTITLE_BACKGROUND_KEY] ?: false }

    val subtitleEmbeddedStyles: Flow<Boolean> = context.dataStore.data
        .map { it[SUBTITLE_EMBEDDED_STYLES_KEY] ?: true }

    val resumePlayback: Flow<Boolean> = context.dataStore.data
        .map { it[RESUME_PLAYBACK_KEY] ?: true }

    val defaultPlaybackSpeed: Flow<Float> = context.dataStore.data
        .map { it[DEFAULT_SPEED_KEY] ?: 1.0f }

    val autoplay: Flow<Boolean> = context.dataStore.data
        .map { it[AUTOPLAY_KEY] ?: true }

    val pipMode: Flow<Boolean> = context.dataStore.data
        .map { it[PIP_MODE_KEY] ?: true }

    val autoPipMode: Flow<Boolean> = context.dataStore.data
        .map { it[AUTO_PIP_MODE_KEY] ?: false }

    val backgroundPlay: Flow<Boolean> = context.dataStore.data
        .map { it[BACKGROUND_PLAY_KEY] ?: false }

    val rememberBrightness: Flow<Boolean> = context.dataStore.data
        .map { it[REMEMBER_BRIGHTNESS_KEY] ?: true }

    val rememberSelections: Flow<Boolean> = context.dataStore.data
        .map { it[REMEMBER_SELECTIONS_KEY] ?: true }

    val playerOrientation: Flow<OrientationPreference> = context.dataStore.data
        .map { preferences ->
            val value = preferences[PLAYER_ORIENTATION_KEY] ?: OrientationPreference.AUTO.ordinal
            OrientationPreference.values().getOrElse(value) { OrientationPreference.AUTO }
        }

    val systemCaptionStyle: Flow<Boolean> = context.dataStore.data
        .map { it[SYSTEM_CAPTION_STYLE_KEY] ?: false }

    val dialogThemePreference: Flow<DialogThemePreference> = context.dataStore.data
        .map { preferences ->
            val value = preferences[DIALOG_THEME_KEY] ?: DialogThemePreference.FOLLOW_SYSTEM.ordinal
            DialogThemePreference.values().getOrElse(value) { DialogThemePreference.FOLLOW_SYSTEM }
        }

    val volumeBoostEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[VOLUME_BOOST_KEY] ?: false }

    val searchAutoKeyboard: Flow<Boolean> = context.dataStore.data
        .map { it[SEARCH_AUTO_KEYBOARD_KEY] ?: true }

    val highContrastDark: Flow<Boolean> = context.dataStore.data
        .map { it[HIGH_CONTRAST_DARK_KEY] ?: false }

    /** Update preferences, matching DownList's UpdateService options. */
    val updatesPreferences: Flow<UpdatesPreferences> = context.dataStore.data
        .map { preferences ->
            UpdatesPreferences(
                autoCheck = preferences[UPDATES_AUTO_CHECK_KEY] ?: true,
                notifications = preferences[UPDATES_NOTIFICATIONS_KEY] ?: true,
                includeBeta = preferences[UPDATES_INCLUDE_BETA_KEY] ?: false,
                popup = preferences[UPDATES_POPUP_KEY] ?: true,
            )
        }

    val updatesNotifiedBuild: Flow<Int> = context.dataStore.data
        .map { it[UPDATES_NOTIFIED_BUILD_KEY] ?: 0 }

    val loadingIndicatorStyle: Flow<LoadingIndicatorStyle> = context.dataStore.data
        .map { LoadingIndicatorStyle.ROUNDED_POLYGON }

    suspend fun setThemePreference(preference: ThemePreference) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = preference.ordinal
        }
    }

    suspend fun setAppColor(appColor: AppColor) {
        context.dataStore.edit { preferences ->
            preferences[APP_COLOR_KEY] = appColor.ordinal
        }
    }

    suspend fun setAppTextStyle(style: AppTextStyle) {
        context.dataStore.edit { preferences ->
            preferences[TEXT_STYLE_KEY] = style.ordinal
        }
    }

    suspend fun setBoldText(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BOLD_TEXT_KEY] = enabled
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

    suspend fun setSubtitleEmbeddedStyles(enabled: Boolean) {
        context.dataStore.edit { it[SUBTITLE_EMBEDDED_STYLES_KEY] = enabled }
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

    suspend fun setAutoPipMode(enabled: Boolean) {
        context.dataStore.edit { it[AUTO_PIP_MODE_KEY] = enabled }
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

    suspend fun setVolumeBoostEnabled(enabled: Boolean) {
        context.dataStore.edit { it[VOLUME_BOOST_KEY] = enabled }
    }

    suspend fun setSearchAutoKeyboard(enabled: Boolean) {
        context.dataStore.edit { it[SEARCH_AUTO_KEYBOARD_KEY] = enabled }
    }

    suspend fun setHighContrastDark(enabled: Boolean) {
        context.dataStore.edit { it[HIGH_CONTRAST_DARK_KEY] = enabled }
    }

    suspend fun setUpdatesAutoCheck(enabled: Boolean) {
        context.dataStore.edit { it[UPDATES_AUTO_CHECK_KEY] = enabled }
    }

    suspend fun setUpdatesNotifications(enabled: Boolean) {
        context.dataStore.edit { it[UPDATES_NOTIFICATIONS_KEY] = enabled }
    }

    suspend fun setUpdatesIncludeBeta(enabled: Boolean) {
        context.dataStore.edit { it[UPDATES_INCLUDE_BETA_KEY] = enabled }
    }

    suspend fun setUpdatesPopup(enabled: Boolean) {
        context.dataStore.edit { it[UPDATES_POPUP_KEY] = enabled }
    }

    suspend fun setUpdatesNotifiedBuild(versionCode: Int) {
        context.dataStore.edit { it[UPDATES_NOTIFIED_BUILD_KEY] = versionCode }
    }

    suspend fun setLoadingIndicatorStyle(style: LoadingIndicatorStyle) {
        context.dataStore.edit { it[LOADING_INDICATOR_STYLE_KEY] = LoadingIndicatorStyle.ROUNDED_POLYGON.ordinal }
    }

    suspend fun resetAllSettings() {
        context.dataStore.edit { preferences ->
            // Keep the "already notified about this build" marker: resetting
            // preferences should not make the update notification fire again for a
            // build the user has already been told about.
            val notifiedBuild = preferences[UPDATES_NOTIFIED_BUILD_KEY]
            preferences.clear()
            if (notifiedBuild != null) preferences[UPDATES_NOTIFIED_BUILD_KEY] = notifiedBuild
        }
    }

    /**
     * One-time cleanup of the LEGACY per-URL keys (pos_<url>, audio_<url>,
     * text_<url>) from before playback state was stored in JSON maps. A large
     * number of those keys makes DataStore's first read slower, which showed up
     * as a frozen first frame on cold start. This is idempotent and cheap.
     */
    suspend fun cleanupLegacyPerUrlKeys() {
        val currentKeys = setOf(
            PLAYBACK_POSITIONS_KEY.name,
            AUDIO_TRACKS_KEY.name,
            TEXT_TRACKS_KEY.name,
        )
        context.dataStore.edit { preferences ->
            val legacy = legacyKeysToRemove(preferences.asMap().keys.map { it.name }, currentKeys)
            if (legacy.isNotEmpty()) {
                legacy.forEach { name ->
                    preferences.asMap().keys.firstOrNull { it.name == name }?.let { preferences.remove(it) }
                }
            }
        }
    }

    /**
     * Playback positions / track selections are stored as small JSON maps under a
     * fixed set of keys instead of one DataStore key per video URL. The old
     * approach grew the preferences file forever (every watched URL added a new
     * key that was never removed); this keeps at most [MAX_TRACKED_MEDIA_ENTRIES]
     * entries, evicting the least recently used.
     */
    suspend fun savePlaybackPosition(url: String, position: Long) {
        context.dataStore.edit { preferences ->
            val map = decodeStringMap(preferences[PLAYBACK_POSITIONS_KEY])
            map.remove(url)
            map[url] = position.toString()
            preferences[PLAYBACK_POSITIONS_KEY] = encodeStringMap(trimMap(map))
        }
    }

    fun getPlaybackPosition(url: String): Flow<Long> = context.dataStore.data.map { preferences ->
        decodeStringMap(preferences[PLAYBACK_POSITIONS_KEY])[url]?.toLongOrNull() ?: 0L
    }

    suspend fun saveAudioTrack(url: String, id: String) {
        context.dataStore.edit { preferences ->
            val map = decodeStringMap(preferences[AUDIO_TRACKS_KEY])
            map.remove(url)
            map[url] = id
            preferences[AUDIO_TRACKS_KEY] = encodeStringMap(trimMap(map))
        }
    }

    fun getAudioTrack(url: String): Flow<String> = context.dataStore.data.map { preferences ->
        decodeStringMap(preferences[AUDIO_TRACKS_KEY])[url] ?: ""
    }

    suspend fun saveTextTrack(url: String, id: String) {
        context.dataStore.edit { preferences ->
            val map = decodeStringMap(preferences[TEXT_TRACKS_KEY])
            map.remove(url)
            map[url] = id
            preferences[TEXT_TRACKS_KEY] = encodeStringMap(trimMap(map))
        }
    }

    fun getTextTrack(url: String): Flow<String> = context.dataStore.data.map { preferences ->
        decodeStringMap(preferences[TEXT_TRACKS_KEY])[url] ?: ""
    }

    /** Persist the player brightness so it can be restored next session. */
    suspend fun saveBrightness(value: Float) {
        context.dataStore.edit { preferences ->
            preferences[BRIGHTNESS_KEY] = value.coerceIn(0f, 1f)
        }
    }

    /** Returns the saved brightness, or -1f when none was saved. */
    fun getBrightness(): Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[BRIGHTNESS_KEY] ?: -1f
    }
}

/** Cap on how many per-video entries (positions/tracks) are remembered. */
private const val MAX_TRACKED_MEDIA_ENTRIES = 100

private fun decodeStringMap(raw: String?): LinkedHashMap<String, String> {
    val map = LinkedHashMap<String, String>()
    if (raw.isNullOrEmpty()) return map
    return try {
        val obj = JSONObject(raw)
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = obj.optString(key)
        }
        map
    } catch (_: Exception) {
        map
    }
}

private fun encodeStringMap(map: Map<String, String>): String =
    JSONObject().apply { map.forEach { (k, v) -> put(k, v) } }.toString()

private fun trimMap(map: LinkedHashMap<String, String>): LinkedHashMap<String, String> {
    while (map.size > MAX_TRACKED_MEDIA_ENTRIES) {
        val oldest = map.keys.firstOrNull() ?: break
        map.remove(oldest)
    }
    return map
}

/**
 * Names of the LEGACY per-URL preference keys (`pos_<url>`, `audio_<url>`,
 * `text_<url>`) that should be deleted.
 *
 * [currentKeys] are the keys the app uses today and must never be returned:
 * `audio_tracks_map` and `text_tracks_map` also start with `audio_` / `text_`,
 * so the earlier prefix-only test deleted the live JSON maps on every launch and
 * "remember track selection" never survived a restart.
 */
internal fun legacyKeysToRemove(
    allKeys: Collection<String>,
    currentKeys: Set<String>,
): List<String> = allKeys.filter { name ->
    name !in currentKeys && (
        name.startsWith("pos_") ||
            name.startsWith("audio_") ||
            name.startsWith("text_")
        )
}
