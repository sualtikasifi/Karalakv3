package com.sualtikasifi.cizimhafiza.presentation.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.sualtikasifi.cizimhafiza.data.repository.EconomyConfigRepository
import com.sualtikasifi.cizimhafiza.presentation.common.AppTextField
import com.sualtikasifi.cizimhafiza.presentation.common.PrimaryButton
import com.sualtikasifi.cizimhafiza.presentation.common.RaisedCard
import com.sualtikasifi.cizimhafiza.presentation.common.SelectableChip
import com.sualtikasifi.cizimhafiza.util.EconomyConfig
import com.sualtikasifi.cizimhafiza.util.PlayerAdmin
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

private fun stamp(millis: Long): String =
    if (millis <= 0) "-" else SimpleDateFormat("d MMM HH:mm", Locale("tr")).format(Date(millis))

private val ECONOMY_LABELS = mapOf(
    "goldPerCorrectWord" to "Doğru kelime başı altın",
    "onlineWinGold" to "Çevrimiçi galibiyet altını",
    "achievementGold" to "Başarım altını",
    "levelGoldPerNewStar" to "Yeni yıldız başı altın",
    "dailyGoldBase" to "Günlük ödül (taban)",
    "dailyGoldPerStreakDay" to "Seri günü başı ek altın",
    "chainBonusGold" to "Zincir bonusu",
    "adGoldAmount" to "Reklam altını",
    "adGoldCooldownHours" to "Reklam altını bekleme (saat)",
    "adChestCooldownHours" to "Reklam kasası bekleme (saat)",
    "leagueMonthlyGold" to "Aylık lig altını"
)

// ---------------------------------------------------------------- economy

data class EconomyAdminState(
    val values: Map<String, String> = emptyMap(),
    val serverValues: Map<String, String> = emptyMap(),
    val loaded: Boolean = false,
    val busy: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class EconomyAdminViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val economyConfigRepository: EconomyConfigRepository,
    private val admin: PlayerAdmin
) : ViewModel() {
    private val _state = MutableStateFlow(EconomyAdminState())
    val state: StateFlow<EconomyAdminState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            runCatching { firestore.document("config/economy").get(Source.SERVER).await().data.orEmpty() }
                .onSuccess { data ->
                    val shown = EconomyConfig.fields.associate { f ->
                        f.key to ((data[f.key] as? Number)?.toInt()?.takeIf { it in f.range } ?: f.default).toString()
                    }
                    _state.update { it.copy(values = shown, serverValues = shown, loaded = true) }
                }
                .onFailure { _state.update { it.copy(loaded = true, message = "Okunamadı (internet ya da yetki).") } }
        }
    }

    fun edit(key: String, value: String) = _state.update { it.copy(values = it.values + (key to value.filter(Char::isDigit).take(7)), message = null) }

    fun reset(key: String) {
        val f = EconomyConfig.fields.first { it.key == key }
        edit(key, f.default.toString())
    }

    fun save() {
        val s = _state.value
        if (s.busy) return
        val changed = mutableMapOf<String, Any>()
        for (f in EconomyConfig.fields) {
            val text = s.values[f.key].orEmpty()
            val v = text.toIntOrNull()
            if (v == null || v !in f.range) {
                _state.update { it.copy(message = "${ECONOMY_LABELS[f.key] ?: f.key}: ${f.range.first} ile ${f.range.last} arasında olmalı.") }
                return
            }
            if (text != s.serverValues[f.key]) changed[f.key] = v.toLong()
        }
        if (changed.isEmpty()) {
            _state.update { it.copy(message = "Değişiklik yok.") }
            return
        }
        _state.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            // merge: the same document carries minVersionCode, which must stay as it is
            runCatching { firestore.document("config/economy").set(changed, SetOptions.merge()).await() }
                .onSuccess {
                    admin.audit("economy", "config/economy", changed.entries.joinToString { "${it.key}=${it.value}" })
                    economyConfigRepository.refreshNow()
                    _state.update { it.copy(busy = false, serverValues = it.values, message = "Kaydedildi (${changed.size} alan). Telefonlara yarım saat içinde ulaşır.") }
                }
                .onFailure { _state.update { it.copy(busy = false, message = "Yazılamadı (internet ya da yetki).") } }
        }
    }
}

/** "Ekonomi": every amount in config/economy, with its allowed range and built-in default. */
@Composable
internal fun EconomyPanel(viewModel: EconomyAdminViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            "Altın ve ödül miktarları. Boş bırakılan ya da aralık dışı değer oyunda yok sayılır ve varsayılan geçerli olur.",
            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (state.loaded) EconomyConfig.fields.forEach { f ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                AppTextField(
                    value = state.values[f.key].orEmpty(),
                    onValueChange = { viewModel.edit(f.key, it) },
                    label = "${ECONOMY_LABELS[f.key] ?: f.key} (${f.range.first}–${f.range.last}, varsayılan ${f.default})",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        state.message?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) }
        PrimaryButton("Kaydet", viewModel::save, Modifier.fillMaxWidth(), enabled = state.loaded && !state.busy, height = 48.dp)
    }
}

// ---------------------------------------------------------------- announcement

private val LANGS = listOf("tr" to "Türkçe", "en" to "English", "pt" to "Português", "es" to "Español")

data class AnnounceState(
    val lang: String = "tr",
    val titles: Map<String, String> = emptyMap(),
    val bodies: Map<String, String> = emptyMap(),
    val active: Boolean = false,
    val days: String = "7",
    val busy: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class AnnounceViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val admin: PlayerAdmin
) : ViewModel() {
    private val _state = MutableStateFlow(AnnounceState())
    val state: StateFlow<AnnounceState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            runCatching { firestore.document("config/announcement").get(Source.SERVER).await().data.orEmpty() }.onSuccess { d ->
                val texts = d["texts"] as? Map<*, *> ?: emptyMap<Any, Any>()
                fun field(name: String) = LANGS.associate { (l, _) -> l to ((texts[l] as? Map<*, *>)?.get(name) as? String).orEmpty() }
                _state.update { it.copy(titles = field("title"), bodies = field("body"), active = d["active"] == true) }
            }
        }
    }

    fun edit(block: AnnounceState.() -> AnnounceState) = _state.update { it.block().copy(message = null) }

    fun publish(active: Boolean) {
        val s = _state.value
        if (s.busy) return
        if (active && s.titles["tr"].isNullOrBlank() && s.bodies["tr"].isNullOrBlank()) {
            _state.update { it.copy(message = "Türkçe metin zorunlu (diğer diller boşsa Türkçe gösterilir).") }
            return
        }
        val days = s.days.toIntOrNull() ?: 0
        val now = System.currentTimeMillis()
        val texts = LANGS.mapNotNull { (l, _) ->
            val t = s.titles[l].orEmpty().trim()
            val b = s.bodies[l].orEmpty().trim()
            if (t.isEmpty() && b.isEmpty()) null else l to mapOf("title" to t.take(60), "body" to b.take(400))
        }.toMap()
        _state.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            // a new id each time: every device shows a notice once per id
            val doc = mapOf(
                "active" to active, "id" to now.toString(),
                "untilMillis" to if (days > 0) now + days * 86_400_000L else 0L,
                "texts" to texts
            )
            runCatching { firestore.document("config/announcement").set(doc).await() }
                .onSuccess {
                    admin.audit(if (active) "announce" else "announce-off", "config/announcement", texts["tr"]?.get("title").orEmpty())
                    _state.update { it.copy(busy = false, active = active, message = if (active) "Yayında. Telefonlara yarım saat içinde ulaşır." else "Duyuru kapatıldı.") }
                }
                .onFailure { _state.update { it.copy(busy = false, message = "Yazılamadı (internet ya da yetki).") } }
        }
    }
}

/** "Duyuru": a window shown once per notice on the home screen, in four languages. */
@Composable
internal fun AnnouncePanel(viewModel: AnnounceViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            if (state.active) "Şu an yayında." else "Şu an yayında duyuru yok.",
            style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            LANGS.forEach { (l, name) -> SelectableChip(name, state.lang == l, { viewModel.edit { copy(lang = l) } }) }
        }
        AppTextField(
            value = state.titles[state.lang].orEmpty(),
            onValueChange = { v -> viewModel.edit { copy(titles = titles + (lang to v.take(60))) } },
            label = "Başlık", modifier = Modifier.fillMaxWidth()
        )
        AppTextField(
            value = state.bodies[state.lang].orEmpty(),
            onValueChange = { v -> viewModel.edit { copy(bodies = bodies + (lang to v.take(400))) } },
            label = "Metin", singleLine = false, minLines = 4, modifier = Modifier.fillMaxWidth()
        )
        AppTextField(
            value = state.days,
            onValueChange = { v -> viewModel.edit { copy(days = v.filter(Char::isDigit).take(3)) } },
            label = "Kaç gün yayında kalsın (0 = süresiz)",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()
        )
        state.message?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) }
        PrimaryButton("Yayınla", { viewModel.publish(true) }, Modifier.fillMaxWidth(), enabled = !state.busy, height = 48.dp)
        if (state.active) {
            com.sualtikasifi.cizimhafiza.presentation.common.SecondaryButton(
                "Duyuruyu kapat", { viewModel.publish(false) }, Modifier.fillMaxWidth(), enabled = !state.busy, height = 48.dp
            )
        }
    }
}

// ---------------------------------------------------------------- push

data class PushState(
    val title: String = "",
    val body: String = "",
    val busy: Boolean = false,
    val message: String? = null,
    val recent: List<String> = emptyList()
)

@HiltViewModel
class PushViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val admin: PlayerAdmin
) : ViewModel() {
    private val _state = MutableStateFlow(PushState())
    val state: StateFlow<PushState> = _state.asStateFlow()

    fun edit(block: PushState.() -> PushState) = _state.update { it.block().copy(message = null) }

    fun loadRecent() {
        viewModelScope.launch {
            runCatching {
                firestore.collection("adminCommands").whereEqualTo("type", "broadcast").limit(10).get().await().documents
                    .sortedByDescending { it.getTimestamp("createdAt")?.toDate()?.time ?: 0L }
                    .map { d ->
                        val at = d.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                        val done = d.getTimestamp("doneAt") != null
                        "${stamp(at)} · ${d.getString("title").orEmpty()} · " + if (done) "gönderildi" else "sırada"
                    }
            }.onSuccess { r -> _state.update { it.copy(recent = r) } }
        }
    }

    fun send() {
        val s = _state.value
        if (s.busy || s.title.isBlank() || s.body.isBlank()) return
        _state.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            runCatching {
                firestore.collection("adminCommands").add(
                    mapOf(
                        "type" to "broadcast", "title" to s.title.trim().take(60), "body" to s.body.trim().take(200),
                        "by" to (auth.currentUser?.email ?: ""), "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }.onSuccess {
                admin.audit("broadcast", "news", s.title.trim())
                _state.update { PushState(message = "Sıraya alındı. Zamanlanmış iş her saat başı çalışır; gönderilince aşağıda “gönderildi” yazar.") }
                loadRecent()
            }.onFailure { _state.update { it.copy(busy = false, message = "Yazılamadı (internet ya da yetki).") } }
        }
    }
}

/** "Bildirim gönder": one push to everyone who has notifications on (the hourly job sends it). */
@Composable
internal fun PushPanel(viewModel: PushViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var confirm by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { viewModel.loadRecent() }
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Bildirimi açık olan 13 yaş ve üstü tüm oyunculara gider. Metin tek dilde (oyuncunun diline çevrilmez); kısa ve net tut.",
            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        AppTextField(
            value = state.title, onValueChange = { v -> viewModel.edit { copy(title = v.take(60)) } },
            label = "Başlık", modifier = Modifier.fillMaxWidth()
        )
        AppTextField(
            value = state.body, onValueChange = { v -> viewModel.edit { copy(body = v.take(200)) } },
            label = "Metin", singleLine = false, minLines = 3, modifier = Modifier.fillMaxWidth()
        )
        state.message?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) }
        PrimaryButton("Gönder", { confirm = true }, Modifier.fillMaxWidth(), enabled = !state.busy && state.title.isNotBlank() && state.body.isNotBlank(), height = 48.dp)
        if (state.recent.isNotEmpty()) {
            Text("Son gönderilenler", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            state.recent.forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Herkese gönderilsin mi?") },
            text = { Text("“${state.title}”\n${state.body}") },
            confirmButton = { TextButton(onClick = { confirm = false; viewModel.send() }) { Text("Gönder") } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Vazgeç") } }
        )
    }
}

// ---------------------------------------------------------------- background jobs

private val JOBS = listOf(
    "build-global-leaderboard" to "Küresel lig özeti (saatlik)",
    "process-admin-commands" to "Panel komutları: telafi, bildirim, planlı XP (saatlik)",
    "rollup-stats" to "Günlük istatistik özeti (gece)",
    "cleanup-abandoned-rooms" to "Terk edilmiş oda temizliği (gece)",
    "finalize-league-period" to "Lig dönemi kapanışı (gece)",
    "grant-referral-rewards" to "Davet ödülleri (gece)",
    "process-deletions" to "Hesap silme talepleri (gece)"
)

data class JobRow(val name: String, val label: String, val lastRunMillis: Long, val ok: Boolean?, val error: String)

data class JobsState(
    val rows: List<JobRow> = emptyList(),
    val pendingDeletions: Long? = null,
    val pendingCommands: Long? = null,
    val loaded: Boolean = false
)

@HiltViewModel
class JobsViewModel @Inject constructor(private val firestore: FirebaseFirestore) : ViewModel() {
    private val _state = MutableStateFlow(JobsState())
    val state: StateFlow<JobsState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            val rows = JOBS.map { (name, label) ->
                val d = runCatching { firestore.collection("adminJobs").document(name).get(Source.SERVER).await() }.getOrNull()
                JobRow(
                    name, label,
                    d?.getTimestamp("lastRunAt")?.toDate()?.time ?: 0L,
                    d?.takeIf { it.exists() }?.getBoolean("ok"),
                    d?.getString("error").orEmpty()
                )
            }
            val deletions = runCatching { firestore.collection("deletionRequests").count().get(AggregateSource.SERVER).await().count }.getOrNull()
            val commands = runCatching {
                // a command the cron has not run has no doneAt at all, so it cannot be matched by a query
                firestore.collection("adminCommands").limit(200).get().await().documents.count { it.get("doneAt") == null }.toLong()
            }.getOrNull()
            _state.value = JobsState(rows, deletions, commands, loaded = true)
        }
    }
}

/** "Arka plan işleri": when each scheduled job last ran and whether it worked, plus what is waiting for them. */
@Composable
internal fun JobsPanel(viewModel: JobsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }
    val now = System.currentTimeMillis()
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (state.loaded) {
            Text(
                "Bekleyen hesap silme talebi: ${state.pendingDeletions ?: "?"} · bekleyen panel komutu: ${state.pendingCommands ?: "?"}",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface
            )
        }
        state.rows.forEach { r ->
            val stale = r.lastRunMillis > 0 && now - r.lastRunMillis > 36 * 3_600_000L
            RaisedCard(corner = 20.dp, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(r.label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    val bad = r.ok == false || stale
                    Text(
                        when {
                            r.lastRunMillis == 0L -> "Henüz kayıt yok"
                            r.ok == false -> "HATA · ${stamp(r.lastRunMillis)} · ${r.error.take(120)}"
                            stale -> "Uzun süredir çalışmadı · son: ${stamp(r.lastRunMillis)}"
                            else -> "Çalıştı · ${stamp(r.lastRunMillis)}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (bad) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------- audit log

data class AuditState(val lines: List<Triple<String, String, String>> = emptyList(), val loaded: Boolean = false)

@HiltViewModel
class AuditViewModel @Inject constructor(private val firestore: FirebaseFirestore) : ViewModel() {
    private val _state = MutableStateFlow(AuditState())
    val state: StateFlow<AuditState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            runCatching {
                firestore.collection("adminAudit").orderBy("at", Query.Direction.DESCENDING).limit(60).get().await().documents.map { d ->
                    Triple(
                        "${stamp(d.getTimestamp("at")?.toDate()?.time ?: 0L)} · ${d.getString("by").orEmpty().substringBefore('@')}",
                        d.getString("action").orEmpty() + " → " + d.getString("target").orEmpty().take(24),
                        d.getString("detail").orEmpty()
                    )
                }
            }.onSuccess { _state.value = AuditState(it, true) }.onFailure { _state.value = AuditState(emptyList(), true) }
        }
    }
}

/** "Denetim kaydı": the latest things done from this panel. */
@Composable
internal fun AuditPanel(viewModel: AuditViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (state.loaded && state.lines.isEmpty()) {
            Text("Kayıt yok.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        state.lines.forEach { (who, what, detail) ->
            RaisedCard(corner = 16.dp, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(what, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(who, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- planned XP events

data class XpPlanRow(val id: String, val startAtMillis: Long, val durationHours: Int, val multiplier: Double, val label: String, val started: Boolean)

data class XpPlanState(
    val plans: List<XpPlanRow> = emptyList(),
    val startInHours: Int = 24,
    val durationHours: Int = 24,
    val multiplier: Double = 2.0,
    val label: String = "",
    val busy: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class XpPlanViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val admin: PlayerAdmin
) : ViewModel() {
    private val _state = MutableStateFlow(XpPlanState())
    val state: StateFlow<XpPlanState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            runCatching {
                firestore.collection("xpEventPlans").limit(50).get().await().documents.map { d ->
                    XpPlanRow(
                        d.id, d.getLong("startAtMillis") ?: 0L, (d.getLong("durationHours") ?: 0L).toInt(),
                        d.getDouble("multiplier") ?: 2.0, d.getString("label").orEmpty(), d.getLong("startedAt") != null
                    )
                }.sortedBy { it.startAtMillis }
            }.onSuccess { r -> _state.update { it.copy(plans = r) } }
        }
    }

    fun edit(block: XpPlanState.() -> XpPlanState) = _state.update { it.block().copy(message = null) }

    /** Fills the form from a calendar day: ten in the morning, Istanbul time, for a day. */
    fun planForDay(date: java.time.LocalDate, name: String) {
        val start = date.atTime(10, 0).atZone(java.time.ZoneId.of("Europe/Istanbul")).toInstant().toEpochMilli()
        val hours = ((start - System.currentTimeMillis()) / 3_600_000L).toInt().coerceAtLeast(1)
        edit { copy(startInHours = hours, durationHours = 24, label = name.take(32)) }
    }

    fun create() {
        val s = _state.value
        if (s.busy) return
        val start = System.currentTimeMillis() + s.startInHours * 3_600_000L
        _state.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            runCatching {
                firestore.collection("xpEventPlans").add(
                    mapOf(
                        "startAtMillis" to start, "durationHours" to s.durationHours.toLong(), "multiplier" to s.multiplier,
                        "label" to s.label.trim().take(32), "createdAt" to System.currentTimeMillis()
                    )
                ).await()
            }.onSuccess {
                admin.audit("xp-plan", "config/xpEvent", "${s.multiplier}x, ${s.durationHours} saat, ${stamp(start)}")
                _state.update { it.copy(busy = false, message = "Planlandı: ${stamp(start)} (en geç bir saat gecikmeyle başlar).") }
                load()
            }.onFailure { _state.update { it.copy(busy = false, message = "Yazılamadı (internet ya da yetki).") } }
        }
    }

    fun cancel(plan: XpPlanRow) {
        viewModelScope.launch {
            runCatching { firestore.collection("xpEventPlans").document(plan.id).delete().await() }
                .onSuccess { admin.audit("xp-plan-cancel", plan.id, stamp(plan.startAtMillis)); load() }
        }
    }
}

/** Under the XP event's start/stop buttons: plan one ahead, from a form or from the next special days. */
@Composable
internal fun XpPlanSection(viewModel: XpPlanViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }
    val upcoming = remember { com.sualtikasifi.cizimhafiza.presentation.reports.console.SpecialDays.upcoming(horizonDays = 45).take(3) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
        Text("İleri tarihli etkinlik", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        if (upcoming.isNotEmpty()) {
            Text("Özel günden planla (sabah 10, bir gün sürer):", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                upcoming.forEach { d -> SelectableChip(d.name.take(28), false, { viewModel.planForDay(d.date, d.name) }, verticalPadding = 8.dp) }
            }
        }
        Text("Kaç saat sonra başlasın", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(1, 6, 12, 24, 48, 72, 168).forEach { h ->
                SelectableChip("$h sa", state.startInHours == h, { viewModel.edit { copy(startInHours = h) } }, verticalPadding = 8.dp)
            }
        }
        Text("Ne kadar sürsün · çarpan", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(3, 6, 24, 48, 72).forEach { h ->
                SelectableChip("$h sa", state.durationHours == h, { viewModel.edit { copy(durationHours = h) } }, verticalPadding = 8.dp)
            }
            listOf(1.3, 1.5, 2.0, 3.0).forEach { m ->
                SelectableChip("x$m", state.multiplier == m, { viewModel.edit { copy(multiplier = m) } }, verticalPadding = 8.dp)
            }
        }
        AppTextField(
            value = state.label, onValueChange = { v -> viewModel.edit { copy(label = v.take(32)) } },
            label = "Etkinlik adı", modifier = Modifier.fillMaxWidth()
        )
        state.message?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) }
        PrimaryButton("Planla", viewModel::create, Modifier.fillMaxWidth(), enabled = !state.busy, height = 48.dp)
        state.plans.forEach { p ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    "${stamp(p.startAtMillis)} · x${p.multiplier} · ${p.durationHours} sa" + (if (p.label.isNotBlank()) " · ${p.label}" else "") +
                        if (p.started) " · başladı" else "",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f)
                )
                if (!p.started) TextButton(onClick = { viewModel.cancel(p) }) { Text("İptal") }
            }
        }
    }
}
