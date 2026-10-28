package com.sualtikasifi.cizimhafiza.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.sualtikasifi.cizimhafiza.R

/**
 * The branded card both the shared picture and the shared video are laid out on: the Karalak mark and the
 * mascot on top and a white card in the middle are baked into R.drawable.share_template (1024x1536 — both sides
 * are multiples of 16, which every AVC encoder takes). The picture carries no words of any language.
 *
 * The app adds everything that is language or the player's own work: the slogan and the official Google Play
 * badge of the player's language under the card (R.drawable.share_play_badge, one per locale — Google's artwork,
 * never redrawn), the drawing inside the card, the word on a paint-stroke band under it, and the small "Karalak"
 * caption. The rectangles below were measured off the template image — if the template is replaced, they have to
 * be measured again, not guessed.
 */
internal object ShareTemplate {
    const val WIDTH = 1024
    const val HEIGHT = 1536

    /** Where the drawing goes: the upper part of the white card, clear of its orange border. */
    val drawingRect = RectF(190f, 380f, 850f, 1000f)

    private val bandRect = RectF(318f, 1026f, 706f, 1114f)
    private const val WORD_CENTER_Y = 1070f
    private const val CAPTION_CENTER_Y = 1158f

    private val bandColor = Color.rgb(0xFD, 0xD6, 0x94)
    private val wordColor = Color.rgb(0x2B, 0x21, 0x18)
    private val captionColor = Color.rgb(0x55, 0x4B, 0x42)
    private val dashColor = Color.rgb(0xF9, 0x73, 0x16)

    /** The single-drawing card with the slogan and the store badge already on it, in the player's language. */
    fun load(context: Context): Bitmap = decodeMutable(context, R.drawable.share_template).also { bitmap ->
        val canvas = Canvas(bitmap)
        drawLettering(context, canvas, context.getString(R.string.share_card_slogan), RectF(56f, 1240f, 510f, 1430f), 52f, underline = true)
        drawStoreBadge(context, canvas, centerX = 762f, centerY = 1334f, height = 120f)
    }

    /** The template for a whole round's drawings ("Can you guess these drawings?"), same size and the same additions. */
    fun loadCollage(context: Context): Bitmap = decodeMutable(context, R.drawable.share_template_collage).also { bitmap ->
        val canvas = Canvas(bitmap)
        drawLettering(context, canvas, context.getString(R.string.share_card_collage_title), RectF(110f, 186f, 560f, 312f), 46f, underline = false)
        drawStoreBadge(context, canvas, centerX = WIDTH / 2f, centerY = 1440f, height = 112f)
    }

    private fun decodeMutable(context: Context, res: Int): Bitmap =
        BitmapFactory.decodeResource(context.resources, res, BitmapFactory.Options().apply { inMutable = true })

    private val inkColor = Color.rgb(0x2B, 0x1A, 0x10)
    private val strokeColor = Color.rgb(0xF5, 0x8A, 0x12)

    /**
     * Hand-lettered lines (the text's own line breaks), tilted a little like the rest of the card's lettering and
     * shrunk until the longest line fits [box]; an orange paint stroke under them when [underline].
     */
    private fun drawLettering(context: Context, canvas: Canvas, text: String, box: RectF, size: Float, underline: Boolean) {
        val lines = text.split('\n').map(String::trim).filter(String::isNotEmpty)
        if (lines.isEmpty()) return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            typeface = ResourcesCompat.getFont(context, R.font.baloo2_extrabold) ?: Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            textSize = size
        }
        val lineHeight = { paint.textSize * 1.12f }
        val widest = lines.maxOf { paint.measureText(it) }
        if (widest > box.width()) paint.textSize *= box.width() / widest
        val blockHeight = lineHeight() * lines.size + if (underline) paint.textSize * 0.45f else 0f
        if (blockHeight > box.height()) paint.textSize *= box.height() / blockHeight
        val total = lineHeight() * lines.size
        canvas.save()
        canvas.rotate(-4f, box.centerX(), box.centerY())
        var baseline = box.centerY() - total / 2f - (if (underline) paint.textSize * 0.2f else 0f) - paint.ascent() * 0.92f
        lines.forEach { line ->
            canvas.drawText(line, box.centerX(), baseline, paint)
            baseline += lineHeight()
        }
        if (underline) {
            val w = paint.measureText(lines.last()) * 0.8f
            val y = baseline - lineHeight() + paint.descent() + paint.textSize * 0.32f
            val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = strokeColor
                style = Paint.Style.STROKE
                strokeWidth = paint.textSize * 0.16f
                strokeCap = Paint.Cap.ROUND
            }
            val path = android.graphics.Path().apply {
                moveTo(box.centerX() - w / 2f, y + paint.textSize * 0.12f)
                quadTo(box.centerX(), y - paint.textSize * 0.08f, box.centerX() + w / 2f, y)
            }
            canvas.drawPath(path, stroke)
        }
        canvas.restore()
    }

    /** Google's own "Get it on Google Play" badge for the player's language, untouched, scaled to [height]. */
    private fun drawStoreBadge(context: Context, canvas: Canvas, centerX: Float, centerY: Float, height: Float) {
        val badge = BitmapFactory.decodeResource(context.resources, R.drawable.share_play_badge) ?: return
        val width = height * badge.width / badge.height
        canvas.drawBitmap(
            badge,
            null,
            RectF(centerX - width / 2f, centerY - height / 2f, centerX + width / 2f, centerY + height / 2f),
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        )
        badge.recycle()
    }

    /** Where the grid of drawings goes on the collage template: inside the big white card. */
    val collageRect = RectF(100f, 362f, 934f, 1312f)

    /**
     * "KEDİ" -> "K _ _ _": only the first letter is ever shown on anything that leaves the app, the rest are
     * blanks — the shared picture is a riddle, not the answer. Spaces stay spaces. Upper-cased in the word's own
     * [language], so a Turkish "inek" starts with "İ", not "I".
     */
    fun maskedWord(word: String, language: String): String {
        val upper = word.uppercase(java.util.Locale.forLanguageTag(language))
        val first = upper.indexOfFirst { it.isLetterOrDigit() }
        if (first < 0) return upper
        return upper.mapIndexed { i, c ->
            when {
                i == first -> c.toString()
                c.isWhitespace() -> " "
                c.isLetterOrDigit() -> "_"
                else -> c.toString()
            }
        }.joinToString(" ")
    }

    /** The word on its band and the "Karalak" caption under it. [word] is drawn exactly as given. */
    fun drawWordAndCaption(context: Context, canvas: Canvas, word: String) {
        canvas.drawRoundRect(bandRect, 30f, 30f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bandColor })

        val display = ResourcesCompat.getFont(context, R.font.baloo2_extrabold) ?: Typeface.DEFAULT_BOLD
        val wordPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = wordColor
            typeface = display
            textAlign = Paint.Align.CENTER
            textSize = 62f
        }
        // Long words shrink to fit the band rather than spilling over its ends.
        val maxWidth = bandRect.width() - 36f
        val measured = wordPaint.measureText(word)
        if (measured > maxWidth) wordPaint.textSize *= maxWidth / measured
        canvas.drawText(word, bandRect.centerX(), WORD_CENTER_Y - (wordPaint.ascent() + wordPaint.descent()) / 2f, wordPaint)

        val captionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = captionColor
            typeface = ResourcesCompat.getFont(context, R.font.baloo2_bold) ?: Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            textSize = 34f
        }
        val cx = WIDTH / 2f
        canvas.drawText("Karalak", cx, CAPTION_CENTER_Y - (captionPaint.ascent() + captionPaint.descent()) / 2f, captionPaint)
        val dash = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = dashColor
            strokeWidth = 5f
            strokeCap = Paint.Cap.ROUND
        }
        val half = captionPaint.measureText("Karalak") / 2f
        canvas.drawLine(cx - half - 70f, CAPTION_CENTER_Y, cx - half - 18f, CAPTION_CENTER_Y, dash)
        canvas.drawLine(cx + half + 18f, CAPTION_CENTER_Y, cx + half + 70f, CAPTION_CENTER_Y, dash)
    }
}
