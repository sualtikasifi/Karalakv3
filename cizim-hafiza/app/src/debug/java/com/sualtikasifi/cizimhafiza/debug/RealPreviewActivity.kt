package com.sualtikasifi.cizimhafiza.debug

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import com.sualtikasifi.cizimhafiza.domain.model.DrawingPoint
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioAssets
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.guess.SpokenLine
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MascotEmotion
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MascotSprites
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.ClipFrames
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealDrawing
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealPhase
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealRenderer
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealReveal
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealRoast
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealScript
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealTimeline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
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
        val sprites = runBlocking(Dispatchers.IO) { MascotSprites.load(this@RealPreviewActivity) }
        val clip = File(cacheDir, "real_preview.mp4").also { f -> this.assets.open("real_preview.mp4").use { i -> f.outputStream().use { i.copyTo(it) } } }
        val frames = ClipFrames(clip, exact = false)

        val drawings = listOf(
            RealDrawing("a", rocket(), "roket", 7),
            RealDrawing("b", spoon(), "bağlama", 7),
            RealDrawing("c", cat(), "kedi", 5),
            RealDrawing("d", tree(), "ağaç", 9)
        )
        fun line(t: String, e: MascotEmotion) = SpokenLine(t, e)
        val script = RealScript(
            word = "", drawSeconds = 7,
            hook = line("Oyunculara birkaç saniyede çizdirdik. İşte Türkiye'nin çizimleri!", MascotEmotion.Excited),
            roasts = listOf(
                RealRoast("a", line("Bu roket mi, kalem mi?", MascotEmotion.Laughing)),
                RealRoast("b", line("Bağlama değil, kaşık!", MascotEmotion.Suspicious)),
                RealRoast("c", line("Kedi çok kızgın görünüyor.", MascotEmotion.Shocked)),
                RealRoast("d", line("Ağaç mı, brokoli mi?", MascotEmotion.Thinking))
            ),
            bridge = line("Peki bunlar gerçek olsaydı?", MascotEmotion.Thinking),
            reveals = listOf(
                RealReveal("a", line("Bu roket gerçekten uçar mı?", MascotEmotion.Shocked), clip = "a.mp4", clipMs = 3_000L),
                RealReveal("c", line("Bu kedi beni yer!", MascotEmotion.Shocked), clip = "c.mp4", clipMs = 3_000L)
            ),
            outro = line("Sen daha iyisini çizebilir misin? karalak.com'da dene!", MascotEmotion.Point)
        )
        val renderer = RealRenderer(
            assets, sprites, script, drawings.associateBy { it.id },
            clips = mapOf("a" to frames, "c" to frames), clipFiles = emptyMap(), clipGain = 0f, voice = null
        )
        val shots = buildList {
            val e = renderer.events
            e.first { it.phase == RealPhase.Hook }.let { add("hook_early" to it.startMs + 700); add("hook_full" to it.endMs - 150) }
            e.filter { it.phase == RealPhase.Roast }.forEachIndexed { i, r -> add("roast_$i" to r.startMs + 1_400) }
            e.first { it.phase == RealPhase.Bridge }.let { add("bridge" to it.startMs + 1_200) }
            e.first { it.phase == RealPhase.Reveal }.let {
                add("reveal_before" to it.startMs + 600)
                add("reveal_wipe" to it.photoAtMs - RealTimeline.WIPE / 2)
                add("reveal_full" to it.lineAtMs + 900)
            }
            e.first { it.phase == RealPhase.Outro }.let { add("outro" to it.startMs + 1_800) }
        }
        val bmp = Bitmap.createBitmap(RealRenderer.W, RealRenderer.H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        shots.forEachIndexed { i, (name, t) ->
            canvas.drawColor(0xFF000000.toInt())
            renderer.draw(canvas, t)
            File(out, "%02d_%s.png".format(i, name)).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        frames.close()
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
