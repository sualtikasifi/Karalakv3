package com.sualtikasifi.cizimhafiza.presentation.reports.studio.real

import com.sualtikasifi.cizimhafiza.presentation.reports.console.OpenRouterClient
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StrokeReplay
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.guess.SpokenLine
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MascotEmotion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Writes "Gerçek Olsaydı" with the assistant: it looks at the drawings chosen for the video (of different
 * words), orders them with a joke each, chooses which would be the most fun as real things (describing
 * their oddities so the picture prompt keeps them), and later, once a realistic picture is in, gives
 * Karalak's reaction to it.
 */
class RealDirector(private val client: OpenRouterClient, private val model: String) {

    class Written(
        val roasts: List<RealRoast>,
        val reveals: List<RealReveal>,
        val outro: SpokenLine?,
        val caption: String,
        val hashtags: String,
        val costUsd: Double?
    )

    /**
     * [candidates] are the drawings chosen for the video, of different words, each with the word it was drawn
     * for. The assistant orders them, writes a joke for each and marks the funniest [revealCount] to be turned real.
     */
    suspend fun pick(candidates: List<RealDrawing>, roastCount: Int, revealCount: Int): Result<Written> = runCatching {
        val shown = candidates.take(MAX_CANDIDATES)
        require(shown.isNotEmpty()) { "Önce çizim seç" }
        val images = withContext(Dispatchers.Default) { shown.map { StrokeReplay.png(it.strokes, 1f, 384) } }
        val want = roastCount.coerceIn(1, shown.size)
        val turn = revealCount.coerceIn(1, want)
        val list = shown.mapIndexed { i, d -> "${i + 1}) ${d.word.ifBlank { "?" }}" + if (d.seconds > 0) " (${d.seconds} sn)" else "" }.joinToString("\n")
        val prompt = """
            Sana oyuncuların farklı kelimeler için birkaç saniyede yaptığı ${shown.size} çizimi gösteriyorum, sırayla 1'den ${shown.size}'e kadar numaralı. Çizimlerin kelimeleri:
            $list

            1) Bu çizimlerden en komik, en tuhaf ve birbirinden en farklı $want tanesini seç. Seçtiklerini en zayıftan en komiğe doğru sırala.
            2) Her biri için Karalak'ın ağzından çizimle tatlı tatlı dalga geçen en fazla 9 kelimelik bir espri yaz. Çizimin kendisine takıl (oranları, eksikleri, neye benzediği, kelimeyle ilgisi); çizen kişiye asla.
            3) Seçtiklerinden, gerçek bir fotoğrafa dönüşünce en eğlenceli görünecek $turn tanesini seç ("reveals"). Her biri için:
               - "word_en": o çizimin kelimesinin İngilizcesi (tek ya da iki kelime);
               - "describe": çizimin şeklini bir plan gibi, gördüğün çizgilerle birebir anlatan kısa İngilizce cümleler: gövdenin şekli, kaç bacak ve neye benzedikleri, baş, kuyruk, özel işaretler (ör. "a long flat rectangle body, four straight stick legs with three-toe feet, a small round head with two triangle ears, a tail drawn as a spiral of three loops"). Ne olması gerektiğini değil, ne çizildiğini yaz — gerçeğe dönüştürülürken bu şekil korunacak;
               - "motion": bu şeklin, çizildiği hâliyle, gerçek hayatta doğal olarak nasıl hareket edeceğini anlatan TEK kısa İngilizce cümle. Yalnızca çizimde ZATEN olan parçaları hareket ettir; göz, yüz, kol, bacak, kanat gibi çizimde olmayan hiçbir şey ekleme (ör. "the three flames flicker and the whole thing shudders and rises slowly", "the four stick legs wobble as it leans from side to side");
               - "line": Karalak'ın o çizimin gerçeğini görünce söyleyeceği en fazla 9 kelimelik şaşkın ya da komik bir cümle.
            4) Kapanış cümlesi: izleyiciye "daha iyisini çizebilir misin" diye meydan oku, en fazla 14 kelime, "karalak.com" geçsin.
            5) TikTok/Reels açıklaması (1–2 cümle, soruyla bitsin) ve 5–6 Türkçe ağırlıklı etiket.

            Yalnızca şu JSON'u yaz:
            {"roasts":[{"index":3,"emotion":"…","line":"…"}],"reveals":[{"index":3,"emotion":"…","line":"…","word_en":"…","describe":"…","motion":"…"}],"outro":{"emotion":"…","line":"…"},"caption":"…","hashtags":"…"}
            emotion yalnızca şunlardan biri: ${EMOTIONS}.
        """.trimIndent()
        val answer = client.generate(SYSTEM, prompt, model, webSearch = false, images = images, json = true, temperature = 0.9, maxTokens = 2200).getOrThrow()
        val parsed = RealCopy.parsePicks(answer.text, shown.size) ?: error("Seçim okunamadı; tekrar dene.")
        Written(
            roasts = parsed.roasts.take(want).map { (i, l) -> RealRoast(shown[i].id, l) },
            reveals = parsed.reveals.filter { r -> parsed.roasts.take(want).any { it.first == r.index } }.take(turn)
                .map { r -> RealReveal(shown[r.index].id, r.line, r.describe, r.wordEn, r.motion) },
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
