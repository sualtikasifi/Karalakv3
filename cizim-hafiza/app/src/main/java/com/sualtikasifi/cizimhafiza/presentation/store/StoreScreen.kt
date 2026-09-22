package com.sualtikasifi.cizimhafiza.presentation.store

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sualtikasifi.cizimhafiza.presentation.common.AppWindowDialog
import com.sualtikasifi.cizimhafiza.presentation.common.PrimaryButton
import com.sualtikasifi.cizimhafiza.presentation.common.SecondaryButton
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.AvatarFrame
import com.sualtikasifi.cizimhafiza.domain.model.JokerType
import com.sualtikasifi.cizimhafiza.presentation.common.descRes
import com.sualtikasifi.cizimhafiza.presentation.common.icon
import com.sualtikasifi.cizimhafiza.presentation.common.labelRes
import com.sualtikasifi.cizimhafiza.presentation.common.tint
import com.sualtikasifi.cizimhafiza.domain.model.PenSkin
import com.sualtikasifi.cizimhafiza.presentation.common.nameRes
import com.sualtikasifi.cizimhafiza.presentation.theme.AppTheme
import com.sualtikasifi.cizimhafiza.presentation.theme.DisplayFont
import kotlinx.coroutines.delay
import java.text.NumberFormat

private val Ink = Color(0xFF3A2416)

/**
 * How far the scrolling grid starts from the top. [R.drawable.store_bg]
 * bakes its own "Karalak Mağaza" signage into roughly the top fifth of the
 * image (measured off the source art: the "Mağaza" plaque's bottom edge
 * sits at ~22% of the image height), so the tab row starts right under that
 * signage instead of at the usual
 * [com.sualtikasifi.cizimhafiza.presentation.common.TopActionsClearance].
 */
private val StoreTopClearance = 178.dp

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
    // The toast's own id changes on every fire, even for the same message
    // twice in a row, so re-showing it (declined, tried again, still short)
    // restarts its animation and its timer instead of doing nothing.
    var toast by remember { mutableStateOf<Pair<Int, Boolean>?>(null) }
    var toastId by remember { mutableIntStateOf(0) }
    fun showToast(res: Int, isError: Boolean) {
        toastId++
        toast = res to isError
    }
    LaunchedEffect(toastId) {
        if (toast != null) {
            delay(2_400)
            toast = null
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    // The workshop-corner photo backdrop, in place of the
                    // shared doodle-paper background: it already carries the
                    // "Karalak Mağaza" signage baked in, so this screen draws
                    // no title text of its own — see StoreTopClearance below
                    // for why the scrolling content starts as low as it does.
                    .paint(painterResource(R.drawable.store_bg), contentScale = ContentScale.Crop)
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = StoreTopClearance, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        // A guest's purchases live on this phone only: say so once they own something.
                        if (isGuest && (owned.isNotEmpty() || jokerCounts.values.any { it > 0 })) {
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
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            TabChip(stringResource(R.string.store_tab_pens), selected = tab == 0, modifier = Modifier.weight(1f)) { tab = 0 }
                            TabChip(stringResource(R.string.store_tab_frames), selected = tab == 1, modifier = Modifier.weight(1f)) { tab = 1 }
                            TabChip(stringResource(R.string.store_tab_jokers), selected = tab == 2, modifier = Modifier.weight(1f)) { tab = 2 }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                if (tab == 0) {
                    items(viewModel.pens) { skin ->
                        val id = StoreViewModel.penId(skin)
                        PenCard(
                            skin = skin,
                            owned = id in owned,
                            equipped = id in owned && selectedPen == skin.name,
                            canAfford = gold >= skin.storePrice,
                            onBuy = { pending = Pending.PenItem(skin) },
                            onEquip = { viewModel.equipPen(skin) },
                            onCannotAfford = { showToast(R.string.store_not_enough, isError = true) },
                            onTry = { tryingPen = skin }
                        )
                    }
                } else if (tab == 2) {
                    items(JokerType.entries, span = { GridItemSpan(maxLineSpan) }) { type ->
                        JokerCard(
                            type = type,
                            owned = jokerCounts[type] ?: 0,
                            gold = gold,
                            onBuy = { qty -> pending = Pending.JokerItem(type, qty) },
                            onCannotAfford = { showToast(R.string.store_not_enough, isError = true) }
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
                            preview = {
                                Image(
                                    painter = painterResource(frame.drawableRes),
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            },
                            name = stringResource(frame.nameRes()),
                            price = frame.storePrice,
                            owned = id in owned,
                            equipped = id in owned && selectedFrame == frame.name,
                            canAfford = gold >= frame.storePrice,
                            onBuy = { pending = Pending.FrameItem(frame) },
                            onEquip = { viewModel.equipFrame(frame) },
                            onCannotAfford = { showToast(R.string.store_not_enough, isError = true) }
                        )
                    }
                }
            }

            // Floats over the backdrop rather than scrolling with the grid,
            // same placement contract as ScreenTopActions elsewhere — but
            // this screen's own back button and balance pill replace it
            // wholesale, since the backdrop already carries the title.
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.store_back_button),
                    contentDescription = stringResource(R.string.cd_back),
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onBack)
                )
                Spacer(modifier = Modifier.weight(1f))
                GoldPill(gold = gold)
            }

            // Anchored a third of the way up, not hugging the bottom edge —
            // the old placement sat almost off-screen under a thumb reaching
            // for the buy button, easy to miss entirely.
            AnimatedVisibility(
                visible = toast != null,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp, start = 24.dp, end = 24.dp),
                enter = fadeIn(tween(160)) + slideInVertically(tween(220)) { it / 3 },
                exit = fadeOut(tween(160)) + slideOutVertically(tween(180)) { it / 3 }
            ) {
                toast?.let { (res, isError) -> StoreToast(res = res, isError = isError) }
            }
        }
    }

    tryingPen?.let { skin -> PenTryDialog(skin = skin, onDismiss = { tryingPen = null }) }

    pending?.let { item ->
        val name = when (item) {
            is Pending.PenItem -> stringResource(item.skin.labelRes)
            is Pending.FrameItem -> stringResource(item.frame.nameRes())
            is Pending.JokerItem -> stringResource(item.type.labelRes()) + " ×" + item.quantity
        }
        val price = when (item) {
            is Pending.PenItem -> item.skin.storePrice
            is Pending.FrameItem -> item.frame.storePrice
            is Pending.JokerItem -> item.type.priceFor(item.quantity)
        }
        val preview: @Composable () -> Unit = {
            when (item) {
                is Pending.PenItem -> PenPreview(item.skin)
                is Pending.FrameItem -> Image(painterResource(item.frame.drawableRes), contentDescription = null, modifier = Modifier.size(84.dp))
                is Pending.JokerItem -> com.sualtikasifi.cizimhafiza.presentation.common.JokerArt(item.type, 72.dp)
            }
        }
        PurchaseConfirmDialog(
            preview = preview,
            name = name,
            price = price,
            gold = gold,
            onConfirm = {
                val ok = when (item) {
                    is Pending.PenItem -> viewModel.buyPen(item.skin)
                    is Pending.FrameItem -> viewModel.buyFrame(item.frame)
                    is Pending.JokerItem -> viewModel.buyJoker(item.type, item.quantity)
                }
                showToast(if (ok) R.string.store_bought else R.string.store_not_enough, isError = !ok)
                pending = null
            },
            onDismiss = { pending = null }
        )
    }
}

/** A small floating card in the game's own voice — green for success, warm red for a failure — instead of a system snackbar. */
@Composable
private fun StoreToast(res: Int, isError: Boolean) {
    val bg = if (isError) Color(0xFF3A1B14) else Color(0xFF14321F)
    val accent = if (isError) Color(0xFFFF7A59) else Color(0xFF4ADE80)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(2.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier.size(30.dp).clip(CircleShape).background(accent.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = if (isError) "✕" else "✓", color = accent, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
        Text(
            text = stringResource(res),
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/** The game's own popup for confirming a purchase, replacing the plain system AlertDialog. */
@Composable
private fun PurchaseConfirmDialog(
    preview: @Composable () -> Unit,
    name: String,
    price: Int,
    gold: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val canAfford = gold >= price
    AppWindowDialog(title = stringResource(R.string.store_buy_confirm_title), onDismiss = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.7f))
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) { preview() }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = name,
            fontFamily = DisplayFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 19.sp,
            color = Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFF2B1A12))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Image(painterResource(R.drawable.icon_gold_coin), contentDescription = null, modifier = Modifier.size(24.dp))
            Text(
                text = NumberFormat.getIntegerInstance().format(price),
                fontFamily = DisplayFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = Color.White
            )
        }
        if (!canAfford) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFFFE3D6))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "⚠", fontSize = 16.sp)
                Text(
                    text = stringResource(R.string.store_missing_gold, price - gold),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFB3401A)
                )
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            SecondaryButton(text = stringResource(R.string.nickname_edit_cancel), onClick = onDismiss, modifier = Modifier.weight(1f))
            PrimaryButton(
                text = stringResource(R.string.store_buy),
                onClick = onConfirm,
                enabled = canAfford,
                modifier = Modifier.weight(1f)
            )
        }
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

/**
 * Just the plaque art the game handed over — no extra border box or dark
 * scrim drawn around it, since a second frame stacked on top of the
 * artwork's own baked-in wood-and-neon edge read as a mistake, not a
 * selection state. The unselected look is the SAME image, only dimmed via
 * [Image]'s own alpha, plus a muted text colour; nothing new is drawn.
 *
 * Sized by this fixed height rather than by the label the way a plain pill
 * would be, so all three tabs land at one common size regardless of word
 * length ("Kalemler" vs "Jokerler") — each call site gives this
 * `Modifier.weight(1f)` in its Row, so the three share the row evenly.
 */
@Composable
private fun TabChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Image(
            painter = painterResource(R.drawable.store_tab_plaque),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            alpha = if (selected) 1f else 0.5f,
            modifier = Modifier.matchParentSize()
        )
        Text(
            text = label,
            fontFamily = DisplayFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            color = if (selected) TabLabelSelected else TabLabelUnselected,
            style = TextStyle(shadow = Shadow(color = Color(0xFF1E0F04), offset = Offset(0f, 2f), blurRadius = 3f)),
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

private val TabLabelSelected = Color(0xFFFFE9C2)
private val TabLabelUnselected = Color(0xFFD8C3A6)

/** A wavy stroke in the pen's colours — what the pen actually draws. */
@Composable
private fun PenPreview(skin: PenSkin, modifier: Modifier = Modifier.fillMaxWidth().height(64.dp)) {
    val colors = skin.colors.map { Color(it) }
    Canvas(modifier = modifier) {
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

/** Each store pen's one-line flavour text under its name — see values(-tr)/strings.xml's pen_*_tagline entries. Null for every non-store pen, which this screen never shows. */
private fun PenSkin.taglineRes(): Int? = when (this) {
    PenSkin.COPPER -> R.string.pen_copper_tagline
    PenSkin.EMERALD -> R.string.pen_emerald_tagline
    PenSkin.RUBY -> R.string.pen_ruby_tagline
    PenSkin.SAPPHIRE -> R.string.pen_sapphire_tagline
    PenSkin.CANDY -> R.string.pen_candy_tagline
    PenSkin.ICE -> R.string.pen_ice_tagline
    PenSkin.SAKURA -> R.string.pen_sakura_tagline
    PenSkin.SUNRISE -> R.string.pen_sunrise_tagline
    PenSkin.NIGHT -> R.string.pen_night_tagline
    PenSkin.DIAMOND -> R.string.pen_diamond_tagline
    else -> null
}

/** The most saturated of a pen's colours, for a card border that reads as "this pen" at a glance — picking the first colour outright washed out badly on Ice (pale CFF3FF first, vivid 6FD3FF second) and Candy (vivid FF5FA2 first, but plain white second would've won on a "last colour" rule instead). */
private fun PenSkin.accentColor(): Color =
    colors.map { Color(it) }.maxByOrNull { c -> (maxOf(c.red, c.green, c.blue) - minOf(c.red, c.green, c.blue)) } ?: Color(0xFFEBCB93)

/**
 * The pens grid's own card shape — a 2-column vertical layout (preview on
 * top, name, a one-line tagline, then price/Dene at the bottom), bordered in
 * the pen's own colour rather than the neutral tan every other card uses, so
 * the grid reads as a rack of distinct pens rather than one repeated
 * template. Frames keep the plainer horizontal [StoreCard] — a ring's own
 * artwork already carries enough colour that it doesn't need this treatment,
 * and it has no tagline to make room for.
 */
@Composable
private fun PenCard(
    skin: PenSkin,
    owned: Boolean,
    equipped: Boolean,
    canAfford: Boolean,
    onBuy: () -> Unit,
    onEquip: () -> Unit,
    onCannotAfford: () -> Unit,
    onTry: () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    val accent = skin.accentColor()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFFFFF6E3).copy(alpha = 0.85f), Color(0xFFFCE6BF).copy(alpha = 0.85f))))
            .border(2.5.dp, if (equipped) AppTheme.tokens.success else accent.copy(alpha = 0.8f), shape)
            .padding(12.dp)
    ) {
        PenPreview(skin, modifier = Modifier.fillMaxWidth().height(56.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = stringResource(skin.labelRes), fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Ink, maxLines = 1)
        skin.taglineRes()?.let { tagline ->
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = stringResource(tagline), style = MaterialTheme.typography.labelSmall, color = Ink.copy(alpha = 0.6f), maxLines = 1)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            when {
                equipped -> ActionPill(stringResource(R.string.store_equipped), AppTheme.tokens.success, Color.White, null)
                owned -> ActionPill(stringResource(R.string.store_equip), Color(0xFFFF7A21), Color.White, onEquip)
                else -> PricePill(price = skin.storePrice, canAfford = canAfford, onClick = { if (canAfford) onBuy() else onCannotAfford() })
            }
            TryButton(onClick = onTry)
        }
    }
}

/**
 * A short, horizontal row instead of the old tall column: a bare preview
 * (no white backing panel — pens draw straight onto the card's own warm
 * translucent face, frames are already-transparent PNGs) beside the name
 * and, in the same line, whatever action this item currently offers — buy,
 * equip, equipped, or (pens only) a Try button next to the price.
 */
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
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            // Slightly transparent on purpose — the workshop backdrop should
            // still read through the card, not just sit behind opaque tiles.
            .background(Brush.verticalGradient(listOf(Color(0xFFFFF6E3).copy(alpha = 0.75f), Color(0xFFFCE6BF).copy(alpha = 0.75f))))
            .border(2.dp, if (equipped) AppTheme.tokens.success else Color(0xFFEBCB93).copy(alpha = 0.85f), shape)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(width = 60.dp, height = 52.dp), contentAlignment = Alignment.Center) { preview() }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Ink, maxLines = 1)
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                when {
                    equipped -> ActionPill(stringResource(R.string.store_equipped), AppTheme.tokens.success, Color.White, null)
                    owned -> ActionPill(stringResource(R.string.store_equip), Color(0xFFFF7A21), Color.White, onEquip)
                    else -> PricePill(price = price, canAfford = canAfford, onClick = { if (canAfford) onBuy() else onCannotAfford() })
                }
                onTry?.let { TryButton(onClick = it) }
            }
        }
    }
}

/** The compact buy price used inline in [StoreCard] — a bigger, standalone version lives in [PurchaseConfirmDialog]. */
@Composable
private fun PricePill(price: Int, canAfford: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (canAfford) Color(0xFF2B1A12) else Color(0xFF2B1A12).copy(alpha = 0.45f))
            .clickable(onClick = onClick)
            .padding(start = 4.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Image(painterResource(R.drawable.icon_gold_coin), contentDescription = null, modifier = Modifier.size(22.dp))
        Text(
            text = NumberFormat.getIntegerInstance().format(price),
            fontFamily = DisplayFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            color = Color.White
        )
    }
}

/**
 * A proper little button now — cream face, orange ink border, pencil glyph —
 * in place of the bare orange text link this used to be, which read as
 * unfinished next to the framed price pill it sits beside.
 */
@Composable
private fun TryButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xFFFFF1D6))
            .border(1.5.dp, Color(0xFFE8672A).copy(alpha = 0.8f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = "✏", fontSize = 12.sp)
        Text(
            text = stringResource(R.string.store_try),
            fontFamily = DisplayFont,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = Color(0xFFB3401A)
        )
    }
}

@Composable
private fun ActionPill(text: String, container: Color, content: Color, onClick: (() -> Unit)?) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = content)
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
            // Same slightly-transparent treatment as StoreCard, but a touch
            // less see-through — this card carries two lines of description
            // text, which needs a steadier, less busy surface behind it than
            // a short name/price row does to stay easily readable over the
            // photo backdrop.
            .background(Brush.verticalGradient(listOf(Color(0xFFFFF6E3).copy(alpha = 0.85f), Color(0xFFFCE6BF).copy(alpha = 0.85f))))
            .border(2.dp, Color(0xFFEBCB93).copy(alpha = 0.85f), shape)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                com.sualtikasifi.cizimhafiza.presentation.common.JokerArt(type, 64.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(type.labelRes()), fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Ink)
                Text(text = stringResource(type.descRes()), style = MaterialTheme.typography.bodySmall, color = Ink.copy(alpha = 0.75f))
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
