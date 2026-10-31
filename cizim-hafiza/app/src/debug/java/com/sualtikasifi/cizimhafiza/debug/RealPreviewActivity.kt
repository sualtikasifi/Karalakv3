package com.sualtikasifi.cizimhafiza.debug

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import com.sualtikasifi.cizimhafiza.domain.model.DrawingPoint
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioAssets
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.CardLayout
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.ClipFrames
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealDrawing
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealOpening
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealTimeline
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.TimedWord
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealPhase
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealProject
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealRenderer
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealReveal
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.VoicedLine
import java.io.File

/**
 * Debug-only: draws a few frames of "Gerçek Olsaydı" from made-up drawings and a made-up clip into
 * the PNG files in files/preview, so what the video looks like can be checked without the developer panel, an
 * account or an assistant key. start it with `adb shell am start -n <pkg>/com.sualtikasifi.cizimhafiza.debug.RealPreviewActivity`,
 * wait for files/preview/done.txt, then read the PNGs with `run-as`.
 */
class RealPreviewActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Thread {
            val out = File(filesDir, "preview").apply { deleteRecursively(); mkdirs() }
            runCatching { render(out) }.onFailure { File(out, "error.txt").writeText(it.stackTraceToString()) }
            File(out, "done.txt").writeText("ok")
            runOnUiThread { finish() }
        }.start()
    }

    private fun render(out: File) {
        val assets = StudioAssets(this)
        val clip = File(cacheDir, "real_preview.mp4").also { f -> this.assets.open("real_preview.mp4").use { i -> f.outputStream().use { i.copyTo(it) } } }
        val frames = ClipFrames(clip, exact = false)

        val drawings = listOf(
            RealDrawing("a", rocket(), "roket", 7, "SinanK_34", 41, ""),
            RealDrawing("b", spoon(), "bağlama", 7, "Zeynep", 12, com.sualtikasifi.cizimhafiza.domain.model.AvatarFrame.entries.filter { !it.retired }.last().name),
            RealDrawing("c", cat(), "kedi", 5, "", 28, "")
        )
        val bmp = Bitmap.createBitmap(RealRenderer.W, RealRenderer.H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        var n = 0
        fun shoot(name: String, renderer: RealRenderer, t: Long) {
            canvas.drawColor(0xFF000000.toInt())
            renderer.draw(canvas, t)
            File(out, "%02d_%s.png".format(n++, name)).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        // the video is the first drawing's: its hook, its clip, its closing
        fun project(first: RealDrawing, hook: VoicedLine, opening: RealOpening, outro: VoicedLine = VoicedLine("Bu rokete kim biner? Yorumlara yaz!")) = RealProject(
            drawings = listOf(first) + drawings.filter { it.id != first.id },
            reveals = (listOf(first) + drawings.filter { it.id != first.id }).map { RealReveal(it.id, clip = it.id + ".mp4", clipMs = 5_000L) }.mapIndexed { i, r ->
                if (i > 0) r else r.copy(hook = hook, outro = outro, opening = opening)
            }
        )
        fun renderer(p: RealProject, endCardMs: Long = 0L, promoMs: Long = 0L, layout: CardLayout = CardLayout()) =
            RealRenderer(assets, p, clips = p.drawings.associate { it.id to frames }, clipFiles = emptyMap(), clipGain = 0f, layout = layout,
                promo = frames.takeIf { promoMs > 0 }, promoMs = promoMs, endCardMs = endCardMs)

        // a voiced hook with its words timed, as ElevenLabs gives them: the subtitle comes in word by word, letter by letter
        val voiced = timed("7 saniyede roket çizebilir misin?", 1_900L)
        // 1. the classic opening: the cover, the subtitle coming in, done, the clip opening out; the clip; the closing; the end card
        val classic = renderer(project(drawings[0], voiced, RealOpening.Classic), endCardMs = RealTimeline.END_CARD_SILENT_MS)
        classic.events.forEach { e ->
            when (e.phase) {
                RealPhase.Hook -> {
                    shoot("classic_first", classic, 0L)
                    shoot("classic_caption_word2", classic, e.startMs + 700)
                    shoot("classic_caption_word4", classic, e.startMs + 1_350)
                    shoot("classic_done", classic, e.durationMs - 500)
                    shoot("classic_open", classic, e.durationMs - 330)
                }
                RealPhase.Clip -> shoot("clip_mid", classic, e.startMs + 1_800)
                RealPhase.Outro -> {
                    shoot("outro_coming_in", classic, e.startMs + 900)
                    shoot("outro_end", classic, e.endMs - 60)
                }
                RealPhase.EndCard -> {
                    shoot("endcard_in", classic, e.startMs + 140)
                    shoot("endcard_slogan", classic, e.startMs + 700)
                    shoot("endcard_steps", classic, e.startMs + 1_250)
                    shoot("endcard_end", classic, e.endMs - 60)
                }
                else -> Unit
            }
        }
        // 2. the teaser opening: the real thing full frame (the cover), shrinking into the canvas, then the drawing
        val teaser = renderer(project(drawings[0], voiced, RealOpening.Teaser))
        shoot("teaser_first", teaser, 0L)
        shoot("teaser_shrink", teaser, RealTimeline.TEASE_HOLD + 220)
        shoot("teaser_draw", teaser, RealTimeline.TEASE_MS + 500)
        // 3. hooks of other lengths and drawings of other shapes, not voiced (read in at a reading pace); a long one goes on over the clip
        shoot("hook_short_done", renderer(project(drawings[1], VoicedLine("Bu ne?"), RealOpening.Classic)), 1_600L)
        val long = renderer(project(drawings[2], VoicedLine("Bu kediyi gerçek hayatta görsen kaç saniyede tanırsın, söyle bakalım?"), RealOpening.Classic))
        shoot("hook_long_coming_in", long, 1_600L)
        shoot("hook_long_over_clip", long, 4_700L)
        // 4. the end card is the game's own picture, over a short voiced line (a short card) and a long one (a long card)
        val shortLine = project(drawings[0], voiced, RealOpening.Classic).let { p ->
            p.copy(reveals = p.reveals.map { r -> if (r.sampleId == "a") r.copy(endLine = timed("Çizmek kolay, hatırlamak zor! Karalak'ta kendini dene!", 3_200L)) else r })
        }
        val short = renderer(shortLine, endCardMs = shortLine.featured!!.endCardMs)
        short.events.first { it.phase == RealPhase.EndCard }.let { e -> shoot("endcard_short_voice_${e.durationMs}ms", short, e.endMs - 60) }
        val longLine = project(drawings[0], voiced, RealOpening.Classic).let { p ->
            p.copy(reveals = p.reveals.map { r -> if (r.sampleId == "a") r.copy(endLine = timed("Birkaç saniyede çiz, sonra ne çizdiğini hatırla. Kulağa kolay geliyor, değil mi?", 6_400L)) else r })
        }
        val long2 = renderer(longLine, endCardMs = longLine.featured!!.endCardMs)
        long2.events.first { it.phase == RealPhase.EndCard }.let { e -> shoot("endcard_long_voice_${e.durationMs}ms", long2, e.startMs + 120); shoot("endcard_long_end", long2, e.endMs - 60) }
        // 5. the promo still takes the end card's place when it is chosen
        val promo = renderer(project(drawings[0], voiced, RealOpening.Classic), promoMs = 2_000L, endCardMs = RealTimeline.END_CARD_SILENT_MS)
        promo.events.firstOrNull { it.phase == RealPhase.Promo }?.let { shoot("promo_mid", promo, it.startMs + 1_000) }
        // 6. the card moved and resized, as the panel's sliders can
        val clipMid = classic.events.first { it.phase == RealPhase.Clip }.startMs + 1_800L
        shoot("layout_small", renderer(project(drawings[0], VoicedLine(), RealOpening.Classic), layout = CardLayout(0.7f, 0.95f, 0.62f)), clipMid)
        shoot("layout_big", renderer(project(drawings[0], VoicedLine(), RealOpening.Classic), layout = CardLayout(1.5f, 0.5f, 0.5f)), clipMid)
        frames.close()
    }

    /** A line as if ElevenLabs had voiced it in [voiceMs] and timed its words: each word gets a share of the time by its length. */
    private fun timed(line: String, voiceMs: Long): VoicedLine {
        val parts = line.split(' ')
        val total = parts.sumOf { it.length + 1 }
        var at = 0L
        val words = parts.map { w ->
            val len = (voiceMs - 300L) * (w.length + 1) / total
            TimedWord(w, at, at + len - 40L).also { at += len }
        }
        return VoicedLine(line, voice = "hook.mp3", voiceMs = voiceMs, words = words)
    }

    private fun path(vararg p: Pair<Float, Float>) = p.map { DrawingPoint(it.first, it.second) }

    private fun rocket() = listOf(
        path(40f to 30f, 55f to 5f, 60f to 30f, 62f to 60f, 38f to 62f, 40f to 30f),
        path(38f to 62f, 40f to 150f, 64f to 150f, 62f to 60f),
        path(42f to 150f, 40f to 185f), path(50f to 150f, 54f to 190f), path(58f to 150f, 66f to 188f)
    )

    private fun spoon() = listOf(
        path(10f to 40f, 20f to 22f, 38f to 16f, 52f to 30f, 48f to 52f, 28f to 62f, 12f to 54f, 10f to 40f),
        path(50f to 38f, 120f to 34f, 124f to 44f, 52f to 48f)
    )

    private fun cat() = listOf(
        path(20f to 60f, 24f to 25f, 42f to 40f, 64f to 40f, 82f to 25f, 86f to 60f, 70f to 90f, 36f to 90f, 20f to 60f),
        path(38f to 58f, 42f to 58f), path(64f to 58f, 68f to 58f), path(46f to 74f, 56f to 78f, 62f to 74f)
    )

    private fun tree() = listOf(
        path(46f to 150f, 48f to 90f, 56f to 90f, 58f to 150f),
        path(10f to 90f, 6f to 50f, 30f to 20f, 60f to 8f, 90f to 24f, 100f to 56f, 84f to 92f, 10f to 90f)
    )
}
