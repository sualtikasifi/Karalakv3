package com.sualtikasifi.cizimhafiza.presentation.reports.studio.real

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
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
 * over each, in the top left, the Karalak card (the game's wooden frame with its logo): the player's drawing in
 * the middle of it, the player's frame with the default picture in the round slot at the bottom and their name
 * (or "anonymous") beside it. A small "made with AI" note under the card. Plain cuts between clips, no voice,
 * no subtitles. The right edge and the bottom band stay clear for the buttons and caption TikTok, Reels and
 * Shorts put there.
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
    private val card = assets.picture(R.drawable.real_card)
    private val dino = assets.picture(R.drawable.avatar_dino)

    // the card is 941 x 1672 (real_card): everything below is placed in its own pixels, then scaled
    private val cardBox = RectF(34f, 190f, 34f + CARD_W, 190f + CARD_W * 1672f / 941f)
    private val k = CARD_W / 941f
    /** The parchment in the middle of the card, where the drawing goes. */
    private val parchment = RectF(122f, 435f, 816f, 1322f).let { r -> RectF(x(r.left), y(r.top), x(r.right), y(r.bottom)) }
    /** The round slot at the bottom left (centre and diameter), for the frame and picture. */
    private val slotCx = x(199f)
    private val slotCy = y(1421f)
    private val slotD = 168f * k
    /** The dark plate beside it, for the name. */
    private val plate = RectF(305f, 1359f, 834f, 1488f).let { r -> RectF(x(r.left), y(r.top), x(r.right), y(r.bottom)) }
    private val labelBox = RectF(34f, cardBox.bottom + 22f, 34f + 470f, cardBox.bottom + 22f + 52f)

    private fun x(tx: Float) = cardBox.left + tx * k
    private fun y(ty: Float) = cardBox.top + ty * k

    private fun replay(id: String): StrokeReplay? = project.drawing(id)?.let { d -> replays.getOrPut(id) { StrokeReplay(d.strokes) } }

    override fun draw(canvas: Canvas, t: Long) {
        val e = RealTimeline.at(events, t)
        val reveal = shown.getOrNull(e.index) ?: return
        canvas.drawColor(C.INK)
        val frame = clips[reveal.sampleId]?.frameAt((t - e.clipAtMs).coerceIn(0L, e.clipMs))
        if (frame != null) cover(canvas, frame, RectF(0f, 0f, W.toFloat(), H.toFloat()))
        val drawing = project.drawing(reveal.sampleId) ?: return
        val local = t - e.startMs
        drawCard(canvas, drawing, Ease.backOut(min(1f, local / 450f)))
        drawLabel(canvas, Ease.out(min(1f, (local - 300L) / 400f)))
    }

    /** The Karalak card with the player's drawing, frame, picture and name on it; it pops in from its top left corner. */
    private fun drawCard(canvas: Canvas, d: RealDrawing, a: Float) {
        val bmp = card ?: return
        canvas.save()
        canvas.scale(a, a, cardBox.left, cardBox.top)
        // a soft shadow under the card
        fill.color = 0x40000000
        canvas.drawRoundRect(RectF(cardBox.left + 8f, cardBox.top + 22f, cardBox.right - 4f, cardBox.bottom + 12f), 36f, 36f, fill)
        canvas.drawBitmap(bmp, null, cardBox, fill)
        replay(d.id)?.draw(canvas, RectF(parchment.left + 8f, parchment.top + 8f, parchment.right - 8f, parchment.bottom - 8f), 1f, ink, 0.03f)
        drawAvatar(canvas, d)
        val name = if (d.named) "@${d.nickname}" else "Anonim oyuncu"
        text.draw(canvas, name, plate.centerX(), plate.centerY(), (plate.width() - 28f).toInt(), 38f, 1, assets.display, C.WHITE)
        canvas.restore()
    }

    /** The player's frame with the default picture in it, in the card's round slot: never their own photo. */
    private fun drawAvatar(canvas: Canvas, d: RealDrawing) {
        val frame = AvatarFrame.resolve(d.frameId, d.level.coerceAtLeast(1))
        val frameBmp = assets.picture(frame.drawableRes)
        val faceD = slotD * 0.94f
        val size = (faceD / (frame.faceDiameterFraction * 1.06f)).coerceAtMost(slotD * 1.5f)
        val fx = slotCx + size * frame.faceOffsetXFraction
        val fy = slotCy + size * frame.faceOffsetYFraction
        val photo = dino
        if (photo != null) {
            val shader = BitmapShader(photo, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            val s = faceD / min(photo.width, photo.height)
            shader.setLocalMatrix(Matrix().apply {
                setScale(s, s)
                postTranslate(fx - photo.width * s / 2f, fy - photo.height * s / 2f)
            })
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader }
            canvas.drawCircle(fx, fy, faceD / 2f, paint)
        }
        frameBmp?.let { canvas.drawBitmap(it, null, RectF(slotCx - size / 2f, slotCy - size / 2f, slotCx + size / 2f, slotCy + size / 2f), fill) }
    }

    /** The "made with AI" note platforms ask for, small, under the card. */
    private fun drawLabel(canvas: Canvas, a: Float) {
        if (a <= 0f) return
        fill.color = ((0x80 * a).toInt() shl 24) or 0x101014
        canvas.drawRoundRect(labelBox, 26f, 26f, fill)
        text.draw(canvas, "Yapay zekâ ile canlandırıldı", labelBox.centerX(), labelBox.centerY(), (labelBox.width() - 30).toInt(), 28f, 1, assets.body, C.WHITE, alpha = a)
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
        /** The card's width on the frame; its height follows its own shape. */
        private const val CARD_W = 450f
    }
}
