package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.text.style.CharacterStyle
import androidx.core.content.res.ResourcesCompat
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.DrawingStroke
import java.io.ByteArrayOutputStream
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.audio.Reveal

/** One drawing made on this phone, ready to be replayed. [answer] is what its drawer typed when trying to remember it. */
data class StudioDrawing(
    val id: Long,
    val word: String,
    val category: String,
    val strokes: List<DrawingStroke>,
    val correct: Boolean,
    val answer: String = ""
)

/** Fonts and brand pictures, read once per studio visit rather than once per frame. */
class StudioAssets(context: Context) {
    val display: Typeface = runCatching { ResourcesCompat.getFont(context, R.font.baloo2_extrabold) }.getOrNull() ?: Typeface.DEFAULT_BOLD
    val body: Typeface = runCatching { ResourcesCompat.getFont(context, R.font.quicksand_bold) }.getOrNull() ?: Typeface.DEFAULT

    /** The round brand badge; its corners are transparent, so it sits on the cream without a box. */
    val logo: Bitmap? = runCatching { BitmapFactory.decodeResource(context.resources, R.drawable.brand_logo) }.getOrNull()

    private val resources = context.resources
    private val pictures = HashMap<Int, Bitmap?>()

    /** One of the game's own painted pictures (a scene, a frame, a sign), at its own size and read once. */
    fun picture(@androidx.annotation.DrawableRes id: Int): Bitmap? = synchronized(pictures) {
        pictures.getOrPut(id) {
            runCatching {
                BitmapFactory.decodeResource(resources, id, BitmapFactory.Options().apply { inScaled = false })
            }.getOrNull()
        }
    }

    private val appContext = context.applicationContext
    private val localPictures = HashMap<String, Bitmap?>()

    /**
     * One of the app's pictures as it is in [locale], whatever the phone's own language: the Google Play badge is
     * Google's artwork, one per language, and a video in Spanish carries the Spanish one. Read once.
     */
    fun picture(@androidx.annotation.DrawableRes id: Int, locale: java.util.Locale): Bitmap? = synchronized(localPictures) {
        localPictures.getOrPut("$id|${locale.language}") {
            runCatching {
                val conf = android.content.res.Configuration(resources.configuration).apply { setLocale(locale) }
                val res = appContext.createConfigurationContext(conf).resources
                BitmapFactory.decodeResource(res, id, BitmapFactory.Options().apply { inScaled = false })
            }.getOrNull()
        }
    }
}

/** The studio's colours: the game's own cream, ink and orange. */
object StudioColors {
    const val CREAM = 0xFFFCF5EA.toInt()
    const val CREAM_DEEP = 0xFFF3E6D2.toInt()
    const val CARD = 0xFFFFFFFF.toInt()
    const val CARD_EDGE = 0xFFEADCC6.toInt()
    const val SHADOW = 0x1A3A2416
    const val INK = 0xFF221E1A.toInt()
    const val MUTED = 0xFF8A7F73.toInt()
    const val FAINT = 0xFFC2B6A8.toInt()
    const val ORANGE = 0xFFF97316.toInt()
    const val ORANGE_DARK = 0xFFDC5F0B.toInt()
    const val GOOD = 0xFF12A150.toInt()
    const val BAD = 0xFFD93B3B.toInt()
    const val WHITE = 0xFFFFFFFF.toInt()
}

/**
 * A drawing laid out for replay: where it sits in a box and how far along each stroke a given share
 * of the total ink reaches, so a replay draws at an even pace the way a hand would.
 */
class StrokeReplay(private val strokes: List<DrawingStroke>) {
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

    /** Draws the first [fraction] of the ink, fitted and centred in [box]. */
    fun draw(canvas: Canvas, box: RectF, fraction: Float, paint: Paint, widthFraction: Float = 0.024f) {
        if (strokes.isEmpty() || fraction <= 0f) return
        val pad = min(box.width(), box.height()) * 0.09f
        val cw = (maxX - minX).coerceAtLeast(1f)
        val ch = (maxY - minY).coerceAtLeast(1f)
        val scale = min((box.width() - pad * 2) / cw, (box.height() - pad * 2) / ch)
        val ox = box.left + (box.width() - cw * scale) / 2f
        val oy = box.top + (box.height() - ch * scale) / 2f
        fun x(v: Float) = ox + (v - minX) * scale
        fun y(v: Float) = oy + (v - minY) * scale
        paint.strokeWidth = min(box.width(), box.height()) * widthFraction

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

    companion object {
        private const val DOT = 4f

        fun inkPaint(color: Int = StudioColors.INK) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
        }

        /** The drawing as far as [fraction], black on white, as PNG bytes: what the assistant is shown. */
        fun png(strokes: List<DrawingStroke>, fraction: Float, size: Int = 512): ByteArray {
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            canvas.drawColor(StudioColors.WHITE)
            StrokeReplay(strokes).draw(canvas, RectF(0f, 0f, size.toFloat(), size.toFloat()), fraction, inkPaint(), 0.02f)
            val out = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
            bmp.recycle()
            return out.toByteArray()
        }
    }
}

/**
 * Centred, wrapped text drawn with android.graphics, shrinking until it fits its lines. Layouts are
 * cached by content, so a line on screen for a hundred frames is laid out once.
 */
class TextPainter {
    private val cache = HashMap<String, StaticLayout>()

    fun paint(size: Float, face: Typeface, color: Int) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size; typeface = face; this.color = color
    }

    // Layout's BREAK_STRATEGY_* are the very values LineBreaker's are (0, 1, 2); lint only knows the newer names,
    // and LineBreaker itself is API 29.
    @android.annotation.SuppressLint("WrongConstant")
    private fun build(
        text: CharSequence, width: Int, size: Float, maxLines: Int, face: Typeface, color: Int, align: Layout.Alignment,
        spacing: Float = 1.02f, balanced: Boolean = false
    ): StaticLayout {
        var s = size
        while (true) {
            val built = StaticLayout.Builder.obtain(text, 0, text.length, paint(s, face, color), width)
                .setAlignment(align)
                .setIncludePad(false)
                .setLineSpacing(0f, spacing)
                .setBreakStrategy(if (balanced) Layout.BREAK_STRATEGY_BALANCED else Layout.BREAK_STRATEGY_SIMPLE)
                .setMaxLines(maxLines)
                .setEllipsize(TextUtils.TruncateAt.END)
                .build()
            val fits = built.lineCount < maxLines || built.getEllipsisCount(built.lineCount - 1) == 0
            if (fits || s <= size * 0.55f) return built
            s *= 0.92f
        }
    }

    /**
     * Draws [text] centred on ([cx], [cy]); returns the height it took. [spacing] multiplies the font's own line height
     * (Baloo's is tall: a big two-line title reads better pulled together); [balanced] breaks lines to even lengths
     * rather than filling the first one.
     */
    fun draw(
        canvas: Canvas, text: String, cx: Float, cy: Float, width: Int, size: Float, maxLines: Int, face: Typeface, color: Int,
        alpha: Float = 1f, align: Layout.Alignment = Layout.Alignment.ALIGN_CENTER, spacing: Float = 1.02f, balanced: Boolean = false
    ): Float {
        if (text.isBlank() || alpha <= 0f) return 0f
        val key = "$text|$width|$size|$maxLines|${face.hashCode()}|$color|$align|$spacing|$balanced"
        val layout = cache.getOrPut(key) { build(text, width, size, maxLines, face, color, align, spacing, balanced) }
        drawLayout(canvas, layout, cx, cy, width, alpha)
        return layout.height.toFloat()
    }

    /** How wide its longest line and how tall [draw] would lay [text] out, without drawing it (to fit a box behind it). */
    fun measure(
        text: String, width: Int, size: Float, maxLines: Int, face: Typeface, color: Int,
        align: Layout.Alignment = Layout.Alignment.ALIGN_CENTER, spacing: Float = 1.02f, balanced: Boolean = false
    ): Pair<Float, Float> {
        if (text.isBlank()) return 0f to 0f
        val key = "$text|$width|$size|$maxLines|${face.hashCode()}|$color|$align|$spacing|$balanced"
        val layout = cache.getOrPut(key) { build(text, width, size, maxLines, face, color, align, spacing, balanced) }
        return (0 until layout.lineCount).maxOf { layout.getLineWidth(it) } to layout.height.toFloat()
    }

    /**
     * A subtitle coming in as it is said: each character appears at its moment in [starts] (see [Reveal]) and fades in,
     * [elapsedMs] into the line. The whole line is laid out from the first frame and the letters not yet said are only
     * transparent, so nothing shifts as it fills in. [lettering] says how it looks. Rebuilt every frame, which is cheap
     * next to everything else a frame draws.
     */
    fun reveal(
        canvas: Canvas, text: String, starts: LongArray, elapsedMs: Long, cx: Float, cy: Float, width: Int, size: Float, maxLines: Int,
        face: Typeface, lettering: Lettering, alpha: Float = 1f, spacing: Float = 1.02f, balanced: Boolean = false
    ) {
        if (text.isBlank() || alpha <= 0f || starts.size != text.length) return
        val span = SpannableString(text)
        var hiddenFrom = text.length
        for (i in text.indices) {
            val a = Reveal.alpha(starts, i, elapsedMs)
            if (a <= 0f) { hiddenFrom = i; break }
            if (a < 1f) span.setSpan(AlphaSpan(a), i, i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        if (hiddenFrom == 0) return
        if (hiddenFrom < text.length) span.setSpan(AlphaSpan(0f), hiddenFrom, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        val layout = build(span, width, size, maxLines, face, lettering.fill, Layout.Alignment.ALIGN_CENTER, spacing, balanced)
        val p = layout.paint
        val a = alpha.coerceIn(0f, 1f)
        canvas.save()
        canvas.translate(cx - width / 2f, cy - layout.height / 2f)
        p.strokeJoin = Paint.Join.ROUND
        if (lettering.shadow != null && lettering.shadowBlur > 0f) {
            p.style = Paint.Style.FILL_AND_STROKE
            p.strokeWidth = lettering.outlineWidth
            p.color = lettering.shadow
            p.alpha = (Color.alpha(lettering.shadow) * a).roundToInt()
            p.maskFilter = BlurMaskFilter(lettering.shadowBlur, BlurMaskFilter.Blur.NORMAL)
            canvas.translate(0f, lettering.shadowDy)
            layout.draw(canvas)
            canvas.translate(0f, -lettering.shadowDy)
            p.maskFilter = null
        }
        if (lettering.outline != null && lettering.outlineWidth > 0f) {
            p.style = Paint.Style.STROKE
            p.strokeWidth = lettering.outlineWidth
            p.color = lettering.outline
            p.alpha = (Color.alpha(lettering.outline) * a).roundToInt()
            layout.draw(canvas)
        }
        p.style = Paint.Style.FILL
        p.color = lettering.fill
        p.alpha = (Color.alpha(lettering.fill) * a).roundToInt()
        layout.draw(canvas)
        canvas.restore()
    }

    private fun drawLayout(canvas: Canvas, layout: StaticLayout, cx: Float, cy: Float, width: Int, alpha: Float) {
        layout.paint.alpha = (alpha.coerceIn(0f, 1f) * 255).toInt()
        canvas.save()
        canvas.translate(cx - width / 2f, cy - layout.height / 2f)
        layout.draw(canvas)
        canvas.restore()
        layout.paint.alpha = 255
    }
}

/**
 * How a subtitle is lettered: [fill] over an [outline] [outlineWidth] wide, on a soft [shadow] blurred by [shadowBlur]
 * and dropped by [shadowDy], the way the game letters its labels over busy pictures; with neither, plain [fill] text.
 */
class Lettering(
    val fill: Int,
    val outline: Int? = null,
    val outlineWidth: Float = 0f,
    val shadow: Int? = null,
    val shadowBlur: Float = 0f,
    val shadowDy: Float = 0f
) {
    companion object {
        /** White with a dark brown edge and shadow: reads on a bright clip and on the game's painted scenes alike. */
        fun onPicture(size: Float) = Lettering(
            fill = 0xFFFFFFFF.toInt(), outline = 0xFF2B1A10.toInt(), outlineWidth = size * 0.17f,
            shadow = 0xB0000000.toInt(), shadowBlur = size * 0.22f, shadowDy = size * 0.08f
        )

        fun plain(color: Int) = Lettering(color)
    }
}

/** Takes a share of the paint's own alpha, whatever its colour: one span serves the shadow, the edge and the fill. */
private class AlphaSpan(private val share: Float) : CharacterStyle() {
    override fun updateDrawState(tp: TextPaint) {
        tp.alpha = (tp.alpha * share).roundToInt()
    }
}

/** Little easing helpers shared by the renderers. */
object Ease {
    fun out(x: Float): Float = 1f - (1f - x) * (1f - x) * (1f - x)
    fun backOut(x: Float): Float {
        val c1 = 1.70158f
        val c3 = c1 + 1f
        val y = x - 1f
        return 1f + c3 * y * y * y + c1 * y * y
    }
}
