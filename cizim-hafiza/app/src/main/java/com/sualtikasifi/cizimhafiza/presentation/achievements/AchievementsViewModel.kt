package com.sualtikasifi.cizimhafiza.presentation.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sualtikasifi.cizimhafiza.data.local.dao.AchievementDao
import com.sualtikasifi.cizimhafiza.domain.model.Achievement
import com.sualtikasifi.cizimhafiza.domain.model.AchievementRewardType
import com.sualtikasifi.cizimhafiza.domain.model.AchievementStats
import com.sualtikasifi.cizimhafiza.util.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * One achievement paired with this device's state for it: whether it's
 * unlocked, whether its reward was already claimed, and — for an incomplete
 * one — how close it is (see Achievement.currentValue), so the grid can show
 * "8/10" instead of just a locked icon.
 */
data class AchievementUiItem(
    val achievement: Achievement,
    val unlocked: Boolean,
    val claimed: Boolean,
    val currentValue: Int
)

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    private val achievementDao: AchievementDao,
    private val settingsRepository: SettingsRepository,
    private val achievementUnlocker: com.sualtikasifi.cizimhafiza.util.AchievementUnlocker
) : ViewModel() {

    // Snapshot of which achievement ids were still unseen when this screen
    // opened — captured BEFORE markAllSeen() below clears the flag, so
    // AchievementsScreen knows exactly which cards to shimmer for 10s (see
    // AchievementDao.seen / the MainMenu badge that sent the player here).
    private val _newlyUnlockedIds = MutableStateFlow<Set<String>>(emptySet())
    val newlyUnlockedIds: StateFlow<Set<String>> = _newlyUnlockedIds.asStateFlow()

    init {
        viewModelScope.launch {
            // Catch up on anything earned outside a finished game first, so it shows (and shimmers) now.
            achievementUnlocker.sync()
            _newlyUnlockedIds.value = achievementDao.getUnseenIds().toSet()
            achievementDao.markAllSeen()
        }
    }

    // The three counters kept as StateFlow already push updates here live;
    // the rest of AchievementStats (games/perfect rounds/online wins/streak)
    // only ever changes at the end of a game, i.e. exactly when this screen
    // isn't open, so reading them plainly at combine-time is enough to keep
    // progress numbers accurate without a live subscription on every field.
    private val stats = combine(
        settingsRepository.lifetimeScore,
        settingsRepository.lifetimeXp,
        settingsRepository.lifetimeWordsDrawn
    ) { score, xp, wordsDrawn ->
        AchievementStats(
            gamesPlayed = settingsRepository.lifetimeGamesPlayed,
            lifetimeWordsDrawn = wordsDrawn,
            lifetimeScore = score,
            currentStreak = settingsRepository.currentStreak,
            bestStreak = settingsRepository.bestStreak,
            perfectRounds = settingsRepository.lifetimePerfectRounds,
            onlineWins = settingsRepository.lifetimeOnlineWins,
            lifetimeXp = xp
        )
    }
    init {
        // XP can move while this page is open (claiming a reward pays XP), which may complete the next XP tier.
        viewModelScope.launch { stats.collect { achievementUnlocker.sync() } }
    }

    val achievements: StateFlow<List<AchievementUiItem>> = combine(
        achievementDao.observeAll(),
        stats
    ) { unlocked, stats ->
        val byId = unlocked.associateBy { it.id }
        Achievement.entries.map { achievement ->
            val row = byId[achievement.name]
            AchievementUiItem(
                achievement = achievement,
                unlocked = row != null,
                claimed = row?.claimed == true,
                currentValue = achievement.currentValue(stats)
            )
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = Achievement.entries.map { AchievementUiItem(it, unlocked = false, claimed = false, currentValue = 0) }
        )

    /**
     * Pays out [achievement]'s reward and marks it claimed — guarded by
     * AchievementDao.claim's atomic "AND claimed = 0", so a double-tap (or
     * this being called twice for any other reason) cannot pay twice.
     */
    fun claim(achievement: Achievement) {
        viewModelScope.launch {
            if (achievementDao.claim(achievement.name) == 1) {
                when (achievement.rewardType) {
                    AchievementRewardType.XP -> settingsRepository.addXp(achievement.xpReward)
                    AchievementRewardType.GOLD -> settingsRepository.earnGold(achievement.goldReward)
                }
            }
        }
    }
}
