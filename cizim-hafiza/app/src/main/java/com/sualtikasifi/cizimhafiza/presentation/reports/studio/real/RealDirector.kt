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
        RealLine.Hook -> hooks(drawing)
        RealLine.Narration -> narrations(drawing, reveal)
        RealLine.Outro -> outros(drawing, reveal.motion, reveal.hook.text)
        // the end card's lines are ready-made, not written by the assistant
        RealLine.End -> Result.success(Lines(RealCopy.END_LINES, null))
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
        val system = KaralakVoice.persona(HOOK_JOB, lang)
        val answer = Backoff.retrying {
            client.generate(system, prompt + KaralakVoice.promptNote(lang, "JSON anahtarı"), model, webSearch = false, images = listOf(image), json = true, temperature = 1.0, maxTokens = 900)
        }.getOrThrow()
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
        val system = KaralakVoice.persona(OUTRO_JOB, lang)
        val answer = Backoff.retrying {
            client.generate(system, prompt + KaralakVoice.promptNote(lang, "JSON anahtarı"), model, webSearch = false, images = listOf(image), json = true, temperature = 1.0, maxTokens = 600)
        }.getOrThrow()
        val outros = RealCopy.parseOutros(answer.text, seconds)
        if (outros.isEmpty()) error("Kapanışlar okunamadı; tekrar dene.")
        Lines(outros, answer.costUsd)
    }

    /**
     * Five ideas for what Karalak says over the clip, as a deadpan nature-documentary narrator: the funny is in how
     * seriously the odd thing is explained, so the comedy is written here and not left to the video model.
     */
    suspend fun narrations(drawing: RealDrawing, reveal: RealReveal): Result<Lines> = runCatching {
        val image = withContext(Dispatchers.Default) { StrokeReplay.png(drawing.strokes, 1f, 384) }
        val word = drawing.word.ifBlank { "?" }
        val facts = buildString {
            if (reveal.kind.isNotBlank()) append("- Türü: ").append(reveal.kind).append('\n')
            if (reveal.describe.isNotBlank()) append("- Tuhaf şekli: ").append(reveal.describe).append('\n')
            if (reveal.size.isNotBlank()) append("- Gerçek boyutu: ").append(reveal.size).append('\n')
            if (reveal.scene.isNotBlank()) append("- Yer: ").append(reveal.scene).append('\n')
            if (reveal.motion.isNotBlank()) append("- Klipte olan: ").append(reveal.motion).append('\n')
        }
        val prompt = """
            Kısa bir TikTok/Reels videosunda, gerçek görünümlü 8 saniyelik klibin üstünde duyulacak ANLATICI cümlesini yazıyorsun.
            Klip, bir çocuğun "$word" çiziminin gerçek hâli (ekteki görsel çizimin kendisi). Bilinenler:
            $facts
            Anlatıcı ciddi bir doğa belgeseli spikeri gibi konuşur: sakin, otoriter, tamamen ciddi. Şaka yapmaya çalışmaz; komiklik, tuhaf olanı ciddiyetle anlatmasından gelir.
            Tarz örnekleri (birebir kopyalama):
            - "Bilim insanları bu canlıyı doğal ortamında ilk kez görüntüledi."
            - "Dikkat edin: nasıl yürüdüğü hâlâ çözülmüş değil."
            - "Doğa bazen kimsenin beklemediği şeyleri üretir."
            - "Uzmanlar bu şeklin neden hayatta kaldığını açıklayamıyor."

            Birbirinden FARKLI $NARRATION_COUNT anlatıcı cümlesi yaz.
            Kurallar:
            - Türkçe, konuşma dilinde; en fazla ${LineRules.NARRATION_WORDS} kelime (yaklaşık 4 saniyede okunur). Bir ya da iki kısa cümle. Emoji, etiket, tırnak, ünlem yok.
            - Nesneyi ya da hayvanı gerçekmiş gibi anlat; "çizim", "çocuk" ya da "oyun" deme.
            - Klipte görünen ve tuhaf olan şeye (şekil, duruş, davranış) değin; klipte olmayan bir şey uydurma.
            - Kimseyle alay etme; ciddiyetini hiç bozma.

            Yalnızca şu JSON'u yaz:
            {"narrations":["…","…"]}
        """.trimIndent()
        val system = KaralakVoice.persona(NARRATION_JOB, lang)
        val answer = Backoff.retrying {
            client.generate(system, prompt + KaralakVoice.promptNote(lang, "JSON anahtarı"), model, webSearch = false, images = listOf(image), json = true, temperature = 1.0, maxTokens = 700)
        }.getOrThrow()
        val lines = RealCopy.parseNarrations(answer.text, 0)
        if (lines.isEmpty()) error("Anlatıcı cümleleri okunamadı; tekrar dene.")
        Lines(lines, answer.costUsd)
    }

    private companion object {
        const val NARRATION_COUNT = RealCopy.NARRATION_COUNT
        const val NARRATION_JOB = "Kısa videolarda ciddi bir belgesel anlatıcısı gibi konuşuyorsun: sakin, otoriter, asla gülmeye çalışmayan; tuhaf olanı tamamen ciddiyetle anlatan tek cümleler yazarsın."

        const val HOOK_COUNT = RealCopy.HOOK_COUNT
        const val OUTRO_COUNT = RealCopy.OUTRO_COUNT
        const val OUTRO_JOB = "Kısa bir videonun sonunda söyleyeceğin tek cümleyi yazıyorsun; çok kısa konuşursun. Çizen kişiyi asla küçümsemezsin."
        const val HOOK_JOB = "Kısa videoların ilk saniyesini yazıyorsun: kısa, çarpıcı, merak uyandıran tek cümleler. Çizen kişiyi asla küçümsemezsin."
        const val MAX_CANDIDATES = 6
        val SYSTEM = """
            Sen Karalak çizim oyununun sosyal medya editörüsün. Çocuk çizimlerinin "gerçek olsaydı" hâlini gösteren kısa videolar için hazırlık yapıyorsun.
            Çizimleri olduğu gibi, süslemeden ve düzeltmeden anlatırsın. Çizen kişiyi asla küçümsemezsin.
        """.trimIndent()
    }
}
