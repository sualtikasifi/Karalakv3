package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedLibraryTest {
    private fun m(id: Long, title: String = "kedi", kind: String = "video", shared: Boolean = false) =
        SavedMedia(id, kind, "Düello", title, file = "f$id", madeAt = id, shared = shared)

    @Test fun newestGoesToTheTop() {
        assertEquals(listOf(2_000_000L, 1L), SavedLibrary.add(listOf(m(1)), m(2_000_000, "köpek")).map { it.id })
    }

    @Test fun sameThingMadeAgainSoonReplacesTheLineAndDropsTheOldFile() {
        val before = listOf(m(1_000))
        val after = SavedLibrary.add(before, m(1_000 + 60_000))
        assertEquals(listOf(61_000L), after.map { it.id })
        assertEquals(listOf(1_000L), SavedLibrary.dropped(before, after).map { it.id })
    }

    @Test fun aPostedOneIsNeverReplaced() {
        val after = SavedLibrary.add(listOf(m(1_000, shared = true)), m(2_000))
        assertEquals(2, after.size)
    }

    @Test fun aVideoAndAnImageOfTheSameWordAreTwoLines() {
        assertEquals(2, SavedLibrary.add(listOf(m(1_000)), m(2_000, kind = "image")).size)
    }

    @Test fun toggleStampsAndClearsTheTime() {
        val on = SavedLibrary.toggle(listOf(m(1)), 1, 99)
        assertTrue(on.first().shared); assertEquals(99L, on.first().sharedAt)
        val off = SavedLibrary.toggle(on, 1, 100)
        assertFalse(off.first().shared); assertEquals(0L, off.first().sharedAt)
    }

    @Test fun removeAndRemoveShared() {
        val list = listOf(m(1), m(2, shared = true), m(3, shared = true))
        assertEquals(listOf(2L, 3L), SavedLibrary.remove(list, 1).map { it.id })
        assertEquals(listOf(1L), SavedLibrary.removeShared(list).map { it.id })
    }

    @Test fun keepsAtMostMax() {
        var list = emptyList<SavedMedia>()
        for (i in 1..SavedLibrary.MAX + 5) list = SavedLibrary.add(list, m(i * 10_000_000L, "w$i"))
        assertEquals(SavedLibrary.MAX, list.size)
    }

    @Test fun roundTripAndGarbage() {
        val list = listOf(m(1, shared = true))
        assertEquals(list, SavedLibrary.decode(SavedLibrary.encode(list)))
        assertEquals(emptyList<SavedMedia>(), SavedLibrary.decode("{oops"))
        assertEquals(emptyList<SavedMedia>(), SavedLibrary.decode(null))
    }
}
