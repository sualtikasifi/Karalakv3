package com.sualtikasifi.cizimhafiza.presentation.online

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateFloatAsState
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.presentation.common.ScreenTopActions

// bg_race is this size; every overlay below is placed by fractions of the picture, so it stays on its sign,
// paper and floor however the window crops it.
private const val ArtW = 841f
private const val ArtH = 1870f

/**
 * Playing WITH somebody you know: open a room, or join theirs. Drawn over one painted scene: the title goes on
 * its sign, the line under it on the paper scroll, the two buttons on the floor.
 */
@Composable
fun OnlineLobbyScreen(
    onBack: () -> Unit,
    onCreateRoom: () -> Unit,
    onJoinRoom: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        // The picture fills the window (Crop): one scale, centred.
        val s = maxOf(widthPx / ArtW, heightPx / ArtH)
        val offX = (widthPx - ArtW * s) / 2f
        val offY = (heightPx - ArtH * s) / 2f
        fun x(fraction: Float): Dp = with(density) { (offX + ArtW * s * fraction).toDp() }
        fun y(fraction: Float): Dp = with(density) { (offY + ArtH * s * fraction).toDp() }
        fun len(artPx: Float): Dp = with(density) { (artPx * s).toDp() }

        Image(
            painter = painterResource(R.drawable.bg_race),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Title on the wooden sign.
        val titleSize = with(density) { (52f * s).toSp() }
        Box(
            modifier = Modifier.offset(x = x(0.5f) - len(330f), y = y(0.478f) - len(40f)).width(len(660f)),
            contentAlignment = Alignment.Center
        ) {
            val base = TextStyle(fontSize = titleSize, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Text(
                text = stringResource(R.string.online_lobby_title),
                style = base.copy(color = Color(0xFF5A2E0C), drawStyle = Stroke(width = with(density) { (9f * s) }, join = androidx.compose.ui.graphics.StrokeJoin.Round)),
                maxLines = 1
            )
            Text(text = stringResource(R.string.online_lobby_title), style = base.copy(color = Color.White), maxLines = 1)
        }

        // Line on the paper scroll.
        Box(
            modifier = Modifier.offset(x = x(0.5f) - len(300f), y = y(0.558f) - len(62f)).width(len(600f)).size(width = len(600f), height = len(124f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.online_lobby_subtitle),
                style = TextStyle(
                    color = Color(0xFF5A3A1A),
                    fontSize = with(density) { (27f * s).toSp() },
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = with(density) { (34f * s).toSp() }
                )
            )
        }

        RaceButton(
            image = R.drawable.race_btn_teal,
            text = stringResource(R.string.online_create_room),
            icon = Icons.Filled.Add,
            textColor = Color.White,
            outline = Color(0xFF0B4F5C),
            onClick = onCreateRoom,
            modifier = Modifier.offset(x = x(0.5f) - len(290f), y = y(0.668f) - len(74f)).width(len(580f))
        )
        RaceButton(
            image = R.drawable.race_btn_white,
            text = stringResource(R.string.online_join_room),
            icon = Icons.AutoMirrored.Filled.Login,
            textColor = Color(0xFFF26A0F),
            outline = null,
            onClick = onJoinRoom,
            modifier = Modifier.offset(x = x(0.5f) - len(290f), y = y(0.748f) - len(74f)).width(len(580f))
        )

        ScreenTopActions(onBack = onBack, modifier = Modifier.align(Alignment.TopStart))
    }
}

/** A painted button with its label on top; it dips a little while pressed. */
@Composable
private fun RaceButton(
    image: Int,
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    textColor: Color,
    outline: Color?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, label = "race-btn")
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        Image(painter = painterResource(image), contentDescription = null, contentScale = ContentScale.FillWidth, modifier = Modifier.fillMaxWidth())
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(30.dp))
            Box(modifier = Modifier.width(10.dp))
            Box(contentAlignment = Alignment.Center) {
                val style = TextStyle(fontSize = androidx.compose.ui.unit.TextUnit(24f, androidx.compose.ui.unit.TextUnitType.Sp), fontWeight = FontWeight.ExtraBold)
                if (outline != null) {
                    Text(text, style = style.copy(color = outline, drawStyle = Stroke(width = 7f, join = androidx.compose.ui.graphics.StrokeJoin.Round)), maxLines = 1)
                }
                Text(text, style = style.copy(color = textColor), maxLines = 1)
            }
        }
    }
}

private fun Modifier.fillMaxWidth(): Modifier = this.then(Modifier.fillMaxSize())
