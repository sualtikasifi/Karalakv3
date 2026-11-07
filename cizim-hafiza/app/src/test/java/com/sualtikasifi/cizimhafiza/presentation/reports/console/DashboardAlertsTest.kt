package com.sualtikasifi.cizimhafiza.presentation.reports.console

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** What the Pano warns about, and the assistant's saved answers kept in step with Firestore. */
class DashboardAlertsTest {

    private fun series(players: List<Long>, revenueMicros: List<Long> = List(players.size) { 0L }, d1: List<Double?> = emptyList()) = DaySeries(
        days = players.indices.map { "2026-11-%02d".format(it + 1) },
        players = players,
        counts = revenueMicros.map { mapOf("rev__reward__micros" to it, "revcur__USD" to 1L) },
        d1Return = d1
    )

    @Test
    fun aQuietDaySaysNothing() {
        val s = series(List(14) { 100L }, List(14) { 2_000_000L })
        assertTrue(DashboardAlerts.compute(s, 3, 0, emptyList(), 0L).isEmpty())
    }

    @Test
    fun aPiledUpQueueAndWaitingReportsAreWarnedAbout() {
        val a = DashboardAlerts.compute(null, 20, 2, emptyList(), 0L)
        assertEquals(listOf("queue", "reports"), a.map { it.target })
        assertTrue(a.all { it.level == AlertLevel.Warning })
        val big = DashboardAlerts.compute(null, 60, 12, emptyList(), 0L)
        assertTrue(big.all { it.level == AlertLevel.Critical })
        assertTrue(DashboardAlerts.compute(null, 14, 0, emptyList(), 0L).isEmpty())
    }

    @Test
    fun yesterdaysFallAgainstTheWeekBeforeIsWarnedAbout() {
        // 14 days: the last full day is index 12, compared with the seven before it
        val players = List(12) { 100L } + listOf(60L, 100L)
        val revenue = List(12) { 2_000_000L } + listOf(500_000L, 2_000_000L)
        val a = DashboardAlerts.compute(series(players, revenue), 0, 0, emptyList(), 0L)
        val titles = a.map { it.title }
        assertTrue(titles.toString(), "Dün oyuncu sayısı düştü" in titles)
        assertTrue(titles.toString(), "Dün reklam geliri düştü" in titles)
        // the revenue fell 75%: critical, and the red ones come first
        assertEquals(AlertLevel.Critical, a.first().level)
        assertTrue(a.first { it.title.contains("gelir") }.body.contains("%75"))
    }

    @Test
    fun todayBeingPartlyInDoesNotCountAsAFall() {
        val players = List(13) { 100L } + listOf(5L)
        assertTrue(DashboardAlerts.compute(series(players), 0, 0, emptyList(), 0L).isEmpty())
    }

    @Test
    fun returnRateFallingIsWarnedAbout() {
        val d1 = List(12) { 0.5 as Double? } + listOf(0.25, null)
        val a = DashboardAlerts.compute(series(List(14) { 100L }, d1 = d1), 0, 0, emptyList(), 0L)
        assertEquals(listOf("Ertesi gün dönen oyuncu azaldı"), a.map { it.title })
        assertEquals(AlertLevel.Critical, a.first().level)
    }

    @Test
    fun aJobThatFailedOrWentQuietIsWarnedAbout() {
        val now = 100L * 3_600_000L
        val jobs = listOf(
            JobHealth("Saatlik iş", now - 5 * 3_600_000L, true, "", DashboardAlerts.maxAgeFor("build-global-leaderboard")),
            JobHealth("Gece işi", now - 5 * 3_600_000L, true, "", DashboardAlerts.maxAgeFor("rollup-stats")),
            JobHealth("Bozuk iş", now - 1_000L, false, "quota exceeded", DashboardAlerts.maxAgeFor("rollup-stats")),
            JobHealth("Hiç çalışmamış", 0L, null, "", DashboardAlerts.maxAgeFor("rollup-stats"))
        )
        val a = DashboardAlerts.compute(null, null, null, jobs, now)
        assertEquals(listOf("Bozuk iş: quota exceeded", "Saatlik iş uzun süredir çalışmadı."), a.map { it.body })
        assertEquals(AlertLevel.Critical, a.first().level)
        assertTrue(a.all { it.target == "jobs" })
    }

    @Test
    fun theDropIsAShareOfTheBaselineAndNeverNegative() {
        assertEquals(0.4, DashboardAlerts.drop(60.0, 100.0), 1e-9)
        assertEquals(0.0, DashboardAlerts.drop(150.0, 100.0), 1e-9)
        assertEquals(0.0, DashboardAlerts.drop(10.0, 0.0), 1e-9)
    }

    // ---- the assistant's saved answers

    private fun saved(at: Long) = SavedAnswer("t$at", "text", at)

    @Test
    fun theTwoCopiesMergeIntoTheNewestThirtyWithNoDuplicates() {
        val local = (1L..20L).map(::saved)
        val remote = (10L..40L).map(::saved)
        val merged = AssistantHistory.merge(local, remote)
        assertEquals(AssistantHistory.MAX, merged.size)
        assertEquals(40L, merged.first().at)
        assertEquals(merged.map { it.at }.distinct(), merged.map { it.at })
    }

    @Test
    fun onlyWhatThePhoneAloneHasIsSentUpAndWhatFallsOffIsDropped() {
        assertEquals(listOf(1L, 2L), AssistantHistory.onlyLocal(listOf(saved(1), saved(2), saved(3)), listOf(saved(3), saved(4))).map { it.at })
        val all = (1L..32L).map(::saved)
        assertEquals(listOf(2L, 1L), AssistantHistory.dropped(all).map { it.at })
        assertTrue(AssistantHistory.dropped((1L..30L).map(::saved)).isEmpty())
    }

    @Test
    fun anAnswerSurvivesBeingMadeIntoADocumentAndBack() {
        val a = SavedAnswer("Etkinlik planı", "metin", 1234L, "google/x", 0.012)
        assertEquals(a, AssistantHistory.fromDoc(AssistantHistory.toDoc(a)))
        assertEquals(SavedAnswer("t", "x", 5L), AssistantHistory.fromDoc(mapOf("title" to "t", "text" to "x", "at" to 5)))
        assertEquals(null, AssistantHistory.fromDoc(mapOf("title" to "t")))
    }
}
