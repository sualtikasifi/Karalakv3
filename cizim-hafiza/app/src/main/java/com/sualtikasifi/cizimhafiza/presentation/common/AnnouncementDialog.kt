package com.sualtikasifi.cizimhafiza.presentation.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.sualtikasifi.cizimhafiza.R

/** A notice from the developer panel (config/announcement), in the same card as the other home windows. */
@Composable
fun AnnouncementDialog(title: String, body: String, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        RaisedCard(corner = 24.dp, modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (title.isNotEmpty()) {
                    Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                }
                if (body.isNotEmpty()) {
                    Text(
                        body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())
                    )
                    Spacer(Modifier.height(20.dp))
                }
                PrimaryButton(text = stringResource(R.string.close), onClick = onClose, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
