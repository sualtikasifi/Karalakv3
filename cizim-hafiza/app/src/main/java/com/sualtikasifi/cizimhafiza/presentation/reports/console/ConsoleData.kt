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
    val counts: List<Map<String, Long>>
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

/** Reads [days] days of statistics; one query for the day documents plus one count() per day. */
suspend fun loadDaySeries(firestore: FirebaseFirestore, days: Int): DaySeries = coroutineScope {
    val today = LocalDate.now(ZoneOffset.UTC)
    val ids = (days - 1 downTo 0).map { today.minusDays(it.toLong()).toString() }
    val docsJob = async {
        firestore.collection(AdminStats.COLLECTION)
            .whereGreaterThanOrEqualTo(FieldPath.documentId(), ids.first())
            .get().await().documents
    }
    val playersJob = ids.map { day ->
        async {
            runCatching {
                firestore.collection(AdminStats.COLLECTION).document(day).collection(AdminStats.USERS)
                    .count().get(AggregateSource.SERVER).await().count
            }.getOrDefault(0L)
        }
    }
    val byDay = docsJob.await().associate { doc ->
        val counts = (doc.get("counts") as? Map<*, *>).orEmpty().mapNotNull { (k, v) ->
            val key = k as? String ?: return@mapNotNull null
            key to ((v as? Number)?.toLong() ?: 0L)
        }.toMap()
        doc.id to counts
    }
    DaySeries(days = ids, players = playersJob.awaitAll(), counts = ids.map { byDay[it].orEmpty() })
}

/** How many documents a collection holds right now (one aggregate read). Null when the rules or the network say no. */
suspend fun countOf(firestore: FirebaseFirestore, collection: String): Long? = runCatching {
    firestore.collection(collection).count().get(AggregateSource.SERVER).await().count
}.getOrNull()
