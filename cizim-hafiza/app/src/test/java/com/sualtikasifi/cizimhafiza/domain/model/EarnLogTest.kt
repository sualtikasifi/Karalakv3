package com.sualtikasifi.cizimhafiza.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EarnLogTest {
    @Test
    fun `amounts add up per source and per kind`() {
        var log: String? = null
        log = EarnLog.add(log, 100, "xp", EarnSource.GAME, 10)
        log = EarnLog.add(log, 100, "xp", EarnSource.GAME, 5)
        log = EarnLog.add(log, 100, "xp", EarnSource.COUPON, 50)
        log = EarnLog.add(log, 100, "gold", EarnSource.CHEST, 300)
        val t = EarnLog.parse(log)
        assertEquals(100L, t.period)
        assertEquals(mapOf("game" to 15L, "coupon" to 50L), t.xp)
        assertEquals(mapOf("chest" to 300L), t.gold)
    }

    @Test
    fun `a new month starts the log again`() {
        var log: String? = EarnLog.add(null, 100, "xp", EarnSource.GAME, 10)
        log = EarnLog.add(log, 101, "xp", EarnSource.ONLINE, 7)
        val t = EarnLog.parse(log)
        assertEquals(101L, t.period)
        assertEquals(mapOf("online" to 7L), t.xp)
    }

    @Test
    fun `nothing or garbage reads as an empty log, and a non-positive amount adds nothing`() {
        assertTrue(EarnLog.parse(null).isEmpty)
        assertTrue(EarnLog.parse("not json").isEmpty)
        assertEquals("x", EarnLog.add("x", 1, "xp", EarnSource.GAME, 0))
    }

    @Test
    fun `every source has its own key and a label`() {
        assertEquals(EarnSource.entries.size, EarnSource.entries.map { it.key }.toSet().size)
        assertEquals("Kupon", EarnSource.labelOf("coupon"))
        assertEquals("zzz", EarnSource.labelOf("zzz"))
    }
}
