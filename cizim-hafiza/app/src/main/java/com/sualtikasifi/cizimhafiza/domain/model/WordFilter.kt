package com.sualtikasifi.cizimhafiza.domain.model

/**
 * What a player may pick several of when setting up a round: categories and difficulties.
 *
 * A selection travels through places that carry one plain string — a navigation route segment, a Firestore field —
 * so several values are joined with a comma. Nothing selected means "all" (the old single "Tümü" choice), and a
 * single value is written exactly as it always was, so older rooms and routes read the same way.
 */
object WordFilter {
    private const val SEPARATOR = ","

    fun encodeCategories(categories: Collection<String>): String? =
        categories.filter { it.isNotBlank() }.sorted().joinToString(SEPARATOR).ifEmpty { null }

    fun decodeCategories(raw: String?): List<String> =
        raw?.split(SEPARATOR)?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()

    fun encodeDifficulties(difficulties: Collection<Difficulty>): String? =
        difficulties.sortedBy { it.ordinal }.joinToString(SEPARATOR) { it.name }.ifEmpty { null }

    fun decodeDifficulties(raw: String?): Set<Difficulty> =
        raw?.split(SEPARATOR)
            ?.mapNotNull { part -> runCatching { Difficulty.valueOf(part.trim()) }.getOrNull() }
            ?.toSet()
            .orEmpty()
}
