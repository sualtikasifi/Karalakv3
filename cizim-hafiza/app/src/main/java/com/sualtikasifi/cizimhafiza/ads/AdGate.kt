package com.sualtikasifi.cizimhafiza.ads

/**
 * The one door every interstitial goes through, so the placements can be added to freely without the player ever meeting
 * two ads in a row. Pure (no Android, no clock of its own) so the rules can be tested.
 *
 * - A new player gets the first [Limits.newPlayerResults] finished games ad-free.
 * - Never two full-screen ads (an interstitial or a rewarded one) closer than [Limits.minGapMillis].
 * - After a rewarded ad (the player already sat through one) an interstitial waits [Limits.afterRewardedMillis].
 * - At most [Limits.perHour] interstitials in any hour.
 *
 * A rewarded ad is not gated: the player asked for it.
 */
object AdGate {
    data class Limits(
        val minGapMillis: Long = DEFAULT_MIN_GAP_MILLIS,
        val afterRewardedMillis: Long = DEFAULT_AFTER_REWARDED_MILLIS,
        val perHour: Int = DEFAULT_PER_HOUR,
        val newPlayerResults: Int = DEFAULT_NEW_PLAYER_RESULTS
    )

    data class State(
        /** Finished games seen on this install, this one included. */
        val resultsSeen: Int,
        val lastFullscreenAt: Long,
        val lastRewardedAt: Long,
        /** When each interstitial of the last hour or so was shown. */
        val recentInterstitials: List<Long>
    )

    fun allows(state: State, limits: Limits, now: Long): Boolean {
        if (state.resultsSeen <= limits.newPlayerResults) return false
        if (since(state.lastFullscreenAt, now) < limits.minGapMillis) return false
        if (since(state.lastRewardedAt, now) < limits.afterRewardedMillis) return false
        if (state.recentInterstitials.count { since(it, now) < HOUR_MILLIS } >= limits.perHour) return false
        return true
    }

    /** The times worth keeping: those within the last hour, newest last, at most [MAX_KEPT]. */
    fun keepRecent(times: List<Long>, now: Long): List<Long> =
        times.filter { since(it, now) < HOUR_MILLIS }.sorted().takeLast(MAX_KEPT)

    /** Milliseconds since [then]; a moment in the future (the phone's clock was set back) counts as long ago, so it cannot block ads for good. */
    private fun since(then: Long, now: Long): Long = if (then <= 0L || then > now) Long.MAX_VALUE else now - then

    const val DEFAULT_MIN_GAP_MILLIS = 90_000L
    const val DEFAULT_AFTER_REWARDED_MILLIS = 3 * 60_000L
    const val DEFAULT_PER_HOUR = 6
    const val DEFAULT_NEW_PLAYER_RESULTS = 5
    private const val HOUR_MILLIS = 60 * 60_000L
    private const val MAX_KEPT = 12
}
