package com.sualtikasifi.cizimhafiza.presentation.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SkipNext
import com.sualtikasifi.cizimhafiza.presentation.common.JokerArt
import com.sualtikasifi.cizimhafiza.presentation.common.shortRes
import com.sualtikasifi.cizimhafiza.presentation.common.tint
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sualtikasifi.cizimhafiza.R
import kotlinx.coroutines.delay
import com.sualtikasifi.cizimhafiza.domain.model.AvatarFrame
import com.sualtikasifi.cizimhafiza.domain.model.LevelProgressState
import com.sualtikasifi.cizimhafiza.presentation.common.AppTextField
import com.sualtikasifi.cizimhafiza.presentation.common.CircularCountdown
import com.sualtikasifi.cizimhafiza.presentation.common.GameTopBar
import com.sualtikasifi.cizimhafiza.presentation.common.LiveLevelBadge
import com.sualtikasifi.cizimhafiza.presentation.common.PrimaryButton
import com.sualtikasifi.cizimhafiza.presentation.common.SecondaryButton
import com.sualtikasifi.cizimhafiza.presentation.common.StatPill
import com.sualtikasifi.cizimhafiza.presentation.common.StrokeCanvas
import com.sualtikasifi.cizimhafiza.presentation.common.TintedBadge
import com.sualtikasifi.cizimhafiza.presentation.common.currentWordLanguage
import com.sualtikasifi.cizimhafiza.presentation.common.dotGridBackground
import com.sualtikasifi.cizimhafiza.presentation.common.hardEdge
import com.sualtikasifi.cizimhafiza.presentation.common.screenBackground
import com.sualtikasifi.cizimhafiza.presentation.theme.AppTheme
import com.sualtikasifi.cizimhafiza.presentation.theme.OrangeDeep
import com.sualtikasifi.cizimhafiza.presentation.theme.TimerWarning
import com.sualtikasifi.cizimhafiza.util.capitalizeForWordLanguage
import com.sualtikasifi.cizimhafiza.util.GameConstants

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun GuessScreen(
    state: GamePhase.Guessing,
    onSubmit: (String) -> Unit,
    onAnswerChanged: (String) -> Unit = {},
    onHintClick: () -> Unit = {},
    jokers: Map<com.sualtikasifi.cizimhafiza.domain.model.JokerType, Int> = emptyMap(),
    onFirstLetterJoker: () -> Unit = {},
    onLetterCountJoker: () -> Unit = {},
    levelProgress: LevelProgressState? = null,
    selectedFrame: AvatarFrame? = null,
    musicEnabled: Boolean = true,
    onToggleMusic: () -> Unit = {},
    adUnavailable: Boolean = false,
    onAdUnavailableShown: () -> Unit = {}
) {
    val wordLanguage = currentWordLanguage()
    var answer by remember(state.guessNumber) { mutableStateOf("") }
    val isAnswered = state.feedback != null
    val timerColor = if (state.isWarning) TimerWarning else MaterialTheme.colorScheme.primary

    // Guards against a double-tap firing two rewarded-ad loads for the same
    // click — resets per word, though once state.hintUsed flips true the
    // button is gone for the rest of the match anyway.
    var hintRequested by remember(state.guessNumber) { mutableStateOf(false) }
    // An ad that never loaded used to leave this button reading
    // "Yükleniyor…" and disabled until the word changed. Now the attempt
    // resolves: the button comes back, and a line says why nothing
    // happened.
    var adErrorShown by remember { mutableStateOf(false) }
    LaunchedEffect(adUnavailable) {
        if (adUnavailable) {
            hintRequested = false
            adErrorShown = true
            onAdUnavailableShown()
        }
    }
    if (adErrorShown) {
        LaunchedEffect(Unit) {
            delay(3_000)
            adErrorShown = false
        }
    }

    // Kept focused (and thus the keyboard kept open) across the whole
    // guessing phase. The field's enabled/readOnly state never changes —
    // even toggling readOnly while focused can make the system hide the
    // IME — so edits during the brief feedback window are blocked purely
    // by ignoring onValueChange, and focus+keyboard are explicitly
    // reasserted on every new guess as a safety net.
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(state.guessNumber) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        // Laid out so the ANSWER FIELD is always on screen with the keyboard
        // open: top bar, canvas, then field + submit directly under it, and
        // every helper (ad hint, jokers, skip) folded into one slim row below.
        // The canvas is the single flexible piece (weight) and takes whatever
        // the keyboard leaves — every other piece is a small fixed height, so
        // it can no longer be squeezed out of view the way a tall stack of
        // buttons used to push the field behind the keyboard.
        val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
        Column(
            modifier = Modifier
                .fillMaxSize()
                .screenBackground()
                .padding(padding)
                .imePadding()
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            GameTopBar(
                progressLabel = "${state.guessNumber} / ${state.totalGuesses}",
                musicEnabled = musicEnabled,
                onToggleMusic = onToggleMusic
            ) {
                // Absent only for the first-launch tutorial's practice round,
                // which has no real ViewModel/XP behind it to show. Hidden
                // while the keyboard is up: it is the widest thing in this
                // row and the one the player can most easily do without.
                if (!imeVisible) {
                    levelProgress?.let {
                        LiveLevelBadge(progress = it, frame = selectedFrame ?: AvatarFrame.highestUnlockedFor(it.level))
                    }
                }
                // totalSeconds == 0 means this guess turn is untimed (the
                // first-launch tutorial) — an empty ring reading "0" would
                // look like an expired timer, so show nothing instead.
                if (state.totalSeconds > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    // Between the music toggle and the ring itself: the
                    // player was earning a speed bonus for answering fast
                    // (see XpAwards.wordXp) with no way to see it happening.
                    LiveXpBonusBadge(
                        secondsLeft = state.secondsLeft,
                        totalSeconds = state.totalSeconds
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    CircularCountdown(
                        secondsLeft = state.secondsLeft,
                        totalSeconds = state.totalSeconds,
                        ringColor = timerColor,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .heightIn(min = 120.dp)
                    .padding(bottom = AppTheme.tokens.raise)
                    .hardEdge(AppTheme.tokens.edge, AppTheme.tokens.raise, 26.dp)
                    .background(AppTheme.tokens.canvasPaper, MaterialTheme.shapes.large)
                    .dotGridBackground(AppTheme.tokens.canvasGrid, spacing = 22.dp, radius = 1.2.dp)
                    .border(2.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
            ) {
                StrokeCanvas(strokes = state.strokes, modifier = Modifier.fillMaxSize())

                // Correct/wrong feedback is drawn ON TOP of the canvas rather
                // than appended below it: as a sibling in the Column it added
                // real height, which stole it from the canvas's weight(1f) and
                // made the drawing visibly shrink the instant an answer landed.
                // As an overlay the layout never moves.
                GuessFeedbackOverlay(
                    visible = isAnswered,
                    feedback = state.feedback,
                    guessNumber = state.guessNumber,
                    wordLanguage = wordLanguage,
                    modifier = Modifier.align(Alignment.Center)
                )

                // Revealed hints sit on the drawing itself instead of taking
                // rows of their own below it — same reason as the feedback.
                //
                // The letter-count joker draws one blank per letter, hangman
                // style, with a gap between words. If the first-letter joker
                // (or the ad hint) was used as well, that letter is written
                // over the first blank. On its own the first letter keeps its
                // plain "İlk harf: B" badge.
                val blankGroups = state.letterGroups ?: state.letterCount?.let { listOf(it) }
                val firstLetterShown = state.hintLetter?.capitalizeForWordLanguage(wordLanguage)
                if (blankGroups != null) {
                    LetterBlanks(
                        groups = blankGroups,
                        firstLetter = firstLetterShown,
                        modifier = Modifier.align(Alignment.TopCenter).padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                } else if (firstLetterShown != null) {
                    // Capitalized the same way the word itself is displayed
                    // everywhere else — a Turkish "i" has to become "İ", not "I".
                    TintedBadge(
                        text = stringResource(R.string.hint_first_letter, firstLetterShown),
                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppTextField(
                    value = answer,
                    onValueChange = { newValue ->
                        // Ignore edits once answered instead of toggling
                        // enabled/readOnly, so the field never loses focus.
                        if (!isAnswered) {
                            answer = newValue
                            onAnswerChanged(newValue)
                        }
                    },
                    centered = true,
                    // "Bu neydi?" used to be a title of its own above the
                    // buttons; as the placeholder it costs no height at all.
                    placeholder = stringResource(R.string.what_did_you_draw),
                    textStyle = MaterialTheme.typography.titleMedium,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { if (!isAnswered && answer.isNotBlank()) onSubmit(answer) }
                    ),
                    focusRequester = focusRequester,
                    corner = 26.dp,
                    modifier = Modifier.weight(1f)
                )
                PrimaryButton(
                    text = stringResource(R.string.submit_guess),
                    onClick = { onSubmit(answer) },
                    enabled = !isAnswered && answer.isNotBlank(),
                    height = 52.dp,
                    modifier = Modifier.width(112.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            // Ad hint (one per whole match, not per word — see
            // GameViewModel/OnlineGameViewModel.useHint), the two jokers
            // and skip: one row of equal-width tiles, so they read as a
            // toolbar instead of loose pills piled against the left edge.
            // With the ad hint gone the remaining tiles simply share the
            // width.
            //
            // Kept on screen (merely disabled) through the isAnswered
            // feedback pause instead of being removed outright: this whole
            // Row used to disappear between words, and since the canvas
            // above it holds weight(1f), removing it let the canvas jump
            // taller for the pause and snap back for the next word — a
            // constant resize/reflow on every single transition. Reserving
            // the same height throughout keeps the canvas' size fixed.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!state.hintUsed && state.hintLetter == null && GameConstants.ADMOB_ENABLED) {
                    GuessActionTile(
                        label = stringResource(if (hintRequested) R.string.loading_hint else R.string.guess_tile_hint),
                        fill = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        borderColor = MaterialTheme.colorScheme.primary,
                        enabled = !hintRequested && !isAnswered,
                        modifier = Modifier.weight(1f),
                        // Countdown is paused (see useHint) the instant this is
                        // tapped, so the label changes to make clear something is
                        // happening — a frozen timer with no other signal would
                        // otherwise look like the screen had just stalled.
                        onClick = {
                            if (!hintRequested) {
                                hintRequested = true
                                onHintClick()
                            }
                        },
                        icon = {
                            Icon(Icons.Filled.PlayCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                        }
                    )
                }
                val firstType = com.sualtikasifi.cizimhafiza.domain.model.JokerType.FIRST_LETTER
                val countType = com.sualtikasifi.cizimhafiza.domain.model.JokerType.LETTER_COUNT
                val firstCount = jokers[firstType] ?: 0
                val letterCountCount = jokers[countType] ?: 0
                GuessActionTile(
                    label = stringResource(firstType.shortRes()),
                    fill = firstType.tint(),
                    contentColor = Color.White,
                    enabled = !isAnswered && state.hintLetter == null && firstCount > 0,
                    badgeCount = firstCount,
                    modifier = Modifier.weight(1f),
                    onClick = onFirstLetterJoker,
                    icon = { JokerArt(firstType, 28.dp) }
                )
                GuessActionTile(
                    label = stringResource(countType.shortRes()),
                    fill = countType.tint(),
                    contentColor = Color.White,
                    enabled = !isAnswered && state.letterCount == null && letterCountCount > 0,
                    badgeCount = letterCountCount,
                    modifier = Modifier.weight(1f),
                    onClick = onLetterCountJoker,
                    icon = { JokerArt(countType, 28.dp) }
                )
                GuessActionTile(
                    label = stringResource(R.string.skip_guess),
                    fill = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    borderColor = MaterialTheme.colorScheme.outline,
                    enabled = !isAnswered,
                    modifier = Modifier.weight(1f),
                    onClick = { onSubmit("") },
                    icon = {
                        Icon(Icons.Filled.SkipNext, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp))
                    }
                )
            }
            if (adErrorShown) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.ad_unavailable),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

/**
 * The word as a row of blanks — one per letter, grouped by word — with an
 * optional [firstLetter] written on the very first one. Wraps onto further
 * lines for a long answer instead of running off the canvas.
 */
@Composable
private fun LetterBlanks(groups: List<Int>, firstLetter: String?, modifier: Modifier = Modifier) {
    val ink = MaterialTheme.colorScheme.primary
    FlowRow(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f), RoundedCornerShape(16.dp))
            .border(1.5.dp, ink.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        groups.forEachIndexed { groupIndex, length ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(length) { letterIndex ->
                    val letter = if (groupIndex == 0 && letterIndex == 0) firstLetter else null
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.width(20.dp).height(26.dp), contentAlignment = Alignment.BottomCenter) {
                            if (letter != null) {
                                Text(
                                    text = letter,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                                    fontSize = 21.sp,
                                    color = ink,
                                    maxLines = 1
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(ink)
                        )
                    }
                }
            }
        }
    }
}

/**
 * One button of the helper toolbar under the answer field: icon over label,
 * same height and (via the caller's weight) the same width as its neighbours.
 * [badgeCount] draws a small count chip on the corner — the joker stock.
 */
@Composable
private fun GuessActionTile(
    label: String,
    fill: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    borderColor: Color? = null,
    enabled: Boolean = true,
    badgeCount: Int? = null,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .height(62.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(shape)
            .background(fill)
            .then(if (borderColor != null) Modifier.border(2.dp, borderColor, shape) else Modifier)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            icon()
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                color = contentColor,
                maxLines = 1
            )
        }
        if (badgeCount != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 6.dp)
                    .size(20.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badgeCount.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                    color = fill
                )
            }
        }
    }
}

/**
 * The correct/wrong badge shown over the drawing once an answer lands.
 * Extracted into its own composable purely so [AnimatedVisibility] resolves
 * against no implicit receiver — called inline inside the canvas Box it
 * would bind to the enclosing Column's scoped overload instead.
 */
@Composable
private fun GuessFeedbackOverlay(
    visible: Boolean,
    feedback: GuessFeedback?,
    guessNumber: Int,
    wordLanguage: String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn(tween(150)),
        modifier = modifier
    ) {
        if (feedback != null) {
            val shakeOffset = remember(guessNumber) { Animatable(0f) }
            LaunchedEffect(feedback) {
                if (!feedback.isCorrect) {
                    shakeOffset.animateTo(
                        targetValue = 0f,
                        animationSpec = keyframes {
                            durationMillis = 400
                            0f at 0
                            -16f at 50
                            16f at 100
                            -12f at 150
                            12f at 200
                            -6f at 250
                            6f at 300
                            0f at 400
                        }
                    )
                }
            }
            Column(
                modifier = Modifier
                    .offset(x = shakeOffset.value.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f), MaterialTheme.shapes.large)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            if (feedback.isCorrect) AppTheme.tokens.successContainer else MaterialTheme.colorScheme.errorContainer,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (feedback.isCorrect) Icons.Filled.Check else Icons.Filled.Close,
                        contentDescription = null,
                        tint = if (feedback.isCorrect) AppTheme.tokens.success else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(34.dp)
                    )
                }
                // The whole point of granting XP live (see GameViewModel/
                // OnlineGameViewModel.submitGuess) is that the player sees it
                // land on THIS word, not just the level badge ticking up in
                // their peripheral vision. Absent for the daily challenge,
                // which pays its own reward at the end instead (xpAwarded is
                // 0 there) — see GuessFeedback's doc.
                if (feedback.xpAwarded > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.xp_gained_format, feedback.xpAwarded),
                        style = MaterialTheme.typography.labelLarge,
                        color = AppTheme.tokens.gold,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (!feedback.isCorrect) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(
                            R.string.correct_answer_was,
                            feedback.correctAnswer.capitalizeForWordLanguage(wordLanguage)
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/** A touch brighter than [AppTheme.tokens.success] — the "still sparkling" top band. */
private val XpBonusSparkleGreen = Color(0xFF4FD97D)
private val XpBonusYellow = Color(0xFFE0C22E)

/**
 * Live preview of the speed bonus answering right now would earn — mirrors
 * XpAwards.wordXp's own 2/4/6-second thresholds exactly, so this is never a
 * promise the actual award can miss, just that formula made visible while
 * the clock is still running. The colour keeps escalating past the point
 * the number hits zero: a still-timed bar in red is "you're out of bonus,
 * hurry anyway" rather than the badge going dark and looking broken.
 */
@Composable
private fun LiveXpBonusBadge(secondsLeft: Int, totalSeconds: Int, modifier: Modifier = Modifier) {
    val elapsed = (totalSeconds - secondsLeft).coerceAtLeast(0)
    val bonus = when {
        elapsed < 2 -> 3
        elapsed < 4 -> 2
        elapsed < 6 -> 1
        else -> 0
    }
    val stage = when {
        elapsed < 2 -> 0
        elapsed < 4 -> 1
        elapsed < 6 -> 2
        elapsed < 8 -> 3
        else -> 4
    }
    val targetColor = when (stage) {
        0 -> XpBonusSparkleGreen
        1 -> AppTheme.tokens.success
        2 -> XpBonusYellow
        3 -> OrangeDeep
        else -> TimerWarning
    }
    val color by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 400),
        label = "xpBonusColor"
    )
    // Only the top band actually sparkles — a gentle pulse marking the one
    // band worth rushing to stay in, rather than decoration on all five.
    val sparkle = rememberInfiniteTransition(label = "xpBonusSparkle")
    val sparklePulse by sparkle.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "xpBonusSparkleAlpha"
    )
    val glow = if (stage == 0) sparklePulse else 1f
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.16f * glow))
            .border(1.dp, color.copy(alpha = glow), CircleShape)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = stringResource(R.string.xp_gained_format, bonus),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}
