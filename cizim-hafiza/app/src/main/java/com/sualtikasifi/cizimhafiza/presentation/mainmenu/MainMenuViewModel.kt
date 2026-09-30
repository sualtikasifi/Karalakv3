package com.sualtikasifi.cizimhafiza.presentation.mainmenu

import com.sualtikasifi.cizimhafiza.domain.model.AvatarFrame
import com.sualtikasifi.cizimhafiza.domain.model.LevelProgressState
import com.sualtikasifi.cizimhafiza.domain.model.LeagueReward
import com.sualtikasifi.cizimhafiza.domain.model.PenSkin
import com.sualtikasifi.cizimhafiza.util.DailyChallengeRepository
import com.sualtikasifi.cizimhafiza.util.DailyChallengeState
import com.sualtikasifi.cizimhafiza.util.SettingsRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.app.Activity
import com.sualtikasifi.cizimhafiza.ads.AdManager
import com.sualtikasifi.cizimhafiza.ads.RewardedOutcome
import com.sualtikasifi.cizimhafiza.data.local.dao.AchievementDao
import com.sualtikasifi.cizimhafiza.domain.model.Penalty
import com.sualtikasifi.cizimhafiza.domain.model.DuelStatus
import com.sualtikasifi.cizimhafiza.domain.repository.DuelRepository
import com.sualtikasifi.cizimhafiza.domain.repository.FriendRepository
import com.sualtikasifi.cizimhafiza.domain.repository.PenaltyRepository
import com.sualtikasifi.cizimhafiza.util.ReferralRewardClaimer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One avatar frame paired with whether the current level has unlocked it and whether it's the active pick. */
data class AvatarFrameUiItem(val frame: AvatarFrame, val unlocked: Boolean, val selected: Boolean)

data class PenSkinUiItem(val skin: PenSkin, val unlocked: Boolean, val selected: Boolean)

/**
 * Whether this cosmetic is actually available to wear.
 *
 * A league prize is NOT reached by levelling — its unlockLevel is 0 purely
 * so PenSkin/AvatarFrame.resolve will render it for somebody already
 * wearing one (including an opponent, see PenSkin.isLeagueReward). Asking
 * the level about it would hand every league prize to every player at level
 * one, which is the exact opposite of what a prize is.
 */
private fun PenSkin.isUnlockedBy(level: Int, earnedRewardIds: Set<String>, owned: Set<String>): Boolean =
    if (isStoreItem) "pen:$name" in owned else
    if (isLeagueReward) LeagueReward.Pen(this).id in earnedRewardIds else level >= unlockLevel

private fun AvatarFrame.isUnlockedBy(level: Int, earnedRewardIds: Set<String>, owned: Set<String>): Boolean =
    if (isStoreItem) "frame:$name" in owned else
    if (isLeagueReward) LeagueReward.Frame(this).id in earnedRewardIds else level >= unlockLevel

/** What a finished rewarded streak action should confirm on screen. */
enum class StreakToast { Rescued, AdGoldEarned, AdUnavailable }

@HiltViewModel
class MainMenuViewModel @Inject constructor(
    achievementDao: AchievementDao,
    private val friendRepository: FriendRepository,
    private val duelRepository: DuelRepository,
    private val authRepository: com.sualtikasifi.cizimhafiza.domain.repository.AuthRepository,
    private val dailyChallengeRepository: DailyChallengeRepository,
    private val settingsRepository: SettingsRepository,
    private val penaltyRepository: PenaltyRepository,
    private val adManager: AdManager,
    private val referralRewardClaimer: ReferralRewardClaimer,
    private val usernameRepository: com.sualtikasifi.cizimhafiza.util.UsernameRepository,
    xpEventRepository: com.sualtikasifi.cizimhafiza.domain.repository.XpEventRepository
) : ViewModel() {

    /** The 2x XP event while one is running (null otherwise) — drives the home banner and the Hızlı Eşleş glow. */
    val xpEvent: StateFlow<com.sualtikasifi.cizimhafiza.domain.repository.XpEvent?> = xpEventRepository.live
        .map { event -> event?.takeIf { it.active && System.currentTimeMillis() < it.endsAtMillis } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** A referral reward claimed since app start, for a one-time "kazandın!" toast — see ReferralRewardClaimer. */
    val referralRewardXp: StateFlow<Int> = referralRewardClaimer.lastClaimedXp

    fun consumeReferralRewardNotice() = referralRewardClaimer.consumeClaimedNotice()

    private val _penaltyWarning = MutableStateFlow<Penalty?>(null)

    /**
     * The rejected round the player is about to be told about, or null.
     *
     * Applied from the main menu because that is where every session starts:
     * somebody whose round was rejected overnight finds out the moment they
     * open the game, rather than whenever they next happen to visit some
     * particular screen.
     */
    val penaltyWarning: StateFlow<Penalty?> = _penaltyWarning.asStateFlow()

    init {
        viewModelScope.launch {
            // Silent and empty on every ordinary launch, and its failures are
            // swallowed inside the repository: a network hiccup must never
            // stand between a player and their main menu.
            _penaltyWarning.value = penaltyRepository.applyOutstanding().firstOrNull()
        }
    }

    fun dismissPenaltyWarning() { _penaltyWarning.value = null }

    val dailyState: StateFlow<DailyChallengeState> = dailyChallengeRepository.state

    /**
     * A one-shot confirmation for the two rewarded streak actions below, so
     * the menu can say the ad actually paid out. Null once shown.
     */
    private val _streakToast = MutableStateFlow<StreakToast?>(null)
    val streakToast: StateFlow<StreakToast?> = _streakToast.asStateFlow()

    fun consumeStreakToast() { _streakToast.value = null }

    /** When the 500-gold ad button unlocks again (epoch millis; in the past = ready now). */
    val adGoldNextAtMillis: StateFlow<Long> = settingsRepository.adGoldNextAtMillis

    /** Epoch day the free ad chest was last taken; anything but today means it is available. */
    val adChestDay: StateFlow<Long> = settingsRepository.adChestDay

    private val _freeChestReward = MutableStateFlow<com.sualtikasifi.cizimhafiza.domain.model.ChestReward?>(null)
    /** The free ad chest just opened, for the opening scene; cleared by [consumeFreeChestReward]. */
    val freeChestReward: StateFlow<com.sualtikasifi.cizimhafiza.domain.model.ChestReward?> = _freeChestReward.asStateFlow()

    fun consumeFreeChestReward() { _freeChestReward.value = null }

    fun watchAdForGold(activity: Activity) {
        if (!settingsRepository.isAdGoldReady()) return
        adManager.maybeShowRewarded(activity, "home_gold") { outcome ->
            when (outcome) {
                RewardedOutcome.EARNED -> if (settingsRepository.claimAdGold()) _streakToast.value = StreakToast.AdGoldEarned
                RewardedOutcome.SKIPPED -> Unit
                else -> _streakToast.value = StreakToast.AdUnavailable
            }
        }
    }

    fun watchAdForChest(activity: Activity) {
        if (!settingsRepository.isAdChestReady()) return
        adManager.maybeShowRewarded(activity, "home_free_chest") { outcome ->
            when (outcome) {
                RewardedOutcome.EARNED -> settingsRepository.claimAdChest()?.let { _freeChestReward.value = it }
                RewardedOutcome.SKIPPED -> Unit
                else -> _streakToast.value = StreakToast.AdUnavailable
            }
        }
    }

    /** Dismisses the rescue offer for this app session without spending it. */
    fun dismissRescuePrompt() = dailyChallengeRepository.dismissRescuePrompt()

    /**
     * Keeps a lapsed streak alive in exchange for a watched ad.
     *
     * The banked "streak freeze" this replaced was spent silently and shown
     * nowhere, so players only ever learned it existed by noticing a streak
     * that should have broken hadn't. A single deliberate offer, made at the
     * moment of the break, is both clearer and honest about its price.
     */
    fun rescueStreak(activity: Activity) {
        if (dailyChallengeRepository.state.value.rescuableStreak <= 0) return
        adManager.maybeShowRewarded(activity, "streak_rescue") { outcome ->
            val earned = outcome == RewardedOutcome.EARNED
            // The rescue dialog stays open on an unavailable ad, so the
            // player can simply tap again — no separate notice needed.
            if (earned && dailyChallengeRepository.rescueStreak()) {
                _streakToast.value = StreakToast.Rescued
            }
        }
    }

    /** Display name for the header — falls back to the localized default if none was chosen. */
    val nickname: StateFlow<String> = settingsRepository.nickname
        .map { it.trim().ifBlank { settingsRepository.nicknameOrDefault } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = settingsRepository.nicknameOrDefault
        )

    /** The player's own badge, shown on the menu so the level is always in sight. */
    val levelProgress: StateFlow<LevelProgressState> = settingsRepository.lifetimeXp
        .map { LevelProgressState.forXp(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LevelProgressState.forXp(0)
        )

    /** The player's own chosen ring (see AvatarFrame.resolve) for the menu's badge. */
    val selectedFrame: StateFlow<AvatarFrame> = combine(
        settingsRepository.selectedAvatarFrameId,
        settingsRepository.lifetimeXp
    ) { selectedId, xp -> AvatarFrame.resolve(selectedId, LevelProgressState.forXp(xp).level) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AvatarFrame.DEFAULT
        )

    val goldBalance: StateFlow<Int> = settingsRepository.goldBalance
    val featureTourSeen: Boolean get() = settingsRepository.featureTourSeen
    fun markFeatureTourSeen() { settingsRepository.featureTourSeen = true }
    val nicknameRenameUsed: StateFlow<Boolean> = settingsRepository.nicknameRenameUsed

    init {
        // New players get "Karalak<number>" automatically; nothing is asked on first launch.
        viewModelScope.launch { usernameRepository.ensureUsername() }
    }



    /** The pen the player is drawing with, for the profile bar's "Kalemim" chip. */
    val selectedPen: StateFlow<PenSkin> = combine(
        settingsRepository.selectedPenSkinId,
        settingsRepository.lifetimeXp
    ) { selectedId, xp -> PenSkin.resolve(selectedId, LevelProgressState.forXp(xp).level) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PenSkin.DEFAULT
        )

    /** The full catalog for the frame picker sheet, each paired with whether it's unlocked/currently worn. */
    val avatarFrameItems: StateFlow<List<AvatarFrameUiItem>> = combine(
        settingsRepository.selectedAvatarFrameId,
        levelProgress,
        settingsRepository.earnedLeagueRewardIds,
        settingsRepository.ownedStoreIds
    ) { selectedId, progress, earned, owned ->
        val resolved = AvatarFrame.resolve(selectedId, progress.level)
        AvatarFrame.entries.map {
            AvatarFrameUiItem(
                it,
                unlocked = it.isUnlockedBy(progress.level, earned, owned),
                selected = it == resolved
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AvatarFrame.entries.map { AvatarFrameUiItem(it, unlocked = it == AvatarFrame.DEFAULT, selected = it == AvatarFrame.DEFAULT) }
    )

    /** The Google account picture, when signed in and the account has one. */
    val googlePhotoUrl: StateFlow<String?> = authRepository.authState
        .map { (it as? com.sualtikasifi.cizimhafiza.domain.repository.AuthState.Linked)?.photoUrl?.takeIf { url -> url.isNotBlank() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** "DINO", "GOOGLE" or "" (automatic) — the player's explicit choice, see [avatarPhoto]. */
    val avatarSource: StateFlow<String> = settingsRepository.avatarSource

    /** The picture inside the avatar frame: their Google photo when available and not opted out of, else the mascot. */
    val avatarPhoto: StateFlow<com.sualtikasifi.cizimhafiza.presentation.common.AvatarPhoto> = combine(googlePhotoUrl, settingsRepository.avatarSource) { url, source ->
        when {
            source == "DINO" || url == null -> com.sualtikasifi.cizimhafiza.presentation.common.AvatarPhoto.Dino
            else -> com.sualtikasifi.cizimhafiza.presentation.common.AvatarPhoto.Url(url)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), com.sualtikasifi.cizimhafiza.presentation.common.AvatarPhoto.Dino)

    fun selectAvatarSource(source: String) = settingsRepository.setAvatarSource(source)

    /** Only ever called for a frame [AvatarFrameUiItem.unlocked] — see MainMenuScreen's picker sheet. */
    fun selectAvatarFrame(frame: AvatarFrame) = settingsRepository.setSelectedAvatarFrame(frame)

    /** The pen catalog, same shape as [avatarFrameItems] — see domain.model.PenSkin. */
    val penSkinItems: StateFlow<List<PenSkinUiItem>> = combine(
        settingsRepository.selectedPenSkinId,
        levelProgress,
        settingsRepository.earnedLeagueRewardIds,
        settingsRepository.ownedStoreIds
    ) { selectedId, progress, earned, owned ->
        val resolved = PenSkin.resolve(selectedId, progress.level)
        PenSkin.entries.map {
            PenSkinUiItem(
                it,
                unlocked = it.isUnlockedBy(progress.level, earned, owned),
                selected = it == resolved
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PenSkin.entries.map { PenSkinUiItem(it, unlocked = it == PenSkin.DEFAULT, selected = it == PenSkin.DEFAULT) }
    )

    /** Only ever called for a pen [PenSkinUiItem.unlocked]. */
    fun selectPenSkin(skin: PenSkin) = settingsRepository.setSelectedPenSkin(skin)

    /**
     * Re-reads the daily state whenever the menu comes back into view — the
     * app can sit in the background across midnight, at which point a
     * "already done today" card is stale and today's challenge is waiting.
     */
    fun refreshDaily() = dailyChallengeRepository.refresh()

    // Drives the small badge on the "İstatistikler" tile — cleared the next
    // time StatisticsScreen opens (see StatisticsViewModel.markAllSeen).
    val hasUnseenAchievement: StateFlow<Boolean> = achievementDao.observeUnseenCount()
        .map { it > 0 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )

    /**
     * How many chest slots are already ready to open, for the badge on the
     * "Kasalarım" tile. Recomputed only when [SettingsRepository.chestSlots]
     * itself changes, not on a live tick — a chest that finishes unlocking
     * while the player is sitting on the main menu updates this the next
     * time chestSlots is touched (opening the Chests screen ticks live
     * instead, see ChestsViewModel), which is close enough for a nudge badge.
     */
    val readyChestCount: StateFlow<Int> = settingsRepository.chestSlots
        .map { slots -> slots.count { it?.isReady(System.currentTimeMillis()) == true } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0
        )

    /**
     * How many friend requests are waiting, for the badge on the Arkadaşlar
     * tile.
     *
     * This count used to live on the online lobby's own button — one screen
     * deeper than the menu. A request only ever appears inside Arkadaşlarım,
     * so a badge you have to already be halfway there to see cannot do the
     * one job it exists for: telling somebody a request arrived. On the menu
     * it is in front of them every time they open the app.
     *
     * Widened to also count duels waiting to be played and duels this
     * player sent whose result just came back (see C2) — a duel a friend
     * sends has no push notification to announce it (a Firestore-write
     * trigger needs the Blaze plan this project deliberately doesn't use,
     * see functions/DEPLOY.md), so this badge is what stands in for one:
     * whatever needs this player's attention on the Friends screen shows up
     * here the moment the app is open, instead of staying invisible until
     * they happen to tap in on their own.
     *
     * An empty collection costs no document reads to watch, and a failure
     * shows no badge rather than an error — a home screen should not grow
     * one over a number this small.
     */
    val pendingFriendRequests: StateFlow<Int> = combine(
        friendRepository.observeFriendRequests().catch { emit(emptyList()) },
        duelRepository.observeIncomingDuels().catch { emit(emptyList()) },
        duelRepository.observeSentDuels().catch { emit(emptyList()) }
    ) { requests, incoming, sent ->
        requests.size + incoming.size + sent.count { it.status == DuelStatus.COMPLETE && !it.seenByChallenger }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = 0
    )
}
