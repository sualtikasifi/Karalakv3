package com.sualtikasifi.cizimhafiza.presentation.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A count that has to be noticed by someone who is not looking for it: a big red disc that breathes, shakes now and then like a
 * ringing bell, and sends a ring outwards with each beat. Centred on whatever box it is given; it may overflow that box.
 */
@Composable
fun AttentionBadge(count: Int, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "attentionBadge")
    val breathe by t.animateFloat(
        initialValue = 1f, targetValue = 1.14f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "attentionBreathe"
    )
    val ring by t.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearOutSlowInEasing)),
        label = "attentionRing"
    )
    val shake by t.animateFloat(
        initialValue = 0f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 2600
                0f at 0
                -16f at 120
                16f at 240
                -12f at 360
                12f at 480
                -6f at 600
                0f at 720
                0f at 2600
            }
        ),
        label = "attentionShake"
    )
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(Modifier.wrapContentSize(unbounded = true), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(24.dp)
                    .graphicsLayer { scaleX = 1f + ring * 0.9f; scaleY = 1f + ring * 0.9f; alpha = 0.7f * (1f - ring) }
                    .border(2.dp, Color(0xFFFF5252), CircleShape)
            )
            Box(
                Modifier
                    .graphicsLayer { scaleX = breathe; scaleY = breathe; rotationZ = shake }
                    .defaultMinSize(minWidth = 24.dp, minHeight = 24.dp)
                    .shadow(3.dp, CircleShape)
                    .background(Brush.verticalGradient(listOf(Color(0xFFFF6B5E), Color(0xFFE02020))), CircleShape)
                    .border(1.5.dp, Color.White, CircleShape)
                    .padding(horizontal = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = count.toString(),
                    style = TextStyle(
                        fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
        }
    }
}
