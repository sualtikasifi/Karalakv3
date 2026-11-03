package com.sualtikasifi.cizimhafiza.presentation.reports.studio.real

import com.sualtikasifi.cizimhafiza.presentation.reports.console.OpenRouterClient
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.Backoff
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.KaralakVoice
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.LineRules
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StrokeReplay
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioLang
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Prepares "Gerçek Olsaydı" with the assistant: it looks at the chosen drawings and, for each, writes what
 * the picture and video prompts need (the drawing's shapes as drawn, a detailed little scene for the video), and a post text for the whole video.
 */
class RealDirector(private val client: OpenRouterClient, private val model: String, private val lang: StudioLang = StudioLang.Tr) {

    class Written(val reveals: List<RealReveal>, val caption: String, val hashtags: String, val costUsd: Double?, val model: String? = null)

    suspend fun describe(candidates: List<RealDrawing>): Result<Written> = runCatching {
        val shown = candidates.take(MAX_CANDIDATES)
        require(shown.isNotEmpty()) { "Önce çizim seç" }
        val images = withContext(Dispatchers.Default) { shown.map { StrokeReplay.png(it.strokes, 1f, 768) } }
        val list = shown.mapIndexed { i, d -> "${i + 1}) ${d.word.ifBlank { "?" }}" + if (d.seconds > 0) " (${d.seconds} sn)" else "" }.joinToString("\n")
        val prompt = """
            Sana oyuncuların farklı kelimeler için birkaç saniyede yaptığı ${shown.size} çizimi gösteriyorum, sırayla 1'den ${shown.size}'e kadar numaralı. Çizimlerin kelimeleri:
            $list

            Her çizim için ("reveals"), çizim gerçek bir fotoğrafa ve sonra kısa bir videoya dönüşecek. Her şeyi TÜRKÇE yaz:
            - "kind": çizimin kelimesi hangisi: "nesne" (şırınga, mikrodalga, saz gibi cansız şeyler), "canlı" (hayvan, insan, yaratık), "yiyecek" (nar, pizza, çay gibi yenen ya da içilen) ya da "bitki" (ağaç, çiçek). Yalnızca bu dört sözcükten birini yaz. Nesnenin, yiyeceğin ve bitkinin gözü, yüzü, ağzı, kolu, bacağı OLMAZ; bunlara canlı gibi davranma;
            - "parts": çizimin her ana parçasını kelimenin GERÇEK bir parçasına bağlayan 3–6 kısa satır, "çizilen parça → gerçek parça" biçiminde (ör. şırınga için: "uzun dikey dikdörtgen gövde → şeffaf plastik silindir", "üstten çıkan ince düz çizgi → pistonun ince kolu", "alttaki yatay T → piston başı"). Çizimde olmayan parçayı yazma;
            - "material": o şeyin gerçekte yapıldığı malzemeler, tek kısa cümle (ör. "şeffaf plastik silindir, siyah kauçuk piston, ince metal uç"; canlıysa tüy, deri, pul ya da kabuk türü);
            - "size": o şeyin GERÇEK boyutu, kısa ve somut (ör. "yaklaşık 15 cm, avuca sığar"; mikrodalga için "tezgâha sığan, yaklaşık 45 cm genişlikte"). "scale_hint": fotoğrafta yanında durup boyutunu belli edecek küçük, tanıdık bir şey (ör. "yanında bir tükenmez kalem");
            - "fixed": bu şeyin ASLA değişmeyen özellikleri, tek cümle (ör. "sert plastik ve metal: bükülmez, esnemez, kendiliğinden hareket etmez"). Canlıysa gerçek bedeni çizimdeki beden olsun;
            - "scene": fotoğrafın çekildiği TEK, gerçek, sıradan mekân ve yüzey, tek kısa cümle (ör. "bir muayene masasının paslanmaz çelik yüzeyinin üstü"; hayvansa doğal yaşam alanı). Çizime ya da kelimeye uygun, abartısız olsun;
            - "describe": çizimin şeklini bir plan gibi, gördüğün çizgilerle birebir anlatan kısa cümleler: gövdenin şekli, kaç bacak ve neye benzedikleri, baş, kuyruk, özel işaretler (ör. "uzun düz dikdörtgen bir gövde, üç parmaklı ayakları olan dört düz çubuk bacak, iki üçgen kulaklı küçük yuvarlak bir baş, üç halkalı bir spiral olarak çizilmiş kuyruk"). Ne olması gerektiğini değil, ne çizildiğini yaz — gerçeğe dönüştürülürken bu şekil korunacak;
            - "motion": videonun İLK TASLAK sahnesi (asıl sahne, gerçek fotoğraf çıkınca ayrıca yazdırılacak): 8 saniyelik, TEK ÇEKİM, TEK mekân, en fazla BİR kişi, 3-4 kısa cümle. Nesne şekil ya da malzeme değiştirmez, kendiliğinden hareket etmez; hareketi yalnızca kişi yapar. Kapı açılması, yeni oda, ikinci kişi yok. Komik an, kişinin nesneye ciddi ve sakin tepkisinden ya da nesnenin gerçek kullanımından gelsin. Duyulacak bir ses ekle.
            Ayrıca üç videoyu birlikte anlatan TikTok/Reels açıklaması (1–2 cümle, soruyla bitsin) ve 5–6 Türkçe ağırlıklı etiket yaz.

            Yalnızca şu JSON'u yaz:
            {"reveals":[{"index":1,"kind":"…","describe":"…","parts":["…","…"],"material":"…","size":"…","scale_hint":"…","fixed":"…","scene":"…","motion":"…"}],"caption":"…","hashtags":"…"}
        """.trimIndent()
        // the picture and video prompts stay Turkish (they are pasted into ChatGPT and Gemini and corrected by hand); only the post text follows the language
        val note = if (lang == StudioLang.Tr) "" else "\n\nÖNEMLİ: \"caption\" ve \"hashtags\" alanlarını ${lang.named} yaz; \"describe\" ve \"motion\" Türkçe kalsın."
        val answer = Backoff.retrying { client.generate(SYSTEM, prompt + note, model, webSearch = false, images = images, json = true, temperature = 0.7, maxTokens = 4000) }.getOrThrow()
        val parsed = RealCopy.parsePicks(answer.text, shown.size) ?: error("Yanıt okunamadı; tekrar dene.")
        Written(
            reveals = parsed.reveals.map { r -> RealReveal(sampleId = shown[r.index].id, describe = r.describe, motion = r.motion, kind = r.kind, parts = r.parts, material = r.material, scene = r.scene) },
            caption = parsed.caption,
            hashtags = parsed.hashtags,
            costUsd = answer.costUsd,
            model = answer.model
        )
    }

    class Pitches(val pitches: List<ScenePitch>, val costUsd: Double?, val model: String?)

    /**
     * Three ideas for the video scene, written with the real picture in front of the assistant ([image], PNG bytes): see
     * [RealCopy.pitchPrompt] for the rules (one shot, one place, one person, the thing never changes shape, its real size).
     */
    suspend fun pitches(word: String, reveal: RealReveal, image: ByteArray): Result<Pitches> = runCatching {
        val answer = Backoff.retrying {
            client.generate(SYSTEM, RealCopy.pitchPrompt(word, reveal), model, webSearch = false, images = listOf(image), json = true, temperature = 1.0, maxTokens = 3500)
        }.getOrThrow()
        val ideas = RealCopy.parsePitches(answer.text) ?: error("Yanıt okunamadı; tekrar dene.")
        Pitches(ideas, answer.costUsd, answer.model)
    }

    class Lines(val lines: List<String>, val costUsd: Double?)

    /** Ideas for one of Karalak's lines in the video of [drawing] and its [reveal]: the hook, or the closing reaction. */
    suspend fun lines(kind: RealLine, drawing: RealDrawing, reveal: RealReveal): Result<Lines> = when (kind) {
        RealLine.Hook -> hooks(drawing, reveal)
        RealLine.Narration -> narrations(drawing, reveal)
        RealLine.Outro -> outros(drawing, reveal, reveal.motion, reveal.hook.text)
        // the end card's lines are ready-made, not written by the assistant
        RealLine.End -> Result.success(Lines(RealCopy.END_LINES, null))
    }

    /**
     * What the three spoken lines of a video are written from: the drawing's word and seconds, and, when the picture and
     * the notes about it exist, what it will turn into and what happens in the clip. The hook may be informed of all of it
     * but never gives the ending away.
     */
    private fun facts(drawing: RealDrawing, reveal: RealReveal): String = buildString {
        append("- Kelime: \"").append(drawing.word.ifBlank { "?" }).append("\"\n")
        if (drawing.seconds > 0) append("- Çizim süresi: ").append(drawing.seconds).append(" saniye\n")
        if (reveal.kind.isNotBlank()) append("- Türü: ").append(reveal.kind).append('\n')
        if (reveal.describe.isNotBlank()) append("- Çizimin tuhaf şekli: ").append(reveal.describe).append('\n')
        if (reveal.size.isNotBlank()) append("- Gerçek boyutu: ").append(reveal.size).append('\n')
        if (reveal.scene.isNotBlank()) append("- Gerçek hâlinin yeri: ").append(reveal.scene).append('\n')
        if (reveal.motion.isNotBlank()) append("- Klipte olan: ").append(reveal.motion).append('\n')
    }

    /** What every spoken line is judged by: a professional's bar, and what makes a line sound written by a machine. */
    private val CRAFT = """
        Bir metin yazarı gibi çalış: önce çok aday yaz, sonra yalnızca en iyilerini bırak. Seçerken şu testi uygula: "Bunu kaydırırken durup bir kez daha bakar mıyım?"
        İyi satırın özellikleri:
        - Somut: kelimeye ve çizimin tuhaf özelliğine (kaç bacak, hangi şekil, ne büyüklük) dair bir ayrıntı taşır; her çizime uyabilecek genel cümle yazma.
        - Cevapsız bir soru ya da tamamlanmamış bir tespit bırakır; izleyici cevabı görmek için kalır. Ama cevabı vermez.
        - Bir bakış açısı vardır: etiket koymaz, hüküm verir, şüphelenir, ciddiyetle yanlış bir şey iddia eder ya da bir iddiada bulunur.
        - İnsan konuşması gibi akar: kısa cümleler, doğal ritim, günlük söyleyiş; yazı dili ya da reklam dili değil.
        Yapay zeka gibi durmasının ve amatörce görünmesinin işaretleri; bunlardan KAÇIN:
        - "inanılmaz", "muhteşem", "harika", "şok", "efsane", "bunu görmelisin", "hazır mısın", "şaşıracaksın", "sen de dene", "bakalım", "işte", "gerçekten", "kesinlikle"
        - Ünlem işareti yığmak, her cümleyi soruyla bitirmek, "Gerçek olsaydı nasıl olurdu?" gibi bilinen kalıplar, "bu çizim" diye başlamak
        - Her aday aynı yapıda olmasın: soru, hüküm, itiraf, şüphe, mini hikâye, rakam, karşılaştırma gibi farklı mekanizmaları karıştır.
    """.trimIndent()

    /**
     * Ten hooks, to pick one from: the line over the opening, in which the drawing is drawn again on the game's screen.
     * Written like a copywriter would: many candidates first, the best ten kept.
     */
    suspend fun hooks(opening: RealDrawing, reveal: RealReveal = RealReveal(opening.id)): Result<Lines> = runCatching {
        val image = withContext(Dispatchers.Default) { StrokeReplay.png(opening.strokes, 1f, 512) }
        val seconds = opening.seconds
        val prompt = """
            Kısa bir TikTok/Reels videosunun ilk 2–3 saniyesi için KANCA yazıyorsun: ekranda duran ve seslendirilen tek bir cümle.
            Video şöyle açılıyor: bir oyuncunun çizim oyununda birkaç saniyede yaptığı karalama (ekteki görsel) oyun ekranında baştan çiziliyor; sonra aynı karalama gerçek bir fotoğrafa ve 8 saniyelik bir videoya dönüşüyor. İzleyici çizimin neye dönüştüğünü kancadan sonra görecek.
            Bilinenler:
            ${facts(opening, reveal)}
            Kancanın tek işi: ilk saniyede durdurmak ve videoyu sonuna kadar izletmek. İzleyici çizimin şekline bakıp kancayla beraber bir şey merak etmeli.

            $CRAFT

            Önce "candidates" listesine 16 farklı aday yaz (her biri tek kısa cümle), sonra yukarıdaki testi ve kuralları en iyi geçen $HOOK_COUNT tanesini "hooks" listesine yaz; en iyisi başta olsun.
            Kurallar:
            - Türkçe, konuşma dilinde; en fazla 8 kelime. Emoji, etiket, tırnak yok.
            - ${if (seconds > 0) "Süreden söz edersen tam olarak $seconds saniye de, başka bir sayı yazma. Süre güçlü bir malzeme: kısa süreyi tuhaf sonuçla yan yana koyabilirsin." else "Süre bilinmiyor: saniyeden ya da süreden hiç söz etme."}
            - Sonu söyleme: gerçek hâlinin neye benzediğini anlatma.
            - Çizeni küçümseme, kimseyle alay etme; komiklik çizimin kendi tuhaflığından gelsin.

            Yalnızca şu JSON'u yaz:
            {"candidates":["…","…"],"hooks":["…","…"]}
        """.trimIndent()
        val answer = Backoff.retrying {
            client.generate(WRITER, prompt + KaralakVoice.promptNote(lang, "JSON anahtarları"), model, webSearch = false, images = listOf(image), json = true, temperature = 1.0, maxTokens = 2500)
        }.getOrThrow()
        val hooks = RealCopy.parseHooks(answer.text, seconds)
        if (hooks.isEmpty()) error("Kancalar okunamadı; tekrar dene.")
        Lines(hooks, answer.costUsd)
    }

    /**
     * Five ideas for the narrator over the clip, as a deadpan nature-documentary voice: the funny is in how seriously
     * the odd thing is explained, so the comedy is written here and not left to the video model.
     */
    suspend fun narrations(drawing: RealDrawing, reveal: RealReveal): Result<Lines> = runCatching {
        val image = withContext(Dispatchers.Default) { StrokeReplay.png(drawing.strokes, 1f, 512) }
        val prompt = """
            Kısa bir TikTok/Reels videosunda, gerçek görünümlü 8 saniyelik klibin üstünde duyulacak ANLATICI cümlesini yazıyorsun.
            Klip, bir çocuğun çiziminin gerçek hâli (ekteki görsel çizimin kendisi). Bilinenler:
            ${facts(drawing, reveal)}
            Anlatıcı bir doğa belgeseli spikeri gibi konuşur: sakin, otoriter, kuru, tamamen ciddi. Komiklik, tuhaf şeyi hiç şaşırmadan, uzman bir ciddiyetle anlatmasından gelir; kendisi komik olmaya çalışmaz, espri yapmaz, ünlem kullanmaz.
            Anlatıcının işi tek: klipte GÖRÜNEN tuhaf ayrıntıyı (şekil, duruş, davranış) belgesel diliyle ciddiyetle yorumlamak.

            $CRAFT
            Anlatıcıya özel:
            - Tek bir kuru, somut ayrıntı en iyisidir: sayı, ölçü, kesin bir iddia ya da bilinmezlik ("kimse bilmiyor", "hâlâ çözülemedi"); büyük laf etme.
            - "Bilim insanları…" kalıbını en fazla bir adayda kullan; diğerlerinde farklı bir yapı kur (gözlem, saha notu, kayıt, ölçüm, sessiz bir hüküm).
            - Klipte olmayan bir şey uydurma.

            Önce "candidates" listesine 12 farklı aday yaz, sonra en iyi $NARRATION_COUNT tanesini "narrations" listesine yaz; en iyisi başta olsun.
            Kurallar:
            - Türkçe, konuşma dilinde; en fazla ${LineRules.NARRATION_WORDS} kelime (yaklaşık 4 saniye). Bir ya da iki kısa cümle. Emoji, etiket, tırnak, ünlem yok.
            - Nesneyi ya da hayvanı gerçekmiş gibi anlat; "çizim", "çocuk" ya da "oyun" deme.
            - Kimseyle alay etme; ciddiyetini hiç bozma.

            Yalnızca şu JSON'u yaz:
            {"candidates":["…","…"],"narrations":["…","…"]}
        """.trimIndent()
        val answer = Backoff.retrying {
            client.generate(WRITER, prompt + KaralakVoice.promptNote(lang, "JSON anahtarları"), model, webSearch = false, images = listOf(image), json = true, temperature = 1.0, maxTokens = 2500)
        }.getOrThrow()
        val lines = RealCopy.parseNarrations(answer.text, 0)
        if (lines.isEmpty()) error("Anlatıcı cümleleri okunamadı; tekrar dene.")
        Lines(lines, answer.costUsd)
    }

    /**
     * Five ideas for the closing, said over the clip's last seconds: a real reaction to what happened, and an invitation
     * to argue in the comments, because a divided audience comments.
     */
    suspend fun outros(drawing: RealDrawing, reveal: RealReveal, scene: String, hook: String): Result<Lines> = runCatching {
        val image = withContext(Dispatchers.Default) { StrokeReplay.png(drawing.strokes, 1f, 512) }
        val seconds = drawing.seconds
        val happens = scene.trim().ifBlank { "Çizimin gerçek hâli sakin, gerçekçi bir mekânda duruyor ve kısa, komik bir an yaşanıyor" }
        val prompt = """
            Kısa bir TikTok/Reels videosunun SON 2 saniyesinde söylenecek KAPANIŞ cümlesini yazıyorsun.
            Video: oyunda birkaç saniyede yapılmış bir karalama (ekteki görsel) çiziliyor${if (hook.isNotBlank()) "; ekranda ve sesli olarak \"${hook.trim()}\" söyleniyor" else ""}; sonra karalamanın gerçek hâli 8 saniyelik bir videoda gösteriliyor. Videoda olan: $happens.
            Video bitince son kare donuyor, bu cümle söyleniyor, sonra video başa dönüyor. Bilinenler:
            ${facts(drawing, reveal)}
            Kapanışın işi: izleyiciyi YORUM yazmaya itmek. Bunun en iyi yolu, izleyiciyi iki tarafa bölen ya da bir şeyi tartışmaya açan bir cümledir (bir tercih, bir puan, bir iddia, "sen olsan" sorusu). Gerçek bir insan tepkisi gibi dur: önce kısa bir tepki ya da hüküm, sonra isteğe göre soru.

            $CRAFT
            Kapanışa özel:
            - "Yorumlara yaz" demeden yorum yazdır: iki seçenek ya da bir iddia ver ("Bunu yer miydin yoksa saklar mıydın?", "Bence bu en az 7. Sence?").
            ${if (hook.isNotBlank()) "- En az biri videonun başındaki kancaya bağlansın; video başa dönünce cümle devam ediyormuş gibi olsun." else ""}

            Önce "candidates" listesine 10 farklı aday yaz, sonra en iyi $OUTRO_COUNT tanesini "outros" listesine yaz; en iyisi başta olsun.
            Kurallar:
            - Türkçe, konuşma dilinde; en fazla 8 kelime. Emoji, etiket, tırnak yok.
            - ${if (seconds > 0) "Süreden söz edersen tam olarak $seconds saniye de." else "Saniyeden ya da süreden söz etme."}
            - Uygulamayı indirmeye çağırma, site ya da mağaza adı verme.
            - Çizeni küçümseme, kimseyle alay etme.

            Yalnızca şu JSON'u yaz:
            {"candidates":["…","…"],"outros":["…","…"]}
        """.trimIndent()
        val answer = Backoff.retrying {
            client.generate(WRITER, prompt + KaralakVoice.promptNote(lang, "JSON anahtarları"), model, webSearch = false, images = listOf(image), json = true, temperature = 1.0, maxTokens = 2000)
        }.getOrThrow()
        val outros = RealCopy.parseOutros(answer.text, seconds)
        if (outros.isEmpty()) error("Kapanışlar okunamadı; tekrar dene.")
        Lines(outros, answer.costUsd)
    }

    private companion object {
        /**
         * Who writes the three spoken lines: a professional short-video copywriter, not the mascot. Karalak only says them
         * (in the voice that is set); a cute persona made them sound like a chatbot trying to be fun.
         */
        const val WRITER = "Sen deneyimli bir kısa video metin yazarısın: TikTok ve Reels'te milyonlarca izlenen, doğal konuşma diliyle yazan, klişeden ve yapay zeka kokusundan nefret eden biri. Yazdığın cümleler sesli okunacak; bir insan yazmış gibi durmalı, hiçbir zaman reklam ya da yapay zeka metni gibi değil. Çizen kişiyi asla küçümsemezsin."
        const val NARRATION_COUNT = RealCopy.NARRATION_COUNT

        const val HOOK_COUNT = RealCopy.HOOK_COUNT
        const val OUTRO_COUNT = RealCopy.OUTRO_COUNT
        const val MAX_CANDIDATES = 6
        val SYSTEM = """
            Sen Karalak çizim oyununun sosyal medya editörüsün. Çocuk çizimlerinin "gerçek olsaydı" hâlini gösteren kısa videolar için hazırlık yapıyorsun.
            Çizimleri olduğu gibi, süslemeden ve düzeltmeden anlatırsın. Çizen kişiyi asla küçümsemezsin.
        """.trimIndent()
    }
}
