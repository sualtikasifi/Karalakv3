package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.res.ResourcesCompat
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.DrawingStroke
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/** Fonts and the logo, read once per studio visit rather than once per frame. */
class StudioAssets(context: Context) {
    val display: Typeface = runCatching { ResourcesCompat.getFont(context, R.font.baloo2_extrabold) }.getOrNull() ?: Typeface.DEFAULT_BOLD
    val body: Typeface = runCatching { ResourcesCompat.getFont(context, R.font.quicksand_bold) }.getOrNull() ?: Typeface.DEFAULT
    /** The round brand badge; its corners are transparent, so it sits on the cream without a box. */
    val logo: Bitmap? = runCatching { BitmapFactory.decodeResource(context.resources, R.drawable.brand_logo) }.getOrNull()
}

/**
 * Draws one frame of a studio video at a given time, in the video's own 720×1280 pixels.
 *
 * The same code draws the preview on screen and every frame of the file, so what the preview
 * shows is what gets posted. Everything is plain android.graphics: it has to run on the encoder's
 * own thread, outside Compose.
 */
class StudioRenderer(private val assets: StudioAssets, private val spec: VideoSpec) {
    val segments: List<Segment> = StudioTimeline.build(spec.template, spec.drawings.size)
    val totalMs: Long = StudioTimeline.totalMs(segments)

    private val plans = spec.drawings.map { StrokePlan(it.strokes) }

    private val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = INK; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val layouts = HashMap<String, StaticLayout>()

    fun draw(canvas: Canvas, tMs: Long) {
        canvas.drawColor(CREAM)
        val seg = StudioTimeline.at(segments, tMs)
        when (seg.kind) {
            SegmentKind.Hook -> drawHook(canvas, seg.progress(tMs))
            SegmentKind.Outro -> drawOutro(canvas, seg.progress(tMs))
            else -> if (spec.template == StudioTemplate.Versus) drawVersus(canvas, seg, tMs) else drawItem(canvas, seg, tMs)
        }
        // how far along the whole video is, so a viewer knows it is short
        fill.color = TRACK
        canvas.drawRect(0f, H - 10f, W.toFloat(), H.toFloat(), fill)
        fill.color = ORANGE
        canvas.drawRect(0f, H - 10f, W * (tMs.toFloat() / totalMs).coerceIn(0f, 1f), H.toFloat(), fill)
    }

    // ---- the opening line and the closing card

    private fun drawHook(canvas: Canvas, p: Float) {
        drawLogo(canvas, cx = W / 2f, top = 150f, width = 360f, alpha = 1f)
        val appear = ease(min(1f, p * 4f))
        canvas.save()
        canvas.translate(0f, (1f - appear) * 40f)
        drawText(canvas, spec.text.hook, cx = W / 2f, centerY = 720f, width = 600, size = 64f, maxLines = 4, face = assets.display, color = INK, alpha = appear)
        canvas.restore()
        drawText(canvas, "çiz, unut, hatırla", cx = W / 2f, centerY = 960f, width = 600, size = 32f, maxLines = 1, face = assets.body, color = MUTED, alpha = appear)
    }

    private fun drawOutro(canvas: Canvas, p: Float) {
        val appear = ease(min(1f, p * 3f))
        drawLogo(canvas, cx = W / 2f, top = 170f, width = 380f, alpha = appear)
        drawText(canvas, spec.text.outro, cx = W / 2f, centerY = 720f, width = 600, size = 58f, maxLines = 3, face = assets.display, color = ORANGE_DARK, alpha = appear)
        drawText(canvas, "Android: Google Play'de \"Karalak\"", cx = W / 2f, centerY = 880f, width = 600, size = 32f, maxLines = 2, face = assets.body, color = INK, alpha = appear)
    }

    // ---- one drawing at a time (Bu ne? and Hızlı tur)

    private fun drawItem(canvas: Canvas, seg: Segment, tMs: Long) {
        val i = seg.item
        val count = spec.drawings.size
        drawLogo(canvas, cx = 96f, top = 40f, width = 112f, alpha = 1f)
        drawText(canvas, spec.text.hook, cx = 425f, centerY = 96f, width = 500, size = 32f, maxLines = 3, face = assets.body, color = INK, alpha = 1f)

        // the card stays clear of where TikTok and Reels put their buttons and caption
        val card = RectF(80f, 180f, W - 80f, 740f)
        drawCard(canvas, card)
        if (count > 1) drawChip(canvas, "${i + 1}/$count", right = card.right - 18f, top = card.top + 18f)
        val p = seg.progress(tMs)
        val drawn = if (seg.kind == SegmentKind.Draw) p else 1f
        plans[i].draw(canvas, card, drawn, ink)

        when (seg.kind) {
            SegmentKind.Draw -> drawText(canvas, "çiziliyor…", cx = W / 2f, centerY = 830f, width = 560, size = 34f, maxLines = 1, face = assets.body, color = MUTED, alpha = 1f)
            SegmentKind.Ask -> {
                drawText(canvas, spec.text.ask, cx = W / 2f, centerY = 830f, width = 560, size = 72f, maxLines = 1, face = assets.display, color = INK, alpha = 1f)
                drawCountdown(canvas, cx = W / 2f, cy = 965f, p = p, seconds = seg.durationMs / 1000f, radius = 58f)
            }
            SegmentKind.Reveal -> drawReveal(canvas, spec.drawings[i].word, p, centerY = 845f)
            else -> Unit
        }
        drawFooter(canvas)
    }

    private fun drawFooter(canvas: Canvas) {
        drawText(canvas, "karalak.com · kurulumsuz oyna", cx = W / 2f, centerY = 1150f, width = 560, size = 28f, maxLines = 1, face = assets.body, color = MUTED, alpha = 1f)
    }

    // ---- the same word twice (Aynı kelime)

    private fun drawVersus(canvas: Canvas, seg: Segment, tMs: Long) {
        drawLogo(canvas, cx = 88f, top = 30f, width = 96f, alpha = 1f)
        drawText(canvas, spec.text.hook, cx = 415f, centerY = 78f, width = 520, size = 30f, maxLines = 2, face = assets.body, color = INK, alpha = 1f)
        val p = seg.progress(tMs)
        val drawn = if (seg.kind == SegmentKind.Draw) p else 1f
        val cards = listOf(RectF(80f, 150f, W - 80f, 520f), RectF(80f, 540f, W - 80f, 910f))
        cards.forEachIndexed { k, card ->
            drawCard(canvas, card)
            plans.getOrNull(k)?.draw(canvas, card, drawn, ink)
            drawChip(canvas, if (k == 0) "Üst" else "Alt", right = card.left + 110f, top = card.top + 16f)
        }
        when (seg.kind) {
            SegmentKind.Ask -> {
                drawText(canvas, spec.text.ask, cx = 320f, centerY = 990f, width = 480, size = 44f, maxLines = 2, face = assets.display, color = INK, alpha = 1f)
                drawCountdown(canvas, cx = 600f, cy = 990f, p = p, seconds = seg.durationMs / 1000f, radius = 44f)
            }
            SegmentKind.Reveal -> drawReveal(canvas, spec.drawings.first().word, p, centerY = 980f, size = 64f)
            else -> drawText(canvas, "çiziliyor…", cx = W / 2f, centerY = 990f, width = 560, size = 32f, maxLines = 1, face = assets.body, color = MUTED, alpha = 1f)
        }
        drawFooter(canvas)
    }

    // ---- pieces

    private fun drawReveal(canvas: Canvas, word: String, p: Float, centerY: Float, size: Float = 84f) {
        val pop = 0.6f + 0.4f * backOut(min(1f, p * 3.5f))
        canvas.save()
        canvas.scale(pop, pop, W / 2f, centerY)
        drawText(canvas, word.uppercase(TR), cx = W / 2f, centerY = centerY, width = 560, size = size, maxLines = 1, face = assets.display, color = ORANGE_DARK, alpha = 1f)
        canvas.restore()
        drawText(canvas, "Bildin mi? Yorumlara yaz", cx = W / 2f, centerY = centerY + size * 0.95f, width = 600, size = 30f, maxLines = 1, face = assets.body, color = MUTED, alpha = min(1f, p * 3f))
    }

    private fun drawCountdown(canvas: Canvas, cx: Float, cy: Float, p: Float, seconds: Float, radius: Float = 62f) {
        line.strokeWidth = radius * 0.2f
        line.color = TRACK
        canvas.drawCircle(cx, cy, radius, line)
        line.color = ORANGE
        line.strokeCap = Paint.Cap.ROUND
        val oval = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        canvas.drawArc(oval, -90f, 360f * (1f - p), false, line)
        val left = ceil((1f - p) * seconds).toInt().coerceAtLeast(1)
        drawText(canvas, left.toString(), cx = cx, centerY = cy, width = (radius * 2).toInt(), size = radius * 0.95f, maxLines = 1, face = assets.display, color = INK, alpha = 1f)
    }

    private fun drawCard(canvas: Canvas, card: RectF) {
        fill.color = CARD_SHADOW
        canvas.drawRoundRect(RectF(card.left, card.top + 8f, card.right, card.bottom + 8f), 36f, 36f, fill)
        fill.color = CARD
        canvas.drawRoundRect(card, 36f, 36f, fill)
        line.color = CARD_EDGE
        line.strokeWidth = 3f
        canvas.drawRoundRect(card, 36f, 36f, line)
    }

    private fun drawChip(canvas: Canvas, text: String, right: Float, top: Float) {
        val paint = textPaint(30f, assets.body, INK)
        val w = paint.measureText(text) + 36f
        val rect = RectF(right - w, top, right, top + 52f)
        fill.color = CHIP
        canvas.drawRoundRect(rect, 26f, 26f, fill)
        val fm = paint.fontMetrics
        canvas.drawText(text, rect.left + 18f, rect.centerY() - (fm.ascent + fm.descent) / 2f, paint)
    }

    private fun drawLogo(canvas: Canvas, cx: Float, top: Float, width: Float, alpha: Float) {
        val logo = assets.logo ?: return
        val h = width * logo.height / logo.width
        fill.alpha = (alpha * 255).toInt()
        canvas.drawBitmap(logo, null, RectF(cx - width / 2f, top, cx + width / 2f, top + h), fill.apply { isFilterBitmap = true })
        fill.alpha = 255
    }

    private fun textPaint(size: Float, face: Typeface, color: Int) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        this.textSize = size; typeface = face; this.color = color
    }

    /** Centred, wrapped text that shrinks until it fits [maxLines]; laid out once and reused across frames. */
    private fun drawText(canvas: Canvas, text: String, cx: Float, centerY: Float, width: Int, size: Float, maxLines: Int, face: Typeface, color: Int, alpha: Float) {
        if (text.isBlank() || alpha <= 0f) return
        val key = "$text|$width|$size|$maxLines|${face.hashCode()}|$color"
        val layout = layouts.getOrPut(key) {
            var s = size
            var built: StaticLayout
            while (true) {
                built = StaticLayout.Builder.obtain(text, 0, text.length, textPaint(s, face, color), width)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .setIncludePad(false)
                    .setLineSpacing(0f, 1.02f)
                    .setMaxLines(maxLines)
                    .setEllipsize(TextUtils.TruncateAt.END)
                    .build()
                val fits = built.lineCount < maxLines || built.getEllipsisCount(built.lineCount - 1) == 0
                if (fits || s <= size * 0.55f) break
                s *= 0.92f
            }
            built
        }
        layout.paint.alpha = (alpha.coerceIn(0f, 1f) * 255).toInt()
        canvas.save()
        canvas.translate(cx - width / 2f, centerY - layout.height / 2f)
        layout.draw(canvas)
        canvas.restore()
        layout.paint.alpha = 255
    }

    private fun ease(x: Float) = 1f - (1f - x) * (1f - x) * (1f - x)

    private fun backOut(x: Float): Float {
        val c1 = 1.70158f
        val c3 = c1 + 1f
        val y = x - 1f
        return 1f + c3 * y * y * y + c1 * y * y
    }

    companion object {
        const val W = 720
        const val H = 1280
        private val TR = java.util.Locale("tr")
        private const val CREAM = 0xFFFCF5EA.toInt()
        private const val CARD = 0xFFFFFFFF.toInt()
        private const val CARD_EDGE = 0xFFF3E6D2.toInt()
        private const val CARD_SHADOW = 0x14000000
        private const val CHIP = 0xFFF1E3CD.toInt()
        private const val TRACK = 0xFFEADCC6.toInt()
        private const val INK = 0xFF221E1A.toInt()
        private const val MUTED = 0xFF8A7F73.toInt()
        private const val ORANGE = 0xFFF97316.toInt()
        private const val ORANGE_DARK = 0xFFDC5F0B.toInt()
    }
}

/**
 * A drawing laid out for replay: where it sits in a card and how far along each stroke a given
 * share of the total ink reaches, so a replay draws at an even pace the way a hand would.
 */
private class StrokePlan(private val strokes: List<DrawingStroke>) {
    private val lengths: List<Float> = strokes.map { s ->
        var len = 0f
        for (k in 1 until s.size) len += hypot(s[k].x - s[k - 1].x, s[k].y - s[k - 1].y)
        max(len, DOT)
    }
    private val total = lengths.sum().coerceAtLeast(DOT)
    private val minX = strokes.flatten().minOfOrNull { it.x } ?: 0f
    private val maxX = strokes.flatten().maxOfOrNull { it.x } ?: 1f
    private val minY = strokes.flatten().minOfOrNull { it.y } ?: 0f
    private val maxY = strokes.flatten().maxOfOrNull { it.y } ?: 1f
    private val path = Path()

    fun draw(canvas: Canvas, card: RectF, fraction: Float, paint: Paint) {
        if (strokes.isEmpty() || fraction <= 0f) return
        val pad = min(card.width(), card.height()) * 0.09f
        val cw = (maxX - minX).coerceAtLeast(1f)
        val ch = (maxY - minY).coerceAtLeast(1f)
        val scale = min((card.width() - pad * 2) / cw, (card.height() - pad * 2) / ch)
        val ox = card.left + (card.width() - cw * scale) / 2f
        val oy = card.top + (card.height() - ch * scale) / 2f
        fun x(v: Float) = ox + (v - minX) * scale
        fun y(v: Float) = oy + (v - minY) * scale
        paint.strokeWidth = min(card.width(), card.height()) * 0.024f

        var budget = total * fraction.coerceIn(0f, 1f)
        strokes.forEachIndexed { si, s ->
            if (budget <= 0f || s.isEmpty()) return@forEachIndexed
            if (s.size == 1 || lengths[si] <= DOT) {
                canvas.drawPoint(x(s[0].x), y(s[0].y), paint)
                budget -= lengths[si]
                return@forEachIndexed
            }
            path.reset()
            path.moveTo(x(s[0].x), y(s[0].y))
            for (k in 1 until s.size) {
                val seg = hypot(s[k].x - s[k - 1].x, s[k].y - s[k - 1].y)
                if (seg <= budget) {
                    path.lineTo(x(s[k].x), y(s[k].y))
                    budget -= seg
                } else {
                    val f = if (seg > 0f) budget / seg else 0f
                    path.lineTo(x(s[k - 1].x + (s[k].x - s[k - 1].x) * f), y(s[k - 1].y + (s[k].y - s[k - 1].y) * f))
                    budget = 0f
                    break
                }
            }
            canvas.drawPath(path, paint)
        }
    }

    private companion object {
        const val DOT = 4f
    }
}
