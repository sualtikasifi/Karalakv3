package com.sualtikasifi.cizimhafiza.presentation.chests

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
    AppWindowDialog(
        title = stringResource(tier.labelRes()),
        onDismiss = onDismiss,
        footer = {
            when {
                ready -> {
                    Text(
                        text = stringResource(R.string.chest_detail_ready),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = AppTheme.tokens.success
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    PrimaryButton(text = stringResource(R.string.chests_open_button), onClick = onOpen, modifier = Modifier.fillMaxWidth())
                }
    
                unlocking -> {
                    if (speedupAvailable) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF2E8B45))
                                .clickable(onClick = onSpeedup)
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Filled.PlayCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = stringResource(R.string.chest_speedup_button),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFFFE066)).padding(horizontal = 9.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.chest_speedup_badge),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF3A2416)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.chest_speedup_used_today),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
    
                anotherUnlocking -> Text(
                    text = stringResource(R.string.chest_slot_busy),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
    
                else -> PrimaryButton(
                    text = stringResource(R.string.chests_start_button) + " · " + stringResource(R.string.chest_duration_hours, tier.durationHours()),
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) {
        ChestStage(tier = tier, bubble = bubble, wobble = wiggle.value, onTapChest = onTapChest)

        Spacer(modifier = Modifier.height(14.dp))

        ChestContentsPanel(tier = tier)

        if (!ready) {
            Spacer(modifier = Modifier.height(12.dp))
            ChestTimerPlate(chest = chest, nowMillis = nowMillis, unlocking = unlocking)
        }
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
            .background(tier.accent().copy(alpha = 0.10f))
            .border(1.5.dp, tier.accent().copy(alpha = 0.45f), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
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
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier.size(28.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color.White.copy(alpha = 0.85f)),
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


/** The tier's own backdrop with the chest on it, tappable, with the chest's answer as a speech bubble above. */
@Composable
private fun ChestStage(tier: ChestTier, bubble: Bubble?, wobble: Float, onTapChest: () -> Unit) {
    // Scales with the screen so a short phone still has room for the timer and the action button.
    val stageHeight = (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp * 0.30f).coerceIn(170f, 282f)
    val chestWidth = stageHeight * 0.84f
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(stageHeight.dp)
            .clip(RoundedCornerShape(22.dp))
            .border(3.dp, tier.borderColor(), RoundedCornerShape(22.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onTapChest),
        contentAlignment = Alignment.Center
    ) {
        ChestBackdrop(tier = tier, modifier = Modifier.matchParentSize())
        Box(
            modifier = Modifier
                .padding(top = 40.dp)
                .graphicsLayer { rotationZ = wobble * 6f }
        ) {
            ChestImage(tier = tier, width = chestWidth.dp)
        }
        AnimatedVisibility(
            visible = bubble != null,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp, start = 16.dp, end = 16.dp),
            enter = fadeIn(tween(120)) + scaleIn(initialScale = 0.7f),
            exit = fadeOut(tween(160)) + scaleOut(targetScale = 0.9f)
        ) {
            val res = bubble?.res
            if (res != null) SpeechBubble(text = stringResource(res))
        }
    }
}
