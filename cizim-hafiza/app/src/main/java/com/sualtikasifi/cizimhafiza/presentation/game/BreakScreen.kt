package com.sualtikasifi.cizimhafiza.presentation.game

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.presentation.common.FitText
import com.sualtikasifi.cizimhafiza.presentation.common.LetteredText
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedStyle
import com.sualtikasifi.cizimhafiza.presentation.common.cachedPainterResource

// bg_break is this size; the sign, the paper strip and the dial are painted on it and everything live is laid over
// them by the picture's own coordinates.
private const val ArtW = 841f
private const val ArtH = 1870f
// The dial the mascot holds: its centre, the radius of the track the progress arc runs on, and the inner disc.
private const val DialX = 422f
private const val DialY = 1170f
private const val TrackR = 186f
private const val DiscR = 160f

private val Ink = Color(0xFF3B2314)
private val Outline = Color(0xFF4A2410)

/**
 * The pause between drawing and guessing, on its painted workshop: the mascot leans over a gold dial under the hanging
 * sign. The title is lettered on the sign in two lines (white, then yellow), the line under it is written on the paper
 * strip, and the dial counts down: the orange arc runs smoothly between whole seconds instead of jumping, the numeral
 * pops in on every tick, and a soft ring keeps spreading out of the dial so the wait reads as something charging up.
 */
@Composable
fun BreakScreen(state: GamePhase.Break) {
    val infinite = rememberInfiniteTransition(label = "break-pulse")
    // State, read inside the Canvas lambdas below: read here they would recompose this screen every frame of the wait.
    val ripple = infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "break-ripple"
    )
    val glow = infinite.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "break-glow"
    )

    // The arc sweeps linearly across each second, so it is always moving.
    val total = state.totalSeconds.coerceAtLeast(1)
    val arc = remember { Animatable(state.secondsLeft.toFloat() / total) }
    LaunchedEffect(state.secondsLeft) {
        arc.animateTo((state.secondsLeft - 1).coerceAtLeast(0).toFloat() / total, tween(1000, easing = LinearEasing))
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // ONE scale in both directions, so the picture is never stretched; spare strips show a blurred copy of it.
        val unit = minOf(maxWidth / ArtW, maxHeight / ArtH)
        val offX = (maxWidth - unit * ArtW) / 2
        val offY = (maxHeight - unit * ArtH) / 2
        val us = unit.value
        // Lettering follows the picture, not the system font-size setting.
        val fontScale0 = LocalDensity.current.fontScale
        fun fs(art: Float) = (art * us / fontScale0).sp
        fun box(x0: Float, y0: Float, x1: Float, y1: Float): Modifier =
            Modifier.offset(offX + unit * x0, offY + unit * y0).size(unit * (x1 - x0), unit * (y1 - y0))

        val scene = cachedPainterResource(R.drawable.bg_break)
        if (offX.value > 1f || offY.value > 1f) {
            Image(scene, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().blur(unit * 24f))
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.18f)))
        }
        Image(scene, contentDescription = null, contentScale = ContentScale.FillBounds, modifier = box(0f, 0f, ArtW, ArtH))

        // ---- The sign: two lines, white then yellow, inside the plank between its paint splashes -----------------
        LetteredText(
            text = stringResource(R.string.get_ready_line1),
            size = fs(62f),
            modifier = box(186f, 262f, 660f, 330f),
            minScale = 0.5f
        )
        LetteredText(
            text = stringResource(R.string.get_ready_line2),
            size = fs(80f),
            fill = Color(0xFFFFD43B),
            modifier = box(232f, 326f, 612f, 406f),
            minScale = 0.5f
        )

        // ---- The paper strip -----------------------------------------------------------------------------------
        FitText(
            text = stringResource(R.string.get_ready_sub),
            style = PaintedStyle(color = Ink, fontSize = fs(29f), textAlign = TextAlign.Center),
            maxLines = 2,
            minScale = 0.6f,
            modifier = box(256f, 486f, 600f, 566f)
        )

        // ---- The dial ------------------------------------------------------------------------------------------
        // A soft ring spreading out of the dial, forever, and a warm glow breathing on the disc behind the numeral.
        Canvas(modifier = box(DialX - 300f, DialY - 300f, DialX + 300f, DialY + 300f)) {
            val k = size.width / 600f
            val t = ripple.value
            drawCircle(
                color = Color(0xFFFFE08A).copy(alpha = 0.6f * (1f - t)),
                radius = (232f + 62f * t) * k,
                style = Stroke(width = 7f * k)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFFFFF4C2).copy(alpha = 0.75f * glow.value), Color.Transparent),
                    center = center,
                    radius = DiscR * k
                ),
                radius = DiscR * k
            )
        }
        // The track and the arc on it, starting at twelve o'clock and shrinking clockwise as the seconds run out.
        Canvas(modifier = box(DialX - TrackR - 20f, DialY - TrackR - 20f, DialX + TrackR + 20f, DialY + TrackR + 20f)) {
            val k = size.width / ((TrackR + 20f) * 2f)
            val stroke = 26f * k
            val inset = 20f * k
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            drawArc(
                color = Color(0xFFF6D9A6),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke)
            )
            rotate(-90f) {
                drawArc(
                    brush = Brush.sweepGradient(listOf(Color(0xFFFFA332), Color(0xFFF26A1B), Color(0xFFFFA332))),
                    startAngle = 0f,
                    sweepAngle = 360f * arc.value,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        // The numeral pops in on every tick.
        Box(box(DialX - 140f, DialY - 140f, DialX + 140f, DialY + 140f), contentAlignment = Alignment.Center) {
            AnimatedContent(
                targetState = state.secondsLeft,
                transitionSpec = {
                    (fadeIn(tween(180)) + scaleIn(spring(dampingRatio = 0.45f, stiffness = 400f), initialScale = 0.4f)) togetherWith
                        (fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 1.5f))
                },
                contentAlignment = Alignment.Center,
                label = "break-digit"
            ) { seconds ->
                LetteredText(seconds.toString(), fs(210f), fill = Color(0xFFFFB21E), outline = Outline)
            }
        }
    }
}
