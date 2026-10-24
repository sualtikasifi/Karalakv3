package com.sualtikasifi.cizimhafiza.presentation.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sualtikasifi.cizimhafiza.R

private const val PenArtW = 880f
private const val PenArtH = 1787f

/**
 * The pen picker's window: one painted picture (wooden sign with pencils on top, parchment, vines), the title written on
 * the sign in the player's language, a round close button on the top-right leaves and the pens on the parchment.
 * Rectangles are in the picture's own pixels (880 x 1787).
 */
@Composable
fun PaintedPenWindow(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            val frameW = minOf(maxWidth - 24.dp, 480.dp, (maxHeight - 48.dp) * (PenArtW / PenArtH))
            val u = frameW / PenArtW
            Box(
                Modifier
                    .size(frameW, frameW * (PenArtH / PenArtW))
                    .springIn(fromY = 44, stepMs = 0)
                    // A tap on the window itself is not a tap outside it.
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
            ) {
                Image(
                    painter = painterResource(R.drawable.pen_window),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )
                FitText(
                    text = title,
                    style = PaintedStyle(color = Color(0xFF3A2416), fontSize = 26.sp, textAlign = TextAlign.Center),
                    maxLines = 1,
                    minScale = 0.6f,
                    modifier = Modifier.offset(u * 262f, u * 160f).size(u * 396f, u * 108f)
                )
                Box(
                    modifier = Modifier
                        .offset(u * 764f, u * 30f)
                        .size(u * 96f)
                        .clip(CircleShape)
                        .background(Color(0xFFFFF3DC))
                        .border(u * 5f, Color(0xFF8A4B12), CircleShape)
                        .clickable(onClick = onDismiss)
                        .a11yButton(stringResource(R.string.close)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", color = Color(0xFF3A2416), fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                }
                Column(modifier = Modifier.offset(u * 118f, u * 322f).size(u * 654f, u * 1236f), content = content)
            }
        }
    }
}
