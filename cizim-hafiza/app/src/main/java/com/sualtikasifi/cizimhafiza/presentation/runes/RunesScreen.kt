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
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.Rune
import com.sualtikasifi.cizimhafiza.domain.model.RuneBranch
import com.sualtikasifi.cizimhafiza.domain.model.AvatarFrame
import com.sualtikasifi.cizimhafiza.domain.model.PenSkin
import com.sualtikasifi.cizimhafiza.domain.model.RuneGates
import com.sualtikasifi.cizimhafiza.domain.model.RuneRewards
import com.sualtikasifi.cizimhafiza.presentation.common.nameRes
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
    private val settingsRepository: SettingsRepository,
    private val analytics: com.sualtikasifi.cizimhafiza.util.GameAnalytics,
    private val runeFunnel: com.sualtikasifi.cizimhafiza.util.RuneFunnel
) : ViewModel() {
    val gold = settingsRepository.goldBalance
    val levels = settingsRepository.runeLevels
    val xp = settingsRepository.lifetimeXp

    init {
        // A ring that has opened since the last visit is reported once (see GameAnalytics.ringOpened).
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(levels, xp) { l, x -> l to com.sualtikasifi.cizimhafiza.domain.model.PlayerLevel.levelForXp(x) }
                .collect { (l, playerLevel) ->
                    (2..4).filter { RuneTree.ringOpen(l, it, playerLevel) && settingsRepository.firstRingOpening(it) }
                        .forEach { analytics.ringOpened(it, playerLevel, l.values.sum()) }
                }
        }
    }

    /** Buys the next level of [id]; [guided] when the first-rune guide brought the player here. */
    fun buy(id: String, guided: Boolean = false): Boolean {
        val first = RuneTree.boughtLevels(settingsRepository.runeLevels.value) == 0
        val bought = settingsRepository.buyRune(id)
        if (bought) runeFunnel.bought(id, first = first, guided = guided, playerLevel = settingsRepository.playerLevelNow())
        if (bought) RuneTree.get(id)?.let { rune ->
            analytics.runeBought(
                runeId = id,
                level = RuneTree.levelOf(settingsRepository.runeLevels.value, id),
                ring = rune.ring,
                playerLevel = settingsRepository.playerLevelNow(),
                goldLeft = settingsRepository.goldBalance.value
            )
        }
        return bought
    }

    fun statusOpened() = analytics.runeStatusOpened()

    fun introSeen() = settingsRepository.runesIntroSeen
    fun markIntroSeen() { settingsRepository.runesIntroSeen = true }

    /** Today's state of the once-a-day / once-a-month abilities, for the status window. */
    fun abilityState() = RuneAbilityState(
        instantReady = settingsRepository.instantChestAvailable(),
        bestFriendReady = settingsRepository.bestFriendBonusAvailable(),
        insuranceReady = settingsRepository.streakInsuranceAvailable(),
        reserveWaiting = settingsRepository.chestReserve.value != null
    )
}

private val Spacing = 116.dp
private val Ink = Color(0xFF3A2416)

/** "Karalak Rünleri": a big pannable and zoomable map of nodes, bought with gold, each a small permanent bonus. */
@Composable
fun RunesScreen(
    onBack: () -> Unit,
    /** Opened from the mascot's first-rune offer: the map points at the rune to buy first and says so. */
    guide: Boolean = false,
    /** A rune to bring to the middle and select as the screen opens (debug previews). */
    focus: String? = null,
    viewModel: RunesViewModel = hiltViewModel()
) {
    val gold by viewModel.gold.collectAsState()
    val levels by viewModel.levels.collectAsState()
    val xp by viewModel.xp.collectAsState()
    val playerLevel = remember(xp) { com.sualtikasifi.cizimhafiza.domain.model.PlayerLevel.levelForXp(xp) }
    // The rune the guide points at, chosen once as the screen opens (Kalem Parası when the gold buys it).
    val guideId = remember { focus ?: if (guide) RuneTree.firstPick(levels, gold, playerLevel)?.id else null }
    val guiding = guideId != null && RuneTree.levelOf(levels, guideId) == 0
    var selected by remember { mutableStateOf(guideId) }
    var celebration by remember { mutableIntStateOf(0) }
    // The purchase that is playing out on the map (see RuneBurst); a new one replaces it, replaying the effects.
    var burst by remember { mutableStateOf<RuneBurst?>(null) }
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    // The player's first rune ever, just bought: a window says what it now does for them.
    var firstRune by remember { mutableStateOf<String?>(null) }
    // The centre of the map (the dog) opens the status sheet instead of being selected.
    var status by remember { mutableStateOf<RuneAbilityState?>(null) }
    // The guide explains itself; the general explanation can wait for a visit of the player's own.
    var showIntro by remember { mutableStateOf(!viewModel.introSeen() && guideId == null) }
    if (showIntro) {
        val close = { viewModel.markIntroSeen(); showIntro = false }
        com.sualtikasifi.cizimhafiza.presentation.common.PaintedFrameDialog(
            title = stringResource(R.string.rune_intro_title),
            body = stringResource(R.string.rune_intro_body),
            buttonText = stringResource(R.string.rune_intro_ok),
            onClick = close
        )
    }

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
                burst = burst,
                focusId = guideId,
                pointAt = guideId.takeIf { guiding },
                onSelect = { id ->
                    if (id == RuneTree.ROOT_ID) {
                        status = viewModel.abilityState()
                        viewModel.statusOpened()
                    }
                    else selected = if (selected == id) null else id
                },
                onClear = { selected = null },
                modifier = Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 6.dp)
            )
            androidx.compose.animation.AnimatedVisibility(
                visible = guiding && selected == guideId,
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically()
            ) {
                GuideNote(Modifier.padding(start = 10.dp, end = 10.dp, top = 4.dp))
            }
            InfoCard(
                selected = selected,
                levels = levels,
                playerLevel = playerLevel,
                gold = gold,
                onBuy = { id ->
                    val first = RuneTree.boughtLevels(levels) == 0
                    val rune = RuneTree.get(id)
                    val reached = RuneTree.levelOf(levels, id) + 1
                    if (viewModel.buy(id, guided = id == guideId)) {
                        celebration++
                        // the last level of a rune (or a one-off ability) is a bigger moment than a level in between
                        val grand = rune != null && reached >= rune.maxLevel
                        burst = RuneBurst(id, reached, grand, celebration)
                        haptics.performHapticFeedback(
                            if (grand) androidx.compose.ui.hapticfeedback.HapticFeedbackType.Confirm
                            else androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress
                        )
                        if (first) firstRune = id
                    }
                },
                modifier = Modifier.navigationBarsPadding().padding(start = 10.dp, end = 10.dp, bottom = 10.dp, top = 2.dp)
            )
        }
        if (celebration > 0) {
            val grand = burst?.grand == true
            ConfettiExplosion(seed = celebration * 7, count = if (grand) 150 else 36, durationMs = if (grand) 2300 else 1200, originY = 0.4f)
        }
        burst?.takeIf { it.grand }?.let { GrandFinish(it.token) }
        status?.let { abilities ->
            RuneStatusDialog(levels = levels, playerLevel = playerLevel, abilities = abilities, onDismiss = { status = null })
        }
        firstRune?.let { id -> RuneTree.get(id) }?.let { rune ->
            com.sualtikasifi.cizimhafiza.presentation.common.PaintedFrameDialog(
                title = stringResource(R.string.rune_first_title),
                body = stringResource(R.string.rune_first_body, stringResource(rune.nameRes)) + "\n\n" + stringResource(rune.effect.descRes, rune.perLevel),
                buttonText = stringResource(R.string.rune_first_ok),
                onClick = { firstRune = null }
            )
        }
    }
}

/** The guide's note above the rune card: this is your first rune, buy it below. */
@Composable
private fun GuideNote(modifier: Modifier = Modifier) {
    val bob = rememberInfiniteTransition(label = "guideHand")
    val dy by bob.animateFloat(-3f, 3f, infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "guideHandY")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xF2FFF4DC))
            .border(2.dp, Color(0xFFE08A1B), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("👇", fontSize = 24.sp, modifier = Modifier.offset(y = dy.dp))
        Text(
            stringResource(R.string.rune_guide_hint),
            fontFamily = DisplayFont,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            color = Ink,
            modifier = Modifier.weight(1f)
        )
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
    /** The purchase playing out on the map, if any. */
    burst: RuneBurst? = null,
    /** A rune to bring to the middle as the map opens. */
    focusId: String? = null,
    /** A rune to point at with a bobbing hand (the first-rune guide). */
    pointAt: String? = null,
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
        // The map scales around its middle, so bringing a node to the middle is moving the pan by its scaled distance.
        var pan by remember {
            mutableStateOf(focusId?.let { RuneTree.get(it) }?.let { Offset(-it.x * spacingPx * 0.78f, -it.y * spacingPx * 0.78f) } ?: Offset.Zero)
        }

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
                            burst = burst?.takeIf { it.runeId == rune.id },
                            onClick = { onSelect(rune.id) }
                        )
                    }
                    if (rune.id == pointAt) GuideHand(cx + Spacing * rune.x, cy + Spacing * rune.y + size / 2)
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
private fun RuneTile(rune: Rune, level: Int, unlocked: Boolean, sealed: Boolean, affordable: Boolean, selected: Boolean, burst: RuneBurst?, onClick: () -> Unit) {
    val owned = level >= 1
    val maxed = owned && level >= rune.maxLevel && !rune.isKeystone && rune.id != RuneTree.ROOT_ID
    val size = tileSize(rune, sealed)
    val pulse = rememberInfiniteTransition(label = "runePulse")
    val glow by pulse.animateFloat(0.35f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "runeGlow")
    // What a purchase does to the tile itself: it jumps (a rune's last level crouches first and leaps higher), the dot of
    // the level reached pops in, and the effects of [RuneBurstEffects] play round it.
    val pop = remember { androidx.compose.animation.core.Animatable(1f) }
    val pipPop = remember { androidx.compose.animation.core.Animatable(1f) }
    val progress = remember { androidx.compose.animation.core.Animatable(1f) }
    LaunchedEffect(burst?.token) {
        val b = burst ?: return@LaunchedEffect
        progress.snapTo(0f)
        pipPop.snapTo(2.6f)
        launch { pipPop.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.4f, stiffness = 240f)) }
        launch { progress.animateTo(1f, tween(if (b.grand) 1700 else 900, easing = androidx.compose.animation.core.LinearOutSlowInEasing)) }
        pop.snapTo(1f)
        if (b.grand) {
            pop.animateTo(0.8f, tween(150))
            pop.animateTo(1.5f, tween(230, easing = androidx.compose.animation.core.FastOutSlowInEasing))
            pop.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.35f, stiffness = 180f))
        } else {
            pop.animateTo(1.32f, tween(110))
            pop.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.35f, stiffness = 320f))
        }
    }
    val playing = burst != null && progress.value < 1f
    Box(
        modifier = Modifier.size(size)
            .graphicsLayer { scaleX = pop.value; scaleY = pop.value }
            .pressable(pressedScale = 0.92f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (playing && burst != null) RuneBurstEffects(burst, rune, size, progress.value, behind = true)
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
        // the last level changes the plate: the plate it had before dissolves over the new one, under a flash
        if (playing && burst != null && burst.grand && maxed) {
            Image(
                painter = painterResource(tileArt(rune, owned = true, unlocked = true, maxed = false)),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.requiredSize(size).alpha((1f - progress.value / 0.45f).coerceIn(0f, 1f))
            )
        }
        if (sealed) return@Box
        // The medallion of the plate sits a little low on the ornate ones, which carry a crown on top.
        val (sx, sy) = medallionShift(rune, owned, unlocked, maxed)
        // Measured per rune and plate, so every picture sits exactly in the middle of its medallion (see RuneIconFix).
        val (fx, fy) = if (unlocked || owned) RuneIconFix.shift(rune.id, if (owned) (if (maxed) 1 else 0) else 2) else RuneIconFix.lockShift(rune)
        val dx = sx + fx
        val dy = sy + fy
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
                modifier = Modifier.align(Alignment.BottomCenter).offset(y = 9.dp).wrapContentSize(unbounded = true),
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                repeat(rune.maxLevel) { i ->
                    Image(
                        painter = painterResource(if (i < level) R.drawable.rune_pip_full else R.drawable.rune_pip_empty),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp).graphicsLayer {
                            if (i == level - 1 && burst != null) { scaleX = pipPop.value; scaleY = pipPop.value }
                        }
                    )
                }
            }
        }
        if (playing && burst != null) RuneBurstEffects(burst, rune, size, progress.value, behind = false)
    }
}

/**
 * A rune level just bought on the map: [token] changes with every purchase so the effects replay, [grand] when it was the
 * rune's last level (or a one-off ability), which gets the bigger show.
 */
private data class RuneBurst(val runeId: String, val level: Int, val grand: Boolean, val token: Int)

/**
 * The show round a tile that was just bought, [t] being 0..1 through it. Behind the tile (a turning crown of rays, the
 * last level only); in front of it (a ring of light that widens and thins out, a second one for the last level, sparks
 * flying off and a white flash as the plate changes, and the gain, "+%8", floating up).
 */
@Composable
private fun RuneBurstEffects(burst: RuneBurst, rune: Rune, size: Dp, t: Float, behind: Boolean) {
    val (light, deep) = branchColors(rune.branch)
    val box = size * (if (burst.grand) 4.2f else 3.2f)
    if (behind) {
        if (!burst.grand) return
        androidx.compose.foundation.Canvas(Modifier.requiredSize(box)) {
            val c = center
            val reach = this.size.minDimension / 2f
            val fade = kotlin.math.sin(Math.PI * t.coerceIn(0f, 1f)).toFloat()
            val spin = t * 70f
            repeat(14) { i ->
                val a = Math.toRadians((spin + i * 360f / 14).toDouble())
                val w = Math.toRadians(5.5)
                val p = androidx.compose.ui.graphics.Path().apply {
                    moveTo(c.x, c.y)
                    lineTo(c.x + reach * kotlin.math.cos(a - w).toFloat(), c.y + reach * kotlin.math.sin(a - w).toFloat())
                    lineTo(c.x + reach * kotlin.math.cos(a + w).toFloat(), c.y + reach * kotlin.math.sin(a + w).toFloat())
                    close()
                }
                drawPath(p, brush = androidx.compose.ui.graphics.Brush.radialGradient(listOf(Color.White.copy(alpha = 0.85f * fade), light.copy(alpha = 0.55f * fade), Color.Transparent), center = c, radius = reach))
            }
        }
        return
    }
    androidx.compose.foundation.Canvas(Modifier.requiredSize(box)) {
        val c = center
        val s = size.toPx()
        // widening rings
        fun ring(tt: Float, from: Float, to: Float, thick: Float) {
            if (tt <= 0f || tt >= 1f) return
            drawCircle(light.copy(alpha = (1f - tt) * 0.95f), radius = s * (from + (to - from) * tt), center = c, style = androidx.compose.ui.graphics.drawscope.Stroke(width = s * thick * (1f - tt) + 1f))
        }
        ring(t, 0.45f, 1.3f, 0.14f)
        if (burst.grand) {
            ring((t - 0.12f) / 0.88f, 0.4f, 1.9f, 0.16f)
            ring((t - 0.28f) / 0.72f, 0.4f, 2.3f, 0.10f)
        }
        // sparks flying off
        val n = if (burst.grand) 24 else 10
        val e = 1f - (1f - t) * (1f - t)
        repeat(n) { i ->
            val a = Math.toRadians((i * 360.0 / n) + (if (burst.grand) 7.0 else 0.0) + (i % 2) * 9.0)
            val dist = s * (0.55f + (0.75f + (i % 3) * 0.28f) * e * (if (burst.grand) 1.6f else 1f))
            val r = s * (if (i % 3 == 0) 0.085f else 0.055f) * (1f - t)
            val x = c.x + dist * kotlin.math.cos(a).toFloat()
            val y = c.y + dist * kotlin.math.sin(a).toFloat()
            val spark = androidx.compose.ui.graphics.Path().apply {
                moveTo(x, y - r * 2f); lineTo(x + r * 0.7f, y - r * 0.7f); lineTo(x + r * 2f, y); lineTo(x + r * 0.7f, y + r * 0.7f)
                lineTo(x, y + r * 2f); lineTo(x - r * 0.7f, y + r * 0.7f); lineTo(x - r * 2f, y); lineTo(x - r * 0.7f, y - r * 0.7f); close()
            }
            drawPath(spark, if (i % 2 == 0) Color.White.copy(alpha = 1f - t) else deep.copy(alpha = 1f - t))
        }
        // the white flash as the plate changes
        if (burst.grand) {
            val f = (1f - kotlin.math.abs(t - 0.2f) / 0.2f).coerceIn(0f, 1f)
            if (f > 0f) drawCircle(Color.White.copy(alpha = 0.9f * f), radius = s * 0.95f, center = c)
        }
    }
    // the gain floats up above the tile
    if (rune.effect.unit != RuneUnit.FLAG && rune.perLevel > 0) {
        val label = tileText(rune.effect.unit, rune.perLevel)
        val rise = size * (0.55f + 0.9f * t)
        Text(
            text = label,
            fontFamily = DisplayFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 17.sp,
            color = Color.White,
            style = androidx.compose.ui.text.TextStyle(shadow = androidx.compose.ui.graphics.Shadow(Color(0xFF5A3A1A), androidx.compose.ui.geometry.Offset(0f, 2f), 6f)),
            modifier = Modifier
                .requiredSize(width = size * 3f, height = 26.dp)
                .offset(y = -rise)
                .alpha((1f - ((t - 0.55f) / 0.45f)).coerceIn(0f, 1f)),
            textAlign = TextAlign.Center
        )
    }
}

/** The screen's part of a rune's last level: a gold wash over it that clears, and the banner that says it is complete. */
@Composable
private fun GrandFinish(token: Int) {
    val wash = remember(token) { androidx.compose.animation.core.Animatable(0f) }
    val banner = remember(token) { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(token) {
        launch {
            wash.animateTo(1f, tween(180))
            wash.animateTo(0f, tween(900))
        }
        banner.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.5f, stiffness = 200f))
        kotlinx.coroutines.delay(1300)
        banner.animateTo(0f, tween(450))
    }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Color(0xFFFFD35E).copy(alpha = 0.38f * wash.value)))
        Box(Modifier.fillMaxSize().padding(top = 150.dp), contentAlignment = Alignment.TopCenter) {
            LetteredText(
                text = stringResource(R.string.rune_complete_banner),
                size = 34.sp,
                outline = Color(0xFF8A3A00),
                maxLines = 1,
                modifier = Modifier.graphicsLayer {
                    val b = banner.value
                    scaleX = 0.4f + 0.6f * b; scaleY = scaleX
                    alpha = b.coerceIn(0f, 1f)
                }
            )
        }
    }
}

/** A hand under the guided rune, bobbing up towards it. [x],[y] is the point just under the tile. */
@Composable
private fun GuideHand(x: Dp, y: Dp) {
    val bob = rememberInfiniteTransition(label = "mapHand")
    val dy by bob.animateFloat(0f, -8f, infiniteRepeatable(tween(520), RepeatMode.Reverse), label = "mapHandY")
    Box(Modifier.offset(x - 22.dp, y + 10.dp + dy.dp).size(44.dp), contentAlignment = Alignment.Center) {
        Text("👆", fontSize = 34.sp)
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
                Spacer(Modifier.size(6.dp))
                Text(
                    text = stringResource(R.string.rune_status_hint),
                    style = MaterialTheme.typography.labelMedium,
                    fontStyle = FontStyle.Italic,
                    color = Color(0xFF8A5A2B),
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
    RuneUnit.COUNT -> stringResource(if (value == 1) R.string.rune_tile_cnt_one else R.string.rune_tile_cnt, value)
    RuneUnit.FLAG -> stringResource(R.string.rune_tile_on)
}

@Composable
private fun RuneDetail(rune: Rune, levels: Map<String, Int>, playerLevel: Int, gold: Int, onBuy: (String) -> Unit) {
    val level = RuneTree.levelOf(levels, rune.id)
    val unlocked = RuneTree.isUnlocked(levels, rune)
    val cost = RuneTree.nextCost(levels, rune)
    val context = LocalContext.current
    if (!unlocked && level < 1) {
        // A rune whose neighbours are not bought yet is still a secret: nothing of it is shown.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(painterResource(R.drawable.rune_soon), null, contentScale = ContentScale.Fit, modifier = Modifier.size(66.dp).alpha(0.85f))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.rune_fog_title), fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp, color = Ink)
                Text(stringResource(R.string.rune_fog_body), style = MaterialTheme.typography.bodyMedium, color = Ink.copy(alpha = 0.85f))
            }
        }
        return
    }
    if (unlocked && level < 1 && !RuneTree.ringOpen(levels, rune.ring, playerLevel)) {
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
        val owned = level >= 1
        val maxed = owned && level >= rune.maxLevel && !rune.isKeystone && rune.id != RuneTree.ROOT_ID
        Box(Modifier.size(66.dp), contentAlignment = Alignment.Center) {
            Image(painterResource(tileArt(rune, owned, true, maxed)), null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
            // The same measured centring as on the map, so the picture sits in the middle of the medallion here too.
            val (sx, sy) = medallionShift(rune, owned, true, maxed)
            val (fx, fy) = RuneIconFix.shift(rune.id, if (owned) (if (maxed) 1 else 0) else 2)
            RuneIcon(rune.effect.iconKey, rune.effect.emoji, 66.dp * medallionIcon(rune, owned), Modifier.offset(x = 66.dp * (sx + fx), y = 66.dp * (sy + fy)), runeId = rune.id)
        }
        Column(Modifier.weight(1f)) {
            // The level sits beside the name, so the description below gets the card's whole width.
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(rune.nameRes),
                    fontFamily = DisplayFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 21.sp,
                    color = Ink,
                    modifier = Modifier.weight(1f)
                )
                if (rune.id != RuneTree.ROOT_ID) {
                    Text(
                        text = stringResource(R.string.rune_level_of, level, rune.maxLevel),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0xFF7A4A22)).padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
            // A one-line bit of flavour under the name (rune_<id>_flavor), when the rune has one.
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
            val giftName = RuneRewards.storeItem[rune.id]?.let { item ->
                when {
                    item.startsWith("pen:") -> runCatching { PenSkin.valueOf(item.removePrefix("pen:")).labelRes }.getOrNull()
                    item.startsWith("frame:") -> runCatching { AvatarFrame.valueOf(item.removePrefix("frame:")).nameRes() }.getOrNull()
                    else -> null
                }
            }
            if (giftName != null) {
                Text(
                    text = stringResource(R.string.rune_gift_line, stringResource(giftName)),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7A4A22)
                )
            }
        }
    }
    if (rune.id == RuneTree.ROOT_ID) return
    // One box per level: what that level brings in total, the reached ones lit. A one-off ability has no boxes, a
    // rune with many levels (the Legend ring) one box for where it stands now and what the next level brings.
    Spacer(Modifier.size(8.dp))
    val showBoxes = !(rune.effect.unit == RuneUnit.FLAG && rune.maxLevel == 1)
    val longRune = rune.maxLevel > 5
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
                    .padding(vertical = 8.dp, horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(painterResource(if (reached) R.drawable.rune_pip_full else R.drawable.rune_pip_empty), null, modifier = Modifier.size(14.dp))
                Text(stringResource(R.string.rune_level_n, n), style = MaterialTheme.typography.labelSmall, fontSize = if (numbers.size >= 5) 9.sp else 11.sp, maxLines = 1, softWrap = false, color = Ink.copy(alpha = 0.8f))
                // One line always: a longer value ("+3 joker") is set a little smaller instead of wrapping.
                val value = tileText(rune.effect.unit, rune.perLevel * n)
                Text(
                    value,
                    fontFamily = DisplayFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = (if (numbers.size >= 5) 15f else 18f).let { if (value.length > 5) it - 3f else it }.sp,
                    maxLines = 1,
                    softWrap = false,
                    color = if (reached) Ink else if (isNext) Ink.copy(alpha = 0.85f) else Color.White
                )
            }
        }
    }
    Spacer(Modifier.size(8.dp))
    when {
        cost == null -> Text(stringResource(R.string.rune_maxed), fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E7A45), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        !unlocked -> Text(stringResource(R.string.rune_locked), style = MaterialTheme.typography.bodyMedium, color = Color(0xFFB3401A), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        // Already owned in a ring that is closed again (the level dropped after an XP penalty): what it gives stays, the next level waits.
        !RuneTree.ringOpen(levels, rune.ring, playerLevel) -> Text(stringResource(R.string.rune_status_ring_locked, RuneGates.levelFor(rune.ring)), style = MaterialTheme.typography.bodyMedium, color = Color(0xFFB3401A), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
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
internal fun Modifier.sliced(@DrawableRes res: Int, srcCorner: Int, dstCorner: Dp): Modifier {
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
