package com.sualtikasifi.cizimhafiza.presentation.game

import com.sualtikasifi.cizimhafiza.presentation.common.cachedPainterResource
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import com.sualtikasifi.cizimhafiza.presentation.common.ButtonOrange
import com.sualtikasifi.cizimhafiza.presentation.common.DescriptionStyle
import com.sualtikasifi.cizimhafiza.presentation.common.InkBrown
import com.sualtikasifi.cizimhafiza.presentation.common.LetteredText
import com.sualtikasifi.cizimhafiza.presentation.common.NinePatch
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedStyle
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
import com.sualtikasifi.cizimhafiza.presentation.common.springIn
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
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // The painted pieces shrink a little on a short phone so the drawings keep their room.
        val k = ((maxHeight - 24.dp) / 891.dp).coerceIn(0.68f, 1f)
        val showDesk = maxHeight >= 700.dp
        Image(
            painter = cachedPainterResource(R.drawable.bg_result_wood),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (showDesk) {
            Image(
                painter = cachedPainterResource(R.drawable.res_desk),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(bottom = if (showDesk) 52.dp * k else 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // The three blocks drop in one after another: the card from above, the rest rising into place.
            androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().springIn(index = 0, fromY = -40)) {
                ResultSummaryCard(state = state, shownXp = shownXp, xpDoubled = xpDoubled, k = k)
            }

            if (levelProgress != null) {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().springIn(index = 2, stepMs = 90)) {
                    ResultLevelCard(progress = levelProgress, gainedXp = shownXp, k = k)
                }
            }

            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 14.dp).springIn(index = 4, stepMs = 90),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                state.duelOpponentName?.let { opponentName ->
                    Spacer(modifier = Modifier.height(6.dp))
                    PaperPanel(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.duel_challenge_sent, opponentName),
                            style = PaintedStyle(color = InkBrown, fontSize = 15.sp, textAlign = TextAlign.Center),
                            modifier = Modifier.fillMaxWidth().padding(6.dp)
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
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (onLevelNextAction != null && nextActionLabel != null) {
                    PaintedPill(
                        res = R.drawable.res_btn_next,
                        height = 50.dp * k,
                        onClick = onLevelNextAction,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, tint = Color(0xFFB5441A), modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            nextActionLabel,
                            style = PaintedStyle(color = Color(0xFFA83A18), fontSize = 19.sp, textAlign = TextAlign.Center),
                            maxLines = 1
                        )
                    }
                }

                // Always ends on the home screen. The XP is already banked: "Ödülü Al" just leaves
                // (and says how much it is), "x2" watches an ad, pays the round a second time, then leaves.
                val canDouble = onDoubleXp != null && state.xpEarned > 0 && GameConstants.ADMOB_ENABLED && !xpDoubled
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    if (canDouble) {
                        PaintedPill(
                            res = R.drawable.res_btn_claim,
                            height = 54.dp * k,
                            onClick = onDoubleXp!!,
                            modifier = Modifier.weight(1f)
                        ) {
                            LetteredText(stringResource(R.string.result_x2_button_amount, state.xpEarned * 2), 17.sp, outline = Color(0xFF8A3A00), modifier = Modifier.weight(1f, fill = false))
                        }
                    }
                    // Where the player's thumb lands, so the XP can fly from exactly there to the home bar.
                    var claimTapWindowPos by remember { mutableStateOf<androidx.compose.ui.geometry.Offset?>(null) }
                    var claimTopLeft by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
                    var claimCenter by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
                    PaintedPill(
                        res = R.drawable.res_btn_claim,
                        height = 54.dp * k,
                        enabled = !(xpDoubled && !startedDoubled),
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
                    ) {
                        if (shownXp > 0) {
                            Icon(Icons.Filled.CardGiftcard, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(10.dp))
                        }
                        LetteredText(
                    // Nothing to collect: the button simply moves on instead of offering "0 XP".
                    if (shownXp > 0) stringResource(R.string.result_claim_amount, shownXp) else stringResource(R.string.result_continue),
                    19.sp,
                    outline = Color(0xFF8A3A00),
                    modifier = Modifier.weight(1f, fill = false)
                )
                    }
                }
            }
        }
        // A round that earned something is celebrated with a shower of paper, once.
        if (state.xpEarned > 0 && state.correctCount > 0) {
            com.sualtikasifi.cizimhafiza.presentation.common.ConfettiBurst(seed = state.totalScore * 31 + state.correctCount)
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
        com.sualtikasifi.cizimhafiza.presentation.common.PaintedDialog(
            title = stringResource(R.string.share_drawing_choose),
            onDismiss = { if (!videoPreparing) shareChoiceFor = null },
            buttons = if (videoPreparing) null else {
                { com.sualtikasifi.cizimhafiza.presentation.common.PaintedPillButton(text = stringResource(R.string.close), onClick = { shareChoiceFor = null }, primary = false, modifier = Modifier.fillMaxWidth()) }
            }
        ) {
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
        }
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

    PaperPanel(modifier = Modifier.fillMaxWidth()) {
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

    PaperPanel(modifier = Modifier.fillMaxWidth()) {
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
    PaperPanel(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.daily_challenge_result_title),
                style = PaintedStyle(color = InkBrown, fontSize = 17.sp, textAlign = TextAlign.Center)
            )
            Spacer(modifier = Modifier.height(6.dp))
            com.sualtikasifi.cizimhafiza.presentation.common.DailyPips(
                flags = correctFlags,
                count = correctFlags.size.coerceAtLeast(com.sualtikasifi.cizimhafiza.domain.model.DailyChallenge.WORD_COUNT),
                size = 30.dp,
                emptyColor = Color(0x33795548),
                rimColor = Color(0xFFFFF6E6)
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
private fun ResultLevelCard(progress: com.sualtikasifi.cizimhafiza.domain.model.LevelProgressState, gainedXp: Int, k: Float) {
    val before = com.sualtikasifi.cizimhafiza.domain.model.LevelProgressState.forXp(progress.totalXp - gainedXp)
    val leveledUp = before.level < progress.level
    val fraction = remember { androidx.compose.animation.core.Animatable(if (leveledUp) 0f else before.progressFraction) }
    LaunchedEffect(progress.totalXp) {
        fraction.animateTo(progress.progressFraction, androidx.compose.animation.core.tween(1100))
    }
    BoxWithConstraints(modifier = Modifier.fillMaxWidth(k).aspectRatio(940f / 204f)) {
        val u = maxWidth / 940f
        fun fs(art: Float) = (u.value * art).sp
        Image(painterResource(R.drawable.res_level), contentDescription = null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
        com.sualtikasifi.cizimhafiza.presentation.common.FitText(
            text = stringResource(R.string.result_level_label, progress.level),
            style = PaintedStyle(color = InkBrown, fontSize = fs(54f), textAlign = TextAlign.Start),
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier.offset(u * 120f, u * 34f).size(u * 360f, u * 64f)
        )
        Box(modifier = Modifier.offset(u * 480f, u * 38f).size(u * 352f, u * 56f), contentAlignment = Alignment.CenterEnd) {
            if (leveledUp) {
                Box(
                    modifier = Modifier
                        .background(Brush.verticalGradient(listOf(Color(0xFFFFA64D), ButtonOrange)), RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                ) { LetteredText(stringResource(R.string.result_level_up), fs(32f), outline = Color(0xFF8A3A00)) }
            } else if (!progress.isMaxLevel) {
                com.sualtikasifi.cizimhafiza.presentation.common.FitText(
                    text = stringResource(R.string.home_xp_to_next, progress.xpToNextLevel, progress.level + 1),
                    style = PaintedStyle(color = InkBrown, fontSize = fs(30f), fontWeight = FontWeight.Bold, textAlign = TextAlign.End),
                    contentAlignment = Alignment.CenterEnd,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        // The XP bar: a tan track, an orange fill and the little dot at its far end, as painted in the picture.
        Box(
            modifier = Modifier
                .offset(u * 118f, u * 102f)
                .size(u * 714f, u * 30f)
                .background(Color(0xFFE6D3B3), RoundedCornerShape(50))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction.value.coerceIn(0.04f, 1f))
                    .background(Brush.verticalGradient(listOf(Color(0xFFFFA23A), Color(0xFFF58A1F))), RoundedCornerShape(50))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = u * 12f)
                    .size(u * 12f)
                    .background(Color(0xFFF58A1F), CircleShape)
            )
        }
        Box(modifier = Modifier.offset(u * 120f, u * 140f).size(u * 420f, u * 40f), contentAlignment = Alignment.CenterStart) {
            Text(
                text = stringResource(R.string.home_xp_format, progress.xpIntoLevel, progress.xpForThisLevel),
                style = PaintedStyle(color = InkBrown, fontSize = fs(31f), fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Start),
                maxLines = 1
            )
        }
    }
}

/** A cream pill carrying a number, the hits / misses counters of the summary card. */
@Composable
private fun CountPill(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, value: Int, size: androidx.compose.ui.unit.TextUnit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size.value.dp * 0.95f))
        Text(value.toString(), style = PaintedStyle(color = tint, fontSize = size, textAlign = TextAlign.Center), maxLines = 1)
    }
}

/** A star of the level rating: gold with a darker edge when earned, a faint brown one when not. */
@Composable
private fun RatingStar(filled: Boolean, size: androidx.compose.ui.unit.Dp, description: String) {
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Icon(
            Icons.Filled.Star,
            contentDescription = description,
            tint = if (filled) Color(0xFFB86A00) else Color(0x66795548),
            modifier = Modifier.fillMaxSize()
        )
        Icon(
            Icons.Filled.Star,
            contentDescription = null,
            tint = if (filled) Color(0xFFFFC21A) else Color(0x44C9A98A),
            modifier = Modifier.fillMaxSize(0.8f)
        )
    }
}

/**
 * The round's summary on the painted "game over" card: the XP earned (big), the one-line sum that explains it, the stars
 * (or the fastest answer), hits / misses, and a chip for every bonus or gold source — each saying what it is.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ResultSummaryCard(state: GamePhase.Result, shownXp: Int, xpDoubled: Boolean, k: Float) {
    val base = state.totalScore
    val mult = state.xpMultiplier
    val explanation = when {
        xpDoubled -> stringResource(R.string.result_xp_doubled_note)
        state.daily != null -> stringResource(R.string.result_xp_daily)
        mult > 1 && state.xpEarned == base * mult -> stringResource(R.string.result_xp_formula_mult, base, mult)
        state.xpEarned == base -> stringResource(R.string.result_xp_formula_plain, base)
        else -> stringResource(R.string.result_xp_formula_extra, base)
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth(k).aspectRatio(940f / 505f)) {
            val u = maxWidth / 940f
            fun fs(art: Float) = (u.value * art).sp
            @Composable
            fun Slot(x0: Float, y0: Float, x1: Float, y1: Float, content: @Composable () -> Unit) {
                Box(
                    modifier = Modifier.offset(u * x0, u * y0).size(u * (x1 - x0), u * (y1 - y0)),
                    contentAlignment = Alignment.Center
                ) { content() }
            }
            Image(painterResource(R.drawable.res_header), contentDescription = null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
            Slot(350f, 88f, 690f, 160f) {
                com.sualtikasifi.cizimhafiza.presentation.common.FitText(
                    text = stringResource(R.string.game_over),
                    style = PaintedStyle(color = Color(0xFF2B1A10), fontSize = fs(56f), textAlign = TextAlign.Center),
                    modifier = Modifier.fillMaxSize()
                )
            }
            Slot(400f, 176f, 650f, 262f) {
                // Counts up to the earned XP when the screen opens (and again when it doubles after the ad).
                val xpCount by androidx.compose.animation.core.animateIntAsState(shownXp, androidx.compose.animation.core.tween(1100, easing = androidx.compose.animation.core.FastOutSlowInEasing), label = "xpCount")
                LetteredText(
                    text = stringResource(R.string.xp_gained_format, xpCount),
                    size = fs(80f),
                    fill = Color(0xFFF26A1B),
                    outline = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Slot(370f, 260f, 680f, 306f) {
                com.sualtikasifi.cizimhafiza.presentation.common.FitText(
                    text = explanation,
                    style = DescriptionStyle(fs(29f), fs(31f)),
                    maxLines = 2,
                    minScale = 0.62f,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Slot(380f, 304f, 660f, 378f) {
                val stars = state.levelStars
                if (stars != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(u * 14f), verticalAlignment = Alignment.CenterVertically) {
                        repeat(3) { index ->
                            RatingStar(index < stars, u * 66f, stringResource(R.string.stars_content_description, stars))
                        }
                    }
                } else {
                    state.fastestCorrectSeconds?.let {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.Bolt, contentDescription = null, tint = Color(0xFFE08A00), modifier = Modifier.size(fs(40f).value.dp))
                            com.sualtikasifi.cizimhafiza.presentation.common.FitText(
                                stringResource(R.string.fastest_correct, it),
                                style = PaintedStyle(color = InkBrown, fontSize = fs(34f), textAlign = TextAlign.Center),
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                    }
                }
            }
            Slot(352f, 388f, 510f, 462f) { CountPill(Icons.Filled.Check, Color(0xFF2EA043), state.correctCount, fs(46f)) }
            Slot(534f, 388f, 692f, 462f) { CountPill(Icons.Filled.Close, Color(0xFFE23B32), state.wrongCount, fs(46f)) }
        }
        val quick = state.quickMatchDailyBonusApplied
        val eventMult = if (state.xpEventMultiplierApplied) (mult / if (quick) 2 else 1).coerceAtLeast(2) else 0
        val hasChips = quick || eventMult > 0 || state.goldFromAchievements > 0 || state.goldFromLevel > 0 || state.goldFromDaily > 0
        if (hasChips) {
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                if (quick) ChipPill(text = stringResource(R.string.result_bonus_quick), color = Color(0xFFB5441A))
                if (eventMult > 0) ChipPill(text = stringResource(R.string.result_bonus_event, eventMult), color = Color(0xFFB5441A))
                val gold = Color(0xFF9A6200)
                if (state.goldFromAchievements > 0) ChipPill(text = stringResource(R.string.result_gold_achievement, state.goldFromAchievements), color = gold)
                if (state.goldFromLevel > 0) ChipPill(text = stringResource(R.string.result_gold_level, state.goldFromLevel), color = gold)
                if (state.goldFromDaily > 0) ChipPill(text = stringResource(R.string.result_gold_daily, state.goldFromDaily), color = gold)
            }
        }
    }
}

/** A small cream pill naming a bonus or a gold source. */
@Composable
private fun ChipPill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .shadow(2.dp, RoundedCornerShape(50))
            .background(Color(0xFFFFF3DA), RoundedCornerShape(50))
            .border(1.2.dp, Color(0xFFE0B878), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = PaintedStyle(color = color, fontSize = 12.sp, textAlign = TextAlign.Center), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** A parchment panel for the extra cards (quick match, duel, daily challenge). */
@Composable
private fun PaperPanel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    NinePatch(
        res = R.drawable.league_card,
        slicePx = 100,
        edge = 22.dp,
        modifier = modifier
    ) {
        Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) { content() }
    }
}

/** One of the screen's wide painted buttons; [content] is laid out in a row in its middle. */
@Composable
private fun PaintedPill(
    res: Int,
    height: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(if (pressed) 0.92f else 1f, animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.5f, stiffness = 650f), label = "pillPress")
    NinePatch(
        res = res,
        slicePx = 64,
        sliceYPx = 46,
        edge = 28.dp,
        edgeY = 21.dp,
        modifier = modifier
            .height(height)
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) 1f else 0.55f }
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            content = content
        )
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
                // The daily challenge has its own share button on its card; a second one here only doubled it.
                onShare = if (state.daily != null) null else {
                    {
                    DrawingShareUtil.shareAllResults(
                        context = context,
                        totalScore = state.totalScore,
                        correctCount = state.correctCount,
                        wrongCount = state.wrongCount,
                        fastestCorrectSeconds = state.fastestCorrectSeconds,
                        items = state.items
                    )
                    }
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
        LetteredText(
            text = title,
            size = 16.sp,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp)
        )
        if (onShare != null) {
            Icon(
                imageVector = Icons.Filled.Share,
                contentDescription = stringResource(R.string.share_all_drawings),
                tint = Color.White,
                modifier = Modifier.align(Alignment.CenterEnd).size(22.dp).clickable(onClick = onShare)
            )
        }
    }
    val total = if (items.isEmpty()) placeholders else items.size
    val slots = List(total) { index -> items.getOrNull(index) }
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(gap), horizontalAlignment = Alignment.CenterHorizontally) {
        slots.chunked(columns).forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally)) {
                rowItems.forEach { item ->
                    val note = RoundedCornerShape(6.dp)
                    Box(modifier = Modifier.size(cell)) {
                        if (item == null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(note)
                                    .background(Color(0x66FFF3DA))
                            )
                        } else {
                            // A sheet from a spiral notebook, with the drawing on it.
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .shadow(3.dp, note)
                                    .clip(note)
                                    .background(Brush.verticalGradient(listOf(Color(0xFFFFF9EA), Color(0xFFF6E6C6))))
                                    .border(1.dp, Color(0xFFD9BC8C), note)
                                    .clickable { onPreview(item) }
                            ) {
                                StrokeCanvas(strokes = item.strokes, modifier = Modifier.fillMaxSize().padding(start = 7.dp, top = 2.dp, end = 2.dp, bottom = 2.dp))
                                Column(
                                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 2.5.dp),
                                    verticalArrangement = Arrangement.spacedBy(cell * 0.09f)
                                ) {
                                    repeat(5) { Box(Modifier.size(2.8.dp).background(Color(0xFFB59A7A), CircleShape)) }
                                }
                            }
                            // The mark grows with the drawing it sits on: a green disc with a tick for a right
                            // answer, a red disc with a cross for a wrong one, white-ringed so it reads on any paper.
                            val badge = (cell.value * 0.22f).coerceIn(18f, 30f).dp
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(2.dp)
                                    .size(badge)
                                    .shadow(3.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(if (item.isCorrect) Color(0xFF34B24A) else Color(0xFFE53935))
                                    .border(2.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (item.isCorrect) Icons.Filled.Check else Icons.Filled.Close,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(badge * 0.66f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
