package com.sualtikasifi.cizimhafiza.presentation.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.sualtikasifi.cizimhafiza.util.AdminStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

/** One ad button's totals over the chosen range. */
data class AdPlacementStats(
    val placement: String,
    val requested: Long = 0,
    val earned: Long = 0,
    val skipped: Long = 0,
    val unavailable: Long = 0,
    /** Interstitials that reached the screen (rewarded ads have no such event: they count as earned/skipped). */
    val shown: Long = 0,

)

data class DayPlayers(val day: String, val players: Long)

data class StatsUiState(
    val days: Int = 7,
    val loading: Boolean = false,
    val failed: Boolean = false,
    val loaded: Boolean = false,
    val appOpens: Long = 0,
    val gamesFinished: Long = 0,

    val players: List<DayPlayers> = emptyList(),
    val placements: List<AdPlacementStats> = emptyList()
)

/**
 * Reads the counters AdminStats writes. Only the reviewer's Google account passes the rules for
 * these documents, so on any other account the load simply fails — same as the other panel tabs.
 * A load is one query for the day documents plus one count() query per day; nothing is listened to.
 */
@HiltViewModel
class StatsViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _state = MutableStateFlow(StatsUiState())
    val state: StateFlow<StatsUiState> = _state.asStateFlow()

    fun selectDays(days: Int) {
        if (_state.value.days == days && _state.value.loaded) return
        _state.value = _state.value.copy(days = days)
        load()
    }

    fun load() {
        val days = _state.value.days
        _state.value = _state.value.copy(loading = true, failed = false)
        viewModelScope.launch {
            runCatching {
                val today = LocalDate.now(ZoneOffset.UTC)
                val dayIds = (0 until days).map { today.minusDays(it.toLong()).toString() }
                val docs = firestore.collection(AdminStats.COLLECTION)
                    .whereGreaterThanOrEqualTo(FieldPath.documentId(), dayIds.last())
                    .get().await().documents
                val totals = HashMap<String, Long>()
                for (doc in docs) {
                    val counts = doc.get("counts") as? Map<*, *> ?: continue
                    for ((k, v) in counts) {
                        val key = k as? String ?: continue
                        totals[key] = (totals[key] ?: 0L) + ((v as? Number)?.toLong() ?: 0L)
                    }
                }
                val players = coroutineScope {
                    dayIds.map { day ->
                        async {
                            val n = runCatching {
                                firestore.collection(AdminStats.COLLECTION).document(day)
                                    .collection(AdminStats.USERS)
                                    .count().get(AggregateSource.SERVER).await().count
                            }.getOrDefault(0L)
                            DayPlayers(day, n)
                        }
                    }.awaitAll()
                }
                buildState(days, totals, players)
            }.onSuccess { _state.value = it }
                .onFailure { _state.value = _state.value.copy(loading = false, failed = true) }
        }
    }

    private fun buildState(days: Int, totals: Map<String, Long>, players: List<DayPlayers>): StatsUiState {
        val byPlacement = HashMap<String, AdPlacementStats>()
        for ((key, value) in totals) {
            val parts = key.split("__")
            if (parts.size != 3 || parts[0] != "ad") continue
            val cur = byPlacement[parts[1]] ?: AdPlacementStats(parts[1])
            byPlacement[parts[1]] = when (parts[2]) {
                "reward_requested" -> cur.copy(requested = value)
                "reward_earned" -> cur.copy(earned = value)
                "reward_skipped" -> cur.copy(skipped = value)
                "reward_unavailable" -> cur.copy(unavailable = value)
                "interstitial_shown" -> cur.copy(shown = value)
                else -> cur
            }
        }
        val rows = byPlacement.values.sortedByDescending { it.requested + it.shown }
        return StatsUiState(
            days = days,
            loaded = true,
            appOpens = totals["app__open__count"] ?: 0L,
            gamesFinished = totals.filterKeys { it.startsWith("games__finished__") }.values.sum(),
            players = players,
            placements = rows
        )
    }
}
