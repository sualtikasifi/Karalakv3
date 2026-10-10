package com.sualtikasifi.cizimhafiza.presentation.game

import com.sualtikasifi.cizimhafiza.presentation.common.cachedPainterResource
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.foundation.layout.BoxScope
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
import com.sualtikasifi.cizimhafiza.presentation.common.FitText
import com.sualtikasifi.cizimhafiza.presentation.common.InkBrown
import com.sualtikasifi.cizimhafiza.presentation.common.LetteredText
import com.sualtikasifi.cizimhafiza.presentation.common.pressable
import com.sualtikasifi.cizimhafiza.presentation.common.a11yButton
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.ui.platform.LocalDensity
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

    // The design top to bottom, all in its own 841-wide units so it holds together on any phone: the painted head (back
    // button, title, XP plate), the stat cards, the level, the match comparison, the daily challenge, every drawing of the
    // round on its board, and the two claim buttons pinned underneath. A tall phone shows it all at once; a short one
    // scrolls the middle.
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val u = maxWidth / ResArtW
        val fontScale0 = LocalDensity.current.fontScale
        fun fs(art: Float) = (art * u.value / fontScale0).sp
        // The empty workshop: the countdown's backdrop used to be here, and its big dog and dial peeked out between the
        // cards.
        Image(
            painter = cachedPainterResource(R.drawable.du_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxSize()
        )
        Box(Modifier.fillMaxSize().background(Color(0x33180A02)))
        Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.windowInsetsTopHeight(androidx.compose.foundation.layout.WindowInsets.statusBars))
                Box(Modifier.fillMaxWidth().springIn(index = 0, fromY = -40)) {
                    ResultHeader(
                        title = stringResource(R.string.game_over),
                        xp = xpCount,
                        explanation = explanation,
                        onBack = { if (!(xpDoubled && !startedDoubled)) onMainMenu() }
                    )
                }
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = u * 40f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(u * 10f)
                ) {
                    ResultStatGrid(stats, u, Modifier.fillMaxWidth().springIn(index = 1, stepMs = 90))

                    if (levelProgress != null) {
                        Box(Modifier.fillMaxWidth().springIn(index = 2, stepMs = 90), contentAlignment = Alignment.Center) {
                            ResultLevelCard(progress = levelProgress, gainedXp = shownXp)
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
                            myAvatarUrl = myAvatarUrl,
                            u = u
                        )
                    }

                    state.duelChallenger?.let { duel ->
                        DuelChallengerVersusCard(
                            duel = duel,
                            playerScore = state.totalScore,
                            u = u,
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
                                    yourName = myName.ifBlank { stringResource(R.string.quick_match_gallery_yours) },
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
                // Lifted clear of the bottom edge (and the gesture bar's swipe zone) rather than sitting right on it.
                modifier = Modifier.fillMaxWidth().padding(start = u * 40f, end = u * 40f, top = u * 8f, bottom = u * 36f),
                verticalArrangement = Arrangement.spacedBy(u * 8f)
            ) {
                if (onLevelNextAction != null && nextActionLabel != null) {
                    PaintedPill(
                        res = R.drawable.res_btn_next,
                        height = 52.dp,
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
                Row(horizontalArrangement = Arrangement.spacedBy(u * 14f), modifier = Modifier.fillMaxWidth()) {
                    run {
                        // Always there, as in the design; dimmed and inert when this round cannot be doubled (nothing
                        // earned, no ad available, or already doubled).
                        ArtButton(
                            res = R.drawable.rs_btn_ad,
                            u = u,
                            enabled = canDouble,
                            onClick = { onDoubleXp?.invoke() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                LetteredText(stringResource(R.string.result_x2_amount, state.xpEarned * 2), fs(40f), outline = Color(0xFF8A3A00), maxLines = 1, minScale = 0.6f, modifier = Modifier.offset(y = -(u * 9f)))
                                FitText(
                                    text = stringResource(R.string.result_watch_ad),
                                    style = PaintedStyle(color = Color.White, fontSize = fs(24f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                                    maxLines = 1,
                                    minScale = 0.6f,
                                    modifier = Modifier.align(Alignment.BottomCenter).offset(y = -(u * 8f)).fillMaxWidth().height(u * 30f)
                                )
                            }
                        }
                    }
                    // Where the player's thumb lands, so the XP can fly from exactly there to the home bar.
                    var claimTapWindowPos by remember { mutableStateOf<androidx.compose.ui.geometry.Offset?>(null) }
                    var claimTopLeft by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
                    var claimCenter by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
                    ArtButton(
                        res = R.drawable.rs_btn_claim,
                        u = u,
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
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            LetteredText(
                                stringResource(R.string.result_claim_amount, shownXp),
                                fs(38f), outline = Color(0xFF8A3A00), maxLines = 1, minScale = 0.5f
                            )
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
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            BackHandler { previewItem = null }
            DrawingPreviewScene(
                item = itemToPreview,
                wordLanguage = wordLanguage,
                reporting = showingOpponentGallery && onReportOpponentDrawing != null,
                onClose = { previewItem = null },
                // Share your own, report somebody else's. The header already withholds sharing for the opponent's gallery on
                // the grounds that their drawings are not the player's to pass on.
                onAction = {
                    if (showingOpponentGallery && onReportOpponentDrawing != null) reportItem = itemToPreview
                    else shareChoiceFor = itemToPreview
                }
            )
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
    myAvatarUrl: String,
    u: androidx.compose.ui.unit.Dp
) {
    VersusCard(
        leftName = myName.ifBlank { stringResource(R.string.quick_match_you) },
        leftScore = playerScore,
        leftAvatar = {
            LevelAvatar(
                level = myLevel,
                frame = myFrame,
                size = u * 84f,
                photo = com.sualtikasifi.cizimhafiza.presentation.common.avatarPhotoOf(myAvatarUrl)
            )
        },
        rightName = ghost.nickname,
        rightScore = ghost.opponentScore,
        rightAvatar = {
            LevelAvatar(
                level = ghost.level,
                frame = AvatarFrame.resolve(ghost.frameId, ghost.level),
                size = u * 84f,
                photo = com.sualtikasifi.cizimhafiza.presentation.common.AvatarPhoto.Persona(ghost.nickname)
            )
        },
        u = u
    )
}

/**
 * The comparison against a duel's challenger — same card as [GhostVersusCard], a friend instead of a recorded opponent.
 * Shown the instant this round finishes: the challenger's score has been sitting on the duel document since they sent it,
 * so there is nothing left to wait on.
 *
 * [onRematch] is only non-null when the caller actually wants the button offered — see ResultScreen's duelChallenger
 * branch, which only passes one when the screen itself was handed an onRematchDuel callback.
 */
@Composable
private fun DuelChallengerVersusCard(
    duel: DuelChallengerSummary,
    playerScore: Int,
    u: androidx.compose.ui.unit.Dp,
    onRematch: (() -> Unit)?
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(u * 8f)) {
        VersusCard(
            leftName = stringResource(R.string.quick_match_you),
            leftScore = playerScore,
            leftAvatar = null,
            rightName = duel.challengerName,
            rightScore = duel.challengerScore,
            rightAvatar = null,
            u = u
        )
        if (onRematch != null) {
            SecondaryButton(
                text = stringResource(R.string.duel_rematch_action),
                onClick = onRematch,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * The painted versus card: you on the left, them on the right, each with name and score (and a picture when there is one),
 * the outcome on a pill between them over "VS", and a crown on the card's top edge when the player won.
 */
@Composable
private fun VersusCard(
    leftName: String,
    leftScore: Int,
    leftAvatar: (@Composable () -> Unit)?,
    rightName: String,
    rightScore: Int,
    rightAvatar: (@Composable () -> Unit)?,
    u: androidx.compose.ui.unit.Dp
) {
    val won = leftScore > rightScore
    val drew = leftScore == rightScore
    val outcome = stringResource(
        when {
            drew -> R.string.quick_match_drew
            won -> R.string.quick_match_won
            else -> R.string.quick_match_lost
        }
    )
    val pill = when {
        drew -> Color(0xFF8A6A50)
        won -> Color(0xFF2EA043)
        else -> Color(0xFFD23B2E)
    }
    val winGreen = Color(0xFF1F9A3A)
    val loseRed = Color(0xFF8E1F1F)
    val fontScale0 = LocalDensity.current.fontScale
    fun fs(art: Float) = (art * u.value / fontScale0).sp
    val cardW = 761f
    Box(Modifier.fillMaxWidth().height(u * 128f)) {
        NinePatch(
            res = R.drawable.rs_vs,
            slicePx = 80,
            edge = u * 65f,
            sliceYPx = 64,
            edgeY = u * 52f,
            modifier = Modifier.fillMaxSize()
        )
        @Composable
        fun side(name: String, score: Int, avatar: (@Composable () -> Unit)?, right: Boolean, scoreColor: Color) {
            val avatarX = if (right) cardW - 44f - 84f else 44f
            val textX = if (avatar == null) (if (right) cardW - 50f - 190f else 50f) else if (right) cardW - 148f - 190f else 148f
            if (avatar != null) Box(Modifier.offset(u * avatarX, u * 22f).size(u * 84f)) { avatar() }
            FitText(
                text = name,
                style = PaintedStyle(color = ResultInk, fontSize = fs(30f), textAlign = if (right) TextAlign.End else TextAlign.Start),
                maxLines = 1,
                minScale = 0.55f,
                contentAlignment = if (right) Alignment.CenterEnd else Alignment.CenterStart,
                modifier = Modifier.offset(u * textX, u * 22f).size(u * 190f, u * 36f)
            )
            FitText(
                text = score.toString(),
                style = PaintedStyle(color = scoreColor, fontSize = fs(58f), textAlign = if (right) TextAlign.End else TextAlign.Start),
                maxLines = 1,
                minScale = 0.5f,
                contentAlignment = if (right) Alignment.CenterEnd else Alignment.CenterStart,
                modifier = Modifier.offset(u * textX, u * 44f).size(u * 190f, u * 60f)
            )
        }
        side(leftName, leftScore, leftAvatar, right = false, scoreColor = if (won) winGreen else if (drew) ResultInk else loseRed)
        side(rightName, rightScore, rightAvatar, right = true, scoreColor = if (!won && !drew) winGreen else if (drew) ResultInk else loseRed)
        Box(
            modifier = Modifier
                .offset(u * (cardW - 190f) / 2f, u * 22f)
                .size(u * 190f, u * 38f)
                .clip(RoundedCornerShape(50))
                .background(pill)
                .border(2.dp, Color(0x55FFFFFF), RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
        ) {
            LetteredText(outcome, fs(28f), outline = Color(0xFF1B3A10), maxLines = 1, minScale = 0.5f, modifier = Modifier.fillMaxWidth().padding(horizontal = u * 8f))
        }
        Text(
            text = stringResource(R.string.quick_match_versus),
            style = PaintedStyle(color = Color(0xFF8A6A50), fontSize = fs(32f), textAlign = TextAlign.Center),
            modifier = Modifier.offset(u * (cardW - 190f) / 2f, u * 60f).width(u * 190f)
        )
        if (won) {
            Image(
                painterResource(R.drawable.lobby_crown),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.offset(u * (cardW - 76f) / 2f, -(u * 32f)).size(u * 76f, u * 66f)
            )
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
    yourName: String,
    opponentName: String,
    opponentReady: Boolean,
    showingOpponent: Boolean,
    onSelect: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Two equal pills filling the plank, so a long name shrinks inside its own pill instead of pushing out of the frame.
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // The longer name gets the wider pill, so neither is cut when the other is short.
        GalleryChip(
            label = yourName,
            selected = !showingOpponent,
            enabled = true,
            onClick = { onSelect(false) },
            modifier = Modifier.weight(yourName.length.coerceAtLeast(5).toFloat())
        )
        GalleryChip(
            label = opponentName,
            selected = showingOpponent,
            enabled = opponentReady,
            onClick = { onSelect(true) },
            modifier = Modifier.weight(opponentName.length.coerceAtLeast(5).toFloat())
        )
    }
}

/** One side of the gallery switch: orange and lettered when chosen, cream with brown ink when not. */
@Composable
private fun GalleryChip(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(50)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(30.dp)
            .clip(shape)
            .background(
                if (selected) Brush.verticalGradient(listOf(Color(0xFFFFB14E), Color(0xFFF47A16)))
                else Brush.verticalGradient(listOf(Color(0xFFFFF6E2), Color(0xFFF3DFBA)))
            )
            .border(1.5.dp, if (selected) Color(0xFFA9440A) else Color(0xFF8A5A2E), shape)
            .graphicsLayer { alpha = if (enabled || selected) 1f else 0.6f }
            .clickable(enabled = enabled && !selected, onClick = onClick)
            .padding(horizontal = 6.dp)
    ) {
        if (selected) {
            LetteredText(label, 15.sp, outline = Color(0xFF8A3A00), minScale = 0.4f, modifier = Modifier.fillMaxWidth())
        } else {
            com.sualtikasifi.cizimhafiza.presentation.common.FitText(
                text = label,
                style = PaintedStyle(color = InkBrown, fontSize = 15.sp, textAlign = TextAlign.Center),
                maxLines = 1,
                minScale = 0.4f,
                modifier = Modifier.fillMaxWidth()
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
    // rs_level_blank (698 x 140 px) is the card with its crown; the lettering and the live bar go where the design has them.
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().aspectRatio(698f / 140f)) {
        val s = maxWidth / 698f
        val fontScale0 = LocalDensity.current.fontScale
        fun fs(px: Float) = (px * s.value / fontScale0).sp
        Image(
            painter = cachedPainterResource(R.drawable.rs_level_blank),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )
        // The crown is drawn here (not painted into the card) so it can sit level with the "Seviye" line.
        Image(
            painterResource(R.drawable.lobby_crown),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.offset(s * 88f, s * 21f).size(s * 46f, s * 40f)
        )
        FitText(
            text = stringResource(R.string.result_level_label, progress.level),
            style = PaintedStyle(color = InkBrown, fontSize = fs(38f), textAlign = TextAlign.Start),
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier.offset(s * 146f, s * 17f).size(s * 240f, s * 48f)
        )
        Box(modifier = Modifier.offset(s * 380f, s * 20f).size(s * 262f, s * 42f), contentAlignment = Alignment.CenterEnd) {
            if (leveledUp) {
                Box(
                    modifier = Modifier
                        .background(Brush.verticalGradient(listOf(Color(0xFFFFA64D), ButtonOrange)), RoundedCornerShape(50))
                        .padding(horizontal = s * 12f, vertical = s * 1f)
                ) { LetteredText(stringResource(R.string.result_level_up), fs(26f), outline = Color(0xFF8A3A00)) }
            } else if (!progress.isMaxLevel) {
                FitText(
                    text = stringResource(R.string.home_xp_to_next, progress.xpToNextLevel, progress.level + 1),
                    style = PaintedStyle(color = InkBrown, fontSize = fs(25f), fontWeight = FontWeight.Bold, textAlign = TextAlign.End),
                    contentAlignment = Alignment.CenterEnd,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        // The XP bar: a tan track, an orange fill and the little dot at its far end.
        Box(
            modifier = Modifier
                .offset(s * 84f, s * 71f)
                .size(s * 534f, s * 27f)
                .background(Color(0xFFE6D3B3), RoundedCornerShape(50))
                .border(s * 1.5f, Color(0xFFC9AE86), RoundedCornerShape(50))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction.value.coerceIn(0.04f, 1f))
                    .background(Brush.verticalGradient(listOf(Color(0xFFFFA23A), Color(0xFFF2701A))), RoundedCornerShape(50))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = s * 9f)
                    .size(s * 9f)
                    .background(Color(0xFFF58A1F), CircleShape)
            )
        }
        Box(modifier = Modifier.offset(s * 86f, s * 99f).size(s * 300f, s * 26f), contentAlignment = Alignment.CenterStart) {
            Text(
                text = stringResource(R.string.home_xp_format, progress.xpIntoLevel, progress.xpForThisLevel),
                style = PaintedStyle(color = InkBrown, fontSize = fs(22f), fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Start),
                maxLines = 1
            )
        }
    }
}

/** One of the screen's wide painted buttons: the picture's icon and leaves kept at its own size, [content] in the middle. */
@Composable
private fun ArtButton(
    res: Int,
    u: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(if (pressed) 0.92f else 1f, animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.5f, stiffness = 650f), label = "artPress")
    // The pictures are 104 px tall and drawn 100 units tall; the left 150 px carry the leaf and the painted icon.
    NinePatch(
        res = res,
        slicePx = 150,
        edge = u * 144f,
        sliceYPx = 40,
        edgeY = u * 38f,
        clampEdgeToHeight = false,
        modifier = modifier
            .height(u * 100f)
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) 1f else 0.55f }
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
    ) {
        Box(Modifier.fillMaxSize().padding(start = u * 140f, end = u * 38f), content = content)
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
    val base = modifier
        .height(height)
        .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) 1f else 0.55f }
        .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
    if (res == R.drawable.res_btn_claim || res == R.drawable.res_btn_next) {
        // Both buttons are drawn, not stretched from their pictures: the pictures' square wooden corners (and a
        // stray dark mark on the orange one's lower edge) showed once stretched to a full-width button.
        val shape = RoundedCornerShape(50)
        val cream = res == R.drawable.res_btn_next
        Box(
            modifier = base
                .shadow(5.dp, shape)
                .background(
                    Brush.verticalGradient(
                        if (cream) listOf(Color(0xFFFFF7E6), Color(0xFFFCE3B4), Color(0xFFF5CD8A))
                        else listOf(Color(0xFFFFB14E), Color(0xFFF47A16), Color(0xFFDD5F0B))
                    ),
                    shape
                )
                .border(2.5.dp, if (cream) Color(0xFFD08A2E) else Color(0xFFA9440A), shape)
        ) {
            // The glossy band across the top of the pill.
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.42f)
                    .padding(horizontal = 14.dp, vertical = 4.dp)
                    .background(Brush.verticalGradient(listOf(Color(0x66FFFFFF), Color(0x00FFFFFF))), shape)
            )
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                content = content
            )
        }
        return
    }
    NinePatch(
        res = res,
        slicePx = 64,
        sliceYPx = 46,
        edge = 28.dp,
        edgeY = 21.dp,
        modifier = base
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


// rs_dialog_bg is the design's whole picture: the workshop, the board with the dog and the cat, its paper, the close and
// replay buttons and the empty orange pill. Everything live is laid over it by the picture's own coordinates (841 x 1870).
private const val PreviewW = 841f
private const val PreviewH = 1870f

/**
 * The window a tapped drawing opens in: the drawing replays on the board's paper, its word is lettered on the board's
 * plank, the painted replay button plays it again, the painted cross closes it and the orange pill underneath is the
 * share (or, for the opponent's drawings, report) button.
 */
@Composable
private fun DrawingPreviewScene(
    item: ResultItem,
    wordLanguage: String,
    reporting: Boolean,
    onClose: () -> Unit,
    onAction: () -> Unit
) {
    var replay by remember { mutableStateOf(0) }
    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color(0xFF2A1708))) {
        val unit = maxOf(maxWidth / PreviewW, maxHeight / PreviewH)
        val offX = (maxWidth - unit * PreviewW) / 2
        val offY = (maxHeight - unit * PreviewH) / 2
        val fontScale0 = LocalDensity.current.fontScale
        fun fs(art: Float) = (art * unit.value / fontScale0).sp
        fun box(x0: Float, y0: Float, x1: Float, y1: Float): Modifier =
            Modifier.offset(offX + unit * x0, offY + unit * y0)
                // Unbounded: the picture can be wider or taller than the screen, and a plain size() would be clamped to it.
                .wrapContentSize(Alignment.TopStart, unbounded = true)
                .size(unit * (x1 - x0), unit * (y1 - y0))

        Image(
            painter = cachedPainterResource(R.drawable.rs_dialog_bg),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = box(0f, 0f, PreviewW, PreviewH)
        )
        // The word, on the board's plank.
        LetteredText(
            text = item.word.capitalizeForWordLanguage(wordLanguage),
            size = fs(54f),
            outline = Color(0xFF4A2410),
            modifier = box(215f, 478f, 690f, 548f),
            minScale = 0.45f,
            title = true
        )
        // The drawing, on a white sheet in the middle of the paper (the replay button sits on its lower right corner's side).
        ReplayableDrawing(
            strokes = item.strokes,
            showReplayButton = false,
            externalReplay = replay,
            modifier = box(188f, 590f, 648f, 1050f)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(unit * 26f))
                .background(AppTheme.tokens.canvasPaper)
        )
        // The painted buttons, made live.
        Box(
            box(704f, 188f, 828f, 316f)
                .pressable(pressedScale = 0.9f, onClick = onClose)
                .a11yButton(stringResource(R.string.close))
        )
        Box(
            box(650f, 1000f, 800f, 1140f)
                .pressable(pressedScale = 0.9f, onClick = { replay++ })
                .a11yButton(stringResource(R.string.replay_drawing))
        )
        Box(
            box(150f, 1205f, 692f, 1345f)
                .pressable(pressedScale = 0.95f, onClick = onAction)
                .a11yButton(stringResource(if (reporting) R.string.report_drawing_action else R.string.share_drawing)),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Icon(
                    if (reporting) Icons.Filled.Flag else Icons.Filled.Share,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(unit * 56f)
                )
                Spacer(Modifier.width(unit * 14f))
                LetteredText(
                    stringResource(if (reporting) R.string.report_drawing_action else R.string.share_drawing),
                    fs(54f),
                    outline = Color(0xFF8A3A00),
                    minScale = 0.5f,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }
    }
}
