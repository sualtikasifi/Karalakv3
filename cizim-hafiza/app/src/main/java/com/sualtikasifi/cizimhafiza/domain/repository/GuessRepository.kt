package com.sualtikasifi.cizimhafiza.domain.repository

import com.sualtikasifi.cizimhafiza.domain.model.DrawingStroke
import com.sualtikasifi.cizimhafiza.domain.model.GuessInboxEntry
import com.sualtikasifi.cizimhafiza.domain.model.GuessLink

/** "Bil Bakalım" links: a drawing sent to a friend who watches it drawn and guesses (guessLinks, users/{uid}/guessInbox). */
interface GuessRepository {

    val currentUid: String?

    /** Why [createLink] gave up, so the screen can say something better than "failed". */
    class LimitReachedException : Exception("too many links today")

    /** Stores [word]'s drawing as a link and returns its id. The word is saved only as a hash. */
    suspend fun createLink(word: String, strokes: List<DrawingStroke>, seconds: Int, lang: String, ownerName: String): Result<String>

    /** The link, or null when it does not exist or has expired. */
    suspend fun getLink(id: String): Result<GuessLink?>

    /** Leaves the outcome in the link owner's inbox. One result per person per link; a second attempt is ignored. */
    suspend fun sendResult(link: GuessLink, solved: Boolean, attempts: Int, myName: String): Result<Unit>

    /** What came of this player's links since they last looked. */
    suspend fun fetchInbox(): Result<List<GuessInboxEntry>>

    /** Removes entries that were shown. */
    suspend fun clearInbox(ids: List<String>)

    /** Removes this player's links that have run out, best effort. */
    suspend fun deleteExpiredLinks()
}
