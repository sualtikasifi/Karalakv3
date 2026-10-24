package com.sualtikasifi.cizimhafiza.util

import android.util.Log
import android.content.Context
import com.sualtikasifi.cizimhafiza.domain.repository.FriendRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Claims whatever XP a Blaze-free cron script has left on this account's
 * private/pendingRewards document — right now, the sole source is the
 * referral reward (see functions/src/index.ts's runGrantReferralRewards):
 * once someone this device invited reaches level 5, the cron drops one
 * entry there for this device to pick up.
 *
 * A cron can't hand out XP directly: [SettingsRepository]'s lifetimeXp is
 * this device's own local counter, and [LeagueScorePublisher] republishes
 * it onto the public profile on every change — a server-side write to that
 * same profile would just be overwritten on the next publish. Claiming has
 * to happen here, applied through [SettingsRepository.addXp] like any other
 * XP gain, so it durably becomes part of the player's own progression
 * instead of a number that only ever lived in Firestore.
 *
 * Runs once at app start (self-healing, same reasoning as
 * LeagueScorePublisher: a claim lost to a dead connection is simply retried
 * next launch) rather than on a live listener — nothing about a pending
 * reward is urgent enough to need it applied mid-session.
 */
@Singleton
class ReferralRewardClaimer @Inject constructor(
    private val friendRepository: FriendRepository,
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var started = false

    private val _lastClaimedXp = MutableStateFlow(0)
    /** The amount just claimed, for a one-time "kazandın!" toast — see MainMenuViewModel. */
    val lastClaimedXp: StateFlow<Int> = _lastClaimedXp.asStateFlow()

    private val _lastClaimedGold = MutableStateFlow(0)
    /** The gold just claimed (the invite reward is 500 gold), for the same one-time toast. */
    val lastClaimedGold: StateFlow<Int> = _lastClaimedGold.asStateFlow()

    /** Safe to call repeatedly; only the first call actually claims. */
    fun start() {
        if (started) return
        started = true
        scope.launch {
            attributeInstallInvite()
            // A reward is only ever added by a once-a-day server job, so looking on every single
            // start is a server read that almost always finds nothing. Once per few hours is
            // plenty; a reward simply waits until the next check.
            val now = System.currentTimeMillis()
            if (now - settingsRepository.lastRewardCheckMillis < REWARD_CHECK_INTERVAL_MILLIS) return@launch
            settingsRepository.lastRewardCheckMillis = now
            runCatching { friendRepository.claimPendingRewards() }
                .onSuccess { totals ->
                    if (totals.xp > 0) {
                        settingsRepository.addXp(totals.xp)
                        _lastClaimedXp.value = totals.xp
                    }
                    if (totals.gold > 0) {
                        settingsRepository.earnGold(totals.gold)
                        _lastClaimedGold.value = totals.gold
                    }
                }
                .onFailure { NonFatal.report(TAG, "Pending reward claim failed", it) }
        }
    }

    fun consumeClaimedNotice() {
        _lastClaimedXp.value = 0
        _lastClaimedGold.value = 0
    }

    /**
     * A new player who installed from a friend's karalak.com/f/CODE link: the Play Store keeps the code (only for the
     * first launch), so it is read once, then attributed like a friend link opened in the app. The friend code of this
     * account is made first, because the attribution is merged onto the same document (see FriendsViewModel).
     */
    private suspend fun attributeInstallInvite() {
        if (!settingsRepository.installReferrerHandled) {
            settingsRepository.pendingInviterCode = InstallReferrerReader.friendCode(context)
            settingsRepository.installReferrerHandled = true
        }
        val code = settingsRepository.pendingInviterCode ?: return
        runCatching {
            friendRepository.ensureFriendCode(settingsRepository.nicknameOrDefault)
            friendRepository.recordReferralIfEligible(code)
        }.onSuccess { settingsRepository.pendingInviterCode = null }
            .onFailure { NonFatal.report(TAG, "Install invite attribution failed", it) }
    }

    private companion object {
        const val TAG = "ReferralRewardClaimer"
        const val REWARD_CHECK_INTERVAL_MILLIS = 4 * 60 * 60 * 1000L
    }
}
