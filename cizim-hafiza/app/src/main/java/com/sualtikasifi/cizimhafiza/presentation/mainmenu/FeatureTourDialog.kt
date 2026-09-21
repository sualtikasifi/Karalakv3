package com.sualtikasifi.cizimhafiza.presentation.mainmenu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.presentation.common.AppWindowDialog
import com.sualtikasifi.cizimhafiza.presentation.common.PrimaryButton

/** Three-step "what is new" tour shown once: chests, jokers, the store. */
@Composable
internal fun FeatureTourDialog(onFinish: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val pages = listOf(
        Triple("🎁", R.string.tour_chests_title, R.string.tour_chests_body),
        Triple("🃏", R.string.tour_jokers_title, R.string.tour_jokers_body),
        Triple("🛍️", R.string.tour_store_title, R.string.tour_store_body)
    )
    val (emoji, title, body) = pages[step]
    AppWindowDialog(title = stringResource(R.string.tour_heading), onDismiss = onFinish) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = emoji, fontSize = 64.sp)
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Text(text = stringResource(body), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                pages.indices.forEach { i ->
                    Spacer(
                        modifier = Modifier
                            .size(if (i == step) 10.dp else 7.dp)
                            .background(if (i == step) Color(0xFFFF7A21) else Color(0xFFD9B57A), CircleShape)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            PrimaryButton(
                text = stringResource(if (step == pages.lastIndex) R.string.tour_start else R.string.tour_next),
                onClick = { if (step == pages.lastIndex) onFinish() else step++ },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
