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
import com.sualtikasifi.cizimhafiza.util.CouponUse
import com.sualtikasifi.cizimhafiza.util.PlayerDetails
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
        else if (state.groups.isEmpty() && state.message == null) ConsoleEmpty(Icons.Filled.Report, "Bekleyen oyuncu raporu yok", body = "Oyuncular birini bildirdiğinde burada kişi kişi toplanır.")
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
    val note: String = "",
    /** The account whose card is open (the only one, or the one tapped), and what was read about it. */
    val selected: String? = null,
    val details: Map<String, PlayerDetails> = emptyMap(),
    val loadingDetails: Boolean = false,
    /** Coupons an account redeemed, read only when asked for. */
    val couponUses: Map<String, List<CouponUse>> = emptyMap(),
    val loadingCoupons: Boolean = false
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
                .onSuccess { r ->
                    _state.update { it.copy(busy = false, results = r, searched = true, selected = null, details = emptyMap(), couponUses = emptyMap()) }
                    // one hit opens its card at once; several are a list to choose from
                    if (r.size == 1) open(r.first().uid)
                }
                .onFailure { e -> _state.update { it.copy(busy = false, message = e.message ?: "Aranamadı.") } }
        }
    }

    /** Opens (or closes, when it is already open) one account's card, reading its record the first time. */
    fun open(uid: String) {
        if (_state.value.selected == uid) {
            _state.update { it.copy(selected = null) }
            return
        }
        _state.update { it.copy(selected = uid) }
        if (uid !in _state.value.details) loadDetails(uid)
    }

    private fun loadDetails(uid: String) {
        _state.update { it.copy(loadingDetails = true) }
        viewModelScope.launch {
            val d = admin.details(uid)
            _state.update { it.copy(loadingDetails = false, details = it.details + (uid to d)) }
        }
    }

    fun loadCoupons(uid: String) {
        if (_state.value.loadingCoupons) return
        _state.update { it.copy(loadingCoupons = true) }
        viewModelScope.launch {
            admin.couponUses(uid)
                .onSuccess { list -> _state.update { it.copy(loadingCoupons = false, couponUses = it.couponUses + (uid to list)) } }
                .onFailure { e -> _state.update { it.copy(loadingCoupons = false, message = "Kuponlar okunamadı: ${e.message}") } }
        }
    }

    private fun act(uid: String, done: String, block: suspend () -> Result<*>) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            val r = block()
            _state.update { it.copy(busy = false, message = if (r.isSuccess) done else "Olmadı: ${r.exceptionOrNull()?.message}") }
            if (r.isSuccess) {
                // the record and the name may have changed: read both again
                admin.find(_state.value.query).onSuccess { list -> _state.update { it.copy(results = list) } }
                loadDetails(uid)
            }
        }
    }

    fun resetName(p: PlayerInfo) = act(p.uid, "Ad sıfırlandı.") { admin.resetName(p.uid, p.nickname) }
    fun lock(p: PlayerInfo) = act(p.uid, "Oyuncu kilitlendi.") { admin.lock(p.uid) }
    fun unlock(p: PlayerInfo) = act(p.uid, "Kilitler kaldırıldı.") { admin.unlock(p.uid) }
    fun grant(p: PlayerInfo) {
        val s = _state.value
        act(p.uid, "Telafi sıraya alındı: oyuncunun uygulaması bir sonraki açılışında (en geç birkaç saat içinde) alır.") {
            admin.grant(p.uid, s.gold.toIntOrNull() ?: 0, s.xp.toIntOrNull() ?: 0, s.note.ifBlank { "telafi" })
        }
    }
}

/** "Oyuncu ara": find accounts by the start of a name, friend code or uid, open one to see its record, and act on it. */
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
                placeholder = "Adın başı, arkadaş kodu ya da kullanıcı kimliği",
                leading = { Icon(Icons.Filled.Search, contentDescription = null, tint = Console.InkMuted, modifier = Modifier.size(18.dp)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { if (state.query.isNotBlank()) viewModel.search() }),
                modifier = Modifier.weight(1f)
            )
            ConsoleButton("Ara", viewModel::search, enabled = !state.busy && state.query.isNotBlank(), loading = state.busy && state.results.isEmpty(), height = 42.dp)
        }
        state.message?.let { ConsoleNotice(it, kind = noticeKindOf(it)) }
        if (!state.searched && state.results.isEmpty()) {
            ConsoleEmpty(Icons.Filled.Search, "Bir oyuncu ara", body = "Adın ilk harflerini yaz (büyük küçük harf fark etmez); profilini, cezalarını ve bakiyesini gör, adını sıfırla, kilitle ya da telafi gönder.")
        } else if (state.searched && state.results.isEmpty()) {
            ConsoleEmpty(Icons.Filled.PersonOff, "Böyle bir oyuncu bulunamadı", body = "Adın ilk harflerini dene ya da arkadaş kodunu yaz.")
        }
        if (state.results.size > 1) Text("${state.results.size} oyuncu · açmak için dokun", style = Console.Small)
        state.results.forEach { p ->
            PlayerCard(p, state, viewModel)
        }
    }
}

@Composable
private fun PlayerCard(p: PlayerInfo, state: PlayersState, viewModel: PlayersViewModel) {
    val open = state.selected == p.uid
    val d = state.details[p.uid]
    val now = System.currentTimeMillis()
    val locked = (d?.lockedUntilMillis ?: 0L) > now
    ConsoleCard(Modifier.fillMaxWidth(), padding = 12.dp, onClick = { viewModel.open(p.uid) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(p.nickname.ifBlank { p.uid }, 42.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(p.nickname.ifBlank { "(adsız)" }, style = Console.Title, maxLines = 1)
                Text("Kod ${p.friendCode.ifBlank { "-" }} · seviye ${p.level}", style = Console.Small)
            }
            if (locked) Tag("Kilitli", fg = Console.Critical, bg = Console.CriticalSoft)
        }
        if (!open) return@ConsoleCard
        Spacer(Modifier.height(10.dp))
        if (d == null) {
            ConsoleSkeleton(rows = 1, rowHeight = 56.dp)
            return@ConsoleCard
        }
        // the record, as figures side by side
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(Console.ControlCorner)).background(Console.SurfaceAlt).padding(vertical = 8.dp)) {
            Figure("Seviye", p.level.toString(), Modifier.weight(1f))
            Figure("Toplam XP", Console.compact((d.lifetimeXp ?: p.totalXp).toDouble()), Modifier.weight(1f))
            Figure("Ceza", d.strikes.toString(), Modifier.weight(1f), alert = d.strikes > 0)
            Figure("Rapor", d.reports.toString(), Modifier.weight(1f), alert = d.reports > 0)
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(Console.ControlCorner)).background(Console.SurfaceAlt).padding(vertical = 8.dp)) {
            Figure("Altın", d.goldBalance?.let { Console.compact(it.toDouble()) } ?: "—", Modifier.weight(1f))
            Figure("Bu ay XP", Console.compact(p.periodXp.toDouble()), Modifier.weight(1f))
            Figure("Havuzda tur", d.poolRuns?.toString() ?: "—", Modifier.weight(1f))
            Figure("Kuyrukta tur", d.queueRuns?.toString() ?: "—", Modifier.weight(1f), alert = (d.queueRuns ?: 0L) > 0)
        }
        d.lastSavedMillis?.let { Text("Son bulut kaydı: ${clock(it)} (oyuncunun son görülme zamanına en yakın iz)", style = Console.Small, modifier = Modifier.padding(top = 6.dp)) }
        if (locked) {
            Spacer(Modifier.height(8.dp))
            ConsoleNotice("${clock(d.lockedUntilMillis)} tarihine kadar kilitli", kind = NoticeKind.Error)
        }
        if (d.penalties.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Overline("Ceza geçmişi")
            d.penalties.forEach { pen ->
                val parts = listOfNotNull(
                    "${pen.strike}. ceza".takeIf { pen.strike > 0 },
                    pen.xpRevoked.takeIf { it > 0 }?.let { "$it XP" },
                    pen.goldRevoked.takeIf { it > 0 }?.let { "$it altın" },
                    pen.lockedUntilMillis.takeIf { it > 0 }?.let { "kilit ${clock(it)}'e kadar" },
                    "iptal edildi".takeIf { pen.canceled }
                )
                Text("${clock(pen.createdAtMillis)} · ${parts.joinToString(", ").ifEmpty { "ceza" }}", style = Console.Small.copy(color = Console.Ink), modifier = Modifier.padding(top = 3.dp))
            }
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
        // the coupons it redeemed: read only when asked for (one read for each of the newest codes)
        Spacer(Modifier.height(10.dp))
        val uses = state.couponUses[p.uid]
        if (uses == null) {
            ConsoleSecondaryButton("Kupon kullanımlarını göster", { viewModel.loadCoupons(p.uid) }, Modifier.fillMaxWidth(), enabled = !state.loadingCoupons, height = 36.dp)
        } else {
            Overline("Kullandığı kuponlar (son ${25} koddan)")
            if (uses.isEmpty()) Text("Hiçbirini kullanmamış.", style = Console.Small, modifier = Modifier.padding(top = 3.dp))
            uses.forEach { u -> Text("${u.code} · ${clock(u.redeemedAtMillis)}", style = Console.Small.copy(color = Console.Ink, fontFamily = FontFamily.Monospace), modifier = Modifier.padding(top = 3.dp)) }
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

/** One labelled figure in a row of them. */
@Composable
private fun Figure(label: String, value: String, modifier: Modifier = Modifier, alert: Boolean = false) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = Console.Subtitle.copy(fontSize = 16.sp, color = if (alert) Console.Critical else Console.Ink))
        Text(label, style = Console.Small.copy(fontSize = 11.sp))
    }
}
