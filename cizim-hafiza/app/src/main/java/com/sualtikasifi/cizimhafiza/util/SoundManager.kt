package com.sualtikasifi.cizimhafiza.util

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import com.sualtikasifi.cizimhafiza.R
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Short UI feedback sounds via [ToneGenerator] — no bundled audio assets
 * exist in this project, so tones are generated on-device rather than
 * played from .mp3/.ogg files. Respects the Settings screen's sound toggle.
 */
@Singleton
class SoundManager @Inject constructor(
    private val settingsRepository: SettingsRepository
) {

    private val toneGenerator: ToneGenerator? by lazy {
        runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 80) }.getOrNull()
    }

    /** Last-2-seconds countdown warning tick. */
    fun playCountdownTick() = play(ToneGenerator.TONE_PROP_BEEP, 100)

    fun playCorrectGuess() = play(ToneGenerator.TONE_PROP_ACK, 150)

    fun playWrongGuess() = play(ToneGenerator.TONE_CDMA_NETWORK_BUSY, 200)

    fun playGameOver() = play(ToneGenerator.TONE_PROP_PROMPT, 350)

    /** The chest slams down. */
    fun playChestLand() = playSynth(ChestSounds.land)

    /** "You won a chest!" fanfare. */
    fun playChestWin() = playSynth(ChestSounds.fanfare)

    /** Coins pouring out of an opened chest. */
    fun playCoinShower() = playSynth(ChestSounds.reward)

    /**
     * The ring-out of the chest-opening clip: the video's sound stops the instant the reward screen takes over, so
     * this soft decay (the clip's last moments with a reverb that dies away, res/raw/chest_open_tail) carries it into
     * the next screen instead of cutting it dead.
     */
    fun playChestOpenTail(context: Context) {
        if (!settingsRepository.soundEnabled.value) return
        runCatching {
            MediaPlayer.create(context.applicationContext, R.raw.chest_open_tail)?.apply {
                setOnCompletionListener { it.release() }
                setOnErrorListener { mp, _, _ -> mp.release(); true }
                start()
            }
        }
    }

    private fun playSynth(samples: ShortArray) {
        if (!settingsRepository.soundEnabled.value) return
        ChestSounds.play(samples)
    }

    private fun play(toneType: Int, durationMs: Int) {
        if (!settingsRepository.soundEnabled.value) return
        runCatching { toneGenerator?.startTone(toneType, durationMs) }
    }
}
