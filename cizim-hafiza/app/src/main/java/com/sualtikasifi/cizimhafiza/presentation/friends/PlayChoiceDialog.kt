package com.sualtikasifi.cizimhafiza.presentation.friends

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

/** The window is one painted picture (play_choice_window, 948 x 1152 units); every box below is a rectangle of it. */
private const val ArtW = 948f
private const val ArtH = 1152f
private val ChoiceInk = Color(0xFF3B2314)
private val RibbonOutline = Color(0xFF3A0F5E)
private val ButtonOutline = Color(0xFF8A3A00)

/**
 * "How do you want to play?" over a friend: a live room right now, or a duel the friend answers whenever they like.
 * The picture carries the ribbon, the two orange buttons with their icons and the two cream boxes under them; the words
 * (the title, each button's name, what each is like) are laid over those, and a tap on a whole orange bar chooses it.
 */
@Composable
internal fun PlayChoiceDialog(onRoom: () -> Unit, onDuel: () -> Unit, onDismiss: () -> Unit) {
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
                    painter = painterResource(R.drawable.play_choice_window),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )

                // The ribbon: the title.
                LetteredText(
                    text = stringResource(R.string.friends_pick_title),
                    size = fs(84f),
                    outline = RibbonOutline,
                    modifier = Modifier.art(225f, 140f, 510f, 105f),
                    minScale = 0.45f,
                    title = true
                )

                Choice(
                    label = stringResource(R.string.friends_pick_room), description = stringResource(R.string.friends_pick_room_desc),
                    barTop = 406f, descTop = 560f, u = u, fs = ::fs, onClick = onRoom
                )
                Choice(
                    label = stringResource(R.string.friends_pick_duel), description = stringResource(R.string.friends_pick_duel_desc),
                    barTop = 708f, descTop = 862f, u = u, fs = ::fs, onClick = onDuel
                )
            }
        }
    }
}

/** One orange bar (its name right of the painted icon, the whole bar a button) and the cream box under it with what it is like. */
@Composable
private fun Choice(
    label: String, description: String, barTop: Float, descTop: Float,
    u: Dp, fs: (Float) -> androidx.compose.ui.unit.TextUnit, onClick: () -> Unit
) {
    fun Modifier.art(x: Float, y: Float, w: Float, h: Float) = offset(u * x, u * y).size(u * w, u * h)
    Box(Modifier.art(95f, barTop, 755f, 144f).pressable(pressedScale = 0.96f, onClick = onClick)) {
        LetteredText(
            text = label,
            size = fs(64f),
            outline = ButtonOutline,
            modifier = Modifier.offset(u * 228f, u * 14f).size(u * 515f, u * 112f),
            minScale = 0.5f
        )
    }
    Box(Modifier.art(125f, descTop, 695f, 96f), contentAlignment = Alignment.Center) {
        FitText(
            text = description,
            style = PaintedStyle(color = ChoiceInk, fontSize = fs(33f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = fs(40f)),
            maxLines = 3,
            minScale = 0.5f,
            modifier = Modifier.fillMaxSize().padding(horizontal = u * 8f)
        )
    }
}
