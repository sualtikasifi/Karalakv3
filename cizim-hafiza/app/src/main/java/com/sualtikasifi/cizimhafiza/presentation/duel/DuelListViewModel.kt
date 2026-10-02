package com.sualtikasifi.cizimhafiza.presentation.duel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sualtikasifi.cizimhafiza.domain.model.Duel
import com.sualtikasifi.cizimhafiza.domain.model.DuelStatus
import com.sualtikasifi.cizimhafiza.domain.model.FriendRequest
import com.sualtikasifi.cizimhafiza.domain.repository.DuelRepository
import com.sualtikasifi.cizimhafiza.domain.repository.FriendRepository
import com.sualtikasifi.cizimhafiza.util.DuelHistoryEntry
import com.sualtikasifi.cizimhafiza.util.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A finished duel seen from THIS player's side, whichever end of it they were on — read from the phone's own history. */
data class RecentDuel(private val entry: DuelHistoryEntry) {
    val id: String get() = entry.id
    val iAmChallenger: Boolean get() = entry.iAmChallenger
    val otherName: String get() = entry.otherName
    val myScore: Int get() = entry.myScore
    val otherScore: Int get() = entry.otherScore

    /** How many of [totalWords] each side actually guessed right — the "8/10" half of the story a bare score doesn't tell. */
    val myCorrectCount: Int get() = entry.myCorrect
    val otherCorrectCount: Int get() = entry.otherCorrect
    val totalWords: Int get() = entry.totalWords

    /** True/false for a win/loss for me, null for a tie. */
    val iWon: Boolean? get() = when {
        entry.myScore > entry.otherScore -> true
        entry.myScore < entry.otherScore -> false
        else -> null
    }

    /** The moment it finished — what "recent" is sorted on. */
    val finishedAt: Long get() = entry.finishedAt

    /** Only a result the challenger has not opened yet is "new"; the opponent played it themselves. */
    val isNew: Boolean get() = entry.iAmChallenger && !entry.seen
}

data class DuelListUiState(
    /** Challenges waiting for this player to play. */
    val incoming: List<Duel> = emptyList(),
    /** Friend requests waiting for an answer — shown alongside the challenges as "incoming". */
    val friendRequests: List<FriendRequest> = emptyList(),
    /** Challenges this player sent that nobody has played yet. */
    val pendingSent: List<Duel> = emptyList(),
    /** Finished duels from both directions, newest first. */
    val recent: List<RecentDuel> = emptyList(),
    /** True until the first snapshot of every source has arrived, so "empty" is never shown before it is known. */
    val isLoading: Boolean = true,
    val answeringRequestUid: String? = null
)

@HiltViewModel
class DuelListViewModel @Inject constructor(
    private val duelRepository: DuelRepository,
    private val friendRepository: FriendRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val myUid: String? get() = duelRepository.currentUid

    private val answeringUid = MutableStateFlow<String?>(null)

    init {
        // Duels this player answered on an older version have no on-phone copy yet; listening here copies them
        // in once (the repository archives whatever a snapshot carries). Nothing is shown from this stream.
        duelRepository.observeCompletedReceivedDuels().catch { }.launchIn(viewModelScope)
    }

    // Finished duels come from the phone's own history; the server listeners below only feed that history
    // (see DuelRepositoryImpl) and drive the "waiting for you" and "waiting for them" lists.
    private val sources = combine(
        duelRepository.observeIncomingDuels().catch { emit(emptyList()) },
        duelRepository.observeSentDuels().catch { emit(emptyList()) },
        friendRepository.observeFriendRequests().catch { emit(emptyList()) },
        duelRepository.history
    ) { incoming, sent, requests, history ->
        DuelListUiState(
            incoming = incoming,
            friendRequests = requests,
            pendingSent = sent.filter { it.status == DuelStatus.AWAITING_OPPONENT },
            recent = history.map { RecentDuel(it) }.sortedByDescending { it.finishedAt }.take(RECENT_LIMIT),
            isLoading = false
        )
    }

    val uiState: StateFlow<DuelListUiState> = combine(sources, answeringUid) { state, answering ->
        state.copy(answeringRequestUid = answering)
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = DuelListUiState())

    /** Marks a completed sent duel as seen — call when its result card is opened. */
    fun markSeen(duelId: String) {
        duelRepository.markHistorySeen(duelId)
        viewModelScope.launch { runCatching { duelRepository.markSeenByChallenger(duelId) } }
    }

    fun deleteDuel(duelId: String) {
        viewModelScope.launch { runCatching { duelRepository.deleteDuel(duelId) } }
    }

    fun acceptFriendRequest(request: FriendRequest) {
        if (answeringUid.value != null) return
        answeringUid.value = request.uid
        viewModelScope.launch {
            friendRepository.acceptFriendRequest(request, settingsRepository.nicknameOrDefault)
            answeringUid.value = null
        }
    }

    fun declineFriendRequest(request: FriendRequest) {
        if (answeringUid.value != null) return
        answeringUid.value = request.uid
        viewModelScope.launch {
            friendRepository.declineFriendRequest(request.uid)
            answeringUid.value = null
        }
    }

    private companion object {
        const val RECENT_LIMIT = 20
    }
}
