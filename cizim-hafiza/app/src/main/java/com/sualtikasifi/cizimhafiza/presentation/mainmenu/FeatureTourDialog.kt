package com.sualtikasifi.cizimhafiza.presentation.mainmenu

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.presentation.common.BackdropCache
import com.sualtikasifi.cizimhafiza.presentation.common.FitText
import com.sualtikasifi.cizimhafiza.presentation.common.LetteredText
import com.sualtikasifi.cizimhafiza.presentation.common.pressable
import com.sualtikasifi.cizimhafiza.presentation.theme.DisplayFont

// tour_card is 956 x 1576; the rectangles are x0, y0, x1, y1 in the picture's own pixels. The picture has the title
// sign, the parchment and the orange button painted in; the lettering and the page's content go on top.
private const val CardW = 956f
private const val CardH = 1576f
private val TitleRect = floatArrayOf(190f, 150f, 770f, 245f)
private val EmojiRect = floatArrayOf(330f, 320f, 626f, 580f)
private val PageTitleRect = floatArrayOf(150f, 600f, 806f, 720f)
private val BodyRect = floatArrayOf(150f, 735f, 806f, 1170f)
private val DotsRect = floatArrayOf(300f, 1195f, 656f, 1255f)
private val ButtonRect = floatArrayOf(160f, 1385f, 800f, 1520f)
private val CloseRect = floatArrayOf(818f, 36f, 952f, 172f)

private val TourInk = Color(0xFF3A2416)
private val TourBodyInk = Color(0xFF5A3A22)
private val TourButtonOutline = Color(0xFF8A3A00)

/** Three-step "what is new" tour shown once: chests, jokers, the store. */
@Composable
internal fun FeatureTourDialog(onFinish: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val pages = listOf(
        Triple("🎁", R.string.tour_chests_title, R.string.tour_chests_body),
        Triple("🃏", R.string.tour_jokers_title, R.string.tour_jokers_body),
        Triple("🛍️", R.string.tour_store_title, R.string.tour_store_body)
    )
    val (emoji, title, body) = pages[step]
    val last = step == pages.lastIndex
    Dialog(onDismissRequest = onFinish, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xB31A0E05))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onFinish),
            contentAlignment = Alignment.Center
        ) {
            val frameW = minOf(maxWidth - 24.dp, (maxHeight - 24.dp) * (CardW / CardH), 440.dp)
            val u = frameW / CardW
            val fontScale0 = LocalDensity.current.fontScale
            val fs: (Float) -> TextUnit = { px -> (px * u.value / fontScale0).sp }
            val context = LocalContext.current
            val sheet = remember { runCatching { BackdropCache.get(context.resources, R.drawable.tour_card) }.getOrNull() }
            val pxK = (sheet?.width ?: CardW.toInt()) / CardW

            fun Modifier.at(r: FloatArray) = offset(u * r[0], u * r[1]).size(u * (r[2] - r[0]), u * (r[3] - r[1]))

            val enter = remember { Animatable(0f) }
            LaunchedEffect(Unit) { enter.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 380f)) }
            Box(
                Modifier
                    .size(frameW, frameW * (CardH / CardW))
                    .graphicsLayer {
                        val e = enter.value
                        scaleX = 0.85f + 0.15f * e
                        scaleY = 0.85f + 0.15f * e
                        alpha = (e * 3f).coerceIn(0f, 1f)
                    }
                    // Taps on the card must not reach the dim area behind it, which closes the tour.
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
            ) {
                Image(
                    painter = painterResource(R.drawable.tour_card),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )
                LetteredText(
                    text = stringResource(R.string.tour_heading),
                    size = fs(78f),
                    outline = TourButtonOutline,
                    maxLines = 1, minScale = 0.5f,
                    modifier = Modifier.at(TitleRect)
                )
                Box(Modifier.at(EmojiRect), contentAlignment = Alignment.Center) {
                    Text(text = emoji, fontSize = fs(190f))
                }
                FitText(
                    text = stringResource(title),
                    style = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = fs(70f), color = TourInk, textAlign = TextAlign.Center),
                    maxLines = 2, minScale = 0.5f,
                    modifier = Modifier.at(PageTitleRect)
                )
                FitText(
                    text = stringResource(body),
                    style = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = fs(46f), lineHeight = fs(62f), color = TourBodyInk, textAlign = TextAlign.Center),
                    maxLines = 9, minScale = 0.55f,
                    modifier = Modifier.at(BodyRect)
                )
                Row(
                    modifier = Modifier.at(DotsRect),
                    horizontalArrangement = Arrangement.spacedBy(u * 16f, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    pages.indices.forEach { i ->
                        Spacer(
                            modifier = Modifier
                                .size(if (i == step) u * 28f else u * 20f)
                                .background(if (i == step) Color(0xFFFF7A21) else Color(0xFFD9B57A), CircleShape)
                        )
                    }
                }

                // The button is painted into the card, so it sinks the way the home screen's painted buttons do.
                val (x0, y0, x1, y1) = ButtonRect
                val sink = rememberSink(0.94f)
                Box(
                    modifier = Modifier
                        .at(ButtonRect)
                        .sunkenArt(sink, sheet, x0 * pxK, y0 * pxK, (x1 - x0) * pxK, (y1 - y0) * pxK, u * ((y1 - y0) / 2f))
                        .clickable(interactionSource = sink.source, indication = null, onClick = { if (last) onFinish() else step++ })
                ) {
                    Box(Modifier.fillMaxSize().sinkWith(sink, 0.5f, 0.5f), contentAlignment = Alignment.Center) {
                        LetteredText(
                            text = stringResource(if (last) R.string.tour_start else R.string.tour_next),
                            size = fs(78f),
                            outline = TourButtonOutline,
                            maxLines = 1, minScale = 0.5f,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = u * 40f)
                        )
                    }
                }

                // The close button is part of the picture; this is its touch area.
                Box(
                    modifier = Modifier
                        .at(CloseRect)
                        .pressable(pressedScale = 0.9f, onClick = onFinish)
                )
            }
        }
    }
}
