package com.sualtikasifi.cizimhafiza.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sualtikasifi.cizimhafiza.R

// The window picture (window_frame) is 1036 x 1518: a gold frame round cream paper, the crown, brushes and palette and a
// pink ribbon on top, paint splashes in the four corners. Nothing in its middle band is drawn, so the band can stretch.
private const val ArtW = 1036f
private const val ArtH = 1518f
/** Where the stretching middle band starts and ends in the picture: below the top splashes, above the bottom ones. */
private const val BandFrom = 470
private const val BandTo = 1290
/** The ribbon's lettering box, in picture units, and where the paper's content starts and ends. */
private const val RibbonX = 270f
private const val RibbonY = 118f
private const val RibbonW = 500f
private const val RibbonH = 120f
private const val ContentTop = 320f
private const val ContentSide = 100f
private const val ContentBottom = 110f

/**
 * The game's window: the painted frame with a lettered [title] on its ribbon and, when [onClose] is given, a round
 * close button on its top-right corner. The picture is scaled to the window's width and only its empty middle band
 * stretches, so a short question and a tall picker both keep the ribbon, the crown and the corners as drawn.
 * [content] is laid out on the cream paper.
 */
@Composable
fun PaintedWindowFrame(
    title: String,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val art: ImageBitmap = ImageBitmap.imageResource(R.drawable.window_frame)
    BoxWithConstraints(modifier) {
        val u = maxWidth / ArtW
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = u * (BandFrom + (ArtH - BandTo)), max = maxHeight)
                .drawBehind {
                    val w = size.width.toInt().coerceAtLeast(1)
                    val topH = (BandFrom * size.width / ArtW).toInt()
                    val botH = ((ArtH - BandTo) * size.width / ArtW).toInt()
                    val midH = (size.height.toInt() - topH - botH).coerceAtLeast(0)
                    fun slice(srcY: Int, srcH: Int, dstY: Int, dstH: Int) = drawImage(
                        image = art,
                        srcOffset = IntOffset(0, srcY), srcSize = IntSize(ArtW.toInt(), srcH),
                        dstOffset = IntOffset(0, dstY), dstSize = IntSize(w, dstH),
                        filterQuality = FilterQuality.High
                    )
                    slice(0, BandFrom, 0, topH)
                    if (midH > 0) slice(BandFrom, BandTo - BandFrom, topH, midH)
                    slice(BandTo, (ArtH - BandTo).toInt(), topH + midH, botH)
                }
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(start = u * ContentSide, end = u * ContentSide, top = u * ContentTop, bottom = u * ContentBottom),
                content = content
            )
            Box(Modifier.offset(u * RibbonX, u * RibbonY).size(u * RibbonW, u * RibbonH), contentAlignment = Alignment.Center) {
                LetteredText(text = title, size = 26.sp, outline = Color(0xFF5B1247), maxLines = 2, minScale = 0.5f, modifier = Modifier.fillMaxWidth())
            }
            if (onClose != null) {
                // on the gold frame's top-right corner, half over its edge
                Box(
                    Modifier
                        .offset(u * 930f, u * 205f)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD7263D))
                        .border(2.dp, Color(0xFFFFE9A8), CircleShape)
                        .clickable(onClick = onClose)
                        .a11yButton(stringResource(R.string.close)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                }
            }
        }
    }
}
