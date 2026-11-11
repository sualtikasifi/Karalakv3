package com.sualtikasifi.cizimhafiza.presentation.friends

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.presentation.common.AppWindowDialog
import com.sualtikasifi.cizimhafiza.presentation.common.PrimaryButton
import com.sualtikasifi.cizimhafiza.presentation.common.SecondaryButton
import com.sualtikasifi.cizimhafiza.presentation.common.adOnly
import com.sualtikasifi.cizimhafiza.presentation.common.nameRes
import com.sualtikasifi.cizimhafiza.util.StreakReward

/**
 * The window a friend streak opens the home screen with (see [nextPrompt]). There are no push messages to say a streak is
 * in danger, so this is where the player finds out: it names the friend and the days, and offers the one thing to do.
 */
@Composable
fun FriendStreakDialog(
    prompt: StreakPrompt,
    onRescued: (StreakPrompt.Rescued) -> Unit,
    onClaim: (StreakPrompt.Reward) -> Unit,
    onRescue: (StreakPrompt.Broken) -> Unit,
    onDuel: () -> Unit,
    onDismiss: () -> Unit
) {
    when (prompt) {
        is StreakPrompt.Rescued -> Window(
            emoji = "🔥",
            title = stringResource(R.string.fstreak_rescued_title, prompt.friendName),
            body = stringResource(R.string.fstreak_rescued_body, prompt.friendName, prompt.count),
            onDismiss = { onRescued(prompt) }
        ) {
            PrimaryButton(text = stringResource(R.string.fstreak_rescued_ok), onClick = { onRescued(prompt) }, modifier = Modifier.fillMaxWidth())
        }

        is StreakPrompt.Reward -> Window(
            emoji = "🏆",
            title = stringResource(R.string.fstreak_reward_title, prompt.friendName, prompt.milestone),
            body = stringResource(R.string.fstreak_reward_body, prompt.friendName, prompt.milestone),
            onDismiss = { onClaim(prompt) },
            extra = {
                Text(
                    text = rewardLines(prompt.reward),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        ) {
            PrimaryButton(text = stringResource(R.string.fstreak_reward_claim), onClick = { onClaim(prompt) }, modifier = Modifier.fillMaxWidth())
        }

        is StreakPrompt.Broken -> Window(
            emoji = "💔",
            title = stringResource(R.string.fstreak_broken_title, prompt.friendName),
            body = stringResource(R.string.fstreak_broken_body, prompt.friendName, prompt.count),
            onDismiss = onDismiss
        ) {
            // The rescue IS an ad, and the button says so (AdMob requires the ad to be stated before the tap).
            PrimaryButton(
                text = stringResource(R.string.fstreak_broken_action),
                icon = Icons.Filled.PlayCircle,
                onClick = adOnly { onRescue(prompt) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            SecondaryButton(text = stringResource(R.string.fstreak_broken_dismiss), onClick = onDismiss, modifier = Modifier.fillMaxWidth())
        }

        is StreakPrompt.AtRisk -> {
            Window(
                emoji = "⏳",
                title = stringResource(R.string.fstreak_reminder_title),
                body = prompt.items.map { stringResource(R.string.fstreak_reminder_body, it.friendName, it.count) }.joinToString("\n"),
                onDismiss = onDismiss
            ) {
                PrimaryButton(text = stringResource(R.string.fstreak_reminder_action), onClick = onDuel, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                SecondaryButton(text = stringResource(R.string.fstreak_reminder_later), onClick = onDismiss, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun Window(
    emoji: String,
    title: String,
    body: String,
    onDismiss: () -> Unit,
    extra: (@Composable () -> Unit)? = null,
    buttons: @Composable () -> Unit
) {
    AppWindowDialog(
        title = title,
        onDismiss = onDismiss,
        centerTitle = true,
        footer = {
            Column(Modifier.padding(top = 12.dp)) { buttons() }
        }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Text(emoji, fontSize = 46.sp)
            Text(body, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            extra?.invoke()
        }
    }
}

/** The prize as one or two lines of text: "+50 altın", "3 joker …", the name of the frame or pen. */
@Composable
internal fun rewardLines(reward: StreakReward): String = when (reward) {
    is StreakReward.Gold -> stringResource(R.string.fstreak_reward_gold, reward.amount)
    StreakReward.Jokers -> stringResource(R.string.fstreak_reward_jokers)
    is StreakReward.Frame -> stringResource(R.string.fstreak_reward_frame, stringResource(reward.frame.nameRes()))
    is StreakReward.Pen -> stringResource(R.string.fstreak_reward_pen, stringResource(reward.pen.labelRes))
}
