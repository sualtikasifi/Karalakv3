package com.sualtikasifi.cizimhafiza.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class EconomyConfigTest {

    @After
    fun backToDefaults() = EconomyConfig.apply(null)

    @Test
    fun aMissingDocumentKeepsTheDesignedAmounts() {
        EconomyConfig.apply(null)
        assertEquals(10, GameConstants.GOLD_PER_CORRECT_WORD)
        assertEquals(600, GameConstants.dailyChallengeGold(10))
        assertEquals(500, EconomyConfig.adGoldAmount)
    }

    @Test
    fun serverValuesAreTakenWithinTheirRange() {
        EconomyConfig.apply(mapOf("goldPerCorrectWord" to 15L, "dailyGoldBase" to 400, "adGoldCooldownHours" to 2.0))
        assertEquals(15, GameConstants.GOLD_PER_CORRECT_WORD)
        assertEquals(400 + 30 * 3, GameConstants.dailyChallengeGold(3))
        assertEquals(2, EconomyConfig.adGoldCooldownHours)
    }

    @Test
    fun outOfRangeOrWrongTypedValuesKeepTheDefault() {
        EconomyConfig.apply(mapOf("goldPerCorrectWord" to 0, "adGoldAmount" to "lots", "adGoldCooldownHours" to 1000))
        assertEquals(10, EconomyConfig.goldPerCorrectWord)
        assertEquals(500, EconomyConfig.adGoldAmount)
        assertEquals(3, EconomyConfig.adGoldCooldownHours)
    }
}
