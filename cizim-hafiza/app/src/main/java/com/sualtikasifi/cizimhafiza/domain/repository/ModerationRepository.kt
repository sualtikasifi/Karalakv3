package com.sualtikasifi.cizimhafiza.domain.repository

import com.sualtikasifi.cizimhafiza.domain.model.PendingDailyChallenge
import com.sualtikasifi.cizimhafiza.domain.model.PendingRun
import com.sualtikasifi.cizimhafiza.domain.model.ResultItem
import com.sualtikasifi.cizimhafiza.domain.model.ReviewerIdentity
import com.sualtikasifi.cizimhafiza.domain.model.RunPage

/**
 * The reviewer's side of the Hızlı Eşleş pool: what is waiting, and the two
 * decisions that can be taken about it.
 *
 * Only ever called from the passcode-gated review screen. Nothing here is
 * reachable by a player.
 */
interface ModerationRepository {

    /** Who this device is to the rules — see [ReviewerIdentity]. */
    fun identity(): ReviewerIdentity

    /**
     * Oldest first — the queue is worked through in the order it arrived.
     *
     * Paged, because a row is expensive: pass the previous page's
     * [RunPage.nextCursor] as [after] to continue, or null to start.
     */
    suspend fun pendingRuns(limit: Int, after: Long? = null): Result<RunPage>

    /**
     * What is already in the live pool, newest first.
     *
     * Read back in the same shape as the queue so the same row can show it:
     * a run in the pool is a run that was approved, and the only thing worth
     * doing with one is looking at it again. Paged for the same reason.
     */
    suspend fun poolRuns(limit: Int, after: Long? = null): Result<RunPage>

    /** Lets a round into the live pool. */
    suspend fun approve(runId: String): Result<Unit>

    /**
     * Changes the name a run is presented under.
     *
     * The pool is seeded largely by one person's own play while it fills, so
     * without this every opponent a player meets carries the same two or
     * three names. The drawings are real and worth keeping; only the label
     * on them is wrong.
     *
     * [inPool] says which collection the run is in — the two are separate
     * documents and a run is only ever in one of them.
     */
    suspend fun rename(runId: String, nickname: String, inPool: Boolean): Result<Unit>

    /**
     * Pulls a round back out of the pool and into the queue.
     *
     * The undo for [approve], and the way rounds that reached the pool before
     * review existed get looked at. No penalty: this only says the round is
     * not playable until somebody has judged it.
     */
    suspend fun sendBackToQueue(runId: String): Result<Unit>

    /**
     * Rejects a round and penalises its author.
     *
     * Deletes the round, writes a [com.sualtikasifi.cizimhafiza.domain.model.Penalty]
     * for the XP it paid out, and — on the third consecutive rejection —
     * locks the account out of the online modes for a day.
     *
     * The lockout deadline is computed here, on the reviewer's clock, and
     * stored as an absolute time. A device could otherwise sit out its
     * lockout by moving its own clock forward.
     */
    suspend fun reject(runId: String, xpToRevoke: Int): Result<Unit>

    /**
     * Turns a round down without judging its author: the round is deleted from
     * the queue, and that is all. No penalty, no strike, no XP taken back —
     * and it never reaches the pool either. For a round that is simply not
     * wanted (a poor drawing, a duplicate) as opposed to one that broke the
     * rules, which is what [reject] is for.
     */
    suspend fun dismiss(runId: String): Result<Unit>

    // --- Günlük Meydan Okuma review — the one exception to this whole
    // interface's "reviewer only" rule: submitDailyChallengeForReview is
    // called by every PLAYER's own device, right after finishing the day's
    // challenge. The XP was already paid at that point (typing instead of
    // drawing costs the player nothing to try, so there is no reason to
    // hold the reward hostage to a review that might take a day) — this
    // queue exists only to catch the cases where they typed the word into
    // the canvas instead of drawing it, same as Hızlı Eşleş's WrittenWordDetector
    // problem, and claw the XP back after the fact. firestore.rules enforces
    // the actual split: create is any signed-in uid writing their own
    // document, read/delete stay reviewer-only exactly like the rest of
    // this interface. ---

    /** Queues this attempt for review. Best-effort — a failed upload never blocks the player's own result screen. */
    suspend fun submitDailyChallengeForReview(
        items: List<ResultItem>,
        score: Int,
        correctCount: Int,
        xpEarned: Int
    ): Result<Unit>

    /** Oldest first, reviewer only. */
    suspend fun pendingDailyChallenges(limit: Int): Result<List<PendingDailyChallenge>>

    /**
     * The attempt was drawn honestly — deletes it from the queue. No
     * notification: an approval is not something the player needs to hear
     * about, it just means the XP they already have stays theirs.
     */
    suspend fun approveDailyChallenge(id: String): Result<Unit>

    /**
     * The word was typed rather than drawn — deletes the attempt, revokes
     * [xpToRevoke] via the same [com.sualtikasifi.cizimhafiza.domain.model.Penalty]
     * pipeline Hızlı Eşleş rejections use (so the player is told, same as
     * any other penalty), and additionally breaks today's daily-challenge
     * streak — see PenaltyRepositoryImpl, which is what actually resets it
     * once this device's own penalty write is applied.
     */
    suspend fun rejectDailyChallenge(id: String, xpToRevoke: Int): Result<Unit>
}
