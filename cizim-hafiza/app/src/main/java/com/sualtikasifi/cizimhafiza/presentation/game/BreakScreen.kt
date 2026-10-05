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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.presentation.common.FitText
import com.sualtikasifi.cizimhafiza.presentation.common.LetteredText
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedStyle
import com.sualtikasifi.cizimhafiza.presentation.common.cachedPainterResource

// bg_offline is 841 x 1870 art units; its painted title plank sits at 17.2% of the height.
private const val ArtW = 841f
private const val ArtH = 1870f

private val Ink = Color(0xFF5A321F)
private val Orange = Color(0xFFF47721)

/**
 * The pause between drawing and guessing, on the game's painted room: the title on the hanging plank, a short line on a
 * paper strip, and below it a dial — a raised cream disc in a gold rim, a progress arc that sweeps smoothly between
 * whole seconds instead of jumping, a numeral that pops in on every tick and a soft ripple spreading outward so the
 * wait reads as something charging up.
 */
@Composable
fun BreakScreen(state: GamePhase.Break) {
    val infinite = rememberInfiniteTransition(label = "break-pulse")
    // State, read inside graphicsLayer/Canvas lambdas below: read here they
    // would recompose this screen every frame of the wait.
    val rippleProgress = infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "break-ripple"
    )
    val breathe = infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "break-breathe"
    )

    // The arc sweeps linearly across each second, so it is always moving.
    val total = state.totalSeconds.coerceAtLeast(1)
    val arc = remember { Animatable(state.secondsLeft.toFloat() / total) }
    LaunchedEffect(state.secondsLeft) {
        arc.animateTo((state.secondsLeft - 1).coerceAtLeast(0).toFloat() / total, tween(1000, easing = LinearEasing))
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val s = maxOf(widthPx / ArtW, heightPx / ArtH)
        val offX = (widthPx - ArtW * s) / 2f
        fun yOf(fraction: Float): Dp = with(density) { (ArtH * s * fraction).toDp() }
        fun len(artPx: Float): Dp = with(density) { (artPx * s).toDp() }
        val textScale = (maxWidth.value / 411f).coerceIn(0.85f, 1.25f)

        Image(
            painter = cachedPainterResource(R.drawable.bg_offline),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxSize()
        )

        // Title, centred on the painted plank.
        val signCentreX = with(density) { (offX + ArtW * s * 0.505f).toDp() }
        Box(
            modifier = Modifier.offset(x = signCentreX - len(290f), y = yOf(0.172f) - len(48f)).size(len(580f), len(96f)),
            contentAlignment = Alignment.Center
        ) {
            FitText(
                text = stringResource(R.string.get_ready),
                style = PaintedStyle(color = Color.White, fontSize = 30.sp * textScale, textAlign = TextAlign.Center),
                maxLines = 2,
                minScale = 0.55f,
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(top = yOf(0.235f), bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically)
        ) {
            // The one line that says what comes next, on a paper strip.
            Box(
                modifier = Modifier
                    .padding(horizontal = 28.dp)
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(16.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFFFFF9EA), Color(0xFFF6E6C6))), RoundedCornerShape(16.dp))
                    .border(1.5.dp, Color(0xFFD9BC8C), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                FitText(
                    text = stringResource(R.string.get_ready_sub),
                    style = PaintedStyle(color = Ink, fontSize = 17.sp, textAlign = TextAlign.Center),
                    maxLines = 2,
                    minScale = 0.7f,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Box(modifier = Modifier.size(248.dp), contentAlignment = Alignment.Center) {
                // Ripple: a ring that grows and fades, forever.
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val t = rippleProgress.value
                    val radius = size.minDimension / 2f * (0.74f + 0.26f * t)
                    drawCircle(
                        color = Color(0xFFFFE08A).copy(alpha = 0.55f * (1f - t)),
                        radius = radius,
                        style = Stroke(width = 6.dp.toPx())
                    )
                }
                // The raised disc: cream paper in a gold rim.
                Box(
                    modifier = Modifier
                        .size(196.dp)
                        .graphicsLayer {
                            scaleX = breathe.value
                            scaleY = breathe.value
                        }
                        .shadow(14.dp, CircleShape, ambientColor = Color(0xFF5A321F), spotColor = Color(0xFF5A321F))
                        .background(Brush.verticalGradient(listOf(Color(0xFFFFF9EA), Color(0xFFF6E0B4))), CircleShape)
                        .border(5.dp, Brush.verticalGradient(listOf(Color(0xFFFFD66B), Color(0xFFE0861B))), CircleShape)
                )
                // Progress arc.
                Canvas(modifier = Modifier.size(204.dp)) {
                    val stroke = 12.dp.toPx()
                    val inset = stroke / 2f + 9.dp.toPx()
                    val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                    drawArc(
                        color = Color(0xFFE8C99A).copy(alpha = 0.7f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = stroke)
                    )
                    drawArc(
                        color = Orange,
                        startAngle = -90f,
                        sweepAngle = 360f * arc.value,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
                // The numeral pops in on every tick.
                AnimatedContent(
                    targetState = state.secondsLeft,
                    transitionSpec = {
                        (fadeIn(tween(180)) + scaleIn(spring(dampingRatio = 0.45f, stiffness = 400f), initialScale = 0.4f)) togetherWith
                            (fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 1.5f))
                    },
                    label = "break-digit"
                ) { seconds ->
                    LetteredText(seconds.toString(), 84.sp, fill = Orange, outline = Color.White)
                }
            }
        }
    }
}
