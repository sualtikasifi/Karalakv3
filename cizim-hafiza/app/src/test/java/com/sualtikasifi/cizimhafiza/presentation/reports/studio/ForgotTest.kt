package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import com.sualtikasifi.cizimhafiza.domain.model.ChestTier
import com.sualtikasifi.cizimhafiza.domain.model.CouponCodes
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotCopy
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotMode
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotPhase
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotScript
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotTimeline
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.SpokenLine
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MascotEmotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ForgotTest {
    private fun line(t: String) = SpokenLine(t, MascotEmotion.Neutral)

    private fun script(guesses: Int = 3, mode: ForgotMode = ForgotMode.Word) = ForgotScript(
        drawingId = 1, word = "şemsiye", drawerAnswer = "denizanası",
        hook = line("hook"), guesses = List(guesses) { line("tahmin $it") }, reaction = line("tepki"),
        codeLine = line("kod"), outro = line("bitiş"), mode = mode, reward = "Sanatçı Kasası", code = "SEMSIYE"
    )

    @Test
    fun linesAreInSpeakingOrderAndCapped() {
        assertEquals(listOf("hook", "tahmin 0", "tahmin 1", "tahmin 2", "tepki", "kod", "bitiş"), script().lines().map { it.text })
        // more looks than the video has room for are left out
        assertEquals(1 + ForgotTimeline.MAX_GUESSES + 3, script(guesses = 7).lines().size)
    }

    @Test
    fun timelineRunsHookDrawGuessCountdownAnswerCodeOutro() {
        val s = script(guesses = 2)
        val events = ForgotTimeline.build(s) { _, _ -> 1_000L }
        assertEquals(
            listOf(
                ForgotPhase.Hook, ForgotPhase.Draw, ForgotPhase.Guess, ForgotPhase.Draw, ForgotPhase.Guess,
                ForgotPhase.Countdown, ForgotPhase.Answer, ForgotPhase.Code, ForgotPhase.Outro
            ),
            events.map { it.phase }
        )
        // back to back, no gaps, and each spoken event carries the index of its line
        events.zipWithNext().forEach { (a, b) -> assertEquals(a.endMs, b.startMs) }
        assertEquals(s.lines().indices.toList(), events.filter { it.line != null }.map { it.lineIndex })
        assertEquals(events.last().endMs, ForgotTimeline.totalMs(events))
    }

    @Test
    fun drawingBuildsUpAcrossTheGuessesToTheWhole() {
        val events = ForgotTimeline.build(script(guesses = 3)) { _, _ -> 500L }
        val draws = events.filter { it.phase == ForgotPhase.Draw }
        assertEquals(0f, draws.first().drawFrom, 0f)
        assertEquals(1f, draws.last().drawTo, 0f)
        draws.zipWithNext().forEach { (a, b) -> assertEquals(a.drawTo, b.drawFrom, 0f) }
    }

    @Test
    fun aLongVoiceStretchesItsStretchOfTheVideo() {
        val short = ForgotTimeline.build(script()) { _, _ -> 200L }
        val long = ForgotTimeline.build(script()) { _, _ -> 6_000L }
        assertTrue(ForgotTimeline.totalMs(long) > ForgotTimeline.totalMs(short))
    }

    @Test
    fun turkishWordsMakeCodesThatCanBeTypedEitherWay() {
        assertEquals("SEMSIYE", ForgotCopy.wordCode("Şemsiye"))
        assertEquals("SEMSIYE", CouponCodes.normalize("şemsiye"))
        assertEquals("SEMSIYE", CouponCodes.normalize(" ŞEMSİYE "))
        assertEquals("CIGLIK", CouponCodes.normalize("çığlık"))
        assertEquals("KARAKEDI", ForgotCopy.wordCode("kara kedi"))
        // too short to be a code
        assertNull(ForgotCopy.wordCode("ağ"))
        // the plain ones still work
        assertEquals("KRLK-7H3QM2", CouponCodes.normalize("krlk-7h3qm2"))
    }

    @Test
    fun aLineThatSaysTheWordIsCaught() {
        assertTrue(ForgotCopy.leaks("Bu bir şemsiye mi?", "şemsiye"))
        assertTrue(ForgotCopy.leaks("ŞEMSİYE!", "şemsiye"))
        assertTrue(ForgotCopy.leaks("Şemsiyeyi çizmiş", "şemsiye"))
        assertFalse(ForgotCopy.leaks("Denizanası mı bu?", "şemsiye"))
        assertFalse(ForgotCopy.leaks("Bir şey var burada", "şemsiye"))
    }

    @Test
    fun theAssistantsLinesAreReadAndTheLeakingOnesDropped() {
        val text = """```json
            {"guesses":[{"emotion":"thinking","line":"Mantar mı bu?"},{"emotion":"excited","line":"Şemsiye bu!"},{"emotion":"confident","line":"Denizanası!"}],
             "reaction":{"emotion":"laughing","line":"Ben de öyle sanmıştım!"},
             "hook":{"emotion":"shocked","line":"Çizen şemsiye dedi"},
             "outro":{"emotion":"wave","line":"Yarın görüşürüz, karalak.com"},
             "caption":"Sen bilebilir misin?","hashtags":"#karalak #çizim"}```"""
        val w = ForgotCopy.parse(text, "şemsiye")
        assertNotNull(w)
        assertEquals(listOf("Mantar mı bu?", "Denizanası!"), w!!.guesses.map { it.text })
        assertEquals(MascotEmotion.Thinking, w.guesses.first().emotion)
        assertEquals("Ben de öyle sanmıştım!", w.reaction?.text)
        // the hook said the word: dropped, the stock one stays
        assertNull(w.hook)
        assertEquals("Yarın görüşürüz, karalak.com", w.outro?.text)
        assertEquals("Sen bilebilir misin?", w.caption)
    }

    @Test
    fun aCaptionThatSaysTheWordIsBlanked() {
        val w = ForgotCopy.parse("""{"guesses":[{"emotion":"thinking","line":"Hmm"}],"caption":"Bu şemsiye mi?","hashtags":"#şemsiye"}""", "şemsiye")
        assertEquals("", w!!.caption)
        assertEquals("", w.hashtags)
    }

    @Test
    fun noGuessesIsNotAnAnswer() {
        assertNull(ForgotCopy.parse("""{"reaction":{"line":"x"}}""", "kedi"))
        assertNull(ForgotCopy.parse("not json", "kedi"))
    }

    @Test
    fun rewardsReadTheWayAViewerWouldSayThem() {
        assertEquals("Sanatçı Kasası", ForgotCopy.rewardLabel(0, 0, mapOf(ChestTier.GOLD to 1)))
        assertEquals("500 altın", ForgotCopy.rewardLabel(500, 0, emptyMap()))
        assertEquals("2 × Çırak Kasası + 300 XP", ForgotCopy.rewardLabel(0, 300, mapOf(ChestTier.SILVER to 2)))
        assertEquals("", ForgotCopy.rewardLabel(0, 0, emptyMap()))
    }

    @Test
    fun theStockPercentIsInTheRangeTheVideosUse() {
        repeat(200) { assertTrue(ForgotCopy.randomPercent() in 55..90) }
    }

    @Test
    fun defaultLinesNameTheModes() {
        assertTrue(ForgotCopy.codeLineFor(ForgotMode.Word).contains("cevap"))
        assertTrue(ForgotCopy.codeLineFor(ForgotMode.Shown).contains("kodun"))
        assertTrue(ForgotCopy.reactionText(" denizanası ").contains("“denizanası”"))
    }
}
