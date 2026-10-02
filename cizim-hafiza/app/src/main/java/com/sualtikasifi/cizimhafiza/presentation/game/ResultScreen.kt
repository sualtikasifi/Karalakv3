package com.sualtikasifi.cizimhafiza.presentation.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Palette
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Videocam
import kotlinx.coroutines.launch
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.AvatarFrame
import com.sualtikasifi.cizimhafiza.domain.model.ResultItem
import com.sualtikasifi.cizimhafiza.domain.model.DrawingReportReason
import com.sualtikasifi.cizimhafiza.presentation.common.LevelAvatar
import com.sualtikasifi.cizimhafiza.presentation.common.PrimaryButton
import com.sualtikasifi.cizimhafiza.presentation.common.RaisedCard
import com.sualtikasifi.cizimhafiza.presentation.common.RatingPromptDialog
import com.sualtikasifi.cizimhafiza.presentation.common.SignInPromptDialog
import com.sualtikasifi.cizimhafiza.util.DailyChallengeShareUtil
import com.sualtikasifi.cizimhafiza.presentation.common.RaisedIconButton
import com.sualtikasifi.cizimhafiza.presentation.common.ReportDrawingDialog
import com.sualtikasifi.cizimhafiza.presentation.common.ReportSendState
import com.sualtikasifi.cizimhafiza.presentation.common.SecondaryButton
import com.sualtikasifi.cizimhafiza.presentation.common.TintedBadge
import com.sualtikasifi.cizimhafiza.presentation.common.StatPill
import com.sualtikasifi.cizimhafiza.presentation.common.ReplayableDrawing
import com.sualtikasifi.cizimhafiza.presentation.common.StrokeCanvas
import com.sualtikasifi.cizimhafiza.presentation.common.currentWordLanguage
import com.sualtikasifi.cizimhafiza.presentation.common.screenBackground
import com.sualtikasifi.cizimhafiza.presentation.theme.AppTheme
import com.sualtikasifi.cizimhafiza.util.DrawingShareUtil
import com.sualtikasifi.cizimhafiza.util.capitalizeForWordLanguage
import com.sualtikasifi.cizimhafiza.util.GameConstants

@Composable
fun ResultScreen(
    state: GamePhase.Result,
    onPlayAgain: () -> Unit,
    onMainMenu: () -> Unit,
    onLevelNextAction: (() -> Unit)? = null,
    nextActionLabel: String? = null,
    onDoubleXp: (() -> Unit)? = null,
    /** Whether this round's doubling ad has already been taken — see GameViewModel.resultXpDoubled. */
    xpDoubled: Boolean = false,
    /** Quick match only: the opponent's own drawings, empty until they load. */
    ghostItems: List<ResultItem> = emptyList(),
    /** Quick match only: go and find a different opponent. */
    onFindAnotherOpponent: (() -> Unit)? = null,
    /** Quick match only: report one of the opponent's drawings. */
    onReportOpponentDrawing: ((ResultItem, DrawingReportReason) -> Unit)? = null,
    reportState: ReportSendState = ReportSendState.Idle,
    onDismissReport: () -> Unit = {},
    /** Called once, only when the rating prompt's "Puanla" is actually tapped — see RatingPromptDialog. */
    onRatingBonusGranted: () -> Unit = {},
    /** Only when [state].duelChallenger is set: send a fresh challenge back to that same person. */
    onRematchDuel: ((opponentUid: String, opponentName: String) -> Unit)? = null,
    levelProgress: com.sualtikasifi.cizimhafiza.domain.model.LevelProgressState? = null,
    /** The player's own username, headed over their drawings. */
    myName: String = "",
    myFrame: AvatarFrame = AvatarFrame.DEFAULT,
    myAvatarUrl: String = ""
) {
    var previewItem by remember { mutableStateOf<ResultItem?>(null) }
    // The drawing the share-as-photo-or-video choice is open for, and whether its video is being made.
    var shareChoiceFor by remember { mutableStateOf<ResultItem?>(null) }
    var videoPreparing by remember { mutableStateOf(false) }
    val shareScope = androidx.compose.runtime.rememberCoroutineScope()
    var reportItem by remember { mutableStateOf<ResultItem?>(null) }
    // Local, not derived from `state`: the phase itself only ever decides
    // whether a prompt is ELIGIBLE to show (once, per PostMatchPrompts) —
    // whether it is still ON SCREEN right now is this composable's own,
    // since the underlying state never flips back to false once true and a
    // dismissed dialog must not reappear on the next recomposition.
    var ratingPromptDismissed by remember { mutableStateOf(false) }
    var signInPromptDismissed by remember { mutableStateOf(false) }
    // Quick match only: which side of the match the gallery is showing.
    // Starts on the player's own drawings — they just made them, and their
    // own round is what they came to see first.
    var showingOpponentGallery by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val wordLanguage = currentWordLanguage()

    val shownXp = state.xpEarned * (if (xpDoubled) 2 else 1)
    // A round that was already doubled when this screen (re)opened stays put;
    // only a double taken here sends the player home.
    val startedDoubled = remember { xpDoubled }
    LaunchedEffect(xpDoubled) {
        if (xpDoubled && !startedDoubled) {
            delay(1_600)
            onMainMenu()
        }
    }

    // One screen, no scrolling: the summary on top, every drawing (yours and the opponent's)
    // in whatever room is left, the two claim buttons pinned underneath.
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .screenBackground()
                .padding(padding)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ResultSummaryCard(state = state, shownXp = shownXp, xpDoubled = xpDoubled)

            if (levelProgress != null) {
                Spacer(modifier = Modifier.height(6.dp))
                ResultLevelCard(progress = levelProgress, gainedXp = shownXp)
            }

            state.duelOpponentName?.let { opponentName ->
                Spacer(modifier = Modifier.height(6.dp))
                RaisedCard(corner = 18.dp, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.duel_challenge_sent, opponentName),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(12.dp)
                    )
                }
            }

            state.ghost?.let { ghost ->
                Spacer(modifier = Modifier.height(6.dp))
                GhostVersusCard(
                    ghost = ghost,
                    playerScore = state.totalScore,
                    myName = myName,
                    myLevel = levelProgress?.level ?: 1,
                    myFrame = myFrame,
                    myAvatarUrl = myAvatarUrl
                )
            }

            state.duelChallenger?.let { duel ->
                Spacer(modifier = Modifier.height(6.dp))
                DuelChallengerVersusCard(
                    duel = duel,
                    playerScore = state.totalScore,
                    onRematch = onRematchDuel?.let { rematch ->
                        { rematch(duel.challengerUid, duel.challengerName) }
                    }
                )
            }

            state.daily?.let { daily ->
                Spacer(modifier = Modifier.height(6.dp))
                DailyChallengeResultCard(
                    daily = daily,
                    correctFlags = state.items.map { it.isCorrect },
                    onShare = {
                        DailyChallengeShareUtil.shareResult(
                            context = context,
                            correctFlags = state.items.map { it.isCorrect },
                            streak = daily.streak
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            ResultDrawings(
                state = state,
                ghostItems = ghostItems,
                onPreview = { previewItem = it },
                wordLanguage = wordLanguage,
                myName = myName,
                modifier = Modifier.weight(1f).fillMaxWidth()
            )

            if (onLevelNextAction != null && nextActionLabel != null) {
                Spacer(modifier = Modifier.height(6.dp))
                SecondaryButton(
                    text = nextActionLabel,
                    onClick = onLevelNextAction,
                    height = 44.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Always ends on the home screen. The XP is already banked: "Ödülü Al" just leaves
            // (and says how much it is), "x2" watches an ad, pays the round a second time, then leaves.
            val canDouble = onDoubleXp != null && state.xpEarned > 0 && GameConstants.ADMOB_ENABLED && !xpDoubled
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                if (canDouble) {
                    PrimaryButton(
                        text = stringResource(R.string.result_x2_button_amount, state.xpEarned * 2),
                        onClick = onDoubleXp!!,
                        face = Color(0xFFF59E0B),
                        height = 54.dp,
                        modifier = Modifier.weight(1f)
                    )
                }
                // Where the player's thumb lands, so the XP can fly from exactly there to the home bar.
                var claimTapWindowPos by remember { mutableStateOf<androidx.compose.ui.geometry.Offset?>(null) }
                var claimTopLeft by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
                var claimCenter by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
                PrimaryButton(
                    text = stringResource(R.string.result_claim_amount, shownXp),
                    onClick = {
                        if (shownXp > 0 && levelProgress != null) {
                            com.sualtikasifi.cizimhafiza.presentation.common.XpFlyBus.start(
                                origin = claimTapWindowPos ?: claimCenter,
                                amount = shownXp,
                                toXp = levelProgress.totalXp
                            )
                        }
                        onMainMenu()
                    },
                    enabled = !(xpDoubled && !startedDoubled),
                    height = 54.dp,
                    modifier = Modifier
                        .weight(1f)
                        .onGloballyPositioned {
                            claimTopLeft = it.positionInWindow()
                            claimCenter = claimTopLeft + androidx.compose.ui.geometry.Offset(it.size.width / 2f, it.size.height / 2f)
                        }
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                                    event.changes.firstOrNull { it.pressed }?.let { claimTapWindowPos = claimTopLeft + it.position }
                                }
                            }
                        }
                )
            }
        }
    }

    val itemToPreview = previewItem
    if (itemToPreview != null) {
        Dialog(
            onDismissRequest = { previewItem = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            BackHandler { previewItem = null }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.94f))
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Replayed rather than shown finished: the order the
                    // strokes went down in is the part of a drawing a
                    // thumbnail throws away, and it is most of what makes
                    // somebody else's attempt funny.
                    ReplayableDrawing(
                        strokes = itemToPreview.strokes,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(MaterialTheme.shapes.large)
                            .background(AppTheme.tokens.canvasPaper)
                    )
                    Text(
                        text = itemToPreview.word.capitalizeForWordLanguage(wordLanguage),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.padding(top = 18.dp, bottom = 20.dp)
                    )
                    // Share your own, report somebody else's. The header
                    // already withholds sharing for the opponent's gallery on
                    // the grounds that their drawings are not the player's to
                    // pass on; this preview used to offer it anyway.
                    if (showingOpponentGallery && onReportOpponentDrawing != null) {
                        SecondaryButton(
                            text = stringResource(R.string.report_drawing_action),
                            onClick = { reportItem = itemToPreview },
                            icon = Icons.Filled.Flag,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        PrimaryButton(
                            text = stringResource(R.string.share_drawing),
                            onClick = { shareChoiceFor = itemToPreview },
                            icon = Icons.Filled.Share,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                RaisedIconButton(
                    icon = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.close),
                    onClick = { previewItem = null },
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                )
            }
        }
    }

    shareChoiceFor?.let { item ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { if (!videoPreparing) shareChoiceFor = null },
            title = { Text(stringResource(R.string.share_drawing_choose)) },
            text = {
                if (videoPreparing) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        Text(
                            text = stringResource(R.string.share_video_preparing),
                            modifier = Modifier.padding(start = 14.dp)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        PrimaryButton(
                            text = stringResource(R.string.share_as_photo),
                            icon = Icons.Filled.Image,
                            onClick = {
                                DrawingShareUtil.shareDrawingOnTemplate(context, item.word, item.strokes)
                                shareChoiceFor = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        SecondaryButton(
                            text = stringResource(R.string.share_as_video),
                            icon = Icons.Filled.Videocam,
                            onClick = {
                                videoPreparing = true
                                shareScope.launch {
                                    com.sualtikasifi.cizimhafiza.util.DrawingVideoExporter
                                        .exportForSharing(context, item.word, item.strokes)
                                        .onSuccess { com.sualtikasifi.cizimhafiza.util.DrawingVideoExporter.share(context, it) }
                                        .onFailure {
                                            android.widget.Toast.makeText(
                                                context, R.string.share_video_failed, android.widget.Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    videoPreparing = false
                                    shareChoiceFor = null
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                if (!videoPreparing) {
                    androidx.compose.material3.TextButton(onClick = { shareChoiceFor = null }) {
                        Text(stringResource(R.string.close))
                    }
                }
            }
        )
    }

    val itemToReport = reportItem
    if (itemToReport != null && onReportOpponentDrawing != null) {
        ReportDrawingDialog(
            word = itemToReport.word.capitalizeForWordLanguage(wordLanguage),
            sendState = reportState,
            onReport = { reason -> onReportOpponentDrawing(itemToReport, reason) },
            onDismiss = {
                reportItem = null
                onDismissReport()
                // The preview goes with it once the report is filed: leaving
                // the player staring at the drawing they just reported invites
                // them to report it again, which counts for nothing.
                if (reportState == ReportSendState.Sent) previewItem = null
            }
        )
    }

    if (state.showRatingPrompt && !ratingPromptDismissed) {
        RatingPromptDialog(
            onRate = {
                onRatingBonusGranted()
                ratingPromptDismissed = true
            },
            onDismiss = { ratingPromptDismissed = true }
        )
    }
    if (state.showSignInPrompt && !signInPromptDismissed) {
        SignInPromptDialog(onDismiss = { signInPromptDismissed = true })
    }
    if (state.chestLost) {
        var chestLostDismissed by remember { mutableStateOf(false) }
        if (!chestLostDismissed) com.sualtikasifi.cizimhafiza.presentation.common.ChestLostDialog(onDismiss = { chestLostDismissed = true })
    }
    state.chestWon?.let { chest ->
        var chestWonDismissed by remember { mutableStateOf(false) }
        if (!chestWonDismissed) {
            com.sualtikasifi.cizimhafiza.presentation.common.ChestWonDialog(chest = chest, onDismiss = { chestWonDismissed = true })
        }
    }
}

/**
 * Who won the quick match, and by how much.
 *
 * Both scores sit side by side rather than as "you scored X, they scored Y":
 * the two rounds were the same ten words under the same clock, so the only
 * thing worth reading here is which column is bigger.
 */
@Composable
private fun GhostVersusCard(
    ghost: GhostMatchSummary,
    playerScore: Int,
    myName: String,
    myLevel: Int,
    myFrame: AvatarFrame,
    myAvatarUrl: String
) {
    val won = playerScore > ghost.opponentScore
    val drew = playerScore == ghost.opponentScore
    val accent = when {
        drew -> MaterialTheme.colorScheme.onSurfaceVariant
        won -> AppTheme.tokens.success
        else -> MaterialTheme.colorScheme.error
    }
    val outcome = stringResource(
        when {
            drew -> R.string.quick_match_drew
            won -> R.string.quick_match_won
            else -> R.string.quick_match_lost
        }
    )

    RaisedCard(corner = 20.dp, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = outcome,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                    .background(accent)
                    .padding(horizontal = 14.dp, vertical = 2.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Duelist(
                    name = stringResource(R.string.quick_match_you_named, myName.ifBlank { stringResource(R.string.quick_match_you) }),
                    level = myLevel,
                    frame = myFrame,
                    photo = com.sualtikasifi.cizimhafiza.presentation.common.avatarPhotoOf(myAvatarUrl),
                    score = playerScore,
                    highlighted = won,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.quick_match_versus),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Duelist(
                    name = ghost.nickname,
                    level = ghost.level,
                    frame = AvatarFrame.resolve(ghost.frameId, ghost.level),
                    photo = com.sualtikasifi.cizimhafiza.presentation.common.AvatarPhoto.Persona(ghost.nickname),
                    score = ghost.opponentScore,
                    highlighted = !won && !drew,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** One side of the Quick Match comparison: picture in its frame, name, the rank/level label, and the score. */
@Composable
private fun Duelist(
    name: String,
    level: Int,
    frame: AvatarFrame,
    photo: com.sualtikasifi.cizimhafiza.presentation.common.AvatarPhoto,
    score: Int,
    highlighted: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        LevelAvatar(level = level, frame = frame, size = 52.dp, photo = photo)
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        com.sualtikasifi.cizimhafiza.presentation.common.RankLevelLabel(level = level, bullet = false)
        Text(
            text = "$score",
            style = MaterialTheme.typography.headlineSmall,
            color = if (highlighted) AppTheme.tokens.success else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun VersusSide(
    name: String,
    score: Int,
    highlighted: Boolean,
    avatar: (@Composable () -> Unit)?
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Only the opponent gets a ring. The player already knows what their
        // own looks like, and a second one here would make the card read as
        // two strangers rather than as "you against them".
        avatar?.invoke()
        if (avatar != null) Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "$score",
            style = MaterialTheme.typography.titleLarge,
            color = if (highlighted) AppTheme.tokens.success else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * The comparison against a duel's challenger — same shape as [GhostVersusCard],
 * a friend instead of a recorded opponent. Shown the instant this round
 * finishes: the challenger's score has been sitting on the duel document
 * since they sent it, so there is nothing left to wait on.
 *
 * [onRematch] is only non-null when the caller actually wants the button
 * offered — see ResultScreen's duelChallenger branch, which only passes one
 * when the screen itself was handed an onRematchDuel callback.
 */
@Composable
private fun DuelChallengerVersusCard(duel: DuelChallengerSummary, playerScore: Int, onRematch: (() -> Unit)?) {
    val won = playerScore > duel.challengerScore
    val drew = playerScore == duel.challengerScore
    val accent = when {
        drew -> MaterialTheme.colorScheme.onSurfaceVariant
        won -> AppTheme.tokens.success
        else -> MaterialTheme.colorScheme.error
    }

    RaisedCard(corner = 20.dp, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(
                    when {
                        drew -> R.string.quick_match_drew
                        won -> R.string.quick_match_won
                        else -> R.string.quick_match_lost
                    }
                ),
                style = MaterialTheme.typography.titleSmall,
                color = accent,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                VersusSide(
                    name = stringResource(R.string.quick_match_you),
                    score = playerScore,
                    highlighted = won,
                    avatar = null
                )
                Text(
                    text = stringResource(R.string.quick_match_versus),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                VersusSide(
                    name = duel.challengerName,
                    score = duel.challengerScore,
                    highlighted = !won && !drew,
                    avatar = null
                )
            }
            if (onRematch != null) {
                Spacer(modifier = Modifier.height(10.dp))
                SecondaryButton(
                    text = stringResource(R.string.duel_rematch_action),
                    onClick = onRematch,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Two chips over the gallery: your ten drawings, or theirs.
 *
 * The opponent's side stays unselectable until their drawings have actually
 * arrived — a chip that switches to the same grid you were already looking at
 * reads as a broken toggle, not as a slow one.
 */
@Composable
private fun GalleryToggle(
    opponentName: String,
    opponentReady: Boolean,
    showingOpponent: Boolean,
    onSelect: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GalleryChip(
            label = stringResource(R.string.quick_match_gallery_yours),
            selected = !showingOpponent,
            enabled = true,
            onClick = { onSelect(false) }
        )
        GalleryChip(
            label = opponentName,
            selected = showingOpponent,
            enabled = opponentReady,
            onClick = { onSelect(true) }
        )
    }
}

@Composable
private fun GalleryChip(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val container = when {
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surface
    }
    val content = when {
        selected -> MaterialTheme.colorScheme.onPrimary
        enabled -> MaterialTheme.colorScheme.onSurface
        else -> AppTheme.tokens.textFaint
    }
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(container)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .clickable(enabled = enabled && !selected, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * The daily-challenge half of the result screen: the streak that was just
 * extended, the XP it paid, and the one action that turns a private result
 * into something a friend sees.
 *
 * The ✅/❌ row is shown here as well as on the share card so what gets
 * posted is exactly what the player is looking at — no surprises about what
 * they're about to reveal.
 */
@Composable
private fun DailyChallengeResultCard(
    daily: DailyResultSummary,
    correctFlags: List<Boolean>,
    onShare: () -> Unit
) {
    RaisedCard(corner = 24.dp, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.daily_challenge_result_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = correctFlags.joinToString(" ") { if (it) "✅" else "❌" },
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                StatPill(text = "🔥 ${daily.streak}", icon = null, contentColor = MaterialTheme.colorScheme.primary)
                StatPill(
                    text = stringResource(R.string.daily_challenge_xp_earned, daily.xpEarned),
                    icon = null,
                    contentColor = AppTheme.tokens.gold
                )
            }
            if (daily.streakMultiplierIncreased) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.daily_challenge_streak_multiplier_increased, daily.streakMultiplier),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            SecondaryButton(
                text = stringResource(R.string.daily_challenge_share),
                onClick = onShare,
                icon = Icons.Filled.Share,
                modifier = Modifier.fillMaxWidth(),
                height = 44.dp
            )
        }
    }
}

/** Where the round left the player on the level ladder, with the bar animating from where it started. */
@Composable
private fun ResultLevelCard(progress: com.sualtikasifi.cizimhafiza.domain.model.LevelProgressState, gainedXp: Int) {
    val before = com.sualtikasifi.cizimhafiza.domain.model.LevelProgressState.forXp(progress.totalXp - gainedXp)
    val leveledUp = before.level < progress.level
    val fraction = remember { androidx.compose.animation.core.Animatable(if (leveledUp) 0f else before.progressFraction) }
    LaunchedEffect(progress.totalXp) {
        fraction.animateTo(progress.progressFraction, androidx.compose.animation.core.tween(1100))
    }
    RaisedCard(corner = 20.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.result_level_label, progress.level),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f)
                )
                if (leveledUp) {
                    TintedBadge(
                        text = stringResource(R.string.result_level_up),
                        container = AppTheme.tokens.gold.copy(alpha = 0.25f),
                        content = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else if (!progress.isMaxLevel) {
                    Text(
                        text = stringResource(R.string.home_xp_to_next, progress.xpToNextLevel, progress.level + 1),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            androidx.compose.material3.LinearProgressIndicator(
                progress = { fraction.value },
                modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.home_xp_format, progress.xpIntoLevel, progress.xpForThisLevel),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * The round's summary in one compact card: the XP earned (big), the one-line sum that explains it,
 * hits / misses / fastest, and a chip for every bonus or gold source — each saying what it is.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ResultSummaryCard(state: GamePhase.Result, shownXp: Int, xpDoubled: Boolean) {
    val base = state.totalScore
    val mult = state.xpMultiplier
    val explanation = when {
        xpDoubled -> stringResource(R.string.result_xp_doubled_note)
        state.daily != null -> stringResource(R.string.result_xp_daily)
        mult > 1 && state.xpEarned == base * mult -> stringResource(R.string.result_xp_formula_mult, base, mult)
        state.xpEarned == base -> stringResource(R.string.result_xp_formula_plain, base)
        else -> stringResource(R.string.result_xp_formula_extra, base)
    }
    RaisedCard(corner = 20.dp, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.game_over),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.xp_gained_format, shownXp),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = explanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            state.levelStars?.let { stars ->
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(3) { index ->
                        Icon(
                            imageVector = if (index < stars) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = stringResource(R.string.stars_content_description, stars),
                            tint = if (index < stars) AppTheme.tokens.gold else AppTheme.tokens.textFaint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                StatPill(text = "${state.correctCount}", icon = Icons.Filled.Check, contentColor = AppTheme.tokens.success)
                StatPill(text = "${state.wrongCount}", icon = Icons.Filled.Close, contentColor = MaterialTheme.colorScheme.error)
                state.fastestCorrectSeconds?.let {
                    StatPill(
                        text = stringResource(R.string.fastest_correct, it),
                        icon = Icons.Filled.Bolt,
                        contentColor = AppTheme.tokens.gold
                    )
                }
            }
            val quick = state.quickMatchDailyBonusApplied
            val eventMult = if (state.xpEventMultiplierApplied) (mult / if (quick) 2 else 1).coerceAtLeast(2) else 0
            val hasChips = quick || eventMult > 0 || state.goldFromAchievements > 0 || state.goldFromLevel > 0 || state.goldFromDaily > 0
            if (hasChips) {
                Spacer(modifier = Modifier.height(6.dp))
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (quick) TintedBadge(text = stringResource(R.string.result_bonus_quick))
                    if (eventMult > 0) TintedBadge(text = stringResource(R.string.result_bonus_event, eventMult))
                    val goldBg = AppTheme.tokens.gold.copy(alpha = 0.2f)
                    if (state.goldFromAchievements > 0) TintedBadge(
                        text = stringResource(R.string.result_gold_achievement, state.goldFromAchievements),
                        container = goldBg, content = AppTheme.tokens.gold
                    )
                    if (state.goldFromLevel > 0) TintedBadge(
                        text = stringResource(R.string.result_gold_level, state.goldFromLevel),
                        container = goldBg, content = AppTheme.tokens.gold
                    )
                    if (state.goldFromDaily > 0) TintedBadge(
                        text = stringResource(R.string.result_gold_daily, state.goldFromDaily),
                        container = goldBg, content = AppTheme.tokens.gold
                    )
                }
            }
        }
    }
}

/**
 * Every drawing of the round, on the result screen itself: yours, and (Hızlı Eşleş) the
 * opponent's under them. Thumbnails are sized from the space available so they all fit without
 * scrolling; tapping one opens the replay.
 */
@Composable
private fun ResultDrawings(
    state: GamePhase.Result,
    ghostItems: List<ResultItem>,
    onPreview: (ResultItem) -> Unit,
    wordLanguage: String,
    myName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val ghost = state.ghost
    val sections = if (ghost != null) 2 else 1
    val count = state.items.size
    androidx.compose.foundation.layout.BoxWithConstraints(modifier = modifier) {
        val gap = 6.dp
        val labelHeight = 26.dp
        var columns = if (count == 5) 3 else 5
        var cell = 0.dp
        // Five drawings (the daily challenge) sit three over two, each row centred, not four over one.
        for (c in if (count == 5) 3..3 else 4..10) {
            val rows = (count + c - 1) / c
            val byWidth = (maxWidth - gap * (c - 1)) / c
            val usedByGaps = gap * (rows * sections + sections * 2) + labelHeight * sections
            val byHeight = (maxHeight - usedByGaps) / (rows * sections)
            val candidate = minOf(byWidth, byHeight, if (sections == 1) 180.dp else 124.dp)
            if (candidate > cell) { cell = candidate; columns = c }
        }
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            DrawingSection(
                title = myName.ifBlank { stringResource(R.string.result_your_drawings) },
                items = state.items,
                columns = columns,
                cell = cell,
                gap = gap,
                labelHeight = labelHeight,
                onPreview = onPreview,
                onShare = {
                    DrawingShareUtil.shareAllResults(
                        context = context,
                        totalScore = state.totalScore,
                        correctCount = state.correctCount,
                        wrongCount = state.wrongCount,
                        fastestCorrectSeconds = state.fastestCorrectSeconds,
                        items = state.items
                    )
                }
            )
            if (ghost != null) {
                DrawingSection(
                    title = ghost.nickname,
                    items = ghostItems,
                    placeholders = count,
                    columns = columns,
                    cell = cell,
                    gap = gap,
                    labelHeight = labelHeight,
                    onPreview = onPreview,
                    onShare = null
                )
            }
        }
    }
}

@Composable
private fun DrawingSection(
    title: String,
    items: List<ResultItem>,
    columns: Int,
    cell: androidx.compose.ui.unit.Dp,
    gap: androidx.compose.ui.unit.Dp,
    labelHeight: androidx.compose.ui.unit.Dp,
    onPreview: (ResultItem) -> Unit,
    onShare: (() -> Unit)?,
    placeholders: Int = 0
) {
    Box(
        modifier = Modifier.fillMaxWidth().height(labelHeight),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp)
        )
        if (onShare != null) {
            Icon(
                imageVector = Icons.Filled.Share,
                contentDescription = stringResource(R.string.share_all_drawings),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterEnd).size(20.dp).clickable(onClick = onShare)
            )
        }
    }
    val total = if (items.isEmpty()) placeholders else items.size
    val slots = List(total) { index -> items.getOrNull(index) }
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(gap), horizontalAlignment = Alignment.CenterHorizontally) {
        slots.chunked(columns).forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally)) {
                rowItems.forEach { item ->
                    Box(modifier = Modifier.size(cell)) {
                        if (item == null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(MaterialTheme.shapes.small)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
                            )
                        } else {
                            StrokeCanvas(
                                strokes = item.strokes,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(MaterialTheme.shapes.small)
                                    .background(AppTheme.tokens.canvasPaper)
                                    .border(
                                        1.5.dp,
                                        if (item.isCorrect) AppTheme.tokens.success.copy(alpha = 0.6f) else MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                                        MaterialTheme.shapes.small
                                    )
                                    .clickable { onPreview(item) }
                            )
                            // The mark grows with the drawing it sits on: a green disc with a tick for a right
                            // answer, a red disc with a cross for a wrong one, white-ringed so it reads on any paper.
                            val badge = (cell.value * 0.2f).coerceIn(18f, 30f).dp
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(badge)
                                    .clip(CircleShape)
                                    .background(if (item.isCorrect) AppTheme.tokens.success else MaterialTheme.colorScheme.error)
                                    .border(1.5.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (item.isCorrect) Icons.Filled.Check else Icons.Filled.Close,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(badge * 0.72f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
