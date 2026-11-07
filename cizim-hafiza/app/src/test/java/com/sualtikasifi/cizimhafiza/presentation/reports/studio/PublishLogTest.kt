package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The log of made videos and how the studio's spend is written. */
class PublishLogTest {

    private fun entry(id: Long, label: String = "şemsiye", code: String = "SEMSIYE", madeAt: Long = id, published: Boolean = false) =
        PublishEntry(id, "Düello", label, "Oyun dili · Bilen alır", code, "500 altın", madeAt, published)

    @Test
    fun aNewVideoGoesToTheTop() {
        val log = PublishLog.record(listOf(entry(1, label = "kedi", code = "KEDI")), entry(2_000_000))
        assertEquals(listOf(2_000_000L, 1L), log.map { it.id })
    }

    @Test
    fun theSameVideoMadeAgainSoonReplacesItsLineAndKeepsItsId() {
        val first = entry(1_000)
        val log = PublishLog.record(listOf(first), entry(1_000 + 60_000, madeAt = 1_000 + 60_000))
        assertEquals(1, log.size)
        assertEquals(1_000L, log.first().id)
        assertEquals(61_000L, log.first().madeAt)
    }

    @Test
    fun theSameVideoLongAfterOrAlreadyPostedIsALineOfItsOwn() {
        val posted = entry(1_000, published = true)
        assertEquals(2, PublishLog.record(listOf(posted), entry(2_000, madeAt = 2_000)).size)
        val old = entry(1_000)
        assertEquals(2, PublishLog.record(listOf(old), entry(5_000_000, madeAt = 5_000_000)).size)
    }

    @Test
    fun theLogKeepsTheNewestTwoHundred() {
        var log = emptyList<PublishEntry>()
        repeat(PublishLog.MAX + 20) { i -> log = PublishLog.record(log, entry(i * 10_000_000L, label = "w$i", code = "C$i")) }
        assertEquals(PublishLog.MAX, log.size)
        assertEquals("w${PublishLog.MAX + 19}", log.first().label)
    }

    @Test
    fun postingStampsTheTimeAndTakingItBackClearsIt() {
        val log = listOf(entry(1), entry(2, label = "kedi", code = "KEDI"))
        val posted = PublishLog.toggle(log, 2, now = 9_999L)
        assertTrue(posted.first { it.id == 2L }.published)
        assertEquals(9_999L, posted.first { it.id == 2L }.publishedAt)
        assertFalse(posted.first { it.id == 1L }.published)
        val back = PublishLog.toggle(posted, 2, now = 12_000L)
        assertFalse(back.first { it.id == 2L }.published)
        assertEquals(0L, back.first { it.id == 2L }.publishedAt)
        assertEquals(1, PublishLog.remove(back, 1).size)
    }

    @Test
    fun theLogSurvivesBeingSavedAndAnythingUnreadableIsEmpty() {
        val log = listOf(entry(1), entry(2, label = "kedi", code = "KEDI", published = true))
        assertEquals(log, PublishLog.decode(PublishLog.encode(log)))
        assertTrue(PublishLog.decode("not json").isEmpty())
        assertTrue(PublishLog.decode(null).isEmpty())
    }

    @Test
    fun spendIsWrittenWithTheRightNumberOfDecimals() {
        assertEquals("$0.0123", SpendText.dollars(0.0123))
        assertEquals("$0.0000", SpendText.dollars(0.0))
        assertEquals("$12.34", SpendText.dollars(12.3412))
        assertEquals("$0.50", SpendText.dollars(0.5))
    }

    @Test
    fun creditsAreAShareOfTheLimitOrNothingWithoutOne() {
        assertEquals(50, SpendText.percent(500, 1000))
        assertEquals(100, SpendText.percent(5000, 1000))
        assertNull(SpendText.percent(10, 0))
    }
}
