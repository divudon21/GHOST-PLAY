package com.ghost.io.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ghost.io.data.AppColorPreference
import com.ghost.io.data.DecoderPriority
import com.ghost.io.data.OrientationPreference
import com.ghost.io.data.SettingsRepository
import com.ghost.io.data.SubtitleFont
import com.ghost.io.data.ThemePreference
import com.ghost.io.data.ThumbnailStrategy
import com.ghost.io.data.ViewLayout
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)

    val themePreference: StateFlow<ThemePreference> = repository.themePreference
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemePreference.SYSTEM
        )

    val colorPreference: StateFlow<AppColorPreference> = repository.colorPreference
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppColorPreference.PURPLE
        )

    val thumbnailStrategy: StateFlow<ThumbnailStrategy> = repository.thumbnailStrategy
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThumbnailStrategy.HYBRID
        )

    val thumbnailPositionPercent: StateFlow<Int> = repository.thumbnailPositionPercent
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 33
        )

    val viewLayout: StateFlow<ViewLayout> = repository.viewLayout
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ViewLayout.GRID
        )

    val decoderPriority: StateFlow<DecoderPriority> = repository.decoderPriority
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DecoderPriority.PREFER_DEVICE
        )

    val gestureSeekEnabled: StateFlow<Boolean> = repository.gestureSeekEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val gestureSeekSensitivity: StateFlow<Float> = repository.gestureSeekSensitivity
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.5f
        )

    val gestureBrightnessEnabled: StateFlow<Boolean> = repository.gestureBrightnessEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val gestureBrightnessSensitivity: StateFlow<Float> = repository.gestureBrightnessSensitivity
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.5f
        )

    val gestureVolumeEnabled: StateFlow<Boolean> = repository.gestureVolumeEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val gestureVolumeSensitivity: StateFlow<Float> = repository.gestureVolumeSensitivity
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.5f
        )

    val gestureZoomEnabled: StateFlow<Boolean> = repository.gestureZoomEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val gesturePanEnabled: StateFlow<Boolean> = repository.gesturePanEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val gestureDoubleTapEnabled: StateFlow<Boolean> = repository.gestureDoubleTapEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val subtitleFont: StateFlow<SubtitleFont> = repository.subtitleFont
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SubtitleFont.DEFAULT
        )

    val subtitleBold: StateFlow<Boolean> = repository.subtitleBold
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val subtitleSize: StateFlow<Int> = repository.subtitleSize
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 20
        )

    val subtitleBackground: StateFlow<Boolean> = repository.subtitleBackground
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val subtitleColor: StateFlow<Int> = repository.subtitleColor
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = android.graphics.Color.WHITE
        )

    val subtitleEmbeddedStyles: StateFlow<Boolean> = repository.subtitleEmbeddedStyles
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val orientationPreference: StateFlow<OrientationPreference> = repository.orientationPreference
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = OrientationPreference.AUTO
        )

    val resumePlayback: StateFlow<Boolean> = repository.resumePlayback
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val defaultPlaybackSpeed: StateFlow<Float> = repository.defaultPlaybackSpeed
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 1.0f
        )

    val autoplay: StateFlow<Boolean> = repository.autoplay
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val pipMode: StateFlow<Boolean> = repository.pipMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val backgroundPlay: StateFlow<Boolean> = repository.backgroundPlay
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val rememberBrightness: StateFlow<Boolean> = repository.rememberBrightness
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val rememberSelections: StateFlow<Boolean> = repository.rememberSelections
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val playerOrientation: StateFlow<OrientationPreference> = repository.playerOrientation
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = OrientationPreference.AUTO
        )

    val systemCaptionStyle: StateFlow<Boolean> = repository.systemCaptionStyle
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun setTheme(theme: ThemePreference) {
        viewModelScope.launch {
            repository.setThemePreference(theme)
        }
    }

    fun setColor(color: AppColorPreference) {
        viewModelScope.launch {
            repository.setColorPreference(color)
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

    fun setSubtitleColor(color: Int) {
        viewModelScope.launch { repository.setSubtitleColor(color) }
    }

    fun setSubtitleEmbeddedStyles(enabled: Boolean) {
        viewModelScope.launch { repository.setSubtitleEmbeddedStyles(enabled) }
    }

    fun setOrientationPreference(orientation: OrientationPreference) {
        viewModelScope.launch { repository.setOrientationPreference(orientation) }
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

    fun resetAllSettings() {
        viewModelScope.launch {
            repository.resetAllSettings()
        }
    }
}
