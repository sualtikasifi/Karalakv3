package com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot

import com.sualtikasifi.cizimhafiza.domain.model.ChestTier
import com.sualtikasifi.cizimhafiza.domain.model.CouponCodes
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioCopy
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.SpokenLine
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MascotEmotion
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.max

/** How the video hands out its kupon kodu. */
enum class ForgotMode(val title: String) {
    /** The code is the drawing's right answer, never said in the video: whoever knows the word (or reads the comments) has it. */
    Word("Bilen alır"),

    /** A made-up code is written out on the end card. */
    Shown("Kod ekranda")
}

/**
 * "Çizen Bile Bilemedi": one drawing whose own drawer could not recognise it later. Karalak guesses while it is drawn,
 * the viewer gets a countdown to guess too, then the drawer's wrong answer is shown, and the kupon kodu follows.
 *
 * [code] is what the viewer types in the app (see [CouponCodes]): in [ForgotMode.Word] the answer itself, so it is
 * never put on screen as letters, only as empty boxes; in [ForgotMode.Shown] a code written out in full.
 */
data class ForgotScript(
    val drawingId: Long,
    val word: String,
    val drawerAnswer: String,
    val hook: SpokenLine,
    /** What Karalak thinks while the drawing builds up, one line per look (at most [ForgotTimeline.MAX_GUESSES]). */
    val guesses: List<SpokenLine>,
    /** Its reaction to what the drawer wrote. */
    val reaction: SpokenLine,
    val codeLine: SpokenLine,
    val outro: SpokenLine,
    val mode: ForgotMode = ForgotMode.Word,
    /** What the code gives, as the viewer reads it ("Sanatçı Kasası"). */
    val reward: String = "",
    val code: String = "",
    /** Hand-entered share of players who could not recognise their own drawing of this word; shown when [showPercent]. */
    val percent: Int = 70,
    val showPercent: Boolean = false
) {
    /** Every spoken line in order: the voice is recorded and laid out in this order. */
    fun lines(): List<SpokenLine> = buildList {
        add(hook)
        addAll(guesses.take(ForgotTimeline.MAX_GUESSES))
        add(reaction)
        add(codeLine)
        add(outro)
    }
}

/** What a video's kupon kodu gives: the presets the panel offers, each turned into a [com.sualtikasifi.cizimhafiza.domain.model.Coupon] (chests open on the spot). */
enum class ForgotReward(val title: String, val gold: Int = 0, val xp: Int = 0, val chests: Map<ChestTier, Int> = emptyMap()) {
    Silver("Çırak", chests = mapOf(ChestTier.SILVER to 1)),
    Gold("Sanatçı", chests = mapOf(ChestTier.GOLD to 1)),
    Rare("Sürpriz", chests = mapOf(ChestTier.RARE to 1)),
    Coins("500 altın", gold = 500),
    Xp("300 XP", xp = 300)
}

enum class ForgotPhase { Hook, Draw, Guess, Countdown, Answer, Code, Outro }

data class ForgotEvent(
    val phase: ForgotPhase,
    /** Which of Karalak's looks this is (Draw and Guess), else -1. */
    val look: Int,
    val startMs: Long,
    val durationMs: Long,
    val line: SpokenLine? = null,
    val lineIndex: Int = -1,
    /** How much of the drawing is on the paper from [drawFrom] to [drawTo]. */
    val drawFrom: Float = 1f,
    val drawTo: Float = 1f
) {
    val endMs: Long get() = startMs + durationMs
    fun progress(t: Long): Float = ((t - startMs).toFloat() / durationMs).coerceIn(0f, 1f)
}

object ForgotTimeline {
    const val MAX_GUESSES = 3
    const val COUNTDOWN_MS = 2_400L
    private const val HOOK_MIN = 3_000L
    private const val DRAW_MS = 1_000L
    private const val GUESS_MIN = 1_400L
    private const val ANSWER_MIN = 3_200L
    private const val CODE_MIN = 3_600L
    private const val OUTRO_HOLD = 1_800L

    fun build(script: ForgotScript, durationOf: (index: Int, line: SpokenLine) -> Long): List<ForgotEvent> {
        val out = mutableListOf<ForgotEvent>()
        var t = 0L
        var index = 0
        fun speak(phase: ForgotPhase, look: Int, line: SpokenLine, min: Long, extra: Long, from: Float, to: Float) {
            val d = max(min, durationOf(index, line) + extra)
            out += ForgotEvent(phase, look, t, d, line, index, from, to)
            index++
            t += d
        }
        // the finished drawing stands on the paper while the hook is said, then it is drawn again, Karalak guessing
        speak(ForgotPhase.Hook, -1, script.hook, HOOK_MIN, 400, 1f, 1f)
        val guesses = script.guesses.take(MAX_GUESSES)
        var shown = 0f
        guesses.forEachIndexed { k, line ->
            val upTo = (k + 1f) / guesses.size
            out += ForgotEvent(ForgotPhase.Draw, k, t, DRAW_MS, drawFrom = shown, drawTo = upTo)
            t += DRAW_MS
            shown = upTo
            speak(ForgotPhase.Guess, k, line, GUESS_MIN, 300, upTo, upTo)
        }
        out += ForgotEvent(ForgotPhase.Countdown, -1, t, COUNTDOWN_MS)
        t += COUNTDOWN_MS
        speak(ForgotPhase.Answer, -1, script.reaction, ANSWER_MIN, 500, 1f, 1f)
        speak(ForgotPhase.Code, -1, script.codeLine, CODE_MIN, 500, 1f, 1f)
        speak(ForgotPhase.Outro, -1, script.outro, 0, OUTRO_HOLD, 1f, 1f)
        return out
    }

    fun at(events: List<ForgotEvent>, t: Long): ForgotEvent = events.firstOrNull { t < it.endMs } ?: events.last()

    fun totalMs(events: List<ForgotEvent>): Long = events.lastOrNull()?.endMs ?: 0L
}

object ForgotCopy {
    const val HOOK = "Bunu çizen kişi bile ne olduğunu bilemedi. Sen bilebilir misin?"
    const val OUTRO = "Yarın daha zoru geliyor. Sen de karalak.com'da çiz!"
    const val CODE_WORD = "Doğru cevap senin kodun. Bilen kasayı alır!"
    const val CODE_SHOWN = "İşte kodun. Uygulamada Ayarlar'dan gir!"

    fun reactionText(drawerAnswer: String) = "Çizen “${drawerAnswer.trim()}” yazmış. Ben de öyle sanmıştım!"

    fun codeLineFor(mode: ForgotMode) = if (mode == ForgotMode.Word) CODE_WORD else CODE_SHOWN

    /** The code a [word] is worth in [ForgotMode.Word]: the word itself, Turkish letters folded; null when it cannot be one (too short or long). */
    fun wordCode(word: String): String? = CouponCodes.normalize(word)

    /** "100 altın + Sanatçı Kasası" and the like, for what the code gives. */
    fun rewardLabel(gold: Int, xp: Int, chests: Map<ChestTier, Int>): String {
        val parts = mutableListOf<String>()
        chests.entries.sortedByDescending { it.key.ordinal }.forEach { (tier, n) ->
            if (n > 0) parts += (if (n > 1) "$n × " else "") + tierName(tier)
        }
        if (xp > 0) parts += "$xp XP"
        if (gold > 0) parts += "$gold altın"
        return parts.joinToString(" + ")
    }

    private fun tierName(tier: ChestTier) = when (tier) {
        ChestTier.SILVER -> "Çırak Kasası"
        ChestTier.GOLD -> "Sanatçı Kasası"
        ChestTier.RARE -> "Sürpriz Kasası"
    }

    /** Whether [text] says the drawing's [word]: a line or caption that does would give the code away. */
    fun leaks(text: String, word: String): Boolean {
        val w = StudioCopy.normalize(word)
        if (w.isEmpty()) return false
        val t = StudioCopy.normalize(text)
        return " $t ".contains(" $w ") || t.split(' ').any { it.length >= 4 && w.length >= 4 && (it == w || it.startsWith(w) || w.startsWith(it)) }
    }

    /** The assistant's lines: `{"guesses":[{"emotion","line"}…],"reaction":{…},"hook":{…},"outro":{…},"caption":"…","hashtags":"…"}`. */
    data class Written(
        val guesses: List<SpokenLine>, val reaction: SpokenLine?, val hook: SpokenLine?, val outro: SpokenLine?,
        val caption: String, val hashtags: String
    )

    fun parse(text: String, word: String): Written? {
        val o = StudioCopy.objectIn(text) ?: return null
        fun line(e: kotlinx.serialization.json.JsonElement?): SpokenLine? {
            val oo = e as? JsonObject ?: return null
            val l = (oo["line"] ?: oo["text"])?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            // a line that says the word would give the code away
            if (leaks(l, word)) return null
            return SpokenLine(l, MascotEmotion.of(oo["emotion"]?.jsonPrimitive?.contentOrNull))
        }
        val guesses = runCatching { o["guesses"]!!.jsonArray.mapNotNull { line(it) } }.getOrNull().orEmpty()
        if (guesses.isEmpty()) return null
        fun clean(key: String) = o[key]?.jsonPrimitive?.contentOrNull?.trim().orEmpty().takeIf { !leaks(it, word) }.orEmpty()
        return Written(guesses.take(ForgotTimeline.MAX_GUESSES), line(o["reaction"]), line(o["hook"]), line(o["outro"]), clean("caption"), clean("hashtags"))
    }

    /** A first percentage when the person has none to enter: any value from 55 to 90, in steps of 1. */
    fun randomPercent(random: kotlin.random.Random = kotlin.random.Random): Int = 55 + random.nextInt(36)

    /** For the preview before the assistant has written: a stand-in set of guesses. */
    fun placeholderGuesses() = listOf(
        SpokenLine("Hmm, bu ne olabilir?", MascotEmotion.Thinking),
        SpokenLine("Bir şeye benziyor…", MascotEmotion.Suspicious),
        SpokenLine("Buldum, eminim!", MascotEmotion.Confident)
    )
}
