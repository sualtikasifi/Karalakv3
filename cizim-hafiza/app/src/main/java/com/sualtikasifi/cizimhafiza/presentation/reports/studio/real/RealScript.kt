package com.sualtikasifi.cizimhafiza.presentation.reports.studio.real

import com.sualtikasifi.cizimhafiza.domain.model.DrawingPoint
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.guess.GuessCopy
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.guess.SpokenLine
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MascotEmotion
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/** One anonymous drawing from the archive, copied into the project so it survives the archive changing. */
@Serializable
data class RealDrawing(val id: String, val strokes: List<List<DrawingPoint>>)

/** A drawing in the opening run and Karalak's joke about it. */
@Serializable
data class RealRoast(val sampleId: String, val line: SpokenLine)

/**
 * A drawing turned real: its realistic picture (made in ChatGPT), optionally a short clip of it
 * moving (made in Gemini), and what Karalak says on seeing it. [image] and [clip] are file names in
 * the project's media folder.
 */
@Serializable
data class RealReveal(
    val sampleId: String,
    val line: SpokenLine,
    /** The doodle's literal shapes in English, written into the picture prompt so they are kept. */
    val describe: String = "",
    val image: String? = null,
    val clip: String? = null,
    val clipMs: Long = 0L
)

/**
 * "Gerçek Olsaydı": a word, a handful of quick drawings of it with Karalak's jokes, then the twist
 * — some of them as if they were real, picture and moving clip — and the challenge to draw better.
 */
@Serializable
data class RealScript(
    val word: String,
    val drawSeconds: Int,
    val hook: SpokenLine,
    val roasts: List<RealRoast>,
    val bridge: SpokenLine,
    val reveals: List<RealReveal>,
    val outro: SpokenLine
) {
    /** Only reveals with their realistic picture make it into the video. */
    val shownReveals: List<RealReveal> get() = reveals.filter { it.sampleId in roastIds && it.image != null }
    private val roastIds: Set<String> get() = roasts.map { it.sampleId }.toSet()

    /** Every spoken line, in the order the video says them; empty lines are silent and left out. */
    fun lines(): List<SpokenLine> = buildList {
        RealTimeline.spoken(hook)?.let { add(it) }
        roasts.forEach { r -> RealTimeline.spoken(r.line)?.let { add(it) } }
        val shown = shownReveals
        if (shown.isNotEmpty()) {
            RealTimeline.spoken(bridge)?.let { add(it) }
            shown.forEach { r -> RealTimeline.spoken(r.line)?.let { add(it) } }
        }
        RealTimeline.spoken(outro)?.let { add(it) }
    }
}

/** Everything the studio keeps for one "Gerçek Olsaydı" video while pictures are being made elsewhere. */
@Serializable
data class RealProject(
    val wordKey: String,
    /** The word in English, for the prompts. */
    val wordEn: String = "",
    val script: RealScript,
    val drawings: List<RealDrawing>
) {
    fun drawing(id: String): RealDrawing? = drawings.find { it.id == id }
    val media: Set<String> get() = script.reveals.flatMap { listOfNotNull(it.image, it.clip) }.toSet()
}

enum class RealPhase { Hook, Roast, Bridge, Reveal, Outro }

/**
 * One stretch of the video. A reveal runs in steps: the doodle held, wiped away to the picture
 * ([photoAtMs]), the picture with Karalak's line, then the clip if there is one ([clipAtMs] for
 * [clipMs]).
 */
data class RealEvent(
    val phase: RealPhase,
    val index: Int,
    val startMs: Long,
    val durationMs: Long,
    val line: SpokenLine? = null,
    val lineIndex: Int = -1,
    val lineAtMs: Long = startMs,
    val photoAtMs: Long = 0L,
    val clipAtMs: Long = 0L,
    val clipMs: Long = 0L
) {
    val endMs: Long get() = startMs + durationMs
    fun progress(t: Long): Float = ((t - startMs).toFloat() / durationMs).coerceIn(0f, 1f)
}

object RealTimeline {
    private const val HOOK_MIN = 3_200L
    private const val ROAST_MIN = 1_900L
    private const val ROAST_LEAD = 350L
    private const val BRIDGE_MIN = 2_400L
    const val DOODLE_HOLD = 900L
    const val WIPE = 700L
    private const val PHOTO_MIN = 2_400L
    private const val OUTRO_HOLD = 1_600L
    /** Gemini clips are about eight seconds; anything longer is cut here. */
    const val CLIP_MAX = 10_000L

    fun spoken(l: SpokenLine): SpokenLine? = l.takeIf { it.text.isNotBlank() }

    fun build(script: RealScript, durationOf: (index: Int, line: SpokenLine) -> Long): List<RealEvent> {
        val out = mutableListOf<RealEvent>()
        var t = 0L
        var index = 0
        fun said(l: SpokenLine?): Pair<SpokenLine?, Long> {
            val s = l?.let { spoken(it) } ?: return null to 0L
            return s to durationOf(index, s)
        }

        val (hook, hookMs) = said(script.hook)
        out += RealEvent(RealPhase.Hook, -1, t, max(HOOK_MIN, hookMs + 400), hook, if (hook != null) index++ else -1)
        t = out.last().endMs

        script.roasts.forEachIndexed { i, r ->
            val (l, ms) = said(r.line)
            out += RealEvent(RealPhase.Roast, i, t, max(ROAST_MIN, ROAST_LEAD + ms + 350), l, if (l != null) index++ else -1, lineAtMs = t + ROAST_LEAD)
            t = out.last().endMs
        }

        val shown = script.shownReveals
        if (shown.isNotEmpty()) {
            val (b, bMs) = said(script.bridge)
            out += RealEvent(RealPhase.Bridge, -1, t, max(BRIDGE_MIN, bMs + 500), b, if (b != null) index++ else -1)
            t = out.last().endMs
            shown.forEachIndexed { i, r ->
                val (l, ms) = said(r.line)
                val photoAt = t + DOODLE_HOLD + WIPE
                val photoMs = max(PHOTO_MIN, ms + 400)
                val clipMs = if (r.clip != null) min(r.clipMs, CLIP_MAX).coerceAtLeast(0L) else 0L
                out += RealEvent(
                    RealPhase.Reveal, i, t, DOODLE_HOLD + WIPE + photoMs + clipMs, l, if (l != null) index++ else -1,
                    lineAtMs = photoAt, photoAtMs = photoAt, clipAtMs = photoAt + photoMs, clipMs = clipMs
                )
                t = out.last().endMs
            }
        }

        val (o, oMs) = said(script.outro)
        out += RealEvent(RealPhase.Outro, -1, t, max(OUTRO_HOLD + 1_000, oMs + OUTRO_HOLD), o, if (o != null) index++ else -1)
        return out
    }

    fun at(events: List<RealEvent>, t: Long): RealEvent = events.firstOrNull { t < it.endMs } ?: events.last()

    fun totalMs(events: List<RealEvent>): Long = events.lastOrNull()?.endMs ?: 0L
}

object RealCopy {
    private val TR = Locale("tr")

    fun hookText(word: String, seconds: Int): String = "$seconds saniyede ${word.lowercase(TR)} çiz dedik. İşte Türkiye'nin çizimleri!"
    const val BRIDGE = "Peki bunlar gerçek olsaydı?"
    const val OUTRO = "Sen daha iyisini çizebilir misin? karalak.com'da dene!"

    /**
     * What to paste into ChatGPT (or Gemini) with the doodle. The drawing is the blueprint, in the
     * spirit of Tom Curtis's "children's drawings made real": every line keeps its shape and every
     * mistake stays; only the surfaces become real. A model left to itself "fixes" the drawing into
     * an ordinary animal, so the prompt says so plainly, more than once.
     */
    fun imagePrompt(word: String, wordEn: String, seconds: Int, describe: String): String = buildString {
        val thing = wordEn.ifBlank { word }
        append("The attached image is a quick doodle of \"").append(word).append("\"")
        if (wordEn.isNotBlank()) append(" (").append(wordEn).append(")")
        append(" that a player drew in ").append(seconds).append(" seconds in a drawing game.\n")
        append("Make a photo of THIS EXACT DOODLE as if it had come to life in the real world, like Tom Curtis's \"children's drawings made real\" photos.\n\n")
        append("The doodle is the blueprint, not a hint:\n")
        append("- Keep its exact silhouette, pose, proportions and layout. Trace it.\n")
        append("- Every drawn line becomes a real body part of the same shape: a single-stroke leg becomes a thin straight tube-like leg, a circle stays a round blob, a triangle stays a sharp triangle, a scribble or spiral stays a scribble or spiral, a rectangle body stays boxy.\n")
        append("- Keep every mistake: the same number of legs, missing or extra parts, a tiny or huge head, parts in odd places. Do NOT fix the anatomy, do NOT add anything that is not drawn, do NOT turn it into a normal ").append(thing).append(".\n")
        append("- Only the surfaces become real: real fur, skin, feathers, scales or material that suit a ").append(thing).append(", with real light and shadows, so it looks like a real living thing that happens to have the drawing's body.\n")
        if (describe.isNotBlank()) append("- The doodle shows: ").append(describe.trim().trimEnd('.')).append(".\n")
        append("\nStrange, funny or slightly creepy is exactly right. Anyone should recognise the drawing in it at a glance.\n")
        append("Place it in a fitting real-world spot (a field, a street, the sea, a room), shot like a casual phone photo. Vertical 9:16. No paper, no pencil lines, no text, no frame.")
    }

    /**
     * What to paste into Gemini with the picture made from the doodle: a short clip of that odd
     * creature moving with its own natural sound, never "corrected" back into a normal one.
     */
    fun videoPrompt(wordEn: String): String {
        val thing = wordEn.ifBlank { "subject" }
        return "Animate this photo into a realistic, vertical 8-second video, like a casual phone video. " +
            "The creature keeps EXACTLY the strange body it has in the photo: the same outline, the same number of legs, the same odd proportions and stick-like parts. " +
            "Do not turn it into a normal $thing and do not fix its anatomy. " +
            "It moves the way a real $thing would, but with its own weird body: a few awkward steps or wiggles, a blink, a look at the camera. " +
            "The camera stays almost still. Natural ambient sound only: no music, no speech, no text on screen."
    }

    /**
     * The assistant's choice:
     * `{"word_en":"…","roasts":[{"index":2,"emotion":"…","line":"…"}],"reveals":[{"index":2,"emotion":"…","line":"…","describe":"…"}],"outro":{…},"caption":"…","hashtags":"…"}`.
     * Indices are 1-based; reveals that are not also in the roasts are dropped.
     */
    data class Picks(
        val wordEn: String,
        val roasts: List<Pair<Int, SpokenLine>>,
        val reveals: List<Triple<Int, SpokenLine, String>>,
        val outro: SpokenLine?,
        val caption: String,
        val hashtags: String
    )

    fun parsePicks(text: String, candidates: Int): Picks? {
        val o = GuessCopy.objectIn(text) ?: return null
        fun lineOf(p: JsonObject): SpokenLine? = p["line"]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
            ?.let { SpokenLine(it, MascotEmotion.of(p["emotion"]?.jsonPrimitive?.contentOrNull)) }
        fun indexOf(p: JsonObject): Int? = p["index"]?.jsonPrimitive?.intOrNull?.takeIf { it in 1..candidates }?.minus(1)

        val roasts = runCatching {
            o["roasts"]!!.jsonArray.mapNotNull { e ->
                val p = e.jsonObject
                val i = indexOf(p) ?: return@mapNotNull null
                i to (lineOf(p) ?: return@mapNotNull null)
            }.distinctBy { it.first }
        }.getOrNull().orEmpty()
        if (roasts.isEmpty()) return null
        val roastIdx = roasts.map { it.first }.toSet()
        val reveals = runCatching {
            o["reveals"]!!.jsonArray.mapNotNull { e ->
                val p = e.jsonObject
                val i = indexOf(p)?.takeIf { it in roastIdx } ?: return@mapNotNull null
                Triple(i, lineOf(p) ?: SpokenLine("", MascotEmotion.Shocked), p["describe"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty())
            }.distinctBy { it.first }
        }.getOrNull().orEmpty()
        val outro = (o["outro"] as? JsonObject)?.let { lineOf(it) }
        return Picks(
            o["word_en"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty(),
            roasts, reveals, outro,
            o["caption"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty(),
            o["hashtags"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        )
    }

    /** Karalak's line on seeing the real thing: `{"emotion":"…","line":"…"}`. */
    fun parseReaction(text: String): SpokenLine? {
        val o = GuessCopy.objectIn(text) ?: return null
        val line = o["line"]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return SpokenLine(line, MascotEmotion.of(o["emotion"]?.jsonPrimitive?.contentOrNull))
    }
}
