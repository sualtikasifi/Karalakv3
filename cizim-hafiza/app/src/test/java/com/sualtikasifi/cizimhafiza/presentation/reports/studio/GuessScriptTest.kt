package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import com.sualtikasifi.cizimhafiza.presentation.reports.studio.guess.GuessCopy
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.guess.GuessPhase
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.guess.GuessRound
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.guess.GuessScript
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.guess.GuessTimeline
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.guess.SpokenLine
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MascotEmotion
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MouthState
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.mouthFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GuessScriptTest {
    private fun line(t: String) = SpokenLine(t, MascotEmotion.Thinking)

    private fun script(correct: List<Boolean>) = GuessScript(
        line("intro"),
        correct.mapIndexed { i, c ->
            GuessRound(i.toLong(), "kedi", listOf(line("a$i"), line("b$i"), line("c$i")), "kedi", c, line("r$i"))
        },
        line("outro")
    )

    @Test
    fun linesAreInSpeakingOrder() {
        val names = script(listOf(true, false)).lines().map { it.text }
        assertEquals(listOf("intro", "a0", "b0", "c0", "r0", "a1", "b1", "c1", "r1", "outro"), names)
    }

    @Test
    fun timelineHasNoGapsAndUsesEveryLineOnce() {
        val sc = script(listOf(true, false, true))
        val events = GuessTimeline.build(sc) { _, _ -> 1_000L }
        events.zipWithNext().forEach { (a, b) -> assertEquals(a.endMs, b.startMs) }
        val spoken = events.filter { it.line != null }.map { it.lineIndex }
        assertEquals((0 until sc.lines().size).toList(), spoken)
        assertEquals(GuessPhase.Intro, events.first().phase)
        assertEquals(GuessPhase.Outro, events.last().phase)
        // the drawing only ever moves forward within a round
        events.filter { it.phase == GuessPhase.Draw }.forEach { assertTrue(it.drawTo > it.drawFrom) }
    }

    @Test
    fun scoreCountsOnlyShownVerdicts() {
        val sc = script(listOf(true, false))
        val events = GuessTimeline.build(sc) { _, _ -> 800L }
        val verdicts = events.filter { it.phase == GuessPhase.Verdict }
        assertEquals(0 to 0, GuessTimeline.scoreAt(sc, events, verdicts[0].startMs - 1))
        assertEquals(1 to 0, GuessTimeline.scoreAt(sc, events, verdicts[0].startMs))
        assertEquals(1 to 1, GuessTimeline.scoreAt(sc, events, GuessTimeline.totalMs(events)))
        assertEquals(1, sc.mascotScore)
        assertEquals(1, sc.playerScore)
    }

    @Test
    fun readsKaralaksLooks() {
        val text = """```json
            {"stages":[{"emotion":"thinking","line":"Bir daire… güneş mi?"},{"emotion":"excited","line":"Kulak çıktı, ayı!"},{"emotion":"confident","line":"Yok yok, kedi!"}],"final_guess":"Kedi"}
            ```"""
        val looks = GuessCopy.parseLooks(text)
        assertNotNull(looks)
        assertEquals(3, looks!!.first.size)
        assertEquals(MascotEmotion.Excited, looks.first[1].emotion)
        assertEquals("Kedi", looks.second)
        assertNull(GuessCopy.parseLooks("bilmiyorum"))
    }

    @Test
    fun readsFrameWithPostText() {
        val text = """{"intro":{"emotion":"confident","line":"Ayşe bana 2 çizim gönderdi."},"reactions":[{"emotion":"victory","line":"Bildim!"},{"emotion":"embarrassed","line":"Balıkmış, utandım."}],"outro":{"emotion":"wave","line":"karalak.com'da çiz!"},"caption":"Sen bilir miydin?","hashtags":"#karalak"}"""
        val f = GuessCopy.parseFrame(text, 2)
        assertNotNull(f)
        assertEquals(2, f!!.reactions.size)
        assertEquals(MascotEmotion.Wave, f.outro.emotion)
        assertEquals("#karalak", f.hashtags)
        assertNull(GuessCopy.parseFrame(text, 3))
    }

    @Test
    fun unknownEmotionFallsBackToCalm() {
        assertEquals(MascotEmotion.Neutral, MascotEmotion.of("melancholic"))
        assertEquals(MascotEmotion.ThumbsUp, MascotEmotion.of(" ThumbsUp "))
    }

    @Test
    fun judgesGuessesFairly() {
        assertTrue(GuessCopy.matches("Kedi", "kedi"))
        assertTrue(GuessCopy.matches("kara kedi", "kedi"))
        assertTrue(GuessCopy.matches("zürafa", "zurafa"))
        assertTrue(GuessCopy.matches("buzdolab", "buzdolabı"))
        assertTrue(GuessCopy.matches("şemsiye!", "Şemsiye"))
        assertFalse(GuessCopy.matches("ayı", "kedi"))
        assertFalse(GuessCopy.matches("at", "ot"))
        assertFalse(GuessCopy.matches("", "kedi"))
    }

    @Test
    fun mouthOpensWithTheVoice() {
        assertEquals(MouthState.Closed, mouthFor(0.05f))
        assertEquals(MouthState.Half, mouthFor(0.3f))
        assertEquals(MouthState.Open, mouthFor(0.9f))
    }
}
