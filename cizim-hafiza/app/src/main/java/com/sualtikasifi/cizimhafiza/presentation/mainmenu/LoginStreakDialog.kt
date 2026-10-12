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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.sualtikasifi.cizimhafiza.presentation.chests.ChestImage
import com.sualtikasifi.cizimhafiza.presentation.common.JokerArt
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedDialog
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedPillButton
import com.sualtikasifi.cizimhafiza.presentation.common.adOnly

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
    PaintedDialog(
        title = title,
        onDismiss = onClose,
        dismissOnOutsideTap = popup !is LoginPopup.Missed,
        buttons = {
            when (popup) {
                is LoginPopup.Claim -> PaintedPillButton(
                    text = stringResource(R.string.login_streak_claim),
                    onClick = onClaim,
                    modifier = Modifier.fillMaxWidth()
                )
                is LoginPopup.Missed -> {
                    PaintedPillButton(
                        text = stringResource(R.string.login_streak_save),
                        onClick = adOnly(onRescue),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    PaintedPillButton(
                        text = stringResource(R.string.login_streak_restart),
                        onClick = onRestart,
                        primary = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                is LoginPopup.Claimed -> PaintedPillButton(
                    text = stringResource(R.string.login_streak_great),
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) {
        // The streak, as a flame and a number.
        Text(
            text = "🔥 " + stringResource(R.string.login_streak_days, if (claimed) (popup as LoginPopup.Claimed).newStreak else streakBefore),
            style = MaterialTheme.typography.titleMedium,
            color = CellInk,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        // Seven days: four, then three.
        listOf((1..4).toList(), (5..7).toList()).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
            ) {
                row.forEach { day ->
                    val done = if (claimed) day <= today else today > 0 && day < today
                    DayCell(
                        day = day,
                        reward = LoginStreakRewards.forDay(day),
                        done = done,
                        isToday = day == today && !claimed,
                        justClaimed = claimed && day == today,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        when (popup) {
            is LoginPopup.Claim -> if (popup.rescued) {
                Hint(stringResource(R.string.login_streak_rescued), Color(0xFF1F9A3A))
            }
            is LoginPopup.Missed -> Hint(stringResource(R.string.login_streak_missed_message, popup.lostStreak), CellInk)
            is LoginPopup.Claimed -> {
                popup.milestone?.let { Hint(stringResource(R.string.login_streak_milestone, popup.newStreak), Color(0xFF8E44D6)) }
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
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .graphicsLayer {
                val s = if (isToday) scale else 1f
                scaleX = s
                scaleY = s
                alpha = if (done && !justClaimed) 0.6f else 1f
            }
            .background(
                Brush.verticalGradient(
                    if (isToday || justClaimed) listOf(Color(0xFFFFF1C2), Color(0xFFFFD66B)) else listOf(Color(0xFFFFF8EA), Color(0xFFF3E2C0))
                ),
                shape
            )
            .border(if (isToday || justClaimed) 2.5.dp else 1.5.dp, if (isToday || justClaimed) Color(0xFFE08A1B) else Color(0xFFC9A46A), shape)
            .padding(vertical = 6.dp, horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.login_streak_day_n, day),
                style = MaterialTheme.typography.labelSmall,
                color = CellInk,
                maxLines = 1
            )
            Spacer(Modifier.height(3.dp))
            Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                when {
                    reward.chest != null -> ChestImage(reward.chest, 40.dp)
                    reward.joker != null -> JokerArt(reward.joker, 34.dp)
                    else -> Image(painterResource(R.drawable.icon_gold_coin), contentDescription = null, modifier = Modifier.size(34.dp))
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = when {
                    reward.chest != null && reward.gold > 0 -> "+${reward.gold}"
                    reward.chest != null -> stringResource(R.string.login_reward_chest)
                    reward.joker != null -> stringResource(R.string.login_reward_joker)
                    else -> reward.gold.toString()
                },
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                color = if (reward.gold > 0 || reward.chest != null) CellInk else Gold,
                maxLines = 1
            )
        }
        if (done) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .graphicsLayer { scaleX = pop.value; scaleY = pop.value }
                    .size(20.dp)
                    .background(Color(0xFF2EA043), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}
