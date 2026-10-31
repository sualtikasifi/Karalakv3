package com.sualtikasifi.cizimhafiza.util

import javax.inject.Inject
import javax.inject.Singleton

/**
 * How players find their way to the rune map and to their first rune, counted twice: as Firebase Analytics events (for
 * digging in later) and as the developer panel's own daily counters (AdminStats), so Pano can show the funnel without
 * leaving the app. Counter names: `rune__open__<source>`, `rune__prompt__<action>`, `rune__bought__count`,
 * `rune__first__guided` / `rune__first__self`. Best-effort, like both of them: nothing here may touch the game.
 */
@Singleton
class RuneFunnel @Inject constructor(
    private val analytics: GameAnalytics,
    private val adminStats: AdminStats
) {
    /** In-memory: the offer is counted once per app run, not every time the home screen is drawn again. */
    @Volatile private var promptCountedThisRun = false

    /** The map was opened from [source] (see GameAnalytics.runeMapOpened). */
    fun mapOpened(source: String, boughtLevels: Int) {
        analytics.runeMapOpened(source, boughtLevels)
        runCatching { adminStats.record("rune__open__$source") }
    }

    /** The mascot's first-rune offer was put on screen. */
    fun promptShown() {
        if (promptCountedThisRun) return
        promptCountedThisRun = true
        analytics.runePrompt("shown")
        runCatching { adminStats.record("rune__prompt__shown") }
    }

    fun promptTapped() {
        analytics.runePrompt("tapped")
        runCatching { adminStats.record("rune__prompt__tapped") }
    }

    fun promptDismissed() {
        analytics.runePrompt("dismissed")
        runCatching { adminStats.record("rune__prompt__dismissed") }
    }

    /** A rune level was bought; [first] when it is the player's first one ever, [guided] when the guide led there. */
    fun bought(runeId: String, first: Boolean, guided: Boolean, playerLevel: Int) {
        runCatching { adminStats.record("rune__bought__count") }
        if (first) {
            analytics.firstRuneBought(runeId, guided, playerLevel)
            runCatching { adminStats.record(if (guided) "rune__first__guided" else "rune__first__self") }
        }
    }
}
