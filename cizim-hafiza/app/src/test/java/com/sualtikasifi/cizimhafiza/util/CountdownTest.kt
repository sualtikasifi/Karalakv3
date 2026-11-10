package com.sualtikasifi.cizimhafiza.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CountdownTest {
    @Test fun `the shown second rounds up so the last second shows 1 until time is up`() {
        assertEquals(20, Countdown.secondsLeft(20_000))
        assertEquals(20, Countdown.secondsLeft(19_001))
        assertEquals(19, Countdown.secondsLeft(19_000))
        assertEquals(1, Countdown.secondsLeft(1))
        assertEquals(0, Countdown.secondsLeft(0))
        assertEquals(0, Countdown.secondsLeft(-5_000))
    }

    @Test fun `the next check is never longer than a second and never a busy loop`() {
        assertEquals(1_000, Countdown.nextCheckMs(20_000))
        assertEquals(500, Countdown.nextCheckMs(19_500))
        assertEquals(20, Countdown.nextCheckMs(19_001))
    }

    @Test fun `ordinary jitter is not carried but time spent away is`() {
        assertEquals(0L, Countdown.carryFor(300))
        assertEquals(0L, Countdown.carryFor(1_999))
        assertEquals(2_000L, Countdown.carryFor(2_000))
        assertEquals(45_000L, Countdown.carryFor(45_000))
    }

    @Test fun `time spent away comes off the next countdown and can use it up`() {
        assertEquals(15_000L, Countdown.startMs(20, 5_000))
        assertEquals(0L, Countdown.startMs(20, 20_000))
        // 45 s away across a 20 s word: the next word's 20 s are gone too, 25 s are still owed to the one after.
        assertEquals(-25_000L, Countdown.startMs(20, 45_000))
    }

    @Test fun `a checkpoint comes back to a clock that kept running`() {
        assertEquals(12_000L, Countdown.remainingFromCheckpoint(deadlineWallMs = 100_000, nowWallMs = 88_000))
        assertEquals(-8_000L, Countdown.remainingFromCheckpoint(deadlineWallMs = 100_000, nowWallMs = 108_000))
        assertNull(Countdown.remainingFromCheckpoint(deadlineWallMs = 0, nowWallMs = 88_000))
    }
}
