package com.sualtikasifi.cizimhafiza.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.sualtikasifi.cizimhafiza.domain.model.AvatarFrame
import com.sualtikasifi.cizimhafiza.domain.model.GlobalLeagueTable
import com.sualtikasifi.cizimhafiza.domain.model.LeagueEntry
import com.sualtikasifi.cizimhafiza.domain.model.LeagueTable
import com.sualtikasifi.cizimhafiza.domain.model.LeaguePeriodResult
import com.sualtikasifi.cizimhafiza.domain.model.LeagueWinner
import com.sualtikasifi.cizimhafiza.domain.repository.GlobalLeagueRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads the one document the scheduled function publishes — see
 * [GlobalLeagueRepository] and functions/src/index.ts.
 */
@Singleton
class GlobalLeagueRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : GlobalLeagueRepository {

    private val snapshotDoc get() = firestore.document("leaderboards/global")
    private val configDoc get() = firestore.document("leaderboards/config")

    // The whole point of publishing one document is that reading it is
    // cheap; re-reading it on every tab switch would give that back.
    private var cached: GlobalLeagueTable? = null
    private var cachedAtMillis = 0L

    override suspend fun table(forceRefresh: Boolean): Result<GlobalLeagueTable> {
        val fresh = cached
        if (!forceRefresh && fresh != null &&
            System.currentTimeMillis() - cachedAtMillis < GlobalLeagueRepository.REFRESH_WINDOW_MILLIS
        ) {
            return Result.success(fresh)
        }
        return runCatching {
            val doc = snapshotDoc.get().await()
            // An absent document is not an error: it is what the app sees
            // before the scheduled function has ever run, and the screen has
            // an empty state for exactly that.
            val table = parse(doc.data.orEmpty())
            cached = table
            cachedAtMillis = System.currentTimeMillis()
            table
        }
    }

    override suspend fun myRank(periodId: Long, myPeriodXp: Int, botsAbove: Int): Result<Int> = runCatching {
        val ahead = firestore.collection("users")
            .whereEqualTo("periodId", periodId)
            .whereGreaterThan("periodXp", myPeriodXp.toLong())
            .count()
            .get(com.google.firebase.firestore.AggregateSource.SERVER)
            .await()
            .count
        ahead.toInt() + botsAbove + 1
    }

    override suspend fun setWeekReward(rewardId: String?): Result<Unit> = runCatching {
        configDoc.set(mapOf("rewardId" to rewardId), SetOptions.merge()).await()
        // The published snapshot still carries the OLD reward until the next
        // rebuild, so drop the cache rather than letting the panel show a
        // change it just made as not having happened.
        cached = null
        Unit
    }

    private fun parse(data: Map<String, Any?>): GlobalLeagueTable {
        val myUid = auth.currentUser?.uid
        val docPeriod = (data["periodId"] as? Number)?.toLong() ?: 0L
        val today = com.sualtikasifi.cizimhafiza.util.TurkeyTime.today()
        val thisPeriod = com.sualtikasifi.cizimhafiza.domain.model.LeaguePeriod.periodIdFor(today)

        // Real players come from the published document: its own `humans` list when the builder wrote one
        // (kept apart from the filler rows so an older writer cannot disturb it), otherwise the real rows
        // of `entries`. A document from a past month says nothing about this one.
        val published = ((data["humans"] as? List<*>) ?: (data["entries"] as? List<*>)).orEmpty()
            .filterIsInstance<Map<*, *>>()
            .filter { it["uid"] is String }
        val rows = if (docPeriod == thisPeriod) published else emptyList()

        val humans = rows.map { row ->
            val uid = row["uid"] as String
            val level = (row["level"] as? Number)?.toInt() ?: 1
            LeagueEntry(
                uid = uid,
                nickname = (row["nickname"] as? String)?.takeIf { it.isNotBlank() } ?: "?",
                periodXp = (row["periodXp"] as? Number)?.toInt() ?: 0,
                level = level,
                frameId = AvatarFrame.highestUnlockedFor(level).name,
                isMe = uid == myUid,
                isBot = false
            )
        }

        // The filler players are worked out here, from the month and the clock (see LeagueBots), so their
        // names and scores are the same on every phone and always current.
        val bots = com.sualtikasifi.cizimhafiza.domain.model.LeagueBots
            .bots(thisPeriod, System.currentTimeMillis(), com.sualtikasifi.cizimhafiza.domain.model.LeagueBots.monthStartMillis(today))
            .mapIndexed { index, bot ->
                LeagueEntry(
                    uid = "$BOT_KEY_PREFIX$index",
                    nickname = bot.nickname,
                    periodXp = bot.periodXp,
                    level = bot.level,
                    frameId = botFrameFor(bot.level, bot.nickname).name,
                    isMe = false,
                    isBot = true
                )
            }
        // Same cut as the server's: the 21 best of everyone, so the app can still show 20 others next to the player.
        val entries = (humans + bots)
            .sortedWith(compareByDescending<LeagueEntry> { it.periodXp }.thenBy { it.nickname })
            .take(PUBLISHED_ROWS)

        val daysRemaining = (data["daysRemaining"] as? Number)?.toInt() ?: 0
        val lastPeriod = parseLastPeriod(data["lastPeriod"] as? Map<*, *>)
        return GlobalLeagueTable(
            // Re-ranked here rather than trusted as ordered: the tie-break
            // then matches the friends table exactly, and myRank comes free.
            table = LeagueTable.rank(entries, daysRemaining),
            periodId = docPeriod,
            generatedAtMillis = (data["generatedAt"] as? Number)?.toLong() ?: 0L,
            rewardId = data["rewardId"] as? String,
            lastPeriod = lastPeriod,
            myLastPeriodWin = lastPeriod?.winners?.firstOrNull { it.uid == myUid }
        )
    }

    /**
     * A filler row's frame: any frame its level could really have unlocked (ladder frames and store
     * frames alike, never the plain default), picked from its own name so it stays the same on
     * every refresh but differs from row to row.
     */
    private fun botFrameFor(level: Int, seed: String): AvatarFrame {
        val choices = AvatarFrame.unlockedFor(level).filter { it != AvatarFrame.DEFAULT } +
            AvatarFrame.entries.filter { it.isStoreItem }
        if (choices.isEmpty()) return AvatarFrame.highestUnlockedFor(level)
        val hash = seed.fold(7) { acc, c -> acc * 31 + c.code }
        return choices[(hash and Int.MAX_VALUE) % choices.size]
    }

    private fun parseLastPeriod(data: Map<*, *>?): LeaguePeriodResult? {
        if (data == null) return null
        val winners = (data["winners"] as? List<*>).orEmpty()
            .filterIsInstance<Map<*, *>>()
            .mapNotNull { row ->
                val uid = row["uid"] as? String ?: return@mapNotNull null
                LeagueWinner(
                    uid = uid,
                    nickname = (row["nickname"] as? String)?.takeIf { it.isNotBlank() } ?: "?",
                    rank = (row["rank"] as? Number)?.toInt() ?: 0,
                    periodXp = (row["periodXp"] as? Number)?.toInt() ?: 0
                )
            }
        return LeaguePeriodResult(
            periodId = (data["periodId"] as? Number)?.toLong() ?: 0L,
            rewardId = data["rewardId"] as? String,
            winners = winners
        )
    }

    private companion object {
        const val BOT_KEY_PREFIX = "filler:"
        const val PUBLISHED_ROWS = 21
    }
}
