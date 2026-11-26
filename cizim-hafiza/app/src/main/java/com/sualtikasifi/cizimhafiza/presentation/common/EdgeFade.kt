package com.sualtikasifi.cizimhafiza.presentation.common

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Melts the scrolling content itself (not the picture behind it) away at its top and/or bottom edge, so a list does not end
 * in a hard cut where it meets the sign above or the system bar below, and a half-visible row reads as "scroll for more".
 *
 * [top] and [bottom] are the lengths of the fade; each only shows while its [topActive] / [bottomActive] says there is
 * more content that way (at rest the first row sits right at the top edge and must not come out faded; at the very end the
 * last row has its own padding and the fade would only dim it). Put it on the scrolling container, before its padding.
 */
fun Modifier.edgeFade(
    top: Dp = 0.dp,
    bottom: Dp = 0.dp,
    topActive: () -> Boolean = { true },
    bottomActive: () -> Boolean = { true }
): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val h = size.height
        if (h <= 0f) return@drawWithContent
        val t = if (top > 0.dp && topActive()) top.toPx().coerceAtMost(h / 2) else 0f
        val b = if (bottom > 0.dp && bottomActive()) bottom.toPx().coerceAtMost(h / 2) else 0f
        if (t == 0f && b == 0f) return@drawWithContent
        val stops = buildList {
            if (t > 0f) { add(0f to Color.Transparent); add(t / h to Color.Black) } else add(0f to Color.Black)
            if (b > 0f) { add(1f - b / h to Color.Black); add(1f to Color.Transparent) } else add(1f to Color.Black)
        }
        drawRect(brush = Brush.verticalGradient(colorStops = stops.toTypedArray()), blendMode = BlendMode.DstIn)
    }
