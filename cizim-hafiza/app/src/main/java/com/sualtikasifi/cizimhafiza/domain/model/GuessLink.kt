package com.sualtikasifi.cizimhafiza.domain.model

/**
 * A drawing turned into a "Bil Bakalım" link (Firestore `guessLinks/{id}`, see firestore.rules). The word itself is not
 * in it: only [hash] (a salted SHA-256 of its folded spelling, see util.GuessCodec), the word's shape and first letter
 * for the hints.
 */
data class GuessLink(
    val id: String,
    val ownerUid: String,
    /** Empty when the owner stayed anonymous. */
    val ownerName: String,
    /** The language of the word: which word list the guess is checked against. */
    val lang: String,
    val salt: String,
    val hash: String,
    /** "4 3" for a two-word answer of four and three letters. */
    val pattern: String,
    val first: String,
    val strokes: List<DrawingStroke>,
    val seconds: Int,
    val createdAt: Long,
    val expiresAt: Long
)

/** What came of one of the player's links: somebody finished it ([solved] in [attempts] tries, or gave up). */
data class GuessInboxEntry(
    val id: String,
    val linkId: String,
    val fromUid: String,
    val fromName: String,
    val solved: Boolean,
    val attempts: Int,
    val at: Long
)
