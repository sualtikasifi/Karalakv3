package com.sualtikasifi.cizimhafiza.presentation.reports.studio.real

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.AvatarFrame
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.ClipSound
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.Ease
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StrokeReplay
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioAssets
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioColors as C
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.TextPainter
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.VoicedVideo
import java.io.File
import kotlin.math.min

/**
 * Draws "Gerçek Olsaydı" in 1080×1920: the clips one after the other, each full frame with its own sound, and
 * over each, in the left column only: the player's drawing on a sheet of paper, who drew it (their name, or
 * "anonymous", beside their frame and the default picture) and what it was, a note that it is AI, and the
 * app's name with the Google Play badge. Plain cuts between clips, no voice, no subtitles. The right edge and
 * the bottom band stay clear for the buttons and caption TikTok, Reels and Shorts put there.
 */
class RealRenderer(
    private val assets: StudioAssets,
    private val project: RealProject,
    /** Clips by drawing id. */
    private val clips: Map<String, ClipFrames>,
    private val clipFiles: Map<String, File>,
    /** How loud the clips' own sound is. */
    private val clipGain: Float
) : VoicedVideo {
    private val shown = project.shown
    val events: List<RealEvent> = RealTimeline.build(shown)
    override val totalMs: Long = RealTimeline.totalMs(events)
    override val voiceStarts: List<Pair<Int, Long>> = emptyList()
    override val sounds: List<ClipSound> = if (clipGain <= 0f) emptyList() else events.filter { it.phase == RealPhase.Clip }.mapNotNull { e ->
        clipFiles[shown.getOrNull(e.index)?.sampleId]?.let { ClipSound(it, e.clipAtMs, e.clipMs, clipGain) }
    }

    private val text = TextPainter()
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val ink = StrokeReplay.inkPaint(0xFF1B1B22.toInt())
    private val replays = HashMap<String, StrokeReplay>()
    private val src = Rect()
    private val dino = assets.picture(R.drawable.avatar_dino)

    // where things sit: the left column only
    private val sheet = RectF(48f, 200f, 408f, 200f + 420f)
    private val avatarBox = RectF(40f, 660f, 40f + 190f, 660f + 190f)
    private val infoBox = RectF(212f, 676f, 212f + 560f, 676f + 150f)
    private val labelBox = RectF(48f, 872f, 48f + 470f, 872f + 52f)
    private val appRow = RectF(48f, 1330f, 48f + 120f, 1330f + 120f)
    private val badge = RectF(48f, 1472f, 48f + 440f, 1472f + 112f)

    private fun replay(id: String): StrokeReplay? = project.drawing(id)?.let { d -> replays.getOrPut(id) { StrokeReplay(d.strokes) } }

    override fun draw(canvas: Canvas, t: Long) {
        val e = RealTimeline.at(events, t)
        val reveal = shown.getOrNull(e.index) ?: return
        canvas.drawColor(C.INK)
        val frame = clips[reveal.sampleId]?.frameAt((t - e.clipAtMs).coerceIn(0L, e.clipMs))
        if (frame != null) cover(canvas, frame, RectF(0f, 0f, W.toFloat(), H.toFloat()))
        val drawing = project.drawing(reveal.sampleId) ?: return
        val local = t - e.startMs
        drawSheet(canvas, drawing, Ease.backOut(min(1f, local / 450f)))
        drawPlayer(canvas, drawing, Ease.out(min(1f, (local - 150L) / 400f)))
        drawLabel(canvas, Ease.out(min(1f, (local - 300L) / 400f)))
        drawApp(canvas, Ease.out(min(1f, (local - 450L) / 400f)))
    }

    /** The player's drawing on a sheet of paper, a little crooked, as if pinned to the scene. */
    private fun drawSheet(canvas: Canvas, d: RealDrawing, a: Float) {
        canvas.save()
        canvas.rotate(-3f, sheet.centerX(), sheet.centerY())
        canvas.scale(a, a, sheet.left, sheet.top)
        fill.color = 0x55000000
        canvas.drawRoundRect(RectF(sheet.left + 8f, sheet.top + 14f, sheet.right + 8f, sheet.bottom + 14f), 14f, 14f, fill)
        fill.color = C.WHITE
        canvas.drawRoundRect(sheet, 14f, 14f, fill)
        replay(d.id)?.draw(canvas, RectF(sheet.left + 14f, sheet.top + 14f, sheet.right - 14f, sheet.bottom - 14f), 1f, ink, 0.028f)
        canvas.restore()
    }

    /**
     * Who drew it: their frame with the default picture inside, then their name and level (or "Anonim oyuncu" when
     * they did not allow it, with no level) over what the drawing was and how long they had.
     */
    private fun drawPlayer(canvas: Canvas, d: RealDrawing, a: Float) {
        if (a <= 0f) return
        canvas.save()
        canvas.translate((1f - a) * -60f, 0f)
        fill.color = ((0x99 * a).toInt() shl 24) or 0x101014
        canvas.drawRoundRect(infoBox, 28f, 28f, fill)
        val who = if (d.named) "@${d.nickname}" + if (d.level > 0) " · Sv. ${d.level}" else "" else "Anonim oyuncu"
        val what = buildString {
            if (d.word.isNotBlank()) append("“").append(d.word).append("”")
            if (d.seconds > 0) { if (isNotEmpty()) append(" · "); append(d.seconds).append(" saniyede çizdi") }
        }
        val left = avatarBox.right - 8f
        val width = (infoBox.right - left - 26f).toInt()
        val cx = left + (infoBox.right - left) / 2f + 8f
        text.draw(canvas, who, cx, infoBox.top + 52f, width, 46f, 1, assets.display, C.WHITE, alpha = a)
        text.draw(canvas, what, cx, infoBox.top + 108f, width, 36f, 1, assets.body, 0xFFFFE2BF.toInt(), alpha = a)
        drawAvatar(canvas, d, a)
        canvas.restore()
    }

    /** The player's frame, with the default picture in it: never their own photo. */
    private fun drawAvatar(canvas: Canvas, d: RealDrawing, a: Float) {
        val frame = AvatarFrame.resolve(d.frameId, d.level.coerceAtLeast(1))
        val frameBmp = assets.picture(frame.drawableRes)
        val size = avatarBox.width()
        val cx = avatarBox.centerX()
        val cy = avatarBox.centerY()
        val faceD = size * frame.faceDiameterFraction * 1.06f
        val fx = cx + size * frame.faceOffsetXFraction
        val fy = cy + size * frame.faceOffsetYFraction
        val photo = dino
        if (photo != null) {
            val shader = BitmapShader(photo, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            val s = faceD / min(photo.width, photo.height)
            shader.setLocalMatrix(Matrix().apply {
                setScale(s, s)
                postTranslate(fx - photo.width * s / 2f, fy - photo.height * s / 2f)
            })
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader; alpha = (a * 255).toInt() }
            canvas.drawCircle(fx, fy, faceD / 2f, paint)
        }
        fill.alpha = (a * 255).toInt()
        frameBmp?.let { canvas.drawBitmap(it, null, avatarBox, fill) }
        fill.alpha = 255
    }

    /** The "made with AI" note platforms ask for, small, under the player. */
    private fun drawLabel(canvas: Canvas, a: Float) {
        if (a <= 0f) return
        fill.color = ((0x80 * a).toInt() shl 24) or 0x101014
        canvas.drawRoundRect(labelBox, 26f, 26f, fill)
        text.draw(canvas, "Yapay zekâ ile canlandırıldı", labelBox.centerX(), labelBox.centerY(), (labelBox.width() - 30).toInt(), 28f, 1, assets.body, C.WHITE, alpha = a)
    }

    /** The app: its badge and name, and the Google Play badge under them, low in the left column. */
    private fun drawApp(canvas: Canvas, a: Float) {
        if (a <= 0f) return
        canvas.save()
        canvas.translate((1f - a) * -60f, 0f)
        assets.logo?.let { logo ->
            fill.alpha = (a * 255).toInt()
            canvas.drawBitmap(logo, null, appRow, fill)
            fill.alpha = 255
        }
        // a dark chip behind the name keeps it readable on any scene
        val nameChip = RectF(appRow.right + 12f, appRow.top + 20f, appRow.right + 12f + 330f, appRow.top + 20f + 80f)
        fill.color = ((0x99 * a).toInt() shl 24) or 0x101014
        canvas.drawRoundRect(nameChip, 40f, 40f, fill)
        text.draw(canvas, "Karalak", nameChip.centerX(), nameChip.centerY(), 290, 58f, 1, assets.display, C.WHITE, alpha = a)
        drawPlayBadge(canvas, a)
        canvas.restore()
    }

    /** A Google Play style download badge: black plate, the coloured play mark, and the app's store line. */
    private fun drawPlayBadge(canvas: Canvas, a: Float) {
        val alpha = (a * 255).toInt()
        fill.color = 0xFF000000.toInt()
        fill.alpha = alpha
        canvas.drawRoundRect(badge, 22f, 22f, fill)
        fill.style = Paint.Style.STROKE
        fill.strokeWidth = 3f
        fill.color = 0xFFA6A6A6.toInt()
        fill.alpha = alpha
        canvas.drawRoundRect(badge, 22f, 22f, fill)
        fill.style = Paint.Style.FILL
        fill.alpha = 255
        // the play mark: one triangle in the four Google colours
        val mark = RectF(badge.left + 26f, badge.top + 24f, badge.left + 26f + 62f, badge.bottom - 24f)
        val tri = Path().apply {
            moveTo(mark.left, mark.top)
            lineTo(mark.right, mark.centerY())
            lineTo(mark.left, mark.bottom)
            close()
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                mark.left, mark.top, mark.right, mark.bottom,
                intArrayOf(0xFF00C3FF.toInt(), 0xFF00E676.toInt(), 0xFFFFD600.toInt(), 0xFFFF3D4D.toInt()), null, Shader.TileMode.CLAMP
            )
            this.alpha = alpha
        }
        canvas.drawPath(tri, paint)
        val textLeft = mark.right + 22f
        val cx = textLeft + (badge.right - textLeft - 14f) / 2f
        val w = (badge.right - textLeft - 14f).toInt()
        text.draw(canvas, "Google Play'den indir", cx, badge.top + 36f, w, 28f, 1, assets.body, C.WHITE, alpha = a)
        text.draw(canvas, "Karalak", cx, badge.top + 76f, w, 46f, 1, assets.display, C.WHITE, alpha = a)
    }

    /** Fills [dst] with [bmp], cropped to its shape. */
    private fun cover(canvas: Canvas, bmp: Bitmap, dst: RectF) {
        if (bmp.isRecycled) return
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

    companion object {
        const val W = 1080
        const val H = 1920
        const val FPS = 30
    }
}
