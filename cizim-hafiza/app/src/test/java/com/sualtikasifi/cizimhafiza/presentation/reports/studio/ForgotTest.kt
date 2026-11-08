package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import com.sualtikasifi.cizimhafiza.domain.model.ChestTier
import com.sualtikasifi.cizimhafiza.domain.model.CouponCodes
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotChest
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotCopy
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotMode
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotStyle
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
        askLine = line("soru"), codeLine = line("kod"), outro = line("bitiş"), mode = mode, reward = "Sanatçı Kasası", code = "SEMSIYE"
    )

    @Test
    fun linesAreInSpeakingOrderAndCapped() {
        assertEquals(listOf("hook", "tahmin 0", "tahmin 1", "tahmin 2", "soru", "kod", "bitiş"), script().lines().map { it.text })
        // more looks than the video has room for are left out
        assertEquals(1 + ForgotTimeline.MAX_GUESSES + 3, script(guesses = 7).lines().size)
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
    fun aStopMovedByTheBarStaysBetweenItsNeighbours() {
        val stops = listOf(0.3f, 0.6f, 1f)
        assertEquals(listOf(0.3f, 0.45f, 1f), ForgotTimeline.moveStop(stops, 1, 0.45f))
        // dragged past the next one or back before the one before: held a hundredth short of it
        assertEquals(0.99f, ForgotTimeline.moveStop(stops, 1, 0.995f)[1], 1e-6f)
        assertEquals(0.31f, ForgotTimeline.moveStop(stops, 1, 0.1f)[1], 1e-6f)
        // the first may not go before the start, the last not past the whole
        assertEquals(ForgotTimeline.MIN_STOP, ForgotTimeline.moveStop(stops, 0, -3f)[0], 0f)
        assertEquals(1f, ForgotTimeline.moveStop(stops, 2, 7f)[2], 0f)
        // an index that is not there changes nothing
        assertEquals(stops, ForgotTimeline.moveStop(stops, 5, 0.5f))
    }

    @Test
    fun theScriptReadsItsStopsToTheNumberOfGuesses() {
        assertEquals(listOf(0.2f, 0.5f, 1f), script(guesses = 3).copy(guessAt = listOf(0.2f, 0.5f, 1f)).stops)
        // fewer guesses than stops set: the extra ones are not used
        assertEquals(listOf(0.2f), script(guesses = 1).copy(guessAt = listOf(0.2f, 0.5f, 1f)).stops)
    }

    @Test
    fun theAssistantIsToldEachGuessSDrawingAndTheScenesAndNotToSayTheWord() {
        val prompt = ForgotCopy.directorPrompt("şemsiye", listOf(0.3f, 0.65f, 1f), "neutral, thinking", ForgotMode.Word, "Sanatçı Kasası", "Sadece 100 kişi · Son: 8 Kasım 21:00")
        assertTrue(prompt.contains("Görsel 1: çizimin %30'i"))
        assertTrue(prompt.contains("Görsel 2: çizimin %65'i"))
        assertTrue(prompt.contains("Görsel 3: çizimin %100'i"))
        assertTrue(prompt.contains("Görsel 4: bitmiş çizim"))
        // the scene order, each with what is on screen at that moment
        assertTrue(prompt.contains("1. AÇILIŞ"))
        assertTrue(prompt.contains("TAHMİN 1 (Görsel 1): ekranda çizim %30"))
        assertTrue(prompt.contains("TAHMİN 3 (Görsel 3): ekranda çizim %100"))
        assertTrue(prompt.contains("5. SORU (geri sayım 3-2-1"))
        assertTrue(prompt.contains("6. KOD ANI"))
        assertTrue(prompt.contains("7. KAPANIŞ"))
        // it works out a plan first, and writes the two new lines
        assertTrue(prompt.contains("\"plan\""))
        assertTrue(prompt.contains("\"ask\""))
        assertTrue(prompt.contains("\"code_line\""))
        assertTrue(prompt.contains("tam 3 eleman"))
        assertTrue(prompt.contains("EN FAZLA ${ForgotCopy.GUESS_WORDS} kelime"))
        assertTrue(prompt.contains("EN FAZLA ${ForgotCopy.HOOK_WORDS} kelime"))
        assertTrue(prompt.contains("EN FAZLA ${ForgotCopy.ASK_WORDS} kelime"))
        assertTrue(prompt.contains("EN FAZLA ${ForgotCopy.CODE_WORDS} kelime"))
        assertTrue(prompt.contains("\"şemsiye\" kelimesi"))
        assertFalse(prompt.contains("@@PICTURES@@"))
        assertTrue(prompt.contains("neutral, thinking"))
        // what the screen already says is not to be said again
        assertTrue(prompt.contains("Sanatçı Kasası"))
        assertTrue(prompt.contains("Sadece 100 kişi"))
        assertTrue(prompt.contains("TEKRARLAMA"))
    }

    @Test
    fun theSceneForTheCodeFollowsTheMode() {
        val word = ForgotCopy.directorPrompt("kedi", listOf(1f), "m", ForgotMode.Word, "", "")
        val shown = ForgotCopy.directorPrompt("kedi", listOf(1f), "m", ForgotMode.Shown, "", "")
        assertTrue(word.contains("BOŞ kutuları"))
        assertFalse(word.contains("Ayarlar'dan girmelerini"))
        assertTrue(shown.contains("Ayarlar'dan girmelerini"))
        assertFalse(shown.contains("BOŞ kutuları"))
        // no reward set: nothing is said about one
        assertFalse(word.contains("Bu videonun ödülü"))
    }

    @Test
    fun codeBoxesGoInEvenRowsNeverALoneOne() {
        // a code written out (KRLK-7H3QM2, 11) fits one row; a word of any usual length too
        assertEquals(11, ForgotCopy.rowSize(11))
        assertEquals(12, ForgotCopy.rowSize(12))
        // past one row the rows are as even as they can be
        assertEquals(7, ForgotCopy.rowSize(13))
        assertEquals(12, ForgotCopy.rowSize(24))
        for (len in 1..24) {
            val per = ForgotCopy.rowSize(len)
            val rows = (len + per - 1) / per
            assertTrue("$len: $per", per <= ForgotCopy.CODE_ROW)
            val last = len - (rows - 1) * per
            assertTrue("$len gives a last row of $last", rows == 1 || last >= per - 1)
        }
    }

    @Test
    fun theLimitIsSaidTheWayTheCodeScreenShowsIt() {
        // 8 Kasım 2026 21:00 UTC
        val at = java.time.ZonedDateTime.of(2026, 11, 8, 21, 0, 0, 0, java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        assertEquals("Sadece 100 kişi · Son: 8 Kasım 21:00", ForgotCopy.limitText(100, at, zone = java.time.ZoneOffset.UTC))
        assertEquals("Sadece 1 kişi", ForgotCopy.limitText(1, 0L, zone = java.time.ZoneOffset.UTC))
        assertEquals("Son: 8 Kasım 21:00", ForgotCopy.limitText(0, at, zone = java.time.ZoneOffset.UTC))
        assertEquals("", ForgotCopy.limitText(0, 0L))
        // the device's own time zone moves the hour, not the day count
        assertEquals("Son: 9 Kasım 00:00", ForgotCopy.limitText(0, at, zone = java.time.ZoneOffset.ofHours(3)))
    }

    @Test
    fun theCountdownIsSpokenAndLastsAsLongAsTheQuestion() {
        val short = ForgotTimeline.build(script()) { _, _ -> 200L }.first { it.phase == ForgotPhase.Countdown }
        assertEquals("soru", short.line?.text)
        assertEquals(ForgotTimeline.COUNTDOWN_MS, short.durationMs)
        val long = ForgotTimeline.build(script()) { _, l -> if (l.text == "soru") 3_000L else 200L }.first { it.phase == ForgotPhase.Countdown }
        assertTrue(long.durationMs >= 3_000L)
    }

    @Test
    fun theStockVideoWithItsEndCardRunsUnderTwentyTwoSeconds() {
        val stock = script(guesses = ForgotCopy.DEFAULT_GUESSES).copy(
            hook = line(ForgotCopy.HOOK), guesses = ForgotCopy.placeholderGuesses().take(ForgotCopy.DEFAULT_GUESSES),
            askLine = line(ForgotCopy.ASK), codeLine = line(ForgotCopy.CODE_WORD), outro = line(ForgotCopy.OUTRO)
        )
        // voices at the pace the preview assumes, and the how-to video that plays in the end card (6.3 s)
        val events = ForgotTimeline.build(stock, howtoMs = 6_300L) { _, l -> StudioCopy.estimateMs(l.text) }
        val total = ForgotTimeline.totalMs(events)
        // the closing introduces the game now, so it is a few seconds longer than the old one-line closing
        assertTrue("took $total ms", total <= 22_000L)
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
             "plan":"Görsel 1'de kemerli bir şey var, mantar sanır.",
             "hook":{"emotion":"shocked","line":"Çizen şemsiye dedi"},
             "ask":{"emotion":"suspicious","line":"Sen ne görüyorsun?"},
             "code_line":{"emotion":"point","line":"Şemsiye mi dedin? Kod sende!"},
             "outro":{"emotion":"wave","line":"Yarın görüşürüz, karalak.com"},
             "caption":"Sen bilebilir misin?","hashtags":"#karalak #çizim"}```"""
        val w = ForgotCopy.parse(text, "şemsiye")
        assertNotNull(w)
        assertEquals(listOf("Mantar mı bu?", "Denizanası!"), w!!.guesses.map { it.text })
        assertEquals(MascotEmotion.Thinking, w.guesses.first().emotion)
        // the hook said the word: dropped, the stock one stays
        assertNull(w.hook)
        assertEquals("Sen ne görüyorsun?", w.ask?.text)
        // the code line said the word too: dropped as well
        assertNull(w.codeLine)
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

    @Test
    fun theThreeWordingsTakeTurns() {
        assertEquals(listOf(ForgotStyle.Game, ForgotStyle.Warm, ForgotStyle.Deal, ForgotStyle.Game), (0..3).map { ForgotStyle.forTurn(it) })
        assertEquals(ForgotStyle.Deal, ForgotStyle.forTurn(-1))
    }

    @Test
    fun theLimitFollowsTheStyle() {
        val at = java.time.ZonedDateTime.of(2026, 11, 8, 21, 0, 0, 0, java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        val utc = java.time.ZoneOffset.UTC
        assertEquals("100 kişi açabilir · Son: 8 Kasım 21:00", ForgotCopy.limitText(100, at, ForgotStyle.Warm, utc))
        assertEquals("Son fırsat · 8 Kasım 21:00 · İlk 100 kişi", ForgotCopy.limitText(100, at, ForgotStyle.Deal, utc))
        assertEquals("Son fırsat · 8 Kasım 21:00", ForgotCopy.limitText(0, at, ForgotStyle.Deal, utc))
    }

    @Test
    fun everyStyleHasItsOwnWordsAndNoneNamesAPrizeKind() {
        assertEquals(3, ForgotStyle.entries.map { it.titleWord + it.explainWord }.toSet().size)
        // the code may give gold, XP or a chest: no wording may assume which
        ForgotStyle.entries.forEach { st ->
            val all = listOfNotNull(st.titleWord, st.titleShown, st.explainWord, st.explainShown, st.steps.joinToString(" "), st.hintWord).joinToString(" ").lowercase(java.util.Locale("tr"))
            assertFalse(st.title, all.contains("kasa"))
        }
        assertTrue(ForgotStyle.Warm.tone in ForgotCopy.directorPrompt("kedi", listOf(1f), "m", ForgotMode.Word, "", "", ForgotStyle.Warm))
    }

    @Test
    fun theLimitComesAsTwoChipsInTheStylesOrder() {
        val at = java.time.ZonedDateTime.of(2026, 11, 8, 21, 0, 0, 0, java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        val utc = java.time.ZoneOffset.UTC
        val game = ForgotCopy.limitChips(100, at, ForgotStyle.Game, utc)
        assertEquals(listOf(true, false), game.map { it.isWho })
        assertEquals(listOf("Sadece 100 kişi", "Son: 8 Kasım 21:00"), game.map { it.text })
        // the deal wording leads with the deadline
        val deal = ForgotCopy.limitChips(100, at, ForgotStyle.Deal, utc)
        assertEquals(listOf(false, true), deal.map { it.isWho })
        // one chip when only one thing is set, none when neither
        assertEquals(1, ForgotCopy.limitChips(5, 0L, ForgotStyle.Warm, utc).size)
        assertTrue(ForgotCopy.limitChips(0, 0L, ForgotStyle.Warm, utc).isEmpty())
    }

    @Test
    fun everyStyleNumbersThreeStepsForTypingTheCode() {
        ForgotStyle.entries.forEach { assertEquals(it.title, 3, it.steps.size) }
        assertEquals(listOf("Ayarlar", "Kupon kodu", "Kullan"), ForgotStyle.Game.steps)
    }

    @Test
    fun theFirstLetterHintIsOffUntilAskedFor() {
        assertFalse(script(mode = ForgotMode.Word).showFirstLetter)
        assertTrue(script(mode = ForgotMode.Word).copy(showFirstLetter = true).showFirstLetter)
    }

    @Test
    fun hooksTakeTurnsBetweenTheTwoKinds() {
        val kinds = (0..3).map { ForgotCopy.HookKind.forTurn(it) }
        assertEquals(listOf(ForgotCopy.HookKind.Challenge, ForgotCopy.HookKind.Crowd, ForgotCopy.HookKind.Challenge, ForgotCopy.HookKind.Crowd), kinds)
        assertEquals(ForgotCopy.HookKind.Crowd, ForgotCopy.HookKind.forTurn(-1))
    }

    @Test
    fun theHookPromptNamesTheKindAvoidsEarlierOnesAndInventsNoNumbers() {
        val p = ForgotCopy.hookPrompt("kedi", ForgotCopy.HookKind.Crowd, "happy", listOf("Bunu görenlerin çoğu yanlış bildi."))
        assertTrue(p.contains("Çoğu yanlış bildi"))
        assertTrue(p.contains("FARKLI"))
        assertTrue(p.contains("Bunu görenlerin çoğu yanlış bildi."))
        assertTrue(p.contains("sayı"))
        assertFalse(ForgotCopy.hookPrompt("kedi", ForgotCopy.HookKind.Challenge, "happy", emptyList()).contains("FARKLI"))
    }

    @Test
    fun theDirectorPromptCarriesTheHookRules() {
        val p = ForgotCopy.directorPrompt("kedi", listOf(0.4f, 0.7f), "happy", ForgotMode.Word, "100 altın", "", ForgotStyle.Game, ForgotCopy.HookKind.Challenge)
        assertTrue(p.contains("KANCA KURALLARI"))
        assertTrue(p.contains("Meydan okuma"))
    }

    @Test
    fun aRewrittenHookThatSaysTheWordIsRefused() {
        val ok = """{"hook":{"emotion":"happy","line":"Bunu bilen çıkmadı. İlk sen ol."}}"""
        assertEquals("Bunu bilen çıkmadı. İlk sen ol.", ForgotCopy.parseHook(ok, "kedi")?.text)
        assertNull(ForgotCopy.parseHook("""{"hook":{"emotion":"happy","line":"Bu bir kedi!"}}""", "kedi"))
        assertNull(ForgotCopy.parseHook("not json", "kedi"))
    }

    @Test
    fun theClosingSendsPeopleToTheMobileAppNotTheWebsite() {
        assertFalse(ForgotCopy.OUTRO.contains("karalak.com"))
        assertTrue(ForgotCopy.OUTRO.contains("Karalak'ı indir"))
        assertTrue(com.sualtikasifi.cizimhafiza.presentation.reports.studio.LineRules.words(ForgotCopy.OUTRO) <= ForgotCopy.OUTRO_WORDS)
        val p = ForgotCopy.directorPrompt("kedi", listOf(0.4f), "happy", ForgotMode.Word, "100 altın", "")
        assertTrue(p.contains("OYUNU TANITIYOR"))
        assertTrue(p.contains("SESLENDİRME İLKELERİ"))
        assertFalse(p.contains("\"karalak.com\" geçsin"))
    }

    @Test
    fun theLetterLinesKnowTheirLettersAndWhichOnesAreWrittenOn() {
        val sc = script(mode = ForgotMode.Word).copy(word = "ışık lamba")
        // spaces are not lines; the Turkish dotless ı stays dotless in capitals
        assertEquals("IŞIKLAMBA".toList(), sc.slotLetters)
        assertTrue(sc.revealed.isEmpty())
        assertEquals(setOf(0, 3), sc.copy(revealed = setOf(0, 3)).revealed)
    }

    @Test
    fun theClosingIntroducesTheGameAndTakesTurnsBetweenAngles() {
        assertFalse(ForgotCopy.OUTRO.contains("Yarın"))
        val prompts = ForgotCopy.OutroAngle.entries.map {
            ForgotCopy.directorPrompt("kedi", listOf(0.4f), "happy", ForgotMode.Word, "100 altın", "", ForgotStyle.Game, ForgotCopy.HookKind.Challenge, it)
        }
        assertEquals(ForgotCopy.OutroAngle.entries.size, prompts.distinct().size)
        prompts.forEach {
            assertTrue(it.contains("OYUNU TANITIYOR"))
            assertTrue(it.contains("\"Yarın\""))
            assertFalse(it.contains("yarın daha zorunun"))
        }
        assertEquals(ForgotCopy.OutroAngle.forTurn(0), ForgotCopy.OutroAngle.forTurn(ForgotCopy.OutroAngle.entries.size))
    }
}
