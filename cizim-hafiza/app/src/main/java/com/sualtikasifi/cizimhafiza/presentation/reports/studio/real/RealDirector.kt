package com.sualtikasifi.cizimhafiza.presentation.reports.studio.real

import com.sualtikasifi.cizimhafiza.presentation.reports.console.OpenRouterClient
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StrokeReplay
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.guess.SpokenLine
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MascotEmotion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Writes "Gerçek Olsaydı" with the assistant: it looks at the word's drawings, picks the funniest
 * and most different ones with a joke each, chooses which of them would be the most fun as real
 * things (describing their oddities so the picture prompt keeps them), and later, once a realistic
 * picture is in, gives Karalak's reaction to it.
 */
class RealDirector(private val client: OpenRouterClient, private val model: String) {

    class Written(
        val wordEn: String,
        val roasts: List<RealRoast>,
        val reveals: List<RealReveal>,
        val outro: SpokenLine?,
        val caption: String,
        val hashtags: String,
        val costUsd: Double?
    )

    suspend fun pick(word: String, seconds: Int, candidates: List<RealDrawing>, roastCount: Int, revealCount: Int): Result<Written> = runCatching {
        val shown = candidates.take(MAX_CANDIDATES)
        require(shown.isNotEmpty()) { "Bu kelime için çizim yok" }
        val images = withContext(Dispatchers.Default) { shown.map { StrokeReplay.png(it.strokes, 1f, 384) } }
        val want = roastCount.coerceIn(1, shown.size)
        val turn = revealCount.coerceIn(1, want)
        val prompt = """
            Sana "$word" kelimesi için oyuncuların $seconds saniyede yaptığı ${shown.size} çizimi gösteriyorum, sırayla 1'den ${shown.size}'e kadar numaralı.

            1) En komik, en tuhaf ve birbirinden en farklı $want çizimi seç (birbirine benzeyenlerden yalnızca birini al). Seçtiklerini en zayıftan en komiğe doğru sırala.
            2) Her biri için Karalak'ın ağzından çizimle tatlı tatlı dalga geçen en fazla 9 kelimelik bir espri yaz. Çizimin kendisine takıl (oranları, eksikleri, neye benzediği); çizen kişiye asla.
            3) Seçtiklerinden, gerçek bir fotoğrafa dönüşünce en eğlenceli görünecek $turn tanesini seç ("reveals"). Her biri için:
               - "describe": çizimin tuhaflıklarını anlatan kısa bir İngilizce cümle (ör. "a cat with six legs, a tiny head and a very long neck") — gerçekçi görsel üretilirken bu tuhaflıklar korunacak;
               - "line": Karalak'ın o çizimin gerçeğini görünce söyleyeceği en fazla 9 kelimelik şaşkın ya da komik bir cümle.
            4) "word_en": kelimenin İngilizcesi (tek ya da iki kelime).
            5) Kapanış cümlesi: izleyiciye "daha iyisini çizebilir misin" diye meydan oku, en fazla 14 kelime, "karalak.com" geçsin.
            6) TikTok/Reels açıklaması (1–2 cümle, soruyla bitsin) ve 5–6 Türkçe ağırlıklı etiket.

            Yalnızca şu JSON'u yaz:
            {"word_en":"…","roasts":[{"index":3,"emotion":"…","line":"…"}],"reveals":[{"index":3,"emotion":"…","line":"…","describe":"…"}],"outro":{"emotion":"…","line":"…"},"caption":"…","hashtags":"…"}
            emotion yalnızca şunlardan biri: ${EMOTIONS}.
        """.trimIndent()
        val answer = client.generate(SYSTEM, prompt, model, webSearch = false, images = images, json = true, temperature = 0.9, maxTokens = 1500).getOrThrow()
        val parsed = RealCopy.parsePicks(answer.text, shown.size) ?: error("Seçim okunamadı; tekrar dene.")
        Written(
            wordEn = parsed.wordEn,
            roasts = parsed.roasts.take(want).map { (i, l) -> RealRoast(shown[i].id, l) },
            reveals = parsed.reveals.filter { (i, _, _) -> parsed.roasts.take(want).any { it.first == i } }.take(turn)
                .map { (i, l, d) -> RealReveal(shown[i].id, l, d) },
            outro = parsed.outro,
            caption = parsed.caption,
            hashtags = parsed.hashtags,
            costUsd = answer.costUsd
        )
    }

    /** Karalak sees the drawing and the realistic picture made from it, and reacts in one line. */
    suspend fun react(word: String, doodle: RealDrawing, photoPng: ByteArray): Result<Pair<SpokenLine, Double?>> = runCatching {
        val sketch = withContext(Dispatchers.Default) { StrokeReplay.png(doodle.strokes, 1f, 384) }
        val prompt = """
            Birinci görsel, bir oyuncunun birkaç saniyede çizdiği "$word". İkinci görsel, aynı çizimin yapay zekâyla gerçek bir fotoğrafa dönüştürülmüş hâli.
            Karalak olarak ikinci görseli ilk kez görüyorsun. En fazla 9 kelimelik, şaşkın ya da komik, sesli okunacak bir tepki cümlesi yaz.
            Fotoğrafta gerçekten gördüğün bir ayrıntıya değin. Kimseyle dalga geçme, korkutucu ya da iğrenç ifadeler kullanma.
            Yalnızca şu JSON'u yaz: {"emotion":"…","line":"…"}
            emotion yalnızca şunlardan biri: ${EMOTIONS}.
        """.trimIndent()
        val answer = client.generate(SYSTEM, prompt, model, webSearch = false, images = listOf(sketch, photoPng), json = true, temperature = 0.9, maxTokens = 300).getOrThrow()
        val line = RealCopy.parseReaction(answer.text) ?: error("Tepki okunamadı; tekrar dene.")
        line to answer.costUsd
    }

    private companion object {
        const val MAX_CANDIDATES = 12
        val EMOTIONS = MascotEmotion.entries.joinToString(", ") { it.key }
        val SYSTEM = """
            Sen Karalak'sın: Karalak çizim oyununun maskotu, sevimli bir köpek yavrusu. Kendinden emin, esprili, sıcak.
            Türkçe, konuşma dilinde, kısa konuşursun. Espri yaparken çizime takılırsın; çizen kişiyi aşağılamazsın. Küfür, argo, siyaset yok.
            Şimdi "Gerçek Olsaydı" adlı kısa bir sosyal medya videosu için çizim seçip espri yazıyorsun; cümleler sesli okunacak.
        """.trimIndent()
    }
}
