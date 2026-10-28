package com.sualtikasifi.cizimhafiza.presentation.reports.console

import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.sualtikasifi.cizimhafiza.util.AdminStats
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The AdminStats day documents and distinct-player counts for the last [DaySeries.days] days,
 * oldest first, as the dashboard, the statistics tab and the assistant all read them.
 */
data class DaySeries(
    /** yyyy-MM-dd, oldest first. */
    val days: List<String>,
    val players: List<Long>,
    /** Each day's raw counters (`app__open__count`, `ad__<placement>__<event>`, `rev__<placement>__micros`…). */
    val counts: List<Map<String, Long>>,
    /** First-seen players per day (not seen in the 30 days before); null for a day with no roll-up yet. */
    val newPlayers: List<Long?> = emptyList(),
    /** Of the players active the day before, the share active today (0..1); null when unknown. */
    val d1Return: List<Double?> = emptyList(),
    /** Of the players active seven days before, the share active today (0..1); null when unknown. */
    val d7Return: List<Double?> = emptyList()
) {
    val size: Int get() = days.size

    fun opens(i: Int): Long = counts[i]["app__open__count"] ?: 0L
    fun games(i: Int): Long = counts[i].filterKeys { it.startsWith("games__finished__") }.values.sum()
    fun adRequested(i: Int): Long = counts[i].filterKeys { it.startsWith("ad__") && it.endsWith("__reward_requested") }.values.sum()
    fun adEarned(i: Int): Long = counts[i].filterKeys { it.startsWith("ad__") && it.endsWith("__reward_earned") }.values.sum()
    fun interstitials(i: Int): Long = counts[i].filterKeys { it.startsWith("ad__") && it.endsWith("__interstitial_shown") }.values.sum()
    fun revenueMicros(i: Int): Long = counts[i].filterKeys { it.startsWith("rev__") && it.endsWith("__micros") }.values.sum()

    /** The currency the revenue counters are in (most-seen code), or null before any paid event arrived. */
    fun currency(): String? = counts.flatMap { it.entries }
        .filter { it.key.startsWith("revcur__") }
        .groupBy({ it.key.removePrefix("revcur__") }, { it.value })
        .maxByOrNull { it.value.sum() }?.key

    fun label(i: Int): String = shortDay(days[i])

    fun sumOf(range: IntRange, f: (Int) -> Long): Long = range.filter { it in days.indices }.sumOf(f)

    companion object {
        private val LABEL = DateTimeFormatter.ofPattern("d MMM", Locale("tr"))
        fun shortDay(iso: String): String = runCatching { LocalDate.parse(iso).format(LABEL) }.getOrDefault(iso)
    }
}

/**
 * Reads [days] days of statistics. A day the nightly roll-up has closed (adminSummary/{day}, see functions/src/adminTasks.ts)
 * is one document; only the days not rolled up yet — today, and any day before the job first ran — are summed from their
 * per-device rows (adminStats/{day}/devices) plus one count() for the distinct players.
 *
 * Every live row is written by a player's own device, so one modified client could still put any number in its OWN row;
 * each device's counters are therefore capped at what one device can plausibly produce in a day before they are
 * added (the roll-up applies the same limits), so a single row cannot swing the totals.
 */
suspend fun loadDaySeries(firestore: FirebaseFirestore, days: Int): DaySeries = coroutineScope {
    val today = LocalDate.now(ZoneOffset.UTC)
    val ids = (days - 1 downTo 0).map { today.minusDays(it.toLong()).toString() }
    val docsJob = async {
        firestore.collection(AdminStats.COLLECTION)
            .whereGreaterThanOrEqualTo(FieldPath.documentId(), ids.first())
            .get().await().documents
    }
    val summariesJob = async {
        runCatching {
            firestore.collection("adminSummary").whereGreaterThanOrEqualTo("day", ids.first()).get().await().documents
                .filter { it.getBoolean("final") == true }
                .associateBy { it.id }
        }.getOrDefault(emptyMap())
    }
    val summaries = summariesJob.await()
    val live = ids.filter { it !in summaries }
    val devicesJob = live.associateWith { day ->
        async {
            runCatching {
                firestore.collection(AdminStats.COLLECTION).document(day).collection(AdminStats.DEVICES)
                    .limit(MAX_DEVICE_ROWS).get().await().documents.map { countsOf(it.get("counts")) }
            }.getOrDefault(emptyList())
        }
    }
    val playersJob = live.associateWith { day ->
        async {
            runCatching {
                firestore.collection(AdminStats.COLLECTION).document(day).collection(AdminStats.USERS)
                    .count().get(AggregateSource.SERVER).await().count
            }.getOrDefault(0L)
        }
    }
    val legacyByDay = docsJob.await().associate { doc -> doc.id to countsOf(doc.get("counts")) }
    val counts = ids.map { day ->
        summaries[day]?.let { return@map countsOf(it.get("counts")) }
        val total = HashMap<String, Long>(legacyByDay[day].orEmpty())
        devicesJob.getValue(day).await().forEach { row ->
            row.forEach { (key, value) -> total[key] = (total[key] ?: 0L) + capPerDevice(key, value) }
        }
        total
    }
    DaySeries(
        days = ids,
        players = ids.map { summaries[it]?.getLong("players") ?: playersJob.getValue(it).await() },
        counts = counts,
        newPlayers = ids.map { summaries[it]?.getLong("newPlayers") },
        d1Return = ids.map { summaries[it]?.getDouble("d1Return") },
        d7Return = ids.map { summaries[it]?.getDouble("d7Return") }
    )
}

private fun countsOf(raw: Any?): Map<String, Long> = (raw as? Map<*, *>).orEmpty().mapNotNull { (k, v) ->
    val key = k as? String ?: return@mapNotNull null
    key to ((v as? Number)?.toLong() ?: 0L)
}.toMap()

/** What one device can plausibly add to a counter in a day: revenue (micros) and event counts have different scales. */
internal fun capPerDevice(key: String, value: Long): Long =
    value.coerceIn(0L, if (key.endsWith("__micros")) MAX_DEVICE_REVENUE_MICROS else MAX_DEVICE_EVENTS)

/** The most device rows read for one day (each is one read). */
private const val MAX_DEVICE_ROWS = 5_000L
private const val MAX_DEVICE_EVENTS = 2_000L
/** 20 units of the account currency in one day from one device — far above what ads pay a single player. */
private const val MAX_DEVICE_REVENUE_MICROS = 20_000_000L

/** How many documents a collection holds right now (one aggregate read). Null when the rules or the network say no. */
suspend fun countOf(firestore: FirebaseFirestore, collection: String): Long? = runCatching {
    firestore.collection(collection).count().get(AggregateSource.SERVER).await().count
}.getOrNull()
