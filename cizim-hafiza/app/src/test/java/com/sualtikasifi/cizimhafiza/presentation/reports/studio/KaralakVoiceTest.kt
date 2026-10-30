package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import com.sualtikasifi.cizimhafiza.presentation.reports.studio.audio.ElevenLabs
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.audio.Karaoke
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.audio.SpeechTiming
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.audio.VoiceModel
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.guess.GuessCopy
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.guess.SpokenLine
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MascotEmotion
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KaralakVoiceTest {

    @Test
    fun turkishPromptsAreLeftAsTheyWereAndOtherLanguagesGetANote() {
        assertEquals("", KaralakVoice.languageNote(StudioLang.Tr))
        assertEquals("", KaralakVoice.promptNote(StudioLang.Tr))
        assertTrue(KaralakVoice.languageNote(StudioLang.En).contains("İngilizce"))
        assertTrue(KaralakVoice.persona("İŞ", StudioLang.Es).contains("İspanyolca"))
        assertEquals("İŞ", KaralakVoice.persona("İŞ").lines().last())
        assertEquals(StudioLang.Pt, StudioLang.of("pt"))
        assertEquals(StudioLang.Tr, StudioLang.of("xx"))
    }

    @Test
    fun aLineIsKeptWithinItsLimitOrCutAtASentence() {
        assertEquals("Merhaba dünya", LineRules.fit("Merhaba dünya", 8))
        // a limit is a target: two words over still pass
        assertFalse(LineRules.tooLong("a b c d e f g h i j", 8))
        assertTrue(LineRules.tooLong("a b c d e f g h i j k", 8))
        assertEquals("Hadi bakalım çok kolay.", LineRules.fit("Hadi bakalım çok kolay. Şimdi ise zor kısım başlıyor ve uzuyor", 8))
        assertEquals("a b c d e f", LineRules.fit("a b c d e f g h i j k l m n", 6))
    }

    @Test
    fun webAddressesAndPercentsAreSpelledOutForTheVoiceOnly() {
        assertEquals("karalak nokta com'da çiz!", Pronunciation.forSpeech("karalak.com'da çiz!"))
        assertEquals("Draw at karalak dot com", Pronunciation.forSpeech("Draw at karalak.com", StudioLang.En))
        assertEquals("yüzde 40 ve yüzde 30", Pronunciation.forSpeech("%40 ve 30%"))
        assertEquals("Merhaba, 4 saniyede!", Pronunciation.forSpeech("Merhaba, 4 saniyede!"))
        val line = SpokenLine("karalak.com'da çiz", MascotEmotion.Neutral)
        assertTrue(ElevenLabs.textFor(line, VoiceModel.V2, tags = false).startsWith("karalak nokta com'da çiz"))
    }

    @Test
    fun aVoiceInAnotherLanguageIsAnotherFileButTurkishKeepsItsOldName() {
        val tr = ElevenLabs.cacheName("Merhaba", "v1", VoiceModel.V3, 0.5)
        assertEquals(tr, ElevenLabs.cacheName("Merhaba", "v1", VoiceModel.V3, 0.5, StudioLang.Tr))
        assertNotEquals(tr, ElevenLabs.cacheName("Merhaba", "v1", VoiceModel.V3, 0.5, StudioLang.En))
    }

    @Test
    fun wordTimingsSkipTheMoodTagAndTheTrailingPause() {
        val text = "[excited] Bu kedi mi? ..."
        val chars = text.map { it.toString() }
        val timing = SpeechTiming.fromAlignment(chars, chars.indices.map { it * 0.1 }, chars.indices.map { it * 0.1 + 0.1 })!!
        assertEquals(listOf("Bu", "kedi", "mi?"), timing.words.map { it.text })
        assertEquals(1000L, timing.words[0].startMs)
        assertEquals(listOf(1000L, 1300L, 1800L), timing.startsFor("Bu kedi mi?")!!.toList())
        // a subtitle with another number of words than the voice said: spread by length, in order
        val spread = timing.startsFor("Bu kedi mi acaba")!!
        assertEquals(4, spread.size)
        assertEquals(1000L, spread[0])
        assertTrue(spread.toList().zipWithNext().all { (a, b) -> a <= b })
        // kept beside the voice file and read back
        val back = SpeechTiming.fromJson(timing.toJson())!!
        assertEquals(timing.words.map { Triple(it.text, it.startMs, it.endMs) }, back.words.map { Triple(it.text, it.startMs, it.endMs) })
        assertNull(SpeechTiming.fromJson("nope"))
    }

    @Test
    fun karaokeLightsAWordAsItIsSaid() {
        val line = "Bu kedi mi?"
        val starts = longArrayOf(1000, 1300, 1800)
        assertEquals(0, Karaoke.splitAt(line, starts, 500))
        assertEquals(2, Karaoke.splitAt(line, starts, 1000))
        assertEquals(7, Karaoke.splitAt(line, starts, 1400))
        assertEquals(line.length, Karaoke.splitAt(line, starts, 5000))
        // a hair early, so the eye is not behind the ear
        assertEquals(2, Karaoke.splitAt(line, starts, 950))
    }

    private class Busy(override val code: Int, override val retryAfterMs: Long? = null) : Exception("busy $code"), Backoff.Coded

    @Test
    fun aBusyServiceIsWaitedOutButARefusalIsNot() = runBlocking {
        var calls = 0
        val ok = Backoff.retrying<String> { calls++; if (calls < 3) Result.failure(Busy(429, 250)) else Result.success("ok") }
        assertEquals("ok", ok.getOrNull())
        assertEquals(3, calls)

        var refused = 0
        assertTrue(Backoff.retrying<String> { refused++; Result.failure(Busy(401)) }.isFailure)
        assertEquals(1, refused)

        var down = 0
        assertTrue(Backoff.retrying<String> { down++; Result.failure(Busy(503, 250)) }.isFailure)
        assertEquals(Backoff.ATTEMPTS, down)
        assertEquals(3000L, Backoff.waitMs(1, Busy(429, 3000)))
        assertEquals(3000L, Backoff.waitMs(2, Busy(500)))
    }

    @Test
    fun otherOpeningsAndClosingsAreReadWithoutRepeats() {
        val frame = GuessCopy.parseFrame(
            """{"intro":{"emotion":"confident","line":"Merhaba ben Karalak"},"reactions":[{"emotion":"excited","line":"Bildim!"}],
               "outro":{"emotion":"wave","line":"Gel çiz"},
               "intro_alts":["Selam!","Merhaba ben Karalak","Hazır mısın?",{"emotion":"point","line":"Bana meydan oku"}],
               "outro_alts":["Yarın görüşürüz","x"],"caption":"c","hashtags":"#a"}""", 1
        )!!
        assertEquals(listOf("Selam!", "Hazır mısın?", "Bana meydan oku"), frame.introAlts.map { it.text })
        assertEquals(frame.intro.emotion, frame.introAlts[0].emotion)
        assertEquals(MascotEmotion.of("point"), frame.introAlts[2].emotion)
        assertEquals(2, frame.outroAlts.size)
        val bare = GuessCopy.parseFrame("""{"intro":{"line":"A"},"reactions":[{"line":"B"}],"outro":{"line":"C"}}""", 1)
        assertNotNull(bare)
        assertTrue(bare!!.introAlts.isEmpty())
    }

    @Test
    fun longLinesAreFoundAndCut() {
        val long = (1..16).joinToString(" ") { "kelime$it" }
        val frame = GuessCopy.parseFrame("""{"intro":{"line":"$long"},"reactions":[{"line":"B"}],"outro":{"line":"C"}}""", 1)!!
        assertEquals(1, GuessCopy.longFrame(frame).size)
        assertEquals(LineRules.INTRO_WORDS, LineRules.words(GuessCopy.fitFrame(frame).intro.text))
        assertEquals(LineRules.STAGE_WORDS, LineRules.words(GuessCopy.fitLooks(listOf(SpokenLine(long, MascotEmotion.Thinking)))[0].text))
    }
}
