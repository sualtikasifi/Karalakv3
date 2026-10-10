package com.sualtikasifi.cizimhafiza.presentation.store

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.draw.drawBehind
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import com.sualtikasifi.cizimhafiza.presentation.mainmenu.sunkenArt
import com.sualtikasifi.cizimhafiza.presentation.mainmenu.sinkWith
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import com.sualtikasifi.cizimhafiza.presentation.common.NinePatch
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.sualtikasifi.cizimhafiza.presentation.common.pressable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.graphics.graphicsLayer
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

// st_bg is this size: the workshop with an empty wooden sign; the title is lettered live on the sign.
private const val BgW = 841f
private const val BgH = 1870f

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

    // 0 = jokers, 1 = pens, 2 = frames. Jokers first so the free daily one is
    // the first thing the store shows.
    var tab by remember { mutableStateOf(0) }
    // Which way the last tab change went (+1 towards the right, -1 towards the left): the new cards slide in from there.
    var tabDirection by remember { mutableIntStateOf(1) }
    var tabOpenedAt by remember { androidx.compose.runtime.mutableLongStateOf(System.currentTimeMillis()) }
    val selectTab = { next: Int -> if (next != tab) { tabDirection = if (next > tab) 1 else -1; tab = next; tabOpenedAt = System.currentTimeMillis() } }
    val dailyJoker by viewModel.dailyJoker.collectAsState()
    val activity = androidx.compose.ui.platform.LocalContext.current as? android.app.Activity
    var pending by remember { mutableStateOf<Pending?>(null) }
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
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            // The picture fills the screen from the top, cropped at the sides or foot when the phone is not quite its shape.
            val artScale = maxOf(maxWidth.value / BgW, maxHeight.value / BgH)
            val fontScale0 = androidx.compose.ui.platform.LocalDensity.current.fontScale
            val artOffX = (maxWidth.value - BgW * artScale) / 2f
            Image(painterResource(R.drawable.st_bg), contentDescription = null, contentScale = ContentScale.Crop, alignment = Alignment.TopCenter, modifier = Modifier.fillMaxSize())
            val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
            LaunchedEffect(tab) { gridState.scrollToItem(0) }
            // The sign and the tabs stay where the picture puts them; only the shelf of cards below the tabs scrolls
            // (and is cut off at the tabs' foot), so nothing drawn live can ever sit in front of the cards.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = padding.calculateBottomPadding())
                    .padding(top = (540f * artScale).dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    TabChip(stringResource(R.string.store_tab_jokers), R.drawable.st_ic_crown, selected = tab == 0, modifier = Modifier.weight(1f)) { selectTab(0) }
                    TabChip(stringResource(R.string.store_tab_pens), R.drawable.st_ic_pencil, selected = tab == 1, modifier = Modifier.weight(1f)) { selectTab(1) }
                    TabChip(stringResource(R.string.store_tab_frames), R.drawable.st_ic_image, selected = tab == 2, modifier = Modifier.weight(1f)) { selectTab(2) }
                }
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // A guest's purchases live on this phone only: say so once they own something.
                if (isGuest && (owned.isNotEmpty() || jokerCounts.values.any { it > 0 })) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFFFFE9C7))
                                .border(2.dp, Color(0xFFF0B24E), RoundedCornerShape(18.dp))
                                .pressable(pressedScale = 0.92f, onClick = onAccount)
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
                }

                if (tab == 1) {
                    itemsIndexed(viewModel.pens) { index, skin ->
                        val id = StoreViewModel.penId(skin)
                        TabEntrance(tab, tabDirection, tabOpenedAt, index) { PenCard(
                            skin = skin,
                            owned = id in owned,
                            equipped = id in owned && selectedPen == skin.name,
                            canAfford = gold >= skin.storePrice,
                            onBuy = { pending = Pending.PenItem(skin) },
                            onEquip = { viewModel.equipPen(skin) },
                            onCannotAfford = { showToast(R.string.store_not_enough, isError = true) }
                        ) }
                    }
                } else if (tab == 0) {
                    dailyJoker?.let { free ->
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            TabEntrance(tab, tabDirection, tabOpenedAt, 0) {
                                DailyJokerCard(
                                    type = free,
                                    onClaim = { activity?.let { viewModel.claimDailyJoker(it) { res, isError -> showToast(res, isError) } } }
                                )
                            }
                        }
                    }
                    itemsIndexed(JokerType.entries, span = { _, _ -> GridItemSpan(maxLineSpan) }) { index, type ->
                        TabEntrance(tab, tabDirection, tabOpenedAt, index + 1) {
                            JokerCard(
                                type = type,
                                owned = jokerCounts[type] ?: 0,
                                gold = gold,
                                onBuy = { qty -> pending = Pending.JokerItem(type, qty) },
                                onCannotAfford = { showToast(R.string.store_not_enough, isError = true) }
                            )
                        }
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
                    itemsIndexed(viewModel.frames) { index, frame ->
                        val id = StoreViewModel.frameId(frame)
                        TabEntrance(tab, tabDirection, tabOpenedAt, index) { FrameCard(
                            frame = frame,
                            name = stringResource(frame.nameRes()),
                            owned = id in owned,
                            equipped = id in owned && selectedFrame == frame.name,
                            canAfford = gold >= frame.storePrice,
                            onBuy = { pending = Pending.FrameItem(frame) },
                            onEquip = { viewModel.equipFrame(frame) },
                            onCannotAfford = { showToast(R.string.store_not_enough, isError = true) }
                        ) }
                    }
                }
            }
            }

            // The title, lettered on the wooden plank: both words in ONE size, one under the other 48 picture px apart,
            // the pair centred on the plank's face (x 425, y 349 of the 841 x 1870 picture; measured on a screenshot).
            Box(
                modifier = Modifier
                    .offset((artOffX + (425f - 190f) * artScale).dp, ((349f - 70f) * artScale).dp)
                    .size((380f * artScale).dp, (140f * artScale).dp)
            ) {
                com.sualtikasifi.cizimhafiza.presentation.common.LetteredText(
                    text = stringResource(R.string.app_name),
                    size = (66f * artScale / fontScale0).sp,
                    fill = Color.White,
                    outline = Color(0xFF4E2406),
                    modifier = Modifier.align(Alignment.Center).offset(y = (-24f * artScale).dp).fillMaxWidth().height((68f * artScale).dp),
                    title = true
                )
                com.sualtikasifi.cizimhafiza.presentation.common.LetteredText(
                    text = stringResource(R.string.store_title),
                    size = (66f * artScale / fontScale0).sp,
                    fill = Color.White,
                    outline = Color(0xFF4E2406),
                    modifier = Modifier.align(Alignment.Center).offset(y = (24f * artScale).dp).fillMaxWidth().height((68f * artScale).dp),
                    title = true
                )
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
                    .padding(start = 14.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                com.sualtikasifi.cizimhafiza.presentation.common.WoodBackArrow(onClick = onBack)
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

/** One tab of the store: the painted wooden plaque (orange when chosen, brown otherwise) with its little picture and word. */
@Composable
private fun TabChip(label: String, icon: Int, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    // The plaque lifts a touch when chosen, instead of snapping.
    val glow by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.7f, stiffness = 380f),
        label = "tabGlow"
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(52.dp)
            .graphicsLayer {
                val lift = 1f + 0.05f * glow
                scaleX = lift
                scaleY = lift
            }
            .pressable(pressedScale = 0.92f, onClick = onClick)
    ) {
        NinePatch(
            res = if (selected) R.drawable.st_tab_on else R.drawable.st_tab_off,
            slicePx = 70,
            edge = 16.dp,
            modifier = Modifier.matchParentSize()
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Image(painterResource(icon), contentDescription = null, modifier = Modifier.height(21.dp).width(23.dp), contentScale = ContentScale.Fit)
            Text(
                text = label,
                fontFamily = DisplayFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                color = if (selected) Color.White else Color(0xFFFFE9C2),
                style = TextStyle(shadow = Shadow(color = Color(0xFF3A1A04), offset = Offset(0f, 2f), blurRadius = 3f)),
                maxLines = 1
            )
        }
    }
}

/** A painted glossy button (orange / blue / green) that stretches to any size without squashing its round ends. */
@Composable
private fun PaintedPill(
    res: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer { alpha = if (enabled) 1f else 0.55f }
            .then(if (onClick != null) Modifier.pressable(pressedScale = 0.92f, onClick = onClick) else Modifier)
    ) {
        NinePatch(res = res, slicePx = 72, edge = 18.dp, modifier = Modifier.matchParentSize())
        content()
    }
}

private val PillTextShadow = TextStyle(shadow = Shadow(color = Color(0xFF4A2200), offset = Offset(0f, 2f), blurRadius = 2f))

@Composable
private fun CoinPrice(price: Int, size: androidx.compose.ui.unit.TextUnit = 14.sp, coin: androidx.compose.ui.unit.Dp = 18.dp) {
    val long = price >= 10_000
    val size = if (long) size * 0.82f else size
    val coin = if (long) coin * 0.85f else coin
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Image(painterResource(R.drawable.icon_gold_coin), contentDescription = null, modifier = Modifier.size(coin))
        Text(
            text = NumberFormat.getIntegerInstance().format(price),
            fontFamily = DisplayFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = size,
            color = Color.White,
            style = PillTextShadow,
            maxLines = 1
        )
    }
}

/** What a card's main button says once the item is bought: green "Kullanımda" while worn, orange "Kuşan" otherwise. */
@Composable
private fun OwnedPill(equipped: Boolean, onEquip: () -> Unit, modifier: Modifier = Modifier) {
    PaintedPill(
        res = if (equipped) R.drawable.st_pill_green else R.drawable.st_pill_orange,
        modifier = modifier,
        onClick = if (equipped) null else onEquip
    ) {
        Text(
            text = stringResource(if (equipped) R.string.store_equipped else R.string.store_equip),
            fontFamily = DisplayFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 12.sp,
            color = Color.White,
            style = PillTextShadow,
            maxLines = 1
        )
    }
}

private fun PenSkin.cardRes(): Int = when (this) {
    PenSkin.COPPER -> R.drawable.st_pen_copper
    PenSkin.EMERALD -> R.drawable.st_pen_emerald
    PenSkin.RUBY -> R.drawable.st_pen_ruby
    PenSkin.SAPPHIRE -> R.drawable.st_pen_sapphire
    PenSkin.CANDY -> R.drawable.st_pen_candy
    PenSkin.ICE -> R.drawable.st_pen_ice
    PenSkin.SAKURA -> R.drawable.st_pen_sakura
    PenSkin.SUNRISE -> R.drawable.st_pen_sunrise
    PenSkin.NIGHT -> R.drawable.st_pen_night
    PenSkin.DIAMOND -> R.drawable.st_pen_diamond
    else -> R.drawable.st_pen_copper
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

/**
 * One pen on the rack: the painted card already holds the pen and its stroke; the name, the one-line
 * flavour text and the two buttons (price or "Kuşan", and "Dene") are drawn live on top of it.
 */
@Composable
private fun PenCard(
    skin: PenSkin,
    owned: Boolean,
    equipped: Boolean,
    canAfford: Boolean,
    onBuy: () -> Unit,
    onEquip: () -> Unit,
    onCannotAfford: () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(1.3f)) {
        val w = maxWidth
        val h = maxHeight
        Image(painterResource(skin.cardRes()), contentDescription = null, contentScale = ContentScale.FillBounds, modifier = Modifier.matchParentSize())
        // Name and tagline hug each other; the button sits clear of the card's lower frame.
        // Name and tagline are right-aligned, ending where the button ends: the pen (and its charm, like Buz's snowflake)
        // fills the card's left side, so text growing from the left used to run into it. The text clips at its own
        // line box, so each line has room for its descenders and the tagline is pulled up to the name by an offset.
        Column(Modifier.offset(w * 0.42f, h * 0.415f).size(w * 0.48f, h * 0.27f), horizontalAlignment = Alignment.End) {
            com.sualtikasifi.cizimhafiza.presentation.common.FitText(
                text = stringResource(skin.labelRes),
                style = com.sualtikasifi.cizimhafiza.presentation.common.PaintedStyle(color = Ink, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 18.sp, textAlign = TextAlign.End),
                maxLines = 1, minScale = 0.65f, contentAlignment = Alignment.CenterEnd,
                modifier = Modifier.fillMaxWidth().height(20.dp)
            )
            skin.taglineRes()?.let {
                com.sualtikasifi.cizimhafiza.presentation.common.FitText(
                    text = stringResource(it),
                    style = com.sualtikasifi.cizimhafiza.presentation.common.PaintedStyle(color = Ink.copy(alpha = 0.7f), fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, lineHeight = 14.sp, textAlign = TextAlign.End),
                    maxLines = 1, minScale = 0.7f, contentAlignment = Alignment.CenterEnd,
                    modifier = Modifier.fillMaxWidth().height(15.dp).offset(y = (-3).dp)
                )
            }
        }
        // One wide button: buy (price) or wear. There is no trying a pen before buying it.
        val m = Modifier.offset(w * 0.34f, h * 0.7f).size(w * 0.56f, h * 0.18f)
        when {
            owned -> OwnedPill(equipped, onEquip, m)
            else -> PaintedPill(R.drawable.st_pill_orange, m, enabled = canAfford, onClick = { if (canAfford) onBuy() else onCannotAfford() }) {
                CoinPrice(skin.storePrice, size = 14.sp, coin = 18.dp)
            }
        }
    }
}

/** One avatar ring on the shelf: the live ring picture on the painted card, its name, and the buy / wear button. */
@Composable
private fun FrameCard(
    frame: AvatarFrame,
    name: String,
    owned: Boolean,
    equipped: Boolean,
    canAfford: Boolean,
    onBuy: () -> Unit,
    onEquip: () -> Unit,
    onCannotAfford: () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(1.18f)) {
        val w = maxWidth
        val h = maxHeight
        Image(painterResource(R.drawable.st_card_frame), contentDescription = null, contentScale = ContentScale.FillBounds, modifier = Modifier.matchParentSize())
        Image(
            painter = painterResource(frame.drawableRes),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.offset(w * 0.2f, h * 0.09f).size(w * 0.6f, h * 0.5f)
        )
        Text(
            text = name,
            fontFamily = DisplayFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            color = Ink,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.offset(w * 0.08f, h * 0.60f).size(w * 0.84f, h * 0.14f)
        )
        val m = Modifier.offset(w * 0.17f, h * 0.75f).size(w * 0.66f, h * 0.17f)
        when {
            owned -> OwnedPill(equipped, onEquip, m)
            else -> PaintedPill(R.drawable.st_pill_orange, m, enabled = canAfford, onClick = { if (canAfford) onBuy() else onCannotAfford() }) {
                CoinPrice(frame.storePrice, size = 13.sp, coin = 17.dp)
            }
        }
    }
}

/**
 * Today's free joker, all in one painted picture (st_daily_full, 1300 x 665 px): the wooden plank with the crown, the
 * paper with the gift, and the green button underneath, all inside one frame. The title is lettered on the plank, the
 * joker's own tile and its name and description on the paper, and the button's words on the button. Shown only while
 * unclaimed — once taken the card is simply gone until tomorrow's.
 */
@Composable
private fun DailyJokerCard(type: JokerType, onClaim: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(1300f / 665f)) {
        val u = maxWidth / 1300f
        val fontScale0 = androidx.compose.ui.platform.LocalDensity.current.fontScale
        fun fs(px: Float) = (px * u.value / fontScale0).sp
        Image(painterResource(R.drawable.st_daily_full), contentDescription = null, contentScale = ContentScale.FillBounds, modifier = Modifier.matchParentSize())
        // The title on the plank, to the right of the crown.
        com.sualtikasifi.cizimhafiza.presentation.common.FitText(
            text = stringResource(R.string.store_daily_joker_tag),
            style = androidx.compose.ui.text.TextStyle(
                fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = fs(54f),
                color = Color(0xFFFFF4D6), textAlign = TextAlign.Center,
                shadow = Shadow(Color(0xFF5A2410), Offset(0f, 3f), 4f)
            ),
            maxLines = 1, minScale = 0.5f,
            modifier = Modifier.offset(u * 440f, u * 78f).size(u * 560f, u * 78f)
        )
        // The joker's tile on the paper's left, its name and description beside it.
        com.sualtikasifi.cizimhafiza.presentation.common.JokerArt(type, u * 220f, Modifier.offset(u * 135f, u * 242f))
        Column(Modifier.offset(u * 400f, u * 250f).size(u * 470f, u * 230f), verticalArrangement = Arrangement.Center) {
            com.sualtikasifi.cizimhafiza.presentation.common.FitText(
                text = stringResource(type.labelRes()),
                style = androidx.compose.ui.text.TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = fs(46f), color = Ink, textAlign = TextAlign.Start),
                maxLines = 1, minScale = 0.6f, contentAlignment = Alignment.CenterStart,
                modifier = Modifier.fillMaxWidth().height(u * 60f)
            )
            Text(
                text = stringResource(type.descRes()),
                style = androidx.compose.ui.text.TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.SemiBold, fontSize = fs(36f), lineHeight = fs(44f), color = Ink.copy(alpha = 0.85f)),
                maxLines = 3
            )
        }
        // The green button (x 309..994, y 509..616 of the picture): the whole button caves in under the finger,
        // not just its words (the picture's own pixels are drawn again, shrunk, over a darker recess).
        val context = androidx.compose.ui.platform.LocalContext.current
        val sheet = remember { runCatching { com.sualtikasifi.cizimhafiza.presentation.common.BackdropCache.get(context.resources, R.drawable.st_daily_full) }.getOrNull() }
        val sink = com.sualtikasifi.cizimhafiza.presentation.mainmenu.rememberSink(0.93f)
        val pxK = (sheet?.width ?: 1300) / 1300f
        Box(
            modifier = Modifier
                .offset(u * 309f, u * 509f)
                .size(u * 685f, u * 107f)
                .sunkenArt(sink, sheet, 309f * pxK, 509f * pxK, 685f * pxK, 107f * pxK, u * 53f)
                .clickable(interactionSource = sink.source, indication = null, onClick = onClaim)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().sinkWith(sink, 0.5f, 0.5f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                androidx.compose.material3.Icon(
                    androidx.compose.material.icons.Icons.Filled.PlayCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(u * 62f)
                )
                Spacer(Modifier.width(u * 16f))
                com.sualtikasifi.cizimhafiza.presentation.common.FitText(
                    text = stringResource(R.string.store_daily_joker_action),
                    style = androidx.compose.ui.text.TextStyle(
                        fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = fs(52f), color = Color.White,
                        shadow = Shadow(Color(0xFF0E3A06), Offset(0f, 3f), 3f)
                    ),
                    maxLines = 1, minScale = 0.5f,
                    modifier = Modifier.width(u * 470f).height(u * 76f)
                )
            }
        }
    }
}

/** One joker row on its painted card: its tile, name, what it does, how many you have, and two ways to buy (1, or a discounted bundle of 5). */
@Composable
private fun JokerCard(
    type: JokerType,
    owned: Int,
    gold: Int,
    onBuy: (Int) -> Unit,
    onCannotAfford: () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(2.416f)) {
        val w = maxWidth
        val h = maxHeight
        Image(painterResource(R.drawable.st_card_joker), contentDescription = null, contentScale = ContentScale.FillBounds, modifier = Modifier.matchParentSize())
        com.sualtikasifi.cizimhafiza.presentation.common.JokerArt(type, h * 0.42f, Modifier.offset(w * 0.1f, h * 0.3f))
        // Title and description stay left of the painted stars (they start at about 0.68 of the card's width).
        com.sualtikasifi.cizimhafiza.presentation.common.FitText(
            text = stringResource(type.labelRes()),
            style = androidx.compose.ui.text.TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Ink, textAlign = TextAlign.Start),
            maxLines = 1, minScale = 0.6f, contentAlignment = Alignment.CenterStart,
            modifier = Modifier.offset(w * 0.34f, h * 0.12f).size(w * 0.33f, h * 0.2f)
        )
        // "Sende: n" sits straight under the joker's tile, centred on it, with no plate behind it.
        Box(Modifier.offset(w * 0.1f, h * 0.74f).width(h * 0.42f), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.joker_owned, owned),
                style = com.sualtikasifi.cizimhafiza.presentation.common.PaintedStyle(
                    color = Color(0xFF9A3412), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center
                ),
                maxLines = 1
            )
        }
        Text(
            text = stringResource(type.descRes()),
            fontSize = 9.5.sp,
            lineHeight = 11.sp,
            color = Ink.copy(alpha = 0.78f),
            maxLines = 3,
            modifier = Modifier.offset(w * 0.34f, h * 0.32f).size(w * 0.33f, h * 0.27f)
        )
        Row(
            Modifier.offset(w * 0.34f, h * 0.58f).size(w * 0.56f, h * 0.27f),
            horizontalArrangement = Arrangement.spacedBy(w * 0.02f)
        ) {
            listOf(1, JokerType.BULK_QUANTITY).forEach { qty ->
                val price = type.priceFor(qty)
                val canAfford = gold >= price
                PaintedPill(
                    res = if (qty == 1) R.drawable.st_pill_orange else R.drawable.st_pill_blue,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    enabled = canAfford,
                    onClick = { if (canAfford) onBuy(qty) else onCannotAfford() }
                ) {
                    if (qty == 1) {
                        CoinPrice(price)
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy((-2).dp), modifier = Modifier.offset(y = 4.dp)) {
                            com.sualtikasifi.cizimhafiza.presentation.common.FitText(
                                text = stringResource(R.string.joker_buy_bulk, qty, JokerType.BULK_DISCOUNT_PERCENT),
                                style = PillTextShadow.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.95f), textAlign = TextAlign.Center),
                                maxLines = 1, minScale = 0.6f,
                                modifier = Modifier.fillMaxWidth(0.82f).height(11.dp)
                            )
                            CoinPrice(price, size = 13.sp, coin = 14.dp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Slides and fades one card in when its tab opens — from the side the tab came from, one card after another —
 * so switching tabs reads as the shelf being swapped rather than as the contents blinking over. Cards far
 * down the list, or scrolled into view later, skip it.
 */
@Composable
private fun TabEntrance(tab: Int, direction: Int, openedAt: Long, index: Int, content: @Composable () -> Unit) {
    // Only cards that appear together with the tab itself; one scrolled back into view later (the grid recomposes
    // it from scratch) must show at once, not slide in again.
    val animate = remember(tab) { index < 9 && System.currentTimeMillis() - openedAt < 700L }
    val progress = remember(tab) { androidx.compose.animation.core.Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(tab) {
        if (!animate) return@LaunchedEffect
        delay(index * 55L)
        progress.animateTo(
            1f,
            androidx.compose.animation.core.spring(dampingRatio = 0.78f, stiffness = 260f)
        )
    }
    Box(
        modifier = Modifier.graphicsLayer {
            val p = progress.value
            alpha = (p * 1.6f).coerceIn(0f, 1f)
            translationX = (1f - p) * direction * 64.dp.toPx()
            translationY = (1f - p) * 10.dp.toPx()
            val scale = 0.94f + 0.06f * p.coerceAtMost(1f)
            scaleX = scale
            scaleY = scale
        }
    ) { content() }
}
