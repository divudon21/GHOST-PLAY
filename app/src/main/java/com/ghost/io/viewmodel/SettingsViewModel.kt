package com.ghost.io.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ghost.io.data.AppColorPreference
import com.ghost.io.data.OrientationPreference
import com.ghost.io.data.SettingsRepository
import com.ghost.io.data.ThemePreference
import com.ghost.io.data.ThumbnailStrategy
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

    // Player settings
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
            initialValue = true
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

    // Player settings setters
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

    // Gesture settings
    val gestureSeekEnabled: StateFlow<Boolean> = repository.gestureSeekEnabled
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = true)

    val gestureSeekSensitivity: StateFlow<Float> = repository.gestureSeekSensitivity
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = 0.5f)

    val gestureBrightnessEnabled: StateFlow<Boolean> = repository.gestureBrightnessEnabled
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = true)

    val gestureBrightnessSensitivity: StateFlow<Float> = repository.gestureBrightnessSensitivity
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = 0.5f)

    val gestureVolumeEnabled: StateFlow<Boolean> = repository.gestureVolumeEnabled
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = true)

    val gestureVolumeSensitivity: StateFlow<Float> = repository.gestureVolumeSensitivity
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = 0.5f)

    val gestureZoomEnabled: StateFlow<Boolean> = repository.gestureZoomEnabled
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = true)

    val gesturePanEnabled: StateFlow<Boolean> = repository.gesturePanEnabled
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = false)

    val gestureDoubleTapEnabled: StateFlow<Boolean> = repository.gestureDoubleTapEnabled
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = true)

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
}
