package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** One video that was made: what it was, which code it carried, and whether it has been posted. */
@Serializable
data class PublishEntry(
    val id: Long,
    /** "Düello" or "Gerçek Olsaydı". */
    val format: String,
    /** The drawing's word (Düello) or the words of the drawings (Gerçek Olsaydı): what the video is about. */
    val label: String,
    /** The wording and the way the code is given, for the Düello. */
    val detail: String = "",
    val code: String = "",
    val reward: String = "",
    val madeAt: Long,
    val published: Boolean = false,
    val publishedAt: Long = 0L
)

/** The log of made videos kept on this phone: which day went out with which code, and which are still waiting. */
object PublishLog {
    /** The log keeps this many videos; the oldest go first. */
    const val MAX = 200

    /** Making the same video again soon (the same words and code, not posted yet) replaces its line instead of adding another. */
    const val SAME_VIDEO_MS = 15 * 60_000L

    private val json = Json { ignoreUnknownKeys = true }

    /** [entry] added at the top; a line of the same video made a moment ago and not posted yet is replaced (keeping its id). */
    fun record(list: List<PublishEntry>, entry: PublishEntry): List<PublishEntry> {
        val same = list.firstOrNull {
            !it.published && it.format == entry.format && it.label == entry.label && it.code == entry.code &&
                entry.madeAt - it.madeAt in 0..SAME_VIDEO_MS
        }
        val rest = if (same == null) list else list.filterNot { it.id == same.id }
        return (listOf(if (same == null) entry else entry.copy(id = same.id)) + rest).take(MAX)
    }

    /** Turns "posted" on or off for one line; posting stamps the time, taking it back clears it. */
    fun toggle(list: List<PublishEntry>, id: Long, now: Long): List<PublishEntry> =
        list.map { if (it.id == id) it.copy(published = !it.published, publishedAt = if (it.published) 0L else now) else it }

    fun remove(list: List<PublishEntry>, id: Long): List<PublishEntry> = list.filterNot { it.id == id }

    fun encode(list: List<PublishEntry>): String = json.encodeToString(ListSerializer(PublishEntry.serializer()), list)

    /** What [encode] wrote; anything unreadable is an empty log, never a crash. */
    fun decode(text: String?): List<PublishEntry> =
        runCatching { json.decodeFromString(ListSerializer(PublishEntry.serializer()), text.orEmpty()) }.getOrDefault(emptyList())
}

/** What the studio costs: the assistant's spend on OpenRouter and the voice's credits on ElevenLabs, as the keys report them. */
data class SpendUi(
    val loading: Boolean = false,
    val openRouterToday: Double? = null,
    val openRouterMonth: Double? = null,
    val openRouterTotal: Double? = null,
    /** What is left of the key's spending limit, when it has one. */
    val openRouterLeft: Double? = null,
    val elevenUsed: Int? = null,
    val elevenLimit: Int? = null,
    /** What could not be read, in words (a missing key, a refused request). */
    val note: String? = null,
    val readAt: Long = 0L
)

/** How the spend is written. */
object SpendText {
    /** "$0.0123" for small sums, "$12.34" from ten cents on. */
    fun dollars(value: Double): String =
        if (value < 0.1) String.format(java.util.Locale.US, "$%.4f", value) else String.format(java.util.Locale.US, "$%.2f", value)

    /** The share used, 0 to 100, or null when there is no limit to measure against. */
    fun percent(used: Int, limit: Int): Int? = if (limit <= 0) null else (used * 100L / limit).toInt().coerceIn(0, 100)
}
