package com.sualtikasifi.cizimhafiza.presentation.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sualtikasifi.cizimhafiza.presentation.common.AppTextField
import com.sualtikasifi.cizimhafiza.presentation.common.PrimaryButton
import com.sualtikasifi.cizimhafiza.presentation.common.RaisedCard
import com.sualtikasifi.cizimhafiza.presentation.common.SecondaryButton
import com.sualtikasifi.cizimhafiza.util.PlayerAdmin
import com.sualtikasifi.cizimhafiza.util.PlayerInfo
import com.sualtikasifi.cizimhafiza.util.PlayerReportGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

private fun clock(millis: Long): String = SimpleDateFormat("d MMM HH:mm", Locale("tr")).format(Date(millis))

private fun reasonLabel(reason: String): String = when (reason) {
    "NAME" -> "Ad"
    "BEHAVIOUR" -> "Davranış"
    "CHEATING" -> "Hile"
    else -> "Diğer"
}

// ---------------------------------------------------------------- player reports

data class PlayerReportsState(
    val groups: List<PlayerReportGroup> = emptyList(),
    val loaded: Boolean = false,
    val busyUid: String? = null,
    val message: String? = null
)

@HiltViewModel
class PlayerReportsViewModel @Inject constructor(private val admin: PlayerAdmin) : ViewModel() {
    private val _state = MutableStateFlow(PlayerReportsState())
    val state: StateFlow<PlayerReportsState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            admin.reports()
                .onSuccess { g -> _state.update { it.copy(groups = g, loaded = true, message = null) } }
                .onFailure { e -> _state.update { it.copy(loaded = true, message = "Raporlar yüklenemedi: ${e.message}") } }
        }
    }

    private fun act(group: PlayerReportGroup, done: String, block: suspend () -> Result<*>) {
        if (_state.value.busyUid != null) return
        _state.update { it.copy(busyUid = group.uid, message = null) }
        viewModelScope.launch {
            val result = block()
            _state.update {
                it.copy(
                    busyUid = null,
                    message = if (result.isSuccess) done else "Olmadı: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    fun resetName(g: PlayerReportGroup) = act(g, "Ad sıfırlandı.") { admin.resetName(g.uid, g.nickname) }
    fun lock(g: PlayerReportGroup) = act(g, "Oyuncu kilitlendi.") { admin.lock(g.uid) }
    fun dismiss(g: PlayerReportGroup) {
        act(g, "Rapor kapatıldı.") { admin.dismissReports(g) }
        viewModelScope.launch { _state.update { s -> s.copy(groups = s.groups.filterNot { it.uid == g.uid }) } }
    }
}

/** "Oyuncu raporları": the players other players reported, most reported first, with the three things a reviewer does. */
@Composable
internal fun PlayerReportsPanel(viewModel: PlayerReportsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        state.message?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) }
        if (state.loaded && state.groups.isEmpty()) {
            Text("Bekleyen oyuncu raporu yok.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        state.groups.forEach { g ->
            RaisedCard(corner = 20.dp, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "${g.nickname.ifBlank { g.uid.take(8) }} · ${g.count} rapor",
                        style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        g.reasons.entries.joinToString("  ") { "${reasonLabel(it.key)} ×${it.value}" } +
                            (if (g.contexts.isNotEmpty()) "  ·  ${g.contexts.joinToString(", ")}" else "") +
                            "  ·  son: ${clock(g.latestMillis)}",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(g.uid, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    val busy = state.busyUid != null
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        SecondaryButton("Adı sıfırla", { viewModel.resetName(g) }, Modifier.weight(1f), enabled = !busy, height = 44.dp)
                        SecondaryButton("Kilitle", { viewModel.lock(g) }, Modifier.weight(1f), enabled = !busy, height = 44.dp)
                        PrimaryButton("Kapat", { viewModel.dismiss(g) }, Modifier.weight(1f), enabled = !busy, height = 44.dp)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- player lookup

data class PlayersState(
    val query: String = "",
    val results: List<PlayerInfo> = emptyList(),
    val searched: Boolean = false,
    val busy: Boolean = false,
    val message: String? = null,
    val gold: String = "",
    val xp: String = "",
    val note: String = ""
)

@HiltViewModel
class PlayersViewModel @Inject constructor(private val admin: PlayerAdmin) : ViewModel() {
    private val _state = MutableStateFlow(PlayersState())
    val state: StateFlow<PlayersState> = _state.asStateFlow()

    fun edit(block: PlayersState.() -> PlayersState) = _state.update { it.block() }

    fun search() {
        val q = _state.value.query
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            admin.find(q)
                .onSuccess { r -> _state.update { it.copy(busy = false, results = r, searched = true) } }
                .onFailure { e -> _state.update { it.copy(busy = false, message = e.message ?: "Aranamadı.") } }
        }
    }

    private fun act(done: String, block: suspend () -> Result<*>) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            val r = block()
            _state.update { it.copy(busy = false, message = if (r.isSuccess) done else "Olmadı: ${r.exceptionOrNull()?.message}") }
            if (r.isSuccess) admin.find(_state.value.query).onSuccess { list -> _state.update { it.copy(results = list) } }
        }
    }

    fun resetName(p: PlayerInfo) = act("Ad sıfırlandı.") { admin.resetName(p.uid, p.nickname) }
    fun lock(p: PlayerInfo) = act("Oyuncu kilitlendi.") { admin.lock(p.uid) }
    fun unlock(p: PlayerInfo) = act("Kilitler kaldırıldı.") { admin.unlock(p.uid) }
    fun grant(p: PlayerInfo) {
        val s = _state.value
        act("Telafi sıraya alındı: oyuncunun uygulaması bir sonraki açılışında (en geç birkaç saat içinde) alır.") {
            admin.grant(p.uid, s.gold.toIntOrNull() ?: 0, s.xp.toIntOrNull() ?: 0, s.note.ifBlank { "telafi" })
        }
    }
}

/** "Oyuncu ara": find an account by name, friend code or uid, see its record, and act on it. */
@Composable
internal fun PlayersPanel(viewModel: PlayersViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AppTextField(
            value = state.query,
            onValueChange = { v -> viewModel.edit { copy(query = v) } },
            label = "Ad, arkadaş kodu ya da kullanıcı kimliği",
            modifier = Modifier.fillMaxWidth()
        )
        PrimaryButton("Ara", viewModel::search, Modifier.fillMaxWidth(), enabled = !state.busy && state.query.isNotBlank(), height = 48.dp)
        state.message?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) }
        if (state.searched && state.results.isEmpty()) {
            Text("Böyle bir oyuncu bulunamadı.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val now = System.currentTimeMillis()
        state.results.forEach { p ->
            RaisedCard(corner = 20.dp, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(p.nickname.ifBlank { "(adsız)" }, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "Seviye ${p.level} · bu ay ${p.periodXp} XP · kod ${p.friendCode.ifBlank { "-" }}",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Ceza: ${p.strikes} · rapor: ${p.reports}" +
                            if (p.lockedUntilMillis > now) " · KİLİTLİ (${clock(p.lockedUntilMillis)}'e kadar)" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (p.lockedUntilMillis > now) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(p.uid, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        SecondaryButton("Adı sıfırla", { viewModel.resetName(p) }, Modifier.weight(1f), enabled = !state.busy, height = 44.dp)
                        if (p.lockedUntilMillis > now) {
                            SecondaryButton("Kilidi aç", { viewModel.unlock(p) }, Modifier.weight(1f), enabled = !state.busy, height = 44.dp)
                        } else {
                            SecondaryButton("Kilitle", { viewModel.lock(p) }, Modifier.weight(1f), enabled = !state.busy, height = 44.dp)
                        }
                    }
                    Text("Telafi", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        AppTextField(
                            value = state.gold, onValueChange = { v -> viewModel.edit { copy(gold = v.filter(Char::isDigit).take(6)) } },
                            label = "Altın", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f)
                        )
                        AppTextField(
                            value = state.xp, onValueChange = { v -> viewModel.edit { copy(xp = v.filter(Char::isDigit).take(6)) } },
                            label = "XP", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f)
                        )
                    }
                    AppTextField(
                        value = state.note, onValueChange = { v -> viewModel.edit { copy(note = v.take(60)) } },
                        label = "Not (denetim kaydına)", modifier = Modifier.fillMaxWidth()
                    )
                    PrimaryButton(
                        "Telafiyi gönder", { viewModel.grant(p) }, Modifier.fillMaxWidth(),
                        enabled = !state.busy && ((state.gold.toIntOrNull() ?: 0) + (state.xp.toIntOrNull() ?: 0)) > 0, height = 44.dp
                    )
                }
            }
        }
    }
}
