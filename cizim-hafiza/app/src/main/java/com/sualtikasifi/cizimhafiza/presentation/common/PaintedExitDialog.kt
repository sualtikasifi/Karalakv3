package com.sualtikasifi.cizimhafiza.presentation.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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

/**
 * The two painted "leave?" windows, each a picture with empty buttons (rectangles are x0, y0, x1, y1 in the picture's
 * own pixels): the sad crowned dog for leaving the app, the stern one (a red Quit button) for leaving a game.
 */
enum class ExitDialogStyle(
    val art: Int,
    val artW: Float,
    val artH: Float,
    val title: FloatArray,
    val message: FloatArray,
    val left: FloatArray,
    val right: FloatArray,
    val confirmOutline: Color
) {
    /** exit_dialog, 1100 x 823 — the main menu's "Karalak'tan çık". */
    Menu(
        R.drawable.exit_dialog, 1100f, 823f,
        title = floatArrayOf(230f, 357f, 882f, 447f), message = floatArrayOf(300f, 455f, 812f, 531f),
        left = floatArrayOf(163f, 566f, 543f, 731f), right = floatArrayOf(560f, 566f, 940f, 731f),
        confirmOutline = Color(0xFF7A2A08)
    ),
    /** exit_game_dialog, 1100 x 795 — leaving a game (offline, daily challenge, quick match, room). */
    InGame(
        R.drawable.exit_game_dialog, 1100f, 795f,
        title = floatArrayOf(264f, 353f, 850f, 440f), message = floatArrayOf(285f, 468f, 821f, 566f),
        left = floatArrayOf(149f, 572f, 545f, 716f), right = floatArrayOf(557f, 572f, 949f, 716f),
        confirmOutline = Color(0xFF7A0E0E)
    )
}

private val ExitInk = Color(0xFF3A2416)
private val ExitSoftInk = Color(0xFF7A4A24)

/**
 * The window shown when the player backs out of the main menu. The picture carries the dog, the sign and both
 * buttons; the question, its small line and the two button words are written over it (so they follow the language),
 * and each painted button caves in under the finger like the game's other painted buttons.
 */
@Composable
fun PaintedExitDialog(
    title: String,
    message: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    style: ExitDialogStyle = ExitDialogStyle.Menu
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            val frameW = minOf(maxWidth - 16.dp, (maxHeight - 24.dp) * (style.artW / style.artH))
            val u = frameW / style.artW
            val fontScale0 = LocalDensity.current.fontScale
            val fs: (Float) -> TextUnit = { px -> (px * u.value / fontScale0).sp }
            val context = LocalContext.current
            val sheet = remember { runCatching { BackdropCache.get(context.resources, style.art) }.getOrNull() }
            val pxK = (sheet?.width ?: style.artW.toInt()) / style.artW

            @Composable
            fun PaintedButton(r: FloatArray, onClick: () -> Unit, label: @Composable BoxScope.() -> Unit) {
                val (x0, y0, x1, y1) = r
                val sink = rememberSink(0.92f)
                Box(
                    modifier = Modifier
                        .offset(u * x0, u * y0)
                        .size(u * (x1 - x0), u * (y1 - y0))
                        .sunkenArt(sink, sheet, x0 * pxK, y0 * pxK, (x1 - x0) * pxK, (y1 - y0) * pxK, u * ((y1 - y0) / 2f))
                        .clickable(interactionSource = sink.source, indication = null, onClick = onClick)
                ) {
                    // The word dips together with the painted button.
                    Box(
                        modifier = Modifier.fillMaxSize().padding(horizontal = u * 40f).sinkWith(sink, 0.5f, 0.5f),
                        contentAlignment = Alignment.Center,
                        content = label
                    )
                }
            }

            // Pops up with a little spring.
            val enter = remember { Animatable(0f) }
            LaunchedEffect(Unit) { enter.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 380f)) }
            Box(
                Modifier
                    .size(frameW, frameW * (style.artH / style.artW))
                    .graphicsLayer {
                        val e = enter.value
                        scaleX = 0.82f + 0.18f * e
                        scaleY = 0.82f + 0.18f * e
                        alpha = (e * 3f).coerceIn(0f, 1f)
                    }
                    // A tap on the sign itself must not count as a tap outside.
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            ) {
                Image(
                    painter = painterResource(style.art),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )
                FitText(
                    text = title,
                    style = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = fs(80f), color = ExitInk, textAlign = TextAlign.Center),
                    maxLines = 1, minScale = 0.5f,
                    modifier = Modifier.offset(u * style.title[0], u * style.title[1]).size(u * (style.title[2] - style.title[0]), u * (style.title[3] - style.title[1]))
                )
                FitText(
                    text = message,
                    style = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.SemiBold, fontSize = fs(46f), color = ExitSoftInk, textAlign = TextAlign.Center),
                    maxLines = 3, minScale = 0.5f,
                    modifier = Modifier.offset(u * style.message[0], u * style.message[1]).size(u * (style.message[2] - style.message[0]), u * (style.message[3] - style.message[1]))
                )
                PaintedButton(style.left, onDismiss) {
                    FitText(
                        text = dismissText,
                        style = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = fs(68f), color = ExitInk, textAlign = TextAlign.Center),
                        maxLines = 1, minScale = 0.55f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                PaintedButton(style.right, onConfirm) {
                    LetteredText(text = confirmText, size = fs(68f), outline = style.confirmOutline, maxLines = 1)
                }
            }
        }
    }
}
