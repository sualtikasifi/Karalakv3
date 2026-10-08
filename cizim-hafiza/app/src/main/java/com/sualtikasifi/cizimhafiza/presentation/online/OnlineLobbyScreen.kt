package com.sualtikasifi.cizimhafiza.presentation.online

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.presentation.common.DescriptionStyle
import com.sualtikasifi.cizimhafiza.presentation.common.LetteredText
import com.sualtikasifi.cizimhafiza.presentation.common.ScreenTopActions
import com.sualtikasifi.cizimhafiza.presentation.common.breathing
import com.sualtikasifi.cizimhafiza.presentation.common.cachedPainterResource
import com.sualtikasifi.cizimhafiza.presentation.common.springIn

// The layout is drawn on a canvas of this size; every piece is placed in these units and scaled with the window, so
// the sign, the paper and the buttons keep their places however the room picture is cropped.
private const val ArtW = 841f
private const val ArtH = 1870f

// Where each piece sits on that canvas (top edge and width; the height follows each picture's own proportions).
private const val SignTop = 300f
private const val SignW = 700f
private const val PaperTop = 884f
private const val PaperW = 680f
private const val Btn1Top = 1236f
private const val Btn2Top = 1424f
private const val BtnW = 640f

/**
 * Playing WITH somebody you know: open a room, or join theirs. The room picture is the backdrop; the two dogs on
 * their blank sign, the paper scroll and the two buttons are separate pictures laid over it, and every word is
 * written live on top (so each language gets its own).
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
        // One scale for both directions, centred; the backdrop crops to fill, the pieces stay in proportion.
        val s = maxOf(widthPx / ArtW, heightPx / ArtH)
        val offX = (widthPx - ArtW * s) / 2f
        fun x(art: Float): Dp = with(density) { (offX + art * s).toDp() }
        fun y(art: Float): Dp = with(density) { (art * s).toDp() }
        fun len(art: Float): Dp = with(density) { (art * s).toDp() }
        fun fs(art: Float): TextUnit = with(density) { (art * s).toSp() }

        Image(
            painter = cachedPainterResource(R.drawable.bg_race2),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxSize()
        )

        // The dogs and their sign; the title goes on the plank (lower half of the picture).
        val signH = SignW * 399f / 472f
        Image(
            painter = painterResource(R.drawable.race2_sign),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .offset(x(ArtW / 2 - SignW / 2), y(SignTop))
                .size(len(SignW), len(signH))
                .springIn(index = 0, stepMs = 100, fromY = 40)
        )
        LetteredText(
            text = stringResource(R.string.online_lobby_title),
            size = fs(70f),
            outline = Color(0xFF5A2815),
            maxLines = 2,
            modifier = Modifier
                .offset(x(ArtW / 2 - 290f), y(SignTop + signH * 0.54f))
                .size(len(580f), len(signH * 0.38f))
        )

        // The line on the paper scroll.
        val paperH = PaperW * 219f / 459f
        Image(
            painter = painterResource(R.drawable.race2_paper),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .offset(x(ArtW / 2 - PaperW / 2), y(PaperTop))
                .size(len(PaperW), len(paperH))
                .springIn(index = 1, stepMs = 100, fromY = 40)
        )
        Box(
            modifier = Modifier
                .offset(x(ArtW / 2 - 270f), y(PaperTop + paperH * 0.25f))
                .size(len(540f), len(paperH * 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            // Four short lines, broken where the sentence breathes, all at one size.
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                listOf(R.string.online_lobby_line1, R.string.online_lobby_line2, R.string.online_lobby_line3, R.string.online_lobby_line4).forEach { line ->
                    com.sualtikasifi.cizimhafiza.presentation.common.FitText(
                        text = stringResource(line),
                        style = androidx.compose.ui.text.TextStyle(
                            fontFamily = com.sualtikasifi.cizimhafiza.presentation.theme.DisplayFont,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            fontSize = fs(40f),
                            lineHeight = fs(46f),
                            color = Color(0xFF4A2814),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        ),
                        maxLines = 1,
                        minScale = 0.6f,
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    )
                }
            }
        }

        RaceButton(
            image = R.drawable.race2_btn_orange,
            text = stringResource(R.string.online_create_room),
            outline = Color(0xFF8A3A00),
            textSize = fs(52f),
            onClick = onCreateRoom,
            modifier = Modifier
                .offset(x(ArtW / 2 - BtnW / 2), y(Btn1Top))
                .size(len(BtnW), len(BtnW * 108f / 449f))
                .springIn(index = 2, stepMs = 100, fromY = 60),
            attention = true
        )
        RaceButton(
            image = R.drawable.race2_btn_blue,
            text = stringResource(R.string.online_join_room),
            outline = Color(0xFF0B3F73),
            textSize = fs(52f),
            onClick = onJoinRoom,
            modifier = Modifier
                .offset(x(ArtW / 2 - BtnW / 2), y(Btn2Top))
                .size(len(BtnW), len(BtnW * 113f / 450f))
                .springIn(index = 3, stepMs = 100, fromY = 60)
        )

        ScreenTopActions(onBack = onBack, modifier = Modifier.align(Alignment.TopStart))
    }
}

/** A painted button with its label on top; it dips a little while pressed (the whole picture, leaves included). */
@Composable
private fun RaceButton(
    image: Int,
    text: String,
    outline: Color,
    textSize: TextUnit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** The main action of the screen: it breathes. */
    attention: Boolean = false
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.93f else 1f, animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.5f, stiffness = 650f), label = "race-btn")
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .then(if (attention) Modifier.breathing(0.02f, 1500) else Modifier)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        Image(painter = painterResource(image), contentDescription = null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
        LetteredText(text, textSize, outline = outline, maxLines = 1, modifier = Modifier.fillMaxSize())
    }
}
