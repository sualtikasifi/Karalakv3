package com.sualtikasifi.cizimhafiza.presentation.quickmatch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sualtikasifi.cizimhafiza.data.local.WordPoolSynchronizer
import com.sualtikasifi.cizimhafiza.domain.model.GhostRun
import com.sualtikasifi.cizimhafiza.domain.model.PlayerLevel
import com.sualtikasifi.cizimhafiza.domain.repository.GhostRunRepository
import com.sualtikasifi.cizimhafiza.domain.repository.PenaltyRepository
import com.sualtikasifi.cizimhafiza.domain.usecase.GetWordsByIdsUseCase
import com.sualtikasifi.cizimhafiza.util.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * What the Hızlı Eşleş screen is doing right now.
 *
 * [Empty] and [Failed] are kept apart on purpose even though both end in
 * "no match". "Nobody has left a round behind yet" is a true and temporary
 * fact about a young game, and telling a player that is very different from
 * telling them something went wrong — the first invites them to go and play
 * a round themselves, the second invites them to try again.
 */
sealed interface QuickMatchState {
    data object Searching : QuickMatchState
    data class Found(val opponent: GhostRun, val me: QuickMatchPlayerSnapshot) : QuickMatchState
    data object Empty : QuickMatchState
    data object Failed : QuickMatchState

    /**
     * The account is serving a lockout for repeated rejected rounds — see
     * Moderation.STRIKES_BEFORE_LOCKOUT. Carries the deadline so the screen
     * can say how long is left rather than just refusing.
     */
    data class Locked(val untilMillis: Long) : QuickMatchState
}

/**
 * The player's own identity, captured alongside the opponent the moment a
 * match is found — so the "found" screen can show "you" next to "them"
 * instead of only ever showing the stranger.
 */
data class QuickMatchPlayerSnapshot(
    val nickname: String,
    val level: Int,
    val frameId: String,
    val lifetimeXp: Int,
    val avatarUrl: String = ""
)

@HiltViewModel
class QuickMatchViewModel @Inject constructor(
    private val ghostRunRepository: GhostRunRepository,
    private val getWordsByIdsUseCase: GetWordsByIdsUseCase,
    private val settingsRepository: SettingsRepository,
    private val penaltyRepository: PenaltyRepository,
    private val wordPoolSynchronizer: WordPoolSynchronizer,
    private val avatarPhotoResolver: com.sualtikasifi.cizimhafiza.util.AvatarPhotoResolver
) : ViewModel() {

    private val _state = MutableStateFlow<QuickMatchState>(QuickMatchState.Searching)
    val state: StateFlow<QuickMatchState> = _state.asStateFlow()

    /**
     * Every run this screen has already turned down or shown.
     *
     * Without it the search was able to hand back the same person over and
     * over: the pool is sampled from a random shard pivot, so with only a
     * handful of rounds in a band every attempt lands on the same one or two
     * — which made the retry loop below three identical attempts rather than
     * three chances, and made "Yeni Rakip" frequently present the opponent
     * the player had just declined.
     *
     * A LinkedHashSet, not a plain one, and capped at [SEEN_CAP]: this used
     * to grow for the whole lifetime of the ViewModel with nothing ever
     * removed from it, so a player mashing "Yeni Rakip" long enough in one
     * sitting could exhaust the entire local candidate pool at their level
     * and see nothing but Empty from then on, even though fresh real
     * opponents kept arriving server-side the whole time. Evicting the
     * oldest entry once the cap is hit keeps recently-declined opponents out
     * without that ceiling.
     */
    private val seen = LinkedHashSet<String>()

    private fun rememberSeen(id: String) {
        seen.remove(id) // re-insert at the end, so a repeat stays "recent"
        seen.add(id)
        if (seen.size > SEEN_CAP) seen.remove(seen.first())
    }

    init {
        _state.value = QuickMatchState.Found(
            GhostRun("x", "u", "Hemsiremelisa", 54, "default", listOf(1, 2), 10, 5, null),
            QuickMatchPlayerSnapshot("Avcution", 41, "default", 43053)
        )
    }

    /** Remembers this opponent's words so the next search can prefer fresh ones. Called when the match actually starts. */
    fun onMatchStarted(opponent: GhostRun) = settingsRepository.rememberQuickMatchWords(opponent.wordIds)

    /** The player walked away from a match they had been given: costs XP (see GameConstants.QUICK_MATCH_ABANDON_PENALTY_XP). */
    fun abandonMatch() = settingsRepository.applyQuickMatchAbandonPenalty()

    fun search() {
        _state.value = QuickMatchState.Searching
        viewModelScope.launch {
            // Before anything else touches WordDao: a search that ran ahead
            // of a still-finishing language reseed used to read whichever
            // rows Room happened to hold at that instant — an English player
            // could get a Turkish word under a shared id. See
            // WordPoolSynchronizer.ensureSynced.
            wordPoolSynchronizer.ensureSynced()
            // Checked here rather than by hiding the button: the lockout is a
            // consequence the player is meant to understand, and a tile that
            // silently does nothing reads as a broken app.
            penaltyRepository.lockedUntilMillis()?.let { until ->
                _state.value = QuickMatchState.Locked(until)
                return@launch
            }
            // A floor under the search, not a delay added to it.
            //
            // Finding somebody takes a few hundred milliseconds, and an
            // opponent who appears the instant the button is pressed does not
            // read as a person who was found — it reads as one who was
            // waiting. The wait runs alongside the search rather than after
            // it, so a slow lookup costs nothing extra.
            val floor = async { delay(Random.nextLong(MIN_SEARCH_MS, MAX_SEARCH_MS + 1)) }

            val myXp = settingsRepository.lifetimeXp.value
            val level = PlayerLevel.levelForXp(myXp)
            val me = QuickMatchPlayerSnapshot(
                nickname = settingsRepository.nicknameOrDefault,
                level = level,
                frameId = settingsRepository.selectedAvatarFrameId.value,
                lifetimeXp = myXp,
                avatarUrl = avatarPhotoResolver.currentUrl()
            )
            // Prefer an opponent whose words the player has NOT played lately,
            // so Hızlı Eşleş does not keep dealing the same handful of words.
            // This only ever chooses AMONG opponents the search already
            // offered (never changes who can be matched, never refuses a
            // match): the first one with few repeats is taken at once, and
            // if every candidate repeats, the least-repeating one is used.
            val recent = settingsRepository.recentQuickMatchWords()
            var best: GhostRun? = null
            var bestRepeats = Int.MAX_VALUE
            for (attempt in 0 until MAX_ATTEMPTS) {
                val result = ghostRunRepository.findOpponent(level, seen)
                val opponent = result.getOrElse {
                    if (best != null) break
                    floor.await()
                    _state.value = QuickMatchState.Failed
                    return@launch
                } ?: run {
                    if (best != null) break
                    floor.await()
                    _state.value = QuickMatchState.Empty
                    return@launch
                }
                rememberSeen(opponent.id)
                if (!isPlayable(opponent)) continue
                val repeats = opponent.wordIds.count { it in recent }
                if (repeats < bestRepeats) {
                    best = opponent
                    bestRepeats = repeats
                }
                // At most one word in five already seen: fresh enough.
                if (repeats * 5 <= opponent.wordIds.size) break
            }
            if (best != null) {
                floor.await()
                _state.value = QuickMatchState.Found(best, me)
                return@launch
            }
            // Every candidate offered was unplayable here. Rare enough to be
            // worth no explanation of its own, and honest: there was no match
            // to be had.
            floor.await()
            _state.value = QuickMatchState.Empty
        }
    }

    /**
     * Whether this device can actually deal every one of the opponent's words.
     *
     * A word pool is versioned and a language's pool deliberately withholds
     * entries that do not translate, so a recorded round can name a word this
     * build has no copy of. Dealing a word short against a full round would
     * quietly rig the score, so such a candidate is skipped rather than
     * played — checked here, before the match is offered, because this is the
     * last point where trying somebody else is still free.
     */
    private suspend fun isPlayable(opponent: GhostRun): Boolean =
        runCatching { getWordsByIdsUseCase(opponent.wordIds).size == opponent.wordIds.size }
            .getOrDefault(false)

    private companion object {
        const val MAX_ATTEMPTS = 4

        /** How long "Rakip aranıyor" is shown at minimum, in millis. */
        const val MIN_SEARCH_MS = 2_000L
        const val MAX_SEARCH_MS = 5_000L

        /** How many recently-shown opponents [seen] remembers before it starts forgetting the oldest. */
        const val SEEN_CAP = 40
    }
}
