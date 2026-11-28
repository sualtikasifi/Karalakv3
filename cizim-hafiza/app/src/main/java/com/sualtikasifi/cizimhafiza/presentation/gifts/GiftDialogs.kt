package com.sualtikasifi.cizimhafiza.presentation.gifts

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.sualtikasifi.cizimhafiza.presentation.common.LetteredText
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedCloseButton
import com.sualtikasifi.cizimhafiza.presentation.common.pressable
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedPillButton
import com.sualtikasifi.cizimhafiza.presentation.common.softShadow
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

/**
 * "Hediye gönder": one painted window (gift_send_window, 924 x 1467 px: the dog and the cat behind a purple banner, a bar
 * for today's allowance, four gold rows and three joker rows, each with an empty yellow price pill). Everything written
 * on it is lettered live in the picture's own proportions; the rows are the buttons. A message (checking, not allowed,
 * sent) takes the place of the rows on a cream panel inside the same frame.
 */
@Composable
fun GiftSendDialog(state: GiftSendState, gold: Int, onSend: (GiftOffer) -> Unit, onClose: () -> Unit) {
    val sent = state.sent
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth(0.94f).widthIn(max = 400.dp).aspectRatio(SendW / SendH)) {
            val w = maxWidth
            val h = maxHeight
            Image(
                painter = painterResource(R.drawable.gift_send_window), contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.FillBounds, modifier = Modifier.fillMaxSize()
            )
            // the purple banner: who the gift is for, or that it has gone
            LetteredText(
                text = if (sent != null) stringResource(R.string.gift_sent_title) else stringResource(R.string.gift_send_title, state.friendName),
                size = 24.sp, fill = Color.White, outline = Color(0xFF3E0B66), title = true, minScale = 0.5f,
                modifier = Modifier.offset(w * 0.29f, h * 0.152f).size(w * 0.52f, h * 0.062f)
            )
            // the red cross, drawn into the picture
            Box(Modifier.offset(w * 0.86f, h * 0.105f).size(w * 0.14f, w * 0.14f).pressable(pressedScale = 0.9f, onClick = onClose))

            when {
                state.loading -> MessagePanel(w, h) { Note(stringResource(R.string.gift_loading)) }
                state.blocked != null -> MessagePanel(w, h) { ErrorCard(stringResource(state.blocked.messageRes())) }
                sent != null -> MessagePanel(w, h) {
                    SentCard(sent)
                    PaintedCloseButton(text = stringResource(R.string.close), onClick = onClose)
                }
                else -> {
                    val usage = state.usage ?: GiftUsage(0, 0, 0)
                    // the bar under the banner: what is left today, or why the last try did not go
                    FitText(
                        text = state.error?.let { stringResource(it.messageRes()) }
                            ?: stringResource(R.string.gift_today_pill, usage.countLeft, usage.goldLeft.coerceAtMost(usage.friendLeft)),
                        style = PaintedStyle(
                            color = if (state.error != null) Color(0xFF9A2F12) else Ink, fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Start, lineHeight = 17.sp
                        ),
                        maxLines = 2, minScale = 0.7f, contentAlignment = Alignment.CenterStart,
                        modifier = Modifier.offset(w * 0.27f, h * 0.2475f).size(w * 0.55f, h * 0.046f)
                    )
                    // the gold plank
                    FitText(
                        text = stringResource(R.string.gift_section_gold),
                        style = PaintedStyle(color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Start),
                        minScale = 0.6f, contentAlignment = Alignment.CenterStart,
                        modifier = Modifier.offset(w * 0.505f, h * 0.317f).size(w * 0.2f, h * 0.041f)
                    )
                    GiftRules.goldOffers().forEachIndexed { i, o ->
                        SendRow(o, gold, usage, state.sending, onSend, w, h, SendRowTop[i], feeTag = i == 0)
                    }
                    FitText(
                        text = stringResource(R.string.gift_section_items),
                        style = PaintedStyle(color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center,
                            shadow = androidx.compose.ui.graphics.Shadow(Color(0xFF3E0B66), androidx.compose.ui.geometry.Offset(0f, 2f), 3f)),
                        minScale = 0.6f,
                        modifier = Modifier.offset(w * 0.34f, h * 0.658f).size(w * 0.33f, h * 0.034f)
                    )
                    // the three rows have their joker's picture painted in, in this order
                    val items = GiftRules.itemOffers()
                    listOf(JokerType.FIRST_LETTER, JokerType.LETTER_COUNT, JokerType.EXTRA_TIME).forEachIndexed { i, type ->
                        items.firstOrNull { it.kind == GiftKind.JOKER && it.itemId == type.name }?.let {
                            SendRow(it, gold, usage, state.sending, onSend, w, h, SendRowTop[4 + i], feeTag = false)
                        }
                    }
                }
            }
        }
    }
}

private const val SendW = 924f
private const val SendH = 1467f

/** Top edge of the seven rows in the picture, as fractions of its height (four gold, then three jokers). */
private val SendRowTop = floatArrayOf(0.375f, 0.440f, 0.504f, 0.571f, 0.698f, 0.763f, 0.829f)

/** The cream panel that takes the rows' place when there is something to say instead. */
@Composable
private fun MessagePanel(w: androidx.compose.ui.unit.Dp, h: androidx.compose.ui.unit.Dp, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.offset(w * 0.078f, h * 0.243f).size(w * 0.844f, h * 0.655f)
            .background(Brush.verticalGradient(listOf(Color(0xFFFFFAEC), Color(0xFFFBE9C4))), shape)
            .border(2.dp, Color(0xFFE2C28A), shape)
            .padding(12.dp),
        content = content
    )
}

/** One of the picture's rows: its words on the left, its price in the yellow pill, the whole row one button. */
@Composable
private fun SendRow(
    offer: GiftOffer, gold: Int, usage: GiftUsage, sending: Boolean, onSend: (GiftOffer) -> Unit,
    w: androidx.compose.ui.unit.Dp, h: androidx.compose.ui.unit.Dp, top: Float, feeTag: Boolean
) {
    val ok = gold >= offer.cost && usage.canAfford(offer) && !sending
    val ink = if (ok) Ink else Color(0xFFA8957F)
    val rowShape = RoundedCornerShape(percent = 50)
    Box(
        Modifier.offset(w * 0.115f, h * top).size(w * 0.785f, h * 0.058f)
            .clip(rowShape)
            .then(if (ok) Modifier.pressable(pressedScale = 0.97f, onClick = { onSend(offer) }) else Modifier.background(Color(0x66F2E3C8)))
    )
    val joker = offer.kind == GiftKind.JOKER
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.offset(w * 0.265f, h * top).size(w * 0.41f, h * 0.058f)
    ) {
        FitText(
            text = if (offer.kind == GiftKind.GOLD) stringResource(R.string.gift_gold_amount, offer.receives) else giftName(offer.kind, offer.itemId, offer.qty),
            style = PaintedStyle(
                color = ink, fontSize = if (joker) 16.sp else 20.sp, lineHeight = if (joker) 17.sp else 22.sp,
                fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Start
            ),
            maxLines = if (joker) 2 else 1, minScale = 0.75f, contentAlignment = Alignment.CenterStart,
            modifier = Modifier.weight(1f)
        )
        if (feeTag) {
            // the fee, once, beside the first gold amount
            Box(
                Modifier.padding(start = 4.dp)
                    .background(Brush.horizontalGradient(listOf(Color(0xFFE8329C), Color(0xFFB81F86))), RoundedCornerShape(50))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                androidx.compose.material3.Text(
                    stringResource(R.string.gift_fee_inline), color = Color.White, fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold, maxLines = 1
                )
            }
        }
    }
    FitText(
        text = "${offer.cost}",
        style = PaintedStyle(color = if (ok) Color(0xFF3B2314) else Color(0xFF8E7B66), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center),
        minScale = 0.6f,
        modifier = Modifier.offset(w * 0.765f, h * top).size(w * 0.125f, h * 0.058f)
    )
}

@Composable
private fun Note(text: String) {
    androidx.compose.material3.Text(
        text, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp)
    )
}

@Composable
private fun ErrorCard(text: String) {
    val shape = RoundedCornerShape(14.dp)
    androidx.compose.material3.Text(
        text, color = Color(0xFF9A2F12), fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)
            .background(Color(0x26D9483B), shape).border(1.5.dp, Color(0x66D9483B), shape).padding(horizontal = 12.dp, vertical = 9.dp)
    )
}

/** After a gift has gone: a gift badge, one line that says so, what was sent and what it cost, and a quiet note on how it arrives. */
@Composable
private fun SentCard(offer: GiftOffer) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
        androidx.compose.foundation.layout.Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(72.dp)
                .softShadow()
                .background(Brush.verticalGradient(listOf(Color(0xFFFFB14E), Color(0xFFF47A16))), androidx.compose.foundation.shape.CircleShape)
                .border(3.dp, Color.White, androidx.compose.foundation.shape.CircleShape)
        ) {
            androidx.compose.material3.Icon(
                Icons.Filled.CardGiftcard, contentDescription = null,
                tint = Color.White, modifier = Modifier.size(38.dp)
            )
        }
        androidx.compose.material3.Text(
            stringResource(R.string.gift_sent_headline), color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
        )
        val shape = RoundedCornerShape(50)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.softShadow()
                .background(Brush.verticalGradient(listOf(Color(0xFFFFFAEC), Color(0xFFFBE9C4))), shape)
                .border(1.5.dp, Color(0xFFE2C28A), shape)
                .padding(start = 14.dp, end = 12.dp, top = 6.dp, bottom = 6.dp)
        ) {
            androidx.compose.material3.Text(
                if (offer.kind == GiftKind.GOLD) stringResource(R.string.gift_gold_amount, offer.receives) else giftName(offer.kind, offer.itemId, offer.qty),
                color = Ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1
            )
            Spacer(Modifier.width(10.dp))
            Image(painterResource(R.drawable.icon_gold_coin), null, Modifier.size(18.dp))
            Spacer(Modifier.width(3.dp))
            androidx.compose.material3.Text("−${offer.cost}", color = Color(0xFFB3261E), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        }
        androidx.compose.material3.Text(
            stringResource(R.string.gift_sent_note), color = InkSoft, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center, lineHeight = 19.sp, modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp)
        )
    }
}

/**
 * The gift window: one painted picture (gift_window, 900 x 943 px: the dog and the cat handing over a present, an empty
 * ribbon, an empty cream bar and an empty orange button). The title, the words and the button's label are lettered
 * live on it in the picture's own proportions, and the orange button is the button.
 */
@Composable
private fun GiftArtWindow(title: String, lines: List<String>, button: String, onClick: () -> Unit, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        androidx.compose.foundation.layout.BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .widthIn(max = 380.dp)
                .aspectRatio(GiftW / GiftH)
        ) {
            val w = maxWidth
            val h = maxHeight
            Image(
                painter = painterResource(R.drawable.gift_window),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )
            // the pink ribbon
            LetteredText(
                text = title, size = 26.sp, fill = Color.White, outline = Color(0xFF8A0F4B), title = true, minScale = 0.5f,
                modifier = Modifier.offset(w * 0.26f, h * 0.12f).size(w * 0.48f, h * 0.09f)
            )
            // the cream bar
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.offset(w * 0.12f, h * 0.64f).size(w * 0.76f, h * 0.105f)
            ) {
                lines.take(3).forEachIndexed { i, line ->
                    FitText(
                        text = line,
                        style = PaintedStyle(
                            color = if (i == 0) Ink else InkSoft, fontSize = if (i == 0) 18.sp else 16.sp,
                            fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, lineHeight = 21.sp
                        ),
                        minScale = 0.6f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            // the orange button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.offset(w * 0.25f, h * 0.795f).size(w * 0.50f, h * 0.11f)
                    .pressable(pressedScale = 0.94f, onClick = onClick)
            ) {
                LetteredText(
                    text = button, size = 24.sp, fill = Color.White, outline = Color(0xFF9A3B00), minScale = 0.55f,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp)
                )
            }
        }
    }
}

private const val GiftW = 900f
private const val GiftH = 943f

/** What arrived: the sender (or the count) and one button to take everything. */
@Composable
fun GiftInboxDialog(gifts: List<IncomingGift>, onClaim: () -> Unit) {
    val first = gifts.firstOrNull()
    val lines = if (gifts.size <= 1 && first != null) listOf(
        stringResource(R.string.gift_from, first.fromName),
        if (first.kind == GiftKind.GOLD) stringResource(R.string.gift_gold_amount, first.gold) else giftName(first.kind, first.itemId, first.qty)
    ) else listOf(
        stringResource(R.string.gift_inbox_several, gifts.size),
        stringResource(R.string.gift_from, gifts.map { it.fromName }.distinct().take(2).joinToString(", "))
    )
    GiftArtWindow(
        title = stringResource(R.string.gift_inbox_title), lines = lines,
        button = stringResource(if (gifts.size > 1) R.string.gift_take_all else R.string.gift_take_one),
        onClick = onClaim, onDismiss = onClaim
    )
}

/** What each gift turned into, shown after taking them (an owned item pays out gold instead). */
@Composable
fun GiftResultDialog(results: List<Pair<IncomingGift, GiftRepository.ClaimResult>>, onClose: () -> Unit) {
    val lines = results.map { (g, r) ->
        when (r) {
            is GiftRepository.ClaimResult.Gold -> stringResource(R.string.gift_got_gold, r.amount)
            is GiftRepository.ClaimResult.Converted -> stringResource(R.string.gift_got_converted, r.amount)
            is GiftRepository.ClaimResult.Item -> giftName(g.kind, g.itemId, g.qty)
        }
    }
    GiftArtWindow(
        title = stringResource(R.string.gift_result_title), lines = lines,
        button = stringResource(R.string.gift_great), onClick = onClose, onDismiss = onClose
    )
}
