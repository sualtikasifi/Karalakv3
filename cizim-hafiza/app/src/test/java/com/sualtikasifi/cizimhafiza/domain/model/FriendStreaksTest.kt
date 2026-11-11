package com.sualtikasifi.cizimhafiza.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FriendStreaksTest {
    private val me = "bbb"
    private val friend = "aaa"
    private val today = 20_000

    private fun streak(count: Int = 5, lastDay: Int = today - 1, pending: Int = -1, run: Int = 1, repairs: Int = 0) = FriendStreak(
        pairId = FriendStreaks.pairId(me, friend), friendUid = friend, count = count, best = count, run = run,
        lastDay = lastDay, pendingDay = pending, repairs = repairs, repairedBy = null, updatedAt = 0L
    )

    private fun StreakWrite.fields(): Map<String, Any> = (this as StreakWrite.Save).fields

    @Test
    fun `the pair id is the same from both sides`() {
        assertEquals("aaa_bbb", FriendStreaks.pairId("aaa", "bbb"))
        assertEquals("aaa_bbb", FriendStreaks.pairId("bbb", "aaa"))
    }

    @Test
    fun `the first duel starts a streak of one`() {
        val f = FriendStreaks.afterDuel(null, me, friend, today, 1L).fields()
        assertEquals(1, f["count"]); assertEquals(1, f["run"]); assertEquals(today, f["lastDay"]); assertEquals(-1, f["pendingDay"])
        assertEquals(listOf("aaa", "bbb"), f["uids"])
    }

    @Test
    fun `a duel the day after carries the streak on`() {
        val f = FriendStreaks.afterDuel(streak(count = 5, lastDay = today - 1), me, friend, today, 1L).fields()
        assertEquals(6, f["count"]); assertEquals(6, f["best"]); assertEquals(today, f["lastDay"])
    }

    @Test
    fun `a second duel on the same day changes nothing`() {
        assertEquals(StreakWrite.None, FriendStreaks.afterDuel(streak(lastDay = today), me, friend, today, 1L))
    }

    @Test
    fun `best keeps the highest ever`() {
        val s = streak(count = 2, lastDay = today - 1).copy(best = 9)
        assertEquals(9, FriendStreaks.afterDuel(s, me, friend, today, 1L).fields()["best"])
    }

    @Test
    fun `a duel while the streak is mendable is kept and not counted`() {
        val f = FriendStreaks.afterDuel(streak(count = 9, lastDay = today - 2), me, friend, today, 1L).fields()
        assertEquals(9, f["count"]); assertEquals(today - 2, f["lastDay"]); assertEquals(today, f["pendingDay"])
    }

    @Test
    fun `a kept duel is not written twice`() {
        assertEquals(StreakWrite.None, FriendStreaks.afterDuel(streak(count = 9, lastDay = today - 2, pending = today), me, friend, today, 1L))
    }

    @Test
    fun `a lost streak starts again as a new run`() {
        val f = FriendStreaks.afterDuel(streak(count = 9, lastDay = today - 6, run = 3), me, friend, today, 1L).fields()
        assertEquals(1, f["count"]); assertEquals(4, f["run"]); assertEquals(today, f["lastDay"])
    }

    @Test
    fun `a duel kept yesterday opens the new run on two`() {
        val f = FriendStreaks.afterDuel(streak(count = 9, lastDay = today - 3, pending = today - 1), me, friend, today, 1L).fields()
        assertEquals(2, f["count"]); assertEquals(2, f["run"])
    }

    @Test
    fun `mending covers the missed day and the streak carries on`() {
        val f = FriendStreaks.afterRepair(streak(count = 9, lastDay = today - 2), me, friend, today, 1L).fields()
        assertEquals(9, f["count"]); assertEquals(today - 1, f["lastDay"]); assertEquals(1, f["repairs"]); assertEquals(me, f["repairedBy"])
    }

    @Test
    fun `mending with today's duel already played counts that day too`() {
        val f = FriendStreaks.afterRepair(streak(count = 9, lastDay = today - 2, pending = today), me, friend, today, 1L).fields()
        assertEquals(10, f["count"]); assertEquals(today, f["lastDay"]); assertEquals(-1, f["pendingDay"])
    }

    @Test
    fun `only a streak that missed exactly one day can be mended`() {
        assertEquals(StreakWrite.None, FriendStreaks.afterRepair(streak(lastDay = today - 1), me, friend, today, 1L))
        assertEquals(StreakWrite.None, FriendStreaks.afterRepair(streak(lastDay = today - 3), me, friend, today, 1L))
    }

    @Test
    fun `status follows the last counted day`() {
        assertEquals(StreakStatus.Done, FriendStreaks.view(streak(lastDay = today), today).status)
        assertEquals(StreakStatus.AtRisk, FriendStreaks.view(streak(lastDay = today - 1), today).status)
        assertEquals(StreakStatus.Broken, FriendStreaks.view(streak(lastDay = today - 2), today).status)
        val lost = FriendStreaks.view(streak(lastDay = today - 3), today)
        assertEquals(StreakStatus.Lost, lost.status); assertEquals(0, lost.count)
    }

    @Test
    fun `a duel kept on a lost streak reads as a new streak of one`() {
        val v = FriendStreaks.view(streak(count = 9, lastDay = today - 3, pending = today - 1), today)
        assertEquals(1, v.count); assertEquals(StreakStatus.AtRisk, v.status)
    }

    @Test
    fun `milestones are reached in order`() {
        assertEquals(listOf(3, 7), FriendStreaks.milestonesReached(8))
        assertEquals(14, FriendStreaks.nextMilestone(7))
        assertNull(FriendStreaks.nextMilestone(100))
        assertTrue(FriendStreaks.MILESTONES.zipWithNext().all { (a, b) -> a < b })
    }
}
