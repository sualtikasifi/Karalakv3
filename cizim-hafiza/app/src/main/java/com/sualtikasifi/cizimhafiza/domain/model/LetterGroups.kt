package com.sualtikasifi.cizimhafiza.domain.model

/**
 * Letters per word of an answer, for the hangman-style blanks the "Harf sayısı"
 * joker draws: "Gün batımı" → [3, 6]. Letters and digits count, punctuation
 * and spaces do not — the same rule the joker's plain count always used.
 * Words with nothing countable are dropped, so an answer is never a row of
 * empty groups.
 */
fun letterGroupsOf(answer: String): List<Int> =
    answer.split(' ', '-', '\u00A0')
        .map { part -> part.count { it.isLetterOrDigit() } }
        .filter { it > 0 }
