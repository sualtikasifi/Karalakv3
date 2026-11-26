package com.sualtikasifi.cizimhafiza.domain.repository

import com.sualtikasifi.cizimhafiza.domain.model.Duel
import com.sualtikasifi.cizimhafiza.util.DuelHistoryEntry
import com.sualtikasifi.cizimhafiza.domain.model.ResultItem
import kotlinx.coroutines.flow.Flow

/** Asynchronous friend-vs-friend duels — see domain.model.Duel and firestore.rules' duels/{duelId}. */
interface DuelRepository {

    val currentUid: String?

    /** Turns a just-finished solo round (see GameViewModel's duel-challenge args) into a challenge for [opponentUid]. */
    suspend fun createDuel(
        opponentUid: String,
        opponentName: String,
        items: List<ResultItem>,
        challengerScore: Int,
        challengerCorrectCount: Int,
        /** The exact word ids this round drew, in order — see Duel.wordIds. */
        wordIds: List<Int>,
        /** Gold each side puts in; 0 for an ordinary duel. The stake leaves this player's wallet once the duel is written. */
        stake: Int = 0
    ): Result<Unit>

    /** One-shot fetch for the play screen — no live updates needed once the round is already loaded. */
    suspend fun getDuel(duelId: String): Result<Duel?>

    /** Duels waiting for this device to play, as the opponent. */
    fun observeIncomingDuels(): Flow<List<Duel>>

    /** Duels this device already played as the opponent — the "recent challenges" history for the person who was challenged. */
    fun observeCompletedReceivedDuels(): Flow<List<Duel>>

    /** Duels this device challenged someone else to, completed or not — for a "did they beat me" follow-up. */
    fun observeSentDuels(): Flow<List<Duel>>

    /** The opponent's guess-round outcome — completes the duel. Only ever called once per duel (see firestore.rules). */
    suspend fun submitDuelResult(duelId: String, opponentScore: Int, opponentCorrectCount: Int): Result<Unit>

    /** Clears the challenger's "new result" flag once they've opened a completed duel they sent. */
    suspend fun markSeenByChallenger(duelId: String): Result<Unit>

    /** Finished duels remembered on this phone, newest first — read without touching the server. */
    val history: Flow<List<DuelHistoryEntry>>

    /** Keeps the opponent's own copy of a duel they just finished. */
    fun recordReceivedResult(duel: Duel, myScore: Int, myCorrectCount: Int)

    /** Clears a result's "new" mark on this phone. */
    fun markHistorySeen(duelId: String)

    /** What this player has staked today (read from the server, so it fails offline) — for the picker and for [friendUid]. */
    suspend fun stakeUsage(friendUid: String): Result<com.sualtikasifi.cizimhafiza.domain.model.StakeUsage>

    /** Whether [friendUid] may take part in wagers (level, ban, Google, switch). Null when it could not be read. */
    suspend fun friendStakeBlock(friendUid: String): com.sualtikasifi.cizimhafiza.domain.model.StakeBlock?

    /** Takes a wager on: writes ACCEPTED with the day's counters and takes the stake from this wallet. */
    suspend fun acceptWager(duel: Duel): Result<Unit>

    /** Turns a wager down (the challenger's stake goes back to them). */
    suspend fun declineWager(duelId: String): Result<Unit>

    /**
     * Takes what the wagers owe this player — refunds, wins, ties — once each, ending expired offers and forfeited rounds on
     * the way. Returns what was paid ([StakeSettlement]); empty when nothing is due.
     */
    suspend fun settleWagers(): List<StakeSettlement>

    /** Either participant may remove a duel from their own view once it no longer needs to be there. */
    suspend fun deleteDuel(duelId: String): Result<Unit>
}

/** One wager settled on this device: who it was against, what it paid (0 = a loss) and how it ended. */
data class StakeSettlement(val otherName: String, val stake: Int, val paid: Int, val status: com.sualtikasifi.cizimhafiza.domain.model.DuelStatus)
