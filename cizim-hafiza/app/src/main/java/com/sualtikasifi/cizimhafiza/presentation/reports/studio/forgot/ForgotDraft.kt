package com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot

import com.sualtikasifi.cizimhafiza.domain.model.DrawingPoint
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioDrawing
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealMix
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.random.Random

/**
 * A Düello as it stands: the drawing (kept in full, so it is still there when the phone's list or the pool has changed), the
 * script with every line, the post text and the panel's choices. Written to the phone as the work goes on, so closing the
 * screen or the app loses nothing, and the video can be picked up where it was left.
 */
@Serializable
data class ForgotDraft(
    val id: Long,
    val updatedAt: Long,
    val word: String,
    val category: String = "",
    val strokes: List<List<DrawingPoint>>,
    val correct: Boolean = false,
    val answer: String = "",
    val drawingId: Long,
    val script: ForgotScript,
    val caption: String = "",
    val hashtags: String = "",
    val source: String = "Phone",
    val chest: String = "Gold",
    val gold: Int = 0,
    val xp: Int = 0,
    val maxUses: Int = 100,
    val expiresAtMillis: Long = 0L,
    val shownCode: String = "",
    val styleChoice: String? = null,
    val music: String? = null,
    val musicVolume: Float = RealMix.DEFAULT_VOLUME,
    /** The language the lines were written in (see StudioLang.code): examples are only taken from the same one. */
    val lang: String = "tr",
    /** "Tamamlandı": the video is finished; its lines are what the assistant is shown as examples. */
    val done: Boolean = false,
    /** The finished video kept with the draft (a file name in the store), and what the script was when it was made. */
    val video: String? = null,
    val videoFingerprint: Int = 0
) {
    fun drawing() = StudioDrawing(drawingId, word, category, strokes, correct, answer)

    /** Whether the kept video still shows this script (it does not once a line was changed after it was made). */
    val videoIsCurrent: Boolean get() = video != null && videoFingerprint == ForgotDraftStore.fingerprint(script)
}

/** Drafts and their finished videos on the phone: one json file per draft, the videos next to them. */
class ForgotDraftStore(private val dir: File) {
    fun save(draft: ForgotDraft) {
        dir.mkdirs()
        val target = file(draft.id)
        val tmp = File(dir, "${draft.id}.json.part")
        tmp.writeText(json.encodeToString(ForgotDraft.serializer(), draft))
        if (!tmp.renameTo(target)) { target.writeText(tmp.readText()); tmp.delete() }
    }

    /** Null when there is no such draft or it cannot be read: never a crash. */
    fun load(id: Long): ForgotDraft? =
        runCatching { json.decodeFromString(ForgotDraft.serializer(), file(id).readText()) }.getOrNull()

    fun delete(id: Long) {
        file(id).delete()
        videoFile("$id.mp4").delete()
    }

    fun videoFile(name: String): File = File(File(dir, "videos"), name)

    /** Copies [src] in as draft [id]'s video; returns its name, null when it could not be kept. Only the newest [MAX_VIDEOS] are kept. */
    fun keepVideo(id: Long, src: File): String? = runCatching {
        val videos = File(dir, "videos").apply { mkdirs() }
        val name = "$id.mp4"
        val tmp = File(videos, "$name.part")
        src.copyTo(tmp, overwrite = true)
        val target = File(videos, name)
        target.delete()
        if (!tmp.renameTo(target)) { tmp.copyTo(target, overwrite = true); tmp.delete() }
        videos.listFiles { f -> f.isFile && f.name.endsWith(".mp4") }?.sortedByDescending { it.lastModified() }?.drop(MAX_VIDEOS)?.forEach { it.delete() }
        name
    }.getOrNull()

    /** The newest finished ("Tamamlandı") drafts, at most [limit]; only the latest [SCAN] files are looked at. */
    fun doneDrafts(limit: Int): List<ForgotDraft> {
        val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".json") }?.sortedByDescending { it.lastModified() }.orEmpty()
        val out = ArrayList<ForgotDraft>()
        for (f in files.take(SCAN)) {
            if (out.size >= limit) break
            runCatching { json.decodeFromString(ForgotDraft.serializer(), f.readText()) }.getOrNull()?.takeIf { it.done }?.let(out::add)
        }
        return out
    }

    private fun file(id: Long) = File(dir, "$id.json")

    companion object {
        const val MAX_VIDEOS = 10
        private const val SCAN = 80
        private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; encodeDefaults = true }

        /** A number for what the script says: stable between runs, so a kept video can tell whether it is still this script's. */
        fun fingerprint(script: ForgotScript): Int = json.encodeToString(ForgotScript.serializer(), script).hashCode()
    }
}

/** Lines of finished videos, to show the assistant by example (see [ForgotLearning]). */
data class ForgotExamples(
    val hooks: List<String> = emptyList(),
    val guesses: List<String> = emptyList(),
    val asks: List<String> = emptyList(),
    val codes: List<String> = emptyList(),
    val outros: List<String> = emptyList()
) {
    val isEmpty: Boolean get() = hooks.isEmpty() && guesses.isEmpty() && asks.isEmpty() && codes.isEmpty() && outros.isEmpty()

    companion object { val NONE = ForgotExamples() }
}

/**
 * What the assistant learns from the videos marked "Tamamlandı": the lines that came out of them (written by it, then
 * corrected by hand where they were not right) are shown to it as examples of the voice wanted, a few of each kind, taken
 * at random every time so the examples vary. It is not training of a model: the examples ride along in the prompt.
 */
object ForgotLearning {
    const val HOOKS = 4
    const val GUESSES = 6
    const val ASKS = 3
    const val CODES = 3
    const val OUTROS = 3
    private const val MAX_LEN = 160

    /** The stock lines the panel starts with: not examples of anything. */
    private val STOCK = setOf(ForgotCopy.HOOK, ForgotCopy.ASK, ForgotCopy.CODE_WORD, ForgotCopy.CODE_SHOWN, ForgotCopy.OUTRO) +
        ForgotCopy.placeholderGuesses().map { it.text }

    fun examples(done: List<ForgotDraft>, word: String, excludeId: Long?, lang: String, random: Random): ForgotExamples {
        val from = done.filter { it.done && it.id != excludeId && it.lang == lang }
        fun pick(n: Int, lines: (ForgotScript) -> List<String>): List<String> =
            from.flatMap { lines(it.script) }
                .map { it.trim() }
                .filter { it.isNotEmpty() && it.length <= MAX_LEN && it !in STOCK && !ForgotCopy.leaks(it, word) }
                .distinct().shuffled(random).take(n)
        return ForgotExamples(
            hooks = pick(HOOKS) { listOf(it.hook.text) },
            guesses = pick(GUESSES) { s -> s.guesses.map { it.text } },
            asks = pick(ASKS) { listOf(it.askLine.text) },
            codes = pick(CODES) { listOf(it.codeLine.text) },
            outros = pick(OUTROS) { listOf(it.outro.text) }
        )
    }
}
