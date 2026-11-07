package com.sualtikasifi.cizimhafiza.presentation.reports

import com.sualtikasifi.cizimhafiza.data.repository.couponSummary
import com.sualtikasifi.cizimhafiza.domain.model.ChestTier
import com.sualtikasifi.cizimhafiza.domain.model.Coupon
import com.sualtikasifi.cizimhafiza.domain.model.CouponCodes
import com.sualtikasifi.cizimhafiza.domain.model.CouponExistsException
import com.sualtikasifi.cizimhafiza.util.AdminAuditText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The developer panel's own rules, kept apart from the screens: the coupon form, the version lock and the audit lines. */
class PanelAdminTest {

    // ---- the coupon form

    @Test
    fun aCouponNeedsAtLeastOneReward() {
        assertEquals("En az bir ödül seç.", CouponForm.problem(0, 0, 0, 10, 0, ""))
        assertNull(CouponForm.problem(100, 0, 0, 10, 0, ""))
        assertNull(CouponForm.problem(0, 50, 0, 10, 0, ""))
        assertNull(CouponForm.problem(0, 0, 1, 10, 0, ""))
    }

    @Test
    fun theFormKeepsToTheLimitsOfAGivenReward() {
        assertNotNull(CouponForm.problem(CouponCodes.MAX_GOLD + 1, 0, 0, 10, 0, ""))
        assertNotNull(CouponForm.problem(0, CouponCodes.MAX_XP + 1, 0, 10, 0, ""))
        assertNotNull(CouponForm.problem(0, 0, CouponCodes.MAX_CHESTS + 1, 10, 0, ""))
        assertNotNull(CouponForm.problem(100, 0, 0, 0, 0, ""))
    }

    @Test
    fun aTypedCodeHasToBeUsableAndOnlyCountsForTheSharedCode() {
        assertNotNull(CouponForm.problem(100, 0, 0, 10, 0, "ab"))
        assertNull(CouponForm.problem(100, 0, 0, 10, 0, "BAHAR-2026"))
        // with separate codes the typed one is not used at all
        assertNull(CouponForm.problem(100, 0, 0, 1, 5, "ab"))
    }

    @Test
    fun daysBecomeAnExpiryAndZeroMeansNever() {
        assertEquals(0L, CouponForm.expiresAt(0, 1_000L))
        assertEquals(1_000L + 7L * 86_400_000L, CouponForm.expiresAt(7, 1_000L))
    }

    @Test
    fun aTakenNameIsSaidWithTheNameInIt() {
        assertTrue(CouponForm.takenMessage("BAHAR").contains("BAHAR"))
        assertTrue(CouponForm.takenMessage("BAHAR").contains("zaten var"))
        assertEquals("BAHAR", CouponExistsException("BAHAR").code)
    }

    @Test
    fun theAuditLineSaysWhatACouponGives() {
        val c = Coupon("BAHAR", gold = 500, xp = 40, chests = mapOf(ChestTier.GOLD to 1), maxUses = 100, expiresAtMillis = 0L)
        val line = couponSummary(c)
        assertTrue(line, line.contains("500 altın"))
        assertTrue(line, line.contains("40 XP"))
        assertTrue(line, line.contains("1 Sanatçı kasa"))
        assertTrue(line, line.contains("100 kişi"))
        assertTrue(line, line.endsWith("süresiz"))
    }

    // ---- the version lock

    @Test
    fun theLockCannotGoAboveThisBuild() {
        assertNull(UpdateLock.problem(0, 120))
        assertNull(UpdateLock.problem(120, 120))
        assertNotNull(UpdateLock.problem(121, 120))
        assertNotNull(UpdateLock.problem(-1, 120))
        assertTrue(UpdateLock.problem(999, 120)!!.contains("120"))
    }

    @Test
    fun theLockLogLineShowsBeforeAndAfter() {
        assertEquals("en düşük sürüm kodu 100 -> 120", UpdateLock.detail(100, 120))
        assertTrue(UpdateLock.detail(100, 0).startsWith("kilit kaldırıldı"))
    }

    // ---- the audit lines

    @Test
    fun auditTextIsKeptShort() {
        assertEquals(200, AdminAuditText.detail("x".repeat(500)).length)
        assertEquals(120, AdminAuditText.target("y".repeat(500)).length)
        assertEquals("a", AdminAuditText.detail("  a  "))
    }
}
