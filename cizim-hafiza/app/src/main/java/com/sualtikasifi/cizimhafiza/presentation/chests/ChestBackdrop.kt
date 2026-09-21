package com.sualtikasifi.cizimhafiza.presentation.chests

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.ChestTier
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Text colour that reads on this tier's backdrop. */
fun ChestTier.onBackdrop(): Color = when (this) {
    ChestTier.SILVER -> Color(0xFF3A2416)
    ChestTier.GOLD -> Color(0xFF4A2F00)
    ChestTier.RARE -> Color.White
}

fun ChestTier.borderColor(): Color = when (this) {
    ChestTier.SILVER -> Color(0xFFD7B48A)
    ChestTier.GOLD -> Color(0xFFE5A800)
    ChestTier.RARE -> Color(0xFFE070FF)
}

/** 1 = common, 3 = legendary — drives the star row on the win screen. */
fun ChestTier.rarityStars(): Int = when (this) {
    ChestTier.SILVER -> 1
    ChestTier.GOLD -> 2
    ChestTier.RARE -> 3
}

fun ChestTier.rarityLabelRes(): Int = when (this) {
    ChestTier.SILVER -> R.string.rarity_common
    ChestTier.GOLD -> R.string.rarity_rare
    ChestTier.RARE -> R.string.rarity_legendary
}

private class Twinkle(val x: Float, val y: Float, val size: Float, val offset: Float)

private fun twinkles(count: Int, seed: Int): List<Twinkle> {
    val rnd = Random(seed)
    return List(count) { Twinkle(rnd.nextFloat(), rnd.nextFloat(), 0.5f + rnd.nextFloat() * 0.9f, rnd.nextFloat()) }
}

/** A 4-point sparkle star. */
fun DrawScope.sparkle(center: Offset, radius: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        quadraticTo(center.x, center.y, center.x + radius, center.y)
        quadraticTo(center.x, center.y, center.x, center.y + radius)
        quadraticTo(center.x, center.y, center.x - radius, center.y)
        quadraticTo(center.x, center.y, center.x, center.y - radius)
        close()
    }
    drawPath(path, color)
}

/**
 * The background a chest sits on, by rarity — the quality of the chest reads
 * before the name does:
 *  - Çırak (common): a plain, warm paper card.
 *  - Sanatçı (rare): a rich gold panel with a sun glow, twinkling sparkles and a shimmer sweep.
 *  - Sürpriz (legendary): a deep cosmic panel — rotating light rays, a bright core and a field of twinkling stars.
 */
@Composable
fun ChestBackdrop(tier: ChestTier, modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "chestBackdrop")
    val sweep = infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3400, easing = LinearEasing), RepeatMode.Restart),
        label = "sweep"
    )
    val spin = infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(16_000, easing = LinearEasing), RepeatMode.Restart),
        label = "spin"
    )
    val pulse = infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val stars = remember(tier) { twinkles(if (tier == ChestTier.RARE) 16 else 7, tier.ordinal * 97 + 3) }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val glowCenter = Offset(w / 2f, h * 0.38f)
        when (tier) {
            ChestTier.SILVER -> {
                drawRect(Brush.verticalGradient(listOf(Color(0xFFFFF4E2), Color(0xFFEBD3AE))))
                drawCircle(
                    Brush.radialGradient(listOf(Color.White.copy(alpha = 0.55f), Color.Transparent), glowCenter, w * 0.6f),
                    radius = w * 0.6f, center = glowCenter
                )
            }
            ChestTier.GOLD -> {
                drawRect(Brush.verticalGradient(listOf(Color(0xFFFFF5C9), Color(0xFFFFD25E), Color(0xFFFFB92E))))
                drawCircle(
                    Brush.radialGradient(listOf(Color.White.copy(alpha = 0.85f), Color(0x00FFF3B0)), glowCenter, w * 0.75f),
                    radius = w * 0.75f, center = glowCenter
                )
                // Shimmer band sweeping diagonally.
                val bandX = (-0.4f + 1.8f * sweep.value) * w
                drawRect(
                    Brush.linearGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.55f), Color.Transparent),
                        start = Offset(bandX - w * 0.2f, 0f),
                        end = Offset(bandX + w * 0.2f, h * 0.5f)
                    )
                )
                stars.forEach { s ->
                    val a = (0.35f + 0.65f * kotlin.math.abs(sin((s.offset + sweep.value) * PI.toFloat() * 2f))).coerceIn(0f, 1f)
                    sparkle(Offset(s.x * w, s.y * h), w * 0.045f * s.size, Color.White.copy(alpha = a))
                }
            }
            ChestTier.RARE -> {
                drawRect(Brush.verticalGradient(listOf(Color(0xFF240B4A), Color(0xFF5B27B8), Color(0xFFB245D6))))
                // Slowly turning rays.
                rotate(spin.value, glowCenter) {
                    val rays = 10
                    for (i in 0 until rays) {
                        val a = i * 2f * PI.toFloat() / rays
                        val half = PI.toFloat() / rays * 0.32f
                        val len = h * 1.3f
                        val p = Path().apply {
                            moveTo(glowCenter.x, glowCenter.y)
                            lineTo(glowCenter.x + cos(a - half) * len, glowCenter.y + sin(a - half) * len)
                            lineTo(glowCenter.x + cos(a + half) * len, glowCenter.y + sin(a + half) * len)
                            close()
                        }
                        drawPath(p, Brush.radialGradient(listOf(Color(0xFFFFD6FF).copy(alpha = 0.32f), Color.Transparent), glowCenter, len))
                    }
                }
                drawCircle(
                    Brush.radialGradient(
                        listOf(Color(0xFFFFE8FF).copy(alpha = 0.55f + 0.3f * pulse.value), Color(0x00C06BFF)),
                        glowCenter, w * (0.6f + 0.1f * pulse.value)
                    ),
                    radius = w * 0.7f, center = glowCenter
                )
                stars.forEach { s ->
                    val a = (0.25f + 0.75f * kotlin.math.abs(sin((s.offset + sweep.value * 2f) * PI.toFloat()))).coerceIn(0f, 1f)
                    sparkle(Offset(s.x * w, s.y * h), w * 0.05f * s.size, Color.White.copy(alpha = a))
                }
            }
        }
    }
}
