package com.sualtikasifi.cizimhafiza.domain.model

/** What one day of the login streak gives. */
data class LoginDayReward(
    val gold: Int = 0,
    val xp: Int = 0,
    /** One of each is given. */
    val jokers: List<JokerType> = emptyList(),
    val chest: ChestTier? = null
)

/**
 * The 7-day "Giriş Serisi" calendar. A day is counted when the player opens the app and takes that day's reward; the
 * seventh day is the big one and the calendar then starts over, while the streak count itself keeps climbing.
 * Every [MILESTONE_EVERY]th day of the streak adds a Sürpriz chest on top of that day's reward.
 */
object LoginStreakRewards {
    const val CYCLE_DAYS = 7
    const val MILESTONE_EVERY = 30
    val MILESTONE_CHEST = ChestTier.RARE

    /** [day] is the position in the cycle, 1..[CYCLE_DAYS]. */
    fun forDay(day: Int): LoginDayReward = when (day) {
        1 -> LoginDayReward(gold = 300)
        2 -> LoginDayReward(gold = 400)
        3 -> LoginDayReward(jokers = JokerType.entries)
        4 -> LoginDayReward(gold = 500)
        5 -> LoginDayReward(chest = ChestTier.GOLD)
        6 -> LoginDayReward(gold = 800)
        else -> LoginDayReward(chest = ChestTier.RARE)
    }

    /** The calendar position (1..7) of the reward the player gets after [streakBefore] claimed days in a row. */
    fun cycleDayFor(streakBefore: Int): Int = streakBefore % CYCLE_DAYS + 1

    /** The milestone chest earned by reaching [newStreak] days, or null. */
    fun milestoneFor(newStreak: Int): ChestTier? =
        if (newStreak > 0 && newStreak % MILESTONE_EVERY == 0) MILESTONE_CHEST else null
}

/** Where the login streak stands for a given (server) day. */
sealed interface LoginStreakState {
    /** Today's reward is already taken. */
    data object ClaimedToday : LoginStreakState

    /** Today's reward can be taken; [streakBefore] days in a row were claimed before it. */
    data class Claimable(val streakBefore: Int) : LoginStreakState

    /** At least one day was skipped, so the [lostStreak]-day streak is broken — it can be saved by watching an ad. */
    data class Missed(val lostStreak: Int) : LoginStreakState
}

object LoginStreaks {
    /** [lastClaimDay] is the epoch day of the last claim, -1 if there never was one. */
    fun evaluate(streak: Int, lastClaimDay: Long, today: Long): LoginStreakState = when {
        lastClaimDay == today -> LoginStreakState.ClaimedToday
        lastClaimDay < 0 || streak <= 0 -> LoginStreakState.Claimable(0)
        lastClaimDay == today - 1 -> LoginStreakState.Claimable(streak)
        lastClaimDay > today -> LoginStreakState.ClaimedToday // a clock that went backwards: nothing to take
        else -> LoginStreakState.Missed(streak)
    }
}
