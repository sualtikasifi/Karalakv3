package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import androidx.lifecycle.viewModelScope
import com.sualtikasifi.cizimhafiza.presentation.reports.console.AssistantViewModel
import com.sualtikasifi.cizimhafiza.presentation.reports.console.OpenRouterClient
import com.sualtikasifi.cizimhafiza.util.SecretStore
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// What the studio keeps track of: the log of made videos, and what making them costs. Extensions of [StudioViewModel].

private const val PUBLISH_LOG = "studio_publish_log"

/** Reads the log from this phone. */
internal fun StudioViewModel.loadPublishLog() {
    _state.update { it.copy(publishLog = PublishLog.decode(prefs().getString(PUBLISH_LOG, null))) }
}

private fun StudioViewModel.changeLog(change: (List<PublishEntry>) -> List<PublishEntry>) {
    val next = change(_state.value.publishLog)
    prefs().edit().putString(PUBLISH_LOG, PublishLog.encode(next)).apply()
    _state.update { it.copy(publishLog = next) }
}

/** Puts the video just made into the log (a Düello with its code and wording, a Gerçek Olsaydı with its words). */
internal fun StudioViewModel.recordMadeVideo(s: StudioState) {
    val now = System.currentTimeMillis()
    val entry = when (s.format) {
        StudioFormat.Forgot -> s.forgot.script?.let { sc ->
            PublishEntry(
                id = now, format = "Düello", label = sc.word.trim(),
                detail = "${sc.style.title} · ${sc.mode.title}", code = sc.code, reward = sc.reward, madeAt = now
            )
        }
        StudioFormat.RealLife -> s.real.project?.let { p ->
            val words = p.shown.mapNotNull { r -> p.drawing(r.sampleId)?.word }.take(3).joinToString(", ")
            PublishEntry(id = now, format = "Gerçek Olsaydı", label = words.ifBlank { "çizimler" }, madeAt = now)
        }
    } ?: return
    changeLog { PublishLog.record(it, entry) }
}

/** Marks a line posted (or takes it back). */
fun StudioViewModel.togglePublished(id: Long) = changeLog { PublishLog.toggle(it, id, System.currentTimeMillis()) }

fun StudioViewModel.deletePublishEntry(id: Long) = changeLog { PublishLog.remove(it, id) }

/** Reads what the studio has cost from OpenRouter (the key's spend) and ElevenLabs (this month's credits); each only where its key is set. */
fun StudioViewModel.refreshSpend() {
    if (_state.value.spend.loading) return
    _state.update { it.copy(spend = it.spend.copy(loading = true)) }
    viewModelScope.launch {
        val notes = mutableListOf<String>()
        val key = SecretStore.get(prefs(), AssistantViewModel.KEY)
        val usage = if (key.isBlank()) { notes += "OpenRouter anahtarı yok (Asistan ayarları)."; null }
        else OpenRouterClient(key).usage().also { r -> r.exceptionOrNull()?.let { notes += "OpenRouter okunamadı: ${it.message}" } }.getOrNull()
        val eleven = elevenClient()
        val credits = if (eleven == null) { notes += "ElevenLabs anahtarı yok."; null }
        else eleven.credits().also { r -> r.exceptionOrNull()?.let { notes += "ElevenLabs okunamadı: ${it.message}" } }.getOrNull()
        _state.update {
            it.copy(
                spend = SpendUi(
                    loading = false,
                    openRouterToday = usage?.daily, openRouterMonth = usage?.monthly, openRouterTotal = usage?.total,
                    openRouterLeft = usage?.limitRemaining,
                    elevenUsed = credits?.used, elevenLimit = credits?.limit,
                    note = notes.joinToString("\n").ifBlank { null },
                    readAt = System.currentTimeMillis()
                )
            )
        }
    }
}
