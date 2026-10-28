package com.sualtikasifi.cizimhafiza.presentation.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/*
 * The game's shared motion vocabulary. Every screen draws from these few pieces instead of inventing its own, so
 * movement feels like one hand made it: things SPRING in (never fade flatly), controls DIP when pressed, calls to
 * action BREATHE and GLINT, and numbers COUNT rather than jump.
 */

/**
 * Springs the element in on first composition: it rises, grows from 88% and fades up, [index] * [stepMs] after the
 * first of its siblings, so a list or a grid cascades instead of appearing at once. Plays once per composition
 * (scrolling a lazy list back over an item does not replay it, because the state is remembered per item).
 */
fun Modifier.springIn(index: Int = 0, stepMs: Int = 55, fromY: Int = 28, key: Any? = Unit): Modifier = composed {
    val progress = remember(key) { Animatable(0f) }
    LaunchedEffect(key) {
        delay((index * stepMs).toLong())
        progress.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow))
    }
    graphicsLayer {
        val p = progress.value
        alpha = (p * 1.6f).coerceIn(0f, 1f)
        translationY = (1f - p) * fromY.dp.toPx()
        val sc = 0.88f + 0.12f * p
        scaleX = sc
        scaleY = sc
    }
}

/** A tappable element that dips under the finger and springs back, with no ripple (the art is the feedback). */
fun Modifier.pressable(
    enabled: Boolean = true,
    pressedScale: Float = 0.94f,
    onClick: () -> Unit
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        label = "press"
    )
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(enabled = enabled, interactionSource = source, indication = null, onClick = onClick)
}

/** A slow, gentle bob and tilt — for a mascot, a crown, a badge sitting idle. */
fun Modifier.floating(amplitude: Float = 5f, tilt: Float = 2f, periodMs: Int = 2600): Modifier = composed {
    val t by rememberInfiniteTransition(label = "float").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(periodMs, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "floatT"
    )
    graphicsLayer {
        val s = t * 2f - 1f
        translationY = s * amplitude.dp.toPx()
        rotationZ = s * tilt
    }
}

/** A slow breathing scale, for a call to action that should draw the eye without shouting. */
fun Modifier.breathing(amount: Float = 0.035f, periodMs: Int = 1500): Modifier = composed {
    val t by rememberInfiniteTransition(label = "breath").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(periodMs, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathT"
    )
    graphicsLayer {
        val sc = 1f + amount * t
        scaleX = sc
        scaleY = sc
    }
}

/**
 * A band of light that glides across the element every [periodMs] and rests in between — the glint on a glossy
 * button. Drawn over the content, clipped to the element's bounds.
 */
fun Modifier.glint(periodMs: Int = 2800, strength: Float = 0.5f, corner: androidx.compose.ui.unit.Dp = 0.dp): Modifier = composed {
    val phase by rememberInfiniteTransition(label = "glint").animateFloat(
        initialValue = -0.35f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(periodMs, easing = LinearEasing)),
        label = "glintX"
    )
    drawWithContent {
        drawContent()
        if (phase in -0.3f..1.3f) {
            val x = size.width * phase
            val band = size.width * 0.16f
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = strength), Color.Transparent),
                    start = Offset(x - band, 0f),
                    end = Offset(x + band, size.height * 0.45f)
                ),
                cornerRadius = CornerRadius(corner.toPx())
            )
        }
    }
}

/**
 * A number that counts up (or down) to [value] instead of jumping — gold, XP, scores. [format] turns the animated
 * integer into the shown text; the first composition counts up from 0 when [fromZero], otherwise it starts at [value].
 */
@Composable
fun CountUpText(
    value: Int,
    style: TextStyle,
    modifier: Modifier = Modifier,
    durationMs: Int = 900,
    fromZero: Boolean = true,
    format: (Int) -> String = { it.toString() }
) {
    val shown = remember { Animatable(if (fromZero) 0f else value.toFloat()) }
    LaunchedEffect(value) { shown.animateTo(value.toFloat(), tween(durationMs, easing = FastOutSlowInEasing)) }
    Text(text = format(shown.value.toInt()), style = style, modifier = modifier)
}

/** A one-shot pop: scales 0 → overshoot → 1 when [trigger] changes to true — a tick, a star, a badge appearing. */
fun Modifier.popIn(trigger: Boolean = true, delayMs: Int = 0): Modifier = composed {
    val scale = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger) {
            delay(delayMs.toLong())
            scale.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessLow))
        } else {
            scale.snapTo(0f)
        }
    }
    graphicsLayer { scaleX = scale.value; scaleY = scale.value; alpha = scale.value.coerceIn(0f, 1f) }
}

private class Confetto(val x: Float, val delay: Float, val drift: Float, val speed: Float, val size: Float, val spin: Float, val color: Color, val round: Boolean)

/**
 * A burst of coloured paper that falls once over the whole area and then stops. Used for a win and for a finished
 * daily challenge. Draws nothing once finished, so it can stay in the tree.
 */
@Composable
fun ConfettiBurst(modifier: Modifier = Modifier, count: Int = 70, durationMs: Int = 2600, seed: Int = 7) {
    val palette = remember {
        listOf(Color(0xFFFF5E5E), Color(0xFFFFC93C), Color(0xFF4FD08A), Color(0xFF5EB6FF), Color(0xFFB78BFF), Color(0xFFFF8FB8), Color(0xFFFF9A3C))
    }
    val pieces = remember(seed) {
        val r = Random(seed)
        List(count) {
            Confetto(
                x = r.nextFloat(), delay = r.nextFloat() * 0.35f, drift = (r.nextFloat() - 0.5f) * 0.25f,
                speed = 0.75f + r.nextFloat() * 0.5f, size = 6f + r.nextFloat() * 8f, spin = (r.nextFloat() - 0.5f) * 14f,
                color = palette[r.nextInt(palette.size)], round = r.nextFloat() < 0.3f
            )
        }
    }
    val t = remember(seed) { Animatable(0f) }
    LaunchedEffect(seed) { t.animateTo(1f, tween(durationMs, easing = LinearEasing)) }
    if (t.value >= 1f) return
    Canvas(modifier = modifier.fillMaxSize()) {
        pieces.forEach { c ->
            val local = ((t.value - c.delay) / (1f - c.delay)).coerceIn(0f, 1f)
            if (local <= 0f) return@forEach
            // Shot up first, then gravity takes it: a quick arc rather than a plain fall.
            val rise = sin(local * PI.toFloat() * 0.5f) * 0.12f
            val fall = local * local * c.speed * 1.25f
            val y = size.height * (0.05f - rise + fall)
            val x = size.width * (c.x + c.drift * local) + sin(local * 9f + c.x * 20f) * 14f
            val alpha = (1f - ((local - 0.75f) / 0.25f).coerceIn(0f, 1f))
            val px = c.size.dp.toPx()
            if (c.round) {
                drawCircle(c.color.copy(alpha = alpha), radius = px * 0.5f, center = Offset(x, y))
            } else {
                rotate(local * c.spin * 57f, Offset(x, y)) {
                    drawRect(c.color.copy(alpha = alpha), topLeft = Offset(x - px / 2f, y - px * 0.3f), size = androidx.compose.ui.geometry.Size(px, px * 0.6f))
                }
            }
        }
    }
}

/** One sparkle's place in a [SparkleField]: a point in 0..1 of the field, and its size as a fraction of the field's width. */
class SparkleSpot(val x: Float, val y: Float, val size: Float)

/**
 * Four-pointed stars that twinkle in and out round a picture, each on its own phase, turning as they go — the glitter
 * that makes a reward look worth reaching for. Draws only while [active]; otherwise nothing is on screen.
 */
@Composable
fun SparkleField(
    spots: List<SparkleSpot>,
    modifier: Modifier = Modifier,
    active: Boolean = true,
    color: Color = Color(0xFFFFF3B0),
    periodMs: Int = 1900
) {
    if (!active) return
    val t by rememberInfiniteTransition(label = "sparkles").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(periodMs, easing = LinearEasing)),
        label = "sparkleT"
    )
    Canvas(modifier = modifier) {
        spots.forEachIndexed { i, spot ->
            val phase = (t + i * 0.27f) % 1f
            // Quick bloom, slow fade: it flashes rather than pulses.
            val k = if (phase < 0.25f) phase / 0.25f else (1f - (phase - 0.25f) / 0.75f)
            if (k <= 0.02f) return@forEachIndexed
            val r = spot.size * size.width * (0.35f + 0.65f * k)
            val c = Offset(spot.x * size.width, spot.y * size.height)
            val turn = phase * 90f
            rotate(turn, c) {
                val p = androidx.compose.ui.graphics.Path().apply {
                    moveTo(c.x, c.y - r)
                    quadraticTo(c.x, c.y, c.x + r, c.y)
                    quadraticTo(c.x, c.y, c.x, c.y + r)
                    quadraticTo(c.x, c.y, c.x - r, c.y)
                    quadraticTo(c.x, c.y, c.x, c.y - r)
                    close()
                }
                drawPath(p, color.copy(alpha = 0.95f * k))
            }
            drawCircle(Color.White.copy(alpha = 0.55f * k), radius = r * 0.22f, center = c)
        }
    }
}


private class Shard(val angle: Float, val power: Float, val size: Float, val spin: Float, val lift: Float, val color: Color, val round: Boolean)

/**
 * Coloured paper that bursts outward from one point ([originX]/[originY] are fractions of the area) like a popper, slows
 * down in the air, then drifts down a little while it fades out. Draws nothing once finished.
 */
@Composable
fun ConfettiExplosion(
    modifier: Modifier = Modifier,
    count: Int = 90,
    durationMs: Int = 1700,
    seed: Int = 11,
    originX: Float = 0.5f,
    originY: Float = 0.45f,
    startDelayMs: Int = 0
) {
    val palette = remember {
        listOf(Color(0xFFFF5E5E), Color(0xFFFFC93C), Color(0xFF4FD08A), Color(0xFF5EB6FF), Color(0xFFB78BFF), Color(0xFFFF8FB8), Color(0xFFFF9A3C))
    }
    val shards = remember(seed) {
        val r = Random(seed)
        List(count) {
            Shard(
                angle = r.nextFloat() * 2f * PI.toFloat(),
                power = 0.25f + r.nextFloat() * 0.75f,
                size = 6f + r.nextFloat() * 8f,
                spin = (r.nextFloat() - 0.5f) * 16f,
                lift = r.nextFloat() * 0.12f,
                color = palette[r.nextInt(palette.size)],
                round = r.nextFloat() < 0.3f
            )
        }
    }
    val t = remember(seed) { Animatable(0f) }
    LaunchedEffect(seed) {
        delay(startDelayMs.toLong())
        t.animateTo(1f, tween(durationMs, easing = LinearEasing))
    }
    if (t.value <= 0f || t.value >= 1f) return
    Canvas(modifier = modifier.fillMaxSize()) {
        val ox = size.width * originX
        val oy = size.height * originY
        val reach = size.minDimension * 0.62f
        shards.forEach { c ->
            val local = t.value
            // Fast out of the origin, braking hard: 1 - (1 - t)^3.
            val out = 1f - (1f - local) * (1f - local) * (1f - local)
            val dist = reach * c.power * out
            val x = ox + cos(c.angle) * dist + sin(local * 7f + c.angle * 5f) * 6f
            // Then gravity pulls it down while it fades.
            val y = oy + sin(c.angle) * dist - size.height * c.lift * out + size.height * 0.22f * local * local
            val alpha = (1f - ((local - 0.6f) / 0.4f).coerceIn(0f, 1f))
            val px = c.size.dp.toPx()
            if (c.round) {
                drawCircle(c.color.copy(alpha = alpha), radius = px * 0.5f, center = Offset(x, y))
            } else {
                rotate(local * c.spin * 57f, Offset(x, y)) {
                    drawRect(c.color.copy(alpha = alpha), topLeft = Offset(x - px / 2f, y - px * 0.3f), size = androidx.compose.ui.geometry.Size(px, px * 0.6f))
                }
            }
        }
    }
}
