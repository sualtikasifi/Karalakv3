package com.sualtikasifi.cizimhafiza.presentation.achievements

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.window.Dialog
import com.sualtikasifi.cizimhafiza.presentation.common.PillShape
import com.sualtikasifi.cizimhafiza.presentation.common.PrimaryButton
import com.sualtikasifi.cizimhafiza.presentation.common.TintedBadge
import com.sualtikasifi.cizimhafiza.domain.model.Achievement
import com.sualtikasifi.cizimhafiza.domain.model.AchievementRewardType
import com.sualtikasifi.cizimhafiza.presentation.theme.AppTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.PI
import kotlin.math.sin
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.presentation.common.WarmCard
import com.sualtikasifi.cizimhafiza.presentation.common.ScreenTopActions
import com.sualtikasifi.cizimhafiza.presentation.common.TopActionsClearance
import com.sualtikasifi.cizimhafiza.presentation.common.screenBackground
import kotlinx.coroutines.delay

/**
 * Purely the badge catalog now — rank/level, the pen and frame pickers moved
 * to the main menu's profile card (see MainMenuScreen.LevelBadgeCard), and
 * the raw session stats/recent-games list were dropped entirely rather than
 * kept as a second section, so this screen is unambiguously "the achievements
 * page" the "Başarımlar" tile promises.
 */
@Composable
fun AchievementsScreen(
    onBack: () -> Unit,
    viewModel: AchievementsViewModel = hiltViewModel()
) {
    val achievements by viewModel.achievements.collectAsState()
    val unlockedCount = achievements.count { it.unlocked }
    // Tapping a chip explains what it takes to earn it — before this,
    // nothing in the UI ever spelled out an achievement's condition beyond
    // the small print on the card itself, easy to miss among 101 of them.
    var selectedAchievement by remember { mutableStateOf<AchievementUiItem?>(null) }

    // No title bar: the back button floats directly on the page's own
    // background instead of sitting in a separate, differently-colored strip.
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .screenBackground()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                // Clears the floating back button (see ScreenTopActions).
                contentPadding = PaddingValues(top = TopActionsClearance, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    AchievementProgressHeader(
                        unlockedCount = unlockedCount,
                        total = achievements.size,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                items(achievements.chunked(3)) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { item ->
                            AchievementChip(
                                item = item,
                                onClick = { if (item.unlocked && !item.claimed) viewModel.claim(item.achievement) else selectedAchievement = item },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Pad the last, possibly-shorter row so its chips stay
                        // the same width as full rows instead of stretching.
                        repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }

            ScreenTopActions(
                onBack = onBack,
                title = stringResource(R.string.menu_achievements),
                modifier = Modifier.align(Alignment.TopStart)
            )

            selectedAchievement?.let { item ->
                AchievementDetailDialog(
                    item = item,
                    onClaim = {
                        viewModel.claim(item.achievement)
                        selectedAchievement = null
                    },
                    onDismiss = { selectedAchievement = null }
                )
            }
        }
    }
}

/**
 * The catalog's progress line, given a card of its own. It used to be a lone
 * muted sentence floating level with the back button — with 101 chips
 * scrolling under it, the one number that says how far along you are read as
 * a caption on the first row rather than as the page's own summary.
 */
@Composable
private fun AchievementProgressHeader(unlockedCount: Int, total: Int, modifier: Modifier = Modifier) {
    val fraction = if (total > 0) unlockedCount.toFloat() / total else 0f
    WarmCard(corner = 24.dp, face = AppTheme.tokens.cardWarm, raise = 7.dp, modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(46.dp)
                        .background(AppTheme.tokens.gold.copy(alpha = 0.18f), CircleShape)
                        .border(2.dp, AppTheme.tokens.gold.copy(alpha = 0.55f), CircleShape)
                ) {
                    Text(text = "\uD83C\uDFC6", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(modifier = Modifier.size(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.achievements_progress_count_format, unlockedCount, total),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = stringResource(R.string.achievements_progress_caption),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TintedBadge(
                    text = stringResource(R.string.achievements_progress_percent, (fraction * 100).toInt()),
                    container = AppTheme.tokens.gold.copy(alpha = 0.18f),
                    content = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, PillShape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(10.dp)
                        .background(AppTheme.tokens.gold, PillShape)
                )
            }
        }
    }
}

@Composable
private fun rewardText(achievement: Achievement): String = when (achievement.rewardType) {
    AchievementRewardType.XP -> stringResource(R.string.achievement_xp_reward, achievement.xpReward)
    AchievementRewardType.GOLD -> stringResource(R.string.achievement_gold_reward, achievement.goldReward)
}

@Composable
private fun AchievementDetailDialog(item: AchievementUiItem, onClaim: () -> Unit, onDismiss: () -> Unit) {
    // A custom dialog rather than a stock AlertDialog: this is the app's one
    // "what did I earn / what am I chasing" moment, and Material's default
    // (small icon, plain title, run of body text, a text button) had none of
    // the app's own language in it — no raised card, no medallion, no
    // separation between the condition and the reward.
    Dialog(onDismissRequest = onDismiss) {
        WarmCard(corner = 30.dp, raise = 8.dp, modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // The badge itself, on a tinted disc ringed like the main
                // menu's logo medallion — gold once earned, muted while it
                // is still locked.
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .background(
                            if (item.unlocked) AppTheme.tokens.gold.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape
                        )
                        .border(3.dp, if (item.unlocked) AppTheme.tokens.gold else AppTheme.tokens.edge, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.achievement.emoji,
                        style = MaterialTheme.typography.displaySmall,
                        modifier = Modifier.alpha(if (item.unlocked) 1f else 0.45f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = stringResource(item.achievement.titleRes),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))
                TintedBadge(
                    text = stringResource(
                        when {
                            item.unlocked && item.claimed -> R.string.achievement_claimed_label
                            item.unlocked -> R.string.achievement_unlocked_label
                            else -> R.string.achievement_locked_label
                        }
                    ),
                    container = if (item.unlocked) AppTheme.tokens.successContainer else MaterialTheme.colorScheme.surfaceVariant,
                    content = if (item.unlocked) AppTheme.tokens.success else MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!item.unlocked) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(
                            R.string.achievement_progress_format,
                            item.currentValue.coerceAtMost(item.achievement.target),
                            item.achievement.target
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))
                // The condition sits in its own inset panel so it reads as
                // the answer to the label above it rather than as one more
                // line of text in a stack. The description is already
                // phrased as the condition (e.g. "Toplamda 250 puana
                // ulaştın"), whether the player is still chasing it or
                // reading it after the fact.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.large)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(
                            if (item.unlocked) R.string.achievement_condition_label else R.string.achievement_unlock_hint_label
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(item.achievement.descriptionRes),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .background(AppTheme.tokens.gold.copy(alpha = 0.16f), PillShape)
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text(text = "\uD83C\uDFC6", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = rewardText(item.achievement),
                        style = MaterialTheme.typography.titleMedium,
                        color = rewardColor(item.achievement)
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))
                if (item.unlocked && !item.claimed) {
                    PrimaryButton(
                        text = stringResource(R.string.achievement_claim_button),
                        onClick = onClaim,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    PrimaryButton(
                        text = stringResource(R.string.close),
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}


/** Gold prizes read in yellow everywhere they appear, so "50 altın" is unmistakable next to "50 XP". */
private val GoldRewardText = Color(0xFFE6A400)

@Composable
private fun rewardColor(achievement: Achievement): Color = when (achievement.rewardType) {
    AchievementRewardType.GOLD -> GoldRewardText
    AchievementRewardType.XP -> MaterialTheme.colorScheme.primary
}

@Composable
private fun AchievementChip(
    item: AchievementUiItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val claimable = item.unlocked && !item.claimed

    // Earned-but-uncollected chips breathe (border, fill, size) until tapped —
    // the one thing on this page that should draw the eye among 101 tiles.
    val transition = rememberInfiniteTransition(label = "claimPulse")
    val pulseAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(650, easing = LinearEasing), RepeatMode.Reverse),
        label = "claimPulseValue"
    )
    val pulse = if (claimable) pulseAnim else 0f

    // One-shot celebration the moment a chip flips from claimable to claimed:
    // a small bounce plus the reward floating up and fading.
    val burst = remember(item.achievement.name) { Animatable(0f) }
    var wasClaimable by remember(item.achievement.name) { mutableStateOf(claimable) }
    LaunchedEffect(claimable) {
        if (wasClaimable && !claimable && item.claimed) {
            burst.snapTo(0f)
            burst.animateTo(1f, tween(1000, easing = LinearEasing))
            burst.snapTo(0f)
        }
        wasClaimable = claimable
    }
    val bounce = sin(burst.value * PI.toFloat()) * 0.12f

    val borderColor = when {
        item.claimed -> AppTheme.tokens.success
        claimable -> AppTheme.tokens.gold.copy(alpha = 0.45f + 0.55f * pulse)
        else -> MaterialTheme.colorScheme.outline
    }
    val face = when {
        item.claimed -> AppTheme.tokens.successContainer
        claimable -> lerp(AppTheme.tokens.cardWarm, Color(0xFFFFE08A), 0.75f * pulse)
        else -> AppTheme.tokens.cardWarm
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // RaisedCard, not a flat Material one: 101 flat white squares on the
        // textured page read as cut-out holes in the artwork rather than as
        // chips sitting on it.
        WarmCard(
            onClick = onClick,
            corner = 20.dp,
            face = face,
            border = borderColor,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    val s = 1f + 0.035f * pulse + bounce
                    scaleX = s
                    scaleY = s
                }
                .alpha(if (item.unlocked) 1f else 0.85f)
        ) {
            // Fixed height + both axes centered: titles run one or two lines, so
            // without this the chips in a row ended up different heights with
            // their emoji/label sitting at different offsets instead of centered.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(126.dp)
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = item.achievement.emoji, style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(item.achievement.titleRes),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(2.dp))
                // Status line: how close (locked), a call to action (earned),
                // or a check (collected).
                when {
                    !item.unlocked -> Text(
                        text = stringResource(
                            R.string.achievement_progress_format,
                            item.currentValue.coerceAtMost(item.achievement.target),
                            item.achievement.target
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    claimable -> Text(
                        text = stringResource(R.string.achievement_claim_button),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFB8740A)
                    )
                    else -> Text(
                        text = "✓",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = AppTheme.tokens.success
                    )
                }
                // Reward line on EVERY chip, so what each one pays is never a
                // mystery — gold in yellow, XP in the accent colour.
                Text(
                    text = rewardText(item.achievement),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = rewardColor(item.achievement)
                )
            }
        }
        if (burst.value > 0f) {
            Text(
                text = rewardText(item.achievement),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = rewardColor(item.achievement),
                modifier = Modifier
                    .offset(y = (-56 * burst.value).dp)
                    .alpha(1f - burst.value)
                    .graphicsLayer {
                        val s = 1f + 0.2f * burst.value
                        scaleX = s
                        scaleY = s
                    }
            )
        }
    }
}
