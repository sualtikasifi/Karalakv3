package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** One finished video or story picture kept on this phone, to be posted from the "Kayıtlar" page. */
@Serializable
data class SavedMedia(
    val id: Long,
    /** "video" or "image". */
    val kind: String,
    /** "Düello", "Gerçek Olsaydı" or "Bil Bakalım". */
    val format: String,
    /** What it is about: the word of the drawing, or the words of the drawings. */
    val title: String,
    val detail: String = "",
    /** The file's name inside the library folder. */
    val file: String,
    val madeAt: Long,
    val shared: Boolean = false,
    val sharedAt: Long = 0L
) {
    val isVideo: Boolean get() = kind == "video"
}

/** The library of finished contents: the list kept in the phone's preferences, the files in `files/studio_saved`. */
object SavedLibrary {
    const val MAX = 150

    /** Making the same thing again soon (same title, not posted yet) replaces its line instead of adding another. */
    const val SAME_MS = 15 * 60_000L

    private val json = Json { ignoreUnknownKeys = true }

    /** [entry] on top; a not-yet-posted one of the same kind and title made a moment ago is replaced. */
    fun add(list: List<SavedMedia>, entry: SavedMedia): List<SavedMedia> {
        val same = list.firstOrNull {
            !it.shared && it.kind == entry.kind && it.format == entry.format && it.title == entry.title && entry.madeAt - it.madeAt in 0..SAME_MS
        }
        val rest = if (same == null) list else list.filterNot { it.id == same.id }
        return (listOf(entry) + rest).take(MAX)
    }

    /** The lines [before] has that [after] does not: their files are no longer needed. */
    fun dropped(before: List<SavedMedia>, after: List<SavedMedia>): List<SavedMedia> {
        val keep = after.map { it.id }.toSet()
        return before.filterNot { it.id in keep }
    }

    /** "Posted" on or off for one line; posting stamps the time, taking it back clears it. */
    fun toggle(list: List<SavedMedia>, id: Long, now: Long): List<SavedMedia> =
        list.map { if (it.id == id) it.copy(shared = !it.shared, sharedAt = if (it.shared) 0L else now) else it }

    fun remove(list: List<SavedMedia>, id: Long): List<SavedMedia> = list.filterNot { it.id == id }

    fun removeShared(list: List<SavedMedia>): List<SavedMedia> = list.filterNot { it.shared }

    fun encode(list: List<SavedMedia>): String = json.encodeToString(ListSerializer(SavedMedia.serializer()), list)

    fun decode(text: String?): List<SavedMedia> =
        runCatching { json.decodeFromString(ListSerializer(SavedMedia.serializer()), text.orEmpty()) }.getOrDefault(emptyList())
}
