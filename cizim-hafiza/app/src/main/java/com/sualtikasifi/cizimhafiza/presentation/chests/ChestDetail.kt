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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.sualtikasifi.cizimhafiza.presentation.common.LetteredText
import com.sualtikasifi.cizimhafiza.presentation.common.NinePatch
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
import androidx.compose.material3.LinearProgressIndicator
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
import com.sualtikasifi.cizimhafiza.domain.model.Chest
import com.sualtikasifi.cizimhafiza.domain.model.ChestTier
import com.sualtikasifi.cizimhafiza.presentation.common.AppWindowDialog
import com.sualtikasifi.cizimhafiza.presentation.common.PrimaryButton
import com.sualtikasifi.cizimhafiza.presentation.common.labelRes
import com.sualtikasifi.cizimhafiza.presentation.theme.AppTheme
import com.sualtikasifi.cizimhafiza.presentation.theme.DisplayFont

/** A short answer the chest "says" — [tick] changes on every message so repeating one still replays the animation. */
private data class Bubble(val res: Int, val tick: Int)

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
            onDismiss = onDismiss
        )
    }

    lastReward?.let { reward ->
        ChestOpeningDialog(reward = reward, onDismiss = viewModel::consumeLastReward)
    }
}

@Composable
private fun ChestDetailDialog(
    chest: Chest,
    nowMillis: Long,
    anotherUnlocking: Boolean,
    speedupAvailable: Boolean,
    bubble: Bubble?,
    onBubbleDone: () -> Unit,
    onTapChest: () -> Unit,
    onStart: () -> Unit,
    onOpen: () -> Unit,
    onSpeedup: () -> Unit,
    onDismiss: () -> Unit
) {
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
    // The design's window: a wooden frame with vines, the dog and the cat peeking over its sign (the chest's name lettered
    // on it), a painted close button, the chest on its stump, what it can hold, the timer and the one button that fits.
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        androidx.compose.foundation.layout.BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            val w = minOf(maxWidth, 430.dp)
            val maxH = maxHeight
            val headW = w * 0.96f
            val headH = headW * 340f / 540f
            // How far down the head picture the plank's middle is: the frame starts there, so the sign overlaps its edge.
            val frameTop = headH * 0.70f
            val sign = Color(0xFFFFE08A)
            Box(
                modifier = Modifier
                    .width(w)
                    .heightIn(max = maxH)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
            ) {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = maxH)) {
                    Spacer(Modifier.height(frameTop))
                    NinePatch(
                        res = R.drawable.cd_frame,
                        slicePx = 96,
                        edge = 40.dp,
                        modifier = Modifier.fillMaxWidth().weight(1f, fill = false)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(start = 30.dp, end = 30.dp, top = (headH - frameTop) + 6.dp, bottom = 30.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ChestStage(tier = tier, bubble = bubble, wobble = wiggle.value, onTapChest = onTapChest)
                            Spacer(modifier = Modifier.height(10.dp))
                            ChestContentsPanel(tier = tier)
                            if (!ready) {
                                Spacer(modifier = Modifier.height(10.dp))
                                ChestTimerPlate(chest = chest, nowMillis = nowMillis, unlocking = unlocking)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            when {
                                ready -> {
                                    LetteredText(stringResource(R.string.chest_detail_ready), 20.sp, fill = Color(0xFFB8F5A0), outline = Color(0xFF1B5E20), modifier = Modifier.fillMaxWidth())
                                    Spacer(modifier = Modifier.height(6.dp))
                                    PillSprite(R.drawable.cd_btn_gr, 334f / 103f, stringResource(R.string.chests_open_button), Color(0xFF1B5E20), onOpen)
                                }
                                unlocking -> {
                                    if (speedupAvailable) {
                                        PillSprite(
                                            R.drawable.cd_btn_gr, 334f / 103f,
                                            stringResource(R.string.chest_speedup_button) + "  " + stringResource(R.string.chest_speedup_badge),
                                            Color(0xFF1B5E20), onSpeedup
                                        )
                                    } else {
                                        NoteStrip(stringResource(R.string.chest_speedup_used_today))
                                    }
                                }
                                anotherUnlocking -> NoteStrip(stringResource(R.string.chest_slot_busy))
                                else -> PillSprite(
                                    R.drawable.cd_btn_or, 336f / 98f,
                                    stringResource(R.string.chests_start_button) + " · " + stringResource(R.string.chest_duration_hours, tier.durationHours()),
                                    Color(0xFF8A3A00), onStart
                                )
                            }
                        }
                    }
                }
                Image(
                    painter = painterResource(R.drawable.cd_head),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.align(Alignment.TopCenter).width(headW).height(headH)
                )
                // The name on the sign: the plank is the lower part of the head picture.
                LetteredText(
                    text = stringResource(tier.labelRes()),
                    size = (headW.value * 0.095f).sp,
                    fill = sign,
                    outline = Color(0xFF5A2815),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = headH * 0.55f)
                        .width(headW * 0.66f)
                        .height(headH * 0.2f),
                    minScale = 0.5f,
                    title = true
                )
                Image(
                    painter = painterResource(R.drawable.cd_close),
                    contentDescription = stringResource(R.string.close),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 4.dp, end = 0.dp)
                        .size(58.dp)
                        .pressable(pressedScale = 0.9f, onClick = onDismiss)
                )
            }
        }
    }
}

/** A painted pill button (the picture with its leaves) with lettering on it. */
@Composable
private fun PillSprite(res: Int, aspect: Float, text: String, outline: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .aspectRatio(aspect)
            .pressable(pressedScale = 0.95f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(painter = painterResource(res), contentDescription = null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
        LetteredText(text, 22.sp, outline = outline, modifier = Modifier.fillMaxWidth(0.62f), minScale = 0.5f)
    }
}

/** The dashed note strip at the bottom of the design ("another chest is opening — wait for it"). */
@Composable
private fun NoteStrip(text: String) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xFFFBEFD6))
            .border(1.5.dp, Color(0xFFD9BC8C), shape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("🎁", fontSize = 18.sp)
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF4A3426),
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * What a chest of this tier can hold, as a small titled panel with one row per
 * kind of prize and the tier's rarity as a chip — in place of the single
 * grey run-on line this used to be. The percentages mirror ChestLoot (the
 * numbers that actually decide the drop); keep the two in step.
 */
@Composable
private fun ChestContentsPanel(tier: ChestTier) {
    val stars = tier.rarityStars()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFFFFF8E8), Color(0xFFFCEACB))))
            .border(1.5.dp, Color(0xFFE5C48F), RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.chest_detail_contents_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF3A2416),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "★".repeat(stars) + "☆".repeat(3 - stars) + " " + stringResource(tier.rarityLabelRes()),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = tier.accent(),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(tier.accent().copy(alpha = 0.16f))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            )
        }
        ContentsRow(emoji = "🪙", text = stringResource(R.string.chest_detail_gold, tier.goldReward.first, tier.goldReward.last))
        ContentsRow(
            emoji = "🎲",
            text = when (tier) {
                ChestTier.SILVER -> stringResource(R.string.chest_detail_jokers_silver, 30)
                ChestTier.GOLD -> stringResource(R.string.chest_detail_jokers_gold, 30)
                ChestTier.RARE -> stringResource(R.string.chest_detail_jokers_rare, 40)
            }
        )
        if (tier == ChestTier.RARE) {
            ContentsRow(emoji = "✏️", text = stringResource(R.string.chest_detail_pen, 12))
        }
    }
}

@Composable
private fun ContentsRow(emoji: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(Color(0xFFF7E6C6))
            .padding(horizontal = 4.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier.size(30.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color.White).border(1.5.dp, Color(0xFFE5C48F), androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center
        ) { Text(text = emoji, fontSize = 15.sp) }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF4A3426)
        )
    }
}

/**
 * The countdown on a dark plate with a gold rim — big amber digits, a label
 * above and, once it is counting, a gold progress bar inside the same plate.
 * Before the unlock has been started the same plate shows the total time, so
 * the two states look like one object rather than a bare number appearing.
 */
@Composable
private fun ChestTimerPlate(chest: Chest, nowMillis: Long, unlocking: Boolean) {
    val shape = RoundedCornerShape(20.dp)
    val amber = Color(0xFFFFC94D)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF3B2617), Color(0xFF1C1109))))
            .border(2.dp, amber.copy(alpha = 0.85f), shape)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Filled.Schedule, contentDescription = null, tint = amber, modifier = Modifier.size(16.dp))
            Text(
                text = stringResource(if (unlocking) R.string.chest_timer_remaining else R.string.chest_timer_total).uppercase(androidx.compose.ui.text.intl.Locale.current.platformLocale),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp,
                color = amber.copy(alpha = 0.9f)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = formatCountdown(if (unlocking) chest.remainingMillis(nowMillis) else chest.tier.unlockDurationMillis),
            textAlign = TextAlign.Center,
            fontFamily = DisplayFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 40.sp,
            color = Color(0xFFFFE08A)
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (unlocking) {
            LinearProgressIndicator(
                progress = { chest.unlockProgress(nowMillis) },
                color = amber,
                trackColor = Color.White.copy(alpha = 0.16f),
                modifier = Modifier.fillMaxWidth().height(9.dp).clip(RoundedCornerShape(5.dp))
            )
        } else {
            Text(
                text = stringResource(R.string.chest_timer_hint),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
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


/** The tier's own backdrop with the chest on its stump, tappable, with the chest's answer as a speech bubble above. */
@Composable
private fun ChestStage(tier: ChestTier, bubble: Bubble?, wobble: Float, onTapChest: () -> Unit) {
    // Scales with the screen so a short phone still has room for the timer and the action button.
    val stageHeight = (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp * 0.25f).coerceIn(150f, 230f)
    val chestWidth = stageHeight * 0.74f
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(stageHeight.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(2.dp, tier.borderColor(), RoundedCornerShape(18.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onTapChest),
        contentAlignment = Alignment.BottomCenter
    ) {
        ChestBackdrop(tier = tier, modifier = Modifier.matchParentSize())
        Image(
            painter = painterResource(R.drawable.cd_stump),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth(1.02f).offset(y = 6.dp)
        )
        Box(
            modifier = Modifier
                .padding(bottom = (stageHeight * 0.15f).dp)
                .graphicsLayer { rotationZ = wobble * 6f }
        ) {
            ChestImage(tier = tier, width = chestWidth.dp)
        }
        AnimatedVisibility(
            visible = bubble != null,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp, start = 16.dp, end = 16.dp),
            enter = fadeIn(tween(120)) + scaleIn(initialScale = 0.7f),
            exit = fadeOut(tween(160)) + scaleOut(targetScale = 0.9f)
        ) {
            val res = bubble?.res
            if (res != null) SpeechBubble(text = stringResource(res))
        }
    }
}
