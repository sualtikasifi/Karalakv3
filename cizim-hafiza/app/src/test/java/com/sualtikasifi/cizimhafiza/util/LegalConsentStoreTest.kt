package com.sualtikasifi.cizimhafiza.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class LegalConsentStoreTest {

    private val today = LocalDate.of(2026, 10, 28)

    // --- age ---

    @Test
    fun `turns a year older on the birthday, not before`() {
        assertEquals(12, LegalConsentStore.ageOf(BirthDate(2013, 10, 29), today))
        assertEquals(13, LegalConsentStore.ageOf(BirthDate(2013, 10, 28), today))
        assertEquals(13, LegalConsentStore.ageOf(BirthDate(2013, 1, 1), today))
    }

    @Test
    fun `a year-only answer gives the youngest the player can be`() {
        assertEquals(12, LegalConsentStore.ageOf(BirthDate(2013), today))
    }

    @Test
    fun `no age before the question was answered`() {
        assertNull(LegalConsentStore.ageOf(BirthDate(0), today))
    }

    // --- validation ---

    @Test
    fun `takes a real date in the past`() {
        assertTrue(LegalConsentStore.isValidBirthDate(BirthDate(2016, 6, 15), today))
        assertTrue(LegalConsentStore.isValidBirthDate(BirthDate(2016, 2, 29), today))
        assertTrue(LegalConsentStore.isValidBirthDate(BirthDate(2026, 10, 28), today))
    }

    @Test
    fun `refuses a date that does not exist or has not come yet`() {
        assertFalse(LegalConsentStore.isValidBirthDate(BirthDate(2015, 2, 29), today))
        assertFalse(LegalConsentStore.isValidBirthDate(BirthDate(2016, 13, 1), today))
        assertFalse(LegalConsentStore.isValidBirthDate(BirthDate(2016, 4, 31), today))
        assertFalse(LegalConsentStore.isValidBirthDate(BirthDate(2026, 10, 29), today))
        assertFalse(LegalConsentStore.isValidBirthDate(BirthDate(1900, 1, 1), today))
    }
}
