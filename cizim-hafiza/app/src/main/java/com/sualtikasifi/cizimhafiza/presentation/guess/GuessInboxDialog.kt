package com.sualtikasifi.cizimhafiza.presentation.guess

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.GuessInboxEntry
import com.sualtikasifi.cizimhafiza.presentation.common.AppWindowDialog
import com.sualtikasifi.cizimhafiza.presentation.common.PrimaryButton

/** The window that tells a player their friends tried the drawings they sent as links. */
@Composable
fun GuessInboxDialog(entries: List<GuessInboxEntry>, rewardGold: Int, onClose: () -> Unit) {
    AppWindowDialog(
        title = stringResource(R.string.guess_inbox_title),
        onDismiss = onClose,
        centerTitle = true,
        footer = {
            PrimaryButton(text = stringResource(R.string.guess_inbox_ok), onClick = onClose, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            entries.take(MAX_LINES).forEach { e ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(if (e.solved) "✅" else "❌", modifier = Modifier.padding(end = 10.dp))
                    Text(
                        text = if (e.solved) stringResource(R.string.guess_inbox_solved, e.fromName, e.attempts)
                        else stringResource(R.string.guess_inbox_failed, e.fromName),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            if (entries.size > MAX_LINES) {
                Text("+${entries.size - MAX_LINES}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (rewardGold > 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.guess_inbox_reward, rewardGold),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private const val MAX_LINES = 8
