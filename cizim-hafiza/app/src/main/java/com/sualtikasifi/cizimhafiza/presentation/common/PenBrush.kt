package com.sualtikasifi.cizimhafiza.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.StrokeJoin
import com.sualtikasifi.cizimhafiza.domain.model.PenEffect
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
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
 * A premium pen (see [PenEffect]) takes a separate, richer path: layered strokes plus a living effect driven by
 * [timeSec]. Pass 0 for a still picture (thumbnails).
 *
 * Returns the distance drawn so far (start + this stroke's own length), so
 * the next stroke in the same drawing can carry the colour on from there.
 */
fun DrawScope.drawPenStroke(
    points: List<Offset>,
    skin: PenSkin?,
    strokeColor: Color,
    strokeWidthPx: Float,
    startDistance: Float,
    timeSec: Float = 0f
): Float {
    if (points.isEmpty()) return startDistance
    if (skin != null && skin.effect.isPremium) {
        val geometry = buildPenGeometry(points, startDistance) ?: return startDistance
        drawPenGeometry(geometry, skin, strokeWidthPx * PREMIUM_PEN_WIDTH, timeSec)
        return geometry.endDistance
    }
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

// ---------------------------------------------------------------------------------------------------------------
// Premium pens
//
// A stroke is first boiled down to a [PenGeometry]: points every ~10 px along it, and runs of three of them joined
// into short paths ("chunks"). Painting chunk by chunk (one colour each) instead of segment by segment keeps the
// draw count low even for a long drawing, and the geometry of a finished stroke never changes — so DrawableCanvas
// builds it once and only re-colours and re-draws it each frame. The clock ([rememberPenTime]) is the only thing
// that moves.
// ---------------------------------------------------------------------------------------------------------------

/** Premium pens draw a touch thicker than the plain width — enough to read as richer, not enough to hide the drawing. */
internal const val PREMIUM_PEN_WIDTH = 1.15f

private const val SAMPLE_STEP_PX = 10f
private const val CHUNK_SAMPLES = 3
private const val TWO_PI = 6.2831855f

/** Sparks, glints, smoke and bolts are sized from this multiple of the pen width: at the plain 9 px they would be specks. */
private const val EFFECT_SCALE = 2f

/**
 * A clock in seconds that ticks every frame while [enabled]. Read the returned state inside a draw scope (not in
 * composition) so only the draw phase re-runs each frame.
 */
@Composable
fun rememberPenTime(enabled: Boolean): State<Float> {
    val time = remember { mutableFloatStateOf(0f) }
    if (enabled) {
        LaunchedEffect(Unit) {
            val start = withFrameNanos { it }
            while (true) {
                withFrameNanos { now -> time.floatValue = ((now - start) / 1_000_000_000f) % 1800f }
            }
        }
    }
    return time
}

internal class PenGeometry(
    val xs: FloatArray,
    val ys: FloatArray,
    /** Distance drawn up to each sample, including the strokes before this one. */
    val ds: FloatArray,
    /** Unit direction of travel at each sample. */
    val dxs: FloatArray,
    val dys: FloatArray,
    val n: Int,
    val chunks: List<Path>,
    val chunkMid: FloatArray
) {
    val endDistance: Float get() = ds[n - 1]
}

internal fun buildPenGeometry(points: List<Offset>, startDistance: Float): PenGeometry? {
    if (points.isEmpty()) return null
    val m = points.size
    var total = 0f
    for (i in 1 until m) total += hypot(points[i].x - points[i - 1].x, points[i].y - points[i - 1].y)
    // Samples are laid at an even spacing ALONG the stroke (interpolated between the recorded points), so a fast
    // finger that left long gaps between points still gets a continuous run of effects.
    val cap = (total / SAMPLE_STEP_PX).toInt() + 3
    val xs = FloatArray(cap)
    val ys = FloatArray(cap)
    val ds = FloatArray(cap)
    var n = 1
    xs[0] = points[0].x
    ys[0] = points[0].y
    ds[0] = startDistance
    var dist = startDistance
    var sinceLast = 0f
    for (i in 1 until m) {
        val ax = points[i - 1].x
        val ay = points[i - 1].y
        val bx = points[i].x
        val by = points[i].y
        val seg = hypot(bx - ax, by - ay)
        if (seg <= 0f) continue
        var consumed = 0f
        while (sinceLast + (seg - consumed) >= SAMPLE_STEP_PX && n < cap) {
            consumed += SAMPLE_STEP_PX - sinceLast
            val t = consumed / seg
            xs[n] = ax + (bx - ax) * t
            ys[n] = ay + (by - ay) * t
            ds[n] = dist + consumed
            n++
            sinceLast = 0f
        }
        sinceLast += seg - consumed
        dist += seg
    }
    if (sinceLast > 0.5f && n < cap) {
        xs[n] = points[m - 1].x
        ys[n] = points[m - 1].y
        ds[n] = dist
        n++
    }
    val dxs = FloatArray(n)
    val dys = FloatArray(n)
    for (i in 0 until n) {
        val a = maxOf(i - 1, 0)
        val b = min(i + 1, n - 1)
        val vx = xs[b] - xs[a]
        val vy = ys[b] - ys[a]
        val len = hypot(vx, vy)
        if (len > 0f) {
            dxs[i] = vx / len
            dys[i] = vy / len
        } else {
            dxs[i] = 1f
        }
    }
    val chunks = ArrayList<Path>()
    val mids = ArrayList<Float>()
    var g = 0
    while (g < n - 1) {
        val end = min(g + CHUNK_SAMPLES, n - 1)
        val path = Path()
        path.moveTo(xs[g], ys[g])
        for (j in g + 1..end) path.lineTo(xs[j], ys[j])
        chunks.add(path)
        mids.add((ds[g] + ds[end]) / 2f)
        g = end
    }
    return PenGeometry(xs, ys, ds, dxs, dys, n, chunks, mids.toFloatArray())
}

private val colorLists = HashMap<PenSkin, List<Color>>()

private fun PenSkin.colorList(): List<Color> = colorLists.getOrPut(this) { colors.map { Color(it) } }

/** Walks the colours forward then backward (so the loop point blends instead of snapping). */
private fun triangleColor(colors: List<Color>, distancePx: Float): Color {
    if (colors.size == 1) return colors.first()
    val segments = colors.size - 1
    val period = PEN_USAGE_CYCLE_PX * 2f
    val phase = distancePx.mod(period)
    val forward = if (phase <= PEN_USAGE_CYCLE_PX) phase else period - phase
    val t = (forward / PEN_USAGE_CYCLE_PX) * segments
    val index = t.toInt().coerceIn(0, segments - 1)
    return lerp(colors[index], colors[index + 1], t - index)
}

/** Walks the colours round in a ring, last blending back into first — for colours that flow along forever. */
private fun cyclicColor(colors: List<Color>, x: Float): Color {
    val pos = (x - floor(x)) * colors.size
    val i = pos.toInt().coerceIn(0, colors.size - 1)
    return lerp(colors[i], colors[(i + 1) % colors.size], pos - floor(pos))
}

private const val FLOW_PERIOD_PX = 700f
private const val FLOW_SPEED_PX = 260f

private fun premiumColorAt(skin: PenSkin, d: Float, t: Float): Color {
    val colors = skin.colorList()
    var c = when (skin.effect) {
        PenEffect.FLOW -> cyclicColor(colors, (d - t * FLOW_SPEED_PX) / FLOW_PERIOD_PX)
        PenEffect.STARS -> triangleColor(colors, d + t * 90f)
        else -> triangleColor(colors, d)
    }
    when (skin.effect) {
        PenEffect.GLINT, PenEffect.ROYAL -> {
            // A bright band 140 px wide, sliding along the stroke every 480 px.
            val period = 480f
            val x = (d - t * 380f).mod(period)
            val k = (1f - abs(x - period / 2f) / 70f).coerceAtLeast(0f)
            c = lerp(c, Color.White, k * k * 0.8f)
        }
        PenEffect.EMBERS, PenEffect.BOLTS -> {
            val flicker = (0.08f * (1f + sin(t * 9f + d * 0.06f))).coerceIn(0f, 0.2f)
            c = lerp(c, Color.White, flicker)
        }
        else -> Unit
    }
    return c
}

internal fun DrawScope.drawPenGeometry(g: PenGeometry, skin: PenSkin, w: Float, time: Float) {
    val count = g.chunks.size
    if (count == 0) {
        // A single tap: a dot with the same layers.
        val c = premiumColorAt(skin, g.ds[0], time)
        val centre = Offset(g.xs[0], g.ys[0])
        drawCircle(c.copy(alpha = 0.22f), radius = w * 1.15f, center = centre)
        drawCircle(lerp(c, Color.Black, 0.35f).copy(alpha = 0.6f), radius = w * 0.62f, center = centre)
        drawCircle(c, radius = w * 0.5f, center = centre)
        drawCircle(lerp(c, Color.White, 0.6f).copy(alpha = 0.8f), radius = w * 0.16f, center = centre)
        drawPenOverlays(g, skin.effect, w * EFFECT_SCALE, time)
        return
    }
    val colors = Array(count) { premiumColorAt(skin, g.chunkMid[it], time) }
    val glow = Stroke(width = w * 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val rim = Stroke(width = w + maxOf(2f, w * 0.28f), cap = StrokeCap.Round, join = StrokeJoin.Round)
    val core = Stroke(width = w, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val shine = Stroke(width = w * 0.3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    // One pass per layer over the whole stroke, so a later chunk's halo never washes over an earlier chunk's core.
    for (i in 0 until count) drawPath(g.chunks[i], colors[i].copy(alpha = 0.2f), style = glow)
    for (i in 0 until count) drawPath(g.chunks[i], lerp(colors[i], Color.Black, 0.4f).copy(alpha = 0.7f), style = rim)
    for (i in 0 until count) drawPath(g.chunks[i], colors[i], style = core)
    for (i in 0 until count) drawPath(g.chunks[i], lerp(colors[i], Color.White, 0.6f).copy(alpha = 0.8f), style = shine)
    drawPenOverlays(g, skin.effect, w * EFFECT_SCALE, time)
}

/** A small, repeatable pseudo-random number in 0..1 for (i, k) — the same on every frame and every device. */
private fun hash01(i: Int, k: Int): Float {
    var x = i * 374761393 + k * 668265263
    x = (x xor (x ushr 13)) * 1274126177
    x = x xor (x ushr 16)
    return (x and 0xFFFF) / 65535f
}

private fun fract(x: Float): Float = x - floor(x)

private val EmberYellow = Color(0xFFFFE082)
private val EmberOrange = Color(0xFFFF7A18)
private val EmberRed = Color(0xFFC62828)
private val GoldLight = Color(0xFFFFE9A0)
private val GoldDeep = Color(0xFFE0922B)
private val SmokeLight = Color(0xFFB44CFF)
private val SmokeDark = Color(0xFF2A0F55)
private val BoltWhite = Color(0xFFEAFBFF)
private val BoltCyan = Color(0xFF4FC3F7)

private fun DrawScope.drawPenOverlays(g: PenGeometry, fx: PenEffect, w: Float, time: Float) {
    when (fx) {
        PenEffect.GLINT -> sparkles(g, w, time, every = 4, salt = 11)
        PenEffect.STARS -> sparkles(g, w, time, every = 3, salt = 23)
        PenEffect.EMBERS -> embers(g, w, time, density = 0.6f, gold = false)
        PenEffect.ROYAL -> {
            embers(g, w, time, density = 0.35f, gold = true)
            sparkles(g, w, time, every = 6, salt = 37)
        }
        PenEffect.SMOKE -> smoke(g, w, time)
        PenEffect.BOLTS -> bolts(g, w, time)
        else -> Unit
    }
}

/** Four-point glints that twinkle beside the stroke. */
private fun DrawScope.sparkles(g: PenGeometry, w: Float, time: Float, every: Int, salt: Int) {
    var i = 0
    while (i < g.n) {
        val h = hash01(i, salt)
        val tw = 0.5f + 0.5f * sin(time * (2.2f + h * 2f) + h * TWO_PI * 3f)
        if (tw >= 0.2f) {
            val side = if (hash01(i, salt + 1) < 0.5f) 1f else -1f
            val off = w * (0.7f + 0.9f * hash01(i, salt + 2)) * side
            val c = Offset(g.xs[i] - g.dys[i] * off, g.ys[i] + g.dxs[i] * off)
            val r = w * (0.7f + 0.7f * hash01(i, salt + 3)) * tw
            drawCircle(Color.White.copy(alpha = 0.3f * tw), radius = r * 0.9f, center = c)
            drawSparkle(c, r, tw)
        }
        i += every
    }
}

private fun DrawScope.drawSparkle(center: Offset, r: Float, alpha: Float) {
    // A four-pointed star with concave sides: four quadratic curves that pinch in towards the middle.
    val k = r * 0.14f
    val star = Path().apply {
        moveTo(center.x, center.y - r)
        quadraticBezierTo(center.x + k, center.y - k, center.x + r, center.y)
        quadraticBezierTo(center.x + k, center.y + k, center.x, center.y + r)
        quadraticBezierTo(center.x - k, center.y + k, center.x - r, center.y)
        quadraticBezierTo(center.x - k, center.y - k, center.x, center.y - r)
        close()
    }
    drawPath(star, Color.White.copy(alpha = alpha.coerceIn(0f, 1f)))
}

/** Tiny sparks thrown off the stroke: they rise, shrink, cool from yellow to red and vanish. */
private fun DrawScope.embers(g: PenGeometry, w: Float, time: Float, density: Float, gold: Boolean) {
    val hot = if (gold) GoldLight else EmberYellow
    val mid = if (gold) GoldDeep else EmberOrange
    val cold = if (gold) EmberOrange else EmberRed
    for (i in 0 until g.n) {
        if (hash01(i, 1) > density) continue
        val life = 0.85f + 0.5f * hash01(i, 2)
        val u = fract(time / life + hash01(i, 3))
        val x0 = g.xs[i] + (hash01(i, 4) - 0.5f) * w * 0.9f + sin(u * 7f + hash01(i, 5) * 6f) * w * 0.35f * u
        val y0 = g.ys[i] - u * w * (2.6f + 1.6f * hash01(i, 6))
        val r = maxOf(1f, w * 0.26f * (1f - 0.7f * u) * (0.7f + 0.6f * hash01(i, 7)))
        val color = if (u < 0.5f) lerp(hot, mid, u * 2f) else lerp(mid, cold, (u - 0.5f) * 2f)
        drawLine(
            color.copy(alpha = ((1f - u) * 1.1f).coerceIn(0f, 1f)),
            Offset(x0, y0), Offset(x0, y0 + r * 2.4f), r * 1.6f, StrokeCap.Round
        )
    }
}

/** Soft purple puffs that curl up from the stroke, growing and fading. */
private fun DrawScope.smoke(g: PenGeometry, w: Float, time: Float) {
    var i = 0
    while (i < g.n) {
        if (hash01(i, 41) <= 0.7f) {
            val life = 1.8f + 1f * hash01(i, 42)
            val u = fract(time / life + hash01(i, 43))
            val x0 = g.xs[i] + sin(u * 3.5f + hash01(i, 44) * 6f) * w * 0.9f * u
            val y0 = g.ys[i] - u * w * 4f
            val alpha = 0.26f * (1f - u) * minOf(1f, u * 8f)
            // Three stacked discs of shrinking size fake a soft edge without a shader per puff.
            val colour = lerp(SmokeLight, SmokeDark, u)
            val radius = w * (0.55f + u)
            val centre = Offset(x0, y0)
            drawCircle(colour.copy(alpha = alpha * 0.45f), radius = radius, center = centre)
            drawCircle(colour.copy(alpha = alpha * 0.45f), radius = radius * 0.68f, center = centre)
            drawCircle(colour.copy(alpha = alpha * 0.45f), radius = radius * 0.38f, center = centre)
        }
        i += 2
    }
}

/** Short lightning cracks that flash in and out along the stroke. */
private fun DrawScope.bolts(g: PenGeometry, w: Float, time: Float) {
    var i = 0
    while (i < g.n) {
        val slot = floor(time * 5f + hash01(i, 31) * 10f).toInt()
        if (hash01(i * 7 + slot, 32) <= 0.3f) {
            val ang = hash01(i * 13 + slot, 33) * TWO_PI
            val len = w * (2.2f + 1.8f * hash01(i + slot, 34))
            val dx = cos(ang)
            val dy = sin(ang)
            val px = -dy
            val py = dx
            val p0 = Offset(g.xs[i], g.ys[i])
            val p1 = Offset(p0.x + dx * len * 0.35f + px * len * 0.22f, p0.y + dy * len * 0.35f + py * len * 0.22f)
            val p2 = Offset(p1.x + dx * len * 0.3f - px * len * 0.4f, p1.y + dy * len * 0.3f - py * len * 0.4f)
            val p3 = Offset(p2.x + dx * len * 0.35f + px * len * 0.25f, p2.y + dy * len * 0.35f + py * len * 0.25f)
            val thick = maxOf(1.5f, w * 0.16f)
            for ((a, b) in listOf(p0 to p1, p1 to p2, p2 to p3)) {
                drawLine(BoltCyan.copy(alpha = 0.35f), a, b, thick * 3f, StrokeCap.Round)
                drawLine(BoltWhite, a, b, thick, StrokeCap.Round)
            }
        }
        i += 4
    }
}
