package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioModelsTest {

    @Test
    fun quizRunsOpeningThenThreeStepsPerDrawingThenClosing() {
        val segs = StudioTimeline.build(StudioTemplate.Quiz, 3)
        assertEquals(SegmentKind.Hook, segs.first().kind)
        assertEquals(SegmentKind.Outro, segs.last().kind)
        assertEquals(2 + 3 * 3, segs.size)
        assertEquals(listOf(SegmentKind.Draw, SegmentKind.Ask, SegmentKind.Reveal), segs.subList(1, 4).map { it.kind })
        assertEquals(2, segs[segs.size - 2].item)
        // no gaps, no overlaps
        segs.zipWithNext().forEach { (a, b) -> assertEquals(a.endMs, b.startMs) }
        assertEquals(2200L + 3 * 7600L + 2500L, StudioTimeline.totalMs(segs))
    }

    @Test
    fun versusIsOneComparisonWhateverTheCount() {
        val segs = StudioTimeline.build(StudioTemplate.Versus, 2)
        assertEquals(listOf(SegmentKind.Hook, SegmentKind.Draw, SegmentKind.Ask, SegmentKind.Reveal, SegmentKind.Outro), segs.map { it.kind })
        assertTrue(segs.filter { it.item >= 0 }.all { it.item == 0 })
    }

    @Test
    fun findsTheStretchPlayingAtAnyMoment() {
        val segs = StudioTimeline.build(StudioTemplate.Speed, 3)
        assertEquals(SegmentKind.Hook, StudioTimeline.at(segs, 0).kind)
        assertEquals(SegmentKind.Draw, StudioTimeline.at(segs, segs[1].startMs).kind)
        assertEquals(SegmentKind.Hook, StudioTimeline.at(segs, segs[1].startMs - 1).kind)
        assertEquals(SegmentKind.Outro, StudioTimeline.at(segs, StudioTimeline.totalMs(segs) + 5_000).kind)
        assertEquals(0.5f, segs[1].progress(segs[1].startMs + segs[1].durationMs / 2), 0.001f)
    }

    @Test
    fun framesCoverTheWholeVideo() {
        assertEquals(900, StudioTimeline.frameCount(30_000))
        assertEquals(1, StudioTimeline.frameCount(0))
    }

    @Test
    fun readsTheFiveLabelledLines() {
        val p = StudioCopy.parse(
            """
            KANCA: 5 dakika önce bunu ben çizdim, sence ne?
            SORU: Bu ne?
            KAPANIŞ: Sen de dene: karalak.com
            AÇIKLAMA: "Kendi çizimimi tanıyamadım. Sen kaç saniyede bildin?"
            ETİKETLER: #çizimoyunu #karalak #oyun
            """.trimIndent()
        )
        assertEquals("5 dakika önce bunu ben çizdim, sence ne?", p.hook)
        assertEquals("Bu ne?", p.ask)
        assertEquals("Sen de dene: karalak.com", p.outro)
        assertEquals("Kendi çizimimi tanıyamadım. Sen kaç saniyede bildin?", p.caption)
        assertEquals("#çizimoyunu #karalak #oyun", p.hashtags)
    }

    @Test
    fun toleratesMarkdownAndLowercaseLabels() {
        val p = StudioCopy.parse("**Kanca:** Bunu hatırlıyor musun?\n- kapanış: karalak.com\nAcıklama: Yorumlara yaz\nEtiketler: #a #b")
        assertEquals("Bunu hatırlıyor musun?", p.hook)
        assertEquals("karalak.com", p.outro)
        assertEquals("Yorumlara yaz", p.caption)
        assertEquals("#a #b", p.hashtags)
        assertNull(p.ask)
    }
}
