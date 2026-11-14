package com.sualtikasifi.cizimhafiza.presentation.mainmenu

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.presentation.common.FitText
import com.sualtikasifi.cizimhafiza.presentation.common.LetteredText
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedStyle
import com.sualtikasifi.cizimhafiza.presentation.common.breathing
import com.sualtikasifi.cizimhafiza.presentation.common.glint
import com.sualtikasifi.cizimhafiza.presentation.common.pressable
import com.sualtikasifi.cizimhafiza.presentation.common.springIn

/** Where the double-XP question is: asking, or told that the 30 minutes have started. */
enum class XpOfferStep { Ask, Started }

/** The window is one painted picture (xp_offer_window, 1035 x 1315 units); every box below is a rectangle of it. */
private const val ArtW = 1035f
private const val ArtH = 1315f
private val OfferInk = Color(0xFF3B2314)
private val RibbonOutline = Color(0xFF3A0F5E)
private val ButtonOutline = Color(0xFF8A3A00)
private val Plum = Color(0xFF6B2DB5)

/**
 * The question asked on a tap on "Hızlı Eşleş": watch an ad for 30 minutes of double XP, or not now (the match then simply
 * starts). Once the ad is watched the same window says the 30 minutes have begun, with one button into the match.
 * The picture carries the ribbon, the ×2 XP star, the speech bubble and the two buttons; the words are laid over them.
 */
@Composable
fun XpBoostOfferDialog(step: XpOfferStep, onWatch: () -> Unit, onSkip: () -> Unit, onPlay: () -> Unit) {
    val asking = step == XpOfferStep.Ask
    Dialog(onDismissRequest = if (asking) onSkip else onPlay, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = if (asking) onSkip else onPlay),
            contentAlignment = Alignment.Center
        ) {
            val frameW = minOf(maxWidth - 20.dp, (maxHeight - 40.dp) * (ArtW / ArtH), 460.dp)
            val u: Dp = frameW / ArtW
            val fontScale = LocalDensity.current.fontScale
            fun fs(art: Float) = (art * u.value / fontScale).sp
            fun Modifier.art(x: Float, y: Float, w: Float, h: Float) = offset(u * x, u * y).size(u * w, u * h)

            Box(
                Modifier
                    .size(frameW, frameW * (ArtH / ArtW))
                    .springIn(fromY = 60, stepMs = 0)
                    // swallow taps on the window itself so only the dim area closes it
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
            ) {
                Image(
                    painter = painterResource(if (asking) R.drawable.xp_offer_window else R.drawable.xp_offer_window_started),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )

                // The ribbon: the title, in white with a deep purple edge.
                LetteredText(
                    text = stringResource(if (asking) R.string.xp_offer_title else R.string.xp_offer_started_title),
                    size = fs(104f),
                    outline = RibbonOutline,
                    modifier = Modifier.art(250f, 150f, 540f, 118f),
                    minScale = 0.45f,
                    title = true
                )

                // The speech bubble, right of the alarm clock.
                val duration = stringResource(R.string.xp_offer_minutes)
                val full = stringResource(if (asking) R.string.xp_offer_body else R.string.xp_offer_started_body, duration)
                val text = buildAnnotatedString {
                    val i = full.indexOf(duration)
                    if (i < 0) append(full) else {
                        append(full.substring(0, i))
                        withStyle(SpanStyle(background = Plum, color = Color.White)) { append(" $duration ") }
                        append(full.substring(i + duration.length))
                    }
                }
                Box(Modifier.art(285f, 640f, 660f, 210f), contentAlignment = Alignment.Center) {
                    val style = PaintedStyle(color = OfferInk, fontSize = fs(50f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = fs(64f))
                    FitAnnotated(text, style)
                }

                // The orange button: the film icon and the words.
                val main = Modifier.art(150f, 893f, 735f, 184f)
                Box(
                    main
                        .breathing(0.018f, 1500)
                        .glint(3200, 0.38f, u * 92f)
                        .pressable(pressedScale = 0.96f, onClick = if (asking) onWatch else onPlay),
                    contentAlignment = Alignment.Center
                ) {
                    Row(Modifier.padding(bottom = u * 14f), verticalAlignment = Alignment.CenterVertically) {
                        if (asking) {
                            Icon(Icons.Filled.Movie, contentDescription = null, tint = Color.White, modifier = Modifier.size(u * 88f))
                            Spacer(Modifier.width(u * 22f))
                        }
                        LetteredText(
                            text = stringResource(if (asking) R.string.xp_offer_watch else R.string.xp_offer_play),
                            size = fs(78f),
                            outline = ButtonOutline,
                            modifier = Modifier.size(width = u * (if (asking) 430f else 520f), height = u * 100f),
                            minScale = 0.5f
                        )
                    }
                }

                // The cream button ("not now"): only while asking.
                if (asking) {
                    Box(
                        Modifier.art(258f, 1100f, 517f, 120f).pressable(pressedScale = 0.96f, onClick = onSkip),
                        contentAlignment = Alignment.Center
                    ) {
                        FitText(
                            text = stringResource(R.string.xp_offer_skip),
                            style = PaintedStyle(color = OfferInk, fontSize = fs(54f), fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center),
                            minScale = 0.55f,
                            modifier = Modifier.fillMaxSize().padding(horizontal = u * 30f)
                        )
                    }
                }
            }
        }
    }
}

/** Body text that shrinks until it fits its box, keeping a highlighted word. */
@Composable
private fun FitAnnotated(text: androidx.compose.ui.text.AnnotatedString, style: androidx.compose.ui.text.TextStyle) {
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        var scale by remember(text) { mutableFloatStateOf(1f) }
        Text(
            text = text,
            style = style.copy(fontSize = style.fontSize * scale.toDouble(), lineHeight = style.lineHeight * scale.toDouble()),
            maxLines = 4,
            textAlign = TextAlign.Center,
            onTextLayout = { r -> if (r.hasVisualOverflow && scale > 0.55f) scale -= 0.06f },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
