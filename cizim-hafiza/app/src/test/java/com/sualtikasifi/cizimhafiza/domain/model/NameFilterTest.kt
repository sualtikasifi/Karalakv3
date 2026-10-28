package com.sualtikasifi.cizimhafiza.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NameFilterTest {

    @Test
    fun `ordinary names pass`() {
        listOf(
            "Zeynep", "Ayşe_34", "Karalak4491", "Mustafa", "İrem", "Işıl", "Çağrı", "sıkıntı", "okuyarak", "bananın",
            "Nazif", "Amina", "Vergara", "Dickens", "grape", "enviado", "disputa", "computadora", "peacock", "Assassin",
            "Merdiven", "João", "Señorita", "Ibrahim", "Cocuk", "Classic", "Lionel 10"
        ).forEach { assertTrue(it, NameFilter.isAllowed(it)) }
    }

    @Test
    fun `profanity is refused in all four languages`() {
        listOf("orospu", "siktir git", "pezevenk", "ibne", "fuck", "bitch", "puta", "hijodeputa", "mierda", "caralho", "merda")
            .forEach { assertFalse(it, NameFilter.isAllowed(it)) }
    }

    @Test
    fun `disguises do not get through`() {
        listOf("0r0spu", "s1kt1r", "F.U.C.K", "fuuuuck", "y4rr4k", "Amına koyim", "A.M.K", "amk34", "PUTA_", "s_g", "OROSPU")
            .forEach { assertFalse(it, NameFilter.isAllowed(it)) }
    }

    @Test
    fun `short words are refused only on their own`() {
        assertFalse(NameFilter.isAllowed("amk"))
        assertFalse(NameFilter.isAllowed("puta 99"))
        assertFalse(NameFilter.isAllowed("Admin"))
        assertFalse(NameFilter.isAllowed("moderatör"))
        assertTrue(NameFilter.isAllowed("Epic Artist"))
        assertFalse(NameFilter.isAllowed("pic"))
    }
}
