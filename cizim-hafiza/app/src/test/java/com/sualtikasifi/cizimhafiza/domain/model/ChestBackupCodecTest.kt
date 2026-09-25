package com.sualtikasifi.cizimhafiza.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChestBackupCodecTest {

    @Test
    fun `slots round trip with positions and countdowns intact`() {
        val slots = listOf(
            Chest("a", ChestTier.SILVER),
            null,
            Chest("b", ChestTier.RARE, unlockStartedAtMillis = 1_700_000_000_000L),
            null
        )
        assertEquals(slots, ChestBackupCodec.decode(ChestBackupCodec.encode(slots)))
    }

    @Test
    fun `an old backup with no chests restores four empty slots`() {
        assertEquals(List(ChestSlots.SLOT_COUNT) { null }, ChestBackupCodec.decode(emptyList()))
    }

    @Test
    fun `a corrupt entry becomes an empty slot instead of failing the restore`() {
        val decoded = ChestBackupCodec.decode(listOf("garbage", "x|NOPE|-", "ok|GOLD|-", ""))
        assertNull(decoded[0])
        assertNull(decoded[1])
        assertEquals(Chest("ok", ChestTier.GOLD), decoded[2])
        assertNull(decoded[3])
    }
}
