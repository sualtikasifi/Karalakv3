package com.sualtikasifi.cizimhafiza.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import com.sualtikasifi.cizimhafiza.domain.model.PenSkin

/**
 * Turns a [PenSkin] into something the canvas can paint with.
 *
 * A single-colour pen becomes a [SolidColor], which costs exactly what a
 * plain colour did before. Kept for the reward-preview swatches (a fixed
 * curve painted once, no drawing to "use") — actual drawing surfaces use
 * [drawPenStroke] instead, so a multi-stop pen's colour tracks how much ink
 * has been laid down rather than where on the empty page a point happens to
 * fall (see drawPenStroke's doc for why that distinction matters).
 */
@Composable
fun rememberPenBrush(skin: PenSkin, canvasWidth: Float, canvasHeight: Float): Brush =
    remember(skin, canvasWidth, canvasHeight) { penBrush(skin, canvasWidth, canvasHeight) }

fun penBrush(skin: PenSkin, canvasWidth: Float, canvasHeight: Float): Brush {
    val colors = skin.colors.map { Color(it) }
    if (colors.size == 1) return SolidColor(colors.first())
    return Brush.linearGradient(
        colors = colors,
        start = Offset.Zero,
        end = Offset(canvasWidth.coerceAtLeast(1f), canvasHeight.coerceAtLeast(1f))
    )
}

/** The pen's representative colour — for swatches in the picker, where a full gradient sweep would be illegible at 40dp. */
fun PenSkin.previewColor(): Color = Color(colors.first())

/**
 * How much drawn distance (px) one one-way sweep through a gradient pen's
 * colour list takes — see [penColorAt] for why a "sweep" is a one-way trip,
 * not a full loop. Distance is the pen's own "usage" — every player draws at
 * a different scale/pace, so this is intentionally generous rather than
 * tuned to any one canvas size. Longer than the first version of this (was
 * 1400f): a short cycle made the colour visibly stride from stop to stop
 * within a single ordinary stroke, which read as choppy rather than as a
 * flowing gradient.
 */
private const val PEN_USAGE_CYCLE_PX = 2200f

/**
 * The colour a gradient pen shows after [distancePx] of ink has been drawn
 * so far. A flat pen just returns its one colour.
 *
 * Walks the colour list forward, then backward, then forward again — a
 * triangle wave, not a sawtooth. A sawtooth (plain `distancePx % cycle`)
 * looks smooth for the whole sweep and then, at the loop point, cuts
 * straight from the LAST colour back to the FIRST with no blend at all —
 * exactly the "harsh, sudden transition when it loops back to the start"
 * players noticed. Reflecting the direction instead of resetting it means
 * every point on the cycle, including the loop point itself, is a
 * continuous blend between neighbours: the pen colour breathes back and
 * forth rather than snapping.
 */
fun penColorAt(skin: PenSkin, distancePx: Float): Color {
    val colors = skin.colors.map { Color(it) }
    if (colors.size == 1) return colors.first()
    val segments = colors.size - 1
    val period = PEN_USAGE_CYCLE_PX * 2f
    val phase = distancePx.mod(period)
    val forward = if (phase <= PEN_USAGE_CYCLE_PX) phase else period - phase
    val t = (forward / PEN_USAGE_CYCLE_PX) * segments
    val index = t.toInt().coerceIn(0, segments - 1)
    return lerp(colors[index], colors[index + 1], t - index)
}

/**
 * Draws one already-mapped-to-canvas stroke, coloured by how far into the
 * pen's own usage it falls rather than by canvas position.
 *
 * [penBrush] paints a gradient laid out across the fixed drawing surface, so
 * a Sunset or Rainbow pen always shows the same colour at the same spot on
 * an empty page — the pen looked "used up" in some corners and fresh in
 * others no matter what was actually drawn. Colour should instead follow the
 * pen the way it does on paper: it shifts as you draw, continuing from
 * wherever the last stroke left off, never resetting because you moved to a
 * different part of the page. That is what walking the stroke segment by
 * segment and colouring each one from [startDistance] achieves; a flat pen
 * skips all of that and draws its one colour exactly as before.
 *
 * Returns the distance drawn so far (start + this stroke's own length), so
 * the next stroke in the same drawing can carry the colour on from there.
 */
fun DrawScope.drawPenStroke(
    points: List<Offset>,
    skin: PenSkin?,
    strokeColor: Color,
    strokeWidthPx: Float,
    startDistance: Float
): Float {
    if (points.isEmpty()) return startDistance
    val flatColor = skin?.let { Color(it.colors.first()) } ?: strokeColor
    if (points.size == 1) {
        val color = if (skin != null && skin.isGradient) penColorAt(skin, startDistance) else flatColor
        drawCircle(color = color, radius = strokeWidthPx / 2f, center = points.first())
        return startDistance
    }
    if (skin == null || !skin.isGradient) {
        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
        }
        drawPath(path = path, color = flatColor, style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round))
        return startDistance
    }
    var distance = startDistance
    for (i in 1 until points.size) {
        val a = points[i - 1]
        val b = points[i]
        drawLine(
            color = penColorAt(skin, distance),
            start = a,
            end = b,
            strokeWidth = strokeWidthPx,
            cap = StrokeCap.Round
        )
        distance += (b - a).getDistance()
    }
    return distance
}
