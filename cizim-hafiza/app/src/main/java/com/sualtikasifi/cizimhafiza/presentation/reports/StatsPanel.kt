package com.sualtikasifi.cizimhafiza.presentation.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ChartPoint
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ColumnChart
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Console
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleCard
import com.sualtikasifi.cizimhafiza.presentation.reports.console.FunnelBars
import com.sualtikasifi.cizimhafiza.presentation.reports.console.FunnelRow
import com.sualtikasifi.cizimhafiza.presentation.reports.console.LineChart
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Meter
import com.sualtikasifi.cizimhafiza.presentation.reports.console.SectionHeader
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Segmented
import com.sualtikasifi.cizimhafiza.presentation.reports.console.StatTile
import java.util.Locale

/**
 * Developer panel "İstatistik": players, opens, games and ads day by day, as charts. Touch a chart
 * to read a single day. Every number comes from AdminStats (see util/AdminStats.kt).
 */
@Composable
fun StatsPanel(viewModel: StatsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { if (!state.loaded && !state.loading) viewModel.load() }
    val s = state.series

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("İstatistik", style = Console.Title)
                    Text("Gün sınırı UTC (Türkiye saatiyle 03:00)", style = Console.Small)
                }
                if (state.loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = Console.Accent)
                else IconButton(onClick = viewModel::load) { Icon(Icons.Filled.Refresh, contentDescription = "Yenile", tint = Console.InkSecondary) }
            }
            Spacer(Modifier.height(8.dp))
            Segmented(
                options = listOf("7" to "7 gün", "30" to "30 gün", "90" to "90 gün"),
                selected = state.days.toString(),
                onSelect = { viewModel.selectDays(it.toInt()) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (state.failed) {
            item {
                ConsoleCard(Modifier.fillMaxWidth()) {
                    Text("Okunamadı", style = Console.Subtitle)
                    Text("Geliştirici hesabıyla giriş yapıldığından ve internet olduğundan emin ol.", style = Console.Small)
                }
            }
        }
        if (s == null) {
            if (!state.failed) item { Row(Modifier.fillMaxWidth().padding(32.dp)) { CircularProgressIndicator(color = Console.Accent) } }
            return@LazyColumn
        }
        val all = s.days.indices
        val playerDays = s.players.sum()
        val avg = playerDays.toDouble() / s.size
        val games = s.sumOf(all) { s.games(it) }
        val opens = s.sumOf(all) { s.opens(it) }
        val req = s.sumOf(all) { s.adRequested(it) }
        val earned = s.sumOf(all) { s.adEarned(it) }
        val inters = s.sumOf(all) { s.interstitials(it) }
        val rev = s.sumOf(all) { s.revenueMicros(it) }
        val cur = s.currency()
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Günlük ort. oyuncu", Console.compact(avg), Modifier.weight(1f), changeCaption = "en yüksek ${s.players.maxOrNull() ?: 0}")
                StatTile("Toplam oyun", Console.compact(games.toDouble()), Modifier.weight(1f), changeCaption = "oyuncu-gün başı " + String.format(Locale("tr"), "%.1f", if (playerDays > 0) games.toDouble() / playerDays else 0.0))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Uygulama açılışı", Console.compact(opens.toDouble()), Modifier.weight(1f), changeCaption = "oyuncu-gün başı " + String.format(Locale("tr"), "%.1f", if (playerDays > 0) opens.toDouble() / playerDays else 0.0))
                StatTile(
                    "Reklam geliri",
                    if (cur != null) String.format(Locale("tr"), "%.2f %s", rev / 1_000_000.0, cur) else "—",
                    Modifier.weight(1f),
                    changeCaption = if (cur != null) "oyuncu-gün başı " + String.format(Locale("tr"), "%.3f", if (playerDays > 0) rev / 1_000_000.0 / playerDays else 0.0) else "AdMob gösterim geliri açık olmalı"
                )
            }
        }
        // Filled in by the nightly roll-up; days it has not covered yet are skipped.
        val newDays = s.newPlayers.filterNotNull()
        val d1 = s.d1Return.filterNotNull()
        val d7 = s.d7Return.filterNotNull()
        if (newDays.isNotEmpty() || d1.isNotEmpty()) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile("Yeni oyuncu", Console.compact(newDays.sum().toDouble()), Modifier.weight(1f), changeCaption = "son 30 günde görülmemiş")
                    StatTile(
                        "Ertesi gün dönen",
                        if (d1.isNotEmpty()) String.format(Locale("tr"), "%%%.0f", d1.average() * 100) else "—",
                        Modifier.weight(1f),
                        changeCaption = if (d7.isNotEmpty()) String.format(Locale("tr"), "7. gün: %%%.0f", d7.average() * 100) else "7. gün: veri yok"
                    )
                }
            }
        }
        item {
            ConsoleCard(Modifier.fillMaxWidth()) {
                SectionHeader("Aktif oyuncu", subtitle = "Gün içinde uygulamayı açan farklı oyuncu")
                LineChart(all.map { ChartPoint(s.label(it), s.players[it].toDouble()) })
            }
        }
        // Which modes the finished games came from, and how the tutorial ends.
        val byMode = HashMap<String, Long>()
        s.counts.forEach { day -> day.forEach { (k, v) -> if (k.startsWith("games__finished__")) byMode.merge(k.removePrefix("games__finished__"), v, Long::plus) } }
        val tutDone = s.sumOf(all) { s.counts[it]["tutorial__finished__count"] ?: 0L }
        val tutSkip = s.sumOf(all) { s.counts[it]["tutorial__skipped__count"] ?: 0L }
        if (byMode.isNotEmpty() || tutDone + tutSkip > 0) {
            item {
                ConsoleCard(Modifier.fillMaxWidth()) {
                    SectionHeader("Kullanım", subtitle = "Moda göre biten oyun ve eğitim sonu")
                    byMode.entries.sortedByDescending { it.value }.take(8).forEach { (mode, n) ->
                        Text("${mode.replace('_', ' ')}: $n", style = Console.Small)
                    }
                    if (tutDone + tutSkip > 0) Text("Eğitim: $tutDone bitirdi · $tutSkip atladı", style = Console.Small)
                }
            }
        }
        item {
            ConsoleCard(Modifier.fillMaxWidth()) {
                SectionHeader("Biten oyun", subtitle = "Sonuç ekranına ulaşan her oyun")
                ColumnChart(all.map { ChartPoint(s.label(it), s.games(it).toDouble()) })
            }
        }
        item {
            ConsoleCard(Modifier.fillMaxWidth()) {
                SectionHeader("Uygulama açılışı")
                LineChart(all.map { ChartPoint(s.label(it), s.opens(it).toDouble()) }, color = Console.Blue)
            }
        }
        if (cur != null) {
            item {
                ConsoleCard(Modifier.fillMaxWidth()) {
                    SectionHeader("Reklam geliri", subtitle = "AdMob'un gösterim başına bildirdiği tutar, $cur")
                    ColumnChart(all.map { ChartPoint(s.label(it), s.revenueMicros(it) / 1_000_000.0) }, color = Console.Accent, format = { String.format(Locale("tr"), "%.2f %s", it, cur) })
                }
            }
        }
        item {
            ConsoleCard(Modifier.fillMaxWidth()) {
                SectionHeader("Ödüllü reklamlar", subtitle = "İstenen reklamın ne kadarı sonuna kadar izlendi")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (req > 0) Console.percent(earned.toDouble() / req) else "—", style = Console.Value)
                    Spacer(Modifier.size(10.dp))
                    Text("$earned izlendi · $req istek · $inters geçiş reklamı", style = Console.Small)
                }
                Spacer(Modifier.height(8.dp))
                Meter(if (req > 0) earned.toFloat() / req else 0f)
                Spacer(Modifier.height(16.dp))
                val rewarded = state.placements.filter { it.requested > 0 }
                if (rewarded.isEmpty()) Text("Bu aralıkta ödüllü reklam isteği yok.", style = Console.Small)
                else FunnelBars(
                    rows = rewarded.map { p ->
                        FunnelRow(
                            label = placementLabel(p.placement),
                            requested = p.requested,
                            earned = p.earned,
                            note = if (p.requested > 0) Console.percent(p.earned.toDouble() / p.requested) else null
                        )
                    },
                    firstLabel = "İstek",
                    secondLabel = "İzlendi"
                )
            }
        }
        val shown = state.placements.filter { it.shown > 0 }
        if (shown.isNotEmpty()) {
            item {
                ConsoleCard(Modifier.fillMaxWidth()) {
                    SectionHeader("Geçiş reklamları", subtitle = "Ekrana gelen")
                    shown.forEach { p ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(placementLabel(p.placement), style = Console.Body, modifier = Modifier.weight(1f))
                            Text(Console.compact(p.shown.toDouble()), style = Console.Subtitle)
                        }
                    }
                }
            }
        }
    }
}

/** Developer-facing names for the placement ids AdManager's call sites pass. */
internal fun placementLabel(placement: String): String = when (placement) {
    "chest_speedup" -> "Sandık hızlandırma"
    "drawing_time_bonus" -> "Çizim süre bonusu"
    "drawing_time_bonus_online" -> "Çizim süre bonusu (online)"
    "guess_hint" -> "Tahmin ipucu"
    "guess_hint_online" -> "Tahmin ipucu (online)"
    "result_xp_x2" -> "Sonuç ekranı 2x XP"
    "interstitial_daily" -> "Geçiş: Günlük meydan okuma"
    "interstitial_level" -> "Geçiş: Bölümler"
    "interstitial_result_solo" -> "Geçiş: Tek oyun sonucu"
    "interstitial_result_online" -> "Geçiş: Online sonuç"
    "store_daily_joker" -> "Mağaza günlük joker"
    "home_gold" -> "Ana ekran: altın"
    "home_free_chest" -> "Ana ekran: ücretsiz kasa"
    "streak_rescue" -> "Seri kurtarma"
    else -> placement
}
