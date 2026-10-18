package com.sualtikasifi.cizimhafiza.presentation.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.sualtikasifi.cizimhafiza.presentation.reports.console.DaySeries
import com.sualtikasifi.cizimhafiza.presentation.reports.console.loadDaySeries
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    /** What AdMob reported this button earned, in micros of [StatsUiState.currency]. */
    val revenueMicros: Long = 0
)

data class StatsUiState(
    val days: Int = 30,
    val loading: Boolean = false,
    val failed: Boolean = false,
    val loaded: Boolean = false,
    val series: DaySeries? = null,
    val placements: List<AdPlacementStats> = emptyList()
)

/**
 * Reads the counters AdminStats writes, day by day. Only the reviewer's Google account passes the
 * rules for these documents, so on any other account the load simply fails — same as the other
 * panel tabs. A load is one query for the day documents plus one count() query per day.
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
            runCatching { loadDaySeries(firestore, days) }
                .onSuccess { s ->
                    _state.value = StatsUiState(days = days, loaded = true, series = s, placements = placements(s))
                }
                .onFailure { _state.value = _state.value.copy(loading = false, failed = true) }
        }
    }

    private fun placements(s: DaySeries): List<AdPlacementStats> {
        val totals = HashMap<String, Long>()
        s.counts.forEach { day -> day.forEach { (k, v) -> totals[k] = (totals[k] ?: 0L) + v } }
        val byPlacement = HashMap<String, AdPlacementStats>()
        for ((key, value) in totals) {
            val parts = key.split("__")
            if (parts.size != 3) continue
            val name = parts[1]
            val cur = byPlacement[name] ?: AdPlacementStats(name)
            byPlacement[name] = when {
                parts[0] == "rev" && parts[2] == "micros" -> cur.copy(revenueMicros = value)
                parts[0] != "ad" -> continue
                parts[2] == "reward_requested" -> cur.copy(requested = value)
                parts[2] == "reward_earned" -> cur.copy(earned = value)
                parts[2] == "reward_skipped" -> cur.copy(skipped = value)
                parts[2] == "reward_unavailable" -> cur.copy(unavailable = value)
                parts[2] == "interstitial_shown" -> cur.copy(shown = value)
                else -> cur
            }
        }
        return byPlacement.values.sortedByDescending { it.requested + it.shown }
    }
}
