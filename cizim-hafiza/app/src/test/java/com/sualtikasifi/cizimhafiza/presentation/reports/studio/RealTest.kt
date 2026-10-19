package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import com.sualtikasifi.cizimhafiza.domain.model.DrawingPoint
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealCopy
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealDrawing
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
        assertEquals(0L, RealTimeline.totalMs(emptyList()))
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
    }

    @Test
    fun videoPromptKeepsTheObjectAsPaintedYetMakesSomethingHappenToIt() {
        val video = RealCopy.videoPrompt("microwave", "a person puts a plate inside and presses the buttons.")
        assertTrue(video.contains("very first frame"))
        // the object is never changed ...
        assertTrue(video.contains("stay EXACTLY as it is in the photo"))
        assertTrue(video.contains("Do NOT add eyes, faces, arms, legs"))
        // ... but it is not left to sway: a small story with a person, and real sound
        assertTrue(video.contains("What happens: a person puts a plate inside and presses the buttons. "))
        assertTrue(video.contains("must not just sit there and sway"))
        assertTrue(video.contains("generate the audio too"))
        // with no scene of its own, a person comes and uses the thing
        val generic = RealCopy.videoPrompt("pomegranate", "")
        assertTrue(generic.contains("walks into the shot"))
        assertTrue(generic.contains("pomegranate"))
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
            reveals = listOf(RealReveal("a", "cat", "six legs", "wobble", image = "a.jpg", clip = "a.mp4", clipMs = 8_000))
        )
        val back = json.decodeFromString(RealProject.serializer(), json.encodeToString(RealProject.serializer(), project))
        assertEquals(project, back)
        assertEquals(setOf("a.jpg", "a.mp4"), back.media)
        // a project saved before the video was simplified carried a script and no reveals of its own
        val old = json.decodeFromString(RealProject.serializer(), """{"wordKey":"kedi","script":{"word":"Kedi"},"drawings":[]}""")
        assertTrue(old.reveals.isEmpty())
    }
}
