package com.sualtikasifi.cizimhafiza.presentation.chests

import com.sualtikasifi.cizimhafiza.presentation.common.pressable

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sualtikasifi.cizimhafiza.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sualtikasifi.cizimhafiza.presentation.common.BackdropCache
import com.sualtikasifi.cizimhafiza.presentation.common.FitText
import com.sualtikasifi.cizimhafiza.presentation.common.LetteredText
import com.sualtikasifi.cizimhafiza.presentation.mainmenu.rememberSink
import com.sualtikasifi.cizimhafiza.presentation.mainmenu.sinkWith
import com.sualtikasifi.cizimhafiza.presentation.mainmenu.sunkenArt
import com.sualtikasifi.cizimhafiza.domain.model.Chest
import com.sualtikasifi.cizimhafiza.domain.model.ChestTier
import com.sualtikasifi.cizimhafiza.presentation.common.labelRes
import com.sualtikasifi.cizimhafiza.presentation.theme.DisplayFont

/** A short answer the chest "says" — [tick] changes on every message so repeating one still replays the animation. */
internal data class Bubble(val res: Int, val tick: Int)

/**
 * The chest window shared by the home screen and My Chests: tap a chest and
 * this opens right on top instead of navigating anywhere. It also plays the
 * opening scene when a chest is opened, so the caller only has to say which
 * chest was tapped ([selectedId]) and how to close the window.
 */
@Composable
fun ChestDetailHost(selectedId: String?, onDismiss: () -> Unit, viewModel: ChestsViewModel) {
    val slots by viewModel.chestSlots.collectAsState()
    val now by viewModel.nowMillis.collectAsState()
    val speedupAvailable by viewModel.speedupAvailable.collectAsState()
    val instantAvailable by viewModel.instantAvailable.collectAsState()
    val lastReward by viewModel.lastReward.collectAsState()
    val activity = LocalContext.current as? Activity
    var bubble by remember { mutableStateOf<Bubble?>(null) }
    var tick by remember { mutableIntStateOf(0) }
    fun say(res: Int) { tick++; bubble = Bubble(res, tick) }

    val chest = slots.firstOrNull { it != null && it.id == selectedId }
    // Opened (slot emptied) or gone: nothing left to show.
    LaunchedEffect(chest == null, selectedId) {
        if (selectedId != null && chest == null) onDismiss()
    }
    val anyUnlocking = slots.any { it?.unlockStartedAtMillis != null && !it.isReady(now) }

    if (chest != null) {
        ChestDetailDialog(
            chest = chest,
            nowMillis = now,
            anotherUnlocking = anyUnlocking && chest.unlockStartedAtMillis == null,
            speedupAvailable = speedupAvailable,
            instantAvailable = instantAvailable,
            bubble = bubble,
            onBubbleDone = { bubble = null },
            onTapChest = {
                when {
                    chest.isReady(now) -> Unit
                    chest.unlockStartedAtMillis != null -> say(R.string.chest_slot_still_unlocking)
                    anyUnlocking -> say(R.string.chest_slot_busy)
                    else -> say(R.string.chest_detail_tap_to_start)
                }
            },
            onStart = { viewModel.startUnlocking(chest.id) },
            onOpen = { viewModel.open(chest.id) },
            onSpeedup = { activity?.let { viewModel.speedUp(it) { res -> say(res) } } },
            onInstant = { viewModel.instantOpen(chest.id) },
            onDismiss = onDismiss
        )
    }

    lastReward?.let { reward ->
        ChestOpeningDialog(reward = reward, onDismiss = viewModel::consumeLastReward)
    }
}

// chest_card is 793 x 1390; the rectangles are x0, y0, x1, y1 in the picture's own pixels. The picture has the title
// banner, the lit stage, the parchment for the prize list, the dark plate for the timer and the orange button painted in.
private const val CardW = 793f
private const val CardH = 1390f
private val TitleRect = floatArrayOf(150f, 108f, 620f, 228f)
private val StageRect = floatArrayOf(92f, 258f, 702f, 704f)
private val ContentsRect = floatArrayOf(96f, 772f, 698f, 940f)
private val TimerRect = floatArrayOf(108f, 1012f, 688f, 1174f)
private val ButtonRect = floatArrayOf(104f, 1224f, 690f, 1346f)
private val CloseRect = floatArrayOf(662f, 28f, 786f, 142f)

private val ChestInk = Color(0xFF3A2416)
private val ChestButtonOutline = Color(0xFF8A3A00)
private val ChestAmber = Color(0xFFFFC94D)

/** One action of the chest window: what it says, what it does, and whether it is an ad. */
private class ChestAction(
    val label: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val ad: Boolean = false,
    val badge: String? = null,
    val veiled: Boolean = false
)

@Composable
internal fun ChestDetailDialog(
    chest: Chest,
    nowMillis: Long,
    anotherUnlocking: Boolean,
    speedupAvailable: Boolean,
    instantAvailable: Boolean,
    bubble: Bubble?,
    onBubbleDone: () -> Unit,
    onTapChest: () -> Unit,
    onStart: () -> Unit,
    onOpen: () -> Unit,
    onSpeedup: () -> Unit,
    onInstant: () -> Unit,
    onDismiss: () -> Unit
) {
    val isOnline = com.sualtikasifi.cizimhafiza.presentation.common.LocalOnline.current
    val tier = chest.tier
    val ready = chest.isReady(nowMillis)
    val unlocking = chest.unlockStartedAtMillis != null && !ready

    // The answer disappears by itself, and the chest gives a little shake each time it speaks.
    val wiggle = remember { Animatable(0f) }
    LaunchedEffect(bubble) {
        if (bubble != null) {
            wiggle.snapTo(0f)
            repeat(3) {
                wiggle.animateTo(1f, tween(70))
                wiggle.animateTo(-1f, tween(70))
            }
            wiggle.animateTo(0f, tween(70))
            kotlinx.coroutines.delay(1_900)
            onBubbleDone()
        }
    }

    // What the window offers: one main button on the picture, and (while unlocking with both on offer) a second one
    // under the card.
    val speedupClick = com.sualtikasifi.cizimhafiza.presentation.common.adOnly(onSpeedup)
    val openLabel = stringResource(R.string.chests_open_button)
    val instantLabel = stringResource(R.string.chest_instant_button)
    val speedupLabel = stringResource(R.string.chest_speedup_button)
    val speedupBadge = stringResource(R.string.chest_speedup_badge)
    val usedTodayLabel = stringResource(R.string.chest_speedup_used_today)
    val busyLabel = stringResource(R.string.chest_slot_busy)
    val startLabel = stringResource(R.string.chests_start_button) + " · " + tier.durationText()
    val actions: List<ChestAction> = when {
        ready -> listOf(ChestAction(openLabel, onOpen))
        unlocking -> buildList {
            if (instantAvailable) add(ChestAction(instantLabel, onInstant))
            if (speedupAvailable) add(ChestAction(speedupLabel, speedupClick, ad = true, badge = speedupBadge, veiled = !isOnline))
            if (isEmpty()) add(ChestAction(usedTodayLabel, {}, enabled = false))
        }
        anotherUnlocking -> listOf(ChestAction(busyLabel, {}, enabled = false))
        else -> listOf(ChestAction(startLabel, onStart))
    }
    val main = actions[0]
    val second = actions.getOrNull(1)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xB31A0E05))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            val reserve = if (second != null) 66.dp else 0.dp
            val frameW = minOf(maxWidth - 24.dp, (maxHeight - 24.dp - reserve) * (CardW / CardH), 440.dp)
            val u = frameW / CardW
            val fontScale0 = LocalDensity.current.fontScale
            val fs: (Float) -> TextUnit = { px -> (px * u.value / fontScale0).sp }
            val context = LocalContext.current
            val sheet = remember { runCatching { BackdropCache.get(context.resources, R.drawable.chest_card) }.getOrNull() }
            val pxK = (sheet?.width ?: CardW.toInt()) / CardW

            fun Modifier.at(r: FloatArray) = offset(u * r[0], u * r[1]).size(u * (r[2] - r[0]), u * (r[3] - r[1]))

            val enter = remember { Animatable(0f) }
            LaunchedEffect(Unit) { enter.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.6f, stiffness = 380f)) }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    val e = enter.value
                    scaleX = 0.85f + 0.15f * e
                    scaleY = 0.85f + 0.15f * e
                    alpha = (e * 3f).coerceIn(0f, 1f)
                }
            ) {
                Box(
                    Modifier
                        .size(frameW, frameW * (CardH / CardW))
                        // Taps on the card itself must not reach the dim area behind it, which closes the window.
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                ) {
                    Image(
                        painter = painterResource(R.drawable.chest_card),
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.fillMaxSize()
                    )
                    FitText(
                        text = stringResource(tier.labelRes()),
                        style = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = fs(68f), color = ChestInk, textAlign = TextAlign.Center),
                        maxLines = 1, minScale = 0.5f,
                        modifier = Modifier.at(TitleRect)
                    )
                    ChestStage(tier = tier, bubble = bubble, wobble = wiggle.value, u = u, fs = fs, onTapChest = onTapChest, modifier = Modifier.at(StageRect))
                    ChestContentsPanel(tier = tier, u = u, fs = fs, modifier = Modifier.at(ContentsRect))
                    ChestTimerPanel(chest = chest, nowMillis = nowMillis, ready = ready, unlocking = unlocking, u = u, fs = fs, modifier = Modifier.at(TimerRect))

                    // The main button is painted into the card, so it sinks the way the home screen's painted buttons do.
                    val (x0, y0, x1, y1) = ButtonRect
                    val sink = rememberSink(0.94f)
                    Box(
                        modifier = Modifier
                            .at(ButtonRect)
                            .sunkenArt(sink, sheet, x0 * pxK, y0 * pxK, (x1 - x0) * pxK, (y1 - y0) * pxK, u * ((y1 - y0) / 2f))
                            .clickable(interactionSource = sink.source, indication = null, enabled = main.enabled, onClick = main.onClick)
                    ) {
                        if (!main.enabled || main.veiled) {
                            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(50)).background(Color(if (main.enabled) 0x66301A08 else 0x99301A08)))
                        }
                        Box(Modifier.fillMaxSize().sinkWith(sink, 0.5f, 0.5f), contentAlignment = Alignment.Center) {
                            ChestButtonFace(action = main, u = u, fs = fs)
                        }
                    }

                    Image(
                        painter = painterResource(R.drawable.chest_card_close),
                        contentDescription = stringResource(R.string.close),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .at(CloseRect)
                            .pressable(pressedScale = 0.9f, onClick = onDismiss)
                    )
                }
                if (second != null) {
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .width(frameW * 0.88f)
                            .height(frameW * 0.88f * (173f / 689f))
                            .graphicsLayer { alpha = if (second.enabled && !second.veiled) 1f else 0.6f }
                            .pressable(enabled = second.enabled, pressedScale = 0.94f, onClick = second.onClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(painter = painterResource(R.drawable.chest_card_btn), contentDescription = null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
                        ChestButtonFace(action = second, u = u, fs = fs)
                    }
                }
            }
        }
    }
}

/** The lettering on a button: the label, with the ad's little play icon and "−2 sa" chip when it is one. */
@Composable
private fun ChestButtonFace(action: ChestAction, u: Dp, fs: (Float) -> TextUnit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = u * 56f)
    ) {
        if (action.ad) {
            Icon(Icons.Filled.PlayCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(u * 54f))
            Spacer(Modifier.width(u * 12f))
        }
        LetteredText(
            text = action.label,
            size = fs(58f),
            outline = ChestButtonOutline,
            maxLines = if (action.enabled) 1 else 2,
            minScale = 0.5f,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (action.badge != null) {
            Spacer(Modifier.width(u * 12f))
            Box(Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFFFE066)).padding(horizontal = u * 16f, vertical = u * 5f)) {
                Text(text = action.badge, style = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = fs(32f), color = ChestInk), maxLines = 1)
            }
        }
    }
}

/**
 * What a chest of this tier can hold, written on the card's parchment: a title, the tier's rarity as a chip and one row
 * per kind of prize. The percentages mirror ChestLoot (the numbers that actually decide the drop); keep the two in step.
 */
@Composable
private fun ChestContentsPanel(tier: ChestTier, u: Dp, fs: (Float) -> TextUnit, modifier: Modifier) {
    val stars = tier.rarityStars()
    Column(
        modifier = modifier.padding(horizontal = u * 14f, vertical = u * 6f),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.chest_detail_contents_title),
                style = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = fs(36f), color = ChestInk),
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "★".repeat(stars) + "☆".repeat(3 - stars) + " " + stringResource(tier.rarityLabelRes()),
                style = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = fs(28f), color = tier.accent()),
                maxLines = 1,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(tier.accent().copy(alpha = 0.16f))
                    .padding(horizontal = u * 18f, vertical = u * 4f)
            )
        }
        ContentsRow(emoji = "🪙", text = stringResource(R.string.chest_detail_gold, tier.goldReward.first, tier.goldReward.last), u = u, fs = fs)
        ContentsRow(
            emoji = "🎲",
            text = when (tier) {
                ChestTier.SILVER -> stringResource(R.string.chest_detail_jokers_silver, 30)
                ChestTier.GOLD -> stringResource(R.string.chest_detail_jokers_gold, 30)
                ChestTier.RARE -> stringResource(R.string.chest_detail_jokers_rare, 40)
            },
            u = u, fs = fs
        )
        if (tier == ChestTier.RARE) {
            ContentsRow(emoji = "✏️", text = stringResource(R.string.chest_detail_pen, 12), u = u, fs = fs)
        }
    }
}

@Composable
private fun ContentsRow(emoji: String, text: String, u: Dp, fs: (Float) -> TextUnit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(u * 16f)) {
        Box(
            modifier = Modifier.size(u * 38f).clip(androidx.compose.foundation.shape.CircleShape).background(Color.White.copy(alpha = 0.8f)),
            contentAlignment = Alignment.Center
        ) { Text(text = emoji, fontSize = fs(24f)) }
        FitText(
            text = text,
            style = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = fs(30f), color = Color(0xFF4A3426)),
            maxLines = 1, minScale = 0.6f,
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * The countdown on the card's dark plate: a label, big amber digits and, once it is counting, a gold progress bar. Before
 * the unlock has been started the same plate shows the total time; a ready chest just says so.
 */
@Composable
private fun ChestTimerPanel(chest: Chest, nowMillis: Long, ready: Boolean, unlocking: Boolean, u: Dp, fs: (Float) -> TextUnit, modifier: Modifier) {
    Column(
        modifier = modifier.padding(horizontal = u * 20f, vertical = u * 8f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        if (ready) {
            LetteredText(
                text = stringResource(R.string.chest_detail_ready),
                size = fs(72f),
                fill = Color(0xFFFFE08A),
                outline = Color(0xFF2A1608),
                maxLines = 1, minScale = 0.5f,
                modifier = Modifier.fillMaxWidth()
            )
            return@Column
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(u * 10f)) {
            Icon(Icons.Filled.Schedule, contentDescription = null, tint = ChestAmber, modifier = Modifier.size(u * 30f))
            Text(
                text = stringResource(if (unlocking) R.string.chest_timer_remaining else R.string.chest_timer_total).uppercase(androidx.compose.ui.text.intl.Locale.current.platformLocale),
                style = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = fs(26f), letterSpacing = 1.2.sp, color = ChestAmber.copy(alpha = 0.9f)),
                maxLines = 1
            )
        }
        Text(
            text = formatCountdown(chest.remainingMillis(nowMillis)),
            textAlign = TextAlign.Center,
            style = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = fs(86f), color = Color(0xFFFFE08A)),
            maxLines = 1
        )
        if (unlocking) {
            Box(Modifier.fillMaxWidth().height(u * 16f).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.16f))) {
                Box(Modifier.fillMaxWidth(chest.unlockProgress(nowMillis).coerceIn(0f, 1f)).height(u * 16f).clip(RoundedCornerShape(50)).background(ChestAmber))
            }
        } else {
            Text(
                text = stringResource(R.string.chest_timer_hint),
                style = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = fs(26f), color = Color.White.copy(alpha = 0.75f)),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

/** A white speech bubble with a small tail pointing down at the chest. */
@Composable
private fun SpeechBubble(text: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(2.dp, Color(0xFF3A2416).copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3A2416),
                textAlign = TextAlign.Center
            )
        }
        Canvas(modifier = Modifier.size(width = 22.dp, height = 11.dp)) {
            val tail = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width / 2f, size.height)
                close()
            }
            drawPath(tail, Color.White)
            drawLine(Color(0xFF3A2416).copy(alpha = 0.25f), Offset(0f, 0f), Offset(size.width / 2f, size.height), strokeWidth = 2.dp.toPx())
            drawLine(Color(0xFF3A2416).copy(alpha = 0.25f), Offset(size.width, 0f), Offset(size.width / 2f, size.height), strokeWidth = 2.dp.toPx())
        }
    }
}


/** The card's lit stage with the chest on it, tappable, with the chest's answer as a speech bubble above. */
@Composable
private fun ChestStage(tier: ChestTier, bubble: Bubble?, wobble: Float, u: Dp, fs: (Float) -> TextUnit, onTapChest: () -> Unit, modifier: Modifier) {
    Box(
        modifier = modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onTapChest),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .padding(bottom = u * 36f)
                .graphicsLayer { rotationZ = wobble * 6f }
        ) {
            ChestImage(tier = tier, width = u * 400f)
        }
        AnimatedVisibility(
            visible = bubble != null,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = u * 14f, start = u * 16f, end = u * 16f),
            enter = fadeIn(tween(120)) + scaleIn(initialScale = 0.7f),
            exit = fadeOut(tween(160)) + scaleOut(targetScale = 0.9f)
        ) {
            val res = bubble?.res
            if (res != null) SpeechBubble(text = stringResource(res))
        }
    }
}
