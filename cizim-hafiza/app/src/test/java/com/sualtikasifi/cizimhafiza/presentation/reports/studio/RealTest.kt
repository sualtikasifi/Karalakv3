package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import com.sualtikasifi.cizimhafiza.domain.model.DrawingPoint
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealCopy
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealDrawing
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealLine
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealMix
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.VoicedLine
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealPhase
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealProject
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealReveal
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealTimeline
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RealTest {

    private fun drawing(id: String, word: String = "kedi") = RealDrawing(id, listOf(listOf(DrawingPoint(1f, 2f), DrawingPoint(3f, 4f))), word, 7, "Zeynep", 12)

    @Test
    fun onlyRevealsWithAClipAndTheirDrawingGoIntoTheVideo() {
        val p = RealProject(
            drawings = listOf(drawing("a"), drawing("b"), drawing("c")),
            reveals = listOf(
                RealReveal("a", image = "a.jpg"),
                RealReveal("b", image = "b.jpg", clip = "b.mp4", clipMs = 8_000),
                RealReveal("z", clip = "z.mp4", clipMs = 8_000),
                RealReveal("c", clip = "c.mp4", clipMs = 8_000)
            )
        )
        // a picture alone is for the swipe-through post; a clip of a drawing no longer in the project is dropped
        assertEquals(listOf("b", "c"), p.shown.map { it.sampleId })
    }

    @Test
    fun timelineIsTheClipsBackToBackWithPlainCuts() {
        val shown = listOf(RealReveal("a", clip = "a.mp4", clipMs = 8_000), RealReveal("b", clip = "b.mp4", clipMs = 30_000), RealReveal("c", clip = "c.mp4", clipMs = 200))
        val events = RealTimeline.build(shown)
        assertEquals(listOf(RealPhase.Clip, RealPhase.Clip, RealPhase.Clip), events.map { it.phase })
        events.zipWithNext().forEach { (x, y) -> assertEquals(x.endMs, y.startMs) }
        // each clip plays from its own start for its own length: a long one is cut, a tiny one gets a second
        assertEquals(8_000L, events[0].durationMs)
        assertEquals(RealTimeline.CLIP_MAX, events[1].durationMs)
        assertEquals(1_000L, events[2].durationMs)
        events.forEach { assertEquals(it.startMs, it.clipAtMs); assertEquals(it.durationMs, it.clipMs) }
        assertEquals(0L, events.first().startMs)
        assertEquals(events.last().endMs, RealTimeline.totalMs(events))
        assertEquals(events.last(), RealTimeline.at(events, 10_000_000L))
    }

    @Test
    fun noClipsMeansNoVideo() {
        assertTrue(RealTimeline.build(emptyList()).isEmpty())
        assertTrue(RealTimeline.build(emptyList(), hookMs = 3_000).isEmpty())
        assertEquals(0L, RealTimeline.totalMs(emptyList()))
    }

    @Test
    fun theHookOpensTheVideoOnTheFirstClipsDrawingAndTheClipsFollowIt() {
        val shown = listOf(RealReveal("a", clip = "a.mp4", clipMs = 8_000), RealReveal("b", clip = "b.mp4", clipMs = 6_000))
        val events = RealTimeline.build(shown, hookMs = 2_800)
        assertEquals(listOf(RealPhase.Hook, RealPhase.Clip, RealPhase.Clip), events.map { it.phase })
        assertEquals(0L, events[0].startMs)
        assertEquals(2_800L, events[0].durationMs)
        assertEquals(0, events[0].index)
        events.zipWithNext().forEach { (x, y) -> assertEquals(x.endMs, y.startMs) }
        // each clip's sound is laid where the clip now starts
        assertEquals(listOf(2_800L, 10_800L), events.drop(1).map { it.clipAtMs })
        assertEquals(16_800L, RealTimeline.totalMs(events))
        assertEquals(RealPhase.Hook, RealTimeline.at(events, 2_799L).phase)
        assertEquals(RealPhase.Clip, RealTimeline.at(events, 2_800L).phase)
    }

    @Test
    fun theOpeningIsAlwaysThereAndLastsAsLongAsTheVoicedHook() {
        // the hook is never written on screen: silent, the opening is a fixed beat for the drawing
        assertEquals(RealTimeline.HOOK_SILENT, RealTimeline.hookMs())
        assertEquals(RealTimeline.HOOK_SILENT, RealTimeline.hookMs(voiceMs = 0))
        // the drawing gets time to be drawn and seen before the clip opens out of it
        assertTrue(RealTimeline.HOOK_MIN > RealTimeline.HOOK_REVEAL + RealTimeline.HOOK_HOLD + 1_000L)
        // a short voice still leaves the drawing its time
        assertEquals(RealTimeline.HOOK_MIN, RealTimeline.hookMs(voiceMs = 500))
        // a longer one keeps the opening on until it has finished, and a beat after
        assertTrue(RealTimeline.hookMs(voiceMs = 3_400) >= RealTimeline.VOICE_AT + 3_400)
        // however long it runs, the opening never drags on
        assertEquals(RealTimeline.HOOK_VOICE_MAX, RealTimeline.hookMs(voiceMs = 20_000))
    }

    @Test
    fun theClipsOwnSoundComesInSoftlyAfterTheHook() {
        assertEquals(0f, RealMix.clipGainAt(0), 0f)
        assertEquals(0.5f, RealMix.clipGainAt(RealMix.CLIP_FADE_IN / 2), 0.01f)
        assertEquals(1f, RealMix.clipGainAt(RealMix.CLIP_FADE_IN), 0f)
        assertEquals(1f, RealMix.clipGainAt(5_000), 0f)
        // and the hook's voice has room before it: the opening outlasts the voice by more than the clip's fade-in
        val voice = 2_500L
        assertTrue(RealTimeline.hookMs(voice) - (RealTimeline.VOICE_AT + voice) >= 300L)
    }

    @Test
    fun theMusicStepsBackUnderTheVoiceAndTheClipAndFadesOut() {
        val total = 13_000L
        val voices = listOf(150L..2_650L, 11_150L..12_400L)
        val clip = 2_800L until 10_800L
        fun g(ms: Long) = RealMix.gainAt(ms, total, voices, clip)
        // a quick fade in, then under the hook's voice
        assertEquals(0f, g(0), 0.001f)
        assertEquals(RealMix.UNDER_VOICE, g(1_000), 0.001f)
        // between the voice and the clip it comes back up, then sits a little lower under the clip
        assertTrue(g(2_780) > RealMix.UNDER_VOICE)
        assertEquals(RealMix.UNDER_CLIP, g(6_000), 0.001f)
        // under the closing's voice again, and out by the very end
        assertEquals(RealMix.UNDER_VOICE, g(11_800), 0.001f)
        assertEquals(0f, g(total), 0.001f)
        // with nobody speaking at the end, it fades out over the last moments
        val quiet = { ms: Long -> RealMix.gainAt(ms, total, emptyList(), clip) }
        assertEquals(1f, quiet(total - RealMix.FADE_OUT - 10), 0.001f)
        assertEquals(0.5f, quiet(total - RealMix.FADE_OUT / 2), 0.01f)
        // never louder than chosen
        (0L..total step 50).forEach { assertTrue(g(it) in 0f..1f) }
    }

    @Test
    fun aLinesVoiceBelongsToItsWordsAndIsKeptWithTheProject() {
        val hook = VoicedLine("7 saniyede kedi çizdi!", listOf("7 saniyede kedi çizdi!", "Bu bir yetenek!"), "hook_voice_1.mp3", 3_000)
        val outro = VoicedLine("Bunu kim sever?", voice = "outro_voice_1.mp3", voiceMs = 1_800)
        val r = RealReveal("a", clip = "a.mp4", clipMs = 8_000, hook = hook, outro = outro)
        val p = RealProject(drawings = listOf(drawing("a")), reveals = listOf(r))
        assertEquals(setOf("a.mp4", "hook_voice_1.mp3", "outro_voice_1.mp3"), p.media)
        assertEquals(RealTimeline.hookMs(3_000), r.hookMs)
        assertEquals(RealTimeline.outroMs(outro.text, 1_800), r.outroMs)
        // the same words keep their voice; other words, or none, let it go
        assertEquals(hook, hook.withText("7 saniyede kedi çizdi!"))
        val other = hook.withText("Bu bir yetenek!")
        assertNull(other.voice)
        assertEquals(0L, other.voiceMs)
        assertEquals(hook.options, other.options)
        // its words changed, the hook is silent again and the opening goes back to its plain beat
        assertEquals(RealTimeline.HOOK_SILENT, r.copy(hook = other).hookMs)
        assertEquals(RealTimeline.HOOK_SILENT, r.copy(hook = hook.withText("")).hookMs)
        assertEquals(0L, r.copy(outro = outro.withText("")).outroMs)
        // each line is reached by its kind
        assertEquals(hook, r.line(RealLine.Hook))
        assertEquals(VoicedLine("x"), r.withLine(RealLine.Outro, VoicedLine("x")).outro)
        assertEquals(hook, r.withLine(RealLine.Outro, VoicedLine("x")).hook)
    }

    @Test
    fun theClosingIsShortAndLastsUntilItHasBeenSaid() {
        assertEquals(0L, RealTimeline.outroMs(""))
        assertEquals(RealTimeline.OUTRO_MIN, RealTimeline.outroMs("Sen?"))
        assertEquals(RealTimeline.OUTRO_MAX, RealTimeline.outroMs("x".repeat(200)))
        assertTrue(RealTimeline.outroMs("Bunu kim sever?", voiceMs = 2_400) >= RealTimeline.VOICE_AT + 2_400)
        assertEquals(RealTimeline.OUTRO_VOICE_MAX, RealTimeline.outroMs("Bunu kim sever?", voiceMs = 20_000))
    }

    @Test
    fun theVideoIsOneDrawingsHookClipAndClosing() {
        val r = RealReveal("a", clip = "a.mp4", clipMs = 8_000, hook = VoicedLine("4 saniyede fırın?"), outro = VoicedLine("Kim alır bunu?"))
        val events = RealTimeline.build(r)
        assertEquals(listOf(RealPhase.Hook, RealPhase.Clip, RealPhase.Outro), events.map { it.phase })
        events.zipWithNext().forEach { (x, y) -> assertEquals(x.endMs, y.startMs) }
        val clip = events[1]
        val outro = events[2]
        // the closing comes in over the clip's last seconds and ends with it: the clip is never stopped for it
        assertEquals(clip.clipMs, outro.clipMs)
        assertEquals(clip.clipAtMs, outro.clipAtMs)
        assertEquals(8_000L, clip.clipMs)
        assertEquals(clip.clipAtMs + clip.clipMs, outro.endMs)
        assertEquals(r.outroMs, outro.durationMs)
        assertEquals(r.hookMs + 8_000, RealTimeline.totalMs(events))
        assertEquals(RealTimeline.totalMs(events), RealTimeline.mainMs(events))
        // inside the 8–20 seconds the video aims at
        assertTrue(RealTimeline.totalMs(events) in 8_000L..20_000L)
        // with neither line it still opens on the game's screen, and ends with the clip
        assertEquals(listOf(RealPhase.Hook, RealPhase.Clip), RealTimeline.build(r.copy(hook = VoicedLine(), outro = VoicedLine())).map { it.phase })
        assertTrue(RealTimeline.build(null).isEmpty())
    }

    @Test
    fun aClosingTooLongForTheClipLetsItPlayFirstThenHoldsItsLastFrame() {
        val voiced = VoicedLine("Bunu kim sürer ki, gerçekten?", voice = "o.mp3", voiceMs = 4_500)
        // a 3-second clip: it plays its first two seconds alone, the closing goes on past its end on the last frame
        val r = RealReveal("a", clip = "a.mp4", clipMs = 3_000, outro = voiced)
        val events = RealTimeline.build(r)
        val clip = events.first { it.phase == RealPhase.Clip }
        val outro = events.first { it.phase == RealPhase.Outro }
        assertEquals(RealTimeline.OUTRO_LEAD, clip.durationMs)
        assertEquals(clip.startMs + RealTimeline.OUTRO_LEAD, outro.startMs)
        assertTrue(outro.endMs > clip.clipAtMs + clip.clipMs)
        assertEquals(outro.endMs, RealTimeline.totalMs(events))
        // a clip shorter than the lead plays whole before the closing
        val tiny = RealTimeline.build(r.copy(clipMs = 1_000)).first { it.phase == RealPhase.Clip }
        assertEquals(1_000L, tiny.durationMs)
    }

    @Test
    fun thePromoClosesTheVideoAfterTheMusicHasGone() {
        val r = RealReveal("a", clip = "a.mp4", clipMs = 8_000, outro = VoicedLine("Kim alır bunu?"))
        val events = RealTimeline.build(r, promoMs = 6_000)
        assertEquals(listOf(RealPhase.Hook, RealPhase.Clip, RealPhase.Outro, RealPhase.Promo), events.map { it.phase })
        events.zipWithNext().forEach { (x, y) -> assertEquals(x.endMs, y.startMs) }
        val main = RealTimeline.mainMs(events)
        assertEquals(main, events.last().startMs)
        assertEquals(main + 6_000, RealTimeline.totalMs(events))
        // a long promo is cut
        assertEquals(RealTimeline.PROMO_MAX, RealTimeline.build(r, promoMs = 60_000).last().durationMs)
        // no clip, no video: the promo alone is not one
        assertTrue(RealTimeline.build(emptyList(), promoMs = 6_000).isEmpty())
    }

    @Test
    fun theClipsOwnSoundStepsBackUnderTheClosing() {
        val clipAt = 3_000L
        val voices = listOf(8_500L..10_500L)
        assertEquals(1f, RealMix.clipGainAt(5_000, clipAt, voices), 0.001f)
        assertEquals(RealMix.CLIP_UNDER_VOICE, RealMix.clipGainAt(9_000, clipAt, voices), 0.001f)
        // and still comes in softly at its start
        assertEquals(0f, RealMix.clipGainAt(clipAt, clipAt, voices), 0.001f)
    }

    @Test
    fun theVideoIsMadeOfTheFirstDrawingWithAClip() {
        val p = RealProject(
            drawings = listOf(drawing("a"), drawing("b"), drawing("c")),
            reveals = listOf(RealReveal("a", image = "a.jpg"), RealReveal("b", clip = "b.mp4", clipMs = 8_000), RealReveal("c", clip = "c.mp4", clipMs = 8_000))
        )
        assertEquals("b", p.featured?.sampleId)
        assertEquals("b", p.opening?.sampleId)
        // before any clip is in, the lines are written for the first chosen one
        val none = p.copy(reveals = p.reveals.map { it.copy(clip = null) })
        assertNull(none.featured)
        assertEquals("a", none.opening?.sampleId)
        assertNull(RealProject().opening)
    }

    @Test
    fun closingsAreCleanedLikeHooksAndKeptToFive() {
        val text = """{"outros":["“Bunu kim alır?”","1. 9 saniyede mi?","Ya sen?","ya sen?","a","b","c","d"]}"""
        val outros = RealCopy.parseOutros(text, 4)
        assertEquals(RealCopy.OUTRO_COUNT, outros.size)
        assertEquals(listOf("Bunu kim alır?", "4 saniyede mi?", "Ya sen?", "a", "b"), outros)
        assertTrue(RealCopy.parseOutros("{\"hooks\":[\"x\"]}", 4).isEmpty())
        assertEquals(com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MascotEmotion.Point, RealCopy.spoken(RealLine.Outro, "x").emotion)
        assertEquals(RealCopy.hookLine("x"), RealCopy.spoken(RealLine.Hook, "x"))
    }

    @Test
    fun karalakSaysTheHookEagerly() {
        val line = RealCopy.hookLine("  5 saniyede elbise çizdi!  ")
        assertEquals("5 saniyede elbise çizdi!", line.text)
        assertEquals(com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MascotEmotion.Excited, line.emotion)
    }

    @Test
    fun hooksAreCleanedTheirSecondsMadeTrueAndKeptToTen() {
        val list = (1..14).joinToString(",") { "\"Kanca numara $it\"" }
        val text = """
            ```json
            {"hooks":["“3 saniyede mikrodalga fırın çizebilir misin?”","1. 10 sn'de elbise çizdi!","Bu gerçekten bir yetenek!","bu gerçekten bir YETENEK!","  ",$list]}
            ```
        """.trimIndent()
        val hooks = RealCopy.parseHooks(text, 4)
        assertEquals(RealCopy.HOOK_COUNT, hooks.size)
        assertEquals("4 saniyede mikrodalga fırın çizebilir misin?", hooks[0])
        assertEquals("4 sn'de elbise çizdi!", hooks[1])
        assertEquals("Bu gerçekten bir yetenek!", hooks[2])
        // the same line twice is offered once
        assertEquals("Kanca numara 1", hooks[3])
        // with the seconds unknown, what was written stays
        assertEquals("3 saniyede çizdi!", RealCopy.cleanHook("3 saniyede çizdi!", 0))
        assertTrue(RealCopy.cleanHook("x".repeat(300), 4).length <= RealCopy.HOOK_CHARS)
        assertTrue(RealCopy.parseHooks("bilmiyorum", 4).isEmpty())
        assertTrue(RealCopy.parseHooks("{\"hooks\":[]}", 4).isEmpty())
    }

    @Test
    fun aPlayerWhoDidNotAllowTheirNameStaysAnonymous() {
        assertTrue(drawing("a").named)
        assertFalse(RealDrawing("b", emptyList(), "kedi", 5, "", 12, "").named)
    }

    @Test
    fun parsesTheAssistantsDescriptionsKeepingOnlyKnownDrawings() {
        val text = """
            ```json
            {"reveals":[{"index":2,"word_en":"cat","describe":"a cat with six legs","motion":"a man picks it up"},{"index":9,"word_en":"x"},{"index":1,"word_en":"rocket","describe":"a tall box","motion":"flames flicker"}],
             "caption":"Hangisi?","hashtags":"#karalak"}
            ```
        """.trimIndent()
        val p = RealCopy.parsePicks(text, 3)
        assertNotNull(p)
        p!!
        assertEquals(listOf(1, 0), p.reveals.map { it.index })
        assertEquals("cat", p.reveals[0].wordEn)
        assertEquals("a cat with six legs", p.reveals[0].describe)
        assertEquals("a man picks it up", p.reveals[0].motion)
        assertEquals("Hangisi?", p.caption)
        assertNull(RealCopy.parsePicks("{\"reveals\":[]}", 3))
        assertNull(RealCopy.parsePicks("bilmiyorum", 3))
    }

    @Test
    fun imagePromptIsTurkishAndCarriesTheWordAndTheOddities() {
        val prompt = RealCopy.imagePrompt("kedi", 5, "altı bacaklı bir kedi.")
        assertTrue(prompt.contains("\"kedi\" kelimesi için 5 saniyede"))
        assertTrue(prompt.contains("Karalamanın şekli: altı bacaklı bir kedi.\n"))
        // the drawing's shape wins over what a cat should look like
        assertTrue(prompt.contains("normal bir \"kedi\" hâline GETİRME"))
        assertTrue(prompt.contains("9:16"))
        // no English is left in it, so it can be corrected by hand
        assertFalse(prompt.contains("The attached image"))
        val noDescribe = RealCopy.imagePrompt("kedi", 5, "")
        assertFalse(noDescribe.contains("Karalamanın şekli"))
    }

    @Test
    fun videoPromptKeepsTheObjectAsPaintedYetMakesSomethingHappenToIt() {
        val video = RealCopy.videoPrompt("mikrodalga", "Mutfakta bir adam tabağı içeri koyar ve düğmelere basar.")
        assertTrue(video.contains("ilk kare olarak kullan"))
        // the object is never changed ...
        assertTrue(video.contains("TAMAMEN AYNI kalmalı"))
        assertTrue(video.contains("göz, yüz, kol, bacak"))
        // ... but it is not left to sway: a small story with a person, and real sound
        assertTrue(video.contains("Neler oluyor: Mutfakta bir adam tabağı içeri koyar ve düğmelere basar. "))
        assertTrue(video.contains("öylece durup sallanmasın"))
        assertTrue(video.contains("sesi de üret"))
        // with no scene of its own, a person comes and uses the thing
        val generic = RealCopy.videoPrompt("nar", "")
        assertTrue(generic.contains("çekime girer"))
        assertTrue(generic.contains("nar"))
        assertFalse(video.contains("Use the attached photo"))
    }

    @Test
    fun aLongSceneIsKeptWholeInTheVideoPrompt() {
        val scene = (1..12).joinToString(" ") { "Sahne cümlesi $it." }
        assertTrue(RealCopy.videoPrompt("saz", scene).contains(scene.trimEnd('.')))
    }

    @Test
    fun aHandPickedProfilePictureIsKeptWithTheProjectsFiles() {
        val p = RealProject(
            drawings = listOf(drawing("a").copy(photo = "a_pfp.jpg"), drawing("b")),
            reveals = listOf(RealReveal("a", image = "a.jpg", clip = "a.mp4"))
        )
        // so clearing the project, or taking a drawing out, lets go of it with the rest
        assertEquals(setOf("a.jpg", "a.mp4", "a_pfp.jpg"), p.media)
    }

    @Test
    fun aLibraryPieceKnowsItsFilesAndSurvivesARoundTrip() {
        val item = com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.LibraryItem(
            "a", 5L, drawing("a").copy(photo = "p.jpg"), RealReveal("a", image = "a.jpg", clip = "a.mp4", clipMs = 8_000)
        )
        assertEquals(setOf("a.jpg", "a.mp4", "p.jpg"), item.media)
        // a piece with its lines voiced is a whole video: its voices are kept with it
        assertEquals(
            setOf("a.jpg", "a.mp4", "p.jpg", "h.mp3", "o.mp3"),
            item.copy(reveal = item.reveal.copy(hook = VoicedLine("x", voice = "h.mp3"), outro = VoicedLine("y", voice = "o.mp3"))).media
        )
        assertTrue(item.hasClip)
        assertFalse(item.copy(reveal = RealReveal("a", image = "a.jpg")).hasClip)
        val json = Json { ignoreUnknownKeys = true }
        val ser = kotlinx.serialization.builtins.ListSerializer(com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.LibraryItem.serializer())
        assertEquals(listOf(item), json.decodeFromString(ser, json.encodeToString(ser, listOf(item))))
    }

    @Test
    fun cardLayoutNeverLeavesWhatTheSlidersAllow() {
        val wild = com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.CardLayout(9f, -3f, 4f).clamped()
        assertEquals(com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.CardLayout.MAX_SCALE, wild.scale, 0f)
        assertEquals(0f, wild.x, 0f)
        assertEquals(1f, wild.y, 0f)
        assertEquals(com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.CardLayout.MIN_SCALE, com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.CardLayout(0f).clamped().scale, 0f)
        val d = com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.CardLayout()
        assertEquals(d, d.clamped())
    }

    @Test
    fun postTextEndsWithTheGooglePlayLinkOnceAndNeverTheWebsite() {
        val text = RealCopy.withStoreLink("Hangisi?\n\n#karalak")
        assertTrue(text.endsWith(RealCopy.STORE_URL))
        assertTrue(RealCopy.STORE_URL.startsWith("https://play.google.com/store/apps/details?id="))
        assertEquals(text, RealCopy.withStoreLink(text))
        assertFalse(text.contains("karalak.com"))
        assertEquals(RealCopy.STORE_LINE, RealCopy.withStoreLink(""))
    }

    @Test
    fun projectSurvivesARoundTripToDiskAndAnOldOneJustComesBackEmpty() {
        val json = Json { ignoreUnknownKeys = true }
        val project = RealProject(
            drawings = listOf(drawing("a")),
            reveals = listOf(
                RealReveal(
                    "a", "cat", "six legs", "wobble", image = "a.jpg", clip = "a.mp4", clipMs = 8_000,
                    hook = VoicedLine("7 saniyede kedi çizdi!", listOf("7 saniyede kedi çizdi!", "Bu gerçekten bir yetenek!"), "hook_voice_1.mp3", 2_900),
                    outro = VoicedLine("Kim sever?", listOf("Kim sever?"))
                )
            )
        )
        val back = json.decodeFromString(RealProject.serializer(), json.encodeToString(RealProject.serializer(), project))
        assertEquals(project, back)
        assertEquals(setOf("a.jpg", "a.mp4", "hook_voice_1.mp3"), back.media)
        // a project saved before the video was simplified carried a script and no reveals of its own
        val old = json.decodeFromString(RealProject.serializer(), """{"wordKey":"kedi","script":{"word":"Kedi"},"drawings":[]}""")
        assertTrue(old.reveals.isEmpty())
        // a drawing saved before there were lines opens on the game's screen and plays its clip, with no music
        val noLines = json.decodeFromString(RealReveal.serializer(), """{"sampleId":"a","clip":"a.mp4","clipMs":8000}""")
        assertEquals(VoicedLine(), noLines.hook)
        assertEquals(VoicedLine(), noLines.outro)
        assertEquals(listOf(RealPhase.Hook, RealPhase.Clip), RealTimeline.build(noLines).map { it.phase })
        assertNull(noLines.music)
        assertEquals(RealMix.DEFAULT_VOLUME, noLines.musicVolume, 0f)
    }
}
