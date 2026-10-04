package com.sualtikasifi.cizimhafiza.presentation.league

import androidx.compose.foundation.Image
import androidx.compose.runtime.remember
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import com.sualtikasifi.cizimhafiza.presentation.common.ButtonOrange
import com.sualtikasifi.cizimhafiza.presentation.common.DescriptionInk
import com.sualtikasifi.cizimhafiza.presentation.common.DescriptionStyle
import com.sualtikasifi.cizimhafiza.presentation.common.InkBrown
import com.sualtikasifi.cizimhafiza.presentation.common.LetteredText
import com.sualtikasifi.cizimhafiza.presentation.common.NinePatch
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedStyle
import com.sualtikasifi.cizimhafiza.presentation.common.PillShape
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.sualtikasifi.cizimhafiza.presentation.theme.AppTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.animation.togetherWith
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sualtikasifi.cizimhafiza.R
import java.time.LocalDate
import com.sualtikasifi.cizimhafiza.domain.model.AvatarFrame
import com.sualtikasifi.cizimhafiza.domain.model.LeagueEntry
import com.sualtikasifi.cizimhafiza.domain.model.LeaguePeriod
import com.sualtikasifi.cizimhafiza.domain.model.LeagueReward
import com.sualtikasifi.cizimhafiza.domain.model.LeagueTable
import com.sualtikasifi.cizimhafiza.domain.model.PenSkin
import com.sualtikasifi.cizimhafiza.util.GameConstants
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.sualtikasifi.cizimhafiza.presentation.common.RankLevelLabel
import com.sualtikasifi.cizimhafiza.presentation.common.LevelAvatar
import com.sualtikasifi.cizimhafiza.presentation.common.PrimaryButton
import com.sualtikasifi.cizimhafiza.presentation.common.WarmCard
import com.sualtikasifi.cizimhafiza.presentation.common.SecondaryButton
import com.sualtikasifi.cizimhafiza.presentation.common.SelectableChip
import com.sualtikasifi.cizimhafiza.presentation.common.penBrush
import com.sualtikasifi.cizimhafiza.presentation.common.TintedBadge
import com.sualtikasifi.cizimhafiza.presentation.common.EmptyState
import com.sualtikasifi.cizimhafiza.presentation.common.LoadingRows
import com.sualtikasifi.cizimhafiza.presentation.common.RaisedIconButton
import com.sualtikasifi.cizimhafiza.presentation.common.ScreenTopActions
import com.sualtikasifi.cizimhafiza.presentation.common.TopActionsClearance
import com.sualtikasifi.cizimhafiza.presentation.common.screenBackground

private const val ArtW = 1080f
private const val ArtH = 2401f

/**
 * Two leaderboards that reset on the first of every month — see
 * domain.model.LeaguePeriod for why a month, and why not lifetime.
 *
 * The friends table is built on this device from each friend's profile; the
 * global one is a single document published by a scheduled function every
 * hour (see functions/src/index.ts). That difference is visible on
 * purpose: the global tab says when it was last rebuilt, because a table
 * that is not live should not pretend to be.
 *
 * Painted trophy-room scene: the sign, tabs and reset timer stay pinned; the prize, the player's standing and the rows
 * scroll beneath them (a 20-row table cannot fit one screen) and melt away at their top edge.
 */
@Composable
fun LeagueScreen(
    onBack: () -> Unit,
    onFriends: () -> Unit = {},
    viewModel: LeagueViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val table = uiState.table
    val shownTable = if (uiState.tab == LeagueTab.Friends) table else uiState.global?.table

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val sc = maxOf(widthPx / ArtW, heightPx / ArtH)
        val offY = 0f
        fun yOf(px: Float): Dp = with(density) { (offY + px * sc).toDp() }
        fun len(px: Float): Dp = with(density) { (px * sc).toDp() }

        Image(
            painter = painterResource(R.drawable.bg_league),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = androidx.compose.ui.Alignment.TopCenter,
            modifier = Modifier.fillMaxSize()
        )

        // ── Scrolling part ──
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = yOf(655f))
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    val fade = 22.dp.toPx().coerceAtMost(size.height)
                    drawRect(
                        brush = Brush.verticalGradient(colorStops = arrayOf(0f to Color.Transparent, (fade / size.height) to Color.Black, 1f to Color.Black)),
                        blendMode = BlendMode.DstIn
                    )
                },
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Global only — a friend-list standing has no monthly prize of its own to show.
            if (uiState.tab == LeagueTab.Global) {
                item(key = "reward") {
                    // Falls back to the month's own frame so the prize is on screen from the first day.
                    val reward = LeagueReward.find(uiState.global?.rewardId)
                        ?: LeagueReward.forPeriod(LeaguePeriod.periodIdFor(com.sualtikasifi.cizimhafiza.util.TurkeyTime.today()))
                    reward?.let { RewardBanner(reward = it) }
                }
                item(key = "xp") {
                    val myXp by viewModel.myPeriodXp.collectAsState()
                    val others = uiState.global?.table?.entries.orEmpty().filterNot { it.isMe }.take(GLOBAL_VISIBLE_ROWS)
                    MonthlyXpCard(
                        myXp = myXp,
                        myRank = uiState.myGlobalRank?.takeIf { it <= GLOBAL_VISIBLE_ROWS },
                        top20Xp = others.getOrNull(GLOBAL_VISIBLE_ROWS - 1)?.periodXp,
                        podiumXp = others.getOrNull(2)?.periodXp
                    )
                }
            }

            // Alone on the friends table: say so, and offer the fix.
            if (uiState.tab == LeagueTab.Friends && !uiState.isLoading && (shownTable?.entries?.size ?: 0) <= 1) {
                item(key = "friends-hint") {
                    PaperPanel(modifier = Modifier.clickable(onClick = onFriends)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(text = "🤝", style = PaintedStyle(fontSize = 28.sp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.league_friends_hint),
                                    style = DescriptionStyle(13.sp, 17.sp).copy(textAlign = TextAlign.Start)
                                )
                                Text(
                                    text = stringResource(R.string.league_friends_hint_action),
                                    style = PaintedStyle(color = ButtonOrange, fontSize = 14.sp, textAlign = TextAlign.Start)
                                )
                            }
                        }
                    }
                }
            }

            when {
                // Row-shaped placeholders rather than a centred spinner: the wait should look like the table arriving.
                uiState.tab == LeagueTab.Friends && uiState.isLoading ->
                    item(key = "loading") { LoadingRows(count = 5, height = 50.dp) }
                uiState.tab == LeagueTab.Global && uiState.globalLoading && uiState.global == null ->
                    item(key = "loading") { LoadingRows(count = 5, height = 50.dp) }
                uiState.tab == LeagueTab.Global && uiState.globalFailed && uiState.global == null ->
                    item(key = "failed") {
                        PaperPanel {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Text("📡", style = PaintedStyle(fontSize = 30.sp))
                                Text(stringResource(R.string.league_global_failed), style = DescriptionStyle(14.sp, 18.sp))
                                Spacer(Modifier.height(10.dp))
                                SecondaryButton(
                                    text = stringResource(R.string.reports_load_more),
                                    onClick = viewModel::refreshGlobal,
                                    icon = Icons.Filled.Refresh
                                )
                            }
                        }
                    }
                shownTable == null || shownTable.entries.isEmpty() ->
                    item(key = "empty") {
                        PaperPanel {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Text("🏅", style = PaintedStyle(fontSize = 30.sp))
                                Text(
                                    stringResource(if (uiState.tab == LeagueTab.Friends) R.string.league_empty else R.string.league_global_empty),
                                    style = DescriptionStyle(14.sp, 18.sp)
                                )
                            }
                        }
                    }
                else -> {
                    // Global: at most 20 rows. The player appears in them only when their XP earns a place in the
                    // top 20, at the spot it earns; anyone further back is not listed and is told so on the card.
                    val visibleEntries = if (uiState.tab == LeagueTab.Global) {
                        val others = shownTable.entries.filterNot { it.isMe }
                        val ownRank = uiState.myGlobalRank
                        val ownEntry = uiState.myGlobalEntry
                        if (ownRank != null && ownEntry != null && ownRank <= GLOBAL_VISIBLE_ROWS) {
                            others.toMutableList().apply { add((ownRank - 1).coerceAtMost(size), ownEntry) }.take(GLOBAL_VISIBLE_ROWS)
                        } else {
                            others.take(GLOBAL_VISIBLE_ROWS)
                        }
                    } else {
                        shownTable.entries
                    }
                    itemsIndexed(visibleEntries, key = { _, entry -> entry.uid }) { index, entry ->
                        LeagueRow(rank = index + 1, entry = entry, showTotalXp = uiState.tab == LeagueTab.Friends)
                    }
                    if (uiState.tab == LeagueTab.Global) {
                        item(key = "rebuilt-note") {
                            LetteredText(
                                text = stringResource(R.string.league_global_refresh_note),
                                size = 12.sp,
                                weight = FontWeight.Bold,
                                maxLines = 3,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, start = 8.dp, end = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // ── Pinned part ──
        val signWidth = maxWidth * 0.64f
        val signHeight = signWidth * (522f / 1199f)
        Box(
            modifier = Modifier.align(Alignment.TopCenter).offset(y = yOf(96f)).width(signWidth).height(signHeight),
            contentAlignment = Alignment.TopCenter
        ) {
            Image(
                painter = painterResource(R.drawable.league_sign),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier.fillMaxWidth().offset(y = signHeight * 0.32f).height(signHeight * 0.36f),
                contentAlignment = Alignment.Center
            ) {
                val title = stringResource(R.string.league_title)
                LetteredText(title, len(if (title.length > 11) 52f else 66f).value.sp)
            }
        }

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
        if (uiState.tab == LeagueTab.Global) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(end = 16.dp, top = 12.dp)
                    .size(56.dp)
                    .alpha(if (uiState.globalLoading) 0.55f else 1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = !uiState.globalLoading,
                        onClick = viewModel::refreshGlobal
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(painterResource(R.drawable.league_wood), contentDescription = null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = stringResource(R.string.reports_refresh),
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        // Tabs.
        Row(
            modifier = Modifier.fillMaxWidth().offset(y = yOf(430f)).padding(horizontal = 26.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LeagueTab.entries.forEach { tab ->
                val selected = uiState.tab == tab
                NinePatch(
                    res = if (selected) R.drawable.league_pill_on else R.drawable.league_pill_off,
                    slicePx = 80,
                    edge = 20.dp,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { viewModel.selectTab(tab) }
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (selected) {
                            LetteredText(stringResource(tab.labelRes()), 17.sp)
                        } else {
                            Text(stringResource(tab.labelRes()), style = PaintedStyle(color = InkBrown, fontSize = 17.sp, textAlign = TextAlign.Center))
                        }
                    }
                }
            }
        }

        // Reset timer / caption.
        val timerText: String? = if (uiState.tab == LeagueTab.Friends) {
            // The friends board is all-time; only the global one resets each month.
            stringResource(R.string.league_friends_total_caption)
        } else shownTable?.let {
            if (it.daysRemaining <= 0) {
                stringResource(R.string.league_resets_countdown, rememberResetCountdown())
            } else {
                stringResource(if (it.daysRemaining == 1) R.string.league_resets_in_one else R.string.league_resets_in, it.daysRemaining)
            }
        }
        if (timerText != null) {
            NinePatch(
                res = R.drawable.league_timer,
                slicePx = 135,
                edge = 31.dp,
                sliceYPx = 40,
                edgeY = 9.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = yOf(556f))
                    .height(38.dp)
                    .widthIn(min = 190.dp)
            ) {
                Box(
                    modifier = Modifier.padding(start = 40.dp, end = 18.dp).heightIn(min = 38.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = timerText,
                        style = PaintedStyle(color = InkBrown, fontSize = 13.sp, textAlign = TextAlign.Center),
                        maxLines = 1
                    )
                }
            }
        }

        uiState.justWon?.let { prize ->
            PrizeWonDialog(
                reward = prize.reward,
                rank = uiState.global?.myLastPeriodWin?.rank ?: 0,
                onDismiss = viewModel::dismissPrize
            )
        }
    }
}

/** A cream painted panel that hugs its content. */
@Composable
private fun PaperPanel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    NinePatch(
        res = R.drawable.league_card,
        slicePx = 100,
        edge = 26.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(Modifier.padding(horizontal = 24.dp, vertical = 18.dp)) { content() }
    }
}

/**
 * This month's prize, shown above the global table so the contest has a point. A pen reward gets a full-width painted
 * stroke below the label; a frame reward shows its real artwork.
 */
@Composable
private fun RewardBanner(reward: LeagueReward, modifier: Modifier = Modifier) {
    val number = java.text.NumberFormat.getIntegerInstance(java.util.Locale.forLanguageTag("tr"))
    val goldInk = Color(0xFFC77A00)
    NinePatch(
        res = R.drawable.league_banner,
        slicePx = 100,
        edge = 26.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (reward is LeagueReward.Frame) {
                RewardSwatch(reward = reward, size = 30.dp)
            } else {
                Text(text = "🏆", style = PaintedStyle(fontSize = 34.sp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.league_reward_title),
                    style = PaintedStyle(color = DescriptionInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                    maxLines = 1
                )
                Text(
                    text = rewardLabel(reward),
                    style = PaintedStyle(color = InkBrown, fontSize = 22.sp),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                if (reward is LeagueReward.Pen) {
                    PenStrokePreview(skin = reward.skin, modifier = Modifier.fillMaxWidth().height(20.dp))
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.league_reward_bonus_gold, number.format(GameConstants.LEAGUE_MONTHLY_GOLD)),
                    style = PaintedStyle(color = goldInk, fontSize = 13.sp),
                    maxLines = 1
                )
                Text(
                    text = stringResource(R.string.league_reward_bonus_xp, number.format(GameConstants.LEAGUE_MONTHLY_XP)),
                    style = PaintedStyle(color = ButtonOrange, fontSize = 13.sp),
                    maxLines = 1
                )
                Text(
                    text = stringResource(R.string.league_reward_top3),
                    style = PaintedStyle(color = DescriptionInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * "Where do I stand this month": the player's own monthly XP, their real rank, and how much more it
 * takes to reach the visible top 20 and the prize places. The cut-offs are the XP of the 20th and 3rd rows of the
 * published table.
 */
@Composable
private fun MonthlyXpCard(myXp: Int, myRank: Int?, top20Xp: Int?, podiumXp: Int?, modifier: Modifier = Modifier) {
    val number = java.text.NumberFormat.getIntegerInstance(java.util.Locale.forLanguageTag("tr"))
    val inPodium = myRank != null && myRank <= 3
    val inTop20 = myRank != null && myRank <= GLOBAL_VISIBLE_ROWS
    val toTop20 = ((top20Xp ?: 0) - myXp + 1).coerceAtLeast(1)
    val toPodium = ((podiumXp ?: 0) - myXp + 1).coerceAtLeast(1)
    val note = DescriptionStyle(13.sp, 17.sp).copy(textAlign = TextAlign.Start)
    NinePatch(
        res = R.drawable.league_card,
        slicePx = 100,
        edge = 26.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.league_monthly_title),
                        style = PaintedStyle(color = InkBrown, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Start),
                        maxLines = 1
                    )
                    Text(
                        text = stringResource(R.string.league_xp_format, myXp),
                        style = PaintedStyle(color = ButtonOrange, fontSize = 28.sp, textAlign = TextAlign.Start),
                        maxLines = 1
                    )
                }
                Text(
                    text = if (myRank != null) stringResource(R.string.league_monthly_rank, myRank)
                    else stringResource(R.string.league_monthly_unranked),
                    style = PaintedStyle(color = InkBrown, fontSize = 15.sp, textAlign = TextAlign.End),
                    maxLines = 2,
                    modifier = Modifier.widthIn(max = 150.dp)
                )
            }
            if (top20Xp != null && !inTop20) {
                Spacer(modifier = Modifier.height(6.dp))
                val fraction = (myXp.toFloat() / top20Xp.coerceAtLeast(1)).coerceIn(0f, 1f)
                Box(Modifier.fillMaxWidth().height(10.dp).background(Color(0xFFE2CDAA), PillShape)) {
                    Box(
                        Modifier
                            .fillMaxWidth(fraction)
                            .height(10.dp)
                            .background(Brush.verticalGradient(listOf(Color(0xFFFF9A3C), ButtonOrange)), PillShape)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = when {
                    inPodium -> stringResource(R.string.league_monthly_in_podium)
                    inTop20 -> stringResource(R.string.league_monthly_in_top, GLOBAL_VISIBLE_ROWS) + " · " +
                        stringResource(R.string.league_monthly_to_podium, number.format(toPodium))
                    else -> stringResource(R.string.league_monthly_to_top, GLOBAL_VISIBLE_ROWS, number.format(toTop20))
                },
                style = note
            )
            if (!inTop20 && podiumXp != null) {
                Text(text = stringResource(R.string.league_monthly_to_podium, number.format(toPodium)), style = note)
            }
        }
    }
}

/** How many rows of the global table are drawn: the published list, minus the player's own row, capped here. */
private const val GLOBAL_VISIBLE_ROWS = 20

/**
 * A wide, hand-drawn-looking curve painted in the pen's own brush — the same
 * shape [PenSkinPickerSheet]'s swatches use, so a gradient reads as the
 * actual sweep the winner will draw with rather than a short straight line.
 *
 * The curve draws itself on in a loop rather than sitting there fully
 * painted: a reward the player hasn't won yet is a preview, not a finished
 * picture, and drawing is the one thing a pen skin is for.
 */
@Composable
private fun PenStrokePreview(skin: PenSkin, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "pen-stroke-preview")
    val cycle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pen-stroke-cycle"
    )
    // The cycle is one 0..1 sweep split into three feels: draw on (0-65%),
    // hold so the finished stroke actually registers (65-80%), fade before
    // the next pass starts (80-100%) — a hard restart read as a glitch.
    val drawFraction = (cycle / 0.65f).coerceIn(0f, 1f)
    val fadeFraction = ((cycle - 0.8f) / 0.2f).coerceIn(0f, 1f)
    val strokeAlpha = 1f - fadeFraction
    Canvas(modifier = modifier) {
        val path = Path().apply {
            moveTo(size.width * 0.04f, size.height * 0.75f)
            cubicTo(
                size.width * 0.28f, size.height * 0.05f,
                size.width * 0.60f, size.height * 1.05f,
                size.width * 0.96f, size.height * 0.25f
            )
        }
        val measure = PathMeasure().apply { setPath(path, false) }
        val drawnPath = Path()
        measure.getSegment(0f, measure.length * drawFraction, drawnPath, startWithMoveTo = true)
        drawPath(
            path = drawnPath,
            brush = penBrush(skin, size.width, size.height),
            alpha = strokeAlpha,
            style = Stroke(width = size.minDimension * 0.14f, cap = StrokeCap.Round)
        )
    }
}

/**
 * What a frame prize actually looks like — the real artwork via
 * [LevelAvatar], since (unlike a pen) there is no gradient a static swatch
 * would otherwise flatten. Pen rewards get [PenStrokePreview] instead.
 *
 * A soft gold halo breathes behind it and the frame itself gently bobs in
 * size — the same "not won yet, but look" energy as the pen's self-drawing
 * curve, so neither reward preview reads as a plain product photo.
 */
@Composable
private fun RewardSwatch(reward: LeagueReward.Frame, size: androidx.compose.ui.unit.Dp) {
    val transition = rememberInfiniteTransition(label = "frame-reward-preview")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "frame-pulse"
    )
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size * 1.7f)) {
        Box(
            modifier = Modifier
                .size(size * (1.35f + 0.15f * pulse))
                .background(
                    Brush.radialGradient(
                        listOf(AppTheme.tokens.gold.copy(alpha = 0.16f + 0.22f * pulse), Color.Transparent)
                    ),
                    CircleShape
                )
        )
        LevelAvatar(
            level = 1,
            frame = reward.frame,
            size = size,
            modifier = Modifier.scale(0.96f + 0.08f * pulse)
        )
    }
}

/**
 * Every podium month now also pays a flat gold+XP bonus on top of the pen/
 * frame — see GameConstants.LEAGUE_MONTHLY_GOLD/XP and
 * LeagueViewModel.collectPrize, which is where it's actually granted. Shown
 * next to the cosmetic preview in both the not-yet-won banner and the
 * just-won dialog so the reward is never a surprise the player has to
 * discover in their balance afterwards.
 */
@Composable
private fun MonthlyBonusRow(modifier: Modifier = Modifier) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        TintedBadge(
            text = stringResource(R.string.league_reward_bonus_gold, java.text.NumberFormat.getIntegerInstance(java.util.Locale.forLanguageTag("tr")).format(GameConstants.LEAGUE_MONTHLY_GOLD)),
            container = AppTheme.tokens.gold.copy(alpha = 0.18f),
            content = AppTheme.tokens.gold
        )
        TintedBadge(
            text = stringResource(R.string.league_reward_bonus_xp, java.text.NumberFormat.getIntegerInstance(java.util.Locale.forLanguageTag("tr")).format(GameConstants.LEAGUE_MONTHLY_XP)),
            container = MaterialTheme.colorScheme.primaryContainer,
            content = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun rewardLabel(reward: LeagueReward): String = when (reward) {
    is LeagueReward.Pen -> stringResource(reward.skin.labelRes)
    // Frames have never been named anywhere in the app — the artwork is the
    // label — so the prize is described by its kind, plus the month it
    // belongs to, which is the one thing that tells them apart.
    is LeagueReward.Frame -> listOfNotNull(
        stringResource(R.string.league_reward_kind_frame),
        reward.periodLabel
    ).joinToString(" · ")
}

/**
 * "Global sıralamada ay sonunda ilk 3'e gir, Ayaz Kalemi'ni kazan!" — names
 * the actual prize rather than saying "bu ödülü" (this prize), which read as
 * filler beside a card that was already showing the prize right above it.
 * Turkish possessive/accusative suffixes ("Kalemi'ni", "Çerçevesi'ni") are
 * fixed per reward TYPE regardless of the specific skin/month, so this stays
 * two plain string templates rather than a general grammar rule.
 */
@Composable
private fun rewardExplainer(reward: LeagueReward): String = when (reward) {
    is LeagueReward.Pen -> stringResource(R.string.league_reward_explainer_pen, stringResource(reward.skin.labelRes))
    is LeagueReward.Frame -> {
        val monthLabel = reward.periodLabel?.let { LeaguePeriod.monthYearLabel(it) }
            ?: stringResource(R.string.league_reward_kind_frame)
        stringResource(R.string.league_reward_explainer_frame, monthLabel)
    }
}

/** Shown once, the first time a won prize is actually handed over. */
@Composable
private fun PrizeWonDialog(reward: LeagueReward?, rank: Int, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            PrimaryButton(text = stringResource(R.string.close), onClick = onDismiss)
        },
        title = { Text(text = stringResource(R.string.league_prize_won_title, rank)) },
        text = {
            Column {
                if (reward is LeagueReward.Pen) {
                    PenStrokePreview(skin = reward.skin, modifier = Modifier.fillMaxWidth().height(40.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                }
                if (reward != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (reward is LeagueReward.Frame) {
                            RewardSwatch(reward = reward, size = 44.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        Text(
                            text = stringResource(R.string.league_prize_won_body, rewardLabel(reward)),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
                MonthlyBonusRow()
            }
        }
    )
}

private fun LeagueTab.labelRes(): Int = when (this) {
    LeagueTab.Friends -> R.string.league_tab_friends
    LeagueTab.Global -> R.string.league_tab_global
}

/**
 * Medal colors per podium place — a border/icon accent and a matching, fully
 * opaque pastel card face for each. The face was a 12%-alpha tint of the
 * accent at first, which read as barely-there grey smudges rather than gold/
 * silver/bronze; a solid pastel plus a deeper accent circle behind the medal
 * emoji is what actually reads as colorful at a glance.
 */
private val SilverAccent = androidx.compose.ui.graphics.Color(0xFF8B94A3)
private val BronzeAccent = androidx.compose.ui.graphics.Color(0xFFB9713F)
private val GoldFace = androidx.compose.ui.graphics.Color(0xFFFFF0C2)
private val SilverFace = androidx.compose.ui.graphics.Color(0xFFE7EAF0)
private val BronzeFace = androidx.compose.ui.graphics.Color(0xFFF7DFC9)

/** Row sprites are one picture each (they carry the crown and the avatar ring), so they keep their proportions. */
private const val PodiumAspect = 1402f / 215f
/** Where the avatar ring sits inside the row picture, as a fraction of its width. */
private const val AvatarCentre = 0.181f

@Composable
private fun LeagueRow(rank: Int, entry: LeagueEntry, showTotalXp: Boolean = false) {
    val sprite = when (rank) {
        1 -> R.drawable.league_row1
        2 -> R.drawable.league_row2
        3 -> R.drawable.league_row3
        else -> null
    }
    // A glow around the row's own frame is what catches a scrolling eye, so the "me" row gets one behind its card.
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val rowWidth = maxWidth
        val rowHeight = if (sprite != null) rowWidth / PodiumAspect else 46.dp
        val avatarSize = if (sprite != null) rowHeight * 0.74f else 34.dp
        Box(modifier = Modifier.fillMaxWidth().height(rowHeight)) {
            if (entry.isMe) {
                MeRowGlow(corner = 18.dp, modifier = Modifier.matchParentSize())
            }
            if (sprite != null) {
                Image(painterResource(sprite), contentDescription = null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
            } else {
                NinePatch(R.drawable.league_row, slicePx = 70, edge = 20.dp, modifier = Modifier.fillMaxSize())
                Box(Modifier.width(rowWidth * 0.125f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.league_rank_format, rank),
                        style = PaintedStyle(color = DescriptionInk, fontSize = 16.sp, textAlign = TextAlign.Center)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .offset(x = rowWidth * AvatarCentre - avatarSize / 2)
                    .size(avatarSize)
                    .align(Alignment.CenterStart)
            ) {
                LevelAvatar(
                    level = entry.level,
                    frame = AvatarFrame.resolve(entry.frameId, entry.level),
                    size = avatarSize,
                    photo = if (entry.isBot) com.sualtikasifi.cizimhafiza.presentation.common.AvatarPhoto.Persona(entry.nickname)
                    else com.sualtikasifi.cizimhafiza.presentation.common.avatarPhotoOf(entry.avatarUrl)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = rowWidth * 0.27f, end = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (entry.isMe) stringResource(R.string.online_you_label, entry.nickname) else entry.nickname,
                        style = PaintedStyle(color = InkBrown, fontSize = 15.sp, textAlign = TextAlign.Start),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    RankLevelLabel(level = entry.level)
                }
                Text(
                    text = stringResource(R.string.league_xp_format, if (showTotalXp) entry.totalXp else entry.periodXp),
                    style = PaintedStyle(color = ButtonOrange, fontSize = 15.sp, textAlign = TextAlign.End),
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * A soft, breathing gold outline just outside the row's own card frame —
 * "which row is even me" scrolling past up to 25 look-alike rows, solved by
 * making the FRAME itself catch the eye rather than the small avatar
 * circle inside it. Deliberately not [CurrentPositionGlow]'s orbiting-
 * sparkle halo: that one is tuned for a small circular node, and the same
 * treatment around a full-width rectangular row read as disconnected from
 * the card's own shape rather than hugging it.
 */
@Composable
private fun MeRowGlow(corner: Dp, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "me-row-glow")
    val pulse by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "me-row-glow-pulse"
    )
    // Resolved here, not inside the Canvas draw lambda below — that lambda
    // runs in DrawScope, not composition, so AppTheme.tokens (a Composable
    // getter) can't be read from inside it.
    val glowColor = AppTheme.tokens.gold
    Canvas(modifier = modifier) {
        val strokeWidth = (2.dp + 2.5.dp * pulse).toPx()
        drawRoundRect(
            color = glowColor.copy(alpha = 0.30f + 0.35f * pulse),
            cornerRadius = CornerRadius(corner.toPx() + strokeWidth / 2f),
            style = Stroke(width = strokeWidth),
            topLeft = Offset(-strokeWidth / 2f, -strokeWidth / 2f),
            size = Size(size.width + strokeWidth, size.height + strokeWidth)
        )
    }
}

/** "HH:MM:SS" until the league resets: midnight Istanbul at the turn of the month. */
@Composable
private fun rememberResetCountdown(): String {
    val zone = java.time.ZoneId.of("Europe/Istanbul")
    fun remaining(): String {
        val now = java.time.ZonedDateTime.now(zone)
        val reset = now.toLocalDate().withDayOfMonth(1).plusMonths(1).atStartOfDay(zone)
        val total = java.time.Duration.between(now, reset).seconds.coerceAtLeast(0)
        return "%02d:%02d:%02d".format(total / 3600, (total % 3600) / 60, total % 60)
    }
    var text by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(remaining()) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1_000)
            text = remaining()
        }
    }
    return text
}
