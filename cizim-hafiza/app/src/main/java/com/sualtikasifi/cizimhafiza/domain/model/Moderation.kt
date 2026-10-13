package com.sualtikasifi.cizimhafiza.domain.model

/**
 * One finished round waiting to be let into the Hızlı Eşleş pool, as the
 * review screen reads it back.
 *
 * Every round goes through here. The automatic check (see
 * [WrittenWordDetector]) was measured against real handwriting and does not
 * work, so the only thing that reliably separates a drawn round from a
 * written one is somebody looking at it — and the pool is small enough that
 * looking is still cheap.
 */
data class PendingRun(
    val id: String,
    val uid: String,
    val nickname: String,
    val level: Int,
    /** All ten drawings, in the order they were played. */
    val items: List<ResultItem>,
    val totalScore: Int,
    val correctCount: Int,
    /** What the round paid its author — exactly what a rejection takes back. */
    val xpEarned: Int,
    val createdAtMillis: Long
)

/**
 * A penalty applied to one account for one rejected round.
 *
 * Written by the reviewer, applied by the offending device the next time it
 * opens the app — see PenaltyRepository. It is deliberately a record of a
 * decision rather than a command that fires once: a device that is offline
 * for a week still applies it when it comes back, and a device that already
 * applied it never applies it twice.
 */
data class Penalty(
    val id: String,
    val uid: String,
    /** XP taken back — what the rejected round paid out. */
    val xpRevoked: Int,
    /**
     * Which offence this was for the account, 1-based and lifetime — an
     * approved round does not clear it. Every third one carries a lockout.
     */
    val strike: Int,
    /**
     * When the account may play Hızlı Eşleş and online rooms again, or 0 for
     * no lockout. Set by the reviewer, not computed on the device: a phone
     * clock can be turned back.
     */
    val lockedUntilMillis: Long,
    val createdAtMillis: Long,
    /**
     * True only for a rejected Günlük Meydan Okuma attempt (see
     * ModerationRepository.rejectDailyChallenge) — on top of the XP revoke
     * every penalty already does, this one also resets the daily-challenge
     * streak to 0 (see PenaltyRepositoryImpl.applyOutstanding), since a
     * streak built partly on typed-not-drawn words was never really kept.
     */
    val breaksDailyStreak: Boolean = false,
    /** Gold taken back on top of [xpRevoked] — only a penalty decided from a player report carries any. */
    val goldRevoked: Int = 0
)

/**
 * One finished Günlük Meydan Okuma attempt waiting to be reviewed — see
 * ModerationRepository's daily-challenge methods. Unlike [PendingRun] the XP
 * has already been paid by the time this is looked at, so the only decisions
 * are "leave it" ([ModerationRepository.approveDailyChallenge]) or "take the
 * XP back and break the streak" ([ModerationRepository.rejectDailyChallenge]).
 */
data class PendingDailyChallenge(
    val id: String,
    val uid: String,
    val nickname: String,
    val items: List<ResultItem>,
    val score: Int,
    val correctCount: Int,
    val xpEarned: Int,
    val createdAtMillis: Long
)

/**
 * One page of runs, with what it takes to ask for the next one.
 *
 * The cursor is a `createdAt` rather than a document snapshot so it can live
 * in UI state and survive a rotation without carrying a Firestore type up
 * through the layers.
 */
data class RunPage(
    val runs: List<PendingRun>,
    /** Pass back as `after` to continue; null when nothing more was read. */
    val nextCursor: Long?,
    /** True once the collection has been read to the end. */
    val endReached: Boolean
)

/**
 * Who this device is as far as the moderation rules are concerned.
 *
 * Shown in the developer panel because the rules gate on a uid, and a uid is
 * invisible: when a write is refused there is otherwise no way to tell "the
 * rules are not published" from "this install is signed in as somebody else"
 * — and a reinstall can quietly produce the second.
 */
data class ReviewerIdentity(
    val uid: String?,
    val email: String?,
    val isReviewer: Boolean
)

object Moderation {

    /**
     * The one account the rules let moderate.
     *
     * MUST match the address in firestore.rules' reviewer() function. It is
     * duplicated here on purpose: the app cannot read the rules, so without a
     * copy it cannot tell the reviewer why a write was refused.
     */
    const val REVIEWER_EMAIL = "raunen3075@gmail.com"


    /**
     * The lockout for the [strike]-th offence (lifetime, never reset — an approved round does not clear it, or somebody
     * could alternate a cheated round with an honest one and never climb the ladder): 6 hours, 12 hours, a day,
     * 3 days, and 7 days for every later one.
     */
    fun lockoutMillisFor(strike: Int, @Suppress("UNUSED_PARAMETER") reason: DrawingReportReason? = null): Long {
        val hour = 60L * 60L * 1000L
        return when {
            strike <= 1 -> 6 * hour
            strike == 2 -> 12 * hour
            strike == 3 -> 24 * hour
            strike == 4 -> 72 * hour
            else -> 168 * hour
        }
    }

    /** How much heavier a report penalty gets with each offence: x1, x2 … capped at x5. */
    fun strikeMultiplier(strike: Int): Int = strike.coerceIn(1, 5)

    /**
     * Rounds fetched in one page of the review queue.
     *
     * Three, because a page is not free: every row costs a second read for a
     * document holding ten drawings — around eighty kilobytes each. Loading
     * twenty-five at once pulled two megabytes off Firestore every time the
     * screen opened, most of it for rows nobody had scrolled to yet. The list
     * asks for the next three when the reviewer reaches the bottom.
     */
    const val REVIEW_PAGE_SIZE = 3

    /**
     * What upholding a player's report costs the drawer at the [strike]-th offence: XP and gold taken back. The base
     * is sized against what play pays (10 gold a word, about 2.500 gold a day) so it is felt, then multiplied by
     * [strikeMultiplier]. Returns XP to gold.
     */
    fun reportPenalty(reason: DrawingReportReason, strike: Int = 1): Pair<Int, Int> {
        val (xp, gold) = when (reason) {
            DrawingReportReason.MEANINGLESS -> 150 to 750
            DrawingReportReason.WRITTEN -> 400 to 2000
            DrawingReportReason.OFFENSIVE -> 800 to 4000
        }
        val m = strikeMultiplier(strike)
        return xp * m to gold * m
    }
}
