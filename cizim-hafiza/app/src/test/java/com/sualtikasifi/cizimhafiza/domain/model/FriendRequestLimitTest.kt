package com.sualtikasifi.cizimhafiza.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FriendRequestLimitTest {
    private val now = 10 * FriendRequestLimit.WINDOW_MILLIS

    @Test
    fun `a fresh account may send`() {
        assertTrue(FriendRequestLimit.allows(emptyList(), now))
    }

    @Test
    fun `the twentieth request in a day is the last`() {
        val nineteen = List(FriendRequestLimit.MAX_PER_WINDOW - 1) { now - it * 1000L }
        assertTrue(FriendRequestLimit.allows(nineteen, now))
        val twenty = nineteen + (now - 30_000L)
        assertFalse(FriendRequestLimit.allows(twenty, now))
    }

    @Test
    fun `requests older than a day stop counting`() {
        val old = List(FriendRequestLimit.MAX_PER_WINDOW) { now - FriendRequestLimit.WINDOW_MILLIS - it }
        assertTrue(FriendRequestLimit.allows(old, now))
        assertEquals(0, FriendRequestLimit.recent(old, now).size)
    }

    @Test
    fun `a request exactly a day old no longer counts`() {
        assertEquals(0, FriendRequestLimit.recent(listOf(now - FriendRequestLimit.WINDOW_MILLIS), now).size)
        assertEquals(1, FriendRequestLimit.recent(listOf(now - FriendRequestLimit.WINDOW_MILLIS + 1), now).size)
    }
}
