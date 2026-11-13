package com.sualtikasifi.cizimhafiza.presentation.guess

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.GuessInboxEntry
import com.sualtikasifi.cizimhafiza.presentation.common.AppWindowDialog
import com.sualtikasifi.cizimhafiza.presentation.common.FitText
import com.sualtikasifi.cizimhafiza.presentation.common.LetteredText
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedPillButton
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedStyle

private val Ink = Color(0xFF3B2314)
private val InkSoft = Color(0xFF7A5A40)

/**
 * The window that tells a player their friends tried the drawings they sent as links: one card per friend (a round
 * tick or cross, the name, how it went), the gold earned on a plaque of its own, and the orange button that closes it.
 */
@Composable
fun GuessInboxDialog(entries: List<GuessInboxEntry>, rewardGold: Int, onClose: () -> Unit) {
    AppWindowDialog(
        title = stringResource(R.string.guess_inbox_title),
        onDismiss = onClose,
        centerTitle = true,
        footer = {
            PaintedPillButton(
                text = stringResource(R.string.guess_inbox_ok),
                onClick = onClose,
                height = 60.dp,
                textSize = 24.sp,
                attention = true,
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp, start = 6.dp, end = 6.dp)
            )
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
            entries.take(MAX_LINES).forEach { e -> EntryCard(e) }
            if (entries.size > MAX_LINES) {
                FitText(
                    text = "+${entries.size - MAX_LINES}",
                    style = PaintedStyle(color = InkSoft, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                    modifier = Modifier.fillMaxWidth().height(24.dp)
                )
            }
            if (rewardGold > 0) RewardPlaque(rewardGold)
        }
    }
}

/** One friend's result: a tick or a cross in a coloured disc, the name, and how many tries it took. */
@Composable
private fun EntryCard(e: GuessInboxEntry) {
    val shape = RoundedCornerShape(18.dp)
    val tone = if (e.solved) Color(0xFF2EA043) else Color(0xFFD9483B)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xFFFFFAEC), Color(0xFFFBE9C4))), shape)
            .border(2.dp, Color(0xFFE2C28A), shape)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .background(Brush.verticalGradient(listOf(tone.copy(alpha = 0.85f), tone)), CircleShape)
                .border(2.dp, Color.White, CircleShape)
        ) {
            Icon(if (e.solved) Icons.Filled.Check else Icons.Filled.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            FitText(
                text = e.fromName,
                style = PaintedStyle(color = Ink, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Start),
                contentAlignment = Alignment.CenterStart,
                minScale = 0.6f,
                modifier = Modifier.fillMaxWidth().height(26.dp)
            )
            FitText(
                text = when {
                    !e.solved -> stringResource(R.string.guess_inbox_failed_sub)
                    e.attempts == 1 -> stringResource(R.string.guess_inbox_first)
                    else -> stringResource(R.string.guess_inbox_tries, e.attempts)
                },
                style = PaintedStyle(color = if (e.solved) Color(0xFF1B7A30) else Color(0xFFB3401A), fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Start),
                contentAlignment = Alignment.CenterStart,
                minScale = 0.7f,
                modifier = Modifier.fillMaxWidth().height(22.dp)
            )
        }
    }
}

/** What the answers earned, on a plaque: the coin, the amount in white on the dark plank, and a small caption. */
@Composable
private fun RewardPlaque(gold: Int) {
    val shape = RoundedCornerShape(50)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        FitText(
            text = stringResource(R.string.guess_inbox_total),
            style = PaintedStyle(color = InkSoft, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
            modifier = Modifier.fillMaxWidth().height(22.dp)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(top = 4.dp)
                .background(Brush.verticalGradient(listOf(Color(0xFF6A3A1E), Color(0xFF3F1F0E))), shape)
                .border(3.dp, Color(0xFFD9A441), shape)
                .padding(start = 10.dp, end = 26.dp, top = 6.dp, bottom = 6.dp)
        ) {
            Image(painterResource(R.drawable.icon_gold_coin), contentDescription = null, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(10.dp))
            LetteredText(
                text = "+$gold",
                size = 30.sp,
                fill = Color(0xFFFFE066),
                outline = Color(0xFF3A1A08),
                modifier = Modifier.height(40.dp),
                minScale = 0.6f
            )
        }
    }
}

private const val MAX_LINES = 8
