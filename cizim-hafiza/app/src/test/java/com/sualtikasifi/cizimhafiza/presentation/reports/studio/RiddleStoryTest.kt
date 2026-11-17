package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RiddleStoryTest {
    @Test fun `the right answer never lands on the letter used last time`() {
        repeat(200) { seed ->
            for (last in -1..3) {
                val (options, at) = RiddleStory.arrange("Elma", listOf("Armut", "Şeftali", "Kiraz"), last, Random(seed))
                assertEquals("Elma", options[at])
                assertEquals(4, options.size)
                if (last >= 0) assertTrue("seed $seed last $last got $at", at != last)
            }
        }
    }

    @Test fun `every letter comes up over many tries`() {
        val seen = HashSet<Int>()
        var last = -1
        val random = Random(7)
        repeat(300) {
            val (_, at) = RiddleStory.arrange("Elma", listOf("a", "b", "c"), last, random)
            seen += at; last = at
        }
        assertEquals(setOf(0, 1, 2, 3), seen)
    }

    @Test fun `the wrong answers all stay in the list`() {
        val (options, at) = RiddleStory.arrange("Elma", listOf("Armut", "Şeftali", "Kiraz"), 2, Random(1))
        assertEquals(setOf("Armut", "Şeftali", "Kiraz"), (options - options[at]).toSet())
    }

    @Test fun `a good answer is read`() {
        val w = RiddleStory.parse("""```json
            {"question":"Bu çizim ne?","answer":"Elma","others":["Armut","Şeftali","Kiraz"]}
            ```""", "Elma")
        assertNotNull(w)
        assertEquals("Bu çizim ne?", w!!.question)
        assertEquals(listOf("Armut", "Şeftali", "Kiraz"), w.others)
    }

    @Test fun `wrong answers that repeat the right one or each other are dropped`() {
        assertNull(RiddleStory.parse("""{"question":"Bu ne?","answer":"Elma","others":["elma","Armut","armut","Kiraz"]}""", "Elma"))
        val ok = RiddleStory.parse("""{"question":"Bu ne?","answer":"Elma","others":["elma","Armut","Kiraz","Şeftali"]}""", "Elma")
        assertEquals(listOf("Armut", "Kiraz", "Şeftali"), ok!!.others)
    }

    @Test fun `text without a question is refused`() {
        assertNull(RiddleStory.parse("""{"answer":"Elma","others":["a","b","c"]}""", "Elma"))
        assertNull(RiddleStory.parse("not json", "Elma"))
    }

    @Test fun `the pasted text marks the right one`() {
        val t = RiddleStory.asText("Bu çizim ne?", listOf("Armut", "Elma", "Kiraz", "Muz"), 1)
        assertEquals("Bu çizim ne?\nA) Armut\nB) Elma   ✓\nC) Kiraz\nD) Muz", t)
    }
}
