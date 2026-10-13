package com.sualtikasifi.cizimhafiza.presentation.runes

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.sualtikasifi.cizimhafiza.presentation.common.dotGridBackground
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
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(26.dp)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFFFFF1CF), Color(0xFFF1D49B))))
            .dotGridBackground(dotColor = Color(0x33805020), spacing = 26.dp, radius = 1.6.dp)
            .border(BorderStroke(4.dp, Color(0xFF9A5B22)), shape)
    ) {
        val mapWidth = maxWidth
        val mapHeight = maxHeight
        val density = LocalDensity.current
        val spacingPx = with(density) { Spacing.toPx() }
        // How far the map reaches from the middle, so panning can bring every node to the centre.
        val reachX = remember { (RuneTree.all.map { abs(it.x) } + RuneTree.placeholders.map { abs(it.x) }).max() }
        val reachY = remember { (RuneTree.all.map { abs(it.y) } + RuneTree.placeholders.map { abs(it.y) }).max() }
        var scale by remember { mutableFloatStateOf(0.78f) }
        var pan by remember { mutableStateOf(Offset.Zero) }

        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, panChange, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.45f, 1.5f)
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

/** The icon of [key]: a drawable named rune_icon_<key> when the project has one, otherwise the [emoji]. */
@Composable
private fun RuneIcon(key: String, emoji: String, size: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val id = remember(key) { context.resources.getIdentifier("rune_icon_$key", "drawable", context.packageName) }
    if (id != 0) {
        Image(painterResource(id), contentDescription = null, contentScale = ContentScale.Fit, modifier = modifier.size(size))
    } else {
        Text(emoji, fontSize = (size.value * 0.78f).sp, modifier = modifier)
    }
}

@Composable
private fun RuneTile(rune: Rune, level: Int, unlocked: Boolean, affordable: Boolean, selected: Boolean, onClick: () -> Unit) {
    val (light, dark) = branchColors(rune.branch)
    val owned = level >= 1
    val size = tileSize(rune)
    val shapeTile = if (rune.id == RuneTree.ROOT_ID) CircleShape else RoundedCornerShape(if (rune.isKeystone) 22.dp else 17.dp)
    val pulse = rememberInfiniteTransition(label = "runePulse")
    val glow by pulse.animateFloat(0.2f, 0.75f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "runeGlow")
    Box(modifier = Modifier.size(size).pressable(pressedScale = 0.92f, onClick = onClick), contentAlignment = Alignment.Center) {
        if (affordable && !selected) {
            Box(Modifier.size(size + 12.dp).alpha(glow).background(Color(0xFFFFE066), shapeTile))
        }
        Box(
            modifier = Modifier
                .size(if (selected) size + 6.dp else size)
                .clip(shapeTile)
                .background(
                    when {
                        owned -> Brush.verticalGradient(listOf(light, dark))
                        unlocked -> Brush.verticalGradient(listOf(Color(0xFFFFF6DD), Color(0xFFE6CE9C)))
                        else -> Brush.verticalGradient(listOf(Color(0xFFB4A892), Color(0xFF7E7360)))
                    }
                )
                .border(
                    width = if (selected) 5.dp else 4.dp,
                    color = when {
                        selected -> Color.White
                        owned && level >= rune.maxLevel && rune.id != RuneTree.ROOT_ID -> Color(0xFFFFF0A0)
                        owned -> Color(0xFFFFF3C4)
                        unlocked -> dark
                        else -> Color(0xFF5E5547)
                    },
                    shape = shapeTile
                ),
            contentAlignment = Alignment.Center
        ) {
            // A soft highlight across the top, like the glossy tiles of the mock-up.
            Box(
                Modifier.align(Alignment.TopCenter).fillMaxWidth().size(size / 2.4f)
                    .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = if (owned) 0.35f else 0.18f), Color.Transparent)))
            )
            if (unlocked || owned) {
                RuneIcon(rune.effect.iconKey, rune.effect.emoji, size * 0.55f, Modifier.alpha(if (owned || rune.id == RuneTree.ROOT_ID) 1f else 0.8f))
            } else {
                RuneIcon("lock", "🔒", size * 0.5f, Modifier.alpha(0.7f))
            }
        }
        if (rune.maxLevel > 1 && rune.id != RuneTree.ROOT_ID) {
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).offset(y = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                repeat(rune.maxLevel) { i ->
                    Box(
                        Modifier.size(9.dp).background(if (i < level) Color(0xFFFFD35E) else Color(0xFF6A4A2A), CircleShape)
                            .border(1.5.dp, Color.White, CircleShape)
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
            .pressable(pressedScale = 0.94f, onClick = onClick)
            .clip(RoundedCornerShape(17.dp))
            .background(Brush.verticalGradient(listOf(Color(0x66B4A892), Color(0x667E7360))))
            .border(BorderStroke(if (selected) 4.dp else 3.dp, Color(0x885E5547)), RoundedCornerShape(17.dp)),
        contentAlignment = Alignment.Center
    ) {
        RuneIcon("soon", "🔒", 30.dp, Modifier.alpha(0.55f))
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
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFFFFF6DD), Color(0xFFF3DDAA))))
            .border(BorderStroke(3.dp, Color(0xFF9A5B22)), shape)
            .padding(12.dp),
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
    val (light, dark) = branchColors(rune.branch)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.size(58.dp).clip(RoundedCornerShape(16.dp)).background(Brush.verticalGradient(listOf(light, dark)))
                .border(3.dp, Color(0xFFFFF3C4), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) { RuneIcon(rune.effect.iconKey, rune.effect.emoji, 34.dp) }
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
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (reached) Color(0xFFFFF3C4) else Color(0xFFF7EBCD))
                    .border(BorderStroke(if (isNext || reached) 2.5.dp else 1.5.dp, if (reached) Color(0xFFE59A12) else if (isNext) Color(0xFFE59A12).copy(alpha = 0.6f) else Color(0xFFCDB98A)), RoundedCornerShape(14.dp))
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(Modifier.size(12.dp).background(if (reached) Color(0xFFFFB400) else Color(0xFFB9AA88), CircleShape))
                Text(stringResource(R.string.rune_level_n, n), style = MaterialTheme.typography.labelSmall, color = Ink.copy(alpha = 0.8f))
                Text(
                    tileText(rune.effect.unit, rune.perLevel * n),
                    fontFamily = DisplayFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = if (reached) Color(0xFFE5701A) else Ink.copy(alpha = 0.75f)
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
            PrimaryButton(
                text = stringResource(R.string.rune_buy, NumberFormat.getIntegerInstance().format(cost)),
                onClick = { onBuy(rune.id) },
                enabled = gold >= cost,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
