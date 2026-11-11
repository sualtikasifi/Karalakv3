package com.sualtikasifi.cizimhafiza.presentation.friends

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.ads.AdManager
import com.sualtikasifi.cizimhafiza.ads.RewardedOutcome
import com.sualtikasifi.cizimhafiza.domain.model.FriendStreak
import com.sualtikasifi.cizimhafiza.domain.model.FriendStreaks
import com.sualtikasifi.cizimhafiza.domain.model.StreakStatus
import com.sualtikasifi.cizimhafiza.domain.repository.FriendRepository
import com.sualtikasifi.cizimhafiza.domain.repository.FriendStreakRepository
import com.sualtikasifi.cizimhafiza.util.FriendStreakRewards
import com.sualtikasifi.cizimhafiza.util.FriendStreakStore
import com.sualtikasifi.cizimhafiza.util.GameConstants
import com.sualtikasifi.cizimhafiza.util.LegalConsentStore
import com.sualtikasifi.cizimhafiza.util.SettingsRepository
import com.sualtikasifi.cizimhafiza.util.StreakReward
import com.sualtikasifi.cizimhafiza.util.TurkeyTime
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The window the home screen opens with for a friend streak, one at a time. */
sealed interface StreakPrompt {
    /** The friend mended the streak while the player was away. */
    data class Rescued(val pairId: String, val friendName: String, val count: Int, val repairs: Int) : StreakPrompt

    /** A milestone is reached: both friends have a prize to take. */
    data class Reward(val pairId: String, val run: Int, val friendName: String, val milestone: Int, val reward: StreakReward) : StreakPrompt

    /** The streak missed a day and can still be mended with an ad. */
    data class Broken(val pairId: String, val friendUid: String, val friendName: String, val count: Int) : StreakPrompt

    /** These streaks end today unless a duel is played. */
    data class AtRisk(val items: List<Risk>) : StreakPrompt {
        data class Risk(val pairId: String, val friendUid: String, val friendName: String, val count: Int)
    }
}

/** What [nextPrompt] needs to know about what the phone has already shown. */
class PromptMemory(
    val isClaimed: (pairId: String, run: Int, milestone: Int) -> Boolean,
    val remindedOn: (pairId: String, day: Int) -> Boolean,
    val seenRepairs: (pairId: String) -> Int
)

/** From this hour (Türkiye) on, a streak that is still to be played gets a reminder when the app opens. */
const val STREAK_REMINDER_HOUR = 14

/**
 * The one window to show now, or null. In this order: a friend mended the streak, a prize is waiting, a streak can still
 * be mended, a streak ends today. Pure so it can be tested without a phone.
 */
fun nextPrompt(
    streaks: List<FriendStreak>,
    names: Map<String, String>,
    me: String,
    today: Int,
    hour: Int,
    adsOn: Boolean,
    memory: PromptMemory
): StreakPrompt? {
    val live = streaks.mapNotNull { s -> names[s.friendUid]?.let { name -> Triple(s, name, FriendStreaks.view(s, today)) } }
        .filter { it.third.status != StreakStatus.Lost }

    live.firstOrNull { (s, _, _) -> s.repairs > memory.seenRepairs(s.pairId) && s.repairedBy != null && s.repairedBy != me }
        ?.let { (s, name, v) -> return StreakPrompt.Rescued(s.pairId, name, v.count, s.repairs) }

    for ((s, name, v) in live) {
        val due = FriendStreaks.milestonesReached(v.count).firstOrNull { !memory.isClaimed(s.pairId, s.run, it) } ?: continue
        return StreakPrompt.Reward(s.pairId, s.run, name, due, FriendStreakRewards.rewardFor(due))
    }

    if (adsOn) {
        live.firstOrNull { (s, _, v) -> v.status == StreakStatus.Broken && !memory.remindedOn(s.pairId, today) }
            ?.let { (s, name, v) -> return StreakPrompt.Broken(s.pairId, s.friendUid, name, v.count) }
    }

    if (hour >= STREAK_REMINDER_HOUR) {
        val risky = live.filter { (s, _, v) -> v.status == StreakStatus.AtRisk && v.count >= 2 && !memory.remindedOn(s.pairId, today) }
        if (risky.isNotEmpty()) {
            return StreakPrompt.AtRisk(risky.map { (s, name, v) -> StreakPrompt.AtRisk.Risk(s.pairId, s.friendUid, name, v.count) })
        }
    }
    return null
}

@HiltViewModel
class FriendStreakViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: FriendStreakRepository,
    friendRepository: FriendRepository,
    private val store: FriendStreakStore,
    private val settingsRepository: SettingsRepository,
    private val adManager: AdManager,
    private val legalConsentStore: LegalConsentStore,
    private val analytics: com.sualtikasifi.cizimhafiza.util.GameAnalytics
) : ViewModel() {

    /** Bumped whenever the phone's own memory (claimed, reminded, seen) changes, so the window moves on. */
    private val memoryVersion = MutableStateFlow(0)

    /** Every streak with its friend's current name, for the lists. */
    val streaks: StateFlow<List<FriendStreak>> = repository.observeStreaks()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val memory = PromptMemory(store::isClaimed, store::remindedOn, store::seenRepairs)

    val prompt: StateFlow<StreakPrompt?> = combine(
        streaks,
        friendRepository.observeFriends().catch { emit(emptyList()) },
        memoryVersion
    ) { streaks, friends, _ ->
        val me = repository.currentUid
        if (me == null || legalConsentStore.childMode.value) {
            null
        } else {
            nextPrompt(
                streaks = streaks,
                names = friends.associate { it.uid to it.nickname },
                me = me,
                today = TurkeyTime.today().toEpochDay().toInt(),
                hour = TurkeyTime.now().hour,
                adsOn = GameConstants.ADMOB_ENABLED,
                memory = memory
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun today(): Int = TurkeyTime.today().toEpochDay().toInt()

    /** The window was shown: a reminder is not repeated the same day. */
    fun onShown(prompt: StreakPrompt) {
        when (prompt) {
            is StreakPrompt.Broken -> store.markReminded(prompt.pairId, today())
            is StreakPrompt.AtRisk -> prompt.items.forEach { store.markReminded(it.pairId, today()) }
            else -> Unit
        }
    }

    /** "Tamam" on the friend-saved-it window. */
    fun acknowledgeRescued(prompt: StreakPrompt.Rescued) {
        store.markRepairsSeen(prompt.pairId, prompt.repairs)
        memoryVersion.value++
    }

    /** Takes the prize of a milestone. */
    fun claim(prompt: StreakPrompt.Reward) {
        if (!store.isClaimed(prompt.pairId, prompt.run, prompt.milestone)) {
            FriendStreakRewards.apply(settingsRepository, prompt.reward)
            store.markClaimed(prompt.pairId, prompt.run, prompt.milestone)
            analytics.friendStreakReward(prompt.milestone)
        }
        memoryVersion.value++
    }

    /** Closes a reminder (already marked as shown). */
    fun dismiss() {
        memoryVersion.value++
    }

    /** The ad that mends a broken streak, for either of the two friends; the repair is written once it was watched to the end. */
    fun rescue(activity: Activity, friendUid: String, pairId: String) {
        adManager.maybeShowRewarded(activity, "friend_streak_rescue") { outcome ->
            when (outcome) {
                RewardedOutcome.EARNED -> viewModelScope.launch {
                    repository.repair(friendUid).onSuccess {
                        analytics.friendStreakRescued()
                        store.markRepairsSeen(pairId, (streaks.value.firstOrNull { it.pairId == pairId }?.repairs ?: 0) + 1)
                        Toast.makeText(context, R.string.fstreak_done_toast, Toast.LENGTH_SHORT).show()
                        memoryVersion.value++
                    }.onFailure {
                        Toast.makeText(context, R.string.fstreak_ad_failed, Toast.LENGTH_SHORT).show()
                    }
                }
                RewardedOutcome.UNAVAILABLE -> Toast.makeText(context, R.string.fstreak_ad_failed, Toast.LENGTH_SHORT).show()
                RewardedOutcome.SKIPPED -> Unit
            }
        }
    }
}
