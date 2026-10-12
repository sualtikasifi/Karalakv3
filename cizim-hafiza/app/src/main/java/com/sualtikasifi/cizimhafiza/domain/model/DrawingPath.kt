package com.sualtikasifi.cizimhafiza.domain.model

import kotlinx.serialization.Serializable

/**
 * One continuous finger stroke, as the raw points the user dragged through.
 * A drawing is a list of these, which lets us re-render (or replay) it as
 * vector paths instead of a fixed-resolution bitmap.
 */
@Serializable
data class DrawingPoint(val x: Float, val y: Float)

typealias DrawingStroke = List<DrawingPoint>

@Serializable
data class DrawingResult(
    val sessionId: Long,
    val wordId: Int,
    val word: Word,
    val strokes: List<DrawingStroke>,
    var userAnswer: String = "",
    var isCorrect: Boolean = false,
    var responseTimeMs: Long = 0L,
    var pointsAwarded: Int = 0
)

/**
 * One finished drawing's shareable outcome — word, whether it was guessed
 * correctly, and its vector strokes. Domain-level (not presentation-only)
 * because it's also what gets synced to Firestore for online-match result
 * comparison, JSON-encoded via [kotlinx.serialization].
 */
@Serializable
data class ResultItem(
    val word: String,
    val isCorrect: Boolean,
    val strokes: List<DrawingStroke>,
    /** The [PenSkin] name the drawer drew with, so a result shows each drawing in its own pen; null for older drawings (plain ink). */
    val penSkin: String? = null
) {
    /** The pen to draw this item with. Not level-gated: it is somebody else's pen as often as it is ours. */
    fun pen(): PenSkin? = penSkin?.let { name -> PenSkin.entries.find { it.name == name } }
}
