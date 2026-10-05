package com.sualtikasifi.cizimhafiza.presentation.common

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified

/**
 * [style] with its font size (and line height, when one is set in sp) multiplied by [scale] — the two have to move
 * together, or a shrunk two-line label keeps the full-size gap between its lines and still does not fit.
 */
internal fun TextStyle.scaledBy(scale: Float): TextStyle {
    if (scale == 1f || !fontSize.isSpecified) return this
    return copy(
        fontSize = fontSize * scale,
        lineHeight = if (lineHeight.isSpecified && lineHeight.isSp) lineHeight * scale else lineHeight,
        letterSpacing = if (letterSpacing.isSpecified && letterSpacing.isSp) letterSpacing * scale else letterSpacing
    )
}

/**
 * The largest scale in [minScale]..1 at which [text] fits the space this scope was given, in at most [maxLines]
 * lines. 1 whenever the width is unbounded (nothing to fit into) or the text already fits.
 */
@Composable
internal fun BoxWithConstraintsScope.rememberFitScale(text: String, style: TextStyle, maxLines: Int, minScale: Float): Float {
    val measurer = rememberTextMeasurer(cacheSize = 16)
    val c = constraints
    return remember(text, style, maxLines, minScale, c.maxWidth, c.maxHeight) {
        if (!c.hasBoundedWidth || text.isEmpty() || !style.fontSize.isSpecified) return@remember 1f
        fun fits(scale: Float): Boolean {
            val result = measurer.measure(
                text = text,
                style = style.scaledBy(scale),
                overflow = TextOverflow.Clip,
                softWrap = true,
                maxLines = maxLines,
                constraints = Constraints(maxWidth = c.maxWidth)
            )
            return !result.hasVisualOverflow && (!c.hasBoundedHeight || result.size.height <= c.maxHeight)
        }
        if (fits(1f)) return@remember 1f
        var lo = minScale
        var hi = 1f
        if (!fits(lo)) return@remember lo
        repeat(7) {
            val mid = (lo + hi) / 2f
            if (fits(mid)) lo = mid else hi = mid
        }
        lo
    }
}

/**
 * A [Text] that shrinks (down to [minScale] of [style]'s size) instead of being cut off when it does not fit the
 * space it is given. Below that it ends in an ellipsis. Use it for every label laid into a fixed spot on a painted
 * scene, where the box size is decided by the picture rather than by the words.
 */
@Composable
fun FitText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    minScale: Float = 0.55f,
    contentAlignment: Alignment = Alignment.Center
) {
    BoxWithConstraints(modifier = modifier, contentAlignment = contentAlignment) {
        val scale = rememberFitScale(text, style, maxLines, minScale)
        Text(
            text = text,
            style = style.scaledBy(scale),
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Convenience for callers that only know a size: the same as [FitText] with [style]'s size replaced. */
@Composable
fun FitText(
    text: String,
    style: TextStyle,
    size: TextUnit,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    minScale: Float = 0.55f,
    contentAlignment: Alignment = Alignment.Center
) = FitText(text, style.copy(fontSize = size), modifier, maxLines, minScale, contentAlignment)
