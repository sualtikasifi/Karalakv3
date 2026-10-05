package com.sualtikasifi.cizimhafiza.presentation.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.sualtikasifi.cizimhafiza.presentation.common.ButtonOrange
import com.sualtikasifi.cizimhafiza.presentation.common.LetteredText
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedStyle
import com.sualtikasifi.cizimhafiza.presentation.common.StretchBackground
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
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
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
    onAdUnavailableShown: () -> Unit = {},
    /** The tutorial only: dim everything but one joker button and ask the player to use it. */
    jokerSpotlight: JokerSpotlight? = null,
    onBackClick: () -> Unit = {}
) {
    val wordLanguage = currentWordLanguage()
    val backDescription = stringResource(R.string.cd_back)
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
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    LaunchedEffect(state.guessNumber, jokerSpotlight != null) {
        if (jokerSpotlight != null) {
            // The tutorial lesson: no keyboard, so the joker button is the only thing there is to do.
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        } else {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    var spotlightHole by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    val noRipple = remember { MutableInteractionSource() }
    Box(modifier = Modifier.fillMaxSize()) {
        // The painted desk scene (bg_guess), 841 art units wide. The header sits on the top band, the answer tray and
        // the joker row on the bottom band, and the drawing frame in between stretches to whatever height is left. With
        // the keyboard up, the desk props under the joker row and the lamp above the header are dropped so the drawing
        // keeps as much room as possible.
        BoxWithConstraints(modifier = Modifier.fillMaxSize().imePadding()) {
            val unit = maxWidth / ArtW
            fun a(v: Float): Dp = unit * v
            val boxHeight = maxHeight
            val compact = WindowInsets.ime.getBottom(LocalDensity.current) > 0
            // 0 = keyboard down, 1 = keyboard up. The scene does not snap between its two layouts the moment the first
            // pixel of keyboard appears (the drawing frame used to stretch and the header jump while the keyboard
            // slid); it eases from one to the other in step with the keyboard instead.
            val compactT by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (compact) 1f else 0f,
                animationSpec = androidx.compose.animation.core.tween(240, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                label = "guessKeyboard"
            )
            val inset = with(LocalDensity.current) { WindowInsets.statusBars.getTop(this).toDp() }
            val f = (maxWidth.value / 411f).coerceIn(0.85f, 1.25f)
            val ink = Color(0xFF3A2A22)
            // Where the pills of the header should start: just under the status bar.
            val headTop = inset + 8.dp
            val topFromUp = (HeaderTop - headTop / unit).coerceIn(0f, HeaderTop)
            val shiftDown = (headTop - a(HeaderTop)).coerceAtLeast(0.dp)
            val topFrom = topFromUp * compactT
            val shift = shiftDown * (1f - compactT)
            val bottomTo = ArtH + (1595f - ArtH) * compactT
            fun yTop(v: Float): Dp = shift + a(v - topFrom)
            fun yBottom(v: Float): Dp = boxHeight - a(bottomTo - v)

            StretchBackground(
                res = R.drawable.bg_guess,
                artHeight = ArtH,
                topFrom = topFrom,
                topEnd = TopBandEnd,
                bottomStart = BottomBandStart,
                bottomTo = bottomTo,
                shift = shift,
                modifier = Modifier.fillMaxSize()
            )

            // --- Header ---
            Box(
                modifier = Modifier
                    .offset(a(22f), yTop(155f))
                    .size(a(84f), a(78f))
                    .clickable(interactionSource = noRipple, indication = null, onClick = onBackClick)
                    .semantics { contentDescription = backDescription }
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.offset(a(118f), yTop(158f)).size(a(114f), a(74f))
            ) {
                Text(
                    text = "${state.guessNumber} / ${state.totalGuesses}",
                    style = PaintedStyle(color = ink, fontSize = 17.sp * f, textAlign = TextAlign.Center),
                    maxLines = 1
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset(a(246f), yTop(158f))
                    .size(a(76f), a(74f))
                    .clickable(interactionSource = noRipple, indication = null, onClick = onToggleMusic)
            ) {
                Icon(
                    imageVector = if (musicEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = stringResource(if (musicEnabled) R.string.cd_music_on else R.string.cd_music_off),
                    tint = ink,
                    modifier = Modifier.size(24.dp * f)
                )
            }
            // Absent only for the first-launch tutorial's practice round, which has no real ViewModel/XP behind it to
            // show. Hidden while the keyboard is up: it is the widest thing in this row.
            if (compactT < 0.99f) {
                levelProgress?.let {
                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier.offset(a(334f), yTop(150f)).size(a(190f), a(90f)).graphicsLayer { alpha = 1f - compactT }
                    ) {
                        LiveLevelBadge(progress = it, frame = selectedFrame ?: AvatarFrame.highestUnlockedFor(it.level))
                    }
                }
            }
            // totalSeconds == 0 means this guess turn is untimed (the first-launch tutorial) — an empty ring reading
            // "0" would look like an expired timer, so show nothing instead.
            if (state.totalSeconds > 0) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.offset(a(532f), yTop(158f)).size(a(184f), a(74f))
                ) {
                    LiveXpBonusBadge(secondsLeft = state.secondsLeft)
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .offset(a(722f), yTop(150f))
                        .size(a(98f))
                        .shadow(2.dp, CircleShape)
                        .background(Color.White, CircleShape)
                        .border(1.dp, Color(0x33000000), CircleShape)
                ) {
                    CircularCountdown(
                        secondsLeft = state.secondsLeft,
                        totalSeconds = state.totalSeconds,
                        ringColor = timerColor,
                        trackColor = Color(0xFFFFE3CC),
                        strokeWidth = 5.dp,
                        textStyle = PaintedStyle(color = timerColor, fontSize = 22.sp * f),
                        modifier = Modifier.fillMaxSize().padding(2.dp)
                    )
                }
            }

            // --- The drawing ---
            val canvasTop = yTop(310f)
            val canvasBottom = yBottom(1112f)
            Box(
                modifier = Modifier
                    .offset(a(66f), canvasTop)
                    .size(a(724f), (canvasBottom - canvasTop).coerceAtLeast(60.dp))
                    .clip(RoundedCornerShape(a(16f)))
            ) {
                StrokeCanvas(strokes = state.strokes, modifier = Modifier.fillMaxSize())

                // Correct/wrong feedback is drawn ON TOP of the canvas rather than appended below it, so the layout
                // never moves when an answer lands.
                GuessFeedbackOverlay(
                    visible = isAnswered,
                    feedback = state.feedback,
                    guessNumber = state.guessNumber,
                    wordLanguage = wordLanguage,
                    modifier = Modifier.align(Alignment.Center)
                )

                // Revealed hints sit on the drawing itself instead of taking rows of their own below it.
                //
                // The letter-count joker draws one blank per letter, hangman style, with a gap between words. If the
                // first-letter joker (or the ad hint) was used as well, that letter is written over the first blank.
                // On its own the first letter keeps its plain "İlk harf: B" badge.
                val blankGroups = state.letterGroups ?: state.letterCount?.let { listOf(it) }
                val firstLetterShown = state.hintLetter?.capitalizeForWordLanguage(wordLanguage)
                if (blankGroups != null) {
                    LetterBlanks(
                        groups = blankGroups,
                        firstLetter = firstLetterShown,
                        modifier = Modifier.align(Alignment.TopCenter).padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                } else if (firstLetterShown != null) {
                    // Capitalized the same way the word itself is displayed everywhere else — a Turkish "i" has to
                    // become "İ", not "I".
                    TintedBadge(
                        text = stringResource(R.string.hint_first_letter, firstLetterShown),
                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                    )
                }
            }

            // --- Answer tray ---
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.offset(a(58f), yBottom(1273f)).size(a(484f), a(92f))
            ) {
                BasicTextField(
                    value = answer,
                    onValueChange = { newValue ->
                        // Ignore edits once answered instead of toggling enabled/readOnly, so the field never loses focus.
                        if (!isAnswered) {
                            answer = newValue
                            onAnswerChanged(newValue)
                        }
                    },
                    singleLine = true,
                    textStyle = PaintedStyle(color = ink, fontSize = 21.sp * f, textAlign = TextAlign.Center),
                    cursorBrush = SolidColor(ButtonOrange),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { if (!isAnswered && answer.isNotBlank()) onSubmit(answer) }
                    ),
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                            if (answer.isEmpty()) {
                                // "Bu neydi?" used to be a title of its own above the buttons; as the placeholder it
                                // costs no height at all.
                                Text(
                                    text = stringResource(R.string.what_did_you_draw),
                                    style = PaintedStyle(color = Color(0xFF9C8F82), fontSize = 21.sp * f, textAlign = TextAlign.Center),
                                    maxLines = 1
                                )
                            }
                            inner()
                        }
                    }
                )
            }
            val canSubmit = !isAnswered && answer.isNotBlank()
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset(a(572f), yBottom(1265f))
                    .size(a(225f), a(105f))
                    .clickable(interactionSource = noRipple, indication = null, enabled = canSubmit) { onSubmit(answer) }
            ) {
                if (!canSubmit) {
                    Box(
                        Modifier.fillMaxSize().background(Color(0x99EADBC8), RoundedCornerShape(50))
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.alpha(if (canSubmit) 1f else 0.7f)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp * f))
                    LetteredText(stringResource(R.string.submit_guess), 19.sp * f, outline = Color(0xFF8A3A00))
                }
            }

            // --- Joker row: ad hint, the two jokers, skip. Kept on screen (merely disabled) through the isAnswered
            // pause so nothing reflows between words. ---
            val tileTop = yBottom(1406f)
            val tileH = a(164f)
            val firstType = com.sualtikasifi.cizimhafiza.domain.model.JokerType.FIRST_LETTER
            val countType = com.sualtikasifi.cizimhafiza.domain.model.JokerType.LETTER_COUNT
            val firstCount = jokers[firstType] ?: 0
            val letterCountCount = jokers[countType] ?: 0
            val hintAvailable = !state.hintUsed && state.hintLetter == null && GameConstants.ADMOB_ENABLED
            JokerTile(
                x = a(48f), top = tileTop, width = a(172f), height = tileH,
                label = stringResource(if (hintRequested) R.string.loading_hint else R.string.guess_tile_hint),
                labelColor = Color(0xFFB4501A), outline = null,
                enabled = hintAvailable && !hintRequested && !isAnswered,
                badge = null, badgeColor = Color.Unspecified,
                onClick = {
                    // Countdown is paused (see useHint) the instant this is tapped, so the label changes to make clear
                    // something is happening.
                    if (!hintRequested) {
                        hintRequested = true
                        onHintClick()
                    }
                },
                icon = { Icon(Icons.Filled.PlayCircle, contentDescription = null, tint = Color(0xFFF26A1B), modifier = Modifier.size(a(66f))) }
            )
            JokerTile(
                x = a(237f), top = tileTop, width = a(171f), height = tileH,
                label = stringResource(firstType.shortRes()),
                labelColor = Color.White, outline = Color(0xFF4A1F8A),
                enabled = !isAnswered && state.hintLetter == null && firstCount > 0,
                badge = firstCount, badgeColor = Color(0xFF6A2DBF),
                modifier = Modifier.onGloballyPositioned {
                    if (jokerSpotlight?.type == firstType) spotlightHole = it.boundsInWindow()
                },
                onClick = onFirstLetterJoker,
                icon = { JokerArt(firstType, a(66f)) }
            )
            JokerTile(
                x = a(426f), top = tileTop, width = a(178f), height = tileH,
                label = stringResource(countType.shortRes()),
                labelColor = Color.White, outline = Color(0xFF0B4F8A),
                enabled = !isAnswered && state.letterCount == null && letterCountCount > 0,
                badge = letterCountCount, badgeColor = Color(0xFF1B7FE0),
                modifier = Modifier.onGloballyPositioned {
                    if (jokerSpotlight?.type == countType) spotlightHole = it.boundsInWindow()
                },
                onClick = onLetterCountJoker,
                icon = { JokerArt(countType, a(66f)) }
            )
            JokerTile(
                x = a(621f), top = tileTop, width = a(176f), height = tileH,
                label = stringResource(R.string.skip_guess),
                labelColor = Color(0xFF4A2A10), outline = null,
                enabled = !isAnswered,
                badge = null, badgeColor = Color.Unspecified,
                onClick = { onSubmit("") },
                icon = { Icon(Icons.Filled.SkipNext, contentDescription = null, tint = Color(0xFF4A2A10), modifier = Modifier.size(a(66f))) }
            )
            if (adErrorShown) {
                Text(
                    text = stringResource(R.string.ad_unavailable),
                    style = PaintedStyle(color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center),
                    modifier = Modifier
                        .offset(a(60f), tileTop - 22.dp)
                        .background(Color(0xCC8A2A10), RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 3.dp)
                )
            }
        }
        jokerSpotlight?.let { JokerSpotlightOverlay(it, spotlightHole) }
    }
}

private const val ArtW = 841f
private const val ArtH = 1870f
/** Top of the header pills in the picture. */
private const val HeaderTop = 158f
private const val TopBandEnd = 400f
private const val BottomBandStart = 985f

/**
 * One tile of the joker row, drawn over the painted tile of the scene: the icon on top, the label under it and, for the
 * two jokers, how many are left in a white badge on the corner. A tile that can't be used right now is veiled.
 */
@Composable
private fun JokerTile(
    x: androidx.compose.ui.unit.Dp,
    top: androidx.compose.ui.unit.Dp,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    label: String,
    labelColor: Color,
    outline: Color?,
    enabled: Boolean,
    badge: Int?,
    badgeColor: Color,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(stiffness = 600f),
        label = "tileScale"
    )
    Box(
        modifier = modifier
            .offset(x, top)
            .size(width, height)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            icon()
            Spacer(modifier = Modifier.height(2.dp))
            val size = (height.value * 0.115f).coerceIn(12f, 17f)
            if (outline != null) {
                LetteredText(label, size.sp, outline = outline)
            } else {
                Text(label, style = PaintedStyle(color = labelColor, fontSize = size.sp, textAlign = TextAlign.Center), maxLines = 1)
            }
        }
        if (badge != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = height * 0.04f, end = width * 0.03f)
                    .size(height * 0.245f)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.5.dp, badgeColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badge.toString(),
                    style = PaintedStyle(color = badgeColor, fontSize = 13.sp, textAlign = TextAlign.Center),
                    maxLines = 1
                )
            }
        }
        if (!enabled) {
            Box(Modifier.fillMaxSize().background(Color(0x99F1E3D0), RoundedCornerShape(height * 0.17f)))
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
                                val pop = remember { androidx.compose.animation.core.Animatable(0.3f) }
                                LaunchedEffect(letter) {
                                    pop.animateTo(
                                        1f,
                                        androidx.compose.animation.core.spring(
                                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy
                                        )
                                    )
                                }
                                Text(
                                    modifier = Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value },
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
 * Live preview of the speed bonus answering right now would earn, read
 * straight off the countdown number the player is already looking at: 10-8
 * seconds left → +3, 7-6 → +2, 5-4 → +1, below that → 0. Deliberately keyed
 * to [secondsLeft] itself (GUESS_DURATION_SECONDS is always 10 for this
 * screen) rather than re-derived from XpAwards.wordXp's own millisecond
 * thresholds — the two used to disagree by up to a second depending on
 * exactly when within a tick the countdown re-rendered, which is what made
 * an earlier version of this badge feel like it was lying. Matching the
 * visible number the player is timing themselves against is what actually
 * reads as honest, even though it means this is its own small ladder rather
 * than a mirror of the scoring formula.
 */
@Composable
private fun LiveXpBonusBadge(secondsLeft: Int, modifier: Modifier = Modifier) {
    val bonus = when {
        secondsLeft >= 8 -> 3
        secondsLeft >= 6 -> 2
        secondsLeft >= 4 -> 1
        else -> 0
    }
    val stage = when {
        secondsLeft >= 8 -> 0
        secondsLeft >= 6 -> 1
        secondsLeft >= 4 -> 2
        secondsLeft >= 2 -> 3
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
            // A solid pill in the stage colour with white lettering: the old 16%-tint with coloured text all but
            // vanished on the painted orange board behind it.
            .background(color.copy(alpha = 0.78f + 0.22f * glow))
            .border(1.5.dp, Color.White.copy(alpha = 0.85f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(
            // "bonus", not the plain xp_gained_format used for the actual
            // post-answer award (GuessScreen's feedback text) — this number
            // is on TOP of the word's own base XP, not the whole reward, and
            // the badge used to read exactly like a total.
            text = stringResource(R.string.xp_live_bonus_format, bonus),
            style = com.sualtikasifi.cizimhafiza.presentation.common.PaintedStyle(
                color = Color.White,
                fontSize = 14.sp,
                shadow = androidx.compose.ui.graphics.Shadow(Color(0x55000000), androidx.compose.ui.geometry.Offset(0f, 1.5f), 2f)
            )
        )
    }
}
