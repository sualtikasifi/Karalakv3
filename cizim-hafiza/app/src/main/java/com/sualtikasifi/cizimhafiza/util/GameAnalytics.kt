package com.sualtikasifi.cizimhafiza.util

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The game's own Analytics events — how the rune map is used, so its pace can be judged from real play: how long a
 * ring takes to open, which runes are bought first. Best-effort: a failing Analytics never touches the game.
 */
@Singleton
class GameAnalytics @Inject constructor(@ApplicationContext private val context: Context) {

    private val analytics by lazy { runCatching { FirebaseAnalytics.getInstance(context) }.getOrNull() }

    private fun log(event: String, params: Bundle.() -> Unit) {
        runCatching { analytics?.logEvent(event, Bundle().apply(params)) }
    }

    /** A rune level was bought: which, to what level, in which ring, at what player level and with how much gold left. */
    fun runeBought(runeId: String, level: Int, ring: Int, playerLevel: Int, goldLeft: Int) = log("rune_bought") {
        putString("rune_id", runeId)
        putLong("rune_level", level.toLong())
        putLong("ring", ring.toLong())
        putLong("player_level", playerLevel.toLong())
        putLong("gold_left", goldLeft.toLong())
    }

    /** A ring of the rune map opened for this player (logged once per ring and account). */
    fun ringOpened(ring: Int, playerLevel: Int, runeLevels: Int) = log("rune_ring_opened") {
        putLong("ring", ring.toLong())
        putLong("player_level", playerLevel.toLong())
        putLong("rune_levels", runeLevels.toLong())
    }

    /** The rune status sheet was opened. */
    fun runeStatusOpened() = log("rune_status_opened") {}
}
