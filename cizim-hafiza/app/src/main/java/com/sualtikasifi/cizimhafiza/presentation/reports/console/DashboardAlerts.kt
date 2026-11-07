package com.sualtikasifi.cizimhafiza.presentation.reports.console

import java.util.Locale

enum class AlertLevel { Warning, Critical }

/** One thing the front page warns about. [target] says where a tap goes: "queue", "reports", "stats" or "jobs". */
data class DashAlert(val level: AlertLevel, val title: String, val body: String, val target: String)

/** How one scheduled job looks from its last run (adminJobs/{name}); [maxAgeMillis] is how long it may stay quiet. */
data class JobHealth(val label: String, val lastRunMillis: Long, val ok: Boolean?, val error: String, val maxAgeMillis: Long)

/**
 * What the Pano warns about, worked out from the numbers it already has: a queue that has piled up, drawings reported and not
 * judged, a fall in the day before's players, ad revenue or returning players, and a scheduled job that failed or stopped
 * running. The last full day is yesterday (today is only partly in); it is compared with the seven days before it.
 */
object DashboardAlerts {
    const val QUEUE_WARN = 15L
    const val QUEUE_CRITICAL = 50L
    const val REPORTS_CRITICAL = 10L
    const val DROP_WARN = 0.30
    const val DROP_CRITICAL = 0.50

    /** Jobs that run every hour may be quiet for a few; the nightly ones for a day and a half. */
    private const val HOURLY_MS = 4 * 3_600_000L
    private const val NIGHTLY_MS = 36 * 3_600_000L
    private val HOURLY = setOf("build-global-leaderboard", "process-admin-commands")

    fun maxAgeFor(job: String): Long = if (job in HOURLY) HOURLY_MS else NIGHTLY_MS

    /** How far [now] is below [baseline], 0..1 (0 when it is not below, or there is nothing to compare with). */
    fun drop(now: Double, baseline: Double): Double = if (baseline <= 0.0) 0.0 else ((baseline - now) / baseline).coerceIn(0.0, 1.0)

    private fun level(drop: Double): AlertLevel? = when {
        drop >= DROP_CRITICAL -> AlertLevel.Critical
        drop >= DROP_WARN -> AlertLevel.Warning
        else -> null
    }

    private fun pct(d: Double) = "%" + Math.round(d * 100)

    fun compute(
        series: DaySeries?, pendingQueue: Long?, openReports: Long?, jobs: List<JobHealth>, now: Long
    ): List<DashAlert> {
        val out = mutableListOf<DashAlert>()
        pendingQueue?.takeIf { it >= QUEUE_WARN }?.let {
            out += DashAlert(
                if (it >= QUEUE_CRITICAL) AlertLevel.Critical else AlertLevel.Warning,
                "Onay kuyruğu birikti", "$it tur karar bekliyor.", "queue"
            )
        }
        openReports?.takeIf { it > 0 }?.let {
            out += DashAlert(
                if (it >= REPORTS_CRITICAL) AlertLevel.Critical else AlertLevel.Warning,
                "Çizim şikayeti bekliyor", "$it şikayet karara bağlanmayı bekliyor.", "reports"
            )
        }
        if (series != null && series.size >= 9) {
            val y = series.size - 2
            val before = (series.size - 9) until (series.size - 2)
            val players = series.players[y].toDouble()
            val playersBase = before.sumOf { series.players[it] } / 7.0
            level(drop(players, playersBase))?.let {
                out += DashAlert(it, "Dün oyuncu sayısı düştü", "Dün ${players.toLong()}; önceki 7 günün ortalaması ${Math.round(playersBase)} (${pct(drop(players, playersBase))} düşüş).", "stats")
            }
            val revenue = series.revenueMicros(y) / 1_000_000.0
            val revenueBase = before.sumOf { series.revenueMicros(it) } / 7.0 / 1_000_000.0
            val cur = series.currency().orEmpty()
            level(drop(revenue, revenueBase))?.let {
                out += DashAlert(
                    it, "Dün reklam geliri düştü",
                    "Dün ${String.format(Locale.US, "%.2f", revenue)} $cur; önceki 7 günün ortalaması ${String.format(Locale.US, "%.2f", revenueBase)} $cur (${pct(drop(revenue, revenueBase))} düşüş).".replace("  ", " "),
                    "stats"
                )
            }
            val returns = series.d1Return.getOrNull(y)
            val returnBase = before.mapNotNull { series.d1Return.getOrNull(it) }.takeIf { it.size >= 3 }?.average()
            if (returns != null && returnBase != null) {
                level(drop(returns, returnBase))?.let {
                    out += DashAlert(it, "Ertesi gün dönen oyuncu azaldı", "Dünün dönüş oranı ${pct(returns)}; önceki 7 günün ortalaması ${pct(returnBase)}.", "stats")
                }
            }
        }
        jobs.forEach { j ->
            when {
                j.ok == false -> out += DashAlert(AlertLevel.Critical, "Arka plan işi hata verdi", "${j.label}: ${j.error.take(100).ifBlank { "hata" }}", "jobs")
                j.lastRunMillis > 0 && now - j.lastRunMillis > j.maxAgeMillis -> out += DashAlert(AlertLevel.Warning, "Arka plan işi çalışmıyor", "${j.label} uzun süredir çalışmadı.", "jobs")
            }
        }
        return out.sortedByDescending { it.level.ordinal }
    }
}
