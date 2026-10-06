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
        canvas.restoreToCount(save)
    }

    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.OPAQUE
}
