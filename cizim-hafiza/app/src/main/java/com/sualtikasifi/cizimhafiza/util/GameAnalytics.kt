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

    /**
     * The rune map was opened, and from where: "chip" (the home screen's Rünler sign), "mascot" (the dog above it),
     * "prompt" (the mascot's first-rune offer) or "tour" (the Yenilikler tour's rune page).
     */
    fun runeMapOpened(source: String, boughtLevels: Int) = log("rune_map_opened") {
        putString("source", source)
        putLong("rune_levels", boughtLevels.toLong())
    }

    /** The mascot's first-rune offer: "shown", "tapped" or "dismissed". */
    fun runePrompt(action: String) = log("rune_prompt") { putString("action", action) }

    /** The player's very first rune, and whether the first-rune guide led them to it. */
    fun firstRuneBought(runeId: String, guided: Boolean, playerLevel: Int) = log("rune_first_bought") {
        putString("rune_id", runeId)
        putLong("guided", if (guided) 1L else 0L)
        putLong("player_level", playerLevel.toLong())
    }

    // ---- "Bil Bakalım" links and friend streaks: the funnel from a shared drawing to a new player who keeps coming back

    /** A drawing was turned into a link: from the result screen ("result") or as the answer to a friend's link ("turn"). */
    fun guessLinkCreated(source: String) = log("guess_link_created") { putString("source", source) }

    /** A friend's link was opened in the app. */
    fun guessLinkOpened() = log("guess_link_opened") {}

    /** The round on a friend's link ended: guessed or not, after how many tries. */
    fun guessFinished(solved: Boolean, attempts: Int) = log("guess_finished") {
        putString("solved", solved.toString())
        putInt("attempts", attempts)
    }

    /** "Sıra sende" was taken up: the same word is being drawn to send on. */
    fun guessTurnStarted() = log("guess_turn_started") {}

    /** A finished duel was counted for the pair's friend streak. */
    fun friendStreakDuel() = log("fstreak_duel") {}

    /** A broken friend streak was mended with an ad. */
    fun friendStreakRescued() = log("fstreak_rescued") {}

    /** A friend-streak milestone prize was taken. */
    fun friendStreakReward(milestone: Int) = log("fstreak_reward") { putInt("milestone", milestone) }
}
