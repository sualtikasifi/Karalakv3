package com.sualtikasifi.cizimhafiza.presentation.quickmatch

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.presentation.common.FitText
import com.sualtikasifi.cizimhafiza.presentation.common.LetteredText
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedStyle
import com.sualtikasifi.cizimhafiza.presentation.common.cachedPainterResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin

// bg_qm_search is this size: the workshop with the hanging sign, the two mascots drawing and the long table between
// them. It is scaled to fill the screen's height (a phone is narrower than the picture, so its sides are trimmed —
// only wall and table out there) and everything live is laid over it by the picture's own coordinates.
private const val ArtW = 841f
private const val ArtH = 1870f

private val Ink = Color(0xFF3B2314)

/**
 * "Rakip aranıyor…": the title lettered on the hanging sign (its dots ticking), and on the table between the two
 * mascots a sheet of paper where a pencil keeps drawing a little doodle — a star, a heart, a house, a sun — one after
 * another, the drawing-themed stand-in for a spinner (what the opponent being looked for did was draw). A note under
 * it says what is happening.
 */
@Composable
internal fun SearchingScene(modifier: Modifier = Modifier) {
    // One clock for everything that moves here, driven by frames rather than by animators: it keeps running even
    // when the phone's animation scale is turned off (an endless spinner stand-in must never sit frozen).
    // Read only inside draw / graphicsLayer lambdas, so it never recomposes the scene.
    val clock = remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        val start = androidx.compose.runtime.withFrameMillis { it }
        while (true) androidx.compose.runtime.withFrameMillis { clock.longValue = it - start }
    }
    val cycle = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { (clock.longValue % (DOODLE_MS.toLong() * Doodles.size)) / DOODLE_MS.toFloat() } }
    val dots = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { (clock.longValue % 1600L) / 400f } }
    val sway = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { sin(clock.longValue / 2400f * PI.toFloat()) } }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val unit = maxOf(maxWidth / ArtW, maxHeight / ArtH)
        val offX = (maxWidth - unit * ArtW) / 2
        val offY = (maxHeight - unit * ArtH) / 2
        val us = unit.value
        val fontScale0 = LocalDensity.current.fontScale
        fun fs(art: Float) = (art * us / fontScale0).sp
        fun box(x0: Float, y0: Float, x1: Float, y1: Float): Modifier =
            Modifier.offset(offX + unit * x0, offY + unit * y0)
                // Unbounded: the picture is wider than the screen, and a plain size() would be clamped to it.
                .wrapContentSize(Alignment.TopStart, unbounded = true)
                .size(unit * (x1 - x0), unit * (y1 - y0))

        Image(
            painter = cachedPainterResource(R.drawable.bg_qm_search),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = box(0f, 0f, ArtW, ArtH)
        )

        // ---- The sign ------------------------------------------------------------------------------------------
        // The workshop picture has no sign of its own (it was painted out): the blank hanging sign is laid on it, and the
        // title is lettered on that.
        Image(
            painter = cachedPainterResource(R.drawable.qm_sign),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = box(60f, 190f, 780f, 555f)
        )
        LetteredText(
            text = stringResource(R.string.quick_match_searching_title),
            size = fs(106f),
            fill = Color.White,
            outline = Color(0xFF5A2815),
            modifier = box(233f, 334f, 630f, 424f),
            minScale = 0.45f,
            title = true
        )
        val second = stringResource(R.string.quick_match_searching_title2).trimEnd('…', '.', ' ')
        Row(
            modifier = box(225f, 408f, 637f, 472f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LetteredText(second, fs(60f), outline = Color(0xFF5A2815), modifier = Modifier.weight(1f, fill = false), minScale = 0.45f, title = true)
            // Three dots that fill in one by one, then start over; their room is always kept so the line never shifts.
            repeat(3) { i ->
                LetteredText(
                    ".",
                    fs(60f),
                    outline = Color(0xFF5A2815),
                    modifier = Modifier.graphicsLayer { alpha = if (dots.value >= i + 1f) 1f else 0.18f },
                    title = true
                )
            }
        }

        // ---- The sheet on the table, and the pencil drawing on it ---------------------------------------------
        Box(
            modifier = box(215f, 1110f, 625f, 1415f)
                .graphicsLayer {
                    rotationZ = -3f + sway.value * 0.6f
                }
                .shadow((10 * us).coerceAtLeast(4f).let { androidx.compose.ui.unit.Dp(it) }, RoundedCornerShape(6))
                .background(Brush.verticalGradient(listOf(Color(0xFFFFFCF2), Color(0xFFF7ECD4))), RoundedCornerShape(6))
                .border(androidx.compose.ui.unit.Dp(1.5f), Color(0xFFE2C89C), RoundedCornerShape(6))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Faint ruled lines, like a page of the sketchbooks the mascots hold.
                val rule = Color(0x22A0784A)
                var y = size.height * 0.16f
                while (y < size.height * 0.95f) {
                    drawLine(rule, Offset(size.width * 0.05f, y), Offset(size.width * 0.95f, y), strokeWidth = size.height * 0.004f)
                    y += size.height * 0.12f
                }
                val c = cycle.value
                val index = c.toInt().coerceIn(0, Doodles.size - 1)
                val local = c - index
                // Drawing takes most of the slot, then the finished doodle rests a moment and fades for the next one.
                val drawn = (local / DRAW_SHARE).coerceIn(0f, 1f)
                val fade = if (local > FADE_FROM) 1f - (local - FADE_FROM) / (1f - FADE_FROM) else 1f
                val doodle = Doodles[index]
                val tip = drawDoodle(doodle, drawn, fade)
                if (drawn < 1f || local < FADE_FROM) drawPencil(tip, size.minDimension * 0.30f, fade)
            }
        }

        // ---- What is going on ----------------------------------------------------------------------------------
        Column(
            modifier = box(70f, 1480f, 770f, 1610f)
                .shadow(androidx.compose.ui.unit.Dp(6f), RoundedCornerShape(50))
                .background(Brush.verticalGradient(listOf(Color(0xFFFFF8E8), Color(0xFFFCE9C8))), RoundedCornerShape(50))
                .border(androidx.compose.ui.unit.Dp(2f), Color(0xFFE9A23B), RoundedCornerShape(50))
                .padding(horizontal = androidx.compose.ui.unit.Dp(34f * us), vertical = androidx.compose.ui.unit.Dp(10f * us)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            FitText(
                text = stringResource(R.string.quick_match_searching_sub),
                style = PaintedStyle(color = Ink, fontSize = fs(26f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                maxLines = 2,
                minScale = 0.6f,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
            FitText(
                text = stringResource(R.string.quick_match_dont_worry_body),
                style = PaintedStyle(color = Color(0xFFB5531A), fontSize = fs(20f), fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center),
                maxLines = 1,
                minScale = 0.6f,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private const val DOODLE_MS = 2600
private const val DRAW_SHARE = 0.72f
private const val FADE_FROM = 0.86f

/** One doodle: its strokes as points in a unit square, and its crayon colour. */
private class Doodle(val strokes: List<List<Offset>>, val color: Color) {
    val lengths: List<Float> = strokes.map { pts -> pts.zipWithNext { a, b -> hypot(b.x - a.x, b.y - a.y) }.sum() }
    val total: Float = lengths.sum()
}

private fun ring(cx: Float, cy: Float, r: Float, from: Float = 0f, turns: Float = 1f, n: Int = 40): List<Offset> =
    (0..n).map { i ->
        val a = from + turns * 2f * PI.toFloat() * i / n
        Offset(cx + r * cos(a), cy + r * sin(a))
    }

private val Doodles: List<Doodle> = listOf(
    // A five-pointed star.
    Doodle(
        listOf((0..10).map { i ->
            val a = -PI.toFloat() / 2f + i * PI.toFloat() / 5f
            val r = if (i % 2 == 0) 0.40f else 0.17f
            Offset(0.5f + r * cos(a), 0.53f + r * sin(a))
        }),
        Color(0xFFF2A100)
    ),
    // A heart.
    Doodle(
        listOf((0..48).map { i ->
            val t = 2f * PI.toFloat() * i / 48
            val x = 16f * sin(t).pow(3)
            val y = 13f * cos(t) - 5f * cos(2 * t) - 2f * cos(3 * t) - cos(4 * t)
            Offset(0.5f + x / 40f, 0.50f - y / 40f)
        }),
        Color(0xFFE5483C)
    ),
    // A little house with a door.
    Doodle(
        listOf(
            listOf(Offset(0.24f, 0.46f), Offset(0.24f, 0.82f), Offset(0.76f, 0.82f), Offset(0.76f, 0.46f)),
            listOf(Offset(0.18f, 0.50f), Offset(0.5f, 0.18f), Offset(0.82f, 0.50f)),
            listOf(Offset(0.44f, 0.82f), Offset(0.44f, 0.62f), Offset(0.56f, 0.62f), Offset(0.56f, 0.82f))
        ),
        Color(0xFF2E86D6)
    ),
    // A sun: a disc and its rays.
    Doodle(
        listOf(ring(0.5f, 0.52f, 0.17f)) + (0 until 8).map { k ->
            val a = k * PI.toFloat() / 4f
            listOf(Offset(0.5f + 0.25f * cos(a), 0.52f + 0.25f * sin(a)), Offset(0.5f + 0.36f * cos(a), 0.52f + 0.36f * sin(a)))
        },
        Color(0xFFF26A1B)
    )
)

/** Draws [fraction] of [doodle] (by length, stroke after stroke) and returns where the pencil's point is now. */
private fun DrawScope.drawDoodle(doodle: Doodle, fraction: Float, alpha: Float): Offset {
    // The doodle sits in a square in the middle of the sheet.
    val side = size.minDimension * 0.86f
    val left = (size.width - side) / 2f
    val top = (size.height - side) / 2f
    fun map(p: Offset) = Offset(left + p.x * side, top + p.y * side)
    var budget = doodle.total * fraction
    var tip = map(doodle.strokes.first().first())
    val style = Stroke(width = side * 0.035f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    doodle.strokes.forEachIndexed { s, pts ->
        if (budget <= 0f) return tip
        val path = Path()
        path.moveTo(map(pts[0]).x, map(pts[0]).y)
        tip = map(pts[0])
        for (i in 1 until pts.size) {
            val a = pts[i - 1]
            val b = pts[i]
            val seg = hypot(b.x - a.x, b.y - a.y)
            if (budget >= seg) {
                budget -= seg
                val m = map(b)
                path.lineTo(m.x, m.y)
                tip = m
            } else {
                val t = if (seg > 0f) budget / seg else 0f
                val m = map(Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t))
                path.lineTo(m.x, m.y)
                tip = m
                budget = 0f
                break
            }
        }
        drawPath(path, doodle.color.copy(alpha = alpha), style = style)
        if (s == doodle.strokes.lastIndex) return tip
    }
    return tip
}

/** A yellow pencil whose graphite point rests on [tip], leaning to the right like a right-handed drawer's. */
private fun DrawScope.drawPencil(tip: Offset, length: Float, alpha: Float) {
    val w = length * 0.16f
    rotate(degrees = -38f, pivot = tip) {
        // Laid out pointing straight down from the tip, before the rotation.
        val coneH = length * 0.2f
        val bodyTop = tip.y - length
        val bodyBottom = tip.y - coneH
        // Body.
        drawRect(Color(0xFFFFC83D).copy(alpha = alpha), Offset(tip.x - w / 2f, bodyTop + length * 0.14f), androidx.compose.ui.geometry.Size(w, bodyBottom - bodyTop - length * 0.14f))
        drawRect(Color(0xFFE59A12).copy(alpha = alpha), Offset(tip.x + w / 6f, bodyTop + length * 0.14f), androidx.compose.ui.geometry.Size(w / 3f, bodyBottom - bodyTop - length * 0.14f))
        // Ferrule and eraser.
        drawRect(Color(0xFFB9BEC6).copy(alpha = alpha), Offset(tip.x - w / 2f, bodyTop + length * 0.07f), androidx.compose.ui.geometry.Size(w, length * 0.07f))
        drawRoundRect(
            Color(0xFFF48FB1).copy(alpha = alpha),
            Offset(tip.x - w / 2f, bodyTop),
            androidx.compose.ui.geometry.Size(w, length * 0.08f),
            androidx.compose.ui.geometry.CornerRadius(w * 0.3f)
        )
        // Sharpened wood and the graphite point.
        val cone = Path().apply {
            moveTo(tip.x - w / 2f, bodyBottom)
            lineTo(tip.x + w / 2f, bodyBottom)
            lineTo(tip.x, tip.y)
            close()
        }
        drawPath(cone, Color(0xFFF5D3A0).copy(alpha = alpha))
        val point = Path().apply {
            moveTo(tip.x - w / 6f, tip.y - coneH / 3f)
            lineTo(tip.x + w / 6f, tip.y - coneH / 3f)
            lineTo(tip.x, tip.y)
            close()
        }
        drawPath(point, Color(0xFF3B2314).copy(alpha = alpha))
        // A thin dark outline round the whole pencil so it reads on the paper.
        val outline = Path().apply {
            moveTo(tip.x, tip.y)
            lineTo(tip.x - w / 2f, bodyBottom)
            lineTo(tip.x - w / 2f, bodyTop + length * 0.04f)
            lineTo(tip.x + w / 2f, bodyTop + length * 0.04f)
            lineTo(tip.x + w / 2f, bodyBottom)
            close()
        }
        drawPath(outline, Color(0xFF5A2815).copy(alpha = alpha * 0.8f), style = Stroke(width = w * 0.08f, join = StrokeJoin.Round))
    }
}
