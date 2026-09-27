package com.sualtikasifi.cizimhafiza.presentation.quickmatch

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sualtikasifi.cizimhafiza.util.GameConstants
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.AvatarFrame
import com.sualtikasifi.cizimhafiza.domain.model.GhostRun
import com.sualtikasifi.cizimhafiza.domain.model.GhostRuns
import com.sualtikasifi.cizimhafiza.domain.model.LevelTier
import com.sualtikasifi.cizimhafiza.domain.model.PlayerLevel
import com.sualtikasifi.cizimhafiza.domain.model.PlayerRank
import com.sualtikasifi.cizimhafiza.presentation.common.LevelAvatar
import com.sualtikasifi.cizimhafiza.presentation.common.PrimaryButton
import com.sualtikasifi.cizimhafiza.presentation.common.RaisedCard
import com.sualtikasifi.cizimhafiza.presentation.common.ScreenTopActions
import com.sualtikasifi.cizimhafiza.presentation.common.TopActionsClearance
import com.sualtikasifi.cizimhafiza.presentation.common.screenBackground
import com.sualtikasifi.cizimhafiza.presentation.theme.DisplayFont
import com.sualtikasifi.cizimhafiza.presentation.theme.Orange
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.sin

/**
 * Finds a stranger's recorded round to play against, shows who it found,
 * and starts the match itself.
 *
 * What the player sees is a name, a level ring and a countdown — never the
 * score. Underneath, the opponent already played these words and is not
 * sitting there waiting; on screen, the two of you are about to start
 * together. That gap is the whole design of the mode, and showing the
 * result of a round that has already happened is what used to give it away.
 */
@Composable
fun QuickMatchScreen(
    onBack: () -> Unit,
    onStart: (GhostRun) -> Unit,
    viewModel: QuickMatchViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Leaving after an opponent was found costs XP, so it is always confirmed first.
    var abandonConfirmOpen by remember { mutableStateOf(false) }
    val matched = state is QuickMatchState.Found
    androidx.activity.compose.BackHandler(enabled = matched) { abandonConfirmOpen = true }
    if (abandonConfirmOpen) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { abandonConfirmOpen = false },
            title = { Text(stringResource(R.string.quick_match_abandon_title)) },
            text = { Text(stringResource(R.string.quick_match_abandon_message, GameConstants.QUICK_MATCH_ABANDON_PENALTY_XP)) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    abandonConfirmOpen = false
                    viewModel.abandonMatch()
                    onBack()
                }) { Text(stringResource(R.string.quick_match_abandon_confirm, GameConstants.QUICK_MATCH_ABANDON_PENALTY_XP)) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { abandonConfirmOpen = false }) {
                    Text(stringResource(R.string.quick_match_abandon_keep))
                }
            }
        )
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // The paper background is always there; the workshop photo fades in
            // over it when the opponent is found, so the switch is a
            // cross-fade instead of a hard cut.
            Box(modifier = Modifier.fillMaxSize().screenBackground())
            val foundBgAlpha by animateFloatAsState(if (matched) 1f else 0f, tween(600), label = "quick_match_found_bg")
            if (foundBgAlpha > 0f) {
                Image(
                    painter = painterResource(R.drawable.match_found_bg),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().alpha(foundBgAlpha)
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    // The found-opponent moment gets its own full-bleed
                    // workshop photo (with "Rakibin Hazır!" already lettered
                    // into it) instead of the shared doodle-paper background
                    // every other state here still uses — see
                    // MatchFoundTopClearance for why the content below
                    // starts as low as it does.
                    .padding(padding)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Clears the floating back button (see ScreenTopActions) —
                // same convention every other screen uses, rather than this
                // screen's own inline title row sitting a row lower than
                // everywhere else's. The Found state needs much more room:
                // its own background bakes the "Rakibin Hazır!" signage into
                // roughly the top quarter of the image.
                val topClearance by animateDpAsState(if (matched) MatchFoundTopClearance else TopActionsClearance, tween(500, easing = FastOutSlowInEasing), label = "quick_match_top_clearance")
                Spacer(modifier = Modifier.height(topClearance))

                // Centred as one block when it fits, scrollable when it does
                // not — same pattern as MainMenuScreen's own masthead+tiles
                // column. FoundBody's redesigned card carries a full masthead
                // (logo, tagline, mascot) on top of the match itself, which
                // does not reliably fit a short phone alongside the other
                // states' simpler content.
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = maxHeight),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Each state fades and scales into place rather than replacing
                    // the last one in a single frame. Keyed on the state's
                    // class so updates INSIDE Found do not replay it.
                    AnimatedContent(
                        targetState = state,
                        contentKey = { it::class },
                        transitionSpec = {
                            (fadeIn(tween(450, delayMillis = 150)) + scaleIn(tween(450, delayMillis = 150), initialScale = 0.9f)) togetherWith
                                fadeOut(tween(200))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                        label = "quick_match_state"
                    ) { current ->
                    // AnimatedContent hosts its content in a Box, which stacks children;
                    // this Column restores the vertical layout each state was written for.
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    when (current) {
                        QuickMatchState.Searching -> SearchingBody()
                        is QuickMatchState.Found -> FoundBody(
                            opponent = current.opponent,
                            me = current.me,
                            onStart = { viewModel.onMatchStarted(current.opponent); onStart(current.opponent) }
                        )
                        QuickMatchState.Empty -> MessageBody(
                            title = stringResource(R.string.quick_match_empty_title),
                            body = stringResource(R.string.quick_match_empty_body),
                            actionLabel = stringResource(R.string.quick_match_search_again),
                            onAction = viewModel::search
                        )
                        QuickMatchState.Failed -> MessageBody(
                            title = stringResource(R.string.quick_match_failed_title),
                            body = stringResource(R.string.quick_match_failed_body),
                            actionLabel = stringResource(R.string.quick_match_search_again),
                            onAction = viewModel::search
                        )
                        is QuickMatchState.Locked -> MessageBody(
                            title = stringResource(R.string.quick_match_locked_title),
                            // Hours remaining rather than a timestamp: "14
                            // saat" is something a player can act on, a date
                            // and time is something they have to work out.
                            body = stringResource(
                                R.string.quick_match_locked_body,
                                hoursRemaining(current.untilMillis)
                            ),
                            // No retry button — there is nothing to retry
                            // until the clock runs out.
                            actionLabel = null,
                            onAction = {}
                        )
                    }
                    }
                    }
                }
                }
            }
            if (matched) {
                // The wood/neon button the new background's own art style
                // calls for, in place of ScreenTopActions' plain round
                // icon — every other state keeps that shared default.
                Image(
                    painter = painterResource(R.drawable.store_back_button),
                    contentDescription = stringResource(R.string.cd_back),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { abandonConfirmOpen = true }
                )
            } else {
                ScreenTopActions(
                    onBack = onBack,
                    modifier = Modifier.align(Alignment.TopStart)
                )
            }
        }
    }
}

/**
 * A pencil visibly sketching a squiggle, endlessly — the drawing-themed
 * stand-in for a bare spinner, since what this app's "opponent" actually
 * did was draw. Built from sampled points rather than a real hand-drawn
 * path: cheap every frame and exactly reproducible, which a spinner also
 * is but a doodle usually isn't.
 */
@Composable
private fun SearchingBody() {
    val transition = rememberInfiniteTransition(label = "quick_match_draw")
    // Draws left to right, pauses briefly at the end, then starts the next
    // squiggle from scratch — a real sketch does not un-draw itself.
    // State rather than `by`: read down in the Canvas, so the squiggle redraws
    // without recomposing anything. Read here it re-ran this whole composable
    // sixty times a second for as long as the search was open.
    val progress = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(SQUIGGLE_DURATION_MS, easing = LinearEasing)),
        label = "quick_match_draw_progress"
    )
    val tipScale = transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(320, easing = LinearEasing), RepeatMode.Reverse),
        label = "quick_match_tip_scale"
    )

    val strokeColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val density = LocalDensity.current
    val strokeWidthPx = with(density) { 5.dp.toPx() }
    val tipRadiusPx = with(density) { 7.dp.toPx() }

    Canvas(modifier = Modifier.width(200.dp).height(110.dp)) {
        fun pointAt(t: Float): Offset {
            val x = t * size.width
            val y = size.height / 2f + sin(t * SQUIGGLE_CYCLES * (2f * PI.toFloat())) * (size.height * 0.32f)
            return Offset(x, y)
        }

        val fullPath = Path().apply {
            for (i in 0..SQUIGGLE_SAMPLES) {
                val point = pointAt(i / SQUIGGLE_SAMPLES.toFloat())
                if (i == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
            }
        }
        drawPath(fullPath, color = trackColor, style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round))

        val drawnSamples = (SQUIGGLE_SAMPLES * progress.value).toInt().coerceIn(0, SQUIGGLE_SAMPLES)
        if (drawnSamples > 0) {
            val drawnPath = Path().apply {
                for (i in 0..drawnSamples) {
                    val point = pointAt(i / SQUIGGLE_SAMPLES.toFloat())
                    if (i == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
                }
            }
            drawPath(drawnPath, color = strokeColor, style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round))

            val tip = pointAt(drawnSamples / SQUIGGLE_SAMPLES.toFloat())
            drawCircle(color = strokeColor, radius = tipRadiusPx * tipScale.value, center = tip)
        }
    }
    Spacer(modifier = Modifier.height(20.dp))
    Text(
        text = stringResource(R.string.quick_match_searching),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground
    )
}

private const val SQUIGGLE_SAMPLES = 48
private const val SQUIGGLE_CYCLES = 2.4f
private const val SQUIGGLE_DURATION_MS = 1500

/**
 * The moment a match is found, and the few seconds before it starts.
 *
 * Two things are deliberately NOT here. There is no score to beat: knowing
 * the number before drawing a single word turns the round into chasing a
 * target somebody already hit, when the whole appeal is finding out at the
 * end who did better. And there is no start button — the countdown starts
 * itself. Both changes serve the same illusion, which is the one thing that
 * makes a recorded round feel like a match: that the two of you are about
 * to begin at the same moment.
 *
 * Losing the reroll button follows from losing the score. It existed so a
 * player could decline an opponent who looked unbeatable; with nothing to
 * judge, declining is just a slower way of starting.
 *
 * Dressed up well past a plain card on purpose: this is the one moment
 * Quick Match has to make a stranger's recorded round feel like an event
 * rather than a database read — a masthead, a "taped note" card and a
 * cheering mascot, the same voice the brand already uses on MainMenuScreen
 * and in tutorial art, not a one-off skin invented for this screen alone.
 */
@Composable
private fun FoundBody(opponent: GhostRun, me: QuickMatchPlayerSnapshot, onStart: () -> Unit) {
    val start by rememberUpdatedState(onStart)
    val progress = remember { Animatable(0f) }
    // Keyed on the run so a genuinely new opponent restarts the countdown,
    // while a recomposition does not.
    LaunchedEffect(opponent.id) {
        // Reset explicitly: the Animatable outlives a change of opponent,
        // and one already sitting at 1f would "finish" instantly and start
        // the match with no countdown at all.
        progress.snapTo(0f)
        progress.animateTo(1f, tween(COUNTDOWN_MS, easing = LinearEasing))
        start()
    }
    // Derived, so this composable wakes once a second when the DIGIT changes
    // rather than on every frame of the animation. Reading progress.value
    // directly here recomposed the whole card — opponent avatar, sparkles and
    // all — sixty times a second for the length of the countdown, which is
    // exactly the moment before the match starts.
    val secondsLeft by remember(progress) {
        derivedStateOf {
            ceil((1f - progress.value) * (COUNTDOWN_MS / 1000f)).toInt().coerceAtLeast(1)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PlayersSection(opponent = opponent, me = me)
        Spacer(modifier = Modifier.height(16.dp))
        CountdownBanner(secondsLeft)
        Spacer(modifier = Modifier.height(16.dp))
        TipsRow()
        Spacer(modifier = Modifier.height(10.dp))
        MatchMascot()
    }
}

/**
 * How far the Found state's content starts from the top. [R.drawable.match_found_bg]
 * bakes its own "Karalak" + "Rakibin Hazır!" signage into roughly the top
 * quarter of the image (measured off the source art), so the player cards
 * start right under that signage instead of at the usual
 * [com.sualtikasifi.cizimhafiza.presentation.common.TopActionsClearance].
 */
private val MatchFoundTopClearance = 215.dp

/**
 * Both players' cards side by side, with the VS mark and a small paper note
 * overlapping the gap between them — the one part of the new background art
 * that couldn't be baked in, since it has to show two different real players.
 */
@Composable
private fun PlayersSection(opponent: GhostRun, me: QuickMatchPlayerSnapshot) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            PlayerCard(
                fromLeft = true,
                nickname = me.nickname,
                level = me.level,
                frameId = me.frameId,
                // Real, not derived — this is the player's own account.
                lifetimeXp = me.lifetimeXp,
                accent = MatchRed,
                ribbon = R.drawable.match_ribbon_red,
                modifier = Modifier.weight(1f)
            )
            PlayerCard(
                fromLeft = false,
                nickname = opponent.nickname,
                level = opponent.level,
                frameId = opponent.frameId,
                // The exact figure was never recorded with the round —
                // only the level it bought. This is the floor XP for
                // that level: a true lower bound, never a guess above it.
                lifetimeXp = PlayerLevel.totalXpForLevel(opponent.level),
                accent = MatchBlue,
                ribbon = R.drawable.match_ribbon_blue,
                modifier = Modifier.weight(1f)
            )
        }
        // Pops in once the two cards have landed on either side of it.
        val vsScale = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            delay(350)
            vsScale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 300f))
        }
        Image(
            painter = painterResource(R.drawable.match_vs),
            contentDescription = stringResource(R.string.quick_match_versus),
            modifier = Modifier
                .align(Alignment.Center)
                .size(64.dp)
                .graphicsLayer {
                    scaleX = vsScale.value
                    scaleY = vsScale.value
                }
        )
    }
}

/** The ribbon's own dominant colour, sampled off the art — also doubles as each card's border/rank-ribbon accent. */
private val MatchRed = Color(0xFFE23A1E)
private val MatchBlue = Color(0xFF1E7FE0)

/**
 * One player's card: a crown for flourish, an avatar in its earned frame, a
 * name on the game's own ribbon art, an XP badge, and a rank ribbon — the
 * same facts for "you" and for the opponent, so the pair reads as a match
 * between two people rather than a stranger being introduced. Neither side
 * is marked as ahead — see FoundBody's own doc comment on why no score
 * appears here.
 */
@Composable
private fun PlayerCard(
    fromLeft: Boolean,
    nickname: String,
    level: Int,
    frameId: String,
    lifetimeXp: Int,
    accent: Color,
    ribbon: Int,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(22.dp)
    val rank = LevelTier.forLevel(level).rank
    // Slides in from its own side with a small overshoot, then keeps a soft
    // glow pulsing round its border. Every animated value is read inside a
    // draw/graphics lambda (as State, not delegated) so the pulse repaints
    // the card without recomposing it — avatar, ribbon and all.
    val slide = remember { Animatable(if (fromLeft) -1f else 1f) }
    LaunchedEffect(Unit) { slide.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 170f)) }
    val pulse = rememberInfiniteTransition(label = "player_card_pulse")
    val glow = pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "player_card_glow"
    )
    val crownBob = pulse.animateFloat(
        initialValue = 0f,
        targetValue = -4f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "player_card_crown"
    )
    Column(
        modifier = modifier
            .graphicsLayer {
                translationX = slide.value * size.width
                alpha = (1f - kotlin.math.abs(slide.value)).coerceIn(0f, 1f)
            }
            .drawBehind {
                val spread = 7.dp.toPx() * glow.value
                drawRoundRect(
                    color = accent.copy(alpha = 0.28f * glow.value),
                    topLeft = Offset(-spread, -spread),
                    size = Size(size.width + spread * 2, size.height + spread * 2),
                    cornerRadius = CornerRadius(22.dp.toPx() + spread)
                )
            }
            .clip(shape)
            .background(Color(0xE6221812))
            .drawBehind {
                val w = 2.5.dp.toPx()
                drawRoundRect(
                    color = accent.copy(alpha = 0.55f + 0.45f * glow.value),
                    topLeft = Offset(w / 2, w / 2),
                    size = Size(size.width - w, size.height - w),
                    cornerRadius = CornerRadius(22.dp.toPx()),
                    style = Stroke(width = w)
                )
            }
            .padding(top = 14.dp, bottom = 12.dp, start = 8.dp, end = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "👑",
            fontSize = 18.sp,
            modifier = Modifier.graphicsLayer { translationY = crownBob.value.dp.toPx() }
        )
        Spacer(modifier = Modifier.height(2.dp))
        LevelAvatar(
            level = level,
            // The stored name is only a preference; resolve() is what
            // decides which ring that level has actually earned.
            frame = AvatarFrame.resolve(frameId, level),
            size = PLAYER_AVATAR_SIZE
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth().height(32.dp)
        ) {
            Image(
                painter = painterResource(ribbon),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.matchParentSize()
            )
            Text(
                text = nickname,
                fontFamily = DisplayFont,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        XpBadge(lifetimeXp)
        Spacer(modifier = Modifier.height(8.dp))
        RankRibbon(rank = rank, accent = accent)
    }
}

private val PLAYER_AVATAR_SIZE = 76.dp

/** "⭐ 6416 XP" as a small pill, rather than plain text — the one stat this card shows gets to look like a badge. */
@Composable
private fun XpBadge(xp: Int) {
    Row(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "⭐", fontSize = 11.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.level_total_xp, xp),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
    }
}

/**
 * A small pennant — pointed at both ends via a hand-drawn Path, since no
 * asset was provided for this element specifically (only the wide name
 * ribbon was). Shows the player's existing [PlayerRank] (the app's real
 * level-tier title), coloured to match the card it sits in.
 */
@Composable
private fun RankRibbon(rank: PlayerRank, accent: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier.height(26.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val notch = size.height * 0.45f
            val path = Path().apply {
                moveTo(notch, 0f)
                lineTo(size.width - notch, 0f)
                lineTo(size.width, size.height / 2f)
                lineTo(size.width - notch, size.height)
                lineTo(notch, size.height)
                lineTo(0f, size.height / 2f)
                close()
            }
            drawPath(path, color = accent)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Text(text = rank.emoji, fontSize = 10.sp)
            Text(
                text = stringResource(rank.nameRes),
                fontFamily = DisplayFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 10.sp,
                color = Color.White,
                maxLines = 1
            )
        }
    }
}

/**
 * The countdown itself — the game's own glowing pill art (its clock icon and
 * "…" are already baked into the two ends) with just the seconds text laid
 * over the middle, in place of the plain orange banner this used to be.
 */
@Composable
private fun CountdownBanner(secondsLeft: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(COUNTDOWN_BAR_ASPECT)
            .paint(painterResource(R.drawable.match_countdown_bar), contentScale = ContentScale.FillBounds),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.quick_match_starting_in, secondsLeft),
            fontFamily = DisplayFont,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            // Keeps the text off the bar's own baked-in clock icon (left)
            // and "…" (right) — asymmetric because the two ends aren't.
            modifier = Modifier.padding(start = 68.dp, end = 52.dp)
        )
    }
}

private const val COUNTDOWN_BAR_ASPECT = 1400f / 250f

/** Three short facts about the round ahead, in place of the single explainer sentence this used to be. */
@Composable
private fun TipsRow() {
    // IntrinsicSize.Max + fillMaxHeight on each card: all three take the height
    // of the tallest, so a longer sentence no longer makes one box bigger.
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max)
    ) {
        TipCard(emoji = "📝", text = stringResource(R.string.quick_match_tip_words, GhostRuns.RUN_WORD_COUNT), modifier = Modifier.weight(1f))
        TipCard(emoji = "🏆", text = stringResource(R.string.quick_match_tip_scoring), modifier = Modifier.weight(1f))
        TipCard(emoji = "😊", text = stringResource(R.string.quick_match_tip_fun), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun TipCard(emoji: String, text: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xCC1E1610))
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = emoji, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 3
        )
    }
}

/**
 * The two mascots high-fiving over a VS spark: the "match found" moment as an
 * illustration. It bobs and tilts a little forever, so the last few seconds
 * before the match do not sit on a still picture.
 *
 * The bob/tilt used to be applied straight to an Image sized flush with its
 * own layout box (fillMaxWidth(0.8f)) — rotating and shifting content that
 * fills its own bounds pushes the edges (a hand, an ear) straight into a
 * hard clip the instant it moves, which read as a rendering glitch rather
 * than an intentional wobble. A rectangular edge-fade over the whole box was
 * tried as a fix and looked worse — a visible rectangle of vignette sitting
 * over transparent PNG artwork reads as exactly the "amateur" look it was
 * supposed to avoid, since the art has no straight edges of its own for a
 * straight-edged fade to follow. The actual fix is simpler: the image is
 * just drawn smaller than its own box (imageScale), so the motion has room
 * to move inside the box and never reaches an edge to clip against at all —
 * no mask needed.
 */
@Composable
private fun MatchMascot() {
    val transition = rememberInfiniteTransition(label = "match_mascot")
    val bob = transition.animateFloat(
        initialValue = 0f,
        targetValue = -7f,
        animationSpec = infiniteRepeatable(tween(850, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "match_mascot_bob"
    )
    val tilt = transition.animateFloat(
        initialValue = -1.6f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "match_mascot_tilt"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .aspectRatio(840f / 446f),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.match_high_five),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize(MascotImageScale)
                .graphicsLayer {
                    translationY = bob.value.dp.toPx()
                    rotationZ = tilt.value
                }
        )
    }
}

/** How much smaller than its box the mascot image is drawn — see [MatchMascot]'s doc comment. */
private const val MascotImageScale = 0.9f

private const val COUNTDOWN_MS = 5_000

/** Whole hours left, rounded up so "1 saat" never means "in three minutes". */
private fun hoursRemaining(untilMillis: Long): Int {
    val left = untilMillis - System.currentTimeMillis()
    if (left <= 0L) return 0
    return ((left + 3_599_999L) / 3_600_000L).toInt()
}

@Composable
private fun MessageBody(
    title: String,
    body: String,
    /** Null when there is nothing useful to retry — see the Locked branch. */
    actionLabel: String?,
    onAction: () -> Unit
) {
    RaisedCard(corner = 24.dp, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
    if (actionLabel != null) {
        Spacer(modifier = Modifier.height(16.dp))
        PrimaryButton(
            text = actionLabel,
            onClick = onAction,
            icon = Icons.Filled.Refresh,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
