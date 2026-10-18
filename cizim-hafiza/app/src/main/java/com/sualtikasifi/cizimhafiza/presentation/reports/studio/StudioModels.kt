package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import com.sualtikasifi.cizimhafiza.domain.model.DrawingStroke

/** How the video is put together: what the viewer is asked and how long each drawing stays. */
enum class StudioTemplate(
    val title: String,
    val blurb: String,
    val minCount: Int,
    val maxCount: Int,
    val defaultHook: String,
    val defaultAsk: String
) {
    Quiz(
        "Bu ne?", "Çizim canlanır, 3 saniye düşündürür, cevap açılır", 1, 5,
        "5 dakika önce bunları ben çizdim. Ne olduklarını hatırlamıyorum.", "Bu ne?"
    ),
    Speed(
        "Hızlı tur", "Arka arkaya çizimler; izleyen kaçını bildiğini sayar", 3, 8,
        "Kaçını bilebilirsin? Say bakalım.", "Bu ne?"
    ),
    Versus(
        "Aynı kelime", "Aynı kelimenin iki çizimi alt alta; hangisi daha iyi?", 2, 2,
        "Aynı kelime, iki çizim. Hangisi daha iyi?", "Üstteki mi, alttaki mi?"
    )
}

/** One drawing made on this phone, ready to be replayed. */
data class StudioDrawing(
    val id: Long,
    val word: String,
    val category: String,
    val strokes: List<DrawingStroke>,
    val correct: Boolean
)

/** The words on screen. */
data class StudioText(val hook: String, val ask: String, val outro: String)

/** Everything a video is made from. */
data class VideoSpec(val template: StudioTemplate, val drawings: List<StudioDrawing>, val text: StudioText)

enum class SegmentKind { Hook, Draw, Ask, Reveal, Outro }

/** One stretch of the video. [item] is the drawing it is about, -1 for the opening and the closing. */
data class Segment(val kind: SegmentKind, val item: Int, val startMs: Long, val durationMs: Long) {
    val endMs: Long get() = startMs + durationMs

    /** 0 at the start of this stretch, 1 at its end. */
    fun progress(tMs: Long): Float = ((tMs - startMs).toFloat() / durationMs).coerceIn(0f, 1f)
}

/**
 * The video's running order. Kept apart from the drawing code so the timing can be checked
 * without a device: the opening line, then per drawing the replay, the pause that asks, the
 * answer, and the closing card.
 */
object StudioTimeline {
    const val FPS = 30

    fun build(template: StudioTemplate, count: Int): List<Segment> {
        val out = mutableListOf<Segment>()
        var t = 0L
        fun add(kind: SegmentKind, item: Int, durationMs: Long) {
            out += Segment(kind, item, t, durationMs)
            t += durationMs
        }
        add(SegmentKind.Hook, -1, if (template == StudioTemplate.Speed) 1800 else 2200)
        when (template) {
            StudioTemplate.Quiz -> repeat(count) { i ->
                add(SegmentKind.Draw, i, 3000); add(SegmentKind.Ask, i, 3000); add(SegmentKind.Reveal, i, 1600)
            }
            StudioTemplate.Speed -> repeat(count) { i ->
                add(SegmentKind.Draw, i, 1500); add(SegmentKind.Ask, i, 1200); add(SegmentKind.Reveal, i, 900)
            }
            // both drawings play at once, so the whole comparison is one item
            StudioTemplate.Versus -> {
                add(SegmentKind.Draw, 0, 3500); add(SegmentKind.Ask, 0, 3000); add(SegmentKind.Reveal, 0, 1800)
            }
        }
        add(SegmentKind.Outro, -1, 2500)
        return out
    }

    fun totalMs(segments: List<Segment>): Long = segments.lastOrNull()?.endMs ?: 0L

    /** The stretch playing at [tMs]; past the end, the closing card. */
    fun at(segments: List<Segment>, tMs: Long): Segment = segments.firstOrNull { tMs < it.endMs } ?: segments.last()

    fun frameCount(totalMs: Long): Int = ((totalMs * FPS) / 1000).toInt().coerceAtLeast(1)
}

/**
 * Reads the assistant's answer for the studio: one labelled line each for the opening line, the
 * question, the closing line, the post caption and the hashtags. Anything missing stays null so
 * the screen keeps what was there.
 */
object StudioCopy {
    data class Parsed(val hook: String?, val ask: String?, val outro: String?, val caption: String?, val hashtags: String?)

    private val line = Regex("""^\s*[-*]?\s*\**\s*(KANCA|SORU|KAPAN[Iİı][SŞş]|A[CÇç][Iİı]KLAMA|ET[IİıiI]KETLER)\s*\**\s*:\s*\**\s*(.+?)\s*$""", RegexOption.IGNORE_CASE)

    fun parse(text: String): Parsed {
        val found = mutableMapOf<String, String>()
        text.lineSequence().forEach { raw ->
            val m = line.find(raw) ?: return@forEach
            val key = m.groupValues[1].uppercase(java.util.Locale.ROOT)
                .replace('Ş', 'S').replace('Ç', 'C').replace('İ', 'I')
            val value = m.groupValues[2].trim().trim('"', '“', '”', '*').trim()
            if (value.isNotEmpty() && key !in found) found[key] = value
        }
        return Parsed(found["KANCA"], found["SORU"], found["KAPANIS"], found["ACIKLAMA"], found["ETIKETLER"])
    }
}
