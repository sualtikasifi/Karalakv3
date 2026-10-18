package com.sualtikasifi.cizimhafiza.presentation.reports.studio.memory

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.Ease
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StrokeReplay
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioAssets
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioColors as C
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.TextPainter
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.VoicedVideo
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.guess.GuessTimeline
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MascotEmotion
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MascotSprites
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MouthState
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.mouthFor
import java.util.Locale
import kotlin.math.min
import kotlin.math.sin

/**
 * Draws "Hafıza Tuzağı" in 1080×1920: Karalak and the headline numbers at the top, its words as
 * karaoke subtitles, then the drawings people could not recognise, the twist (drawing is meant
 * to help memory — but not in a few seconds), and the challenge to try it.
 */
class MemoryRenderer(
    private val assets: StudioAssets,
    private val sprites: MascotSprites,
    private val script: MemoryScript,
    private val samples: Map<String, MemorySample>,
    /** Extra drawings for the opening mosaic (forgotten ones of the same word). */
    private val mosaic: List<MemorySample>,
    private val voice: Map<Int, FloatArray>?
) : VoicedVideo {
    val events: List<MemoryEvent> = MemoryTimeline.build(script) { i, l ->
        voice?.get(i)?.let { it.size * 1000L / FPS } ?: GuessTimeline.estimateMs(l.text)
    }
    override val totalMs: Long = MemoryTimeline.totalMs(events)
    override val voiceStarts: List<Pair<Int, Long>> = events.filter { it.line != null }.map { it.lineIndex to it.startMs }

    private val text = TextPainter()
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val ink = StrokeReplay.inkPaint()
    private val replays = HashMap<String, StrokeReplay>()

    private val mascotTile = RectF(40f, 150f, 400f, 510f)
    private val statBox = RectF(430f, 150f, 1040f, 510f)
    private val card = RectF(130f, 720f, 950f, 1400f)

    private fun replay(s: MemorySample) = replays.getOrPut(s.id) { StrokeReplay(s.strokes) }

    override fun draw(canvas: Canvas, t: Long) {
        canvas.drawColor(C.CREAM)
        val e = MemoryTimeline.at(events, t)
        val spoken = e.line?.let { l -> voice?.get(e.lineIndex)?.let { it.size * 1000L / FPS } ?: GuessTimeline.estimateMs(l.text) } ?: 0L
        val speaking = e.line != null && t - e.startMs < spoken
        val level = level(e, t, speaking)

        drawMascotTile(canvas, e, speaking, level, t)
        drawStats(canvas, e, t)
        e.line?.let { l ->
            if (e.phase != MemoryPhase.Outro) {
                val p = ((t - e.startMs).toFloat() / spoken.coerceAtLeast(1)).coerceIn(0f, 1f)
                text.karaoke(canvas, l.text, p, 540f, 615f, 980, 50f, 2, assets.display, C.INK, C.FAINT)
            }
        }
        when (e.phase) {
            MemoryPhase.Hook -> drawMosaic(canvas, e, t)
            MemoryPhase.Gallery -> drawPick(canvas, e, t)
            MemoryPhase.Paradox -> drawParadox(canvas, e, t)
            MemoryPhase.Outro -> Unit
        }
        if (e.phase != MemoryPhase.Outro) {
            text.draw(canvas, "karalak.com · Google Play: Karalak", 540f, 1640f, 900, 32f, 1, assets.body, C.MUTED)
            fill.color = C.CARD_EDGE
            canvas.drawRect(0f, H - 10f, W.toFloat(), H.toFloat(), fill)
            fill.color = C.ORANGE
            canvas.drawRect(0f, H - 10f, W * (t.toFloat() / totalMs).coerceIn(0f, 1f), H.toFloat(), fill)
        } else {
            drawEndCard(canvas, e, t, speaking, level)
        }
    }

    private fun level(e: MemoryEvent, t: Long, speaking: Boolean): Float {
        if (!speaking) return 0f
        val local = t - e.startMs
        val env = voice?.get(e.lineIndex)
        if (env != null) {
            val i = (local * FPS / 1000).toInt()
            return if (i in env.indices) env[i] else 0f
        }
        return (0.55f + 0.45f * sin(local / 55.0).toFloat()).coerceIn(0f, 1f) * if ((local / 260) % 4 == 3L) 0.2f else 1f
    }

    // ---- top: Karalak and the numbers

    private fun drawMascotTile(canvas: Canvas, e: MemoryEvent, speaking: Boolean, level: Float, t: Long) {
        fill.color = C.SHADOW
        canvas.drawRoundRect(RectF(mascotTile.left, mascotTile.top + 10f, mascotTile.right, mascotTile.bottom + 10f), 40f, 40f, fill)
        fill.color = 0xFFFFE8CC.toInt()
        canvas.drawRoundRect(mascotTile, 40f, 40f, fill)
        line.color = if (speaking) C.ORANGE else C.CARD_EDGE
        line.strokeWidth = if (speaking) 9f else 4f
        canvas.drawRoundRect(mascotTile, 40f, 40f, line)
        canvas.save()
        canvas.clipRect(mascotTile.left + 4f, mascotTile.top + 4f, mascotTile.right - 4f, mascotTile.bottom - 4f)
        val emotion = e.line?.emotion ?: if (e.phase == MemoryPhase.Gallery) MascotEmotion.Shocked else MascotEmotion.Thinking
        drawMascot(canvas, RectF(mascotTile.left + 16f, mascotTile.top + 30f, mascotTile.right - 16f, mascotTile.bottom), emotion, speaking, level, t)
        canvas.restore()
    }

    private fun drawMascot(canvas: Canvas, box: RectF, emotion: MascotEmotion, speaking: Boolean, level: Float, t: Long) {
        val mouth = when {
            speaking -> mouthFor(level)
            (t % 3_400L) < 140L -> MouthState.Blink
            else -> MouthState.Closed
        }
        val sprite = sprites.frame(emotion, mouth)
        val bmp = sprite ?: sprites.fallback ?: return
        val scale = min(box.width() / bmp.width, box.height() / bmp.height)
        val w = bmp.width * scale
        val h = bmp.height * scale
        val bob = if (speaking) level * (if (sprite == null) 14f else 5f) else 0f
        val dst = RectF(box.centerX() - w / 2f, box.bottom - h - bob, box.centerX() + w / 2f, box.bottom - bob)
        val breathe = 1f + 0.012f * sin(t / 520.0).toFloat()
        canvas.save()
        canvas.scale(breathe, breathe, dst.centerX(), dst.bottom)
        canvas.drawBitmap(bmp, null, dst, fill)
        canvas.restore()
    }

    private fun drawStats(canvas: Canvas, e: MemoryEvent, t: Long) {
        val cx = statBox.centerX()
        val w = statBox.width().toInt()
        text.draw(canvas, script.word.uppercase(TR), cx, statBox.top + 62f, w, 78f, 1, assets.display, C.ORANGE_DARK)
        // the number counts up while the opening line is said, then stays
        val counted = if (e.phase == MemoryPhase.Hook) (script.forgot * Ease.out(min(1f, e.progress(t) * 1.6f))).toInt() else script.forgot
        text.draw(canvas, "$counted / ${script.drawn}", cx, statBox.top + 172f, w, 100f, 1, assets.display, C.INK)
        text.draw(canvas, "kişi kendi çizimini tanıyamadı", cx, statBox.top + 262f, w, 34f, 1, assets.body, C.MUTED)
        val chip = buildString {
            append("≈ ${MemoryMath.roundForSpeech(script.delaySec)} sn sonra")
            if (script.showPercent) append(" · %${script.percent}")
        }
        val p = text.paint(30f, assets.body, C.INK)
        val cw = p.measureText(chip) + 48f
        val box = RectF(cx - cw / 2f, statBox.top + 300f, cx + cw / 2f, statBox.top + 356f)
        fill.color = C.CREAM_DEEP
        canvas.drawRoundRect(box, 28f, 28f, fill)
        canvas.drawText(chip, box.left + 24f, box.centerY() + 10f, p)
    }

    // ---- the card in the middle

    private fun paper(canvas: Canvas, r: RectF = card) {
        fill.color = C.SHADOW
        canvas.drawRoundRect(RectF(r.left, r.top + 12f, r.right, r.bottom + 12f), 44f, 44f, fill)
        fill.color = C.CARD
        canvas.drawRoundRect(r, 44f, 44f, fill)
        line.color = C.CARD_EDGE; line.strokeWidth = 4f
        canvas.drawRoundRect(r, 44f, 44f, line)
    }

    /** The opening: a wall of the forgotten drawings, appearing one by one. */
    private fun drawMosaic(canvas: Canvas, e: MemoryEvent, t: Long) {
        paper(canvas)
        val shown = (min(9, mosaic.size) * Ease.out(min(1f, e.progress(t) * 1.8f))).toInt()
        val cols = 3
        val cell = (card.width() - 60f) / cols
        mosaic.take(9).forEachIndexed { i, s ->
            if (i >= shown) return@forEachIndexed
            val r = i / cols
            val c = i % cols
            val box = RectF(card.left + 30f + c * cell, card.top + 30f + r * cell * 0.72f, card.left + 30f + (c + 1) * cell, card.top + 30f + (r + 1) * cell * 0.72f)
            replay(s).draw(canvas, RectF(box.left + 10f, box.top + 6f, box.right - 10f, box.bottom - 6f), 1f, ink, 0.03f)
            fill.color = C.BAD
            canvas.drawCircle(box.right - 26f, box.top + 26f, 18f, fill)
            text.draw(canvas, "✗", box.right - 26f, box.top + 26f, 40, 26f, 1, assets.display, C.WHITE)
        }
    }

    /** One forgotten drawing at a time, sliding in and drawing itself quickly. */
    private fun drawPick(canvas: Canvas, e: MemoryEvent, t: Long) {
        val pick = script.picks.getOrNull(e.pick) ?: return
        val sample = samples[pick.sampleId] ?: return
        val p = e.progress(t)
        val slide = (1f - Ease.out(min(1f, p * 5f))) * 260f
        canvas.save()
        canvas.translate(slide, 0f)
        paper(canvas)
        val local = t - e.startMs
        replay(sample).draw(canvas, RectF(card.left, card.top + 40f, card.right, card.bottom - 90f), min(1f, local / 700f), ink)
        // the stamp
        val pop = Ease.backOut(min(1f, (local - 600f).coerceAtLeast(0f) / 350f))
        if (pop > 0f) {
            val cx = card.right - 110f
            val cy = card.top + 110f
            canvas.save()
            canvas.scale(pop, pop, cx, cy)
            fill.color = if (pick.correct) C.GOOD else C.BAD
            canvas.drawCircle(cx, cy, 70f, fill)
            text.draw(canvas, if (pick.correct) "✓" else "✗", cx, cy, 130, 90f, 1, assets.display, C.WHITE)
            canvas.restore()
            text.draw(canvas, if (pick.correct) "hatırladı" else "çizen kişi tanıyamadı", card.centerX(), card.bottom - 50f, 700, 38f, 1, assets.body, if (pick.correct) C.GOOD else C.BAD, alpha = pop.coerceIn(0f, 1f))
        }
        canvas.restore()
        // where we are in the run
        val n = script.picks.size
        val gap = 34f
        val startX = 540f - (n - 1) * gap / 2f
        for (i in 0 until n) {
            fill.color = if (i == e.pick) C.ORANGE else C.FAINT
            canvas.drawCircle(startX + i * gap, card.bottom + 60f, if (i == e.pick) 11f else 8f, fill)
        }
    }

    /** The twist: drawing is supposed to help you remember. */
    private fun drawParadox(canvas: Canvas, e: MemoryEvent, t: Long) {
        paper(canvas)
        val a = Ease.out(min(1f, e.progress(t) * 3f))
        text.draw(canvas, "Araştırma: çizmek hafızayı güçlendirir", card.centerX(), card.top + 110f, 700, 40f, 2, assets.body, C.MUTED, alpha = a)
        text.draw(canvas, "Waterloo Üniversitesi, 2016 · kelime başına 40 saniye", card.centerX(), card.top + 190f, 700, 28f, 2, assets.body, C.FAINT, alpha = a)
        // Karalak's clock: the word's real drawing time
        val cx = card.centerX()
        val cy = card.top + 410f
        val r = 130f
        line.color = C.CARD_EDGE; line.strokeWidth = 24f; line.strokeCap = Paint.Cap.ROUND
        canvas.drawCircle(cx, cy, r, line)
        line.color = C.ORANGE
        val sweep = 360f * (1f - min(1f, e.progress(t) * 1.4f))
        canvas.drawArc(RectF(cx - r, cy - r, cx + r, cy + r), -90f, sweep, false, line)
        text.draw(canvas, "${script.drawSeconds}", cx, cy - 10f, 240, 120f, 1, assets.display, C.INK)
        text.draw(canvas, "saniye", cx, cy + 70f, 240, 32f, 1, assets.body, C.MUTED)
        text.draw(canvas, "Karalak'ta", card.centerX(), card.bottom - 70f, 700, 44f, 1, assets.display, C.ORANGE_DARK, alpha = a)
    }

    // ---- the closing card

    private fun drawEndCard(canvas: Canvas, e: MemoryEvent, t: Long, speaking: Boolean, level: Float) {
        val a = Ease.out(min(1f, e.progress(t) * 4f))
        fill.color = C.CREAM
        fill.alpha = (a * 255).toInt()
        canvas.drawRect(0f, 0f, W.toFloat(), H.toFloat(), fill)
        fill.alpha = 255
        if (a < 0.6f) return
        canvas.save()
        canvas.translate(0f, (1f - a) * 60f)
        drawMascot(canvas, RectF(240f, 170f, 840f, 820f), e.line?.emotion ?: MascotEmotion.Point, speaking, level, t)
        text.draw(canvas, "Sen hatırlar mıydın?", 540f, 900f, 960, 76f, 1, assets.display, C.INK)
        e.line?.let { l ->
            val spoken = voice?.get(e.lineIndex)?.let { it.size * 1000L / FPS } ?: GuessTimeline.estimateMs(l.text)
            val prog = ((t - e.startMs).toFloat() / spoken.coerceAtLeast(1)).coerceIn(0f, 1f)
            text.karaoke(canvas, l.text, prog, 540f, 1040f, 960, 48f, 2, assets.display, C.ORANGE_DARK, C.FAINT)
        }
        fill.color = C.ORANGE
        val cta = RectF(140f, 1170f, 940f, 1300f)
        canvas.drawRoundRect(cta, 65f, 65f, fill)
        text.draw(canvas, "karalak.com · kurulumsuz oyna", cta.centerX(), cta.centerY(), 760, 46f, 1, assets.display, C.WHITE)
        text.draw(canvas, "Android: Google Play'de \"Karalak\"", 540f, 1370f, 900, 38f, 1, assets.body, C.INK)
        canvas.restore()
    }

    companion object {
        const val W = 1080
        const val H = 1920
        const val FPS = 30
        private val TR = Locale("tr")
    }
}
