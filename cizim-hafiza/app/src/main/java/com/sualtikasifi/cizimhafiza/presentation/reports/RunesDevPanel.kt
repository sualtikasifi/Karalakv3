package com.sualtikasifi.cizimhafiza.presentation.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Console
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleButtonStyle
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleNotice
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleSection
import com.sualtikasifi.cizimhafiza.presentation.reports.console.NoticeKind
import com.sualtikasifi.cizimhafiza.presentation.reports.console.StatTile
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleSwitchRow
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Overline
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Segmented
import com.sualtikasifi.cizimhafiza.presentation.reports.console.noticeKindOf
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.RuneTree
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleButton
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleSurface
import com.sualtikasifi.cizimhafiza.util.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.NumberFormat
import javax.inject.Inject

@HiltViewModel
class RunesDevViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val runeConfigRepository: com.sualtikasifi.cizimhafiza.data.repository.RuneConfigRepository,
    private val economyConfigRepository: com.sualtikasifi.cizimhafiza.data.repository.EconomyConfigRepository,
    private val firestore: com.google.firebase.firestore.FirebaseFirestore,
    private val admin: com.sualtikasifi.cizimhafiza.util.PlayerAdmin
) : ViewModel() {
    val levels = settingsRepository.runeLevels

    /** The mascot's first-rune offer as the server has it (config/runes), and the last save's outcome. */
    data class PromptState(val on: Boolean, val snoozeDays: Int, val busy: Boolean = false, val message: String? = null)

    private val _prompt = kotlinx.coroutines.flow.MutableStateFlow(
        PromptState(com.sualtikasifi.cizimhafiza.domain.model.RuneGates.firstRunePrompt, com.sualtikasifi.cizimhafiza.domain.model.RuneGates.promptSnoozeDays)
    )
    val prompt: kotlinx.coroutines.flow.StateFlow<PromptState> = _prompt

    /** Turns the offer on or off for every player and sets how long it keeps quiet once closed (config/runes). */
    fun savePrompt(on: Boolean, snoozeDays: Int) {
        _prompt.value = _prompt.value.copy(on = on, snoozeDays = snoozeDays, busy = true, message = null)
        viewModelScope.launch {
            runCatching {
                firestore.document("config/runes").set(
                    mapOf("firstRunePrompt" to on, "promptSnoozeDays" to snoozeDays.toLong()),
                    com.google.firebase.firestore.SetOptions.merge()
                ).await()
            }.onSuccess {
                admin.audit("rune-prompt", "config/runes", "firstRunePrompt=$on, promptSnoozeDays=$snoozeDays")
                runeConfigRepository.refreshNow()
                _prompt.value = _prompt.value.copy(busy = false, message = "Kaydedildi. Telefonlara birkaç saat içinde ulaşır.")
            }.onFailure {
                _prompt.value = _prompt.value.copy(busy = false, message = "Yazılamadı (internet ya da yetki).")
            }
        }
    }

    /** This phone only: the offer and the tour's rune page come back, to look at them again. */
    fun replayPromptHere() {
        settingsRepository.runePromptSnoozedUntil = 0L
        settingsRepository.runesTourSeen = false
        _prompt.value = _prompt.value.copy(message = "Bu cihazda teklif ve tur sayfası yeniden gösterilecek (rün alınmamışsa).")
    }

    /** Takes every rune level back and returns the gold they cost, which is paid back too. */
    fun resetAll(): Int = settingsRepository.resetRunes()

    /** Gives every rune its top level without paying, to look at the whole map. */
    fun unlockAll() = settingsRepository.unlockAllRunes()

    /** Reads config/runes and config/economy from the server now (they are otherwise re-read only every few hours). */
    fun refreshConfig() {
        runeConfigRepository.refreshNow()
        economyConfigRepository.refreshNow()
    }
}

/** The developer panel's rune tab: one button that undoes every rune upgrade, for testing the map from the start. */
@Composable
fun RunesDevPanel(viewModel: RunesDevViewModel = hiltViewModel()) {
    val levels by viewModel.levels.collectAsState()
    var confirm by remember { mutableStateOf(false) }
    var refunded by remember { mutableStateOf<Int?>(null) }
    val format = remember { NumberFormat.getIntegerInstance() }
    val boughtLevels = RuneTree.all.sumOf { RuneTree.levelOf(levels, it.id).coerceAtMost(it.maxLevel) - (if (it.id == RuneTree.ROOT_ID) 1 else 0) }
    val spent = RuneTree.all.filter { it.id != RuneTree.ROOT_ID }.sumOf { r -> r.costs.take(RuneTree.levelOf(levels, r.id)).sum() }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(Console.Gap)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Console.Gap)) {
            StatTile("Alınan rün seviyesi", boughtLevels.toString(), Modifier.weight(1f))
            StatTile("Harcanan altın", format.format(spent), Modifier.weight(1f))
        }
        refunded?.let { ConsoleNotice(stringResource(R.string.runes_dev_done, format.format(it)), kind = NoticeKind.Success) }
        val prompt by viewModel.prompt.collectAsState()
        ConsoleSection("İlk rün teklifi", subtitle = "Hiç rün almamış ve altını yeten oyuncuya maskot balonla rünleri gösterir") {
            ConsoleSwitchRow(
                title = if (prompt.on) "Açık" else "Kapalı",
                subtitle = "Tüm oyuncular için (config/runes)",
                checked = prompt.on,
                enabled = !prompt.busy,
                onChange = { viewModel.savePrompt(it, prompt.snoozeDays) }
            )
            Spacer(Modifier.height(8.dp))
            Overline("Kapatılınca kaç gün sussun")
            Spacer(Modifier.height(6.dp))
            Segmented(
                options = listOf(1, 3, 7, 14).map { it.toString() to "$it gün" },
                selected = prompt.snoozeDays.toString(),
                onSelect = { viewModel.savePrompt(prompt.on, it.toInt()) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            ConsoleButton(
                text = "Bu cihazda yeniden göster",
                onClick = viewModel::replayPromptHere,
                icon = Icons.Filled.Refresh,
                style = ConsoleButtonStyle.Secondary,
                height = 38.dp,
                modifier = Modifier.fillMaxWidth()
            )
            prompt.message?.let { Spacer(Modifier.height(8.dp)); ConsoleNotice(it, kind = noticeKindOf(it)) }
        }
        ConsoleSection(stringResource(R.string.runes_dev_title), subtitle = stringResource(R.string.runes_dev_state, boughtLevels, format.format(spent))) {
            ConsoleButton(
                text = stringResource(R.string.runes_dev_refresh_config),
                onClick = viewModel::refreshConfig,
                icon = Icons.Filled.Refresh,
                style = ConsoleButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            ConsoleButton(
                text = stringResource(R.string.runes_dev_unlock),
                onClick = viewModel::unlockAll,
                icon = Icons.Filled.LockOpen,
                style = ConsoleButtonStyle.Tonal,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            ConsoleButton(
                text = stringResource(R.string.runes_dev_reset),
                onClick = { confirm = true },
                enabled = boughtLevels > 0,
                icon = Icons.Filled.RestartAlt,
                style = ConsoleButtonStyle.DangerSoft,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.runes_dev_confirm_title)) },
            text = { Text(stringResource(R.string.runes_dev_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    refunded = viewModel.resetAll()
                    confirm = false
                }) { Text(stringResource(R.string.runes_dev_reset)) }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.reports_feedback_delete_confirm_cancel)) }
            }
        )
    }
}
