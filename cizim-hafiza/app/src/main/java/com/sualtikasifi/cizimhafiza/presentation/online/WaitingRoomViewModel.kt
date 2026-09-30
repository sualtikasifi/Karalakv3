package com.sualtikasifi.cizimhafiza.presentation.online

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sualtikasifi.cizimhafiza.data.bot.BotRoomEngine
import com.sualtikasifi.cizimhafiza.domain.model.OnlineRoom
import com.sualtikasifi.cizimhafiza.domain.model.Reaction
import com.sualtikasifi.cizimhafiza.domain.model.RoomStatus
import com.sualtikasifi.cizimhafiza.domain.repository.OnlineGameRepository
import com.sualtikasifi.cizimhafiza.domain.usecase.GetWordsByIdsUseCase
import com.sualtikasifi.cizimhafiza.domain.usecase.GetWordsForGameUseCase
import com.sualtikasifi.cizimhafiza.util.GameConstants
import com.sualtikasifi.cizimhafiza.domain.model.Friend
import com.sualtikasifi.cizimhafiza.domain.model.InviteEligibility
import com.sualtikasifi.cizimhafiza.domain.repository.FriendRepository
import com.sualtikasifi.cizimhafiza.util.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.util.UiText

data class WaitingRoomUiState(
    val room: OnlineRoom? = null,
    val isStarting: Boolean = false,
    val errorMessage: UiText? = null,
    val reactions: List<Reaction> = emptyList(),
    // Rough estimate of the in-progress round's total length (drawing +
    // guessing for every word), for a pendingNextRound joiner sitting in
    // the lobby — see room.startedAt and WaitingRoomScreen's countdown.
    // Null until the shared word list resolves to local Word objects.
    val estimatedRoundSeconds: Int? = null
)

@HiltViewModel
class WaitingRoomViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getWordsForGameUseCase: GetWordsForGameUseCase,
    private val getWordsByIdsUseCase: GetWordsByIdsUseCase,
    private val onlineGameRepository: OnlineGameRepository,
    private val settingsRepository: SettingsRepository,
    private val friendRepository: FriendRepository,
    botRoomEngine: BotRoomEngine
) : ViewModel() {

    /**
     * The player's friends, for the invite sheet an empty seat opens.
     *
     * Distinct from FriendsScreen's own invite (which creates a fresh room):
     * this one pulls someone into the room already open, which is the whole
     * point of asking from inside the lobby.
     */
    val friends: StateFlow<List<Friend>> = friendRepository.observeFriends()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _inviteState = MutableStateFlow(LobbyInviteState())
    val inviteState: StateFlow<LobbyInviteState> = _inviteState.asStateFlow()

    fun consumeInviteMessage() = _inviteState.update { it.copy(message = null) }

    /** Invites [friend] into THIS room — see [friends]. */
    fun inviteFriendToRoom(friend: Friend) {
        if (_inviteState.value.sendingToUid != null) return
        val nickname = settingsRepository.nicknameOrDefault
        _inviteState.update { it.copy(sendingToUid = friend.uid, message = null) }
        viewModelScope.launch {
            // Same UX-only pre-check as FriendsViewModel.inviteFriend: the
            // real gate is firestore.rules' invites-create rule, but checking
            // first turns a silent rejection into a specific reason.
            var sent = false
            val message = when (val eligibility = friendRepository.canInvite(friend.uid)) {
                InviteEligibility.Blocked -> UiText.of(R.string.error_invite_blocked)
                is InviteEligibility.OnCooldown ->
                    ((eligibility.remainingMillis / 60_000L) + 1).let { minutes ->
                        UiText.of(if (minutes == 1L) R.string.error_invite_cooldown_one else R.string.error_invite_cooldown, minutes)
                    }
                InviteEligibility.Eligible -> friendRepository
                    .sendMatchInvite(friend.uid, roomCode, nickname)
                    .fold(
                        onSuccess = { sent = true; UiText.of(R.string.info_invite_sent) },
                        onFailure = { UiText.of(R.string.error_lobby_invite_failed) }
                    )
            }
            _inviteState.update {
                it.copy(
                    sendingToUid = null,
                    message = message,
                    sentToUids = if (sent) it.sentToUids + friend.uid else it.sentToUids
                )
            }
        }
    }

    private companion object {
        // Two full beats still fit inside PRESENCE_TIMEOUT_MS (75 s), so a
        // brief network blip does not get anyone dropped. 30 s rather than
        // 20 s: every beat is a write that each player's room listener then
        // downloads, so the cost grows with the square of the lobby size.
        const val PRESENCE_HEARTBEAT_MS = 30_000L

        /** startGame()'s fallback: how long to wait for the room to flip to PLAYING before giving the "Başlat" button back. */
        const val START_GAME_TIMEOUT_MS = 12_000L
    }

    val roomCode: String = checkNotNull(savedStateHandle["roomCode"])
    val myUid: String? get() = onlineGameRepository.currentUid

    /** How often each preset chat phrase has actually been sent from this device — see ReactionSendRow's usage-sorted "Bir şey söyle" sheet. */
    val phraseUsageCounts: StateFlow<Map<String, Int>> = settingsRepository.phraseUsageCounts

    /** Same idea, for the quick-send emoji row's 5 usage-sorted slots. */
    val emojiUsageCounts: StateFlow<Map<String, Int>> = settingsRepository.emojiUsageCounts

    private val _uiState = MutableStateFlow(WaitingRoomUiState())
    val uiState: StateFlow<WaitingRoomUiState> = _uiState.asStateFlow()

    init {
        // Harmless/no-op for any other room — only ever drives room 130246
        // (see BotRoomEngine) and only starts its listener once per process.
        botRoomEngine.ensureRunning()
        // Both Flows close with an exception on a Firestore listener error
        // (see OnlineGameRepositoryImpl.observeRoom/observeReactions) —
        // catching keeps that from crashing the app. The room listener now
        // says so instead of failing silently: a lobby frozen on its last
        // good snapshot is indistinguishable from a quiet one, and players
        // sat waiting for a match that could never start. Reactions stay
        // silent by design — a lost emoji is not worth an error banner.
        viewModelScope.launch {
            onlineGameRepository.observeRoom(roomCode)
                .catch { _uiState.update { it.copy(errorMessage = UiText.of(R.string.error_room_listener_failed)) } }
                .collect { room -> _uiState.update { it.copy(room = room, errorMessage = null) } }
        }
        viewModelScope.launch {
            onlineGameRepository.observeReactions(roomCode)
                .catch { }
                .collect { reactions -> _uiState.update { it.copy(reactions = reactions) } }
        }
        // Presence heartbeat. Without it a lobby can't tell a player who
        // force-closed the app from one who's still sitting there — the stale
        // entry shows up as a phantom player AND, being permanently
        // un-ready, blocks the match from ever starting. See
        // OnlinePlayer.isPresent.
        viewModelScope.launch {
            while (true) {
                runCatching {
                    // Another device's cleanup may have pruned this one while
                    // it was offline; rejoining is friendlier than silently
                    // vanishing from a lobby the player is still looking at.
                    //
                    // Decided from the room snapshot the lobby is ALREADY
                    // listening to, not from a fresh document read: that read
                    // used to run every beat for every player, and it told us
                    // nothing the live listener had not. Until the first
                    // snapshot arrives (room == null) the player is assumed
                    // to be in.
                    val room = _uiState.value.room
                    val me = myUid
                    if (room == null || me == null || room.players.any { it.uid == me }) {
                        onlineGameRepository.touchPresence(roomCode)
                    } else {
                        val nickname = settingsRepository.nicknameOrDefault
                        onlineGameRepository.joinRoom(roomCode, nickname)
                    }
                }
                delay(PRESENCE_HEARTBEAT_MS)
            }
        }
        // Resolves the shared wordIds list to local Word objects (same ids,
        // same words on every device — see GetWordsByIdsUseCase) just to sum
        // up their drawing durations; only worth doing once per round, not
        // on every room snapshot, hence distinctUntilChanged on the ids.
        viewModelScope.launch {
            uiState.map { it.room?.wordIds ?: emptyList() }
                .distinctUntilChanged()
                .collect { wordIds ->
                    val estimate = if (wordIds.isEmpty()) {
                        null
                    } else {
                        runCatching { getWordsByIdsUseCase(wordIds) }.getOrDefault(emptyList())
                            .sumOf { GameConstants.drawingDurationSeconds(it.difficulty) + GameConstants.GUESS_DURATION_SECONDS }
                    }
                    _uiState.update { it.copy(estimatedRoundSeconds = estimate) }
                }
        }
    }

    // Fire-and-forget Firestore writes (reaction, ready toggle, leave below):
    // wrapped in runCatching, not left to throw, because a transient network
    // failure here would otherwise crash the whole app mid-match — these are
    // all safe to just silently fail and let the player retry the tap; there
    // is no local state to roll back since none was optimistically applied.
    fun sendReaction(emoji: String, messageKey: String) {
        viewModelScope.launch { runCatching { onlineGameRepository.sendReaction(roomCode, emoji, messageKey) } }
    }

    fun toggleReady() {
        val amReady = _uiState.value.room?.players?.find { it.uid == myUid }?.ready ?: false
        viewModelScope.launch { runCatching { onlineGameRepository.setReady(roomCode, !amReady) } }
    }

    /** 2v2 rooms only (see OnlineRoom.teamMode) — self-service team switch, shown on the player's own slot. */
    fun switchTeam(teamId: String) {
        viewModelScope.launch { runCatching { onlineGameRepository.setTeam(roomCode, teamId) } }
    }

    /** Host-only: locks in the shared word list (same words for both players) and starts the match. */
    fun startGame() {
        val room = _uiState.value.room ?: return
        if (room.hostUid != myUid || _uiState.value.isStarting) return
        _uiState.update { it.copy(isStarting = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching {
                val words = getWordsForGameUseCase(room.wordCount, room.category, room.difficulty)
                onlineGameRepository.startGame(roomCode, words.map { it.id })
            }.onFailure {
                _uiState.update { state -> state.copy(isStarting = false, errorMessage = UiText.of(R.string.error_game_start_failed)) }
            }
        }
        // A success above only queues the Firestore write — the actual
        // "Başlat" → navigate-away happens when the room observer sees
        // status flip to PLAYING (see WaitingRoomScreen's LaunchedEffect).
        // If that update never arrives (a dropped listener, a rules
        // rejection the write itself didn't surface as a failure) isStarting
        // had no other path back to false: the host was left staring at a
        // permanently disabled button with no way to retry. This timeout is
        // that way back — harmless if the room really did start in time,
        // since this screen will have already navigated away by then and
        // the update below lands on a cleared ViewModel.
        viewModelScope.launch {
            delay(START_GAME_TIMEOUT_MS)
            if (_uiState.value.isStarting) {
                _uiState.update { state -> state.copy(isStarting = false, errorMessage = UiText.of(R.string.error_game_start_failed)) }
            }
        }
    }

    fun leaveRoom() {
        viewModelScope.launch { runCatching { onlineGameRepository.leaveRoom(roomCode) } }
    }

    /** Host-only (WaitingRoomScreen only shows the button when isHost) — removes and 30-minute-bans a player. */
    fun kickPlayer(targetUid: String, targetDisplayName: String) {
        viewModelScope.launch { runCatching { onlineGameRepository.kickPlayer(roomCode, targetUid, targetDisplayName) } }
    }

    /** Host-only — lifts an active kick ban early. */
    fun unbanPlayer(targetUid: String) {
        viewModelScope.launch { runCatching { onlineGameRepository.unbanPlayer(roomCode, targetUid) } }
    }
}

/** In-lobby invite progress and its one-shot result message. */
data class LobbyInviteState(
    val sendingToUid: String? = null,
    val message: UiText? = null,
    /** Friends an invite has actually gone out to — their row says so instead of offering to send again. */
    val sentToUids: Set<String> = emptySet()
)
