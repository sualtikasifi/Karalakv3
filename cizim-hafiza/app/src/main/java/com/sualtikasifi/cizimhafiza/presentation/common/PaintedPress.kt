package com.sualtikasifi.cizimhafiza.presentation.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import com.sualtikasifi.cizimhafiza.presentation.mainmenu.rememberSink
import com.sualtikasifi.cizimhafiza.presentation.mainmenu.sinkWith
import com.sualtikasifi.cizimhafiza.presentation.mainmenu.sunkenArt

/**
 * A button that is PAINTED into a window's picture ([artRes]) and so cannot shrink itself: the area (given as fractions of
 * the picture, [left] [top] [width] [height]) is placed over the picture inside a parent of [parentW] x [parentH], and
 * while a finger is down the picture's own pixels there are drawn again, shrunk, over a darker recess — the button visibly
 * caves in, like the room buttons and the store's free-joker button do. What the area carries ([content], laid out in the
 * area's own size) dips together with it, so the lettering never slides off its button. A disabled area does neither.
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
    onClick: () -> Unit,
    content: @Composable BoxWithConstraintsScope.() -> Unit = {}
) {
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
