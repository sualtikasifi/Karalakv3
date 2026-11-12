package com.sualtikasifi.cizimhafiza.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PenTrialRulesTest {
    @Test fun a_new_day_allows_any_pen() = assertTrue(PenTrialRules.canTry("RUBY", 5, 3, setOf("RUBY"), 6))

    @Test fun a_pen_is_tried_once_a_day() {
        assertTrue(PenTrialRules.canTry("EMERALD", 5, 1, setOf("RUBY"), 5))
        assertFalse(PenTrialRules.canTry("RUBY", 5, 1, setOf("RUBY"), 5))
    }

    @Test fun three_a_day_at_most() {
        assertTrue(PenTrialRules.canTry("ICE", 5, 2, setOf("A", "B"), 5))
        assertFalse(PenTrialRules.canTry("ICE", 5, 3, setOf("A", "B", "C"), 5))
    }

    @Test fun an_unplayed_trial_expires() {
        val armed = 1_000_000_000L
        assertTrue(PenTrialRules.isLive(armed, armed + 1))
        assertFalse(PenTrialRules.isLive(armed, armed + PenTrialRules.EXPIRES_MILLIS))
        assertFalse(PenTrialRules.isLive(0, armed))
        // a clock set back: armed in the future
        assertFalse(PenTrialRules.isLive(armed + 5_000, armed))
    }
}
