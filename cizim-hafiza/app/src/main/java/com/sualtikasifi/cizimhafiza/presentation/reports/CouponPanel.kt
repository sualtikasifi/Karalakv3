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
    val chest: ChestTier? = null,
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
            gold == 0 && xp == 0 && s.chest == null -> "En az bir ödül seç."
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
                couponRepository.create(Coupon(code, gold, xp, s.chest, maxUses, 0, expires))
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
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        Text(
            text = "Oyuncu kodu Ayarlar'daki kupon alanına yazar. Tek ortak kod için kullanım hakkını (örn. 10) gir, her kazanana ayrı kod için aşağıdan adet seç.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        AppTextField(
            value = state.gold,
            onValueChange = { v -> viewModel.update { copy(gold = v.filter(Char::isDigit).take(6)) } },
            label = "Altın",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )
        AppTextField(
            value = state.xp,
            onValueChange = { v -> viewModel.update { copy(xp = v.filter(Char::isDigit).take(6)) } },
            label = "XP",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )
        Text("Kasa", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 10.dp)) {
            listOf<ChestTier?>(null, ChestTier.SILVER, ChestTier.GOLD, ChestTier.RARE).forEach { tier ->
                SelectableChip(
                    label = when (tier) { null -> "Yok"; ChestTier.SILVER -> "Çırak"; ChestTier.GOLD -> "Sanatçı"; ChestTier.RARE -> "Sürpriz" },
                    selected = state.chest == tier,
                    onClick = { viewModel.update { copy(chest = tier) } },
                    verticalPadding = 10.dp
                )
            }
        }
        Text("Her kazanana ayrı kod", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 10.dp)) {
            listOf(0, 3, 5, 10, 20).forEach { n ->
                SelectableChip(
                    label = if (n == 0) "Hayır, tek ortak kod" else "$n kod",
                    selected = state.separateCodes == n,
                    onClick = { viewModel.update { copy(separateCodes = n) } },
                    verticalPadding = 10.dp
                )
            }
        }
        if (state.separateCodes == 0) {
            AppTextField(
                value = state.maxUses,
                onValueChange = { v -> viewModel.update { copy(maxUses = v.filter(Char::isDigit).take(5)) } },
                label = "Kaç kişi kullanabilir",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
            AppTextField(
                value = state.customCode,
                onValueChange = { v -> viewModel.update { copy(customCode = v.uppercase().filter { it.isLetterOrDigit() || it == '-' }.take(CouponCodes.MAX_LENGTH)) } },
                label = "Kod (boş bırakırsan otomatik üretilir)",
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
        }
        AppTextField(
            value = state.days,
            onValueChange = { v -> viewModel.update { copy(days = v.filter(Char::isDigit).take(3)) } },
            label = "Kaç gün geçerli (0 = süresiz)",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        )
        PrimaryButton(
            text = "Kodu oluştur",
            onClick = viewModel::create,
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )
        state.message?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp))
        }
        if (state.lastCreated.isNotEmpty()) {
            Text("Yeni kodlar (basılı tutup kopyalayabilirsin):", style = MaterialTheme.typography.labelLarge)
            androidx.compose.foundation.text.selection.SelectionContainer {
                Text(
                    state.lastCreated.joinToString("\n"),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 14.dp)
                )
            }
        }
        Text("Son kodlar", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 6.dp, bottom = 6.dp))
        state.recent.forEach { c ->
            val reward = listOfNotNull(
                c.gold.takeIf { it > 0 }?.let { "$it altın" },
                c.xp.takeIf { it > 0 }?.let { "$it XP" },
                c.chest?.let { when (it) { ChestTier.SILVER -> "Çırak"; ChestTier.GOLD -> "Sanatçı"; ChestTier.RARE -> "Sürpriz" } + " kasa" }
            ).joinToString(", ")
            val until = if (c.expiresAtMillis > 0) " · " + SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(c.expiresAtMillis)) + "'e kadar" else ""
            Text(
                text = "${c.code} — $reward — ${c.usedCount}/${c.maxUses}$until",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
    }
}
