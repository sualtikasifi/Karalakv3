package com.sualtikasifi.cizimhafiza.presentation.duel

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.Duel
import com.sualtikasifi.cizimhafiza.domain.model.FriendRequest
import com.sualtikasifi.cizimhafiza.presentation.common.LetteredText
import com.sualtikasifi.cizimhafiza.presentation.common.NinePatch
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedStyle
import com.sualtikasifi.cizimhafiza.presentation.common.cachedPainterResource
import com.sualtikasifi.cizimhafiza.presentation.common.pressable
import com.sualtikasifi.cizimhafiza.presentation.common.sceneIn
import com.sualtikasifi.cizimhafiza.presentation.common.springIn

private val Ink = Color(0xFF3B2314)
private val SoftInk = Color(0xFF7A5A40)
private val Win = Color(0xFF1E8A3A)
private val Loss = Color(0xFFC0392B)

// The cards are drawn at this many picture pixels wide; every size below is a share of the card's real width, so
// the pictures are only ever stretched downwards (the middle band of a card grows with its rows).
private const val CardPx = 860f

@Composable
fun DuelListScreen(
    onBack: () -> Unit,
    onPlayDuel: (duel: Duel) -> Unit,
    viewModel: DuelListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var resultDuel by remember { mutableStateOf<RecentDuel?>(null) }

    Box(modifier = Modifier.fillMaxSize().sceneIn()) {
        Image(
            painter = cachedPainterResource(R.drawable.du_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxSize()
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.du_title),
                contentDescription = stringResource(R.string.duel_list_title),
                modifier = Modifier.fillMaxWidth(0.82f).aspectRatio(1000f / 347f).padding(top = 6.dp).springIn(index = 0, stepMs = 0, fromY = 20)
            )

            // --- Incoming: challenges to play + friend requests to answer ---
            DuelCard(R.drawable.du_card_a, 434f, stringResource(R.string.duel_list_requests_title), Modifier.springIn(index = 1, stepMs = 70, fromY = 30)) {
                if (uiState.isLoading) {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(modifier = Modifier.size(28.dp)) }
                } else if (uiState.incoming.isEmpty() && uiState.friendRequests.isEmpty()) {
                    EmptyPanel(R.drawable.du_ic_mail, stringResource(R.string.duel_list_requests_empty))
                } else {
                    uiState.incoming.forEach { duel -> androidx.compose.runtime.key("duel_" + duel.id) { IncomingDuelRow(duel = duel, onClick = { onPlayDuel(duel) }) } }
                    uiState.friendRequests.forEach { request ->
                        androidx.compose.runtime.key("req_" + request.uid) {
                            FriendRequestRow(
                                request = request,
                                busy = uiState.answeringRequestUid == request.uid,
                                onAccept = { viewModel.acceptFriendRequest(request) },
                                onDecline = { viewModel.declineFriendRequest(request) }
                            )
                        }
                    }
                }
            }

            // --- Recent: finished duels, both directions ---
            DuelCard(R.drawable.du_card_b, 440f, stringResource(R.string.duel_list_recent_title), Modifier.springIn(index = 2, stepMs = 70, fromY = 30)) {
                if (!uiState.isLoading && uiState.recent.isEmpty()) {
                    EmptyPanel(R.drawable.du_ic_out, stringResource(R.string.duel_list_recent_empty))
                } else {
                    uiState.recent.forEach { recent ->
                        androidx.compose.runtime.key("recent_" + recent.id) {
                            RecentDuelRow(
                                recent = recent,
                                onClick = {
                                    resultDuel = recent
                                    if (recent.isNew) viewModel.markSeen(recent.id)
                                },
                                onDelete = { viewModel.deleteDuel(recent.id) }
                            )
                        }
                    }
                }
            }

            // --- Sent and still waiting ---
            DuelCard(R.drawable.du_card_c, 431f, stringResource(R.string.duel_list_sent_title), Modifier.springIn(index = 3, stepMs = 70, fromY = 30)) {
                if (!uiState.isLoading && uiState.pendingSent.isEmpty()) {
                    EmptyPanel(R.drawable.du_ic_out, stringResource(R.string.duel_list_sent_empty))
                } else {
                    uiState.pendingSent.forEach { duel -> androidx.compose.runtime.key("sent_" + duel.id) { PendingSentRow(duel = duel, onDelete = { viewModel.deleteDuel(duel.id) }) } }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        Image(
            painter = painterResource(R.drawable.du_back),
            contentDescription = stringResource(R.string.cd_back),
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 14.dp, top = 12.dp)
                .size(56.dp)
                .pressable(pressedScale = 0.88f, onClick = onBack)
        )
    }

    resultDuel?.let { recent ->
        DuelResultDialog(recent = recent, onDismiss = { resultDuel = null })
    }
}

/**
 * One painted card: the wooden ribbon with [title] lettered on it, and the rows in the paper below. The picture is
 * drawn at its own proportions across the width, so only its middle band stretches (downwards) with the rows.
 */
@Composable
private fun DuelCard(res: Int, nativeH: Float, title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val unit = maxWidth / CardPx            // dp per picture pixel
        val top = unit * 150f                   // the ribbon and the top of the frame
        val bottom = unit * 90f
        val minHeight = unit * nativeH
        Box(Modifier.fillMaxWidth()) {
            NinePatch(
                res = res,
                slicePx = 110,
                edge = unit * 110f,
                sliceYPx = 150,
                edgeY = top,
                modifier = Modifier.matchParentSize()
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = top, bottom = bottom, start = unit * 70f, end = unit * 70f)
                    .heightIn(min = minHeight - top - bottom),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) { content() }
            // The title on the ribbon (the ribbon runs from 6% to 62% of the card's width).
            LetteredText(
                text = title,
                size = (unit.value * 56f).sp,
                outline = Color(0xFF5A2815),
                modifier = Modifier.offset(unit * 90f, unit * 20f).size(unit * 440f, unit * 78f),
                minScale = 0.6f
            )
        }
    }
}

@Composable
private fun EmptyPanel(icon: Int, message: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(84.dp))
        Spacer(Modifier.height(6.dp))
        Text(
            text = message,
            style = PaintedStyle(color = SoftInk, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, lineHeight = 19.sp)
        )
    }
}

@Composable
private fun PillButton(res: Int, text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.height(38.dp).pressable(pressedScale = 0.92f, onClick = onClick)
    ) {
        Image(painterResource(res), contentDescription = null, contentScale = ContentScale.FillBounds, modifier = Modifier.matchParentSize())
        LetteredText(text, 14.sp, outline = null, modifier = Modifier.padding(horizontal = 10.dp).fillMaxWidth().height(24.dp), minScale = 0.6f)
    }
}

@Composable
private fun IncomingDuelRow(duel: Duel, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().pressable(pressedScale = 0.96f, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Image(painterResource(R.drawable.du_av_other), contentDescription = null, modifier = Modifier.size(46.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(duel.challengerName, style = PaintedStyle(color = Ink, fontSize = 18.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(stringResource(R.string.duel_list_word_count, duel.totalWords), style = PaintedStyle(color = SoftInk, fontSize = 14.sp, fontWeight = FontWeight.SemiBold))
        }
        PillButton(R.drawable.du_pill_orange, stringResource(R.string.duel_list_incoming_play), Modifier.width(88.dp), onClick)
    }
}

@Composable
private fun FriendRequestRow(request: FriendRequest, busy: Boolean, onAccept: () -> Unit, onDecline: () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Image(painterResource(R.drawable.du_av_other), contentDescription = null, modifier = Modifier.size(46.dp))
            Text(
                text = stringResource(R.string.duel_list_friend_request, request.nickname),
                style = PaintedStyle(color = Ink, fontSize = 16.sp, lineHeight = 19.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        if (busy) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(modifier = Modifier.size(26.dp)) }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                PillButton(R.drawable.du_pill_blue, stringResource(R.string.friends_request_decline), Modifier.weight(1f), onDecline)
                PillButton(R.drawable.du_pill_green, stringResource(R.string.friends_request_accept), Modifier.weight(1f), onAccept)
            }
        }
    }
}

/**
 * A finished duel as a small scoreboard: the verdict and its delete cross on top, then both sides — avatar, name,
 * score and "X/N doğru" — either side of the splash "VS". The winner's figure is picked out in green.
 */
@Composable
private fun RecentDuelRow(recent: RecentDuel, onClick: () -> Unit, onDelete: () -> Unit) {
    val accent = when (recent.iWon) { true -> Win; false -> Loss; null -> SoftInk }
    Column(Modifier.fillMaxWidth().pressable(pressedScale = 0.97f, onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = when (recent.iWon) {
                    true -> stringResource(R.string.duel_list_status_won)
                    false -> stringResource(R.string.duel_list_status_lost)
                    null -> stringResource(R.string.duel_list_status_tied)
                },
                style = PaintedStyle(color = accent, fontSize = 20.sp),
                modifier = Modifier.weight(1f)
            )
            if (recent.isNew) {
                Box(Modifier.size(9.dp).background(Loss, CircleShape))
                Spacer(Modifier.width(8.dp))
            }
            Image(
                painterResource(R.drawable.du_x),
                contentDescription = stringResource(R.string.duel_list_delete),
                modifier = Modifier.size(32.dp).pressable(pressedScale = 0.85f, onClick = onDelete)
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Image(painterResource(R.drawable.du_av_me), contentDescription = null, modifier = Modifier.size(52.dp))
            ScoreColumn(stringResource(R.string.quick_match_you), recent.myScore, recent.myCorrectCount, recent.totalWords, recent.iWon == true, Modifier.weight(1f))
            Image(painterResource(R.drawable.du_vs), contentDescription = stringResource(R.string.quick_match_versus), modifier = Modifier.size(52.dp))
            ScoreColumn(recent.otherName, recent.otherScore, recent.otherCorrectCount, recent.totalWords, recent.iWon == false, Modifier.weight(1f))
            Image(painterResource(R.drawable.du_av_other), contentDescription = null, modifier = Modifier.size(52.dp))
        }
    }
}

@Composable
private fun ScoreColumn(name: String, score: Int, correctCount: Int, totalWords: Int, highlighted: Boolean, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(name, style = PaintedStyle(color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("$score", style = PaintedStyle(color = if (highlighted) Win else Ink, fontSize = 26.sp, textAlign = TextAlign.Center), maxLines = 1)
        Text(
            stringResource(R.string.online_correct_of_total, correctCount, totalWords),
            style = PaintedStyle(color = SoftInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center),
            maxLines = 1
        )
    }
}

@Composable
private fun PendingSentRow(duel: Duel, onDelete: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Image(painterResource(R.drawable.du_av_other), contentDescription = null, modifier = Modifier.size(46.dp))
        Column(Modifier.weight(1f)) {
            Text(duel.opponentName, style = PaintedStyle(color = Ink, fontSize = 18.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(stringResource(R.string.duel_list_status_waiting), style = PaintedStyle(color = SoftInk, fontSize = 14.sp, fontWeight = FontWeight.SemiBold))
        }
        Image(
            painterResource(R.drawable.du_x),
            contentDescription = stringResource(R.string.duel_list_delete),
            modifier = Modifier.size(32.dp).pressable(pressedScale = 0.85f, onClick = onDelete)
        )
    }
}

@Composable
private fun DuelResultDialog(recent: RecentDuel, onDismiss: () -> Unit) {
    com.sualtikasifi.cizimhafiza.presentation.common.PaintedDialog(
        title = when (recent.iWon) {
            true -> stringResource(R.string.duel_list_status_won)
            false -> stringResource(R.string.duel_list_status_lost)
            null -> stringResource(R.string.duel_list_status_tied)
        },
        onDismiss = onDismiss,
        buttons = { com.sualtikasifi.cizimhafiza.presentation.common.PaintedPillButton(text = stringResource(R.string.close), onClick = onDismiss, modifier = Modifier.fillMaxWidth()) }
    ) {
        com.sualtikasifi.cizimhafiza.presentation.common.PaintedDialogText(stringResource(R.string.duel_result_score_format, stringResource(R.string.quick_match_you), recent.myScore))
        com.sualtikasifi.cizimhafiza.presentation.common.PaintedDialogText(stringResource(R.string.duel_result_score_format, recent.otherName, recent.otherScore))
    }
}
