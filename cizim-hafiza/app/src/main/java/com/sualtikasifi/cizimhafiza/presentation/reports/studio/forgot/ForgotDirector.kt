package com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot

import com.sualtikasifi.cizimhafiza.presentation.reports.console.OpenRouterClient
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.Backoff
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.KaralakVoice
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.LineRules
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StrokeReplay
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioDrawing
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioLang
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.SpokenLine
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MascotEmotion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Has the assistant look at one drawing and write Karalak's part: what it thinks while the drawing builds up (wrong, but
 * funny, and never the right word), its reaction to what the drawer wrote, a hook and a closing, and the post text.
 * The right word is told to it only so it can stay away from it; a line that says it anyway is dropped (see [ForgotCopy.leaks]).
 */
class ForgotDirector(private val client: OpenRouterClient, private val model: String, private val lang: StudioLang = StudioLang.Tr) {

    class Written(val lines: ForgotCopy.Written, val costUsd: Double?)

    suspend fun write(drawing: StudioDrawing, looks: Int = 3): Result<Written> = runCatching {
        val image = withContext(Dispatchers.Default) { StrokeReplay.png(drawing.strokes, 1f, 512) }
        val n = looks.coerceIn(1, ForgotTimeline.MAX_GUESSES)
        val prompt = """
            Sana bir oyuncunun çizimini gösteriyorum. Çizilmesi istenen kelime: "${drawing.word}".
            Çizen kişi bir süre sonra kendi çizimini hatırlayamadı.

            Video: Karalak çizimi canlı izliyor ve tahmin ediyor; izleyici de aynı anda tahmin edecek. Doğru cevap bir kupon kodu, o yüzden hiçbir cümlede "${drawing.word}" kelimesi (ya da kökü) geçmemeli.

            1) "guesses": Karalak'ın çizim oluşurken söylediği $n tahmin. Her biri en fazla 8 kelime, çizimde gerçekten görünen bir şeye bakarak komik ve yanlış (ama makul) tahminler, sonuncusu en kendinden emini. Çizenle asla dalga geçme.
            2) "reaction": geri sayım bittikten sonra Karalak'ın tepkisi, en fazla 10 kelime; izleyiciye "bildin mi?" diye takılsın, çizimdeki bir şeye değinebilir.
            3) "hook": videonun ilk cümlesi; "çizen kişi bile bilemedi" fikrini söylesin ve izleyiciyi tahmine çağırsın, en fazla 12 kelime.
            4) "outro": kapanış, yarın daha zorunun geleceğini söylesin, en fazla 14 kelime, "karalak.com" geçsin.
            5) "caption": TikTok/Reels açıklaması (1–2 cümle, izleyiciye soruyla bitsin, doğru kelime geçmesin) ve "hashtags": 5–6 Türkçe ağırlıklı etiket.

            Yalnızca şu JSON'u yaz:
            {"guesses":[{"emotion":"…","line":"…"}],"reaction":{"emotion":"…","line":"…"},"hook":{"emotion":"…","line":"…"},"outro":{"emotion":"…","line":"…"},"caption":"…","hashtags":"…"}
            emotion yalnızca şunlardan biri: ${MascotEmotion.entries.joinToString(", ") { it.key }}.
        """.trimIndent()
        val system = KaralakVoice.persona(JOB, lang)
        val answer = Backoff.retrying {
            client.generate(system, prompt + KaralakVoice.promptNote(lang, "JSON anahtarları ve emotion değerleri"), model, webSearch = false, images = listOf(image), json = true, temperature = 0.9, maxTokens = 1200)
        }.getOrThrow()
        val parsed = ForgotCopy.parse(answer.text, drawing.word) ?: error("Cümleler okunamadı; tekrar dene.")
        fun SpokenLine?.fit(words: Int) = this?.copy(text = LineRules.fit(text, words))
        Written(
            ForgotCopy.Written(
                parsed.guesses.take(n).map { it.copy(text = LineRules.fit(it.text, LineRules.STAGE_WORDS)) },
                parsed.reaction.fit(LineRules.REACTION_WORDS), parsed.hook.fit(LineRules.INTRO_WORDS), parsed.outro.fit(LineRules.OUTRO_WORDS),
                parsed.caption, parsed.hashtags
            ),
            answer.costUsd
        )
    }

    private companion object {
        const val JOB = "Şimdi \"Çizen Bile Bilemedi\" adlı kısa bir sosyal medya videosu için cümle yazıyorsun; cümleler sesli okunacak."
    }
}
