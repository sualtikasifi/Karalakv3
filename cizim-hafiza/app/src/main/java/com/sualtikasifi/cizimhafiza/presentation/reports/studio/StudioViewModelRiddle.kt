package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import android.graphics.Bitmap
import androidx.lifecycle.viewModelScope
import com.sualtikasifi.cizimhafiza.presentation.reports.console.AssistantViewModel
import com.sualtikasifi.cizimhafiza.presentation.reports.console.OpenRouterClient
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.update
import java.io.File

// "Bil Bakalım Story": the drawing, its question and four answers, and the story picture made from them.

private const val RIDDLE_LAST = "riddle_last_correct"
private const val RIDDLE_PAGE = 12

internal fun StudioViewModel.updateRiddle(change: (RiddleUi) -> RiddleUi) = _state.update { it.copy(riddle = change(it.riddle)) }

fun StudioViewModel.setRiddleSource(source: ForgotSource) {
    updateRiddle { it.copy(source = source, shown = RIDDLE_PAGE) }
    if (source == ForgotSource.Pool && _state.value.real.recent.isEmpty()) refreshRecent()
}

fun StudioViewModel.showMoreRiddle() = updateRiddle { it.copy(shown = it.shown + RIDDLE_PAGE) }

/** Takes a drawing: its picture is made at once, its question and answers are written when asked for. */
fun StudioViewModel.selectRiddle(d: StudioDrawing) {
    updateRiddle { it.copy(drawingId = d.id, question = "", options = emptyList(), correct = -1, error = null, image = null) }
    makeRiddleImage(d)
}

/** Draws the story picture (the drawing on the painted board, the title on the sign) into the cache, where it can be shared from. */
internal fun StudioViewModel.makeRiddleImage(d: StudioDrawing) {
    viewModelScope.launch {
        val file = withContext(Dispatchers.Default) {
            runCatching {
                val bmp = RiddleStory.render(context, assetsOrLoad(), d.strokes)
                val dir = File(context.cacheDir, "studio_share").apply { mkdirs() }
                dir.listFiles()?.filter { it.name.startsWith("bilbakalim_") }?.forEach { it.delete() }
                val out = File(dir, "bilbakalim_${System.currentTimeMillis()}.png")
                out.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                bmp.recycle()
                out
            }
        }
        file.onSuccess { f -> updateRiddle { if (it.drawingId == d.id) it.copy(image = f, imageVersion = it.imageVersion + 1) else it } }
            .onFailure { e -> updateRiddle { it.copy(error = "Görsel çizilemedi: ${e.message}") } }
    }
}

/**
 * Has the assistant write the question, the right answer and three look-alikes, then puts the right one on a letter of its own
 * choosing: any of A-D except the one used the time before, so it does not stay on the same letter (the last one is kept on this phone).
 */
fun StudioViewModel.writeRiddle() {
    val s = _state.value
    val d = s.forgotDrawing(s.riddle.drawingId) ?: return
    if (s.riddle.writing) return
    val key = com.sualtikasifi.cizimhafiza.util.SecretStore.get(prefs(), AssistantViewModel.KEY)
    if (key.isBlank()) { updateRiddle { it.copy(error = "Önce Asistan ayarlarından OpenRouter anahtarını gir.") }; return }
    updateRiddle { it.copy(writing = true, error = null) }
    viewModelScope.launch {
        val client = OpenRouterClient(key)
        // the assistant is shown the drawing itself: a word like "fare" means an animal or a computer mouse, and only the picture says which
        val png = withContext(Dispatchers.Default) { runCatching { RiddleStory.drawingPng(d.strokes) }.getOrNull() }
        val result = runCatching {
            // one more try when the answer cannot be used (a repeated word, a missing question)
            var written: RiddleStory.Written? = null
            var last: Throwable? = null
            for (attempt in 1..2) {
                val answer = Backoff.retrying {
                    client.generate(RiddleStory.SYSTEM, RiddleStory.prompt(d.word, d.category), analysisModel(), webSearch = false, images = listOfNotNull(png), json = true, temperature = 0.8, maxTokens = 600)
                }.onFailure { last = it }.getOrNull() ?: break
                written = RiddleStory.parse(answer.text, d.word.trim().replaceFirstChar { it.uppercase() })
                if (written != null) break
            }
            written ?: throw (last ?: IllegalStateException("Yanıt okunamadı; tekrar dene."))
        }
        result.onSuccess { w ->
            val lastCorrect = prefs().getInt(RIDDLE_LAST, -1)
            val (options, at) = RiddleStory.arrange(w.answer, w.others, lastCorrect)
            prefs().edit().putInt(RIDDLE_LAST, at).apply()
            updateRiddle { it.copy(writing = false, question = w.question, options = options, correct = at, error = null) }
        }.onFailure { e -> updateRiddle { it.copy(writing = false, error = e.message ?: "Yazılamadı") } }
    }
}

/** Edits one answer by hand (the assistant's wording is a start, not the last word). */
fun StudioViewModel.setRiddleOption(index: Int, text: String) =
    updateRiddle { r -> if (index in r.options.indices) r.copy(options = r.options.toMutableList().also { it[index] = text.take(24) }) else r }

fun StudioViewModel.setRiddleQuestion(text: String) = updateRiddle { it.copy(question = text.take(60)) }
