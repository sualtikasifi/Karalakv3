package com.sualtikasifi.cizimhafiza.presentation.runes

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.text.font.FontStyle
import com.sualtikasifi.cizimhafiza.presentation.common.WoodBackArrow
import com.sualtikasifi.cizimhafiza.presentation.common.LetteredText
import androidx.compose.foundation.layout.statusBarsPadding
import kotlin.math.atan2
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
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
import com.sualtikasifi.cizimhafiza.domain.model.RuneGates
import com.sualtikasifi.cizimhafiza.domain.model.RuneTree
import com.sualtikasifi.cizimhafiza.domain.model.RuneUnit
import com.sualtikasifi.cizimhafiza.presentation.common.ConfettiExplosion
import com.sualtikasifi.cizimhafiza.presentation.common.PrimaryButton
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
    val xp = settingsRepository.lifetimeXp
    fun buy(id: String): Boolean = settingsRepository.buyRune(id)
}

private val Spacing = 116.dp
private const val SoonKey = "soon:"
private val Ink = Color(0xFF3A2416)

/** "Karalak Rünleri": a big pannable and zoomable map of nodes, bought with gold, each a small permanent bonus. */
@Composable
fun RunesScreen(onBack: () -> Unit, @Suppress("UNUSED_PARAMETER") onStore: () -> Unit = {}, viewModel: RunesViewModel = hiltViewModel()) {
    val gold by viewModel.gold.collectAsState()
    val levels by viewModel.levels.collectAsState()
    val xp by viewModel.xp.collectAsState()
    val playerLevel = remember(xp) { com.sualtikasifi.cizimhafiza.domain.model.PlayerLevel.levelForXp(xp) }
    var selected by remember { mutableStateOf<String?>(null) }
    var celebration by remember { mutableIntStateOf(0) }

    Box(Modifier.fillMaxSize()) {
        Image(
            painter = cachedPainterResource(R.drawable.rune_screen_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Column(Modifier.fillMaxSize()) {
            // Back arrow, the title in whatever room is left, and a small gold counter to the right of it, so they never overlap.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WoodBackArrow(onClick = onBack)
                LetteredText(
                    text = stringResource(R.string.rune_screen_title),
                    size = 26.sp,
                    title = true,
                    modifier = Modifier.weight(1f)
                )
                GoldPill(gold)
            }
            RuneMap(
                levels = levels,
                playerLevel = playerLevel,
                gold = gold,
                selected = selected,
                onSelect = { selected = if (selected == it) null else it },
                onClear = { selected = null },
                modifier = Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 6.dp)
            )
            InfoCard(
                selected = selected,
                levels = levels,
                playerLevel = playerLevel,
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

/** The coin counter: a small dark pill with the coin and the gold. */
@Composable
private fun GoldPill(gold: Int) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xCC2B1A12))
            .padding(start = 4.dp, end = 10.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Image(painterResource(R.drawable.icon_gold_coin), contentDescription = null, modifier = Modifier.size(22.dp))
        Text(
            text = NumberFormat.getIntegerInstance().format(gold),
            fontFamily = DisplayFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp,
            color = Color.White,
            maxLines = 1
        )
    }
}

@Composable
private fun RuneMap(
    levels: Map<String, Int>,
    playerLevel: Int,
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
        val pathLocked = ImageBitmap.imageResource(R.drawable.rune_path_locked)
        val pathOpen = ImageBitmap.imageResource(R.drawable.rune_path_open)
        val pathOwned = ImageBitmap.imageResource(R.drawable.rune_path_owned)
        val density = LocalDensity.current
        val spacingPx = with(density) { Spacing.toPx() }
        // How far the map reaches from the middle, so panning can bring every node to the centre.
        // Fog of war: owned and buyable runes, plus a foggy hint after the latest rune; the rest is not drawn at all.
        val shown = remember(levels, playerLevel) { visibleRunes(levels, playerLevel) }
        val visible = remember(shown) { shown.map { it.rune } }
        val sealedIds = remember(shown) { shown.filter { it.sealed }.map { it.rune.id }.toSet() }
        val visibleIds = remember(visible) { visible.map { it.id }.toSet() }
        val reachX = remember(visible) { visible.maxOf { abs(it.x) } }
        val reachY = remember(visible) { visible.maxOf { abs(it.y) } }
        // Zooming out goes as far as it takes to see every visible node at once (and a little margin).
        val minScale = remember(mapWidth, mapHeight, visible) {
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
                // Keyed on the limits: they change as the map opens up, and a stale copy would keep the old ones.
                .pointerInput(minScale, reachX, reachY, spacingPx) {
                    detectTransformGestures { centroid, panChange, zoom, _ ->
                        val oldScale = scale
                        val newScale = (oldScale * zoom).coerceIn(minScale, 1.5f)
                        scale = newScale
                        // The layer scales around the middle of the map, so the point under the fingers has to be
                        // kept in place by moving the pan: it is the pinch's own centre that must not slide.
                        val focus = Offset(centroid.x - size.width / 2f, centroid.y - size.height / 2f)
                        val ratio = newScale / oldScale
                        val moved = focus - (focus - pan) * ratio + panChange
                        val limX = reachX * spacingPx * newScale
                        val limY = reachY * spacingPx * newScale
                        pan = Offset(moved.x.coerceIn(-limX, limX), moved.y.coerceIn(-limY, limY))
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
                // The paths: a painted rope tiled along each link (dashed trail while locked, beige rope when open, gold once owned).
                Canvas(Modifier.fillMaxSize()) {
                    fun point(x: Float, y: Float) = Offset(cx.toPx() + x * spacingPx, cy.toPx() + y * spacingPx)
                    fun path(from: Offset, to: Offset, tile: ImageBitmap, thickness: Float, alpha: Float = 1f) {
                        val length = (to - from).getDistance()
                        val angle = Math.toDegrees(atan2((to.y - from.y).toDouble(), (to.x - from.x).toDouble())).toFloat()
                        val h = thickness.roundToInt()
                        val w = (tile.width * thickness / tile.height).roundToInt().coerceAtLeast(1)
                        withTransform({
                            translate(from.x, from.y)
                            rotate(angle, Offset.Zero)
                        }) {
                            clipRect(0f, -thickness / 2, length, thickness / 2) {
                                var x = 0
                                while (x < length) {
                                    drawImage(tile, dstOffset = IntOffset(x, -h / 2), dstSize = IntSize(w, h), alpha = alpha)
                                    x += w
                                }
                            }
                        }
                    }
                    visible.forEach { rune ->
                        rune.requires.forEach { parentId ->
                            val parent = RuneTree.get(parentId)?.takeIf { it.id in visibleIds } ?: return@forEach
                            val owned = RuneTree.levelOf(levels, rune.id) >= 1
                            val open = RuneTree.canBuy(levels, rune, playerLevel)
                            path(
                                point(parent.x, parent.y), point(rune.x, rune.y),
                                if (owned) pathOwned else if (open) pathOpen else pathLocked,
                                (if (owned || open) 20.dp else 14.dp).toPx()
                            )
                        }
                    }
                }
                visible.forEach { rune ->
                    val sealed = rune.id in sealedIds
                    val size = tileSize(rune, sealed)
                    Box(Modifier.offset(cx + Spacing * rune.x - size / 2, cy + Spacing * rune.y - size / 2)) {
                        val level = RuneTree.levelOf(levels, rune.id)
                        val unlocked = RuneTree.canBuy(levels, rune, playerLevel)
                        val cost = RuneTree.nextCost(levels, rune)
                        RuneTile(
                            rune = rune,
                            level = level,
                            unlocked = unlocked,
                            sealed = sealed,
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

private fun tileSize(rune: Rune, sealed: Boolean = false): Dp = when {
    sealed -> 62.dp
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
    RuneBranch.GREEN -> Color(0xFF8BE08B) to Color(0xFF2E9B3E)
    RuneBranch.BLUE -> Color(0xFF8CC0FF) to Color(0xFF2A62C8)
    RuneBranch.PURPLE -> Color(0xFFC79BFF) to Color(0xFF7A3FC8)
    RuneBranch.CYAN -> Color(0xFF8FE6EA) to Color(0xFF1FA6B0)
    RuneBranch.LEGEND -> Color(0xFFB69AE0) to Color(0xFF3C2A5E)
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
            RuneBranch.GREEN -> R.drawable.rune_key_green
            RuneBranch.BLUE -> R.drawable.rune_key_blue
            RuneBranch.PURPLE -> R.drawable.rune_key_purple
            RuneBranch.CYAN -> R.drawable.rune_key_cyan
            RuneBranch.LEGEND -> R.drawable.rune_key_legend
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
        RuneBranch.GREEN -> R.drawable.rune_frame_green
        RuneBranch.BLUE -> R.drawable.rune_frame_blue
        RuneBranch.PURPLE -> R.drawable.rune_frame_purple
        RuneBranch.CYAN -> R.drawable.rune_frame_cyan
        RuneBranch.LEGEND -> R.drawable.rune_frame_legend
        else -> R.drawable.rune_frame_gold
    }
}

@Composable
private fun RuneTile(rune: Rune, level: Int, unlocked: Boolean, sealed: Boolean, affordable: Boolean, selected: Boolean, onClick: () -> Unit) {
    val owned = level >= 1
    val maxed = owned && level >= rune.maxLevel && !rune.isKeystone && rune.id != RuneTree.ROOT_ID
    val size = tileSize(rune, sealed)
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
            painter = painterResource(if (sealed) R.drawable.rune_gate_seal else tileArt(rune, owned, unlocked, maxed)),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.requiredSize(size * (if (maxed) 1.16f else 1f))
        )
        if (sealed) return@Box
        // The medallion of the plate sits a little low on the ornate ones, which carry a crown on top.
        val (dx, dy) = medallionShift(rune, owned, unlocked, maxed)
        Box(Modifier.offset(x = size * dx, y = size * dy), contentAlignment = Alignment.Center) {
            if (unlocked || owned) {
                RuneIcon(rune.effect.iconKey, rune.effect.emoji, size * medallionIcon(rune, owned), Modifier.alpha(if (owned || rune.id == RuneTree.ROOT_ID) 1f else 0.7f), runeId = rune.id)
            } else {
                RuneIcon("lock", "🔒", size * 0.42f, Modifier.alpha(0.85f))
            }
        }
        if (affordable) {
            Image(
                painter = painterResource(R.drawable.badge_upgrade),
                contentDescription = null,
                modifier = Modifier.align(Alignment.TopEnd).offset(x = 5.dp, y = (-5).dp).size(20.dp)
            )
        }
        if (rune.maxLevel > 5) {
            // A rune with many levels (the Legend ring) shows its level as a number instead of one dot per level.
            Text(
                text = "$level/${rune.maxLevel}",
                fontFamily = DisplayFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                color = Color.White,
                modifier = Modifier.align(Alignment.BottomCenter).offset(y = 9.dp)
                    .clip(RoundedCornerShape(50)).background(Color(0xCC3A2416)).padding(horizontal = 6.dp, vertical = 1.dp)
            )
        } else if (rune.maxLevel > 1 && rune.id != RuneTree.ROOT_ID) {
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
    playerLevel: Int,
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
            rune != null -> RuneDetail(rune, levels, playerLevel, gold, onBuy)
            else -> {
                val next = RuneTree.all
                    .filter { RuneTree.canBuy(levels, it, playerLevel) && RuneTree.nextCost(levels, it) != null }
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
    RuneUnit.FLAG -> stringResource(R.string.rune_tile_on)
}

@Composable
private fun RuneDetail(rune: Rune, levels: Map<String, Int>, playerLevel: Int, gold: Int, onBuy: (String) -> Unit) {
    val level = RuneTree.levelOf(levels, rune.id)
    val unlocked = RuneTree.isUnlocked(levels, rune)
    val cost = RuneTree.nextCost(levels, rune)
    if (unlocked && !RuneTree.ringOpen(levels, rune.ring, playerLevel)) {
        // A sealed ring: say what opens it and where the player stands, and show nothing of what is behind it.
        val percent = (RuneTree.ringProgress(levels, rune.ring - 1) * 100).toInt()
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(painterResource(R.drawable.rune_gate_seal), null, contentScale = ContentScale.Fit, modifier = Modifier.size(66.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.rune_gate_title), fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp, color = Ink)
                Text(
                    stringResource(R.string.rune_gate_body, RuneGates.levelFor(rune.ring), RuneGates.percentNeeded),
                    style = MaterialTheme.typography.bodyMedium, color = Ink.copy(alpha = 0.85f)
                )
                Text(
                    stringResource(R.string.rune_gate_now, playerLevel, percent),
                    style = MaterialTheme.typography.labelMedium, fontStyle = FontStyle.Italic, color = Color(0xFF8A5A2B)
                )
            }
        }
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(66.dp), contentAlignment = Alignment.Center) {
            Image(painterResource(tileArt(rune, level >= 1, true, level >= rune.maxLevel && !rune.isKeystone)), null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
            RuneIcon(rune.effect.iconKey, rune.effect.emoji, 42.dp, runeId = rune.id)
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(rune.nameRes),
                fontFamily = DisplayFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 21.sp,
                color = Ink
            )
            // A one-line bit of flavour under the name (rune_<id>_flavor), when the rune has one.
            val context = LocalContext.current
            val flavorId = remember(rune.id) { context.resources.getIdentifier("rune_${rune.id}_flavor", "string", context.packageName) }
            if (flavorId != 0) {
                Text(
                    text = stringResource(flavorId),
                    style = MaterialTheme.typography.labelMedium,
                    fontStyle = FontStyle.Italic,
                    color = Color(0xFF8A5A2B)
                )
            }
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
    // One box per level: what that level brings in total, the reached ones lit. A one-off ability has no boxes, a
    // rune with many levels (the Legend ring) one box for where it stands now and what the next level brings.
    Spacer(Modifier.size(8.dp))
    val showBoxes = !(rune.effect.unit == RuneUnit.FLAG && rune.maxLevel == 1)
    val longRune = rune.maxLevel > 4
    if (showBoxes) Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        val numbers = if (longRune) listOf(level.coerceAtLeast(1), (level + 1).coerceAtMost(rune.maxLevel)).distinct() else (1..rune.maxLevel).toList()
        numbers.forEach { n ->
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

/** A rune on the map; [sealed] means its ring is still closed, so only the seal is drawn on it. */
private data class Shown(val rune: Rune, val sealed: Boolean)

/**
 * The runes the player gets to see: everything owned or buyable, plus a foggy hint of what comes after the latest rune
 * (the deepest one owned): its next branches and the step after those. Other locked runes stay hidden, so no branch is
 * revealed ahead of time and the map opens step by step. The first runes of a ring that is not open yet show as a seal.
 */
private fun visibleRunes(levels: Map<String, Int>, playerLevel: Int): List<Shown> {
    val open = RuneTree.all.filter { RuneTree.levelOf(levels, it.id) >= 1 || RuneTree.canBuy(levels, it, playerLevel) }.map { it.id }.toSet()
    val sealed = RuneTree.all.filter { it.id !in open && RuneTree.isUnlocked(levels, it) && !RuneTree.ringOpen(levels, it.ring, playerLevel) }.map { it.id }.toSet()
    val latest = RuneTree.all.filter { it.id != RuneTree.ROOT_ID && RuneTree.levelOf(levels, it.id) >= 1 }.maxByOrNull { it.costs.first() }
    val front = HashSet<String>()
    if (latest != null) {
        front += latest.id
        RuneTree.all.filter { r -> r.requires.contains(latest.id) }.forEach { front += it.id }
    }
    // Fog only in rings that are open: a closed ring shows nothing beyond its seal.
    val fog = RuneTree.all.filter {
        it.id !in open && it.id !in sealed && RuneTree.ringOpen(levels, it.ring, playerLevel) && it.requires.any { parent -> parent in front }
    }.map { it.id }.toSet()
    return RuneTree.all.filter { it.id in open || it.id in sealed || it.id in fog }.map { Shown(it, it.id in sealed) }
}

/** Where the medallion of a plate sits, relative to the middle of the tile (in tile sizes), measured from the art itself. */
private fun medallionShift(rune: Rune, owned: Boolean, unlocked: Boolean, maxed: Boolean): Pair<Float, Float> = when {
    rune.id == RuneTree.ROOT_ID -> 0f to -0.01f
    rune.isKeystone -> 0f to when {
        owned -> when (rune.branch) {
            RuneBranch.GREEN -> 0.075f
            RuneBranch.BLUE -> 0.052f
            RuneBranch.PURPLE, RuneBranch.CYAN -> 0.053f
            RuneBranch.LEGEND -> 0.041f
            else -> 0.06f
        }
        unlocked -> 0.042f
        else -> 0.046f
    }
    maxed -> -0.046f to 0.023f
    owned && rune.branch == RuneBranch.LUCK -> 0f to -0.022f
    else -> 0f to 0f
}

/** How big the icon is drawn inside a plate's medallion, in tile sizes (the art has a little margin in its square). */
private fun medallionIcon(rune: Rune, owned: Boolean): Float = when {
    rune.id == RuneTree.ROOT_ID -> 0.56f
    // The newer keystone plates have a smaller medallion than the first three.
    rune.isKeystone && owned && rune.branch !in listOf(RuneBranch.GOLD, RuneBranch.CHEST, RuneBranch.LUCK) -> 0.5f
    rune.isKeystone -> 0.56f
    else -> 0.6f
}
