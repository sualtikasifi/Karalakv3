package com.sualtikasifi.cizimhafiza.presentation.mainmenu

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.LoginDayReward
import com.sualtikasifi.cizimhafiza.domain.model.LoginStreakRewards
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedPillButton
import com.sualtikasifi.cizimhafiza.presentation.common.adOnly
import com.sualtikasifi.cizimhafiza.presentation.common.pressable
import com.sualtikasifi.cizimhafiza.presentation.common.pressable

private val CellInk = Color(0xFF4A2A10)
private val Gold = Color(0xFFF2A100)

/**
 * The daily login window: the seven-day calendar with today lit up and the days already taken ticked, and — depending
 * on the state — the button that takes today's reward, the choice after a missed day (watch an ad to carry the streak
 * on, or start again), or the confirmation of what was just taken.
 */
@Composable
internal fun LoginStreakDialog(
    popup: LoginPopup,
    onClaim: () -> Unit,
    onRescue: () -> Unit,
    onRestart: () -> Unit,
    onClose: () -> Unit
) {
    val streakBefore = when (popup) {
        is LoginPopup.Claim -> popup.streakBefore
        is LoginPopup.Missed -> popup.lostStreak
        is LoginPopup.Claimed -> popup.newStreak - 1
    }
    // Which calendar slot is "today", and how many before it are already ticked.
    val today = when (popup) {
        is LoginPopup.Claimed -> popup.day
        is LoginPopup.Missed -> 0
        is LoginPopup.Claim -> LoginStreakRewards.cycleDayFor(streakBefore)
    }
    val claimed = popup is LoginPopup.Claimed
    val title = when (popup) {
        is LoginPopup.Missed -> stringResource(R.string.login_streak_missed_title)
        is LoginPopup.Claimed -> stringResource(R.string.login_streak_claimed_title)
        is LoginPopup.Claim -> stringResource(R.string.login_streak_title)
    }
    // The painted button carries Turkish lettering, so only the Turkish resources turn it on.
    val artButton = popup is LoginPopup.Claim && androidx.compose.ui.res.booleanResource(R.bool.login_streak_painted_art)
    val canDismiss = popup !is LoginPopup.Missed
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = canDismiss, dismissOnBackPress = canDismiss)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { if (canDismiss) onClose() },
            contentAlignment = Alignment.Center
        ) {
            // The painted frame is 1024 x 1536; everything inside is placed in its own units, so it scales with it.
            val frameW = minOf(maxWidth - 8.dp, maxHeight / 1.5f)
            val u = frameW / 1024f
            Box(
                Modifier
                    .size(frameW, frameW * 1.5f)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            ) {
                Image(painterResource(R.drawable.ls_frame), contentDescription = null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
                if (canDismiss) {
                    // The X is painted into the frame, so it cannot scale itself: while pressed its own pixels cave in (see sunkenArt).
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val frameBitmap = remember { runCatching { com.sualtikasifi.cizimhafiza.presentation.common.BackdropCache.get(context.resources, R.drawable.ls_frame) }.getOrNull() }
                    val sink = rememberSink(0.86f)
                    val px = (frameBitmap?.width ?: 1024) / 1024f
                    Box(
                        Modifier
                            .offset(u * 876f, u * 240f)
                            .size(u * 128f, u * 128f)
                            .sunkenArt(sink, frameBitmap, 876f * px, 240f * px, 128f * px, 128f * px, u * 64f)
                            .clickable(interactionSource = sink.source, indication = null, onClick = onClose)
                    )
                }
                Column(
                    Modifier.offset(u * 84f, u * 436f).size(u * 856f, u * 900f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // What this window says right now: the streak, or what just happened.
                    Box(Modifier.fillMaxWidth().height(u * 70f), contentAlignment = Alignment.Center) {
                        val hint: Pair<String, Color>? = when (popup) {
                            is LoginPopup.Claim -> if (popup.rescued) stringResource(R.string.login_streak_rescued) to Color(0xFF1F9A3A) else null
                            is LoginPopup.Missed -> stringResource(R.string.login_streak_missed_message, popup.lostStreak) to CellInk
                            is LoginPopup.Claimed -> popup.milestone?.let { stringResource(R.string.login_streak_milestone, popup.newStreak) to Color(0xFF8E44D6) }
                        }
                        when {
                            hint != null -> Text(hint.first, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 15.sp), color = hint.second, textAlign = TextAlign.Center, maxLines = 3)
                            claimed -> Text(title, style = MaterialTheme.typography.titleMedium, color = CellInk, textAlign = TextAlign.Center)
                        }
                    }
                    // Seven days: four, then three.
                    listOf((1..4).toList(), (5..7).toList()).forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(u * 14f, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().weight(1f).padding(bottom = u * 12f).zIndex(if (claimed && today in row) 1f else 0f)
                        ) {
                            if (row.size < 4) Spacer(Modifier.weight(0.5f))
                            row.forEach { day ->
                                val done = if (claimed) day <= today else today > 0 && day < today
                                DayCell(
                                    day = day,
                                    reward = LoginStreakRewards.forDay(day),
                                    done = done,
                                    isToday = day == today && !claimed,
                                    justClaimed = claimed && day == today,
                                    modifier = Modifier.weight(1f).aspectRatio(998f / 1343f)
                                )
                            }
                            if (row.size < 4) Spacer(Modifier.weight(0.5f))
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(u * 150f), contentAlignment = Alignment.Center) {
                        when (popup) {
                            is LoginPopup.Claim -> if (artButton) {
                                Image(
                                    painterResource(R.drawable.ls_btn_claim),
                                    contentDescription = stringResource(R.string.login_streak_claim),
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxHeight().pressable(pressedScale = 0.96f, onClick = onClaim)
                                )
                            } else PaintedPillButton(
                                text = stringResource(R.string.login_streak_claim),
                                onClick = onClaim,
                                modifier = Modifier.fillMaxWidth(0.8f)
                            )
                            is LoginPopup.Missed -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PaintedPillButton(
                                    text = stringResource(R.string.login_streak_save),
                                    onClick = adOnly(onRescue),
                                    modifier = Modifier.weight(1f)
                                )
                                PaintedPillButton(
                                    text = stringResource(R.string.login_streak_restart),
                                    onClick = onRestart,
                                    primary = false,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            is LoginPopup.Claimed -> PaintedPillButton(
                                text = stringResource(R.string.login_streak_great),
                                onClick = onClose,
                                modifier = Modifier.fillMaxWidth(0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Hint(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = color,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
    )
}

/** One day of the calendar: its number, what it gives, and whether it is taken, today or still ahead. */
@Composable
private fun DayCell(day: Int, reward: LoginDayReward, done: Boolean, isToday: Boolean, justClaimed: Boolean, modifier: Modifier = Modifier) {
    val pulse = rememberInfiniteTransition(label = "dayPulse")
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "dayScale"
    )
    val pop = remember { Animatable(if (justClaimed) 0.2f else 1f) }
    LaunchedEffect(justClaimed) {
        if (justClaimed) pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 260f))
    }
    val lit = isToday || justClaimed
    // Every day wears a painted card: its two wooden plates take the day and the reward.
    val cardArt = when {
        reward.chest != null -> if (reward.chest == com.sualtikasifi.cizimhafiza.domain.model.ChestTier.RARE) R.drawable.ls_card_pink else R.drawable.ls_card_blue
        reward.jokers.isNotEmpty() -> R.drawable.ls_card_joker
        reward.gold > 0 -> R.drawable.ls_card_gold
        else -> null
    }
    if (cardArt != null) {
        BoxWithConstraints(
            modifier = modifier
                .aspectRatio(998f / 1343f)
                .zIndex(if (justClaimed) 1f else 0f)
                .graphicsLayer {
                    val s = if (isToday) scale else 1f
                    scaleX = s
                    scaleY = s
                    alpha = if (done && !justClaimed) 0.75f else 1f
                }
        ) {
            val w = maxWidth
            val h = maxHeight
            val labelSize = (w.value * 0.15f).sp
            Image(painterResource(cardArt), contentDescription = null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
            PlateText(
                text = stringResource(R.string.login_streak_day_n, day),
                size = labelSize * 0.92f,
                lift = 0.2f,
                modifier = Modifier.offset(w * 0.183f, h * 0.1485f).size(w * 0.64f, h * 0.136f)
            )
            PlateText(
                text = when {
                    reward.chest != null -> stringResource(R.string.login_reward_chest)
                    reward.jokers.isNotEmpty() -> stringResource(R.string.login_reward_joker)
                    else -> reward.gold.toString()
                },
                size = labelSize,
                lift = 0.19f,
                modifier = Modifier.offset(w * 0.163f, h * 0.7345f).size(w * 0.675f, h * 0.133f)
            )
            // The state of the day sits just under the card, clear of the wooden plate.
            Box(
                modifier = Modifier
                    .offset(w * 0.5f - w * 0.06f, h + 1.dp)
                    .graphicsLayer { if (done) { scaleX = pop.value; scaleY = pop.value } }
                    .size(w * 0.12f)
                    .background(if (done) Color(0xFF2EA043) else Color(0xFFE6D0A2), CircleShape)
                    .border(1.5.dp, if (done) Color(0xFF1B7A30) else Color(0xFFC9A46A), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (done) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(w * 0.085f))
            }
            if (justClaimed && (reward.gold > 0 || reward.jokers.isNotEmpty())) {
                RewardBurst(reward.gold, reward.jokers, Modifier.fillMaxSize())
            }
        }
        return
    }
    val shape = RoundedCornerShape(18.dp)
    val dayPill = if (lit) Color(0xFF2E8B34) else Color(0xFF8A5A33)
    Column(
        modifier = modifier
            .graphicsLayer {
                val s = if (isToday) scale else 1f
                scaleX = s
                scaleY = s
                alpha = if (done && !justClaimed) 0.75f else 1f
            }
            .background(
                Brush.verticalGradient(
                    if (lit) listOf(Color(0xFFE9F7A8), Color(0xFF8ED63F)) else listOf(Color(0xFFFFF6DE), Color(0xFFF6E2B4))
                ),
                shape
            )
            .border(if (lit) 3.dp else 2.dp, if (lit) Color(0xFF4FA12A) else Color(0xFFD9A55A), shape)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Text(
            text = stringResource(R.string.login_streak_day_n, day),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
            color = Color.White,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().background(dayPill, RoundedCornerShape(50)).padding(vertical = 2.dp)
        )
        Spacer(Modifier.height(4.dp))
        Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            when {
                reward.chest != null -> Image(painterResource(if (reward.gold > 0) R.drawable.ls_chest_gold else R.drawable.ls_chest_silver), contentDescription = null, modifier = Modifier.size(40.dp))
                reward.jokers.isNotEmpty() -> Image(painterResource(R.drawable.ls_joker), contentDescription = null, modifier = Modifier.size(38.dp))
                else -> Image(painterResource(R.drawable.ls_coins), contentDescription = null, modifier = Modifier.size(40.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().background(Color(0xFF7A4A28), RoundedCornerShape(50)).padding(vertical = 3.dp)
        ) {
            val showCoin = reward.gold > 0 && reward.jokers.isEmpty() && (reward.chest == null)
            if (showCoin || (reward.chest != null && reward.gold > 0)) {
                Image(painterResource(R.drawable.icon_gold_coin), contentDescription = null, modifier = Modifier.size(14.dp))
            }
            Text(
                text = when {
                    reward.chest != null && reward.gold > 0 -> "+${reward.gold}"
                    reward.chest != null -> stringResource(R.string.login_reward_chest)
                    reward.jokers.isNotEmpty() -> stringResource(R.string.login_reward_joker)
                    else -> reward.gold.toString()
                },
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                color = Color.White,
                maxLines = 1
            )
        }
        Spacer(Modifier.height(5.dp))
        // The state of the day: an empty dish still ahead, a green tick once taken.
        Box(
            modifier = Modifier
                .graphicsLayer { if (done) { scaleX = pop.value; scaleY = pop.value } }
                .size(20.dp)
                .background(if (done) Color(0xFF2EA043) else Color(0xFFE6D0A2), CircleShape)
                .border(1.5.dp, if (done) Color(0xFF1B7A30) else Color(0xFFC9A46A), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (done) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        }
    }
}

/** White lettering centred on a wooden plate of the painted card, whatever the font's own padding. */
@Composable
private fun PlateText(text: String, size: androidx.compose.ui.unit.TextUnit, modifier: Modifier, lift: Float = 0.12f) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = size,
                lineHeight = size,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(
                    alignment = androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,
                    trim = androidx.compose.ui.text.style.LineHeightStyle.Trim.Both
                )
            ),
            color = Color.White,
            maxLines = 1,
            textAlign = TextAlign.Center,
            // The line box sits a little low on the glyphs; lift it so the figures read as centred on the plate.
            modifier = Modifier.graphicsLayer { translationY = -size.toPx() * lift }
        )
    }
}
