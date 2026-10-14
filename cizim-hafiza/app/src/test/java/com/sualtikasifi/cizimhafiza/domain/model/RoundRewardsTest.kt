package com.sualtikasifi.cizimhafiza.domain.model

import com.sualtikasifi.cizimhafiza.util.GameConstants
import org.junit.Assert.assertEquals
import org.junit.Test

class RoundRewardsTest {

    private val none = emptyMap<String, Int>()
    private val everything = RuneTree.all.associate { it.id to it.maxLevel }

    @Test
    fun aPlainSoloRoundPaysTenGoldPerWordAndNothingFromRunes() {
        val gold = RoundRewards.solo(none, listOf(true, false, true, true), isDaily = false, newAchievements = 0, newStars = 0, dailyStreak = null, quickMatchWon = false)
        assertEquals(3 * GameConstants.GOLD_PER_CORRECT_WORD, gold.play)
        assertEquals(gold.play, gold.total)
        assertEquals(0, gold.runeBonus)
    }

    @Test
    fun theWordRuneGrowsTheWholeRoundSoNothingIsRoundedAway() {
        val levels = mapOf(RuneTree.WORD_GOLD to 1)
        // Six words of 10 at +8%: 64, where 10 per word rounded down would have paid 60.
        val gold = RoundRewards.solo(levels, List(6) { true }, isDaily = false, newAchievements = 0, newStars = 0, dailyStreak = null, quickMatchWon = false)
        assertEquals(64, gold.play)
        assertEquals(4, gold.runeBonus)
    }

    @Test
    fun theChainRunePaysPerFullThreeInARow() {
        val levels = mapOf("streak_key" to 1)
        assertEquals(0, RoundRewards.chainBonus(none, List(6) { true }))
        assertEquals(20, RoundRewards.chainBonus(levels, List(6) { true }))
        assertEquals(0, RoundRewards.chainBonus(levels, listOf(true, true, false, true, true, false)))
        assertEquals(10, RoundRewards.chainBonus(levels, listOf(true, true, true, false, true, true)))
    }

    @Test
    fun theDailyChallengePaysItsOwnGoldInsteadOfWordGold() {
        val gold = RoundRewards.solo(none, List(5) { true }, isDaily = true, newAchievements = 0, newStars = 0, dailyStreak = 3, quickMatchWon = false)
        assertEquals(0, gold.play)
        assertEquals(GameConstants.dailyChallengeGold(3), gold.daily)
        assertEquals(gold.daily, gold.total)
    }

    @Test
    fun starsAchievementsAndAQuickMatchWinAddUp() {
        val gold = RoundRewards.solo(none, listOf(true), isDaily = false, newAchievements = 2, newStars = 3, dailyStreak = null, quickMatchWon = true)
        assertEquals(2 * GameConstants.ACHIEVEMENT_GOLD, gold.achievements)
        assertEquals(3 * GameConstants.LEVEL_GOLD_PER_NEW_STAR, gold.level)
        assertEquals(GameConstants.ONLINE_WIN_GOLD, gold.win)
        assertEquals(gold.play + gold.achievements + gold.level + gold.win, gold.total)
    }

    @Test
    fun negativeStarsNeverTakeGoldAway() {
        val gold = RoundRewards.solo(none, emptyList(), isDaily = false, newAchievements = 0, newStars = -2, dailyStreak = null, quickMatchWon = false)
        assertEquals(0, gold.total)
    }

    @Test
    fun theRuneBonusIsExactlyWhatTheRunesAdded() {
        val correct = List(6) { true }
        val plain = RoundRewards.solo(none, correct, isDaily = false, newAchievements = 1, newStars = 2, dailyStreak = null, quickMatchWon = true)
        val boosted = RoundRewards.solo(everything, correct, isDaily = false, newAchievements = 1, newStars = 2, dailyStreak = null, quickMatchWon = true)
        assertEquals(boosted.total - plain.total, boosted.runeBonus)
    }

    @Test
    fun roomPercentagesAreTakenOfThePlainAmountOnly() {
        val raw = RoundRewards.roomRaw(correctCount = 4, won = true, newAchievements = 0)
        assertEquals(4 * GameConstants.GOLD_PER_CORRECT_WORD + GameConstants.ONLINE_WIN_GOLD, raw)
        val plain = RoundRewards.room(none, 4, won = true, newAchievements = 0, asHost = false, bestFriendBonus = false)
        assertEquals(raw, plain.total)
        assertEquals(0, plain.runeBonus)

        val levels = mapOf("fr_1" to 1)
        val withRoom = RoundRewards.room(levels, 4, won = true, newAchievements = 0, asHost = false, bestFriendBonus = false)
        assertEquals(raw * 10 / 100, withRoom.room)
        assertEquals(withRoom.total - raw, withRoom.runeBonus)
    }

    @Test
    fun theFounderRuneCountsOnlyForTheHostAndTheFriendBonusAddsHalf() {
        val levels = mapOf("fr_2" to 1)
        val guest = RoundRewards.room(levels, 5, won = false, newAchievements = 0, asHost = false, bestFriendBonus = false)
        val host = RoundRewards.room(levels, 5, won = false, newAchievements = 0, asHost = true, bestFriendBonus = false)
        assertEquals(0, guest.room)
        assertEquals(50 * 10 / 100, host.room)
        val best = RoundRewards.room(none, 5, won = false, newAchievements = 0, asHost = false, bestFriendBonus = true)
        assertEquals(50 * RoundRewards.BEST_FRIEND_PERCENT / 100, best.room)
    }

    @Test
    fun anEmptyRoomRoundPaysNothing() {
        val gold = RoundRewards.room(everything, 0, won = false, newAchievements = 0, asHost = true, bestFriendBonus = true)
        assertEquals(0, gold.total)
        assertEquals(0, gold.runeBonus)
    }
}
