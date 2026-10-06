package com.sualtikasifi.cizimhafiza.presentation.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sualtikasifi.cizimhafiza.R

// bg_room is this size: the workshop with the lamp, the hanging sign (blank, crown on top) and the long table.
private const val RoomW = 941f
private const val RoomH = 1670f

/**
 * The workshop page used by Hesap and Sorun Bildir: the painted room, [title] lettered on the hanging sign (one line,
 * or two when it is long), the wooden back button, and [content] in a column that starts under the sign and scrolls
 * when it has to. [beside] is drawn over the sign's right end (Sorun Bildir's mascot).
 */
@Composable
fun RoomPage(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    beside: (@Composable (signHeight: Dp) -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // Filled by height: a phone is narrower than the picture, so only wall and table are trimmed at the sides.
        val unit = maxOf(maxWidth / RoomW, maxHeight / RoomH)
        val offX = (maxWidth - unit * RoomW) / 2
        val fontScale0 = LocalDensity.current.fontScale
        fun fs(art: Float) = (art * unit.value / fontScale0).sp
        Image(
            painter = cachedPainterResource(R.drawable.bg_room),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxSize()
        )
        // The sign's plank, between its bolts.
        val words = title.split(' ')
        if (words.size >= 2 && title.length > 8) {
            LetteredText(
                text = words.first(),
                size = fs(96f),
                fill = Color(0xFFFFC21F),
                outline = Color(0xFF5A2815),
                modifier = Modifier.offset(offX + unit * 286f, unit * 330f).size(unit * 392f, unit * 92f),
                minScale = 0.45f
            )
            LetteredText(
                text = words.drop(1).joinToString(" "),
                size = fs(96f),
                outline = Color(0xFF5A2815),
                modifier = Modifier.offset(offX + unit * 286f, unit * 410f).size(unit * 392f, unit * 88f),
                minScale = 0.45f
            )
        } else {
            LetteredText(
                text = title,
                size = fs(132f),
                fill = Color(0xFFFFC21F),
                outline = Color(0xFF5A2815),
                modifier = Modifier.offset(offX + unit * 286f, unit * 336f).size(unit * 392f, unit * 156f),
                minScale = 0.4f
            )
        }
        beside?.invoke(unit * 560f)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = unit * 548f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            content = content
        )
        PaintedBackButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart))
    }
}

/** The cream parchment card in a wooden frame that every panel on the workshop pages sits on. */
@Composable
fun ParchmentCard(modifier: Modifier = Modifier, padding: Dp = 16.dp, content: @Composable ColumnScope.() -> Unit) {
    NinePatch(res = R.drawable.league_card, slicePx = 100, edge = 22.dp, modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = padding, vertical = padding * 0.8f), content = content)
    }
}

/** A gap between the panels of a workshop page. */
@Composable
fun ColumnScope.RoomGap(height: Dp = 10.dp) = Spacer(Modifier.height(height))
