package com.sualtikasifi.cizimhafiza.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UsernameRepositoryTest {

    @Test
    fun caseAndTurkishLettersFoldToOneKey() {
        val key = UsernameRepository.keyOf("Ali")
        assertEquals(key, UsernameRepository.keyOf("ALI"))
        assertEquals(key, UsernameRepository.keyOf("Alİ"))
        assertEquals(key, UsernameRepository.keyOf("alı"))
    }

    @Test
    fun diacriticsAreFolded() {
        assertEquals(UsernameRepository.keyOf("Sema"), UsernameRepository.keyOf("Şema".replace("Ş", "S")))
        assertEquals("cagri", UsernameRepository.keyOf("Çağrı"))
    }

    @Test
    fun validNamesAndInvalidOnes() {
        assertTrue(UsernameRepository.isValid("Ay_se.12"))
        assertFalse(UsernameRepository.isValid("a"))
        assertFalse(UsernameRepository.isValid("a".repeat(17)))
        assertFalse(UsernameRepository.isValid("bad/name"))
        assertFalse(UsernameRepository.isValid("___"))
    }
}
