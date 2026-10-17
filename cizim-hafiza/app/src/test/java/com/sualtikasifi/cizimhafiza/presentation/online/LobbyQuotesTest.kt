package com.sualtikasifi.cizimhafiza.presentation.online

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LobbyQuotesTest {
    @Test
    fun neverShowsTheSameNoteTwiceInARow() {
        var last = -1
        repeat(500) {
            val i = LobbyQuotes.next(13)
            assertTrue(i in 0 until 13)
            assertTrue(i != last)
            last = i
        }
    }

    @Test
    fun aSingleNoteIsAlwaysIndexZero() {
        assertEquals(0, LobbyQuotes.next(1))
        assertEquals(0, LobbyQuotes.next(1))
    }
}
