package com.sualtikasifi.cizimhafiza.presentation.reports.studio.memory

import com.sualtikasifi.cizimhafiza.presentation.reports.console.OpenRouterClient
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.Backoff
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.KaralakVoice
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.LineRules
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StrokeReplay
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioLang
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.guess.SpokenLine
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MascotEmotion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Has the assistant look at the forgotten drawings of one word, pick the most interesting and
 * varied ones, and give Karalak a short line for each, plus the closing line and the post text.
 * The numbers in the opening and the twist are not its to write (see MemoryCopy).
 */
class MemoryDirector(private val client: OpenRouterClient, private val model: String, private val lang: StudioLang = StudioLang.Tr) {

    class Written(val picks: List<MemoryPick>, val outro: SpokenLine?, val caption: String, val hashtags: String, val costUsd: Double?)

    suspend fun pick(stat: MemoryWordStat, candidates: List<MemorySample>, count: Int): Result<Written> = runCatching {
        val shown = candidates.take(MAX_CANDIDATES)
        require(shown.isNotEmpty()) { "Bu kelime için saklanmış çizim yok" }
        val images = withContext(Dispatchers.Default) { shown.map { StrokeReplay.png(it.strokes, 1f, 384) } }
        val want = count.coerceIn(1, shown.size)
        val prompt = """
            Sana "${stat.word}" kelimesi için çizilmiş ${shown.size} çizim gösteriyorum, sırayla 1'den ${shown.size}'e kadar numaralı.
            Bunları çizen oyuncular yaklaşık ${MemoryMath.roundForSpeech(stat.avgDelaySec)} saniye sonra kendi çizimlerinin ne olduğunu hatırlayamadı.
            Toplam: ${stat.drawn} kişi çizdi, ${stat.forgot} kişi tanıyamadı.

            1) En ilginç, en komik ve birbirinden en farklı $want çizimi seç (aynı şeye benzeyenlerden yalnızca birini al).
            2) Her biri için Karalak'ın ağzından en fazla 7 kelimelik sevimli bir yorum yaz: çizimin neye benzediğine dair, çizen kişiyle asla dalga geçmeden.
            3) Kapanış cümlesi: izleyiciye meydan oku, en fazla 14 kelime, "karalak.com" geçsin.
            4) TikTok/Reels açıklaması (1–2 cümle, soruyla bitsin) ve 5–6 Türkçe ağırlıklı etiket.

            Yalnızca şu JSON'u yaz:
            {"picks":[{"index":3,"emotion":"…","line":"…"}],"outro":{"emotion":"…","line":"…"},"caption":"…","hashtags":"…"}
            emotion yalnızca şunlardan biri: ${MascotEmotion.entries.joinToString(", ") { it.key }}.
        """.trimIndent()
        val system = KaralakVoice.persona(JOB, lang)
        val answer = Backoff.retrying {
            client.generate(system, prompt + KaralakVoice.promptNote(lang, "JSON anahtarları, emotion değerleri ve index"), model, webSearch = false, images = images, json = true, temperature = 0.85, maxTokens = 1200)
        }.getOrThrow()
        val parsed = MemoryCopy.parsePicks(answer.text, shown.size) ?: error("Seçim okunamadı; tekrar dene.")
        Written(
            parsed.picks.take(want).map { (i, line) -> MemoryPick(shown[i].id, shown[i].correct, line?.let { it.copy(text = LineRules.fit(it.text, LineRules.PICK_WORDS)) }) },
            parsed.outro?.let { it.copy(text = LineRules.fit(it.text, LineRules.OUTRO_WORDS)) }, parsed.caption, parsed.hashtags, answer.costUsd
        )
    }

    private companion object {
        const val MAX_CANDIDATES = 12
        const val JOB = "Şimdi \"Hafıza Tuzağı\" adlı kısa bir sosyal medya videosu için çizim seçip yorum yazıyorsun; yorumlar sesli okunacak."
    }
}
