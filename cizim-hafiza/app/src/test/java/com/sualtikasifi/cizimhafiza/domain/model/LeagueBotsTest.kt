package com.sualtikasifi.cizimhafiza.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Pins the phone-side bots to the figures the web game's JavaScript generator (hosting/oyna/league.js) produces for the same
 * moment, so the two never drift apart, and checks what the table has to look like: varied levels, and a level that
 * never contradicts the league score.
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
            0 to Triple("karaeylem", 3178, 41),
            1 to Triple("kemalpasali", 763, 10),
            2 to Triple("cbf150", 6365, 58),
            10 to Triple("hsncn", 5570, 54),
            20 to Triple("tekbasina", 374, 6)
        )
        expected.forEach { (i, want) ->
            assertEquals("name of bot $i", want.first, bots[i].nickname)
            // Math.pow may differ from the browser's in the last bit: a point of XP here or there is the same bot
            assertTrue("xp of bot $i: ${bots[i].periodXp}", kotlin.math.abs(bots[i].periodXp - want.second) <= 3)
            assertTrue("level of bot $i: ${bots[i].level}", kotlin.math.abs(bots[i].level - want.third) <= 1)
        }
    }

    @Test
    fun namesAreUniqueAndScoresOnlyGrow() {
        val early = LeagueBots.bots(periodId, now, monthStart)
        val later = LeagueBots.bots(periodId, now + 24 * 3_600_000L, monthStart)
        assertEquals(LeagueBots.COUNT, early.map { it.nickname }.toSet().size)
        early.zip(later).forEach { (a, b) ->
            assertTrue(b.periodXp > a.periodXp)
            // From a few hundred XP a day for a rookie to about 5,500 for a veteran.
            assertTrue("daily gain ${b.periodXp - a.periodXp}", (b.periodXp - a.periodXp) in 50..6_200)
        }
    }

    @Test
    fun startOfMonthBeginsAtZero() {
        LeagueBots.bots(periodId, monthStart, monthStart).forEach { assertEquals(0, it.periodXp) }
    }

    /** Lifetime XP that a level needs — the same curve the game uses. */
    private fun totalXpForLevel(level: Int): Int { val n = maxOf(level - 1, 0); return 25 * n * n + 75 * n }

    @Test
    fun theTableShowsEveryKindOfPlayer() {
        // across several months and days, the shown levels are spread from rookies to the cap, never bunched together
        for (month in 0 until 6) for (day in listOf(2, 10, 20, 28)) {
            val id = periodId + month
            val start = LeagueBots.monthStartMillis(LocalDate.of(2026, 10, 2).plusMonths(month.toLong()))
            val bots = LeagueBots.bots(id, start + day * 24 * 3_600_000L, start)
            val levels = bots.map { it.level }
            assertTrue("levels $levels (month $month, day $day) are too alike", levels.max() - levels.min() >= 30)
            // the cap (70) is where the veterans end up late in the month: a few there is normal, a crowd is not
            val perLevel = levels.groupingBy { it }.eachCount()
            assertTrue("too many bots on one level $levels", perLevel.filterKeys { it < 70 }.values.maxOrNull().let { it == null || it <= 4 })
            assertTrue("too many bots at the cap $levels", (perLevel[70] ?: 0) <= 7)
        }
    }

    @Test
    fun aLevelNeverContradictsTheLeagueScore() {
        for (month in 0 until 6) for (day in listOf(1, 5, 12, 20, 29)) {
            val id = periodId + month
            val start = LeagueBots.monthStartMillis(LocalDate.of(2026, 10, 2).plusMonths(month.toLong()))
            LeagueBots.bots(id, start + day * 24 * 3_600_000L, start).forEach {
                // the XP a bot earned this month is part of its lifetime XP: its level must be one that has room for it
                // (the shown level stops at the cap, which has room for anything)
                if (it.level < 70) assertTrue("${it.nickname}: ${it.periodXp} XP this month at level ${it.level}", it.periodXp < totalXpForLevel(it.level + 1))
                // and the busiest players are not the rookies: a bot with a big score is no beginner
                if (it.periodXp > 15_000) assertTrue("${it.nickname} has ${it.periodXp} XP at level ${it.level}", it.level >= 20)
            }
        }
    }
}
