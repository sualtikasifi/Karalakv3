package com.sualtikasifi.cizimhafiza.presentation.runes

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.layout.height
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.Rune
import com.sualtikasifi.cizimhafiza.domain.model.RuneBranch
import com.sualtikasifi.cizimhafiza.domain.model.RuneTree
import com.sualtikasifi.cizimhafiza.domain.model.RuneUnit
import com.sualtikasifi.cizimhafiza.presentation.common.ConfettiExplosion
import com.sualtikasifi.cizimhafiza.presentation.common.PrimaryButton
import com.sualtikasifi.cizimhafiza.presentation.common.ScreenTopActions
import com.sualtikasifi.cizimhafiza.presentation.common.cachedPainterResource
import com.sualtikasifi.cizimhafiza.presentation.common.pressable
import com.sualtikasifi.cizimhafiza.presentation.theme.DisplayFont
import com.sualtikasifi.cizimhafiza.util.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.NumberFormat
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt

@HiltViewModel
class RunesViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val gold = settingsRepository.goldBalance
    val levels = settingsRepository.runeLevels
    fun buy(id: String): Boolean = settingsRepository.buyRune(id)
}

private val Spacing = 116.dp
private const val SoonKey = "soon:"
private val Ink = Color(0xFF3A2416)

/** "Karalak Rünleri": a big pannable and zoomable map of nodes, bought with gold, each a small permanent bonus. */
@Composable
fun RunesScreen(onBack: () -> Unit, onStore: () -> Unit = {}, viewModel: RunesViewModel = hiltViewModel()) {
    val gold by viewModel.gold.collectAsState()
    val levels by viewModel.levels.collectAsState()
    var selected by remember { mutableStateOf<String?>(null) }
    var celebration by remember { mutableIntStateOf(0) }

    Box(Modifier.fillMaxSize()) {
        Image(
            painter = cachedPainterResource(R.drawable.bg_result_wood),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Column(Modifier.fillMaxSize()) {
            ScreenTopActions(
                onBack = onBack,
                title = stringResource(R.string.rune_screen_title),
                trailing = { GoldPill(gold, onStore) }
            )
            RuneMap(
                levels = levels,
                gold = gold,
                selected = selected,
                onSelect = { selected = if (selected == it) null else it },
                onClear = { selected = null },
                modifier = Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 6.dp)
            )
            InfoCard(
                selected = selected,
                levels = levels,
                gold = gold,
                onBuy = { id -> if (viewModel.buy(id)) celebration++ },
                modifier = Modifier.navigationBarsPadding().padding(start = 10.dp, end = 10.dp, bottom = 10.dp, top = 2.dp)
            )
        }
        if (celebration > 0) {
            ConfettiExplosion(seed = celebration * 7, count = 60, durationMs = 1400, originY = 0.4f)
        }
    }
}

/** The coin counter and its "+" — the gold is spent in the store too, so one tap leads there. */
@Composable
private fun GoldPill(gold: Int, onStore: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF2B1A12))
            .padding(start = 6.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Image(painterResource(R.drawable.icon_gold_coin), contentDescription = null, modifier = Modifier.size(28.dp))
        Text(
            text = NumberFormat.getIntegerInstance().format(gold),
            fontFamily = DisplayFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 17.sp,
            color = Color.White
        )
        Box(
            modifier = Modifier
                .size(32.dp)
                .pressable(pressedScale = 0.9f, onClick = onStore)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFFFFC93C), Color(0xFFF08A1B)))),
            contentAlignment = Alignment.Center
        ) {
            Text("+", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Color.White)
        }
    }
}

@Composable
private fun RuneMap(
    levels: Map<String, Int>,
    gold: Int,
    selected: String?,
    onSelect: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(26.dp)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clip(shape)
            .background(Color(0xFFE9D3A6))
    ) {
        Image(
            painter = painterResource(R.drawable.rune_map_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        val mapWidth = maxWidth
        val mapHeight = maxHeight
        val density = LocalDensity.current
        val spacingPx = with(density) { Spacing.toPx() }
        // How far the map reaches from the middle, so panning can bring every node to the centre.
        val reachX = remember { (RuneTree.all.map { abs(it.x) } + RuneTree.placeholders.map { abs(it.x) }).max() }
        val reachY = remember { (RuneTree.all.map { abs(it.y) } + RuneTree.placeholders.map { abs(it.y) }).max() }
        // Zooming out goes as far as it takes to see the whole map at once (all nodes and a little margin).
        val minScale = remember(mapWidth, mapHeight) {
            val w = with(density) { mapWidth.toPx() }
            val h = with(density) { mapHeight.toPx() }
            minOf(w / ((2 * reachX + 1.4f) * spacingPx), h / ((2 * reachY + 1.4f) * spacingPx)).coerceIn(0.12f, 0.6f)
        }
        var scale by remember { mutableFloatStateOf(0.78f) }
        var pan by remember { mutableStateOf(Offset.Zero) }

        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures(onTap = { onClear() }) }
                .pointerInput(Unit) {
                    detectTransformGestures { _, panChange, zoom, _ ->
                        scale = (scale * zoom).coerceIn(minScale, 1.5f)
                        val limX = reachX * spacingPx * scale
                        val limY = reachY * spacingPx * scale
                        pan = Offset((pan.x + panChange.x).coerceIn(-limX, limX), (pan.y + panChange.y).coerceIn(-limY, limY))
                    }
                }
        ) {
            val cx = mapWidth / 2
            val cy = mapHeight / 2
            Box(
                Modifier.fillMaxSize().graphicsLayer {
                    translationX = pan.x
                    translationY = pan.y
                    scaleX = scale
                    scaleY = scale
                }
            ) {
                // The paths.
                Canvas(Modifier.fillMaxSize()) {
                    fun point(x: Float, y: Float) = Offset(cx.toPx() + x * spacingPx, cy.toPx() + y * spacingPx)
                    RuneTree.all.forEach { rune ->
                        rune.requires.forEach { parentId ->
                            val parent = RuneTree.get(parentId) ?: return@forEach
                            val owned = RuneTree.levelOf(levels, rune.id) >= 1
                            val open = RuneTree.isUnlocked(levels, rune)
                            drawLine(
                                color = if (owned) branchColors(rune.branch).second else Color(0xFF7A4A22).copy(alpha = if (open) 0.5f else 0.22f),
                                start = point(parent.x, parent.y),
                                end = point(rune.x, rune.y),
                                strokeWidth = if (owned) 9.dp.toPx() else 6.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }
                    RuneTree.placeholders.forEach { p ->
                        val from = p.parent?.let { RuneTree.placeholders[it] }
                        drawLine(
                            color = Color(0xFF7A4A22).copy(alpha = 0.18f),
                            start = point(from?.x ?: 0f, from?.y ?: 0f),
                            end = point(p.x, p.y),
                            strokeWidth = 5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
                RuneTree.placeholders.forEachIndexed { i, p ->
                    Box(Modifier.offset(cx + Spacing * p.x - 31.dp, cy + Spacing * p.y - 31.dp)) {
                        LockedTile(selected == SoonKey + i) { onSelect(SoonKey + i) }
                    }
                }
                RuneTree.all.forEach { rune ->
                    val size = tileSize(rune)
                    Box(Modifier.offset(cx + Spacing * rune.x - size / 2, cy + Spacing * rune.y - size / 2)) {
                        val level = RuneTree.levelOf(levels, rune.id)
                        val unlocked = RuneTree.isUnlocked(levels, rune)
                        val cost = RuneTree.nextCost(levels, rune)
                        RuneTile(
                            rune = rune,
                            level = level,
                            unlocked = unlocked,
                            affordable = unlocked && cost != null && gold >= cost,
                            selected = selected == rune.id,
                            onClick = { onSelect(rune.id) }
                        )
                    }
                }
            }
        }
    }
}

private fun tileSize(rune: Rune): Dp = when {
    rune.id == RuneTree.ROOT_ID -> 84.dp
    rune.isKeystone -> 74.dp
    else -> 62.dp
}

/** Light and dark of each branch: the tile's face, and the path colour once the node is owned. */
private fun branchColors(branch: RuneBranch): Pair<Color, Color> = when (branch) {
    RuneBranch.ROOT -> Color(0xFFFFD35E) to Color(0xFFE08A1B)
    RuneBranch.GOLD -> Color(0xFFFFD35E) to Color(0xFFE59A12)
    RuneBranch.CHEST -> Color(0xFFFF8F80) to Color(0xFFD42A1E)
    RuneBranch.LUCK -> Color(0xFFFF9BD0) to Color(0xFFD43A8E)
}

/** The icon of [key]: rune_n_<id> (one of a kind) first, then rune_icon_<key>, otherwise the [emoji]. */
@Composable
private fun RuneIcon(key: String, emoji: String, size: Dp, modifier: Modifier = Modifier, runeId: String? = null) {
    val context = LocalContext.current
    val id = remember(key, runeId) {
        val own = runeId?.let { context.resources.getIdentifier("rune_n_$it", "drawable", context.packageName) } ?: 0
        if (own != 0) own else context.resources.getIdentifier("rune_icon_$key", "drawable", context.packageName)
    }
    if (id != 0) {
        Image(painterResource(id), contentDescription = null, contentScale = ContentScale.Fit, modifier = modifier.size(size))
    } else {
        Text(emoji, fontSize = (size.value * 0.78f).sp, modifier = modifier)
    }
}

/** The painted plate of [rune]: the root's medallion, an ornate keystone, otherwise coloured by branch, open (cream) or locked (grey). */
private fun tileArt(rune: Rune, owned: Boolean, unlocked: Boolean, maxed: Boolean = false): Int = when {
    rune.id == RuneTree.ROOT_ID -> R.drawable.rune_root
    rune.isKeystone -> when {
        owned -> when (rune.branch) {
            RuneBranch.CHEST -> R.drawable.rune_key_red
            RuneBranch.LUCK -> R.drawable.rune_key_pink
            else -> R.drawable.rune_key_gold
        }
        unlocked -> R.drawable.rune_key_open
        else -> R.drawable.rune_key_locked
    }
    !owned && !unlocked -> R.drawable.rune_frame_locked
    !owned -> R.drawable.rune_frame_open
    maxed -> R.drawable.rune_frame_max
    else -> when (rune.branch) {
        RuneBranch.CHEST -> R.drawable.rune_frame_red
        RuneBranch.LUCK -> R.drawable.rune_frame_pink
        else -> R.drawable.rune_frame_gold
    }
}

@Composable
private fun RuneTile(rune: Rune, level: Int, unlocked: Boolean, affordable: Boolean, selected: Boolean, onClick: () -> Unit) {
    val owned = level >= 1
    val maxed = owned && level >= rune.maxLevel && !rune.isKeystone && rune.id != RuneTree.ROOT_ID
    val size = tileSize(rune)
    val pulse = rememberInfiniteTransition(label = "runePulse")
    val glow by pulse.animateFloat(0.35f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "runeGlow")
    Box(modifier = Modifier.size(size).pressable(pressedScale = 0.92f, onClick = onClick), contentAlignment = Alignment.Center) {
        if (selected || affordable) {
            Image(
                painter = painterResource(R.drawable.rune_halo),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.requiredSize(size * 1.45f).alpha(if (selected) 1f else glow)
            )
        }
        Image(
            painter = painterResource(tileArt(rune, owned, unlocked, maxed)),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.requiredSize(size * (if (maxed) 1.16f else 1f))
        )
        // The medallion of the plate sits a little low on the ornate ones, which carry a crown on top.
        Box(Modifier.offset(y = if (rune.isKeystone && rune.id != RuneTree.ROOT_ID) size * 0.07f else 0.dp), contentAlignment = Alignment.Center) {
            if (unlocked || owned) {
                RuneIcon(rune.effect.iconKey, rune.effect.emoji, size * 0.58f, Modifier.alpha(if (owned || rune.id == RuneTree.ROOT_ID) 1f else 0.7f), runeId = rune.id)
            } else {
                RuneIcon("lock", "🔒", size * 0.45f, Modifier.alpha(0.85f))
            }
        }
        if (affordable) {
            Image(
                painter = painterResource(R.drawable.rune_badge_up),
                contentDescription = null,
                modifier = Modifier.align(Alignment.TopEnd).offset(x = 5.dp, y = (-5).dp).size(20.dp)
            )
        }
        if (rune.maxLevel > 1 && rune.id != RuneTree.ROOT_ID) {
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).offset(y = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                repeat(rune.maxLevel) { i ->
                    Image(
                        painter = painterResource(if (i < level) R.drawable.rune_pip_full else R.drawable.rune_pip_empty),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LockedTile(selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(62.dp)
            .pressable(pressedScale = 0.94f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) Image(painterResource(R.drawable.rune_halo), null, contentScale = ContentScale.Fit, modifier = Modifier.requiredSize(90.dp))
        Image(painterResource(R.drawable.rune_soon), null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().alpha(0.8f))
        RuneIcon("q", "🔒", 30.dp, Modifier.alpha(0.9f))
    }
}

@Composable
private fun InfoCard(
    selected: String?,
    levels: Map<String, Int>,
    gold: Int,
    onBuy: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 150.dp)
            .sliced(R.drawable.rune_ui_card, srcCorner = 58, dstCorner = 26.dp)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.Center
    ) {
        val rune = selected?.let { RuneTree.get(it) }
        when {
            selected != null && rune == null -> {
                Text(stringResource(R.string.rune_soon), fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Ink)
                Spacer(Modifier.size(4.dp))
                Text(stringResource(R.string.rune_soon_desc), style = MaterialTheme.typography.bodyMedium, color = Ink.copy(alpha = 0.85f))
            }
            rune != null -> RuneDetail(rune, levels, gold, onBuy)
            else -> {
                val next = RuneTree.all
                    .filter { RuneTree.isUnlocked(levels, it) && RuneTree.nextCost(levels, it) != null }
                    .minByOrNull { RuneTree.nextCost(levels, it) ?: Int.MAX_VALUE }
                Text(
                    text = if (next == null) stringResource(R.string.rune_hint_all) else stringResource(
                        R.string.rune_hint_next,
                        stringResource(next.nameRes),
                        NumberFormat.getIntegerInstance().format(RuneTree.nextCost(levels, next) ?: 0)
                    ),
                    fontFamily = DisplayFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun tileText(unit: RuneUnit, value: Int): String = when (unit) {
    RuneUnit.PERCENT -> stringResource(R.string.rune_tile_pct, value)
    RuneUnit.POINTS -> stringResource(R.string.rune_tile_pts, value)
    RuneUnit.COUNT -> stringResource(R.string.rune_tile_cnt, value)
}

@Composable
private fun RuneDetail(rune: Rune, levels: Map<String, Int>, gold: Int, onBuy: (String) -> Unit) {
    val level = RuneTree.levelOf(levels, rune.id)
    val unlocked = RuneTree.isUnlocked(levels, rune)
    val cost = RuneTree.nextCost(levels, rune)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(66.dp), contentAlignment = Alignment.Center) {
            Image(painterResource(tileArt(rune, level >= 1, true, level >= rune.maxLevel && !rune.isKeystone)), null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
            RuneIcon(rune.effect.iconKey, rune.effect.emoji, 38.dp, runeId = rune.id)
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(rune.nameRes),
                fontFamily = DisplayFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 21.sp,
                color = Ink
            )
            Text(
                text = if (rune.id == RuneTree.ROOT_ID) stringResource(R.string.rune_root_desc) else stringResource(rune.effect.descRes, rune.perLevel),
                style = MaterialTheme.typography.bodyMedium,
                color = Ink.copy(alpha = 0.85f)
            )
        }
        if (rune.id != RuneTree.ROOT_ID) {
            Text(
                text = stringResource(R.string.rune_level_of, level, rune.maxLevel),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0xFF7A4A22)).padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
    }
    if (rune.id == RuneTree.ROOT_ID) return
    // One box per level: what that level brings in total, the reached ones lit.
    Spacer(Modifier.size(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        repeat(rune.maxLevel) { i ->
            val n = i + 1
            val reached = level >= n
            val isNext = level + 1 == n
            Column(
                modifier = Modifier
                    .weight(1f)
                    .sliced(
                        if (reached) R.drawable.rune_ui_box_gold else if (isNext) R.drawable.rune_ui_box_empty else R.drawable.rune_ui_box_gray,
                        srcCorner = 40, dstCorner = 14.dp
                    )
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(painterResource(if (reached) R.drawable.rune_pip_full else R.drawable.rune_pip_empty), null, modifier = Modifier.size(14.dp))
                Text(stringResource(R.string.rune_level_n, n), style = MaterialTheme.typography.labelSmall, color = Ink.copy(alpha = 0.8f))
                Text(
                    tileText(rune.effect.unit, rune.perLevel * n),
                    fontFamily = DisplayFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = if (reached) Ink else if (isNext) Ink.copy(alpha = 0.85f) else Color.White
                )
                Text(stringResource(rune.effect.labelRes), style = MaterialTheme.typography.labelSmall, color = Ink.copy(alpha = 0.7f), textAlign = TextAlign.Center)
            }
        }
    }
    Spacer(Modifier.size(8.dp))
    when {
        cost == null -> Text(stringResource(R.string.rune_maxed), fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E7A45), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        !unlocked -> Text(stringResource(R.string.rune_locked), style = MaterialTheme.typography.bodyMedium, color = Color(0xFFB3401A), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        else -> {
            if (gold < cost) {
                Text(
                    stringResource(R.string.rune_missing, NumberFormat.getIntegerInstance().format(cost - gold)),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFB3401A),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.size(4.dp))
            }
            val canBuy = gold >= cost
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .pressable(pressedScale = 0.96f, onClick = { if (canBuy) onBuy(rune.id) })
                    .sliced(if (canBuy) R.drawable.rune_ui_btn_green else R.drawable.rune_ui_btn_gray, srcCorner = 73, dstCorner = 27.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Image(painterResource(R.drawable.icon_gold_coin), null, modifier = Modifier.size(26.dp))
                    Text(
                        text = stringResource(R.string.rune_buy, NumberFormat.getIntegerInstance().format(cost)),
                        fontFamily = DisplayFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Paints [res] behind the content as a nine-slice: the corners keep their size ([srcCorner] px of the art drawn at [dstCorner]),
 * the edges and the middle stretch. A frame can then be any size without its studs or round ends getting squashed.
 */
@Composable
private fun Modifier.sliced(@DrawableRes res: Int, srcCorner: Int, dstCorner: Dp): Modifier {
    val bmp = ImageBitmap.imageResource(res)
    return drawBehind {
        val c = srcCorner
        val d = dstCorner.toPx().roundToInt().coerceAtMost(minOf(size.width, size.height).toInt() / 2)
        val w = size.width.roundToInt()
        val h = size.height.roundToInt()
        val xs = intArrayOf(0, c, bmp.width - c, bmp.width)
        val ys = intArrayOf(0, c, bmp.height - c, bmp.height)
        val dx = intArrayOf(0, d, w - d, w)
        val dy = intArrayOf(0, d, h - d, h)
        for (r in 0 until 3) for (q in 0 until 3) {
            drawImage(
                bmp,
                srcOffset = IntOffset(xs[q], ys[r]),
                srcSize = IntSize(xs[q + 1] - xs[q], ys[r + 1] - ys[r]),
                dstOffset = IntOffset(dx[q], dy[r]),
                dstSize = IntSize(dx[q + 1] - dx[q], dy[r + 1] - dy[r])
            )
        }
    }
}
