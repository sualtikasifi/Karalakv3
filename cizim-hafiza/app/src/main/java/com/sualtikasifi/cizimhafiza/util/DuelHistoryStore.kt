package com.sualtikasifi.cizimhafiza.util

import android.content.Context
import com.sualtikasifi.cizimhafiza.domain.model.Duel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** One finished duel as this phone remembers it: just the scoreboard, no drawings. */
@Serializable
data class DuelHistoryEntry(
    val id: String,
    val iAmChallenger: Boolean,
    val otherName: String,
    val myScore: Int,
    val otherScore: Int,
    val myCorrect: Int,
    val otherCorrect: Int,
    val totalWords: Int,
    val finishedAt: Long,
    /** Only meaningful for the challenger: false until they have opened the result once. */
    val seen: Boolean = true
)

/**
 * The finished-duel history ("Düellolar → Geçmiş sonuçlar"), kept on the phone.
 *
 * A finished duel used to exist only as a Firestore document, so the list was re-read from the server on
 * every visit and vanished the moment the document was deleted. Now each result is copied here the first
 * time it is seen — by the challenger when the listener delivers it, by the opponent the instant they
 * finish — and the screen reads this copy. The server documents can stay or go; the history does not
 * depend on them, and showing it costs no reads.
 */
@Singleton
class DuelHistoryStore @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("duel_history", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Any()

    private val _entries = MutableStateFlow(load())
    val entries: StateFlow<List<DuelHistoryEntry>> = _entries.asStateFlow()

    /** Records every COMPLETE duel in [duels] that is not already here and was not removed by the player. */
    fun archive(duels: List<Duel>, myUid: String) {
        val fresh = duels.mapNotNull { it.toEntry(myUid) }
        if (fresh.isEmpty()) return
        synchronized(lock) {
            val removed = removedIds()
            val known = _entries.value.associateBy { it.id }
            val added = fresh.filter { it.id !in known && it.id !in removed }
            if (added.isNotEmpty()) save((_entries.value + added).sortedByDescending { it.finishedAt }.take(MAX_ENTRIES))
        }
    }

    /** The opponent's own copy, written the moment they finish — no server read involved. */
    fun archiveReceived(duel: Duel, myScore: Int, myCorrect: Int) {
        val entry = DuelHistoryEntry(
            id = duel.id,
            iAmChallenger = false,
            otherName = duel.challengerName,
            myScore = myScore,
            otherScore = duel.challengerScore,
            myCorrect = myCorrect,
            otherCorrect = duel.challengerCorrectCount,
            totalWords = duel.totalWords,
            finishedAt = System.currentTimeMillis()
        )
        synchronized(lock) {
            if (_entries.value.any { it.id == entry.id }) return
            save((_entries.value + entry).sortedByDescending { it.finishedAt }.take(MAX_ENTRIES))
        }
    }

    fun markSeen(id: String) = synchronized(lock) {
        if (_entries.value.none { it.id == id && !it.seen }) return
        save(_entries.value.map { if (it.id == id) it.copy(seen = true) else it })
    }

    fun remove(id: String) = synchronized(lock) {
        prefs.edit().putStringSet(KEY_REMOVED, removedIds() + id).apply()
        save(_entries.value.filterNot { it.id == id })
    }

    private fun Duel.toEntry(myUid: String): DuelHistoryEntry? {
        val theirScore = opponentScore ?: return null
        if (status != com.sualtikasifi.cizimhafiza.domain.model.DuelStatus.COMPLETE) return null
        val mine = challengerUid == myUid
        return DuelHistoryEntry(
            id = id,
            iAmChallenger = mine,
            otherName = if (mine) opponentName else challengerName,
            myScore = if (mine) challengerScore else theirScore,
            otherScore = if (mine) theirScore else challengerScore,
            myCorrect = if (mine) challengerCorrectCount else opponentCorrectCount ?: 0,
            otherCorrect = if (mine) opponentCorrectCount ?: 0 else challengerCorrectCount,
            totalWords = totalWords,
            finishedAt = completedAt ?: createdAt,
            seen = if (mine) seenByChallenger else true
        )
    }

    private fun removedIds(): Set<String> = prefs.getStringSet(KEY_REMOVED, emptySet()).orEmpty()

    private fun save(list: List<DuelHistoryEntry>) {
        _entries.value = list
        prefs.edit().putString(KEY_ENTRIES, json.encodeToString(list)).apply()
    }

    private fun load(): List<DuelHistoryEntry> =
        runCatching { json.decodeFromString<List<DuelHistoryEntry>>(prefs.getString(KEY_ENTRIES, null) ?: "[]") }
            .getOrDefault(emptyList())

    private companion object {
        const val KEY_ENTRIES = "entries"
        const val KEY_REMOVED = "removed"
        const val MAX_ENTRIES = 100
    }
}
