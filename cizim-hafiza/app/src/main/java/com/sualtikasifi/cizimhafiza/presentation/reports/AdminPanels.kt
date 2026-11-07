package com.sualtikasifi.cizimhafiza.presentation.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Console
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleButtonStyle
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleCard
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleEmpty
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleNotice
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleSection
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleSkeleton
import com.sualtikasifi.cizimhafiza.presentation.reports.console.NoticeKind
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Overline
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Segmented
import com.sualtikasifi.cizimhafiza.presentation.reports.console.StatTile
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Tag
import com.sualtikasifi.cizimhafiza.presentation.reports.console.noticeKindOf
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleTextField
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleButton
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleSecondaryButton
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleChip
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
import androidx.compose.foundation.layout.FlowRow
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
    val message: String? = null,
    /** What the last save changed, as the values it replaced (key to number): written back by "geri al". Null when there is nothing to undo. */
    val undo: Map<String, Long>? = null
)

/** What an economy save changes, as the audit line and the undo need it. */
object EconomyChange {
    /** "Doğru kelime başı altın: 10→15; Reklam altını: 5→8", the label (or the key) first; the numbers as they were and are. */
    fun detail(labels: Map<String, String>, before: Map<String, String>, after: Map<String, Long>): String =
        after.entries.joinToString("; ") { (key, now) -> "${labels[key] ?: key}: ${before[key].orEmpty().ifEmpty { "?" }}→$now" }

    /** The values a save replaces, as numbers, so the same fields can be written back. Fields whose old value is not a number are left out. */
    fun undoOf(before: Map<String, String>, after: Map<String, Long>): Map<String, Long> =
        after.keys.mapNotNull { key -> before[key]?.toLongOrNull()?.let { key to it } }.toMap()
}

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

    /** Writes the values the last save replaced back (audited as its own line), and forgets the undo. */
    fun revert() {
        val s = _state.value
        val back = s.undo ?: return
        if (s.busy) return
        _state.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            runCatching {
                admin.audited("economy-undo", "config/economy", EconomyChange.detail(ECONOMY_LABELS, s.serverValues, back)) { b ->
                    b.set(firestore.document("config/economy"), back, SetOptions.merge())
                }
            }.onSuccess {
                economyConfigRepository.refreshNow()
                val shown = back.mapValues { it.value.toString() }
                _state.update { it.copy(busy = false, values = it.values + shown, serverValues = it.serverValues + shown, undo = null, message = "Son değişiklik geri alındı.") }
            }.onFailure { _state.update { it.copy(busy = false, message = "Yazılamadı (internet ya da yetki).") } }
        }
    }

    fun save() {
        val s = _state.value
        if (s.busy) return
        val changed = mutableMapOf<String, Long>()
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
            // merge: the same document carries minVersionCode, which must stay as it is; the audit line (old → new) goes in the same batch
            val detail = EconomyChange.detail(ECONOMY_LABELS, s.serverValues, changed)
            val undo = EconomyChange.undoOf(s.serverValues, changed)
            runCatching {
                admin.audited("economy", "config/economy", detail) { b -> b.set(firestore.document("config/economy"), changed, SetOptions.merge()) }
            }
                .onSuccess {
                    economyConfigRepository.refreshNow()
                    _state.update { it.copy(busy = false, serverValues = it.values, undo = undo.takeIf { u -> u.isNotEmpty() }, message = "Kaydedildi (${changed.size} alan). Telefonlara yarım saat içinde ulaşır.") }
                }
                .onFailure { _state.update { it.copy(busy = false, message = "Yazılamadı (internet ya da yetki).") } }
        }
    }
}

/**
 * "Ekonomi": every amount in config/economy, with its allowed range and built-in default, two to a row. A field that
 * differs from what is live gets an accent mark, and the save bar at the bottom counts them.
 */
@Composable
internal fun EconomyPanel(viewModel: EconomyAdminViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }
    val changed = EconomyConfig.fields.count { f -> state.loaded && state.values[f.key] != state.serverValues[f.key] }
    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(Console.Gap)
        ) {
            ConsoleNotice("Boş bırakılan ya da aralık dışı değer oyunda yok sayılır; varsayılan geçerli olur.", kind = NoticeKind.Info)
            if (!state.loaded) ConsoleSkeleton(rows = 3, rowHeight = 64.dp)
            else ConsoleSection("Altın ve ödüller", subtitle = "${EconomyConfig.fields.size} ayar · telefonlara yarım saat içinde ulaşır") {
                EconomyConfig.fields.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                        pair.forEach { f ->
                            val dirty = state.values[f.key] != state.serverValues[f.key]
                            ConsoleTextField(
                                value = state.values[f.key].orEmpty(),
                                onValueChange = { viewModel.edit(f.key, it) },
                                label = (if (dirty) "● " else "") + (ECONOMY_LABELS[f.key] ?: f.key),
                                supporting = "${f.range.first}–${f.range.last} · vars. ${f.default}",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            state.message?.let { ConsoleNotice(it, kind = noticeKindOf(it)) }
            if (state.undo != null) {
                ConsoleButton(
                    "Son değişikliği geri al", viewModel::revert, Modifier.fillMaxWidth(), enabled = !state.busy,
                    icon = Icons.Filled.Undo, style = ConsoleButtonStyle.Secondary
                )
            }
        }
        SaveBar(
            text = if (changed > 0) "Kaydet · $changed değişiklik" else "Kaydet",
            enabled = state.loaded && !state.busy && changed > 0,
            busy = state.busy,
            onClick = viewModel::save
        )
    }
}

/** The bottom of a settings panel: one full-width save button that stays in view while the fields scroll. */
@Composable
internal fun SaveBar(text: String, enabled: Boolean, busy: Boolean, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Console.Bg).padding(top = 8.dp, bottom = 12.dp)) {
        ConsoleButton(text, onClick, Modifier.fillMaxWidth(), enabled = enabled, loading = busy, icon = Icons.Filled.Check, height = 46.dp)
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
            runCatching {
                admin.audited(if (active) "announce" else "announce-off", "config/announcement", texts["tr"]?.get("title").orEmpty()) { b ->
                    b.set(firestore.document("config/announcement"), doc)
                }
            }
                .onSuccess {
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
        verticalArrangement = Arrangement.spacedBy(Console.Gap)
    ) {
        ConsoleCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(if (state.active) Console.Good else Console.InkMuted))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Overline("Durum")
                    Text(if (state.active) "Şu an yayında" else "Yayında duyuru yok", style = Console.Subtitle)
                }
                if (state.active) {
                    ConsoleButton("Kapat", { viewModel.publish(false) }, enabled = !state.busy, icon = Icons.Filled.Close, height = 34.dp, style = ConsoleButtonStyle.DangerSoft)
                }
            }
        }
        ConsoleSection("Duyuru metni", subtitle = "Her dil ayrı yazılır; oyuncu kendi dilindekini görür") {
            Segmented(
                options = LANGS.map { (l, name) -> l to name + if (state.titles[l].isNullOrBlank()) "" else " ✓" },
                selected = state.lang,
                onSelect = { l -> viewModel.edit { copy(lang = l) } },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            ConsoleTextField(
                value = state.titles[state.lang].orEmpty(),
                onValueChange = { v -> viewModel.edit { copy(titles = titles + (lang to v.take(60))) } },
                label = "Başlık", supporting = "${state.titles[state.lang].orEmpty().length}/60", modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            ConsoleTextField(
                value = state.bodies[state.lang].orEmpty(),
                onValueChange = { v -> viewModel.edit { copy(bodies = bodies + (lang to v.take(400))) } },
                label = "Metin", singleLine = false, minLines = 4, supporting = "${state.bodies[state.lang].orEmpty().length}/400",
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            ConsoleTextField(
                value = state.days,
                onValueChange = { v -> viewModel.edit { copy(days = v.filter(Char::isDigit).take(3)) } },
                label = "Yayında kalacağı gün", supporting = "0 = süresiz",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(14.dp))
            ConsoleButton("Yayınla", { viewModel.publish(true) }, Modifier.fillMaxWidth(), enabled = !state.busy, loading = state.busy, icon = Icons.Filled.Campaign)
        }
        state.message?.let { ConsoleNotice(it, kind = noticeKindOf(it)) }
    }
}

// ---------------------------------------------------------------- push

data class PushState(
    /** The Turkish text, which is also what a device gets when its language has none of its own. */
    val title: String = "",
    val body: String = "",
    /** The texts in the other languages (en, pt, es); one with only half written is refused. */
    val extra: Map<String, PushText> = emptyMap(),
    /** Which language the two fields are showing: "tr" or one of [PushForm.EXTRA_LANGS]. */
    val lang: String = "tr",
    val audience: PushAudience = PushAudience.All,
    /** For everyone: whether languages without a text of their own (and older app versions) get the Turkish one. */
    val fallback: Boolean = true,
    val whenSend: PushWhen = PushWhen.Now,
    val busy: Boolean = false,
    val message: String? = null,
    val recent: List<String> = emptyList()
) {
    /** The title and text being edited, whichever language that is. */
    val shown: PushText get() = if (lang == "tr") PushText(title, body) else extra[lang] ?: PushText()
}

@HiltViewModel
class PushViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val admin: PlayerAdmin
) : ViewModel() {
    private val _state = MutableStateFlow(PushState())
    val state: StateFlow<PushState> = _state.asStateFlow()

    fun edit(block: PushState.() -> PushState) = _state.update { it.block().copy(message = null) }

    /** Types into the language being edited. */
    fun type(title: String? = null, body: String? = null) = edit {
        if (lang == "tr") copy(title = title ?: this.title, body = body ?: this.body)
        else {
            val now = extra[lang] ?: PushText()
            copy(extra = extra + (lang to PushText(title ?: now.title, body ?: now.body)))
        }
    }

    fun loadRecent() {
        viewModelScope.launch {
            runCatching {
                val now = System.currentTimeMillis()
                recentBroadcasts(firestore).map { d ->
                    val at = d.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                    val due = d.getLong("sendAtMillis") ?: 0L
                    val status = when {
                        d.getTimestamp("doneAt") != null -> "gönderildi"
                        due > now -> "planlı"
                        else -> "sırada"
                    }
                    val when_ = if (due > 0L) " · ${stamp(due)}'te" else ""
                    "${stamp(at)} · ${d.getString("title").orEmpty()}$when_ · $status"
                }
            }.onSuccess { r -> _state.update { it.copy(recent = r) } }
        }
    }

    fun send() {
        val s = _state.value
        if (s.busy) return
        val problem = PushForm.problem(s.title, s.body, s.extra)
        if (problem != null) { _state.update { it.copy(message = problem) }; return }
        _state.update { it.copy(busy = true, message = null) }
        val dueAt = PushForm.sendAt(s.whenSend, System.currentTimeMillis())
        viewModelScope.launch {
            runCatching {
                val command = PushForm.command(s.title, s.body, s.extra, s.audience, s.fallback, dueAt, auth.currentUser?.email.orEmpty())
                admin.audited("broadcast", "news", "${s.title.trim()} · ${PushForm.summary(s.audience, s.extra, s.fallback, s.whenSend)}") { b ->
                    b.set(firestore.collection("adminCommands").document(), command + ("createdAt" to FieldValue.serverTimestamp()))
                }
            }.onSuccess {
                _state.update {
                    PushState(message = "Sıraya alındı. Zamanlanmış iş her saat başı çalışır" +
                        (if (dueAt > 0L) " ve ${stamp(dueAt)}'ten sonraki ilk çalışmada gönderir" else "") +
                        "; gönderilince aşağıda “gönderildi” yazar.")
                }
                loadRecent()
            }.onFailure { _state.update { it.copy(busy = false, message = "Yazılamadı (internet ya da yetki).") } }
        }
    }

    /** One push to this phone alone, in the language being edited, before the real one goes to everybody. */
    fun sendTest() {
        val s = _state.value
        if (s.busy) return
        val text = s.shown.takeIf { it.filled } ?: PushText(s.title, s.body)
        val uid = auth.currentUser?.uid
        if (!text.filled || uid == null) { _state.update { it.copy(message = "Test için başlık ve metin gerekli.") }; return }
        _state.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            runCatching {
                admin.audited("broadcast-test", uid.take(8), text.title.trim()) { b ->
                    b.set(
                        firestore.collection("adminCommands").document(),
                        mapOf(
                            "type" to "test", "uid" to uid, "title" to text.title.trim().take(PushForm.MAX_TITLE),
                            "body" to text.body.trim().take(PushForm.MAX_BODY), "by" to (auth.currentUser?.email ?: ""),
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.onSuccess {
                _state.update {
                    it.copy(
                        busy = false,
                        message = "Test sıraya alındı: bu telefona bir sonraki saatlik çalışmada (en geç yaklaşık bir saat) gelir. " +
                            "Hemen görmek için GitHub'da “League scheduled tasks” işini process-admin-commands ile elle çalıştır."
                    )
                }
            }.onFailure { _state.update { it.copy(busy = false, message = "Yazılamadı (internet ya da yetki).") } }
        }
    }
}

/**
 * The ten newest broadcasts, newest first, by the server's own ordering. The first query needs the composite index on
 * (type, createdAt) from firestore.indexes.json; until that index has been built (just after a deploy) the second one
 * reads the newest commands of any kind and picks the broadcasts out here, so the list is never the wrong ten.
 */
internal suspend fun recentBroadcasts(firestore: FirebaseFirestore): List<com.google.firebase.firestore.DocumentSnapshot> = try {
    firestore.collection("adminCommands").whereEqualTo("type", "broadcast")
        .orderBy("createdAt", Query.Direction.DESCENDING).limit(10).get().await().documents
} catch (e: Exception) {
    firestore.collection("adminCommands").orderBy("createdAt", Query.Direction.DESCENDING).limit(60).get().await().documents
        .filter { it.getString("type") == "broadcast" }.take(10)
}

/** "Bildirim gönder": one push to the players who have notifications on, in their languages, now or later (the hourly job sends it). */
@Composable
internal fun PushPanel(viewModel: PushViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var confirm by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { viewModel.loadRecent() }
    val problem = PushForm.problem(state.title, state.body, state.extra)
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(Console.Gap)
    ) {
        ConsoleNotice(
            "Bildirimi açık olan 13 yaş ve üstü oyunculara gider. Türkçe metin her zaman gerekir; öbür dillere ayrı metin yazarsan her cihaz kendi dilinde okur. " +
                "Dil ve “girmeyenler” seçimi uygulamanın yeni sürümünü ister: eski sürümler yalnızca “Herkes” gönderimini (Türkçe metinle) alır.",
            kind = NoticeKind.Warning
        )
        ConsoleSection("Yeni bildirim") {
            // the language being written
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                (listOf("tr" to "Türkçe") + PushForm.EXTRA_LANGS).forEach { (code, name) ->
                    val filled = code == "tr" && state.title.isNotBlank() && state.body.isNotBlank() || state.extra[code]?.filled == true
                    ConsoleChip(
                        label = name + if (filled) " ✓" else "",
                        selected = state.lang == code,
                        onClick = { viewModel.edit { copy(lang = code) } }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            ConsoleTextField(
                value = state.shown.title, onValueChange = { v -> viewModel.type(title = v.take(PushForm.MAX_TITLE)) },
                label = "Başlık", supporting = "${state.shown.title.length}/${PushForm.MAX_TITLE}", modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            ConsoleTextField(
                value = state.shown.body, onValueChange = { v -> viewModel.type(body = v.take(PushForm.MAX_BODY)) },
                label = "Metin", singleLine = false, minLines = 3, supporting = "${state.shown.body.length}/${PushForm.MAX_BODY}", modifier = Modifier.fillMaxWidth()
            )
            if (state.shown.title.isNotBlank() || state.shown.body.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Overline("Telefonda böyle görünür")
                Spacer(Modifier.height(6.dp))
                PushPreview(state.shown.title, state.shown.body)
            }
            Spacer(Modifier.height(14.dp))
            Overline("Kime")
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PushAudience.entries.forEach { a ->
                    ConsoleChip(label = a.label, selected = state.audience == a, onClick = { viewModel.edit { copy(audience = a) } })
                }
            }
            if (state.audience == PushAudience.All && PushForm.languages(state.extra).size > 1) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Metni olmayan dillere Türkçe gitsin", style = Console.Small.copy(color = Console.Ink), modifier = Modifier.weight(1f))
                    androidx.compose.material3.Switch(
                        checked = state.fallback, onCheckedChange = { v -> viewModel.edit { copy(fallback = v) } },
                        colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = Console.Accent)
                    )
                }
            }
            if (state.audience == PushAudience.Lapsed) {
                Text("Yalnızca metni yazılmış dillerdeki cihazlara gider; yeni sürümde olmayan cihazlar bu seçimde dışarıda kalır.", style = Console.Small, modifier = Modifier.padding(top = 6.dp))
            }
            Spacer(Modifier.height(12.dp))
            Overline("Ne zaman")
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PushWhen.entries.forEach { w ->
                    ConsoleChip(label = w.label, selected = state.whenSend == w, onClick = { viewModel.edit { copy(whenSend = w) } })
                }
            }
            if (state.whenSend != PushWhen.Now) {
                Text("Saatlik iş çalıştığında zamanı gelmişse gider; en fazla bir saat gecikebilir.", style = Console.Small, modifier = Modifier.padding(top = 6.dp))
            }
            problem?.takeIf { state.title.isNotBlank() || state.body.isNotBlank() || state.extra.isNotEmpty() }?.let {
                Spacer(Modifier.height(10.dp))
                ConsoleNotice(it, kind = NoticeKind.Error)
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ConsoleSecondaryButton(
                    "Kendime test gönder", viewModel::sendTest, Modifier.weight(1f),
                    enabled = !state.busy && state.shown.filled || !state.busy && state.title.isNotBlank() && state.body.isNotBlank()
                )
                ConsoleButton(
                    if (state.whenSend == PushWhen.Now) "Gönder" else "Planla", { confirm = true }, Modifier.weight(1f),
                    enabled = !state.busy && problem == null, loading = state.busy, icon = Icons.AutoMirrored.Filled.Send
                )
            }
        }
        state.message?.let { ConsoleNotice(it, kind = noticeKindOf(it)) }
        if (state.recent.isNotEmpty()) {
            ConsoleSection("Son gönderilenler") {
                state.recent.forEachIndexed { i, line ->
                    if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(Console.Grid))
                    val status = line.substringAfterLast(" · ")
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(line.substringBeforeLast(" · "), style = Console.Small.copy(color = Console.Ink), modifier = Modifier.weight(1f), maxLines = 2)
                        Tag(
                            status,
                            fg = if (status == "gönderildi") Console.GoodText else Console.WarningText,
                            bg = if (status == "gönderildi") Console.GoodSoft else Console.WarningSoft
                        )
                    }
                }
            }
        }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(if (state.whenSend == PushWhen.Now) "Gönderilsin mi?" else "Planlansın mı?") },
            text = { Text("“${state.title}”\n${state.body}\n\n${PushForm.summary(state.audience, state.extra, state.fallback, state.whenSend)}") },
            confirmButton = { TextButton(onClick = { confirm = false; viewModel.send() }) { Text(if (state.whenSend == PushWhen.Now) "Gönder" else "Planla") } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Vazgeç") } }
        )
    }
}

/** A notification as the shade would show it: app icon, name, title, text. */
@Composable
private fun PushPreview(title: String, body: String) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Console.Grid).padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(Modifier.size(30.dp).clip(CircleShape).background(Console.Accent), contentAlignment = Alignment.Center) {
            Text("K", style = Console.Subtitle.copy(color = androidx.compose.ui.graphics.Color.White))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("Karalak · şimdi", style = Console.Small.copy(fontSize = 11.sp))
            Text(title.ifBlank { "Başlık" }, style = Console.Subtitle, maxLines = 1)
            Text(body.ifBlank { "Metin" }, style = Console.Small.copy(color = Console.Ink), maxLines = 3)
        }
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
            // A command the cron has not run has no doneAt at all, so it cannot be matched by a query: waiting = all - done.
            // Two count queries, so the figure is right however many old commands there are (it used to read 200 documents).
            val commands = runCatching {
                val all = firestore.collection("adminCommands").count().get(AggregateSource.SERVER).await().count
                val done = firestore.collection("adminCommands").whereGreaterThan("doneAt", com.google.firebase.Timestamp(0, 0))
                    .count().get(AggregateSource.SERVER).await().count
                (all - done).coerceAtLeast(0)
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
        verticalArrangement = Arrangement.spacedBy(Console.Gap)
    ) {
        if (!state.loaded) {
            ConsoleSkeleton(rows = 5, rowHeight = 56.dp)
            return@Column
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Console.Gap)) {
            StatTile("Bekleyen hesap silme", state.pendingDeletions?.toString() ?: "—", Modifier.weight(1f))
            StatTile("Bekleyen panel komutu", state.pendingCommands?.toString() ?: "—", Modifier.weight(1f))
        }
        ConsoleCard(Modifier.fillMaxWidth(), padding = 4.dp) {
            state.rows.forEachIndexed { i, r ->
                if (i > 0) Box(Modifier.fillMaxWidth().padding(horizontal = 10.dp).height(1.dp).background(Console.Grid))
                val stale = r.lastRunMillis > 0 && now - r.lastRunMillis > 36 * 3_600_000L
                val (dot, status) = when {
                    r.lastRunMillis == 0L -> Console.InkMuted to "Henüz kayıt yok"
                    r.ok == false -> Console.Critical to "Hata · ${stamp(r.lastRunMillis)} · ${r.error.take(120)}"
                    stale -> Console.Warning to "Uzun süredir çalışmadı · son: ${stamp(r.lastRunMillis)}"
                    else -> Console.Good to "Çalıştı · ${stamp(r.lastRunMillis)}"
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(9.dp).clip(CircleShape).background(dot))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(r.label, style = Console.Subtitle.copy(fontSize = 13.5.sp))
                        Text(status, style = Console.Small.copy(color = if (r.ok == false) Console.Critical else Console.InkSecondary), maxLines = 3)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- audit log

data class AuditState(val lines: List<Triple<String, String, String>> = emptyList(), val loaded: Boolean = false, val failed: Boolean = false)

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
            }.onSuccess { _state.value = AuditState(it, true) }.onFailure { _state.value = AuditState(emptyList(), loaded = true, failed = true) }
        }
    }
}

/** "Denetim kaydı": the latest things done from this panel, as a timeline. */
@Composable
internal fun AuditPanel(viewModel: AuditViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        when {
            !state.loaded -> ConsoleSkeleton(rows = 5, rowHeight = 56.dp)
            state.failed -> ConsoleEmpty(
                Icons.Filled.CloudOff, "Kayıtlar okunamadı",
                body = "İnternet bağlantısını ve bu cihazın geliştirici hesabıyla giriş yaptığını kontrol et.",
                action = { ConsoleButton("Tekrar dene", viewModel::load, icon = Icons.Filled.Refresh, style = ConsoleButtonStyle.Secondary) }
            )
            state.lines.isEmpty() -> ConsoleEmpty(Icons.Filled.History, "Kayıt yok", body = "Panelden yapılan işlemler burada sırayla görünür.")
            else -> ConsoleCard(Modifier.fillMaxWidth(), padding = 4.dp) {
                state.lines.forEachIndexed { i, (who, what, detail) ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 4.dp)) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(if (i == 0) Console.Accent else Console.BorderStrong))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(what, style = Console.Subtitle.copy(fontSize = 13.5.sp))
                            if (detail.isNotBlank()) Text(detail, style = Console.Small.copy(color = Console.Ink))
                            Text(who, style = Console.Small.copy(color = Console.InkMuted, fontSize = 11.sp))
                        }
                    }
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
                // the newest 50 by start time (an unordered limit would drop plans at random once there are more), shown oldest first
                firestore.collection("xpEventPlans").orderBy("startAtMillis", Query.Direction.DESCENDING).limit(50).get().await().documents.map { d ->
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
                admin.audited("xp-plan", "config/xpEvent", "${s.multiplier}x, ${s.durationHours} saat, ${stamp(start)}") { b ->
                    b.set(
                        firestore.collection("xpEventPlans").document(),
                        mapOf(
                            "startAtMillis" to start, "durationHours" to s.durationHours.toLong(), "multiplier" to s.multiplier,
                            "label" to s.label.trim().take(32), "createdAt" to System.currentTimeMillis()
                        )
                    )
                }
            }.onSuccess {
                _state.update { it.copy(busy = false, message = "Planlandı: ${stamp(start)} (en geç bir saat gecikmeyle başlar).") }
                load()
            }.onFailure { _state.update { it.copy(busy = false, message = "Yazılamadı (internet ya da yetki).") } }
        }
    }

    fun cancel(plan: XpPlanRow) {
        viewModelScope.launch {
            runCatching {
                admin.audited("xp-plan-cancel", plan.id, stamp(plan.startAtMillis)) { b -> b.delete(firestore.collection("xpEventPlans").document(plan.id)) }
            }.onSuccess { load() }
        }
    }
}

/** Under the XP event's start/stop buttons: plan one ahead, from a form or from the next special days. */
@Composable
internal fun XpPlanSection(viewModel: XpPlanViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }
    val upcoming = remember { com.sualtikasifi.cizimhafiza.presentation.reports.console.SpecialDays.upcoming(horizonDays = 45).take(3) }
    ConsoleSection("İleri tarihli etkinlik", subtitle = "Zamanı gelince kendiliğinden başlar (en geç bir saat gecikmeyle)") {
        if (upcoming.isNotEmpty()) {
            Overline("Özel günden doldur · sabah 10, bir gün")
            Spacer(Modifier.height(6.dp))
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                upcoming.forEach { d -> ConsoleChip(d.name.take(28), state.label == d.name.take(32), { viewModel.planForDay(d.date, d.name) }, accent = Console.Violet) }
            }
            Spacer(Modifier.height(12.dp))
        }
        Overline("Kaç saat sonra başlasın")
        Spacer(Modifier.height(6.dp))
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(1, 6, 12, 24, 48, 72, 168).forEach { h ->
                ConsoleChip("$h sa", state.startInHours == h, { viewModel.edit { copy(startInHours = h) } })
            }
        }
        Spacer(Modifier.height(12.dp))
        Overline("Süre")
        Spacer(Modifier.height(6.dp))
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(3, 6, 24, 48, 72).forEach { h ->
                ConsoleChip("$h sa", state.durationHours == h, { viewModel.edit { copy(durationHours = h) } })
            }
        }
        Spacer(Modifier.height(12.dp))
        Overline("Çarpan")
        Spacer(Modifier.height(6.dp))
        Segmented(
            options = listOf(1.3, 1.5, 2.0, 3.0).map { it.toString() to "x$it" },
            selected = state.multiplier.toString(),
            onSelect = { m -> viewModel.edit { copy(multiplier = m.toDouble()) } },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        ConsoleTextField(
            value = state.label, onValueChange = { v -> viewModel.edit { copy(label = v.take(32)) } },
            label = "Etkinlik adı", modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(14.dp))
        ConsoleButton("Planla", viewModel::create, Modifier.fillMaxWidth(), enabled = !state.busy, loading = state.busy, icon = Icons.Filled.Schedule, style = ConsoleButtonStyle.Dark)
        state.message?.let { Spacer(Modifier.height(10.dp)); ConsoleNotice(it, kind = noticeKindOf(it)) }
        if (state.plans.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Overline("Planlananlar")
            state.plans.forEach { p ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            (if (p.label.isNotBlank()) p.label else "XP etkinliği") + " · x${p.multiplier}",
                            style = Console.Subtitle.copy(fontSize = 13.5.sp)
                        )
                        Text("${stamp(p.startAtMillis)} · ${p.durationHours} sa", style = Console.Small)
                    }
                    if (p.started) Tag("başladı", fg = Console.GoodText, bg = Console.GoodSoft)
                    else ConsoleButton("İptal", { viewModel.cancel(p) }, height = 32.dp, style = ConsoleButtonStyle.Ghost)
                }
            }
        }
    }
}
