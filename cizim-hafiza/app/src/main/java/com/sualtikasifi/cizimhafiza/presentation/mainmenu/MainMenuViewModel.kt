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
    private val achievementUnlocker: com.sualtikasifi.cizimhafiza.util.AchievementUnlocker,
    xpEventRepository: com.sualtikasifi.cizimhafiza.domain.repository.XpEventRepository,
    private val serverClock: com.sualtikasifi.cizimhafiza.domain.repository.ServerClock
) : ViewModel() {

    // --- Giriş Serisi (login streak) ---

    private val _loginPopup = MutableStateFlow<LoginPopup?>(null)
    /** The daily login window while there is something to show in it, else null. */
    val loginPopup: StateFlow<LoginPopup?> = _loginPopup.asStateFlow()

    /** Chests the login reward gave, opened one after another once the window has closed. */
    private val _loginChests = MutableStateFlow<List<com.sualtikasifi.cizimhafiza.domain.model.ChestReward>>(emptyList())
    val loginChests: StateFlow<List<com.sualtikasifi.cizimhafiza.domain.model.ChestReward>> = _loginChests.asStateFlow()
    private var pendingLoginChests: List<com.sualtikasifi.cizimhafiza.domain.model.ChestReward> = emptyList()

    private var loginDay = 0L
    private var loginChecking = false
    private var loginHandled = false

    /**
     * Asks whether today's login reward is waiting and, if so, opens the window. Needs the internet: "today" comes from
     * the server (see ServerClock), so a phone whose clock was moved cannot collect early. Cheap on the days it is
     * already taken — the phone's own date settles that without a server call. Safe to call again (e.g. when the
     * connection comes back): it does nothing once handled or while a check is running.
     */
    fun checkLoginStreak() {
        if (loginHandled || loginChecking || _loginPopup.value != null) return
        // Already taken today by the phone's own date: nothing to ask the server. (A moved clock cannot take a day early:
        // anything that is not settled by this check goes to the server's date below.)
        if (com.sualtikasifi.cizimhafiza.util.TurkeyTime.today().toEpochDay() == settingsRepository.loginLastClaimDay) {
            loginHandled = true
            return
        }
        loginChecking = true
        viewModelScope.launch {
            val today = serverClock.todayEpochDay()
            loginChecking = false
            // No server answer, no window: "today" is only ever the server's day, never the phone's.
            today ?: return@launch
            loginDay = today
            loginHandled = true
            _loginPopup.value = when (val state = com.sualtikasifi.cizimhafiza.domain.model.LoginStreaks.evaluate(
                settingsRepository.loginStreak, settingsRepository.loginLastClaimDay, today
            )) {
                com.sualtikasifi.cizimhafiza.domain.model.LoginStreakState.ClaimedToday -> null
                is com.sualtikasifi.cizimhafiza.domain.model.LoginStreakState.Claimable -> LoginPopup.Claim(state.streakBefore)
                // The Seri Sigortası rune: once a month a missed day is forgiven and the streak carries on.
                is com.sualtikasifi.cizimhafiza.domain.model.LoginStreakState.Missed ->
                    if (settingsRepository.useStreakInsurance()) LoginPopup.Claim(state.lostStreak, rescued = true) else LoginPopup.Missed(state.lostStreak)
            }
        }
    }

    fun claimLoginReward() {
        val popup = _loginPopup.value as? LoginPopup.Claim ?: return
        // One reward per day: if this day is already recorded (a second tap, a second window), nothing more is paid.
        if (settingsRepository.loginLastClaimDay >= loginDay) {
            _loginPopup.value = null
            return
        }
        val rewards = com.sualtikasifi.cizimhafiza.domain.model.LoginStreakRewards
        val day = rewards.cycleDayFor(popup.streakBefore)
        val reward = rewards.forDay(day)
        val newStreak = settingsRepository.recordLoginClaim(loginDay, popup.streakBefore, continuing = true)
        if (reward.gold > 0) settingsRepository.earnGold(com.sualtikasifi.cizimhafiza.domain.model.RuneEffects.loginGold(settingsRepository.runeLevels.value, reward.gold))
        if (reward.xp > 0) settingsRepository.addXp(reward.xp)
        // The Şanslı Zar rune sometimes adds a joker to the day's reward; it is shown in the window with the rest.
        val dice = settingsRepository.luckyDiceJoker()
        val jokers = reward.jokers + listOfNotNull(dice)
        jokers.forEach { settingsRepository.addJoker(it, 1) }
        val milestone = rewards.milestoneFor(newStreak)
        // Chests are opened on the spot, after the window closes (see consumeLoginPopup).
        pendingLoginChests = listOfNotNull(reward.chest, milestone).map { settingsRepository.openChestNow(it) }
        _loginPopup.value = LoginPopup.Claimed(day, reward.copy(jokers = jokers), newStreak, milestone)
    }

    /** The streak was broken: a watched ad carries it on, as if no day had been missed. */
    fun rescueLoginStreak(activity: Activity) {
        val popup = _loginPopup.value as? LoginPopup.Missed ?: return
        adManager.maybeShowRewarded(activity, "login_streak_rescue") { outcome ->
            when (outcome) {
                RewardedOutcome.EARNED -> _loginPopup.value = LoginPopup.Claim(popup.lostStreak, rescued = true)
                RewardedOutcome.SKIPPED -> Unit
                else -> _streakToast.value = StreakToast.AdUnavailable
            }
        }
    }

    fun restartLoginStreak() {
        if (_loginPopup.value is LoginPopup.Missed) _loginPopup.value = LoginPopup.Claim(0)
    }

    /** Closes the window; if the reward just taken included chests, they open now. */
    fun consumeLoginPopup() {
        val wasClaimed = _loginPopup.value is LoginPopup.Claimed
        _loginPopup.value = null
        if (wasClaimed && pendingLoginChests.isNotEmpty()) {
            _loginChests.value = pendingLoginChests
            pendingLoginChests = emptyList()
        }
    }

    fun consumeLoginChest() { _loginChests.value = _loginChests.value.drop(1) }

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
            val found = penaltyRepository.outstanding()
            if (found.isEmpty()) return@launch
            outstandingPenalties = found
            // One window for everything found, with the totals — nothing is taken until the player acknowledges it.
            _penaltyWarning.value = found.first().copy(
                xpRevoked = found.sumOf { it.xpRevoked },
                goldRevoked = found.sumOf { it.goldRevoked },
                strike = found.maxOf { it.strike },
                lockedUntilMillis = found.maxOf { it.lockedUntilMillis }
            )
        }
    }

    private var outstandingPenalties: List<Penalty> = emptyList()

    /** "Anladım": the window closes, and only now do the XP and gold come off — the home screen counts them down. */
    fun dismissPenaltyWarning() {
        _penaltyWarning.value = null
        val toApply = outstandingPenalties
        outstandingPenalties = emptyList()
        if (toApply.isNotEmpty()) viewModelScope.launch { penaltyRepository.apply(toApply) }
    }

    val dailyState: StateFlow<DailyChallengeState> = dailyChallengeRepository.state

    /**
     * A one-shot confirmation for the two rewarded streak actions below, so
     * the menu can say the ad actually paid out. Null once shown.
     */
    private val _streakToast = MutableStateFlow<StreakToast?>(null)
    val streakToast: StateFlow<StreakToast?> = _streakToast.asStateFlow()

    fun consumeStreakToast() { _streakToast.value = null }

    /** Records achievements earned outside a finished game (see AchievementUnlocker); the menu badge follows. */
    fun syncAchievements() { viewModelScope.launch { achievementUnlocker.sync() } }

    private val _ratingPrompt = MutableStateFlow(false)

    /** True when the one-time "rate us" ask should be on screen: from the third finished game on, asked here on the menu rather than on the result screen. */
    val ratingPrompt: StateFlow<Boolean> = _ratingPrompt.asStateFlow()

    fun checkRatingPrompt() {
        if (!_ratingPrompt.value && com.sualtikasifi.cizimhafiza.util.PostMatchPrompts.shouldShowRating(settingsRepository)) {
            _ratingPrompt.value = true
        }
    }

    fun dismissRatingPrompt() { _ratingPrompt.value = false }

    fun rateAndClaimBonus() {
        settingsRepository.grantRatingBonusXpOnce(com.sualtikasifi.cizimhafiza.util.PostMatchPrompts.RATING_BONUS_XP)
        _ratingPrompt.value = false
    }

    /** When the 500-gold ad button unlocks again (epoch millis; in the past = ready now). */
    val adGoldNextAtMillis: StateFlow<Long> = settingsRepository.adGoldNextAtMillis

    /** Epoch day the free ad chest was last taken; anything but today means it is available. */
    val adChestNextAtMillis: StateFlow<Long> = settingsRepository.adChestNextAtMillis

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

    /** A rune can be bought right now — the home screen's mascot shows a badge for it. */
    val runeReady: StateFlow<Boolean> = combine(settingsRepository.goldBalance, settingsRepository.runeLevels, settingsRepository.lifetimeXp) { gold, levels, xp ->
        com.sualtikasifi.cizimhafiza.domain.model.RuneTree.anyAffordable(levels, gold, com.sualtikasifi.cizimhafiza.domain.model.PlayerLevel.levelForXp(xp))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
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
    fun refreshDaily() {
        settingsRepository.refreshPeriodXp() // the month may have turned while the app sat open
        dailyChallengeRepository.refresh()
    }

    // Drives the small badge on the "İstatistikler" tile — cleared the next
    // time StatisticsScreen opens (see StatisticsViewModel.markAllSeen).
    /** True while no Google account is linked — the Ayarlar tile carries a dot nudging the player towards the Hesap row. */
    val accountNotLinked: StateFlow<Boolean> = authRepository.authState
        .map { it !is com.sualtikasifi.cizimhafiza.domain.repository.AuthState.Linked }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = false)

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

/** What the daily login window is showing. */
sealed interface LoginPopup {
    /** Today's reward can be taken after [streakBefore] days in a row; [rescued] when an ad just saved a broken streak. */
    data class Claim(val streakBefore: Int, val rescued: Boolean = false) : LoginPopup

    /** A day was missed: the [lostStreak]-day streak can be saved with an ad or started over. */
    data class Missed(val lostStreak: Int) : LoginPopup

    /** The reward was just taken: [day] of the calendar, the streak now [newStreak] days, plus a [milestone] chest on every 30th. */
    data class Claimed(
        val day: Int,
        val reward: com.sualtikasifi.cizimhafiza.domain.model.LoginDayReward,
        val newStreak: Int,
        val milestone: com.sualtikasifi.cizimhafiza.domain.model.ChestTier?
    ) : LoginPopup
}
