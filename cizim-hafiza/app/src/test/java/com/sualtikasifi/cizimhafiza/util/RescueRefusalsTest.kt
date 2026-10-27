package com.sualtikasifi.cizimhafiza.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers [RescueRefusals]: the "serin kırılmak üzere" offer stops after it has been turned down three times for the
 * same lapse, so the game does not keep pushing an ad at someone who said no, while a later break is offered again.
 */
class RescueRefusalsTest {

    private val lapse = 19_998L

    @Test
    fun `three refusals of the same lapse end the offer`() {
        var refusedFor = -1L
        var refusals = 0
        repeat(RescueRefusals.MAX) { i ->
            assertFalse("still offered before refusal ${i + 1}", RescueRefusals.enough(refusedFor, refusals, lapse))
            refusals = RescueRefusals.next(refusedFor, refusals, lapse)
            refusedFor = lapse
        }
        assertEquals(3, refusals)
        assertTrue(RescueRefusals.enough(refusedFor, refusals, lapse))
    }

    @Test
    fun `a new lapse is offered again and counts from one`() {
        assertFalse(RescueRefusals.enough(lapse, 3, lapse + 20))
        assertEquals(1, RescueRefusals.next(lapse, 3, lapse + 20))
    }

    @Test
    fun `nothing refused yet never blocks the offer`() {
        assertFalse(RescueRefusals.enough(-1L, 0, lapse))
        assertEquals(1, RescueRefusals.next(-1L, 0, lapse))
    }
}
