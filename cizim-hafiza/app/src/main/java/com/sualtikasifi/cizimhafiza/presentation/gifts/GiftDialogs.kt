package com.sualtikasifi.cizimhafiza.presentation.gifts

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
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
import com.sualtikasifi.cizimhafiza.domain.model.AvatarFrame
import com.sualtikasifi.cizimhafiza.domain.model.GiftBlock
import com.sualtikasifi.cizimhafiza.domain.model.GiftKind
import com.sualtikasifi.cizimhafiza.domain.model.GiftOffer
import com.sualtikasifi.cizimhafiza.domain.model.GiftRules
import com.sualtikasifi.cizimhafiza.domain.model.GiftUsage
import com.sualtikasifi.cizimhafiza.domain.model.IncomingGift
import com.sualtikasifi.cizimhafiza.domain.model.JokerType
import com.sualtikasifi.cizimhafiza.domain.model.PenSkin
import com.sualtikasifi.cizimhafiza.domain.repository.GiftRepository
import com.sualtikasifi.cizimhafiza.presentation.common.AppWindowDialog
import com.sualtikasifi.cizimhafiza.presentation.common.FitText
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedPillButton
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedStyle
import com.sualtikasifi.cizimhafiza.presentation.common.labelRes
import com.sualtikasifi.cizimhafiza.presentation.common.nameRes

private val Ink = Color(0xFF3B2314)
private val InkSoft = Color(0xFF7A5A40)

@StringRes
fun GiftBlock.messageRes(): Int = when (this) {
    GiftBlock.CHILD_MODE -> R.string.gift_block_child
    GiftBlock.NOT_LINKED -> R.string.gift_block_link
    GiftBlock.LEVEL -> R.string.gift_block_level
    GiftBlock.ACCOUNT_AGE -> R.string.gift_block_age
    GiftBlock.FRIENDSHIP_AGE -> R.string.gift_block_friendship
    GiftBlock.FRIEND_NOT_READY -> R.string.gift_block_friend
    GiftBlock.DISABLED -> R.string.gift_block_disabled
    GiftBlock.OFFLINE -> R.string.gift_block_offline
    GiftBlock.NO_GOLD -> R.string.gift_block_gold
    GiftBlock.LIMIT -> R.string.gift_block_limit
    GiftBlock.REFUSED -> R.string.gift_block_refused
}

@Composable
private fun giftName(kind: GiftKind, itemId: String, qty: Int): String = when (kind) {
    GiftKind.GOLD -> stringResource(R.string.gift_gold_name)
    GiftKind.PEN -> PenSkin.entries.firstOrNull { it.name == itemId }?.let { stringResource(it.labelRes) } ?: itemId
    GiftKind.FRAME -> AvatarFrame.entries.firstOrNull { it.name == itemId }?.let { stringResource(it.nameRes()) } ?: itemId
    GiftKind.JOKER -> (JokerType.entries.firstOrNull { it.name == itemId }?.let { stringResource(it.labelRes()) } ?: itemId) + " ×$qty"
}

/** Pick what to send to [friendName]: gold on top, items below, today's allowance as the caption. */
@Composable
fun GiftSendDialog(state: GiftSendState, gold: Int, onSend: (GiftOffer) -> Unit, onClose: () -> Unit) {
    AppWindowDialog(title = stringResource(R.string.gift_send_title, state.friendName), onDismiss = onClose) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            when {
                state.loading -> Note(stringResource(R.string.gift_loading))
                state.blocked != null -> Note(stringResource(state.blocked.messageRes()))
                state.sent != null -> Note(stringResource(R.string.gift_sent))
                else -> {
                    val usage = state.usage ?: GiftUsage(0, 0, 0)
                    Note(stringResource(R.string.gift_today_left, usage.countLeft, usage.goldLeft.coerceAtMost(usage.friendLeft)))
                    state.error?.let { Note(stringResource(it.messageRes()), error = true) }
                    Section(stringResource(R.string.gift_section_gold))
                    GiftRules.goldOffers().forEach { Offer(it, gold, usage, state.sending, onSend) }
                    val items = GiftRules.itemOffers()
                    if (items.isNotEmpty()) {
                        Section(stringResource(R.string.gift_section_items))
                        items.forEach { Offer(it, gold, usage, state.sending, onSend) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(text: String) {
    FitText(text, PaintedStyle(color = InkSoft, fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Start),
        contentAlignment = Alignment.CenterStart, modifier = Modifier.fillMaxWidth().height(24.dp).padding(top = 2.dp))
}

@Composable
private fun Note(text: String, error: Boolean = false) {
    androidx.compose.material3.Text(
        text, color = if (error) Color(0xFFB3401A) else Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    )
}

@Composable
private fun Offer(offer: GiftOffer, gold: Int, usage: GiftUsage, sending: Boolean, onSend: (GiftOffer) -> Unit) {
    val ok = gold >= offer.cost && usage.canAfford(offer) && !sending
    val shape = RoundedCornerShape(16.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xFFFFFAEC), Color(0xFFFBE9C4))), shape)
            .border(2.dp, Color(0xFFE2C28A), shape)
            .clickable(enabled = ok) { onSend(offer) }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(Modifier.weight(1f)) {
            androidx.compose.material3.Text(
                if (offer.kind == GiftKind.GOLD) stringResource(R.string.gift_gold_amount, offer.receives) else giftName(offer.kind, offer.itemId, offer.qty),
                color = Ink, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1
            )
            if (offer.kind == GiftKind.GOLD) androidx.compose.material3.Text(stringResource(R.string.gift_fee_note), color = InkSoft, fontSize = 12.sp, maxLines = 1)
        }
        Image(painterResource(R.drawable.icon_gold_coin), null, Modifier.size(22.dp))
        Spacer(Modifier.width(4.dp))
        androidx.compose.material3.Text("${offer.cost}", color = if (ok) Ink else Color(0xFFB0A090), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
    }
}

/** What arrived: one card per gift and one button to take them all. */
@Composable
fun GiftInboxDialog(gifts: List<IncomingGift>, onClaim: () -> Unit) {
    AppWindowDialog(
        title = stringResource(R.string.gift_inbox_title), onDismiss = onClaim, centerTitle = true,
        footer = {
            PaintedPillButton(stringResource(R.string.gift_accept), onClaim, height = 50.dp, textSize = 21.sp, attention = true,
                modifier = Modifier.fillMaxWidth(0.62f).padding(top = 14.dp, bottom = 8.dp).align(Alignment.CenterHorizontally))
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
            gifts.take(8).forEach { g ->
                Column(Modifier.fillMaxWidth().background(Color(0xFFFBE9C4), RoundedCornerShape(16.dp)).padding(12.dp)) {
                    androidx.compose.material3.Text(stringResource(R.string.gift_from, g.fromName), color = InkSoft, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    androidx.compose.material3.Text(
                        if (g.kind == GiftKind.GOLD) stringResource(R.string.gift_gold_amount, g.gold) else giftName(g.kind, g.itemId, g.qty),
                        color = Ink, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1
                    )
                }
            }
        }
    }
}

/** What each gift turned into, shown after taking them (an owned item pays out gold instead). */
@Composable
fun GiftResultDialog(results: List<Pair<IncomingGift, GiftRepository.ClaimResult>>, onClose: () -> Unit) {
    AppWindowDialog(
        title = stringResource(R.string.gift_result_title), onDismiss = onClose, centerTitle = true,
        footer = {
            PaintedPillButton(stringResource(R.string.close), onClose, height = 50.dp, textSize = 21.sp, attention = true,
                modifier = Modifier.fillMaxWidth(0.62f).padding(top = 14.dp, bottom = 8.dp).align(Alignment.CenterHorizontally))
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
            results.forEach { (g, r) ->
                val line = when (r) {
                    is GiftRepository.ClaimResult.Gold -> stringResource(R.string.gift_got_gold, r.amount)
                    is GiftRepository.ClaimResult.Converted -> stringResource(R.string.gift_got_converted, r.amount)
                    is GiftRepository.ClaimResult.Item -> giftName(g.kind, g.itemId, g.qty)
                }
                androidx.compose.material3.Text(line, color = Ink, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
