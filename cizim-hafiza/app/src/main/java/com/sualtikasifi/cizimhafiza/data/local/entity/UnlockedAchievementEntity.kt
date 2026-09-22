package com.sualtikasifi.cizimhafiza.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per unlocked achievement — presence of a row IS the unlocked
 * state (see AchievementDao.getUnlockedIds), nothing else to track. [id] is
 * an Achievement enum constant's `.name` (see domain.model.Achievement).
 */
@Entity(tableName = "unlocked_achievements")
data class UnlockedAchievementEntity(
    @PrimaryKey val id: String,
    val unlockedAtMillis: Long,
    // False until StatisticsScreen has shown this unlock's shimmer once
    // (see AchievementDao.markAllSeen) — drives the MainMenu's "new
    // achievement" badge. Defaults false for a fresh unlock; MIGRATION_8_9
    // backfills true for every row that already existed before this column,
    // so upgrading the app doesn't suddenly badge old achievements.
    val seen: Boolean = false,
    // False until the player taps this achievement's reward on the
    // Achievements screen (see AchievementDao.claim) — unlocking only
    // records that the condition was met, the reward itself is paid on
    // claim. Defaults false for a fresh unlock; MIGRATION_9_10 backfills
    // true for every row that already existed before this column, since
    // those were already auto-paid under the old behaviour and must not be
    // paid a second time.
    val claimed: Boolean = false
)
