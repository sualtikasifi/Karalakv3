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
 * The branded card both the shared picture and the shared video are laid out on: the Karalak mark and
 * mascot on top, a white card in the middle, "Sen de oyna" and the Google Play badge below (all of it baked
 * into R.drawable.share_template, 1024x1536 — both sides are multiples of 16, which every AVC encoder takes).
 *
 * The app only adds what must be real type or the player's own work: the drawing inside the card, the word on
 * a paint-stroke band under it, and the small "Karalak" caption. The rectangles below were measured off the
 * template image — if the template is replaced, they have to be measured again, not guessed.
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

    fun load(context: Context): Bitmap =
        BitmapFactory.decodeResource(context.resources, R.drawable.share_template)

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
