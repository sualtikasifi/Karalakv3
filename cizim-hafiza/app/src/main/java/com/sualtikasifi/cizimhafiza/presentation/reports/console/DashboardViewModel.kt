package com.sualtikasifi.cizimhafiza.presentation.reports.console

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.sualtikasifi.cizimhafiza.domain.repository.BugReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
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

    fun load() {
        if (_state.value.loading) return
        _state.value = _state.value.copy(loading = true, failed = false)
        viewModelScope.launch {
            val series = async { runCatching { loadDaySeries(firestore, 14) }.getOrNull() }
            val queue = async { countOf(firestore, "pendingRuns") }
            val daily = async { countOf(firestore, "pendingDailyChallenges") }
            val reports = async { countOf(firestore, "drawingReports") }
            val feedback = async { bugReports.allReports(40).getOrNull()?.count { !it.report.isSeen } }
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
                special = SpecialDays.upcoming()
            )
        }
    }
}
