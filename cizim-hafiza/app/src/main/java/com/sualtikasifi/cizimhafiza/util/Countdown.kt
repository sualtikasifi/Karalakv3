package com.sualtikasifi.cizimhafiza.util

/**
 * The arithmetic of a deadline-based countdown.
 *
 * A round's clock is a DEADLINE, not a count of ticks: it keeps running while the app is in the background, while the
 * phone is locked, and — through the wall-clock deadline written into the recovery checkpoint — while the app is closed
 * altogether. When the player comes back, how much time is left is read off the clock, never remembered.
 */
object Countdown {
    /** Anything later than this past a deadline means the player was away (the clock kept running), not timer jitter. */
    const val AWAY_THRESHOLD_MS = 2_000L

    /** The second shown for [msLeft] milliseconds to go: 20.0 s -> 20, 19.001 s -> 20, 19.0 s -> 19. 0 or less -> 0. */
    fun secondsLeft(msLeft: Long): Int = if (msLeft <= 0) 0 else ((msLeft + 999) / 1000).toInt()

    /** How long to sleep before the shown second next changes, kept short enough to catch up quickly after a freeze. */
    fun nextCheckMs(msLeft: Long): Long = (((msLeft - 1) % 1000) + 1).coerceIn(20L, 1000L)

    /**
     * What a countdown that ended [lateMs] after its deadline hands on to the next one: the time the player spent away,
     * which the next phase has already been running through. Ordinary jitter (under [AWAY_THRESHOLD_MS]) is not handed on.
     */
    fun carryFor(lateMs: Long): Long = if (lateMs >= AWAY_THRESHOLD_MS) lateMs else 0L

    /** The milliseconds a countdown of [startSeconds] still has once [carryMs] of away time has been taken off. */
    fun startMs(startSeconds: Int, carryMs: Long): Long = startSeconds * 1000L - carryMs

    /**
     * What is left of a countdown whose wall-clock deadline was written into a checkpoint, when the app comes back at
     * [nowWallMs]: positive while there is time, zero or negative (how late it is) when the deadline has passed. Null for
     * a checkpoint written before deadlines were stored.
     */
    fun remainingFromCheckpoint(deadlineWallMs: Long, nowWallMs: Long): Long? =
        if (deadlineWallMs <= 0L) null else deadlineWallMs - nowWallMs
}
