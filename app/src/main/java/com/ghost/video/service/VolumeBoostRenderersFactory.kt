package com.ghost.video.service

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink

/**
 * Renderers factory that installs [VolumeBoostAudioProcessor] in the audio sink.
 *
 * ExoPlayer builds its own [AudioSink] inside the renderers factory, so the only
 * supported way to add an audio processor is to override [buildAudioSink] here.
 */
@OptIn(UnstableApi::class)
class VolumeBoostRenderersFactory(
    context: Context,
    extensionRendererMode: Int,
    private val volumeBoostProcessor: VolumeBoostAudioProcessor,
) : DefaultRenderersFactory(context) {

    init {
        setExtensionRendererMode(extensionRendererMode)
    }

    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean,
    ): AudioSink = DefaultAudioSink.Builder(context)
        .setAudioProcessors(arrayOf<AudioProcessor>(volumeBoostProcessor))
        .setEnableFloatOutput(enableFloatOutput)
        .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
        .build()
}
