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
import java.util.Locale
import kotlin.math.max

/**
 * The wording of the on-screen texts (and the tone the assistant writes in): three sets that are used in turn from one
 * video to the next, so the feed does not read like the same ad every time.
 */
enum class ForgotStyle(
    val title: String,
    val titleWord: String,
    val titleShown: String,
    val explainWord: String,
    val explainShown: String,
    /** Where to type the code, as the three steps the code screen numbers: "Ayarlar › Kupon kodu › Kullan". */
    val steps: List<String>,
    val hintWord: String,
    /** The assistant's voice for this set. */
    val tone: String
) {
    // nothing here names what the code gives (it may be gold, XP or a chest): the prize is written only by the board's "Ödül:" frame
    Game(
        "Oyun dili", "GİZLİ KOD", "ÖDÜL KODU",
        "Kod, çizilen şeyin adı", "Kodu uygulamaya gir",
        listOf("Ayarlar", "Kupon kodu", "Kullan"), "Takıldıysan yorumlara bak",
        "oyun dili: kod, şifre, görev, meydan okuma gibi sözcükler; enerjik ve meydan okuyan. Ödülün ne olduğunu (kasa, altın, XP) sen söyleme, ekranda yazıyor"
    ),
    Warm(
        "Sıcak dil", "Cevabı bil, ödülü al", "Hediye kodun hazır",
        "Kod, az önce çizilen şeyin adı", "Kodu uygulamaya yaz",
        listOf("Ayarlar'a gir", "Kodu yaz", "Kullan'a bas"), "Bilemedin mi? İpucu yorumlarda",
        "samimi ve sıcak: hediye veren, yanında duran bir arkadaş gibi; yarışmacı değil davetkâr. Ödülün ne olduğunu sen söyleme, ekranda yazıyor"
    ),
    Deal(
        "Fırsat dili", "Hediye kodun", "Hediye kodun",
        "Kod, çizilen şeyin adı", "Kodu uygulamaya gir",
        listOf("Ayarlar", "Kupon kodu", "Kullan"), "İpucu yorumlarda",
        "net ve fırsat vurgulu: kısa, süreli ve sınırlı olduğunu hissettiren ama abartısız, bağıran değil. Ödülün ne olduğunu sen söyleme, ekranda yazıyor"
    );

    fun codeTitle(word: Boolean) = if (word) titleWord else titleShown
    fun explain(word: Boolean) = if (word) explainWord else explainShown

    companion object {
        /** The set for the [turn]th video, the sets taking it in turns. */
        fun forTurn(turn: Int): ForgotStyle = entries[Math.floorMod(turn, entries.size)]
    }
}

/** Where the Düello's drawing comes from. */
enum class ForgotSource(val title: String) {
    Phone("Telefonum"),
    /** The players' rounds in the reviewed pool: the ones whose drawer did not recognise them come first. */
    Pool("Havuz")
}

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
    /** Said over the countdown, to the viewer: the question that makes the 3 · 2 · 1 mean something. */
    val askLine: SpokenLine,
    /** Karalak's reaction and the code, one line, said as the code comes up. */
    val codeLine: SpokenLine,
    val outro: SpokenLine,
    val mode: ForgotMode = ForgotMode.Word,
    /** What the code gives, as the viewer reads it ("Sanatçı Kasası"). */
    val reward: String = "",
    val code: String = "",
    /** Hand-entered share of players who could not recognise their own drawing of this word; shown when [showPercent]. */
    val percent: Int = 70,
    val showPercent: Boolean = false,
    /** In [ForgotMode.Word]: whether the code's first letter is shown in its first box (a hint for the viewer). */
    val showFirstLetter: Boolean = false,
    /** Which of the word's letter lines (counted from 0, spaces not counted) have their letter written on them in the video, picked by hand. */
    val revealed: Set<Int> = emptySet(),
    /** How many accounts may use the code and when it stops working (0: not set), as the coupon will hold them; shown on the code screen. */
    val uses: Int = 0,
    val expiresAtMillis: Long = 0L,
    /** Which wording the on-screen texts use. */
    val style: ForgotStyle = ForgotStyle.Game
) {
    /** The word's letters in the order of its lines (spaces left out), as the lines show them: capitals, Turkish dotted/dotless i kept. */
    val slotLetters: List<Char> get() = word.trim().filter { !it.isWhitespace() }.uppercase(Locale("tr")).toList()

    /** Where each guess stops the drawing, ready to use: one per guess, in order, none past the whole. */
    val stops: List<Float> get() = ForgotTimeline.stopsFor(guesses.size, guessAt)

    /** Every spoken line in order: the voice is recorded and laid out in this order. */
    fun lines(): List<SpokenLine> = buildList {
        add(hook)
        addAll(guesses.take(ForgotTimeline.MAX_GUESSES))
        add(askLine)
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
        // the countdown is a question put to the viewer, out loud; it lasts as long as the question takes, 1.5 s at the least
        speak(ForgotPhase.Countdown, -1, script.askLine, COUNTDOWN_MS, 150, 1f, 1f)
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

    /** [stops] with guess [index] moved to [wanted], held between the stops on either side of it (a hundredth apart) and the paper's ends. */
    fun moveStop(stops: List<Float>, index: Int, wanted: Float): List<Float> {
        if (index !in stops.indices) return stops
        val low = if (index == 0) MIN_STOP else stops[index - 1] + 0.01f
        val high = if (index == stops.lastIndex) 1f else stops[index + 1] - 0.01f
        return stops.toMutableList().also { it[index] = wanted.coerceIn(low, high.coerceAtLeast(low)) }
    }

    fun at(events: List<ForgotEvent>, t: Long): ForgotEvent = events.firstOrNull { t < it.endMs } ?: events.last()

    fun totalMs(events: List<ForgotEvent>): Long = events.lastOrNull()?.endMs ?: 0L
}

object ForgotCopy {
    /** Before the prize on the board: "Ödül: 500 altın" or "Ödül: Çırak Kasası". */
    const val REWARD_LABEL = "Ödül:"
    /** The format's name, as the banner shows it (capitals), and the same without the dots over Ö and Ü (for measuring the capitals' body). */
    const val NAME = "Ödüllü Karalak Düellosu"
    const val TITLE = "ÖDÜLLÜ KARALAK DÜELLOSU"
    const val TITLE_BODY = "ODULLU KARALAK DUELLOSU"
    const val HOOK = "Çizen bile bilemedi. Sen bilir misin?"
    const val OUTRO = "Yarın daha zoru geliyor. Karalak'ı Google Play'den indir, sen de çiz!"
    const val ASK = "Sen ne görüyorsun?"
    const val CODE_WORD = "Bilemedin mi? Doğru cevap senin kodun!"
    const val CODE_SHOWN = "Bilemedin mi? Kodu Ayarlar'dan gir!"

    /** Karalak makes two guesses unless told otherwise (three at most), and none runs longer than this, nor the hook or the code line. */
    const val DEFAULT_GUESSES = 2
    const val GUESS_WORDS = 5
    const val HOOK_WORDS = 8
    const val ASK_WORDS = 4
    const val CODE_WORDS = 9

    fun codeLineFor(mode: ForgotMode) = if (mode == ForgotMode.Word) CODE_WORD else CODE_SHOWN

    /**
     * The two kinds of hook the video opens with, taking turns (and on every "yenile"): a challenge that sets the viewer against
     * the drawing ("ben bilirim"), and the crowd that got it wrong ("çoğu yanlış bildi", never with a number made up).
     */
    enum class HookKind(val title: String, val how: String, val examples: List<String>) {
        Challenge(
            "Meydan okuma",
            "izleyiciye doğrudan \"sen\" diye seslen ve bilebileceğine kendini inandır: bunu çözmek kolay görünsün ama çözülmesin, gurur dokunsun.",
            listOf("Çizenin kendisi bile bilemedi. Sen bilirsin belki?", "Herkes yanlış bildi. Sen bilir misin?", "Bunu bilen çıkmadı. İlk sen ol.")
        ),
        Crowd(
            "Çoğu yanlış bildi",
            "başkalarının yanıldığını söyle (ama HİÇBİR sayı, yüzde ya da kişi sayısı uydurma) ve izleyiciyi \"ben farklı olurum\" demeye it.",
            listOf("Bunu görenlerin çoğu yanlış bildi.", "Bu çizimde herkes aynı yere takıldı.", "Bakınca kolay sanıyorlar. Sonra yanılıyorlar.")
        );

        /** The kind for the [turn]-th hook: they alternate. */
        companion object { fun forTurn(turn: Int): HookKind = entries[Math.floorMod(turn, entries.size)] }
    }

    /** What every hook must keep to, whatever its kind. */
    private fun hookRules(kind: HookKind): String = """
        KANCA KURALLARI (ilk 2 saniyede izleyiciyi durdurmalı ve "ben bilirim" dedirtmeli):
        - Tür: ${kind.title}. ${kind.how}
        - Örnekler (kopyalama, aynı havada yeni bir cümle kur): ${kind.examples.joinToString(" / ") { "\"$it\"" }}
        - Emir ya da soruyla başla; en fazla $HOOK_WORDS kelime, tek nefeste söylenecek kadar kısa.
        - Selamlama, tanıtım ya da "Bugün size…" gibi girişler YASAK. Cevabı ya da cevaba dair ipucu verme. Çizenle alay etme.
        - Ödülün adını söyleme (kod ekranında zaten yazıyor).
    """.trimIndent()

    /**
     * Just the hook, for the "yenile" button: a new one of [kind], different from the [avoid]ed ones (those already tried).
     * Answer: `{"hook":{"emotion":"…","line":"…"}}`.
     */
    fun hookPrompt(word: String, kind: HookKind, moods: String, avoid: List<String>): String = """
        Videonun açılış cümlesini (kanca) yaz. Çizilmesi istenen kelime: "$word"; hiçbir cümlede bu kelime ya da kökü geçmesin.
        Çizim şu an çiziliyor ve ekranda görünüyor; Karalak bu cümleyi videonun ilk saniyesinde söylüyor.

        ${hookRules(kind)}
        ${if (avoid.isEmpty()) "" else "Bunlardan FARKLI olsun, hiçbirine benzemesin: " + avoid.joinToString(" / ") { "\"$it\"" }}

        Yalnızca şu JSON'u yaz: {"hook":{"emotion":"…","line":"…"}}
        emotion yalnızca şunlardan biri: $moods.
    """.trimIndent()

    /** The hook out of an answer to [hookPrompt]; null when it is missing or gives the [word] away. */
    fun parseHook(text: String, word: String): SpokenLine? {
        val o = StudioCopy.objectIn(text) ?: return null
        val h = o["hook"] as? JsonObject ?: return null
        val l = (h["line"] ?: h["text"])?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (leaks(l, word)) return null
        return SpokenLine(l, MascotEmotion.of(h["emotion"]?.jsonPrimitive?.contentOrNull))
    }

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

    /**
     * The assistant's lines: `{"plan":"…","hook":{…},"guesses":[{"emotion","line"}…],"ask":{…},"code_line":{…},"outro":{…},
     * "caption":"…","hashtags":"…"}`. The plan is its own working-out and is not kept.
     */
    data class Written(
        val guesses: List<SpokenLine>, val hook: SpokenLine?, val ask: SpokenLine?, val codeLine: SpokenLine?, val outro: SpokenLine?,
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
        return Written(
            guesses.take(ForgotTimeline.MAX_GUESSES), line(o["hook"]), line(o["ask"]), line(o["code_line"]), line(o["outro"]),
            clean("caption"), clean("hashtags")
        )
    }

    /** When a new code stops working unless the person picks another time: two days from now. */
    fun defaultExpiry(now: Long = System.currentTimeMillis()): Long = now + 48L * 3_600_000L

    /** A share of the drawing as the panel and the assistant say it: 0.35 → 35. */
    fun percent(share: Float): Int = kotlin.math.round(share * 100f).toInt()

    /** How many boxes the code screen puts in a row at most. */
    const val CODE_ROW = 12

    /** How many boxes go in a row: one row up to [CODE_ROW], else the same in every row (11 letters are 11, 13 are 7 and 6), never a lone one. */
    fun rowSize(length: Int): Int {
        if (length <= 0) return CODE_ROW
        val rows = (length + CODE_ROW - 1) / CODE_ROW
        return (length + rows - 1) / rows
    }

    /**
     * How many may use the code and until when, as the code screen says it in [style]'s words (empty when neither is set):
     * "Sadece 100 kişi · Son: 8 Kasım 21:00", "100 kişi açabilir · Son: …", "Son fırsat · 8 Kasım 21:00 · İlk 100 kişi".
     */
    fun limitText(uses: Int, expiresAtMillis: Long, style: ForgotStyle = ForgotStyle.Game, zone: java.time.ZoneId = java.time.ZoneId.systemDefault()): String =
        limitChips(uses, expiresAtMillis, style, zone).joinToString(" · ") { it.text }

    /** The two things [limitText] says, one chip each on the code screen: how many may use the code, and until when. */
    data class LimitChip(val isWho: Boolean, val text: String)

    fun limitChips(uses: Int, expiresAtMillis: Long, style: ForgotStyle = ForgotStyle.Game, zone: java.time.ZoneId = java.time.ZoneId.systemDefault()): List<LimitChip> {
        val who = if (uses <= 0) null else when (style) {
            ForgotStyle.Game -> "Sadece $uses kişi"
            ForgotStyle.Warm -> "$uses kişi açabilir"
            ForgotStyle.Deal -> "İlk $uses kişi"
        }
        val until = if (expiresAtMillis <= 0) null else {
            val at = java.time.format.DateTimeFormatter.ofPattern("d MMMM HH:mm", Locale("tr")).withZone(zone).format(java.time.Instant.ofEpochMilli(expiresAtMillis))
            if (style == ForgotStyle.Deal) "Son fırsat · $at" else "Son: $at"
        }
        val chips = listOfNotNull(who?.let { LimitChip(true, it) }, until?.let { LimitChip(false, it) })
        // the "Fırsat" wording leads with the deadline
        return if (style == ForgotStyle.Deal) chips.reversed() else chips
    }

    /**
     * What the assistant is told: the drawing as it looks at each guess, the video's scene order with what is on screen at
     * every spoken moment and how long it lasts, and what it must keep in mind (the mode, the reward, the word it may not say).
     * It works its plan out first, then writes each line for its own moment.
     */
    fun directorPrompt(word: String, stops: List<Float>, moods: String, mode: ForgotMode, reward: String, limit: String, style: ForgotStyle = ForgotStyle.Game, hookKind: HookKind = HookKind.Challenge): String {
        val n = stops.size
        val pictures = stops.mapIndexed { i, share -> "Görsel ${i + 1}: çizimin %${percent(share)}'i çizilmiş." }.joinToString("\n") + "\nGörsel ${n + 1}: bitmiş çizim."
        val guessScenes = stops.mapIndexed { i, share ->
            val before = if (i == 0) "" else " Çizim kaldığı yerden devam etti."
            "${i + 2}. TAHMİN ${i + 1} (Görsel ${i + 1}): ekranda çizim %${percent(share)} çizilmiş durumda.$before Karalak o anda gördüğüne bakarak tahmin ediyor (≈2 sn, EN FAZLA $GUESS_WORDS kelime)."
        }.joinToString("\n")
        val codeScene = if (mode == ForgotMode.Word) {
            "tahtada kupon kodunun BOŞ kutuları (harf sayısı kadar), ödül${if (limit.isNotBlank()) " ve \"$limit\"" else ""} ile \"nereye girilir\" bilgisi yazıyor. Kod, çizilen şeyin adının kendisi: bilen girer, bilmeyen yorumlara bakar. Karalak tek cümleyle hem bilemeyenle ilgili bir tepki veriyor hem de kodun çizilen şeyin adı olduğunu sezdiriyor (cevabı VERMEDEN)."
        } else {
            "tahtada kupon kodu yazılı olarak beliriyor, ödül${if (limit.isNotBlank()) " ve \"$limit\"" else ""} ile \"nereye girilir\" bilgisi yazıyor. Karalak tek cümleyle hem bir tepki veriyor hem de kodu uygulamada Ayarlar'dan girmelerini söylüyor."
        }
        val rewardLine = if (reward.isNotBlank()) "Bu videonun ödülü: $reward. Kod cümlesi bu ödüle heyecan katabilir (adını söylemek zorunda değil)." else ""
        return """
            Sana bir oyuncunun çizimini adım adım gösteriyorum. Çizilmesi istenen kelime: "$word". Çizen kişi bir süre sonra kendi çizimini hatırlayamadı. Doğru cevap bir kupon kodu: hiçbir cümlede ya da açıklamada "$word" kelimesi (ya da kökü) geçmemeli.

            @@PICTURES@@

            VİDEONUN SAHNE AKIŞI. Karalak'ın sesi bu sırayla, bu anlarda duyulur; her cümle O ANDA ekranda görünenle tutarlı olmalı:
            1. AÇILIŞ (0 sn): çizim çizilmeye başlıyor, ilk tahmine kadar çiziliyor. Karalak kancayı söylüyor (≈2,5 sn, EN FAZLA ${HOOK_WORDS} kelime).
            $guessScenes
            ${n + 2}. SORU (geri sayım 3-2-1, ≈1,5 sn): çizim tamamlandı, ekranda geri sayım var. Karalak izleyiciye soruyor (EN FAZLA ${ASK_WORDS} kelime).
            ${n + 3}. KOD ANI (≈3 sn): $codeScene (EN FAZLA ${CODE_WORDS} kelime)
            ${n + 4}. KAPANIŞ: kapanış kartı. Karalak yarın daha zorunun geleceğini söyleyip izleyiciyi MOBİL UYGULAMAYA yönlendiriyor: Google Play'den "Karalak"ı indirmesini söylesin (en fazla 14 kelime). Oyun mobil uygulamada oynanıyor; web sitesi, "karalak.com", "kurulumsuz", "link" gibi sözler ASLA geçmesin.
            $rewardLine

            ${hookRules(hookKind)}

            ÖNCE "plan" alanında düşün (en fazla 4 kısa cümle): her görselde ne görünüyor, Karalak onu nasıl yanlış ama makul yorumlar, izleyici o anda ne hissetmeli? SONRA her cümleyi kendi anı için yaz:
            SESLENDİRME İLKELERİ (cümleler sesli okunacak; kulağa doğal gelmeli, yazıda iyi durması yetmez):
            - Konuşma dilinde, kısa, ritimli yaz; noktalama nefes yeridir. Resmî dil, yazı dili, "Merhaba arkadaşlar", "Evet", "Peki" gibi dolgu girişler yok.
            - Her cümle farklı bir kalıpla kurulsun: soru, ünlem, itiraf, inatla savunma, kendi kendine yorum. Aynı kalıpla iki cümle başlamasın ("Hmm," ve "Bence" ile başlama, "Bu ne olabilir?" deme).
            - Somut ol: "bir şeye benziyor" gibi boş cümle yerine görselde gördüğün ŞEKLE bağla ("Bu çizgi bir kuyruk mu, yoksa ip mi?").
            - Karalak'ın kişiliği duyulsun: kendinden emin, muzip, heyecanlı; yanılınca utanıp hemen toparlanır. Her cümlede küçük bir duygu hareketi olsun (şaşkınlık, iddia, sevinç).
            - Kötü: "Bu bir hayvan olabilir." İyi: "Bunlar kulak, eminim, kulak!" (Yalnızca tarz örneği; kopyalama.)
            - Soru cümlesi son tahmine bağlansın ve izleyiciyi "ben bilirim" demeye itsin ("Sence de öyle mi?", "Yanılıyor muyum?" gibi, kopyalama).
            - Kod anı cümlesi merak ve hızlı kazanma duygusu versin; ödülü ya da yazıyı tekrar etmesin.
            - Kapanış kısa, sıcak ve davetkâr olsun, mobil uygulamaya götürsün; "Google Play" ve "Karalak" kelimeleri geçsin.
            - Tahminleri YALNIZCA kendi görseline bakarak yaz: Karalak o anda yalnızca o kadarını görüyor, sonrasını bilmiyor. Görselde gerçekten görünen bir şeye bakan komik, yanlış ama makul tahminler olsun; ilk tahmin belirsiz, sonuncusu en kendinden emin. Tahminler birbirini izlesin: sonraki, öncekinden çıkarım yapsın ya da onu inatla savunsun; aynı şeyi tekrarlama.
            - Soru, son tahminle bağlantılı olsun ve izleyiciyi yorum yazmaya itsin ("Sen ne görüyorsun?" gibi).
            - Çizenle ve izleyiciyle asla dalga geçme.
            - Üslup: ${style.tone}.
            - Bütün cümleler kısa ve konuşma diliyle olsun; video 20 saniyenin altında kalmalı.
            - Kod ekranında zaten yazan bilgiyi (ödül, son tarih, nereye girileceği) cümlede TEKRARLAMA; cümle yazının üstüne duygu katsın.

            "caption": TikTok/Reels açıklaması (1–2 cümle, izleyiciye soruyla bitsin, doğru kelime geçmesin; web sitesi ya da "karalak.com" yazma, uygulamayı Google Play'den indirmeye yönlendir); "hashtags": 5–6 Türkçe ağırlıklı etiket.

            Yalnızca şu JSON'u yaz:
            {"plan":"…","hook":{"emotion":"…","line":"…"},"guesses":[{"emotion":"…","line":"…"}],"ask":{"emotion":"…","line":"…"},"code_line":{"emotion":"…","line":"…"},"outro":{"emotion":"…","line":"…"},"caption":"…","hashtags":"…"}
            emotion yalnızca şunlardan biri: $moods. "guesses" tam $n eleman içermeli.
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
