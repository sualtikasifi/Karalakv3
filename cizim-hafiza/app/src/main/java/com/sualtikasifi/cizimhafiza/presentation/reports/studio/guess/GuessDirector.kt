package com.sualtikasifi.cizimhafiza.presentation.reports.studio.guess

import com.sualtikasifi.cizimhafiza.presentation.reports.console.OpenRouterClient
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StrokeReplay
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioDrawing
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MascotEmotion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Writes "Karalak Bilir mi?" with the assistant, in two steps.
 *
 * 1. For each drawing, Karalak is shown the same drawing at three moments (a third, two thirds,
 *    finished) and says what it sees each time, then names one word. It is never told the answer,
 *    so its guesses — the wrong ones included — are really its own.
 * 2. Knowing which guesses were right, it writes the opening line, a reaction per drawing and the
 *    closing line.
 */
class GuessDirector(private val client: OpenRouterClient, private val model: String) {

    class Progress(val done: Int, val total: Int)

    var spentUsd: Double = 0.0
        private set

    /** A finished script and the post text the assistant wrote with it. */
    class Written(val script: GuessScript, val caption: String, val hashtags: String)

    suspend fun write(drawings: List<StudioDrawing>, playerName: String, onProgress: (Progress) -> Unit): Result<Written> = runCatching {
        val total = drawings.size + 1
        val rounds = drawings.mapIndexed { i, d ->
            onProgress(Progress(i, total))
            val images = withContext(Dispatchers.Default) { GuessTimeline.STAGE_FRACTIONS.map { f -> StrokeReplay.png(d.strokes, f) } }
            val answer = client.generate(LOOK_SYSTEM, LOOK_PROMPT, model, webSearch = false, images = images, json = true, temperature = 0.8, maxTokens = 600)
                .getOrThrow()
            answer.costUsd?.let { spentUsd += it }
            val (stages, guess) = GuessCopy.parseLooks(answer.text) ?: error("Karalak'ın tahmini okunamadı; tekrar dene.")
            GuessRound(
                drawingId = d.id,
                word = d.word,
                stages = (stages + List(3) { SpokenLine("Hmm…", MascotEmotion.Thinking) }).take(3),
                finalGuess = guess,
                correct = GuessCopy.matches(guess, d.word),
                reaction = SpokenLine("", MascotEmotion.Neutral),
                playerAnswer = d.answer.takeIf { it.isNotBlank() }
            )
        }
        onProgress(Progress(drawings.size, total))
        val frame = writeFrame(rounds, playerName)
        onProgress(Progress(total, total))
        Written(GuessScript(frame.intro, rounds.mapIndexed { i, r -> r.copy(reaction = frame.reactions[i]) }, frame.outro), frame.caption, frame.hashtags)
    }

    /** Rewrites only the opening, reactions and closing — after a verdict was flipped by hand, say. */
    suspend fun rewriteFrame(script: GuessScript, playerName: String): Result<Written> = runCatching {
        val frame = writeFrame(script.rounds, playerName)
        Written(
            script.copy(intro = frame.intro, rounds = script.rounds.mapIndexed { i, r -> r.copy(reaction = frame.reactions[i]) }, outro = frame.outro),
            frame.caption, frame.hashtags
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
            {"intro":{"emotion":"…","line":"…"},"reactions":[{"emotion":"…","line":"…"} (her çizim için bir tane, sırayla)],"outro":{"emotion":"…","line":"…"},"caption":"…","hashtags":"…"}

            intro: videonun ilk 2 saniyesi. İddialı, merak uyandıran tek cümle; en fazla 12 kelime; oyuncunun adını kullan; kelimeleri ASLA söyleme. Örnek ton: "$playerName bana ${rounds.size} çizim gönderdi. Hepsini bilirsem o kaybeder!"
            reactions: her çizimin sonucuna kısa tepki, en fazla 10 kelime. Bildiyse gururlu ya da şımarık; bilemediyse doğru kelimeyi söyleyip utanarak ya da şaşırarak.
            outro: sonucu söyle ve izleyiciye meydan oku; en fazla 14 kelime; "karalak.com" geçsin. Örnek ton: "Beni yenebileceğini mi sanıyorsun? karalak.com'da çiz, ben tahmin edeyim!"
            caption: TikTok/Reels açıklaması; 1–2 doğal cümle, izleyiciyi yoruma çağıran bir soruyla bitsin; kelimeleri söyleme.
            hashtags: 5–6 etiket, # ile, Türkçe ağırlıklı, aralarında boşluk.
            emotion yalnızca şunlardan biri: ${EMOTIONS}.
        """.trimIndent()
        val answer = client.generate(FRAME_SYSTEM, prompt, model, webSearch = false, json = true, temperature = 0.9, maxTokens = 1000).getOrThrow()
        answer.costUsd?.let { spentUsd += it }
        return GuessCopy.parseFrame(answer.text, rounds.size) ?: error("Açılış ve kapanış cümleleri okunamadı; tekrar dene.")
    }

    private companion object {
        val EMOTIONS = MascotEmotion.entries.joinToString(", ") { it.key }

        val PERSONA = """
            Sen Karalak'sın: Karalak çizim oyununun maskotu, sevimli bir köpek yavrusu. Kendinden fazla emin, çabuk heyecanlanır, yanılınca utanır ama hemen toparlanır.
            Türkçe, konuşma dilinde, kısa ve esprili konuşursun. Şakaların çizimle ilgilidir; oyuncuyla asla dalga geçmez, kimseyi aşağılamazsın. Küfür, argo, siyaset yok.
        """.trimIndent()

        val LOOK_SYSTEM = PERSONA + "\nŞu an bir oyuncunun çizimini canlı izliyor ve ne çizdiğini tahmin ediyorsun."

        val FRAME_SYSTEM = PERSONA + "\nŞimdi kısa bir sosyal medya videosunun cümlelerini yazıyorsun; sesli okunacaklar, o yüzden doğal ve akıcı olsunlar."

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
