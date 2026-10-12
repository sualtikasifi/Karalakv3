package com.sualtikasifi.cizimhafiza.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CouponCodesTest {
    @Test
    fun `normalize upper-cases and drops spaces`() {
        assertEquals("KRLK-7H3QM2", CouponCodes.normalize("  krlk-7h3 qm2 "))
    }

    @Test
    fun `normalize refuses a code that cannot be one`() {
        assertNull(CouponCodes.normalize("ab"))
        assertNull(CouponCodes.normalize("a".repeat(CouponCodes.MAX_LENGTH + 1)))
        assertNull(CouponCodes.normalize("KOD/../x"))
    }

    @Test
    fun `generated codes are valid and avoid look-alike characters`() {
        repeat(200) {
            val code = CouponCodes.generate()
            assertNotNull(code, CouponCodes.normalize(code))
            assertTrue(code.removePrefix("KRLK-").none { it in "01OIL" })
        }
    }
}
