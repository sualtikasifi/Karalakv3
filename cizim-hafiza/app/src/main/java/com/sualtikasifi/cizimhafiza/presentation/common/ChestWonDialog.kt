package com.sualtikasifi.cizimhafiza.presentation.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.Chest
import com.sualtikasifi.cizimhafiza.domain.model.ChestTier
import com.sualtikasifi.cizimhafiza.presentation.chests.ChestImage
import com.sualtikasifi.cizimhafiza.presentation.chests.accent
import com.sualtikasifi.cizimhafiza.presentation.chests.durationHours
import com.sualtikasifi.cizimhafiza.presentation.chests.glow
import com.sualtikasifi.cizimhafiza.presentation.chests.rarityLabelRes
import com.sualtikasifi.cizimhafiza.presentation.chests.rarityStars
import com.sualtikasifi.cizimhafiza.presentation.chests.sparkle
import com.sualtikasifi.cizimhafiza.presentation.theme.DisplayFont
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Shown once, right after a WON match found a free chest slot (see
 * GameViewModel.finishGame / OnlineResultViewModel) — a full-screen
 * celebration, not a notice: the chest drops in from above and lands with a
 * squash, a shockwave and a flash; light rays, confetti and sparkles take
 * over; then "KASA KAZANDIN!" pops in with the chest's rarity and how long it
 * takes to open. Colours follow the chest's tier, so a legendary Sürpriz
 * chest looks visibly bigger than a plain Çırak one.
 */
@Composable
fun ChestWonDialog(chest: Chest, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        ChestWonScene(chest = chest, onDismiss = onDismiss)
    }
}

private class Confetti(
    val x: Float,
    val start: Float,
    val speed: Float,
    val sway: Float,
    val swayRate: Float,
    val spin: Float,
    val size: Float,
    val color: Int
)

@Composable
private fun ChestWonScene(chest: Chest, onDismiss: () -> Unit) {
    val tier = chest.tier
    val accent = tier.accent()
    val light = tier.glow()
    val legendary = tier == ChestTier.RARE
    val palette = remember(tier) {
        listOf(accent, light, Color(0xFFFFD84D), Color.White, Color(0xFFFF7A59), Color(0xFF5BD6FF), Color(0xFFB388FF))
    }
    val confetti = remember(tier) {
        val rnd = Random(tier.ordinal * 31 + 7)
        List(if (legendary) 90 else 64) {
            Confetti(
                x = rnd.nextFloat(),
                start = rnd.nextFloat(),
                speed = 0.22f + rnd.nextFloat() * 0.45f,
                sway = 0.015f + rnd.nextFloat() * 0.03f,
                swayRate = 1.2f + rnd.nextFloat() * 2.2f,
                spin = 120f + rnd.nextFloat() * 420f,
                size = 6f + rnd.nextFloat() * 8f,
                color = rnd.nextInt(palette.size)
            )
        }
    }
    val twinkles = remember(tier) {
        val rnd = Random(tier.ordinal * 53 + 11)
        List(28) { Triple(Offset(rnd.nextFloat(), rnd.nextFloat()), 0.5f + rnd.nextFloat(), rnd.nextFloat()) }
    }

    val fall = remember { Animatable(0f) }
    val impact = remember { Animatable(0f) }
    val settle = remember { Animatable(1f) }
    val title = remember { Animatable(0f) }
    val info = remember { Animatable(0f) }
    val button = remember { Animatable(0f) }
    val haptic = LocalHapticFeedback.current
    val sound = rememberSoundManager()

    LaunchedEffect(Unit) {
        fall.animateTo(1f, tween(650, easing = FastOutLinearInEasing))
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        sound.playChestLand()
        coroutineScope {
            launch { impact.animateTo(1f, tween(1100, easing = LinearOutSlowInEasing)) }
            launch {
                settle.snapTo(0f)
                settle.animateTo(1f, spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessLow))
            }
            launch {
                delay(260)
                sound.playChestWin()
                title.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow))
            }
            launch {
                delay(650)
                info.animateTo(1f, tween(450))
            }
            launch {
                delay(900)
                button.animateTo(1f, tween(400))
            }
        }
    }

    val infinite = rememberInfiniteTransition(label = "chestWon")
    val time = infinite.animateFloat(
        initialValue = 0f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(tween(600_000, easing = LinearEasing), RepeatMode.Restart),
        label = "time"
    )
    val bob = infinite.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob"
    )
    val glowPulse = infinite.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glowPulse"
    )
    val buttonPulse = infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "buttonPulse"
    )
    val density = LocalDensity.current

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color(0xFF120A22))) {
        val chestCenterY = maxHeight * 0.34f

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val dp = density.density
            val center = Offset(w / 2f, chestCenterY.toPx())
            val landed = impact.value
            // Tinted atmosphere.
            drawRect(
                Brush.radialGradient(
                    listOf(accent.copy(alpha = 0.55f), Color(0xFF241046), Color(0xFF0B0616)),
                    center = center,
                    radius = h * 0.9f
                )
            )
            // Rays: appear on landing, keep turning.
            val rayAlpha = (0.16f + 0.2f * (if (legendary) 1f else 0.7f)) * landed
            rotate(time.value * 14f, center) {
                val rays = if (legendary) 20 else 14
                for (i in 0 until rays) {
                    val a = i * 2f * PI.toFloat() / rays
                    val half = PI.toFloat() / rays * 0.36f
                    val len = h * 1.2f
                    val p = Path().apply {
                        moveTo(center.x, center.y)
                        lineTo(center.x + cos(a - half) * len, center.y + sin(a - half) * len)
                        lineTo(center.x + cos(a + half) * len, center.y + sin(a + half) * len)
                        close()
                    }
                    drawPath(p, Brush.radialGradient(listOf(light.copy(alpha = rayAlpha), Color.Transparent), center, len * 0.9f))
                }
            }
            // Glow behind the chest.
            val glowR = (150f + 90f * landed) * dp * glowPulse.value
            drawCircle(
                Brush.radialGradient(listOf(light.copy(alpha = 0.95f * landed), accent.copy(alpha = 0.35f * landed), Color.Transparent), center, glowR),
                radius = glowR, center = center
            )
            // Shockwave rings from the impact.
            if (landed > 0f && landed < 1f) {
                val ringCenter = Offset(center.x, center.y + 90.dp.toPx())
                for (k in 0..1) {
                    val p = ((landed - k * 0.18f) / (1f - k * 0.18f)).coerceIn(0f, 1f)
                    if (p > 0f) {
                        drawCircle(
                            color = Color.White.copy(alpha = (1f - p) * 0.7f),
                            radius = (40f + 520f * p) * dp,
                            center = ringCenter,
                            style = Stroke(width = ((1f - p) * 14f + 2f) * dp)
                        )
                    }
                }
            }
            // Confetti rain after landing.
            val t = time.value
            confetti.forEach { c ->
                val prog = ((c.start + t * c.speed) % 1.2f) - 0.1f
                val x = (c.x + sin(t * c.swayRate + c.start * 6f) * c.sway) * w
                val y = prog * h
                val s = c.size * dp
                rotate(t * c.spin + c.start * 360f, Offset(x, y)) {
                    drawRect(
                        color = palette[c.color].copy(alpha = landed.coerceIn(0f, 1f) * 0.95f),
                        topLeft = Offset(x - s / 2f, y - s * 0.85f),
                        size = Size(s, s * 1.7f)
                    )
                }
            }
            // Twinkling stars.
            twinkles.forEach { (pos, sz, off) ->
                val a = (0.2f + 0.8f * abs(sin((off * 3f + t * 0.9f) * PI.toFloat()))) * landed
                sparkle(Offset(pos.x * w, pos.y * h), 9f * dp * sz, Color.White.copy(alpha = a))
            }
            // Landing flash.
            val flash = (1f - landed / 0.16f).coerceIn(0f, 1f)
            if (landed > 0f && flash > 0f) drawRect(Color.White.copy(alpha = flash * 0.85f))
        }

        // The chest: falls, squashes on landing, then floats.
        Box(
            modifier = Modifier
                .offset(y = chestCenterY - 170.dp)
                .align(Alignment.TopCenter)
                .size(340.dp)
                .graphicsLayer {
                    val drop = (1f - fall.value)
                    translationY = -drop * (chestCenterY.toPx() + 260.dp.toPx()) + bob.value * 6.dp.toPx() * settle.value
                    rotationZ = drop * -14f
                    val sq = settle.value
                    scaleX = 1.22f - 0.22f * sq
                    scaleY = 0.72f + 0.28f * sq
                    transformOrigin = TransformOrigin(0.5f, 0.85f)
                },
            contentAlignment = Alignment.Center
        ) {
            ChestImage(tier = tier, width = 320.dp)
        }

        // Title, rarity, unlock time, button.
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = chestCenterY + 175.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.chest_win_title),
                style = TextStyle(
                    brush = Brush.verticalGradient(listOf(Color(0xFFFFF3B0), Color(0xFFFFC72E), Color(0xFFFF8A00))),
                    shadow = Shadow(Color(0xFF3A1A00), Offset(0f, 6f), 10f),
                    fontFamily = DisplayFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 38.sp,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.graphicsLayer {
                    scaleX = title.value
                    scaleY = title.value
                    alpha = title.value.coerceIn(0f, 1f)
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .graphicsLayer { alpha = info.value; translationY = (1f - info.value) * 24f }
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(lerp(accent, Color.Black, 0.25f), accent)))
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "★".repeat(tier.rarityStars()) + "☆".repeat(3 - tier.rarityStars()),
                    fontSize = 16.sp,
                    color = Color(0xFFFFE066)
                )
                Text(
                    text = stringResource(tier.labelRes()),
                    fontFamily = DisplayFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = Color.White
                )
                Text(
                    text = "· " + stringResource(tier.rarityLabelRes()),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Column(
                modifier = Modifier.graphicsLayer { alpha = info.value },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.Schedule, contentDescription = null, tint = Color(0xFFFFE066), modifier = Modifier.size(20.dp))
                    Text(
                        text = stringResource(R.string.chest_win_unlock_time, tier.durationHours()),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.chest_win_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(22.dp))
            PrimaryButton(
                text = stringResource(R.string.chest_won_button),
                onClick = onDismiss,
                enabled = button.value > 0.95f,
                modifier = Modifier
                    .width(240.dp)
                    .graphicsLayer {
                        alpha = button.value
                        val s = buttonPulse.value
                        scaleX = s
                        scaleY = s
                    }
            )
        }
    }
}

fun ChestTier.labelRes(): Int = when (this) {
    ChestTier.SILVER -> R.string.chest_tier_silver
    ChestTier.GOLD -> R.string.chest_tier_gold
    ChestTier.RARE -> R.string.chest_tier_rare
}

/** The chest's own illustration — see store-assets/chest-art for the originals. */
fun ChestTier.artRes(): Int = when (this) {
    ChestTier.SILVER -> R.drawable.chest_apprentice
    ChestTier.GOLD -> R.drawable.chest_artist
    ChestTier.RARE -> R.drawable.chest_surprise
}
