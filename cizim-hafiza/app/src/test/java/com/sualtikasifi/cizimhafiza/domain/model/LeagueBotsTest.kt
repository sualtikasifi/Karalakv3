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
            0 to Triple("karaeylem", 7255, 43),
            1 to Triple("kemalpasali", 6426, 14),
            2 to Triple("cbf150", 8609, 58),
            10 to Triple("hsncn", 8144, 55),
            20 to Triple("tekbasina", 5226, 11)
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
            // A bot earns roughly 3,000–5,500 XP a day: the league has to feel like a race.
            assertTrue("daily gain ${b.periodXp - a.periodXp}", (b.periodXp - a.periodXp) in 2_500..6_200)
        }
    }

    @Test
    fun startOfMonthBeginsAtZero() {
        LeagueBots.bots(periodId, monthStart, monthStart).forEach { assertEquals(0, it.periodXp) }
    }

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
    fun theLeagueIsARaceAllMonth() {
        // by the middle of the month every bot has a score worth chasing, and the table is not a list of beginners
        val start = LeagueBots.monthStartMillis(LocalDate.of(2026, 10, 2))
        val bots = LeagueBots.bots(periodId, start + 14 * 24 * 3_600_000L, start)
        assertTrue("slowest bot has only ${bots.minOf { it.periodXp }}", bots.minOf { it.periodXp } >= 35_000)
        assertTrue("fastest bot has only ${bots.maxOf { it.periodXp }}", bots.maxOf { it.periodXp } >= 60_000)
    }

    @Test
    fun levelsDoNotRunAwayWithTheScore() {
        // the level moves with a fifth of the league XP: a veteran stays a veteran, a rookie does not jump to the cap by the 20th
        val start = LeagueBots.monthStartMillis(LocalDate.of(2026, 10, 2))
        val early = LeagueBots.bots(periodId, start + 24 * 3_600_000L, start)
        val late = LeagueBots.bots(periodId, start + 27 * 24 * 3_600_000L, start)
        early.zip(late).forEach { (a, b) -> assertTrue("${a.nickname}: ${a.level} -> ${b.level}", b.level >= a.level && b.level - a.level <= 25) }
    }
}
