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
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.Rune
import com.sualtikasifi.cizimhafiza.domain.model.RuneBranch
import com.sualtikasifi.cizimhafiza.domain.model.RunePlaceholder
import com.sualtikasifi.cizimhafiza.domain.model.RuneTree
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
import kotlin.math.roundToInt

@HiltViewModel
class RunesViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val gold = settingsRepository.goldBalance
    val levels = settingsRepository.runeLevels
    fun buy(id: String): Boolean = settingsRepository.buyRune(id)
}

private val NodeSize = 72.dp
private val Spacing = 116.dp
private const val SoonKey = "soon"

private val Ink = Color(0xFF3A2416)

/** "Karalak Rünleri": a pannable map of nodes, bought with gold, each a small permanent bonus. */
@Composable
fun RunesScreen(onBack: () -> Unit, viewModel: RunesViewModel = hiltViewModel()) {
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
                trailing = { GoldPill(gold) }
            )
            RuneMap(
                levels = levels,
                gold = gold,
                selected = selected,
                onSelect = { selected = if (selected == it) null else it },
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 6.dp)
            )
            InfoCard(
                selected = selected,
                levels = levels,
                gold = gold,
                onBuy = { id ->
                    if (viewModel.buy(id)) celebration++
                },
                modifier = Modifier.navigationBarsPadding().padding(start = 12.dp, end = 12.dp, bottom = 12.dp, top = 2.dp)
            )
        }
        if (celebration > 0) {
            ConfettiExplosion(seed = celebration * 7, count = 60, durationMs = 1400, originY = 0.4f)
        }
    }
}

@Composable
private fun GoldPill(gold: Int) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF2B1A12))
            .padding(start = 6.dp, end = 16.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        androidx.compose.foundation.Image(painterResource(R.drawable.icon_gold_coin), contentDescription = null, modifier = Modifier.size(28.dp))
        Text(
            text = NumberFormat.getIntegerInstance().format(gold),
            fontFamily = DisplayFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
            color = Color.White
        )
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
        val density = androidx.compose.ui.platform.LocalDensity.current
        val spacingPx = with(density) { Spacing.toPx() }
        // The tree spreads from about x -2..3, y -2..1: start with its middle in the middle of the panel.
        var pan by remember { mutableStateOf(Offset(-0.5f * spacingPx, 0.5f * spacingPx)) }
        val limitX = spacingPx * 2.6f
        val limitY = spacingPx * 2.6f

        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        pan = Offset((pan.x + drag.x).coerceIn(-limitX, limitX), (pan.y + drag.y).coerceIn(-limitY, limitY))
                    }
                }
        ) {
            val cx = maxWidth / 2
            val cy = maxHeight / 2
            Box(Modifier.fillMaxSize().offset { androidx.compose.ui.unit.IntOffset(pan.x.roundToInt(), pan.y.roundToInt()) }) {
                // The paths.
                Canvas(Modifier.fillMaxSize()) {
                    fun point(x: Float, y: Float) = Offset(cx.toPx() + x * spacingPx, cy.toPx() + y * spacingPx)
                    RuneTree.all.forEach { rune ->
                        rune.requires.forEach { parentId ->
                            val parent = RuneTree.get(parentId) ?: return@forEach
                            val owned = RuneTree.levelOf(levels, rune.id) >= 1
                            val open = RuneTree.isUnlocked(levels, rune)
                            drawLine(
                                color = if (owned) Color(0xFFE08A1B) else Color(0xFF7A4A22).copy(alpha = if (open) 0.55f else 0.25f),
                                start = point(parent.x, parent.y),
                                end = point(rune.x, rune.y),
                                strokeWidth = if (owned) 9.dp.toPx() else 6.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }
                    RuneTree.placeholders.forEach { p ->
                        val from = RuneTree.get(p.requires) ?: return@forEach
                        drawLine(
                            color = Color(0xFF7A4A22).copy(alpha = 0.18f),
                            start = point(from.x, from.y),
                            end = point(p.x, p.y),
                            strokeWidth = 5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
                RuneTree.placeholders.forEach { p ->
                    Box(Modifier.offset(cx + Spacing * p.x - NodeSize / 2, cy + Spacing * p.y - NodeSize / 2)) {
                        PlaceholderNode(selected == SoonKey + p.x + p.y) { onSelect(SoonKey + p.x + p.y) }
                    }
                }
                RuneTree.all.forEach { rune ->
                    Box(Modifier.offset(cx + Spacing * rune.x - NodeSize / 2, cy + Spacing * rune.y - NodeSize / 2)) {
                        val level = RuneTree.levelOf(levels, rune.id)
                        val unlocked = RuneTree.isUnlocked(levels, rune)
                        val cost = RuneTree.nextCost(levels, rune)
                        RuneNode(
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

private fun branchColors(branch: RuneBranch): Pair<Color, Color> = when (branch) {
    RuneBranch.ROOT -> Color(0xFFFFD35E) to Color(0xFFE08A1B)
    RuneBranch.ECONOMY -> Color(0xFF6BE3A0) to Color(0xFF1E9E5A)
    RuneBranch.CHEST -> Color(0xFF7CC4FF) to Color(0xFF2A6FD4)
}

@Composable
private fun RuneNode(rune: Rune, level: Int, unlocked: Boolean, affordable: Boolean, selected: Boolean, onClick: () -> Unit) {
    val (light, dark) = branchColors(rune.branch)
    val owned = level >= 1
    val pulse = rememberInfiniteTransition(label = "runePulse")
    val glow by pulse.animateFloat(0.2f, 0.75f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "runeGlow")
    Box(
        modifier = Modifier.size(NodeSize).pressable(pressedScale = 0.92f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (affordable && !selected) {
            Box(Modifier.size(NodeSize + 12.dp).alpha(glow).background(Color(0xFFFFE066), CircleShape))
        }
        Box(
            modifier = Modifier
                .size(if (selected) NodeSize + 6.dp else NodeSize)
                .clip(CircleShape)
                .background(
                    when {
                        owned -> Brush.verticalGradient(listOf(light, dark))
                        unlocked -> Brush.verticalGradient(listOf(Color(0xFFFFF6DD), Color(0xFFE9D3A4)))
                        else -> Brush.verticalGradient(listOf(Color(0xFFA89C88), Color(0xFF7D725F)))
                    }
                )
                .border(
                    width = if (selected) 5.dp else 4.dp,
                    color = when {
                        selected -> Color.White
                        rune.maxLevel > 0 && level >= rune.maxLevel && rune.id != RuneTree.ROOT_ID -> Color(0xFFFFD35E)
                        owned -> Color(0xFFFFF3C4)
                        unlocked -> dark
                        else -> Color(0xFF5E5547)
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (unlocked || owned) rune.icon else "🔒",
                fontSize = 30.sp,
                modifier = Modifier.alpha(if (owned || unlocked && rune.id == RuneTree.ROOT_ID) 1f else if (unlocked) 0.85f else 0.55f)
            )
        }
        // Level pips under the node.
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
private fun PlaceholderNode(selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(NodeSize)
            .pressable(pressedScale = 0.94f, onClick = onClick)
            .clip(CircleShape)
            .background(Color(0x22301808))
            .border(BorderStroke(if (selected) 4.dp else 3.dp, Color(0x66503010)), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text("?", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Color(0x88503010))
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
            .heightIn(min = 150.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFFFFF6DD), Color(0xFFF3DDAA))))
            .border(BorderStroke(3.dp, Color(0xFF9A5B22)), shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.Center
    ) {
        val rune = selected?.let { RuneTree.get(it) }
        when {
            selected != null && rune == null -> {
                // A "coming soon" silhouette.
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
private fun RuneDetail(rune: Rune, levels: Map<String, Int>, gold: Int, onBuy: (String) -> Unit) {
    val level = RuneTree.levelOf(levels, rune.id)
    val unlocked = RuneTree.isUnlocked(levels, rune)
    val cost = RuneTree.nextCost(levels, rune)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(rune.icon, fontSize = 30.sp)
        Text(
            text = stringResource(rune.nameRes),
            fontFamily = DisplayFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp,
            color = Ink,
            modifier = Modifier.weight(1f, fill = true)
        )
        if (rune.id != RuneTree.ROOT_ID) {
            Text(stringResource(R.string.rune_level_of, level, rune.maxLevel), style = MaterialTheme.typography.labelLarge, color = Ink.copy(alpha = 0.8f))
        }
    }
    Spacer(Modifier.size(4.dp))
    Text(stringResource(rune.descRes, rune.percentPerLevel), style = MaterialTheme.typography.bodyMedium, color = Ink.copy(alpha = 0.9f))
    if (rune.id != RuneTree.ROOT_ID && level > 0) {
        Text(
            stringResource(R.string.rune_now_total, level * rune.percentPerLevel),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E7A45)
        )
    }
    if (rune.id == RuneTree.ROOT_ID) return
    Spacer(Modifier.size(8.dp))
    when {
        cost == null -> Text(stringResource(R.string.rune_maxed), fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E7A45))
        !unlocked -> Text(stringResource(R.string.rune_locked), style = MaterialTheme.typography.bodyMedium, color = Color(0xFFB3401A))
        else -> {
            if (gold < cost) {
                Text(
                    stringResource(R.string.rune_missing, NumberFormat.getIntegerInstance().format(cost - gold)),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFB3401A)
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
