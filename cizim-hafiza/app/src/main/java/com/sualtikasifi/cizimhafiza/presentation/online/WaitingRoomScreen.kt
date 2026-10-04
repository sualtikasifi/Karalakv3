package com.sualtikasifi.cizimhafiza.presentation.online

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.sualtikasifi.cizimhafiza.presentation.common.NinePatch
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.Image
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloat
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedStyle
import java.util.Locale
import com.sualtikasifi.cizimhafiza.data.bot.BotRoomEngine
import com.sualtikasifi.cizimhafiza.domain.model.KickedUser
import com.sualtikasifi.cizimhafiza.domain.model.OnlinePlayer
import com.sualtikasifi.cizimhafiza.domain.model.Friend
import com.sualtikasifi.cizimhafiza.domain.model.LevelTier
import com.sualtikasifi.cizimhafiza.domain.model.Reaction
import com.sualtikasifi.cizimhafiza.domain.model.RoomStatus
import com.sualtikasifi.cizimhafiza.presentation.common.EmptyState
import com.sualtikasifi.cizimhafiza.presentation.common.PrimaryButton
import com.sualtikasifi.cizimhafiza.presentation.common.RaisedCard
import com.sualtikasifi.cizimhafiza.domain.model.AvatarFrame
import com.sualtikasifi.cizimhafiza.presentation.common.LevelAvatar
import com.sualtikasifi.cizimhafiza.presentation.common.TintedBadge
import com.sualtikasifi.cizimhafiza.presentation.common.SecondaryButton
import com.sualtikasifi.cizimhafiza.presentation.common.ScreenTopActions
import com.sualtikasifi.cizimhafiza.presentation.common.TopActionsClearance
import com.sualtikasifi.cizimhafiza.presentation.common.screenBackground
import com.sualtikasifi.cizimhafiza.presentation.common.drawOrbitSparks
import com.sualtikasifi.cizimhafiza.presentation.theme.AppTheme
import com.sualtikasifi.cizimhafiza.util.GameConstants
import com.sualtikasifi.cizimhafiza.util.InviteShareUtil
import kotlinx.coroutines.delay
import com.sualtikasifi.cizimhafiza.util.UiText
import com.sualtikasifi.cizimhafiza.util.asString

@Composable
fun WaitingRoomScreen(
    onGameStarted: (roomCode: String) -> Unit,
    onLeave: () -> Unit,
    viewModel: WaitingRoomViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val phraseUsageCounts by viewModel.phraseUsageCounts.collectAsState()
    val emojiUsageCounts by viewModel.emojiUsageCounts.collectAsState()
    val context = LocalContext.current
    val room = uiState.room
    val myUid = viewModel.myUid
    val isHost = room != null && room.hostUid == myUid
    val me = room?.players?.find { it.uid == myUid }
    // Only players actually still here. leaveRoom covers a clean exit, but a
    // force-closed app never sets it — that entry would otherwise linger as a
    // phantom player forever, and being permanently un-ready it would also
    // stop the room from ever starting a match. The bot is exempt: it has no
    // device to check in from (see OnlinePlayer.isPresent).
    val isPresent: (OnlinePlayer) -> Boolean = { it.uid == BotRoomEngine.BOT_UID || it.isPresent() }
    val others = room?.players?.filter { it.uid != myUid && isPresent(it) } ?: emptyList()
    val presentPlayerCount = room?.players?.count(isPresent) ?: 1
    val amPending = me?.pendingNextRound == true
    val teamMode = room?.teamMode == true
    // Ready-check only applies to players actually in this round — a
    // pendingNextRound joiner (sitting out the round already in progress)
    // shouldn't block the others from starting/being "all ready".
    val activePlayers = room?.players?.filter { !it.pendingNextRound && isPresent(it) } ?: emptyList()
    // A 2v2 room additionally needs BOTH teams actually full — two ready
    // players who both picked Team A is not a match, whatever the total
    // headcount says.
    val allReady = if (teamMode) {
        activePlayers.size == GameConstants.TEAM_ROOM_SIZE &&
            activePlayers.count { it.teamId == "A" } == GameConstants.TEAM_SIZE &&
            activePlayers.count { it.teamId == "B" } == GameConstants.TEAM_SIZE &&
            activePlayers.all { it.ready }
    } else {
        activePlayers.size >= 2 && activePlayers.all { it.ready }
    }
    val amReady = uiState.readyOverride ?: (me?.ready == true)

    // Why a full, all-ready 2v2 room still cannot start. Computed here
    // alongside allReady so the two can never disagree.
    val teamImbalanceHint: Int? = when {
        !teamMode -> null
        activePlayers.size < GameConstants.TEAM_ROOM_SIZE -> R.string.online_team_needs_players
        activePlayers.count { it.teamId == "A" } != GameConstants.TEAM_SIZE ||
            activePlayers.count { it.teamId == "B" } != GameConstants.TEAM_SIZE ->
            R.string.online_team_unbalanced
        else -> null
    }

    var kickTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    var invitePickerOpen by remember { mutableStateOf(false) }
    val friends by viewModel.friends.collectAsState()
    val inviteState by viewModel.inviteState.collectAsState()

    // Per-sender chat bubble state (see ReactionBar.rememberActiveReactionsByUid)
    // — each entry is shown anchored on that sender's own slot card below,
    // instead of one shared pop-up that didn't say who was talking.
    val activeReactionsByUid = rememberActiveReactionsByUid(uiState.reactions)

    // Sude shows the same level-badge avatar a real player would (see
    // BotRoomEngine.BOT_LEVEL) rather than dedicated bot art, so she reads as
    // another player filling a lobby slot.
    val mySlot = me?.let {
        PlayerSlotUiState(
            uid = it.uid,
            name = it.displayName,
            level = it.level,
            frame = AvatarFrame.resolve(it.frameId, it.level),
            avatarUrl = it.avatarUrl,
            ready = amReady,
            isHost = room?.hostUid == it.uid,
            isYou = true,
            pending = amPending,
            onKick = null,
            teamId = it.teamId,
            // Only your own slot can switch — this is a self-service pick,
            // not something a teammate or the host can move for you.
            onSwitchTeam = if (teamMode) {
                { viewModel.switchTeam(if (it.teamId == "A") "B" else "A") }
            } else null
        )
    }
    val otherSlots = others.map { player ->
        val isBot = player.uid == BotRoomEngine.BOT_UID
        PlayerSlotUiState(
            uid = player.uid,
            name = player.displayName,
            level = if (isBot) BotRoomEngine.BOT_LEVEL else player.level,
            avatarUrl = if (isBot) "" else player.avatarUrl,
            frame = if (isBot) {
                AvatarFrame.highestUnlockedFor(BotRoomEngine.BOT_LEVEL)
            } else {
                AvatarFrame.resolve(player.frameId, player.level)
            },
            ready = player.ready,
            isHost = room?.hostUid == player.uid,
            isYou = false,
            pending = player.pendingNextRound,
            onKick = if (isHost) {
                { kickTarget = player.uid to player.displayName }
            } else null,
            teamId = player.teamId,
            onSwitchTeam = null
        )
    }
    // In the permanent bot room, Sude always takes the very first slot
    // (top-left) rather than wherever she happens to land by join order —
    // she is the one constant face in that room, so her seat should be too.
    val occupiedSlots = (listOfNotNull(mySlot) + otherSlots).let { slots ->
        if (viewModel.roomCode == BotRoomEngine.ROOM_CODE) {
            slots.sortedByDescending { it.uid == BotRoomEngine.BOT_UID }
        } else {
            slots
        }
    }
    // Padded to a fixed 8-slot grid (see GameConstants.MAX_ROOM_SIZE) so the
    // lobby reads as slots being filled in, not a list that happens to be
    // short right now. Team mode instead pads each team to exactly
    // GameConstants.TEAM_SIZE — see teamASlots/teamBSlots below.
    val playerSlots: List<PlayerSlotUiState?> =
        occupiedSlots + List((GameConstants.MAX_ROOM_SIZE - occupiedSlots.size).coerceAtLeast(0)) { null }
    val teamASlots: List<PlayerSlotUiState?> = occupiedSlots.filter { it.teamId == "A" }
        .let { it + List((GameConstants.TEAM_SIZE - it.size).coerceAtLeast(0)) { null } }
    val teamBSlots: List<PlayerSlotUiState?> = occupiedSlots.filter { it.teamId == "B" }
        .let { it + List((GameConstants.TEAM_SIZE - it.size).coerceAtLeast(0)) { null } }

    LaunchedEffect(room?.status, amPending) {
        if (room?.status == RoomStatus.PLAYING && !amPending) {
            onGameStarted(viewModel.roomCode)
        }
    }

    BackHandler {
        viewModel.leaveRoom()
        onLeave()
    }

    // No title bar: the back button floats directly on the page's own
    // background instead of sitting in a separate, differently-colored strip.
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val sceneDensity = LocalDensity.current
    val scenePxW = with(sceneDensity) { maxWidth.toPx() }
    val scenePxH = with(sceneDensity) { maxHeight.toPx() }
    val sceneScale = maxOf(scenePxW / LOBBY_ART_W, scenePxH / LOBBY_ART_H)
    val sceneOffY = (scenePxH - LOBBY_ART_H * sceneScale) / 2f
    fun sceneY(fraction: Float): Dp = with(sceneDensity) { (sceneOffY + LOBBY_ART_H * sceneScale * fraction).toDp() }
    Image(
        painter = painterResource(R.drawable.bg_lobby),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
    )
    Scaffold(
        containerColor = Color.Transparent,
        // The action controls are pinned rather than living at the end of the
        // scroll: the player list is the only part that should ever grow, and
        // in a full room it used to push "Hazırım" (and the last player row)
        // off the bottom of the screen entirely.
        bottomBar = {
            WaitingRoomActions(
                amPending = amPending,
                hasOthers = others.isNotEmpty(),
                allReady = allReady,
                teamImbalanceHint = teamImbalanceHint,
                amReady = amReady,
                isHost = isHost,
                isStarting = uiState.isStarting,
                countdownSeconds = uiState.countdownSeconds,
                errorMessage = uiState.errorMessage,
                phraseUsageCounts = phraseUsageCounts,
                emojiUsageCounts = emojiUsageCounts,
                onToggleReady = viewModel::toggleReady,
                onStartGame = viewModel::startGame,
                onSendReaction = viewModel::sendReaction
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
        // A plain scrollable Column, not a LazyColumn: with sizes trimmed
        // down (SLOT_HEIGHT, the card paddings below) a normal room's whole
        // grid sits still inside the viewport, and this scrolls only as a
        // fallback on a short phone or a full 8-slot room — it no longer
        // sways up and down on its own the way the lazily-measured list did.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                // Starts under the painted sign.
                .padding(top = sceneY(0.205f), bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RoomCodeCard(
                roomCode = viewModel.roomCode,
                onInvite = { InviteShareUtil.shareRoomInvite(context, viewModel.roomCode) }
            )

            if (amPending) {
                PendingNextRoundNotice(
                    startedAtMillis = room?.startedAt,
                    estimatedRoundSeconds = uiState.estimatedRoundSeconds
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.height(46.dp).aspectRatio(720f / 233f)) {
                    Image(painter = painterResource(R.drawable.lobby_band), contentDescription = null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
                    OutlinedLabel(stringResource(R.string.online_players_section_title), 19.sp)
                }
                Box(contentAlignment = Alignment.Center, modifier = Modifier.height(40.dp).aspectRatio(568f / 173f)) {
                    Image(painter = painterResource(R.drawable.lobby_pill), contentDescription = null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = Color(0xFFE9801D), modifier = Modifier.size(18.dp))
                        Text(
                            text = stringResource(
                                R.string.online_room_occupancy,
                                presentPlayerCount,
                                if (teamMode) GameConstants.TEAM_ROOM_SIZE else GameConstants.MAX_ROOM_SIZE
                            ),
                            style = PaintedStyle(color = Color(0xFF5A3A1A), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold),
                            maxLines = 1
                        )
                    }
                }
            }

            if (teamMode) {
                // Two team columns instead of one flat grid — a 2v2 room's
                // whole point is which SIDE you're on, so the lobby needs to
                // show that grouping, not just a headcount.
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TeamColumn(
                        title = stringResource(R.string.online_team_a),
                        slots = teamASlots,
                        activeReactionsByUid = activeReactionsByUid,
                        onInvite = { invitePickerOpen = true },
                        modifier = Modifier.weight(1f)
                    )
                    TeamColumn(
                        title = stringResource(R.string.online_team_b),
                        slots = teamBSlots,
                        activeReactionsByUid = activeReactionsByUid,
                        onInvite = { invitePickerOpen = true },
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                // A fixed 2-column grid of GameConstants.MAX_ROOM_SIZE slots —
                // each occupied player takes half a row instead of a whole one,
                // and the still-empty slots stay visible as placeholders so the
                // room reads as "being filled in" rather than a short list.
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    playerSlots.chunked(2).forEach { rowSlots ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowSlots.forEach { slot ->
                                PlayerSlotCell(
                                    slot = slot,
                                    activeReaction = slot?.let { activeReactionsByUid[it.uid] },
                                    onInvite = { invitePickerOpen = true },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            if (others.isEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .background(Color(0xCC3B1E08), RoundedCornerShape(18.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Image(painter = painterResource(R.drawable.lobby_hourglass), contentDescription = null, modifier = Modifier.height(30.dp))
                    Text(
                        text = stringResource(R.string.online_waiting_for_friend),
                        style = PaintedStyle(color = Color(0xFFFFF1D6), fontSize = 15.sp, fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (isHost && room?.kickedUsers?.isNotEmpty() == true) {
                KickedUsersSection(kickedUsers = room.kickedUsers, onUnban = viewModel::unbanPlayer)
            }
        }
        // The title, written on the painted sign.
        val signTitle = stringResource(R.string.online_waiting_room_title)
        Box(
            modifier = Modifier.fillMaxWidth().offset(y = sceneY(0.147f) - 30.dp),
            contentAlignment = Alignment.Center
        ) {
            val base = PaintedStyle(fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
            Text(signTitle, style = base.copy(color = Color(0xFF5A2E0C), drawStyle = Stroke(width = 8f, join = StrokeJoin.Round)), maxLines = 1)
            Text(signTitle, style = base.copy(color = Color.White), maxLines = 1)
        }
        Image(
            painter = painterResource(R.drawable.join_back),
            contentDescription = stringResource(R.string.cd_back),
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 16.dp, top = 12.dp)
                .size(56.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    viewModel.leaveRoom()
                    onLeave()
                }
        )
        }
    }
    }

    if (invitePickerOpen) {
        LobbyInviteSheet(
            friends = friends,
            sendingToUid = inviteState.sendingToUid,
            sentToUids = inviteState.sentToUids,
            message = inviteState.message?.asString(),
            onInvite = viewModel::inviteFriendToRoom,
            onDismiss = { invitePickerOpen = false }
        )
    }

    // The send result is reported over the lobby rather than inside the
    // sheet: the sheet is dismissible mid-send, and an invite that has
    // actually left should still say so.
    inviteState.message?.let { message ->
        LaunchedEffect(message) {
            delay(2_500)
            viewModel.consumeInviteMessage()
        }
        Box(
            modifier = Modifier.fillMaxSize().padding(bottom = 96.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            TintedBadge(text = message.asString())
        }
    }

    kickTarget?.let { (targetUid, targetName) ->
        AlertDialog(
            onDismissRequest = { kickTarget = null },
            title = { Text(stringResource(R.string.online_kick_confirm_title)) },
            text = { Text(stringResource(R.string.online_kick_confirm_message, targetName)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.kickPlayer(targetUid, targetName)
                    kickTarget = null
                }) {
                    Text(stringResource(R.string.online_kick_confirm_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { kickTarget = null }) {
                    Text(stringResource(R.string.online_kick_confirm_cancel))
                }
            }
        )
    }
}

/** The room code plus its one action — the thing you actually came to this screen to hand someone. */
@Composable
private fun RoomCodeCard(roomCode: String, onInvite: () -> Unit) {
    // lobby_codepanel is painted with its code well and its invite button; the texts are laid over them by fractions.
    androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxWidth().aspectRatio(753f / 388f)) {
        val w = maxWidth
        val h = maxHeight
        Image(
            painter = painterResource(R.drawable.lobby_codepanel),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )
        Text(
            text = stringResource(R.string.online_room_code_hint),
            style = PaintedStyle(color = Color(0xFF5A3A1A), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold),
            maxLines = 1,
            modifier = Modifier.align(Alignment.TopCenter).offset(y = h * 0.1f)
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.offset(x = w * 0.17f, y = h * 0.26f).width(w * 0.68f).height(h * 0.265f)
        ) {
            Text(
                text = roomCode,
                style = PaintedStyle(color = Color(0xFF5A3A1A), fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 8.sp),
                maxLines = 1
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset(x = w * 0.2f, y = h * 0.585f)
                .width(w * 0.62f)
                .height(h * 0.27f)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onInvite)
        ) {
            Icon(Icons.Filled.Share, contentDescription = null, tint = Color.White, modifier = Modifier.align(Alignment.CenterStart).padding(start = w * 0.07f).size(22.dp))
            OutlinedLabel(stringResource(R.string.online_invite_friend), 20.sp, outline = Color(0xFF8A3A00))
        }
    }
}

/** A short message on a dark plate, readable over the painted scene. */
@Composable
private fun HintPlate(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = PaintedStyle(color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
        modifier = modifier
            .background(Color(0xCC3B1E08), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

/** The painted pill with its label: orange to press, green once chosen; it dips while pressed and can pulse. */
@Composable
private fun LobbyButton(text: String, green: Boolean, pulse: Float, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "lobby-btn")
    NinePatch(
        res = if (green) R.drawable.lobby_btn_green else R.drawable.lobby_btn,
        slicePx = 86,
        edge = 27.dp,
        modifier = modifier
            .height(56.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = 1f - 0.12f * pulse }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            OutlinedLabel(text, 24.sp, outline = if (green) Color(0xFF1B6B12) else Color(0xFF8A3A00))
        }
    }
}

/** White text with a dark outline, the lettering style of every painted title. */
@Composable
private fun OutlinedLabel(text: String, size: androidx.compose.ui.unit.TextUnit, outline: Color = Color(0xFF5A2E0C)) {
    val base = PaintedStyle(fontSize = size, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
    Box(contentAlignment = Alignment.Center) {
        Text(text, style = base.copy(color = outline, drawStyle = Stroke(width = 7f, join = StrokeJoin.Round)), maxLines = 1)
        Text(text, style = base.copy(color = Color.White), maxLines = 1)
    }
}

/** Shown only to someone who joined mid-round and is sitting the current one out. */
@Composable
private fun PendingNextRoundNotice(startedAtMillis: Long?, estimatedRoundSeconds: Int?) {
    RaisedCard(corner = 20.dp, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.online_pending_next_round_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            RoundCountdown(startedAtMillis = startedAtMillis, estimatedRoundSeconds = estimatedRoundSeconds)
        }
    }
}

/**
 * The pinned bottom half of the screen: the chat/emoji dock and whichever
 * single action is available right now. Messages themselves no longer show
 * here — see [PlayerSlotCell]'s per-sender chat bubble, anchored on that
 * player's own card up in the grid instead of one shared pop-up down here.
 */
@Composable
private fun WaitingRoomActions(
    amPending: Boolean,
    hasOthers: Boolean,
    allReady: Boolean,
    @StringRes teamImbalanceHint: Int? = null,
    amReady: Boolean,
    isHost: Boolean,
    isStarting: Boolean,
    countdownSeconds: Int?,
    errorMessage: UiText?,
    phraseUsageCounts: Map<String, Int>,
    emojiUsageCounts: Map<String, Int>,
    onToggleReady: () -> Unit,
    onStartGame: () -> Unit,
    onSendReaction: (String, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // A custom bottomBar isn't auto-inset like Scaffold's own content
            // slot is — without this, the ready/start button and the reaction
            // row sat behind the phone's own on-screen back/home/recents bar
            // on edge-to-edge devices (see MainActivity.enableEdgeToEdge).
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 6.dp, bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Always shown, including in a room you are still alone in. Gating
        // this on hasOthers meant the host — who by definition arrives first
        // — opened a lobby with no chat dock at all, and had no way to know
        // one existed until somebody else turned up. Nothing about it is
        // dead while you wait, either: your own message comes back through
        // the same reaction stream and lands as a bubble on your own card
        // (see rememberActiveReactionsByUid, which filters nobody out), and
        // anyone joining a moment later sees it if it is still fresh.
        ReactionSendRow(
            onSend = onSendReaction,
            modifier = Modifier.padding(bottom = 8.dp),
            phraseUsageCounts = phraseUsageCounts,
            emojiUsageCounts = emojiUsageCounts
        )

        errorMessage?.let { message ->
            HintPlate(message.asString(), Color(0xFFFFD6D0), Modifier.padding(bottom = 8.dp))
        }

        when {
            // A sitting-out joiner has no controls — the notice above the
            // player list already explains what they're waiting for.
            amPending -> Unit
            !hasOthers -> Unit
            else -> Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                // In a 2v2 room "everyone is ready" is not enough — the sides
                // have to be even. Without this line four ready players in a
                // 3-1 split sat looking at a ready button that would never
                // turn into a start button, with nothing saying why.
                if (!allReady) teamImbalanceHint?.let { hint ->
                    HintPlate(stringResource(hint), Color(0xFFFFF1D6), Modifier.padding(bottom = 8.dp))
                }
                // The ready button stays for everybody, host included, so "Hazır" can always be taken back.
                // Once everyone is ready the host's start button joins it and the others are told who they wait for.
                if (allReady && countdownSeconds == null && !isStarting) {
                    if (isHost) {
                        LobbyButton(
                            text = stringResource(R.string.online_start_game),
                            green = false,
                            pulse = 0f,
                            onClick = onStartGame,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        HintPlate(stringResource(R.string.online_waiting_for_host), Color(0xFFFFF1D6))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                ReadyButton(
                    amReady = amReady,
                    countdownSeconds = countdownSeconds,
                    locked = isStarting,
                    onClick = onToggleReady,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * The lobby's one ready button. Not ready: green, slowly blinking. Ready: steady green with a soft halo; tapping
 * again takes it back. Once the host starts the match the face shows 3, 2, 1 and the halo grows brighter.
 */
@Composable
private fun ReadyButton(
    amReady: Boolean,
    countdownSeconds: Int?,
    locked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val green = AppTheme.tokens.success
    val counting = countdownSeconds != null
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "ready")
    val blink by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1300, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "blink"
    )
    val halo by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(if (counting) 520 else 1500, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "halo"
    )
    // A soft halo (none while waiting to be pressed, calm once ready, a little stronger during the count) and
    // coloured sparks circling the button — quicker and more of them once the count has begun.
    val glowStrength = when {
        counting -> 0.45f + 0.15f * halo
        amReady -> 0.22f + 0.10f * halo
        else -> 0f
    }
    val glowReach = if (counting) 16.dp else 12.dp
    val lap by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(if (counting) 2200 else 4800, easing = androidx.compose.animation.core.LinearEasing)
        ),
        label = "lap"
    )
    val sparkColors = remember {
        listOf(Color(0xFFFFD54F), Color(0xFFFF8FB8), Color(0xFF7FE3FF), Color(0xFFB7F26B), Color.White)
    }
    Box(
        modifier = modifier
            .drawBehind {
                if (glowStrength > 0f) {
                    val layers = 5
                    for (i in layers downTo 1) {
                        val grow = glowReach.toPx() * i / layers
                        drawRoundRect(
                            color = green.copy(alpha = glowStrength * (1f - (i - 1f) / layers) * 0.4f),
                            topLeft = androidx.compose.ui.geometry.Offset(-grow, -grow),
                            size = androidx.compose.ui.geometry.Size(size.width + 2 * grow, size.height + 2 * grow),
                            cornerRadius = CornerRadius(size.height / 2 + grow)
                        )
                    }
                    drawOrbitSparks(
                        progress = lap,
                        count = if (counting) 18 else 11,
                        colors = sparkColors,
                        radius = 2.8.dp.toPx(),
                        outset = 7.dp.toPx(),
                        wobble = 4.dp.toPx()
                    )
                }
            }
    ) {
        LobbyButton(
            text = countdownSeconds?.toString() ?: stringResource(R.string.online_ready),
            green = amReady || counting,
            pulse = if (amReady || counting) 0f else blink,
            onClick = { if (!counting && !locked) onClick() },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * A rough, ticking "time left in the round" estimate for a pendingNextRound
 * joiner sitting in the lobby — see WaitingRoomViewModel.estimatedRoundSeconds
 * and OnlineRoom.startedAt. Shows nothing while either piece is unavailable
 * (e.g. the shared word list hasn't resolved locally yet) rather than a
 * misleading number.
 */
@Composable
private fun RoundCountdown(startedAtMillis: Long?, estimatedRoundSeconds: Int?) {
    if (startedAtMillis == null || estimatedRoundSeconds == null) return
    var remainingSeconds by remember(startedAtMillis, estimatedRoundSeconds) {
        val elapsed = (System.currentTimeMillis() - startedAtMillis) / 1000
        mutableStateOf((estimatedRoundSeconds - elapsed).coerceAtLeast(0))
    }
    LaunchedEffect(startedAtMillis, estimatedRoundSeconds) {
        while (remainingSeconds > 0) {
            delay(1_000)
            val elapsed = (System.currentTimeMillis() - startedAtMillis) / 1000
            remainingSeconds = (estimatedRoundSeconds - elapsed).coerceAtLeast(0)
        }
    }
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = stringResource(
            R.string.online_round_time_remaining,
            String.format(Locale.US, "%d:%02d", remainingSeconds / 60, remainingSeconds % 60)
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun KickedUsersSection(kickedUsers: List<KickedUser>, onUnban: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = stringResource(R.string.online_kicked_users_section_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.heightIn(max = 140.dp)) {
            kickedUsers.forEach { kicked ->
                val remainingMinutes = ((kicked.untilMillis - System.currentTimeMillis()) / 60_000L + 1).coerceAtLeast(0)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.online_kicked_user_remaining_format, kicked.displayName, remainingMinutes),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        TextButton(onClick = { onUnban(kicked.uid) }) {
                            Text(stringResource(R.string.online_unban_player))
                        }
                    }
                }
            }
        }
    }
}

/** One slot in the lobby's 2-column grid — a real player, or null for a still-empty seat. */
private data class PlayerSlotUiState(
    val uid: String,
    val name: String,
    val level: Int,
    val frame: AvatarFrame,
    val avatarUrl: String = "",
    val ready: Boolean,
    val isHost: Boolean = false,
    val isYou: Boolean,
    val pending: Boolean,
    val onKick: (() -> Unit)?,
    val teamId: String? = null,
    /** Non-null only on the current player's own slot, only in a team room — see WaitingRoomScreen's mySlot. */
    val onSwitchTeam: (() -> Unit)? = null
)

/** One 2v2 team's column: a header and its (padded-to-TEAM_SIZE) slots stacked vertically. */
@Composable
private fun TeamColumn(
    title: String,
    slots: List<PlayerSlotUiState?>,
    activeReactionsByUid: Map<String, Reaction>,
    onInvite: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.align(Alignment.CenterHorizontally).height(36.dp).aspectRatio(568f / 173f)) {
            Image(painter = painterResource(R.drawable.lobby_pill), contentDescription = null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
            Text(title, style = PaintedStyle(color = Color(0xFF5A3A1A), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold), maxLines = 1)
        }
        slots.forEach { slot ->
            PlayerSlotCell(
                slot = slot,
                activeReaction = slot?.let { activeReactionsByUid[it.uid] },
                onInvite = onInvite,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** A fixed row height shared by [PlayerSlotCard] and [EmptySlotCard] so occupied and empty seats line up in the grid. */
private val SLOT_HEIGHT = 64.dp

// bg_lobby is this size; its hanging sign is painted on it, so the title and the content start are placed by fractions.
private const val LOBBY_ART_W = 841f
private const val LOBBY_ART_H = 1870f

/**
 * One grid cell: [slot]'s card (or an empty placeholder). A player's own
 * [activeReaction] — if any — renders inside their own card, in the same
 * spot the ready/pending status normally sits (see [PlayerSlotCard]),
 * instead of a bubble growing out of the top of it: a bubble there pushed
 * every card below it down the grid whenever someone spoke, breaking the
 * fixed 2-column layout it sits in.
 */
@Composable
private fun PlayerSlotCell(
    slot: PlayerSlotUiState?,
    activeReaction: Reaction?,
    onInvite: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (slot == null) {
        EmptySlotCard(onInvite = onInvite, modifier = modifier)
    } else {
        PlayerSlotCard(slot = slot, activeReaction = activeReaction, modifier = modifier)
    }
}

@Composable
private fun PlayerSlotCard(slot: PlayerSlotUiState, activeReaction: Reaction?, modifier: Modifier = Modifier) {
    // Ready reads as the whole card turning a light "go" green instead of a
    // small checkmark next to the name — a glance at the grid says who's
    // ready without having to read every row.
    val readyTint = if (slot.ready) ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
        0.78f, 0f, 0f, 0f, 0f,
        0f, 0.98f, 0f, 0f, 0f,
        0f, 0f, 0.72f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    ))) else null
    NinePatch(
        res = R.drawable.lobby_card,
        slicePx = 90,
        edge = 22.dp,
        tint = readyTint,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth().heightIn(min = SLOT_HEIGHT)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // The level badge stands in for a profile picture — it's the
                // whole point of the ladder that opponents see it. The frame
                // is each player's own pick, synced onto the room alongside
                // their level (see OnlinePlayer.frameId), so everyone sees
                // what that player actually chose.
                LevelAvatar(
                    level = slot.level,
                    frame = slot.frame,
                    size = 38.dp,
                    photo = com.sualtikasifi.cizimhafiza.presentation.common.avatarPhotoOf(slot.avatarUrl)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (slot.isYou) stringResource(R.string.online_you_label, slot.name) else slot.name,
                            style = MaterialTheme.typography.labelLarge,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (slot.isHost) {
                            Spacer(modifier = Modifier.width(3.dp))
                            Image(painter = painterResource(R.drawable.lobby_crown), contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                    // Level and rank take turns under the name every five seconds, set in the same
                    // type as the league table's rows (see RankLevelLabel).
                    com.sualtikasifi.cizimhafiza.presentation.common.RankLevelLabel(level = slot.level, bullet = false)
                    // A chat message takes over this exact spot instead of
                    // opening a bubble above the card. A fixed-height Box
                    // around the Crossfade (rather than letting an empty
                    // state collapse to 0dp) means this line's own presence
                    // never changes the row's height — the card stays
                    // exactly SLOT_HEIGHT tall whether or not there's
                    // anything to show here right now.
                    Box(modifier = Modifier.height(16.dp)) {
                        Crossfade(targetState = activeReaction, label = "slot-status") { reaction ->
                            if (reaction != null) {
                                val phraseTextRes = presetPhraseTextRes(reaction.messageKey)
                                Text(
                                    text = phraseTextRes?.let { stringResource(it) } ?: reaction.emoji,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else if (slot.pending) {
                                Text(
                                    text = stringResource(R.string.online_pending_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
            if (slot.onKick != null) {
                IconButton(
                    onClick = slot.onKick,
                    modifier = Modifier.align(Alignment.TopEnd).size(26.dp)
                ) {
                    Icon(
                        Icons.Filled.PersonRemove,
                        contentDescription = stringResource(R.string.online_kick_player),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            // Never on the same slot as onKick above (that's always someone
            // ELSE's card, this is always your own) — same corner is fine.
            if (slot.onSwitchTeam != null) {
                IconButton(
                    onClick = slot.onSwitchTeam,
                    modifier = Modifier.align(Alignment.TopEnd).size(26.dp)
                ) {
                    Icon(
                        Icons.Filled.SwapHoriz,
                        contentDescription = stringResource(R.string.online_switch_team),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

/**
 * A still-unfilled lobby seat, and the way to fill it.
 *
 * It used to be a barely-there grey rectangle with a 40%-opacity silhouette
 * on it — so faint that a half-empty lobby read as a rendering glitch
 * rather than as seats waiting for people. It is now a dashed outline with
 * a legible "invite" affordance, and tapping it opens the friends list to
 * send an in-game invite: the seat itself is the most obvious place to ask
 * for someone to sit in it.
 */
@Composable
private fun EmptySlotCard(onInvite: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    NinePatch(
        res = R.drawable.lobby_slot,
        slicePx = 90,
        edge = 21.dp,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SLOT_HEIGHT)
            .then(if (onInvite != null) Modifier.clickable(onClick = onInvite) else Modifier)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.Center).padding(6.dp)) {
            Image(painter = painterResource(R.drawable.lobby_adduser), contentDescription = null, modifier = Modifier.size(26.dp))
            if (onInvite != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.online_invite_friend_slot),
                    style = PaintedStyle(color = Color(0xFF5A3A1A), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center),
                    maxLines = 2
                )
            }
        }
    }
}

/**
 * The friends list, opened by tapping an empty seat.
 *
 * Sends an invite into the room already open, unlike FriendsScreen's own
 * invite button which spins up a fresh one — asking from inside a lobby can
 * only sensibly mean "come to this one".
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun LobbyInviteSheet(
    friends: List<Friend>,
    sendingToUid: String?,
    sentToUids: Set<String>,
    message: String?,
    onInvite: (Friend) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(
                text = stringResource(R.string.online_invite_sheet_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            // The sheet is its own window with a scrim over the lobby, so the
            // result has to be said in here too or it is hidden while open.
            message?.let {
                TintedBadge(text = it, modifier = Modifier.padding(bottom = 10.dp))
            }
            if (friends.isEmpty()) {
                EmptyState(
                    emoji = "🤝",
                    message = stringResource(R.string.online_invite_sheet_empty),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 360.dp)
                ) {
                    items(friends, key = { it.uid }) { friend ->
                        RaisedCard(corner = 18.dp, modifier = Modifier.fillMaxWidth()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = friend.nickname,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                                if (sendingToUid == friend.uid) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else if (friend.uid in sentToUids) {
                                    Text(
                                        text = stringResource(R.string.online_invite_sent_short),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        color = AppTheme.tokens.success
                                    )
                                } else {
                                    SecondaryButton(
                                        text = stringResource(R.string.online_invite_send),
                                        onClick = { onInvite(friend) },
                                        height = 38.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
