package com.sualtikasifi.cizimhafiza.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DailyJokerTest {

    @Test
    fun `rotates first letter, letter count, extra time, then starts over`() {
        val start = 20_000L
        val seen = (0L..5L).map { DailyJoker.typeFor(start + it) }
        assertEquals(seen[0], seen[3])
        assertEquals(seen[1], seen[4])
        assertEquals(seen[2], seen[5])
        assertEquals(setOf(JokerType.FIRST_LETTER, JokerType.LETTER_COUNT, JokerType.EXTRA_TIME), seen.take(3).toSet())
    }

    @Test
    fun `two consecutive days never offer the same joker`() {
        (0L..30L).forEach { day -> assertNotEquals(DailyJoker.typeFor(day), DailyJoker.typeFor(day + 1)) }
    }

    @Test
    fun `order follows the requested sequence`() {
        val first = (0L..2L).first { DailyJoker.typeFor(it) == JokerType.FIRST_LETTER }
        assertEquals(JokerType.LETTER_COUNT, DailyJoker.typeFor(first + 1))
        assertEquals(JokerType.EXTRA_TIME, DailyJoker.typeFor(first + 2))
    }

    @Test
    fun `letter groups count letters per word and skip spaces`() {
        assertEquals(listOf(3, 6), letterGroupsOf("Gün batımı"))
        assertEquals(listOf(9, 7), letterGroupsOf("Haşlanmış yumurta"))
        assertEquals(listOf(4), letterGroupsOf("Kedi"))
        assertEquals(emptyList<Int>(), letterGroupsOf("  - "))
    }
}
