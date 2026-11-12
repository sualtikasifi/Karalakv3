package com.sualtikasifi.cizimhafiza.presentation.account

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortenEmailTest {
    @Test fun short_address_is_kept() = assertEquals("raunen3075@gmail.com", shortenEmail("raunen3075@gmail.com", 30))

    @Test fun long_address_keeps_its_domain() {
        val out = shortenEmail("very.long.address.that.goes.on.and.on@gmail.com", 30)
        assertTrue(out, out.endsWith("@gmail.com"))
        assertTrue(out, out.contains("…"))
        assertTrue(out, out.length <= 31)
    }

    @Test fun long_domain_is_not_cut() {
        val out = shortenEmail("a.b@some-very-long-company-domain-name.example.com", 30)
        assertTrue(out, out.endsWith("@some-very-long-company-domain-name.example.com"))
    }
}
