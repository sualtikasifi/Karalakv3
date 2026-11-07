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
    private val rewardFrame = assets.picture(R.drawable.reward_frame)

    /**
     * The title banner's free inside, measured on the template (its pencil and blue line take the right end, so the empty
     * part is centred left of the pill's middle): centre (515.5, 384.5), 633 wide.
     */
    private val bannerText = RectF(199f, 318f, 832f, 451f)

    /** The big board: the drawing, the countdown and the code. */
    private val board = RectF(87f, 629f, 989f, 1490f)

    /** The square Karalak stands in (centre 227.9, 1780.1). */
    private val tile = RectF(65f, 1628f, 390f, 1933f)

    /**
     * The speech bubble's oval, measured on the template without its tail: centre (718.5, 1776.3), 572 × 293. The words keep to
     * the rectangle that fits inside that oval (82% of its width, 57% of its height) and are centred on it.
     */
    private val bubbleText = RectF(484f, 1692f, 953f, 1860f)

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
                bubbleText.width().toInt(), subtitleSize(l.text), 3, assets.display, Lettering.plain(C.INK), balanced = true, ink = true
            )
        }
        when (e.phase) {
            ForgotPhase.Hook -> drawDrawing(canvas, e.drawFrom + (e.drawTo - e.drawFrom) * Ease.inOut(e.progress(t)), 1f, 0L)
            ForgotPhase.Draw -> drawDrawing(canvas, e.drawFrom + (e.drawTo - e.drawFrom) * Ease.inOut(e.progress(t)), 1f, 0L)
            ForgotPhase.Guess -> drawDrawing(canvas, e.drawTo, 1f, 0L)
            // the question over the finished drawing: nothing but Karalak asking, the blanks and the prize stay
            ForgotPhase.Countdown -> drawDrawing(canvas, 1f, 1f, 0L)
            ForgotPhase.Code -> drawCode(canvas, e, t)
            ForgotPhase.Outro -> Unit
        }
        // the prize frame stands on the board's lower edge all through; the word's blanks only while the drawing is shown
        if (e.phase != ForgotPhase.Code) drawLetterSlots(canvas, board.centerX(), board.width() - 100f)
        drawRewardFrame(canvas)
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

    /**
     * The title, centred in the banner's opening on its letters (their ink, as large as fits [bannerText]'s width, 74 at most).
     * The dots over Ö and Ü are no part of what the eye centres, so the capitals' body is centred vertically and the dots stand above it.
     */
    private fun drawBanner(canvas: Canvas) {
        val (size, cy) = bannerFit
        text.draw(canvas, ForgotCopy.TITLE, bannerText.centerX(), cy, BANNER_MEASURE_W, size, 1, assets.display, C.ORANGE_DARK, ink = true)
    }

    /** The title's size and the centre line it is drawn on, worked out once: the same for every frame. */
    private val bannerFit: Pair<Float, Float> by lazy {
        val title = ForgotCopy.TITLE
        var size = BANNER_SIZE
        while (size > 20f && text.measureInk(title, BANNER_MEASURE_W, size, 1, assets.display, C.ORANGE_DARK).first > bannerText.width()) size -= 0.5f
        val full = text.measureInk(title, BANNER_MEASURE_W, size, 1, assets.display, C.ORANGE_DARK).second
        val body = text.measureInk(ForgotCopy.TITLE_BODY, BANNER_MEASURE_W, size, 1, assets.display, C.ORANGE_DARK).second
        // the ink box is centred by [draw]; moving it up by half the dots' height puts the capitals' body on the centre
        size to (bannerText.centerY() - (full - body) / 2f)
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
        drawMascot(canvas, RectF(tile.left + 14f, tile.top + 14f, tile.right - 14f, tile.bottom - 14f), emotion, speaking, level, t)
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
        // centred in the box both ways (it is the square's middle that is Karalak's, not its floor)
        val dst = RectF(box.centerX() - w / 2f, box.centerY() - h / 2f - bob, box.centerX() + w / 2f, box.centerY() + h / 2f - bob)
        val breathe = 1f + 0.012f * sin(t / 520.0).toFloat()
        canvas.save()
        canvas.scale(breathe, breathe, dst.centerX(), dst.centerY())
        canvas.drawBitmap(bmp, null, dst, fill)
        canvas.restore()
    }

    /**
     * One line per letter of the drawing's word, as many as it has (words apart by a wider gap): the viewer sees how long the
     * answer is from the first frame. Centred on [cx] in [width]; the lines shrink together when the word is too long for it.
     */
    private fun drawLetterSlots(canvas: Canvas, cx: Float, width: Float) {
        val letters = script.word.trim()
        if (letters.none { !it.isWhitespace() }) return
        val gap = 12f
        val wordGap = 40f
        val n = letters.count { !it.isWhitespace() }
        val gaps = letters.zipWithNext().count { (a, b) -> !a.isWhitespace() && !b.isWhitespace() } * gap + letters.count { it.isWhitespace() } * wordGap
        val w = ((width - gaps) / n).coerceIn(14f, 56f)
        val total = n * w + gaps
        val y = board.top + 54f
        var x = cx - total / 2f
        line.color = C.MUTED
        line.strokeWidth = 9f
        line.strokeCap = Paint.Cap.ROUND
        letters.forEachIndexed { i, ch ->
            if (ch.isWhitespace()) { x += wordGap; return@forEachIndexed }
            canvas.drawLine(x, y, x + w, y, line)
            x += w
            if (i + 1 < letters.length && !letters[i + 1].isWhitespace()) x += gap
        }
        line.strokeCap = Paint.Cap.BUTT
    }

    /**
     * "Ödül: 500 altın" in the painted gold frame (reward_frame), at the board's bottom: the frame is [REWARD_W] wide and sits
     * on the board's lower edge; the words are centred on the middle of its purple field, the dots over the Ö not counted.
     * Nothing is drawn when the code gives nothing.
     */
    private fun drawRewardFrame(canvas: Canvas) {
        val frame = rewardFrame ?: return
        val reward = script.reward.takeIf { it.isNotBlank() } ?: return
        val w = REWARD_W
        val h = w * frame.height / frame.width
        val left = board.centerX() - w / 2f
        // it straddles the board's lower edge (the purple field a little below it), so it takes next to nothing of the drawing's room
        val top = board.bottom + REWARD_DROP - h * (FIELD_T + FIELD_B) / 2f
        canvas.drawBitmap(frame, null, RectF(left, top, left + w, top + h), fill)
        // the purple field, as shares of the picture (measured on it)
        val field = RectF(left + w * FIELD_L, top + h * FIELD_T, left + w * FIELD_R, top + h * FIELD_B)
        val label = "${ForgotCopy.REWARD_LABEL} $reward"
        val room = (field.width() - 56f).toInt()
        // one line while it stays readable (34 or more); a long prize (chest + XP + gold) goes on two lines instead of shrinking
        var size = 46f
        while (size > 34f && text.measureInk(label, 1000, size, 1, assets.display, C.WHITE).first > room) size -= 1f
        val two = text.measureInk(label, 1000, size, 1, assets.display, C.WHITE).first > room
        val lines = if (two) 2 else 1
        val width = if (two) room else 1000
        if (two) {
            size = 38f
            while (size > 22f && text.measureInk(label, width, size, 2, assets.display, C.WHITE, balanced = true).let { it.first > room || it.second > field.height() - 18f }) size -= 1f
        }
        val full = text.measureInk(label, width, size, lines, assets.display, C.WHITE, balanced = two).second
        val body = text.measureInk(label.replace('Ö', 'O'), width, size, lines, assets.display, C.WHITE, balanced = two).second
        text.draw(canvas, label, field.centerX(), field.centerY() - (full - body) / 2f, width, size, lines, assets.display, C.WHITE, balanced = two, ink = true)
    }

    // ---- the board in the middle

    /** The drawing on the board, [fraction] of it drawn; [bobMs] > 0 gives a slow push-in while it stands. */
    private fun drawDrawing(canvas: Canvas, fraction: Float, alpha: Float, bobMs: Long) {
        val box = RectF(board.left + 30f, board.top + 100f, board.right - 30f, board.bottom - 130f)
        val zoom = 1f + 0.02f * min(1f, bobMs / 3_000f)
        canvas.save()
        canvas.scale(zoom, zoom, box.centerX(), box.centerY())
        ink.alpha = (alpha * 255).toInt()
        replay.draw(canvas, box, fraction, ink)
        ink.alpha = 255
        canvas.restore()
    }

    /** The letters' height of [text] as it is drawn with ink centering (see [TextPainter.measureInk]). */
    private fun inkHeight(line: String, width: Int, size: Float, maxLines: Int, face: android.graphics.Typeface) =
        text.measureInk(line, width, size, maxLines, face, C.INK).second

    /** The biggest subtitle size (50 down to 30) whose letters fit the oval's rectangle, [SUBTITLE_LINES] lines at most. */
    private fun subtitleSize(line: String): Float = subtitleSizes.getOrPut(line) {
        var size = 50f
        while (size > 30f) {
            val (w, h) = text.measureInk(line, bubbleText.width().toInt(), size, SUBTITLE_LINES, assets.display, C.INK, balanced = true)
            if (w <= bubbleText.width() && h <= bubbleText.height()) break
            size -= 2f
        }
        size
    }
    private val subtitleSizes = HashMap<String, Float>()

    /**
     * The code screen on the board, top to bottom: what it is, the code (empty boxes, one per letter, when the answer is
     * the code; the letters dropping in when it is written out), what it wins as the biggest thing on it, how many may use it
     * and until when, and where to type it. Everything is stacked and the stack is centred; it shrinks as a whole when it
     * would not fit.
     */
    private fun drawCode(canvas: Canvas, e: ForgotEvent, t: Long) {
        val a = Ease.out(min(1f, e.progress(t) * 4f))
        val word = script.mode == ForgotMode.Word
        val cx = board.centerX()
        val code = script.code
        val style = script.style
        val title = style.codeTitle(word)
        val explain = style.explain(word)
        val limit = ForgotCopy.limitText(script.uses, script.expiresAtMillis, style).takeIf { it.isNotBlank() }
        val howTo = style.howTo
        val hint = if (word) style.hintWord else null

        val per = ForgotCopy.rowSize(code.length)
        val rows = if (code.isEmpty()) 0 else (code.length + per - 1) / per
        val cell0 = min(96f, (board.width() - 100f) / per - 8f).coerceAtLeast(40f)
        val boxes0 = if (code.isEmpty()) 0f else rows * cell0 * 1.2f + (rows - 1) * BOX_ROW_GAP
        val hTitle = inkHeight(title, 800, 68f, 1, assets.display)
        val hExplain = inkHeight(explain, 820, 42f, 1, assets.body)
        val hLimit = limit?.let { inkHeight(it, 820, 42f, 1, assets.display) } ?: 0f
        val hHow = inkHeight(howTo, 820, 40f, 1, assets.body)
        val hHint = hint?.let { inkHeight(it, 820, 36f, 1, assets.body) } ?: 0f
        val gaps = floatArrayOf(
            26f, 48f,                                        // title | explain | boxes
            54f,                                             // boxes | limit (or how-to)
            if (limit != null) 34f else 0f,                  // limit | how-to
            if (hint != null) 14f else 0f                    // how-to | hint
        )
        val total = hTitle + hExplain + boxes0 + hLimit + hHow + hHint + gaps.sum()
        // the whole stack shrinks together when it is taller than the room the prize frame leaves it (it stands on the lower edge)
        val roomTop = board.top + 30f
        val roomBottom = board.bottom - CODE_FRAME_ROOM
        val k = min(1f, (roomBottom - roomTop) / total)

        var y = (roomTop + roomBottom) / 2f - total * k / 2f
        fun put(txt: String, h: Float, size: Float, width: Int, face: android.graphics.Typeface, color: Int) {
            text.draw(canvas, txt, cx, y + h * k / 2f, width, size * k, 1, face, color, alpha = a, ink = true)
            y += h * k
        }
        put(title, hTitle, 68f, 800, assets.display, C.ORANGE_DARK)
        y += gaps[0] * k
        put(explain, hExplain, 42f, 820, assets.body, C.MUTED)
        y += gaps[1] * k
        val local = t - e.startMs
        val shown = if (word) 0 else (code.length * Ease.out(min(1f, (local - 250f).coerceAtLeast(0f) / 900f))).toInt().coerceAtMost(code.length)
        drawBoxes(canvas, code, shown, word, y, cell0 * k, BOX_ROW_GAP * k, a)
        y += boxes0 * k + gaps[2] * k
        if (limit != null) {
            put(limit, hLimit, 42f, 820, assets.display, C.ORANGE_DARK)
            y += gaps[3] * k
        }
        put(howTo, hHow, 40f, 820, assets.body, C.INK)
        if (hint != null) {
            y += gaps[4] * k
            put(hint, hHint, 36f, 820, assets.body, C.MUTED)
        }
    }

    /** The code as one box a character, in rows of [ForgotCopy.rowSize] boxes, the block starting at [top]; each letter centred in its box. */
    private fun drawBoxes(canvas: Canvas, code: String, shown: Int, hidden: Boolean, top: Float, cell: Float, rowGap: Float, alpha: Float) {
        if (code.isEmpty()) return
        val gap = 8f
        var index = 0
        code.chunked(ForgotCopy.rowSize(code.length)).forEachIndexed { r, row ->
            val rowW = row.length * (cell + gap) - gap
            val boxTop = top + r * (cell * 1.2f + rowGap)
            row.forEachIndexed { i, ch ->
                val x = board.centerX() - rowW / 2f + i * (cell + gap)
                val box = RectF(x, boxTop, x + cell, boxTop + cell * 1.2f)
                val on = index < shown
                fill.color = if (on) 0xFFFFE8CC.toInt() else C.CREAM_DEEP
                fill.alpha = (255 * alpha).toInt()
                canvas.drawRoundRect(box, 16f, 16f, fill)
                fill.alpha = 255
                line.color = if (on) C.ORANGE else C.CARD_EDGE
                line.strokeWidth = if (on) 5f else 3f
                canvas.drawRoundRect(box, 16f, 16f, line)
                if (on && !hidden) text.draw(canvas, ch.toString(), box.centerX(), box.centerY(), cell.toInt(), cell * 0.8f, 1, assets.display, C.INK, ink = true)
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
        private const val BOX_ROW_GAP = 24f
        private const val BANNER_SIZE = 74f
        /** The prize frame: its width, and how far below the board's lower edge the middle of its purple field lies. */
        private const val REWARD_W = 580f
        private const val REWARD_DROP = 10f
        /** What the code screen keeps free above the board's lower edge for the prize frame's coins. */
        private const val CODE_FRAME_ROOM = 150f
        /** The frame's purple field as shares of the picture (left, top, right, bottom), measured on the picture. */
        private const val FIELD_L = 0.1231f
        private const val FIELD_T = 0.4457f
        private const val FIELD_R = 0.8748f
        private const val FIELD_B = 0.8403f
        /** A layout width wider than the title ever is, so the title is measured and drawn on one line at the size chosen. */
        private const val BANNER_MEASURE_W = 1000
        private const val SUBTITLE_LINES = 3
    }
}
