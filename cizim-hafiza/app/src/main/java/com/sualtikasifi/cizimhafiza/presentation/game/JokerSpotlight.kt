package com.sualtikasifi.cizimhafiza.presentation.game

import androidx.annotation.StringRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sualtikasifi.cizimhafiza.domain.model.JokerType
import com.sualtikasifi.cizimhafiza.presentation.common.RaisedCard

/** The tutorial asking the player to use one specific joker: which, and what the card above it says. */
data class JokerSpotlight(
    val type: JokerType,
    @StringRes val titleRes: Int,
    @StringRes val bodyRes: Int
)

/**
 * Dims the whole guess screen except one joker button, which pulses with a pointing hand over it, and
 * swallows every touch outside that button — so the only thing the player can do is use the joker being
 * taught. [hole] is that button's bounds in window coordinates (null until it has been measured, during
 * which everything is blocked).
 */
@Composable
fun JokerSpotlightOverlay(spotlight: JokerSpotlight, hole: Rect?) {
    var origin by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val pulse by rememberInfiniteTransition(label = "spotlightPulse").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(750, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val padPx = with(density) { 6.dp.toPx() }
    val local = hole?.translate(-origin.x, -origin.y)?.let {
        Rect(it.left - padPx, it.top - padPx, it.right + padPx, it.bottom + padPx)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { origin = it.positionInWindow(); size = it.size }
    ) {
        if (size == IntSize.Zero) return@Box
        val width = size.width.toFloat()
        val height = size.height.toFloat()

        Canvas(modifier = Modifier.fillMaxSize()) {
            val corner = CornerRadius(18.dp.toPx())
            val scrim = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(0f, 0f, width, height))
                if (local != null) addRoundRect(RoundRect(local, corner))
            }
            drawPath(scrim, Color.Black.copy(alpha = 0.74f))
            if (local != null) {
                val grow = 2.dp.toPx() + pulse * 5.dp.toPx()
                drawRoundRect(
                    color = Color(0xFFFF8A1F).copy(alpha = 0.95f - 0.45f * pulse),
                    topLeft = Offset(local.left - grow, local.top - grow),
                    size = androidx.compose.ui.geometry.Size(local.width + grow * 2, local.height + grow * 2),
                    cornerRadius = CornerRadius(18.dp.toPx() + grow),
                    style = Stroke(width = 3.5.dp.toPx())
                )
            }
        }

        // Touch blockers: the four strips around the hole (or everything, before it is measured). The hole
        // itself has nothing over it, so a tap there reaches the joker button underneath.
        val strips = if (local == null) {
            listOf(Rect(0f, 0f, width, height))
        } else {
            listOf(
                Rect(0f, 0f, width, local.top),
                Rect(0f, local.bottom, width, height),
                Rect(0f, local.top, local.left, local.bottom),
                Rect(local.right, local.top, width, local.bottom)
            )
        }
        strips.forEach { strip ->
            if (strip.width <= 0f || strip.height <= 0f) return@forEach
            Box(
                modifier = Modifier
                    .offset { IntOffset(strip.left.toInt(), strip.top.toInt()) }
                    .size(with(density) { strip.width.toDp() }, with(density) { strip.height.toDp() })
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) awaitPointerEvent().changes.forEach { it.consume() }
                        }
                    }
            )
        }

        if (local != null) {
            val handBox = 56.dp
            val aboveHole = with(density) { (height - local.top).toDp() }
            // The explanation sits above the pointing hand, which hangs right over the button's centre.
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = aboveHole + handBox + 4.dp)
            ) {
                RaisedCard(corner = 24.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(spotlight.titleRes),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(spotlight.bodyRes),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset {
                        IntOffset(
                            (local.center.x - handBox.toPx() / 2f).toInt(),
                            -(height - local.top).toInt() - 2.dp.roundToPx()
                        )
                    }
                    .size(handBox),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "👇",
                    fontSize = 38.sp,
                    modifier = Modifier.graphicsLayer { translationY = 10.dp.toPx() * pulse }
                )
            }
        }
    }
}
