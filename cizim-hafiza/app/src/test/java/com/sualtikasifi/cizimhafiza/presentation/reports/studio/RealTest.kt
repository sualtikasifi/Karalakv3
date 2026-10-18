package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import com.sualtikasifi.cizimhafiza.domain.model.DrawingPoint
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.guess.SpokenLine
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MascotEmotion
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealCopy
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealDrawing
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealPhase
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealProject
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealReveal
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealRoast
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealScript
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealTimeline
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RealTest {

    private fun line(text: String) = SpokenLine(text, MascotEmotion.Laughing)

    private fun script(reveals: List<RealReveal>) = RealScript(
        word = "Kedi",
        drawSeconds = 5,
        hook = line(RealCopy.hookText("Kedi", 5)),
        roasts = listOf(RealRoast("a", line("bir")), RealRoast("b", line("")), RealRoast("c", line("üç"))),
        bridge = line(RealCopy.BRIDGE),
        reveals = reveals,
        outro = line(RealCopy.OUTRO)
    )

    @Test
    fun hookAvoidsTheAccusativeAndLowerCasesTheWord() {
        assertEquals("5 saniyede kedi çiz dedik. İşte Türkiye'nin çizimleri!", RealCopy.hookText("KEDİ", 5))
        assertEquals("7 saniyede ırmak çiz dedik. İşte Türkiye'nin çizimleri!", RealCopy.hookText("IRMAK", 7))
    }

    @Test
    fun revealsWithoutAPictureStayOutOfTheVideo() {
        val sc = script(listOf(RealReveal("a", line("vay")), RealReveal("c", line("eyvah"), image = "c.jpg")))
        assertEquals(listOf("c"), sc.shownReveals.map { it.sampleId })
        // empty roast lines are silent; the bridge only comes when something turns real
        assertEquals(listOf(RealCopy.hookText("Kedi", 5), "bir", "üç", RealCopy.BRIDGE, "eyvah", RealCopy.OUTRO), sc.lines().map { it.text })
        val none = script(listOf(RealReveal("a", line("vay"))))
        assertEquals(listOf(RealCopy.hookText("Kedi", 5), "bir", "üç", RealCopy.OUTRO), none.lines().map { it.text })
    }

    @Test
    fun aRevealOfADrawingNoLongerInTheRunIsDropped() {
        val sc = script(listOf(RealReveal("z", line("x"), image = "z.jpg")))
        assertTrue(sc.shownReveals.isEmpty())
    }

    @Test
    fun timelineIndexesMatchTheSpokenLines() {
        val sc = script(listOf(RealReveal("a", line("vay"), image = "a.jpg", clip = "a.mp4", clipMs = 8_000), RealReveal("c", line(""), image = "c.jpg")))
        val events = RealTimeline.build(sc) { _, l -> 300L + l.text.length * 50L }
        val spoken = events.filter { it.line != null }
        assertEquals(sc.lines().map { it.text }, spoken.map { it.line!!.text })
        assertEquals(spoken.indices.toList(), spoken.map { it.lineIndex })
        // back to back, no gaps
        events.zipWithNext().forEach { (x, y) -> assertEquals(x.endMs, y.startMs) }
        assertEquals(listOf(RealPhase.Hook, RealPhase.Roast, RealPhase.Roast, RealPhase.Roast, RealPhase.Bridge, RealPhase.Reveal, RealPhase.Reveal, RealPhase.Outro), events.map { it.phase })
    }

    @Test
    fun aRevealHoldsTheDoodleWipesThenShowsPictureThenClip() {
        val sc = script(listOf(RealReveal("a", line("vay"), image = "a.jpg", clip = "a.mp4", clipMs = 30_000)))
        val events = RealTimeline.build(sc) { _, _ -> 1_000L }
        val r = events.first { it.phase == RealPhase.Reveal }
        assertEquals(r.startMs + RealTimeline.DOODLE_HOLD + RealTimeline.WIPE, r.photoAtMs)
        assertEquals(r.photoAtMs, r.lineAtMs)
        // a long clip is cut
        assertEquals(RealTimeline.CLIP_MAX, r.clipMs)
        assertEquals(r.endMs, r.clipAtMs + r.clipMs)
        assertTrue(r.clipAtMs - r.photoAtMs >= 1_000L)
    }

    @Test
    fun roastLinesStartAfterTheDrawingAppears() {
        val events = RealTimeline.build(script(emptyList())) { _, _ -> 500L }
        events.filter { it.phase == RealPhase.Roast }.forEach { assertTrue(it.lineAtMs > it.startMs) }
    }

    @Test
    fun parsesPicksKeepingOnlyRevealsThatAreAlsoInTheRun() {
        val text = """
            ```json
            {"word_en":"cat","roasts":[{"index":2,"emotion":"laughing","line":"Bu kedi mi, patates mi?"},{"index":9,"emotion":"x","line":"yok"},{"index":4,"emotion":"shocked","line":"Altı bacak!"}],
             "reveals":[{"index":4,"emotion":"shocked","line":"Koşarak kaçıyorum!","describe":"a cat with six legs"},{"index":1,"line":"olmaz","describe":"x"}],
             "outro":{"emotion":"point","line":"Sen daha iyisini çiz! karalak.com"},"caption":"Hangisi?","hashtags":"#karalak"}
            ```
        """.trimIndent()
        val p = RealCopy.parsePicks(text, 5)
        assertNotNull(p)
        p!!
        assertEquals("cat", p.wordEn)
        assertEquals(listOf(1, 3), p.roasts.map { it.first })
        assertEquals(MascotEmotion.Laughing, p.roasts[0].second.emotion)
        assertEquals(listOf(3), p.reveals.map { it.first })
        assertEquals("a cat with six legs", p.reveals[0].third)
        assertEquals("Sen daha iyisini çiz! karalak.com", p.outro?.text)
        assertNull(RealCopy.parsePicks("{\"roasts\":[]}", 5))
        assertNull(RealCopy.parsePicks("bilmiyorum", 5))
    }

    @Test
    fun parsesAReaction() {
        assertEquals(SpokenLine("Bu kedi beni yer!", MascotEmotion.Shocked), RealCopy.parseReaction("""{"emotion":"shocked","line":"Bu kedi beni yer!"}"""))
        assertNull(RealCopy.parseReaction("""{"emotion":"shocked","line":"  "}"""))
    }

    @Test
    fun imagePromptCarriesTheWordAndTheOddities() {
        val prompt = RealCopy.imagePrompt("kedi", "cat", 5, "a cat with six legs.")
        assertTrue(prompt.contains("\"kedi\" (cat)"))
        assertTrue(prompt.contains("in 5 seconds"))
        assertTrue(prompt.contains("The doodle shows: a cat with six legs.\n"))
        // the drawing's shape wins over what a cat should look like
        assertTrue(prompt.contains("do NOT turn it into a normal cat"))
        assertTrue(prompt.contains("9:16"))
        val noEnglish = RealCopy.imagePrompt("kedi", "", 5, "")
        assertFalse(noEnglish.contains("The doodle shows"))
        assertTrue(noEnglish.contains("a normal kedi"))
        assertTrue(RealCopy.videoPrompt("cat").contains("Do not turn it into a normal cat"))
    }

    @Test
    fun projectSurvivesARoundTripToDisk() {
        val json = Json { ignoreUnknownKeys = true }
        val project = RealProject(
            wordKey = "kedi", wordEn = "cat",
            script = script(listOf(RealReveal("a", line("vay"), "six legs", image = "a.jpg", clip = "a.mp4", clipMs = 8_000))),
            drawings = listOf(RealDrawing("a", listOf(listOf(DrawingPoint(1f, 2f), DrawingPoint(3f, 4f)))))
        )
        val back = json.decodeFromString(RealProject.serializer(), json.encodeToString(RealProject.serializer(), project))
        assertEquals(project, back)
        assertEquals(setOf("a.jpg", "a.mp4"), back.media)
    }
}
