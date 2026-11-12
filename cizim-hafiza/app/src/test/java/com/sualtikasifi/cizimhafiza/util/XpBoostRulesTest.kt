package com.sualtikasifi.cizimhafiza.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class XpBoostRulesTest {
    private val now = 5_000_000_000L

    @Test fun three_a_day_then_none() {
        assertEquals(3, XpBoostRules.offersLeft(storedDay = -1, storedCount = 0, today = 100))
        assertEquals(2, XpBoostRules.offersLeft(100, 1, 100))
        assertEquals(0, XpBoostRules.offersLeft(100, 3, 100))
        assertEquals(0, XpBoostRules.offersLeft(100, 9, 100))
    }

    @Test fun a_new_day_starts_again() = assertEquals(3, XpBoostRules.offersLeft(100, 3, 101))

    @Test fun a_boost_runs_for_thirty_minutes() {
        val until = now + XpBoostRules.DURATION_MILLIS
        assertTrue(XpBoostRules.isActive(until, now))
        assertTrue(XpBoostRules.isActive(until, now + XpBoostRules.DURATION_MILLIS - 1))
        assertFalse(XpBoostRules.isActive(until, now + XpBoostRules.DURATION_MILLIS))
        assertEquals(2.0, XpBoostRules.multiplier(until, now), 0.0)
        assertEquals(1.0, XpBoostRules.multiplier(until, now + XpBoostRules.DURATION_MILLIS + 1), 0.0)
    }

    @Test fun a_clock_set_back_cannot_stretch_a_boost() {
        val until = now + XpBoostRules.DURATION_MILLIS
        // the phone's clock now says it is a day earlier: the deadline is far further than one boost can last
        assertFalse(XpBoostRules.isActive(until, now - 86_400_000L))
    }
}
