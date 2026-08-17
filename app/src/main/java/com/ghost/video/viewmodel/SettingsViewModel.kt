package com.ghost.video.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ghost.video.data.AppPalette
import com.ghost.video.data.AppTextStyle
import com.ghost.video.data.DecoderPriority
import com.ghost.video.data.OrientationPreference
import com.ghost.video.data.SettingsRepository
import com.ghost.video.data.SubtitleFont
import com.ghost.video.data.ThemePreference
import com.ghost.video.data.ThumbnailStrategy
import com.ghost.video.data.ThemeSettings
import com.ghost.video.data.DialogThemePreference
import com.ghost.video.data.LoadingIndicatorStyle
import com.ghost.video.data.ViewLayout
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)

    // Null only during the initial disk read. MainActivity holds its first draw
    // until this atomic appearance snapshot is available.
    val startupThemeSettings: StateFlow<ThemeSettings?> = repository.themeSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    val themePreference: StateFlow<ThemePreference> = repository.themePreference
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ThemePreference.SYSTEM
        )

    val appPalette: StateFlow<AppPalette> = repository.appPalette
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppPalette.MONOCHROME
        )

    val appTextStyle: StateFlow<AppTextStyle> = repository.appTextStyle
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppTextStyle.DEFAULT
        )

    val boldText: StateFlow<Boolean> = repository.boldText
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    val thumbnailStrategy: StateFlow<ThumbnailStrategy> = repository.thumbnailStrategy
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ThumbnailStrategy.HYBRID
        )

    val thumbnailPositionPercent: StateFlow<Int> = repository.thumbnailPositionPercent
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = 33
        )

    val viewLayout: StateFlow<ViewLayout> = repository.viewLayout
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ViewLayout.GRID
        )

    val decoderPriority: StateFlow<DecoderPriority> = repository.decoderPriority
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = DecoderPriority.PREFER_DEVICE
        )

    val gestureSeekEnabled: StateFlow<Boolean> = repository.gestureSeekEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = true
        )

    val gestureSeekSensitivity: StateFlow<Float> = repository.gestureSeekSensitivity
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = 0.5f
        )

    val gestureBrightnessEnabled: StateFlow<Boolean> = repository.gestureBrightnessEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = true
        )

    val gestureBrightnessSensitivity: StateFlow<Float> = repository.gestureBrightnessSensitivity
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = 0.5f
        )

    val gestureVolumeEnabled: StateFlow<Boolean> = repository.gestureVolumeEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = true
        )

    val gestureVolumeSensitivity: StateFlow<Float> = repository.gestureVolumeSensitivity
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = 0.5f
        )

    val gestureZoomEnabled: StateFlow<Boolean> = repository.gestureZoomEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = true
        )

    val gesturePanEnabled: StateFlow<Boolean> = repository.gesturePanEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    val gestureDoubleTapEnabled: StateFlow<Boolean> = repository.gestureDoubleTapEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = true
        )

    val subtitleFont: StateFlow<SubtitleFont> = repository.subtitleFont
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = SubtitleFont.DEFAULT
        )

    val subtitleBold: StateFlow<Boolean> = repository.subtitleBold
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = true
        )

    val subtitleSize: StateFlow<Int> = repository.subtitleSize
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = 20
        )

    val subtitleBackground: StateFlow<Boolean> = repository.subtitleBackground
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    val subtitleEmbeddedStyles: StateFlow<Boolean> = repository.subtitleEmbeddedStyles
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = true
        )

    val resumePlayback: StateFlow<Boolean> = repository.resumePlayback
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = true
        )

    val defaultPlaybackSpeed: StateFlow<Float> = repository.defaultPlaybackSpeed
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = 1.0f
        )

    val autoplay: StateFlow<Boolean> = repository.autoplay
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = true
        )

    val pipMode: StateFlow<Boolean> = repository.pipMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = true
        )

    val autoPipMode: StateFlow<Boolean> = repository.autoPipMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    val backgroundPlay: StateFlow<Boolean> = repository.backgroundPlay
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    val rememberBrightness: StateFlow<Boolean> = repository.rememberBrightness
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = true
        )

    val rememberSelections: StateFlow<Boolean> = repository.rememberSelections
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = true
        )

    val playerOrientation: StateFlow<OrientationPreference> = repository.playerOrientation
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = OrientationPreference.AUTO
        )

    val systemCaptionStyle: StateFlow<Boolean> = repository.systemCaptionStyle
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    val dialogThemePreference: StateFlow<DialogThemePreference> = repository.dialogThemePreference
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = DialogThemePreference.FOLLOW_SYSTEM
        )

    val volumeBoostEnabled: StateFlow<Boolean> = repository.volumeBoostEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    val highContrastDark: StateFlow<Boolean> = repository.highContrastDark
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    val glowEffect: StateFlow<Boolean> = repository.glowEffect
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    val batterySaver: StateFlow<Boolean> = repository.batterySaver
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    val updateNotifications: StateFlow<Boolean> = repository.updateNotifications
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    val loadingIndicatorStyle: StateFlow<LoadingIndicatorStyle> = repository.loadingIndicatorStyle
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = LoadingIndicatorStyle.ROUNDED_POLYGON
        )

    fun setTheme(theme: ThemePreference) {
        viewModelScope.launch {
            repository.setThemePreference(theme)
        }
    }

    fun setThumbnailStrategy(strategy: ThumbnailStrategy) {
        viewModelScope.launch {
            repository.setThumbnailStrategy(strategy)
        }
    }

    fun setThumbnailPositionPercent(percent: Int) {
        viewModelScope.launch {
            repository.setThumbnailPositionPercent(percent)
        }
    }

    fun setViewLayout(layout: ViewLayout) {
        viewModelScope.launch {
            repository.setViewLayout(layout)
        }
    }

    fun setDecoderPriority(priority: DecoderPriority) {
        viewModelScope.launch {
            repository.setDecoderPriority(priority)
        }
    }

    fun setGestureSeekEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setGestureSeekEnabled(enabled) }
    }

    fun setGestureSeekSensitivity(sensitivity: Float) {
        viewModelScope.launch { repository.setGestureSeekSensitivity(sensitivity) }
    }

    fun setGestureBrightnessEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setGestureBrightnessEnabled(enabled) }
    }

    fun setGestureBrightnessSensitivity(sensitivity: Float) {
        viewModelScope.launch { repository.setGestureBrightnessSensitivity(sensitivity) }
    }

    fun setGestureVolumeEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setGestureVolumeEnabled(enabled) }
    }

    fun setGestureVolumeSensitivity(sensitivity: Float) {
        viewModelScope.launch { repository.setGestureVolumeSensitivity(sensitivity) }
    }

    fun setGestureZoomEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setGestureZoomEnabled(enabled) }
    }

    fun setGesturePanEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setGesturePanEnabled(enabled) }
    }

    fun setGestureDoubleTapEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setGestureDoubleTapEnabled(enabled) }
    }

    fun setSubtitleFont(font: SubtitleFont) {
        viewModelScope.launch { repository.setSubtitleFont(font) }
    }

    fun setSubtitleBold(bold: Boolean) {
        viewModelScope.launch { repository.setSubtitleBold(bold) }
    }

    fun setSubtitleSize(size: Int) {
        viewModelScope.launch { repository.setSubtitleSize(size) }
    }

    fun setSubtitleBackground(background: Boolean) {
        viewModelScope.launch { repository.setSubtitleBackground(background) }
    }

    fun setSubtitleEmbeddedStyles(enabled: Boolean) {
        viewModelScope.launch { repository.setSubtitleEmbeddedStyles(enabled) }
    }

    fun setResumePlayback(enabled: Boolean) {
        viewModelScope.launch { repository.setResumePlayback(enabled) }
    }

    fun setDefaultPlaybackSpeed(speed: Float) {
        viewModelScope.launch { repository.setDefaultPlaybackSpeed(speed) }
    }

    fun setAutoplay(enabled: Boolean) {
        viewModelScope.launch { repository.setAutoplay(enabled) }
    }

    fun setPipMode(enabled: Boolean) {
        viewModelScope.launch { repository.setPipMode(enabled) }
    }

    fun setAutoPipMode(enabled: Boolean) {
        viewModelScope.launch { repository.setAutoPipMode(enabled) }
    }

    fun setBackgroundPlay(enabled: Boolean) {
        viewModelScope.launch { repository.setBackgroundPlay(enabled) }
    }

    fun setRememberBrightness(enabled: Boolean) {
        viewModelScope.launch { repository.setRememberBrightness(enabled) }
    }

    fun setRememberSelections(enabled: Boolean) {
        viewModelScope.launch { repository.setRememberSelections(enabled) }
    }

    fun setPlayerOrientation(orientation: OrientationPreference) {
        viewModelScope.launch { repository.setPlayerOrientation(orientation) }
    }

    fun setSystemCaptionStyle(enabled: Boolean) {
        viewModelScope.launch { repository.setSystemCaptionStyle(enabled) }
    }

    fun setDialogTheme(theme: DialogThemePreference) {
        viewModelScope.launch { repository.setDialogTheme(theme) }
    }

    fun setVolumeBoostEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setVolumeBoostEnabled(enabled) }
    }

    fun setHighContrastDark(enabled: Boolean) {
        viewModelScope.launch { repository.setHighContrastDark(enabled) }
    }

    fun setGlowEffect(enabled: Boolean) {
        viewModelScope.launch { repository.setGlowEffect(enabled) }
    }

    fun setBatterySaver(enabled: Boolean) {
        viewModelScope.launch { repository.setBatterySaver(enabled) }
    }

    fun setUpdateNotifications(enabled: Boolean) {
        viewModelScope.launch { repository.setUpdateNotifications(enabled) }
    }

    fun setLastSeenRelease(tag: String) {
        viewModelScope.launch { repository.setLastSeenRelease(tag) }
    }

    fun setLoadingIndicatorStyle(style: LoadingIndicatorStyle) {
        viewModelScope.launch { repository.setLoadingIndicatorStyle(style) }
    }

    fun setAppPalette(palette: AppPalette) {
        viewModelScope.launch { repository.setAppPalette(palette) }
    }

    fun setAppTextStyle(style: AppTextStyle) {
        viewModelScope.launch { repository.setAppTextStyle(style) }
    }

    fun setBoldText(enabled: Boolean) {
        viewModelScope.launch { repository.setBoldText(enabled) }
    }

    fun resetAllSettings() {
        viewModelScope.launch {
            repository.resetAllSettings()
        }
    }
}
