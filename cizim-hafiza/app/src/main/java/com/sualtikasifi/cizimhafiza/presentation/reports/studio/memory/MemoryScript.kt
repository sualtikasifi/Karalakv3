package com.sualtikasifi.cizimhafiza.presentation.reports.studio.memory

import com.sualtikasifi.cizimhafiza.presentation.reports.studio.guess.GuessCopy
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.guess.SpokenLine
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MascotEmotion
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale
import kotlin.math.max

/** One drawing shown in the video, and what Karalak says about it (if anything). */
data class MemoryPick(val sampleId: String, val correct: Boolean, val comment: SpokenLine?)

/**
 * "Hafıza Tuzağı": how many drew a word, how many could not recognise their own drawing a little
 * later, a run of those drawings, why that is surprising, and the challenge to try it.
 *
 * The numbers are the archive's, written into [hook] by [MemoryCopy.hookText] — never by the
 * assistant — so what the video claims is what was counted.
 */
data class MemoryScript(
    val word: String,
    val drawn: Int,
    val forgot: Int,
    val delaySec: Int,
    val drawSeconds: Int,
    val hook: SpokenLine,
    val picks: List<MemoryPick>,
    val paradox: SpokenLine,
    val outro: SpokenLine
) {
    /** Every spoken line in order: the voice is recorded and laid out in this order. */
    fun lines(): List<SpokenLine> = buildList {
        add(hook)
        picks.forEach { p -> p.comment?.let { add(it) } }
        add(paradox)
        add(outro)
    }

    /** A share is only shown once there is enough behind it to mean something. */
    val showPercent: Boolean get() = drawn >= MemoryCopy.PERCENT_MIN
    val percent: Int get() = if (drawn == 0) 0 else (forgot * 100 + drawn / 2) / drawn
}

enum class MemoryPhase { Hook, Gallery, Paradox, Outro }

data class MemoryEvent(
    val phase: MemoryPhase,
    val pick: Int,
    val startMs: Long,
    val durationMs: Long,
    val line: SpokenLine? = null,
    val lineIndex: Int = -1
) {
    val endMs: Long get() = startMs + durationMs
    fun progress(t: Long): Float = ((t - startMs).toFloat() / durationMs).coerceIn(0f, 1f)
}

object MemoryTimeline {
    private const val HOOK_MIN = 3_600L
    private const val PICK_MIN = 1_500L
    private const val PARADOX_MIN = 3_000L
    private const val OUTRO_HOLD = 1_600L

    fun build(script: MemoryScript, durationOf: (index: Int, line: SpokenLine) -> Long): List<MemoryEvent> {
        val out = mutableListOf<MemoryEvent>()
        var t = 0L
        var index = 0
        fun add(phase: MemoryPhase, pick: Int, line: SpokenLine?, min: Long, extra: Long) {
            val d = if (line == null) min else max(min, durationOf(index, line) + extra)
            out += MemoryEvent(phase, pick, t, d, line, if (line == null) -1 else index)
            if (line != null) index++
            t += d
        }
        add(MemoryPhase.Hook, -1, script.hook, HOOK_MIN, 400)
        script.picks.forEachIndexed { i, p -> add(MemoryPhase.Gallery, i, p.comment, PICK_MIN, 300) }
        add(MemoryPhase.Paradox, -1, script.paradox, PARADOX_MIN, 400)
        add(MemoryPhase.Outro, -1, script.outro, 0, OUTRO_HOLD)
        return out
    }

    fun at(events: List<MemoryEvent>, t: Long): MemoryEvent = events.firstOrNull { t < it.endMs } ?: events.last()

    fun totalMs(events: List<MemoryEvent>): Long = events.lastOrNull()?.endMs ?: 0L
}

object MemoryCopy {
    /** Fewer drawings than this and the video gives counts only, never a percentage. */
    const val PERCENT_MIN = 50
    private val TR = Locale("tr")

    fun hookText(word: String, drawn: Int, forgot: Int, delaySec: Int): String =
        "$drawn kişi ${word.lowercase(TR)} çizdi. $forgot kişi, yaklaşık ${MemoryMath.roundForSpeech(delaySec)} saniye sonra kendi çizimini tanıyamadı."

    fun paradoxText(word: String, drawSeconds: Int): String =
        "Bilim insanları çizmenin hafızayı güçlendirdiğini söylüyor. Ama Karalak'ta ${word.lowercase(TR)} için yalnızca $drawSeconds saniyen var."

    const val OUTRO = "Sence sen hatırlar mısın? Bugünün beş kelimesi karalak.com'da seni bekliyor."

    /** The assistant's picks: `{"picks":[{"index":3,"emotion":"…","line":"…"}…],"outro":{…},"caption":"…","hashtags":"…"}`. */
    data class Picks(val picks: List<Pair<Int, SpokenLine?>>, val outro: SpokenLine?, val caption: String, val hashtags: String)

    fun parsePicks(text: String, candidates: Int): Picks? {
        val o = GuessCopy.objectIn(text) ?: return null
        val picks = runCatching {
            o["picks"]!!.jsonArray.mapNotNull { e ->
                val p = e.jsonObject
                val i = p["index"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
                if (i !in 1..candidates) return@mapNotNull null
                val line = p["line"]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
                (i - 1) to line?.let { SpokenLine(it, MascotEmotion.of(p["emotion"]?.jsonPrimitive?.contentOrNull)) }
            }.distinctBy { it.first }
        }.getOrNull().orEmpty()
        if (picks.isEmpty()) return null
        val outro = (o["outro"] as? JsonObject)?.let { oo ->
            oo["line"]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
                ?.let { SpokenLine(it, MascotEmotion.of(oo["emotion"]?.jsonPrimitive?.contentOrNull)) }
        }
        return Picks(
            picks, outro,
            o["caption"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty(),
            o["hashtags"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        )
    }
}
