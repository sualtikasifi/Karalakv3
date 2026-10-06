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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Movie
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

    val explanation = when {
        xpDoubled -> stringResource(R.string.result_xp_doubled_note)
        state.daily != null -> stringResource(R.string.result_xp_daily)
        state.xpMultiplier > 1 && state.xpEarned == state.totalScore * state.xpMultiplier ->
            stringResource(R.string.result_xp_formula_mult, state.totalScore, state.xpMultiplier)
        state.xpEarned == state.totalScore -> stringResource(R.string.result_xp_formula_plain, state.totalScore)
        else -> stringResource(R.string.result_xp_formula_extra, state.totalScore)
    }
    // Counts up to the earned XP when the screen opens (and again when it doubles after the ad).
    val xpCount by androidx.compose.animation.core.animateIntAsState(
        shownXp,
        androidx.compose.animation.core.tween(1100, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "xpCount"
    )
    val stats = resultStats(state)

    // The design top to bottom: the painted head (title, XP), the stat cards, the level, the daily challenge, every
    // drawing of the round on its board — scrolling when a short phone cannot hold it all — and the two claim buttons
    // pinned underneath.
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = cachedPainterResource(R.drawable.bg_break),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(Modifier.fillMaxSize().background(Color(0x33180A02)))
        Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(Modifier.fillMaxWidth().statusBarsPadding().springIn(index = 0, fromY = -40)) {
                    ResultHeader(
                        title = stringResource(R.string.game_over),
                        xp = xpCount,
                        explanation = explanation,
                        onBack = { if (!(xpDoubled && !startedDoubled)) onMainMenu() }
                    )
                }
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ResultStatGrid(stats, Modifier.fillMaxWidth().springIn(index = 1, stepMs = 90))

                    if (levelProgress != null) {
                        Box(Modifier.fillMaxWidth().springIn(index = 2, stepMs = 90), contentAlignment = Alignment.Center) {
                            ResultLevelCard(progress = levelProgress, gainedXp = shownXp, k = 1f)
                        }
                    }

                    state.duelOpponentName?.let { opponentName ->
                        PaperPanel(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = stringResource(R.string.duel_challenge_sent, opponentName),
                                style = PaintedStyle(color = InkBrown, fontSize = 15.sp, textAlign = TextAlign.Center),
                                modifier = Modifier.fillMaxWidth().padding(6.dp)
                            )
                        }
                    }

                    state.ghost?.let { ghost ->
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
                        DuelChallengerVersusCard(
                            duel = duel,
                            playerScore = state.totalScore,
                            onRematch = onRematchDuel?.let { rematch ->
                                { rematch(duel.challengerUid, duel.challengerName) }
                            }
                        )
                    }

                    state.daily?.let { daily ->
                        ResultDailyCard(
                            daily = daily,
                            correctFlags = state.items.map { it.isCorrect },
                            onShare = {
                                DailyChallengeShareUtil.shareResult(
                                    context = context,
                                    correctFlags = state.items.map { it.isCorrect },
                                    streak = daily.streak
                                )
                            },
                            modifier = Modifier.fillMaxWidth().springIn(index = 3, stepMs = 90)
                        )
                    }

                    val ghost = state.ghost
                    val boardItems = if (showingOpponentGallery && ghost != null) ghostItems else state.items
                    ResultWordsBoard(
                        title = if (showingOpponentGallery && ghost != null) ghost.nickname
                        else stringResource(R.string.result_words_title, state.items.size),
                        items = boardItems,
                        placeholders = state.items.size,
                        wordLanguage = wordLanguage,
                        onPreview = { previewItem = it },
                        // The daily challenge has its own share button on its card, and the opponent's drawings are
                        // not the player's to pass on.
                        onShareAll = if (ghost == null && state.daily == null) {
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
                        } else null,
                        header = if (ghost != null) {
                            {
                                GalleryToggle(
                                    opponentName = ghost.nickname,
                                    opponentReady = ghostItems.isNotEmpty(),
                                    showingOpponent = showingOpponentGallery,
                                    onSelect = { showingOpponentGallery = it }
                                )
                            }
                        } else null,
                        modifier = Modifier.fillMaxWidth().springIn(index = 4, stepMs = 90)
                    )
                    Spacer(Modifier.height(2.dp))
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (onLevelNextAction != null && nextActionLabel != null) {
                    PaintedPill(
                        res = R.drawable.res_btn_next,
                        height = 50.dp,
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
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    if (canDouble) {
                        PaintedPill(
                            res = R.drawable.res_btn_claim,
                            height = 58.dp,
                            onClick = onDoubleXp!!,
                            modifier = Modifier.weight(1f)
                        ) {
                            ClaimContent(Icons.Filled.Movie, stringResource(R.string.result_x2_amount, state.xpEarned * 2), stringResource(R.string.result_watch_ad))
                        }
                    }
                    // Where the player's thumb lands, so the XP can fly from exactly there to the home bar.
                    var claimTapWindowPos by remember { mutableStateOf<androidx.compose.ui.geometry.Offset?>(null) }
                    var claimTopLeft by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
                    var claimCenter by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
                    PaintedPill(
                        res = R.drawable.res_btn_claim,
                        height = 58.dp,
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
                        // Nothing to collect: the button simply moves on instead of offering "0 XP".
                        if (shownXp > 0) {
                            ClaimContent(Icons.Filled.CardGiftcard, stringResource(R.string.result_claim_amount, shownXp))
                        } else {
                            LetteredText(stringResource(R.string.result_continue), 19.sp, outline = Color(0xFF8A3A00))
                        }
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
        Image(
            painterResource(R.drawable.lobby_crown),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.offset(u * 108f, u * 30f).size(u * 76f, u * 66f)
        )
        com.sualtikasifi.cizimhafiza.presentation.common.FitText(
            text = stringResource(R.string.result_level_label, progress.level),
            style = PaintedStyle(color = InkBrown, fontSize = fs(54f), textAlign = TextAlign.Start),
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier.offset(u * 196f, u * 34f).size(u * 290f, u * 64f)
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

/** The green disc with a tick (right answer) or red disc with a cross (wrong one), white-ringed so it reads on any paper. */
@Composable
internal fun ResultMark(correct: Boolean, size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(3.dp, CircleShape)
            .clip(CircleShape)
            .background(if (correct) Color(0xFF34B24A) else Color(0xFFE53935))
            .border((size.value * 0.07f).coerceAtLeast(1.5f).dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (correct) Icons.Filled.Check else Icons.Filled.Close,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size * 0.66f)
        )
    }
}
