package com.sualtikasifi.cizimhafiza.presentation.splash

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sualtikasifi.cizimhafiza.R
import kotlin.math.PI
import kotlin.math.sin

/**
 * The brand moment: the painted Karalak scene fades in over the system splash, drifts in slowly, twinkles, and a loading
 * bar fills underneath it before the app opens.
 *
 * The hand-off from the system splash (see Theme.Karalak.Splash) is a plain colour with the round logo in the middle; this
 * screen's first frame is exactly that — same colour, same logo, same place — and only then does the scene fade in over
 * it, so the player never sees a seam.
 *
 * It is kept honestly short: [TOTAL_MILLIS] the first time the app is ever opened and a quicker [FAST_TOTAL_MILLIS] on
 * every cold start after that. It plays on cold start only (the caller's rememberSaveable), a tap skips to the end, and
 * it is skipped outright when the device has animations turned off.
 */
@Composable
fun BrandSplash(onFinished: () -> Unit) {
    val context = LocalContext.current
    val animationsDisabled = remember {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
    // Seen once -> every later cold start plays the faster run. Read once and remembered: the flag flips at the end of
    // this composition's own run and must not change which speed the run in flight is using.
    val returning = remember {
        runCatching { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_SEEN, false) }
            .getOrDefault(false)
    }
    val finish = {
        runCatching { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_SEEN, true).apply() }
        onFinished()
    }

    val progress = remember { Animatable(0f) }
    var skipped by remember { mutableStateOf(false) }
    val total = if (returning) FAST_TOTAL_MILLIS else TOTAL_MILLIS

    LaunchedEffect(animationsDisabled) {
        if (animationsDisabled) {
            finish()
            return@LaunchedEffect
        }
        progress.animateTo(1f, tween(total, easing = LinearEasing))
        finish()
    }
    // A second animateTo on the same Animatable cancels the first, so the timeline simply stops where the tap caught it
    // and this one runs out the rest.
    LaunchedEffect(skipped) {
        if (!skipped) return@LaunchedEffect
        progress.animateTo(1f, tween(SKIP_MILLIS, easing = LinearEasing))
        finish()
    }

    val twinkle = rememberInfiniteTransition(label = "splashTwinkle")
    val pulse = twinkle.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = "pulse"
    )
    val stripes = twinkle.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Restart),
        label = "stripes"
    )

    val scene = painterResource(R.drawable.splash_art)
    val mark = painterResource(R.drawable.splash_mark)

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(SplashColor)
            .graphicsLayer {
                // Read inside the lambda so the fade re-runs in the draw phase only.
                alpha = 1f - phase(progress.value * total, total - FADE_OUT_MILLIS, total.toFloat())
            }
            .pointerInput(Unit) { detectTapGestures { skipped = true } }
    ) {
        val density = LocalDensity.current
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }
        // How the Crop-scaled scene maps onto the screen (art units are pixels of the 841 x 1870 picture).
        val cover = maxOf(wPx / ART_W, hPx / ART_H)
        val offX = (wPx - ART_W * cover) / 2f
        val offY = (hPx - ART_H * cover) / 2f

        // The system splash's own frame, held for a moment and then handed over to the scene.
        val sceneAlpha = phase(progress.value * total, MARK_HOLD, MARK_HOLD + CROSS_FADE)
        Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = 1f - sceneAlpha }) {
            val side = 288.dp.toPx()
            translate(size.width / 2f - side / 2f, size.height / 2f - side / 2f) {
                with(mark) { draw(Size(side, side)) }
            }
        }

        Image(
            painter = scene,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = sceneAlpha
                    // A slow push-in: the whole picture breathes toward the player over the run.
                    val zoom = 1f + ZOOM * FastOutSlowInEasing.transform(progress.value)
                    scaleX = zoom
                    scaleY = zoom
                }
        )

        Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = sceneAlpha }) {
            // Sparkles on the scene's painted stars (art coordinates), each on its own phase.
            STARS.forEachIndexed { i, star ->
                val t = (pulse.value + i * 0.23f) % 1f
                val k = 0.5f + 0.5f * sin(t * 2f * PI.toFloat())
                val c = Offset(offX + star.x * cover, offY + star.y * cover)
                val r = star.r * cover * (0.7f + 0.5f * k)
                drawStar(c, r, Color(0xFFFFF3B0).copy(alpha = 0.25f + 0.65f * k))
            }

            // The loading bar: a dark track with a golden rim, an orange fill with moving stripes, and a bright head.
            val left = offX + BAR_L * cover
            val top = offY + BAR_T * cover
            val barW = (BAR_R - BAR_L) * cover
            val barH = (BAR_B - BAR_T) * cover
            val round = CornerRadius(barH / 2f)
            drawRoundRect(Color(0xFFFFE08A), Offset(left - 3f, top - 3f), Size(barW + 6f, barH + 6f), round)
            drawRoundRect(Color(0xFF8A4E12), Offset(left, top), Size(barW, barH), round)
            drawRoundRect(Color(0xFF3F2210), Offset(left + barH * 0.09f, top + barH * 0.09f), Size(barW - barH * 0.18f, barH * 0.82f), CornerRadius(barH * 0.41f))
            val frac = FastOutSlowInEasing.transform(phase(progress.value * total, BAR_FROM, total - FADE_OUT_MILLIS * 0.6f))
            val inL = left + barH * 0.17f
            val inT = top + barH * 0.17f
            val inH = barH * 0.66f
            val inMax = barW - barH * 0.34f
            val fillW = (inMax * frac).coerceAtLeast(inH)
            val fillPath = Path().apply {
                addRoundRect(androidx.compose.ui.geometry.RoundRect(inL, inT, inL + fillW, inT + inH, CornerRadius(inH / 2f)))
            }
            clipPath(fillPath) {
                drawRect(
                    Brush.verticalGradient(listOf(Color(0xFFFFC04A), Color(0xFFF58A1F)), startY = inT, endY = inT + inH),
                    Offset(inL, inT), Size(fillW, inH)
                )
                // Diagonal stripes sliding along the fill.
                val step = inH * 1.1f
                var x = inL - step * 2 + stripes.value * step
                while (x < inL + fillW + step) {
                    val p = Path().apply {
                        moveTo(x, inT + inH); lineTo(x + step * 0.5f, inT + inH)
                        lineTo(x + step * 0.5f + inH * 0.8f, inT); lineTo(x + inH * 0.8f, inT); close()
                    }
                    drawPath(p, Color.White.copy(alpha = 0.22f))
                    x += step
                }
                drawRect(Color.White.copy(alpha = 0.28f), Offset(inL, inT), Size(fillW, inH * 0.3f))
            }
            if (frac > 0.02f) {
                drawCircle(Color(0xFFFFF3B0).copy(alpha = 0.55f), inH * 0.95f, Offset(inL + fillW - inH * 0.3f, inT + inH / 2f))
            }
        }
    }
}

private class StarSpot(val x: Float, val y: Float, val r: Float)

private val STARS = listOf(
    StarSpot(472f, 328f, 22f), StarSpot(794f, 598f, 20f), StarSpot(133f, 812f, 20f),
    StarSpot(540f, 1296f, 26f), StarSpot(326f, 1138f, 14f), StarSpot(780f, 130f, 14f)
)

/** A four-pointed sparkle. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStar(c: Offset, r: Float, color: Color) {
    val p = Path().apply {
        moveTo(c.x, c.y - r)
        quadraticTo(c.x, c.y, c.x + r, c.y)
        quadraticTo(c.x, c.y, c.x, c.y + r)
        quadraticTo(c.x, c.y, c.x - r, c.y)
        quadraticTo(c.x, c.y, c.x, c.y - r)
        close()
    }
    drawPath(p, color)
}

/** 0f before [fromMs], 1f after [toMs], linear in between. */
private fun phase(elapsed: Float, fromMs: Float, toMs: Float): Float =
    ((elapsed - fromMs) / (toMs - fromMs)).coerceIn(0f, 1f)

private val SplashColor = Color(0xFFB07F34)

// First run: the logo holds, the scene fades in, the bar fills, the whole thing leaves. Later runs are quicker.
private const val TOTAL_MILLIS = 2300
private const val FAST_TOTAL_MILLIS = 1500
private const val SKIP_MILLIS = 170
private const val MARK_HOLD = 120f
private const val CROSS_FADE = 300f
private const val BAR_FROM = 380f
private const val FADE_OUT_MILLIS = 260f
private const val ZOOM = 0.05f

private const val PREFS_NAME = "brand_splash"
private const val KEY_SEEN = "seen"

// The scene picture and where its loading bar sits in it (pixels of the 841 x 1870 art).
private const val ART_W = 841f
private const val ART_H = 1870f
private const val BAR_L = 196f
private const val BAR_R = 642f
private const val BAR_T = 1354f
private const val BAR_B = 1430f
