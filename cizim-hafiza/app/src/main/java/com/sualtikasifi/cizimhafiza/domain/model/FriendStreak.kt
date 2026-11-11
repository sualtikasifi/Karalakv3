package com.sualtikasifi.cizimhafiza.domain.model

/**
 * The streak two friends keep together: the days in a row on which a duel between them (ten words at least) was played
 * to the end. One Firestore document per pair, `friendStreaks/{smallerUid}_{largerUid}` (see firestore.rules, which holds
 * the same transitions as [FriendStreaks] writes). Days are Türkiye days as epoch-day numbers, the same clock as the
 * daily challenge.
 */
data class FriendStreak(
    val pairId: String,
    /** The other player of the pair, from the point of view of whoever holds this object. */
    val friendUid: String,
    val count: Int,
    val best: Int,
    /** Which streak of the pair this is: grows by one each time a streak is lost and a new one begins (milestone prizes are paid once per run). */
    val run: Int,
    /** The last day a duel was counted. */
    val lastDay: Int,
    /** A duel played on this day while the streak was broken but still mendable; counted when it is mended, or starts the next run. -1: none. */
    val pendingDay: Int,
    val repairs: Int,
    val repairedBy: String?,
    val updatedAt: Long
)

/** Where a streak stands today. */
enum class StreakStatus {
    /** A duel was counted today. */
    Done,

    /** Alive, but today's duel is still to be played. */
    AtRisk,

    /** Missed yesterday: gone unless one of the two watches an ad today. */
    Broken,

    /** Gone for good (or never was). */
    Lost
}

/** A streak as the screens show it today: the number to show and its [status]. */
data class StreakView(val streak: FriendStreak, val count: Int, val status: StreakStatus) {
    val needsDuel: Boolean get() = status == StreakStatus.AtRisk || status == StreakStatus.Broken
}

/** What to write to Firestore after a change: nothing, a new document, or the whole document again. */
sealed interface StreakWrite {
    data object None : StreakWrite
    data class Save(val fields: Map<String, Any>) : StreakWrite
}

/**
 * The rules of a friend streak, kept in one pure place so they can be tested without a phone and so they match
 * firestore.rules transition for transition.
 */
object FriendStreaks {
    /** A duel counts for the streak with at least this many words. */
    const val MIN_WORDS = 10

    /** The days at which both friends are rewarded (each claims theirs on their own phone, once per run). */
    val MILESTONES: List<Int> = listOf(3, 7, 14, 30, 50, 100)

    fun pairId(a: String, b: String): String = if (a < b) "${a}_$b" else "${b}_$a"

    fun milestonesReached(count: Int): List<Int> = MILESTONES.filter { count >= it }

    fun nextMilestone(count: Int): Int? = MILESTONES.firstOrNull { it > count }

    /** The streak as it reads on [today]: a mended one carries on, a lost one with a kept duel is a new streak of one. */
    fun view(s: FriendStreak, today: Int): StreakView = when {
        s.lastDay >= today -> StreakView(s, s.count, StreakStatus.Done)
        s.lastDay == today - 1 -> StreakView(s, s.count, StreakStatus.AtRisk)
        s.lastDay == today - 2 -> StreakView(s, s.count, StreakStatus.Broken)
        s.pendingDay >= today - 1 -> StreakView(s, 1, if (s.pendingDay >= today) StreakStatus.Done else StreakStatus.AtRisk)
        else -> StreakView(s, 0, StreakStatus.Lost)
    }

    private fun fields(
        me: String, friend: String, count: Int, best: Int, run: Int, lastDay: Int, pendingDay: Int,
        repairs: Int, repairedBy: String?, now: Long
    ): Map<String, Any> = buildMap {
        put("uids", listOf(minOf(me, friend), maxOf(me, friend)))
        put("count", count)
        put("best", maxOf(best, count))
        put("run", run)
        put("lastDay", lastDay)
        put("pendingDay", pendingDay)
        put("repairs", repairs)
        if (repairedBy != null) put("repairedBy", repairedBy)
        put("updatedAt", now)
    }

    /** A duel between [me] and [friend] was played to the end on [today]. */
    fun afterDuel(s: FriendStreak?, me: String, friend: String, today: Int, now: Long): StreakWrite {
        if (s == null) return StreakWrite.Save(fields(me, friend, 1, 1, 1, today, -1, 0, null, now))
        return when {
            s.lastDay >= today -> StreakWrite.None
            s.lastDay == today - 1 ->
                StreakWrite.Save(fields(me, friend, s.count + 1, s.best, s.run, today, -1, s.repairs, s.repairedBy, now))
            // Missed yesterday: the duel is kept until someone mends the streak (or it starts the next run tomorrow).
            s.lastDay == today - 2 ->
                if (s.pendingDay == today) StreakWrite.None
                else StreakWrite.Save(fields(me, friend, s.count, s.best, s.run, s.lastDay, today, s.repairs, s.repairedBy, now))
            // Gone: a new run, which already holds yesterday's kept duel if there was one.
            else -> {
                val count = if (s.pendingDay == today - 1) 2 else 1
                StreakWrite.Save(fields(me, friend, count, s.best, s.run + 1, today, -1, s.repairs, s.repairedBy, now))
            }
        }
    }

    /** [me] watched the ad that mends a broken streak (only a streak that missed exactly yesterday can be mended). */
    fun afterRepair(s: FriendStreak, me: String, friend: String, today: Int, now: Long): StreakWrite {
        if (s.lastDay != today - 2) return StreakWrite.None
        return if (s.pendingDay == today) {
            StreakWrite.Save(fields(me, friend, s.count + 1, s.best, s.run, today, -1, s.repairs + 1, me, now))
        } else {
            StreakWrite.Save(fields(me, friend, s.count, s.best, s.run, today - 1, -1, s.repairs + 1, me, now))
        }
    }
}
