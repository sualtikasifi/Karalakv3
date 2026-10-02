package com.sualtikasifi.cizimhafiza.data.bot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The longest-honest-round limit that decides when a room's round is treated as abandoned. */
class RoundTimeLimitTest {
    @Test
    fun scalesWithWordCount() {
        assertEquals(450_000L, BotRoomEngine.maxRoundMillis(10))
        assertTrue(BotRoomEngine.maxRoundMillis(20) > BotRoomEngine.maxRoundMillis(10))
    }

    @Test
    fun alwaysLeavesRoomForTheRealClockOfARound() {
        // 10 words: at most ~10 s drawing + ~10 s guessing (+ hint bonuses and breaks) each — well under the limit.
        val honestRoundMs = 10 * (10_000L + 10_000L + 15_000L)
        assertTrue(BotRoomEngine.maxRoundMillis(10) >= honestRoundMs)
    }

    @Test
    fun isCapped() {
        assertEquals(20 * 60_000L, BotRoomEngine.maxRoundMillis(500))
    }
}
