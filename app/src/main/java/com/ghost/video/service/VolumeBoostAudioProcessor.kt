package com.ghost.video.service

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer

/**
 * Applies the "Volume Boost 200%" setting.
 *
 * ExoPlayer clamps its own volume to 0..1, so asking it for `2f` silently did
 * nothing. Boost above 100% has to happen inside the audio pipeline: this
 * processor scales every PCM sample by [gain] (clipped to the sample range) and
 * is a plain copy when the gain is 1.
 */
@OptIn(UnstableApi::class)
class VolumeBoostAudioProcessor : BaseAudioProcessor() {

    /** Linear gain applied to every sample. 1f = untouched, 2f = +200%. */
    @Volatile
    var gain: Float = 1f

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        // Sample format is untouched, so the output format equals the input format.
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val currentGain = gain
        val remaining = inputBuffer.remaining()
        val outputBuffer = replaceOutputBuffer(remaining)
        val encoding = inputAudioFormat.pcmEncoding

        when (encoding) {
            C.ENCODING_PCM_16BIT -> {
                while (inputBuffer.remaining() >= 2) {
                    val sample = inputBuffer.getShort().toInt()
                    val boosted = (sample * currentGain).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    outputBuffer.putShort(boosted.toShort())
                }
            }

            C.ENCODING_PCM_FLOAT -> {
                while (inputBuffer.remaining() >= 4) {
                    val boosted = (inputBuffer.getFloat() * currentGain).coerceIn(-1f, 1f)
                    outputBuffer.putFloat(boosted)
                }
            }

            // 8/24/32-bit or unknown layouts: pass through untouched rather than corrupt them.
            else -> outputBuffer.put(inputBuffer)
        }

        // Odd trailing bytes (never a whole sample) are passed through as-is.
        outputBuffer.put(inputBuffer)
        outputBuffer.flip()
    }
}
