package com.sualtikasifi.cizimhafiza.presentation.duel

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.presentation.common.InkBrown
import com.sualtikasifi.cizimhafiza.presentation.common.LetteredText
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedStyle
import com.sualtikasifi.cizimhafiza.presentation.common.pressable
import com.sualtikasifi.cizimhafiza.presentation.common.rememberFitScale
import com.sualtikasifi.cizimhafiza.presentation.common.scaledBy
import com.sualtikasifi.cizimhafiza.util.GameConstants

/** The challenge page's painted scene is drawn in this many units; everything below is placed in them. */
private const val ArtW = 841f
private const val ArtH = 1870f
private val PurpleOutline = Color(0xFF3C1568)
private val ButtonOutline = Color(0xFF8A3A00)

/**
 * Picks how many words the challenge round should be, then hands off to the
 * normal Game destination (see Screen.duelChallengeRoute) — the challenger
 * plays an ordinary solo round exactly like free play; only the word list
 * being random-mix (no category/difficulty picker here, unlike CreateRoomScreen)
 * and the fact that its result also becomes a challenge for [opponentName]
 * make this a duel. No ViewModel: nothing here needs a repository, the
 * actual challenge is created once the round finishes (see GameViewModel).
 *
 * The page is one painted scene (duel_create_bg: the arena, the sign, two paper cards, a button, all left empty); the
 * texts and the word-count choice are laid over it, so every box below is a rectangle of that picture.
 */
@Composable
fun CreateDuelScreen(
    opponentName: String,
    onBack: () -> Unit,
    onChallengeStarted: (wordCount: Int) -> Unit
) {
    var selectedCount by remember { mutableIntStateOf(GameConstants.WORD_COUNT_OPTIONS.first()) }
    BackHandler(onBack = onBack)

    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFF2A1233))) {
        val density = LocalDensity.current
        val sc = maxOf(with(density) { maxWidth.toPx() } / ArtW, with(density) { maxHeight.toPx() } / ArtH)
        val u: Dp = with(density) { sc.toDp() }
        val offX: Dp = (maxWidth - u * ArtW) / 2f
        val fontScale = density.fontScale
        fun fs(art: Float) = (art * u.value / fontScale).sp
        fun Modifier.art(x: Float, y: Float, w: Float, h: Float) = offset(offX + u * x, u * y).size(u * w, u * h)

        Image(
            painter = painterResource(R.drawable.duel_create_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxSize()
        )

        // The painted back button in the corner.
        Box(Modifier.art(10f, 14f, 120f, 110f).pressable(pressedScale = 0.9f, onClick = onBack))

        // The sign: the dog covers its right end, so the title keeps to the left of him.
        LetteredText(
            text = stringResource(R.string.duel_create_title),
            size = fs(66f),
            modifier = Modifier.art(128f, 352f, 372f, 96f),
            minScale = 0.5f,
            title = true
        )

        // The first card: the opponent on the purple ribbon, the explanation on the paper, the streak hint on its strip.
        LetteredText(
            text = stringResource(R.string.duel_create_opponent, opponentName),
            size = fs(50f),
            outline = PurpleOutline,
            modifier = Modifier.art(190f, 596f, 462f, 76f),
            minScale = 0.5f
        )
        FitText(
            text = stringResource(R.string.duel_create_explainer, opponentName),
            size = fs(30f),
            color = InkBrown,
            maxLines = 8,
            modifier = Modifier.art(78f, 694f, 685f, 246f)
        )
        FitText(
            text = stringResource(R.string.duel_create_streak_hint),
            size = fs(29f),
            color = InkBrown,
            maxLines = 3,
            modifier = Modifier.art(236f, 974f, 522f, 94f)
        )

        // The second card: how many words.
        LetteredText(
            text = stringResource(R.string.select_word_count),
            size = fs(38f),
            outline = PurpleOutline,
            modifier = Modifier.art(168f, 1192f, 506f, 68f),
            minScale = 0.5f
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(u * 16f),
            modifier = Modifier.art(50f, 1290f, 741f, 108f)
        ) {
            GameConstants.WORD_COUNT_OPTIONS.forEach { count ->
                CountCard(
                    count = count, selected = count == selectedCount, u = u, fs = ::fs,
                    onClick = { selectedCount = count },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // The button: the picture holds the brushes and the arrow, the words go between them.
        Box(
            Modifier.art(45f, 1470f, 750f, 152f).clickable(
                interactionSource = remember { MutableInteractionSource() }, indication = null,
                onClick = { onChallengeStarted(selectedCount) }
            )
        )
        LetteredText(
            text = stringResource(R.string.duel_create_start),
            size = fs(62f),
            outline = ButtonOutline,
            modifier = Modifier.art(268f, 1503f, 380f, 88f),
            minScale = 0.5f
        )
    }
}

/** One answer of the word-count choice: cream while open, the game's orange with a tick once chosen. */
@Composable
private fun CountCard(
    count: Int,
    selected: Boolean,
    u: Dp,
    fs: (Float) -> androidx.compose.ui.unit.TextUnit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(u * 28f)
    Box(modifier.fillMaxSize()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .shadow(if (selected) 6.dp else 3.dp, shape)
                .background(
                    if (selected) Brush.verticalGradient(listOf(Color(0xFFFFB347), Color(0xFFF47721)))
                    else Brush.verticalGradient(listOf(Color(0xFFFFFAEE), Color(0xFFF6E3BE))),
                    shape
                )
                .border(u * 4f, if (selected) Color(0xFFB04A0E) else Color(0xFFC9A36B), shape)
                .pressable(pressedScale = 0.94f, onClick = onClick)
        ) {
            if (selected) LetteredText("$count", fs(56f), outline = ButtonOutline, modifier = Modifier.fillMaxSize(0.8f))
            else Text("$count", style = PaintedStyle(color = Color(0xFF2B1A4A), fontSize = fs(56f), textAlign = TextAlign.Center), maxLines = 1)
        }
        if (selected) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(u * 8f, u * -14f)
                    .size(u * 40f)
                    .background(Color(0xFFF47721), CircleShape)
                    .border(u * 3f, Color.White, CircleShape)
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(u * 26f))
            }
        }
    }
}

/** Body lettering on the paper: centred, and shrunk (never cut, never wrapped past its box) to fit whatever language it is in. */
@Composable
private fun FitText(
    text: String,
    size: androidx.compose.ui.unit.TextUnit,
    color: Color,
    maxLines: Int,
    modifier: Modifier
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val style = PaintedStyle(color = color, fontSize = size, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = size * 1.22f)
        val scale = fitScale(text, style, maxLines)
        Text(text = text, style = style.scaledBy(scale), maxLines = maxLines, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun BoxWithConstraintsScope.fitScale(text: String, style: androidx.compose.ui.text.TextStyle, maxLines: Int): Float =
    rememberFitScale(text, style, maxLines, 0.55f)
