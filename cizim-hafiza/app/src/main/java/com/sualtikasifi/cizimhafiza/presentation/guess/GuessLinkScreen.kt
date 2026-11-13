package com.sualtikasifi.cizimhafiza.presentation.guess

import android.app.Activity
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.DrawingStroke
import com.sualtikasifi.cizimhafiza.presentation.common.AppTextField
import com.sualtikasifi.cizimhafiza.presentation.common.DrawableCanvas
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedPage
import com.sualtikasifi.cizimhafiza.presentation.common.PrimaryButton
import com.sualtikasifi.cizimhafiza.presentation.common.RaisedCard
import com.sualtikasifi.cizimhafiza.presentation.common.ReplayStrokeCanvas
import com.sualtikasifi.cizimhafiza.presentation.common.ScreenTopActions
import com.sualtikasifi.cizimhafiza.presentation.common.SecondaryButton
import com.sualtikasifi.cizimhafiza.presentation.common.StrokeCanvas
import com.sualtikasifi.cizimhafiza.presentation.common.TopActionsClearance
import com.sualtikasifi.cizimhafiza.presentation.common.screenBackground
import com.sualtikasifi.cizimhafiza.presentation.theme.AppTheme
import com.sualtikasifi.cizimhafiza.util.GuessCodec
import com.sualtikasifi.cizimhafiza.util.GuessShareUtil

/**
 * "Bil Bakalım": opened from a friend's link (karalak://guess/ID, https://karalak.com/b/ID). Watch the drawing being
 * drawn, guess it in three tries, then draw the same word yourself and send the link on.
 */
@Composable
fun GuessLinkScreen(
    onBack: () -> Unit,
    viewModel: GuessLinkViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity

    // Leaving through the result ends with an ad (never mid-drawing or mid-guess); anywhere else leaves at once.
    val leave: () -> Unit = {
        if (state.phase == GuessPhase.Solved || state.phase == GuessPhase.Failed || state.phase == GuessPhase.Shared) {
            viewModel.withInterstitial(activity, onBack)
        } else {
            onBack()
        }
    }

    androidx.activity.compose.BackHandler(onBack = leave)

    // The share sheet opens by itself once, the moment the new link exists.
    LaunchedEffect(state.newLinkId) {
        val id = state.newLinkId ?: return@LaunchedEffect
        GuessShareUtil.share(context, id, state.word.orEmpty(), state.live)
    }

    // Nothing to guess (own drawing, dead link, offline, turned off): the painted notice scene, not the card page.
    if (state.phase == GuessPhase.Unavailable) {
        GuessNoticeScreen(
            problem = state.problem ?: GuessProblem.NotFound,
            onRetry = viewModel::load,
            onClose = onBack,
            onBack = onBack
        )
        return
    }

    // Watching and guessing: the painted answer scene, not the card page.
    if (state.phase == GuessPhase.Guessing) {
        GuessAnswerScreen(
            state = state,
            onReplay = viewModel::replay,
            onReplayFinished = viewModel::onReplayFinished,
            onInput = viewModel::onInput,
            onSubmit = viewModel::submitGuess,
            onGiveUp = viewModel::giveUp,
            onBack = leave
        )
        return
    }

    // The game's painted page (wood wall, hanging sign, painted back arrow), with the round on a cream card so it reads on
    // the wall. While a drawing is being made the page does not scroll: a vertical stroke must not drag the page with it.
    PaintedPage(
        title = stringResource(R.string.guess_page_title),
        onBack = leave,
        bodyScroll = state.phase != GuessPhase.Drawing
    ) {
        RaisedCard(corner = 24.dp, face = AppTheme.tokens.cardWarm, modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (state.phase) {
                    GuessPhase.Loading -> Box(Modifier.fillMaxWidth().height(280.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    GuessPhase.Unavailable -> Unit
                    GuessPhase.Guessing -> Unit
                    GuessPhase.Solved, GuessPhase.Failed -> Verdict(state, viewModel, onClose = leave)
                    GuessPhase.Drawing -> Drawing(state, viewModel)
                    GuessPhase.Shared -> Shared(state, onShare = {
                        state.newLinkId?.let { GuessShareUtil.share(context, it, state.word.orEmpty(), state.live) }
                    }, onClose = leave)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

/** The white card a drawing sits on. */
@Composable
private fun PaperCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(24.dp))
            .background(AppTheme.tokens.canvasPaper),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun Header(text: String, sub: String? = null) {
    Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    if (sub != null) {
        Spacer(Modifier.height(4.dp))
        Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
    Spacer(Modifier.height(14.dp))
}

@Composable
private fun Icon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    androidx.compose.material3.Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp).padding(end = 4.dp))
}

@Composable
private fun Verdict(state: GuessUiState, vm: GuessLinkViewModel, onClose: () -> Unit) {
    val link = state.link ?: return
    val solved = state.solved
    val word = state.word
    Header(
        if (solved) stringResource(R.string.guess_solved) else stringResource(R.string.guess_failed),
        when {
            solved && state.attempts == 1 -> stringResource(R.string.guess_solved_first)
            solved -> stringResource(R.string.guess_solved_tries, state.attempts)
            word != null -> stringResource(R.string.guess_answer, word)
            else -> null
        }
    )
    PaperCard { StrokeCanvas(strokes = link.strokes, modifier = Modifier.fillMaxSize()) }
    if (state.limitReached || state.shareFailed) {
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(if (state.limitReached) R.string.guess_limit else R.string.guess_share_failed),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
    Spacer(Modifier.height(18.dp))
    if (word != null) {
        Text(
            stringResource(R.string.guess_your_turn_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        PrimaryButton(
            text = stringResource(R.string.guess_your_turn),
            onClick = { vm.dismissNotice(); vm.startDrawing() },
            icon = Icons.Filled.Brush,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
    }
    SecondaryButton(text = stringResource(R.string.close), onClick = onClose, modifier = Modifier.fillMaxWidth())
    TextButton(onClick = vm::report, enabled = !state.reported) {
        Text(stringResource(if (state.reported) R.string.guess_reported else R.string.guess_report))
    }
}

@Composable
private fun Drawing(state: GuessUiState, vm: GuessLinkViewModel) {
    val word = state.word.orEmpty()
    Header(stringResource(R.string.guess_draw_title, word.uppercase()), stringResource(R.string.guess_draw_seconds, state.secondsLeft))
    PaperCard {
        DrawableCanvas(
            liveStrokes = state.live,
            onStrokeFinished = { stroke: DrawingStroke -> vm.onStroke(stroke) },
            modifier = Modifier.fillMaxSize()
        )
    }
    Spacer(Modifier.height(14.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        SecondaryButton(text = stringResource(R.string.guess_undo), onClick = vm::undoStroke, enabled = state.live.isNotEmpty() && !state.busy, modifier = Modifier.weight(1f))
        PrimaryButton(text = stringResource(R.string.guess_done), onClick = vm::finishDrawing, enabled = state.live.isNotEmpty() && !state.busy, modifier = Modifier.weight(1f))
    }
    if (state.busy) {
        Spacer(Modifier.height(10.dp))
        CircularProgressIndicator(modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun Shared(state: GuessUiState, onShare: () -> Unit, onClose: () -> Unit) {
    Header(stringResource(R.string.guess_shared_title), stringResource(R.string.guess_shared_sub))
    PaperCard { StrokeCanvas(strokes = state.live, modifier = Modifier.fillMaxSize()) }
    Spacer(Modifier.height(18.dp))
    PrimaryButton(text = stringResource(R.string.guess_share_again), onClick = onShare, icon = Icons.Filled.Share, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(8.dp))
    SecondaryButton(text = stringResource(R.string.close), onClick = onClose, modifier = Modifier.fillMaxWidth())
}
