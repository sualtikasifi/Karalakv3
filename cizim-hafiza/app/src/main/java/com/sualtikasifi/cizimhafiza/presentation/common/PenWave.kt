package com.sualtikasifi.cizimhafiza.presentation.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import com.sualtikasifi.cizimhafiza.domain.model.PenSkin
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

/**
 * A wavy stroke in the pen's colours, [strokeWidth] thick, with a soft shadow under it — what the pen actually draws.
 * A premium pen is drawn by the real pen renderer, so its effects play live. The store and the pen picker share it.
 */
@Composable
fun PenWave(skin: PenSkin, strokeWidth: Dp, modifier: Modifier = Modifier) {
    val colors = skin.colors.map { Color(it) }
    val premium = skin.effect.isPremium
    val time = rememberPenTime(premium)
    Canvas(modifier = modifier) {
        val path = Path().apply {
            moveTo(size.width * 0.08f, size.height * 0.62f)
            cubicTo(
                size.width * 0.28f, size.height * 0.05f,
                size.width * 0.42f, size.height * 1.05f,
                size.width * 0.58f, size.height * 0.45f
            )
            cubicTo(
                size.width * 0.70f, size.height * 0.05f,
                size.width * 0.82f, size.height * 0.55f,
                size.width * 0.92f, size.height * 0.30f
            )
        }
        val w = strokeWidth.toPx()
        if (premium) {
            // The real pen renderer, fed the same wave as points.
            val measure = PathMeasure().apply { setPath(path, false) }
            val points = (0..80).map { measure.getPosition(measure.length * it / 80f) }
            drawPenStroke(points, skin, Color.Black, w / 1.15f, 0f, time.value)
            return@Canvas
        }
        drawPath(path, Color.Black.copy(alpha = 0.18f), style = Stroke(width = w + w * 0.3f, cap = StrokeCap.Round))
        drawPath(
            path,
            brush = Brush.horizontalGradient(if (colors.size > 1) colors else listOf(colors.first(), colors.first())),
            style = Stroke(width = w, cap = StrokeCap.Round)
        )
    }
}

/**
 * The pen as a brush swash, a simpler take on the store cards' painted stroke: one sweep that starts thin, swells and
 * curls up thin again at the end, in the pen's colours, with a soft shadow under it and a glossy highlight along its top.
 * A premium pen ([PenEffect.isPremium][com.sualtikasifi.cizimhafiza.domain.model.PenEffect.isPremium]) has a few stars
 * twinkling round it. Static otherwise, so a whole grid of them costs nothing per frame.
 */
@Composable
fun PenSwoosh(skin: PenSkin, modifier: Modifier = Modifier) {
    val colors = remember(skin) { skin.colors.map { Color(it) }.let { if (it.size > 1) it else listOf(it.first(), it.first()) } }
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val path = Path().apply {
                moveTo(size.width * 0.07f, size.height * 0.78f)
                cubicTo(
                    size.width * 0.30f, size.height * 0.10f,
                    size.width * 0.48f, size.height * 0.95f,
                    size.width * 0.70f, size.height * 0.48f
                )
                cubicTo(
                    size.width * 0.80f, size.height * 0.26f,
                    size.width * 0.88f, size.height * 0.14f,
                    size.width * 0.95f, size.height * 0.22f
                )
            }
            val measure = PathMeasure().apply { setPath(path, false) }
            val thick = size.height * 0.42f
            val outline = swash(measure, thick, 0f)
            // a soft shadow a little below, then the stroke, then a thin light streak along its upper edge
            translate(top = size.height * 0.06f) { drawPath(outline, Color.Black.copy(alpha = 0.16f)) }
            drawPath(outline, Brush.horizontalGradient(colors, startX = size.width * 0.07f, endX = size.width * 0.95f))
            drawPath(swash(measure, thick * 0.32f, -thick * 0.18f), Color.White.copy(alpha = 0.38f))
        }
        if (skin.effect.isPremium) {
            SparkleField(
                spots = PREMIUM_SPARKLES,
                modifier = Modifier.fillMaxSize(),
                color = lerp(colors.first(), Color.White, 0.25f),
                periodMs = 2200
            )
        }
    }
}

/**
 * The outline of a brush swash along [measure]: [thick] at its widest, thin at both ends (a quicker swell than taper,
 * as a brush lands then lifts), its middle line moved [shift] across the stroke (negative: towards its upper edge).
 */
private fun swash(measure: PathMeasure, thick: Float, shift: Float): Path {
    val n = 48
    val left = ArrayList<Offset>(n + 1)
    val right = ArrayList<Offset>(n + 1)
    for (i in 0..n) {
        val t = i / n.toFloat()
        val d = measure.length * t
        val p = measure.getPosition(d)
        val tan = measure.getTangent(d)
        val normal = Offset(-tan.y, tan.x)
        val half = thick / 2f * sin(PI * t.toDouble().pow(0.8)).toFloat().coerceAtLeast(0.06f)
        val c = p + normal * shift
        left += c + normal * half
        right += c - normal * half
    }
    return Path().apply {
        moveTo(left.first().x, left.first().y)
        left.drop(1).forEach { lineTo(it.x, it.y) }
        right.asReversed().forEach { lineTo(it.x, it.y) }
        close()
    }
}

private fun lerp(a: Color, b: Color, f: Float) = androidx.compose.ui.graphics.lerp(a, b, f)

private val PREMIUM_SPARKLES = listOf(
    SparkleSpot(0.20f, 0.20f, 0.045f),
    SparkleSpot(0.55f, 0.88f, 0.035f),
    SparkleSpot(0.86f, 0.62f, 0.05f)
)
