package com.sualtikasifi.cizimhafiza.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What this phone remembers about friend streaks: which milestone prizes it already took (one per pair, run and
 * milestone), when it last reminded about each streak, and how many mends it has already told about. Plain
 * preferences in their own file; none of it needs to leave the phone.
 */
@Singleton
class FriendStreakStore @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("friend_streaks", Context.MODE_PRIVATE)

    private fun claimKey(pairId: String, run: Int, milestone: Int) = "$pairId:$run:$milestone"

    fun isClaimed(pairId: String, run: Int, milestone: Int): Boolean =
        claimKey(pairId, run, milestone) in (prefs.getStringSet(KEY_CLAIMED, emptySet()) ?: emptySet())

    fun markClaimed(pairId: String, run: Int, milestone: Int) {
        val now = HashSet(prefs.getStringSet(KEY_CLAIMED, emptySet()) ?: emptySet())
        now += claimKey(pairId, run, milestone)
        prefs.edit().putStringSet(KEY_CLAIMED, now).apply()
    }

    /** True once a reminder for this streak has been shown on [day] (a Türkiye epoch day). */
    fun remindedOn(pairId: String, day: Int): Boolean = prefs.getInt("remind_$pairId", Int.MIN_VALUE) == day

    fun markReminded(pairId: String, day: Int) = prefs.edit().putInt("remind_$pairId", day).apply()

    /** How many mends of this streak the player has already been told about. */
    fun seenRepairs(pairId: String): Int = prefs.getInt("repairs_$pairId", 0)

    fun markRepairsSeen(pairId: String, repairs: Int) = prefs.edit().putInt("repairs_$pairId", repairs).apply()

    private companion object {
        const val KEY_CLAIMED = "claimed"
    }
}
