package com.sualtikasifi.cizimhafiza.presentation.reports.studio.guess

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.Ease
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StrokeReplay
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioAssets
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioColors as C
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioDrawing
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.TextPainter
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MascotEmotion
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MascotSprites
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MouthState
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.mouthFor
import java.util.Locale
import kotlin.math.min
import kotlin.math.sin

/** The person on the right-hand tile: their name (or an anonymous label), level, chosen frame and picture. */
data class PlayerCard(
    val name: String,
    val level: Int,
    val frame: Bitmap?,
    val faceDiameterFraction: Float,
    val faceOffsetXFraction: Float,
    val faceOffsetYFraction: Float,
    val photo: Bitmap?
)

/**
 * Draws "Karalak Bilir mi?" frames in 1080×1920: a video call between Karalak (left tile) and the
 * player (right tile), the score between them, Karalak's words as karaoke subtitles, and the
 * drawing building up on a sheet of paper below. Everything a viewer must read sits clear of the
 * band TikTok and Reels cover with their buttons and caption.
 */
class GuessRenderer(
    private val assets: StudioAssets,
    private val sprites: MascotSprites,
    private val script: GuessScript,
    private val drawings: Map<Long, StudioDrawing>,
    private val player: PlayerCard,
    /** Voice loudness per video frame for each line (by GuessScript.lines() index); null in the silent preview. */
    private val voice: Map<Int, FloatArray>?
) {
    val events: List<GuessEvent> = GuessTimeline.build(script) { i, line ->
        voice?.get(i)?.let { env -> env.size * 1000L / FPS } ?: GuessTimeline.estimateMs(line.text)
    }
    val totalMs: Long = GuessTimeline.totalMs(events)

    private val replays = script.rounds.associate { r -> r.drawingId to StrokeReplay(drawings[r.drawingId]?.strokes.orEmpty()) }
    private val text = TextPainter()
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val ink = StrokeReplay.inkPaint()

    private val mascotTile = RectF(40f, 150f, 525f, 640f)
    private val playerTile = RectF(555f, 150f, 1040f, 640f)
    private val paper = RectF(130f, 900f, 950f, 1500f)

    fun draw(canvas: Canvas, t: Long) {
        canvas.drawColor(C.CREAM)
        val e = GuessTimeline.at(events, t)
        val speaking = e.line != null && t - e.startMs < spokenMs(e)
        val level = voiceLevel(e, t)

        drawMascotTile(canvas, e, t, speaking, level)
        drawPlayerTile(canvas, e, t)
        drawScore(canvas, t)
        drawSubtitle(canvas, e, t)
        drawPaper(canvas, e, t)
        drawFooter(canvas)
        if (e.phase == GuessPhase.Outro) drawEndCard(canvas, e, t, speaking, level)
    }

    // ---- timing helpers

    private fun spokenMs(e: GuessEvent): Long {
        val l = e.line ?: return 0L
        return voice?.get(e.lineIndex)?.let { it.size * 1000L / FPS } ?: GuessTimeline.estimateMs(l.text)
    }

    /** How loud the voice is now: the recorded line when there is one, a lively stand-in in the silent preview. */
    private fun voiceLevel(e: GuessEvent, t: Long): Float {
        val l = e.line ?: return 0f
        val local = t - e.startMs
        val env = voice?.get(e.lineIndex)
        if (env != null) {
            val i = (local * FPS / 1000).toInt()
            return if (i in env.indices) env[i] else 0f
        }
        if (local >= GuessTimeline.estimateMs(l.text)) return 0f
        return (0.55f + 0.45f * sin(local / 55.0).toFloat()).coerceIn(0f, 1f) * if ((local / 260) % 4 == 3L) 0.2f else 1f
    }

    // ---- the two video tiles

    private fun tile(canvas: Canvas, r: RectF, bg: Int, active: Boolean) {
        fill.color = C.SHADOW
        canvas.drawRoundRect(RectF(r.left, r.top + 10f, r.right, r.bottom + 10f), 40f, 40f, fill)
        fill.color = bg
        canvas.drawRoundRect(r, 40f, 40f, fill)
        line.color = if (active) C.ORANGE else C.CARD_EDGE
        line.strokeWidth = if (active) 9f else 4f
        canvas.drawRoundRect(r, 40f, 40f, line)
    }

    private fun nameTag(canvas: Canvas, r: RectF, name: String, badge: String?) {
        val p = text.paint(34f, assets.body, C.WHITE)
        val bp = text.paint(26f, assets.body, C.WHITE)
        val nameW = min(p.measureText(name), r.width() - 140f)
        val badgeW = badge?.let { bp.measureText(it) + 28f } ?: 0f
        val w = nameW + 40f + (if (badge != null) badgeW + 12f else 0f)
        val box = RectF(r.left + 20f, r.bottom - 82f, r.left + 20f + w, r.bottom - 22f)
        fill.color = 0xB3221E1A.toInt()
        canvas.drawRoundRect(box, 30f, 30f, fill)
        val shown = android.text.TextUtils.ellipsize(name, p, nameW, android.text.TextUtils.TruncateAt.END).toString()
        canvas.drawText(shown, box.left + 20f, box.centerY() + 12f, p)
        if (badge != null) {
            val b = RectF(box.left + 20f + nameW + 12f, box.top + 12f, box.left + 20f + nameW + 12f + badgeW, box.bottom - 12f)
            fill.color = C.ORANGE
            canvas.drawRoundRect(b, 18f, 18f, fill)
            canvas.drawText(badge, b.left + 14f, b.centerY() + 9f, bp)
        }
    }

    private fun liveTag(canvas: Canvas, r: RectF, t: Long) {
        val box = RectF(r.left + 20f, r.top + 20f, r.left + 168f, r.top + 70f)
        fill.color = 0xB3221E1A.toInt()
        canvas.drawRoundRect(box, 25f, 25f, fill)
        fill.color = 0xFFFF3B30.toInt()
        fill.alpha = (180 + 75 * sin(t / 300.0)).toInt().coerceIn(0, 255)
        canvas.drawCircle(box.left + 28f, box.centerY(), 10f, fill)
        fill.alpha = 255
        canvas.drawText("CANLI", box.left + 48f, box.centerY() + 10f, text.paint(28f, assets.body, C.WHITE))
    }

    private fun drawMascotTile(canvas: Canvas, e: GuessEvent, t: Long, speaking: Boolean, level: Float) {
        tile(canvas, mascotTile, 0xFFFFE8CC.toInt(), speaking)
        canvas.save()
        canvas.clipRect(mascotTile.left + 4f, mascotTile.top + 4f, mascotTile.right - 4f, mascotTile.bottom - 4f)
        val emotion = e.line?.emotion ?: idleEmotion(e)
        drawMascot(canvas, RectF(mascotTile.left + 20f, mascotTile.top + 40f, mascotTile.right - 20f, mascotTile.bottom), emotion, speaking, level, t)
        canvas.restore()
        liveTag(canvas, mascotTile, t)
        nameTag(canvas, mascotTile, "Karalak", "YZ")
    }

    /** What Karalak looks like while nobody is talking: curious while a drawing builds, pleased or sheepish after a verdict. */
    private fun idleEmotion(e: GuessEvent): MascotEmotion = when (e.phase) {
        GuessPhase.Draw -> if (e.stage == 0) MascotEmotion.Thinking else MascotEmotion.Suspicious
        GuessPhase.Verdict -> if (script.rounds.getOrNull(e.round)?.correct == true) MascotEmotion.Excited else MascotEmotion.Shocked
        else -> MascotEmotion.Neutral
    }

    /** Karalak in [box], sitting on its bottom edge like someone in front of a webcam. */
    private fun drawMascot(canvas: Canvas, box: RectF, emotion: MascotEmotion, speaking: Boolean, level: Float, t: Long) {
        val blink = !speaking && (t % 3_400L) < 140L
        val mouth = when {
            speaking -> mouthFor(level)
            blink -> MouthState.Blink
            else -> MouthState.Closed
        }
        val sprite = sprites.frame(emotion, mouth)
        val bmp = sprite ?: sprites.fallback ?: return
        val scale = min(box.width() / bmp.width, box.height() / bmp.height)
        val w = bmp.width * scale
        val h = bmp.height * scale
        // a little life: breathing when still, a bob with the voice when talking (the stand-in mascot only has the bob)
        val breathe = 1f + 0.012f * sin(t / 520.0).toFloat()
        val bob = if (speaking) level * (if (sprite == null) 16f else 6f) else 0f
        val dst = RectF(box.centerX() - w / 2f, box.bottom - h - bob, box.centerX() + w / 2f, box.bottom - bob)
        canvas.save()
        canvas.scale(breathe, breathe, dst.centerX(), dst.bottom)
        if (sprite == null && speaking) canvas.rotate(2.5f * sin(t / 90.0).toFloat() * level, dst.centerX(), dst.bottom)
        canvas.drawBitmap(bmp, null, dst, fill)
        canvas.restore()
    }

    private fun drawPlayerTile(canvas: Canvas, e: GuessEvent, t: Long) {
        tile(canvas, playerTile, 0xFFE6EEFB.toInt(), false)
        val size = 290f
        val cx = playerTile.centerX()
        val cy = playerTile.top + 228f
        val badge = RectF(cx - size / 2f, cy - size / 2f, cx + size / 2f, cy + size / 2f)
        val faceD = size * player.faceDiameterFraction * 1.06f
        val fx = cx + size * player.faceOffsetXFraction
        val fy = cy + size * player.faceOffsetYFraction
        val photo = player.photo
        if (photo != null) {
            val shader = BitmapShader(photo, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            val s = faceD / min(photo.width, photo.height)
            shader.setLocalMatrix(Matrix().apply {
                setScale(s, s)
                postTranslate(fx - photo.width * s / 2f, fy - photo.height * s / 2f)
            })
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader }
            canvas.drawCircle(fx, fy, faceD / 2f, p)
        } else {
            fill.color = 0xFFFFF4E3.toInt()
            canvas.drawCircle(fx, fy, faceD / 2f, fill)
            text.draw(canvas, player.level.toString(), fx, fy, faceD.toInt(), faceD * 0.45f, 1, assets.display, C.INK)
        }
        player.frame?.let { canvas.drawBitmap(it, null, badge, fill) }
        nameTag(canvas, playerTile, player.name, "Sv. ${player.level}")

        // their own guess, said back as a bubble once the verdict is in
        val round = script.rounds.getOrNull(e.round)
        val answer = round?.playerAnswer?.trim().orEmpty()
        if (answer.isNotEmpty() && (e.phase == GuessPhase.Verdict || e.phase == GuessPhase.Reaction)) {
            val appear = if (e.phase == GuessPhase.Verdict) Ease.backOut(min(1f, e.progress(t) * 2f)) else 1f
            val box = RectF(playerTile.left + 24f, playerTile.top + 24f, playerTile.right - 24f, playerTile.top + 150f)
            canvas.save()
            canvas.scale(appear, appear, box.centerX(), box.bottom)
            fill.color = C.WHITE
            canvas.drawRoundRect(box, 28f, 28f, fill)
            line.color = C.CARD_EDGE; line.strokeWidth = 3f
            canvas.drawRoundRect(box, 28f, 28f, line)
            text.draw(canvas, "Kendi tahmini:", box.centerX(), box.top + 34f, (box.width() - 30f).toInt(), 26f, 1, assets.body, C.MUTED)
            val mark = if (round!!.word.equals(answer, ignoreCase = true) || GuessCopy.matches(answer, round.word)) "✓ " else "✗ "
            text.draw(canvas, mark + "“$answer”", box.centerX(), box.top + 88f, (box.width() - 30f).toInt(), 40f, 1, assets.display, C.INK)
            canvas.restore()
        }
    }

    // ---- between and below the tiles

    private fun drawScore(canvas: Canvas, t: Long) {
        val (m, p) = GuessTimeline.scoreAt(script, events, t)
        val label = "Karalak  $m : $p  ${player.name.take(12)}"
        val paint = text.paint(36f, assets.display, C.WHITE)
        val w = paint.measureText(label) + 70f
        val box = RectF(540f - w / 2f, 600f, 540f + w / 2f, 684f)
        fill.color = C.INK
        canvas.drawRoundRect(box, 42f, 42f, fill)
        canvas.drawText(label, box.left + 35f, box.centerY() + 13f, paint)
    }

    private fun drawSubtitle(canvas: Canvas, e: GuessEvent, t: Long) {
        val l = e.line ?: return
        if (e.phase == GuessPhase.Outro) return
        val p = ((t - e.startMs).toFloat() / spokenMs(e).coerceAtLeast(1)).coerceIn(0f, 1f)
        text.karaoke(canvas, l.text, p, 540f, 790f, 980, 56f, 2, assets.display, C.INK, C.FAINT)
    }

    private fun drawPaper(canvas: Canvas, e: GuessEvent, t: Long) {
        fill.color = C.SHADOW
        canvas.drawRoundRect(RectF(paper.left, paper.top + 12f, paper.right, paper.bottom + 12f), 44f, 44f, fill)
        fill.color = C.CARD
        canvas.drawRoundRect(paper, 44f, 44f, fill)
        line.color = C.CARD_EDGE; line.strokeWidth = 4f
        canvas.drawRoundRect(paper, 44f, 44f, line)

        if (e.round < 0) {
            // before the first drawing: what is coming
            text.draw(canvas, "?", paper.centerX(), paper.centerY() - 40f, 400, 260f, 1, assets.display, C.FAINT)
            text.draw(canvas, "${script.rounds.size} çizim · kelimeler gizli", paper.centerX(), paper.bottom - 80f, 700, 38f, 1, assets.body, C.MUTED)
            return
        }
        val round = script.rounds[e.round]
        val fraction = when (e.phase) {
            GuessPhase.Draw -> e.drawFrom + (e.drawTo - e.drawFrom) * Ease.out(e.progress(t))
            GuessPhase.Speak -> e.drawTo
            else -> 1f
        }
        // once the verdict is in, the drawing moves up to make room for the word under it
        val lift = when (e.phase) {
            GuessPhase.Verdict -> WORD_BAND * Ease.out(min(1f, e.progress(t) * 3f))
            GuessPhase.Reaction -> WORD_BAND
            else -> 0f
        }
        replays[round.drawingId]?.draw(canvas, RectF(paper.left, paper.top + 40f, paper.right, paper.bottom - lift), fraction, ink)
        chip(canvas, "Çizim ${e.round + 1}/${script.rounds.size}", paper.left + 24f, paper.top + 24f)

        when (e.phase) {
            GuessPhase.Draw, GuessPhase.Speak ->
                text.draw(canvas, "Karalak izliyor" + ".".repeat(((t / 350) % 4).toInt()), 540f, paper.bottom + 60f, 700, 32f, 1, assets.body, C.MUTED)
            GuessPhase.Verdict, GuessPhase.Reaction -> verdict(canvas, round, if (e.phase == GuessPhase.Verdict) e.progress(t) else 1f)
            else -> Unit
        }
    }

    private fun verdict(canvas: Canvas, round: GuessRound, p: Float) {
        val pop = Ease.backOut(min(1f, p * 2.2f))
        // stamp in the corner of the paper
        val cx = paper.right - 110f
        val cy = paper.top + 110f
        canvas.save()
        canvas.scale(pop, pop, cx, cy)
        fill.color = if (round.correct) C.GOOD else C.BAD
        canvas.drawCircle(cx, cy, 74f, fill)
        text.draw(canvas, if (round.correct) "✓" else "✗", cx, cy, 140, 96f, 1, assets.display, C.WHITE)
        canvas.restore()
        // the real word along the bottom of the paper, in the room the drawing made for it
        val band = RectF(paper.left, paper.bottom - WORD_BAND, paper.right, paper.bottom)
        canvas.save()
        canvas.scale(pop, pop, band.centerX(), band.centerY())
        text.draw(canvas, round.word.uppercase(TR), band.centerX(), band.centerY() - 10f, 760, 84f, 1, assets.display, C.ORANGE_DARK)
        canvas.restore()
        if (!round.correct && round.finalGuess.isNotBlank()) {
            text.draw(canvas, "Karalak “${round.finalGuess}” dedi", 540f, paper.bottom + 60f, 760, 34f, 1, assets.body, C.MUTED, alpha = min(1f, p * 3f))
        }
    }

    private fun chip(canvas: Canvas, label: String, left: Float, top: Float) {
        val p = text.paint(28f, assets.body, C.INK)
        val box = RectF(left, top, left + p.measureText(label) + 40f, top + 52f)
        fill.color = C.CREAM_DEEP
        canvas.drawRoundRect(box, 26f, 26f, fill)
        canvas.drawText(label, box.left + 20f, box.centerY() + 10f, p)
    }

    private fun drawFooter(canvas: Canvas) {
        text.draw(canvas, "karalak.com · Google Play: Karalak", 540f, 1640f, 900, 32f, 1, assets.body, C.MUTED)
    }

    // ---- the closing card

    private fun drawEndCard(canvas: Canvas, e: GuessEvent, t: Long, speaking: Boolean, level: Float) {
        val a = Ease.out(min(1f, e.progress(t) * 4f))
        fill.color = C.CREAM
        fill.alpha = (a * 255).toInt()
        canvas.drawRect(0f, 0f, W.toFloat(), H.toFloat(), fill)
        fill.alpha = 255
        if (a < 0.6f) return
        canvas.save()
        canvas.translate(0f, (1f - a) * 60f)
        drawMascot(canvas, RectF(240f, 170f, 840f, 820f), e.line?.emotion ?: MascotEmotion.Wave, speaking, level, t)
        val m = script.mascotScore
        val p = script.playerScore
        val result = when {
            m > p -> "Karalak $m–$p kazandı!"
            p > m -> "${player.name.take(16)} $p–$m kazandı!"
            else -> "$m–$p berabere!"
        }
        text.draw(canvas, result, 540f, 900f, 960, 76f, 1, assets.display, C.INK)
        e.line?.let { l ->
            val prog = ((t - e.startMs).toFloat() / spokenMs(e).coerceAtLeast(1)).coerceIn(0f, 1f)
            text.karaoke(canvas, l.text, prog, 540f, 1040f, 960, 50f, 2, assets.display, C.ORANGE_DARK, C.FAINT)
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
        private const val WORD_BAND = 150f
        private val TR = Locale("tr")
    }
}
