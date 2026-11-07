package com.sualtikasifi.cizimhafiza.presentation.reports.console

/**
 * The assistant's saved answers, kept on this phone and in Firestore (`adminAssistantHistory`, readable and writable by the
 * reviewer only) so a new phone, or a reinstall, still has them. Each answer is identified by the moment it was made.
 */
object AssistantHistory {
    /** How many answers are kept. */
    const val MAX = 30

    /** Both lists together, an answer in both counted once (the phone's own copy kept), the newest [MAX]. */
    fun merge(local: List<SavedAnswer>, remote: List<SavedAnswer>): List<SavedAnswer> =
        (local + remote).distinctBy { it.at }.sortedByDescending { it.at }.take(MAX)

    /** The answers only this phone has: they are sent up. */
    fun onlyLocal(local: List<SavedAnswer>, remote: List<SavedAnswer>): List<SavedAnswer> {
        val there = remote.map { it.at }.toSet()
        return local.filter { it.at !in there }
    }

    /** What falls off the end when [before] gets a new answer in front: these are removed from Firestore too. */
    fun dropped(all: List<SavedAnswer>): List<SavedAnswer> = all.sortedByDescending { it.at }.drop(MAX)

    /** The document for one answer. */
    fun toDoc(s: SavedAnswer): Map<String, Any> = buildMap {
        put("title", s.title.take(200))
        put("text", s.text.take(30_000))
        put("at", s.at)
        put("model", s.model.take(80))
        s.costUsd?.let { put("costUsd", it) }
    }

    /** The answer a document holds, or null when it is not one. */
    fun fromDoc(data: Map<String, Any?>): SavedAnswer? {
        val title = data["title"] as? String ?: return null
        val text = data["text"] as? String ?: return null
        val at = (data["at"] as? Number)?.toLong() ?: return null
        return SavedAnswer(title, text, at, data["model"] as? String ?: "", (data["costUsd"] as? Number)?.toDouble())
    }

    fun docId(s: SavedAnswer) = s.at.toString()
}
