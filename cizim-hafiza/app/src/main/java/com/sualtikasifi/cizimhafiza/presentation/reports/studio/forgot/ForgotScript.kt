package com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot

import com.sualtikasifi.cizimhafiza.domain.model.ChestTier
import com.sualtikasifi.cizimhafiza.domain.model.CouponCodes
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioCopy
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.SpokenLine
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MascotEmotion
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealTimeline
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
    val hook: SpokenLine,
    /** What Karalak thinks while the drawing builds up, one line per look (at most [ForgotTimeline.MAX_GUESSES]). */
    val guesses: List<SpokenLine>,
    /**
     * How much of the drawing is on the paper at each guess (0 to 1), set by hand, one per guess; what is missing is
     * spread evenly (see [ForgotTimeline.stopsFor]). The assistant writes each guess from the drawing as far as this.
     */
    val guessAt: List<Float> = emptyList(),
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
    /** Where each guess stops the drawing, ready to use: one per guess, in order, none past the whole. */
    val stops: List<Float> get() = ForgotTimeline.stopsFor(guesses.size, guessAt)

    /** Every spoken line in order: the voice is recorded and laid out in this order. */
    fun lines(): List<SpokenLine> = buildList {
        add(hook)
        addAll(guesses.take(ForgotTimeline.MAX_GUESSES))
        add(codeLine)
        add(outro)
    }
}

/** Which chest the code gives besides the gold and XP set by hand (none, or one of a tier; chests open on the spot). */
enum class ForgotChest(val title: String, val tier: ChestTier?) {
    None("Yok", null),
    Silver("Çırak", ChestTier.SILVER),
    Gold("Sanatçı", ChestTier.GOLD),
    Rare("Sürpriz", ChestTier.RARE);

    /** The chests as a coupon holds them. */
    val chests: Map<ChestTier, Int> get() = tier?.let { mapOf(it to 1) } ?: emptyMap()
}

enum class ForgotPhase { Hook, Draw, Guess, Countdown, Code, Outro }

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
    /** The earliest a guess may come: a few strokes in. */
    const val MIN_STOP = 0.05f
    /** 3 · 2 · 1, half a second each: still readable, and tenser. */
    const val COUNTDOWN_MS = 1_500L
    private const val HOOK_MIN = 2_000L
    private const val DRAW_MS = 600L
    private const val GUESS_MIN = 1_100L
    private const val CODE_MIN = 2_600L
    /** The drawing is already begun on the first frame (a few strokes), so the video's cover is never an empty board. */
    private const val HOOK_FROM = 0.08f
    private const val OUTRO_HOLD = 1_800L

    /**
     * [howtoMs] > 0: the closing is the end card with the phone, and it stays until the how-to video has played in it
     * (see [RealTimeline.END_VIDEO_LEAD]) and a moment more, however short Karalak's line is.
     */
    fun build(script: ForgotScript, howtoMs: Long = 0L, durationOf: (index: Int, line: SpokenLine) -> Long): List<ForgotEvent> {
        val out = mutableListOf<ForgotEvent>()
        var t = 0L
        var index = 0
        fun speak(phase: ForgotPhase, look: Int, line: SpokenLine, min: Long, extra: Long, from: Float, to: Float) {
            val d = max(min, durationOf(index, line) + extra)
            out += ForgotEvent(phase, look, t, d, line, index, from, to)
            index++
            t += d
        }
        val guesses = script.guesses.take(MAX_GUESSES)
        val stops = stopsFor(guesses.size, script.guessAt)
        // the drawing is drawn while the hook is said, as far as the first guess stops it; no finished picture first
        val first = stops.firstOrNull() ?: 1f
        speak(ForgotPhase.Hook, -1, script.hook, HOOK_MIN, 250, minOf(HOOK_FROM, first), first)
        guesses.forEachIndexed { k, line ->
            val upTo = stops[k]
            if (k > 0) {
                out += ForgotEvent(ForgotPhase.Draw, k, t, DRAW_MS, drawFrom = stops[k - 1], drawTo = upTo)
                t += DRAW_MS
            }
            speak(ForgotPhase.Guess, k, line, GUESS_MIN, 200, upTo, upTo)
        }
        out += ForgotEvent(ForgotPhase.Countdown, -1, t, COUNTDOWN_MS)
        t += COUNTDOWN_MS
        // Karalak's reaction and the code are one line, said as the code comes up
        speak(ForgotPhase.Code, -1, script.codeLine, CODE_MIN, 400, 1f, 1f)
        val cardMin = if (howtoMs > 0) howtoMs.coerceAtMost(RealTimeline.HOWTO_MAX) + RealTimeline.END_VIDEO_LEAD + RealTimeline.END_VIDEO_TAIL else 0L
        speak(ForgotPhase.Outro, -1, script.outro, cardMin, OUTRO_HOLD, 1f, 1f)
        return out
    }

    /**
     * Where each of [count] guesses stops the drawing: the [set] shares the person gave, else spread evenly (a third, two
     * thirds, the whole); never before [MIN_STOP] or past the whole, and never earlier than the guess before it.
     */
    fun stopsFor(count: Int, set: List<Float>): List<Float> {
        var floor = MIN_STOP
        return List(count.coerceAtLeast(0)) { k ->
            val wanted = set.getOrNull(k) ?: ((k + 1f) / count)
            wanted.coerceIn(floor, 1f).also { floor = it }
        }
    }

    fun at(events: List<ForgotEvent>, t: Long): ForgotEvent = events.firstOrNull { t < it.endMs } ?: events.last()

    fun totalMs(events: List<ForgotEvent>): Long = events.lastOrNull()?.endMs ?: 0L
}

object ForgotCopy {
    const val HOOK = "Çizen bile bilemedi. Sen bilir misin?"
    const val OUTRO = "Yarın daha zoru geliyor. Sen de karalak.com'da çiz!"
    const val CODE_WORD = "Bilemedin mi? Doğru cevap senin kodun!"
    const val CODE_SHOWN = "Bilemedin mi? Kodu Ayarlar'dan gir!"

    /** Karalak makes two guesses unless told otherwise (three at most), and none runs longer than this, nor the hook or the code line. */
    const val DEFAULT_GUESSES = 2
    const val GUESS_WORDS = 5
    const val HOOK_WORDS = 8
    const val CODE_WORDS = 9

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

    /** The assistant's lines: `{"guesses":[{"emotion","line"}…],"hook":{…},"outro":{…},"caption":"…","hashtags":"…"}`. */
    data class Written(
        val guesses: List<SpokenLine>, val hook: SpokenLine?, val outro: SpokenLine?,
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
        return Written(guesses.take(ForgotTimeline.MAX_GUESSES), line(o["hook"]), line(o["outro"]), clean("caption"), clean("hashtags"))
    }

    /** When a new code stops working unless the person picks another time: two days from now. */
    fun defaultExpiry(now: Long = System.currentTimeMillis()): Long = now + 48L * 3_600_000L

    /** A share of the drawing as the panel and the assistant say it: 0.35 → 35. */
    fun percent(share: Float): Int = kotlin.math.round(share * 100f).toInt()

    /**
     * What the assistant is told: one picture per guess, the drawing as far as that guess stops it, then the whole drawing.
     * Each guess is written from its own picture only, so it fits what is on the paper at that moment.
     */
    fun guessPrompt(word: String, stops: List<Float>, moods: String): String {
        val n = stops.size
        val pictures = stops.mapIndexed { i, share -> "Görsel ${i + 1}: çizimin %${percent(share)}'i çizilmiş." }.joinToString("\n") + "\nGörsel ${n + 1}: bitmiş çizim."
        return """
            Sana bir oyuncunun çizimini adım adım gösteriyorum. Çizilmesi istenen kelime: "$word". Çizen kişi bir süre sonra kendi çizimini hatırlayamadı.

            @@PICTURES@@

            Video: Karalak çizimi canlı izliyor ve tahmin ediyor; izleyici de aynı anda tahmin edecek. Doğru cevap bir kupon kodu, o yüzden hiçbir cümlede "$word" kelimesi (ya da kökü) geçmemeli.

            Video 20 saniyenin altında olmalı: bütün cümleler kısa ve konuşma diliyle olsun.
            1) "guesses": tam $n tahmin, Görsel 1'den Görsel $n'e sırayla. Her tahmini YALNIZCA kendi görseline bakarak yaz: Karalak o anda yalnızca o kadarını görüyor, sonrasını bilmiyor. Her biri EN FAZLA ${GUESS_WORDS} kelime (ör. "Mantar mı bu?"), o görselde gerçekten görünen bir şeye bakan komik ve yanlış (ama makul) bir tahmin olsun; ilk tahminler daha belirsiz, sonuncusu en kendinden emini. Çizenle asla dalga geçme.
            2) "hook": videonun ilk cümlesi; "çizen kişi bile bilemedi" fikrini söylesin ve izleyiciyi tahmine çağırsın, EN FAZLA ${HOOK_WORDS} kelime.
            3) "outro": kapanış, yarın daha zorunun geleceğini söylesin, en fazla 14 kelime, "karalak.com" geçsin.
            4) "caption": TikTok/Reels açıklaması (1–2 cümle, izleyiciye soruyla bitsin, doğru kelime geçmesin) ve "hashtags": 5–6 Türkçe ağırlıklı etiket.

            Yalnızca şu JSON'u yaz:
            {"guesses":[{"emotion":"…","line":"…"}],"hook":{"emotion":"…","line":"…"},"outro":{"emotion":"…","line":"…"},"caption":"…","hashtags":"…"}
            emotion yalnızca şunlardan biri: $moods.
        """.trimIndent().replace("@@PICTURES@@", pictures)
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
