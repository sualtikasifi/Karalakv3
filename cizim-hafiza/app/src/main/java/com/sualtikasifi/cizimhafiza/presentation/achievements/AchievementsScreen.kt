package com.sualtikasifi.cizimhafiza.presentation.achievements

import com.sualtikasifi.cizimhafiza.presentation.common.cachedPainterResource
import androidx.compose.animation.core.Animatable
import com.sualtikasifi.cizimhafiza.presentation.common.springIn
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.statusBarsPadding
import com.sualtikasifi.cizimhafiza.presentation.common.ButtonOrange
import com.sualtikasifi.cizimhafiza.presentation.common.DescriptionInk
import com.sualtikasifi.cizimhafiza.presentation.common.InkBrown
import com.sualtikasifi.cizimhafiza.presentation.common.LetteredText
import com.sualtikasifi.cizimhafiza.presentation.common.NinePatch
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedStyle
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
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.ui.graphics.drawscope.rotate as drawRotate
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

private const val ArtW = 1080f
private const val ArtH = 2401f
private val CardDrawables = intArrayOf(R.drawable.ach_card_1, R.drawable.ach_card_2, R.drawable.ach_card_3, R.drawable.ach_card_4)

/**
 * The badge catalog on the painted trophy-room scene: a hanging sign for the title, a progress panel and a grid of
 * painted cards. The grid is long by nature (101 badges), so it scrolls under the sign and melts away at its top edge.
 */
@Composable
fun AchievementsScreen(
    onBack: () -> Unit,
    viewModel: AchievementsViewModel = hiltViewModel()
) {
    val achievements by viewModel.achievements.collectAsState()
    val unlockedCount = achievements.count { it.unlocked }
    // Tapping a chip explains what it takes to earn it.
    var selectedAchievement by remember { mutableStateOf<AchievementUiItem?>(null) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val sc = maxOf(widthPx / ArtW, heightPx / ArtH)
        val offX = (widthPx - ArtW * sc) / 2f
        val offY = 0f
        fun yOf(px: Float): Dp = with(density) { (offY + px * sc).toDp() }
        fun len(px: Float): Dp = with(density) { (px * sc).toDp() }

        Image(
            painter = cachedPainterResource(R.drawable.bg_achievements),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = androidx.compose.ui.Alignment.TopCenter,
            modifier = Modifier.fillMaxSize()
        )

        // The cards scroll between the sign and the bottom edge.
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = yOf(440f))
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    val fade = 26.dp.toPx().coerceAtMost(size.height)
                    drawRect(
                        brush = Brush.verticalGradient(colorStops = arrayOf(0f to Color.Transparent, (fade / size.height) to Color.Black, 1f to Color.Black)),
                        blendMode = BlendMode.DstIn
                    )
                },
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                AchievementProgressHeader(unlockedCount = unlockedCount, total = achievements.size)
            }
            itemsIndexed(achievements.chunked(3)) { rowIndex, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.springIn(index = rowIndex.coerceAtMost(7), stepMs = 60)) {
                    row.forEachIndexed { col, item ->
                        AchievementChip(
                            item = item,
                            variant = (rowIndex * 3 + col + rowIndex) % CardDrawables.size,
                            onClick = { if (item.unlocked && !item.claimed) viewModel.claim(item.achievement) else selectedAchievement = item },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }

        // Hanging sign with the title.
        val signWidth = maxWidth * 0.66f
        val signHeight = signWidth * (507f / 1201f)
        Box(
            modifier = Modifier.align(Alignment.TopCenter).offset(y = yOf(120f)).width(signWidth).height(signHeight),
            contentAlignment = Alignment.TopCenter
        ) {
            Image(
                painter = painterResource(R.drawable.ach_sign),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier.fillMaxWidth().offset(x = signWidth * 0.028f, y = signHeight * 0.415f).height(signHeight * 0.36f),
                contentAlignment = Alignment.Center
            ) {
                LetteredText(stringResource(R.string.menu_achievements), (len(64f).value).sp)
            }
        }

        // The painted back button.
        val backInteraction = remember { MutableInteractionSource() }
        Image(
            painter = painterResource(R.drawable.join_back),
            contentDescription = stringResource(R.string.cd_back),
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 16.dp, top = 12.dp)
                .size(56.dp)
                .clickable(interactionSource = backInteraction, indication = null, onClick = onBack)
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

/** The catalog's summary: trophy, how many are open, the percentage and a progress bar, on a painted panel. */
@Composable
private fun AchievementProgressHeader(unlockedCount: Int, total: Int, modifier: Modifier = Modifier) {
    val fraction = if (total > 0) unlockedCount.toFloat() / total else 0f
    NinePatch(
        res = R.drawable.ach_panel,
        slicePx = 150,
        edge = 34.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 30.dp, vertical = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFFFFE9B0), CircleShape)
                        .border(2.dp, Color(0xFFE0A030), CircleShape)
                ) {
                    Text(text = "\uD83C\uDFC6", style = PaintedStyle(fontSize = 22.sp, textAlign = TextAlign.Center))
                }
                Spacer(modifier = Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.achievements_progress_count_format, unlockedCount, total),
                        style = PaintedStyle(color = InkBrown, fontSize = 24.sp),
                        maxLines = 1
                    )
                    Text(
                        text = stringResource(R.string.achievements_progress_caption),
                        style = PaintedStyle(color = DescriptionInk, fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                        maxLines = 1
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .background(Brush.verticalGradient(listOf(Color(0xFFFFA64D), ButtonOrange)), PillShape)
                        .border(1.5.dp, Color(0xFFB04A0E), PillShape)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    LetteredText(stringResource(R.string.achievements_progress_percent, (fraction * 100).toInt()), 16.sp)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .background(Color(0xFFE2CDAA), PillShape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                        .height(12.dp)
                        .background(Brush.verticalGradient(listOf(Color(0xFFFFD04D), Color(0xFFF5A300))), PillShape)
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
    variant: Int,
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
            burst.animateTo(1f, tween(1400, easing = LinearEasing))
            burst.snapTo(0f)
        }
        wasClaimable = claimable
    }
    // Quick pop up, then a settling wobble — not a single lazy bump.
    // The chip itself stays put while collecting (no scale wobble): its size changing under the
    // player's finger read as the tile "changing shape". The effects are the flash, ring and pill.
    val bounce = 0f

    val tint = when {
        item.claimed -> ColorFilter.tint(Color(0xFFA9DE9A), BlendMode.Modulate)
        claimable -> ColorFilter.tint(Color(0xFFFFD84D).copy(alpha = 0.55f * pulse), BlendMode.SrcAtop)
        !item.unlocked -> ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.55f) })
        else -> null
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        NinePatch(
            res = CardDrawables[variant],
            slicePx = 120,
            edge = 26.dp,
            tint = tint,
            modifier = Modifier
                .fillMaxWidth()
                .height(128.dp)
                .graphicsLayer {
                    val sc = 1f + 0.035f * pulse + bounce
                    scaleX = sc
                    scaleY = sc
                }
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 13.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    text = item.achievement.emoji,
                    style = PaintedStyle(fontSize = 26.sp, textAlign = TextAlign.Center),
                    modifier = Modifier.alpha(if (item.unlocked) 1f else 0.6f)
                )
                Text(
                    text = stringResource(item.achievement.titleRes),
                    style = PaintedStyle(color = InkBrown, fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 13.sp),
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                // Status line: how close (locked), a call to action (earned), or a check (collected).
                when {
                    !item.unlocked -> Text(
                        text = stringResource(
                            R.string.achievement_progress_format,
                            item.currentValue.coerceAtMost(item.achievement.target),
                            item.achievement.target
                        ),
                        style = PaintedStyle(color = DescriptionInk, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center),
                        maxLines = 1
                    )
                    claimable -> Text(
                        text = stringResource(R.string.achievement_claim_button),
                        style = PaintedStyle(color = Color(0xFFB8740A), fontSize = 12.sp, textAlign = TextAlign.Center),
                        maxLines = 1
                    )
                    else -> Text(
                        text = stringResource(R.string.achievement_claimed_label),
                        style = PaintedStyle(color = Color(0xFF14602A), fontSize = 11.sp, textAlign = TextAlign.Center),
                        maxLines = 1
                    )
                }
                RewardPill(item.achievement)
            }
        }
        if (item.claimed) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 4.dp)
                    .size(22.dp)
                    .shadow(2.dp, CircleShape)
                    .background(Brush.verticalGradient(listOf(Color(0xFF5FD068), Color(0xFF2EA043))), CircleShape)
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    androidx.compose.material.icons.Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        if (burst.value > 0f) {
            val p = burst.value
            val rewardTint = rewardColor(item.achievement)
            ClaimBurstEffects(progress = p, ringColor = rewardTint, modifier = Modifier.matchParentSize())
            // The reward rides up on a dark pill with a small overshoot, holds,
            // then fades — big enough to actually read on a phone.
            val rise = (1f - (1f - (p * 1.25f).coerceAtMost(1f)).let { it * it * it })
            Text(
                text = "+" + rewardText(item.achievement).trimStart('+'),
                // Plain, unscaled text kept inside the chip's own width — a glow or a scale-up here
                // used to be cut off at the sides by the text's own bounds.
                style = MaterialTheme.typography.titleSmall.copy(
                    shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = 0.25f), androidx.compose.ui.geometry.Offset(0f, 2f), 2f)
                ),
                fontWeight = FontWeight.ExtraBold,
                color = rewardTint,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .offset(y = (-30 * rise).dp)
                    // Fades out smoothly over the back two thirds of the animation
                    // instead of holding and then vanishing.
                    .alpha(1f - androidx.compose.animation.core.FastOutSlowInEasing.transform(((p - 0.3f) / 0.7f).coerceIn(0f, 1f)))
            )
        }
    }
}

/** What a badge pays: blue for XP, orange for gold. */
@Composable
private fun RewardPill(achievement: Achievement) {
    val gold = achievement.rewardType == AchievementRewardType.GOLD
    val top = if (gold) Color(0xFFFFA64D) else Color(0xFF4CB3FF)
    val bottom = if (gold) ButtonOrange else Color(0xFF1B7FE0)
    val edge = if (gold) Color(0xFFB04A0E) else Color(0xFF0E4FA0)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(20.dp)
            .background(Brush.verticalGradient(listOf(top, bottom)), PillShape)
            .border(1.2.dp, edge, PillShape)
            .padding(horizontal = 8.dp)
    ) {
        LetteredText(rewardText(achievement), 11.sp, outline = edge)
    }
}

/**
 * The confetti-and-shockwave half of the claim celebration: a ring that
 * expands and fades, and a fan of small sparks that burst outward and fall
 * under a little gravity. Deterministic (fixed angles and distances), so it
 * looks the same every time and costs a single Canvas.
 */
@Composable
private fun ClaimBurstEffects(progress: Float, ringColor: Color, modifier: Modifier = Modifier) {
    val sparks = remember {
        val palette = listOf(
            Color(0xFFFFC94D), Color(0xFFFF7A1A), Color(0xFFFF5C8A),
            Color(0xFF4CD27A), Color(0xFF4FACFE), Color(0xFFFFFFFF)
        )
        List(18) { i ->
            val angle = (i * 360f / 18f + (i % 3) * 7f) * (PI.toFloat() / 180f)
            Spark(
                angle = angle,
                distance = 42f + (i % 4) * 14f,
                size = 3.2f + (i % 3) * 1.6f,
                color = palette[i % palette.size],
                square = i % 2 == 0
            )
        }
    }
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
        val out = 1f - (1f - progress).let { it * it * it }
        // Sparks.
        val fade = 1f - androidx.compose.animation.core.FastOutSlowInEasing.transform(((progress - 0.3f) / 0.7f).coerceIn(0f, 1f))
        sparks.forEach { spark ->
            val r = spark.distance.dp.toPx() * out
            val x = center.x + kotlin.math.cos(spark.angle) * r
            val y = center.y + kotlin.math.sin(spark.angle) * r + 26.dp.toPx() * progress * progress
            val s = spark.size.dp.toPx() * (1f - 0.4f * progress)
            drawCircle(color = spark.color.copy(alpha = fade), radius = s, center = androidx.compose.ui.geometry.Offset(x, y))
        }
    }
}

private class Spark(
    val angle: Float,
    val distance: Float,
    val size: Float,
    val color: Color,
    val square: Boolean
)
