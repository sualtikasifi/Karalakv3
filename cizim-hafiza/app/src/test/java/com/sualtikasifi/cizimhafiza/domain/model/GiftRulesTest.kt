package com.sualtikasifi.cizimhafiza.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GiftRulesTest {
    @Test fun goldFeeIsBurnedTwentyPercent() {
        assertEquals(80, GiftRules.goldReceived(100))
        assertEquals(800, GiftRules.goldReceived(1000))
    }

    @Test fun itemCostAddsTenPercentRoundedUp() {
        assertEquals(110, GiftRules.itemCost(100))
        assertEquals(332, GiftRules.itemCost(301))
    }

    @Test fun everyOfferFitsTheCeilingsAndKeepsPrizesOut() {
        val offers = GiftRules.offers()
        assertTrue(offers.all { it.cost in GiftRules.MIN_COST..GiftRules.MAX_PER_FRIEND_PER_DAY })
        assertTrue(offers.none { it.kind == GiftKind.PEN && PenSkin.valueOf(it.itemId).let { p -> p.isRuneReward || p.isFriendReward || p.isLeagueReward } })
        assertTrue(offers.none { it.kind == GiftKind.FRAME && AvatarFrame.valueOf(it.itemId).let { f -> f.isRuneReward || f.isFriendReward || f.isLeagueReward } })
        assertTrue(offers.any { it.kind == GiftKind.GOLD })
    }

    @Test fun usageChecksEveryCeiling() {
        val offer = GiftOffer(GiftKind.GOLD, "", 1, 500, 400)
        assertTrue(GiftUsage(0, 0, 0).canAfford(offer))
        assertFalse(GiftUsage(4600, 1, 0).canAfford(offer))
        assertFalse(GiftUsage(0, 3, 0).canAfford(offer))
        assertFalse(GiftUsage(0, 1, 600).canAfford(offer))
    }
}
