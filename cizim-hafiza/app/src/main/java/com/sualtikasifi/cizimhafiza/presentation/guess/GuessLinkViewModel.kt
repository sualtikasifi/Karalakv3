package com.sualtikasifi.cizimhafiza.presentation.guess

import android.app.Activity
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sualtikasifi.cizimhafiza.ads.AdManager
import com.sualtikasifi.cizimhafiza.data.local.WordSeeder
import com.sualtikasifi.cizimhafiza.domain.model.DrawingStroke
import com.sualtikasifi.cizimhafiza.domain.model.GuessLink
import com.sualtikasifi.cizimhafiza.domain.repository.GuessRepository
import com.sualtikasifi.cizimhafiza.util.GuessCodec
import com.sualtikasifi.cizimhafiza.util.LegalConsentStore
import com.sualtikasifi.cizimhafiza.util.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Where the "Bil Bakalım" screen is. */
enum class GuessPhase { Loading, Unavailable, Guessing, Solved, Failed, Drawing, Shared }

/** Why a link cannot be played. */
enum class GuessProblem { NotFound, Offline, Child, Own }

data class GuessUiState(
    val phase: GuessPhase = GuessPhase.Loading,
    val problem: GuessProblem? = null,
    val link: GuessLink? = null,
    /** The drawing has finished drawing itself: the guess box opens. */
    val replayDone: Boolean = false,
    /** Bumped by "tekrar izle". */
    val playToken: Int = 0,
    val input: String = "",
    val attempts: Int = 0,
    val solved: Boolean = false,
    val wrong: List<String> = emptyList(),
    /** True for a moment after a wrong guess, for the shake. */
    val missed: Boolean = false,
    /** The word, once the round is over (null: the language's list does not hold it). */
    val word: String? = null,
    // "Sıra sende"
    val live: List<DrawingStroke> = emptyList(),
    val secondsLeft: Int = DRAW_SECONDS,
    val busy: Boolean = false,
    val newLinkId: String? = null,
    val limitReached: Boolean = false,
    val shareFailed: Boolean = false,
    val reported: Boolean = false
) {
    /** What the hints show: nothing at first, the shape of the word after the first miss, its first letter after the second. */
    val hint: String
        get() {
            val l = link ?: return ""
            if (attempts < 1) return ""
            return GuessCodec.blanks(l.pattern, if (attempts >= 2) l.first else null)
        }
}

const val DRAW_SECONDS = 10

/**
 * One screen for the whole life of a link a friend sent: watch the drawing, guess it (three tries, a hint after each
 * miss), and, win or lose, "sıra sende": draw the same word and send a link of your own back. The result is left in
 * the sender's inbox (see GuessRepository.sendResult) so their app can tell them.
 */
@HiltViewModel
class GuessLinkViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val repository: GuessRepository,
    private val settingsRepository: SettingsRepository,
    private val legalConsentStore: LegalConsentStore,
    private val adManager: AdManager,
    private val playerReporter: com.sualtikasifi.cizimhafiza.util.PlayerReporter
) : ViewModel() {

    private val linkId: String = savedStateHandle.get<String>("linkId").orEmpty()

    private val _state = MutableStateFlow(GuessUiState())
    val state: StateFlow<GuessUiState> = _state.asStateFlow()

    /** The words of the link's language, the candidates a guess is checked against and the word is found among. */
    private var candidates: List<String> = emptyList()
    private var timer: Job? = null
    private var drawStartedAt = 0L

    init {
        load()
    }

    fun load() {
        _state.value = GuessUiState()
        if (legalConsentStore.childMode.value) {
            _state.value = GuessUiState(phase = GuessPhase.Unavailable, problem = GuessProblem.Child)
            return
        }
        if (!GuessCodec.isValidId(linkId)) {
            _state.value = GuessUiState(phase = GuessPhase.Unavailable, problem = GuessProblem.NotFound)
            return
        }
        viewModelScope.launch {
            val result = repository.getLink(linkId)
            val link = result.getOrNull()
            when {
                result.isFailure -> _state.value = GuessUiState(phase = GuessPhase.Unavailable, problem = GuessProblem.Offline)
                link == null -> _state.value = GuessUiState(phase = GuessPhase.Unavailable, problem = GuessProblem.NotFound)
                link.ownerUid == repository.currentUid -> _state.value = GuessUiState(phase = GuessPhase.Unavailable, problem = GuessProblem.Own, link = link)
                else -> {
                    candidates = withContext(Dispatchers.Default) { wordsOf(link.lang) }
                    _state.value = GuessUiState(phase = GuessPhase.Guessing, link = link)
                }
            }
        }
    }

    private fun wordsOf(lang: String): List<String> = runCatching {
        WordSeeder.loadFromAssets(context, WordSeeder.assetFileFor(lang)).map { it.text }
    }.getOrDefault(emptyList())

    fun onReplayFinished() = _state.update { it.copy(replayDone = true) }

    fun replay() = _state.update { it.copy(replayDone = false, playToken = it.playToken + 1) }

    fun onInput(text: String) = _state.update { it.copy(input = text.take(40), missed = false) }

    fun submitGuess() {
        val s = _state.value
        val link = s.link ?: return
        if (s.phase != GuessPhase.Guessing || s.input.isBlank()) return
        val guess = s.input.trim()
        if (GuessCodec.isCorrect(guess, candidates, link.salt, link.hash)) {
            finish(solved = true, attempts = s.attempts + 1)
        } else {
            val attempts = s.attempts + 1
            if (attempts >= GuessCodec.MAX_ATTEMPTS) {
                _state.update { it.copy(attempts = attempts, wrong = it.wrong + guess, input = "") }
                finish(solved = false, attempts = attempts)
            } else {
                _state.update { it.copy(attempts = attempts, wrong = it.wrong + guess, input = "", missed = true) }
            }
        }
    }

    fun giveUp() {
        val s = _state.value
        if (s.phase != GuessPhase.Guessing) return
        finish(solved = false, attempts = (s.attempts + 1).coerceAtMost(GuessCodec.MAX_ATTEMPTS).coerceAtLeast(1))
    }

    private fun finish(solved: Boolean, attempts: Int) {
        val link = _state.value.link ?: return
        val word = GuessCodec.reveal(candidates, link.salt, link.hash)
        _state.update {
            it.copy(phase = if (solved) GuessPhase.Solved else GuessPhase.Failed, solved = solved, attempts = attempts, word = word, missed = false)
        }
        viewModelScope.launch {
            repository.sendResult(link, solved, attempts, settingsRepository.nicknameOrDefault)
        }
    }

    /** An ad (never in the middle of a drawing or a guess) on the way out of the result, then [then]. */
    fun withInterstitial(activity: Activity?, then: () -> Unit) {
        if (activity == null) {
            then()
            return
        }
        adManager.maybeShowInterstitial(
            activity,
            onDismissed = then,
            force = true,
            forceMinGapMillis = AD_MIN_GAP_MILLIS,
            placement = "interstitial_guess"
        )
    }

    // ---- sıra sende -----------------------------------------------------------------------------------------------

    fun startDrawing() {
        if (_state.value.word == null) return
        _state.update { it.copy(phase = GuessPhase.Drawing, live = emptyList(), secondsLeft = DRAW_SECONDS, busy = false, newLinkId = null) }
        drawStartedAt = System.currentTimeMillis()
        timer?.cancel()
        timer = viewModelScope.launch {
            repeat(DRAW_SECONDS) {
                delay(1_000)
                _state.update { s -> if (s.phase == GuessPhase.Drawing) s.copy(secondsLeft = (s.secondsLeft - 1).coerceAtLeast(0)) else s }
            }
            finishDrawing()
        }
    }

    fun onStroke(stroke: DrawingStroke) {
        if (_state.value.phase != GuessPhase.Drawing || stroke.isEmpty()) return
        _state.update { it.copy(live = it.live + listOf(stroke)) }
    }

    fun undoStroke() = _state.update { if (it.phase == GuessPhase.Drawing) it.copy(live = it.live.dropLast(1)) else it }

    fun finishDrawing() {
        val s = _state.value
        if (s.phase != GuessPhase.Drawing || s.busy) return
        timer?.cancel()
        val word = s.word ?: return
        val link = s.link ?: return
        if (s.live.none { it.isNotEmpty() }) {
            // an empty page is not worth a link: back to the verdict
            _state.update { it.copy(phase = if (it.solved) GuessPhase.Solved else GuessPhase.Failed) }
            return
        }
        _state.update { it.copy(busy = true) }
        val seconds = ((System.currentTimeMillis() - drawStartedAt) / 1000).toInt().coerceIn(1, DRAW_SECONDS)
        viewModelScope.launch {
            val result = repository.createLink(
                word = word,
                strokes = s.live,
                seconds = seconds,
                lang = link.lang,
                ownerName = settingsRepository.nicknameOrDefault
            )
            result.onSuccess { id ->
                _state.update { it.copy(phase = GuessPhase.Shared, busy = false, newLinkId = id, shareFailed = false) }
            }.onFailure { e ->
                _state.update {
                    it.copy(
                        phase = if (it.solved) GuessPhase.Solved else GuessPhase.Failed,
                        busy = false,
                        limitReached = e is GuessRepository.LimitReachedException,
                        shareFailed = e !is GuessRepository.LimitReachedException
                    )
                }
            }
        }
    }

    /** Sends the sender of this drawing to the reviewer (the developer panel's player reports). One per player and link. */
    fun report() {
        val link = _state.value.link ?: return
        _state.update { it.copy(reported = true) }
        viewModelScope.launch {
            playerReporter.report(link.ownerUid, link.ownerName.ifBlank { "?" }, com.sualtikasifi.cizimhafiza.domain.model.PlayerReportReason.OTHER, "guess ${link.id}")
        }
    }

    fun dismissNotice() = _state.update { it.copy(limitReached = false, shareFailed = false) }

    override fun onCleared() {
        timer?.cancel()
        super.onCleared()
    }

    private companion object {
        /** An ad on the way out of a result, but not closer than this to the previous one. */
        const val AD_MIN_GAP_MILLIS = 90_000L
    }
}
