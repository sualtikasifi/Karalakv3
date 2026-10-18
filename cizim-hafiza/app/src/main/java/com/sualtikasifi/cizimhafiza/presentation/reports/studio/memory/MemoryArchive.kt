package com.sualtikasifi.cizimhafiza.presentation.reports.studio.memory

import android.content.Context
import com.sualtikasifi.cizimhafiza.domain.model.DrawingPoint
import com.sualtikasifi.cizimhafiza.domain.model.ResultItem
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.guess.GuessCopy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Where a round came from. */
enum class MemorySource { Match, Daily }

/** One round as the archive takes it: its id (to count it once), where it came from, and its drawings in play order. */
class ArchiveRun(val id: String, val source: MemorySource, val items: List<ResultItem>)

/** How one word has gone: how many drew it, how many could not recognise their own drawing, and about how long after. */
@Serializable
data class MemoryWordStat(
    val key: String,
    val word: String,
    val drawn: Int = 0,
    val forgot: Int = 0,
    val delaySum: Long = 0,
    /** Example drawings kept: forgotten ones, and a few remembered ones for contrast. */
    val keptForgot: Int = 0,
    val keptRemembered: Int = 0,
    val daily: Int = 0
) {
    val samples: Int get() = keptForgot + keptRemembered
    val avgDelaySec: Int get() = if (drawn == 0) 0 else (delaySum / drawn).toInt()
    val forgotPercent: Int get() = if (drawn == 0) 0 else (forgot * 100 + drawn / 2) / drawn
}

/** One kept drawing: no author, no uid — only the word, whether its drawer remembered it, and the strokes. */
@Serializable
data class MemorySample(
    val id: String,
    val correct: Boolean,
    val source: MemorySource,
    val delaySec: Int,
    val strokes: List<List<DrawingPoint>>
)

/**
 * The studio's record of how well players remember their own drawings, kept on the reviewer's
 * phone only. The panel feeds it every queue, pool and daily-challenge page it loads (daily
 * attempts are deleted from the server once reviewed, so this is the only place their numbers
 * survive), and a one-off scan can read the whole pool.
 *
 * Nothing that names a player is kept: per word, the counts and a capped set of example drawings.
 */
@Singleton
class MemoryArchive @Inject constructor(@ApplicationContext context: Context) {
    private val root = File(context.filesDir, "studio_memory").apply { mkdirs() }
    private val samplesDir = File(root, "samples").apply { mkdirs() }
    private val seenFile = File(root, "seen.txt")
    private val statsFile = File(root, "stats.json")
    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Mutex()

    private var seen: MutableSet<String>? = null
    private var stats: MutableMap<String, MemoryWordStat>? = null

    private fun loadLocked() {
        if (seen == null) seen = runCatching { seenFile.readLines().filter { it.isNotBlank() }.toMutableSet() }.getOrDefault(mutableSetOf())
        if (stats == null) stats = runCatching { json.decodeFromString<Map<String, MemoryWordStat>>(statsFile.readText()).toMutableMap() }.getOrDefault(mutableMapOf())
    }

    /** Counts every round not seen before. Returns how many were new. */
    suspend fun ingest(runs: List<ArchiveRun>): Int = withContext(Dispatchers.IO) {
        lock.withLock {
            loadLocked()
            val seenSet = seen!!
            val map = stats!!
            val newIds = mutableListOf<String>()
            val newSamples = HashMap<String, MutableList<MemorySample>>()
            runs.forEach { run ->
                val runKey = run.source.name + ":" + run.id
                if (!seenSet.add(runKey)) return@forEach
                newIds += runKey
                run.items.forEachIndexed { i, item ->
                    if (item.strokes.isEmpty() || item.word.isBlank()) return@forEachIndexed
                    val key = MemoryMath.keyOf(item.word)
                    val delay = MemoryMath.delaySeconds(i, run.items.size)
                    val before = map[key] ?: MemoryWordStat(key, item.word.trim())
                    val keep = MemoryMath.shouldKeep(before, item.isCorrect)
                    map[key] = before.copy(
                        drawn = before.drawn + 1,
                        forgot = before.forgot + if (item.isCorrect) 0 else 1,
                        delaySum = before.delaySum + delay,
                        keptForgot = before.keptForgot + if (keep && !item.isCorrect) 1 else 0,
                        keptRemembered = before.keptRemembered + if (keep && item.isCorrect) 1 else 0,
                        daily = before.daily + if (run.source == MemorySource.Daily) 1 else 0
                    )
                    if (keep) newSamples.getOrPut(key) { mutableListOf() } += MemorySample("$runKey#$i", item.isCorrect, run.source, delay.toInt(), item.strokes)
                }
            }
            if (newIds.isNotEmpty()) {
                seenFile.appendText(newIds.joinToString("\n", postfix = "\n"))
                newSamples.forEach { (key, list) ->
                    File(samplesDir, "$key.jsonl").appendText(list.joinToString("") { json.encodeToString(it) + "\n" })
                }
                statsFile.writeText(json.encodeToString(map.toMap()))
            }
            newIds.size
        }
    }

    /** Every word with at least [minDrawn] drawings, most-forgotten share first. */
    suspend fun words(minDrawn: Int): List<MemoryWordStat> = withContext(Dispatchers.IO) {
        lock.withLock {
            loadLocked()
            stats!!.values.filter { it.drawn >= minDrawn }.sortedWith(compareByDescending<MemoryWordStat> { it.forgotPercent }.thenByDescending { it.drawn })
        }
    }

    suspend fun samples(key: String): List<MemorySample> = withContext(Dispatchers.IO) {
        lock.withLock {
            runCatching {
                File(samplesDir, "$key.jsonl").readLines().filter { it.isNotBlank() }.mapNotNull { runCatching { json.decodeFromString<MemorySample>(it) }.getOrNull() }
            }.getOrDefault(emptyList())
        }
    }

    suspend fun roundsSeen(): Int = withContext(Dispatchers.IO) { lock.withLock { loadLocked(); seen!!.size } }

    suspend fun clear() = withContext(Dispatchers.IO) {
        lock.withLock {
            root.listFiles()?.forEach { it.deleteRecursively() }
            samplesDir.mkdirs()
            seen = mutableSetOf()
            stats = mutableMapOf()
        }
    }
}

/** The archive's arithmetic, kept apart so it can be checked without a phone. */
object MemoryMath {
    /** Most forgotten drawings kept per word (the video shows those), and a few remembered ones for contrast. */
    const val MAX_FORGOT_SAMPLES = 40
    const val MAX_REMEMBERED_SAMPLES = 8

    /** A file-safe key: the word folded to plain lower-case letters, spaces as underscores. */
    fun keyOf(word: String): String = GuessCopy.normalize(word).replace(' ', '_').ifBlank { "bos" }.take(48)

    /**
     * About how many seconds passed between finishing a drawing and being asked what it was: the
     * later words still had to be drawn, then the break, then the earlier words were guessed.
     * Average drawing and answering times are used, which is why the studio only ever says
     * "yaklaşık".
     */
    fun delaySeconds(index: Int, count: Int): Long {
        val after = (count - 1 - index).coerceAtLeast(0)
        val before = index.coerceAtLeast(0)
        return after * (AVG_DRAW_SEC + STEP_SEC) + BREAK_SEC + before * (AVG_GUESS_SEC + STEP_SEC)
    }

    /** Keeps forgotten drawings up to their cap first; remembered ones only until their own, smaller cap. */
    fun shouldKeep(stat: MemoryWordStat, correct: Boolean): Boolean =
        if (correct) stat.keptRemembered < MAX_REMEMBERED_SAMPLES else stat.keptForgot < MAX_FORGOT_SAMPLES

    private const val AVG_DRAW_SEC = 7L
    private const val AVG_GUESS_SEC = 6L
    private const val STEP_SEC = 1L
    private const val BREAK_SEC = 3L

    /** Rounds to a number a person would say: 37 → 35, 42 → 40. */
    fun roundForSpeech(seconds: Int): Int = if (seconds < 10) seconds else ((seconds + 2) / 5) * 5
}
