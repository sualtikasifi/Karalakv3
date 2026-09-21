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

    private const val RATE = 22_050

    private fun tone(freq: Double, seconds: Double, volume: Double = 0.5, decay: Double = 6.0): DoubleArray {
        val n = (RATE * seconds).toInt()
        return DoubleArray(n) { i ->
            val t = i / RATE.toDouble()
            val env = exp(-decay * t) * minOf(1.0, i / 120.0)
            // A touch of 2nd/3rd harmonic so it reads as a bell, not a beep.
            volume * env * (sin(2 * PI * freq * t) + 0.35 * sin(4 * PI * freq * t) + 0.15 * sin(6 * PI * freq * t))
        }
    }

    private fun mix(into: DoubleArray, part: DoubleArray, offsetSeconds: Double) {
        val start = (offsetSeconds * RATE).toInt()
        for (i in part.indices) if (start + i < into.size) into[start + i] += part[i]
    }

    private fun pcm(data: DoubleArray): ShortArray =
        ShortArray(data.size) { (data[it].coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort() }

    /** A heavy landing: a low sweep plus a burst of noise. */
    val land: ShortArray by lazy {
        val seconds = 0.35
        val n = (RATE * seconds).toInt()
        val rnd = Random(7)
        pcm(DoubleArray(n) { i ->
            val t = i / RATE.toDouble()
            val freq = 110.0 - 70.0 * (t / seconds)
            val env = exp(-9.0 * t)
            0.75 * env * sin(2 * PI * freq * t) + 0.25 * exp(-40.0 * t) * (rnd.nextDouble() * 2 - 1)
        })
    }

    /** Four rising notes and a held chord. */
    val fanfare: ShortArray by lazy {
        val out = DoubleArray((RATE * 1.3).toInt())
        listOf(523.25, 659.25, 783.99, 1046.5).forEachIndexed { i, f -> mix(out, tone(f, 0.28, 0.4), i * 0.11) }
        listOf(523.25, 659.25, 783.99, 1046.5).forEach { f -> mix(out, tone(f, 0.8, 0.22, decay = 3.0), 0.5) }
        pcm(out)
    }

    /** A quick shower of high chimes — coins pouring out. */
    val coins: ShortArray by lazy {
        val out = DoubleArray((RATE * 1.1).toInt())
        val rnd = Random(21)
        repeat(16) { i -> mix(out, tone(1200.0 + rnd.nextDouble() * 1400.0, 0.12, 0.3, decay = 22.0), i * 0.055) }
        pcm(out)
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
