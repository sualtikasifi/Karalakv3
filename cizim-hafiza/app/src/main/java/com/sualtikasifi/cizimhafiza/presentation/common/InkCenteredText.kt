package com.sualtikasifi.cizimhafiza.presentation.common

import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import com.sualtikasifi.cizimhafiza.R

/**
 * A short label (a code, a digit) centred by its ink rather than by its line box: Baloo's tall ascent puts digits
 * visibly off-centre in a cell sized for them, and letter spacing leaves a gap after the last character. Here the
 * glyphs' own outline is measured and that is put in the middle of the space this is given (it fills its parent).
 */
@Composable
fun InkCenteredText(
    text: String,
    color: Color,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    letterSpacing: TextUnit = 0.sp,
    fontRes: Int = R.font.baloo2_extrabold
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val typeface = remember(fontRes) { ResourcesCompat.getFont(context, fontRes) }
    val sizePx = with(density) { fontSize.toPx() }
    val spacingPx = with(density) { letterSpacing.toPx() }
    val paint = remember(typeface, sizePx, color) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = sizePx
            this.color = color.toArgb()
        }
    }
    // The outline of the whole label, character by character so the spacing goes only between them.
    val shape = remember(text, paint, spacingPx) {
        val path = Path()
        val glyph = Path()
        var x = 0f
        text.forEachIndexed { i, _ ->
            paint.getTextPath(text, i, i + 1, x, 0f, glyph)
            path.addPath(glyph)
            x += paint.measureText(text, i, i + 1) + spacingPx
        }
        val bounds = RectF()
        path.computeBounds(bounds, true)
        path to bounds
    }
    Canvas(modifier.fillMaxSize().semantics { contentDescription = text }) {
        val (path, bounds) = shape
        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            native.save()
            native.translate(size.width / 2f - bounds.centerX(), size.height / 2f - bounds.centerY())
            native.drawPath(path, paint)
            native.restore()
        }
    }
}
