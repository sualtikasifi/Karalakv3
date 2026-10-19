package com.sualtikasifi.cizimhafiza.presentation.tutorial

import androidx.compose.foundation.Image
import com.sualtikasifi.cizimhafiza.presentation.common.floating
import com.sualtikasifi.cizimhafiza.presentation.common.springIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.clickable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.presentation.common.PrimaryButton
import com.sualtikasifi.cizimhafiza.presentation.common.RaisedCard
import com.sualtikasifi.cizimhafiza.presentation.common.screenBackground
import com.sualtikasifi.cizimhafiza.presentation.game.DrawingScreen
import com.sualtikasifi.cizimhafiza.presentation.game.GamePhase
import com.sualtikasifi.cizimhafiza.presentation.game.GuessScreen

/**
 * First-launch walkthrough: the real DrawingScreen/GuessScreen composables
 * driven by [TutorialViewModel]'s hardcoded 3-word run, with a full-screen
 * coaching card between turns. Nothing here is scored or saved.
 */
@Composable
fun TutorialScreen(
    onFinished: () -> Unit,
    viewModel: TutorialViewModel = hiltViewModel()
) {
    val phase by viewModel.phase.collectAsState()
    val coach by viewModel.coach.collectAsState()
    val isFinished by viewModel.isFinished.collectAsState()
    val jokers by viewModel.jokers.collectAsState()
    val spotlight by viewModel.spotlight.collectAsState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(isFinished) {
        if (isFinished) {
            viewModel.completeTutorial()
            onFinished()
        }
    }

    // The guess screen's answer field keeps keyboard focus by design (see
    // GuessScreen), so a coaching card popping up over it — most notably the
    // finale, right after the last guess — would otherwise leave the
    // keyboard open behind the card.
    LaunchedEffect(coach) {
        if (coach != null) {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val current = phase) {
            is GamePhase.Drawing -> DrawingScreen(
                state = current,
                onStrokeFinished = viewModel::onStrokeFinished,
                onStrokeProgress = viewModel::onStrokeProgress,
                onClearCanvas = viewModel::onClearCanvas,
                onEraseStroke = viewModel::onEraseStroke,
                onUndoLastStroke = viewModel::onUndoLastStroke,
                onNextWord = viewModel::advanceUntimedDrawing,
                // Leaving mid-tutorial just marks it done and drops the
                // player at the main menu — no exit-confirm dialog needed
                // since there's no real progress to lose.
                onBackClick = {
                    viewModel.completeTutorial()
                    onFinished()
                }
            )

            is GamePhase.Guessing -> GuessScreen(
                state = current,
                onSubmit = viewModel::submitGuess,
                onAnswerChanged = viewModel::onAnswerChanged,
                jokers = jokers,
                onFirstLetterJoker = viewModel::useFirstLetterJoker,
                onLetterCountJoker = viewModel::useLetterCountJoker,
                jokerSpotlight = spotlight,
                onBackClick = {
                    viewModel.completeTutorial()
                    onFinished()
                }
            )

            else -> Box(
                modifier = Modifier.fillMaxSize().screenBackground(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        }

        coach?.let { message ->
            val skip = {
                viewModel.completeTutorial()
                onFinished()
            }
            if (message.titleRes == R.string.tutorial_intro_title) {
                // The very first card is the full welcome scene (painted workshop + card), not the dark scrim.
                WelcomeScene(onStart = viewModel::dismissCoach, onSkip = skip)
            } else {
                CoachOverlay(
                    coach = message,
                    onContinue = viewModel::dismissCoach,
                    onSkip = skip,
                    // The last card has nothing left to skip: it only starts the game.
                    showSkip = message.titleRes != R.string.tutorial_finale_title
                )
            }
        }
    }
}

@Composable
private fun CoachOverlay(
    coach: TutorialCoach,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    showSkip: Boolean = true
) {
    // Opaque scrim: while a coaching card is up the timer underneath is
    // stopped anyway (see TutorialViewModel), so there's nothing behind it
    // the player needs to keep watching.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2B1A10).copy(alpha = 0.72f))
            // Swallows taps so the screen underneath cannot be drawn on while the card is up.
            .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null, onClick = {}),
        contentAlignment = Alignment.Center
    ) {
        // Three hand-drawn frames (tut_frame_*), picked by how much text the card carries. They are pictures with vines down
        // their sides, so they are shown at their own proportions (never stretched) and the card grows with the frame.
        val body = stringResource(coach.bodyRes)
        val frame = when {
            body.length <= 90 -> Triple(R.drawable.tut_frame_short, 1000f / 773f, listOf(0.12f, 0.20f, 0.12f, 0.15f))
            body.length <= 190 -> Triple(R.drawable.tut_frame_mid, 1000f / 1137f, listOf(0.12f, 0.17f, 0.12f, 0.14f))
            else -> Triple(R.drawable.tut_frame_long, 1000f / 1337f, listOf(0.12f, 0.165f, 0.12f, 0.14f))
        }
        val showImage = coach.imageRes != null && body.length > 90
        androidx.compose.foundation.layout.BoxWithConstraints(
            modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth().padding(horizontal = 12.dp)
                .springIn(key = coach.titleRes, fromY = 50, stepMs = 0)
        ) {
            val w = maxWidth
            val h = w / frame.second
            Image(
                painter = painterResource(frame.first),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.FillBounds,
                modifier = Modifier.width(w).height(h)
            )
            Column(
                modifier = Modifier.width(w).height(h)
                    .padding(start = w * frame.third[0], top = h * frame.third[1], end = w * frame.third[2], bottom = h * frame.third[3]),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (showImage) {
                    Image(
                        painter = painterResource(coach.imageRes!!),
                        contentDescription = null,
                        modifier = Modifier.size(if (body.length > 190) 120.dp else 96.dp).floating(amplitude = 4f, tilt = 3f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
                com.sualtikasifi.cizimhafiza.presentation.common.FitText(
                    text = stringResource(coach.titleRes),
                    style = com.sualtikasifi.cizimhafiza.presentation.common.PaintedStyle(color = Color(0xFF3A2416), fontSize = 24.sp, textAlign = TextAlign.Center),
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                // The body takes whatever height the frame has left after the title and the button, and shrinks to fit it,
                // so a long text never squeezes the button.
                com.sualtikasifi.cizimhafiza.presentation.common.FitText(
                    text = body,
                    style = com.sualtikasifi.cizimhafiza.presentation.common.DescriptionStyle(16.sp, 22.sp),
                    maxLines = 10,
                    minScale = 0.7f,
                    modifier = Modifier.weight(1f, fill = false).fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(14.dp))
                com.sualtikasifi.cizimhafiza.presentation.common.PaintedPillButton(
                    text = stringResource(coach.buttonRes),
                    onClick = onContinue,
                    height = 52.dp,
                    textSize = 19.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                if (showSkip) {
                    TextButton(onClick = onSkip) {
                        Text(
                            text = stringResource(R.string.tutorial_skip),
                            style = com.sualtikasifi.cizimhafiza.presentation.common.PaintedStyle(color = Color(0xFF7A5A44), fontSize = 15.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}
