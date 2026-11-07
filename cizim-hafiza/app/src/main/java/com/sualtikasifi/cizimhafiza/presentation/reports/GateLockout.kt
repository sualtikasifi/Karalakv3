package com.sualtikasifi.cizimhafiza.presentation.reports

import android.content.SharedPreferences

/**
 * How long the developer panel's passcode screen refuses to listen after wrong codes. The code is digits only, so without a
 * limit it can simply be tried through; the real protection stays the Firestore rules (a wrong guess opens nothing that the
 * server would not refuse anyway), this only makes guessing slow and visible.
 */
object GatePolicy {
    /** Wrong codes in a row that are forgiven before the first wait. */
    const val FREE_TRIES = 5
    const val FIRST_WAIT_MS = 30_000L
    const val MAX_WAIT_MS = 3_600_000L

    /**
     * The wait that follows the [fails]th wrong code in a row: none for the first [FREE_TRIES], then 30 seconds, doubling with
     * every further wrong code (1, 2, 4 minutes…) up to an hour.
     */
    fun waitAfter(fails: Int): Long {
        if (fails < FREE_TRIES) return 0L
        val doublings = (fails - FREE_TRIES).coerceAtMost(12)
        return (FIRST_WAIT_MS shl doublings).coerceAtMost(MAX_WAIT_MS)
    }
}

/**
 * The gate's memory of wrong codes: how many in a row and until when it is shut. Kept in the preferences, not in the screen,
 * so leaving the screen and coming back (or closing the app) does not hand the tries back. A right code clears it.
 */
class GateLockout(private val prefs: SharedPreferences, private val clock: () -> Long = System::currentTimeMillis) {

    /** Milliseconds until the gate listens again; 0 when it is open. A clock set far back cannot stretch it past the longest wait. */
    fun remainingMillis(): Long {
        val until = prefs.getLong(UNTIL, 0L)
        return (until - clock()).coerceIn(0L, GatePolicy.MAX_WAIT_MS)
    }

    /** Wrong codes in a row so far. */
    fun fails(): Int = prefs.getInt(FAILS, 0)

    /** Counts a wrong code and shuts the gate for what that earns; returns that wait (0 when this one was still forgiven). */
    fun recordWrong(): Long {
        val fails = fails() + 1
        val wait = GatePolicy.waitAfter(fails)
        prefs.edit().putInt(FAILS, fails).putLong(UNTIL, if (wait > 0) clock() + wait else 0L).apply()
        return wait
    }

    /** A right code: the count starts again. */
    fun recordRight() {
        prefs.edit().remove(FAILS).remove(UNTIL).apply()
    }

    companion object {
        const val PREFS = "reports_gate_lockout"
        private const val FAILS = "fails"
        private const val UNTIL = "until"
    }
}
