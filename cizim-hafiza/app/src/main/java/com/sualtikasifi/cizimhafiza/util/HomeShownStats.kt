package com.sualtikasifi.cizimhafiza.util

/**
 * Gold and XP earned by playing since the home screen last drew its numbers, kept in memory for the life of the process.
 *
 * The home screen counts its numbers up from "what they were before" to the real values when it opens, so a match's
 * winnings are seen arriving. "Before" is the real value minus what was earned in between — never a remembered or
 * assumed older value, which on a fresh start made the whole lifetime total climb from zero. Nothing earned, nothing
 * animates.
 */
object HomeShownStats {
    @Volatile var pendingGold: Int = 0
    @Volatile var pendingXp: Int = 0

    /** Gold and XP just taken back by a moderation penalty — the home screen counts them DOWN, with a red "-N". */
    @Volatile var pendingGoldLoss: Int = 0
    @Volatile var pendingXpLoss: Int = 0
}
