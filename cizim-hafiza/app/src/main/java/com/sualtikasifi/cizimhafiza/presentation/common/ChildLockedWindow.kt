package com.sualtikasifi.cizimhafiza.presentation.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sualtikasifi.cizimhafiza.R

private const val ArtW = 1090f
private const val ArtH = 1225f

/**
 * The page a player under 13 meets in place of the online screens: the painted card (child_locked_window: the sad
 * mascot over the scroll, the three locked features, the big button) with every word written live on top, so each
 * language gets its own. The card's own lettering was cleaned out of the picture for that.
 */
@Composable
fun ChildLockedWindow(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF28140C))
            .clickable(onClick = {}),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 20.dp), contentAlignment = Alignment.Center) {
            val frameW = minOf(maxWidth, 440.dp, (maxHeight) * (ArtW / ArtH))
            val u = frameW / ArtW
            val fontScale0 = LocalDensity.current.fontScale
            fun fs(px: Float) = (px * u.value / fontScale0).sp
            fun Modifier.at(x: Float, y: Float, w: Float, h: Float): Modifier = offset(u * x, u * y).size(u * w, u * h)
            val ink = Color(0xFF3A1E10)
            Box(Modifier.size(frameW, frameW * (ArtH / ArtW))) {
                Image(painterResource(R.drawable.child_locked_window), null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
                // The title, on a yellow brush stroke drawn here (the picture's own was lettered over)
                Box(
                    Modifier.at(192f, 296f, 730f, 92f)
                        .clip(RoundedCornerShape(50))
                        .background(Brush.verticalGradient(listOf(Color(0xFFFFE070), Color(0xFFFFC82E)))),
                    contentAlignment = Alignment.Center
                ) {
                    FitText(
                        text = stringResource(R.string.child_locked_title),
                        style = PaintedStyle(color = ink, fontSize = fs(78f), textAlign = TextAlign.Center),
                        maxLines = 1,
                        minScale = 0.5f,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = u * 40f)
                    )
                }
                FitText(
                    text = stringResource(R.string.child_locked_body),
                    style = DescriptionStyle(fs(47f), fs(60f)).copy(color = Color(0xFF4A2A18)),
                    maxLines = 6,
                    minScale = 0.55f,
                    modifier = Modifier.at(92f, 428f, 925f, 232f)
                )
                val plaqueInk = Color(0xFF8A0A14)
                LetteredText(stringResource(R.string.menu_friends), fs(42f), outline = plaqueInk, maxLines = 1, minScale = 0.5f, modifier = Modifier.at(162f, 879f, 175f, 46f))
                LetteredText(stringResource(R.string.duel_list_title), fs(42f), outline = plaqueInk, maxLines = 1, minScale = 0.5f, modifier = Modifier.at(498f, 879f, 165f, 46f))
                LetteredText(stringResource(R.string.menu_play_online), fs(40f), outline = plaqueInk, maxLines = 2, minScale = 0.5f, modifier = Modifier.at(816f, 868f, 185f, 66f))
                // The big button is part of the picture; this is its touch area and its word
                Box(
                    Modifier.at(192f, 992f, 705f, 170f).clip(RoundedCornerShape(50)).clickable(onClick = onBack).a11yButton(stringResource(R.string.cd_back)),
                    contentAlignment = Alignment.Center
                ) {
                    LetteredText(stringResource(R.string.cd_back), fs(80f), outline = Color(0xFF8A3A00), maxLines = 1, minScale = 0.5f, modifier = Modifier.size(u * 305f, u * 84f))
                }
            }
        }
    }
}
