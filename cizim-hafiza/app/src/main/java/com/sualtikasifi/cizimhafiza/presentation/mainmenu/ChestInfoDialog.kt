package com.sualtikasifi.cizimhafiza.presentation.mainmenu

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.integerArrayResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.presentation.common.BackdropCache

// The guide is one finished picture per language (chests_info in drawable-nodpi = English, drawable-tr-nodpi = Turkish,
// drawable-es-nodpi = Spanish): the title, the three steps, the three chests with their gold and jokers and the ad tip are
// all painted into it, and so is the "Anladım" / "Got it" button, which the code only makes tappable and lets cave in.
// Each language's picture has its own size and button, so both come with it from res/values*/chest_info_art.xml.

/**
 * "Kasalar nasıl çalışır?": the painted guide picture over a dimmed screen. The numbers in it (gold, jokers, times) are
 * part of the picture, so they must be repainted when the chest values in [ChestTier] change.
 */
@Composable
internal fun ChestInfoDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            val context = LocalContext.current
            val sheet = remember { runCatching { BackdropCache.get(context.resources, R.drawable.chests_info) }.getOrNull() }
            // read as resources, so a language switched while the guide is open is followed
            val title = stringResource(R.string.home_chests_info_title)
            val ok = stringResource(R.string.home_chests_info_ok)
            // [width, height, left, top, right, bottom] of the picture this language uses, in its own pixels
            val art = integerArrayResource(R.array.chest_info_art)
            val artW = sheet?.width?.toFloat() ?: art[0].toFloat()
            val artH = sheet?.height?.toFloat() ?: art[1].toFloat()
            val frameW = minOf(maxWidth - 16.dp, 480.dp, (maxHeight - 32.dp) * (artW / artH))
            val u = frameW / artW
            // measured on the full-size picture, scaled to the bitmap actually decoded
            val sx = artW / art[0]; val sy = artH / art[1]
            val x0 = art[2] * sx; val y0 = art[3] * sy; val x1 = art[4] * sx; val y1 = art[5] * sy
            val sink = rememberSink(0.94f)
            val enter = remember { Animatable(0f) }
            LaunchedEffect(Unit) { enter.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 380f)) }
            Box(
                Modifier
                    .size(frameW, frameW * (artH / artW))
                    .graphicsLayer {
                        val e = enter.value
                        scaleX = 0.85f + 0.15f * e
                        scaleY = 0.85f + 0.15f * e
                        alpha = (e * 3f).coerceIn(0f, 1f)
                    }
                    .semantics { contentDescription = title }
                    // A tap on the guide itself is not a tap outside it.
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
            ) {
                Image(
                    painter = painterResource(R.drawable.chests_info),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .offset(u * x0, u * y0)
                        .size(u * (x1 - x0), u * (y1 - y0))
                        .sunkenArt(sink, sheet, x0, y0, x1 - x0, y1 - y0, u * ((y1 - y0) / 2f))
                        .clickable(interactionSource = sink.source, indication = null, onClick = onDismiss)
                        .semantics { contentDescription = ok }
                )
            }
        }
    }
}
