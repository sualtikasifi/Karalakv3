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

    // ---- the economy audit line and its undo

    @Test
    fun theEconomyLineShowsOldAndNewValues() {
        val labels = mapOf("adGoldAmount" to "Reklam altını")
        val line = EconomyChange.detail(labels, mapOf("adGoldAmount" to "5", "other" to "10"), linkedMapOf("adGoldAmount" to 8L, "other" to 12L))
        assertEquals("Reklam altını: 5→8; other: 10→12", line)
    }

    @Test
    fun theUndoHoldsTheValuesAsTheyWere() {
        val back = EconomyChange.undoOf(mapOf("a" to "5", "b" to "", "c" to "x"), mapOf("a" to 8L, "b" to 3L, "c" to 1L))
        assertEquals(mapOf("a" to 5L), back)
    }

    // ---- the passcode gate

    @Test
    fun fiveWrongCodesAreForgivenThenTheWaitDoubles() {
        assertEquals(0L, GatePolicy.waitAfter(4))
        assertEquals(30_000L, GatePolicy.waitAfter(5))
        assertEquals(60_000L, GatePolicy.waitAfter(6))
        assertEquals(120_000L, GatePolicy.waitAfter(7))
        assertEquals(GatePolicy.MAX_WAIT_MS, GatePolicy.waitAfter(40))
    }

    @Test
    fun theGateRemembersWrongCodesAndForgetsThemOnARightOne() {
        var now = 1_000_000L
        val prefs = MemoryPrefs()
        val gate = GateLockout(prefs) { now }
        repeat(4) { assertEquals(0L, gate.recordWrong()) }
        assertEquals(0L, gate.remainingMillis())
        assertEquals(30_000L, gate.recordWrong())
        assertEquals(30_000L, gate.remainingMillis())
        now += 10_000
        assertEquals(20_000L, gate.remainingMillis())
        // a new GateLockout over the same preferences (the screen was left and opened again) still knows
        assertEquals(20_000L, GateLockout(prefs) { now }.remainingMillis())
        now += 25_000
        assertEquals(0L, gate.remainingMillis())
        // the next wrong code after the wait is the sixth in a row: a minute
        assertEquals(60_000L, gate.recordWrong())
        gate.recordRight()
        assertEquals(0, gate.fails())
        assertEquals(0L, gate.remainingMillis())
    }

    @Test
    fun aClockSetBackCannotStretchTheWaitPastTheLongestOne() {
        var now = 5_000_000L
        val gate = GateLockout(MemoryPrefs()) { now }
        repeat(5) { gate.recordWrong() }
        now = 0L
        assertTrue(gate.remainingMillis() <= GatePolicy.MAX_WAIT_MS)
    }

    @Test
    fun waitsAreSaidInSecondsAndMinutes() {
        assertEquals("1 sn", gateWaitText(1))
        assertEquals("45 sn", gateWaitText(44_001))
        assertEquals("1 dk", gateWaitText(60_000))
        assertEquals("3 dk 20 sn", gateWaitText(200_000))
    }
}

/** SharedPreferences in memory, enough for the gate's own two numbers. */
private class MemoryPrefs : android.content.SharedPreferences {
    private val map = HashMap<String, Any?>()
    override fun getAll(): MutableMap<String, *> = map
    override fun getString(key: String?, defValue: String?) = map[key] as? String ?: defValue
    override fun getStringSet(key: String?, defValues: MutableSet<String>?) = defValues
    override fun getInt(key: String?, defValue: Int) = map[key] as? Int ?: defValue
    override fun getLong(key: String?, defValue: Long) = map[key] as? Long ?: defValue
    override fun getFloat(key: String?, defValue: Float) = map[key] as? Float ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean) = map[key] as? Boolean ?: defValue
    override fun contains(key: String?) = map.containsKey(key)
    override fun registerOnSharedPreferenceChangeListener(l: android.content.SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    override fun unregisterOnSharedPreferenceChangeListener(l: android.content.SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    override fun edit(): android.content.SharedPreferences.Editor = object : android.content.SharedPreferences.Editor {
        private val pending = HashMap<String, Any?>()
        private val removed = HashSet<String>()
        private var clear = false
        override fun putString(key: String, value: String?) = apply { pending[key] = value }
        override fun putStringSet(key: String, values: MutableSet<String>?) = apply { pending[key] = values }
        override fun putInt(key: String, value: Int) = apply { pending[key] = value }
        override fun putLong(key: String, value: Long) = apply { pending[key] = value }
        override fun putFloat(key: String, value: Float) = apply { pending[key] = value }
        override fun putBoolean(key: String, value: Boolean) = apply { pending[key] = value }
        override fun remove(key: String) = apply { removed += key }
        override fun clear() = apply { clear = true }
        override fun commit(): Boolean { apply(); return true }
        override fun apply() {
            if (clear) map.clear()
            removed.forEach { map.remove(it) }
            map.putAll(pending)
        }
    }
}
