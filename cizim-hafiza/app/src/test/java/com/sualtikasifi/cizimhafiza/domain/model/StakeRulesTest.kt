package com.sualtikasifi.cizimhafiza.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StakeRulesTest {
    private fun duel(status: DuelStatus, c: Int = 30, o: Int? = null, stake: Int = 500) = Duel(
        id = "d", challengerUid = "c", challengerName = "C", opponentUid = "o", opponentName = "O", items = emptyList(),
        challengerScore = c, challengerCorrectCount = 0, opponentScore = o, status = status, stake = stake
    )

    @Test fun potLosesFivePercentToTheBurn() {
        assertEquals(950, StakeRules.winnerPayout(500))
        assertEquals(1900, StakeRules.winnerPayout(1000))
        assertEquals(450, StakeRules.netWin(500))
    }

    @Test fun plainDuelPaysNothing() {
        assertNull(StakeRules.payout(duel(DuelStatus.COMPLETE, 30, 20, stake = 0), true))
    }

    @Test fun winnerGetsThePotAndLoserNothing() {
        val d = duel(DuelStatus.COMPLETE, c = 40, o = 30)
        assertEquals(950, StakeRules.payout(d, iAmChallenger = true))
        assertEquals(0, StakeRules.payout(d, iAmChallenger = false))
        val e = duel(DuelStatus.COMPLETE, c = 20, o = 30)
        assertEquals(0, StakeRules.payout(e, true))
        assertEquals(950, StakeRules.payout(e, false))
    }

    @Test fun tieReturnsBothStakes() {
        val d = duel(DuelStatus.COMPLETE, c = 30, o = 30)
        assertEquals(500, StakeRules.payout(d, true))
        assertEquals(500, StakeRules.payout(d, false))
    }

    @Test fun refusedAndExpiredOffersRefundOnlyTheChallenger() {
        for (s in listOf(DuelStatus.DECLINED, DuelStatus.EXPIRED)) {
            assertEquals(500, StakeRules.payout(duel(s), true))
            assertEquals(0, StakeRules.payout(duel(s), false))
        }
    }

    @Test fun forfeitGivesTheChallengerThePot() {
        assertEquals(950, StakeRules.payout(duel(DuelStatus.FORFEIT), true))
        assertEquals(0, StakeRules.payout(duel(DuelStatus.FORFEIT), false))
    }

    @Test fun undecidedWagerPaysNothingYet() {
        assertNull(StakeRules.payout(duel(DuelStatus.AWAITING_OPPONENT), true))
        assertNull(StakeRules.payout(duel(DuelStatus.ACCEPTED), false))
    }

    @Test fun dailyCeilings() {
        assertTrue(StakeUsage(0, 0, 0).canStake(1000))
        assertFalse(StakeUsage(9500, 3, 0).canStake(1000))
        assertFalse(StakeUsage(0, 10, 0).canStake(100))
        assertFalse(StakeUsage(0, 1, 9500).canStake(1000))
        assertTrue(StakeUsage(9000, 9, 9000).canStake(1000))
    }

    @Test fun roundLengthFitsTheTenMinutes() {
        assertTrue(StakeRules.roundFits(10))
        assertTrue(StakeRules.roundFits(20))
        assertFalse(StakeRules.roundFits(30))
    }
}
