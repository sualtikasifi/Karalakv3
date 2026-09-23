package com.sualtikasifi.cizimhafiza.presentation.duel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sualtikasifi.cizimhafiza.domain.model.Duel
import com.sualtikasifi.cizimhafiza.domain.model.DuelStatus
import com.sualtikasifi.cizimhafiza.domain.model.FriendRequest
import com.sualtikasifi.cizimhafiza.domain.repository.DuelRepository
import com.sualtikasifi.cizimhafiza.domain.repository.FriendRepository
import com.sualtikasifi.cizimhafiza.util.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A finished duel seen from THIS player's side, whichever end of it they were on. */
data class RecentDuel(val duel: Duel, val iAmChallenger: Boolean) {
    val otherName: String get() = if (iAmChallenger) duel.opponentName else duel.challengerName
    val myScore: Int get() = if (iAmChallenger) duel.challengerScore else duel.opponentScore ?: 0
    val otherScore: Int get() = if (iAmChallenger) duel.opponentScore ?: 0 else duel.challengerScore

    /** True/false for a win/loss for me, null for a tie. */
    val iWon: Boolean? get() = duel.challengerWon?.let { if (iAmChallenger) it else !it }

    /** The moment it finished — what "recent" is sorted on. */
    val finishedAt: Long get() = duel.completedAt ?: duel.createdAt

    /** Only a result the challenger has not opened yet is "new"; the opponent played it themselves. */
    val isNew: Boolean get() = iAmChallenger && !duel.seenByChallenger
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

    private val sources = combine(
        duelRepository.observeIncomingDuels().catch { emit(emptyList()) },
        duelRepository.observeSentDuels().catch { emit(emptyList()) },
        duelRepository.observeCompletedReceivedDuels().catch { emit(emptyList()) },
        friendRepository.observeFriendRequests().catch { emit(emptyList()) }
    ) { incoming, sent, receivedDone, requests ->
        val recent = (sent.filter { it.status == DuelStatus.COMPLETE }.map { RecentDuel(it, iAmChallenger = true) } +
            receivedDone.map { RecentDuel(it, iAmChallenger = false) })
            .sortedByDescending { it.finishedAt }
            .take(RECENT_LIMIT)
        DuelListUiState(
            incoming = incoming,
            friendRequests = requests,
            pendingSent = sent.filter { it.status == DuelStatus.AWAITING_OPPONENT },
            recent = recent,
            isLoading = false
        )
    }

    val uiState: StateFlow<DuelListUiState> = combine(sources, answeringUid) { state, answering ->
        state.copy(answeringRequestUid = answering)
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = DuelListUiState())

    /** Marks a completed sent duel as seen — call when its result card is opened. */
    fun markSeen(duelId: String) {
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
