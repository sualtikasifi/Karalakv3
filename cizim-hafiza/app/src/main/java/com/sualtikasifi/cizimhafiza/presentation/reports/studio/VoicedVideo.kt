package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import android.graphics.Canvas

/**
 * What every studio format hands the renderer pipeline: how long the video runs, when each
 * spoken line starts (by its index in the format's own line list, so its recorded voice can be
 * laid on the track there), and how to draw any moment of it.
 */
interface VoicedVideo {
    val totalMs: Long
    val voiceStarts: List<Pair<Int, Long>>
    fun draw(canvas: Canvas, t: Long)
}
