package com.sualtikasifi.cizimhafiza.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JokerTypeTest {

    @Test
    fun `a single joker costs its list price`() {
        JokerType.entries.forEach { assertEquals(it.price, it.priceFor(1)) }
    }

    @Test
    fun `a bundle is cheaper per piece than singles but never free`() {
        JokerType.entries.forEach {
            val bundle = it.priceFor(JokerType.BULK_QUANTITY)
            assertTrue(bundle < it.price * JokerType.BULK_QUANTITY)
            assertTrue(bundle > 0)
        }
        assertEquals(510, JokerType.FIRST_LETTER.priceFor(5)) // 5 x 120 less 15%
    }

    @Test
    fun `prices are positive and names are stable identifiers`() {
        // Persisted verbatim in the joker inventory and the cloud backup.
        assertEquals(listOf("FIRST_LETTER", "LETTER_COUNT", "EXTRA_TIME"), JokerType.entries.map { it.name })
        JokerType.entries.forEach { assertTrue(it.price > 0) }
    }

    @Test
    fun `store pens are priced and sit outside the level ladder`() {
        PenSkin.entries.filter { it.isStoreItem }.forEach {
            assertEquals("${it.name} must not unlock by level", 0, it.unlockLevel)
            assertTrue(!it.isLeagueReward)
            assertTrue(it !in PenSkin.ladder)
            assertTrue(it.storePrice >= 1_000)
        }
    }
}
