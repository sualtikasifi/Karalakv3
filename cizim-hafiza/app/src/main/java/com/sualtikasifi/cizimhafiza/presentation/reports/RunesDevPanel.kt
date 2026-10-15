package com.sualtikasifi.cizimhafiza.presentation.reports

import androidx.compose.foundation.layout.Arrangement
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
import com.sualtikasifi.cizimhafiza.presentation.common.PrimaryButton
import com.sualtikasifi.cizimhafiza.presentation.common.RaisedCard
import com.sualtikasifi.cizimhafiza.util.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.NumberFormat
import javax.inject.Inject

@HiltViewModel
class RunesDevViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val runeConfigRepository: com.sualtikasifi.cizimhafiza.data.repository.RuneConfigRepository,
    private val economyConfigRepository: com.sualtikasifi.cizimhafiza.data.repository.EconomyConfigRepository
) : ViewModel() {
    val levels = settingsRepository.runeLevels

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
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RaisedCard(corner = 20.dp, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.runes_dev_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    stringResource(R.string.runes_dev_state, boughtLevels, format.format(spent)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                refunded?.let {
                    Text(
                        stringResource(R.string.runes_dev_done, format.format(it)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                PrimaryButton(
                    text = stringResource(R.string.runes_dev_refresh_config),
                    onClick = viewModel::refreshConfig,
                    modifier = Modifier.fillMaxWidth()
                )
                PrimaryButton(
                    text = stringResource(R.string.runes_dev_unlock),
                    onClick = viewModel::unlockAll,
                    modifier = Modifier.fillMaxWidth()
                )
                PrimaryButton(
                    text = stringResource(R.string.runes_dev_reset),
                    onClick = { confirm = true },
                    enabled = boughtLevels > 0,
                    modifier = Modifier.fillMaxWidth()
                )
            }
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
