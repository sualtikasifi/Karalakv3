package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import com.sualtikasifi.cizimhafiza.domain.model.DrawingPoint
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotCopy
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotDraft
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotDraftStore
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotExamples
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotLearning
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotMode
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotScript
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MascotEmotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.random.Random

class ForgotDraftTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun line(t: String) = SpokenLine(t, MascotEmotion.Neutral)

    private fun script(word: String = "şemsiye", hook: String = "Bunu bilen çıkmadı. İlk sen ol.") = ForgotScript(
        drawingId = 7, word = word, hook = line(hook),
        guesses = listOf(line("Bunlar kulak, eminim!"), line("Yok yok, bir ip!")),
        askLine = line("Yanılıyor muyum?"), codeLine = line("Cevap kodun kendisi, bilen kapar!"), outro = line("Karalak'ı indir, katıl!"),
        mode = ForgotMode.Word, reward = "500 altın", code = "SEMSIYE", revealed = setOf(2)
    )

    private fun draft(id: Long, s: ForgotScript = script(), done: Boolean = false, lang: String = "tr") = ForgotDraft(
        id = id, updatedAt = id, word = s.word, category = "nesne",
        strokes = listOf(listOf(DrawingPoint(0.1f, 0.2f), DrawingPoint(0.3f, 0.4f))), drawingId = s.drawingId, script = s,
        caption = "Sen bilir misin?", hashtags = "#karalak", gold = 500, done = done, lang = lang
    )

    @Test
    fun aDraftSurvivesBeingWrittenAndReadBack() {
        val store = ForgotDraftStore(tmp.newFolder("d"))
        val d = draft(1L)
        store.save(d)
        val back = store.load(1L)
        assertEquals(d, back)
        assertEquals(setOf(2), back!!.script.revealed)
        assertEquals(2, back.drawing().strokes[0].size)
        // a draft that is not there, or a file that cannot be read, is nothing: never a crash
        assertNull(store.load(99L))
        java.io.File(tmp.root, "d/5.json").writeText("not json")
        assertNull(store.load(5L))
    }

    @Test
    fun aKeptVideoOnlyCountsWhileTheScriptIsTheOneItWasMadeFrom() {
        val s = script()
        val made = draft(1L, s).copy(video = "1.mp4", videoFingerprint = ForgotDraftStore.fingerprint(s))
        assertTrue(made.videoIsCurrent)
        // a line changed after the video was made: its last form no longer shows this script
        val edited = made.copy(script = s.copy(hook = line("Başka bir kanca")))
        assertFalse(edited.videoIsCurrent)
        assertNotEquals(ForgotDraftStore.fingerprint(s), ForgotDraftStore.fingerprint(edited.script))
        assertFalse(draft(2L).videoIsCurrent)
    }

    @Test
    fun onlyTheNewestVideosAreKeptAndDeletingADraftTakesItsVideo() {
        val dir = tmp.newFolder("v")
        val store = ForgotDraftStore(dir)
        val src = tmp.newFile("out.mp4").apply { writeText("video") }
        repeat(ForgotDraftStore.MAX_VIDEOS + 3) { i ->
            val name = store.keepVideo(100L + i, src)
            assertEquals("${100L + i}.mp4", name)
            store.videoFile(name!!).setLastModified(1_000L * (i + 1))
        }
        val kept = java.io.File(dir, "videos").listFiles { f -> f.name.endsWith(".mp4") }!!.size
        assertEquals(ForgotDraftStore.MAX_VIDEOS, kept)
        store.save(draft(102L))
        store.delete(102L)
        assertNull(store.load(102L))
        assertFalse(store.videoFile("102.mp4").exists())
    }

    @Test
    fun onlyFinishedDraftsAreListedNewestFirstAndAtMostTheLimit() {
        val store = ForgotDraftStore(tmp.newFolder("f"))
        listOf(1L to false, 2L to true, 3L to true, 4L to true).forEach { (id, done) ->
            store.save(draft(id, done = done))
            java.io.File(tmp.root, "f/$id.json").setLastModified(id * 1_000)
        }
        assertEquals(listOf(4L, 3L), store.doneDrafts(2).map { it.id })
        assertEquals(listOf(4L, 3L, 2L), store.doneDrafts(10).map { it.id })
    }

    @Test
    fun theExamplesAreTheFinishedVideosOwnLinesAndNeverGiveTheWordAway() {
        val finished = draft(1L, script(word = "kedi", hook = "Bunu bilen çıkmadı. İlk sen ol."), done = true)
        val withLeak = draft(2L, script(word = "kedi", hook = "Bu bir kedi, herkes bilir."), done = true)
        val unfinished = draft(3L, script(hook = "Bitmemiş kanca"), done = false)
        val other = draft(4L, script(hook = "Başka dilde"), done = true, lang = "en")
        val ex = ForgotLearning.examples(listOf(finished, withLeak, unfinished, other), word = "kedi", excludeId = null, lang = "tr", random = Random(1))
        assertTrue("Bunu bilen çıkmadı. İlk sen ol." in ex.hooks)
        assertFalse(ex.hooks.any { it.contains("kedi") })
        assertFalse(ex.hooks.any { it == "Bitmemiş kanca" || it == "Başka dilde" })
        assertTrue(ex.guesses.isNotEmpty() && ex.guesses.size <= ForgotLearning.GUESSES)
        // the draft being worked on never teaches itself
        assertTrue(ForgotLearning.examples(listOf(finished), "kedi", excludeId = 1L, lang = "tr", random = Random(1)).isEmpty)
        // the stock lines the panel starts with are no examples of anything
        val stock = draft(5L, script().copy(hook = line(ForgotCopy.HOOK), askLine = line(ForgotCopy.ASK), codeLine = line(ForgotCopy.CODE_WORD), outro = line(ForgotCopy.OUTRO), guesses = ForgotCopy.placeholderGuesses()), done = true)
        assertTrue(ForgotLearning.examples(listOf(stock), "kedi", null, "tr", Random(1)).isEmpty)
    }

    @Test
    fun theExamplesGoIntoThePromptsOnlyWhenThereAreSome() {
        val ex = ForgotExamples(hooks = listOf("Bunu bilen çıkmadı."), outros = listOf("Karalak'ı indir, katıl!"))
        val with = ForgotCopy.directorPrompt("kedi", listOf(0.4f), "happy", ForgotMode.Word, "100 altın", "", examples = ex)
        assertTrue(with.contains("ÖNCEKİ TAMAMLANAN VİDEOLARDAN ÖRNEKLER"))
        assertTrue(with.contains("\"Bunu bilen çıkmadı.\""))
        assertTrue(with.contains("TEKRARLAMA"))
        assertFalse(ForgotCopy.directorPrompt("kedi", listOf(0.4f), "happy", ForgotMode.Word, "100 altın", "").contains("ÖNCEKİ TAMAMLANAN"))
        // the hook button is shown the hooks only
        val hook = ForgotCopy.hookPrompt("kedi", ForgotCopy.HookKind.Challenge, "happy", emptyList(), ex)
        assertTrue(hook.contains("Bunu bilen çıkmadı."))
        assertFalse(hook.contains("Karalak'ı indir"))
    }

    @Test
    fun aDraftsLineInTheLogFollowsItFromDraftToFinishedVideo() {
        val old = PublishEntry(id = 1L, format = "Gerçek Olsaydı", label = "x", madeAt = 1L)
        val d = PublishEntry(id = 50L, format = "Düello", label = "kedi", detail = "a", madeAt = 50L, draftId = 50L, draft = true)
        var log = PublishLog.addDraft(listOf(old), d)
        assertEquals(listOf(50L, 1L), log.map { it.id })
        // the same draft added again replaces its line
        assertEquals(2, PublishLog.addDraft(log, d.copy(label = "kedi2")).size)
        // nothing changed: the very same list (no write)
        assertSame(log, PublishLog.touchDraft(log, 50L, "kedi", "a", "", ""))
        assertEquals("kopek", PublishLog.touchDraft(log, 50L, "kopek", "a", "", "").first().label)
        // a draft cannot be finished; once its video is made its line stops being one and keeps its id
        assertTrue(PublishLog.toggleDone(log, 50L).none { it.done })
        log = PublishLog.finish(log, 50L, "b", "KEDI", "500 altın", 99L)!!
        assertFalse(log.first().draft)
        assertEquals(99L, log.first().madeAt)
        assertEquals(50L, log.first().id)
        assertTrue(PublishLog.toggleDone(log, 50L).first().done)
        assertNull(PublishLog.finish(log, 12345L, "", "", "", 1L))
        // older lines (no draft) are still readable, and the new fields default
        assertEquals(0L, PublishLog.decode(PublishLog.encode(listOf(old))).first().draftId)
        assertNotNull(PublishLog.decode("""[{"id":1,"format":"Düello","label":"x","madeAt":1}]""").firstOrNull())
    }
}
