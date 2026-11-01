package com.sualtikasifi.cizimhafiza.presentation.online

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.Friend
import com.sualtikasifi.cizimhafiza.presentation.common.DescriptionStyle
import com.sualtikasifi.cizimhafiza.presentation.common.LetteredText
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedPillButton
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedStyle
import com.sualtikasifi.cizimhafiza.presentation.common.a11yButton
import com.sualtikasifi.cizimhafiza.presentation.common.springIn

// The window is one picture (invite_window, 1189 x 1323): the two friends over a pink ribbon, a close button on its
// right end, a cream page between the paint splashes. All positions are in the picture's own pixels.
private const val ArtW = 1189f
private const val ArtH = 1323f
private val RibbonRect = floatArrayOf(295f, 208.5f, 895f, 316.5f)
private val CloseRect = floatArrayOf(1030f, 195f, 1170f, 335f)
private val PageRect = floatArrayOf(95f, 450f, 1095f, 1120f)
private val Ink = Color(0xFF3A2416)
private val Good = Color(0xFF1E8E3E)

/**
 * The friends list, opened by tapping an empty seat in the lobby: a painted window over the lobby with the friends as
 * rows, each with a coloured initial, the name and a "Davet Et" button that turns into a spinner while it is sent and
 * into "Gönderildi" once it has gone.
 */
@Composable
internal fun LobbyInviteWindow(
    friends: List<Friend>,
    sendingToUid: String?,
    sentToUids: Set<String>,
    message: String?,
    onInvite: (Friend) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            val frameW = minOf(maxWidth - 12.dp, 480.dp, (maxHeight - 40.dp) * (ArtW / ArtH))
            val u = frameW / ArtW
            val fontScale0 = LocalDensity.current.fontScale
            val fs: (Float) -> TextUnit = { px -> (px * u.value / fontScale0).sp }
            fun Modifier.at(r: FloatArray) = offset(u * r[0], u * r[1]).size(u * (r[2] - r[0]), u * (r[3] - r[1]))
            Box(
                Modifier
                    .size(frameW, frameW * (ArtH / ArtW))
                    .springIn(fromY = 44, stepMs = 0)
                    // A tap on the window itself is not a tap outside it.
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
            ) {
                Image(painterResource(R.drawable.invite_window), contentDescription = null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
                Box(Modifier.at(RibbonRect), contentAlignment = Alignment.Center) {
                    LetteredText(
                        text = stringResource(R.string.online_invite_sheet_title),
                        size = fs(64f),
                        outline = Color(0xFF5B1247),
                        maxLines = 2,
                        minScale = 0.5f,
                        modifier = Modifier.fillMaxWidth().offset(y = (fs(64f).value * 0.11f).dp)
                    )
                }
                // the close button is part of the picture; this is its touch area
                Box(Modifier.at(CloseRect).clip(CircleShape).clickable(onClick = onDismiss).a11yButton(stringResource(R.string.close)))
                Column(Modifier.at(PageRect)) {
                    message?.let {
                        Text(
                            text = it,
                            style = DescriptionStyle(fs(40f), fs(50f)).copy(textAlign = TextAlign.Center, color = Good),
                            modifier = Modifier.fillMaxWidth().padding(bottom = u * 10f)
                        )
                    }
                    if (friends.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = u * 40f)) {
                                Text("🤝", fontSize = fs(130f))
                                Spacer(Modifier.height(u * 14f))
                                Text(
                                    text = stringResource(R.string.online_invite_sheet_empty),
                                    style = DescriptionStyle(fs(44f), fs(58f)).copy(textAlign = TextAlign.Center),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(u * 16f),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(friends, key = { it.uid }) { friend ->
                                FriendRow(friend, u, fs, sending = sendingToUid == friend.uid, sent = friend.uid in sentToUids) { onInvite(friend) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendRow(friend: Friend, u: androidx.compose.ui.unit.Dp, fs: (Float) -> TextUnit, sending: Boolean, sent: Boolean, onInvite: () -> Unit) {
    val tints = listOf(0xFFEB6834, 0xFF2A78D6, 0xFF7457D9, 0xFF138A7E, 0xFFD43A8E)
    val tint = Color(tints[(friend.nickname.hashCode() and 0x7fffffff) % tints.size])
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(u * 40f))
            .background(Color.White.copy(alpha = 0.78f))
            .padding(start = u * 18f, end = u * 22f, top = u * 14f, bottom = u * 14f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(u * 96f).clip(CircleShape).background(tint), contentAlignment = Alignment.Center) {
            Text(
                friend.nickname.trim().take(1).uppercase().ifBlank { "?" },
                style = PaintedStyle(color = Color.White, fontSize = fs(54f), textAlign = TextAlign.Center)
            )
        }
        Spacer(Modifier.width(u * 22f))
        Text(
            text = friend.nickname,
            style = PaintedStyle(color = Ink, fontSize = fs(52f), textAlign = TextAlign.Start),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(u * 14f))
        Box(Modifier.width(u * 300f), contentAlignment = Alignment.Center) {
            when {
                sending -> CircularProgressIndicator(modifier = Modifier.size(u * 64f), strokeWidth = 2.5.dp, color = Color(0xFFEB6834))
                sent -> Text(
                    text = stringResource(R.string.online_invite_sent_short),
                    style = PaintedStyle(color = Good, fontSize = fs(34f), textAlign = TextAlign.Center, lineHeight = fs(38f)),
                    maxLines = 2
                )
                else -> PaintedPillButton(
                    text = stringResource(R.string.online_invite_send),
                    onClick = onInvite,
                    height = u * 96f,
                    textSize = fs(44f),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
