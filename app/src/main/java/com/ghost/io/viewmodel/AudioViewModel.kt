package com.ghost.io.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ghost.io.service.MusicService
import com.ghost.io.ui.screens.LocalAudio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AudioViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _currentAudio = MutableStateFlow<LocalAudio?>(null)
    val currentAudio: StateFlow<LocalAudio?> = _currentAudio.asStateFlow()
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    // Store the full playlist so we can pass it to the service
    private var currentPlaylist: List<LocalAudio> = emptyList()

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

    fun setPlaylist(playlist: List<LocalAudio>) {
        currentPlaylist = playlist
    }

    fun playAudio(context: Context, audio: LocalAudio) {
        MusicService.playAudio(context, audio, currentPlaylist)
    }
    
    fun togglePlayPause() {
        MusicService.togglePlayPause()
    }
    
    fun skipToNext() {
        MusicService.skipToNext()
    }
    
    fun skipToPrevious() {
        MusicService.skipToPrevious()
    }
    
    fun stop(context: Context) {
        MusicService.stop(context)
    }
}
