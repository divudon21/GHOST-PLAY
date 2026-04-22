package com.fireplay.com.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fireplay.com.service.MusicService
import com.fireplay.com.ui.screens.LocalAudio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AudioViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _currentAudio = MutableStateFlow<LocalAudio?>(null)
    val currentAudio: StateFlow<LocalAudio?> = _currentAudio.asStateFlow()
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    init {
        // Observe service state
        viewModelScope.launch {
            MusicService.getCurrentAudioFlow().collect { audio ->
                _currentAudio.value = audio
            }
        }
        viewModelScope.launch {
            MusicService.getIsPlayingFlow().collect { playing ->
                _isPlaying.value = playing
            }
        }
    }

    fun playAudio(context: Context, audio: LocalAudio) {
        MusicService.playAudio(context, audio)
    }
    
    fun togglePlayPause() {
        MusicService.togglePlayPause()
    }
    
    fun stop(context: Context) {
        MusicService.stop(context)
    }
}