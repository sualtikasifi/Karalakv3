package com.sualtikasifi.cizimhafiza.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Which topics a device joins and leaves: the names the server builds its conditions from (functions/src/adminTasks.ts). */
class BroadcastTopicTest {

    @Test
    fun weekTopicsAreTheIsoWeekLikeTheServersAreNamed() {
        assertEquals("seen_2026w45", BroadcastTopic.weekTopic(LocalDate.of(2026, 11, 7)))
        // the first of January can still be the last week of the year before
        assertEquals("seen_2026w53", BroadcastTopic.weekTopic(LocalDate.of(2027, 1, 1)))
        assertEquals("seen_2026w02", BroadcastTopic.weekTopic(LocalDate.of(2026, 1, 5)))
    }

    @Test
    fun aDeviceJoinsNewsItsLanguageAndThisAndLastWeek() {
        val (join, leave) = BroadcastTopic.plan(true, "tr", LocalDate.of(2026, 11, 7))
        assertEquals(setOf("news", "seen_v1", "news_tr", "seen_2026w45", "seen_2026w44"), join)
        // the other languages and the weeks before last go
        assertTrue("news_en" in leave && "news_pt" in leave && "news_es" in leave)
        assertTrue("seen_2026w43" in leave && "seen_2026w42" in leave)
        assertTrue(join.intersect(leave).isEmpty())
    }

    @Test
    fun aLanguageThatIsNotShippedFallsBackToEnglish() {
        val (join, _) = BroadcastTopic.plan(true, "de", LocalDate.of(2026, 11, 7))
        assertTrue("news_en" in join)
    }

    @Test
    fun withNotificationsOffEveryTopicIsLeft() {
        val (join, leave) = BroadcastTopic.plan(false, "tr", LocalDate.of(2026, 11, 7))
        assertTrue(join.isEmpty())
        assertTrue(setOf("news", "seen_v1", "news_tr", "news_en", "seen_2026w45", "seen_2026w44").all { it in leave })
    }
}
