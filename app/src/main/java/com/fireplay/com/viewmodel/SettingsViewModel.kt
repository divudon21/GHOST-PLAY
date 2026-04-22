package com.fireplay.com.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fireplay.com.data.AppColorPreference
import com.fireplay.com.data.SettingsRepository
import com.fireplay.com.data.ThemePreference
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
}