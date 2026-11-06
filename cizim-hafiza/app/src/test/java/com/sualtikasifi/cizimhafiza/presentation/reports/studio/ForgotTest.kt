package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import com.sualtikasifi.cizimhafiza.domain.model.ChestTier
import com.sualtikasifi.cizimhafiza.domain.model.CouponCodes
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotChest
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotCopy
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotMode
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotPhase
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotScript
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotTimeline
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.SpokenLine
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MascotEmotion
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealTimeline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ForgotTest {
    private fun line(t: String) = SpokenLine(t, MascotEmotion.Neutral)

    private fun script(guesses: Int = 3, mode: ForgotMode = ForgotMode.Word) = ForgotScript(
        drawingId = 1, word = "şemsiye",
        hook = line("hook"), guesses = List(guesses) { line("tahmin $it") },
        codeLine = line("kod"), outro = line("bitiş"), mode = mode, reward = "Sanatçı Kasası", code = "SEMSIYE"
    )

    @Test
    fun linesAreInSpeakingOrderAndCapped() {
        assertEquals(listOf("hook", "tahmin 0", "tahmin 1", "tahmin 2", "kod", "bitiş"), script().lines().map { it.text })
        // more looks than the video has room for are left out
        assertEquals(1 + ForgotTimeline.MAX_GUESSES + 2, script(guesses = 7).lines().size)
    }

    @Test
    fun timelineRunsHookGuessesCountdownCodeOutro() {
        val s = script(guesses = 2)
        val events = ForgotTimeline.build(s) { _, _ -> 1_000L }
        assertEquals(
            // the hook already draws the drawing as far as the first guess: only the later guesses have a stretch of drawing of their own
            listOf(
                ForgotPhase.Hook, ForgotPhase.Guess, ForgotPhase.Draw, ForgotPhase.Guess,
                ForgotPhase.Countdown, ForgotPhase.Code, ForgotPhase.Outro
            ),
            events.map { it.phase }
        )
        // back to back, no gaps, and each spoken event carries the index of its line
        events.zipWithNext().forEach { (a, b) -> assertEquals(a.endMs, b.startMs) }
        assertEquals(s.lines().indices.toList(), events.filter { it.line != null }.map { it.lineIndex })
        assertEquals(events.last().endMs, ForgotTimeline.totalMs(events))
    }

    @Test
    fun drawingIsBegunOnTheFirstFrameAndBuildsUpToTheWhole() {
        val events = ForgotTimeline.build(script(guesses = 3)) { _, _ -> 500L }
        val hook = events.first()
        // never an empty board: a few strokes are on it from the start, and the hook draws it on to the first stop
        assertTrue(hook.drawFrom > 0f)
        assertTrue(hook.drawTo > hook.drawFrom)
        val draws = events.filter { it.phase == ForgotPhase.Draw }
        assertEquals(1f, events.filter { it.phase == ForgotPhase.Guess }.last().drawTo, 0f)
        // one stretch of drawing between each pair of guesses, each starting where the last guess stopped
        assertEquals(2, draws.size)
        assertEquals(hook.drawTo, events.filter { it.phase == ForgotPhase.Guess }.first().drawTo, 0f)
        draws.zip(events.filter { it.phase == ForgotPhase.Guess }.dropLast(1)).forEach { (draw, before) -> assertEquals(before.drawTo, draw.drawFrom, 0f) }
    }

    @Test
    fun guessesStopWhereTheyWereToldTo() {
        val events = ForgotTimeline.build(script(guesses = 3).copy(guessAt = listOf(0.2f, 0.6f, 1f))) { _, _ -> 500L }
        assertEquals(listOf(0.2f, 0.6f, 1f), events.filter { it.phase == ForgotPhase.Guess }.map { it.drawTo })
        val draws = events.filter { it.phase == ForgotPhase.Draw }
        assertEquals(listOf(0.2f, 0.6f), draws.map { it.drawFrom })
        assertEquals(listOf(0.6f, 1f), draws.map { it.drawTo })
        // the hook draws it from the first frame (a few strokes) to the first stop
        assertEquals(0.08f, events.first().drawFrom, 0f)
        assertEquals(0.2f, events.first().drawTo, 0f)
    }

    @Test
    fun stopsAreSpreadEvenlyWhereNoneWasSet() {
        assertEquals(listOf(1f), ForgotTimeline.stopsFor(1, emptyList()))
        assertEquals(listOf(0.5f, 1f), ForgotTimeline.stopsFor(2, emptyList()))
        // one set by hand, the rest even
        assertEquals(listOf(0.1f, 2f / 3f, 1f), ForgotTimeline.stopsFor(3, listOf(0.1f)))
        assertTrue(ForgotTimeline.stopsFor(0, listOf(0.5f)).isEmpty())
    }

    @Test
    fun stopsNeverGoBackwardsOrOffThePaper() {
        // a later stop that was set before an earlier one is lifted to it; the ones past the whole or before the start are held in
        assertEquals(listOf(0.6f, 0.6f, 1f), ForgotTimeline.stopsFor(3, listOf(0.6f, 0.3f, 2f)))
        assertEquals(listOf(ForgotTimeline.MIN_STOP, 1f), ForgotTimeline.stopsFor(2, listOf(-1f, 5f)))
    }

    @Test
    fun theScriptReadsItsStopsToTheNumberOfGuesses() {
        assertEquals(listOf(0.2f, 0.5f, 1f), script(guesses = 3).copy(guessAt = listOf(0.2f, 0.5f, 1f)).stops)
        // fewer guesses than stops set: the extra ones are not used
        assertEquals(listOf(0.2f), script(guesses = 1).copy(guessAt = listOf(0.2f, 0.5f, 1f)).stops)
    }

    @Test
    fun theAssistantIsToldEachGuessSDrawingAndNotToSayTheWord() {
        val prompt = ForgotCopy.guessPrompt("şemsiye", listOf(0.3f, 0.65f, 1f), "neutral, thinking")
        assertTrue(prompt.contains("Görsel 1: çizimin %30'i"))
        assertTrue(prompt.contains("Görsel 2: çizimin %65'i"))
        assertTrue(prompt.contains("Görsel 3: çizimin %100'i"))
        assertTrue(prompt.contains("Görsel 4: bitmiş çizim"))
        assertTrue(prompt.contains("tam 3 tahmin"))
        assertTrue(prompt.contains("EN FAZLA ${ForgotCopy.GUESS_WORDS} kelime"))
        assertTrue(prompt.contains("EN FAZLA ${ForgotCopy.HOOK_WORDS} kelime"))
        assertFalse(prompt.contains("reaction"))
        assertTrue(prompt.contains("\"şemsiye\" kelimesi"))
        assertFalse(prompt.contains("@@PICTURES@@"))
        assertTrue(prompt.contains("neutral, thinking"))
    }

    @Test
    fun theStockVideoWithItsEndCardRunsUnderTwentySeconds() {
        val stock = script(guesses = ForgotCopy.DEFAULT_GUESSES).copy(
            hook = line(ForgotCopy.HOOK), guesses = ForgotCopy.placeholderGuesses().take(ForgotCopy.DEFAULT_GUESSES),
            codeLine = line(ForgotCopy.CODE_WORD), outro = line(ForgotCopy.OUTRO)
        )
        // voices at the pace the preview assumes, and the how-to video that plays in the end card (6.3 s)
        val events = ForgotTimeline.build(stock, howtoMs = 6_300L) { _, l -> StudioCopy.estimateMs(l.text) }
        val total = ForgotTimeline.totalMs(events)
        assertTrue("took $total ms", total <= 20_000L)
        // and it was around 30 s before the cuts
        assertTrue("took $total ms", total >= 12_000L)
    }

    @Test
    fun twoGuessesIsTheDefault() {
        assertEquals(2, ForgotCopy.DEFAULT_GUESSES)
        assertEquals(listOf(0.5f, 1f), ForgotTimeline.stopsFor(ForgotCopy.DEFAULT_GUESSES, emptyList()))
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
             "hook":{"emotion":"shocked","line":"Çizen şemsiye dedi"},
             "outro":{"emotion":"wave","line":"Yarın görüşürüz, karalak.com"},
             "caption":"Sen bilebilir misin?","hashtags":"#karalak #çizim"}```"""
        val w = ForgotCopy.parse(text, "şemsiye")
        assertNotNull(w)
        assertEquals(listOf("Mantar mı bu?", "Denizanası!"), w!!.guesses.map { it.text })
        assertEquals(MascotEmotion.Thinking, w.guesses.first().emotion)
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
        assertNull(ForgotCopy.parse("""{"hook":{"line":"x"}}""", "kedi"))
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
        assertTrue(ForgotCopy.codeLineFor(ForgotMode.Shown).contains("Ayarlar"))
        // the reaction and the code are one line
        assertTrue(ForgotCopy.codeLineFor(ForgotMode.Word).startsWith("Bilemedin mi?"))
    }

    @Test
    fun aChestIsNoneOrOneOfATier() {
        assertTrue(ForgotChest.None.chests.isEmpty())
        assertEquals(mapOf(ChestTier.GOLD to 1), ForgotChest.Gold.chests)
        assertEquals(ChestTier.RARE, ForgotChest.Rare.tier)
    }

    @Test
    fun theHandSetRewardReadsWithoutAChest() {
        assertEquals("40 XP + 1250 altın", ForgotCopy.rewardLabel(1250, 40, ForgotChest.None.chests))
        assertEquals("Sürpriz Kasası + 40 XP", ForgotCopy.rewardLabel(0, 40, ForgotChest.Rare.chests))
    }

    @Test
    fun theClosingStaysUntilTheHowToVideoHasPlayedInTheEndCard() {
        val plain = ForgotTimeline.build(script()) { _, _ -> 500L }
        val withCard = ForgotTimeline.build(script(), howtoMs = 8_000L) { _, _ -> 500L }
        assertEquals(plain.dropLast(1).map { it.endMs }, withCard.dropLast(1).map { it.endMs })
        assertTrue(withCard.last().durationMs >= 8_000L + RealTimeline.END_VIDEO_LEAD + RealTimeline.END_VIDEO_TAIL)
        assertTrue(withCard.last().durationMs > plain.last().durationMs)
    }

    @Test
    fun aHowToVideoLongerThanTheLimitIsCutAtIt() {
        val limit = RealTimeline.HOWTO_MAX + RealTimeline.END_VIDEO_LEAD + RealTimeline.END_VIDEO_TAIL
        val long = ForgotTimeline.build(script(), howtoMs = 60_000L) { _, _ -> 500L }
        assertEquals(limit, long.last().durationMs)
    }

    @Test
    fun aNewCodeStopsWorkingTwoDaysOut() {
        assertEquals(1_000L + 48L * 3_600_000L, ForgotCopy.defaultExpiry(1_000L))
    }
}
