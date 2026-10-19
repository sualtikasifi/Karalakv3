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

/**
 * One anonymous drawing of the pool, copied into the project so it survives the pool changing. Each carries its own
 * [word] and the [seconds] the drawer had, because one video mixes different words; projects saved before that have
 * neither, and fall back to the script's single word.
 */
@Serializable
data class RealDrawing(val id: String, val strokes: List<List<DrawingPoint>>, val word: String = "", val seconds: Int = 0)

/** One of the pool's latest drawings as the word list shows it. */
@Serializable
data class RecentDrawing(val id: String, val word: String, val seconds: Int, val strokes: List<List<DrawingPoint>>)

/** A drawing in the opening run and Karalak's joke about it. */
@Serializable
data class RealRoast(val sampleId: String, val line: SpokenLine)

/**
 * A drawing turned real: its realistic picture (made in ChatGPT, kept for sharing as a swipe-through
 * post and as the starting frame for Gemini), the short clip of that picture moving (made in Gemini,
 * the only thing of it that goes into the video), and what Karalak says on seeing it. [image] and
 * [clip] are file names in the project's media folder.
 */
@Serializable
data class RealReveal(
    val sampleId: String,
    val line: SpokenLine,
    /** The doodle's literal shapes in English, written into the picture prompt so they are kept. */
    val describe: String = "",
    /** The drawing's word in English, for its prompts. */
    val wordEn: String = "",
    /** How the thing, exactly as drawn, would naturally move (English), written into the video prompt. */
    val motion: String = "",
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
    /** Only reveals with their clip make it into the video; the pictures are shared on their own. */
    val shownReveals: List<RealReveal> get() = reveals.filter { it.sampleId in roastIds && it.clip != null }
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
    /** Only projects from before a video could mix words carry these two; the words now live on each drawing and reveal. */
    val wordKey: String = "",
    val wordEn: String = "",
    val script: RealScript,
    val drawings: List<RealDrawing>
) {
    fun drawing(id: String): RealDrawing? = drawings.find { it.id == id }
    fun wordOf(id: String): String = drawing(id)?.word?.takeIf { it.isNotBlank() } ?: script.word
    fun secondsOf(id: String): Int = drawing(id)?.seconds?.takeIf { it > 0 } ?: script.drawSeconds
    val media: Set<String> get() = script.reveals.flatMap { listOfNotNull(it.image, it.clip) }.toSet()
}

enum class RealPhase { Hook, Roast, Bridge, Reveal, Outro }

/**
 * One stretch of the video. A reveal runs in steps: the doodle held, then wiped away by the clip, which
 * starts playing as the wipe begins ([clipAtMs] for [clipMs]) and is fully on screen at [photoAtMs],
 * when Karalak's line starts.
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
                val clipMs = min(r.clipMs, CLIP_MAX).coerceAtLeast(0L)
                val wipeEnd = t + DOODLE_HOLD + WIPE
                // the clip sets the length; a line longer than the clip lets its last frame stay
                val onScreen = max(clipMs, WIPE + ms + 400L)
                out += RealEvent(
                    RealPhase.Reveal, i, t, DOODLE_HOLD + onScreen, l, if (l != null) index++ else -1,
                    lineAtMs = wipeEnd, photoAtMs = wipeEnd, clipAtMs = t + DOODLE_HOLD, clipMs = clipMs
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
    /** The opening when the drawings are of different words. */
    const val HOOK_MANY = "Oyunculara birkaç saniyede çizdirdik. İşte Türkiye'nin çizimleri!"
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
     * What goes to Gemini together with the picture made from the doodle. Gemini makes a video from the
     * attached picture only if told to start from it, and invents a "better" creature if the prompt
     * describes one, so this names no body parts and forbids every change: the picture is the first
     * frame and only motion is added. [motion] is the assistant's one-line idea of how this exact
     * shape would move; without it a generic, parts-it-already-has motion is asked for.
     */
    fun videoPrompt(wordEn: String, motion: String): String {
        val thing = wordEn.ifBlank { "subject" }
        val move = motion.trim().trimEnd('.').ifBlank {
            "only the parts that can naturally move do so, a little: flames flicker, hair or cloth sways, dust drifts, the whole thing wobbles or shifts its weight, as if this exact $thing were alive and clumsily trying to move"
        }
        return "Use the attached photo as the very first frame and animate it into a vertical 9:16, 8-second video that looks like a casual phone video.\n\n" +
            "The photo is a real version of a child's quick doodle of a $thing, and its clumsy, odd shape is the whole point. Keep it EXACTLY as it is in the photo: the same silhouette, proportions, colours, materials, background and light. " +
            "Do NOT redesign, smooth out, fix or \"improve\" it. Do NOT make it more professional or more realistic. " +
            "Do NOT add eyes, faces, arms, legs, wings, windows, decorations or any part that is not already in the photo, and do not remove any. " +
            "It must still look like a child's drawing made real.\n\n" +
            "Motion: " + move + ". Keep the motion simple, physical and slightly awkward, the way a real object of exactly this shape would move, using only the parts it has.\n\n" +
            "Camera: fixed, or a very slow push-in; no cuts. Natural ambient sound that matches what happens; no music, no speech, no text on screen."
    }

    /**
     * The assistant's choice:
     * `{"roasts":[{"index":2,"emotion":"…","line":"…"}],"reveals":[{"index":2,"emotion":"…","line":"…","describe":"…","motion":"…","word_en":"…"}],"outro":{…},"caption":"…","hashtags":"…"}`.
     * Indices are 1-based; reveals that are not also in the roasts are dropped.
     */
    data class RevealPick(val index: Int, val line: SpokenLine, val describe: String, val motion: String, val wordEn: String)

    data class Picks(
        val roasts: List<Pair<Int, SpokenLine>>,
        val reveals: List<RevealPick>,
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
                RevealPick(
                    i, lineOf(p) ?: SpokenLine("", MascotEmotion.Shocked),
                    p["describe"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty(),
                    p["motion"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty(),
                    p["word_en"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                )
            }.distinctBy { it.index }
        }.getOrNull().orEmpty()
        val outro = (o["outro"] as? JsonObject)?.let { lineOf(it) }
        return Picks(
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
