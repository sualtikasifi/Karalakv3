package com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot

import com.sualtikasifi.cizimhafiza.presentation.reports.console.OpenRouterClient
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.Backoff
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.KaralakVoice
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.LineRules
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StrokeReplay
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioDrawing
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioLang
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.SpokenLine
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MascotEmotion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Has the assistant look at one drawing and write Karalak's part: what it thinks while the drawing builds up (wrong, but
 * funny, and never the right word), the question over the countdown, the line that comes with the code, a hook and a closing,
 * and the post text, each for its own moment of the video.
 * The right word is told to it only so it can stay away from it; a line that says it anyway is dropped (see [ForgotCopy.leaks]).
 */
class ForgotDirector(private val client: OpenRouterClient, private val model: String, private val lang: StudioLang = StudioLang.Tr) {

    class Written(val lines: ForgotCopy.Written, val costUsd: Double?)

    /**
     * Shows the assistant the drawing as it looks at each of [stops] (shares of it drawn), then whole, and has the lines
     * written; each guess comes from its own picture. Model: whatever [client] is told to use (Sonnet 5.5 through OpenRouter).
     */
    suspend fun write(drawing: StudioDrawing, stops: List<Float>, mode: ForgotMode, reward: String, limit: String, style: ForgotStyle = ForgotStyle.Game): Result<Written> = runCatching {
        val n = stops.size.coerceIn(1, ForgotTimeline.MAX_GUESSES)
        val shares = ForgotTimeline.stopsFor(n, stops)
        val images = withContext(Dispatchers.Default) {
            shares.map { StrokeReplay.png(drawing.strokes, it, 512) } + StrokeReplay.png(drawing.strokes, 1f, 512)
        }
        val prompt = ForgotCopy.directorPrompt(drawing.word, shares, MascotEmotion.entries.joinToString(", ") { it.key }, mode, reward, limit, style)
        val system = KaralakVoice.persona(JOB, lang)
        val answer = Backoff.retrying {
            client.generate(system, prompt + KaralakVoice.promptNote(lang, "JSON anahtarları ve emotion değerleri"), model, webSearch = false, images = images, json = true, temperature = 0.9, maxTokens = 1800)
        }.getOrThrow()
        val parsed = ForgotCopy.parse(answer.text, drawing.word) ?: error("Cümleler okunamadı; tekrar dene.")
        fun SpokenLine?.fit(words: Int) = this?.copy(text = LineRules.fit(text, words))
        Written(
            ForgotCopy.Written(
                parsed.guesses.take(n).map { it.copy(text = LineRules.fit(it.text, ForgotCopy.GUESS_WORDS)) },
                parsed.hook.fit(ForgotCopy.HOOK_WORDS), parsed.ask.fit(ForgotCopy.ASK_WORDS), parsed.codeLine.fit(ForgotCopy.CODE_WORDS), parsed.outro.fit(LineRules.OUTRO_WORDS),
                parsed.caption, parsed.hashtags
            ),
            answer.costUsd
        )
    }

    private companion object {
        const val JOB = "Şimdi \"Ödüllü Karalak Düellosu\" adlı kısa bir sosyal medya videosu için cümle yazıyorsun; cümleler sesli okunacak."
    }
}
