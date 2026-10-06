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
        // The picture's own plank is smudged where its old lettering was wiped; a fresh board covers it, between the bolts.
        SignBoard(Modifier.offset(offX + unit * 272f, unit * 319f).size(unit * 409f, unit * 188f))
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

/** A freshly drawn plank of the same orange wood as the sign it covers: crisp at any size, with grain, a rim and a sheen. */
@Composable
private fun SignBoard(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val r = androidx.compose.ui.geometry.CornerRadius(size.height * 0.1f)
        val rim = 3.dp.toPx()
        drawRoundRect(Color(0xFF6B3410), cornerRadius = r)
        val inner = androidx.compose.ui.geometry.Size(size.width - rim * 2, size.height - rim * 2)
        val innerTop = androidx.compose.ui.geometry.Offset(rim, rim)
        drawRoundRect(
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFFFFA83A), Color(0xFFF28A1E), Color(0xFFD9701A))),
            topLeft = innerTop, size = inner,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(r.x - rim * 0.6f)
        )
        // Grain: long, slightly wavering strokes.
        val grain = listOf(0.14f, 0.27f, 0.41f, 0.55f, 0.68f, 0.82f)
        grain.forEachIndexed { i, fy ->
            val y = rim + inner.height * fy
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(rim + inner.width * 0.04f, y)
                cubicTo(
                    rim + inner.width * 0.3f, y + (if (i % 2 == 0) 3f else -3f).dp.toPx(),
                    rim + inner.width * 0.65f, y + (if (i % 2 == 0) -3f else 3f).dp.toPx(),
                    rim + inner.width * 0.96f, y
                )
            }
            drawPath(path, Color(0xFF9A4A10).copy(alpha = 0.22f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.6.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
        }
        // Sheen along the top edge and a soft shade along the bottom.
        drawRoundRect(
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.32f), Color.Transparent), endY = inner.height * 0.28f, startY = rim),
            topLeft = innerTop, size = inner,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(r.x - rim * 0.6f)
        )
        drawRoundRect(
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF5A2408).copy(alpha = 0.3f)), startY = size.height * 0.72f, endY = size.height - rim),
            topLeft = innerTop, size = inner,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(r.x - rim * 0.6f)
        )
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
