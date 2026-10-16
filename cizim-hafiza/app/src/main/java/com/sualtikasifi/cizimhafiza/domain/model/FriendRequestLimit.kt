package com.sualtikasifi.cizimhafiza.domain.model

/**
 * How many NEW friend requests one account may send in a day. The recipient's inbox takes one request per sender, so
 * a loop over friend codes was the only way to flood someone — or everyone. The rules cannot count (and a rule that
 * demanded a counter write would break every installed older version), so the app itself keeps the count; honest
 * players never come near it.
 */
object FriendRequestLimit {
    const val MAX_PER_WINDOW = 20
    const val WINDOW_MILLIS = 24 * 60 * 60 * 1000L

    /** The sends that still count at [now]. */
    fun recent(sentAt: List<Long>, now: Long): List<Long> = sentAt.filter { it in (now - WINDOW_MILLIS + 1)..now }

    /** Whether one more request may go out at [now], given the times of earlier ones. */
    fun allows(sentAt: List<Long>, now: Long): Boolean = recent(sentAt, now).size < MAX_PER_WINDOW
}
