package com.ghost.video

import androidx.media3.exoplayer.ExoPlayer

object SharedPlayer {
    var player: ExoPlayer? = null
    var isFloatingMode = false

    /** True while "Auto Picture in Picture Mode" is on (set by the player screen). */
    var autoPipEnabled = false
}
