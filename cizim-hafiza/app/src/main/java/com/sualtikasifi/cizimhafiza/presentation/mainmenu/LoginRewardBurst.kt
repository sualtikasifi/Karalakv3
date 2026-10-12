package com.sualtikasifi.cizimhafiza.presentation.mainmenu

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.JokerType
import androidx.compose.ui.res.painterResource
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val BurstMillis = 1700

/**
 * What plays on the day's card the moment its reward is taken: a light ring and sparkles spread out from the card, then
 * either a shower of gold coins (they leap up, spin and fall, with the amount floating up) or the jokers themselves
 * (each one fans out of the card with its "+1"). Drawn over its parent without clipping, so it spills past the card.
 */
@Composable
internal fun RewardBurst(gold: Int, jokers: List<JokerType>, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(BurstMillis, easing = LinearEasing)) }
    val p = progress.value
    if (p >= 1f) return
    val coin = ImageBitmap.imageResource(R.drawable.icon_gold_coin)
    val rnd = remember { Random(7) }
    // Fixed per burst: where each coin heads, how hard, and how fast it spins.
    val coins = remember(gold) {
        if (gold <= 0) emptyList() else List(20) {
            Coin(
                angle = (-PI * (0.12 + 0.76 * rnd.nextDouble())).toFloat(),
                speed = 0.85f + 0.95f * rnd.nextFloat(),
                spin = 5f + 9f * rnd.nextFloat(),
                size = 0.22f + 0.12f * rnd.nextFloat(),
                delay = 0.06f * rnd.nextFloat()
            )
        }
    }
    val sparks = remember {
        List(12) { i -> Spark(angle = (2 * PI * i / 12 + rnd.nextDouble() * 0.3).toFloat(), reach = 0.55f + 0.5f * rnd.nextFloat(), size = 0.07f + 0.07f * rnd.nextFloat()) }
    }
    BoxWithConstraints(modifier) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val wDp = maxWidth
        val hDp = maxHeight
        Canvas(Modifier.fillMaxSize()) {
            val cx = w / 2f
            val cy = h * 0.46f
            // The ring of light that opens from the middle of the card.
            val ringP = (p / 0.55f).coerceIn(0f, 1f)
            val ease = FastOutSlowInEasing.transform(ringP)
            drawCircle(
                Color(0xFFFFE066).copy(alpha = (1f - ringP) * 0.9f),
                radius = w * (0.15f + 0.85f * ease), center = Offset(cx, cy),
                style = Stroke(width = w * 0.07f * (1f - ringP) + 1f)
            )
            drawCircle(
                Color.White.copy(alpha = (1f - ringP) * 0.55f),
                radius = w * (0.1f + 0.5f * ease), center = Offset(cx, cy)
            )
            // Sparkles, thrown outwards and shrinking away.
            sparks.forEach { s ->
                val sp = ((p - 0.04f) / 0.6f).coerceIn(0f, 1f)
                if (sp > 0f && sp < 1f) {
                    val d = w * s.reach * FastOutSlowInEasing.transform(sp)
                    val a = (1f - sp)
                    drawStar(Offset(cx + cos(s.angle) * d, cy + sin(s.angle) * d), w * s.size * (0.6f + 0.8f * a), Color(0xFFFFF3B0).copy(alpha = a), s.angle * 3f + sp * 90f)
                }
            }
            // The coin shower: a throw upwards, then gravity.
            coins.forEach { c ->
                val t = ((p - c.delay) * BurstMillis / 1000f).coerceAtLeast(0f)
                if (p > c.delay) {
                    val v = w * 0.95f * c.speed
                    val x = cx + cos(c.angle) * v * t
                    val y = cy + sin(c.angle) * v * t + 0.5f * (w * 3.4f) * t * t
                    val fade = if (p > 0.75f) ((1f - p) / 0.25f).coerceIn(0f, 1f) else 1f
                    val s = w * c.size
                    // Spinning about its vertical axis: the coin's width follows |cos|.
                    val flip = abs(cos(c.spin * t)).coerceAtLeast(0.18f)
                    if (y < h * 1.4f) {
                        scale(flip, 1f, pivot = Offset(x, y)) {
                            drawImage(
                                coin,
                                dstOffset = IntOffset((x - s / 2).toInt(), (y - s / 2).toInt()),
                                dstSize = IntSize(s.toInt(), s.toInt()),
                                alpha = fade
                            )
                        }
                    }
                }
            }
        }
        // "+150" rising out of the card.
        if (gold > 0) {
            val tp = ((p - 0.1f) / 0.8f).coerceIn(0f, 1f)
            val a = if (tp < 0.2f) tp / 0.2f else if (tp > 0.7f) (1f - tp) / 0.3f else 1f
            val pop = if (tp < 0.25f) 0.5f + 2f * tp else 1f
            Box(
                Modifier
                    .size(wDp, hDp)
                    .graphicsLayer { alpha = a; translationY = -h * 0.42f * FastOutSlowInEasing.transform(tp); scaleX = pop; scaleY = pop },
                contentAlignment = Alignment.Center
            ) {
                com.sualtikasifi.cizimhafiza.presentation.common.LetteredText(
                    text = "+$gold",
                    size = 26.sp,
                    fill = Color(0xFFFFE066),
                    outline = Color(0xFF5A2815),
                    modifier = Modifier.size(wDp * 1.1f, hDp * 0.3f)
                )
            }
        }
        // The jokers fan out of the card, one after another.
        jokers.forEachIndexed { i, type ->
            val jp = ((p - 0.08f - i * 0.07f) / 0.75f).coerceIn(0f, 1f)
            if (jp > 0f && jp < 1f) {
                val rise = FastOutSlowInEasing.transform(jp)
                val a = if (jp > 0.8f) (1f - jp) / 0.2f else 1f
                val pop = if (jp < 0.3f) 0.2f + 3.2f * jp else 1.16f - 0.16f * ((jp - 0.3f) / 0.7f)
                val spread = (i - (jokers.size - 1) / 2f)
                Box(
                    Modifier
                        .size(wDp, hDp)
                        .graphicsLayer {
                            alpha = a
                            translationX = spread * w * 0.52f * rise
                            translationY = -h * 0.5f * rise
                            rotationZ = spread * 14f * rise
                            scaleX = pop; scaleY = pop
                        },
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        androidx.compose.foundation.Image(
                            painterResource(type.cutoutRes()),
                            contentDescription = null,
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                            modifier = Modifier.size(wDp * 0.42f)
                        )
                        com.sualtikasifi.cizimhafiza.presentation.common.LetteredText(
                            text = "+1",
                            size = 20.sp,
                            fill = Color(0xFFFFE066),
                            outline = Color(0xFF5A2815),
                            modifier = Modifier.size(wDp * 0.3f, hDp * 0.16f)
                        )
                    }
                }
            }
        }
    }
}

private class Coin(val angle: Float, val speed: Float, val spin: Float, val size: Float, val delay: Float)
private class Spark(val angle: Float, val reach: Float, val size: Float)

/** A four-pointed sparkle centred on [c], [r] from the middle to a tip. */
private fun DrawScope.drawStar(c: Offset, r: Float, color: Color, degrees: Float) {
    val path = Path().apply {
        moveTo(c.x, c.y - r)
        quadraticTo(c.x, c.y, c.x + r, c.y)
        quadraticTo(c.x, c.y, c.x, c.y + r)
        quadraticTo(c.x, c.y, c.x - r, c.y)
        quadraticTo(c.x, c.y, c.x, c.y - r)
        close()
    }
    rotate(degrees, pivot = c) { drawPath(path, color) }
}

/** The joker's picture without its tile, so it can fly about as a free-standing sticker. */
private fun JokerType.cutoutRes(): Int = when (this) {
    JokerType.FIRST_LETTER -> R.drawable.ls_joker_first_letter
    JokerType.LETTER_COUNT -> R.drawable.ls_joker_letter_count
    JokerType.EXTRA_TIME -> R.drawable.ls_joker_extra_time
}
