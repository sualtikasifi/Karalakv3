package com.sualtikasifi.cizimhafiza.presentation.tutorial

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.presentation.common.BackdropCache
import com.sualtikasifi.cizimhafiza.presentation.common.FitText
import com.sualtikasifi.cizimhafiza.presentation.common.LetteredText
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedStyle
import com.sualtikasifi.cizimhafiza.presentation.common.a11yButton
import com.sualtikasifi.cizimhafiza.presentation.common.cachedPainterResource
import com.sualtikasifi.cizimhafiza.presentation.common.springIn
import com.sualtikasifi.cizimhafiza.presentation.mainmenu.SinkState
import com.sualtikasifi.cizimhafiza.presentation.mainmenu.rememberSink
import com.sualtikasifi.cizimhafiza.presentation.mainmenu.sinkWith
import com.sualtikasifi.cizimhafiza.presentation.mainmenu.sunkenArt

// bg_tutorial_welcome is this size: the workshop, the big parchment card with the round logo on top of it and the two
// buttons painted in; the words on the card and on the buttons are lettered live over it.
private const val ArtW = 841f
private const val ArtH = 1870f
private val Ink = Color(0xFF3B2314)

/**
 * The first screen of the tutorial: "Karalak'a hoş geldin!". The painted workshop fills the screen from the very first
 * frame (this used to open on a bare background), with the welcome card, the logo and both buttons already in it.
 */
@Composable
internal fun WelcomeScene(onStart: () -> Unit, onSkip: () -> Unit) {
    val context = LocalContext.current
    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color(0xFF5A3A1C))) {
        val unit = minOf(maxWidth / ArtW, maxHeight / ArtH)
        val offX = (maxWidth - unit * ArtW) / 2
        val offY = (maxHeight - unit * ArtH) / 2
        val us = unit.value
        val fontScale0 = LocalDensity.current.fontScale
        fun fs(art: Float) = (art * us / fontScale0).sp
        fun box(x0: Float, y0: Float, x1: Float, y1: Float): Modifier =
            Modifier.offset(offX + unit * x0, offY + unit * y0).requiredSize(unit * (x1 - x0), unit * (y1 - y0))

        val scene = cachedPainterResource(R.drawable.bg_tutorial_welcome)
        val sceneBitmap = remember {
            runCatching { BackdropCache.get(context.resources, R.drawable.bg_tutorial_welcome) }.getOrNull()
        }
        val pxX = (sceneBitmap?.width ?: 1) / ArtW
        val pxY = (sceneBitmap?.height ?: 1) / ArtH
        fun sunk(sink: SinkState, x0: Float, y0: Float, x1: Float, y1: Float, cornerArt: Float): Modifier =
            Modifier.sunkenArt(sink, sceneBitmap, x0 * pxX, y0 * pxY, (x1 - x0) * pxX, (y1 - y0) * pxY, (cornerArt * us).dp)

        if (offX > 1.dp || offY > 1.dp) {
            Image(scene, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().blur(20.dp))
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.18f)))
        }
        Image(scene, contentDescription = null, contentScale = ContentScale.FillBounds, modifier = box(0f, 0f, ArtW, ArtH))

        // Title and the three short lines under the orange scribble.
        FitText(
            text = stringResource(R.string.tutorial_intro_title),
            style = PaintedStyle(color = Ink, fontSize = fs(56f), textAlign = TextAlign.Center),
            maxLines = 1,
            minScale = 0.55f,
            modifier = box(126f, 830f, 716f, 898f).springIn(index = 0, stepMs = 0, fromY = 20)
        )
        FitText(
            text = stringResource(R.string.tutorial_intro_body),
            style = PaintedStyle(color = Ink, fontSize = fs(36f), fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, lineHeight = fs(46f)),
            maxLines = 6,
            minScale = 0.55f,
            modifier = box(104f, 934f, 738f, 1130f).springIn(index = 1, stepMs = 70, fromY = 20)
        )

        // "Hadi başlayalım": the orange button.
        val startSink = rememberSink(0.94f)
        Box(
            box(125f, 1170f, 700f, 1306f)
                .then(sunk(startSink, 125f, 1170f, 700f, 1306f, 66f))
                .clickable(interactionSource = startSink.source, indication = null, onClick = onStart)
                .a11yButton(stringResource(R.string.tutorial_intro_button))
        ) {
            Box(Modifier.fillMaxSize().sinkWith(startSink, 0.5f, 0.5f), contentAlignment = Alignment.Center) {
                LetteredText(
                    text = stringResource(R.string.tutorial_intro_button),
                    size = fs(60f),
                    outline = Color(0xFF8A3A00),
                    modifier = Modifier.requiredSize(unit * 390f, unit * 82f),
                    minScale = 0.5f
                )
            }
        }

        // "Eğitimi atla": the quiet one.
        val skipSink = rememberSink(0.95f)
        Box(
            box(200f, 1334f, 642f, 1424f)
                .then(sunk(skipSink, 200f, 1334f, 642f, 1424f, 45f))
                .clickable(interactionSource = skipSink.source, indication = null, onClick = onSkip)
                .a11yButton(stringResource(R.string.tutorial_skip))
        ) {
            Box(Modifier.fillMaxSize().sinkWith(skipSink, 0.5f, 0.5f), contentAlignment = Alignment.Center) {
                FitText(
                    text = stringResource(R.string.tutorial_skip),
                    style = PaintedStyle(color = Color(0xFF5A4636), fontSize = fs(34f), textAlign = TextAlign.Center),
                    maxLines = 1,
                    minScale = 0.55f,
                    modifier = Modifier.requiredSize(unit * 300f, unit * 50f)
                )
            }
        }
    }
}
