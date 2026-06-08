package com.ghost.io

import androidx.media3.exoplayer.ExoPlayer

object SharedPlayer {
    var player: ExoPlayer? = null
    var isFloatingMode = false
}
