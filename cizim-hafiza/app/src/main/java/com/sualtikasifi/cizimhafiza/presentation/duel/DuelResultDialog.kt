package com.sualtikasifi.cizimhafiza.presentation.duel

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.presentation.common.FitText
import com.sualtikasifi.cizimhafiza.presentation.common.LetteredText
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedStyle
import com.sualtikasifi.cizimhafiza.presentation.common.pressable
import com.sualtikasifi.cizimhafiza.presentation.common.springIn

/** The window is one painted picture (duel_result_window, 1098 x 1225 units); every box below is a rectangle of it. */
private const val ArtW = 1098f
private const val ArtH = 1225f
private val GoldInk = Color(0xFF4A2A0C)
private val SilverInk = Color(0xFF3F3F44)
private val RibbonOutline = Color(0xFF3A0F5E)
private val ButtonOutline = Color(0xFF8A3A00)

/**
 * A finished duel's result, from the list of recent ones: the verdict on the ribbon, the winner on the gold bar (with the crown) and
 * the other player on the silver one, each with their score, and one button to close it. The picture carries all of that; the words
 * are laid over it. Closing it is only closing: no ad follows, as this is not the end of a game.
 */
@Composable
internal fun DuelResultDialog(recent: RecentDuel, myName: String, onDismiss: () -> Unit) {
    // the player's own username ("You" only for an account that has none yet)
    val me = myName.ifBlank { stringResource(R.string.quick_match_you) }
    // the winner is on top, on the gold bar; a draw keeps the player there
    val meOnTop = recent.iWon != false
    val topName = if (meOnTop) me else recent.otherName
    val topScore = if (meOnTop) recent.myScore else recent.otherScore
    val bottomName = if (meOnTop) recent.otherName else me
    val bottomScore = if (meOnTop) recent.otherScore else recent.myScore
    val verdict = stringResource(
        when (recent.iWon) { true -> R.string.quick_match_won; false -> R.string.quick_match_lost; null -> R.string.quick_match_drew }
    )

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            val frameW = minOf(maxWidth - 20.dp, (maxHeight - 40.dp) * (ArtW / ArtH), 460.dp)
            val u: Dp = frameW / ArtW
            val fontScale = LocalDensity.current.fontScale
            fun fs(art: Float) = (art * u.value / fontScale).sp
            fun Modifier.art(x: Float, y: Float, w: Float, h: Float) = offset(u * x, u * y).size(u * w, u * h)

            Box(
                Modifier
                    .size(frameW, frameW * (ArtH / ArtW))
                    .springIn(fromY = 60, stepMs = 0)
                    // swallow taps on the window itself so only the dim area closes it
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
            ) {
                Image(
                    painter = painterResource(R.drawable.duel_result_window),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )

                // The ribbon: the verdict.
                LetteredText(
                    text = verdict,
                    size = fs(92f),
                    outline = RibbonOutline,
                    modifier = Modifier.art(250f, 169f, 600f, 108f),
                    minScale = 0.45f,
                    title = true
                )

                // The two bars: a name on the left, the score on the right (both scores end at the same place).
                Bar(topName, topScore, 597f, GoldInk, u, ::fs)
                Bar(bottomName, bottomScore, 742f, SilverInk, u, ::fs)

                // The orange button.
                Box(
                    Modifier.art(170f, 914f, 760f, 135f).pressable(pressedScale = 0.96f, onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    LetteredText(
                        text = stringResource(R.string.close),
                        size = fs(78f),
                        outline = ButtonOutline,
                        modifier = Modifier.size(u * 560f, u * 100f),
                        minScale = 0.5f
                    )
                }
            }
        }
    }
}

@Composable
private fun Bar(name: String, score: Int, top: Float, ink: Color, u: Dp, fs: (Float) -> androidx.compose.ui.unit.TextUnit) {
    fun Modifier.art(x: Float, y: Float, w: Float, h: Float) = offset(u * x, u * y).size(u * w, u * h)
    FitText(
        text = name,
        style = PaintedStyle(color = ink, fontSize = fs(54f), fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Start),
        maxLines = 1,
        minScale = 0.45f,
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier.art(305f, top, 365f, 98f).padding(bottom = u * 6f)
    )
    FitText(
        text = "$score",
        style = PaintedStyle(color = ink, fontSize = fs(66f), fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.End),
        maxLines = 1,
        minScale = 0.45f,
        contentAlignment = Alignment.CenterEnd,
        modifier = Modifier.art(675f, top, 135f, 98f).padding(bottom = u * 6f)
    )
}
