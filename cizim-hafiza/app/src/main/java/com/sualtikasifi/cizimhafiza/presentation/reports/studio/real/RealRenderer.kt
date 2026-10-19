package com.sualtikasifi.cizimhafiza.presentation.reports.studio.real

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.RectF
import android.text.TextPaint
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.ClipSound
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.Ease
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StrokeReplay
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioAssets
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioColors as C
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.TextPainter
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.VoicedVideo
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.guess.GuessTimeline
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.guess.SpokenLine
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MascotEmotion
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MascotSprites
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MouthState
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.mouthFor
import java.io.File
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Draws "Gerçek Olsaydı" in 1080×1920 out of the game's own painted pieces: the workshop wall with its hanging
 * sign, the parchment board each drawing is pinned on, the speech board under it and Karalak standing in front.
 * The first half is the run of drawings, each with its word on the sign and its joke below. Then the twist:
 * each chosen drawing is wiped away, left to right, by its clip filling the whole frame (the clip starts from the
 * realistic picture and brings it to life). Only clips are in the video; the pictures are shared separately.
 * The original drawing stays pinned in a corner, and the clip is labelled as made with AI.
 */
class RealRenderer(
    private val assets: StudioAssets,
    private val sprites: MascotSprites,
    private val script: RealScript,
    private val drawings: Map<String, RealDrawing>,
    /** Clips by drawing id. */
    private val clips: Map<String, ClipFrames>,
    private val clipFiles: Map<String, File>,
    /** How loud the clips' own sound is; 0 leaves it out. */
    private val clipGain: Float,
    private val voice: Map<Int, FloatArray>?
) : VoicedVideo {
    val events: List<RealEvent> = RealTimeline.build(script) { i, l -> spokenMs(i, l) }
    override val totalMs: Long = RealTimeline.totalMs(events)
    override val voiceStarts: List<Pair<Int, Long>> = events.filter { it.line != null }.map { it.lineIndex to it.lineAtMs }
    private val shown = script.shownReveals
    override val sounds: List<ClipSound> = if (clipGain <= 0f) emptyList() else events.filter { it.phase == RealPhase.Reveal && it.clipMs > 0 }.mapNotNull { e ->
        clipFiles[shown.getOrNull(e.index)?.sampleId]?.let { ClipSound(it, e.clipAtMs, e.clipMs, clipGain) }
    }

    private val text = TextPainter()
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        colorFilter = PorterDuffColorFilter(0x66000000, PorterDuff.Mode.SRC_IN)
    }
    private val ink = StrokeReplay.inkPaint(BROWN_INK)
    private val replays = HashMap<String, StrokeReplay>()
    private val src = Rect()
    private val glyph = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val glyphLine = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeJoin = Paint.Join.ROUND }

    // the pieces
    private val wall = assets.picture(R.drawable.bg_offline)
    private val hall = assets.picture(R.drawable.bg_friends)
    private val paper = assets.picture(R.drawable.offline_paper)
    private val board = assets.picture(R.drawable.rp_card_big)
    private val tag = assets.picture(R.drawable.rp_label)
    private val ribbon = assets.picture(R.drawable.fr_ribbon)
    private val signPiece = assets.picture(R.drawable.fr_sign)
    private val button = assets.picture(R.drawable.join_btn)

    // where things sit
    private val paperBox = RectF(125f, 400f, 955f, 1480f)
    /** The parchment inside the paper's frame (the frame and its corner leaves take the rest). */
    private val parchment = inner(paperBox, 70f / 1099f, 80f / 1431f, 1030f / 1099f, 1300f / 1431f)
    private val boardBox = RectF(40f, 1495f, 1040f, 1818f)
    private val mascotBox = RectF(10f, 1390f, 440f, 1820f)

    private fun inner(box: RectF, l: Float, t: Float, r: Float, b: Float): RectF =
        RectF(box.left + box.width() * l, box.top + box.height() * t, box.left + box.width() * r, box.top + box.height() * b)

    private fun spokenMs(index: Int, l: SpokenLine): Long =
        voice?.get(index)?.let { it.size * 1000L / FPS } ?: GuessTimeline.estimateMs(l.text)

    private fun wordOf(id: String): String = drawings[id]?.word?.takeIf { it.isNotBlank() } ?: script.word
    private fun secondsOf(id: String): Int = drawings[id]?.seconds?.takeIf { it > 0 } ?: script.drawSeconds

    private fun replay(id: String): StrokeReplay? = drawings[id]?.let { d -> replays.getOrPut(id) { StrokeReplay(d.strokes) } }

    override fun draw(canvas: Canvas, t: Long) {
        val e = RealTimeline.at(events, t)
        val spoken = e.line?.let { spokenMs(e.lineIndex, it) } ?: 0L
        val speaking = e.line != null && t >= e.lineAtMs && t - e.lineAtMs < spoken
        val level = level(e, t, speaking)
        if (e.phase == RealPhase.Reveal && t >= e.photoAtMs - RealTimeline.WIPE) {
            val wipe = ((t - (e.photoAtMs - RealTimeline.WIPE)).toFloat() / RealTimeline.WIPE).coerceIn(0f, 1f)
            val x = W * Ease.out(wipe)
            if (wipe < 1f) drawStandard(canvas, e, t, speaking, level, spoken)
            canvas.save()
            canvas.clipRect(0f, 0f, x, H.toFloat())
            drawFullBleed(canvas, e, t, speaking, level, spoken, overlays = wipe >= 1f)
            canvas.restore()
            if (wipe < 1f) {
                fill.color = C.WHITE
                canvas.drawRect(x - 6f, 0f, x + 6f, H.toFloat(), fill)
                fill.color = C.ORANGE
                canvas.drawRect(x + 6f, 0f, x + 12f, H.toFloat(), fill)
            }
            return
        }
        drawStandard(canvas, e, t, speaking, level, spoken)
    }

    private fun level(e: RealEvent, t: Long, speaking: Boolean): Float {
        if (!speaking) return 0f
        val local = t - e.lineAtMs
        val env = voice?.get(e.lineIndex)
        if (env != null) {
            val i = (local * FPS / 1000).toInt()
            return if (i in env.indices) env[i] else 0f
        }
        return (0.55f + 0.45f * sin(local / 55.0).toFloat()).coerceIn(0f, 1f) * if ((local / 260) % 4 == 3L) 0.2f else 1f
    }

    // ---- the workshop: hook, jokes, bridge, the moment before each reveal

    private fun drawStandard(canvas: Canvas, e: RealEvent, t: Long, speaking: Boolean, level: Float, spoken: Long) {
        if (e.phase == RealPhase.Outro) {
            drawEndCard(canvas, e, t, speaking, level, spoken)
            return
        }
        scene(canvas, wall)
        sign(canvas, signTitle(e))
        piece(canvas, paper, paperBox, shadow = true)
        when (e.phase) {
            RealPhase.Hook -> drawHook(canvas, e, t)
            RealPhase.Roast -> drawRoast(canvas, e, t)
            RealPhase.Bridge -> drawBridge(canvas, e, t)
            RealPhase.Reveal -> drawBeforeReveal(canvas, e, t)
            RealPhase.Outro -> Unit
        }
        chips(canvas, e)
        val said = when {
            e.phase == RealPhase.Reveal -> "Gerçek olsaydı…"
            else -> e.line?.text.orEmpty()
        }
        val progress = if (e.phase == RealPhase.Reveal || e.line == null) 1f else ((t - e.lineAtMs).toFloat() / spoken.coerceAtLeast(1)).coerceIn(0f, 1f)
        speech(canvas, said, progress, karaoke = e.phase != RealPhase.Reveal)
        mascot(canvas, mascotBox, e.line?.emotion ?: defaultEmotion(e), speaking, level, t)
        brand(canvas)
        progressBar(canvas, t)
    }

    private fun defaultEmotion(e: RealEvent) = when (e.phase) {
        RealPhase.Roast -> MascotEmotion.Laughing
        RealPhase.Reveal -> MascotEmotion.Shocked
        else -> MascotEmotion.Thinking
    }

    /** What the hanging sign says: the word being drawn, or what this part of the video is about. */
    private fun signTitle(e: RealEvent): String = when (e.phase) {
        RealPhase.Roast -> script.roasts.getOrNull(e.index)?.let { wordOf(it.sampleId) }?.takeIf { it.isNotBlank() } ?: "ÇİZİM"
        RealPhase.Bridge, RealPhase.Reveal -> "GERÇEK OLSAYDI?"
        else -> script.word.ifBlank { "TÜRKİYE ÇİZİYOR" }
    }.uppercase(TR)

    /** The wall's own hanging sign (painted into the picture): its title, in the game's lettering. */
    private fun sign(canvas: Canvas, title: String) {
        lettered(canvas, title, 540f, SIGN_Y, 640f, 92f, C.WHITE, SIGN_OUTLINE, 12f)
    }

    /** The tags on the paper's top corners: how long the drawer had, and which drawing this is. */
    private fun chips(canvas: Canvas, e: RealEvent) {
        val n = script.roasts.size
        var left: String? = null
        var right: String? = null
        when (e.phase) {
            RealPhase.Hook -> left = "$n çizim"
            RealPhase.Roast -> {
                left = "⏱ ${script.roasts.getOrNull(e.index)?.let { secondsOf(it.sampleId) } ?: script.drawSeconds} sn"
                right = "${e.index + 1} / $n"
            }
            RealPhase.Reveal -> right = "${e.index + 1} / ${shown.size}"
            else -> Unit
        }
        val w = 210f
        val h = w * 111f / 196f
        left?.let { woodTag(canvas, it, RectF(paperBox.left - 14f, paperBox.top - 22f, paperBox.left - 14f + w, paperBox.top - 22f + h)) }
        right?.let { woodTag(canvas, it, RectF(paperBox.right + 14f - w, paperBox.top - 22f, paperBox.right + 14f, paperBox.top - 22f + h)) }
    }

    private fun woodTag(canvas: Canvas, label: String, box: RectF) {
        piece(canvas, tag, box, shadow = true)
        lettered(canvas, label, box.centerX(), box.centerY(), box.width() * 0.78f, 40f, C.WHITE, 0xFF4A2410.toInt(), 7f)
    }

    /** The speech board under the paper, its words in the ink the game uses on parchment. */
    private fun speech(canvas: Canvas, said: String, progress: Float, karaoke: Boolean) {
        piece(canvas, board, boardBox, shadow = true)
        val cx = (mascotBox.right - 20f + boardBox.right - 70f) / 2f
        val width = (boardBox.right - 70f - (mascotBox.right - 20f)).toInt()
        val cy = boardBox.centerY() + 4f
        if (said.isBlank()) return
        if (karaoke) text.karaoke(canvas, said, progress, cx, cy, width, 52f, 3, assets.display, BROWN_INK, 0xFFB89B7A.toInt())
        else text.draw(canvas, said, cx, cy, width, 58f, 2, assets.display, C.ORANGE_DARK)
    }

    private fun progressBar(canvas: Canvas, t: Long) {
        val r = RectF(60f, 1846f, 1020f, 1864f)
        fill.color = 0x66000000
        canvas.drawRoundRect(r, 9f, 9f, fill)
        fill.color = C.ORANGE
        canvas.drawRoundRect(RectF(r.left, r.top, r.left + r.width() * (t.toFloat() / totalMs).coerceIn(0f, 1f), r.bottom), 9f, 9f, fill)
    }

    private fun brand(canvas: Canvas) {
        lettered(canvas, "karalak.com · Google Play: Karalak", 540f, 1892f, 900f, 30f, C.WHITE, 0xFF3A1E0A.toInt(), 6f)
    }

    /** The opening: every drawing of the run on the board at once, as large as they can be, each drawing itself in turn. */
    private fun drawHook(canvas: Canvas, e: RealEvent, t: Long) {
        val local = t - e.startMs
        val items = script.roasts.take(6)
        if (items.isEmpty()) return
        val area = RectF(parchment.left + 24f, parchment.top + 24f, parchment.right - 24f, parchment.bottom - 24f)
        val grid = squareGrid(items.size, area)
        items.forEachIndexed { i, r ->
            val f = ((local - i * 260L).toFloat() / 650f).coerceIn(0f, 1f)
            if (f <= 0f) return@forEachIndexed
            replay(r.sampleId)?.draw(canvas, grid[i], f, ink, 0.03f)
        }
    }

    /** [n] equal squares laid out in rows inside [area], as big as they will go, every row centred and the whole grid centred. */
    private fun squareGrid(n: Int, area: RectF, gap: Float = 18f): List<RectF> {
        var best = 1
        var bestSize = 0f
        for (cols in 1..n) {
            val rows = ceil(n / cols.toFloat()).toInt()
            val size = min((area.width() - gap * (cols - 1)) / cols, (area.height() - gap * (rows - 1)) / rows)
            if (size > bestSize) { bestSize = size; best = cols }
        }
        val rows = ceil(n / best.toFloat()).toInt()
        val top = area.centerY() - (rows * bestSize + (rows - 1) * gap) / 2f
        return List(n) { i ->
            val row = i / best
            val inRow = min(best, n - row * best)
            val left = area.centerX() - (inRow * bestSize + (inRow - 1) * gap) / 2f
            val x = left + (i % best) * (bestSize + gap)
            val y = top + row * (bestSize + gap)
            RectF(x, y, x + bestSize, y + bestSize)
        }
    }

    /** One drawing at a time: it slides in and draws itself at a hurried pace, centred on the board, then Karalak's joke. */
    private fun drawRoast(canvas: Canvas, e: RealEvent, t: Long) {
        val roast = script.roasts.getOrNull(e.index) ?: return
        val local = t - e.startMs
        val slide = (1f - Ease.out(min(1f, local / 300f))) * 220f
        canvas.save()
        canvas.translate(slide, 0f)
        replay(roast.sampleId)?.draw(canvas, doodleBox(), min(1f, local / 900f), ink)
        canvas.restore()
        dots(canvas, script.roasts.size, e.index)
    }

    /** Where a single drawing sits on the parchment: centred, as large as the board allows. */
    private fun doodleBox() = RectF(parchment.left + 14f, parchment.top + 14f, parchment.right - 14f, parchment.bottom - 14f)

    /** The page dots, on the paper's wooden foot. */
    private fun dots(canvas: Canvas, n: Int, current: Int) {
        val y = paperBox.top + paperBox.height() * (1352f / 1431f)
        val gap = 42f
        val startX = 540f - (n - 1) * gap / 2f
        for (i in 0 until n) {
            fill.color = if (i == current) C.ORANGE else 0xFFF6E3BC.toInt()
            canvas.drawCircle(startX + i * gap, y, if (i == current) 15f else 11f, fill)
        }
    }

    /** "What if they were real?": the drawings about to turn real, side by side under a question mark. */
    private fun drawBridge(canvas: Canvas, e: RealEvent, t: Long) {
        val local = t - e.startMs
        val pop = Ease.backOut(min(1f, local / 450f))
        val cy = parchment.top + 190f
        canvas.save()
        canvas.scale(pop, pop, parchment.centerX(), cy)
        lettered(canvas, "?", parchment.centerX(), cy, 300f, 300f, C.ORANGE, 0xFF8A3A00.toInt(), 16f)
        canvas.restore()
        val n = shown.size
        if (n == 0) return
        val gap = 22f
        val size = min(230f, (parchment.width() - 60f - (n - 1) * gap) / n)
        val startX = parchment.centerX() - (n * size + (n - 1) * gap) / 2f
        shown.forEachIndexed { i, r ->
            val a = Ease.out(((local - 300L - i * 180L).toFloat() / 400f).coerceIn(0f, 1f))
            if (a <= 0f) return@forEachIndexed
            val x = startX + i * (size + gap)
            val y = parchment.centerY() + 70f + (1f - a) * 40f
            val box = RectF(x, y, x + size, y + size)
            fill.color = 0xFFFFF8E6.toInt()
            canvas.drawRoundRect(box, 24f, 24f, fill)
            replay(r.sampleId)?.draw(canvas, box, 1f, ink, 0.03f)
            val wobble = sin((t + i * 300) / 180.0).toFloat()
            lettered(canvas, "✦", box.right - 14f, box.top + 10f + wobble * 6f, 60f, 44f, C.ORANGE, 0xFF8A3A00.toInt(), 5f)
        }
    }

    /** The drawing held for a beat before its clip wipes over it. */
    private fun drawBeforeReveal(canvas: Canvas, e: RealEvent, t: Long) {
        val r = shown.getOrNull(e.index) ?: return
        val pop = Ease.out(min(1f, (t - e.startMs) / 400f))
        canvas.save()
        canvas.scale(0.94f + 0.06f * pop, 0.94f + 0.06f * pop, parchment.centerX(), parchment.centerY())
        replay(r.sampleId)?.draw(canvas, doodleBox(), 1f, ink)
        canvas.restore()
        dots(canvas, shown.size, e.index)
    }

    // ---- the reveal: the real thing, edge to edge

    private fun drawFullBleed(canvas: Canvas, e: RealEvent, t: Long, speaking: Boolean, level: Float, spoken: Long, overlays: Boolean) {
        val r = shown.getOrNull(e.index) ?: return
        canvas.drawColor(C.INK)
        val full = RectF(0f, 0f, W.toFloat(), H.toFloat())
        // the clip plays from the start of the wipe; past its end the last frame stays
        val clipFrame = clips[r.sampleId]?.frameAt((t - e.clipAtMs).coerceIn(0L, e.clipMs))
        if (clipFrame != null) {
            cover(canvas, clipFrame, full, 1f)
        } else {
            scene(canvas, wall)
            piece(canvas, paper, paperBox, shadow = true)
            replay(r.sampleId)?.draw(canvas, doodleBox(), 1f, ink)
        }
        if (!overlays) return
        val a = Ease.out(min(1f, (t - e.photoAtMs) / 350f))
        // the AI label on a ribbon, top left; the original drawing pinned on a small paper, top right
        ribbonLabel(canvas, "Yapay zekâ ile üretildi", a)
        pinned(canvas, r, a)
        val said = e.line?.text.orEmpty()
        if (said.isNotBlank() && t - e.lineAtMs < spoken + 1_200L) {
            val p = ((t - e.lineAtMs).toFloat() / spoken.coerceAtLeast(1)).coerceIn(0f, 1f)
            speech(canvas, said, p, karaoke = true)
        } else {
            piece(canvas, board, boardBox, shadow = true)
        }
        mascot(canvas, mascotBox, e.line?.emotion ?: MascotEmotion.Shocked, speaking, level, t, alpha = (a * 255).toInt())
        progressBar(canvas, t)
    }

    private fun ribbonLabel(canvas: Canvas, label: String, a: Float) {
        val w = 560f
        val box = RectF(20f, 120f, 20f + w, 120f + w * 248f / 875f)
        canvas.save()
        canvas.translate(-(1f - a) * 300f, 0f)
        piece(canvas, ribbon, box, shadow = true)
        lettered(canvas, label, box.centerX(), box.centerY() + 2f, box.width() * 0.66f, 34f, C.WHITE, 0xFF4A2410.toInt(), 7f)
        canvas.restore()
    }

    /** The original drawing on a small parchment, pinned in the corner, so the two can be compared. */
    private fun pinned(canvas: Canvas, r: RealReveal, a: Float) {
        val pop = Ease.backOut(a)
        val box = RectF(770f, 110f, 770f + 270f, 110f + 270f * 1431f / 1099f)
        canvas.save()
        canvas.rotate(5f, box.centerX(), box.centerY())
        canvas.scale(pop, pop, box.centerX(), box.centerY())
        piece(canvas, paper, box, shadow = true)
        val p = inner(box, 70f / 1099f, 80f / 1431f, 1030f / 1099f, 1300f / 1431f)
        replay(r.sampleId)?.draw(canvas, RectF(p.left + 6f, p.top + 6f, p.right - 6f, p.bottom - 6f), 1f, ink, 0.03f)
        canvas.restore()
    }

    // ---- the closing card

    private fun drawEndCard(canvas: Canvas, e: RealEvent, t: Long, speaking: Boolean, level: Float, spoken: Long) {
        val a = Ease.out(min(1f, e.progress(t) * 4f))
        scene(canvas, hall)
        val signBox = RectF(110f, 120f, 970f, 120f + 860f * 393f / 1067f)
        piece(canvas, signPiece, signBox, shadow = true)
        lettered(canvas, "SEN DE ÇİZ!", signBox.centerX(), signBox.top + signBox.height() * 0.6f, signBox.width() * 0.6f, 96f, C.WHITE, SIGN_OUTLINE, 12f)
        if (a < 0.6f) return
        canvas.save()
        canvas.translate(0f, (1f - a) * 60f)
        // the mascot picture ends at the chest, so the button is drawn over its foot
        mascot(canvas, RectF(190f, 520f, 890f, 1235f), e.line?.emotion ?: MascotEmotion.Point, speaking, level, t)
        val cta = RectF(110f, 1160f, 970f, 1160f + 860f * 355f / 1115f)
        piece(canvas, button, cta, shadow = true)
        lettered(canvas, "karalak.com'da oyna", cta.centerX(), cta.centerY() - 4f, cta.width() * 0.66f, 62f, C.WHITE, 0xFF8A3A00.toInt(), 10f)
        canvas.restore()
        val said = e.line?.text.orEmpty()
        val p = ((t - e.lineAtMs).toFloat() / spoken.coerceAtLeast(1)).coerceIn(0f, 1f)
        piece(canvas, board, RectF(60f, 1470f, 1020f, 1470f + 960f * 391f / 1208f), shadow = true)
        text.karaoke(canvas, said, p, 540f, 1470f + 960f * 391f / 1208f / 2f + 4f, 760, 50f, 3, assets.display, BROWN_INK, 0xFFB89B7A.toInt())
        lettered(canvas, "Android: Google Play'de \"Karalak\"", 540f, 1860f, 900f, 38f, C.WHITE, 0xFF3A1E0A.toInt(), 7f)
    }

    // ---- drawing helpers

    /** A painted scene covering the whole frame, kept to the top so its hanging sign lands where [sign] writes. */
    private fun scene(canvas: Canvas, bmp: Bitmap?) {
        if (bmp == null || bmp.isRecycled) { canvas.drawColor(C.CREAM); return }
        val scale = W.toFloat() / bmp.width
        val h = bmp.height * scale
        val top = min(0f, max(H - h, -SCENE_SHIFT))
        src.set(0, 0, bmp.width, bmp.height)
        canvas.drawBitmap(bmp, src, RectF(0f, top, W.toFloat(), top + h), fill)
    }

    /** [bmp] fitted into [box] (never stretched out of its own shape), with a soft drop shadow when asked. */
    private fun piece(canvas: Canvas, bmp: Bitmap?, box: RectF, shadow: Boolean) {
        if (bmp == null || bmp.isRecycled) return
        if (shadow) canvas.drawBitmap(bmp, null, RectF(box.left + 6f, box.top + 14f, box.right + 6f, box.bottom + 14f), shadowPaint)
        canvas.drawBitmap(bmp, null, box, fill)
    }

    /** One line of the game's lettering: [fillColor] with a rounded [outline] around it, shrunk to fit [maxWidth]. */
    private fun lettered(canvas: Canvas, label: String, cx: Float, cy: Float, maxWidth: Float, size: Float, fillColor: Int, outline: Int, outlineWidth: Float) {
        if (label.isEmpty()) return
        glyph.typeface = assets.display
        glyphLine.typeface = assets.display
        var s = size
        glyph.textSize = s
        while (glyph.measureText(label) > maxWidth && s > size * 0.45f) {
            s *= 0.94f
            glyph.textSize = s
        }
        glyphLine.textSize = s
        glyphLine.strokeWidth = outlineWidth * (s / size)
        glyphLine.color = outline
        glyph.color = fillColor
        val x = cx - glyph.measureText(label) / 2f
        val y = cy - (glyph.ascent() + glyph.descent()) / 2f
        canvas.drawText(label, x, y, glyphLine)
        canvas.drawText(label, x, y, glyph)
    }

    private fun mascot(canvas: Canvas, box: RectF, emotion: MascotEmotion, speaking: Boolean, level: Float, t: Long, alpha: Int = 255) {
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
        fill.alpha = alpha
        canvas.save()
        canvas.scale(breathe, breathe, dst.centerX(), dst.bottom)
        canvas.drawBitmap(bmp, null, dst, fill)
        canvas.restore()
        fill.alpha = 255
    }

    /** Fills [dst] with [bmp], cropped to its shape, [zoom] times closer than a plain fit. */
    private fun cover(canvas: Canvas, bmp: Bitmap, dst: RectF, zoom: Float) {
        if (bmp.isRecycled) return
        val aspect = dst.width() / dst.height()
        var sw = bmp.width.toFloat()
        var sh = sw / aspect
        if (sh > bmp.height) {
            sh = bmp.height.toFloat()
            sw = sh * aspect
        }
        sw /= zoom
        sh /= zoom
        val sx = (bmp.width - sw) / 2f
        val sy = (bmp.height - sh) / 2f
        src.set(sx.toInt(), sy.toInt(), (sx + sw).toInt(), (sy + sh).toInt())
        canvas.drawBitmap(bmp, src, dst, fill)
    }

    companion object {
        const val W = 1080
        const val H = 1920
        const val FPS = 30
        /** How far the workshop picture is pushed up, so its sign sits high and the board under it has room. */
        private const val SCENE_SHIFT = 130f
        /** The middle of the hanging sign painted into the workshop picture, once pushed up. */
        private const val SIGN_Y = 277f
        private const val BROWN_INK = 0xFF3D2314.toInt()
        private val SIGN_OUTLINE = 0xFF5A2E00.toInt()
        private val TR = Locale("tr")
    }
}
