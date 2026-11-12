package com.sualtikasifi.cizimhafiza.presentation.achievements

import com.sualtikasifi.cizimhafiza.presentation.common.pressable
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.RoundedCornerShape
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
    // The first rows slide in when the page opens; rows scrolled into view later must simply be there.
    var listIntro by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1_500)
        listIntro = false
    }
    val unlockedCount = achievements.count { it.unlocked }
    // Tapping a chip explains what it takes to earn it.
    var selectedAchievement by remember { mutableStateOf<AchievementUiItem?>(null) }
    // A reward just collected stays in the top section for a moment so its celebration can play, then drops to the shelf.
    var justClaimed by remember { mutableStateOf(emptySet<String>()) }
    LaunchedEffect(justClaimed) {
        if (justClaimed.isNotEmpty()) {
            delay(1_500)
            justClaimed = emptySet()
        }
    }
    val toCollect = achievements.filter { it.unlocked && (!it.claimed || it.achievement.name in justClaimed) }
    val earned = achievements.filter { it.unlocked && it.claimed && it.achievement.name !in justClaimed }
    // Closest to done first: the next badge within reach is always the first one in the list.
    val inProgress = achievements.filter { !it.unlocked }.sortedByDescending { it.fraction }

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
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            item(key = "header") {
                AchievementProgressHeader(unlockedCount = unlockedCount, total = achievements.size, waiting = toCollect.size)
            }
            if (toCollect.isNotEmpty()) {
                item(key = "h_claim") {
                    SectionHeader(
                        title = stringResource(R.string.achievements_section_claim), count = toCollect.size, accent = true,
                        action = if (toCollect.count { !it.claimed } > 1) stringResource(R.string.achievements_claim_all) else null,
                        onAction = {
                            justClaimed = justClaimed + toCollect.filter { !it.claimed }.map { it.achievement.name }
                            viewModel.claimAll(toCollect.filter { !it.claimed }.map { it.achievement })
                        },
                        modifier = Modifier.animateItem()
                    )
                }
                items(toCollect, key = { "c_" + it.achievement.name }) { item ->
                    AchievementRow(
                        item = item, celebrating = item.claimed,
                        onClick = {
                            if (!item.claimed) {
                                justClaimed = justClaimed + item.achievement.name
                                viewModel.claim(item.achievement)
                            }
                        },
                        modifier = Modifier.animateItem()
                    )
                }
            }
            if (inProgress.isNotEmpty()) {
                item(key = "h_progress") {
                    SectionHeader(title = stringResource(R.string.achievements_section_progress), count = inProgress.size, modifier = Modifier.animateItem())
                }
                items(inProgress, key = { "p_" + it.achievement.name }) { item ->
                    AchievementRow(item = item, celebrating = false, onClick = { selectedAchievement = item }, modifier = Modifier.animateItem())
                }
            }
            if (earned.isNotEmpty()) {
                item(key = "h_done") {
                    SectionHeader(title = stringResource(R.string.achievements_section_done), count = earned.size, modifier = Modifier.animateItem())
                }
                // The trophy shelf: every collected badge as a gold medal, four to a row.
                itemsIndexed(earned.chunked(4), key = { i, _ -> "e_$i" }) { _, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { item ->
                            MedalCell(item = item, onClick = { selectedAchievement = item }, modifier = Modifier.weight(1f))
                        }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
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
                modifier = Modifier.fillMaxWidth().offset(x = signWidth * 0.016f, y = signHeight * 0.432f).height(signHeight * 0.36f),
                contentAlignment = Alignment.Center
            ) {
                LetteredText(stringResource(R.string.menu_achievements), (len(64f).value).sp, title = true)
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
                .pressable(pressedScale = 0.88f, onClick = onBack)
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
private fun AchievementProgressHeader(unlockedCount: Int, total: Int, waiting: Int, modifier: Modifier = Modifier) {
    val fraction = if (total > 0) unlockedCount.toFloat() / total else 0f
    NinePatch(
        res = R.drawable.ach_panel,
        slicePx = 150,
        edge = 34.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 50.dp, end = 50.dp, top = 40.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Palette and trophy at the two ends, a laurel branch on each side of the count.
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Image(painterResource(R.drawable.ach_hero_palette), null, Modifier.height(54.dp))
                Spacer(Modifier.weight(0.25f))
                Image(painterResource(R.drawable.ach_laurel_l), null, Modifier.height(44.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.achievements_progress_count_format, unlockedCount, total),
                        style = PaintedStyle(color = InkBrown, fontSize = 30.sp, textAlign = TextAlign.Center),
                        maxLines = 1
                    )
                    Text(
                        text = stringResource(R.string.achievements_progress_caption),
                        style = PaintedStyle(color = DescriptionInk, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center),
                        maxLines = 1
                    )
                    if (waiting > 0) Text(
                        text = stringResource(R.string.achievements_waiting_format, waiting),
                        style = PaintedStyle(color = Color(0xFFB8740A), fontSize = 14.sp, textAlign = TextAlign.Center),
                        maxLines = 1
                    )
                }
                Image(painterResource(R.drawable.ach_laurel_r), null, Modifier.height(44.dp))
                Spacer(Modifier.weight(0.25f))
                Image(painterResource(R.drawable.ach_hero_trophy), null, Modifier.height(54.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            // The bar carries the percentage inside it.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth().height(22.dp).background(Color(0xFFE2CDAA), PillShape).border(1.5.dp, Color(0xFFB9976A), PillShape)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxWidth(fraction.coerceIn(0.04f, 1f))
                        .height(22.dp)
                        .background(Brush.verticalGradient(listOf(Color(0xFFFFD04D), Color(0xFFF5A300))), PillShape)
                )
                LetteredText(stringResource(R.string.achievements_progress_percent, (fraction * 100).toInt()), 13.sp, outline = Color(0xFF8A4A00))
            }
        }
    }
}

@Composable
private fun rewardText(achievement: Achievement): String = when (achievement.rewardType) {
    AchievementRewardType.XP -> stringResource(R.string.achievement_xp_reward, achievement.xpReward)
    AchievementRewardType.GOLD -> stringResource(R.string.achievement_gold_reward, achievement.goldReward)
}

/**
 * The achievement window on its painted card (ach_dialog, 1016 x 1394 px): the badge sits on the golden coin at the
 * top, the title is lettered on the red ribbon, and everything else is written on the paper below. Every position
 * below is a share of the card's real width, so the picture is never stretched.
 */
@Composable
private fun AchievementDetailDialog(item: AchievementUiItem, onClaim: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 18.dp), contentAlignment = Alignment.Center) {
            val cardW = minOf(maxWidth, 400.dp)
            val u = cardW / 1016f
            val ink = Color(0xFF3B2314)
            val fontScale0 = androidx.compose.ui.platform.LocalDensity.current.fontScale
            fun fs(px: Float) = (px * u.value / fontScale0).sp
            Box(Modifier.width(cardW).height(u * 1394f)) {
                Image(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.ach_dialog),
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.FillBounds,
                    modifier = Modifier.matchParentSize()
                )
                // The badge on the coin (the coin's face is about 215 px across, centred at 503 / 265).
                Box(
                    Modifier.offset(u * 398f, u * 160f).size(u * 210f, u * 210f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.achievement.emoji,
                        fontSize = fs(120f),
                        modifier = Modifier.alpha(if (item.unlocked) 1f else 0.45f)
                    )
                }
                // The title on the ribbon.
                com.sualtikasifi.cizimhafiza.presentation.common.LetteredText(
                    text = stringResource(item.achievement.titleRes),
                    size = fs(64f),
                    outline = Color(0xFF8A2A10),
                    modifier = Modifier.offset(u * 248f, u * 388f).size(u * 520f, u * 100f),
                    minScale = 0.5f
                )
                // Everything else on the paper (x 90..930, y 570..1320).
                Column(
                    modifier = Modifier.offset(u * 100f, u * 570f).size(u * 820f, u * 740f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    TintedBadge(
                        text = stringResource(
                            when {
                                item.unlocked && item.claimed -> R.string.achievement_claimed_label
                                item.unlocked -> R.string.achievement_unlocked_label
                                else -> R.string.achievement_locked_label
                            }
                        ),
                        container = if (item.unlocked) AppTheme.tokens.successContainer else Color(0xFFEFE3CF),
                        content = if (item.unlocked) AppTheme.tokens.success else Color(0xFF7A5A40)
                    )
                    if (!item.unlocked) {
                        Text(
                            text = stringResource(
                                R.string.achievement_progress_format,
                                item.currentValue.coerceAtMost(item.achievement.target),
                                item.achievement.target
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF7A5A40)
                        )
                    }
                    // The condition in its own inset panel.
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x66FFFFFF), RoundedCornerShape(18.dp))
                            .border(1.5.dp, Color(0x55C98543), RoundedCornerShape(18.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(
                                if (item.unlocked) R.string.achievement_condition_label else R.string.achievement_unlock_hint_label
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF7A5A40)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(item.achievement.descriptionRes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = ink,
                            textAlign = TextAlign.Center
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .background(AppTheme.tokens.gold.copy(alpha = 0.22f), PillShape)
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Text(text = "\uD83C\uDFC6", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = rewardText(item.achievement),
                            style = MaterialTheme.typography.titleMedium,
                            color = rewardColor(item.achievement)
                        )
                    }
                    if (item.unlocked && !item.claimed) {
                        PrimaryButton(
                            text = stringResource(R.string.achievement_claim_button),
                            onClick = onClaim,
                            // Narrower and lower than the page's usual button, so it stays clear of the two leaf
                            // clusters in the card's bottom corners.
                            modifier = Modifier.fillMaxWidth(0.62f),
                            height = 46.dp
                        )
                    } else {
                        PrimaryButton(
                            text = stringResource(R.string.close),
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(0.62f),
                            height = 46.dp
                        )
                    }
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

private val RowCream = Color(0xFFFFF8E8)
private val RowCreamDeep = Color(0xFFFBE9C2)
private val EdgeBrown = Color(0xFF9C6A38)
private val GoldTop = Color(0xFFFFE27A)
private val GoldBottom = Color(0xFFF2A91B)
private val GoldEdge = Color(0xFFA9680A)
private val MedalGreen = Color(0xFF2EA043)

private val AchievementUiItem.fraction: Float
    get() = (currentValue.toFloat() / achievement.target.coerceAtLeast(1)).coerceIn(0f, 1f)

/** A section's title on a small cream plank: the name, how many badges it holds, and optionally one action. */
@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    action: String? = null,
    onAction: () -> Unit = {}
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .shadow(3.dp, PillShape)
                .background(if (accent) Brush.verticalGradient(listOf(GoldTop, GoldBottom)) else Brush.verticalGradient(listOf(RowCream, RowCreamDeep)), PillShape)
                .border(2.dp, if (accent) GoldEdge else EdgeBrown, PillShape)
                .padding(start = 16.dp, end = 6.dp, top = 5.dp, bottom = 5.dp)
        ) {
            Text(title, style = PaintedStyle(color = if (accent) Color(0xFF5A2D00) else InkBrown, fontSize = 17.sp), maxLines = 1)
            Spacer(Modifier.width(8.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.defaultMinSize(minWidth = 28.dp, minHeight = 24.dp)
                    .background(if (accent) Color(0xFF5A2D00) else EdgeBrown, PillShape)
                    .padding(horizontal = 8.dp)
            ) {
                Text("$count", style = PaintedStyle(color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center), maxLines = 1)
            }
        }
        Spacer(Modifier.weight(1f))
        if (action != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .height(36.dp)
                    .shadow(3.dp, PillShape)
                    .background(Brush.verticalGradient(listOf(Color(0xFFFFA64D), ButtonOrange)), PillShape)
                    .border(1.5.dp, Color(0xFFB04A0E), PillShape)
                    .pressable(pressedScale = 0.94f, onClick = onAction)
                    .padding(horizontal = 16.dp)
            ) {
                LetteredText(action, 15.sp, outline = Color(0xFF8A3A00))
            }
        }
    }
}

/** The badge's round medallion: gold with a rim once earned, a quiet grey disc while it is still out of reach. */
@Composable
private fun Medallion(item: AchievementUiItem, size: androidx.compose.ui.unit.Dp, ring: Float = 0f) {
    val earned = item.unlocked
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .shadow(if (earned) 4.dp else 1.dp, CircleShape)
            .background(
                if (earned) Brush.verticalGradient(listOf(GoldTop, GoldBottom)) else Brush.verticalGradient(listOf(Color(0xFFEDE3D2), Color(0xFFD8CBB4))),
                CircleShape
            )
            .border(if (earned) 2.5.dp else 1.5.dp, if (earned) GoldEdge else Color(0xFFB8A88C), CircleShape)
            .padding(size * 0.1f)
            .background(if (earned) Color(0xFFFFF3C2) else Color(0xFFF6EFE2), CircleShape)
            .border(1.dp, if (earned) Color(0xFFFFFFFF) else Color(0x33FFFFFF), CircleShape)
    ) {
        Text(
            text = item.achievement.emoji,
            style = PaintedStyle(fontSize = (size.value * 0.46f).sp, textAlign = TextAlign.Center),
            modifier = Modifier.alpha(if (earned) 1f else 0.5f).graphicsLayer { scaleX = 1f + ring * 0.06f; scaleY = 1f + ring * 0.06f }
        )
    }
}

/**
 * One badge as a full-width row: the medallion, its name and a progress bar with the numbers, and its reward. An earned
 * but uncollected badge glows gold and offers "Ödülü topla" in place of the reward; [celebrating] plays the collect burst.
 */
@Composable
private fun AchievementRow(
    item: AchievementUiItem,
    celebrating: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val claimable = item.unlocked && !item.claimed
    val transition = rememberInfiniteTransition(label = "rowGlow")
    val glowAnim by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = androidx.compose.animation.core.FastOutSlowInEasing), RepeatMode.Reverse),
        label = "rowGlowValue"
    )
    val glow = if (claimable) glowAnim else 0f
    val shape = RoundedCornerShape(22.dp)
    val burst = remember(item.achievement.name) { Animatable(0f) }
    LaunchedEffect(celebrating) {
        if (celebrating) {
            burst.snapTo(0f)
            burst.animateTo(1f, tween(1400, easing = LinearEasing))
        }
    }
    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .shadow(if (claimable) (4 + 6 * glow).dp else 3.dp, shape, ambientColor = Color(0xFF6E3C0A), spotColor = Color(0xFF6E3C0A))
                .background(
                    if (claimable) Brush.verticalGradient(listOf(Color(0xFFFFF4C9), Color(0xFFFFE08A))) else Brush.verticalGradient(listOf(RowCream, RowCreamDeep)),
                    shape
                )
                .border(if (claimable) 3.dp else 2.dp, if (claimable) lerp(GoldEdge, GoldBottom, glow) else EdgeBrown, shape)
                .pressable(pressedScale = 0.97f, onClick = onClick)
                .padding(start = 10.dp, end = 12.dp, top = 10.dp, bottom = 10.dp)
        ) {
            Medallion(item, 56.dp, ring = glow)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(item.achievement.titleRes),
                    style = PaintedStyle(color = InkBrown, fontSize = 16.sp, lineHeight = 17.sp),
                    maxLines = 2
                )
                // How to earn it, right under the name: nobody should have to open a badge to find out.
                Text(
                    text = stringResource(item.achievement.descriptionRes),
                    style = PaintedStyle(color = DescriptionInk, fontSize = 12.sp, lineHeight = 13.sp, fontWeight = FontWeight.SemiBold),
                    maxLines = 2
                )
                if (item.unlocked) {
                    Text(
                        text = stringResource(if (item.claimed) R.string.achievement_claimed_label else R.string.achievement_unlocked_label),
                        style = PaintedStyle(color = if (item.claimed) Color(0xFF14602A) else Color(0xFFB8740A), fontSize = 13.sp),
                        maxLines = 1
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.weight(1f).height(10.dp).background(Color(0xFFE2CDAA), PillShape)
                        ) {
                            Box(
                                Modifier.fillMaxWidth(item.fraction.coerceAtLeast(0.04f)).height(10.dp)
                                    .background(Brush.verticalGradient(listOf(Color(0xFFFFD04D), Color(0xFFF5A300))), PillShape)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.achievement_progress_format, item.currentValue.coerceAtMost(item.achievement.target), item.achievement.target),
                            style = PaintedStyle(color = DescriptionInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                            maxLines = 1
                        )
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            if (claimable) CollectButton() else if (item.unlocked) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(30.dp).background(Brush.verticalGradient(listOf(Color(0xFF5FD068), MedalGreen)), CircleShape).border(1.5.dp, Color.White, CircleShape)
                ) {
                    androidx.compose.material3.Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            } else RewardChip(item.achievement)
        }
        if (celebrating && burst.value > 0f) {
            val p = burst.value
            ClaimBurstEffects(progress = p, ringColor = rewardColor(item.achievement), modifier = Modifier.matchParentSize())
            val rise = 1f - (1f - (p * 1.25f).coerceAtMost(1f)).let { it * it * it }
            Text(
                text = "+" + rewardText(item.achievement).trimStart('+'),
                style = MaterialTheme.typography.titleMedium.copy(shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = 0.3f), androidx.compose.ui.geometry.Offset(0f, 2f), 3f)),
                fontWeight = FontWeight.ExtraBold,
                color = rewardColor(item.achievement),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 14.dp)
                    .offset(y = (-22 * rise).dp)
                    .alpha(1f - androidx.compose.animation.core.FastOutSlowInEasing.transform(((p - 0.3f) / 0.7f).coerceIn(0f, 1f)))
            )
        }
    }
}

@Composable
private fun CollectButton() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(38.dp)
            .shadow(3.dp, PillShape)
            .background(Brush.verticalGradient(listOf(Color(0xFFFFA64D), ButtonOrange)), PillShape)
            .border(1.5.dp, Color(0xFFB04A0E), PillShape)
            .padding(horizontal = 14.dp)
    ) {
        LetteredText(stringResource(R.string.achievement_claim_button), 14.sp, outline = Color(0xFF8A3A00))
    }
}

/** What a badge pays: blue for XP, orange for gold. */
@Composable
private fun RewardChip(achievement: Achievement) {
    val gold = achievement.rewardType == AchievementRewardType.GOLD
    val top = if (gold) Color(0xFFFFA64D) else Color(0xFF4CB3FF)
    val bottom = if (gold) ButtonOrange else Color(0xFF1B7FE0)
    val edge = if (gold) Color(0xFFB04A0E) else Color(0xFF0E4FA0)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(28.dp)
            .background(Brush.verticalGradient(listOf(top, bottom)), PillShape)
            .border(1.5.dp, edge, PillShape)
            .padding(horizontal = 11.dp)
    ) {
        LetteredText(rewardText(achievement), 13.sp, outline = edge)
    }
}

/** An earned, collected badge on the trophy shelf: a gold medal and its name beneath. */
@Composable
private fun MedalCell(item: AchievementUiItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(Brush.verticalGradient(listOf(RowCream, RowCreamDeep)), RoundedCornerShape(18.dp))
            .border(2.dp, EdgeBrown, RoundedCornerShape(18.dp))
            .pressable(pressedScale = 0.94f, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp)
    ) {
        Medallion(item, 52.dp)
        Spacer(Modifier.height(5.dp))
        Text(
            text = stringResource(item.achievement.titleRes),
            style = PaintedStyle(color = InkBrown, fontSize = 11.sp, lineHeight = 12.sp, textAlign = TextAlign.Center),
            maxLines = 2,
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
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
