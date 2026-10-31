package com.sualtikasifi.cizimhafiza.presentation.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.sp
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Console
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleButtonStyle
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleCard
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleEmpty
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleNotice
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleSkeleton
import com.sualtikasifi.cizimhafiza.presentation.reports.console.NoticeKind
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Overline
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Tag
import com.sualtikasifi.cizimhafiza.presentation.reports.console.noticeKindOf
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
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleTextField
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleButton
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleSurface
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleSecondaryButton
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
        verticalArrangement = Arrangement.spacedBy(Console.Gap)
    ) {
        state.message?.let { ConsoleNotice(it, kind = noticeKindOf(it)) }
        if (!state.loaded) ConsoleSkeleton(rows = 3, rowHeight = 120.dp)
        else if (state.groups.isEmpty()) ConsoleEmpty(Icons.Filled.Report, "Bekleyen oyuncu raporu yok", body = "Oyuncular birini bildirdiğinde burada kişi kişi toplanır.")
        state.groups.forEach { g ->
            ConsoleCard(Modifier.fillMaxWidth(), padding = 12.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(g.nickname.ifBlank { g.uid })
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(g.nickname.ifBlank { g.uid.take(8) }, style = Console.Title, maxLines = 1)
                        Text("son: ${clock(g.latestMillis)}", style = Console.Small)
                    }
                    Tag("${g.count} rapor", fg = Console.Critical, bg = Console.CriticalSoft)
                }
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    g.reasons.entries.forEach { Tag("${reasonLabel(it.key)} ×${it.value}") }
                    g.contexts.forEach { Tag(it, fg = Console.Blue, bg = Console.BlueSoft) }
                }
                Spacer(Modifier.height(6.dp))
                Text(g.uid, style = Console.Small.copy(color = Console.InkMuted, fontFamily = FontFamily.Monospace, fontSize = 11.sp))
                Spacer(Modifier.height(10.dp))
                val busy = state.busyUid != null
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ConsoleSecondaryButton("Adı sıfırla", { viewModel.resetName(g) }, Modifier.weight(1f), enabled = !busy, height = 38.dp)
                    ConsoleButton("Kilitle", { viewModel.lock(g) }, Modifier.weight(1f), enabled = !busy, height = 38.dp, style = ConsoleButtonStyle.DangerSoft)
                    ConsoleButton("Kapat", { viewModel.dismiss(g) }, Modifier.weight(1f), enabled = !busy, height = 38.dp, style = ConsoleButtonStyle.Dark)
                }
            }
        }
    }
}

/** A round initial for a player: their first letter on a tint picked from their name, so the same player keeps a color. */
@Composable
internal fun Avatar(name: String, size: androidx.compose.ui.unit.Dp = 38.dp) {
    val tints = listOf(Console.Accent to Console.AccentSoft, Console.Blue to Console.BlueSoft, Console.Violet to Console.VioletSoft, Console.Teal to Console.TealSoft)
    val (fg, bg) = tints[(name.hashCode() and 0x7fffffff) % tints.size]
    Box(Modifier.size(size).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
        Text(name.trim().take(1).uppercase(java.util.Locale("tr")).ifBlank { "?" }, style = Console.Subtitle.copy(color = fg, fontSize = (size.value * 0.42f).sp))
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
        verticalArrangement = Arrangement.spacedBy(Console.Gap)
    ) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ConsoleTextField(
                value = state.query,
                onValueChange = { v -> viewModel.edit { copy(query = v) } },
                placeholder = "Ad, arkadaş kodu ya da kullanıcı kimliği",
                leading = { Icon(Icons.Filled.Search, contentDescription = null, tint = Console.InkMuted, modifier = Modifier.size(18.dp)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { if (state.query.isNotBlank()) viewModel.search() }),
                modifier = Modifier.weight(1f)
            )
            ConsoleButton("Ara", viewModel::search, enabled = !state.busy && state.query.isNotBlank(), loading = state.busy && state.results.isEmpty(), height = 46.dp)
        }
        state.message?.let { ConsoleNotice(it, kind = noticeKindOf(it)) }
        if (!state.searched && state.results.isEmpty()) {
            ConsoleEmpty(Icons.Filled.Search, "Bir oyuncu ara", body = "Profilini, cezalarını ve raporlarını gör; adını sıfırla, kilitle ya da telafi gönder.")
        } else if (state.searched && state.results.isEmpty()) {
            ConsoleEmpty(Icons.Filled.PersonOff, "Böyle bir oyuncu bulunamadı", body = "Yazımı kontrol et ya da arkadaş kodunu dene.")
        }
        val now = System.currentTimeMillis()
        state.results.forEach { p ->
            val locked = p.lockedUntilMillis > now
            ConsoleCard(Modifier.fillMaxWidth(), padding = 12.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(p.nickname.ifBlank { p.uid }, 42.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.nickname.ifBlank { "(adsız)" }, style = Console.Title, maxLines = 1)
                        Text("Kod ${p.friendCode.ifBlank { "-" }}", style = Console.Small)
                    }
                    if (locked) Tag("Kilitli", fg = Console.Critical, bg = Console.CriticalSoft)
                }
                Spacer(Modifier.height(10.dp))
                // the record, as four figures side by side
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(Console.ControlCorner)).background(Console.SurfaceAlt).padding(vertical = 8.dp)) {
                    Figure("Seviye", p.level.toString(), Modifier.weight(1f))
                    Figure("Bu ay XP", Console.compact(p.periodXp.toDouble()), Modifier.weight(1f))
                    Figure("Ceza", p.strikes.toString(), Modifier.weight(1f), alert = p.strikes > 0)
                    Figure("Rapor", p.reports.toString(), Modifier.weight(1f), alert = p.reports > 0)
                }
                if (locked) {
                    Spacer(Modifier.height(8.dp))
                    ConsoleNotice("${clock(p.lockedUntilMillis)} tarihine kadar kilitli", kind = NoticeKind.Error)
                }
                Spacer(Modifier.height(6.dp))
                Text(p.uid, style = Console.Small.copy(color = Console.InkMuted, fontFamily = FontFamily.Monospace, fontSize = 11.sp))
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ConsoleSecondaryButton("Adı sıfırla", { viewModel.resetName(p) }, Modifier.weight(1f), enabled = !state.busy, height = 38.dp)
                    if (locked) {
                        ConsoleButton("Kilidi aç", { viewModel.unlock(p) }, Modifier.weight(1f), enabled = !state.busy, height = 38.dp, style = ConsoleButtonStyle.Tonal)
                    } else {
                        ConsoleButton("Kilitle", { viewModel.lock(p) }, Modifier.weight(1f), enabled = !state.busy, height = 38.dp, style = ConsoleButtonStyle.DangerSoft)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(Console.Grid))
                Spacer(Modifier.height(10.dp))
                Overline("Telafi gönder")
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ConsoleTextField(
                        value = state.gold, onValueChange = { v -> viewModel.edit { copy(gold = v.filter(Char::isDigit).take(6)) } },
                        placeholder = "Altın", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f)
                    )
                    ConsoleTextField(
                        value = state.xp, onValueChange = { v -> viewModel.edit { copy(xp = v.filter(Char::isDigit).take(6)) } },
                        placeholder = "XP", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                ConsoleTextField(
                    value = state.note, onValueChange = { v -> viewModel.edit { copy(note = v.take(60)) } },
                    placeholder = "Not (denetim kaydına yazılır)", modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                ConsoleButton(
                    "Telafiyi gönder", { viewModel.grant(p) }, Modifier.fillMaxWidth(),
                    enabled = !state.busy && ((state.gold.toIntOrNull() ?: 0) + (state.xp.toIntOrNull() ?: 0)) > 0,
                    icon = Icons.Filled.Redeem, height = 40.dp
                )
            }
        }
    }
}

/** One labelled figure in a row of them. */
@Composable
private fun Figure(label: String, value: String, modifier: Modifier = Modifier, alert: Boolean = false) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = Console.Subtitle.copy(fontSize = 16.sp, color = if (alert) Console.Critical else Console.Ink))
        Text(label, style = Console.Small.copy(fontSize = 11.sp))
    }
}
