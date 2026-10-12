package com.sualtikasifi.cizimhafiza.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LoginStreakTest {
    private val today = 20_000L

    @Test
    fun `a first ever login can be claimed`() {
        assertEquals(LoginStreakState.Claimable(0), LoginStreaks.evaluate(0, -1L, today))
    }

    @Test
    fun `the next day continues the streak`() {
        assertEquals(LoginStreakState.Claimable(4), LoginStreaks.evaluate(4, today - 1, today))
    }

    @Test
    fun `the same day is already claimed`() {
        assertEquals(LoginStreakState.ClaimedToday, LoginStreaks.evaluate(4, today, today))
    }

    @Test
    fun `a skipped day breaks the streak and offers the rescue`() {
        assertEquals(LoginStreakState.Missed(4), LoginStreaks.evaluate(4, today - 2, today))
        assertEquals(LoginStreakState.Missed(9), LoginStreaks.evaluate(9, today - 30, today))
    }

    @Test
    fun `a claim date in the future is never claimable`() {
        assertEquals(LoginStreakState.ClaimedToday, LoginStreaks.evaluate(4, today + 3, today))
    }

    @Test
    fun `the calendar repeats every seven days`() {
        assertEquals(1, LoginStreakRewards.cycleDayFor(0))
        assertEquals(7, LoginStreakRewards.cycleDayFor(6))
        assertEquals(1, LoginStreakRewards.cycleDayFor(7))
        assertEquals(3, LoginStreakRewards.cycleDayFor(23))
    }

    @Test
    fun `every thirtieth day adds the milestone chest`() {
        assertNull(LoginStreakRewards.milestoneFor(29))
        assertEquals(ChestTier.RARE, LoginStreakRewards.milestoneFor(30))
        assertEquals(ChestTier.RARE, LoginStreakRewards.milestoneFor(60))
        assertNull(LoginStreakRewards.milestoneFor(0))
    }

    @Test
    fun `every day of the calendar gives something`() {
        (1..LoginStreakRewards.CYCLE_DAYS).forEach { day ->
            val r = LoginStreakRewards.forDay(day)
            assert(r.gold > 0 || r.xp > 0 || r.jokers.isNotEmpty() || r.chest != null) { "day $day is empty" }
        }
    }
}
