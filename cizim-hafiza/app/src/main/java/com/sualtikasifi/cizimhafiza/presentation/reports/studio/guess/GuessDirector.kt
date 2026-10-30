package com.sualtikasifi.cizimhafiza.presentation.reports.studio.guess

import com.sualtikasifi.cizimhafiza.presentation.reports.console.AiAnswer
import com.sualtikasifi.cizimhafiza.presentation.reports.console.OpenRouterClient
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.Backoff
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.KaralakVoice
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.LineRules
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StrokeReplay
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioDrawing
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioLang
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MascotEmotion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

/**
 * Writes "Karalak Bilir mi?" with the assistant, in two steps.
 *
 * 1. For each drawing, Karalak is shown the same drawing at three moments (a third, two thirds,
 *    finished) and says what it sees each time, then names one word. It is never told the answer,
 *    so its guesses — the wrong ones included — are really its own. The drawings are looked at a few
 *    at a time, not one after another.
 * 2. Knowing which guesses were right, it writes the opening line, a reaction per drawing and the
 *    closing line.
 */
class GuessDirector(private val client: OpenRouterClient, private val model: String, private val lang: StudioLang = StudioLang.Tr) {

    class Progress(val done: Int, val total: Int)

    @Volatile
    var spentUsd: Double = 0.0
        private set

    @Synchronized
    private fun spend(answer: AiAnswer) {
        answer.costUsd?.let { spentUsd += it }
    }

    /** A finished script and the post text the assistant wrote with it, with other openings and closings to pick from. */
    class Written(
        val script: GuessScript, val caption: String, val hashtags: String,
        val introAlts: List<SpokenLine> = emptyList(), val outroAlts: List<SpokenLine> = emptyList()
    )

    /** One call to the assistant; a busy service (429, 5xx) is waited out and asked again. */
    private suspend fun ask(system: String, prompt: String, images: List<ByteArray> = emptyList(), temperature: Double, maxTokens: Int): AiAnswer =
        Backoff.retrying { client.generate(system, prompt, model, webSearch = false, images = images, json = true, temperature = temperature, maxTokens = maxTokens) }
            .getOrThrow().also { spend(it) }

    suspend fun write(drawings: List<StudioDrawing>, playerName: String, onProgress: (Progress) -> Unit): Result<Written> = runCatching {
        val total = drawings.size + 1
        val done = AtomicInteger(0)
        val gate = Semaphore(PARALLEL)
        onProgress(Progress(0, total))
        // a few drawings at a time, each in its own place in the list
        val rounds = coroutineScope {
            drawings.map { d -> async { gate.withPermit { look(d) }.also { onProgress(Progress(done.incrementAndGet(), total)) } } }.awaitAll()
        }
        val frame = writeFrame(rounds, playerName)
        onProgress(Progress(total, total))
        Written(
            GuessScript(frame.intro, rounds.mapIndexed { i, r -> r.copy(reaction = frame.reactions[i]) }, frame.outro),
            frame.caption, frame.hashtags, frame.introAlts, frame.outroAlts
        )
    }

    /** Karalak's three looks at one drawing, and its guess. A look with a line that runs on is asked for once more, then cut. */
    private suspend fun look(d: StudioDrawing): GuessRound {
        val images = withContext(Dispatchers.Default) { GuessTimeline.STAGE_FRACTIONS.map { f -> StrokeReplay.png(d.strokes, f) } }
        val system = KaralakVoice.persona("Şu an bir oyuncunun çizimini canlı izliyor ve ne çizdiğini tahmin ediyorsun.", lang)
        val note = KaralakVoice.promptNote(lang, "JSON anahtarları, emotion değerleri ve final_guess (Türkçe, oyunun kelime listesindeki gibi kalır)")
        var parsed: Pair<List<SpokenLine>, String>? = null
        var feedback = ""
        for (attempt in 1..ATTEMPTS) {
            val answer = ask(system, LOOK_PROMPT + note + feedback, images, temperature = 0.8, maxTokens = 600)
            val p = GuessCopy.parseLooks(answer.text) ?: continue
            parsed = p
            val long = GuessCopy.longLooks(p.first)
            if (long.isEmpty()) break
            feedback = tooLong(long, "her cümle en fazla ${LineRules.STAGE_WORDS} kelime")
        }
        val (stages, guess) = parsed ?: error("Karalak'ın tahmini okunamadı; tekrar dene.")
        return GuessRound(
            drawingId = d.id,
            word = d.word,
            stages = (GuessCopy.fitLooks(stages) + List(3) { SpokenLine("Hmm…", MascotEmotion.Thinking) }).take(3),
            finalGuess = guess,
            correct = GuessCopy.matches(guess, d.word),
            reaction = SpokenLine("", MascotEmotion.Neutral),
            playerAnswer = d.answer.takeIf { it.isNotBlank() }
        )
    }

    private fun tooLong(lines: List<String>, limit: String): String =
        "\n\nÖnceki cevabındaki şu cümleler çok uzundu: ${lines.joinToString(" | ") { "\"$it\"" }}. Sınır: $limit. Aynı JSON'u kısaltarak yeniden yaz."

    /** Rewrites only the opening, reactions and closing — after a verdict was flipped by hand, say. */
    suspend fun rewriteFrame(script: GuessScript, playerName: String): Result<Written> = runCatching {
        val frame = writeFrame(script.rounds, playerName)
        Written(
            script.copy(intro = frame.intro, rounds = script.rounds.mapIndexed { i, r -> r.copy(reaction = frame.reactions[i]) }, outro = frame.outro),
            frame.caption, frame.hashtags, frame.introAlts, frame.outroAlts
        )
    }

    private suspend fun writeFrame(rounds: List<GuessRound>, playerName: String): FrameCopy {
        val mascot = rounds.count { it.correct }
        val player = rounds.size - mascot
        val list = rounds.mapIndexed { i, r ->
            "${i + 1}. doğru kelime: \"${r.word}\" · Karalak'ın son tahmini: \"${r.finalGuess}\" · ${if (r.correct) "BİLDİ" else "BİLEMEDİ"}"
        }.joinToString("\n")
        val prompt = """
            Oyuncunun adı: $playerName
            Çizim sayısı: ${rounds.size}
            $list
            Sonuç: Karalak $mascot – $player $playerName

            Şu JSON'u yaz (başka hiçbir şey yazma):
            {"intro":{"emotion":"…","line":"…"},"reactions":[{"emotion":"…","line":"…"} (her çizim için bir tane, sırayla)],"outro":{"emotion":"…","line":"…"},"intro_alts":["…","…","…"],"outro_alts":["…","…","…"],"caption":"…","hashtags":"…"}

            intro: videonun ilk 2 saniyesi. İddialı, merak uyandıran tek cümle; en fazla 12 kelime; oyuncunun adını kullan; kelimeleri ASLA söyleme. Örnek ton: "$playerName bana ${rounds.size} çizim gönderdi. Hepsini bilirsem o kaybeder!"
            reactions: her çizimin sonucuna kısa tepki, en fazla 10 kelime. Bildiyse gururlu ya da şımarık; bilemediyse doğru kelimeyi söyleyip utanarak ya da şaşırarak.
            outro: sonucu söyle ve izleyiciye meydan oku; en fazla 14 kelime; "karalak.com" geçsin. Örnek ton: "Beni yenebileceğini mi sanıyorsun? karalak.com'da çiz, ben tahmin edeyim!"
            intro_alts: introya üç farklı alternatif, yalnızca cümle (biri meydan okuyan, biri esprili, biri iddialı); intro'nun kurallarının aynısı.
            outro_alts: outro'ya üç farklı alternatif, yalnızca cümle; outro'nun kurallarının aynısı.
            caption: TikTok/Reels açıklaması; 1–2 doğal cümle, izleyiciyi yoruma çağıran bir soruyla bitsin; kelimeleri söyleme.
            hashtags: 5–6 etiket, # ile, Türkçe ağırlıklı, aralarında boşluk.
            emotion yalnızca şunlardan biri: ${EMOTIONS}.
        """.trimIndent()
        val system = KaralakVoice.persona("Şimdi kısa bir sosyal medya videosunun cümlelerini yazıyorsun; sesli okunacaklar, o yüzden doğal ve akıcı olsunlar.", lang)
        val note = KaralakVoice.promptNote(lang)
        var frame: FrameCopy? = null
        var feedback = ""
        for (attempt in 1..ATTEMPTS) {
            val answer = ask(system, prompt + note + feedback, temperature = 0.9, maxTokens = 1400)
            val f = GuessCopy.parseFrame(answer.text, rounds.size) ?: continue
            frame = f
            val long = GuessCopy.longFrame(f)
            if (long.isEmpty()) break
            feedback = tooLong(long, "intro en fazla ${LineRules.INTRO_WORDS}, tepkiler ${LineRules.REACTION_WORDS}, outro ${LineRules.OUTRO_WORDS} kelime")
        }
        return GuessCopy.fitFrame(frame ?: error("Açılış ve kapanış cümleleri okunamadı; tekrar dene."))
    }

    private companion object {
        val EMOTIONS = MascotEmotion.entries.joinToString(", ") { it.key }

        /** Drawings looked at at once: enough to cut the wait, few enough not to be turned away for asking too fast. */
        const val PARALLEL = 3

        /** A line that runs on is asked for again once; after that it is cut. */
        const val ATTEMPTS = 2

        val LOOK_PROMPT = """
            Sana aynı çizimin üç anını gösteriyorum, sırayla: 1) yaklaşık üçte biri çizilmişken, 2) üçte ikisi çizilmişken, 3) bitmiş hâli.
            Her an için o ana kadar gördüğüne göre aklından geçeni söyle; en fazla 9 kelime. İlk anlarda yanlış tahmin etmen çok normal, çizim tamamlandıkça fikrini değiştirebilirsin.
            En sonda tek kelimelik son tahminini ver. Kelime oyundaki Türkçe kelime listesinden: hayvan, eşya, meslek, spor, doğa, yiyecek, taşıt ya da duygu olabilir.
            Yalnızca şu JSON'u yaz:
            {"stages":[{"emotion":"…","line":"…"},{"emotion":"…","line":"…"},{"emotion":"…","line":"…"}],"final_guess":"…"}
            emotion yalnızca şunlardan biri: neutral, thinking, suspicious, excited, confident, shocked, embarrassed, laughing, point, shrug.
        """.trimIndent()
    }
}
