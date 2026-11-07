package com.sualtikasifi.cizimhafiza.presentation.reports.console

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/** The statistics are read once and shared: how long the copy lives, and when it must be read again. */
class DaySeriesCacheTest {

    private fun series(n: Int) = DaySeries(
        days = List(n) { "2026-11-%02d".format(it + 1) },
        players = List(n) { it.toLong() },
        counts = List(n) { mapOf("app__open__count" to it.toLong()) },
        newPlayers = List(n) { it.toLong() },
        d1Return = List(n) { 0.5 },
        d7Return = List(n) { null }
    )

    private class Clock(var now: Long = 0L, var day: String = "2026-11-30")

    private fun cache(clock: Clock, ttl: Long = 600_000L) = DaySeriesCache(ttl, { clock.now }, { clock.day })

    @Test
    fun lastCutsEveryListTheSameWay() {
        val s = series(30).last(14)
        assertEquals(14, s.size)
        assertEquals(14, s.players.size)
        assertEquals(14, s.counts.size)
        assertEquals(14, s.newPlayers.size)
        assertEquals(14, s.d1Return.size)
        assertEquals("2026-11-30", s.days.last())
        assertEquals(29L, s.players.last())
        // asking for more than there is hands back what there is
        assertEquals(30, series(30).last(90).size)
    }

    @Test
    fun oneReadServesEveryScreenWithinTheTtl() = runBlocking {
        val clock = Clock()
        val c = cache(clock)
        var reads = 0
        val load: suspend (Int) -> DaySeries = { reads++; series(it) }
        assertEquals(14, c.get(14, load = load).size)
        clock.now += 60_000
        assertEquals(14, c.get(14, load = load).size)
        assertEquals(7, c.get(7, load = load).size)
        assertEquals(30, c.get(30, load = load).size)
        assertEquals(1, reads)
    }

    @Test
    fun theFirstReadTakesAMonthSoTheShorterViewsAreCutFromIt() = runBlocking {
        val asked = mutableListOf<Int>()
        cache(Clock()).get(7) { asked += it; series(it) }
        assertEquals(listOf(30), asked)
    }

    @Test
    fun refreshingTheTtlOrANewDayReadsAgain() = runBlocking {
        val clock = Clock()
        val c = cache(clock, ttl = 600_000L)
        var reads = 0
        val load: suspend (Int) -> DaySeries = { reads++; series(it) }
        c.get(14, load = load)
        c.get(14, force = true, load = load)
        assertEquals(2, reads)
        clock.now += 600_001
        c.get(14, load = load)
        assertEquals(3, reads)
        clock.day = "2026-12-01"
        c.get(14, load = load)
        assertEquals(4, reads)
    }

    @Test
    fun aLongerViewThanTheKeptCopyReadsAgainForIt() = runBlocking {
        val c = cache(Clock())
        val asked = mutableListOf<Int>()
        val load: suspend (Int) -> DaySeries = { asked += it; series(it) }
        c.get(14, load = load)
        assertEquals(90, c.get(90, load = load).size)
        assertEquals(listOf(30, 90), asked)
    }
}
