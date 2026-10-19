package com.sualtikasifi.cizimhafiza.presentation.splash

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import kotlin.math.max

/**
 * The opening scene as the WINDOW's own background, centre-cropped exactly the way [BrandSplash] crops it.
 *
 * It lets the very first frame the player sees already be the scene: the Activity puts this behind everything, the
 * window is drawn (which is when the system's plain splash goes away), and only then is the app itself composed on top.
 * BrandSplash's own first frame is this same picture in the same place, so the hand-over is invisible.
 */
class SplashSceneDrawable(private val bitmap: Bitmap) : Drawable() {
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun draw(canvas: Canvas) {
        val b = bounds
        if (b.isEmpty) return
        val scale = max(b.width().toFloat() / bitmap.width, b.height().toFloat() / bitmap.height)
        val dx = b.left + (b.width() - bitmap.width * scale) / 2f
        val dy = b.top + (b.height() - bitmap.height * scale) / 2f
        val save = canvas.save()
        canvas.clipRect(b)
        canvas.translate(dx, dy)
        canvas.scale(scale, scale)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        drawEmptyBar(canvas)
        canvas.restoreToCount(save)
    }

    /**
     * The picture has a half-filled loading bar painted into it. BrandSplash draws its own bar over the same spot,
     * starting empty, so without this the bar visibly emptied the moment the app took over. This draws that bar's
     * empty track (same rim, same colours) in the window's own first frame as well.
     */
    private fun drawEmptyBar(canvas: Canvas) {
        val k = bitmap.width / ART_W
        val left = BAR_L * k
        val top = BAR_T * k
        val w = (BAR_R - BAR_L) * k
        val h = (BAR_B - BAR_T) * k
        barPaint.color = 0xFFFFE08A.toInt()
        canvas.drawRoundRect(left - 3f, top - 3f, left + w + 3f, top + h + 3f, h / 2f, h / 2f, barPaint)
        barPaint.color = 0xFF8A4E12.toInt()
        canvas.drawRoundRect(left, top, left + w, top + h, h / 2f, h / 2f, barPaint)
        barPaint.color = 0xFF3F2210.toInt()
        canvas.drawRoundRect(left + h * 0.09f, top + h * 0.09f, left + w - h * 0.09f, top + h * 0.91f, h * 0.41f, h * 0.41f, barPaint)
    }

    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.OPAQUE
}

// Where the loading bar sits in the picture (pixels of the 841 x 1870 art); the same numbers BrandSplash uses.
private const val ART_W = 841f
private const val BAR_L = 196f
private const val BAR_R = 642f
private const val BAR_T = 1354f
private const val BAR_B = 1430f
