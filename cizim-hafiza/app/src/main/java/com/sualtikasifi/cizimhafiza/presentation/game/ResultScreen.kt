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
    levelProgress: com.sualtikasifi.cizimhafiza.domain.model.LevelProgressState? = null
) {
    var previewItem by remember { mutableStateOf<ResultItem?>(null) }
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
    var galleryOpen by remember { mutableStateOf(false) }
    // A round that was already doubled when this screen (re)opened stays put;
    // only a double taken here sends the player home.
    val startedDoubled = remember { xpDoubled }
    LaunchedEffect(xpDoubled) {
        if (xpDoubled && !startedDoubled) {
            delay(1_600)
            onMainMenu()
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .screenBackground()
                .padding(padding)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- Score = XP hero -------------------------------------------
                RaisedCard(corner = 24.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.game_over),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.xp_gained_format, shownXp),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (xpDoubled) {
                                stringResource(R.string.result_xp_doubled_note)
                            } else {
                                stringResource(R.string.result_points_are_xp, state.totalScore)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        state.levelStars?.let { stars ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                repeat(3) { index ->
                                    Icon(
                                        imageVector = if (index < stars) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                        contentDescription = stringResource(R.string.stars_content_description, stars),
                                        tint = if (index < stars) AppTheme.tokens.gold else AppTheme.tokens.textFaint,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatPill(
                                text = "${state.correctCount}",
                                icon = Icons.Filled.Check,
                                contentColor = AppTheme.tokens.success
                            )
                            StatPill(
                                text = "${state.wrongCount}",
                                icon = Icons.Filled.Close,
                                contentColor = MaterialTheme.colorScheme.error
                            )
                            state.fastestCorrectSeconds?.let {
                                StatPill(
                                    text = stringResource(R.string.fastest_correct, it),
                                    icon = Icons.Filled.Bolt,
                                    contentColor = AppTheme.tokens.gold
                                )
                            }
                        }
                        if (state.goldEarned > 0 || state.quickMatchDailyBonusApplied || state.xpEventMultiplierApplied) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (state.goldEarned > 0) {
                                    TintedBadge(
                                        text = stringResource(R.string.result_gold_earned, state.goldEarned),
                                        container = AppTheme.tokens.gold.copy(alpha = 0.2f),
                                        content = AppTheme.tokens.gold
                                    )
                                }
                                if (state.quickMatchDailyBonusApplied || state.xpEventMultiplierApplied) {
                                    TintedBadge(text = stringResource(R.string.result_xp_bonus_applied))
                                }
                            }
                        }
                    }
                }

                if (levelProgress != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ResultLevelCard(progress = levelProgress, gainedXp = shownXp)
                }

                state.duelOpponentName?.let { opponentName ->
                    Spacer(modifier = Modifier.height(8.dp))
                    RaisedCard(corner = 20.dp, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.duel_challenge_sent, opponentName),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        )
                    }
                }

                state.ghost?.let { ghost ->
                    Spacer(modifier = Modifier.height(8.dp))
                    GhostVersusCard(ghost = ghost, playerScore = state.totalScore)
                }

                state.duelChallenger?.let { duel ->
                    Spacer(modifier = Modifier.height(8.dp))
                    DuelChallengerVersusCard(
                        duel = duel,
                        playerScore = state.totalScore,
                        onRematch = onRematchDuel?.let { rematch ->
                            { rematch(duel.challengerUid, duel.challengerName) }
                        }
                    )
                }

                state.daily?.let { daily ->
                    Spacer(modifier = Modifier.height(8.dp))
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

                Spacer(modifier = Modifier.height(8.dp))

                // The drawings live on their own page: ten of them inline
                // used to push everything else off the screen.
                RaisedCard(corner = 22.dp, modifier = Modifier.fillMaxWidth(), onClick = { galleryOpen = true }) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy((-10).dp)) {
                            state.items.take(4).forEach { item ->
                                StrokeCanvas(
                                    strokes = item.strokes,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(AppTheme.tokens.canvasPaper)
                                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.result_view_drawings),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = stringResource(R.string.result_view_drawings_sub, state.items.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Filled.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }

                if (onLevelNextAction != null && nextActionLabel != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SecondaryButton(
                        text = nextActionLabel,
                        onClick = onLevelNextAction,
                        height = 50.dp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Always ends on the home screen. XP is already banked: Claim just
            // leaves, x2 watches an ad, pays the round a second time, then leaves.
            val canDouble = onDoubleXp != null && state.xpEarned > 0 && GameConstants.ADMOB_ENABLED && !xpDoubled
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                if (canDouble) {
                    PrimaryButton(
                        text = stringResource(R.string.result_x2_button),
                        onClick = onDoubleXp!!,
                        face = Color(0xFFF59E0B),
                        height = 56.dp,
                        modifier = Modifier.weight(1f)
                    )
                }
                PrimaryButton(
                    text = stringResource(R.string.result_claim),
                    onClick = onMainMenu,
                    enabled = !(xpDoubled && !startedDoubled),
                    height = 56.dp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (galleryOpen) {
        ResultGalleryPage(
            state = state,
            ghostItems = ghostItems,
            showingOpponent = showingOpponentGallery,
            onSelectOpponent = { showingOpponentGallery = it },
            onPreview = { previewItem = it },
            onClose = { galleryOpen = false },
            wordLanguage = wordLanguage
        )
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
                            onClick = { DrawingShareUtil.shareDrawing(context, itemToPreview.word, itemToPreview.strokes) },
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
private fun GhostVersusCard(ghost: GhostMatchSummary, playerScore: Int) {
    val won = playerScore > ghost.opponentScore
    val drew = playerScore == ghost.opponentScore
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
                    name = ghost.nickname,
                    score = ghost.opponentScore,
                    highlighted = !won && !drew,
                    avatar = {
                        LevelAvatar(
                            level = ghost.level,
                            frame = AvatarFrame.resolve(ghost.frameId, ghost.level),
                            size = 32.dp
                        )
                    }
                )
            }
        }
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

/** Every drawing of the round on a page of its own, so none of them is squeezed or cut off. */
@Composable
private fun ResultGalleryPage(
    state: GamePhase.Result,
    ghostItems: List<ResultItem>,
    showingOpponent: Boolean,
    onSelectOpponent: (Boolean) -> Unit,
    onPreview: (ResultItem) -> Unit,
    onClose: () -> Unit,
    wordLanguage: String
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BackHandler(onBack = onClose)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .screenBackground()
                .systemBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (state.ghost != null) {
                    GalleryToggle(
                        opponentName = state.ghost.nickname,
                        opponentReady = ghostItems.isNotEmpty(),
                        showingOpponent = showingOpponent,
                        onSelect = onSelectOpponent,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Text(
                        text = stringResource(R.string.your_drawings),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (!showingOpponent) {
                    RaisedIconButton(
                        icon = Icons.Filled.Share,
                        contentDescription = stringResource(R.string.share_all_drawings),
                        onClick = {
                            DrawingShareUtil.shareAllResults(
                                context = context,
                                totalScore = state.totalScore,
                                correctCount = state.correctCount,
                                wrongCount = state.wrongCount,
                                fastestCorrectSeconds = state.fastestCorrectSeconds,
                                items = state.items
                            )
                        },
                        size = 42.dp
                    )
                }
                RaisedIconButton(
                    icon = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.close),
                    onClick = onClose,
                    size = 42.dp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(if (showingOpponent && ghostItems.isNotEmpty()) ghostItems else state.items) { item ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box {
                            StrokeCanvas(
                                strokes = item.strokes,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(AppTheme.tokens.canvasPaper)
                                    .border(
                                        1.5.dp,
                                        if (item.isCorrect) AppTheme.tokens.success.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline,
                                        MaterialTheme.shapes.medium
                                    )
                                    .clickable { onPreview(item) }
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(5.dp)
                                    .size(21.dp)
                                    .clip(CircleShape)
                                    .background(if (item.isCorrect) AppTheme.tokens.success else MaterialTheme.colorScheme.error),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (item.isCorrect) Icons.Filled.Check else Icons.Filled.Close,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Text(
                            text = item.word.capitalizeForWordLanguage(wordLanguage),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier.fillMaxWidth().padding(top = 3.dp)
                        )
                    }
                }
            }
        }
    }
}
