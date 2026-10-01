package com.sualtikasifi.cizimhafiza.presentation.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import com.sualtikasifi.cizimhafiza.domain.model.LevelProgressState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Carries one "XP flies from the result screen's button to the home XP bar" moment between two screens
 * that never meet: the result screen calls [start] with where the player tapped just before going home,
 * the home bar registers where it is ([target]) and holds its old value until the spark lands
 * ([arrivedId]), and [XpFlyOverlay] — drawn over the whole app — flies the spark in between.
 */
object XpFlyBus {
    data class Fly(val id: Long, val origin: Offset, val amount: Int, val toXp: Int) {
        val fromXp: Int get() = (toXp - amount).coerceAtLeast(0)
    }

    private val _active = MutableStateFlow<Fly?>(null)
    val active: StateFlow<Fly?> = _active.asStateFlow()

    private val _arrivedId = MutableStateFlow(0L)
    val arrivedId: StateFlow<Long> = _arrivedId.asStateFlow()

    /** Window position the spark flies to; null whenever the home bar is not on screen. */
    val target = MutableStateFlow<Offset?>(null)

    fun start(origin: Offset, amount: Int, toXp: Int) {
        if (amount <= 0) return
        target.value = null
        _active.value = Fly(System.nanoTime(), origin, amount, toXp)
    }

    internal fun arrived(id: Long) { _arrivedId.value = id }

    internal fun finish(id: Long) { if (_active.value?.id == id) _active.value = null }

    /** What the home bar should show right now: the pre-round progress until the spark has landed. */
    fun displayProgress(actual: LevelProgressState, fly: Fly?, arrivedId: Long): LevelProgressState =
        if (fly != null && fly.id != arrivedId) LevelProgressState.forXp(fly.fromXp) else actual
}

private val SparkGold = Color(0xFFFFD34D)
private val SparkOrange = Color(0xFFFF8A1F)

/** Draws the flying spark, its trail, the "+XP" label and the landing burst. Place once, over everything. */
@Composable
fun XpFlyOverlay() {
    val fly by XpFlyBus.active.collectAsState()
    val current = fly ?: return
    val flight = remember(current.id) { Animatable(0f) }
    val burst = remember(current.id) { Animatable(0f) }
    var targetPos by remember(current.id) { mutableStateOf<Offset?>(null) }

    LaunchedEffect(current.id) {
        // Home may still be animating in; wait for its bar to say where it is (never forever).
        val target = withTimeoutOrNull(2500) {
            var t = XpFlyBus.target.value
            while (t == null) { delay(30); t = XpFlyBus.target.value }
            t
        }
        if (target == null) {
            XpFlyBus.arrived(current.id)
            XpFlyBus.finish(current.id)
            return@LaunchedEffect
        }
        delay(250)
        targetPos = XpFlyBus.target.value ?: target
        flight.animateTo(1f, tween(950, easing = FastOutSlowInEasing))
        XpFlyBus.arrived(current.id)
        burst.animateTo(1f, tween(650))
        XpFlyBus.finish(current.id)
    }

    val to = targetPos
    val from = current.origin
    if (to != null) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val control = Offset((from.x + to.x) / 2f - size.width * 0.22f, minOf(from.y, to.y) - size.height * 0.12f)
            fun at(t: Float): Offset {
                val u = 1f - t
                return Offset(
                    u * u * from.x + 2 * u * t * control.x + t * t * to.x,
                    u * u * from.y + 2 * u * t * control.y + t * t * to.y
                )
            }
            val p = flight.value
            if (p < 1f) {
                val head = at(p)
                // Trail: older points are smaller, fainter and a deeper orange.
                for (k in 0 until 14) {
                    val tp = (p - k * 0.022f).coerceAtLeast(0f)
                    val pos = at(tp)
                    val f = 1f - k / 14f
                    drawCircle(
                        color = Color(
                            red = SparkOrange.red, green = SparkOrange.green, blue = SparkOrange.blue, alpha = 0.55f * f
                        ),
                        radius = 11.dp.toPx() * f,
                        center = pos
                    )
                }
                // Twinkles drifting off the head.
                for (i in 0 until 5) {
                    val ang = (p * 9f + i * 2f * PI.toFloat() / 5f)
                    val r = 16.dp.toPx() * (0.6f + 0.4f * sin(p * 14f + i))
                    drawCircle(Color.White.copy(alpha = 0.8f), 2.2.dp.toPx(), head + Offset(cos(ang) * r, sin(ang) * r))
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color.White, SparkGold, SparkOrange.copy(alpha = 0.6f), Color.Transparent),
                        center = head,
                        radius = 30.dp.toPx()
                    ),
                    radius = 30.dp.toPx(),
                    center = head
                )
            }
            val b = burst.value
            if (b > 0f && b < 1f) {
                val a = 1f - b
                drawCircle(
                    Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.9f * a), SparkGold.copy(alpha = 0.6f * a), Color.Transparent),
                        center = to, radius = 90.dp.toPx() * (0.3f + b)
                    ),
                    radius = 90.dp.toPx() * (0.3f + b),
                    center = to
                )
                for (i in 0 until 10) {
                    val ang = i * 2f * PI.toFloat() / 10f
                    val r = 70.dp.toPx() * b
                    drawCircle(SparkGold.copy(alpha = a), 3.dp.toPx() * a + 1f, to + Offset(cos(ang) * r, sin(ang) * r))
                }
            }
        }
        // "+XP" rides along with the spark and fades as it nears the bar.
        val p = flight.value
        if (p < 0.9f) {
            val u = 1f - p
            val x = u * u * from.x + 2 * u * p * ((from.x + to.x) / 2f - 120f) + p * p * to.x
            val y = u * u * from.y + 2 * u * p * (minOf(from.y, to.y) - 160f) + p * p * to.y
            Box(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "+${current.amount} XP",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .offset { IntOffset((x + 24.dp.toPx()).toInt(), (y - 36.dp.toPx()).toInt()) }
                        .alpha((1f - p / 0.9f).coerceIn(0f, 1f))
                )
            }
        }
    }
}
