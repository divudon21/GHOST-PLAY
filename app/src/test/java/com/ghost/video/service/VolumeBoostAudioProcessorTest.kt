package com.ghost.video.service

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * JVM tests for the volume-boost audio processor. These run without a device:
 * the processor only touches ByteBuffers and the media3 common API.
 */
class VolumeBoostAudioProcessorTest {

    private val pcm16 = AudioProcessor.AudioFormat(44_100, 2, C.ENCODING_PCM_16BIT)
    private val pcmFloat = AudioProcessor.AudioFormat(44_100, 2, C.ENCODING_PCM_FLOAT)

    private fun shorts(vararg values: Int): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(values.size * 2).order(ByteOrder.nativeOrder())
        values.forEach { buffer.putShort(it.toShort()) }
        buffer.flip()
        return buffer
    }

    private fun configure(processor: VolumeBoostAudioProcessor, format: AudioProcessor.AudioFormat) {
        processor.configure(format)
        processor.flush()
    }

    @Test
    fun `unity gain copies samples unchanged`() {
        val processor = VolumeBoostAudioProcessor().apply { gain = 1f }
        configure(processor, pcm16)
        processor.queueInput(shorts(1000, -2000, 0, 500))

        val output = processor.getOutput()
        assertEquals(8, output.remaining())
        val out = output.order(ByteOrder.nativeOrder()).asShortBuffer()
        assertEquals(1000.toShort(), out.get(0))
        assertEquals((-2000).toShort(), out.get(1))
        assertEquals(0.toShort(), out.get(2))
        assertEquals(500.toShort(), out.get(3))
    }

    @Test
    fun `double gain scales samples`() {
        val processor = VolumeBoostAudioProcessor().apply { gain = 2f }
        configure(processor, pcm16)
        processor.queueInput(shorts(1000, -2000, 0, 500))

        val out = processor.getOutput().order(ByteOrder.nativeOrder()).asShortBuffer()
        assertEquals(2000.toShort(), out.get(0))
        assertEquals((-4000).toShort(), out.get(1))
        assertEquals(0.toShort(), out.get(2))
        assertEquals(1000.toShort(), out.get(3))
    }

    @Test
    fun `double gain clips instead of wrapping around`() {
        val processor = VolumeBoostAudioProcessor().apply { gain = 2f }
        configure(processor, pcm16)
        processor.queueInput(shorts(30_000, -30_000, Short.MAX_VALUE.toInt(), Short.MIN_VALUE.toInt()))

        val out = processor.getOutput().order(ByteOrder.nativeOrder()).asShortBuffer()
        assertEquals(Short.MAX_VALUE, out.get(0))       // 60000 clipped, not wrapped negative
        assertEquals(Short.MIN_VALUE, out.get(1))
        assertEquals(Short.MAX_VALUE, out.get(2))
        assertEquals(Short.MIN_VALUE, out.get(3))
    }

    @Test
    fun `output is the same size as the input for 16 bit pcm`() {
        val processor = VolumeBoostAudioProcessor().apply { gain = 1.37f }
        configure(processor, pcm16)
        val input = shorts(*IntArray(4410) { (it % 1000) - 500 })
        val inputBytes = input.remaining()
        processor.queueInput(input)
        assertEquals(inputBytes, processor.getOutput().remaining())
    }

    @Test
    fun `float samples are scaled and clipped to minus one and one`() {
        val processor = VolumeBoostAudioProcessor().apply { gain = 2f }
        configure(processor, pcmFloat)
        val buffer = ByteBuffer.allocateDirect(16).order(ByteOrder.nativeOrder())
        buffer.putFloat(0.25f).putFloat(-0.25f).putFloat(0.9f).putFloat(-0.9f)
        buffer.flip()
        processor.queueInput(buffer)

        val out = processor.getOutput().order(ByteOrder.nativeOrder()).asFloatBuffer()
        assertEquals(0.5f, out.get(0), 0.0001f)
        assertEquals(-0.5f, out.get(1), 0.0001f)
        assertEquals(1f, out.get(2), 0.0001f)   // clipped
        assertEquals(-1f, out.get(3), 0.0001f)  // clipped
    }

    @Test
    fun `unknown encoding passes through untouched`() {
        val processor = VolumeBoostAudioProcessor().apply { gain = 2f }
        configure(processor, AudioProcessor.AudioFormat(44_100, 2, C.ENCODING_PCM_24BIT))
        val buffer = ByteBuffer.allocateDirect(6).order(ByteOrder.nativeOrder())
        buffer.put(byteArrayOf(1, 2, 3, 4, 5, 6))
        buffer.flip()
        processor.queueInput(buffer)

        val out = processor.getOutput()
        assertEquals(6, out.remaining())
        assertEquals(1, out.get(0))
        assertEquals(6, out.get(5))
    }
}
