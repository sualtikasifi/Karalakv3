package com.sualtikasifi.cizimhafiza.domain.repository

import com.sualtikasifi.cizimhafiza.domain.model.FriendStreak
import kotlinx.coroutines.flow.Flow

/** The streaks this player keeps with their friends — see domain.model.FriendStreak and firestore.rules' friendStreaks. */
interface FriendStreakRepository {

    val currentUid: String?

    /** Every streak this player is part of, live. */
    fun observeStreaks(): Flow<List<FriendStreak>>

    /** A duel with [friendUid] was just played to the end (ten words or more): counts today for the pair. Safe to repeat. */
    suspend fun recordDuel(friendUid: String): Result<Unit>

    /** Mends the pair's broken streak (the caller has just watched the ad that pays for it). */
    suspend fun repair(friendUid: String): Result<Unit>

    /** The friendship ended: the streak goes with it. */
    suspend fun remove(friendUid: String)
}
