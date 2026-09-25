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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.presentation.common.screenBackground
import com.sualtikasifi.cizimhafiza.presentation.theme.DisplayFont

/**
 * The pause between drawing and guessing. Built as a small composed scene
 * rather than a bare ring: a tag and title on top, and below them a dial — a
 * raised white disc, a progress arc that sweeps smoothly between whole
 * seconds instead of jumping, a numeral that pops in on every tick, and a
 * soft ripple spreading outward so the wait reads as something charging up.
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
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "break-breathe"
    )

    // The arc sweeps linearly across each second, so it is always moving.
    val total = state.totalSeconds.coerceAtLeast(1)
    val arc = remember { Animatable(state.secondsLeft.toFloat() / total) }
    LaunchedEffect(state.secondsLeft) {
        arc.animateTo((state.secondsLeft - 1).coerceAtLeast(0).toFloat() / total, tween(1000, easing = LinearEasing))
    }

    val primary = MaterialTheme.colorScheme.primary
    val track = primary.copy(alpha = 0.14f)

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .screenBackground()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Filled.Psychology, contentDescription = null, tint = primary, modifier = Modifier.size(18.dp))
                Text(
                    text = stringResource(R.string.get_ready_tag),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.get_ready),
                fontFamily = DisplayFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 30.sp,
                lineHeight = 34.sp,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.get_ready_sub),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(34.dp))

            Box(modifier = Modifier.size(232.dp), contentAlignment = Alignment.Center) {
                // Ripple: a ring that grows and fades, forever.
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val t = rippleProgress.value
                    val radius = size.minDimension / 2f * (0.72f + 0.28f * t)
                    drawCircle(
                        color = primary.copy(alpha = 0.30f * (1f - t)),
                        radius = radius,
                        style = Stroke(width = 6.dp.toPx())
                    )
                }
                // The raised disc.
                Box(
                    modifier = Modifier
                        .size(184.dp)
                        .graphicsLayer {
                            scaleX = breathe.value
                            scaleY = breathe.value
                        }
                        .shadow(14.dp, CircleShape, ambientColor = primary, spotColor = primary)
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                        .border(3.dp, primary.copy(alpha = 0.22f), CircleShape)
                )
                // Progress arc.
                Canvas(modifier = Modifier.size(196.dp)) {
                    val stroke = 11.dp.toPx()
                    val inset = stroke / 2f
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawArc(
                        color = track,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = stroke)
                    )
                    drawArc(
                        color = primary,
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
                    Text(
                        text = seconds.toString(),
                        fontFamily = DisplayFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 84.sp,
                        color = primary
                    )
                }
            }
        }
    }
}
