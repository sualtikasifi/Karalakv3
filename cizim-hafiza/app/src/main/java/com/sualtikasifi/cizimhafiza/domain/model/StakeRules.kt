package com.sualtikasifi.cizimhafiza.domain.model

/**
 * Duels played for gold. Every number here is also written into firestore.rules (duels/{duelId}, stakeStats) —
 * change them together. The rules decide what a document may say; this object decides what each device does with it.
 *
 * The pot is both stakes; [BURN_PERCENT] of it is burned, the rest goes to the winner. A tie returns both stakes.
 */
object StakeRules {
    val STAKES = listOf(100, 250, 500, 1000)
    const val MAX_WORDS = 20
    const val MAX_POINTS_PER_WORD = 7
    const val MAX_STAKED_PER_DAY = 10_000
    const val MAX_DUELS_PER_DAY = 10
    const val MAX_PER_FRIEND_PER_DAY = 10_000
    const val MIN_LEVEL = 10
    const val MIN_CHALLENGER_ACCOUNT_DAYS = 7
    const val MIN_FRIENDSHIP_DAYS = 2
    const val BURN_PERCENT = 5
    const val ACCEPT_MILLIS = 24L * 3600_000
    /** What the player is told; the rules allow a minute more for the network. */
    const val FINISH_MILLIS = 10L * 60_000
    const val FINISH_GRACE_MILLIS = 60_000L

    /** What the winner is paid out of a pot of two [stake]s. */
    fun winnerPayout(stake: Int): Int = stake * 2 - stake * 2 * BURN_PERCENT / 100

    /** Gold gained or lost by playing a wager of [stake] to a win (+) — for the words on the buttons. */
    fun netWin(stake: Int): Int = winnerPayout(stake) - stake

    fun roundFits(wordCount: Int): Boolean = wordCount in 1..MAX_WORDS

    /** What a wager that ended as [duel] pays THIS player, in gold (0 = nothing). Null while it is still undecided for them. */
    fun payout(duel: Duel, iAmChallenger: Boolean): Int? {
        if (!duel.isWager) return null
        val stake = duel.stake
        return when (duel.status) {
            DuelStatus.DECLINED, DuelStatus.EXPIRED -> if (iAmChallenger) stake else 0
            DuelStatus.FORFEIT -> if (iAmChallenger) winnerPayout(stake) else 0
            DuelStatus.COMPLETE -> {
                val theirs = duel.opponentScore ?: return null
                when {
                    duel.challengerScore == theirs -> stake
                    (duel.challengerScore > theirs) == iAmChallenger -> winnerPayout(stake)
                    else -> 0
                }
            }
            else -> null
        }
    }
}

/** What this player has staked today, as the server counted it. */
data class StakeUsage(val gold: Int, val count: Int, val withFriend: Int) {
    fun canStake(stake: Int): Boolean =
        count < StakeRules.MAX_DUELS_PER_DAY &&
            gold + stake <= StakeRules.MAX_STAKED_PER_DAY &&
            withFriend + stake <= StakeRules.MAX_PER_FRIEND_PER_DAY
}

/** Why a wager could not start. */
enum class StakeBlock { CHILD_MODE, NOT_LINKED, LEVEL, ACCOUNT_AGE, FRIENDSHIP_AGE, FRIEND_NOT_READY, FRIEND_LEVEL, BANNED, DISABLED, OFFLINE, NO_GOLD, LIMIT, TOO_LATE, REFUSED }

class StakeException(val block: StakeBlock) : Exception(block.name)
