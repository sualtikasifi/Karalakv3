package com.sualtikasifi.cizimhafiza.presentation.reports.console

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.sualtikasifi.cizimhafiza.domain.repository.BugReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import com.sualtikasifi.cizimhafiza.presentation.reports.JOBS
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardState(
    val loading: Boolean = false,
    val loaded: Boolean = false,
    val failed: Boolean = false,
    val series: DaySeries? = null,
    val pendingQueue: Long? = null,
    val pendingDaily: Long? = null,
    val openReports: Long? = null,
    val unseenFeedback: Int? = null,
    /** How the scheduled jobs last ran (adminJobs); empty until read. */
    val jobs: List<JobHealth> = emptyList(),
    val special: List<SpecialDay> = SpecialDays.upcoming()
)

/** The overview: two weeks of activity, what is waiting for a decision, and the dates coming up. */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val bugReports: BugReportRepository
) : ViewModel() {
    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init { load() }

    /** The refresh button: reads everything again, past the shared copy of the statistics. */
    fun refresh() = load(force = true)

    fun load(force: Boolean = false) {
        if (_state.value.loading) return
        _state.value = _state.value.copy(loading = true, failed = false)
        viewModelScope.launch {
            val series = async { runCatching { SharedDaySeries.get(firestore, 14, force) }.getOrNull() }
            val queue = async { countOf(firestore, "pendingRuns") }
            val daily = async { countOf(firestore, "pendingDailyChallenges") }
            val reports = async { countOf(firestore, "drawingReports") }
            // one count query (all, minus the seen ones), not forty reports read to count them
            val feedback = async { bugReports.unseenCount().getOrNull()?.toInt() }
            // seven small documents: which scheduled job failed or stopped, for the warnings at the top
            val jobs = async {
                JOBS.map { (name, label) ->
                    async {
                        val d = runCatching { firestore.collection("adminJobs").document(name).get().await() }.getOrNull()?.takeIf { it.exists() }
                        d?.let {
                            JobHealth(
                                label, it.getTimestamp("lastRunAt")?.toDate()?.time ?: 0L, it.getBoolean("ok"),
                                it.getString("error").orEmpty(), DashboardAlerts.maxAgeFor(name)
                            )
                        }
                    }
                }.awaitAll().filterNotNull()
            }
            val s = series.await()
            _state.value = _state.value.copy(
                loading = false,
                loaded = true,
                failed = s == null,
                series = s ?: _state.value.series,
                pendingQueue = queue.await(),
                pendingDaily = daily.await(),
                openReports = reports.await(),
                unseenFeedback = feedback.await(),
                jobs = jobs.await(),
                special = SpecialDays.upcoming()
            )
        }
    }
}
