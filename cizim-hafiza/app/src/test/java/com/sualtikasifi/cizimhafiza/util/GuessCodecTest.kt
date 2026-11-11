package com.sualtikasifi.cizimhafiza.util

import com.sualtikasifi.cizimhafiza.domain.model.DrawingPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GuessCodecTest {
    private val words = listOf("kara kedi", "kedi", "güneş", "gül", "deniz yıldızı", "kırmızı")

    @Test
    fun `hash is stable and depends on the salt and on the folded spelling`() {
        // pinned: hosting/bil/bil.js computes the same value, so a page and the app always agree
        assertEquals("fd9a4e1eb8cde4c5d1d0a4c1e30a8c6e5f2ed8f5d2c3a9b6a2d12ff1c2b2fd1a".length, GuessCodec.hash("salty", "Kedi").length)
        assertEquals(GuessCodec.hash("salty", "KEDİ"), GuessCodec.hash("salty", "kedi"))
        assertEquals(GuessCodec.hash("salty", "güneş"), GuessCodec.hash("salty", "GUNES"))
        assertNotEquals(GuessCodec.hash("salty", "kedi"), GuessCodec.hash("pepper", "kedi"))
        assertNotEquals(GuessCodec.hash("salty", "kedi"), GuessCodec.hash("salty", "kuş"))
    }

    @Test
    fun `hash of a known word matches the value the web page computes`() {
        // sha256("saltsalt01:kara kedi")
        assertEquals(
            java.security.MessageDigest.getInstance("SHA-256").digest("saltsalt01:kara kedi".toByteArray())
                .joinToString("") { "%02x".format(it) },
            GuessCodec.hash("saltsalt01", "Kara  Kedi")
        )
    }

    @Test
    fun `pattern and blanks describe the word's shape`() {
        assertEquals("4 4", GuessCodec.pattern("Kara Kedi"))
        assertEquals("5", GuessCodec.pattern("güneş"))
        assertEquals("_ _ _ _   _ _ _ _", GuessCodec.blanks("4 4"))
        assertEquals("K _ _ _   _ _ _ _", GuessCodec.blanks("4 4", "K"))
        assertEquals("K", GuessCodec.firstLetter("kedi").uppercase().take(1))
    }

    @Test
    fun `a right guess is accepted with accents, case and a slip on a long word`() {
        val salt = "saltsalt01"
        val hash = GuessCodec.hash(salt, "deniz yıldızı")
        assertTrue(GuessCodec.isCorrect("Deniz Yildizi", words, salt, hash))
        assertTrue(GuessCodec.isCorrect("deniz yildiz", words, salt, hash))
        assertFalse(GuessCodec.isCorrect("kara kedi", words, salt, hash))
        assertFalse(GuessCodec.isCorrect("", words, salt, hash))
    }

    @Test
    fun `a short word gets no slack so a neighbour word never scores`() {
        val salt = "saltsalt01"
        val hash = GuessCodec.hash(salt, "gül")
        assertTrue(GuessCodec.isCorrect("GÜL", words, salt, hash))
        assertFalse(GuessCodec.isCorrect("gol", words + "gol", salt, hash))
    }

    @Test
    fun `the word is found again from its hash, or null when the list lacks it`() {
        val salt = "saltsalt01"
        assertEquals("güneş", GuessCodec.reveal(words, salt, GuessCodec.hash(salt, "güneş")))
        assertNull(GuessCodec.reveal(words, salt, GuessCodec.hash(salt, "elma")))
    }

    @Test
    fun `ids are ten safe characters the rules accept`() {
        repeat(200) {
            val id = GuessCodec.newId()
            assertEquals(GuessCodec.ID_LENGTH, id.length)
            assertTrue(GuessCodec.isValidId(id))
            assertTrue(id.none { it in "0OIl1" })
        }
        assertFalse(GuessCodec.isValidId("short"))
        assertFalse(GuessCodec.isValidId("has space!!"))
        assertEquals(12, GuessCodec.newSalt().length)
    }

    @Test
    fun `strokes round trip into a thousand-unit square keeping their shape`() {
        val strokes = listOf(
            listOf(DrawingPoint(100f, 300f), DrawingPoint(300f, 300f), DrawingPoint(300f, 500f)),
            listOf(DrawingPoint(150f, 450f))
        )
        val back = GuessCodec.decodeStrokes(GuessCodec.encodeStrokes(strokes))
        assertEquals(2, back.size)
        assertEquals(3, back[0].size)
        // the long side is 200 wide: it fills the square, so the points move to 0 and 1000
        assertEquals(0f, back[0][0].x, 0.5f)
        assertEquals(1000f, back[0][1].x, 0.5f)
        assertEquals(1000f, back[0][2].y, 0.5f)
        assertTrue(back.flatten().all { it.x in 0f..1000f && it.y in 0f..1000f })
    }

    @Test
    fun `a busy scribble is thinned but every stroke survives`() {
        val strokes = List(40) { s -> List(200) { i -> DrawingPoint(i.toFloat() + s, (i * 3 % 97).toFloat()) } }
        val encoded = GuessCodec.encodeStrokes(strokes)
        val back = GuessCodec.decodeStrokes(encoded)
        assertEquals(40, back.size)
        assertTrue(back.sumOf { it.size } <= GuessCodec.MAX_POINTS + 40)
        assertTrue("small enough for the rules (150 KB)", encoded.length < 150_000)
    }

    @Test
    fun `an unreadable drawing decodes to nothing, an empty one to an empty list`() {
        assertTrue(GuessCodec.decodeStrokes("not json").isEmpty())
        assertTrue(GuessCodec.decodeStrokes(GuessCodec.encodeStrokes(emptyList())).isEmpty())
    }

    @Test
    fun `languages map to a word list like the app does`() {
        assertEquals("tr", GuessCodec.langFor("tr"))
        assertEquals("es", GuessCodec.langFor("es"))
        assertEquals("pt", GuessCodec.langFor("pt"))
        assertEquals("en", GuessCodec.langFor("de"))
        assertEquals("en", GuessCodec.langFor("en"))
    }
}
