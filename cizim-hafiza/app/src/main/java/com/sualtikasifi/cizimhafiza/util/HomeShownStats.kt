package com.sualtikasifi.cizimhafiza.util

/**
 * What the home screen last showed for gold and total XP, kept in memory for the life of the process.
 *
 * The home screen counts its numbers up from here to the real values when it opens: a match paid out somewhere else
 * (the game and result screens), and without a remembered "before" the home screen could only ever draw the final
 * numbers, never the climb. Null until the first time the screen has drawn them.
 */
object HomeShownStats {
    @Volatile var gold: Int? = null
    @Volatile var totalXp: Int? = null
}
