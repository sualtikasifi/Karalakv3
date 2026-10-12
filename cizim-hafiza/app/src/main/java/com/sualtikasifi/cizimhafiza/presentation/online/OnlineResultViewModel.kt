package com.sualtikasifi.cizimhafiza.presentation.online

import android.app.Activity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sualtikasifi.cizimhafiza.ads.AdManager
import com.sualtikasifi.cizimhafiza.data.bot.BotRoomEngine
import com.sualtikasifi.cizimhafiza.domain.model.Chest
import com.sualtikasifi.cizimhafiza.domain.model.OnlineRoom
import com.sualtikasifi.cizimhafiza.domain.model.Reaction
import com.sualtikasifi.cizimhafiza.domain.model.ResultItem
import com.sualtikasifi.cizimhafiza.domain.model.DrawingReportReason
import com.sualtikasifi.cizimhafiza.domain.model.RoomStatus
import com.sualtikasifi.cizimhafiza.domain.repository.AuthRepository
import com.sualtikasifi.cizimhafiza.domain.repository.OnlineGameRepository
import com.sualtikasifi.cizimhafiza.domain.repository.DrawingReportRepository
import com.sualtikasifi.cizimhafiza.presentation.common.ReportSendState
import com.sualtikasifi.cizimhafiza.domain.usecase.GetWordsForGameUseCase
import com.sualtikasifi.cizimhafiza.domain.usecase.SaveOnlineGameSessionUseCase
import com.sualtikasifi.cizimhafiza.presentation.navigation.Screen
import com.sualtikasifi.cizimhafiza.util.PostMatchPrompts
import com.sualtikasifi.cizimhafiza.util.GameConstants
import com.sualtikasifi.cizimhafiza.domain.model.XpAwards
import com.sualtikasifi.cizimhafiza.util.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnlineResultUiState(
    val room: OnlineRoom? = null,
    val itemsByUid: Map<String, List<ResultItem>> = emptyMap(),
    val selectedUid: String? = null,
    val reportState: ReportSendState = ReportSendState.Idle,
    val isLoadingItems: Boolean = true,
    val rematchRequested: Boolean = false,
    val navigateToRematchRoomCode: String? = null,
    // Set once someone joins mid-round: instead of a normal vote-based
    // rematch, the whole group is about to be forced back to the lobby
    // (see maybeTriggerRematchReset) — the Screen disables "Tekrar Oyna"
    // and shows a message instead while this is true.
    val navigateToWaitingRoomCode: String? = null,
    val rematchBlockedByNewJoiner: Boolean = false,
    val reactions: List<Reaction> = emptyList(),
    // One-shot onboarding nudges — see util/PostMatchPrompts.kt for when each fires.
    val showSignInPrompt: Boolean = false,
    val showRatingPrompt: Boolean = false,
    /** Non-null exactly once, right after a 1st-place finish that found a free chest slot. */
    val chestWon: Chest? = null,
    val chestLost: Boolean = false,
    /** What this round paid out, shown once the standings are in: per-word XP + the match bonus, and gold. */
    val xpEarned: Int = 0,
    val goldEarned: Int = 0,
)

@HiltViewModel
class OnlineResultViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val onlineGameRepository: OnlineGameRepository,
    private val drawingReportRepository: DrawingReportRepository,
    private val getWordsForGameUseCase: GetWordsForGameUseCase,
    private val saveOnlineGameSessionUseCase: SaveOnlineGameSessionUseCase,
    private val adManager: AdManager,
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
    botRoomEngine: BotRoomEngine
) : ViewModel() {

    val roomCode: String = checkNotNull(savedStateHandle[Screen.ArgRoomCode])
    val myUid: String? get() = onlineGameRepository.currentUid

    private val _uiState = MutableStateFlow(OnlineResultUiState())
    val uiState: StateFlow<OnlineResultUiState> = _uiState.asStateFlow()

    private var lastKnownWordIds: List<Int>? = null

    /**
     * Reports the drawing currently open in the preview.
     *
     * A room's drawings vanish with the room, so the strokes travel with the
     * report rather than a pointer to them — see DrawingReport.strokesJson.
     * Reporting yourself is refused rather than hidden: the gallery lets you
     * flip to your own drawings too, and this is the only place that knows
     * which player is selected.
     */
    fun reportSelectedPlayersDrawing(item: ResultItem, reason: DrawingReportReason) {
        val target = _uiState.value.selectedUid ?: return
        if (target == myUid || target == BotRoomEngine.BOT_UID) return
        if (_uiState.value.reportState == ReportSendState.Sending) return
        _uiState.update { it.copy(reportState = ReportSendState.Sending) }
        viewModelScope.launch {
            val sent = drawingReportRepository.reportRoomDrawing(
                roomCode = roomCode,
                reportedUid = target,
                word = item.word,
                strokes = item.strokes,
                reason = reason
            ).isSuccess
            _uiState.update {
                it.copy(reportState = if (sent) ReportSendState.Sent else ReportSendState.Failed)
            }
        }
    }

    fun dismissReport() {
        _uiState.update { it.copy(reportState = ReportSendState.Idle) }
    }

    /** RatingPromptDialog's "Puanla" tap — see SettingsRepository.grantRatingBonusXpOnce for why this is safe to call more than once. */
    fun grantRatingBonusXp() {
        settingsRepository.grantRatingBonusXpOnce(PostMatchPrompts.RATING_BONUS_XP)
    }

    private companion object {
        /** Extra passes for a player whose drawings had not landed yet — see loadItems. */
        const val ITEM_FETCH_ATTEMPTS = 25
        const val ITEM_FETCH_RETRY_DELAY_MS = 500L
    }
    private var hasTriggeredRematchReset = false
    private var hasLoadedItems = false
    private val requestedUids = mutableSetOf<String>()

    init {
        // Harmless/no-op for any other room — only ever drives room 130246
        // (see BotRoomEngine) and only starts its listener once per process.
        botRoomEngine.ensureRunning()
        // Both Flows close with an exception on a Firestore listener error
        // (see OnlineGameRepositoryImpl.observeRoom/observeReactions) —
        // .catch{} keeps that from crashing the app; the screen just stops
        // updating until the listener recovers, same as a brief network drop.
        viewModelScope.launch {
            onlineGameRepository.observeRoom(roomCode).catch { }.collect { room ->
                _uiState.update { it.copy(room = room, selectedUid = it.selectedUid ?: myUid) }
                if (room == null) return@collect

                // Forced back to the lobby elsewhere (a joiner appeared
                // mid-round — see maybeTriggerRematchReset/
                // returnToWaitingRoom): whichever device's transaction won
                // the race, every device on this screen reacts the same way
                // once it observes the resulting WAITING status.
                if (room.status == RoomStatus.WAITING) {
                    _uiState.update { it.copy(navigateToWaitingRoomCode = roomCode) }
                    return@collect
                }

                val previousWordIds = lastKnownWordIds
                // A rematch was reset elsewhere (by the host): the room went
                // back to PLAYING with a fresh word list — jump back into a
                // new match instead of staying on this finished-round screen.
                if (room.status == RoomStatus.PLAYING &&
                    previousWordIds != null &&
                    room.wordIds != previousWordIds
                ) {
                    _uiState.update { it.copy(navigateToRematchRoomCode = roomCode) }
                    return@collect
                }
                lastKnownWordIds = room.wordIds

                // pendingNextRound players (joined mid-round) never finish
                // THIS round, and a player who quit (left=true) never will
                // either — both excluded here the same way submitResult()
                // excludes them when deciding the room is FINISHED. Without
                // the left check, a departed player's finished=false entry
                // would keep this screen's "everyone's done" condition (and
                // the rematch-vote count below) permanently unsatisfiable.
                val activePlayers = room.players.filterNot { it.pendingNextRound || it.left }
                // Each player's drawings are fetched the moment THEY are
                // finished — not once everyone is. submitResult writes the
                // drawings before the finished flag, so the flag is the
                // promise they are there; waiting for the slowest player
                // before even starting made every gallery appear late.
                room.players.filter { it.finished && it.uid !in requestedUids }.forEach { player ->
                    requestedUids += player.uid
                    loadPlayerItems(player.uid)
                }
                // Counted over everyone who started the round, so an opponent who quit half-way does not cost the
                // players who finished it their match reward.
                val started = room.players.filterNot { it.pendingNextRound }
                if (!hasLoadedItems && started.size >= 2 && activePlayers.isNotEmpty() && activePlayers.all { it.finished }) {
                    hasLoadedItems = true
                    recordSession(room, myUid)
                }

                _uiState.update { it.copy(rematchBlockedByNewJoiner = room.players.any { p -> p.pendingNextRound }) }
                maybeTriggerRematchReset(room)
            }
        }
        viewModelScope.launch {
            onlineGameRepository.observeReactions(roomCode).catch { }.collect { reactions ->
                _uiState.update { it.copy(reactions = reactions) }
            }
        }
    }

    /**
     * Fetches one player's drawings, retrying until they arrive.
     *
     * A single failed or too-early read used to leave that player's gallery
     * blank for the whole screen. An empty result is never a real answer —
     * every round has one item per word — so it is simply retried, with
     * growing pauses, for about a minute. Drawings that already arrived are
     * never replaced.
     */
    private fun loadPlayerItems(uid: String) {
        viewModelScope.launch {
            var attempt = 0
            while (attempt < ITEM_FETCH_ATTEMPTS) {
                val items = runCatching { onlineGameRepository.getPlayerResultItems(roomCode, uid) }
                    .getOrDefault(emptyList())
                if (items.isNotEmpty()) {
                    _uiState.update {
                        it.copy(itemsByUid = it.itemsByUid + (uid to items))
                    }
                    break
                }
                attempt++
                delay(ITEM_FETCH_RETRY_DELAY_MS * attempt.coerceAtMost(6))
            }
            if (uid == myUid || attempt >= ITEM_FETCH_ATTEMPTS) {
                _uiState.update { it.copy(isLoadingItems = false) }
            }
        }
    }

    private fun recordSession(room: OnlineRoom, myUidLocal: String?) {
        val me = room.players.find { it.uid == myUidLocal }
        viewModelScope.launch {
            // Recorded once per finished round (guarded by hasLoadedItems —
            // a rematch gets a brand new OnlineResultViewModel next round).
            //
            // Ranked over the players who actually PLAYED this round, not
            // room.players: a pendingNextRound joiner sat the round out in the
            // lobby and still carries a fresh totalScore of 0, so counting
            // them would inflate playerCount and hand the player a better
            // placement than they earned ("2nd of 3" for a two-player round).
            if (me != null) {
                val roundPlayers = room.players.filterNot { it.pendingNextRound || it.left }
                val ranked = roundPlayers.sortedByDescending { it.totalScore }
                val placement = ranked.indexOfFirst { it.uid == myUidLocal } + 1
                if (placement > 0) {
                    // Return value (newly unlocked achievements) isn't needed
                    // here — see GameViewModel.finishGame's matching comment.
                    val newlyUnlocked = saveOnlineGameSessionUseCase(
                        totalScore = me.totalScore,
                        wordCount = room.wordCount,
                        correctCount = me.correctCount,
                        fastestCorrectMs = me.fastestCorrectMs,
                        placement = placement,
                        playerCount = roundPlayers.size
                    )
                    // Chests are earned ONLY here — a won real online-room
                    // match — never from solo play, Hızlı Eşleş or the daily
                    // challenge (see SettingsRepository.awardChestForWin).
                    // A tie for first is nobody's win: no win gold, XP or chest for any of the tied players.
                    val tiedForFirst = placement == 1 && ranked.count { it.totalScore == me.totalScore } > 1
                    val won = placement == 1 && !tiedForFirst && roundPlayers.size >= 2
                    val gold = me.correctCount * GameConstants.GOLD_PER_CORRECT_WORD +
                        (if (won) GameConstants.ONLINE_WIN_GOLD else 0) +
                        newlyUnlocked.size * GameConstants.ACHIEVEMENT_GOLD
                    if (gold > 0) settingsRepository.earnGold(gold)
                    val roundXp = settingsRepository.lastOnlineRoundXp
                    val matchXp = XpAwards.ONLINE_MATCH + if (won) XpAwards.ONLINE_WIN else 0
                    settingsRepository.lastOnlineRoundXp = 0
                    val chestAward = if (placement == 1 && !tiedForFirst) settingsRepository.awardChestForWinOrPay() else null
                    val chestWon = chestAward?.chest
                    val chestLost = placement == 1 && !tiedForFirst && chestWon == null
                    _uiState.update {
                        it.copy(
                            showSignInPrompt = PostMatchPrompts.shouldShowSignIn(settingsRepository, authRepository.authState.value),
                            showRatingPrompt = false,
                            chestWon = chestWon,
                            chestLost = chestLost,
                            xpEarned = roundXp + matchXp,
                            goldEarned = gold
                        )
                    }
                }
            }
        }
    }

    /** Called once when the finished-round comparison is actually showing — see AdManager's placement doc. */
    fun showInterstitial(activity: Activity, onDismissed: () -> Unit = {}) {
        adManager.maybeShowInterstitial(activity, onDismissed, placement = "interstitial_result_online")
    }

    fun selectPlayer(uid: String) {
        _uiState.update { it.copy(selectedUid = uid) }
    }

    // Either player can trigger this — not just the host — so a rematch
    // isn't stuck forever if the host happened to leave this screen first.
    // resetForRematch()/returnToWaitingRoom() are Firestore transactions,
    // so if multiple clients race to call one at once only one applies.
    private fun maybeTriggerRematchReset(room: OnlineRoom) {
        if (hasTriggeredRematchReset) return
        if (room.status != RoomStatus.FINISHED) return
        val activePlayers = room.players.filterNot { it.pendingNextRound || it.left }
        if (activePlayers.size < 2) return

        // Someone joined mid-round: skip the normal vote entirely and force
        // everyone back to a fresh lobby together instead of an instant
        // rematch — see OnlineGameRepository.returnToWaitingRoom. Not
        // gated on this call's own success: if another client's own
        // transaction wins the race instead, this device's room observer
        // (see the collect block above) reacts to the resulting WAITING
        // status the same way regardless of which device caused it.
        if (room.players.any { it.pendingNextRound }) {
            hasTriggeredRematchReset = true
            viewModelScope.launch { runCatching { onlineGameRepository.returnToWaitingRoom(roomCode) } }
            return
        }

        // Require every currently-listed player to vote yes — the direct
        // generalization of the old "both players vote" rule. Counted as a
        // set intersection, not a bare size comparison: rematchVotes is
        // never pruned when a player leaves, so a stale vote from someone
        // who has since left could otherwise inflate the count past
        // activePlayers.size while a real active player never voted at all.
        val activeVotes = room.rematchVotes.intersect(activePlayers.map { it.uid }.toSet())
        if (activeVotes.size < activePlayers.size) return
        hasTriggeredRematchReset = true
        viewModelScope.launch {
            // Fire-and-forget: a failure here just means the rematch reset
            // doesn't happen yet — the other client racing to call the same
            // transaction (see the class doc above) can still succeed, and
            // this ViewModel's own room observer will retry via the next
            // room update either way. Must not crash on a network blip.
            runCatching {
                val words = getWordsForGameUseCase(
                    room.wordCount,
                    com.sualtikasifi.cizimhafiza.domain.model.WordFilter.decodeCategories(room.category),
                    room.difficulties
                )
                onlineGameRepository.resetForRematch(roomCode, words.map { it.id })
            }
        }
    }

    fun requestRematch() {
        if (_uiState.value.rematchRequested || _uiState.value.rematchBlockedByNewJoiner) return
        _uiState.update { it.copy(rematchRequested = true) }
        viewModelScope.launch { runCatching { onlineGameRepository.voteRematch(roomCode) } }
    }

    fun sendReaction(emoji: String, messageKey: String) {
        viewModelScope.launch { runCatching { onlineGameRepository.sendReaction(roomCode, emoji, messageKey) } }
    }
}
