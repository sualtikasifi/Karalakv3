package com.sualtikasifi.cizimhafiza.presentation.duel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.Duel
import com.sualtikasifi.cizimhafiza.domain.model.DuelStatus
import com.sualtikasifi.cizimhafiza.domain.repository.DuelRepository
import com.sualtikasifi.cizimhafiza.presentation.navigation.Screen
import com.sualtikasifi.cizimhafiza.util.AnswerMatcher
import com.sualtikasifi.cizimhafiza.util.GameConstants
import com.sualtikasifi.cizimhafiza.util.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

/** SavedStateHandle key for [DuelPlayViewModel]'s process-death recovery checkpoint. */
private const val DUEL_RECOVERY_KEY = "duel_recovery"

/**
 * How far into the duel this device had gotten — see [DuelPlayViewModel]'s
 * class doc for why this exists. Small on purpose: unlike GameViewModel's
 * snapshot, there is no in-progress drawing to preserve here (the opponent
 * is only ever guessing already-recorded strokes), just the guess position
 * and running totals.
 */
@Serializable
private data class DuelRecovery(
    val currentIndex: Int,
    val correctCount: Int,
    val score: Int
)

/** Correct/wrong feedback for one guess, local to the duel play flow — see GamePhase's GuessFeedback for the (unrelated) solo/online equivalent. */
data class DuelGuessFeedback(val isCorrect: Boolean, val correctAnswer: String)

data class DuelPlayUiState(
    val isLoading: Boolean = true,
    val duel: Duel? = null,
    val currentIndex: Int = 0,
    val userAnswer: String = "",
    val feedback: DuelGuessFeedback? = null,
    val correctCount: Int = 0,
    val score: Int = 0,
    val isComplete: Boolean = false,
    val errorMessage: UiText? = null
) {
    val currentWord: String? get() = duel?.items?.getOrNull(currentIndex)?.word
}

/**
 * The opponent's half of a duel: look at each of the challenger's drawings
 * (already-recorded strokes — see Duel.items) and guess it, one at a time,
 * with no clock. Deliberately untimed and hint-free, unlike GameViewModel's
 * guessing phase — an async challenge that can be opened days later has no
 * "speed" to reward, and adding a countdown here would just punish whoever
 * happened to get interrupted mid-round.
 */
@HiltViewModel
class DuelPlayViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val duelRepository: DuelRepository
) : ViewModel() {

    private val duelId: String = checkNotNull(savedStateHandle[Screen.ArgDuelId])
    private val recoveryJson = Json { ignoreUnknownKeys = true }

    private val _uiState = MutableStateFlow(DuelPlayUiState())
    val uiState: StateFlow<DuelPlayUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            duelRepository.getDuel(duelId)
                .onSuccess { duel ->
                    // Already COMPLETE (this device already submitted its
                    // guesses and just re-entered the same duel screen, e.g.
                    // via a restored back-stack) — show it finished rather
                    // than letting the whole guessing flow run a second time
                    // and overwrite the real result with a fresh, possibly
                    // different, score.
                    if (duel?.status == DuelStatus.COMPLETE) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                duel = duel,
                                isComplete = true,
                                score = duel.opponentScore ?: 0,
                                correctCount = duel.opponentCorrectCount ?: 0
                            )
                        }
                        return@launch
                    }
                    // A killed-and-restarted process hands the same
                    // SavedStateHandle back — resume from the checkpointed
                    // guess position instead of restarting the duel from
                    // word 0 with the score reset. Without this, a process
                    // death mid-duel silently discarded every guess already
                    // answered.
                    val recovery = savedStateHandle.get<String>(DUEL_RECOVERY_KEY)?.let {
                        runCatching { recoveryJson.decodeFromString<DuelRecovery>(it) }.getOrNull()
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            duel = duel,
                            errorMessage = if (duel == null) UiText.of(R.string.duel_not_found) else null,
                            currentIndex = recovery?.currentIndex?.coerceIn(0, (duel?.items?.size ?: 1) - 1) ?: 0,
                            correctCount = recovery?.correctCount ?: 0,
                            score = recovery?.score ?: 0
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false, errorMessage = UiText.of(R.string.duel_not_found)) }
                }
        }
    }

    private fun saveRecovery(state: DuelPlayUiState) {
        savedStateHandle[DUEL_RECOVERY_KEY] = recoveryJson.encodeToString(
            DuelRecovery(currentIndex = state.currentIndex, correctCount = state.correctCount, score = state.score)
        )
    }

    fun onAnswerChanged(text: String) {
        if (_uiState.value.feedback != null) return
        _uiState.update { it.copy(userAnswer = text) }
        val target = _uiState.value.currentWord ?: return
        // Auto-submits on an exact (tolerant) match, same UX as the normal
        // guess screen — typing the last letter is the "submit" action.
        if (text.isNotBlank() && AnswerMatcher.isCorrect(userAnswer = text, target = target)) {
            submitGuess(text)
        }
    }

    fun submitGuess(answer: String) {
        val state = _uiState.value
        if (state.feedback != null) return
        val target = state.currentWord ?: return
        val correct = AnswerMatcher.isCorrect(userAnswer = answer, target = target)
        val updated = state.copy(
            feedback = DuelGuessFeedback(isCorrect = correct, correctAnswer = target),
            correctCount = state.correctCount + if (correct) 1 else 0,
            // Flat points, no speed bonus — see the class doc on why a
            // duel is deliberately untimed.
            score = state.score + if (correct) GameConstants.POINTS_CORRECT else 0
        )
        _uiState.value = updated
        // Checkpointed with this guess already folded in, so a process
        // death in the 1s pause below (or any time before the next guess)
        // resumes past this word rather than re-asking it.
        saveRecovery(updated)
        viewModelScope.launch {
            delay(1_000)
            advance()
        }
    }

    fun skip() = submitGuess("")

    private fun advance() {
        val state = _uiState.value
        val duel = state.duel ?: return
        val nextIndex = state.currentIndex + 1
        if (nextIndex < duel.items.size) {
            val updated = state.copy(currentIndex = nextIndex, userAnswer = "", feedback = null)
            _uiState.value = updated
            saveRecovery(updated)
        } else {
            _uiState.update { it.copy(isComplete = true) }
            // state was read at the top of this function, AFTER submitGuess's
            // own update already folded in this final guess — so it's the
            // true final total, not a stale pre-last-guess snapshot.
            viewModelScope.launch {
                duelRepository.submitDuelResult(
                    duelId = duelId,
                    opponentScore = state.score,
                    opponentCorrectCount = state.correctCount
                ).onSuccess {
                    // Only cleared once the result actually landed — a
                    // failed submit leaves the checkpoint in place so a
                    // retry (re-entering the screen) still has the final
                    // score to resend rather than losing it.
                    savedStateHandle.remove<String>(DUEL_RECOVERY_KEY)
                }
            }
        }
    }
}
