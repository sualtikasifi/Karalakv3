package com.sualtikasifi.cizimhafiza.data.repository

import com.sualtikasifi.cizimhafiza.util.NonFatal
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sualtikasifi.cizimhafiza.data.local.dao.AchievementDao
import com.sualtikasifi.cizimhafiza.domain.model.Achievement
import com.sualtikasifi.cizimhafiza.domain.model.AchievementStats
import com.sualtikasifi.cizimhafiza.domain.model.Penalty
import com.sualtikasifi.cizimhafiza.domain.repository.PenaltyRepository
import com.sualtikasifi.cizimhafiza.util.DailyChallengeRepository
import com.sualtikasifi.cizimhafiza.util.SettingsRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore layout: `penalties/{id}` with `appliedAt` starting at 0 and set
 * once by the device it belongs to. See firestore.rules — a player may edit
 * that single field on their own penalty and nothing else, which is what
 * makes "applied exactly once" survive a reinstall rather than depending on
 * a local flag that a fresh install would not have.
 */
@Singleton
class PenaltyRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val settingsRepository: SettingsRepository,
    private val achievementDao: AchievementDao,
    private val dailyChallengeRepository: DailyChallengeRepository
) : PenaltyRepository {

    private val penalties get() = firestore.collection("penalties")

    override suspend fun outstanding(): List<Penalty> {
        // Deliberately NOT signing in anonymously to find out. This runs on
        // every launch and a signed-out device has no penalties by
        // definition; creating an account just to check would be a write on
        // the cold path for nothing.
        val uid = auth.currentUser?.uid ?: return emptyList()
        // Even an empty answer costs a server read, and it is empty on nearly every launch. A penalty waits for its
        // device, so looking every half hour instead of every start loses nothing a player could notice.
        val now = System.currentTimeMillis()
        if (now - settingsRepository.lastPenaltyCheckMillis < PENALTY_CHECK_INTERVAL_MILLIS) return emptyList()
        val queried = runCatching {
            penalties
                .whereEqualTo("uid", uid)
                .whereEqualTo("appliedAt", 0L)
                .get()
                .await()
                .documents
        }.onFailure { NonFatal.report(TAG, "Could not read penalties", it) }.getOrNull()
        // Only a query that actually answered AND found nothing counts as a check: a penalty that was found but not yet
        // acknowledged is shown again on the next launch.
        if (queried != null && queried.isEmpty()) settingsRepository.lastPenaltyCheckMillis = now
        return queried.orEmpty().map { doc ->
            Penalty(
                id = doc.id,
                uid = uid,
                xpRevoked = (doc.getLong("xpRevoked") ?: 0L).toInt(),
                strike = (doc.getLong("strike") ?: 1L).toInt(),
                lockedUntilMillis = doc.getLong("lockedUntil") ?: 0L,
                createdAtMillis = doc.getLong("createdAt") ?: 0L,
                breaksDailyStreak = doc.getBoolean("breaksDailyStreak") ?: false,
                goldRevoked = (doc.getLong("goldRevoked") ?: 0L).toInt()
            )
        }.sortedByDescending { it.createdAtMillis }
    }

    override suspend fun apply(penalties: List<Penalty>) {
        var applied = 0
        for (penalty in penalties) {
            // Marked BEFORE the local effect, not after. If the write fails the penalty is simply applied the next time
            // the window is acknowledged; if it were the other way round, a crash in between would take the XP again
            // every time the app opened.
            val marked = runCatching {
                this.penalties.document(penalty.id).update("appliedAt", System.currentTimeMillis()).await()
            }.onFailure { NonFatal.report(TAG, "Could not mark penalty ${penalty.id} applied", it) }.isSuccess
            if (!marked) continue
            // The loss is noted for the home screen BEFORE the balances change, so its count-down sees both together.
            com.sualtikasifi.cizimhafiza.util.HomeShownStats.pendingXpLoss += minOf(penalty.xpRevoked, settingsRepository.lifetimeXp.value)
            com.sualtikasifi.cizimhafiza.util.HomeShownStats.pendingGoldLoss += minOf(penalty.goldRevoked, settingsRepository.goldBalance.value)
            settingsRepository.revokeXp(penalty.xpRevoked)
            settingsRepository.revokeGold(penalty.goldRevoked)
            settingsRepository.penaltiesApplied = settingsRepository.penaltiesApplied + 1
            if (penalty.breaksDailyStreak) dailyChallengeRepository.breakStreak()
            applied++
        }
        if (applied > 0) revokeAchievementsNoLongerEarned()
    }

    /**
     * Re-checks the whole catalogue against what the account now has and
     * takes back anything it no longer qualifies for.
     *
     * Re-evaluated rather than tracked: an achievement's condition is a
     * function of the stats (see Achievement), so asking the catalogue is
     * always right, while a hand-maintained list of "XP achievements" would
     * quietly go stale the first time somebody adds one.
     */
    private suspend fun revokeAchievementsNoLongerEarned() {
        runCatching {
            val unlocked = achievementDao.getUnlockedIds().toSet()
            if (unlocked.isEmpty()) return@runCatching
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
            val lost = Achievement.entries
                .filter { it.name in unlocked && !it.isUnlocked(stats) }
                .map { it.name }
            if (lost.isNotEmpty()) {
                Log.i(TAG, "Revoking ${lost.size} achievement(s) after penalty")
                achievementDao.deleteByIds(lost)
            }
        }.onFailure { NonFatal.report(TAG, "Could not re-check achievements", it) }
    }

    /**
     * The account's live lockout deadline, or null if it may play.
     *
     * One equality filter and the maximum taken on the device, rather than
     * `orderBy("lockedUntil").limit(1)` on the server. The ordered form needs
     * a composite index, and this project's indexes are not deployed — the CI
     * job that would deploy them has never had its service-account secret, so
     * the query failed, the failure was swallowed as "no lockout", and an
     * account could collect any number of penalties without ever being shut
     * out of anything.
     *
     * The cost of doing it here is every penalty this account has, which for
     * an honest player is none and for a cheat is a handful.
     */
    override suspend fun lockedUntilMillis(): Long? {
        val uid = auth.currentUser?.uid ?: return null
        val now = System.currentTimeMillis()
        return runCatching {
            penalties
                .whereEqualTo("uid", uid)
                .get()
                .await()
                .documents
                .mapNotNull { it.getLong("lockedUntil") }
                .maxOrNull()
                ?.takeIf { it > now }
        }.onFailure { NonFatal.report(TAG, "Could not read lockout", it) }.getOrNull()
    }

    private companion object {
        const val TAG = "Penalties"

        /** How often the launch-time penalty check actually asks the server. */
        const val PENALTY_CHECK_INTERVAL_MILLIS = 30 * 60 * 1000L
    }
}
