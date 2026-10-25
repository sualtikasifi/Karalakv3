package com.sualtikasifi.cizimhafiza.presentation.reports.studio.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Mono 16-bit PCM at [sampleRate]. */
class Pcm(val sampleRate: Int, val samples: ShortArray) {
    val durationMs: Long get() = samples.size * 1000L / sampleRate
}

/**
 * Reads the WAV files the phone's text-to-speech engine writes. Engines differ in what they put
 * around the sound (LIST chunks, odd header sizes), so the chunks are walked rather than assuming
 * the classic 44-byte header. Stereo is folded down to mono; anything but 16-bit PCM is refused.
 */
object WavReader {
    fun parse(bytes: ByteArray): Pcm {
        require(bytes.size >= 12 && String(bytes, 0, 4, Charsets.US_ASCII) == "RIFF" && String(bytes, 8, 4, Charsets.US_ASCII) == "WAVE") { "WAV değil" }
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        var pos = 12
        var channels = 1
        var rate = 0
        var bits = 16
        var format = 1
        while (pos + 8 <= bytes.size) {
            val id = String(bytes, pos, 4, Charsets.US_ASCII)
            val size = buf.getInt(pos + 4).let { if (it < 0 || pos + 8 + it > bytes.size) bytes.size - pos - 8 else it }
            val body = pos + 8
            when (id) {
                "fmt " -> {
                    format = buf.getShort(body).toInt() and 0xFFFF
                    channels = buf.getShort(body + 2).toInt().coerceAtLeast(1)
                    rate = buf.getInt(body + 4)
                    bits = buf.getShort(body + 14).toInt()
                }
                "data" -> {
                    require(rate > 0) { "WAV biçim bilgisi yok" }
                    require((format == 1 || format == 0xFFFE) && bits == 16) { "Yalnızca 16-bit PCM okunur" }
                    val frames = size / (2 * channels)
                    val out = ShortArray(frames)
                    for (f in 0 until frames) {
                        var sum = 0
                        for (c in 0 until channels) sum += buf.getShort(body + (f * channels + c) * 2)
                        out[f] = (sum / channels).toShort()
                    }
                    return Pcm(rate, out)
                }
            }
            pos = body + size + (size and 1)
        }
        error("WAV içinde ses yok")
    }
}

object PcmOps {
    /** Linear-interpolation resampling: plenty for a voice track that ends up as AAC anyway. */
    fun resample(pcm: Pcm, toRate: Int): Pcm {
        if (pcm.sampleRate == toRate || pcm.samples.isEmpty()) return Pcm(toRate, pcm.samples.copyOf())
        val ratio = pcm.sampleRate.toDouble() / toRate
        val outSize = max(1, (pcm.samples.size / ratio).toInt())
        val out = ShortArray(outSize)
        val src = pcm.samples
        for (i in 0 until outSize) {
            val x = i * ratio
            val i0 = x.toInt().coerceAtMost(src.size - 1)
            val i1 = min(i0 + 1, src.size - 1)
            val t = x - i0
            out[i] = (src[i0] * (1 - t) + src[i1] * t).toInt().toShort()
        }
        return Pcm(toRate, out)
    }

    /** [pcm] with each sample scaled by [gainAt] its time in ms from the start: music ducked under a voice, faded out. */
    fun shaped(pcm: Pcm, gainAt: (Long) -> Float): Pcm {
        val out = ShortArray(pcm.samples.size)
        for (i in out.indices) {
            val g = gainAt(i * 1000L / pcm.sampleRate)
            out[i] = (pcm.samples[i] * g).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return Pcm(pcm.sampleRate, out)
    }

    /**
     * Lays clips onto one silent track of [totalMs]: each clip at its own start time, mixed (and clipped) where two
     * overlap, scaled by its own gain.
     */
    fun mix(totalMs: Long, rate: Int, clips: List<Placed>): Pcm {
        val out = IntArray(((totalMs * rate) / 1000).toInt().coerceAtLeast(1))
        clips.forEach { c ->
            val pcm = if (c.pcm.sampleRate == rate) c.pcm else resample(c.pcm, rate)
            val start = ((c.startMs * rate) / 1000).toInt()
            for (i in pcm.samples.indices) {
                val j = start + i
                if (j < 0) continue
                if (j >= out.size) break
                out[j] += (pcm.samples[i] * c.gain).toInt()
            }
        }
        return Pcm(rate, ShortArray(out.size) { out[it].coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort() })
    }

    /**
     * How loud the voice is at each video frame, 0..1 relative to the clip's own loud parts. Drives the mouth: a
     * quiet consonant and the gap between words close it, a vowel opens it.
     */
    fun envelope(pcm: Pcm, fps: Int): FloatArray {
        val window = max(1, pcm.sampleRate / fps)
        val frames = (pcm.samples.size + window - 1) / window
        val rms = FloatArray(frames)
        for (f in 0 until frames) {
            var sum = 0.0
            val from = f * window
            val to = min(pcm.samples.size, from + window)
            for (i in from until to) {
                val v = pcm.samples[i] / 32768.0
                sum += v * v
            }
            rms[f] = sqrt(sum / max(1, to - from)).toFloat()
        }
        // normalise against a loud-but-not-peak level so one shout does not leave every other word "quiet"
        val sorted = rms.sorted()
        val ref = sorted.getOrElse((sorted.size * 0.9).toInt().coerceAtMost(sorted.size - 1)) { 0f }.coerceAtLeast(1e-4f)
        return FloatArray(frames) { (rms[it] / ref).coerceIn(0f, 1f) }
    }

    /** True when the clip holds anything above a whisper (an engine that failed quietly writes silence). */
    fun audible(pcm: Pcm): Boolean = pcm.samples.any { abs(it.toInt()) > 600 }
}

/** A clip placed on the master track. */
class Placed(val pcm: Pcm, val startMs: Long, val gain: Float = 1f)
