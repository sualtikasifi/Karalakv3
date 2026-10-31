package com.sualtikasifi.cizimhafiza.presentation.reports.console

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The panel's front page: today against yesterday, two weeks of players, what is waiting for a
 * decision, and the special days coming up (each one a tap away from an event plan).
 */
@Composable
fun DashboardPanel(
    onOpenQueue: () -> Unit,
    onOpenTool: (String) -> Unit,
    onOpenStats: () -> Unit,
    onPlanEvent: (SpecialDay) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val st by viewModel.state.collectAsState()
    val s = st.series
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Console.Gap),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // the screen's name is in the header; here only the day, and a way to read it all again
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    LocalDate.now().format(DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", Locale("tr"))),
                    style = Console.Label,
                    modifier = Modifier.weight(1f)
                )
                if (st.loading) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Console.Accent)
                else ConsoleIconButton(Icons.Filled.Refresh, "Yenile", viewModel::load, size = 32.dp)
            }
        }
        if (st.loading && s == null) {
            item { ConsoleSkeleton(rows = 2, rowHeight = 88.dp) }
        }
        if (st.failed && s == null) {
            item {
                ConsoleNotice("İstatistikler okunamadı. Bu cihaz geliştirici hesabıyla giriş yapmamış olabilir ya da internet yok.", kind = NoticeKind.Warning)
            }
        }
        if (s != null && s.size >= 2) {
            val t = s.size - 1
            val y = s.size - 2
            fun change(now: Long, before: Long): Double? = if (before <= 0L) null else (now - before).toDouble() / before
            val last7 = (s.size - 7 until s.size)
            val prev7 = (s.size - 14 until s.size - 7)
            val avg7 = s.sumOf(last7) { s.players[it] } / 7.0
            val avgPrev7 = s.sumOf(prev7) { s.players[it] } / 7.0
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(Console.Gap)) {
                    StatTile(
                        "Bugün oyuncu", Console.compact(s.players[t].toDouble()), Modifier.weight(1f),
                        change = change(s.players[t], s.players[y]), changeCaption = "düne göre",
                        trend = s.players.map { it.toFloat() }, onClick = onOpenStats
                    )
                    StatTile(
                        "7 gün ort. oyuncu", Console.compact(avg7), Modifier.weight(1f),
                        change = if (avgPrev7 > 0) (avg7 - avgPrev7) / avgPrev7 else null, changeCaption = "önceki 7 gün",
                        onClick = onOpenStats
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(Console.Gap)) {
                    StatTile(
                        "Bugün oyun", Console.compact(s.games(t).toDouble()), Modifier.weight(1f),
                        change = change(s.games(t), s.games(y)), changeCaption = "düne göre",
                        trend = (0..t).map { s.games(it).toFloat() }, trendColor = Console.Blue, onClick = onOpenStats
                    )
                    val perPlayer = if (s.players[t] > 0) s.games(t).toDouble() / s.players[t] else 0.0
                    StatTile(
                        "Oyuncu başı oyun", String.format(Locale("tr"), "%.1f", perPlayer), Modifier.weight(1f),
                        changeCaption = "bugün, derinlik ölçüsü", onClick = onOpenStats
                    )
                }
            }
            item {
                val req = s.adRequested(t)
                val earned = s.adEarned(t)
                val rev = s.revenueMicros(t)
                val cur = s.currency()
                Row(horizontalArrangement = Arrangement.spacedBy(Console.Gap)) {
                    StatTile(
                        "Ödüllü reklam izlendi", if (req > 0) Console.percent(earned.toDouble() / req) else "—", Modifier.weight(1f),
                        changeCaption = "$earned / $req istek", onClick = onOpenStats
                    )
                    StatTile(
                        "Reklam geliri (bugün)",
                        if (cur != null) String.format(Locale("tr"), "%.2f %s", rev / 1_000_000.0, cur) else "—",
                        Modifier.weight(1f),
                        change = change(rev, s.revenueMicros(y)), changeCaption = if (cur == null) "AdMob gelir verisi bekleniyor" else "düne göre",
                        onClick = onOpenStats
                    )
                }
            }
            item {
                ConsoleCard(Modifier.fillMaxWidth(), onClick = onOpenStats) {
                    SectionHeader("Aktif oyuncular", subtitle = "Son 14 gün · dokunup gün seç")
                    LineChart(s.days.indices.map { ChartPoint(s.label(it), s.players[it].toDouble()) })
                }
            }
        }
        item {
            SectionHeader("Bekleyen işler")
            ConsoleCard(Modifier.fillMaxWidth(), padding = 4.dp) {
                WorkRow(Icons.Filled.Inbox, "Onay kuyruğu", "Hızlı Eşleş'e girmeyi bekleyen turlar", st.pendingQueue, onOpenQueue)
                WorkRow(Icons.Filled.Today, "Günlük meydan okuma", "İncelenecek günlük denemeler", st.pendingDaily) { onOpenTool("daily") }
                WorkRow(Icons.Filled.Flag, "Çizim şikayetleri", "Oyuncuların bildirdiği çizimler", st.openReports) { onOpenTool("reports") }
                WorkRow(Icons.Filled.Style, "Okunmamış geri bildirim", "Öneri ve şikayetler", st.unseenFeedback?.toLong()) { onOpenTool("feedback") }
            }
        }
        item {
            SectionHeader("Yaklaşan özel günler", subtitle = "Dokun: yapay zeka etkinlik planı hazırlasın")
        }
        st.special.take(5).forEach { day ->
            item {
                ConsoleCard(Modifier.fillMaxWidth(), padding = 12.dp, onClick = { onPlanEvent(day) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Console.AccentSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(day.date.dayOfMonth.toString(), style = Console.Subtitle.copy(color = Console.Accent, fontWeight = FontWeight.Bold))
                                Text(day.date.format(DateTimeFormatter.ofPattern("MMM", Locale("tr"))), style = Console.Tick.copy(color = Console.Accent))
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(day.name + if (day.estimated) " (tahmini)" else "", style = Console.Subtitle)
                            Text(day.angle, style = Console.Small, maxLines = 2)
                        }
                        Spacer(Modifier.width(8.dp))
                        val left = day.daysLeft()
                        Tag(if (left == 0L) "bugün" else "$left gün", fg = if (left <= 14) Console.Accent else Console.InkSecondary, bg = if (left <= 14) Console.AccentSoft else Console.Grid)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Console.Accent, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Etkinlik planı hazırla", style = Console.Label.copy(color = Console.Accent))
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkRow(icon: ImageVector, title: String, subtitle: String, count: Long?, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Console.InkSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = Console.Subtitle)
            Text(subtitle, style = Console.Small)
        }
        when {
            count == null -> Text("—", style = Console.Small)
            count > 0 -> Tag(count.toString(), fg = androidx.compose.ui.graphics.Color.White, bg = Console.Accent)
            else -> Tag("0", fg = Console.GoodText, bg = Console.GoodSoft)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Console.InkMuted)
    }
}

