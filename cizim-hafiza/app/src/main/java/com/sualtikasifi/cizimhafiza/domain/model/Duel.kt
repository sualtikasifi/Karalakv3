package com.sualtikasifi.cizimhafiza.domain.model

/** Lifecycle of an asynchronous duel — see DuelRepository/firestore.rules' duels/{duelId}. */
enum class DuelStatus { AWAITING_OPPONENT, COMPLETE }

/**
 * One asynchronous duel: the challenger plays a normal solo round (see
 * GameViewModel's duel-challenge args), and [items] — that round's own
 * drawings, kept only so the challenger has something to look back on —
 * becomes a challenge the opponent can open whenever they next launch the
 * app. Opening it has the opponent play the exact same [wordIds], in the
 * same order, through an ordinary draw-then-guess round of their own (see
 * GameViewModel's duel-completion arg) — never a look at the challenger's
 * drawings before playing, so nothing about the challenger's round can hint
 * at the answers. Whoever scored higher wins (see [challengerWon]).
 */
data class Duel(
    val id: String,
    val challengerUid: String,
    val challengerName: String,
    val opponentUid: String,
    val opponentName: String,
    /** The challenger's own finished round — word, whether THEY guessed it right, and its strokes shown while the opponent draws the same words themselves. */
    val items: List<ResultItem>,
    val challengerScore: Int,
    val challengerCorrectCount: Int,
    /**
     * The exact word ids the challenger drew, in the order they drew them —
     * what lets the opponent's own round (see GameViewModel's duelId-to-complete
     * arg) ask the identical questions instead of a fresh random set. Empty
     * only for a duel created before this field existed; such a duel can no
     * longer be completed (see DuelListViewModel).
     */
    val wordIds: List<Int> = emptyList(),
    /** Null until the opponent has played (see DuelStatus.COMPLETE). */
    val opponentScore: Int? = null,
    val opponentCorrectCount: Int? = null,
    val status: DuelStatus = DuelStatus.AWAITING_OPPONENT,
    val createdAt: Long = 0L,
    val completedAt: Long? = null,
    /** False right after the opponent completes it — clears once the challenger has opened the result once. */
    val seenByChallenger: Boolean = true
) {
    val totalWords: Int get() = items.size

    /** Null until COMPLETE. True if the challenger's own round outscored the opponent's guesses; null again on an exact tie. */
    val challengerWon: Boolean?
        get() = opponentScore?.let { score ->
            when {
                challengerScore > score -> true
                challengerScore < score -> false
                else -> null
            }
        }
}
