package com.sualtikasifi.cizimhafiza.presentation.guess

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.presentation.common.InkBrown
import com.sualtikasifi.cizimhafiza.presentation.common.LetteredText
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedStyle
import com.sualtikasifi.cizimhafiza.presentation.common.pressable
import com.sualtikasifi.cizimhafiza.presentation.common.rememberFitScale
import com.sualtikasifi.cizimhafiza.presentation.common.scaledBy

/** The notice scene is painted in this many units; every box below is a rectangle of that picture. */
private const val ArtW = 841f
private const val ArtH = 1870f
private val PurpleOutline = Color(0xFF3C1568)
private val ButtonOutline = Color(0xFF8A3A00)

/**
 * The page a Guess It link shows when there is nothing to guess: the player's own drawing, a link that no longer works,
 * no connection, or the feature turned off. One painted scene (guess_notice_bg: sign, dog, purple ribbon, a yellow strip,
 * a paper plane, the orange button, all left empty) with the texts laid on it.
 */
@Composable
internal fun GuessNoticeScreen(
    problem: GuessProblem,
    onRetry: () -> Unit,
    onClose: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val title = stringResource(
        when (problem) {
            GuessProblem.Own -> R.string.guess_own_title
            GuessProblem.NotFound -> R.string.guess_gone_title
            GuessProblem.Offline -> R.string.guess_offline_title
            GuessProblem.Child -> R.string.guess_child_title
        }
    )
    val body = stringResource(
        when (problem) {
            GuessProblem.Own -> R.string.guess_own_body
            GuessProblem.NotFound -> R.string.guess_gone
            GuessProblem.Offline -> R.string.guess_offline
            GuessProblem.Child -> R.string.guess_child
        }
    )
    val retry = problem == GuessProblem.Offline

    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFF5A2A0C))) {
        val density = LocalDensity.current
        val sc = maxOf(with(density) { maxWidth.toPx() } / ArtW, with(density) { maxHeight.toPx() } / ArtH)
        val u: Dp = with(density) { sc.toDp() }
        val offX: Dp = (maxWidth - u * ArtW) / 2f
        val fontScale = density.fontScale
        fun fs(art: Float) = (art * u.value / fontScale).sp
        fun Modifier.art(x: Float, y: Float, w: Float, h: Float) = offset(offX + u * x, u * y).size(u * w, u * h)

        Image(
            painter = painterResource(R.drawable.guess_notice_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxSize()
        )

        // The painted back button in the corner.
        Box(Modifier.art(20f, 90f, 130f, 125f).pressable(pressedScale = 0.9f, onClick = onBack))

        // The sign; the dog covers its right end, so the title keeps to the left of him.
        NoticeText(
            text = stringResource(R.string.guess_page_title),
            size = fs(62f),
            color = Color(0xFF3B2314),
            maxLines = 1,
            modifier = Modifier.art(170f, 172f, 350f, 104f)
        )

        // The purple ribbon.
        LetteredText(
            text = title,
            size = fs(56f),
            outline = PurpleOutline,
            modifier = Modifier.art(108f, 812f, 630f, 100f),
            minScale = 0.5f
        )

        // The yellow strip.
        NoticeText(
            text = body,
            size = fs(40f),
            color = InkBrown,
            maxLines = 3,
            modifier = Modifier.art(125f, 985f, 600f, 140f)
        )

        // The orange button between the two paws.
        LetteredText(
            text = stringResource(if (retry) R.string.guess_retry else R.string.close),
            size = fs(58f),
            outline = ButtonOutline,
            modifier = Modifier.art(280f, 1305f, 285f, 100f),
            minScale = 0.5f
        )
        Box(Modifier.art(150f, 1290f, 540f, 135f).pressable(pressedScale = 0.96f, onClick = if (retry) onRetry else onClose))
    }
}

/** Body lettering on the paper: centred, and shrunk (never cut, never wrapped past its box) to fit whatever language it is in. */
@Composable
private fun NoticeText(
    text: String,
    size: androidx.compose.ui.unit.TextUnit,
    color: Color,
    maxLines: Int,
    modifier: Modifier
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val style = PaintedStyle(color = color, fontSize = size, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = size * 1.2f)
        val scale = rememberFitScale(text, style, maxLines, 0.5f)
        Text(text = text, style = style.scaledBy(scale), maxLines = maxLines, modifier = Modifier.fillMaxWidth())
    }
}
