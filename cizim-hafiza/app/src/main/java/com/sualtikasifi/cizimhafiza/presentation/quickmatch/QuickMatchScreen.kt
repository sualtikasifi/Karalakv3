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
import androidx.compose.ui.res.integerArrayResource
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
import com.sualtikasifi.cizimhafiza.presentation.common.a11yButton
import androidx.compose.foundation.layout.wrapContentSize
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
        com.sualtikasifi.cizimhafiza.presentation.common.PaintedExitDialog(
            style = com.sualtikasifi.cizimhafiza.presentation.common.ExitDialogStyle.InGame,
            title = stringResource(R.string.quick_match_abandon_title),
            message = stringResource(R.string.quick_match_abandon_message, GameConstants.QUICK_MATCH_ABANDON_PENALTY_XP),
            confirmText = stringResource(R.string.quick_match_abandon_confirm, GameConstants.QUICK_MATCH_ABANDON_PENALTY_XP),
            dismissText = stringResource(R.string.quick_match_abandon_keep),
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
                SearchingScene(onBack = onBack, modifier = Modifier.alpha(searchingAlpha))
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
                            body = minutesRemaining(current.untilMillis).let { minutes ->
                                stringResource(
                                    R.string.quick_match_locked_body,
                                    when {
                                        minutes < 60 -> stringResource(if (minutes == 1) R.string.lock_duration_minutes_one else R.string.lock_duration_minutes, minutes)
                                        minutes >= 24 * 60 -> ((minutes + 24 * 60 - 1) / (24 * 60)).let { stringResource(if (it == 1) R.string.lock_duration_days_one else R.string.lock_duration_days, it) }
                                        else -> ((minutes + 59) / 60).let { stringResource(if (it == 1) R.string.lock_duration_hours_one else R.string.lock_duration_hours, it) }
                                    }
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
                    // Once matched, leaving costs XP, so the back button (part of the scene's picture) asks first.
                    onBack = { abandonConfirmOpen = true },
                    modifier = Modifier.graphicsLayer { alpha = sceneAlpha.value }
                )
            }
            (state as? QuickMatchState.Locked)?.let { locked ->
                LockedScene(untilMillis = locked.untilMillis, onUnlocked = viewModel::search)
            }
            if (!matched && state !is QuickMatchState.Searching) {
                // While searching or matched, the scene's own picture carries the back button (and its touch area).
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
 * itself, so the round plays like a match that begins for both of you at
 * once. What the opponent can be (a live player, a recorded round or an AI)
 * is said once, on the search screen before this (SearchingScene), rather
 * than on each card.
 *
 * The workshop picture (bg_qm_found) carries only the painted parts: the sign, the blank note plate, the two empty cards
 * with the VS between them, the mascots, the countdown plank with its empty groove and the three tip notes with their icons.
 * Everything that is a word or belongs to a player is written live over it by the picture's own coordinates (841 x 1870), so
 * one picture serves every language: the title on the sign, the note, each player's avatar in its earned frame with the
 * level on it, the name on the ribbon and the XP under it, the countdown and its bar, and the three tips.
 */
@Composable
internal fun FoundScene(opponent: GhostRun, me: QuickMatchPlayerSnapshot, onStart: () -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
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
    // rather than on every frame of the animation.
    val secondsLeft by remember(progress) {
        derivedStateOf {
            ceil((1f - progress.value) * (COUNTDOWN_MS / 1000f)).toInt().coerceAtLeast(1)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val unit = maxOf(maxWidth / MfArtW, maxHeight / MfArtH)
        val offX = (maxWidth - unit * MfArtW) / 2
        val offY = (maxHeight - unit * MfArtH) / 2
        val us = unit.value
        val fontScale0 = LocalDensity.current.fontScale
        fun fs(art: Float) = (art * us / fontScale0).sp
        fun box(x0: Float, y0: Float, x1: Float, y1: Float): Modifier =
            Modifier.offset(offX + unit * x0, offY + unit * y0)
                // Unbounded: the picture is wider than the screen, and a plain size() would be clamped to it.
                .wrapContentSize(Alignment.TopStart, unbounded = true)
                .size(unit * (x1 - x0), unit * (y1 - y0))

        Image(
            painter = cachedPainterResource(R.drawable.bg_qm_found),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = box(0f, 0f, MfArtW, MfArtH)
        )

        // The back button is part of the picture; this is its touch area (leaving now costs XP, so the caller asks first).
        Box(
            box(22f, 80f, 146f, 192f)
                .clip(RoundedCornerShape(24))
                .clickable(onClick = onBack)
                .a11yButton(stringResource(R.string.cd_back))
        )

        // ---- The sign: two lines on the plank's face (x 215..625, y 245..420) -------------------------------------
        LetteredText(
            text = stringResource(R.string.quick_match_found_title1),
            size = fs(96f),
            fill = Color.White,
            outline = Color(0xFF5A2815),
            modifier = box(205f, 232f, 635f, 352f),
            minScale = 0.45f,
            title = true
        )
        LetteredText(
            text = stringResource(R.string.quick_match_found_title2),
            size = fs(104f),
            fill = Color(0xFFFFE04A),
            outline = Color(0xFF8A4300),
            modifier = box(205f, 312f, 635f, 432f),
            minScale = 0.45f,
            title = true
        )
        // ---- The note plate under it (x 150..690, y 470..548) -------------------------------------------------------
        FitText(
            text = stringResource(R.string.quick_match_found_tagline).replace('\n', ' '),
            style = PaintedStyle(color = Color(0xFF2B1A10), fontSize = fs(36f), fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center),
            maxLines = 1,
            minScale = 0.5f,
            modifier = box(195f, 482f, 645f, 536f)
        )

        // ---- The two players: their avatar, name and XP over the empty cards ------------------------------------------
        PlayerPanel(
            u = unit, box = ::box, fs = ::fs, cx = 205f,
            nickname = me.nickname, level = me.level, frameId = me.frameId, avatarUrl = me.avatarUrl,
            // Real, not derived — this is the player's own account.
            lifetimeXp = me.lifetimeXp
        )
        PlayerPanel(
            u = unit, box = ::box, fs = ::fs, cx = 637f,
            nickname = opponent.nickname, level = opponent.level, frameId = opponent.frameId,
            // The exact figure was never recorded with the round — only the level it bought. This is the floor
            // XP for that level: a true lower bound, never a guess above it.
            lifetimeXp = PlayerLevel.totalXpForLevel(opponent.level),
            // The same face the result screen gives this opponent (see ResultScreen's versus card).
            photo = com.sualtikasifi.cizimhafiza.presentation.common.AvatarPhoto.Persona(opponent.nickname)
        )

        // ---- The countdown: the words above the groove, the yellow bar growing along it ----------------------------------
        LetteredText(
            text = stringResource(if (secondsLeft == 1) R.string.quick_match_starting_in_one else R.string.quick_match_starting_in, secondsLeft),
            size = fs(34f),
            outline = Color(0xFF3B1A08),
            maxLines = 1,
            minScale = 0.5f,
            modifier = box(190f, 1280f, 735f, 1324f)
        )
        Box(
            modifier = box(192f, 1334f, 728f, 1362f)
                .drawBehind {
                    val w = size.width * progress.value.coerceIn(0.03f, 1f)
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(Color(0xFFFFE867), Color(0xFFF7B500))),
                        size = Size(w, size.height),
                        cornerRadius = CornerRadius(size.height / 2f)
                    )
                }
        )

        // ---- Three short facts about the round ahead, one size for all three -----------------------------------------
        val tipTexts = listOf(
            stringResource(R.string.quick_match_tip_words, GhostRuns.RUN_WORD_COUNT),
            stringResource(R.string.quick_match_tip_scoring),
            stringResource(R.string.quick_match_tip_fun)
        )
        // x of each note and its width, in the picture's units
        val notes = listOf(50f to 238f, 305f to 235f, 557f to 236f)
        notes.forEachIndexed { i, (x, w) ->
            FitText(
                text = tipTexts[i],
                style = PaintedStyle(color = Color(0xFF3B2314), fontSize = fs(26f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = fs(30f)),
                maxLines = 3,
                minScale = 0.7f,
                modifier = box(x + 20f, 1560f, x + w - 20f, 1654f)
            )
        }
    }
}

private const val MfArtW = 841f
private const val MfArtH = 1870f

/**
 * One player's side of the empty card (the card itself is in the picture): the avatar in the frame its level has earned, with
 * the level on it, the name on the card's ribbon and the XP under it. [cx] is the middle of the card in the picture's units.
 * It comes in a moment after the scene does, with a small overshoot.
 */
@Composable
private fun PlayerPanel(
    u: androidx.compose.ui.unit.Dp,
    box: (Float, Float, Float, Float) -> Modifier,
    fs: (Float) -> androidx.compose.ui.unit.TextUnit,
    cx: Float,
    nickname: String,
    level: Int,
    frameId: String,
    avatarUrl: String = "",
    photo: com.sualtikasifi.cizimhafiza.presentation.common.AvatarPhoto? = null,
    lifetimeXp: Int
) {
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(250)
        enter.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 220f))
    }
    val avatar = 190f
    Box(
        box(cx - avatar / 2f, 650f, cx + avatar / 2f, 650f + avatar)
            .graphicsLayer {
                val k = enter.value
                alpha = k.coerceIn(0f, 1f)
                scaleX = 0.8f + 0.2f * k; scaleY = 0.8f + 0.2f * k
            }
    ) {
        LevelAvatar(
            level = level,
            // The stored name is only a preference; resolve() is what decides which ring that level has actually earned.
            frame = AvatarFrame.resolve(frameId, level),
            size = u * avatar,
            photo = photo ?: com.sualtikasifi.cizimhafiza.presentation.common.avatarPhotoOf(avatarUrl),
            levelBadge = true
        )
    }
    // The name on the ribbon (its cloth is x cx-125..cx+125, y 848..910)
    FitText(
        text = nickname,
        style = PaintedStyle(color = Color.White, fontSize = fs(31f), textAlign = TextAlign.Center, shadow = androidx.compose.ui.graphics.Shadow(Color(0x99000000), Offset(0f, 2f), 4f)),
        maxLines = 1,
        minScale = 0.5f,
        modifier = box(cx - 105f, 856f, cx + 105f, 904f).graphicsLayer { alpha = enter.value.coerceIn(0f, 1f) }
    )
    Row(
        box(cx - 125f, 922f, cx + 125f, 966f).graphicsLayer { alpha = enter.value.coerceIn(0f, 1f) },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painterResource(R.drawable.mf_star), contentDescription = null, modifier = Modifier.fillMaxHeight(0.95f).aspectRatio(1f))
        Spacer(Modifier.width(8.dp))
        FitText(
            text = stringResource(R.string.level_total_xp, lifetimeXp),
            style = PaintedStyle(color = Color(0xFF3B2314), fontSize = fs(31f)),
            maxLines = 1,
            minScale = 0.6f,
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

private const val COUNTDOWN_MS = 5_000

/** Whole minutes left, rounded up. */
private fun minutesRemaining(untilMillis: Long): Int {
    val left = untilMillis - System.currentTimeMillis()
    if (left <= 0L) return 1
    return ((left + 59_999L) / 60_000L).toInt()
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


private const val LockArtW = 841f
private const val LockArtH = 1870f

/**
 * The lockout screen: the painted "Bu mod geçici olarak KAPALI" picture (see bg_locked, one per language), with the
 * reason and a live countdown written into the empty part of its paper. Laid out in the picture's own units so it scales
 * with it; how far down they sit comes from each language's lock_art resource, as some paint a longer title.
 */
@Composable
private fun LockedScene(untilMillis: Long, onUnlocked: () -> Unit) {
    var now by remember { androidx.compose.runtime.mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(untilMillis) {
        while (true) {
            now = System.currentTimeMillis()
            if (now >= untilMillis) {
                onUnlocked()
                break
            }
            delay(1000)
        }
    }
    val left = (untilMillis - now).coerceAtLeast(0L) / 1000L
    val days = left / 86_400L
    val clock = String.format(java.util.Locale.US, "%02d:%02d:%02d", (left % 86_400L) / 3600L, (left % 3600L) / 60L, left % 60L)
    val countdown = if (days > 0) stringResource(R.string.lock_countdown_days, days.toInt(), clock) else clock

    // [countdown top, countdown height, reason top, reason height] on this language's picture
    val art = integerArrayResource(R.array.lock_art)
    val pillTop = art[0].toFloat(); val pillH = art[1].toFloat(); val bodyTop = art[2].toFloat(); val bodyH = art[3].toFloat()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        // Crop to fill, centred; the pieces keep their place on the picture.
        val s = maxOf(widthPx / LockArtW, heightPx / LockArtH)
        val offX = (widthPx - LockArtW * s) / 2f
        val offY = (heightPx - LockArtH * s) / 2f
        val fontScale0 = density.fontScale
        fun x(art: Float) = with(density) { (offX + art * s).toDp() }
        fun y(art: Float) = with(density) { (offY + art * s).toDp() }
        fun len(art: Float) = with(density) { (art * s).toDp() }
        fun fs(art: Float) = with(density) { (art * s / fontScale0).toSp() }

        Image(
            painter = cachedPainterResource(R.drawable.bg_locked),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // The live countdown, under KAPALI: cream digits on a dark wooden pill, so it reads against the paper.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset(x(190f), y(pillTop))
                .size(len(460f), len(pillH))
                .clip(RoundedCornerShape(len(pillH / 2f)))
                .background(androidx.compose.ui.graphics.Color(0xFF4A2814))
                .border(len(5f), androidx.compose.ui.graphics.Color(0xFFD9892B), RoundedCornerShape(len(pillH / 2f)))
        ) {
            com.sualtikasifi.cizimhafiza.presentation.common.FitText(
                text = countdown,
                style = androidx.compose.ui.text.TextStyle(
                    fontFamily = com.sualtikasifi.cizimhafiza.presentation.theme.DisplayFont,
                    fontSize = fs(pillH * 0.74f),
                    fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                    color = androidx.compose.ui.graphics.Color(0xFFFFE7B0),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                ),
                maxLines = 1,
                minScale = 0.6f,
                modifier = Modifier.fillMaxSize().padding(horizontal = len(24f))
            )
        }
        // Why, and what still works.
        com.sualtikasifi.cizimhafiza.presentation.common.FitText(
            text = stringResource(R.string.lock_screen_body),
            style = androidx.compose.ui.text.TextStyle(
                fontSize = fs(31f),
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                color = androidx.compose.ui.graphics.Color(0xFF4A2814),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = fs(37f)
            ),
            maxLines = 4,
            minScale = 0.6f,
            modifier = Modifier.offset(x(110f), y(bodyTop)).size(len(620f), len(bodyH))
        )
    }
}
