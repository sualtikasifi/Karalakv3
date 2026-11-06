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
 * Draws "Çizen Bile Bilemedi" in 1080×2400 on the game-styled template (forgot_template): the title in the banner at the
 * top, one drawing on the big board in the middle, Karalak in the square at the bottom left and, in the speech bubble beside
 * it, his words as subtitles that come in as they are said. The drawing is shown, drawn again while Karalak guesses, a
 * countdown asks the viewer to guess too, Karalak reacts, and the kupon kodu comes up on the board: as empty boxes (the
 * answer itself) or written out. The closing is the game's end card with the phone.
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

    /** The template picture, 841 × 1870; the places below are its panels' insides, measured on it and scaled to the video. */
    private val template = assets.picture(R.drawable.forgot_template)

    /** The title banner (its pencil sits at the right end: the words keep to [bannerText]). */
    private val bannerText = RectF(200f, 318f, 835f, 452f)

    /** The big board: the drawing, the countdown and the code. */
    private val board = RectF(87f, 629f, 989f, 1490f)

    /** The square Karalak stands in, and the speech bubble beside it (its tail is on the left: the words keep to [bubbleText]). */
    private val tile = RectF(65f, 1628f, 390f, 1933f)
    private val bubbleText = RectF(480f, 1685f, 950f, 1870f)

    override fun draw(canvas: Canvas, t: Long) {
        canvas.drawColor(C.CREAM)
        val e = ForgotTimeline.at(events, t)
        val spoken = e.line?.let { l -> voice?.get(e.lineIndex)?.let { it.size * 1000L / FPS } ?: StudioCopy.estimateMs(l.text) } ?: 0L
        val speaking = e.line != null && t - e.startMs < spoken
        val level = level(e, t, speaking)
        if (e.phase == ForgotPhase.Outro) {
            if (phoneCard != null) {
                drawPhoneEnd(canvas, phoneCard, e, t)
            } else {
                // no how-to video to play: the closing keeps its own plain card, laid out for 1080 × 1920 and centred
                canvas.save()
                canvas.translate(0f, TOP_PAD)
                drawEndCard(canvas, e, t, speaking, level)
                canvas.restore()
            }
            return
        }
        template?.let { canvas.drawBitmap(it, null, RectF(0f, 0f, W.toFloat(), H.toFloat()), fill) }

        drawBanner(canvas)
        drawMascotTile(canvas, e, speaking, level, t)
        e.line?.let { l ->
            text.reveal(
                canvas, l.text, startsOf(e.lineIndex, l.text, spoken), t - e.startMs, bubbleText.centerX(), bubbleText.centerY(),
                bubbleText.width().toInt(), 46f, 3, assets.display, Lettering.plain(C.INK), balanced = true
            )
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
        if (e.phase != ForgotPhase.Code && e.phase != ForgotPhase.Countdown) drawChip(canvas, e)
        fill.color = C.CARD_EDGE
        canvas.drawRect(0f, H - 10f, W.toFloat(), H.toFloat(), fill)
        fill.color = C.ORANGE
        canvas.drawRect(0f, H - 10f, W * (t.toFloat() / totalMs).coerceIn(0f, 1f), H.toFloat(), fill)
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

    // ---- the banner and Karalak's square

    private fun drawBanner(canvas: Canvas) {
        text.draw(canvas, "ÇİZEN BİLE BİLEMEDİ", bannerText.centerX(), bannerText.centerY(), bannerText.width().toInt(), 74f, 1, assets.display, C.ORANGE_DARK)
    }

    private fun drawMascotTile(canvas: Canvas, e: ForgotEvent, speaking: Boolean, level: Float, t: Long) {
        if (speaking) {
            line.color = C.ORANGE
            line.strokeWidth = 9f
            canvas.drawRoundRect(tile, 44f, 44f, line)
        }
        canvas.save()
        canvas.clipRect(tile.left + 4f, tile.top + 4f, tile.right - 4f, tile.bottom - 4f)
        val emotion = e.line?.emotion ?: if (e.phase == ForgotPhase.Countdown) MascotEmotion.Suspicious else MascotEmotion.Thinking
        drawMascot(canvas, RectF(tile.left + 10f, tile.top + 24f, tile.right - 10f, tile.bottom), emotion, speaking, level, t)
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

    /** What the code gives (or, once Karalak has reacted and a share is set, how many missed it), a small chip on the board's top edge. */
    private fun drawChip(canvas: Canvas, e: ForgotEvent) {
        val chip = when {
            script.showPercent && e.phase == ForgotPhase.Answer -> "%${script.percent} kendi çizimini tanıyamadı"
            script.reward.isNotBlank() -> "Ödül: ${script.reward}"
            else -> return
        }
        val p = text.paint(34f, assets.body, C.INK)
        val cw = min(p.measureText(chip) + 64f, board.width() - 60f)
        val box = RectF(board.centerX() - cw / 2f, board.top + 22f, board.centerX() + cw / 2f, board.top + 86f)
        fill.color = C.CREAM_DEEP
        canvas.drawRoundRect(box, 32f, 32f, fill)
        text.draw(canvas, chip, box.centerX(), box.centerY(), (cw - 32f).toInt(), 34f, 1, assets.body, C.INK)
    }

    // ---- the board in the middle

    /** The drawing on the board, [fraction] of it drawn; [bobMs] > 0 gives a slow push-in while it stands. */
    private fun drawDrawing(canvas: Canvas, fraction: Float, alpha: Float, bobMs: Long) {
        val box = RectF(board.left + 30f, board.top + 100f, board.right - 30f, board.bottom - 30f)
        val zoom = 1f + 0.02f * min(1f, bobMs / 3_000f)
        canvas.save()
        canvas.scale(zoom, zoom, box.centerX(), box.centerY())
        ink.alpha = (alpha * 255).toInt()
        replay.draw(canvas, box, fraction, ink)
        ink.alpha = 255
        canvas.restore()
    }

    /** "Sen ne görüyorsun?" and 3 · 2 · 1 over the board, the drawing pale behind it. */
    private fun drawCountdown(canvas: Canvas, e: ForgotEvent, t: Long) {
        drawDrawing(canvas, 1f, 1f, 0L)
        val local = t - e.startMs
        fill.color = C.CARD
        fill.alpha = 215
        canvas.drawRoundRect(RectF(board.left + 8f, board.top + 8f, board.right - 8f, board.bottom - 8f), 52f, 52f, fill)
        fill.alpha = 255
        val step = (local * 3 / ForgotTimeline.COUNTDOWN_MS).toInt().coerceIn(0, 2)
        val within = ((local * 3f / ForgotTimeline.COUNTDOWN_MS) - step).coerceIn(0f, 1f)
        val pop = Ease.backOut(min(1f, within * 3f))
        text.draw(canvas, "Sen ne görüyorsun?", board.centerX(), board.top + 140f, 760, 64f, 2, assets.display, C.INK)
        val cx = board.centerX()
        val cy = board.centerY() + 70f
        canvas.save()
        canvas.scale(pop, pop, cx, cy)
        fill.color = C.ORANGE
        canvas.drawCircle(cx, cy, 190f, fill)
        text.draw(canvas, "${3 - step}", cx, cy, 260, 260f, 1, assets.display, C.WHITE)
        canvas.restore()
    }

    /** The whole drawing again on the board while Karalak reacts. */
    private fun drawAnswer(canvas: Canvas) = drawDrawing(canvas, 1f, 1f, 0L)

    /**
     * The kupon kodu on the board. In [ForgotMode.Word] it is empty boxes, one per letter (the answer is the code, the
     * letter count is the only hint); in [ForgotMode.Shown] the letters drop in one by one.
     */
    private fun drawCode(canvas: Canvas, e: ForgotEvent, t: Long) {
        val a = Ease.out(min(1f, e.progress(t) * 4f))
        val word = script.mode == ForgotMode.Word
        text.draw(canvas, "KUPON KODU", board.centerX(), board.top + 130f, 760, 72f, 1, assets.display, C.ORANGE_DARK, alpha = a)
        text.draw(
            canvas, if (word) "Doğru cevap = kod" else "Ayarlar → Kupon kodu",
            board.centerX(), board.top + 225f, 760, 44f, 1, assets.body, C.MUTED, alpha = a
        )
        val code = script.code
        val local = t - e.startMs
        val shown = if (word) 0 else (code.length * Ease.out(min(1f, (local - 250f).coerceAtLeast(0f) / 900f))).toInt().coerceAtMost(code.length)
        drawBoxes(canvas, code, shown, word, board.centerY() + 30f, a)
        if (script.reward.isNotBlank()) {
            text.draw(canvas, "Ödül: ${script.reward}", board.centerX(), board.bottom - 150f, 780, 52f, 1, assets.display, C.INK, alpha = a)
        }
        if (word) text.draw(canvas, "Bilemedin mi? Yorumlara bak.", board.centerX(), board.bottom - 75f, 780, 38f, 1, assets.body, C.MUTED, alpha = a)
    }

    /** The code as one box a character (a dash stays a dash), in rows of at most [ROW] boxes. */
    private fun drawBoxes(canvas: Canvas, code: String, shown: Int, hidden: Boolean, cy: Float, alpha: Float) {
        if (code.isEmpty()) return
        val rows = code.chunked(ROW)
        val cell = min(92f, (board.width() - 100f) / ROW - 8f).coerceAtLeast(40f)
        val gap = 8f
        var index = 0
        rows.forEachIndexed { r, row ->
            val rowW = row.length * (cell + gap) - gap
            val top = cy - (rows.size * (cell + 24f)) / 2f + r * (cell + 24f)
            row.forEachIndexed { i, ch ->
                val x = board.centerX() - rowW / 2f + i * (cell + gap)
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
