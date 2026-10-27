package com.ghost.video

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.media3.exoplayer.ExoPlayer

object SharedPlayer {
    var player: ExoPlayer? = null

    /** True while "Auto Picture in Picture Mode" is on (set by the player screen). */
    var autoPipEnabled = false

    /** Application context for helpers outside a composable (e.g. reading system brightness). */
    var appContext: Context? = null

    /**
     * Mirrors [android.app.Activity.onPictureInPictureModeChanged] so the player
     * screen can keep its Compose overlays out of the Picture-in-Picture window.
     */
    val isInPictureInPicture = mutableStateOf(false)
}
