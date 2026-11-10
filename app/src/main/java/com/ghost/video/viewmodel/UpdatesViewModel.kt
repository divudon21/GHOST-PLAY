package com.ghost.video.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ghost.video.data.UpdateRelease
import com.ghost.video.data.UpdatesService
import com.ghost.video.data.UpdatesState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Drives the Updates settings screen and the automatic check that runs when the
 * app opens (same behaviour as DownList's UpdateService).
 */
class UpdatesViewModel(application: Application) : AndroidViewModel(application) {

    private val service = UpdatesService(application)

    val state: StateFlow<UpdatesState> = service.state

    init {
        viewModelScope.launch { service.initialize() }
    }

    fun check() {
        viewModelScope.launch { service.check() }
    }

    fun setOption(key: String, value: Boolean) {
        viewModelScope.launch { service.setOption(key, value) }
    }

    fun openRelease(release: UpdateRelease) {
        service.openRelease(release)
    }

    /**
     * Mirrors `_checkUpdatesOnOpen()`: only runs when auto check is enabled,
     * posts a notification at most once per build and then lets the caller
     * decide about the pop-up.
     */
    fun checkOnOpen(onChecked: (UpdateRelease?) -> Unit = {}) {
        val current = service.state.value
        if (!current.ready || !current.autoCheck || current.checking) return
        viewModelScope.launch {
            val release = service.check()
            if (release == null) return@launch
            service.notifyIfNeeded(release)
            onChecked(release)
        }
    }
}
