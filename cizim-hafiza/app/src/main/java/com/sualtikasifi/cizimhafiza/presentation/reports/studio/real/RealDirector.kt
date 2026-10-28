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

    class Lines(val lines: List<String>, val costUsd: Double?)

    /** Ideas for one of Karalak's lines in the video of [drawing] and its [reveal]: the hook, or the closing reaction. */
    suspend fun lines(kind: RealLine, drawing: RealDrawing, reveal: RealReveal): Result<Lines> = when (kind) {
        RealLine.Hook -> hooks(drawing)
        RealLine.Outro -> outros(drawing, reveal.motion, reveal.hook.text)
    }

    /**
     * Ten ideas for the line the video opens with, to pick one from. The video opens on [opening] being drawn again
     * on the game's screen with its word above it, so the lines may speak of the word, and of its seconds when they
     * are known.
     */
    suspend fun hooks(opening: RealDrawing): Result<Lines> = runCatching {
        val image = withContext(Dispatchers.Default) { StrokeReplay.png(opening.strokes, 1f, 384) }
        val word = opening.word.ifBlank { "?" }
        val seconds = opening.seconds
        val prompt = """
            Kısa bir TikTok/Reels videosunun ilk 2–3 saniyesi için KANCA yazıyorsun: ekranda büyük harflerle duracak tek bir cümle.
            Video şöyle açılıyor: Karalak çizim oyununun ekranında bir oyuncunun "$word" kelimesi için ${if (seconds > 0) "$seconds saniyede " else ""}yaptığı karalama (ekteki görsel) baştan çiziliyor; kelime ekranın üstünde yazıyor ve kanca çizimin hemen altında duruyor.
            Sonra bu karalamanın gerçek olsaydı nasıl görüneceği 8 saniyelik bir video olarak açılıyor.

            Kancanın tek işi izleyiciyi ilk saniyede durdurup sonuna kadar izletmek: merak uyandırsın, şaşırtsın, meydan okusun ya da tatlı bir iğneleme yapsın.
            Tarz örnekleri (birebir kopyalama):
            - "4 saniyede mikrodalga fırın çizebilir misin?"
            - "5 saniyede elbise çizdi!"
            - "Bu gerçekten bir yetenek!"

            Birbirinden FARKLI $HOOK_COUNT kanca yaz ve tarzları karıştır: izleyiciye meydan okuyan soru, şaşkın bir tespit, ironik bir övgü, "gerçek olsaydı" merakı, izleyiciye doğrudan seslenen bir cümle.
            Kurallar:
            - Türkçe, konuşma dilinde; en fazla 8 kelime. Emoji, etiket, tırnak yok.
            - ${if (seconds > 0) "Süreden söz edersen tam olarak $seconds saniye de, başka bir sayı yazma." else "Süre bilinmiyor: saniyeden ya da süreden hiç söz etme."}
            - Sonu söyleme: gerçek hâlinin neye benzediğini anlatma.
            - Çizeni küçümseme, kimseyle alay etme; şaka çizimle ilgili olsun.

            Yalnızca şu JSON'u yaz:
            {"hooks":["…","…"]}
        """.trimIndent()
        val answer = client.generate(HOOK_SYSTEM, prompt, model, webSearch = false, images = listOf(image), json = true, temperature = 1.0, maxTokens = 900).getOrThrow()
        val hooks = RealCopy.parseHooks(answer.text, seconds)
        if (hooks.isEmpty()) error("Kancalar okunamadı; tekrar dene.")
        Lines(hooks, answer.costUsd)
    }

    /**
     * Five ideas for what Karalak says once the clip is over, on its last frame: a quick reaction to what happened in
     * it ([scene], the video prompt's own) and, mostly, a question that asks for comments. Some tie back to [hook], so
     * that when the video starts over it sounds like one sentence going on.
     */
    suspend fun outros(drawing: RealDrawing, scene: String, hook: String): Result<Lines> = runCatching {
        val image = withContext(Dispatchers.Default) { StrokeReplay.png(drawing.strokes, 1f, 384) }
        val word = drawing.word.ifBlank { "?" }
        val seconds = drawing.seconds
        val happens = scene.trim().ifBlank { "Bu nesneyle ilgili kısa, komik, gerçekçi bir sahne: biri gelip onu kullanıyor ve tepki veriyor" }
        val prompt = """
            Kısa bir TikTok/Reels videosunun SON 2 saniyesinde Karalak'ın söyleyeceği KAPANIŞ cümlesini yazıyorsun.
            Video şöyle: Karalak çizim oyununun ekranında bir oyuncunun "$word" için ${if (seconds > 0) "$seconds saniyede " else ""}yaptığı karalama (ekteki görsel) çiziliyor${if (hook.isNotBlank()) "; ekranda \"${hook.trim()}\" yazıyor ve Karalak bunu söylüyor" else ""}.
            Sonra karalamanın gerçek olsaydı nasıl görüneceği 8 saniyelik, sesli bir videoda gösteriliyor. Videoda olan: $happens.
            Video bitince son kare donuyor ve Karalak kapanış cümlesini söylüyor; sonra video başa dönüyor.

            Kapanışın işi: klipte olana kısa, komik bir tepki verip izleyiciyi yorum yazmaya çağırmak; çoğu bir soruyla bitsin.
            Tarz örnekleri (birebir kopyalama):
            - "Bunu kim mutfağına koyar?"
            - "Ben olsam kullanmazdım… ya sen?"
            - "On üzerinden kaç? Yorumlara yaz!"
            ${if (hook.isNotBlank()) "En az biri videonun başındaki kancaya bağlansın: video başa dönünce cümle devam ediyormuş gibi olsun (ör. kanca \"4 saniyede mikrodalga fırın çizebilir misin?\" ise kapanış \"Sen daha iyisini çizebilir misin?\")." else ""}

            Birbirinden FARKLI $OUTRO_COUNT kapanış yaz.
            Kurallar:
            - Türkçe, konuşma dilinde; en fazla 8 kelime. Emoji, etiket, tırnak yok.
            - ${if (seconds > 0) "Süreden söz edersen tam olarak $seconds saniye de." else "Saniyeden ya da süreden söz etme."}
            - Uygulamayı indirmeye çağırma, site ya da mağaza adı verme.
            - Çizeni küçümseme, kimseyle alay etme; şaka çizimle ve klipte olanla ilgili olsun.

            Yalnızca şu JSON'u yaz:
            {"outros":["…","…"]}
        """.trimIndent()
        val answer = client.generate(OUTRO_SYSTEM, prompt, model, webSearch = false, images = listOf(image), json = true, temperature = 1.0, maxTokens = 600).getOrThrow()
        val outros = RealCopy.parseOutros(answer.text, seconds)
        if (outros.isEmpty()) error("Kapanışlar okunamadı; tekrar dene.")
        Lines(outros, answer.costUsd)
    }

    private companion object {
        const val HOOK_COUNT = RealCopy.HOOK_COUNT
        const val OUTRO_COUNT = RealCopy.OUTRO_COUNT
        val OUTRO_SYSTEM = """
            Sen Karalak'sın: Karalak çizim oyununun maskotu, sevimli bir köpek yavrusu. Esprili, sıcak, biraz muzip.
            Türkçe, konuşma dilinde, çok kısa konuşursun; söylediklerin sesli okunacak. Çizen kişiyi asla küçümsemezsin. Küfür, argo, siyaset yok.
        """.trimIndent()
        val HOOK_SYSTEM = """
            Sen Karalak çizim oyununun sosyal medya editörüsün. Kısa videoların ilk saniyesini yazarsın: kısa, çarpıcı, merak uyandıran tek cümleler.
            Çizen kişiyi asla küçümsemezsin; şakaların çizimle ilgilidir. Küfür, argo, siyaset yok.
        """.trimIndent()
        const val MAX_CANDIDATES = 6
        val SYSTEM = """
            Sen Karalak çizim oyununun sosyal medya editörüsün. Çocuk çizimlerinin "gerçek olsaydı" hâlini gösteren kısa videolar için hazırlık yapıyorsun.
            Çizimleri olduğu gibi, süslemeden ve düzeltmeden anlatırsın. Çizen kişiyi asla küçümsemezsin.
        """.trimIndent()
    }
}
