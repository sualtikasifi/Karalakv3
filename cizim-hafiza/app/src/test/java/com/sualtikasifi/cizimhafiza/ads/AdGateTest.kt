package com.sualtikasifi.cizimhafiza.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdGateTest {
    private val limits = AdGate.Limits()
    private val now = 10_000_000_000L
    private fun state(
        seen: Int = 10,
        lastFull: Long = 0L,
        lastRewarded: Long = 0L,
        recent: List<Long> = emptyList()
    ) = AdGate.State(seen, lastFull, lastRewarded, recent)

    @Test fun an_ordinary_moment_is_allowed() = assertTrue(AdGate.allows(state(), limits, now))

    @Test fun a_new_player_has_the_first_games_ad_free() {
        assertFalse(AdGate.allows(state(seen = 1), limits, now))
        assertFalse(AdGate.allows(state(seen = 5), limits, now))
        assertTrue(AdGate.allows(state(seen = 6), limits, now))
    }

    @Test fun two_fullscreen_ads_are_never_closer_than_the_gap() {
        assertFalse(AdGate.allows(state(lastFull = now - 60_000), limits, now))
        assertTrue(AdGate.allows(state(lastFull = now - 91_000), limits, now))
    }

    @Test fun an_interstitial_waits_after_a_rewarded_ad() {
        assertFalse(AdGate.allows(state(lastFull = now - 120_000, lastRewarded = now - 120_000), limits, now))
        assertTrue(AdGate.allows(state(lastFull = now - 200_000, lastRewarded = now - 200_000), limits, now))
    }

    @Test fun the_hourly_cap_holds() {
        val six = List(6) { now - (it + 1) * 8 * 60_000L }
        assertFalse(AdGate.allows(state(recent = six), limits, now))
        // one of them slides out of the hour
        val old = listOf(now - 61 * 60_000L) + six.drop(1)
        assertTrue(AdGate.allows(state(recent = old), limits, now))
    }

    @Test fun a_clock_set_back_cannot_block_ads_for_good() {
        assertTrue(AdGate.allows(state(lastFull = now + 3_600_000, lastRewarded = now + 3_600_000), limits, now))
    }

    @Test fun the_limits_can_be_changed_from_the_server() {
        val tight = AdGate.Limits(minGapMillis = 300_000, perHour = 2)
        assertFalse(AdGate.allows(state(lastFull = now - 200_000), tight, now))
        assertFalse(AdGate.allows(state(recent = listOf(now - 600_000, now - 900_000)), tight, now))
    }

    @Test fun only_the_last_hour_is_kept() {
        val kept = AdGate.keepRecent(listOf(now - 7_200_000, now - 1_000, now - 2_000), now)
        assertEquals(listOf(now - 2_000, now - 1_000), kept)
    }
}
