package com.sualtikasifi.cizimhafiza.presentation.reports.studio.guess

import com.sualtikasifi.cizimhafiza.presentation.reports.studio.LineRules
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MascotEmotion
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/** One thing Karalak says, and the face it says it with. */
@Serializable
data class SpokenLine(val text: String, val emotion: MascotEmotion)

/**
 * One drawing in "Karalak Bilir mi?": what Karalak said while it was being drawn (one line per
 * look, see [GuessTimeline.STAGE_FRACTIONS]), its final one-word answer, whether that was right,
 * and how it took the result.
 */
data class GuessRound(
    val drawingId: Long,
    val word: String,
    val stages: List<SpokenLine>,
    val finalGuess: String,
    val correct: Boolean,
    val reaction: SpokenLine,
    /** What the person who drew it typed when they tried to remember it themselves; shown as a bubble on their tile. */
    val playerAnswer: String? = null
)

data class GuessScript(val intro: SpokenLine, val rounds: List<GuessRound>, val outro: SpokenLine) {
    val mascotScore: Int get() = rounds.count { it.correct }
    val playerScore: Int get() = rounds.size - mascotScore

    /** Every line in speaking order — the order the voice is recorded and the timeline is built in. */
    fun lines(): List<SpokenLine> = buildList {
        add(intro)
        rounds.forEach { r -> addAll(r.stages.take(3)); add(r.reaction) }
        add(outro)
    }
}

enum class GuessPhase { Intro, Draw, Speak, Verdict, Reaction, Outro }

/** One stretch of the video. [line] is what is being said, if anything; the drawing shows from [drawFrom] to [drawTo]. */
data class GuessEvent(
    val phase: GuessPhase,
    val round: Int,
    val stage: Int,
    val startMs: Long,
    val durationMs: Long,
    val line: SpokenLine? = null,
    /** Index into GuessScript.lines() of [line], so its recorded voice can be found. */
    val lineIndex: Int = -1,
    val drawFrom: Float = 0f,
    val drawTo: Float = 0f
) {
    val endMs: Long get() = startMs + durationMs
    fun progress(t: Long): Float = ((t - startMs).toFloat() / durationMs).coerceIn(0f, 1f)
}

/**
 * The running order: Karalak's opening line; per drawing, a stretch of drawing then a line about
 * what it sees so far (three times), the verdict and its reaction; then the closing card.
 *
 * Built from how long each line actually takes to say ([durationOf]), so the voice is never cut
 * off and never followed by dead air.
 */
object GuessTimeline {
    /** How much of the drawing Karalak has seen when it speaks each line. */
    val STAGE_FRACTIONS = listOf(0.34f, 0.67f, 1f)
    private val DRAW_MS = listOf(1000L, 850L, 850L)
    private const val VERDICT_MS = 900L
    private const val TAIL_MS = 250L
    private const val OUTRO_HOLD_MS = 1400L

    fun build(script: GuessScript, durationOf: (index: Int, line: SpokenLine) -> Long): List<GuessEvent> {
        val out = mutableListOf<GuessEvent>()
        var t = 0L
        var lineIndex = 0
        fun speak(phase: GuessPhase, round: Int, stage: Int, line: SpokenLine, hold: Float, extra: Long) {
            val d = max(600L, durationOf(lineIndex, line)) + extra
            out += GuessEvent(phase, round, stage, t, d, line, lineIndex, hold, hold)
            t += d
            lineIndex++
        }
        speak(GuessPhase.Intro, -1, -1, script.intro, 0f, 400L)
        script.rounds.forEachIndexed { r, round ->
            var shown = 0f
            STAGE_FRACTIONS.forEachIndexed { k, f ->
                out += GuessEvent(GuessPhase.Draw, r, k, t, DRAW_MS[k], drawFrom = shown, drawTo = f)
                t += DRAW_MS[k]
                shown = f
                val line = round.stages.getOrNull(k)
                if (line != null) speak(GuessPhase.Speak, r, k, line, f, TAIL_MS)
            }
            out += GuessEvent(GuessPhase.Verdict, r, -1, t, VERDICT_MS, drawFrom = 1f, drawTo = 1f)
            t += VERDICT_MS
            speak(GuessPhase.Reaction, r, -1, round.reaction, 1f, 350L)
        }
        speak(GuessPhase.Outro, -1, -1, script.outro, 0f, OUTRO_HOLD_MS)
        return out
    }

    fun at(events: List<GuessEvent>, t: Long): GuessEvent = events.firstOrNull { t < it.endMs } ?: events.last()

    fun totalMs(events: List<GuessEvent>): Long = events.lastOrNull()?.endMs ?: 0L

    /** Karalak's points and the player's at [t]: a round counts once its verdict has been shown. */
    fun scoreAt(script: GuessScript, events: List<GuessEvent>, t: Long): Pair<Int, Int> {
        var mascot = 0
        var player = 0
        events.filter { it.phase == GuessPhase.Verdict && t >= it.startMs }.forEach { e ->
            if (script.rounds[e.round].correct) mascot++ else player++
        }
        return mascot to player
    }

    /** A guess at how long a line takes to say, for the preview before any voice is recorded. */
    fun estimateMs(text: String): Long = 350L + text.length * 62L
}

/** The words around the rounds, plus what goes under the post. */
data class FrameCopy(
    val intro: SpokenLine, val reactions: List<SpokenLine>, val outro: SpokenLine, val caption: String, val hashtags: String,
    /** Other openings and closings to pick from (the same mood as the first), best first; may be empty. */
    val introAlts: List<SpokenLine> = emptyList(),
    val outroAlts: List<SpokenLine> = emptyList()
)

/** Reading the assistant's answers, and judging Karalak's guesses. */
object GuessCopy {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val TR = Locale("tr")

    /** The JSON object inside an answer, whatever the model wrapped around it (code fences, a sentence first). */
    fun objectIn(text: String): JsonObject? {
        val from = text.indexOf('{')
        val to = text.lastIndexOf('}')
        if (from < 0 || to <= from) return null
        return runCatching { json.parseToJsonElement(text.substring(from, to + 1)).jsonObject }.getOrNull()
    }

    private fun lineOf(o: JsonObject?): SpokenLine? {
        o ?: return null
        val text = (o["line"] ?: o["text"])?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return SpokenLine(text, MascotEmotion.of(o["emotion"]?.jsonPrimitive?.contentOrNull))
    }

    /** Karalak's three looks at one drawing: `{"stages":[{"emotion","line"}×3],"final_guess":"…"}`. */
    fun parseLooks(text: String): Pair<List<SpokenLine>, String>? {
        val o = objectIn(text) ?: return null
        val stages = runCatching { o["stages"]!!.jsonArray.mapNotNull { lineOf(it.jsonObject) } }.getOrNull().orEmpty()
        val guess = o["final_guess"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        if (stages.isEmpty() || guess.isEmpty()) return null
        return stages.take(3) to guess
    }

    /** The lines around the rounds, and the post text: `{"intro":{…},"reactions":[{…}…],"outro":{…},"caption":"…","hashtags":"…"}`. */
    fun parseFrame(text: String, rounds: Int): FrameCopy? {
        val o = objectIn(text) ?: return null
        val intro = lineOf(o["intro"] as? JsonObject) ?: return null
        val outro = lineOf(o["outro"] as? JsonObject) ?: return null
        val reactions = runCatching { o["reactions"]!!.jsonArray.mapNotNull { lineOf(it.jsonObject) } }.getOrNull().orEmpty()
        if (reactions.size < rounds) return null
        return FrameCopy(
            intro, reactions.take(rounds), outro,
            o["caption"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty(),
            o["hashtags"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty(),
            alternatives(o["intro_alts"], intro), alternatives(o["outro_alts"], outro)
        )
    }

    /** Other ways to say a line: `["…","…"]` (or objects like the line's own), in [like]'s mood, without repeats of it or of each other. */
    private fun alternatives(e: kotlinx.serialization.json.JsonElement?, like: SpokenLine): List<SpokenLine> {
        val array = runCatching { e?.jsonArray }.getOrNull() ?: return emptyList()
        val seen = mutableSetOf(normalize(like.text))
        return array.mapNotNull { item ->
            (item as? JsonObject)?.let { lineOf(it) }
                ?: runCatching { item.jsonPrimitive.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }?.let { SpokenLine(it, like.emotion) } }.getOrNull()
        }.filter { seen.add(normalize(it.text)) }.take(MAX_ALTS)
    }

    /** How many other openings, and other closings, the panel offers. */
    const val MAX_ALTS = 4

    private fun SpokenLine.fitted(maxWords: Int) = copy(text = LineRules.fit(text, maxWords))

    /** Karalak's looks with each line cut to its length (see [LineRules]). */
    fun fitLooks(stages: List<SpokenLine>): List<SpokenLine> = stages.map { it.fitted(LineRules.STAGE_WORDS) }

    /** The frame's lines cut to their lengths; what came back short is left as it is. */
    fun fitFrame(f: FrameCopy): FrameCopy = f.copy(
        intro = f.intro.fitted(LineRules.INTRO_WORDS),
        reactions = f.reactions.map { it.fitted(LineRules.REACTION_WORDS) },
        outro = f.outro.fitted(LineRules.OUTRO_WORDS),
        introAlts = f.introAlts.map { it.fitted(LineRules.INTRO_WORDS) },
        outroAlts = f.outroAlts.map { it.fitted(LineRules.OUTRO_WORDS) }
    )

    /** The lines of Karalak's looks that are too long to keep, as they are written. */
    fun longLooks(stages: List<SpokenLine>): List<String> = stages.map { it.text }.filter { LineRules.tooLong(it, LineRules.STAGE_WORDS) }

    /** The frame's lines that are too long to keep (the alternatives are only dropped when too long, never asked about). */
    fun longFrame(f: FrameCopy): List<String> = buildList {
        if (LineRules.tooLong(f.intro.text, LineRules.INTRO_WORDS)) add(f.intro.text)
        f.reactions.forEach { if (LineRules.tooLong(it.text, LineRules.REACTION_WORDS)) add(it.text) }
        if (LineRules.tooLong(f.outro.text, LineRules.OUTRO_WORDS)) add(f.outro.text)
    }

    /** Lower-case, Turkish letters folded to plain ones, punctuation gone: "Kara Kedi!" → "kara kedi". */
    fun normalize(s: String): String = s.lowercase(TR)
        .replace('ç', 'c').replace('ğ', 'g').replace('ı', 'i').replace('ö', 'o').replace('ş', 's').replace('ü', 'u').replace('â', 'a')
        .replace(Regex("[^a-z0-9 ]"), " ").replace(Regex("\\s+"), " ").trim()

    /** Whether Karalak's answer counts: the same word, a phrase containing it, or one slip of the keyboard away. */
    fun matches(guess: String, word: String): Boolean {
        val g = normalize(guess)
        val w = normalize(word)
        if (g.isEmpty() || w.isEmpty()) return false
        if (g == w) return true
        if (g.split(' ').contains(w) || w.split(' ').contains(g)) return true
        val allowed = when {
            w.length >= 8 -> 2
            w.length >= 4 -> 1
            else -> 0
        }
        return allowed > 0 && distance(g, w) <= allowed
    }

    private fun distance(a: String, b: String): Int {
        var prev = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            val cur = IntArray(b.length + 1)
            cur[0] = i
            for (j in 1..b.length) {
                cur[j] = min(min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1)
            }
            prev = cur
        }
        return prev[b.length]
    }
}
