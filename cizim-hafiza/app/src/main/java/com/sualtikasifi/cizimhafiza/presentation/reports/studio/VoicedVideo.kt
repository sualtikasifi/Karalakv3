package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import android.graphics.Canvas
import java.io.File

/**
 * Sound from a file, laid on the track at [startMs] for [durationMs]: an imported clip's own audio, a line voiced
 * beforehand and kept with the project (the "Gerçek Olsaydı" hook), or background music — not the lines a format
 * voices while rendering. [shape], when given, scales [gain] at each moment (in ms from the sound's own start); a
 * [voice] has its very end faded so it never stops dead.
 */
class ClipSound(
    val file: File, val startMs: Long, val durationMs: Long, val gain: Float,
    val shape: ((Long) -> Float)? = null,
    val voice: Boolean = false
)

/**
 * What every studio format hands the renderer pipeline: how long the video runs, when each
 * spoken line starts (by its index in the format's own line list, so its recorded voice can be
 * laid on the track there), any other sound it carries, and how to draw any moment of it.
 */
interface VoicedVideo {
    val totalMs: Long
    val voiceStarts: List<Pair<Int, Long>>
    val sounds: List<ClipSound> get() = emptyList()
    /** The picture's size in pixels: the encoder makes the video this big (or the same shape, smaller, when the phone's encoder cannot). */
    val width: Int get() = 1080
    val height: Int get() = 1920
    fun draw(canvas: Canvas, t: Long)
}
