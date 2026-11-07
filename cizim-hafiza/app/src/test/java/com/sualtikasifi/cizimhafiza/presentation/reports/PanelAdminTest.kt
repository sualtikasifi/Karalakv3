package com.sualtikasifi.cizimhafiza.presentation.reports

import com.sualtikasifi.cizimhafiza.data.repository.couponSummary
import com.sualtikasifi.cizimhafiza.domain.model.ChestTier
import com.sualtikasifi.cizimhafiza.domain.model.Coupon
import com.sualtikasifi.cizimhafiza.domain.model.CouponCodes
import com.sualtikasifi.cizimhafiza.domain.model.CouponExistsException
import com.sualtikasifi.cizimhafiza.util.AdminAuditText
import com.sualtikasifi.cizimhafiza.util.PlayerSearch
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

    // ---- the push form

    @Test
    fun aPushNeedsTheTurkishTextAndHalfAWrittenLanguageIsRefused() {
        assertNotNull(PushForm.problem("", "x", emptyMap()))
        assertNotNull(PushForm.problem("x", "", emptyMap()))
        assertNull(PushForm.problem("Merhaba", "Yeni etkinlik", emptyMap()))
        assertNotNull(PushForm.problem("Merhaba", "Yeni etkinlik", mapOf("en" to PushText("Hello", ""))))
        assertNull(PushForm.problem("Merhaba", "Yeni etkinlik", mapOf("en" to PushText("Hello", "New event"), "pt" to PushText())))
    }

    @Test
    fun theCommandHoldsOnlyWhatDiffersFromEveryoneNow() {
        val plain = PushForm.command(" Merhaba ", "Metin", emptyMap(), PushAudience.All, true, 0L, "a@b.c")
        assertEquals(setOf("type", "title", "body", "by"), plain.keys)
        assertEquals("Merhaba", plain["title"])
        val rich = PushForm.command(
            "Merhaba", "Metin", mapOf("en" to PushText("Hello", "Text"), "es" to PushText()), PushAudience.Lapsed, false, 5_000L, "a@b.c"
        )
        assertEquals(setOf("type", "title", "body", "by", "texts", "audience", "sendAtMillis"), rich.keys)
        assertEquals("lapsed", rich["audience"])
        assertEquals(5_000L, rich["sendAtMillis"])
        assertEquals(setOf("en"), (rich["texts"] as Map<*, *>).keys)
        // the fallback is written only when it is off and means something (everyone, with other languages)
        val noFallback = PushForm.command("a", "b", mapOf("en" to PushText("x", "y")), PushAudience.All, false, 0L, "")
        assertEquals(false, noFallback["fallback"])
        assertNull(PushForm.command("a", "b", mapOf("en" to PushText("x", "y")), PushAudience.Lapsed, false, 0L, "")["fallback"])
    }

    @Test
    fun theSendTimeIsNowOrAFutureMoment() {
        val now = 1_700_000_000_000L
        assertEquals(0L, PushForm.sendAt(PushWhen.Now, now))
        assertEquals(now + 3_600_000L, PushForm.sendAt(PushWhen.InOneHour, now))
        assertEquals(now + 3 * 3_600_000L, PushForm.sendAt(PushWhen.InThreeHours, now))
        val tomorrow = PushForm.sendAt(PushWhen.TomorrowMorning, now)
        assertTrue(tomorrow > now && tomorrow - now <= 48 * 3_600_000L)
        // ten in the morning in Istanbul
        val hour = java.time.Instant.ofEpochMilli(tomorrow).atZone(java.time.ZoneId.of("Europe/Istanbul")).hour
        assertEquals(10, hour)
    }

    @Test
    fun theSummaryNamesWhoInWhichLanguagesAndWhen() {
        val extra = mapOf("en" to PushText("a", "b"))
        assertEquals("herkes · tr+en · hemen", PushForm.summary(PushAudience.All, extra, true, PushWhen.Now))
        assertTrue(PushForm.summary(PushAudience.All, extra, false, PushWhen.InOneHour).contains("metinsiz dillere gitmez"))
        assertEquals(listOf("tr", "en"), PushForm.languages(extra))
    }

    // ---- the player search

    @Test
    fun aNameIsLookedForFromItsBeginningInSeveralSpellings() {
        assertEquals(listOf("ahm", "Ahm"), PlayerSearch.nicknamePrefixes("ahm"))
        assertEquals(listOf("Ahm"), PlayerSearch.nicknamePrefixes("Ahm").let { it.filter { p -> p == "Ahm" } })
        // Turkish i: the capital of "ismet" is İsmet, not Ismet
        assertTrue(PlayerSearch.nicknamePrefixes("ismet").contains("İsmet"))
        // one letter is too little to search by
        assertTrue(PlayerSearch.nicknamePrefixes("a").isEmpty())
        assertEquals("ahm\uf8ff", PlayerSearch.upperBound("ahm"))
    }

    @Test
    fun friendCodesAndUidsAreToldApart() {
        assertTrue(PlayerSearch.isFriendCode("123456"))
        assertTrue(!PlayerSearch.isFriendCode("12345"))
        assertTrue(!PlayerSearch.isFriendCode("12345a"))
        assertTrue(PlayerSearch.looksLikeUid("abcdefghijklmnopqrstuvwxyz12"))
        assertTrue(!PlayerSearch.looksLikeUid("ahmet yılmaz"))
    }

    // ---- coupons by video

    private fun instant(iso: String) = java.time.Instant.parse(iso).toEpochMilli()

    @Test
    fun theDaysOfACodeRunFromItsDayToToday() {
        val created = instant("2026-11-05T10:00:00Z")
        val now = instant("2026-11-07T09:00:00Z")
        assertEquals(listOf("2026-11-05", "2026-11-06", "2026-11-07"), CouponStats.days(created, now))
        assertEquals(listOf("2026-11-06", "2026-11-07"), CouponStats.days(created, now, max = 2))
        assertTrue(CouponStats.days(0L, now).isEmpty())
        assertTrue(CouponStats.days(now, created).isEmpty())
    }

    @Test
    fun newPlayersAreCountedAmongTheUsers() {
        assertEquals(2, CouponStats.newAmong(setOf("a", "b", "c"), setOf("a", "c", "z")))
        assertEquals(0, CouponStats.newAmong(emptySet(), setOf("a")))
    }

    @Test
    fun theCodesAreAddedUpPerVideoMostUsedFirst() {
        val coupons = listOf(
            Coupon("A1", gold = 1, maxUses = 10, usedCount = 4, source = "Düello · 7 Kas"),
            Coupon("A2", gold = 1, maxUses = 10, usedCount = 3, source = "Düello · 7 Kas"),
            Coupon("B1", gold = 1, maxUses = 5, usedCount = 5, source = "Düello · 5 Kas"),
            Coupon("C1", gold = 1, maxUses = 5, usedCount = 2)
        )
        val rows = CouponStats.bySource(coupons)
        assertEquals(listOf("Düello · 7 Kas", "Düello · 5 Kas"), rows.map { it.source })
        assertEquals(CouponStats.SourceRow("Düello · 7 Kas", 2, 7, 20), rows.first())
    }

    @Test
    fun aCodeThatIsOverHasALineToPasteAndAnOpenOneHasNone() {
        val now = 10_000L
        assertNull(CouponStats.closingComment(Coupon("OPEN", gold = 1, maxUses = 10, usedCount = 3), now))
        val full = CouponStats.closingComment(Coupon("FULL", gold = 1, maxUses = 10, usedCount = 10), now)!!
        assertTrue(full, full.contains("FULL") && full.contains("ilk 10 kişi"))
        val over = CouponStats.closingComment(Coupon("OLD", gold = 1, maxUses = 10, usedCount = 4, expiresAtMillis = 5_000L), now)!!
        assertTrue(over, over.contains("OLD") && over.contains("4/10"))
    }

    // ---- the pool as the Düello's source

    private fun recent(id: String, correct: Boolean = true) = com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RecentDrawing(
        id, " şemsiye ", 40, "Ali", 5, "", listOf(listOf(com.sualtikasifi.cizimhafiza.domain.model.DrawingPoint(0f, 0f))), correct
    )

    @Test
    fun aPoolDrawingGetsAStableNegativeIdSoItNeverMeetsARowOfThisPhone() {
        val a = recent("run1#2")
        assertTrue(a.studioId < 0)
        assertEquals(a.studioId, recent("run1#2").studioId)
        assertTrue(a.studioId != recent("run1#3").studioId)
        val d = a.toStudio()
        assertEquals("şemsiye", d.word)
        assertEquals(a.studioId, d.id)
    }

    @Test
    fun theDuelloFindsItsDrawingAmongThisPhonesOwnAndThePools() {
        val own = com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioDrawing(7L, "kedi", "", emptyList(), false)
        val pool = recent("run9#0", correct = false)
        val state = com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioState(
            drawings = listOf(own),
            real = com.sualtikasifi.cizimhafiza.presentation.reports.studio.RealUi(recent = listOf(pool))
        )
        assertEquals("kedi", state.forgotDrawing(7L)?.word)
        assertEquals("şemsiye", state.forgotDrawing(pool.studioId)?.word)
        assertNull(state.forgotDrawing(99L))
        assertNull(state.forgotDrawing(null))
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
