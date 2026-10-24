package com.sualtikasifi.cizimhafiza.presentation.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.presentation.mainmenu.rememberSink
import com.sualtikasifi.cizimhafiza.presentation.mainmenu.sinkWith
import com.sualtikasifi.cizimhafiza.presentation.mainmenu.sunkenArt
import com.sualtikasifi.cizimhafiza.presentation.theme.DisplayFont

// penalty_card is 1064 x 1383; the rectangles are x0, y0, x1, y1 in the picture's own pixels.
private const val ArtW = 1064f
private const val ArtH = 1383f
// The parchment runs x 120-960, y 420-1040; the pencil's shards fall into its top middle, so the title starts below them.
private val TitleRect = floatArrayOf(170f, 495f, 910f, 605f)
private val MessageRect = floatArrayOf(170f, 615f, 910f, 900f)
private val WarningRect = floatArrayOf(170f, 910f, 910f, 1020f)
private val ButtonRect = floatArrayOf(205f, 1095f, 860f, 1270f)

private val PenaltyInk = Color(0xFF3A2416)
private val PenaltySoftInk = Color(0xFF6B4122)
private val PenaltyWarning = Color(0xFFC0392B)
private val PenaltyButtonOutline = Color(0xFF8A3A00)

/**
 * The "your round was rejected" window: the angry dog snapping a pencil over a wooden board, with one painted button.
 * The picture carries all of that; the title, what was taken, the warning and the button word are written over it,
 * so they follow the language. Every way out of it ([onDismiss]) is the acknowledgement that applies the penalty.
 */
@Composable
fun PaintedPenaltyDialog(
    title: String,
    message: String,
    warning: String,
    buttonText: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val frameW = minOf(maxWidth - 24.dp, (maxHeight - 24.dp) * (ArtW / ArtH), 440.dp)
            val u = frameW / ArtW
            val fontScale0 = LocalDensity.current.fontScale
            val fs: (Float) -> TextUnit = { px -> (px * u.value / fontScale0).sp }
            val context = LocalContext.current
            val sheet = remember { runCatching { BackdropCache.get(context.resources, R.drawable.penalty_card) }.getOrNull() }
            val pxK = (sheet?.width ?: ArtW.toInt()) / ArtW

            fun Modifier.at(r: FloatArray) = offset(u * r[0], u * r[1]).size(u * (r[2] - r[0]), u * (r[3] - r[1]))

            val enter = remember { Animatable(0f) }
            LaunchedEffect(Unit) { enter.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 380f)) }
            Box(
                Modifier
                    .size(frameW, frameW * (ArtH / ArtW))
                    .graphicsLayer {
                        val e = enter.value
                        scaleX = 0.82f + 0.18f * e
                        scaleY = 0.82f + 0.18f * e
                        alpha = (e * 3f).coerceIn(0f, 1f)
                    }
            ) {
                Image(
                    painter = painterResource(R.drawable.penalty_card),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )
                FitText(
                    text = title,
                    style = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = fs(72f), color = PenaltyInk, textAlign = TextAlign.Center),
                    maxLines = 2, minScale = 0.5f,
                    modifier = Modifier.at(TitleRect)
                )
                FitText(
                    text = message,
                    style = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = fs(44f), lineHeight = fs(56f), color = PenaltySoftInk, textAlign = TextAlign.Center),
                    maxLines = 6, minScale = 0.6f,
                    modifier = Modifier.at(MessageRect)
                )
                FitText(
                    text = warning,
                    style = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = fs(44f), lineHeight = fs(54f), color = PenaltyWarning, textAlign = TextAlign.Center),
                    maxLines = 2, minScale = 0.6f,
                    modifier = Modifier.at(WarningRect)
                )
                val (x0, y0, x1, y1) = ButtonRect
                val sink = rememberSink(0.94f)
                Box(
                    modifier = Modifier
                        .at(ButtonRect)
                        .sunkenArt(sink, sheet, x0 * pxK, y0 * pxK, (x1 - x0) * pxK, (y1 - y0) * pxK, u * ((y1 - y0) / 2f))
                        .clickable(interactionSource = sink.source, indication = null, onClick = onDismiss)
                ) {
                    Box(Modifier.fillMaxSize().sinkWith(sink, 0.5f, 0.5f), contentAlignment = Alignment.Center) {
                        LetteredText(text = buttonText, size = fs(72f), outline = PenaltyButtonOutline, maxLines = 1, modifier = Modifier.fillMaxWidth().padding(horizontal = u * 40f))
                    }
                }
            }
        }
    }
}
