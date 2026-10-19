package com.sualtikasifi.cizimhafiza.presentation.reports.studio.real

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.ClipSound
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.Ease
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StrokeReplay
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioAssets
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioColors as C
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.TextPainter
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.VoicedVideo
import java.io.File
import kotlin.math.min

/**
 * Draws "Gerçek Olsaydı" in 1080×1920: the clips one after the other, each full frame with its own sound, and
 * over each the player's drawing on a sheet of paper in the top left, with who drew it and what it was.
 * Then a short closing card that sends people to the app in Google Play. Nothing else: no voice, no
 * subtitles. The right edge and the bottom band stay clear for the buttons and caption TikTok, Reels and
 * Shorts put there.
 */
class RealRenderer(
    private val assets: StudioAssets,
    private val project: RealProject,
    /** Clips by drawing id. */
    private val clips: Map<String, ClipFrames>,
    private val clipFiles: Map<String, File>,
    /** How loud the clips' own sound is. */
    private val clipGain: Float
) : VoicedVideo {
    private val shown = project.shown
    val events: List<RealEvent> = RealTimeline.build(shown)
    override val totalMs: Long = RealTimeline.totalMs(events)
    override val voiceStarts: List<Pair<Int, Long>> = emptyList()
    override val sounds: List<ClipSound> = if (clipGain <= 0f) emptyList() else events.filter { it.phase == RealPhase.Clip }.mapNotNull { e ->
        clipFiles[shown.getOrNull(e.index)?.sampleId]?.let { ClipSound(it, e.clipAtMs, e.clipMs, clipGain) }
    }

    private val text = TextPainter()
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val ink = StrokeReplay.inkPaint(0xFF1B1B22.toInt())
    private val replays = HashMap<String, StrokeReplay>()
    private val src = Rect()

    // where things sit: the left column only
    private val sheet = RectF(48f, 210f, 408f, 210f + 440f)
    private val infoBox = RectF(48f, 690f, 700f, 690f + 150f)
    private val labelBox = RectF(48f, 862f, 48f + 470f, 862f + 52f)

    private fun replay(id: String): StrokeReplay? = project.drawing(id)?.let { d -> replays.getOrPut(id) { StrokeReplay(d.strokes) } }

    override fun draw(canvas: Canvas, t: Long) {
        val e = RealTimeline.at(events, t)
        if (e.phase == RealPhase.End) drawEndCard(canvas, e, t) else drawClip(canvas, e, t)
    }

    // ---- a clip with its drawing

    private fun drawClip(canvas: Canvas, e: RealEvent, t: Long) {
        val reveal = shown.getOrNull(e.index) ?: return
        canvas.drawColor(C.INK)
        val frame = clips[reveal.sampleId]?.frameAt((t - e.clipAtMs).coerceIn(0L, e.clipMs))
        if (frame != null) cover(canvas, frame, RectF(0f, 0f, W.toFloat(), H.toFloat()))
        val drawing = project.drawing(reveal.sampleId) ?: return
        val a = Ease.backOut(min(1f, (t - e.startMs) / 450f))
        drawSheet(canvas, drawing, reveal, a)
        drawInfo(canvas, drawing, Ease.out(min(1f, (t - e.startMs - 150L) / 400f)))
        drawLabel(canvas, Ease.out(min(1f, (t - e.startMs - 300L) / 400f)))
    }

    /** The player's drawing on a sheet of paper, a little crooked, as if pinned to the scene. */
    private fun drawSheet(canvas: Canvas, d: RealDrawing, r: RealReveal, a: Float) {
        canvas.save()
        canvas.rotate(-3f, sheet.centerX(), sheet.centerY())
        canvas.scale(a, a, sheet.left, sheet.top)
        fill.color = 0x55000000
        canvas.drawRoundRect(RectF(sheet.left + 8f, sheet.top + 14f, sheet.right + 8f, sheet.bottom + 14f), 14f, 14f, fill)
        fill.color = C.WHITE
        canvas.drawRoundRect(sheet, 14f, 14f, fill)
        replay(r.sampleId)?.draw(canvas, RectF(sheet.left + 14f, sheet.top + 14f, sheet.right - 14f, sheet.bottom - 14f), 1f, ink, 0.028f)
        canvas.restore()
    }

    /** Who drew it and what it was: `@name · Sv. 12` over `"Roket" · 7 saniyede çizdi`. */
    private fun drawInfo(canvas: Canvas, d: RealDrawing, a: Float) {
        if (a <= 0f) return
        val slide = (1f - a) * -60f
        canvas.save()
        canvas.translate(slide, 0f)
        fill.color = ((0x99 * a).toInt() shl 24) or 0x101014
        canvas.drawRoundRect(infoBox, 28f, 28f, fill)
        val who = buildString {
            append("@").append(d.nickname.ifBlank { "oyuncu" })
            if (d.level > 0) append(" · Sv. ").append(d.level)
        }
        val what = buildString {
            if (d.word.isNotBlank()) append("“").append(d.word).append("”")
            if (d.seconds > 0) { if (isNotEmpty()) append(" · "); append(d.seconds).append(" saniyede çizdi") }
        }
        val width = (infoBox.width() - 56).toInt()
        text.draw(canvas, who, infoBox.centerX(), infoBox.top + 52f, width, 50f, 1, assets.display, C.WHITE, alpha = a)
        text.draw(canvas, what, infoBox.centerX(), infoBox.top + 108f, width, 38f, 1, assets.body, 0xFFFFE2BF.toInt(), alpha = a)
        canvas.restore()
    }

    /** The "made with AI" note platforms ask for, small, under the info. */
    private fun drawLabel(canvas: Canvas, a: Float) {
        if (a <= 0f) return
        fill.color = ((0x80 * a).toInt() shl 24) or 0x101014
        canvas.drawRoundRect(labelBox, 26f, 26f, fill)
        text.draw(canvas, "Yapay zekâ ile canlandırıldı", labelBox.centerX(), labelBox.centerY(), (labelBox.width() - 30).toInt(), 28f, 1, assets.body, C.WHITE, alpha = a)
    }

    // ---- the closing card

    private fun drawEndCard(canvas: Canvas, e: RealEvent, t: Long) {
        canvas.drawColor(C.CREAM)
        val a = Ease.out(min(1f, (t - e.startMs) / 450f))
        assets.logo?.let { logo ->
            val size = 430f
            val cx = W / 2f
            val cy = 620f + (1f - a) * 40f
            fill.alpha = (a * 255).toInt()
            canvas.drawBitmap(logo, null, RectF(cx - size / 2f, cy - size / 2f, cx + size / 2f, cy + size / 2f), fill)
            fill.alpha = 255
        }
        text.draw(canvas, "Sen de çiz!", W / 2f, 1010f, 940, 124f, 1, assets.display, C.ORANGE_DARK, alpha = a)
        // the store line, as a button
        val button = RectF(110f, 1150f, 970f, 1310f)
        fill.color = C.ORANGE
        fill.alpha = (a * 255).toInt()
        canvas.drawRoundRect(button, 80f, 80f, fill)
        fill.alpha = 255
        text.draw(canvas, "Google Play'de Karalak", button.centerX(), button.centerY(), 780, 66f, 1, assets.display, C.WHITE, alpha = a)
        text.draw(canvas, "Ücretsiz · Android", W / 2f, 1385f, 800, 40f, 1, assets.body, C.MUTED, alpha = a)
    }

    /** Fills [dst] with [bmp], cropped to its shape. */
    private fun cover(canvas: Canvas, bmp: Bitmap, dst: RectF) {
        if (bmp.isRecycled) return
        val aspect = dst.width() / dst.height()
        var sw = bmp.width.toFloat()
        var sh = sw / aspect
        if (sh > bmp.height) {
            sh = bmp.height.toFloat()
            sw = sh * aspect
        }
        val sx = (bmp.width - sw) / 2f
        val sy = (bmp.height - sh) / 2f
        src.set(sx.toInt(), sy.toInt(), (sx + sw).toInt(), (sy + sh).toInt())
        canvas.drawBitmap(bmp, src, dst, fill)
    }

    companion object {
        const val W = 1080
        const val H = 1920
        const val FPS = 30
    }
}
