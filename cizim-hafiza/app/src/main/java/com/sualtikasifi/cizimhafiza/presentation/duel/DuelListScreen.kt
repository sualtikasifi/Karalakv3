package com.sualtikasifi.cizimhafiza.presentation.duel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.text.style.TextOverflow
import com.sualtikasifi.cizimhafiza.domain.model.FriendRequest
import com.sualtikasifi.cizimhafiza.presentation.common.SecondaryButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.sualtikasifi.cizimhafiza.presentation.theme.AppTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.Duel
import com.sualtikasifi.cizimhafiza.domain.model.DuelStatus
import com.sualtikasifi.cizimhafiza.presentation.common.IconWell
import com.sualtikasifi.cizimhafiza.presentation.common.RaisedCard
import com.sualtikasifi.cizimhafiza.presentation.common.EmptyState
import com.sualtikasifi.cizimhafiza.presentation.common.ScreenTopActions
import com.sualtikasifi.cizimhafiza.presentation.common.TopActionsClearance
import com.sualtikasifi.cizimhafiza.presentation.common.screenBackground


@Composable
fun DuelListScreen(
    onBack: () -> Unit,
    onPlayDuel: (duelId: String) -> Unit,
    viewModel: DuelListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var resultDuel by remember { mutableStateOf<RecentDuel?>(null) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .screenBackground()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            // Clears the floating back button (see ScreenTopActions).
            contentPadding = PaddingValues(top = TopActionsClearance, bottom = 16.dp)
        ) {
            // --- Incoming: challenges to play + friend requests to answer ---
            item {
                Text(text = stringResource(R.string.duel_list_requests_title), style = MaterialTheme.typography.titleMedium)
            }
            if (uiState.isLoading) {
                item { CircularProgressIndicator(modifier = Modifier.padding(16.dp).size(28.dp)) }
            } else if (uiState.incoming.isEmpty() && uiState.friendRequests.isEmpty()) {
                item {
                    EmptyState(emoji = "📭", message = stringResource(R.string.duel_list_requests_empty))
                }
            } else {
                items(uiState.incoming, key = { "duel_" + it.id }) { duel ->
                    IncomingDuelCard(duel = duel, onClick = { onPlayDuel(duel.id) })
                }
                items(uiState.friendRequests, key = { "req_" + it.uid }) { request ->
                    FriendRequestCard(
                        request = request,
                        busy = uiState.answeringRequestUid == request.uid,
                        onAccept = { viewModel.acceptFriendRequest(request) },
                        onDecline = { viewModel.declineFriendRequest(request) }
                    )
                }
            }

            // --- Recent: finished duels, both directions ---
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = stringResource(R.string.duel_list_recent_title), style = MaterialTheme.typography.titleMedium)
            }
            if (!uiState.isLoading && uiState.recent.isEmpty()) {
                item {
                    EmptyState(emoji = "🏁", message = stringResource(R.string.duel_list_recent_empty))
                }
            } else {
                items(uiState.recent, key = { "recent_" + it.duel.id }) { recent ->
                    RecentDuelCard(
                        recent = recent,
                        onClick = {
                            resultDuel = recent
                            if (recent.isNew) viewModel.markSeen(recent.duel.id)
                        },
                        onDelete = { viewModel.deleteDuel(recent.duel.id) }
                    )
                }
            }

            // --- Sent and still waiting ---
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = stringResource(R.string.duel_list_sent_title), style = MaterialTheme.typography.titleMedium)
            }
            if (!uiState.isLoading && uiState.pendingSent.isEmpty()) {
                item {
                    EmptyState(emoji = "📤", message = stringResource(R.string.duel_list_sent_empty))
                }
            } else {
                items(uiState.pendingSent, key = { "sent_" + it.id }) { duel ->
                    PendingSentCard(duel = duel, onDelete = { viewModel.deleteDuel(duel.id) })
                }
            }
        }
        ScreenTopActions(onBack = onBack, title = stringResource(R.string.duel_list_title), modifier = Modifier.align(Alignment.TopStart))
        }
    }

    resultDuel?.let { recent ->
        DuelResultDialog(recent = recent, onDismiss = { resultDuel = null })
    }
}

@Composable
private fun IncomingDuelCard(duel: Duel, onClick: () -> Unit) {
    RaisedCard(corner = 18.dp, onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconWell(icon = Icons.Filled.EmojiEvents)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = duel.challengerName, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = stringResource(R.string.duel_list_word_count, duel.totalWords),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FriendRequestCard(request: FriendRequest, busy: Boolean, onAccept: () -> Unit, onDecline: () -> Unit) {
    RaisedCard(corner = 18.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconWell(icon = Icons.Filled.PersonAdd)
            Text(
                text = stringResource(R.string.duel_list_friend_request, request.nickname),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                TextButton(onClick = onDecline) {
                    Text(
                        text = stringResource(R.string.friends_request_decline),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                SecondaryButton(text = stringResource(R.string.friends_request_accept), onClick = onAccept)
            }
        }
    }
}

@Composable
private fun RecentDuelCard(recent: RecentDuel, onClick: () -> Unit, onDelete: () -> Unit) {
    RaisedCard(corner = 18.dp, onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconWell(
                icon = Icons.Filled.EmojiEvents,
                tint = when (recent.iWon) {
                    true -> AppTheme.tokens.success
                    false -> MaterialTheme.colorScheme.error
                    null -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = recent.otherName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = when (recent.iWon) {
                        true -> stringResource(R.string.duel_list_status_won)
                        false -> stringResource(R.string.duel_list_status_lost)
                        null -> stringResource(R.string.duel_list_status_tied)
                    } + "  ·  " + stringResource(R.string.duel_list_score_line, recent.myScore, recent.otherScore),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (recent.isNew) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(MaterialTheme.colorScheme.error, CircleShape)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.duel_list_delete), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PendingSentCard(duel: Duel, onDelete: () -> Unit) {
    RaisedCard(corner = 18.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconWell(icon = Icons.Filled.HourglassEmpty, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = duel.opponentName, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = stringResource(R.string.duel_list_status_waiting),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.duel_list_delete), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DuelResultDialog(recent: RecentDuel, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (recent.iWon) {
                    true -> stringResource(R.string.duel_list_status_won)
                    false -> stringResource(R.string.duel_list_status_lost)
                    null -> stringResource(R.string.duel_list_status_tied)
                }
            )
        },
        text = {
            Column {
                Text(stringResource(R.string.duel_result_score_format, stringResource(R.string.quick_match_you), recent.myScore))
                Text(stringResource(R.string.duel_result_score_format, recent.otherName, recent.otherScore))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        }
    )
}
