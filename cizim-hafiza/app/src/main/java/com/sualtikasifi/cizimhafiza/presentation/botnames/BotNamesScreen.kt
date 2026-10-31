package com.sualtikasifi.cizimhafiza.presentation.botnames

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Console
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleButton
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleCard
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleEmpty
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleIconButton
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleNotice
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleScaffold
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleSection
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleTextField
import com.sualtikasifi.cizimhafiza.presentation.reports.console.IdentityChip
import com.sualtikasifi.cizimhafiza.presentation.reports.console.NoticeKind

import com.sualtikasifi.cizimhafiza.domain.model.BotNameEntry
import com.sualtikasifi.cizimhafiza.util.asString

/**
 * Where the curated bot-nickname pool (see BotNameRepository) actually gets
 * typed in — one name at a time, by hand, until there are enough
 * (200-300, per the plan) for functions/src/index.ts's league builder to
 * switch over from the old procedurally generated names. That switch is a
 * separate, later step; this screen only fills the pool.
 */
@Composable
fun BotNamesScreen(onBack: () -> Unit, viewModel: BotNamesViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var identityShown by remember { mutableStateOf(false) }

    ConsoleScaffold(
        title = stringResource(R.string.menu_bot_names),
        overline = "Araçlar",
        onBack = onBack,
        // Always on screen, not only after a failure — see DrawingReportsScreen. botNames is reviewer()-only end to
        // end, so a device signed in under any other account reads back an empty list with no error of its own;
        // without this, "0 kayıtlı isim" is indistinguishable from a genuinely empty pool.
        actions = { uiState.identity?.let { who -> IdentityChip(ok = who.isReviewer, onClick = { identityShown = true }) } }
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Console.Gap)) {
            ConsoleSection(
                title = "Yeni isim",
                subtitle = stringResource(R.string.bot_names_count_format, uiState.names.size)
            ) {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ConsoleTextField(
                        value = uiState.input,
                        onValueChange = viewModel::setInput,
                        placeholder = stringResource(R.string.bot_names_input_label),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { viewModel.addName() }),
                        modifier = Modifier.weight(1f)
                    )
                    ConsoleButton(
                        text = stringResource(R.string.bot_names_add_button),
                        onClick = viewModel::addName,
                        enabled = uiState.input.isNotBlank() && !uiState.isSubmitting,
                        loading = uiState.isSubmitting,
                        icon = Icons.Filled.Add,
                        height = 46.dp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                if (uiState.duplicateWarning) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ConsoleNotice(stringResource(R.string.bot_names_duplicate), kind = NoticeKind.Warning)
                }
            }
            uiState.errorMessage?.let { message -> ConsoleNotice(message.asString(), kind = NoticeKind.Error) }
            if (uiState.names.isEmpty()) {
                ConsoleEmpty(Icons.Filled.Badge, stringResource(R.string.bot_names_empty))
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    items(uiState.names, key = { it.id }) { entry ->
                        BotNameRow(entry = entry, onDelete = { viewModel.deleteName(entry.id) }, modifier = Modifier.animateItem())
                    }
                }
            }
        }
    }
    if (identityShown) {
        uiState.identity?.let { who ->
            AlertDialog(
                onDismissRequest = { identityShown = false },
                title = { Text(if (who.isReviewer) "Yetkili hesap" else "Yetkisiz hesap") },
                text = {
                    Text(
                        if (who.isReviewer) stringResource(R.string.reports_identity_ok, who.email.orEmpty())
                        else stringResource(R.string.reports_identity_wrong, who.uid.orEmpty())
                    )
                },
                confirmButton = { TextButton(onClick = { identityShown = false }) { Text("Tamam") } }
            )
        }
    }
}

@Composable
private fun BotNameRow(entry: BotNameEntry, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    ConsoleCard(modifier = modifier.fillMaxWidth(), padding = 0.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = entry.name, style = Console.Body, modifier = Modifier.weight(1f))
            ConsoleIconButton(
                icon = Icons.Filled.Delete,
                contentDescription = stringResource(R.string.bot_names_delete_content_description),
                onClick = onDelete,
                size = 34.dp,
                tint = Console.Critical,
                bordered = false
            )
        }
    }
}
