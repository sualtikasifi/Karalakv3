package com.sualtikasifi.cizimhafiza.domain.model

import com.sualtikasifi.cizimhafiza.util.GameConstants

/**
 * The gold one round pays, split by where it came from. [runeBonus] is the part the runes added on top of the plain
 * amounts — it is already inside the other fields, the result screens only show it so the runes can be felt.
 */
data class GoldBreakdown(
    val play: Int = 0,
    val achievements: Int = 0,
    val level: Int = 0,
    val daily: Int = 0,
    val win: Int = 0,
    /** Online rooms only: what the room, founder and Dostluk Bağı runes added. */
    val room: Int = 0,
    val runeBonus: Int = 0
) {
    val total: Int get() = play + achievements + level + daily + win + room
}

/**
 * How much gold a finished round pays. Pure — no storage, no side effects — so every rule of the economy that a round
 * touches can be tested on its own; the view models only gather the inputs and pay [GoldBreakdown.total].
 */
object RoundRewards {

    /** Extra gold for runs of three right words in a row (the Zincir Ustası rune): EconomyConfig.chainBonusGold per full three. */
    fun chainBonus(levels: Map<String, Int>, correctInOrder: List<Boolean>): Int {
        if (!RuneEffects.has(levels, RuneEffect.STREAK)) return 0
        var run = 0
        var bonus = 0
        for (ok in correctInOrder) {
            if (ok) {
                run++
                if (run % 3 == 0) bonus += com.sualtikasifi.cizimhafiza.util.EconomyConfig.chainBonusGold
            } else run = 0
        }
        return bonus
    }

    /**
     * A solo round: a level, an offline game, a Quick Match or the daily challenge. [newStars] is how many stars beyond
     * the level's best were earned; [dailyStreak] is set only when this round completed today's challenge for the first
     * time; [quickMatchWon] when a Quick Match was won outright.
     */
    fun solo(
        levels: Map<String, Int>,
        correctInOrder: List<Boolean>,
        isDaily: Boolean,
        newAchievements: Int,
        newStars: Int,
        dailyStreak: Int?,
        quickMatchWon: Boolean
    ): GoldBreakdown {
        val rawWords = correctInOrder.count { it } * GameConstants.GOLD_PER_CORRECT_WORD
        val words = RuneEffects.wordGold(levels, rawWords)
        val chain = chainBonus(levels, correctInOrder)
        // The daily challenge pays its own gold instead of the per-word gold.
        val play = if (isDaily) 0 else words + chain
        val rawDaily = dailyStreak?.let { GameConstants.dailyChallengeGold(it) } ?: 0
        val daily = if (dailyStreak == null) 0 else RuneEffects.dailyGold(levels, rawDaily)
        val rawWin = if (quickMatchWon) GameConstants.ONLINE_WIN_GOLD else 0
        val win = if (quickMatchWon) RuneEffects.winGold(levels, rawWin) else 0
        return GoldBreakdown(
            play = play,
            achievements = newAchievements * GameConstants.ACHIEVEMENT_GOLD,
            level = newStars.coerceAtLeast(0) * GameConstants.LEVEL_GOLD_PER_NEW_STAR,
            daily = daily,
            win = win,
            runeBonus = (if (isDaily) 0 else words - rawWords + chain) + (daily - rawDaily) + (win - rawWin)
        )
    }

    /** The plain gold of an online room round, before any rune: what the room percentages are taken of. */
    fun roomRaw(correctCount: Int, won: Boolean, newAchievements: Int): Int =
        correctCount * GameConstants.GOLD_PER_CORRECT_WORD +
            (if (won) GameConstants.ONLINE_WIN_GOLD else 0) +
            newAchievements * GameConstants.ACHIEVEMENT_GOLD

    /**
     * An online room round. The word and win runes grow their own parts; the room runes (friends always, founder when
     * [asHost]) and the once-a-day Dostluk Bağı +50% ([bestFriendBonus]) are taken of the plain amount, so they do not
     * multiply what the other runes already added.
     */
    fun room(
        levels: Map<String, Int>,
        correctCount: Int,
        won: Boolean,
        newAchievements: Int,
        asHost: Boolean,
        bestFriendBonus: Boolean
    ): GoldBreakdown {
        val raw = roomRaw(correctCount, won, newAchievements)
        val words = RuneEffects.wordGold(levels, correctCount * GameConstants.GOLD_PER_CORRECT_WORD)
        val win = if (won) RuneEffects.winGold(levels, GameConstants.ONLINE_WIN_GOLD) else 0
        val achievements = newAchievements * GameConstants.ACHIEVEMENT_GOLD
        val percent = RuneEffects.roomGoldPercent(levels, asHost) + if (bestFriendBonus) BEST_FRIEND_PERCENT else 0
        val room = if (raw > 0) raw * percent / 100 else 0
        val breakdown = GoldBreakdown(play = words, achievements = achievements, win = win, room = room)
        return breakdown.copy(runeBonus = breakdown.total - raw)
    }

    const val BEST_FRIEND_PERCENT = 50
}
