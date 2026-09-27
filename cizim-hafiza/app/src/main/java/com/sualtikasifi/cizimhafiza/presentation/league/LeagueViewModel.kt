package com.sualtikasifi.cizimhafiza.presentation.league

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sualtikasifi.cizimhafiza.domain.model.GlobalLeagueTable
import com.sualtikasifi.cizimhafiza.domain.model.LeagueReward
import com.sualtikasifi.cizimhafiza.domain.model.LeagueTable
import com.sualtikasifi.cizimhafiza.domain.repository.FriendRepository
import com.sualtikasifi.cizimhafiza.domain.repository.GlobalLeagueRepository
import com.sualtikasifi.cizimhafiza.util.GameConstants
import com.sualtikasifi.cizimhafiza.util.SettingsRepository
import com.sualtikasifi.cizimhafiza.util.LeagueScorePublisher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The two tables the screen switches between — see [LeagueUiState]. */
enum class LeagueTab { Friends, Global }

data class LeagueUiState(
    val tab: LeagueTab = LeagueTab.Friends,
    val table: LeagueTable? = null,
    val isLoading: Boolean = true,
    val global: GlobalLeagueTable? = null,
    val globalLoading: Boolean = false,
    val globalFailed: Boolean = false,
    /**
     * A prize collected on THIS visit, shown once and then dismissed.
     *
     * Distinct from simply having won: the published table carries last
     * month's winners for the whole month, so it is read again on every open.
     * Only the first read actually grants anything (see
     * SettingsRepository.grantLeagueReward), and only that read should
     * celebrate.
     */
    val justWon: LeaguePrize? = null,
    /**
     * The player's own row, always shown pinned under the table with their
     * real computed rank (see resolveOwnRank). Null only when they have not
     * scored yet this period.
     */
    val myGlobalRank: Int? = null,
    val myGlobalEntry: com.sualtikasifi.cizimhafiza.domain.model.LeagueEntry? = null
)

/**
 * What one visit collected: always the monthly gold + XP bonus, plus a
 * cosmetic when this build knows the prize's id. [reward] is null for an id
 * this build does not ship (an old app, or a month with no artwork) — the
 * bonus is still real, so the celebration still fires.
 */
data class LeaguePrize(val reward: LeagueReward?)

@HiltViewModel
class LeagueViewModel @Inject constructor(
    private val friendRepository: FriendRepository,
    private val globalLeagueRepository: GlobalLeagueRepository,
    private val settingsRepository: SettingsRepository,
    private val leagueScorePublisher: LeagueScorePublisher
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeagueUiState())
    val uiState: StateFlow<LeagueUiState> = _uiState.asStateFlow()

    init {
        // The week may have rolled over since this device last opened the
        // app — refreshed here rather than by a scheduled worker, same
        // lazy-on-read reasoning as DailyChallengeRepository.refresh.
        settingsRepository.refreshPeriodXp()

        // A publish is already following this device's XP (see
        // LeagueScorePublisher, started in CizimHafizaApp) — this only asks
        // it not to wait out its debounce, so a table opened seconds after a
        // match does not show a stale row for the player looking at it.
        // Friends see the update next time their own table loads;
        // eventually consistent is fine for a monthly number.
        leagueScorePublisher.publishNow()

        viewModelScope.launch {
            friendRepository.observeLeagueTable()
                .catch { } // a listener retry (see firestoreFlow) is invisible here; the last good table just stays on screen
                .collect { table -> _uiState.update { it.copy(table = table, isLoading = false) } }
        }

        // Loaded even though the friends tab opens first: this is the read
        // that hands over a prize won last month, and a player who never
        // switches tabs should still collect it.
        loadGlobal()
    }

    fun selectTab(tab: LeagueTab) {
        _uiState.update { it.copy(tab = tab) }
        // Only if the first attempt failed — the published table is rebuilt
        // every hour and the repository caches it, so switching tabs is
        // otherwise free.
        if (tab == LeagueTab.Global && _uiState.value.globalFailed) loadGlobal()
    }

    fun refreshGlobal() = loadGlobal(force = true)

    fun dismissPrize() = _uiState.update { it.copy(justWon = null) }

    private fun loadGlobal(force: Boolean = false) {
        if (_uiState.value.globalLoading) return
        _uiState.update { it.copy(globalLoading = true, globalFailed = false) }
        viewModelScope.launch {
            globalLeagueRepository.table(forceRefresh = force)
                .onSuccess { table ->
                    _uiState.update {
                        it.copy(
                            global = table,
                            globalLoading = false,
                            justWon = it.justWon ?: collectPrize(table)
                        )
                    }
                    resolveOwnRank(table)
                }
                .onFailure {
                    _uiState.update { state -> state.copy(globalLoading = false, globalFailed = true) }
                }
        }
    }

    /**
     * Always works out the player's real place — counting every real user
     * AND bot actually ahead of them — rather than trusting a spot they
     * happen to occupy in the published top 25. A weak scorer can still slip
     * into that list early in the month, before the bots have grown much;
     * showing them there as "25." would say they're near the top when
     * hundreds of real players are ahead of them. So the own row is always
     * shown pinned under the table with this computed rank (see
     * LeagueScreen, which also drops the player's own entry out of the
     * rendered list so a bot — or whichever real player actually earned it —
     * occupies that slot instead).
     */
    private fun resolveOwnRank(table: GlobalLeagueTable) {
        val myXp = settingsRepository.periodXp.value
        if (myXp <= 0) {
            _uiState.update { it.copy(myGlobalRank = null, myGlobalEntry = null) }
            return
        }
        val botsAbove = table.table.entries.count { it.isBot && it.periodXp > myXp }
        viewModelScope.launch {
            globalLeagueRepository.myRank(table.periodId, myXp, botsAbove)
                .onSuccess { rank ->
                    val level = com.sualtikasifi.cizimhafiza.domain.model.PlayerLevel.levelForXp(settingsRepository.lifetimeXp.value)
                    val entry = com.sualtikasifi.cizimhafiza.domain.model.LeagueEntry(
                        uid = "me",
                        nickname = settingsRepository.nicknameOrDefault,
                        periodXp = myXp,
                        level = level,
                        frameId = com.sualtikasifi.cizimhafiza.domain.model.AvatarFrame
                            .resolve(settingsRepository.selectedAvatarFrameId.value, level).name,
                        isMe = true
                    )
                    _uiState.update { it.copy(myGlobalRank = rank, myGlobalEntry = entry) }
                }
                // A failed count is not worth a message: the table itself
                // loaded, and the player just does not see a rank line.
                .onFailure { _uiState.update { it.copy(myGlobalRank = null, myGlobalEntry = null) } }
        }
    }

    /**
     * Grants last month's prize if this device won one and has not already
     * been given it, and returns it only when something was actually
     * granted — so the celebration fires once rather than on every open.
     *
     * Granted from the published table because the app was going to read it
     * anyway: a separate per-player award document would be a second read
     * for a prize almost nobody has won.
     */
    private fun collectPrize(table: GlobalLeagueTable): LeaguePrize? {
        if (table.myLastPeriodWin == null) return null
        val period = table.lastPeriod ?: return null
        // ONE gate per MONTH, not per prize id. The bonus used to ride on the
        // prize id being new, but the id can be pinned from the review panel
        // (leaderboards/config) — a pinned id repeats month after month, so
        // from the second month on nobody would have been paid.
        if (!settingsRepository.grantLeagueReward("$PERIOD_KEY_PREFIX${period.periodId}")) return null
        // Ownership of the cosmetic itself is recorded separately; its result
        // no longer decides whether the bonus is due.
        period.rewardId?.let { settingsRepository.grantLeagueReward(it) }
        settingsRepository.earnGold(GameConstants.LEAGUE_MONTHLY_GOLD)
        settingsRepository.addXp(GameConstants.LEAGUE_MONTHLY_XP)
        // Null when this build does not know the id — an older app reading a
        // prize whose artwork it does not ship. The grant still stands, so
        // updating the app later reveals it.
        return LeaguePrize(LeagueReward.find(period.rewardId))
    }

    private companion object {
        const val PERIOD_KEY_PREFIX = "PERIOD:"
    }
}
