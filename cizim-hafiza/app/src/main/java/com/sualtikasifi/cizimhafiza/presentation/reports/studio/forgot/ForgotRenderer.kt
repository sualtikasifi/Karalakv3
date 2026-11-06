package com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.EndCardPhone
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.ClipFrames
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealTimeline
import kotlin.math.hypot
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.Ease
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.Lettering
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StrokeReplay
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioAssets
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioColors as C
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioDrawing
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.TextPainter
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.VoicedVideo
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.audio.Reveal
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioCopy
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MascotEmotion
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MascotSprites
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MouthState
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.mouthFor
import kotlin.math.min
import kotlin.math.sin

/**
 * Draws "Çizen Bile Bilemedi" in 1080×1920: Karalak and the title with what the code gives at the top, its words as
 * subtitles that come in as they are said, and one drawing on a sheet of paper below. The drawing is shown, drawn
 * again while Karalak guesses, a countdown asks the viewer to guess too, Karalak reacts, and the kupon
 * kodu comes up: as empty boxes (the answer itself) or written out. Everything a viewer must read sits clear of the
 * band TikTok and Reels cover.
 */
class ForgotRenderer(
    private val assets: StudioAssets,
    private val sprites: MascotSprites,
    private val script: ForgotScript,
    private val drawing: StudioDrawing,
    private val voice: Map<Int, FloatArray>?,
    private val wordTimes: Map<Int, List<LongRange>>? = null,
    /** The video of how the game is played, shown without its sound inside the phone on the end card; [howtoMs] long (0: none). */
    private val howto: ClipFrames? = null,
    private val howtoMs: Long = 0L
) : VoicedVideo {
    val events: List<ForgotEvent> = ForgotTimeline.build(script, howtoMs) { i, l ->
        voice?.get(i)?.let { it.size * 1000L / FPS } ?: StudioCopy.estimateMs(l.text)
    }
    override val width: Int get() = W
    override val height: Int get() = H
    override val totalMs: Long = ForgotTimeline.totalMs(events)
    override val voiceStarts: List<Pair<Int, Long>> = events.filter { it.line != null }.map { it.lineIndex to it.startMs }

    private val text = TextPainter()
    private val revealStarts = HashMap<Int, LongArray>()
    private fun startsOf(index: Int, line: String, spokenMs: Long): LongArray = revealStarts.getOrPut(index) {
        Reveal.charStarts(line, wordTimes?.get(index), if (voice?.get(index) != null) spokenMs else 0L)
    }

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val ink = StrokeReplay.inkPaint()
    private val replay = StrokeReplay(drawing.strokes)
    private val src = Rect()
    private val plain = Paint(Paint.ANTI_ALIAS_FLAG)
    private val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = C.WHITE }
    private val circle = Path()

    /** The end card with the phone, when there is a how-to video to play in it; otherwise the closing keeps its own plain card. */
    private val phoneCard = if (howto != null && howtoMs > 0) assets.picture(R.drawable.real_end_card_phone) else null

    private val mascotTile = RectF(40f, 150f, 400f, 510f)
    private val titleBox = RectF(430f, 150f, 1040f, 510f)
    private val card = RectF(130f, 720f, 950f, 1400f)

    override fun draw(canvas: Canvas, t: Long) {
        canvas.drawColor(C.CREAM)
        val e = ForgotTimeline.at(events, t)
        if (e.phase == ForgotPhase.Outro && phoneCard != null) {
            drawPhoneEnd(canvas, phoneCard, e, t)
            return
        }
        // the video is 9:20, taller than the 9:16 everything below is laid out for: it sits in the middle, cream above and below
        canvas.save()
        canvas.translate(0f, TOP_PAD)
        val spoken = e.line?.let { l -> voice?.get(e.lineIndex)?.let { it.size * 1000L / FPS } ?: StudioCopy.estimateMs(l.text) } ?: 0L
        val speaking = e.line != null && t - e.startMs < spoken
        val level = level(e, t, speaking)

        drawMascotTile(canvas, e, speaking, level, t)
        drawTitle(canvas, e, t)
        e.line?.let { l ->
            if (e.phase != ForgotPhase.Outro) {
                text.reveal(canvas, l.text, startsOf(e.lineIndex, l.text, spoken), t - e.startMs, 540f, 615f, 980, 50f, 2, assets.display, Lettering.plain(C.INK))
            }
        }
        when (e.phase) {
            ForgotPhase.Hook -> drawDrawing(canvas, 1f, 1f, t - e.startMs)
            ForgotPhase.Draw -> drawDrawing(canvas, e.drawFrom + (e.drawTo - e.drawFrom) * Ease.inOut(e.progress(t)), 1f, 0L)
            ForgotPhase.Guess -> drawDrawing(canvas, e.drawTo, 1f, 0L)
            ForgotPhase.Countdown -> drawCountdown(canvas, e, t)
            ForgotPhase.Answer -> drawAnswer(canvas)
            ForgotPhase.Code -> drawCode(canvas, e, t)
            ForgotPhase.Outro -> Unit
        }
        if (e.phase != ForgotPhase.Outro) {
            text.draw(canvas, "karalak.com · Google Play: Karalak", 540f, 1640f, 900, 32f, 1, assets.body, C.MUTED)
        } else {
            drawEndCard(canvas, e, t, speaking, level)
        }
        canvas.restore()
        if (e.phase != ForgotPhase.Outro) {
            fill.color = C.CARD_EDGE
            canvas.drawRect(0f, H - 10f, W.toFloat(), H.toFloat(), fill)
            fill.color = C.ORANGE
            canvas.drawRect(0f, H - 10f, W * (t.toFloat() / totalMs).coerceIn(0f, 1f), H.toFloat(), fill)
        }
    }

    /**
     * The closing on the game's end card: it opens out of the last frame in a growing circle, the how-to video plays in the
     * phone's screen once the card has opened and holds its last frame, and Karalak's closing line is only said (no subtitle,
     * the card has its own words).
     */
    private fun drawPhoneEnd(canvas: Canvas, card: Bitmap, e: ForgotEvent, t: Long) {
        val local = t - e.startMs
        val open = Ease.out((local.toFloat() / END_OPEN).coerceIn(0f, 1f))
        // what the circle opens over: the frame before it
        if (open < 1f && e.startMs > 0) draw(canvas, e.startMs - 1)
        val cx = W / 2f
        val cy = H / 2f
        val r = open * hypot(cx, cy)
        circle.reset()
        circle.addCircle(cx, cy, r, Path.Direction.CW)
        canvas.save()
        canvas.clipPath(circle)
        canvas.drawColor(C.CREAM)
        val screen = EndCardPhone.screen()
        val at = (local - RealTimeline.END_VIDEO_LEAD).coerceIn(0L, (howtoMs - 1L).coerceAtLeast(0L))
        val frame = howto?.frameAt(at)
        plain.color = C.INK
        canvas.drawRect(screen, plain)
        if (frame != null && !frame.isRecycled) {
            canvas.save()
            canvas.clipRect(screen)
            cover(canvas, frame, screen)
            canvas.restore()
        }
        canvas.drawBitmap(card, null, RectF(0f, 0f, W.toFloat(), H.toFloat()), fill)
        canvas.restore()
        edge.strokeWidth = 10f * (1f - open)
        if (edge.strokeWidth > 0.5f) canvas.drawCircle(cx, cy, r, edge)
    }

    /** Fills [dst] with [bmp], cropped to its shape. */
    private fun cover(canvas: Canvas, bmp: Bitmap, dst: RectF) {
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

    private fun level(e: ForgotEvent, t: Long, speaking: Boolean): Float {
        if (!speaking) return 0f
        val local = t - e.startMs
        val env = voice?.get(e.lineIndex)
        if (env != null) {
            val i = (local * FPS / 1000).toInt()
            return if (i in env.indices) env[i] else 0f
        }
        return (0.55f + 0.45f * sin(local / 55.0).toFloat()).coerceIn(0f, 1f) * if ((local / 260) % 4 == 3L) 0.2f else 1f
    }

    // ---- top: Karalak and the title

    private fun drawMascotTile(canvas: Canvas, e: ForgotEvent, speaking: Boolean, level: Float, t: Long) {
        fill.color = C.SHADOW
        canvas.drawRoundRect(RectF(mascotTile.left, mascotTile.top + 10f, mascotTile.right, mascotTile.bottom + 10f), 40f, 40f, fill)
        fill.color = 0xFFFFE8CC.toInt()
        canvas.drawRoundRect(mascotTile, 40f, 40f, fill)
        line.color = if (speaking) C.ORANGE else C.CARD_EDGE
        line.strokeWidth = if (speaking) 9f else 4f
        canvas.drawRoundRect(mascotTile, 40f, 40f, line)
        canvas.save()
        canvas.clipRect(mascotTile.left + 4f, mascotTile.top + 4f, mascotTile.right - 4f, mascotTile.bottom - 4f)
        val emotion = e.line?.emotion ?: if (e.phase == ForgotPhase.Countdown) MascotEmotion.Suspicious else MascotEmotion.Thinking
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

    /** The title, always the same, and under it what the code gives (or, once the answer has landed, the share who missed it). */
    private fun drawTitle(canvas: Canvas, e: ForgotEvent, t: Long) {
        val cx = titleBox.centerX()
        val w = titleBox.width().toInt()
        text.draw(canvas, "ÇİZEN BİLE BİLEMEDİ", cx, titleBox.top + 120f, w, 80f, 2, assets.display, C.ORANGE_DARK, spacing = 0.92f, balanced = true)
        val chip = when {
            script.showPercent && e.phase >= ForgotPhase.Answer && e.phase != ForgotPhase.Outro -> "%${script.percent} kendi çizimini tanıyamadı"
            script.reward.isNotBlank() -> "Ödül: ${script.reward}"
            else -> ""
        }
        if (chip.isNotEmpty()) {
            val p = text.paint(32f, assets.body, C.INK)
            val cw = min(p.measureText(chip) + 56f, titleBox.width())
            val box = RectF(cx - cw / 2f, titleBox.top + 250f, cx + cw / 2f, titleBox.top + 312f)
            fill.color = C.CREAM_DEEP
            canvas.drawRoundRect(box, 31f, 31f, fill)
            text.draw(canvas, chip, box.centerX(), box.centerY(), (cw - 28f).toInt(), 32f, 1, assets.body, C.INK)
        }
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

    /** The drawing on the paper, [fraction] of it drawn; [bobMs] > 0 gives a slow push-in while it stands. */
    private fun drawDrawing(canvas: Canvas, fraction: Float, alpha: Float, bobMs: Long, box: RectF = RectF(card.left, card.top + 40f, card.right, card.bottom - 40f)) {
        paper(canvas)
        val zoom = 1f + 0.02f * min(1f, bobMs / 3_000f)
        canvas.save()
        canvas.scale(zoom, zoom, card.centerX(), card.centerY())
        ink.alpha = (alpha * 255).toInt()
        replay.draw(canvas, box, fraction, ink)
        ink.alpha = 255
        canvas.restore()
    }

    /** "Sen ne görüyorsun?" and 3 · 2 · 1 over the paper, the drawing pale behind it. */
    private fun drawCountdown(canvas: Canvas, e: ForgotEvent, t: Long) {
        drawDrawing(canvas, 1f, 1f, 0L)
        val local = t - e.startMs
        fill.color = C.CARD
        fill.alpha = 205
        canvas.drawRoundRect(card, 44f, 44f, fill)
        fill.alpha = 255
        val step = (local * 3 / ForgotTimeline.COUNTDOWN_MS).toInt().coerceIn(0, 2)
        val within = ((local * 3f / ForgotTimeline.COUNTDOWN_MS) - step).coerceIn(0f, 1f)
        val pop = Ease.backOut(min(1f, within * 3f))
        text.draw(canvas, "Sen ne görüyorsun?", card.centerX(), card.top + 100f, 700, 56f, 2, assets.display, C.INK)
        val cx = card.centerX()
        val cy = card.centerY() + 40f
        canvas.save()
        canvas.scale(pop, pop, cx, cy)
        fill.color = C.ORANGE
        canvas.drawCircle(cx, cy, 170f, fill)
        text.draw(canvas, "${3 - step}", cx, cy, 240, 240f, 1, assets.display, C.WHITE)
        canvas.restore()
    }

    /** The whole drawing again on the paper while Karalak reacts. */
    private fun drawAnswer(canvas: Canvas) = drawDrawing(canvas, 1f, 1f, 0L)

    /**
     * The kupon kodu on the paper. In [ForgotMode.Word] it is empty boxes, one per letter (the answer is the code, the
     * letter count is the only hint); in [ForgotMode.Shown] the letters drop in one by one.
     */
    private fun drawCode(canvas: Canvas, e: ForgotEvent, t: Long) {
        paper(canvas)
        val a = Ease.out(min(1f, e.progress(t) * 4f))
        val word = script.mode == ForgotMode.Word
        text.draw(canvas, "KUPON KODU", card.centerX(), card.top + 100f, 700, 64f, 1, assets.display, C.ORANGE_DARK, alpha = a)
        text.draw(
            canvas, if (word) "Doğru cevap = kod" else "Ayarlar → Kupon kodu",
            card.centerX(), card.top + 180f, 700, 40f, 1, assets.body, C.MUTED, alpha = a
        )
        val code = script.code
        val local = t - e.startMs
        val shown = if (word) 0 else (code.length * Ease.out(min(1f, (local - 250f).coerceAtLeast(0f) / 900f))).toInt().coerceAtMost(code.length)
        drawBoxes(canvas, code, shown, word, card.centerY() + 20f, a)
        if (script.reward.isNotBlank()) {
            text.draw(canvas, "Ödül: ${script.reward}", card.centerX(), card.bottom - 110f, 720, 46f, 1, assets.display, C.INK, alpha = a)
        }
        if (word) text.draw(canvas, "Bilemedin mi? Yorumlara bak.", card.centerX(), card.bottom - 50f, 720, 34f, 1, assets.body, C.MUTED, alpha = a)
    }

    /** The code as one box a character (a dash stays a dash), in rows of at most [ROW] boxes. */
    private fun drawBoxes(canvas: Canvas, code: String, shown: Int, hidden: Boolean, cy: Float, alpha: Float) {
        if (code.isEmpty()) return
        val rows = code.chunked(ROW)
        val cell = min(86f, (card.width() - 80f) / ROW - 8f).coerceAtLeast(40f)
        val gap = 8f
        var index = 0
        rows.forEachIndexed { r, row ->
            val rowW = row.length * (cell + gap) - gap
            val top = cy - (rows.size * (cell + 24f)) / 2f + r * (cell + 24f)
            row.forEachIndexed { i, ch ->
                val x = card.centerX() - rowW / 2f + i * (cell + gap)
                val box = RectF(x, top, x + cell, top + cell * 1.2f)
                val on = index < shown
                fill.color = if (on) 0xFFFFE8CC.toInt() else C.CREAM_DEEP
                fill.alpha = (255 * alpha).toInt()
                canvas.drawRoundRect(box, 16f, 16f, fill)
                fill.alpha = 255
                line.color = if (on) C.ORANGE else C.CARD_EDGE
                line.strokeWidth = if (on) 5f else 3f
                canvas.drawRoundRect(box, 16f, 16f, line)
                if (on && !hidden) text.draw(canvas, ch.toString(), box.centerX(), box.centerY(), cell.toInt(), cell * 0.8f, 1, assets.display, C.INK)
                index++
            }
        }
    }

    // ---- the closing card

    private fun drawEndCard(canvas: Canvas, e: ForgotEvent, t: Long, speaking: Boolean, level: Float) {
        val a = Ease.out(min(1f, e.progress(t) * 4f))
        fill.color = C.CREAM
        fill.alpha = (a * 255).toInt()
        canvas.drawRect(0f, 0f, W.toFloat(), H.toFloat(), fill)
        fill.alpha = 255
        if (a < 0.6f) return
        canvas.save()
        canvas.translate(0f, (1f - a) * 60f)
        drawMascot(canvas, RectF(240f, 170f, 840f, 820f), e.line?.emotion ?: MascotEmotion.Point, speaking, level, t)
        text.draw(canvas, "Yarın daha zoru geliyor", 540f, 900f, 960, 76f, 1, assets.display, C.INK)
        e.line?.let { l ->
            val spoken = voice?.get(e.lineIndex)?.let { it.size * 1000L / FPS } ?: StudioCopy.estimateMs(l.text)
            text.reveal(canvas, l.text, startsOf(e.lineIndex, l.text, spoken), t - e.startMs, 540f, 1040f, 960, 48f, 2, assets.display, Lettering.plain(C.ORANGE_DARK))
        }
        fill.color = C.ORANGE
        val cta = RectF(140f, 1170f, 940f, 1300f)
        canvas.drawRoundRect(cta, 65f, 65f, fill)
        text.draw(canvas, "karalak.com · kurulumsuz oyna", cta.centerX(), cta.centerY(), 760, 46f, 1, assets.display, C.WHITE)
        text.draw(canvas, "Android: Google Play'de \"Karalak\"", 540f, 1370f, 900, 38f, 1, assets.body, C.INK)
        text.draw(canvas, "Kodu Ayarlar → Kupon kodu'na yaz", 540f, 1440f, 900, 34f, 1, assets.body, C.MUTED)
        canvas.restore()
    }

    companion object {
        const val W = 1080
        /** 9:20, the proportions of a phone's full screen, like "Gerçek Olsaydı". */
        const val H = 2400
        /** What everything but the end card is laid out on: 1080 × 1920, centred in the frame. */
        private const val CONTENT_H = 1920
        private const val TOP_PAD = (H - CONTENT_H) / 2f
        private const val END_OPEN = 560L
        const val FPS = 30
        private const val ROW = 10
    }
}
