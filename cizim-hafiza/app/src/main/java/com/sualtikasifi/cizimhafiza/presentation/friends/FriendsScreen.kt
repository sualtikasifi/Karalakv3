package com.sualtikasifi.cizimhafiza.presentation.friends

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.BlockedUser
import com.sualtikasifi.cizimhafiza.domain.model.Friend
import com.sualtikasifi.cizimhafiza.domain.model.FriendRequest
import com.sualtikasifi.cizimhafiza.presentation.common.ButtonOrange
import com.sualtikasifi.cizimhafiza.presentation.common.DescriptionInk
import com.sualtikasifi.cizimhafiza.presentation.common.InkBrown
import com.sualtikasifi.cizimhafiza.presentation.common.LetteredText
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedStyle
import com.sualtikasifi.cizimhafiza.util.InviteShareUtil
import com.sualtikasifi.cizimhafiza.util.UiText
import com.sualtikasifi.cizimhafiza.util.asString

// The scene art is 1080x2400; every position below is in pixels of that canvas (measured on the painted background).
private const val ArtW = 1080f
private const val ArtH = 2400f

/** Maps canvas pixels of the painted scene onto the screen (the art is cropped to fill, like every painted page). */
private class Scene(val s: Float, val offX: Float, val offY: Float, val density: androidx.compose.ui.unit.Density) {
    fun x(px: Float): Dp = with(density) { (offX + px * s).toDp() }
    fun y(px: Float): Dp = with(density) { (offY + px * s).toDp() }
    fun len(px: Float): Dp = with(density) { (px * s).toDp() }
    fun fs(px: Float) = len(px).value.sp
}

@Composable
private fun SceneBox(
    scene: Scene, x0: Float, y0: Float, x1: Float, y1: Float,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.offset(scene.x(x0), scene.y(y0)).size(scene.len(x1 - x0), scene.len(y1 - y0)),
        contentAlignment = contentAlignment,
        content = content
    )
}

@Composable
fun FriendsScreen(
    onNavigateToWaitingRoom: (roomCode: String) -> Unit,
    onBack: () -> Unit,
    onDuel: (opponentUid: String, opponentName: String) -> Unit,
    onDuelList: () -> Unit,
    viewModel: FriendsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val duelBadgeCount by viewModel.duelBadgeCount.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.navigateToWaitingRoomCode) {
        uiState.navigateToWaitingRoomCode?.let { roomCode ->
            onNavigateToWaitingRoom(roomCode)
            viewModel.onNavigatedToWaitingRoom()
        }
    }

    uiState.confirmRemove?.let { friend ->
        AlertDialog(
            onDismissRequest = viewModel::dismissRemoveConfirm,
            title = { Text(stringResource(R.string.friends_remove_confirm_title)) },
            text = { Text(stringResource(R.string.friends_remove_confirm_message, friend.nickname)) },
            confirmButton = {
                TextButton(onClick = { viewModel.removeFriend(friend) }) {
                    Text(stringResource(R.string.friends_remove_confirm_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissRemoveConfirm) {
                    Text(stringResource(R.string.friends_remove_confirm_cancel))
                }
            }
        )
    }

    uiState.confirmBlock?.let { friend ->
        AlertDialog(
            onDismissRequest = viewModel::dismissBlockConfirm,
            title = { Text(stringResource(R.string.friends_block_confirm_title)) },
            text = { Text(stringResource(R.string.friends_block_confirm_message, friend.nickname)) },
            confirmButton = {
                TextButton(onClick = { viewModel.blockFriend(friend) }) {
                    Text(stringResource(R.string.friends_block_confirm_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissBlockConfirm) {
                    Text(stringResource(R.string.friends_block_confirm_cancel))
                }
            }
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val s = maxOf(widthPx / ArtW, heightPx / ArtH)
        val scene = Scene(s, (widthPx - ArtW * s) / 2f, (heightPx - ArtH * s) / 2f, density)
        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current

        Image(
            painter = painterResource(R.drawable.bg_friends),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // The painted back and duel-list buttons are part of the art; these are the touch targets over them.
        val noRipple = remember { MutableInteractionSource() }
        SceneBox(scene, 40f, 250f, 180f, 400f) {
            Box(
                Modifier.fillMaxSize().clickable(interactionSource = noRipple, indication = null, onClick = onBack)
            )
        }
        SceneBox(scene, 900f, 250f, 1040f, 400f) {
            Box(
                Modifier.fillMaxSize().clickable(interactionSource = noRipple, indication = null, onClick = onDuelList)
            )
            if (duelBadgeCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .background(MaterialTheme.colorScheme.error, CircleShape)
                        .border(2.dp, Color.White, CircleShape)
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = duelBadgeCount.toString(),
                        style = PaintedStyle(color = Color.White, fontSize = 12.sp)
                    )
                }
            }
        }

        // Title on the hanging sign.
        SceneBox(scene, 290f, 370f, 800f, 500f) {
            LetteredText(stringResource(R.string.online_friends_entry), scene.fs(68f))
        }

        // ── Code card ──
        SceneBox(scene, 215f, 568f, 870f, 636f) {
            Text(
                stringResource(R.string.friends_my_code_label),
                style = PaintedStyle(color = InkBrown, fontSize = scene.fs(52f), textAlign = TextAlign.Center)
            )
        }
        SceneBox(scene, 215f, 640f, 870f, 708f) {
            Text(
                stringResource(R.string.friends_invite_reward_hint),
                style = PaintedStyle(
                    color = DescriptionInk, fontSize = scene.fs(32f), fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center, lineHeight = scene.fs(40f)
                )
            )
        }
        SceneBox(scene, 270f, 735f, 815f, 830f) {
            val code = uiState.myFriendCode
            if (code != null) {
                Text(
                    code,
                    style = PaintedStyle(
                        color = InkBrown, fontSize = scene.fs(82f), textAlign = TextAlign.Center,
                        letterSpacing = scene.fs(14f)
                    ),
                    maxLines = 1
                )
            } else {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), color = ButtonOrange, strokeWidth = 3.dp)
            }
        }
        SceneBox(scene, 330f, 862f, 750f, 962f) {
            val code = uiState.myFriendCode
            Box(
                Modifier.fillMaxSize().then(
                    if (code != null) Modifier.clickable(interactionSource = noRipple, indication = null) {
                        InviteShareUtil.shareFriendCode(context, code)
                    } else Modifier
                ),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(scene.len(14f))) {
                    Icon(Icons.Filled.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(scene.len(46f)))
                    LetteredText(stringResource(R.string.friends_share_code), scene.fs(42f))
                }
            }
        }

        // ── Add a friend ──
        SceneBox(scene, 150f, 1048f, 490f, 1128f) {
            LetteredText(stringResource(R.string.friends_add_friend_label), scene.fs(52f))
        }
        SceneBox(scene, 520f, 1052f, 1040f, 1124f) {
            FriendsMessage(uiState.infoMessage, uiState.errorMessage, scene)
        }
        SceneBox(scene, 112f, 1172f, 700f, 1244f, contentAlignment = Alignment.CenterStart) {
            BasicTextField(
                value = uiState.addFriendCodeInput,
                onValueChange = viewModel::setAddFriendCodeInput,
                singleLine = true,
                textStyle = PaintedStyle(
                    color = InkBrown, fontSize = scene.fs(44f), fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Start, letterSpacing = scene.fs(4f)
                ),
                cursorBrush = SolidColor(ButtonOrange),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth().padding(horizontal = scene.len(14f)),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (uiState.addFriendCodeInput.isEmpty()) {
                            Text(
                                stringResource(R.string.friends_add_friend_hint),
                                style = PaintedStyle(
                                    color = Color(0xFF9C8F82), fontSize = scene.fs(44f), fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                        inner()
                    }
                }
            )
        }
        val canAdd = uiState.addFriendCodeInput.length == 6
        SceneBox(scene, 775f, 1150f, 960f, 1266f) {
            if (uiState.isAddingFriend) {
                CircularProgressIndicator(modifier = Modifier.size(26.dp), color = Color.White, strokeWidth = 3.dp)
            } else {
                Box(
                    Modifier.fillMaxSize().then(
                        if (canAdd) Modifier.clickable(interactionSource = noRipple, indication = null) {
                            focusManager.clearFocus(force = true)
                            keyboardController?.hide()
                            viewModel.addFriend()
                        } else Modifier
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    LetteredText(
                        stringResource(R.string.friends_add_button), scene.fs(50f),
                        modifier = Modifier.alpha(if (canAdd) 1f else 0.6f)
                    )
                }
            }
        }

        // ── Friends list ──
        SceneBox(scene, 175f, 1318f, 520f, 1400f) {
            LetteredText(stringResource(R.string.friends_list_title), scene.fs(52f))
        }
        SceneBox(scene, 135f, 1432f, 950f, 1800f, contentAlignment = Alignment.TopCenter) {
            FriendsPanel(uiState, viewModel, scene, onDuel)
        }
    }
}

@Composable
private fun FriendsMessage(info: UiText?, error: UiText?, scene: Scene) {
    var last by remember { mutableStateOf<UiText?>(null) }
    LaunchedEffect(info) { if (info != null) last = info }
    if (error != null) {
        LetteredText(error.asString(), scene.fs(34f), outline = Color(0xFF7A1A10), maxLines = 2)
    } else {
        AnimatedVisibility(visible = info != null, exit = fadeOut(tween(800))) {
            LetteredText(last?.asString().orEmpty(), scene.fs(34f), maxLines = 2)
        }
    }
}

@Composable
private fun FriendsPanel(uiState: FriendsUiState, viewModel: FriendsViewModel, scene: Scene, onDuel: (String, String) -> Unit) {
    val nothing = uiState.friends.isEmpty() && uiState.friendRequests.isEmpty() && uiState.blockedUsers.isEmpty()
    if (nothing) {
        val text = stringResource(R.string.friends_empty)
        val cut = text.indexOf(". ").let { if (it >= 0) it + 1 else -1 }
        val first = if (cut > 0) text.substring(0, cut) else text
        val rest = if (cut > 0) text.substring(cut).trim() else ""
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(R.drawable.friends_handshake),
                contentDescription = null,
                modifier = Modifier.size(scene.len(190f))
            )
            Spacer(Modifier.height(scene.len(26f)))
            Text(first, style = PaintedStyle(color = InkBrown, fontSize = scene.fs(44f), textAlign = TextAlign.Center))
            if (rest.isNotEmpty()) {
                Text(
                    rest,
                    style = PaintedStyle(
                        color = DescriptionInk, fontSize = scene.fs(38f), fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center, lineHeight = scene.fs(46f)
                    )
                )
            }
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (uiState.friendRequests.isNotEmpty()) {
            item(key = "requests-title") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.friends_requests_title),
                        style = PaintedStyle(color = InkBrown, fontSize = 15.sp)
                    )
                    Box(
                        Modifier.background(ButtonOrange, CircleShape).padding(horizontal = 7.dp, vertical = 1.dp),
                        contentAlignment = Alignment.Center
                    ) { Text(uiState.friendRequests.size.toString(), style = PaintedStyle(color = Color.White, fontSize = 12.sp)) }
                }
            }
            items(uiState.friendRequests, key = { "request-" + it.uid }) { request ->
                FriendRequestRow(
                    request = request,
                    busy = uiState.answeringRequestUid == request.uid,
                    onAccept = { viewModel.acceptFriendRequest(request) },
                    onDecline = { viewModel.declineFriendRequest(request) }
                )
            }
        }
        items(uiState.friends, key = { it.uid }) { friend ->
            FriendRow(
                friend = friend,
                inviting = uiState.invitingFriendUid == friend.uid,
                busy = uiState.removingFriendUid == friend.uid || uiState.blockingFriendUid == friend.uid,
                onInvite = { viewModel.inviteFriend(friend) },
                onRemove = { viewModel.confirmRemoveFriend(friend) },
                onBlock = { viewModel.confirmBlockFriend(friend) },
                onDuel = { onDuel(friend.uid, friend.nickname) }
            )
        }
        if (uiState.blockedUsers.isNotEmpty()) {
            item(key = "blocked-title") {
                Text(
                    stringResource(R.string.friends_blocked_section_title),
                    style = PaintedStyle(color = InkBrown, fontSize = 15.sp),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            items(uiState.blockedUsers, key = { "blocked-" + it.uid }) { blocked ->
                BlockedUserRow(
                    blocked = blocked,
                    unblocking = uiState.unblockingUid == blocked.uid,
                    onUnblock = { viewModel.unblockUser(blocked) }
                )
            }
        }
    }
}

private val RowShape = RoundedCornerShape(14.dp)

@Composable
private fun ListRow(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(Color(0xFFFFF1D2), RowShape)
            .border(BorderStroke(1.5.dp, Color(0xFFE3A25C)), RowShape)
            .padding(start = 12.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        content = content
    )
}

@Composable
private fun RowName(name: String, modifier: Modifier) {
    Text(
        text = name,
        style = PaintedStyle(color = InkBrown, fontSize = 16.sp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
private fun OrangePill(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(30.dp)
            .background(Brush.verticalGradient(listOf(Color(0xFFFF9A3C), ButtonOrange)), RoundedCornerShape(15.dp))
            .border(BorderStroke(1.5.dp, Color(0xFFB04A0E)), RoundedCornerShape(15.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) { LetteredText(text, 13.sp) }
}

@Composable
private fun FriendRow(
    friend: Friend,
    inviting: Boolean,
    busy: Boolean,
    onInvite: () -> Unit,
    onRemove: () -> Unit,
    onBlock: () -> Unit,
    onDuel: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    ListRow {
        RowName(friend.nickname, Modifier.weight(1f))
        if (inviting || busy) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp).padding(end = 6.dp), color = ButtonOrange, strokeWidth = 2.5.dp)
        } else {
            OrangePill(stringResource(R.string.friends_invite_action), onInvite)
            Box {
                Box(
                    Modifier.size(34.dp).clickable { menuExpanded = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.friends_row_more_actions),
                        tint = InkBrown
                    )
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.duel_challenge_action)) },
                        leadingIcon = { Icon(Icons.Filled.SportsMma, contentDescription = null) },
                        onClick = { menuExpanded = false; onDuel() }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.friends_remove_action)) },
                        leadingIcon = { Icon(Icons.Filled.PersonRemove, contentDescription = null) },
                        onClick = { menuExpanded = false; onRemove() }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.friends_block_action)) },
                        leadingIcon = { Icon(Icons.Filled.Block, contentDescription = null) },
                        onClick = { menuExpanded = false; onBlock() }
                    )
                }
            }
        }
    }
}

/** One pending request; accepting is the highlighted answer, declining a quiet text action. */
@Composable
private fun FriendRequestRow(request: FriendRequest, busy: Boolean, onAccept: () -> Unit, onDecline: () -> Unit) {
    ListRow {
        RowName(request.nickname, Modifier.weight(1f))
        if (busy) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp).padding(end = 6.dp), color = ButtonOrange, strokeWidth = 2.5.dp)
        } else {
            Text(
                stringResource(R.string.friends_request_decline),
                style = PaintedStyle(color = DescriptionInk, fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                modifier = Modifier.clickable(onClick = onDecline).padding(horizontal = 6.dp, vertical = 8.dp)
            )
            OrangePill(stringResource(R.string.friends_request_accept), onAccept)
        }
    }
}

@Composable
private fun BlockedUserRow(blocked: BlockedUser, unblocking: Boolean, onUnblock: () -> Unit) {
    ListRow {
        RowName(blocked.nickname, Modifier.weight(1f))
        if (unblocking) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp).padding(end = 6.dp), color = ButtonOrange, strokeWidth = 2.5.dp)
        } else {
            OrangePill(stringResource(R.string.friends_unblock_action), onUnblock)
        }
    }
}
