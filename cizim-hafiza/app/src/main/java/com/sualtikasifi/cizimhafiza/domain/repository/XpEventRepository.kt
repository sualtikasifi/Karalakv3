package com.sualtikasifi.cizimhafiza.domain.repository

/**
 * The admin-controlled, app-wide "2x XP etkinliği" (see GeliştiriciPaneli /
 * DrawingReportsViewModel) — a single Firestore doc every device reads, so
 * turning it on reaches every signed-in player live, not just the admin's
 * own install. See [XpEvent].
 */
interface XpEventRepository {

    /**
     * The event document as it stands, kept live by ONE document listener
     * (see [startListening]) — null until the first snapshot or when no event
     * has ever been set. A change reaches every open app within a moment, and
     * costs one read per device per change.
     */
    val live: kotlinx.coroutines.flow.StateFlow<XpEvent?>

    /** Starts the listener behind [live]. Safe to call more than once. */
    fun startListening()

    /**
     * The multiplier in effect right now (1 when no event is running or the
     * stored one has already expired). Cached briefly client-side (see
     * REFRESH_WINDOW_MILLIS) — this is read once per match, not once per
     * word, so a stale-by-a-few-minutes value costs nothing.
     */
    suspend fun currentMultiplier(): Int

    /** The raw event doc, for the Developer Panel's own status display. */
    suspend fun current(): Result<XpEvent?>

    /** Reviewer-only — Firestore rules on config/xpEvent enforce it. */
    suspend fun startEvent(multiplier: Int, durationMillis: Long, label: String?): Result<Unit>

    /** Reviewer-only — ends the event immediately, before its own expiry. */
    suspend fun stopEvent(): Result<Unit>

    companion object {
        const val REFRESH_WINDOW_MILLIS = 5 * 60 * 1000L
    }
}

/** @param endsAtMillis epoch millis; the event is treated as inactive once `now >= endsAtMillis`. */
data class XpEvent(
    val active: Boolean,
    val multiplier: Int,
    val endsAtMillis: Long,
    val label: String?
)
