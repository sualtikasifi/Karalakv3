package com.sualtikasifi.cizimhafiza.presentation.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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

// streak_rescue_card is 1032 x 1336; the rectangles are x0, y0, x1, y1 in the picture's own pixels.
private const val ArtW = 1032f
private const val ArtH = 1336f
private val TitleRect = floatArrayOf(240f, 338f, 810f, 490f)
private val MessageRect = floatArrayOf(236f, 782f, 796f, 912f)
private val AdButtonRect = floatArrayOf(114f, 938f, 919f, 1078f)
// Where the ad label sits inside the button: right of the painted play badge (x only; it fills the button's height).
private const val AdLabelX0 = 300f
private const val AdLabelX1 = 880f
private val DismissButtonRect = floatArrayOf(139f, 1098f, 894f, 1198f)

private val RescueInk = Color(0xFF3A2416)
private val RescueSoftInk = Color(0xFF6B4122)
private val RescueTitleOutline = Color(0xFF6A2E08)
private val RescueAdOutline = Color(0xFF8A3A00)

/**
 * The "your streak is about to break" window: the crowned dog peeking over a wooden board with a pencil and a paint
 * pot, the broken flame, and two painted buttons. The picture carries all of that; the title, the line under the
 * flame and both button words are written over it (so they follow the language), and each button caves in under
 * the finger like the game's other painted buttons. [onRescue] opens a rewarded ad — wrap it with adOnly().
 */
@Composable
fun PaintedStreakRescueDialog(
    title: String,
    message: String,
    rescueText: String,
    dismissText: String,
    onRescue: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            val frameW = minOf(maxWidth - 24.dp, (maxHeight - 24.dp) * (ArtW / ArtH), 440.dp)
            val u = frameW / ArtW
            val fontScale0 = LocalDensity.current.fontScale
            val fs: (Float) -> TextUnit = { px -> (px * u.value / fontScale0).sp }
            val context = LocalContext.current
            val sheet = remember { runCatching { BackdropCache.get(context.resources, R.drawable.streak_rescue_card) }.getOrNull() }
            val pxK = (sheet?.width ?: ArtW.toInt()) / ArtW

            @Composable
            fun PaintedButton(r: FloatArray, onClick: () -> Unit, label: @Composable BoxScope.() -> Unit) {
                val (x0, y0, x1, y1) = r
                val sink = rememberSink(0.94f)
                Box(
                    modifier = Modifier
                        .offset(u * x0, u * y0)
                        .size(u * (x1 - x0), u * (y1 - y0))
                        .sunkenArt(sink, sheet, x0 * pxK, y0 * pxK, (x1 - x0) * pxK, (y1 - y0) * pxK, u * ((y1 - y0) / 2f))
                        .clickable(interactionSource = sink.source, indication = null, onClick = onClick)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().sinkWith(sink, 0.5f, 0.5f),
                        contentAlignment = Alignment.CenterStart,
                        content = label
                    )
                }
            }

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
                    // A tap on the card itself must not count as a tap outside.
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            ) {
                Image(
                    painter = painterResource(R.drawable.streak_rescue_card),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )
                LetteredText(
                    text = title,
                    size = fs(84f),
                    outline = RescueTitleOutline,
                    maxLines = 2,
                    minScale = 0.5f,
                    modifier = Modifier
                        .offset(u * TitleRect[0], u * TitleRect[1])
                        .size(u * (TitleRect[2] - TitleRect[0]), u * (TitleRect[3] - TitleRect[1]))
                )
                FitText(
                    text = message,
                    style = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = fs(50f), color = RescueSoftInk, textAlign = TextAlign.Center),
                    maxLines = 3, minScale = 0.5f,
                    modifier = Modifier
                        .offset(u * MessageRect[0], u * MessageRect[1])
                        .size(u * (MessageRect[2] - MessageRect[0]), u * (MessageRect[3] - MessageRect[1]))
                )
                PaintedButton(AdButtonRect, onRescue) {
                    // Right of the painted play badge.
                    Box(
                        Modifier.padding(start = u * (AdLabelX0 - AdButtonRect[0])).size(u * (AdLabelX1 - AdLabelX0), u * (AdButtonRect[3] - AdButtonRect[1])),
                        contentAlignment = Alignment.Center
                    ) {
                        LetteredText(text = rescueText, size = fs(66f), outline = RescueAdOutline, maxLines = 1, modifier = Modifier.fillMaxWidth())
                    }
                }
                PaintedButton(DismissButtonRect, onDismiss) {
                    FitText(
                        text = dismissText,
                        style = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = fs(60f), color = RescueInk, textAlign = TextAlign.Center),
                        maxLines = 1, minScale = 0.55f,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = u * 40f)
                    )
                }
            }
        }
    }
}
