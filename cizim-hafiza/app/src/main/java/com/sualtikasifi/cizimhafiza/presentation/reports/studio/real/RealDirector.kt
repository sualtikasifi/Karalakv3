package com.sualtikasifi.cizimhafiza.presentation.reports.studio.real

import com.sualtikasifi.cizimhafiza.presentation.reports.console.OpenRouterClient
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StrokeReplay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Prepares "Gerçek Olsaydı" with the assistant: it looks at the chosen drawings and, for each, writes what
 * the picture and video prompts need (the word in English, the drawing's shapes as drawn, how that exact
 * shape would move), and a post text for the whole video.
 */
class RealDirector(private val client: OpenRouterClient, private val model: String) {

    class Written(val reveals: List<RealReveal>, val caption: String, val hashtags: String, val costUsd: Double?)

    suspend fun describe(candidates: List<RealDrawing>): Result<Written> = runCatching {
        val shown = candidates.take(MAX_CANDIDATES)
        require(shown.isNotEmpty()) { "Önce çizim seç" }
        val images = withContext(Dispatchers.Default) { shown.map { StrokeReplay.png(it.strokes, 1f, 384) } }
        val list = shown.mapIndexed { i, d -> "${i + 1}) ${d.word.ifBlank { "?" }}" + if (d.seconds > 0) " (${d.seconds} sn)" else "" }.joinToString("\n")
        val prompt = """
            Sana oyuncuların farklı kelimeler için birkaç saniyede yaptığı ${shown.size} çizimi gösteriyorum, sırayla 1'den ${shown.size}'e kadar numaralı. Çizimlerin kelimeleri:
            $list

            Her çizim için ("reveals"), çizim gerçek bir fotoğrafa ve sonra kısa bir videoya dönüşecek:
            - "word_en": çizimin kelimesinin İngilizcesi (tek ya da iki kelime);
            - "describe": çizimin şeklini bir plan gibi, gördüğün çizgilerle birebir anlatan kısa İngilizce cümleler: gövdenin şekli, kaç bacak ve neye benzedikleri, baş, kuyruk, özel işaretler (ör. "a long flat rectangle body, four straight stick legs with three-toe feet, a small round head with two triangle ears, a tail drawn as a spiral of three loops"). Ne olması gerektiğini değil, ne çizildiğini yaz — gerçeğe dönüştürülürken bu şekil korunacak;
            - "motion": bu şeklin, çizildiği hâliyle, gerçek hayatta doğal olarak nasıl hareket edeceğini anlatan TEK kısa İngilizce cümle. Yalnızca çizimde ZATEN olan parçaları hareket ettir; göz, yüz, kol, bacak, kanat gibi çizimde olmayan hiçbir şey ekleme (ör. "the three flames flicker and the whole thing shudders and rises slowly", "the four stick legs wobble as it leans from side to side").
            Ayrıca üç videoyu birlikte anlatan TikTok/Reels açıklaması (1–2 cümle, soruyla bitsin) ve 5–6 Türkçe ağırlıklı etiket yaz.

            Yalnızca şu JSON'u yaz:
            {"reveals":[{"index":1,"word_en":"…","describe":"…","motion":"…"}],"caption":"…","hashtags":"…"}
        """.trimIndent()
        val answer = client.generate(SYSTEM, prompt, model, webSearch = false, images = images, json = true, temperature = 0.7, maxTokens = 1800).getOrThrow()
        val parsed = RealCopy.parsePicks(answer.text, shown.size) ?: error("Yanıt okunamadı; tekrar dene.")
        Written(
            reveals = parsed.reveals.map { r -> RealReveal(shown[r.index].id, r.wordEn, r.describe, r.motion) },
            caption = parsed.caption,
            hashtags = parsed.hashtags,
            costUsd = answer.costUsd
        )
    }

    private companion object {
        const val MAX_CANDIDATES = 6
        val SYSTEM = """
            Sen Karalak çizim oyununun sosyal medya editörüsün. Çocuk çizimlerinin "gerçek olsaydı" hâlini gösteren kısa videolar için hazırlık yapıyorsun.
            Çizimleri olduğu gibi, süslemeden ve düzeltmeden anlatırsın. Çizen kişiyi asla küçümsemezsin.
        """.trimIndent()
    }
}
