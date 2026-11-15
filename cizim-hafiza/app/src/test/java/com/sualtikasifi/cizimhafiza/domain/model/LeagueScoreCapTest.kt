package com.sualtikasifi.cizimhafiza.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The app's mirror of firestore.rules' plausibleMonthScore: what it plans must be what the rules accept. */
class LeagueScoreCapTest {

    private val october = 2026L * 12 + 9
    private val monthStart = LeagueScoreCap.monthStartMillis(october)
    private val hour = 3_600_000L

    @Test
    fun `the month starts at midnight Istanbul on the 1st`() {
        assertEquals(java.time.Instant.parse("2026-09-30T21:00:00Z").toEpochMilli(), monthStart)
        assertEquals(java.time.Instant.parse("2026-12-31T21:00:00Z").toEpochMilli(), LeagueScoreCap.monthStartMillis(2027L * 12))
    }

    @Test
    fun `a first score of the month opens the month and is held to the burst`() {
        val plan = LeagueScoreCap.plan(100_000, october, storedPeriodId = null, storedStartMillis = null, storedXp = null, nowMillis = monthStart + 200 * hour)
        assertTrue(plan.freshStart)
        assertEquals(LeagueScoreCap.BURST_XP - LeagueScoreCap.SAFETY_MARGIN_XP, plan.publishXp)
        assertFalse(plan.isComplete(100_000))
    }

    @Test
    fun `an honest score goes through whole`() {
        val plan = LeagueScoreCap.plan(4_500, october, october, monthStart + hour, 3_000, monthStart + 2 * hour)
        assertFalse(plan.freshStart)
        assertEquals(4_500L, plan.publishXp)
        assertTrue(plan.isComplete(4_500))
    }

    @Test
    fun `the allowance grows by 3000 an hour from the account's own start`() {
        val start = monthStart + 10 * hour
        val plan = LeagueScoreCap.plan(1_000_000, october, october, start, 29_000, start + 10 * hour)
        assertEquals(30_000L + 30_000L - LeagueScoreCap.SAFETY_MARGIN_XP, plan.publishXp)
    }

    @Test
    fun `never above the ceiling`() {
        val plan = LeagueScoreCap.plan(1_000_000, october, october, monthStart, 200_000, monthStart + 25 * 24 * hour)
        assertEquals(LeagueScoreCap.CEILING_XP - LeagueScoreCap.SAFETY_MARGIN_XP, plan.publishXp)
    }

    @Test
    fun `a new month is a fresh start even with last month's stamp`() {
        val plan = LeagueScoreCap.plan(500, october, october - 1, monthStart - 5 * 24 * hour, null, monthStart + hour)
        assertTrue(plan.freshStart)
        assertEquals(500L, plan.publishXp)
    }

    @Test
    fun `a clock behind the server never lowers what is already published`() {
        // The phone thinks less time has passed than the server does; the stored score was accepted, so it stays.
        val start = monthStart + 100 * hour
        val plan = LeagueScoreCap.plan(80_000, october, october, start, 70_000, start + hour)
        assertEquals(70_000L, plan.publishXp)
    }

    @Test
    fun `a penalty still brings the score down`() {
        val plan = LeagueScoreCap.plan(5_000, october, october, monthStart, 9_000, monthStart + 10 * hour)
        assertEquals(5_000L, plan.publishXp)
    }
}
