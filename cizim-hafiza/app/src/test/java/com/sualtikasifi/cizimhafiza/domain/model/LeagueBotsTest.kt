package com.sualtikasifi.cizimhafiza.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Pins the phone-side bots to the figures the server's TypeScript generator produces for the same moment
 * (reference values computed with functions/src/index.ts's own functions), so the two never drift apart.
 */
class LeagueBotsTest {
    private val periodId = 2026L * 12 + 9           // October 2026
    private val monthStart = LeagueBots.monthStartMillis(LocalDate.of(2026, 10, 2))
    private val now = 1790942400000L                // 2026-10-02 12:00 UTC

    @Test
    fun monthStartIsMidnightInIstanbul() {
        assertEquals(1790802000000L, monthStart)
    }

    @Test
    fun matchesTheServerGenerator() {
        val bots = LeagueBots.bots(periodId, now, monthStart)
        assertEquals(LeagueBots.COUNT, bots.size)
        val expected = mapOf(
            0 to Triple("karaeylem", 7413, 48),
            1 to Triple("kemalpasali", 7728, 32),
            2 to Triple("cbf150", 8353, 57),
            10 to Triple("hsncn", 8154, 55),
            20 to Triple("tekbasina", 5377, 29)
        )
        expected.forEach { (i, want) ->
            assertEquals("name of bot $i", want.first, bots[i].nickname)
            assertEquals("xp of bot $i", want.second, bots[i].periodXp)
            assertEquals("level of bot $i", want.third, bots[i].level)
        }
    }

    @Test
    fun namesAreUniqueAndScoresOnlyGrow() {
        val early = LeagueBots.bots(periodId, now, monthStart)
        val later = LeagueBots.bots(periodId, now + 24 * 3_600_000L, monthStart)
        assertEquals(LeagueBots.COUNT, early.map { it.nickname }.toSet().size)
        early.zip(later).forEach { (a, b) ->
            assertTrue(b.periodXp > a.periodXp)
            // A bot earns roughly 3,000–5,500 XP a day.
            assertTrue("daily gain ${b.periodXp - a.periodXp}", (b.periodXp - a.periodXp) in 2_500..6_200)
        }
    }

    @Test
    fun startOfMonthBeginsAtZero() {
        LeagueBots.bots(periodId, monthStart, monthStart).forEach { assertEquals(0, it.periodXp) }
    }
}
