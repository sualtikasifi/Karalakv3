package com.sualtikasifi.cizimhafiza.presentation.game

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/** One spark: where on the frame it was born (0..1 around it), how it drifts, and how long it lives. */
private class Spark(
    val along: Float,
    val inset: Float,
    val speed: Float,
    val drift: Float,
    val size: Float,
    val life: Float,
    val spin: Float,
    val color: Color
) {
    var age = 0f
}

private val SparkColors = listOf(
    Color(0xFFFFF4C2), Color(0xFFFFD84D), Color(0xFFFFB21E), Color(0xFFFFFFFF), Color(0xFFFF8A3D)
)

/**
 * The last seconds of a drawing turn: sparks fly off the edges of the drawing area, a few at first and then faster and
 * faster (the rate grows exponentially), so running out of time is felt rather than read. Draw it over the drawing
 * area at the same size; the sparks spill outside it.
 */
@Composable
internal fun EdgeSparkles(modifier: Modifier = Modifier) {
    val sparks = remember { ArrayList<Spark>() }
    var frame by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        val rnd = Random(System.nanoTime())
        val start = withFrameNanos { it }
        var last = start
        var due = 0f
        while (true) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1e9f).coerceIn(0f, 0.05f)
            last = now
            val t = (now - start) / 1e9f
            // About 8 a second at first, ~22 after one second, ~60 after two, ~160 at the end.
            due += 8f * exp(t) * dt
            while (due >= 1f && sparks.size < 420) {
                due -= 1f
                val late = (t / 3f).coerceIn(0f, 1f)
                sparks += Spark(
                    along = rnd.nextFloat(),
                    inset = rnd.nextFloat() * 10f - 4f,
                    speed = 26f + rnd.nextFloat() * 50f + 40f * late,
                    drift = rnd.nextFloat() * 36f - 18f,
                    size = 4f + rnd.nextFloat() * 6f + 3f * late,
                    life = 0.45f + rnd.nextFloat() * 0.5f,
                    spin = rnd.nextFloat() * 6f - 3f,
                    color = SparkColors[rnd.nextInt(SparkColors.size)]
                )
            }
            if (due > 1f) due = 0f
            val it = sparks.iterator()
            while (it.hasNext()) {
                val s = it.next()
                s.age += dt
                if (s.age >= s.life) it.remove()
            }
            frame = now
        }
    }
    val star = remember { Path() }
    Canvas(modifier) {
        frame // redraw every frame
        val unit = 1.dp.toPx()
        sparks.forEach { s -> drawSpark(s, unit, star) }
    }
}

private fun DrawScope.drawSpark(s: Spark, unit: Float, star: Path) {
    val w = size.width
    val h = size.height
    // Walk around the frame: top, right, bottom, left, in proportion to each side's length.
    val per = 2f * (w + h)
    var d = s.along * per
    val base: Offset
    val normal: Offset
    when {
        d < w -> { base = Offset(d, 0f); normal = Offset(0f, -1f) }
        d < w + h -> { d -= w; base = Offset(w, d); normal = Offset(1f, 0f) }
        d < 2f * w + h -> { d -= w + h; base = Offset(w - d, h); normal = Offset(0f, 1f) }
        else -> { d -= 2f * w + h; base = Offset(0f, h - d); normal = Offset(-1f, 0f) }
    }
    val tangent = Offset(-normal.y, normal.x)
    val k = s.age / s.life
    val out = (s.inset + s.speed * s.age) * unit
    val p = base + normal * out + tangent * (s.drift * s.age * unit)
    // Grows in, then twinkles out.
    val twinkle = sin(PI * k).toFloat()
    val r = s.size * unit * twinkle
    if (r <= 0.3f) return
    val alpha = (1f - k * k).coerceIn(0f, 1f)
    drawCircle(s.color.copy(alpha = 0.28f * alpha), radius = r * 1.5f, center = p)
    // A four-pointed star, slowly turning.
    val a0 = s.spin * s.age
    star.rewind()
    for (i in 0 until 8) {
        val ang = a0 + i * (PI / 4).toFloat()
        val rr = if (i % 2 == 0) r else r * 0.28f
        val x = p.x + rr * kotlin.math.cos(ang)
        val y = p.y + rr * kotlin.math.sin(ang)
        if (i == 0) star.moveTo(x, y) else star.lineTo(x, y)
    }
    star.close()
    drawPath(star, s.color.copy(alpha = alpha))
    drawCircle(Color.White.copy(alpha = alpha), radius = r * 0.22f, center = p)
}
