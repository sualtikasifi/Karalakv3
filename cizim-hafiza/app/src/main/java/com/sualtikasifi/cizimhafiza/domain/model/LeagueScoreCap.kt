package com.sualtikasifi.cizimhafiza.domain.model

import java.time.LocalDate
import java.time.ZoneOffset

/**
 * How much of a month's league score the server accepts right now — the app-side mirror of firestore.rules'
 * plausibleMonthScore, so the profile is written with a value the rules take instead of being refused as a whole.
 *
 * The rules allow a burst plus 3000 XP an hour, measured both from the start of the month and from the moment this
 * account published its first score of the month (`periodStartAt`, stamped by the server on that write). A player
 * whose real score is above that — someone who reinstalled and starts a fresh profile, say — publishes the allowed
 * part now and the rest catches up on later publishes.
 */
object LeagueScoreCap {
    const val BURST_XP = 30_000L
    /** 3000 XP an hour. */
    const val MILLIS_PER_XP = 1_200L
    const val CEILING_XP = 300_000L
    /** Kept under the rules' limit so a phone clock running a little fast does not tip a write over it (~20 minutes). */
    const val SAFETY_MARGIN_XP = 1_000L

    private val ISTANBUL = ZoneOffset.ofHours(3)

    /** Midnight of the 1st in Istanbul for [periodId] (year * 12 + month - 1, see [LeaguePeriod.periodIdFor]). */
    fun monthStartMillis(periodId: Long): Long =
        LocalDate.of((periodId / 12).toInt(), (periodId % 12).toInt() + 1, 1)
            .atStartOfDay(ISTANBUL).toInstant().toEpochMilli()

    /**
     * [publishXp] is what to write now; [freshStart] says this write opens the month for the profile and so must carry
     * a new `periodStartAt`.
     */
    data class Plan(val publishXp: Long, val freshStart: Boolean) {
        fun isComplete(targetXp: Long): Boolean = publishXp >= targetXp
    }

    /**
     * @param storedPeriodId the profile's `periodId` on the server (null when the profile has none yet)
     * @param storedStartMillis its `periodStartAt` (null when it has none)
     * @param storedXp its `periodXp` when that belongs to [periodId], else null
     */
    fun plan(
        targetXp: Long,
        periodId: Long,
        storedPeriodId: Long?,
        storedStartMillis: Long?,
        storedXp: Long?,
        nowMillis: Long
    ): Plan {
        val fresh = storedStartMillis == null || storedPeriodId != periodId
        val start = if (fresh) nowMillis else storedStartMillis!!
        val byMonth = BURST_XP + (nowMillis - monthStartMillis(periodId)) / MILLIS_PER_XP
        val byAccount = BURST_XP + (nowMillis - start) / MILLIS_PER_XP
        val cap = (minOf(CEILING_XP, byMonth, byAccount) - SAFETY_MARGIN_XP).coerceAtLeast(0L)
        // Never write less than the server already holds for this month just because this phone's clock is behind:
        // the stored value was accepted once, so it is within the rules. A smaller target (a penalty) still goes down.
        val allowed = maxOf(cap, if (fresh) 0L else storedXp ?: 0L)
        return Plan(publishXp = targetXp.coerceIn(0L, allowed), freshStart = fresh)
    }
}
