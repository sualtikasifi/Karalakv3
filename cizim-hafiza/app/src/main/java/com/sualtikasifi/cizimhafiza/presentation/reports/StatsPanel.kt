package com.sualtikasifi.cizimhafiza.presentation.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.presentation.common.RaisedCard
import com.sualtikasifi.cizimhafiza.presentation.common.SecondaryButton
import com.sualtikasifi.cizimhafiza.presentation.common.SelectableChip
import java.util.Locale

/** Developer panel "İstatistik" tab: players, opens, games and per-button ad results from AdminStats. */
@Composable
fun StatsPanel(viewModel: StatsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { if (!state.loaded && !state.loading) viewModel.load() }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.stats_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(1 to R.string.stats_range_1, 7 to R.string.stats_range_7, 30 to R.string.stats_range_30)
                .forEach { (days, label) ->
                    SelectableChip(
                        label = stringResource(label),
                        selected = state.days == days,
                        onClick = { viewModel.selectDays(days) },
                        modifier = Modifier.weight(1f),
                        verticalPadding = 8.dp,
                        style = MaterialTheme.typography.bodySmall,
                        fillWidth = true
                    )
                }
        }
        when {
            state.loading && !state.loaded -> Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) { CircularProgressIndicator(modifier = Modifier.padding(top = 32.dp)) }

            state.failed -> Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = stringResource(R.string.reports_load_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                SecondaryButton(text = stringResource(R.string.stats_refresh), onClick = viewModel::load)
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    val today = state.players.firstOrNull()?.players ?: 0L
                    val avg = if (state.players.isEmpty()) 0L else state.players.sumOf { it.players } / state.players.size
                    RaisedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            StatLine(stringResource(R.string.stats_players_today), today.toString())
                            StatLine(stringResource(R.string.stats_players_avg), avg.toString())
                            StatLine(stringResource(R.string.stats_opens), state.appOpens.toString())
                            StatLine(stringResource(R.string.stats_games), state.gamesFinished.toString())
                            StatLine(stringResource(R.string.stats_revenue), money(state.revenueMicros))
                        }
                    }
                }
                item {
                    Text(
                        text = stringResource(R.string.stats_ads_title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                if (state.placements.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.stats_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(state.placements, key = { it.placement }) { row ->
                    RaisedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(placementLabel(row.placement), style = MaterialTheme.typography.titleSmall)
                                Text(money(row.revenueMicros), style = MaterialTheme.typography.titleSmall)
                            }
                            val detail = if (row.requested > 0 || row.shown == 0L) {
                                val rate = if (row.requested > 0) (row.earned * 100 / row.requested).toInt() else 0
                                stringResource(
                                    R.string.stats_ad_rewarded,
                                    row.requested, row.earned, rate, row.skipped, row.unavailable
                                )
                            } else {
                                stringResource(R.string.stats_ad_interstitial, row.shown)
                            }
                            Text(
                                text = detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}

private fun money(micros: Long): String = String.format(Locale.US, "$%.2f", micros / 1_000_000.0)

/** Developer-facing names for the placement ids AdManager's call sites pass. */
private fun placementLabel(placement: String): String = when (placement) {
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
