package com.sualtikasifi.cizimhafiza.presentation.online

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.data.bot.BotRoomEngine
import com.sualtikasifi.cizimhafiza.domain.model.AvatarFrame
import com.sualtikasifi.cizimhafiza.domain.model.OnlinePlayer
import com.sualtikasifi.cizimhafiza.domain.model.ResultItem
import com.sualtikasifi.cizimhafiza.presentation.common.BotMascot
import com.sualtikasifi.cizimhafiza.presentation.common.BotMascotPose
import com.sualtikasifi.cizimhafiza.presentation.common.ChestWonDialog
import com.sualtikasifi.cizimhafiza.presentation.common.FitText
import com.sualtikasifi.cizimhafiza.presentation.common.InkBrown
import com.sualtikasifi.cizimhafiza.presentation.common.LetteredText
import com.sualtikasifi.cizimhafiza.presentation.common.LevelAvatar
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedStyle
import com.sualtikasifi.cizimhafiza.presentation.common.RatingPromptDialog
import com.sualtikasifi.cizimhafiza.presentation.common.ReportDrawingDialog
import com.sualtikasifi.cizimhafiza.presentation.common.ReportSendState
import com.sualtikasifi.cizimhafiza.presentation.common.SignInPromptDialog
import com.sualtikasifi.cizimhafiza.presentation.common.avatarPhotoOf
import com.sualtikasifi.cizimhafiza.presentation.common.cachedPainterResource
import com.sualtikasifi.cizimhafiza.presentation.common.currentWordLanguage
import com.sualtikasifi.cizimhafiza.presentation.common.springIn
import com.sualtikasifi.cizimhafiza.presentation.game.ArtButton
import com.sualtikasifi.cizimhafiza.presentation.game.DrawingPreviewScene
import com.sualtikasifi.cizimhafiza.presentation.game.GalleryChip
import com.sualtikasifi.cizimhafiza.presentation.game.PaperPanel
import com.sualtikasifi.cizimhafiza.presentation.game.ResArtW
import com.sualtikasifi.cizimhafiza.presentation.game.ResultHeader
import com.sualtikasifi.cizimhafiza.presentation.game.ResultInk
import com.sualtikasifi.cizimhafiza.presentation.game.MiniStat
import com.sualtikasifi.cizimhafiza.presentation.game.ResultPairRow
import com.sualtikasifi.cizimhafiza.presentation.game.StatIcon
import com.sualtikasifi.cizimhafiza.presentation.game.ResultStat
import com.sualtikasifi.cizimhafiza.presentation.game.ResultStatGrid
import com.sualtikasifi.cizimhafiza.presentation.game.ResultWordsBoard
import com.sualtikasifi.cizimhafiza.presentation.game.VersusCard
import com.sualtikasifi.cizimhafiza.presentation.game.XpOrange
import com.sualtikasifi.cizimhafiza.util.DrawingShareUtil
import com.sualtikasifi.cizimhafiza.util.placementEmoji

@Composable
fun OnlineResultScreen(
    onRematchStarted: (roomCode: String) -> Unit,
    onReturnToWaitingRoom: (roomCode: String) -> Unit,
    onMainMenu: () -> Unit,
    viewModel: OnlineResultViewModel = hiltViewModel()
) {
    val wordLanguage = currentWordLanguage()
    val uiState by viewModel.uiState.collectAsState()
    val room = uiState.room
    val myUid = viewModel.myUid
    val me = room?.players?.find { it.uid == myUid }
    // pendingNextRound players (joined mid-round) never played this round, and left=true players will never finish it:
    // both are left out of every comparison below, the same way OnlineResultViewModel leaves them out of "is everyone
    // done" — otherwise one player quitting would keep everybody else waiting for ever.
    val others = room?.players?.filter { it.uid != myUid && !it.pendingNextRound && !it.left } ?: emptyList()
    val context = LocalContext.current
    var ratingPromptDismissed by remember { mutableStateOf(false) }
    var signInPromptDismissed by remember { mutableStateOf(false) }
    var chestWonDismissed by remember { mutableStateOf(false) }

    // The interstitial is asked for once, the moment the finished comparison is actually showing.
    val resultIsShowing = others.isNotEmpty() && others.all { it.finished }
    LaunchedEffect(resultIsShowing) {
        if (resultIsShowing) {
            (context as? Activity)?.let { viewModel.showInterstitial(it) }
        }
    }
    LaunchedEffect(uiState.navigateToRematchRoomCode) {
        uiState.navigateToRematchRoomCode?.let(onRematchStarted)
    }
    LaunchedEffect(uiState.navigateToWaitingRoomCode) {
        uiState.navigateToWaitingRoomCode?.let(onReturnToWaitingRoom)
    }

    if (room == null || me == null) {
        Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        return
    }

    // Not everyone has submitted yet: an unfinished player's score is still the pre-game 0, so showing the comparison now
    // would make them look like they had already lost. Until then, a waiting scene of its own — no chat dock (a message
    // sent into a round that is over has nowhere to land).
    if (others.isEmpty() || others.any { !it.finished }) {
        OnlineWaitingScene(me = me, others = others, myUid = myUid, onBack = onMainMenu)
        return
    }

    var previewItem by remember { mutableStateOf<ResultItem?>(null) }
    var reportItem by remember { mutableStateOf<ResultItem?>(null) }

    val roundPlayers = listOf(me) + others
    val ranked = roundPlayers.sortedByDescending { it.totalScore }
    val myPlacement = ranked.indexOfFirst { it.uid == myUid } + 1
    // Shown in the standings, tagged, but kept out of ranked/roundPlayers: those drive the placement and the team totals.
    val leftPlayers = room.players.filter { it.uid != myUid && it.left && !it.pendingNextRound }
    val teamMode = room.teamMode
    val teamATotal = roundPlayers.filter { it.teamId == "A" }.sumOf { it.totalScore }
    val teamBTotal = roundPlayers.filter { it.teamId == "B" }.sumOf { it.totalScore }
    val selectedUid = uiState.selectedUid ?: myUid
    val selectedPlayer = roundPlayers.find { it.uid == selectedUid } ?: me
    val galleryItems = uiState.itemsByUid[selectedUid].orEmpty()
    val isMine = selectedUid == myUid
    val reportable = !isMine && selectedUid != BotRoomEngine.BOT_UID

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val u = maxWidth / ResArtW
        val fontScale0 = LocalDensity.current.fontScale
        fun fs(art: Float) = (art * u.value / fontScale0).sp
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
                Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                Box(Modifier.fillMaxWidth().springIn(index = 0, fromY = -40)) {
                    ResultHeader(title = stringResource(R.string.game_over), onBack = onMainMenu)
                }
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = u * 40f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(u * 10f)
                ) {
                    // ---- How the round went: the versus card for two (or the two teams), a standings card for more ----
                    Box(Modifier.fillMaxWidth().springIn(index = 1, stepMs = 90)) {
                        when {
                            teamMode -> {
                                val mineIsB = me.teamId == "B"
                                val myTeam = if (mineIsB) teamBTotal else teamATotal
                                val theirTeam = if (mineIsB) teamATotal else teamBTotal
                                VersusCard(
                                    leftName = stringResource(if (mineIsB) R.string.online_team_b else R.string.online_team_a),
                                    leftScore = myTeam,
                                    leftAvatar = null,
                                    rightName = stringResource(if (mineIsB) R.string.online_team_a else R.string.online_team_b),
                                    rightScore = theirTeam,
                                    rightAvatar = null,
                                    u = u
                                )
                            }
                            roundPlayers.size == 2 -> {
                                val other = others.first()
                                VersusCard(
                                    leftName = me.displayName,
                                    leftScore = me.totalScore,
                                    leftAvatar = { LevelAvatar(me.level, AvatarFrame.resolve(me.frameId, me.level), size = u * 84f, photo = avatarPhotoOf(me.avatarUrl)) },
                                    rightName = other.displayName,
                                    rightScore = other.totalScore,
                                    rightAvatar = { PlayerPicture(other, u * 84f) },
                                    u = u
                                )
                            }
                            else -> StandingsCard(
                                ranked = ranked,
                                left = leftPlayers,
                                myUid = myUid,
                                rematchVotes = room.rematchVotes,
                                placement = myPlacement,
                                u = u
                            )
                        }
                    }

                    // ---- Your numbers, and what the round paid ----
                    ResultPairRow(
                        left = listOf(
                            MiniStat(StatIcon.Art(R.drawable.rs_ic_check), me.correctCount.toString(), Color(0xFF2EA043)),
                            MiniStat(StatIcon.Art(R.drawable.rs_ic_x), me.wrongCount.toString(), Color(0xFFE23B32))
                        ),
                        right = buildList {
                            if (uiState.goldEarned > 0) add(MiniStat(StatIcon.Art(R.drawable.icon_gold_coin), "+" + uiState.goldEarned, Color(0xFFE8650F)))
                            if (uiState.xpEarned > 0) add(MiniStat(StatIcon.Art(R.drawable.ls_star), com.sualtikasifi.cizimhafiza.domain.model.RuneLive.xp(uiState.xpEarned).toString() + " XP", XpOrange))
                        },
                        u = u,
                        modifier = Modifier.springIn(index = 2, stepMs = 90)
                    )
                    // The runes' share of that gold, so the rune map is felt where the gold lands.
                    if (uiState.runeBonusGold > 0) {
                        Text(
                            text = stringResource(R.string.result_rune_bonus_line, uiState.runeBonusGold),
                            color = Color(0xFF8A5A2B),
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // ---- Everyone's drawings, one player at a time ----
                    ResultWordsBoard(
                        title = if (isMine) stringResource(R.string.result_words_title, me.correctCount + me.wrongCount) else selectedPlayer.displayName,
                        items = galleryItems,
                        placeholders = (me.correctCount + me.wrongCount).coerceAtLeast(room.wordCount),
                        wordLanguage = wordLanguage,
                        onPreview = { previewItem = it },
                        onShareAll = if (isMine && galleryItems.isNotEmpty()) {
                            {
                                DrawingShareUtil.shareAllResults(
                                    context = context,
                                    totalScore = me.totalScore,
                                    correctCount = me.correctCount,
                                    wrongCount = me.wrongCount,
                                    fastestCorrectSeconds = me.fastestCorrectMs?.let { it / 1000.0 },
                                    items = galleryItems
                                )
                            }
                        } else null,
                        header = {
                            // One pill per player, side by side for two; a room holds up to 8, so more than two slide sideways,
                            // with a soft fade at whichever end still has players hidden behind it.
                            val chipScroll = rememberScrollState()
                            LaunchedEffect(selectedUid, roundPlayers.size) {
                                val index = roundPlayers.indexOfFirst { it.uid == selectedUid }
                                if (roundPlayers.size > 2 && index >= 0) chipScroll.animateScrollTo(index * 116 * 3)
                            }
                            Box(Modifier.fillMaxWidth()) {
                            Row(
                                // Two pills share the plank; more than two scroll (weights need a bounded row).
                                modifier = Modifier.fillMaxWidth().then(if (roundPlayers.size > 2) Modifier.horizontalScroll(chipScroll) else Modifier).padding(horizontal = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                roundPlayers.forEach { player ->
                                    val ready = uiState.itemsByUid[player.uid].orEmpty().isNotEmpty()
                                    GalleryChip(
                                        label = player.displayName,
                                        selected = player.uid == selectedUid,
                                        enabled = ready || player.uid == myUid,
                                        onClick = { viewModel.selectPlayer(player.uid) },
                                        modifier = if (roundPlayers.size <= 2) Modifier.weight(1f) else Modifier.width(110.dp)
                                    )
                                }
                            }
                            if (roundPlayers.size > 2 && chipScroll.canScrollBackward) {
                                Box(Modifier.align(Alignment.CenterStart).width(18.dp).height(30.dp).background(Brush.horizontalGradient(listOf(Color(0xCC8A4A12), Color.Transparent))))
                            }
                            if (roundPlayers.size > 2 && chipScroll.canScrollForward) {
                                Box(Modifier.align(Alignment.CenterEnd).width(18.dp).height(30.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, Color(0xCC8A4A12)))))
                            }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().springIn(index = 3, stepMs = 90)
                    )

                    // ---- Chat and the rematch vote ----
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            ReactionSendRow(onSend = viewModel::sendReaction, reactions = uiState.reactions, myUid = myUid, players = room.players)
                            if (uiState.rematchBlockedByNewJoiner) {
                                Text(
                                    text = stringResource(R.string.online_rematch_blocked_message),
                                    style = PaintedStyle(color = InkBrown, fontSize = 14.sp, textAlign = TextAlign.Center),
                                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                                )
                            } else if (room.rematchVotes.isNotEmpty()) {
                                Text(
                                    text = stringResource(
                                        R.string.online_rematch_ready_count,
                                        room.rematchVotes.count { uid -> roundPlayers.any { it.uid == uid } },
                                        roundPlayers.size
                                    ),
                                    style = PaintedStyle(color = Color(0xFF1F9A3A), fontSize = 14.sp, textAlign = TextAlign.Center),
                                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                                )
                            }
                    }
                    Spacer(Modifier.height(2.dp))
                }
            }

            // ---- The two buttons, pinned under the scrolling part ----
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = u * 40f, end = u * 40f, top = u * 8f, bottom = u * 36f),
                horizontalArrangement = Arrangement.spacedBy(u * 14f)
            ) {
                ArtButton(res = R.drawable.rs_btn_claim, u = u, iconRoom = 0f, onClick = onMainMenu, modifier = Modifier.weight(1f)) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        LetteredText(stringResource(R.string.main_menu), fs(38f), outline = Color(0xFF8A3A00), maxLines = 1, minScale = 0.5f)
                    }
                }
                val waitingForOthers = uiState.rematchRequested
                ArtButton(
                    res = R.drawable.rs_btn_claim,
                    u = u,
                    iconRoom = 0f,
                    enabled = !waitingForOthers && !uiState.rematchBlockedByNewJoiner,
                    onClick = viewModel::requestRematch,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        LetteredText(
                            stringResource(if (waitingForOthers) R.string.online_rematch_waiting else R.string.play_again),
                            fs(38f), outline = Color(0xFF8A3A00), maxLines = 1, minScale = 0.5f
                        )
                    }
                }
            }
        }
    }

    // ---- The drawing window: the same one the solo result opens, with share for yours and report for theirs ----
    previewItem?.let { item ->
        val position = galleryItems.indexOf(item)
        Dialog(
            onDismissRequest = { previewItem = null },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            BackHandler { previewItem = null }
            DrawingPreviewScene(
                item = item,
                position = if (position >= 0) position + 1 else null,
                total = galleryItems.size,
                onPrevious = if (position > 0) ({ previewItem = galleryItems[position - 1] }) else null,
                onNext = if (position in 0 until galleryItems.size - 1) ({ previewItem = galleryItems[position + 1] }) else null,
                wordLanguage = wordLanguage,
                reporting = reportable,
                onClose = { previewItem = null },
                onAction = {
                    if (reportable) reportItem = item
                    else DrawingShareUtil.shareDrawingOnTemplate(context, item.word, item.strokes)
                }
            )
        }
    }

    reportItem?.let { itemToReport ->
        ReportDrawingDialog(
            word = itemToReport.word,
            sendState = uiState.reportState,
            onReport = { reason -> viewModel.reportSelectedPlayersDrawing(itemToReport, reason) },
            onDismiss = {
                reportItem = null
                viewModel.dismissReport()
                if (uiState.reportState == ReportSendState.Sent) previewItem = null
            }
        )
    }

    // Local, not derived from uiState: the flag only decides eligibility once, dismissing is what takes it off screen.
    if (uiState.showRatingPrompt && !ratingPromptDismissed) {
        RatingPromptDialog(
            onRate = {
                viewModel.grantRatingBonusXp()
                ratingPromptDismissed = true
            },
            onDismiss = { ratingPromptDismissed = true }
        )
    }
    if (uiState.showSignInPrompt && !signInPromptDismissed) {
        SignInPromptDialog(onDismiss = { signInPromptDismissed = true })
    }
    if (uiState.chestLost) {
        var chestLostDismissed by remember { mutableStateOf(false) }
        if (!chestLostDismissed) com.sualtikasifi.cizimhafiza.presentation.common.ChestLostDialog(onDismiss = { chestLostDismissed = true })
    }
    uiState.chestWon?.let { chest ->
        if (!chestWonDismissed) {
            ChestWonDialog(chest = chest, onDismiss = { chestWonDismissed = true })
        }
    }
}

/** A player's picture: the bot's mascot, or their frame with their photo or level. */
@Composable
internal fun PlayerPicture(player: OnlinePlayer, size: androidx.compose.ui.unit.Dp) {
    if (player.uid == BotRoomEngine.BOT_UID) {
        BotMascot(pose = BotMascotPose.HAPPY, modifier = Modifier.size(size))
    } else {
        LevelAvatar(player.level, AvatarFrame.resolve(player.frameId, player.level), size = size, photo = avatarPhotoOf(player.avatarUrl))
    }
}

/**
 * The scene a player who is already done waits in, until the others have finished: the studio from the lobby, one paper
 * card with their own score and a line for every player — a tick when they are done, an hourglass while they are not.
 */
@Composable
private fun OnlineWaitingScene(me: OnlinePlayer, others: List<OnlinePlayer>, myUid: String?, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val everyone = listOf(me) + others
    val doneCount = everyone.count { it.finished }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val u = maxWidth / ResArtW
        val fontScale0 = LocalDensity.current.fontScale
        fun fs(art: Float) = (art * u.value / fontScale0).sp
        Image(
            painter = cachedPainterResource(R.drawable.bg_lobby),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxSize()
        )
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x66180A02), Color(0x22180A02), Color(0x88180A02)))))
        Column(
            modifier = Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = u * 56f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LetteredText(
                text = stringResource(R.string.online_finished_title),
                size = fs(78f),
                outline = Color(0xFF4A2410),
                maxLines = 1,
                minScale = 0.5f,
                title = true,
                modifier = Modifier.fillMaxWidth().springIn(index = 0, fromY = -40)
            )
            Spacer(Modifier.height(u * 18f))
            PaperPanel(modifier = Modifier.fillMaxWidth().springIn(index = 1, stepMs = 90)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = me.totalScore.toString(),
                        style = PaintedStyle(color = XpOrange, fontSize = fs(110f), textAlign = TextAlign.Center),
                        maxLines = 1
                    )
                    Text(
                        text = stringResource(R.string.online_score_label),
                        style = PaintedStyle(color = InkBrown, fontSize = fs(34f), textAlign = TextAlign.Center)
                    )
                    Spacer(Modifier.height(u * 14f))
                    everyone.forEach { player ->
                        WaitingRow(player = player, isYou = player.uid == myUid, u = u)
                    }
                    Spacer(Modifier.height(u * 12f))
                    val pulse = androidx.compose.animation.core.rememberInfiniteTransition(label = "waitPulse")
                    val alpha by pulse.animateFloat(
                        initialValue = 0.45f,
                        targetValue = 1f,
                        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                            androidx.compose.animation.core.tween(900),
                            androidx.compose.animation.core.RepeatMode.Reverse
                        ),
                        label = "waitPulseAlpha"
                    )
                    Text(
                        text = stringResource(R.string.online_waiting_for_opponent_result),
                        style = PaintedStyle(color = ResultInk, fontSize = fs(30f), textAlign = TextAlign.Center),
                        modifier = Modifier.fillMaxWidth().graphicsLayer { this.alpha = alpha }
                    )
                    Text(
                        text = stringResource(R.string.online_players_done, doneCount, everyone.size),
                        style = PaintedStyle(color = Color(0xFF8A6A50), fontSize = fs(26f), textAlign = TextAlign.Center),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WaitingRow(player: OnlinePlayer, isYou: Boolean, u: androidx.compose.ui.unit.Dp) {
    val fontScale0 = LocalDensity.current.fontScale
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = u * 4f)
    ) {
        PlayerPicture(player, u * 64f)
        Spacer(Modifier.width(u * 14f))
        FitText(
            text = if (isYou) stringResource(R.string.online_you_label, player.displayName) else player.displayName,
            style = PaintedStyle(color = ResultInk, fontSize = (30f * u.value / fontScale0).sp, textAlign = TextAlign.Start),
            maxLines = 1,
            minScale = 0.55f,
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(u * 48f)
                .background(if (player.finished) Color(0xFF2EA043) else Color(0xFFE0B25C), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (player.finished) Icons.Filled.Check else Icons.Filled.HourglassTop,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(u * 30f)
            )
        }
    }
}

/**
 * The standings for three or more players, on a paper card: place, picture, name, how many they got right, and their score.
 * A player who left after the round is listed too (a row that vanished read as a bug), tagged and without a place.
 */
@Composable
private fun StandingsCard(
    ranked: List<OnlinePlayer>,
    left: List<OnlinePlayer>,
    myUid: String?,
    rematchVotes: Collection<String>,
    placement: Int,
    u: androidx.compose.ui.unit.Dp
) {
    val fontScale0 = LocalDensity.current.fontScale
    fun fs(art: Float) = (art * u.value / fontScale0).sp
    PaperPanel(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(u * 6f)) {
            FitText(
                text = stringResource(R.string.online_result_placement, placement),
                style = PaintedStyle(
                    color = if (placement == 1) Color(0xFF1F9A3A) else ResultInk,
                    fontSize = fs(38f),
                    textAlign = TextAlign.Center
                ),
                maxLines = 1,
                minScale = 0.6f,
                modifier = Modifier.fillMaxWidth()
            )
            (ranked.map { it to false } + left.map { it to true }).forEachIndexed { index, (player, hasLeft) ->
                val rank = if (hasLeft) null else ranked.indexOf(player) + 1
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = rank?.let { placementEmoji(it) ?: "$it." } ?: "•",
                        style = PaintedStyle(color = ResultInk, fontSize = fs(34f), textAlign = TextAlign.Center),
                        modifier = Modifier.width(u * 58f)
                    )
                    PlayerPicture(player, u * 66f)
                    Spacer(Modifier.width(u * 12f))
                    Column(modifier = Modifier.weight(1f)) {
                        FitText(
                            text = if (player.uid == myUid) stringResource(R.string.online_you_label, player.displayName) else player.displayName,
                            style = PaintedStyle(color = if (hasLeft) Color(0xFF8E1F1F) else ResultInk, fontSize = fs(30f), textAlign = TextAlign.Start),
                            maxLines = 1,
                            minScale = 0.55f,
                            contentAlignment = Alignment.CenterStart,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = if (hasLeft) stringResource(R.string.online_player_left_lobby)
                            else stringResource(R.string.online_correct_of_total, player.correctCount, player.correctCount + player.wrongCount),
                            style = PaintedStyle(color = if (hasLeft) Color(0xFF8E1F1F) else Color(0xFF8A6A50), fontSize = fs(22f), textAlign = TextAlign.Start),
                            maxLines = 1
                        )
                    }
                    if (!hasLeft && player.uid in rematchVotes) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF2EA043), modifier = Modifier.size(u * 34f))
                        Spacer(Modifier.width(u * 6f))
                    }
                    Text(
                        text = player.totalScore.toString(),
                        style = PaintedStyle(color = XpOrange, fontSize = fs(46f), textAlign = TextAlign.End),
                        maxLines = 1
                    )
                }
            }
        }
    }
}
