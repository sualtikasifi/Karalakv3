package com.sualtikasifi.cizimhafiza.presentation.reports.studio.real

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.ClipSound
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
import java.io.File
import java.util.Locale
import kotlin.math.min
import kotlin.math.sin

/**
 * Draws "Gerçek Olsaydı" in 1080×1920. The first half looks like the other formats — Karalak and
 * the word at the top, subtitles, a paper card with each quick drawing and its joke. Then the
 * twist: each chosen drawing is wiped away, left to right, by its realistic picture filling the
 * whole frame, which then comes alive as a clip. Karalak watches from a small tile, the original
 * drawing pinned beside it, and the picture is labelled as made with AI.
 */
class RealRenderer(
    private val assets: StudioAssets,
    private val sprites: MascotSprites,
    private val script: RealScript,
    private val drawings: Map<String, RealDrawing>,
    /** Realistic pictures and clips by drawing id. */
    private val photos: Map<String, Bitmap>,
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
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val ink = StrokeReplay.inkPaint()
    private val replays = HashMap<String, StrokeReplay>()
    private val shade = Paint().apply {
        shader = LinearGradient(0f, H * 0.62f, 0f, H.toFloat(), 0x00000000, 0xD0000000.toInt(), Shader.TileMode.CLAMP)
    }
    private val src = Rect()

    private val mascotTile = RectF(40f, 150f, 400f, 510f)
    private val titleBox = RectF(430f, 150f, 1040f, 510f)
    private val card = RectF(130f, 720f, 950f, 1400f)
    private val cam = RectF(40f, 110f, 330f, 400f)
    private val pin = RectF(760f, 120f, 1030f, 390f)

    private fun spokenMs(index: Int, l: com.sualtikasifi.cizimhafiza.presentation.reports.studio.guess.SpokenLine): Long =
        voice?.get(index)?.let { it.size * 1000L / FPS } ?: GuessTimeline.estimateMs(l.text)

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

    // ---- the cream layout: hook, jokes, bridge, the moment before each reveal

    private fun drawStandard(canvas: Canvas, e: RealEvent, t: Long, speaking: Boolean, level: Float, spoken: Long) {
        canvas.drawColor(C.CREAM)
        drawMascotTile(canvas, e, speaking, level, t)
        drawTitle(canvas, e)
        if (e.phase != RealPhase.Outro && e.phase != RealPhase.Reveal) {
            e.line?.let { l ->
                val p = ((t - e.lineAtMs).toFloat() / spoken.coerceAtLeast(1)).coerceIn(0f, 1f)
                text.karaoke(canvas, l.text, p, 540f, 615f, 980, 50f, 2, assets.display, C.INK, C.FAINT)
            }
        }
        when (e.phase) {
            RealPhase.Hook -> drawHook(canvas, e, t)
            RealPhase.Roast -> drawRoast(canvas, e, t)
            RealPhase.Bridge -> drawBridge(canvas, e, t)
            RealPhase.Reveal -> drawBeforeReveal(canvas, e, t)
            RealPhase.Outro -> Unit
        }
        if (e.phase != RealPhase.Outro) {
            text.draw(canvas, "karalak.com · Google Play: Karalak", 540f, 1640f, 900, 32f, 1, assets.body, C.MUTED)
            progress(canvas, t)
        } else {
            drawEndCard(canvas, e, t, speaking, level, spoken)
        }
    }

    private fun progress(canvas: Canvas, t: Long) {
        fill.color = C.CARD_EDGE
        canvas.drawRect(0f, H - 10f, W.toFloat(), H.toFloat(), fill)
        fill.color = C.ORANGE
        canvas.drawRect(0f, H - 10f, W * (t.toFloat() / totalMs).coerceIn(0f, 1f), H.toFloat(), fill)
    }

    private fun drawMascotTile(canvas: Canvas, e: RealEvent, speaking: Boolean, level: Float, t: Long, r: RectF = mascotTile, alpha: Int = 255) {
        fill.alpha = alpha
        fill.color = C.SHADOW
        canvas.drawRoundRect(RectF(r.left, r.top + 10f, r.right, r.bottom + 10f), 40f, 40f, fill)
        fill.color = 0xFFFFE8CC.toInt()
        fill.alpha = alpha
        canvas.drawRoundRect(r, 40f, 40f, fill)
        line.color = if (speaking) C.ORANGE else C.CARD_EDGE
        line.strokeWidth = if (speaking) 9f else 4f
        line.alpha = alpha
        canvas.drawRoundRect(r, 40f, 40f, line)
        line.alpha = 255
        canvas.save()
        canvas.clipRect(r.left + 4f, r.top + 4f, r.right - 4f, r.bottom - 4f)
        val emotion = e.line?.emotion ?: when (e.phase) {
            RealPhase.Roast -> MascotEmotion.Laughing
            RealPhase.Reveal -> MascotEmotion.Shocked
            else -> MascotEmotion.Thinking
        }
        drawMascot(canvas, RectF(r.left + 16f, r.top + 30f, r.right - 16f, r.bottom), emotion, speaking, level, t)
        canvas.restore()
        fill.alpha = 255
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

    private fun drawTitle(canvas: Canvas, e: RealEvent) {
        val cx = titleBox.centerX()
        val w = titleBox.width().toInt()
        val chip: String
        if (e.phase == RealPhase.Bridge || e.phase == RealPhase.Reveal) {
            text.draw(canvas, "GERÇEK", cx, titleBox.top + 90f, w, 96f, 1, assets.display, C.ORANGE_DARK)
            text.draw(canvas, "OLSAYDI?", cx, titleBox.top + 200f, w, 96f, 1, assets.display, C.INK)
            chip = "Yapay zekâ ile üretildi"
        } else {
            text.draw(canvas, script.word.uppercase(TR), cx, titleBox.top + 62f, w, 78f, 1, assets.display, C.ORANGE_DARK)
            text.draw(canvas, "${script.drawSeconds} saniyede", cx, titleBox.top + 172f, w, 92f, 1, assets.display, C.INK)
            text.draw(canvas, "Türkiye böyle çizdi", cx, titleBox.top + 262f, w, 34f, 1, assets.body, C.MUTED)
            chip = if (e.phase == RealPhase.Roast) "${e.index + 1} / ${script.roasts.size}" else "${script.roasts.size} çizim"
        }
        val p = text.paint(30f, assets.body, C.INK)
        val cw = p.measureText(chip) + 48f
        val box = RectF(cx - cw / 2f, titleBox.top + 300f, cx + cw / 2f, titleBox.top + 356f)
        fill.color = C.CREAM_DEEP
        canvas.drawRoundRect(box, 28f, 28f, fill)
        canvas.drawText(chip, box.left + 24f, box.centerY() + 10f, p)
    }

    private fun paper(canvas: Canvas, r: RectF = card) {
        fill.color = C.SHADOW
        canvas.drawRoundRect(RectF(r.left, r.top + 12f, r.right, r.bottom + 12f), 44f, 44f, fill)
        fill.color = C.CARD
        canvas.drawRoundRect(r, 44f, 44f, fill)
        line.color = C.CARD_EDGE; line.strokeWidth = 4f
        canvas.drawRoundRect(r, 44f, 44f, line)
    }

    /** The opening: every drawing of the run on one sheet, each drawing itself in turn. */
    private fun drawHook(canvas: Canvas, e: RealEvent, t: Long) {
        paper(canvas)
        val local = t - e.startMs
        val items = script.roasts.take(6)
        val cols = if (items.size <= 4) 2 else 3
        val rows = (items.size + cols - 1) / cols
        val cw = (card.width() - 60f) / cols
        val ch = (card.height() - 60f) / rows.coerceAtLeast(1)
        items.forEachIndexed { i, r ->
            val f = ((local - i * 260L).toFloat() / 650f).coerceIn(0f, 1f)
            if (f <= 0f) return@forEachIndexed
            val c = i % cols
            val row = i / cols
            val box = RectF(card.left + 30f + c * cw, card.top + 30f + row * ch, card.left + 30f + (c + 1) * cw, card.top + 30f + (row + 1) * ch)
            replay(r.sampleId)?.draw(canvas, RectF(box.left + 8f, box.top + 8f, box.right - 8f, box.bottom - 8f), f, ink, 0.03f)
        }
    }

    /** One drawing at a time: it slides in and draws itself at a hurried pace, then Karalak's joke. */
    private fun drawRoast(canvas: Canvas, e: RealEvent, t: Long) {
        val roast = script.roasts.getOrNull(e.index) ?: return
        val local = t - e.startMs
        val slide = (1f - Ease.out(min(1f, local / 300f))) * 260f
        canvas.save()
        canvas.translate(slide, 0f)
        paper(canvas)
        replay(roast.sampleId)?.draw(canvas, RectF(card.left, card.top + 50f, card.right, card.bottom - 30f), min(1f, local / 900f), ink)
        // the clock the drawer had
        val chip = "⏱ ${script.drawSeconds} sn"
        val p = text.paint(32f, assets.body, C.INK)
        val w = p.measureText(chip) + 44f
        val box = RectF(card.left + 28f, card.top + 28f, card.left + 28f + w, card.top + 86f)
        fill.color = C.CREAM_DEEP
        canvas.drawRoundRect(box, 29f, 29f, fill)
        canvas.drawText(chip, box.left + 22f, box.centerY() + 11f, p)
        canvas.restore()
        dots(canvas, script.roasts.size, e.index, card.bottom + 60f)
    }

    private fun dots(canvas: Canvas, n: Int, current: Int, y: Float) {
        val gap = 34f
        val startX = 540f - (n - 1) * gap / 2f
        for (i in 0 until n) {
            fill.color = if (i == current) C.ORANGE else C.FAINT
            canvas.drawCircle(startX + i * gap, y, if (i == current) 11f else 8f, fill)
        }
    }

    /** "What if they were real?": the drawings about to turn real, side by side. */
    private fun drawBridge(canvas: Canvas, e: RealEvent, t: Long) {
        paper(canvas)
        val local = t - e.startMs
        val pop = Ease.backOut(min(1f, local / 450f))
        canvas.save()
        canvas.scale(pop, pop, card.centerX(), card.top + 190f)
        text.draw(canvas, "?", card.centerX(), card.top + 190f, 300, 230f, 1, assets.display, C.ORANGE)
        canvas.restore()
        val n = shown.size
        val size = min(230f, (card.width() - 80f - (n - 1) * 24f) / n.coerceAtLeast(1))
        val startX = card.centerX() - (n * size + (n - 1) * 24f) / 2f
        shown.forEachIndexed { i, r ->
            val a = Ease.out(((local - 300L - i * 180L).toFloat() / 400f).coerceIn(0f, 1f))
            if (a <= 0f) return@forEachIndexed
            val x = startX + i * (size + 24f)
            val y = card.top + 400f + (1f - a) * 40f
            val box = RectF(x, y, x + size, y + size)
            fill.color = C.CREAM
            canvas.drawRoundRect(box, 24f, 24f, fill)
            replay(r.sampleId)?.draw(canvas, box, 1f, ink, 0.03f)
            val wobble = sin((t + i * 300) / 180.0).toFloat()
            text.draw(canvas, "✦", box.right - 14f, box.top + 10f + wobble * 6f, 60, 44f, 1, assets.display, C.ORANGE)
        }
    }

    /** The drawing held for a beat before its picture wipes over it. */
    private fun drawBeforeReveal(canvas: Canvas, e: RealEvent, t: Long) {
        val r = shown.getOrNull(e.index) ?: return
        paper(canvas)
        replay(r.sampleId)?.draw(canvas, RectF(card.left, card.top + 30f, card.right, card.bottom - 30f), 1f, ink)
        val a = Ease.out(min(1f, (t - e.startMs) / 400f))
        text.draw(canvas, "Gerçek olsaydı…", 540f, 615f, 980, 56f, 1, assets.display, C.ORANGE_DARK, alpha = a)
        dots(canvas, shown.size, e.index, card.bottom + 60f)
    }

    // ---- the reveal: the real thing, edge to edge

    private fun drawFullBleed(canvas: Canvas, e: RealEvent, t: Long, speaking: Boolean, level: Float, spoken: Long, overlays: Boolean) {
        val r = shown.getOrNull(e.index) ?: return
        canvas.drawColor(C.INK)
        val clipFrame = if (e.clipMs > 0 && t >= e.clipAtMs) clips[r.sampleId]?.frameAt(t - e.clipAtMs) else null
        val full = RectF(0f, 0f, W.toFloat(), H.toFloat())
        if (clipFrame != null) {
            cover(canvas, clipFrame, full, 1f)
        } else {
            val photo = photos[r.sampleId]
            if (photo != null) {
                // a slow push-in across the picture's time on screen
                val span = (if (e.clipMs > 0) e.clipAtMs else e.endMs) - e.photoAtMs
                val k = ((t - e.photoAtMs).toFloat() / span.coerceAtLeast(1)).coerceIn(0f, 1f)
                cover(canvas, photo, full, 1f + 0.07f * k)
            } else {
                canvas.drawColor(C.CREAM)
                replay(r.sampleId)?.draw(canvas, RectF(100f, 500f, 980f, 1380f), 1f, ink)
            }
        }
        if (!overlays) return
        val a = Ease.out(min(1f, (t - e.photoAtMs) / 350f))
        canvas.drawRect(0f, H * 0.62f, W.toFloat(), H.toFloat(), shade)
        drawMascotTile(canvas, e, speaking, level, t, cam, (a * 255).toInt())
        drawPin(canvas, r, a)
        e.line?.let { l ->
            val p = ((t - e.lineAtMs).toFloat() / spoken.coerceAtLeast(1)).coerceIn(0f, 1f)
            if (t - e.lineAtMs < spoken + 1_200L) text.karaoke(canvas, l.text, p, 540f, 1560f, 960, 58f, 2, assets.display, C.WHITE, 0x99FFFFFF.toInt())
        }
        val label = "Yapay zekâ ile üretildi"
        val lp = text.paint(28f, assets.body, C.WHITE)
        val lw = lp.measureText(label) + 40f
        val box = RectF(540f - lw / 2f, 1760f, 540f + lw / 2f, 1812f)
        fill.color = 0x66000000
        canvas.drawRoundRect(box, 26f, 26f, fill)
        canvas.drawText(label, box.left + 20f, box.centerY() + 10f, lp)
        progress(canvas, t)
    }

    /** The original drawing pinned in the corner, so the two can be compared. */
    private fun drawPin(canvas: Canvas, r: RealReveal, a: Float) {
        val pop = Ease.backOut(a)
        canvas.save()
        canvas.rotate(5f, pin.centerX(), pin.centerY())
        canvas.scale(pop, pop, pin.centerX(), pin.centerY())
        fill.color = 0x33000000
        canvas.drawRoundRect(RectF(pin.left + 6f, pin.top + 12f, pin.right + 6f, pin.bottom + 62f), 18f, 18f, fill)
        fill.color = C.WHITE
        val frame = RectF(pin.left, pin.top, pin.right, pin.bottom + 50f)
        canvas.drawRoundRect(frame, 18f, 18f, fill)
        replay(r.sampleId)?.draw(canvas, RectF(pin.left + 14f, pin.top + 14f, pin.right - 14f, pin.bottom - 14f), 1f, ink, 0.03f)
        text.draw(canvas, "çizim", pin.centerX(), pin.bottom + 22f, 200, 30f, 1, assets.body, C.MUTED)
        canvas.restore()
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

    // ---- the closing card

    private fun drawEndCard(canvas: Canvas, e: RealEvent, t: Long, speaking: Boolean, level: Float, spoken: Long) {
        val a = Ease.out(min(1f, e.progress(t) * 4f))
        fill.color = C.CREAM
        fill.alpha = (a * 255).toInt()
        canvas.drawRect(0f, 0f, W.toFloat(), H.toFloat(), fill)
        fill.alpha = 255
        if (a < 0.6f) return
        canvas.save()
        canvas.translate(0f, (1f - a) * 60f)
        drawMascot(canvas, RectF(240f, 170f, 840f, 820f), e.line?.emotion ?: MascotEmotion.Point, speaking, level, t)
        text.draw(canvas, "Sen daha iyisini çizer misin?", 540f, 900f, 960, 72f, 2, assets.display, C.INK)
        e.line?.let { l ->
            val prog = ((t - e.lineAtMs).toFloat() / spoken.coerceAtLeast(1)).coerceIn(0f, 1f)
            text.karaoke(canvas, l.text, prog, 540f, 1050f, 960, 48f, 2, assets.display, C.ORANGE_DARK, C.FAINT)
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
