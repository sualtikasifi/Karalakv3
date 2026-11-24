package com.sualtikasifi.cizimhafiza.notifications

/**
 * Which of the duel notification texts comes next. A player who gets several duels does not see the same line twice in a row:
 * the phone remembers the last one shown and takes the one after it, round and round. The first time, [firstPick] is used, so
 * not every player starts on the same line.
 */
internal object DuelNotificationText {
    fun nextIndex(last: Int, size: Int, firstPick: Int): Int = when {
        size <= 0 -> 0
        last < 0 || last >= size -> firstPick.coerceIn(0, size - 1)
        else -> (last + 1) % size
    }
}
