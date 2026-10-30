package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import java.util.Locale

/** A language Karalak's videos can be made in: what it speaks, what it is called to the assistant, and what the phone's voice is set to. */
enum class StudioLang(val code: String, val label: String, val named: String, val locale: Locale) {
    Tr("tr", "TR", "Türkçe", Locale("tr", "TR")),
    En("en", "EN", "İngilizce", Locale("en", "US")),
    Pt("pt", "PT", "Portekizce", Locale("pt", "BR")),
    Es("es", "ES", "İspanyolca", Locale("es", "ES"));

    companion object {
        fun of(code: String?): StudioLang = entries.find { it.code == code } ?: Tr
    }
}

/**
 * Who Karalak is, written once for every prompt that has it speak (the guessing video, the memory one, the hooks and
 * closings of the real one), and the language those prompts ask the lines in. The prompts themselves stay in Turkish;
 * for another language one note at their end moves the spoken lines (and the post text) over, and the words the
 * program itself reads back (the guesses, the JSON keys, the emotion names) stay as they are.
 */
object KaralakVoice {
    /** The mascot, in Turkish, as every prompt starts. */
    private const val CHARACTER =
        "Sen Karalak'sın: Karalak çizim oyununun maskotu, sevimli bir köpek yavrusu. Kendinden emin, esprili, sıcak, biraz muzip; " +
            "çabuk heyecanlanır, yanılınca utanır ama hemen toparlanır."

    private const val MANNERS =
        "Konuşma dilinde, kısa ve esprili konuşursun; söylediklerin sesli okunacak. Şakaların çizimle ilgilidir; " +
            "oyuncuyla asla dalga geçmez, kimseyi aşağılamazsın. Küfür, argo, siyaset yok."

    /** The system prompt: who Karalak is, then [job] (what it is doing now), then the language note when it is not Turkish. */
    fun persona(job: String, lang: StudioLang = StudioLang.Tr): String =
        listOf("$CHARACTER\n$MANNERS", job.trim(), languageNote(lang)).filter { it.isNotEmpty() }.joinToString("\n")

    /**
     * The sentence that moves the lines to [lang]; empty for Turkish, so the Turkish prompts are exactly what they
     * were. [kept] names what must not be translated (it goes into the sentence).
     */
    fun languageNote(lang: StudioLang, kept: String = "JSON anahtarları ve emotion değerleri"): String =
        if (lang == StudioLang.Tr) ""
        else "ÖNEMLİ: Karalak'ın söyleyeceği bütün cümleleri ve gönderi metnini (açıklama, etiketler) ${lang.named} yaz, Türkçe değil; " +
            "kelime sınırları aynı kalsın, konuşma dili ve ${lang.named} konuşan birinin doğal söyleyişi olsun. $kept değişmez. " +
            "Kelime olarak Türkçe verilen çizim sözcüklerini ${lang.named} karşılığıyla söyle."

    /** The same note for a prompt (the user turn) rather than the system one, set apart by a blank line. */
    fun promptNote(lang: StudioLang, kept: String = "JSON anahtarları ve emotion değerleri"): String =
        languageNote(lang, kept).let { if (it.isEmpty()) "" else "\n\n$it" }
}

/**
 * How long a spoken line may run, in words. The prompts ask for these, but a model gives a few words more now and then,
 * and a longer line is a longer voice (credits), a subtitle that no longer fits and a slower video.
 */
object LineRules {
    const val STAGE_WORDS = 9
    const val INTRO_WORDS = 12
    const val REACTION_WORDS = 10
    const val OUTRO_WORDS = 14
    const val HOOK_WORDS = 8
    const val CLOSING_WORDS = 8
    const val PICK_WORDS = 7

    /** How many words past the limit still pass: a limit is a target, not a wall. */
    const val SLACK = 2

    private val WORD = Regex("\\S+")

    fun words(text: String): Int = WORD.findAll(text).count()

    /** Whether [text] is too long to keep as it is. */
    fun tooLong(text: String, maxWords: Int): Boolean = words(text) > maxWords + SLACK

    /**
     * [text] cut to [maxWords] words if it is longer than the limit with its slack: back to the last sentence end
     * or comma that still fits when there is one past the first half, otherwise at a word. Never more than needed.
     */
    fun fit(text: String, maxWords: Int): String {
        val t = text.trim()
        if (!tooLong(t, maxWords)) return t
        val kept = WORD.findAll(t).take(maxWords).toList()
        val cut = t.substring(0, kept.last().range.last + 1)
        val soft = cut.indexOfLast { it == '.' || it == '!' || it == '?' || it == ',' || it == ';' }
        val out = if (soft >= cut.length / 2) cut.substring(0, soft + 1) else cut
        return out.trimEnd(',', ';', ' ')
    }
}

/**
 * Text as a voice should be given it. What is read back by the subtitle is the plain line; what is sent to the voice
 * has the things a text-to-speech reads badly (a web address, "&", "%") written the way they are said.
 */
object Pronunciation {
    private val SITE = Regex("\\b([a-zA-Z][a-zA-Z0-9-]*)\\.(com|net|org|app)\\b")

    private fun dot(lang: StudioLang) = when (lang) {
        StudioLang.Tr -> "nokta"
        StudioLang.En -> "dot"
        StudioLang.Pt -> "ponto"
        StudioLang.Es -> "punto"
    }

    private fun and(lang: StudioLang) = when (lang) {
        StudioLang.Tr -> "ve"
        StudioLang.En -> "and"
        StudioLang.Pt -> "e"
        StudioLang.Es -> "y"
    }

    private fun percent(lang: StudioLang) = when (lang) {
        StudioLang.Tr -> "yüzde"
        StudioLang.En -> "percent"
        StudioLang.Pt -> "por cento"
        StudioLang.Es -> "por ciento"
    }

    /** "karalak.com'da" → "karalak nokta com'da"; "%40" → "yüzde 40"; "&" → "ve". */
    fun forSpeech(text: String, lang: StudioLang = StudioLang.Tr): String {
        var s = SITE.replace(text) { m -> "${m.groupValues[1]} ${dot(lang)} ${m.groupValues[2]}" }
        s = s.replace(Regex("%\\s?(\\d+)")) { m -> "${percent(lang)} ${m.groupValues[1]}" }
            .replace(Regex("(\\d+)\\s?%")) { m -> if (lang == StudioLang.Tr) "yüzde ${m.groupValues[1]}" else "${m.groupValues[1]} ${percent(lang)}" }
            .replace(" & ", " ${and(lang)} ")
        return s
    }
}
