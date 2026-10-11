package com.sualtikasifi.cizimhafiza.presentation.quickmatch

import com.sualtikasifi.cizimhafiza.presentation.common.cachedPainterResource
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
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Groups
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
import com.sualtikasifi.cizimhafiza.presentation.common.FitText
import com.sualtikasifi.cizimhafiza.presentation.common.LetteredText
import com.sualtikasifi.cizimhafiza.presentation.common.LevelAvatar
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedStyle
import androidx.compose.ui.graphics.Brush
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
        com.sualtikasifi.cizimhafiza.presentation.common.PaintedConfirmDialog(
            title = stringResource(R.string.quick_match_abandon_title),
            message = stringResource(R.string.quick_match_abandon_message, GameConstants.QUICK_MATCH_ABANDON_PENALTY_XP),
            confirmText = stringResource(R.string.quick_match_abandon_confirm, GameConstants.QUICK_MATCH_ABANDON_PENALTY_XP),
            dismissText = stringResource(R.string.quick_match_abandon_keep),
            destructive = true,
            onConfirm = {
                abandonConfirmOpen = false
                viewModel.abandonMatch()
                onBack()
            },
            onDismiss = { abandonConfirmOpen = false }
        )
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // The paper background is always there; the workshop photo fades in
            // over it when the opponent is found, so the switch is a
            // cross-fade instead of a hard cut.
            Image(
                painter = cachedPainterResource(R.drawable.bg_result_wood),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            val searchingAlpha by animateFloatAsState(if (state is QuickMatchState.Searching) 1f else 0f, tween(450), label = "quick_match_search_scene")
            if (searchingAlpha > 0f) {
                SearchingScene(modifier = Modifier.alpha(searchingAlpha))
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
                Spacer(modifier = Modifier.height(TopActionsClearance))

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
                        // The search is its own painted scene (SearchingScene, behind this column).
                        QuickMatchState.Searching -> Spacer(modifier = Modifier.height(1.dp))
                        // The found moment is its own full-screen scene (FoundScene, over this column).
                        is QuickMatchState.Found -> Spacer(modifier = Modifier.height(1.dp))
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
                            body = hoursRemaining(current.untilMillis).let { hours ->
                                stringResource(
                                    if (hours == 1) R.string.quick_match_locked_body_one else R.string.quick_match_locked_body,
                                    hours
                                )
                            },
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
            (state as? QuickMatchState.Found)?.let { found ->
                val sceneAlpha = remember { Animatable(0f) }
                LaunchedEffect(Unit) { sceneAlpha.animateTo(1f, tween(600)) }
                FoundScene(
                    opponent = found.opponent,
                    me = found.me,
                    onStart = { viewModel.onMatchStarted(found.opponent); onStart(found.opponent) },
                    modifier = Modifier.graphicsLayer { alpha = sceneAlpha.value }
                )
            }
            if (matched) {
                // Once matched, leaving costs XP, so the back button asks first.
                com.sualtikasifi.cizimhafiza.presentation.common.PaintedBackButton(
                    onClick = { abandonConfirmOpen = true },
                    modifier = Modifier.align(Alignment.TopStart)
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
 * Laid out as a painted scene in the design's own 841-wide units (see [MfArtW]): the workshop wall, the "Rakibin Hazır!"
 * splash and its sign, the two player cards hanging side by side with the VS between them, the mascots, the countdown
 * plank with its live bar, and the three tips.
 */
@Composable
private fun FoundScene(opponent: GhostRun, me: QuickMatchPlayerSnapshot, onStart: () -> Unit, modifier: Modifier = Modifier) {
    val start by rememberUpdatedState(onStart)
    val progress = remember { Animatable(0f) }
    // Keyed on the run so a genuinely new opponent restarts the countdown,
    // while a recomposition does not.
    LaunchedEffect(opponent.id) {
        // Reset explicitly: the Animatable outlives a change of opponent,
        // and one already sitting at 1f would "finish" instantly and start
        // the match with no countdown at all.
        progress.snapTo(0f)
        progress.snapTo(0.6f)
        delay(100_000)
        start()
    }
    // Derived, so this composable wakes once a second when the DIGIT changes
    // rather than on every frame of the animation.
    val secondsLeft by remember(progress) {
        derivedStateOf {
            ceil((1f - progress.value) * (COUNTDOWN_MS / 1000f)).toInt().coerceAtLeast(1)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = cachedPainterResource(R.drawable.mf_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        BoxWithConstraints(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            val u = maxWidth / MfArtW
            val fontScale0 = LocalDensity.current.fontScale
            fun fs(art: Float) = (art * u.value / fontScale0).sp
            // A picture from the sheet (px) drawn at [scale] design units per px, its top-left at (x, y).
            @Composable
            fun Pic(res: Int, wPx: Int, hPx: Int, scale: Float, x: Float, y: Float, modifier: Modifier = Modifier) {
                Image(
                    painter = painterResource(res),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = modifier.offset(u * x, u * y).size(u * wPx * scale, u * hPx * scale)
                )
            }
            Box(Modifier.fillMaxWidth().height(u * 1650f)) {
                // The splash with "Rakibin Hazır!" lettered into it, and the sign under it.
                // The sign is drawn over the lettering's lower edge at exactly the place it has in the design, so the two read as one
                // picture (no cut, no fade between them).
                Pic(R.drawable.mf_title, 450, 272, 1.2f, 150f, 62f)
                Pic(R.drawable.mf_tag, 354, 106, 1.2f, 150f + 68f * 1.2f, 62f + 266f * 1.2f)
                Box(
                    Modifier.offset(u * (231.6f + 354f * 1.2f * 0.18f), u * (381.2f + 106f * 1.2f * 0.1f))
                        .size(u * 354f * 1.2f * 0.64f, u * 106f * 1.2f * 0.68f),
                    contentAlignment = Alignment.Center
                ) {
                    FitText(
                        text = stringResource(R.string.quick_match_found_tagline),
                        style = PaintedStyle(color = Color(0xFF2B1A10), fontSize = fs(38f), textAlign = TextAlign.Center, lineHeight = fs(41f)),
                        maxLines = 2,
                        minScale = 0.5f,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // The two players, hanging from their ropes; the VS pops in between them once they have landed.
                MatchCard(
                    u = u, fromLeft = true, x = 20f, y = 541f,
                    card = R.drawable.mf_card_r, cardW = 290, cardH = 384, centerX = 148f,
                    ribbon = R.drawable.mf_ribbon_r,
                    nickname = me.nickname, level = me.level, frameId = me.frameId, avatarUrl = me.avatarUrl,
                    // Real, not derived — this is the player's own account.
                    lifetimeXp = me.lifetimeXp
                )
                MatchCard(
                    u = u, fromLeft = false, x = 507f, y = 534f,
                    card = R.drawable.mf_card_b, cardW = 266, cardH = 392, centerX = 133f,
                    ribbon = R.drawable.mf_ribbon_b,
                    nickname = opponent.nickname, level = opponent.level, frameId = opponent.frameId,
                    // The exact figure was never recorded with the round — only the level it bought. This is the floor
                    // XP for that level: a true lower bound, never a guess above it.
                    lifetimeXp = PlayerLevel.totalXpForLevel(opponent.level),
                    // The same face the result screen gives this opponent (see ResultScreen's versus card).
                    photo = com.sualtikasifi.cizimhafiza.presentation.common.AvatarPhoto.Persona(opponent.nickname)
                )
                val vsScale = remember { Animatable(0f) }
                LaunchedEffect(Unit) {
                    delay(350)
                    vsScale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 300f))
                }
                Pic(
                    R.drawable.mf_vs, 275, 252, 0.9f, 297f, 664f,
                    Modifier.graphicsLayer { scaleX = vsScale.value; scaleY = vsScale.value }
                )

                // The mascots peek over the plank.
                Pic(R.drawable.mf_mascots, 479, 291, 1.1f, 157f, 966f)

                // The countdown plank: its clock and leaves kept at the picture's own size, the wood between stretched, the
                // bar and its words live.
                Box(Modifier.offset(u * 25f, u * 1235f).size(u * 790f, u * 148f)) {
                    com.sualtikasifi.cizimhafiza.presentation.common.NinePatch(
                        res = R.drawable.mf_plank,
                        slicePx = 175,
                        edge = u * 175f,
                        sliceYPx = 40,
                        edgeY = u * 40f,
                        clampEdgeToHeight = false,
                        modifier = Modifier.fillMaxSize()
                    )
                    LetteredText(
                        text = stringResource(if (secondsLeft == 1) R.string.quick_match_starting_in_one else R.string.quick_match_starting_in, secondsLeft),
                        size = fs(34f),
                        outline = Color(0xFF3B1A08),
                        maxLines = 1,
                        minScale = 0.5f,
                        modifier = Modifier.offset(u * 168f, u * 34f).size(u * 560f, u * 40f)
                    )
                    // The bar: a dark groove with a golden rim and the yellow fill growing along it.
                    Box(
                        modifier = Modifier
                            .offset(u * 160f, u * 76f)
                            .size(u * 580f, u * 36f)
                            .clip(RoundedCornerShape(50))
                            .background(Brush.verticalGradient(listOf(Color(0xFF3A1F0E), Color(0xFF5A3418))))
                            .border(u * 2.5f, Color(0xFFF2B33A), RoundedCornerShape(50))
                            .padding(u * 4f)
                    ) {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .fillMaxWidth()
                                .drawBehind {
                                    val w = size.width * progress.value.coerceIn(0.03f, 1f)
                                    drawRoundRect(
                                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFFFFE867), Color(0xFFF7B500))),
                                        size = Size(w, size.height),
                                        cornerRadius = CornerRadius(size.height / 2f)
                                    )
                                }
                        )
                    }
                }

                // Three short facts about the round ahead.
                val tipTexts = listOf(
                    stringResource(R.string.quick_match_tip_words, GhostRuns.RUN_WORD_COUNT),
                    stringResource(R.string.quick_match_tip_scoring),
                    stringResource(R.string.quick_match_tip_fun)
                )
                val tips = listOf(
                    Triple(R.drawable.mf_tip1, 225 to 197, 24f),
                    Triple(R.drawable.mf_tip2, 227 to 197, 296f),
                    Triple(R.drawable.mf_tip3, 229 to 197, 568f)
                )
                tips.forEachIndexed { i, (res, size, x) ->
                    Pic(res, size.first, size.second, 1.1f, x, 1405f)
                    // The words go where the picture's own lettering was: the lower half of the note.
                    Box(
                        Modifier.offset(u * (x + 30f), u * (1405f + 100f * 1.1f)).size(u * (size.first * 1.1f - 60f), u * 94f),
                        contentAlignment = Alignment.Center
                    ) {
                        FitText(
                            text = tipTexts[i],
                            style = PaintedStyle(color = Color(0xFF3B2314), fontSize = fs(29f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = fs(32f)),
                            maxLines = 3,
                            minScale = 0.55f,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

private const val MfArtW = 841f

/**
 * One player's card: the painted card with its own picture hung from the rope, the avatar in its earned frame (with the
 * level on it), the name on the game's ribbon and the XP under it. [x], [y] place the card's picture; [centerX] is the
 * middle of its paper in the picture's own pixels. Slides in from its own side with a small overshoot.
 */
@Composable
private fun MatchCard(
    u: androidx.compose.ui.unit.Dp,
    fromLeft: Boolean,
    x: Float,
    y: Float,
    card: Int,
    cardW: Int,
    cardH: Int,
    centerX: Float,
    ribbon: Int,
    nickname: String,
    level: Int,
    frameId: String,
    avatarUrl: String = "",
    photo: com.sualtikasifi.cizimhafiza.presentation.common.AvatarPhoto? = null,
    lifetimeXp: Int
) {
    val s = 1.18f
    val fontScale0 = LocalDensity.current.fontScale
    fun fs(art: Float) = (art * u.value / fontScale0).sp
    val slide = remember { Animatable(if (fromLeft) -1f else 1f) }
    LaunchedEffect(Unit) { slide.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 170f)) }
    Box(
        modifier = Modifier
            .offset(u * x, u * y)
            .size(u * cardW * s, u * cardH * s)
            .graphicsLayer {
                translationX = slide.value * size.width
                alpha = (1f - kotlin.math.abs(slide.value)).coerceIn(0f, 1f)
            }
    ) {
        Image(painterResource(card), contentDescription = null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
        val cx = centerX * s
        val avatar = 190f
        Box(Modifier.offset(u * (cx - avatar / 2f), u * (190f * s - avatar / 2f)).size(u * avatar)) {
            LevelAvatar(
                level = level,
                // The stored name is only a preference; resolve() is what decides which ring that level has actually earned.
                frame = AvatarFrame.resolve(frameId, level),
                size = u * avatar,
                photo = photo ?: com.sualtikasifi.cizimhafiza.presentation.common.avatarPhotoOf(avatarUrl),
                levelBadge = true
            )
        }
        val ribbonW = 292f
        Box(
            Modifier.offset(u * (cx - ribbonW / 2f), u * (290f * s - 28f)).size(u * ribbonW, u * 54f),
            contentAlignment = Alignment.Center
        ) {
            Image(painterResource(ribbon), contentDescription = null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
            FitText(
                text = nickname,
                style = PaintedStyle(color = Color.White, fontSize = fs(30f), textAlign = TextAlign.Center, shadow = androidx.compose.ui.graphics.Shadow(Color(0x99000000), Offset(0f, 2f), 4f)),
                maxLines = 1,
                minScale = 0.5f,
                modifier = Modifier.fillMaxSize().padding(horizontal = u * 34f, vertical = u * 4f)
            )
        }
        Row(
            Modifier.offset(u * (cx - 120f), u * (337f * s - 30f)).size(u * 240f, u * 40f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(painterResource(R.drawable.mf_star), contentDescription = null, modifier = Modifier.size(u * 40f))
            Spacer(Modifier.width(u * 8f))
            Text(
                text = stringResource(R.string.level_total_xp, lifetimeXp),
                style = PaintedStyle(color = Color(0xFF3B2314), fontSize = fs(30f)),
                maxLines = 1
            )
        }
    }
}

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
    com.sualtikasifi.cizimhafiza.presentation.common.NinePatch(
        res = R.drawable.qm_pad,
        slicePx = 120,
        edge = 26.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 34.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = com.sualtikasifi.cizimhafiza.presentation.common.PaintedStyle(color = Color(0xFF2B1A10), fontSize = 22.sp, textAlign = TextAlign.Center)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = body,
                style = com.sualtikasifi.cizimhafiza.presentation.common.DescriptionStyle(15.sp, 20.sp)
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
