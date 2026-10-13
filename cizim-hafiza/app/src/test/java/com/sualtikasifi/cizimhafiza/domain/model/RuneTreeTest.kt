package com.sualtikasifi.cizimhafiza.domain.model

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
        RuneTree.placeholders.forEach { assertNotNull(RuneTree.get(it.requires)) }
    }

    @Test
    fun costsGrowWithEachLevel() {
        RuneTree.all.filter { it.id != RuneTree.ROOT_ID }.forEach { rune ->
            assertTrue(rune.costs.zipWithNext().all { (a, b) -> a < b })
        }
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
        assertEquals(300, RuneTree.nextCost(emptyMap(), word))
        assertEquals(600, RuneTree.nextCost(mapOf(RuneTree.WORD_GOLD to 1), word))
        assertNull(RuneTree.nextCost(mapOf(RuneTree.WORD_GOLD to 3), word))
        assertNull(RuneTree.nextCost(emptyMap(), RuneTree.get(RuneTree.ROOT_ID)!!))
    }

    @Test
    fun effectsScaleTheBaseValues() {
        assertEquals(10, RuneEffects.goldPerWord(emptyMap(), 10))
        assertEquals(13, RuneEffects.goldPerWord(mapOf(RuneTree.WORD_GOLD to 3), 10))
        assertEquals(120, RuneEffects.loginGold(mapOf(RuneTree.LOGIN_GOLD to 1), 100))
        assertEquals(15, RuneEffects.chestGoldPercent(mapOf(RuneTree.CHEST_GOLD to 3)))
    }

    @Test
    fun somethingIsAffordableOnlyWithEnoughGold() {
        assertFalse(RuneTree.anyAffordable(emptyMap(), 299))
        assertTrue(RuneTree.anyAffordable(emptyMap(), 300))
    }
}
