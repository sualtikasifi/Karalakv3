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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.presentation.common.BackdropCache

// The guide is one finished picture per language (chests_info in drawable-nodpi = English, drawable-tr-nodpi = Turkish):
// the title, the three steps, the three chests with their gold and jokers and the ad tip are all painted into it, and
// so is the "Anladım" / "Got it" button, which the code only makes tappable and lets cave in.
private const val InfoArtW = 922f

/** The painted button, as fractions of the picture's size (the two language versions are not exactly the same height). */
private fun buttonFractions(artH: Float): FloatArray =
    if (artH > 1690f) floatArrayOf(175f / InfoArtW, 1500f / artH, 750f / InfoArtW, 1625f / artH)   // Turkish, 922 x 1694
    else floatArrayOf(175f / InfoArtW, 1498f / artH, 750f / InfoArtW, 1622f / artH)               // English, 922 x 1673

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
            val artW = sheet?.width?.toFloat() ?: InfoArtW
            val artH = sheet?.height?.toFloat() ?: 1694f
            val frameW = minOf(maxWidth - 16.dp, 480.dp, (maxHeight - 32.dp) * (artW / artH))
            val u = frameW / artW
            val b = buttonFractions(artH)
            val x0 = b[0] * artW; val y0 = b[1] * artH; val x1 = b[2] * artW; val y1 = b[3] * artH
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
                    .semantics { contentDescription = context.getString(R.string.home_chests_info_title) }
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
                        .semantics { contentDescription = context.getString(R.string.home_chests_info_ok) }
                )
            }
        }
    }
}
