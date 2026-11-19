package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import kotlinx.coroutines.flow.update
import java.io.File

// The library of finished videos and story pictures ("Kayıtlar"). Extensions of [StudioViewModel].

private const val SAVED_LOG = "studio_saved_media"

internal fun StudioViewModel.savedDir(): File = File(context.filesDir, "studio_saved").apply { mkdirs() }

internal fun StudioViewModel.savedFile(m: SavedMedia): File = File(savedDir(), m.file)

internal fun StudioViewModel.loadSaved() {
    val list = SavedLibrary.decode(prefs().getString(SAVED_LOG, null))
    _state.update { it.copy(saved = list) }
}

private fun StudioViewModel.changeSaved(change: (List<SavedMedia>) -> List<SavedMedia>) {
    val before = _state.value.saved
    val next = change(before)
    prefs().edit().putString(SAVED_LOG, SavedLibrary.encode(next)).apply()
    SavedLibrary.dropped(before, next).forEach { runCatching { savedFile(it).delete() } }
    _state.update { it.copy(saved = next) }
}

/** Keeps a copy of a finished file (the cache is wiped by the next render) and puts it on top of the library. */
internal fun StudioViewModel.keepSaved(source: File, kind: String, format: String, title: String, detail: String = "") {
    runCatching {
        if (!source.exists() || title.isBlank()) return
        val now = System.currentTimeMillis()
        val name = (if (kind == "video") "video_" else "gorsel_") + now + (if (kind == "video") ".mp4" else ".png")
        source.copyTo(File(savedDir(), name), overwrite = true)
        changeSaved { SavedLibrary.add(it, SavedMedia(id = now, kind = kind, format = format, title = title.trim(), detail = detail, file = name, madeAt = now)) }
    }
}

/** The video just rendered: its title is what the log line says (the Düello's word, the words of the Gerçek Olsaydı drawings). */
internal fun StudioViewModel.keepMadeVideo(s: StudioState, video: File) {
    when (s.format) {
        StudioFormat.Forgot -> s.forgot.script?.let { sc -> keepSaved(video, "video", "Düello", sc.word, "${sc.style.title} · ${sc.mode.title}") }
        StudioFormat.RealLife -> s.real.project?.let { p ->
            val words = p.shown.mapNotNull { r -> p.drawing(r.sampleId)?.word }.take(3).joinToString(", ")
            keepSaved(video, "video", "Gerçek Olsaydı", words.ifBlank { "çizimler" })
        }
        StudioFormat.Riddle -> Unit
    }
}

fun StudioViewModel.toggleSavedShared(id: Long) = changeSaved { SavedLibrary.toggle(it, id, System.currentTimeMillis()) }

fun StudioViewModel.deleteSaved(id: Long) = changeSaved { SavedLibrary.remove(it, id) }

fun StudioViewModel.deleteSharedSaved() = changeSaved { SavedLibrary.removeShared(it) }
