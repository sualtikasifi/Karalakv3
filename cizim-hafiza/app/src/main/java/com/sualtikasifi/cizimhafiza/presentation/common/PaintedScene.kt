package com.sualtikasifi.cizimhafiza.presentation.common

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

/**
 * A full-screen painted scene cut into three bands: a top and a bottom band drawn at their natural proportions, and a
 * middle band (only straight frame sides in the art) that stretches to whatever height is left. All measurements are in
 * "art units" of the source picture ([artHeight] units tall, as wide as the screen is). [topFrom]..[topEnd] is the part
 * of the top band that is shown (so the very top can be dropped on a short screen), [bottomStart]..[bottomTo] likewise
 * for the bottom band, and [shift] pushes the whole picture down (a tall status bar).
 */
@Composable
fun StretchBackground(
    res: Int,
    artHeight: Float,
    topFrom: Float,
    topEnd: Float,
    bottomStart: Float,
    bottomTo: Float,
    modifier: Modifier = Modifier,
    shift: Dp = Dp.Hairline
) {
    val img: ImageBitmap = ImageBitmap.imageResource(res)
    Box(
        modifier = modifier.drawBehind {
            val ks = img.height / artHeight
            val unit = size.width / img.width * ks
            val w = size.width.roundToInt()
            val h = size.height.roundToInt()
            val off = shift.toPx().roundToInt().coerceAtLeast(0)
            val topDst = ((topEnd - topFrom) * unit).roundToInt()
            val botDst = ((bottomTo - bottomStart) * unit).roundToInt()
            val midDst = (h - off - topDst - botDst).coerceAtLeast(0)
            fun slice(srcA: Float, srcB: Float, dy: Int, dh: Int) {
                if (dh <= 0) return
                val sy = (srcA * ks).roundToInt()
                val sh = ((srcB - srcA) * ks).roundToInt().coerceAtLeast(1)
                drawImage(
                    image = img,
                    srcOffset = IntOffset(0, sy),
                    srcSize = IntSize(img.width, sh),
                    dstOffset = IntOffset(0, dy),
                    dstSize = IntSize(w, dh),
                    filterQuality = FilterQuality.Medium
                )
            }
            if (off > 0) slice(topFrom, topFrom + 1f, 0, off)
            slice(topFrom, topEnd, off, topDst)
            slice(topEnd, bottomStart, off + topDst, midDst)
            slice(bottomStart, bottomTo, off + topDst + midDst, botDst)
        }
    )
}
