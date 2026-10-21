package com.sualtikasifi.cizimhafiza.presentation.reports.studio.real

import com.sualtikasifi.cizimhafiza.presentation.reports.console.OpenRouterClient
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StrokeReplay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Prepares "Gerçek Olsaydı" with the assistant: it looks at the chosen drawings and, for each, writes what
 * the picture and video prompts need (the drawing's shapes as drawn, a detailed little scene for the video), and a post text for the whole video.
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

            Her çizim için ("reveals"), çizim gerçek bir fotoğrafa ve sonra kısa bir videoya dönüşecek. Her şeyi TÜRKÇE yaz:
            - "describe": çizimin şeklini bir plan gibi, gördüğün çizgilerle birebir anlatan kısa cümleler: gövdenin şekli, kaç bacak ve neye benzedikleri, baş, kuyruk, özel işaretler (ör. "uzun düz dikdörtgen bir gövde, üç parmaklı ayakları olan dört düz çubuk bacak, iki üçgen kulaklı küçük yuvarlak bir baş, üç halkalı bir spiral olarak çizilmiş kuyruk"). Ne olması gerektiğini değil, ne çizildiğini yaz — gerçeğe dönüştürülürken bu şekil korunacak;
            - "motion": bu nesneyle ilgili 8 saniyelik, YARATICI ve KOMİK bir sahne, AYRINTILI (6–9 cümle). Nesne tek başına durup sallanmasın: bir insan gelsin, onunla doğal ama beklenmedik bir şey yapsın, küçük bir tepki versin. Nesnenin şekli hiç değişmez, insan hep sıradan ve gerçek biri. Sahneyi sırayla yaz: nerede olduğu, kim girdiği (yaşı, üstü başı), ilk saniyelerde ne yaptığı, komik anın ne olduğu, sonda nasıl tepki verdiği; araya duyulacak sesleri de koy (ör. düğme tıkırtısı, adım sesi, nesnenin kendi sesi). Örnekler: mikrodalga fırın için "Mutfakta, tezgâhın üstünde duran mikrodalga fırının önüne pijamalı orta yaşlı bir adam gelir. Elindeki yemek tabağını içeri koyar, tuhaf küçük pencereden içeri bakarak kapağı kapatır ve düğmelere basar. Düğme tıkırdar, tabak dönmeye başlar, fırın hafifçe vızıldar ve içi turuncu parlar. Adam iyice eğilip pencereden içeri gözlerini kısarak bakar, sonra memnun bir şekilde başını sallar"; nar için "Pazar yerinde bir adam tezgâhın önünden geçerken durur ve nara şaşkınlıkla bakar. Narı eline alıp çevirir, ters yüz eder, şekline inanamayarak kaşlarını kaldırır. Sonra omuz silker ve narı poşetine atar. Poşet hışırdar, çevrede pazarın uğultusu duyulur"; saz için "Bir ev odasında duvara yaslı duran saza yaşlı bir adam yaklaşır, onu elinde yoklar, sandalyeye oturur ve neşeli bir türkü çalmaya başlar. Teller tınlar, adamın başı ritme göre sallanır, sonda gülümseyerek sazı kucağında sallar". Nesnenin ne işe yaradığını düşün ve ona uygun, günlük hayattan, eğlenceli bir sahne kur.
            Ayrıca üç videoyu birlikte anlatan TikTok/Reels açıklaması (1–2 cümle, soruyla bitsin) ve 5–6 Türkçe ağırlıklı etiket yaz.

            Yalnızca şu JSON'u yaz:
            {"reveals":[{"index":1,"describe":"…","motion":"…"}],"caption":"…","hashtags":"…"}
        """.trimIndent()
        val answer = client.generate(SYSTEM, prompt, model, webSearch = false, images = images, json = true, temperature = 0.7, maxTokens = 4000).getOrThrow()
        val parsed = RealCopy.parsePicks(answer.text, shown.size) ?: error("Yanıt okunamadı; tekrar dene.")
        Written(
            reveals = parsed.reveals.map { r -> RealReveal(shown[r.index].id, "", r.describe, r.motion) },
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
