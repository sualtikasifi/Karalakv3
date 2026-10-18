package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import com.sualtikasifi.cizimhafiza.presentation.reports.studio.guess.SpokenLine
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MascotEmotion
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.memory.MemoryCopy
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.memory.MemoryMath
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.memory.MemoryPhase
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.memory.MemoryPick
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.memory.MemoryScript
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.memory.MemoryTimeline
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.memory.MemoryWordStat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryTest {

    @Test
    fun delayCountsLaterDrawingsTheBreakAndEarlierGuesses() {
        // first of five: four more drawings (4×8) and the break (3)
        assertEquals(35L, MemoryMath.delaySeconds(0, 5))
        // last of five: the break, then four earlier guesses (4×7)
        assertEquals(31L, MemoryMath.delaySeconds(4, 5))
        // a ten-word Quick Match round waits longer
        assertTrue(MemoryMath.delaySeconds(0, 10) > MemoryMath.delaySeconds(0, 5))
    }

    @Test
    fun keysAreFileSafe() {
        assertEquals("ahtapot", MemoryMath.keyOf("Ahtapot"))
        assertEquals("kara_kedi", MemoryMath.keyOf("kara kedi"))
        assertEquals("cicek", MemoryMath.keyOf("Çiçek!"))
    }

    @Test
    fun keepsForgottenDrawingsFirstAndAFewRemembered() {
        val full = MemoryWordStat("k", "k", keptForgot = MemoryMath.MAX_FORGOT_SAMPLES, keptRemembered = 0)
        assertFalse(MemoryMath.shouldKeep(full, correct = false))
        assertTrue(MemoryMath.shouldKeep(full, correct = true))
        val remembered = MemoryWordStat("k", "k", keptRemembered = MemoryMath.MAX_REMEMBERED_SAMPLES)
        assertFalse(MemoryMath.shouldKeep(remembered, correct = true))
        assertTrue(MemoryMath.shouldKeep(remembered, correct = false))
    }

    @Test
    fun statsRoundTheWayAPersonWouldSayThem() {
        val s = MemoryWordStat("k", "ahtapot", drawn = 300, forgot = 234, delaySum = 300L * 37)
        assertEquals(37, s.avgDelaySec)
        assertEquals(78, s.forgotPercent)
        assertEquals(35, MemoryMath.roundForSpeech(37))
        assertEquals(40, MemoryMath.roundForSpeech(38))
        assertEquals(8, MemoryMath.roundForSpeech(8))
    }

    @Test
    fun hookUsesTheArchiveNumbersExactly() {
        val text = MemoryCopy.hookText("Ahtapot", 300, 234, 37)
        assertEquals("300 kişi ahtapot çizdi. 234 kişi, yaklaşık 35 saniye sonra kendi çizimini tanıyamadı.", text)
        assertTrue(MemoryCopy.paradoxText("Ahtapot", 7).contains("ahtapot için yalnızca 7 saniyen var"))
    }

    @Test
    fun readsPicksAndIgnoresBadIndexes() {
        val text = """{"picks":[{"index":2,"emotion":"laughing","line":"Bu ahtapot mu örümcek mi?"},{"index":9,"line":"yok"},{"index":2,"line":"tekrar"},{"index":1}],"outro":{"emotion":"point","line":"Sen hatırlar mısın? karalak.com"},"caption":"Sen bilir miydin?","hashtags":"#karalak"}"""
        val p = MemoryCopy.parsePicks(text, 4)
        assertNotNull(p)
        assertEquals(listOf(1, 0), p!!.picks.map { it.first })
        assertEquals(MascotEmotion.Laughing, p.picks[0].second!!.emotion)
        assertNull(p.picks[1].second)
        assertEquals(MascotEmotion.Point, p.outro!!.emotion)
        assertNull(MemoryCopy.parsePicks("{}", 4))
    }

    @Test
    fun timelineRunsHookPicksTwistAndOutro() {
        val l = SpokenLine("x", MascotEmotion.Neutral)
        val script = MemoryScript(
            "ahtapot", 300, 234, 37, 7, l,
            listOf(MemoryPick("a", false, l), MemoryPick("b", false, null), MemoryPick("c", true, l)),
            l, l
        )
        assertEquals(5, script.lines().size)
        val events = MemoryTimeline.build(script) { _, _ -> 1_000L }
        assertEquals(listOf(MemoryPhase.Hook, MemoryPhase.Gallery, MemoryPhase.Gallery, MemoryPhase.Gallery, MemoryPhase.Paradox, MemoryPhase.Outro), events.map { it.phase })
        assertEquals(listOf(0, 1, 2, 3, 4), events.filter { it.line != null }.map { it.lineIndex })
        events.zipWithNext().forEach { (a, b) -> assertEquals(a.endMs, b.startMs) }
        assertTrue(script.showPercent)
        assertEquals(78, script.percent)
    }
}
