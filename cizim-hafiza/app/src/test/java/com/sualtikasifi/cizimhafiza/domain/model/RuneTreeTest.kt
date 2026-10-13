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
    fun theMapIsBigAndTakesAboutAYear() {
        assertTrue(RuneTree.all.size >= 38)
        assertTrue("total ${RuneTree.totalCost}", RuneTree.totalCost in 400_000..550_000)
    }

    @Test
    fun noTwoNodesSitOnTopOfEachOther() {
        val points = RuneTree.all.map { it.x to it.y } + RuneTree.placeholders.map { it.x to it.y }
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
        val login = RuneTree.get(RuneTree.LOGIN_GOLD)!!
        assertTrue(RuneTree.isUnlocked(emptyMap(), word))
        assertFalse(RuneTree.isUnlocked(emptyMap(), login))
        assertTrue(RuneTree.isUnlocked(mapOf(RuneTree.WORD_GOLD to 1), login))
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
    fun levelsOfTheSameEffectAddUp() {
        val a = RuneTree.get("word_gold")!!
        val b = RuneTree.get("word_2")!!
        val levels = mapOf(a.id to 3, b.id to 2)
        assertEquals(3 * a.perLevel + 2 * b.perLevel, RuneEffects.total(levels, RuneEffect.WORD))
        assertEquals(100, RuneEffects.goldPerWord(emptyMap(), 100))
        assertEquals(100 + 3 * a.perLevel, RuneEffects.goldPerWord(mapOf(a.id to 3), 100))
    }

    @Test
    fun theDiscountIsCappedAndNothingIsFreeByDefault() {
        assertEquals(0, RuneEffects.jokerDiscountPercent(emptyMap()))
        val all = RuneTree.all.associate { it.id to it.maxLevel }
        assertTrue(RuneEffects.jokerDiscountPercent(all) <= 60)
        assertEquals(100, JokerType.FIRST_LETTER.priceFor(1, 0).let { 100 })
    }

    @Test
    fun somethingIsAffordableOnlyWithEnoughGold() {
        val cheapest = RuneTree.all.filter { it.id != RuneTree.ROOT_ID && it.requires == listOf(RuneTree.ROOT_ID) }.minOf { it.costs[0] }
        assertFalse(RuneTree.anyAffordable(emptyMap(), cheapest - 1))
        assertTrue(RuneTree.anyAffordable(emptyMap(), cheapest))
    }
}
