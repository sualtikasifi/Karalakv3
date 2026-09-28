package com.sualtikasifi.cizimhafiza.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * The chest celebration sounds, synthesised in code (no audio files to
 * bundle): a landing thud, a rising fanfare, and a shower of coin chimes.
 * Each is rendered once, lazily, and played through a short static
 * [AudioTrack].
 */
internal object ChestSounds {

    // Was 22_050 — audibly gritty/lo-fi on a modern phone speaker. Doubling
    // the rate is the single cheapest fix for "sounds amateur": it moves the
    // aliasing noise floor well above what these speakers reproduce anyway.
    private const val RATE = 44_100

    /** A one-pole low-pass, used to turn hard random noise into a soft thud/rumble instead of static hiss. */
    private fun lowPass(noise: DoubleArray, cutoffHz: Double): DoubleArray {
        val alpha = (2.0 * PI * cutoffHz / RATE).coerceIn(0.0, 1.0)
        var prev = 0.0
        return DoubleArray(noise.size) { i ->
            prev += alpha * (noise[i] - prev)
            prev
        }
    }

    private fun tone(freq: Double, seconds: Double, volume: Double = 0.5, decay: Double = 6.0, vibratoHz: Double = 0.0): DoubleArray {
        val n = (RATE * seconds).toInt()
        return DoubleArray(n) { i ->
            val t = i / RATE.toDouble()
            val env = exp(-decay * t) * minOf(1.0, i / 200.0)
            val f = freq * (1.0 + 0.006 * sin(2 * PI * vibratoHz * t))
            // A touch of 2nd/3rd/4th harmonic (with a slightly detuned upper
            // partial) so it reads as a struck bell rather than a flat beep.
            volume * env * (
                sin(2 * PI * f * t) +
                    0.35 * sin(4 * PI * f * t) +
                    0.18 * sin(6.03 * PI * f * t) +
                    0.08 * sin(8 * PI * f * t)
                )
        }
    }

    private fun mix(into: DoubleArray, part: DoubleArray, offsetSeconds: Double) {
        val start = (offsetSeconds * RATE).toInt()
        for (i in part.indices) if (start + i < into.size) into[start + i] += part[i]
    }

    private fun pcm(data: DoubleArray): ShortArray =
        ShortArray(data.size) { (data[it].coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort() }

    /**
     * A heavy landing: a deep pitch-dropping sweep for weight, a soft
     * low-passed thump for body, and a short filtered "creak" of the lid
     * hinge — the noise burst used to be full-bandwidth static, which is
     * exactly what read as cheap; band-limiting it is most of the fix.
     */
    val land: ShortArray by lazy {
        val seconds = 0.42
        val n = (RATE * seconds).toInt()
        val rnd = Random(7)
        val thump = lowPass(DoubleArray(n) { rnd.nextDouble() * 2 - 1 }, 180.0)
        val creak = lowPass(DoubleArray(n) { rnd.nextDouble() * 2 - 1 }, 900.0)
        pcm(DoubleArray(n) { i ->
            val t = i / RATE.toDouble()
            val freq = 95.0 - 55.0 * (t / seconds)
            val body = exp(-8.5 * t) * sin(2 * PI * freq * t)
            val hit = exp(-30.0 * t) * thump[i]
            val hinge = exp(-16.0 * t) * exp(-((t - 0.05) * (t - 0.05)) / 0.0006) * creak[i]
            0.8 * body + 0.45 * hit + 0.5 * hinge
        })
    }

    /** Four rising notes with a light vibrato and a held chord — a small bell choir rather than four flat beeps. */
    val fanfare: ShortArray by lazy {
        val out = DoubleArray((RATE * 1.4).toInt())
        val notes = listOf(523.25, 659.25, 783.99, 1046.5)
        notes.forEachIndexed { i, f -> mix(out, tone(f, 0.3, 0.42, vibratoHz = 5.0), i * 0.11) }
        notes.forEach { f -> mix(out, tone(f, 0.9, 0.24, decay = 2.6, vibratoHz = 4.0), 0.5) }
        // A soft octave-up sparkle under the held chord — what makes a
        // fanfare feel bright rather than just loud.
        notes.forEach { f -> mix(out, tone(f * 2.0, 0.7, 0.08, decay = 4.0), 0.52) }
        pcm(out)
    }

    /**
     * A shower of high chimes — coins pouring out, each one a genuinely
     * pitched bell instead of a single tone repeated: the pitch drifts
     * downward across the shower and every third chime rings an octave
     * partner, which is what separates "coins" from "sixteen identical
     * blips".
     */
    val coins: ShortArray by lazy {
        val out = DoubleArray((RATE * 1.2).toInt())
        val rnd = Random(21)
        repeat(18) { i ->
            val t0 = i * 0.05 + rnd.nextDouble() * 0.012
            val base = 1500.0 - i * 24.0 + rnd.nextDouble() * 500.0
            mix(out, tone(base, 0.16, 0.28, decay = 18.0, vibratoHz = 30.0), t0)
            if (i % 3 == 0) mix(out, tone(base * 2.0, 0.1, 0.12, decay = 26.0), t0 + 0.01)
        }
        pcm(out)
    }


    /**
     * The chest reward chime: a soft rising pentatonic arpeggio in pure,
     * warm bell tones with a gentle echo — pleasant instead of the harsh
     * coin-shower pile-up it replaces.
     */
    val reward: ShortArray by lazy {
        val out = DoubleArray((RATE * 1.9).toInt())
        fun bell(freq: Double, seconds: Double, volume: Double, decay: Double): DoubleArray {
            val n = (RATE * seconds).toInt()
            return DoubleArray(n) { i ->
                val t = i / RATE.toDouble()
                val env = exp(-decay * t) * minOf(1.0, t / 0.012)
                volume * env * (sin(2 * PI * freq * t) + 0.16 * sin(4 * PI * freq * t) + 0.05 * sin(6 * PI * freq * t))
            }
        }
        val arpeggio = listOf(523.25 to 0.0, 659.25 to 0.11, 783.99 to 0.22, 1046.5 to 0.34)
        arpeggio.forEach { (f, at) -> mix(out, bell(f, 1.1, 0.30, 3.6), at) }
        listOf(523.25, 783.99, 1318.5).forEach { f -> mix(out, bell(f, 1.4, 0.13, 2.4), 0.46) }
        val echoed = out.copyOf()
        val delay = (RATE * 0.21).toInt()
        for (i in delay until out.size) echoed[i] += out[i - delay] * 0.28
        for (i in 2 * delay until out.size) echoed[i] += out[i - 2 * delay] * 0.12
        val fade = (RATE * 0.25).toInt()
        for (i in 0 until fade) echoed[echoed.size - 1 - i] *= i / fade.toDouble()
        pcm(echoed.map { it * 0.85 }.toDoubleArray())
    }

    fun play(samples: ShortArray) {
        runCatching {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(samples.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(samples, 0, samples.size)
            track.setNotificationMarkerPosition(samples.size)
            track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(t: AudioTrack) = t.release()
                override fun onPeriodicNotification(t: AudioTrack) = Unit
            })
            track.play()
        }
    }
}
