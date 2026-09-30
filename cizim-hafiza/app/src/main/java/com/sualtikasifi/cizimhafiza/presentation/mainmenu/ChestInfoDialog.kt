package com.sualtikasifi.cizimhafiza.presentation.mainmenu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.ChestSlots
import com.sualtikasifi.cizimhafiza.domain.model.ChestTier
import com.sualtikasifi.cizimhafiza.presentation.chests.ChestImage
import com.sualtikasifi.cizimhafiza.presentation.chests.accent
import com.sualtikasifi.cizimhafiza.presentation.chests.durationHours
import com.sualtikasifi.cizimhafiza.presentation.common.AppWindowDialog
import com.sualtikasifi.cizimhafiza.presentation.common.PrimaryButton
import com.sualtikasifi.cizimhafiza.presentation.common.labelRes
import com.sualtikasifi.cizimhafiza.presentation.theme.DisplayFont

private val InfoInk = Color(0xFF3A2416)
private val InfoSoft = Color(0xFF7A5A44)

/**
 * "Kasalar nasıl çalışır?" as a compact guide that fits on one screen: a three-step timeline, one card
 * per chest (picture, opening time, what is inside) and the ad speed-up tip, with the confirm button
 * pinned at the bottom. Only a genuinely short screen falls back to scrolling.
 */
@Composable
internal fun ChestInfoDialog(onDismiss: () -> Unit) {
    val shortScreen = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp < 760
    AppWindowDialog(
        title = stringResource(R.string.home_chests_info_title),
        onDismiss = onDismiss,
        centerTitle = true,
        scrollBody = shortScreen,
        footer = {
            PrimaryButton(
                text = stringResource(R.string.home_chests_info_ok),
                onClick = onDismiss,
                height = 50.dp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    ) {
        InfoStep(1, "🏆", stringResource(R.string.chest_info_step_win_title), stringResource(R.string.chest_info_step_win_body), first = true, last = false)
        InfoStep(2, "⏳", stringResource(R.string.chest_info_step_open_title), stringResource(R.string.chest_info_step_open_body), first = false, last = false)
        InfoStep(3, "🔓", stringResource(R.string.chest_info_step_slots_title), stringResource(R.string.chest_info_step_slots_body, ChestSlots.SLOT_COUNT), first = false, last = true)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.chest_info_contents_title),
            fontFamily = DisplayFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            color = InfoInk,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            ChestTier.entries.forEach { tier -> InfoChestCard(tier) }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF2E8B45).copy(alpha = 0.12f))
                .border(1.5.dp, Color(0xFF2E8B45).copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "🎬", fontSize = 20.sp)
            Text(
                text = stringResource(R.string.chest_info_speedup_tip),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = InfoInk
            )
        }
    }
}

/**
 * One numbered step on a vertical timeline: a glossy orange badge (white ring, soft shadow) joined to the
 * next one by a thin line, with the step's emoji-led title and a short explanation beside it.
 */
@Composable
private fun InfoStep(number: Int, emoji: String, title: String, body: String, first: Boolean, last: Boolean) {
    val line = Color(0xFFFF7A21).copy(alpha = 0.45f)
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .fillMaxHeight()
                .drawBehind {
                    val x = size.width / 2f
                    val mid = size.height / 2f
                    if (!first) drawLine(line, Offset(x, 0f), Offset(x, mid), strokeWidth = 3.dp.toPx())
                    if (!last) drawLine(line, Offset(x, mid), Offset(x, size.height), strokeWidth = 3.dp.toPx())
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(Color(0xFFFFA24D), Color(0xFFF2611B))))
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number.toString(),
                    color = Color.White,
                    fontFamily = DisplayFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
            }
        }
        Column(modifier = Modifier.weight(1f).padding(vertical = 2.dp)) {
            Text(text = "$emoji  $title", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = InfoInk)
            Text(text = body, fontSize = 12.sp, lineHeight = 15.sp, color = InfoSoft)
        }
    }
}

@Composable
private fun InfoChestCard(tier: ChestTier) {
    val accent = tier.accent()
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(accent.copy(alpha = 0.10f))
            .border(1.5.dp, accent.copy(alpha = 0.5f), shape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(modifier = Modifier.width(48.dp), contentAlignment = Alignment.Center) {
            ChestImage(tier = tier, width = 46.dp)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(tier.labelRes()),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = InfoInk,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = "⏱ " + stringResource(R.string.chest_duration_hours, tier.durationHours()),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(accent)
                        .padding(horizontal = 8.dp, vertical = 1.dp)
                )
            }
            Text(
                text = "🪙 " + stringResource(R.string.chest_detail_gold, tier.goldReward.first, tier.goldReward.last),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = InfoInk
            )
            Text(
                text = "🎲 " + when (tier) {
                    ChestTier.SILVER -> stringResource(R.string.chest_detail_jokers_silver, 30)
                    ChestTier.GOLD -> stringResource(R.string.chest_detail_jokers_gold, 30)
                    ChestTier.RARE -> stringResource(R.string.chest_detail_jokers_rare, 40)
                },
                fontSize = 12.sp,
                color = InfoSoft
            )
            if (tier == ChestTier.RARE) {
                Text(
                    text = "✏️ " + stringResource(R.string.chest_detail_pen, 12),
                    fontSize = 12.sp,
                    color = InfoSoft
                )
            }
        }
    }
}
