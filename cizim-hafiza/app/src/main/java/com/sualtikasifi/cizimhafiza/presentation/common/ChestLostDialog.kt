package com.sualtikasifi.cizimhafiza.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sualtikasifi.cizimhafiza.R

/** Shown when a win earned a chest but all four slots were full — so the player is told, not silently robbed. */
@Composable
fun ChestLostDialog(onDismiss: () -> Unit) {
    AppWindowDialog(title = stringResource(R.string.chest_lost_title), onDismiss = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "📦", style = MaterialTheme.typography.displayMedium)
            Text(
                text = stringResource(R.string.chest_lost_body),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            PrimaryButton(text = stringResource(R.string.chest_won_button), onClick = onDismiss, modifier = Modifier.fillMaxWidth())
        }
    }
}
