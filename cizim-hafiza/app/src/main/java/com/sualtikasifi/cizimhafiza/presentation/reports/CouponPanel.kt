package com.sualtikasifi.cizimhafiza.presentation.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import com.sualtikasifi.cizimhafiza.presentation.common.AppTextField
import com.sualtikasifi.cizimhafiza.presentation.common.PrimaryButton
import com.sualtikasifi.cizimhafiza.presentation.common.SelectableChip
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
    val recent: List<Coupon> = emptyList()
)

@HiltViewModel
class CouponAdminViewModel @Inject constructor(
    private val couponRepository: CouponRepository
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
    val compact = MaterialTheme.typography.labelMedium
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)
    ) {
        // Reward: gold and XP side by side, then one stepper line per chest tier.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            AppTextField(
                value = state.gold,
                onValueChange = { v -> viewModel.update { copy(gold = v.filter(Char::isDigit).take(6)) } },
                label = "Altın",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            AppTextField(
                value = state.xp,
                onValueChange = { v -> viewModel.update { copy(xp = v.filter(Char::isDigit).take(6)) } },
                label = "XP",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }
        Text("Kasa (hemen açılır, toplam en fazla ${CouponCodes.MAX_CHESTS})", style = compact, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(ChestTier.SILVER to "Çırak", ChestTier.GOLD to "Sanatçı", ChestTier.RARE to "Sürpriz").forEach { (tier, name) ->
                ChestStepper(
                    name = name,
                    count = state.chests[tier] ?: 0,
                    onChange = { n -> viewModel.update { copy(chests = chests + (tier to n.coerceIn(0, CouponCodes.MAX_CHESTS))) } },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        // How it is given out.
        Text("Dağıtım", style = compact, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(0, 3, 5, 10, 20).forEach { n ->
                SelectableChip(
                    label = if (n == 0) "Tek ortak kod" else "$n ayrı kod",
                    selected = state.separateCodes == n,
                    onClick = { viewModel.update { copy(separateCodes = n) } },
                    horizontalPadding = 12.dp,
                    verticalPadding = 6.dp,
                    style = compact
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            if (state.separateCodes == 0) {
                AppTextField(
                    value = state.maxUses,
                    onValueChange = { v -> viewModel.update { copy(maxUses = v.filter(Char::isDigit).take(5)) } },
                    label = "Kaç kişi",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            AppTextField(
                value = state.days,
                onValueChange = { v -> viewModel.update { copy(days = v.filter(Char::isDigit).take(3)) } },
                label = "Gün (0 = süresiz)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }
        if (state.separateCodes == 0) {
            AppTextField(
                value = state.customCode,
                onValueChange = { v -> viewModel.update { copy(customCode = v.uppercase().filter { it.isLetterOrDigit() || it == '-' }.take(CouponCodes.MAX_LENGTH)) } },
                label = "Kod (boşsa otomatik)",
                modifier = Modifier.fillMaxWidth()
            )
        }
        PrimaryButton(text = "Kodu oluştur", onClick = viewModel::create, enabled = !state.busy, modifier = Modifier.fillMaxWidth())
        state.message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        if (state.lastCreated.isNotEmpty()) {
            androidx.compose.foundation.text.selection.SelectionContainer {
                Text(state.lastCreated.joinToString("\n"), style = MaterialTheme.typography.titleSmall)
            }
        }
        if (state.recent.isNotEmpty()) {
            Text("Son kodlar", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 4.dp))
        }
        state.recent.forEach { c ->
            val reward = listOfNotNull(
                c.gold.takeIf { it > 0 }?.let { "$it altın" },
                c.xp.takeIf { it > 0 }?.let { "$it XP" },
                c.chests[ChestTier.SILVER]?.let { "$it Çırak" },
                c.chests[ChestTier.GOLD]?.let { "$it Sanatçı" },
                c.chests[ChestTier.RARE]?.let { "$it Sürpriz" }
            ).joinToString(", ")
            val until = if (c.expiresAtMillis > 0) " · " + SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(c.expiresAtMillis)) else ""
            Text(
                text = "${c.code} · $reward · ${c.usedCount}/${c.maxUses}$until",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/** One chest tier's count with a minus and a plus beside it — three of these fit on one line. */
@Composable
private fun ChestStepper(name: String, count: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, modifier = modifier) {
        Text(name, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SelectableChip(label = "−", selected = false, onClick = { onChange(count - 1) }, horizontalPadding = 10.dp, verticalPadding = 4.dp)
            Text(count.toString(), style = MaterialTheme.typography.titleMedium)
            SelectableChip(label = "+", selected = false, onClick = { onChange(count + 1) }, horizontalPadding = 10.dp, verticalPadding = 4.dp)
        }
    }
}
