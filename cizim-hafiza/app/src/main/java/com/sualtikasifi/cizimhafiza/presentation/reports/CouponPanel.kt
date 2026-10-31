package com.sualtikasifi.cizimhafiza.presentation.reports

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Console
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleButtonStyle
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleCard
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleNotice
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleSection
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleStepper
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Overline
import com.sualtikasifi.cizimhafiza.presentation.reports.console.SectionHeader
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Tag
import com.sualtikasifi.cizimhafiza.presentation.reports.console.noticeKindOf
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.sualtikasifi.cizimhafiza.R
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sualtikasifi.cizimhafiza.domain.model.ChestTier
import com.sualtikasifi.cizimhafiza.domain.model.Coupon
import com.sualtikasifi.cizimhafiza.domain.model.CouponCodes
import com.sualtikasifi.cizimhafiza.domain.repository.CouponRepository
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleTextField
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleButton
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleChip
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

data class CouponAdminState(
    val gold: String = "",
    val xp: String = "",
    /** How many chests of each tier the code gives. */
    val chests: Map<ChestTier, Int> = emptyMap(),
    val maxUses: String = "10",
    val days: String = "7",
    /** Several single-use codes at once ("her kazanana ayrı kod"); 1 = one code with [maxUses] uses. */
    val separateCodes: Int = 0,
    val customCode: String = "",
    val busy: Boolean = false,
    val message: String? = null,
    val lastCreated: List<String> = emptyList(),
    val recent: List<Coupon> = emptyList(),
    /** Who redeemed one code (nicknames), after "Kullananlar" was tapped. */
    val redeemers: Pair<String, List<String>>? = null
)

@HiltViewModel
class CouponAdminViewModel @Inject constructor(
    private val couponRepository: CouponRepository,
    private val firestore: com.google.firebase.firestore.FirebaseFirestore,
    private val admin: com.sualtikasifi.cizimhafiza.util.PlayerAdmin
) : ViewModel() {
    private val _state = MutableStateFlow(CouponAdminState())
    val state: StateFlow<CouponAdminState> = _state.asStateFlow()

    fun update(block: CouponAdminState.() -> CouponAdminState) = _state.update { it.block().copy(message = null) }

    fun refresh() {
        viewModelScope.launch {
            couponRepository.recent(40).onSuccess { list -> _state.update { it.copy(recent = list) } }
                .onFailure { _state.update { s -> s.copy(message = "Liste yüklenemedi.") } }
        }
    }

    /** Ends a code now: its expiry is set to a moment in the past, which the redeem check already treats as expired. */
    fun close(code: String) {
        viewModelScope.launch {
            runCatching {
                firestore.collection("coupons").document(code)
                    .update("expiresAt", 1L).await()
            }.onSuccess {
                admin.audit("coupon-close", code, "")
                _state.update { it.copy(message = "$code kapatıldı.") }
                refresh()
            }.onFailure { _state.update { it.copy(message = "Kapatılamadı (internet ya da yetki).") } }
        }
    }

    fun showRedeemers(code: String) {
        viewModelScope.launch {
            runCatching {
                firestore.collection("coupons").document(code).collection("redemptions").limit(60).get().await().documents.map { d ->
                    runCatching { firestore.collection("users").document(d.id).get().await().getString("nickname") }.getOrNull()
                        ?.takeIf { it.isNotBlank() } ?: d.id.take(8)
                }
            }.onSuccess { names -> _state.update { it.copy(redeemers = code to names) } }
                .onFailure { _state.update { it.copy(message = "Liste yüklenemedi.") } }
        }
    }

    fun hideRedeemers() = _state.update { it.copy(redeemers = null) }

    fun create() {
        val s = _state.value
        if (s.busy) return
        val gold = s.gold.toIntOrNull() ?: 0
        val xp = s.xp.toIntOrNull() ?: 0
        val maxUses = if (s.separateCodes > 0) 1 else (s.maxUses.toIntOrNull() ?: 0)
        val days = s.days.toIntOrNull() ?: 0
        val problem = when {
            gold !in 0..CouponCodes.MAX_GOLD || xp !in 0..CouponCodes.MAX_XP -> "Altın ve XP 0 ile ${CouponCodes.MAX_GOLD} arasında olmalı."
            s.chests.values.sum() > CouponCodes.MAX_CHESTS -> "En fazla ${CouponCodes.MAX_CHESTS} kasa verilebilir (her biri sırayla açılır)."
            gold == 0 && xp == 0 && s.chests.values.sum() == 0 -> "En az bir ödül seç."
            maxUses < 1 -> "Kullanım hakkı en az 1 olmalı."
            s.separateCodes == 0 && s.customCode.isNotBlank() && CouponCodes.normalize(s.customCode) == null ->
                "Kod 4-24 karakter olmalı, sadece harf, rakam ve tire."
            else -> null
        }
        if (problem != null) {
            _state.update { it.copy(message = problem) }
            return
        }
        val expires = if (days > 0) System.currentTimeMillis() + days * 24L * 60 * 60 * 1000 else 0L
        _state.update { it.copy(busy = true, message = null, lastCreated = emptyList()) }
        viewModelScope.launch {
            val count = s.separateCodes.coerceAtLeast(1)
            val codes = if (s.separateCodes == 0 && s.customCode.isNotBlank()) {
                listOf(CouponCodes.normalize(s.customCode)!!)
            } else {
                List(count) { CouponCodes.generate() }.distinct()
            }
            val done = mutableListOf<String>()
            var failed = false
            for (code in codes) {
                couponRepository.create(Coupon(code, gold, xp, s.chests.filterValues { it > 0 }, maxUses, 0, expires))
                    .onSuccess { done += code }
                    .onFailure { failed = true }
            }
            _state.update {
                it.copy(
                    busy = false,
                    lastCreated = done,
                    message = if (failed) "Bazı kodlar oluşturulamadı (internet ya da yetki)." else "${done.size} kod oluşturuldu."
                )
            }
            refresh()
        }
    }
}

/** The Developer Panel's Kupon tab: make codes and see how often each one has been used. */
@Composable
internal fun CouponPanel(viewModel: CouponAdminViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.refresh() }
    val context = LocalContext.current
    Column(
        verticalArrangement = Arrangement.spacedBy(Console.Gap),
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)
    ) {
        ConsoleSection("Yeni kupon", subtitle = "Ödül, dağıtım ve süre") {
            // Reward: gold and XP side by side, then one stepper per chest tier.
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                ConsoleTextField(
                    value = state.gold,
                    onValueChange = { v -> viewModel.update { copy(gold = v.filter(Char::isDigit).take(6)) } },
                    label = "Altın",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                ConsoleTextField(
                    value = state.xp,
                    onValueChange = { v -> viewModel.update { copy(xp = v.filter(Char::isDigit).take(6)) } },
                    label = "XP",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(12.dp))
            Overline(stringResource(R.string.coupon_panel_chests, CouponCodes.MAX_CHESTS))
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(ChestTier.SILVER to "Çırak", ChestTier.GOLD to "Sanatçı", ChestTier.RARE to "Sürpriz").forEach { (tier, name) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(name, style = Console.Label, maxLines = 1)
                        Spacer(Modifier.height(4.dp))
                        ConsoleStepper(
                            value = state.chests[tier] ?: 0,
                            onChange = { n -> viewModel.update { copy(chests = chests + (tier to n.coerceIn(0, CouponCodes.MAX_CHESTS))) } },
                            min = 0, max = CouponCodes.MAX_CHESTS
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            // How it is given out.
            Overline(stringResource(R.string.coupon_panel_distribution))
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0, 3, 5, 10, 20).forEach { n ->
                    ConsoleChip(
                        label = if (n == 0) "Tek ortak kod" else "$n ayrı kod",
                        selected = state.separateCodes == n,
                        onClick = { viewModel.update { copy(separateCodes = n) } }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                if (state.separateCodes == 0) {
                    ConsoleTextField(
                        value = state.maxUses,
                        onValueChange = { v -> viewModel.update { copy(maxUses = v.filter(Char::isDigit).take(5)) } },
                        label = "Kaç kişi",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                ConsoleTextField(
                    value = state.days,
                    onValueChange = { v -> viewModel.update { copy(days = v.filter(Char::isDigit).take(3)) } },
                    label = "Gün", supporting = "0 = süresiz",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            if (state.separateCodes == 0) {
                Spacer(Modifier.height(10.dp))
                ConsoleTextField(
                    value = state.customCode,
                    onValueChange = { v -> viewModel.update { copy(customCode = v.uppercase().filter { it.isLetterOrDigit() || it == '-' }.take(CouponCodes.MAX_LENGTH)) } },
                    label = "Kod", placeholder = "Boş bırakılırsa otomatik",
                    textStyle = Console.Body.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(14.dp))
            ConsoleButton(text = "Kodu oluştur", onClick = viewModel::create, enabled = !state.busy, loading = state.busy, icon = Icons.Filled.ConfirmationNumber, modifier = Modifier.fillMaxWidth())
        }
        state.message?.let { ConsoleNotice(it, kind = noticeKindOf(it)) }
        if (state.lastCreated.isNotEmpty()) {
            ConsoleCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Oluşturulan kodlar", style = Console.Subtitle, modifier = Modifier.weight(1f))
                    if (state.lastCreated.size > 1) {
                        // all of them at once, one per line, ready to paste into a message
                        ConsoleButton("Hepsini kopyala", {
                            copyToClipboard(context, state.lastCreated.joinToString("\n"), "${state.lastCreated.size} kod kopyalandı")
                        }, icon = Icons.Filled.ContentCopy, height = 32.dp, style = ConsoleButtonStyle.Tonal)
                    }
                }
                Spacer(Modifier.height(8.dp))
                state.lastCreated.forEach { code -> CodeChip(code) { copyToClipboard(context, code) } }
            }
        }
        if (state.recent.isNotEmpty()) {
            SectionHeader(stringResource(R.string.coupon_panel_recent))
            ConsoleCard(Modifier.fillMaxWidth(), padding = 4.dp) {
                state.recent.forEachIndexed { i, c ->
                    if (i > 0) Box(Modifier.fillMaxWidth().padding(horizontal = 10.dp).height(1.dp).background(Console.Grid))
                    val reward = listOfNotNull(
                        c.gold.takeIf { it > 0 }?.let { "$it altın" },
                        c.xp.takeIf { it > 0 }?.let { "$it XP" },
                        c.chests[ChestTier.SILVER]?.let { "$it Çırak" },
                        c.chests[ChestTier.GOLD]?.let { "$it Sanatçı" },
                        c.chests[ChestTier.RARE]?.let { "$it Sürpriz" }
                    ).joinToString(", ")
                    // the developer panel is Turkish throughout, so its dates are too
                    val until = if (c.expiresAtMillis > 0) SimpleDateFormat("d MMM", Locale("tr")).format(Date(c.expiresAtMillis)) else "süresiz"
                    val closed = c.expiresAtMillis in 1..System.currentTimeMillis()
                    val full = c.usedCount >= c.maxUses
                    Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                c.code,
                                style = Console.Subtitle.copy(fontFamily = FontFamily.Monospace),
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(4.dp)).clickable { copyToClipboard(context, c.code) },
                                maxLines = 1
                            )
                            when {
                                closed -> Tag("kapalı", fg = Console.InkSecondary, bg = Console.Grid)
                                full -> Tag("doldu", fg = Console.WarningText, bg = Console.WarningSoft)
                                else -> Tag("açık", fg = Console.GoodText, bg = Console.GoodSoft)
                            }
                        }
                        Text("$reward · $until", style = Console.Small)
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            UsageBar(c.usedCount, c.maxUses, Modifier.weight(1f))
                            Spacer(Modifier.width(8.dp))
                            Text("${c.usedCount}/${c.maxUses}", style = Console.Label)
                        }
                        if (c.usedCount > 0 || (!closed && !full)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                                if (c.usedCount > 0) {
                                    ConsoleButton("Kullananlar (${c.usedCount})", { viewModel.showRedeemers(c.code) }, height = 30.dp, style = ConsoleButtonStyle.Secondary)
                                }
                                if (!closed && !full) {
                                    ConsoleButton("Kapat", { viewModel.close(c.code) }, height = 30.dp, style = ConsoleButtonStyle.DangerSoft)
                                }
                            }
                        }
                        state.redeemers?.takeIf { it.first == c.code }?.let { (_, names) ->
                            Text(
                                names.joinToString(", ").ifEmpty { "-" },
                                style = Console.Small.copy(color = Console.Ink),
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Console.SurfaceAlt)
                                    .clickable { viewModel.hideRedeemers() }
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** How much of a coupon is used: a thin bar, accent while there is room, grey once full. */
@Composable
private fun UsageBar(used: Int, max: Int, modifier: Modifier = Modifier) {
    val f = if (max <= 0) 0f else (used.toFloat() / max).coerceIn(0f, 1f)
    Box(modifier.height(6.dp).clip(RoundedCornerShape(3.dp)).background(Console.Grid)) {
        Box(Modifier.fillMaxWidth(f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(if (f >= 1f) Console.InkMuted else Console.Accent))
    }
}

/** A freshly made code, big and monospaced, with a copy button: the line that gets pasted into a message. */
@Composable
private fun CodeChip(code: String, onCopy: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(Console.ControlCorner))
            .background(Console.AccentSoft)
            .clickable(onClick = onCopy)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(code, style = Console.Title.copy(fontFamily = FontFamily.Monospace, color = Console.AccentInk, letterSpacing = 1.sp), modifier = Modifier.weight(1f))
        Icon(Icons.Filled.ContentCopy, contentDescription = "Kopyala", tint = Console.AccentInk, modifier = Modifier.size(18.dp))
    }
}

private fun copyToClipboard(context: Context, text: String, message: String = "Kopyalandı: $text") {
    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Karalak kuponu", text))
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
