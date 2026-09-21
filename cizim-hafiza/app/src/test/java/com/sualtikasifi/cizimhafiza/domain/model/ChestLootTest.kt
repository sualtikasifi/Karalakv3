package com.sualtikasifi.cizimhafiza.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ChestLootTest {

    private fun rolls(tier: ChestTier, owned: Set<String> = emptySet(), n: Int = 4_000) =
        (0 until n).map { ChestLoot.roll(tier, owned, Random(it)) }

    @Test
    fun `gold always stays inside the tier range and grows with rarity`() {
        ChestTier.entries.forEach { tier ->
            rolls(tier).forEach { assertTrue("${tier.name} paid ${it.gold}", it.gold in tier.goldReward) }
        }
        assertTrue(ChestTier.SILVER.goldReward.last < ChestTier.GOLD.goldReward.first)
        assertTrue(ChestTier.GOLD.goldReward.last < ChestTier.RARE.goldReward.first)
    }

    @Test
    fun `joker counts follow the tier rules`() {
        rolls(ChestTier.SILVER).forEach { assertTrue(it.jokers.values.sum() in 0..1) }
        rolls(ChestTier.GOLD).forEach { assertTrue(it.jokers.values.sum() in 1..2) }
        rolls(ChestTier.RARE).forEach { assertTrue(it.jokers.values.sum() in 2..3) }
    }

    @Test
    fun `a common chest gives a joker about three times in ten`() {
        val withJoker = rolls(ChestTier.SILVER).count { it.jokers.isNotEmpty() }
        assertTrue("got $withJoker of 4000", withJoker in 1_000..1_400)
    }

    @Test
    fun `only a legendary chest can drop a pen, an unowned store pen within the price cap`() {
        ChestTier.entries.filter { it != ChestTier.RARE }.forEach { tier ->
            rolls(tier).forEach { assertNull(it.penDrop) }
        }
        val drops = rolls(ChestTier.RARE).mapNotNull { it.penDrop }
        assertTrue("expected some pen drops", drops.isNotEmpty())
        drops.forEach {
            assertTrue(it.isStoreItem)
            assertTrue(it.storePrice <= 6_500)
        }
        val rate = drops.size / 4_000.0
        assertTrue("pen drop rate $rate", rate in 0.08..0.16)
    }

    @Test
    fun `a pen the player already owns is never dropped`() {
        val owned = PenSkin.entries.filter { it.isStoreItem }.map { "pen:${it.name}" }.toSet()
        rolls(ChestTier.RARE, owned).forEach { assertNull(it.penDrop) }
    }

    @Test
    fun `reward tier is the chest tier`() {
        ChestTier.entries.forEach { assertEquals(it, ChestLoot.roll(it, emptySet(), Random(1)).tier) }
        assertNotNull(ChestLoot.roll(ChestTier.GOLD, emptySet(), Random(2)))
    }
}
