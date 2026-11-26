package com.sualtikasifi.cizimhafiza.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EarnSourceTest {
    @Test
    fun playCountsTowardsTheMonthlyLeague() {
        listOf(EarnSource.GAME, EarnSource.DAILY, EarnSource.ONLINE, EarnSource.AD, EarnSource.GUESS, EarnSource.OTHER)
            .forEach { assertTrue("${it.name} is play", it.countsForLeague) }
    }

    @Test
    fun windfallsDoNotCountTowardsTheMonthlyLeague() {
        listOf(EarnSource.ACHIEVEMENT, EarnSource.COUPON, EarnSource.LOGIN, EarnSource.REFERRAL, EarnSource.FRIEND_STREAK, EarnSource.CHEST, EarnSource.LEAGUE)
            .forEach { assertFalse("${it.name} is a windfall", it.countsForLeague) }
    }
}
