package com.sualtikasifi.cizimhafiza.domain.model

import kotlin.math.hypot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RuneTreeTest {

    @Test
    fun everyRequirementExistsAndIdsAreUnique() {
        val ids = RuneTree.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        RuneTree.all.forEach { rune -> rune.requires.forEach { assertNotNull("${rune.id} needs $it", RuneTree.get(it)) } }
    }

    @Test
    fun costsGrowWithEachLevel() {
        RuneTree.all.filter { it.id != RuneTree.ROOT_ID }.forEach { rune ->
            assertTrue(rune.costs.zipWithNext().all { (a, b) -> a < b })
        }
    }

    @Test
    fun eachEffectHasOneNodeAndEachRingHasItsOwnPrice() {
        val stats = RuneTree.all.filter { it.effect.unit != RuneUnit.FLAG && it.effect != RuneEffect.NONE }
        assertEquals(stats.size, stats.map { it.effect }.toSet().size)
        fun ringCost(ring: Int) = RuneTree.all.filter { it.ring == ring }.sumOf { it.costs.sum() }
        assertTrue("ring 1 ${ringCost(1)}", ringCost(1) in 150_000..200_000)
        assertTrue("ring 2 ${ringCost(2)}", ringCost(2) in 380_000..460_000)
        assertTrue("ring 3 ${ringCost(3)}", ringCost(3) in 300_000..400_000)
    }

    @Test
    fun everyAbilityIsTheEndOfAPath() {
        RuneTree.all.filter { it.effect.unit == RuneUnit.FLAG && it.effect != RuneEffect.STYLE }.forEach { assertTrue(it.id, it.isKeystone) }
    }

    @Test
    fun laterRingsOpenWithTheLevelAndTheShareOfTheRingBefore() {
        assertTrue(RuneTree.ringOpen(emptyMap(), 1, 1))
        // Level alone is not enough: the first ring is not far enough along.
        assertFalse(RuneTree.ringOpen(emptyMap(), 2, 50))
        val ring1 = RuneTree.all.filter { it.ring == 1 && it.id != RuneTree.ROOT_ID }
        val most = ring1.associate { it.id to it.maxLevel }
        assertTrue(RuneTree.ringProgress(most, 1) >= 0.99f)
        assertTrue(RuneTree.ringOpen(most, 2, RuneGates.ring2Level))
        // The share alone is not enough either: the level counts.
        assertFalse(RuneTree.ringOpen(most, 2, RuneGates.ring2Level - 1))
        assertFalse(RuneTree.ringOpen(most, 3, 100))
    }

    @Test
    fun theStackedEffectsStayUnderTheirCeilings() {
        val everything = RuneTree.all.associate { it.id to it.maxLevel }
        RuneEffect.entries.forEach { effect ->
            val cap = RuneEffects.capOf(effect) ?: return@forEach
            assertTrue(effect.name, RuneEffects.total(everything, effect) <= cap)
        }
    }

    @Test
    fun everyRuneHasItsIconAndTextAndTheOneOffRunesAreSingleLevel() {
        RuneTree.all.filter { it.effect.unit == RuneUnit.FLAG }.forEach { assertEquals(it.id, 1, it.maxLevel) }
        assertTrue(RuneRewards.storeItem.keys.all { RuneTree.get(it)?.effect == RuneEffect.STYLE })
    }

    @Test
    fun noTwoNodesSitOnTopOfEachOther() {
        val points = RuneTree.all.map { it.x to it.y }
        for (i in points.indices) for (j in i + 1 until points.size) {
            assertTrue("nodes $i and $j are too close", hypot(points[i].first - points[j].first, points[i].second - points[j].second) >= 0.8f)
        }
    }

    @Test
    fun everyRuneCanBeReachedFromTheCentre() {
        val reached = mutableSetOf(RuneTree.ROOT_ID)
        var grew = true
        while (grew) {
            grew = false
            RuneTree.all.filter { it.id !in reached && it.requires.all { r -> r in reached } }.forEach { reached += it.id; grew = true }
        }
        assertEquals(RuneTree.all.size, reached.size)
    }

    @Test
    fun aRuneOpensOnlyOnceItsNeighbourIsBought() {
        val word = RuneTree.get(RuneTree.WORD_GOLD)!!
        val win = RuneTree.get(RuneTree.WIN_GOLD)!!
        assertTrue(RuneTree.isUnlocked(emptyMap(), word))
        assertFalse(RuneTree.isUnlocked(emptyMap(), win))
        assertTrue(RuneTree.isUnlocked(mapOf(RuneTree.WORD_GOLD to 1), win))
    }

    @Test
    fun nextCostFollowsTheLevelAndEndsAtTheMax() {
        val word = RuneTree.get(RuneTree.WORD_GOLD)!!
        assertEquals(word.costs[0], RuneTree.nextCost(emptyMap(), word))
        assertEquals(word.costs[1], RuneTree.nextCost(mapOf(RuneTree.WORD_GOLD to 1), word))
        assertNull(RuneTree.nextCost(mapOf(RuneTree.WORD_GOLD to word.maxLevel), word))
        assertNull(RuneTree.nextCost(emptyMap(), RuneTree.get(RuneTree.ROOT_ID)!!))
    }

    @Test
    fun theWordRuneGrowsTheRoundsWholeGold() {
        val a = RuneTree.get(RuneTree.WORD_GOLD)!!
        assertEquals(3 * a.perLevel, RuneEffects.total(mapOf(a.id to 3), RuneEffect.WORD))
        assertEquals(60, RuneEffects.wordGold(emptyMap(), 60))
        // Six words of 10 gold at +8%: 64 for the round, where 10 per word rounded down would have paid nothing extra.
        assertEquals(64, RuneEffects.wordGold(mapOf(a.id to 1), 60))
    }

    @Test
    fun levelsAboveTheMaxAreHeldToIt() {
        val a = RuneTree.get(RuneTree.WORD_GOLD)!!
        assertEquals(a.maxLevel, RuneTree.levelOf(mapOf(a.id to 999), a.id))
        assertEquals(0, RuneTree.levelOf(mapOf("gone" to 3), "gone"))
    }

    @Test
    fun theDiscountIsCappedAndNothingIsFreeByDefault() {
        assertEquals(0, RuneEffects.jokerDiscountPercent(emptyMap()))
        val all = RuneTree.all.associate { it.id to it.maxLevel }
        assertTrue(RuneEffects.jokerDiscountPercent(all) <= 40)
        assertEquals(100, JokerType.FIRST_LETTER.priceFor(1, 0).let { 100 })
    }

    @Test
    fun somethingIsAffordableOnlyWithEnoughGold() {
        val cheapest = RuneTree.all.filter { it.id != RuneTree.ROOT_ID && it.requires == listOf(RuneTree.ROOT_ID) }.minOf { it.costs[0] }
        assertFalse(RuneTree.anyAffordable(emptyMap(), cheapest - 1, 1))
        assertTrue(RuneTree.anyAffordable(emptyMap(), cheapest, 1))
    }
}
