package com.sualtikasifi.cizimhafiza.presentation.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.sualtikasifi.cizimhafiza.presentation.mainmenu.rememberSink
import com.sualtikasifi.cizimhafiza.presentation.mainmenu.sinkWith
import com.sualtikasifi.cizimhafiza.presentation.mainmenu.sunkenArt
import kotlin.math.roundToInt

/**
 * How a window's painted button is pressed for real: [cut] is the button alone, cut out of the window's picture on its
 * own exact shape (icons sticking out of a row included), and [patch] what lies behind it — the window with the button
 * painted out, on the button's own (slightly grown) shape only, so a neighbour is never covered. Both are the size of
 * the button's bounding box, which is [left]..[height] as fractions of the window's picture.
 */
class PressArt(val cut: Int, val patch: Int, val left: Float, val top: Float, val width: Float, val height: Float)

/**
 * A button that is PAINTED into a window's picture. The area [left] [top] [width] [height] (fractions of the picture,
 * laid over it inside a parent of [parentW] x [parentH]) is what is touched and where [content] (its words) is laid out.
 *
 * With [art] the press moves the whole button like a real one: while the finger is down the button's own cut-out sinks
 * a little (smaller, a few dp lower, a touch darker) over the picture with the button painted out, and its words go
 * with it; on release it springs back once. Without [art] the older look is kept (the area's pixels redrawn, shrunk,
 * over a darker recess). A disabled area does neither.
 */
@Composable
fun PaintedPress(
    artRes: Int,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    parentW: Dp,
    parentH: Dp,
    corner: Dp,
    enabled: Boolean = true,
    depth: Float = 0.93f,
    art: PressArt? = null,
    onClick: () -> Unit,
    content: @Composable BoxWithConstraintsScope.() -> Unit = {}
) {
    if (art != null) {
        CutOutPress(art, left, top, width, height, parentW, parentH, enabled, onClick, content)
        return
    }
    val context = LocalContext.current
    val sheet = remember(artRes) { runCatching { BackdropCache.get(context.resources, artRes) }.getOrNull() }
    val sink = rememberSink(depth)
    val sw = sheet?.width?.toFloat() ?: 1f
    val sh = sheet?.height?.toFloat() ?: 1f
    val base = Modifier
        .offset(parentW * left, parentH * top)
        .size(parentW * width, parentH * height)
        .sunkenArt(sink, if (enabled) sheet else null, sw * left, sh * top, sw * width, sh * height, corner)
    BoxWithConstraints(
        if (enabled) base.clickable(interactionSource = sink.source, indication = null, onClick = onClick) else base
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().sinkWith(sink, 0.5f, 0.5f)) { content() }
    }
}

@Composable
private fun CutOutPress(
    art: PressArt,
    left: Float, top: Float, width: Float, height: Float,
    parentW: Dp, parentH: Dp,
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable BoxWithConstraintsScope.() -> Unit
) {
    val context = LocalContext.current
    val cut = remember(art.cut) { runCatching { BackdropCache.get(context.resources, art.cut) }.getOrNull() }
    val patch = remember(art.patch) { runCatching { BackdropCache.get(context.resources, art.patch) }.getOrNull() }
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed && enabled) 1f else 0f,
        animationSpec = if (pressed) tween(90) else spring(dampingRatio = 0.55f, stiffness = 600f),
        label = "painted_press"
    )
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    androidx.compose.runtime.LaunchedEffect(pressed) {
        if (pressed && enabled) haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
    }
    val k = 1f - 0.045f * press
    val drop = 2.5.dp
    // the cut-out: nothing is drawn at rest (the window's picture already shows the button exactly)
    Box(
        Modifier
            .offset(parentW * art.left, parentH * art.top)
            .size(parentW * art.width, parentH * art.height)
            .drawBehind {
                if (press <= 0f || cut == null || patch == null) return@drawBehind
                val dst = IntSize(size.width.roundToInt(), size.height.roundToInt())
                drawImage(patch, dstSize = dst, filterQuality = FilterQuality.Medium)
                translate(top = drop.toPx() * press) {
                    scale(k, k, pivot = center) {
                        drawImage(
                            cut, dstSize = dst, filterQuality = FilterQuality.Medium,
                            colorFilter = ColorFilter.tint(Color.Black.copy(alpha = 0.13f * press), BlendMode.SrcAtop)
                        )
                    }
                }
            }
    )
    // the touch area and the button's words, sinking about the cut-out's centre
    val pivotX = ((art.left + art.width / 2f) - left) / width
    val pivotY = ((art.top + art.height / 2f) - top) / height
    val base = Modifier
        .offset(parentW * left, parentH * top)
        .size(parentW * width, parentH * height)
        .graphicsLayer {
            scaleX = k; scaleY = k
            translationY = drop.toPx() * press
            transformOrigin = TransformOrigin(pivotX, pivotY)
        }
    BoxWithConstraints(
        if (enabled) base.clickable(interactionSource = source, indication = null, onClick = onClick) else base
    ) { content() }
}
