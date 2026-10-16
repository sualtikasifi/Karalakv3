package com.sualtikasifi.cizimhafiza.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PostMatchPromptsTest {

    @Test
    fun `no reminder before the first threshold`() {
        assertNull(PostMatchPrompts.nextReminderStage(games = 9, stage = 0))
    }

    @Test
    fun `the first reminder is due at ten games and moves to stage one`() {
        assertEquals(1, PostMatchPrompts.nextReminderStage(games = 10, stage = 0))
    }

    @Test
    fun `the second reminder waits for thirty games`() {
        assertNull(PostMatchPrompts.nextReminderStage(games = 29, stage = 1))
        assertEquals(2, PostMatchPrompts.nextReminderStage(games = 30, stage = 1))
    }

    @Test
    fun `a restored high game count asks once, not once per threshold`() {
        assertEquals(2, PostMatchPrompts.nextReminderStage(games = 400, stage = 0))
        assertNull(PostMatchPrompts.nextReminderStage(games = 400, stage = 2))
    }

    @Test
    fun `all reminders used up means never again`() {
        assertNull(PostMatchPrompts.nextReminderStage(games = 10_000, stage = PostMatchPrompts.SIGN_IN_REMINDER_AT.size))
    }
}
