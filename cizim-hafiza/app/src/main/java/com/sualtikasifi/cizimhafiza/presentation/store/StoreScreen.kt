package com.sualtikasifi.cizimhafiza.presentation.store

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.AvatarFrame
import com.sualtikasifi.cizimhafiza.domain.model.JokerType
import com.sualtikasifi.cizimhafiza.presentation.common.descRes
import com.sualtikasifi.cizimhafiza.presentation.common.icon
import com.sualtikasifi.cizimhafiza.presentation.common.labelRes
import com.sualtikasifi.cizimhafiza.presentation.common.tint
import com.sualtikasifi.cizimhafiza.domain.model.PenSkin
import com.sualtikasifi.cizimhafiza.presentation.common.ScreenTopActions
import com.sualtikasifi.cizimhafiza.presentation.common.TopActionsClearance
import com.sualtikasifi.cizimhafiza.presentation.common.nameRes
import com.sualtikasifi.cizimhafiza.presentation.common.screenBackground
import com.sualtikasifi.cizimhafiza.presentation.theme.AppTheme
import com.sualtikasifi.cizimhafiza.presentation.theme.DisplayFont
import kotlinx.coroutines.delay
import java.text.NumberFormat

private val Ink = Color(0xFF3A2416)

private sealed interface Pending {
    data class PenItem(val skin: PenSkin) : Pending
    data class FrameItem(val frame: AvatarFrame) : Pending
    data class JokerItem(val type: JokerType, val quantity: Int) : Pending
}

/** The gold store. Two tabs — pens and frames — of things that are bought, not levelled into. */
@Composable
fun StoreScreen(onBack: () -> Unit, onAccount: () -> Unit = {}, viewModel: StoreViewModel = hiltViewModel()) {
    val gold by viewModel.gold.collectAsState()
    val isGuest by viewModel.isGuest.collectAsState()
    val owned by viewModel.owned.collectAsState()
    val selectedPen by viewModel.selectedPenId.collectAsState()
    val selectedFrame by viewModel.selectedFrameId.collectAsState()
    val jokerCounts by viewModel.jokerCounts.collectAsState()

    var tab by remember { mutableStateOf(0) }
    var pending by remember { mutableStateOf<Pending?>(null) }
    var tryingPen by remember { mutableStateOf<PenSkin?>(null) }
    var noticeRes by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(noticeRes) {
        if (noticeRes != null) {
            delay(2_200)
            noticeRes = null
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .screenBackground()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = TopActionsClearance, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        GoldPill(gold = gold)
                        // A guest's purchases live on this phone only: say so once they own something.
                        if (isGuest && (owned.isNotEmpty() || jokerCounts.values.any { it > 0 })) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color(0xFFFFE9C7))
                                    .border(2.dp, Color(0xFFF0B24E), RoundedCornerShape(18.dp))
                                    .clickable(onClick = onAccount)
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(text = "☁", style = MaterialTheme.typography.titleLarge)
                                Text(
                                    text = stringResource(R.string.store_guest_warning),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TabChip(stringResource(R.string.store_tab_pens), selected = tab == 0) { tab = 0 }
                            TabChip(stringResource(R.string.store_tab_frames), selected = tab == 1) { tab = 1 }
                            TabChip(stringResource(R.string.store_tab_jokers), selected = tab == 2) { tab = 2 }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                if (tab == 0) {
                    items(viewModel.pens) { skin ->
                        val id = StoreViewModel.penId(skin)
                        StoreCard(
                            preview = { PenPreview(skin) },
                            onTry = { tryingPen = skin },
                            name = stringResource(skin.labelRes),
                            price = skin.storePrice,
                            owned = id in owned,
                            equipped = id in owned && selectedPen == skin.name,
                            canAfford = gold >= skin.storePrice,
                            onBuy = { pending = Pending.PenItem(skin) },
                            onEquip = { viewModel.equipPen(skin) },
                            onCannotAfford = { noticeRes = R.string.store_not_enough }
                        )
                    }
                } else if (tab == 2) {
                    items(JokerType.entries, span = { GridItemSpan(maxLineSpan) }) { type ->
                        JokerCard(
                            type = type,
                            owned = jokerCounts[type] ?: 0,
                            gold = gold,
                            onBuy = { qty -> pending = Pending.JokerItem(type, qty) },
                            onCannotAfford = { noticeRes = R.string.store_not_enough }
                        )
                    }
                } else if (viewModel.frames.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                                .padding(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.store_frames_soon),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(viewModel.frames) { frame ->
                        val id = StoreViewModel.frameId(frame)
                        StoreCard(
                            preview = { Image(painterResource(frame.drawableRes), contentDescription = null, modifier = Modifier.size(84.dp)) },
                            name = stringResource(frame.nameRes()),
                            price = frame.storePrice,
                            owned = id in owned,
                            equipped = id in owned && selectedFrame == frame.name,
                            canAfford = gold >= frame.storePrice,
                            onBuy = { pending = Pending.FrameItem(frame) },
                            onEquip = { viewModel.equipFrame(frame) },
                            onCannotAfford = { noticeRes = R.string.store_not_enough }
                        )
                    }
                }
            }

            ScreenTopActions(
                onBack = onBack,
                title = stringResource(R.string.store_title),
                modifier = Modifier.align(Alignment.TopStart)
            )

            noticeRes?.let { res ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 24.dp, vertical = 32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xE62B1A12))
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    Text(text = stringResource(res), color = Color.White, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                }
            }
        }
    }

    tryingPen?.let { skin -> PenTryDialog(skin = skin, onDismiss = { tryingPen = null }) }

    pending?.let { item ->
        val (name, price) = when (item) {
            is Pending.PenItem -> stringResource(item.skin.labelRes) to item.skin.storePrice
            is Pending.FrameItem -> stringResource(item.frame.nameRes()) to item.frame.storePrice
            is Pending.JokerItem -> (stringResource(item.type.labelRes()) + " ×" + item.quantity) to item.type.priceFor(item.quantity)
        }
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(stringResource(R.string.store_buy_confirm_title)) },
            text = { Text(stringResource(R.string.store_buy_confirm_body, name, price)) },
            confirmButton = {
                TextButton(onClick = {
                    val ok = when (item) {
                        is Pending.PenItem -> viewModel.buyPen(item.skin)
                        is Pending.FrameItem -> viewModel.buyFrame(item.frame)
                        is Pending.JokerItem -> viewModel.buyJoker(item.type, item.quantity)
                    }
                    noticeRes = if (ok) R.string.store_bought else R.string.store_not_enough
                    pending = null
                }) { Text(stringResource(R.string.store_buy)) }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text(stringResource(R.string.nickname_edit_cancel)) } }
        )
    }
}


@Composable
private fun GoldPill(gold: Int) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF2B1A12))
            .padding(start = 6.dp, end = 20.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Image(painterResource(R.drawable.icon_gold_coin), contentDescription = null, modifier = Modifier.size(34.dp))
        Text(
            text = NumberFormat.getIntegerInstance().format(gold),
            fontFamily = DisplayFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 22.sp,
            color = Color.White
        )
    }
}

@Composable
private fun TabChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Color(0xFFFF7A21) else Color.White.copy(alpha = 0.85f))
            .border(2.dp, if (selected) Color(0xFFC85A0A) else Color(0xFFEBCB93), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 9.dp)
    ) {
        Text(
            text = label,
            fontFamily = DisplayFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            color = if (selected) Color.White else Ink
        )
    }
}

/** A wavy stroke in the pen's colours — what the pen actually draws. */
@Composable
private fun PenPreview(skin: PenSkin) {
    val colors = skin.colors.map { Color(it) }
    Canvas(modifier = Modifier.fillMaxWidth().height(64.dp)) {
        val path = Path().apply {
            moveTo(size.width * 0.08f, size.height * 0.62f)
            cubicTo(
                size.width * 0.28f, size.height * 0.05f,
                size.width * 0.42f, size.height * 1.05f,
                size.width * 0.58f, size.height * 0.45f
            )
            cubicTo(
                size.width * 0.70f, size.height * 0.05f,
                size.width * 0.82f, size.height * 0.55f,
                size.width * 0.92f, size.height * 0.30f
            )
        }
        val w = 13.dp.toPx()
        drawPath(path, Color.Black.copy(alpha = 0.18f), style = Stroke(width = w + 4.dp.toPx(), cap = StrokeCap.Round))
        drawPath(
            path,
            brush = if (colors.size > 1) Brush.horizontalGradient(colors) else Brush.horizontalGradient(listOf(colors.first(), colors.first())),
            style = Stroke(width = w, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun StoreCard(
    preview: @Composable () -> Unit,
    name: String,
    price: Int,
    owned: Boolean,
    equipped: Boolean,
    canAfford: Boolean,
    onBuy: () -> Unit,
    onEquip: () -> Unit,
    onCannotAfford: () -> Unit,
    onTry: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFFFFF6E3), Color(0xFFFCE6BF))))
            .border(2.dp, if (equipped) AppTheme.tokens.success else Color(0xFFEBCB93), shape)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.7f))
                .padding(vertical = 10.dp, horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) { preview() }
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = name, fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Ink, maxLines = 1)
        onTry?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "✏ " + stringResource(R.string.store_try),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE8672A),
                modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = it).padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        when {
            equipped -> ActionPill(stringResource(R.string.store_equipped), AppTheme.tokens.success, Color.White, null)
            owned -> ActionPill(stringResource(R.string.store_equip), Color(0xFFFF7A21), Color.White, onEquip)
            else -> Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (canAfford) Color(0xFF2B1A12) else Color(0xFF2B1A12).copy(alpha = 0.45f))
                    .clickable { if (canAfford) onBuy() else onCannotAfford() }
                    .padding(start = 4.dp, end = 14.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Image(painterResource(R.drawable.icon_gold_coin), contentDescription = null, modifier = Modifier.size(26.dp))
                Text(
                    text = NumberFormat.getIntegerInstance().format(price),
                    fontFamily = DisplayFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun ActionPill(text: String, container: Color, content: Color, onClick: (() -> Unit)?) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 22.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = content)
    }
}

/** One joker row: what it does, how many the player has, and two ways to buy (1, or a discounted bundle). */
@Composable
private fun JokerCard(
    type: JokerType,
    owned: Int,
    gold: Int,
    onBuy: (Int) -> Unit,
    onCannotAfford: () -> Unit,
    onTry: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFFFFF6E3), Color(0xFFFCE6BF))))
            .border(2.dp, Color(0xFFEBCB93), shape)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                com.sualtikasifi.cizimhafiza.presentation.common.JokerArt(type, 64.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(type.labelRes()), fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Ink)
                Text(text = stringResource(type.descRes()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(
                modifier = Modifier.clip(RoundedCornerShape(50)).background(type.tint().copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(text = stringResource(R.string.joker_owned, owned), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, color = type.tint())
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(1, JokerType.BULK_QUANTITY).forEach { qty ->
                val price = type.priceFor(qty)
                val canAfford = gold >= price
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(if (canAfford) Color(0xFF2B1A12) else Color(0xFF2B1A12).copy(alpha = 0.45f))
                        .clickable { if (canAfford) onBuy(qty) else onCannotAfford() }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (qty == 1) stringResource(R.string.joker_buy_one) else stringResource(R.string.joker_buy_bulk, qty, JokerType.BULK_DISCOUNT_PERCENT),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 1
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Image(painterResource(R.drawable.icon_gold_coin), contentDescription = null, modifier = Modifier.size(20.dp))
                            Text(
                                text = NumberFormat.getIntegerInstance().format(price),
                                fontFamily = DisplayFont,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Scribble with a pen before buying it: a small paper canvas that draws with the pen's real colours. */
@Composable
private fun PenTryDialog(skin: PenSkin, onDismiss: () -> Unit) {
    var strokes by remember { mutableStateOf(emptyList<com.sualtikasifi.cizimhafiza.domain.model.DrawingStroke>()) }
    com.sualtikasifi.cizimhafiza.presentation.common.AppWindowDialog(
        title = stringResource(R.string.store_try_title, stringResource(skin.labelRes)),
        onDismiss = onDismiss
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .border(2.dp, Color(0xFFEBCB93), RoundedCornerShape(20.dp))
        ) {
            com.sualtikasifi.cizimhafiza.presentation.common.DrawableCanvas(
                liveStrokes = strokes,
                onStrokeFinished = { strokes = strokes + listOf(it) },
                penSkin = skin,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            com.sualtikasifi.cizimhafiza.presentation.common.SecondaryButton(
                text = stringResource(R.string.store_try_clear),
                onClick = { strokes = emptyList() },
                modifier = Modifier.weight(1f)
            )
            com.sualtikasifi.cizimhafiza.presentation.common.PrimaryButton(
                text = stringResource(R.string.close),
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
