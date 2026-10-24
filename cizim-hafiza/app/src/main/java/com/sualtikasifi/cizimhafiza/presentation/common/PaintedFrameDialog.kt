package com.sualtikasifi.cizimhafiza.presentation.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
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

// The picture's own pixels (966 x 1595): parchment text area, and the painted orange button under it.
private const val FrameW = 966f
private const val FrameH = 1595f
private val TitleRect = floatArrayOf(150f, 205f, 820f, 305f)
private val BodyRect = floatArrayOf(140f, 315f, 830f, 1290f)
private val ButtonRect = floatArrayOf(178f, 1378f, 795f, 1516f)

/**
 * A window in the painted vine-and-parchment frame that has its button built in: a title and a scrolling body are
 * written on the parchment (so they follow the language) and [buttonText] on the painted button, which caves in
 * under the finger.
 */
@Composable
fun PaintedFrameDialog(
    title: String,
    body: String,
    buttonText: String,
    onClick: () -> Unit,
    onDismiss: () -> Unit = onClick
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            val frameW = minOf(maxWidth - 24.dp, 460.dp, (maxHeight - 48.dp) * (FrameW / FrameH))
            val u = frameW / FrameW
            val fontScale0 = LocalDensity.current.fontScale
            val fs: (Float) -> TextUnit = { px -> (px * u.value / fontScale0).sp }
            val context = LocalContext.current
            val sheet = remember { runCatching { BackdropCache.get(context.resources, R.drawable.dialog_frame_btn) }.getOrNull() }
            val pxK = (sheet?.width ?: FrameW.toInt()) / FrameW
            val sink = rememberSink(0.93f)
            val enter = remember { Animatable(0f) }
            LaunchedEffect(Unit) { enter.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 380f)) }
            Box(
                Modifier
                    .size(frameW, frameW * (FrameH / FrameW))
                    .graphicsLayer {
                        val e = enter.value
                        scaleX = 0.85f + 0.15f * e
                        scaleY = 0.85f + 0.15f * e
                        alpha = (e * 3f).coerceIn(0f, 1f)
                    }
                    // A tap on the window itself is not a tap outside it.
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
            ) {
                Image(
                    painter = painterResource(R.drawable.dialog_frame_btn),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )
                FitText(
                    text = title,
                    style = PaintedStyle(color = Color(0xFF3A2416), fontSize = fs(66f), textAlign = TextAlign.Center),
                    maxLines = 1,
                    minScale = 0.6f,
                    modifier = Modifier.offset(u * TitleRect[0], u * TitleRect[1]).size(u * (TitleRect[2] - TitleRect[0]), u * (TitleRect[3] - TitleRect[1]))
                )
                Column(
                    modifier = Modifier
                        .offset(u * BodyRect[0], u * BodyRect[1])
                        .size(u * (BodyRect[2] - BodyRect[0]), u * (BodyRect[3] - BodyRect[1]))
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = body,
                        style = DescriptionStyle(fs(44f), fs(60f)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = u * 6f)
                    )
                }
                val (x0, y0, x1, y1) = ButtonRect
                Box(
                    modifier = Modifier
                        .offset(u * x0, u * y0)
                        .size(u * (x1 - x0), u * (y1 - y0))
                        .sunkenArt(sink, sheet, x0 * pxK, y0 * pxK, (x1 - x0) * pxK, (y1 - y0) * pxK, u * ((y1 - y0) / 2f))
                        .clickable(interactionSource = sink.source, indication = null, onClick = onClick)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(horizontal = u * 40f).sinkWith(sink, 0.5f, 0.5f),
                        contentAlignment = Alignment.Center
                    ) {
                        LetteredText(text = buttonText, size = fs(60f), outline = Color(0xFF8A3A00), maxLines = 1)
                    }
                }
            }
        }
    }
}
