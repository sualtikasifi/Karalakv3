package com.sualtikasifi.cizimhafiza.util

import com.sualtikasifi.cizimhafiza.data.local.dao.AchievementDao
import com.sualtikasifi.cizimhafiza.data.local.entity.UnlockedAchievementEntity
import com.sualtikasifi.cizimhafiza.domain.model.Achievement
import com.sualtikasifi.cizimhafiza.domain.model.AchievementStats
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Records every achievement whose condition already holds, whenever the app looks.
 *
 * Unlocks used to be decided only at the end of a saved game. Anything that moved a counter outside
 * a game — XP from the daily challenge, a claimed reward, a referral, a restored backup — left an
 * achievement sitting at "500/500" yet locked until some later round happened to finish. Running the
 * same check on the menu, on the achievements page and after restores closes that gap. Inserts ignore
 * duplicates, so it is safe to call as often as wanted and alongside the end-of-game check.
 */
@Singleton
class AchievementUnlocker @Inject constructor(
    private val achievementDao: AchievementDao,
    private val settingsRepository: SettingsRepository
) {
    suspend fun sync(): List<Achievement> {
        val stats = AchievementStats(
            gamesPlayed = settingsRepository.lifetimeGamesPlayed,
            lifetimeWordsDrawn = settingsRepository.lifetimeWordsDrawn.value,
            lifetimeScore = settingsRepository.lifetimeScore.value,
            currentStreak = settingsRepository.currentStreak,
            bestStreak = settingsRepository.bestStreak,
            perfectRounds = settingsRepository.lifetimePerfectRounds,
            onlineWins = settingsRepository.lifetimeOnlineWins,
            lifetimeXp = settingsRepository.lifetimeXp.value
        )
        val already = achievementDao.getUnlockedIds().toSet()
        val newly = Achievement.entries.filter { it.name !in already && it.isUnlocked(stats) }
        val now = System.currentTimeMillis()
        newly.forEach { achievementDao.insert(UnlockedAchievementEntity(id = it.name, unlockedAtMillis = now)) }
        return newly
    }
}
