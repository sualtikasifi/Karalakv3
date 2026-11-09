package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import androidx.lifecycle.viewModelScope
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotChest
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotDraft
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotDraftStore
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotExamples
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotLearning
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotScript
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotSource
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.random.Random

// Drafts: the Düello is kept on the phone as the work goes on, so closing the screen or the app loses nothing, a line of the
// publish log opens it where it was left (or shows its finished video), and the videos marked "Tamamlandı" teach the assistant.
// Extensions of [StudioViewModel].

private const val LAST_DRAFT = "studio_forgot_last_draft"

/** How many finished videos the assistant looks at for its examples. */
private const val LEARN_FROM = 12

/** What counts as a change worth saving: everything of the draft but the drawing (which does not change). */
private data class DraftKey(
    val id: Long, val script: ForgotScript, val caption: String, val hashtags: String, val source: ForgotSource, val chest: ForgotChest,
    val gold: Int, val xp: Int, val maxUses: Int, val expires: Long, val shownCode: String, val styleChoice: ForgotStyle?,
    val music: String?, val volume: Float, val lang: StudioLang, val done: Boolean, val video: String?, val videoFingerprint: Int
)

/** Writes the draft to the phone a moment after each change (not on every keystroke), and keeps its line in the log in step. */
@OptIn(FlowPreview::class)
internal fun StudioViewModel.startDraftAutosave() {
    viewModelScope.launch {
        _state.map { s ->
            val f = s.forgot
            val sc = f.script
            val id = f.draftId
            if (id == null || sc == null) null
            else DraftKey(id, sc, s.caption, s.hashtags, f.source, f.chest, f.gold, f.xp, f.maxUses, f.expiresAtMillis, f.shownCode, f.styleChoice, f.music, f.musicVolume, s.lang, f.done, f.draftVideo, f.draftVideoFingerprint)
        }.distinctUntilChanged().debounce(600).collect { key -> key?.let { saveDraft(it) } }
    }
}

private suspend fun StudioViewModel.saveDraft(key: DraftKey) {
    val s = _state.value
    if (s.forgot.draftId != key.id) return
    val drawing = s.forgotDrawing(s.forgot.drawingId) ?: return
    val draft = ForgotDraft(
        id = key.id, updatedAt = System.currentTimeMillis(),
        word = drawing.word, category = drawing.category, strokes = drawing.strokes, correct = drawing.correct, answer = drawing.answer,
        drawingId = drawing.id, script = key.script, caption = key.caption, hashtags = key.hashtags,
        source = key.source.name, chest = key.chest.name, gold = key.gold, xp = key.xp, maxUses = key.maxUses,
        expiresAtMillis = key.expires, shownCode = key.shownCode, styleChoice = key.styleChoice?.name,
        music = key.music, musicVolume = key.volume, lang = key.lang.code, done = key.done,
        video = key.video, videoFingerprint = key.videoFingerprint
    )
    withContext(Dispatchers.IO) { runCatching { draftStore.save(draft) } }
    // the log's line says what the draft is about now (its word, the wording, the code, the prize)
    val sc = key.script
    changeLog { PublishLog.touchDraft(it, key.id, sc.word.trim(), "${sc.style.title} · ${sc.mode.title}", sc.code, sc.reward) }
}

/** A new draft for [word]'s Düello: its line goes to the top of the log as "taslak"; returns its id. */
internal fun StudioViewModel.beginDraftEntry(id: Long, sc: ForgotScript) {
    prefs().edit().putLong(LAST_DRAFT, id).apply()
    changeLog {
        PublishLog.addDraft(it, PublishEntry(
            id = id, format = "Düello", label = sc.word.trim(), detail = "${sc.style.title} · ${sc.mode.title}",
            code = sc.code, reward = sc.reward, madeAt = id, draftId = id, draft = true
        ))
    }
}

/** The video was made: keeps it with the draft (so its last form can be seen again) and notes which script it was made from. */
internal fun StudioViewModel.keepDraftVideo(s: StudioState, out: File) {
    val id = s.forgot.draftId ?: return
    val sc = s.forgot.script ?: return
    viewModelScope.launch {
        val name = withContext(Dispatchers.IO) { draftStore.keepVideo(id, out) }
        if (name != null) updateForgot { if (it.draftId == id) it.copy(draftVideo = name, draftVideoFingerprint = ForgotDraftStore.fingerprint(sc)) else it }
    }
}

/** Opens a draft where it was left: its drawing, every line, the choices, and (when it was made and not changed since) its video. */
fun StudioViewModel.openForgotDraft(id: Long, announce: Boolean = true) {
    viewModelScope.launch {
        val d = withContext(Dispatchers.IO) { draftStore.load(id) }
        if (d == null) { _state.update { it.copy(message = "Taslak dosyası bulunamadı.") }; return@launch }
        val video = d.takeIf { it.videoIsCurrent }?.video?.let { draftStore.videoFile(it) }?.takeIf { it.exists() }
        prefs().edit().putLong(LAST_DRAFT, id).apply()
        _state.update { s ->
            s.copy(
                format = StudioFormat.Forgot, caption = d.caption, hashtags = d.hashtags, video = video, renderError = null, previewMs = 0L,
                openedDraft = if (announce) s.openedDraft + 1 else s.openedDraft,
                forgot = s.forgot.copy(
                    drawingId = d.drawingId, draftDrawing = d.drawing(), draftId = d.id, script = d.script, done = d.done,
                    source = runCatching { ForgotSource.valueOf(d.source) }.getOrDefault(s.forgot.source),
                    chest = runCatching { ForgotChest.valueOf(d.chest) }.getOrDefault(s.forgot.chest),
                    gold = d.gold, xp = d.xp, maxUses = d.maxUses, expiresAtMillis = d.expiresAtMillis, shownCode = d.shownCode.ifBlank { s.forgot.shownCode },
                    styleChoice = d.styleChoice?.let { n -> runCatching { ForgotStyle.valueOf(n) }.getOrNull() },
                    music = d.music, musicVolume = d.musicVolume, draftVideo = d.video, draftVideoFingerprint = d.videoFingerprint,
                    writeError = null, couponStatus = null
                )
            )
        }
        refreshPreview()
    }
}

/** On opening the studio, the draft that was open last time is open again. */
internal fun StudioViewModel.restoreLastDraft() {
    val id = prefs().getLong(LAST_DRAFT, 0L)
    if (id != 0L) openForgotDraft(id, announce = false)
}

/** A tap on a line of the log: a Düello draft opens where it was left (or as its finished video); a Gerçek Olsaydı line goes to its own project. */
fun StudioViewModel.openLogEntry(e: PublishEntry) {
    when {
        e.draftId != 0L -> openForgotDraft(e.draftId)
        e.format == "Gerçek Olsaydı" -> {
            setFormat(StudioFormat.RealLife)
            _state.update { it.copy(openedDraft = it.openedDraft + 1, message = "Gerçek Olsaydı projesi kayıtlı; kaldığın yerden devam edebilirsin.") }
        }
        else -> _state.update { it.copy(message = "Bu eski kaydın taslağı tutulmamış.") }
    }
}

/** "Tamamlandı" on or off for a line of the log; a finished Düello's lines are what the assistant takes as examples from then on. */
fun StudioViewModel.toggleDone(entryId: Long) {
    val e = _state.value.publishLog.firstOrNull { it.id == entryId } ?: return
    if (e.draft) { _state.update { it.copy(message = "Önce videoyu oluştur, sonra tamamlandı de.") }; return }
    val done = !e.done
    changeLog { PublishLog.toggleDone(it, entryId) }
    if (e.draftId == 0L) return
    updateForgot { if (it.draftId == e.draftId) it.copy(done = done) else it }
    viewModelScope.launch {
        // the open draft is written by the autosave; any other is changed in its file
        if (_state.value.forgot.draftId != e.draftId) withContext(Dispatchers.IO) {
            draftStore.load(e.draftId)?.let { runCatching { draftStore.save(it.copy(done = done, updatedAt = System.currentTimeMillis())) } }
        }
        refreshLearned()
        _state.update { it.copy(message = if (done) "Tamamlandı: bu videonun cümleleri örnek olarak kullanılacak." else "Tamamlandı etiketi kaldırıldı.") }
    }
}

/** The "Tamamlandı" button under the finished video. */
fun StudioViewModel.toggleDoneForOpenDraft() {
    val id = _state.value.forgot.draftId ?: return
    _state.value.publishLog.firstOrNull { it.draftId == id }?.let { toggleDone(it.id) }
}

/** Removes a draft's files (and, if it is the one open, lets go of it: the work on screen stays, it is just no longer saved). */
internal fun StudioViewModel.dropDraft(draftId: Long) {
    if (_state.value.forgot.draftId == draftId) updateForgot { it.copy(draftId = null, done = false, draftVideo = null) }
    if (prefs().getLong(LAST_DRAFT, 0L) == draftId) prefs().edit().remove(LAST_DRAFT).apply()
    viewModelScope.launch { withContext(Dispatchers.IO) { runCatching { draftStore.delete(draftId) } }; refreshLearned() }
}

/** How many finished videos the assistant has examples from. */
internal fun StudioViewModel.refreshLearned() {
    viewModelScope.launch {
        val n = withContext(Dispatchers.IO) { runCatching { draftStore.doneDrafts(LEARN_FROM).count { it.lang == _state.value.lang.code } }.getOrDefault(0) }
        updateForgot { it.copy(learned = n) }
    }
}

/** The finished videos' lines to show the assistant for the drawing of [word]; none when nothing is marked "Tamamlandı" yet. */
internal suspend fun StudioViewModel.learnedExamples(word: String): ForgotExamples = withContext(Dispatchers.IO) {
    runCatching {
        ForgotLearning.examples(draftStore.doneDrafts(LEARN_FROM), word, _state.value.forgot.draftId, _state.value.lang.code, Random.Default)
    }.getOrDefault(ForgotExamples.NONE)
}
