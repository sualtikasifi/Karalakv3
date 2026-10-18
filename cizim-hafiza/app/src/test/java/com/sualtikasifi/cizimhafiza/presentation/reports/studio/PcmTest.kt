package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import com.sualtikasifi.cizimhafiza.presentation.reports.studio.audio.Pcm
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.audio.PcmOps
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.audio.Placed
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.audio.WavReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PcmTest {

    private fun wav(rate: Int, channels: Int, frames: ShortArray, extraChunk: Boolean = false): ByteArray {
        val data = ByteBuffer.allocate(frames.size * 2).order(ByteOrder.LITTLE_ENDIAN).apply { frames.forEach { putShort(it) } }.array()
        val list = if (extraChunk) "LIST".toByteArray() + intLe(4) + "INFO".toByteArray() else ByteArray(0)
        val fmt = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN).apply {
            putShort(1); putShort(channels.toShort()); putInt(rate); putInt(rate * channels * 2); putShort((channels * 2).toShort()); putShort(16)
        }.array()
        val body = "WAVE".toByteArray() + "fmt ".toByteArray() + intLe(16) + fmt + list + "data".toByteArray() + intLe(data.size) + data
        return "RIFF".toByteArray() + intLe(body.size) + body
    }

    private fun intLe(v: Int) = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(v).array()

    @Test
    fun readsMonoWav() {
        val pcm = WavReader.parse(wav(22_050, 1, shortArrayOf(1, -2, 300)))
        assertEquals(22_050, pcm.sampleRate)
        assertEquals(listOf<Short>(1, -2, 300), pcm.samples.toList())
    }

    @Test
    fun foldsStereoAndSkipsUnknownChunks() {
        val pcm = WavReader.parse(wav(24_000, 2, shortArrayOf(100, 300, -100, -300), extraChunk = true))
        assertEquals(listOf<Short>(200, -200), pcm.samples.toList())
    }

    @Test
    fun resampleKeepsDuration() {
        val src = Pcm(22_050, ShortArray(22_050) { 1000 })
        val out = PcmOps.resample(src, 44_100)
        assertEquals(44_100, out.samples.size)
        assertEquals(1000.toShort(), out.samples[123])
    }

    @Test
    fun mixPlacesClipsAtTheirTimes() {
        val clip = Pcm(1_000, ShortArray(100) { 500 })
        val out = PcmOps.mix(1_000, 1_000, listOf(Placed(clip, 200), Placed(clip, 250)))
        assertEquals(1_000, out.samples.size)
        assertEquals(0.toShort(), out.samples[199])
        assertEquals(500.toShort(), out.samples[210])
        assertEquals(1000.toShort(), out.samples[260])
        assertEquals(0.toShort(), out.samples[400])
    }

    @Test
    fun envelopeFollowsLoudness() {
        val rate = 3_000
        val samples = ShortArray(rate) { i -> if (i < rate / 2) 0 else (if (i % 2 == 0) 12_000 else -12_000).toShort() }
        val env = PcmOps.envelope(Pcm(rate, samples), 30)
        assertEquals(30, env.size)
        assertTrue(env[2] < 0.05f)
        assertTrue(env[25] > 0.9f)
        assertTrue(PcmOps.audible(Pcm(rate, samples)))
        assertFalse(PcmOps.audible(Pcm(rate, ShortArray(10))))
    }

    @Test
    fun karaokeStopsAtWordEnds() {
        val text = "Bir daire güneş mi"
        assertEquals(0, TextPainter.karaokeSplit(text, 0f))
        assertEquals(text.length, TextPainter.karaokeSplit(text, 1f))
        assertEquals(text.indexOf(' ', 5), TextPainter.karaokeSplit(text, 0.3f))
    }
}
