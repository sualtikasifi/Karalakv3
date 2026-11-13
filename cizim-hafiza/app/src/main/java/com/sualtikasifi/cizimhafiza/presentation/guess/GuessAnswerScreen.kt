package com.sualtikasifi.cizimhafiza.presentation.guess

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.presentation.common.InkBrown
import com.sualtikasifi.cizimhafiza.presentation.common.LetteredText
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedStyle
import com.sualtikasifi.cizimhafiza.presentation.common.ReplayStrokeCanvas
import com.sualtikasifi.cizimhafiza.presentation.common.pressable
import com.sualtikasifi.cizimhafiza.util.GuessCodec

/** The answer scene is painted in this many units; every box below is a rectangle of that picture. */
private const val ArtW = 841f
private const val ArtH = 1870f
private val PurpleOutline = Color(0xFF3C1568)
private val ButtonOutline = Color(0xFF8A3A00)
private val PurpleInk = Color(0xFF5B3A9A)
private val DotOn = Color(0xFFF47721)
private val DotOff = Color(0xFFF1D7A4)

/**
 * The guessing page of a Guess It link: a friend's drawing draws itself on the painted board, then three tries to name it.
 * One painted scene (guess_answer_bg: sign, purple ribbon, yellow strip, the board, a replay pill, the answer field, the
 * orange guess button, a promo ribbon with the dog and a white pill, all left empty) with the live parts laid on it. The
 * scene is as tall as the picture and scrolls when the phone is shorter or the keyboard is up.
 */
@Composable
internal fun GuessAnswerScreen(
    state: GuessUiState,
    onReplay: () -> Unit,
    onReplayFinished: () -> Unit,
    onInput: (String) -> Unit,
    onSubmit: () -> Unit,
    onGiveUp: () -> Unit,
    onBack: () -> Unit
) {
    val link = state.link ?: return
    val who = link.ownerName.ifBlank { stringResource(R.string.guess_someone) }
    val canAnswer = state.replayDone
    val canGuess = canAnswer && state.input.isNotBlank()
    androidx.activity.compose.BackHandler(onBack = onBack)

    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFF7A4A22)).imePadding()) {
        val density = LocalDensity.current
        val sc = maxOf(with(density) { maxWidth.toPx() } / ArtW, with(density) { maxHeight.toPx() } / ArtH)
        val u: Dp = with(density) { sc.toDp() }
        val offX: Dp = (maxWidth - u * ArtW) / 2f
        val fontScale = density.fontScale
        fun fs(art: Float) = (art * u.value / fontScale).sp
        fun Modifier.art(x: Float, y: Float, w: Float, h: Float) = offset(offX + u * x, u * y).size(u * w, u * h)

        val scroll = rememberScrollState()
        var positioned by remember { mutableStateOf(false) }
        // A phone shorter than the picture starts scrolled just far enough to show the bottom pill (only the top lights are lost).
        LaunchedEffect(scroll.maxValue) {
            if (!positioned && scroll.maxValue > 0) {
                scroll.scrollTo((1775f * sc - with(density) { maxHeight.toPx() }).toInt().coerceIn(0, scroll.maxValue))
                positioned = true
            }
        }
        Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
            Box(Modifier.fillMaxWidth().height(u * ArtH)) {
                Image(
                    painter = painterResource(R.drawable.guess_answer_bg),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier.fillMaxSize()
                )

                // The painted back button and the sign (the dog covers its right end).
                Box(Modifier.art(25f, 90f, 130f, 125f).pressable(pressedScale = 0.9f, onClick = onBack))
                NoticeText(
                    text = stringResource(R.string.guess_page_title),
                    size = fs(62f),
                    color = Color(0xFF3B2314),
                    maxLines = 1,
                    modifier = Modifier.art(150f, 126f, 350f, 92f)
                )

                // The purple ribbon: who drew it. The yellow strip: the rule, then the hints once a guess missed.
                LetteredText(
                    text = stringResource(R.string.guess_title, who),
                    size = fs(54f),
                    outline = PurpleOutline,
                    modifier = Modifier.art(160f, 312f, 520f, 84f),
                    minScale = 0.45f
                )
                if (state.attempts > 0) {
                    Column(Modifier.art(190f, 422f, 490f, 80f), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (state.hint.isNotEmpty()) {
                            NoticeText(
                                text = state.hint,
                                size = fs(34f),
                                color = InkBrown,
                                maxLines = 1,
                                modifier = Modifier.fillMaxWidth().height(u * if (state.wrong.isNotEmpty()) 46f else 80f)
                            )
                        }
                        if (state.wrong.isNotEmpty()) {
                            NoticeText(
                                text = state.wrong.joinToString("  ·  "),
                                size = fs(25f),
                                color = Color(0xFFB3261E),
                                maxLines = 1,
                                modifier = Modifier.fillMaxWidth().height(u * if (state.hint.isNotEmpty()) 32f else 80f)
                            )
                        }
                    }
                } else {
                    NoticeText(
                        text = stringResource(R.string.guess_subtitle),
                        size = fs(36f),
                        color = InkBrown,
                        maxLines = 1,
                        modifier = Modifier.art(190f, 427f, 490f, 70f)
                    )
                }

                // The board: the drawing draws itself, fitted into it whatever its shape.
                ReplayStrokeCanvas(
                    strokes = link.strokes,
                    modifier = Modifier.art(100f, 568f, 645f, 362f),
                    playToken = state.playToken,
                    onFinished = onReplayFinished
                )

                // Tries left: orange dots, the used ones pale. They cover the three painted ones.
                repeat(GuessCodec.MAX_ATTEMPTS) { i ->
                    val left = i < GuessCodec.MAX_ATTEMPTS - state.attempts
                    Box(
                        Modifier
                            .art(378f + 44f * i - 17f, 1012f - 17f, 34f, 34f)
                            .background(if (left) Brush.verticalGradient(listOf(Color(0xFFFFA24D), DotOn)) else SolidColor(DotOff), CircleShape)
                            .border(u * 2f, if (left) Color(0xFFD9600F) else Color(0xFFE6C48A), CircleShape)
                    )
                }

                // "Watch again": the label sits right of the painted replay arrow.
                NoticeText(
                    text = stringResource(R.string.guess_replay),
                    size = fs(33f),
                    color = PurpleInk,
                    maxLines = 1,
                    modifier = Modifier.art(372f, 1048f, 205f, 54f)
                )
                Box(Modifier.art(250f, 1038f, 342f, 70f).pressable(pressedScale = 0.96f, onClick = onReplay))

                // The answer field, right of the painted pencil.
                BasicTextField(
                    value = state.input,
                    onValueChange = onInput,
                    enabled = canAnswer,
                    singleLine = true,
                    textStyle = TextStyle(color = PurpleInk, fontSize = fs(42f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                    cursorBrush = SolidColor(DotOn),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            if (state.input.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.guess_placeholder),
                                    style = TextStyle(color = Color(0xFFB59AD6), fontSize = fs(42f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                                    maxLines = 1
                                )
                            }
                            inner()
                        }
                    },
                    modifier = Modifier.art(165f, 1140f, 590f, 80f)
                )

                // The orange button; dimmed until there is something to send.
                LetteredText(
                    text = stringResource(R.string.guess_submit),
                    size = fs(60f),
                    outline = ButtonOutline,
                    modifier = Modifier.art(320f, 1268f, 400f, 90f),
                    minScale = 0.5f
                )
                if (!canGuess) Box(Modifier.art(98f, 1256f, 646f, 110f).background(Color(0x99F6E6C8), RoundedCornerShape(u * 55f)))
                else Box(Modifier.art(98f, 1256f, 646f, 110f).pressable(pressedScale = 0.97f, onClick = onSubmit))

                // The promo ribbon, right of the dog.
                NoticeText(
                    text = stringResource(R.string.guess_promo_1),
                    size = fs(33f),
                    color = Color.White,
                    maxLines = 2,
                    modifier = Modifier.art(280f, 1440f, 355f, 86f)
                )
                NoticeText(
                    text = stringResource(R.string.guess_promo_2),
                    size = fs(35f),
                    color = Color(0xFFFFE04D),
                    maxLines = 2,
                    modifier = Modifier.art(262f, 1528f, 410f, 70f)
                )

                // The white pill: skip. It covers the painted share icon so the label sits in the middle.
                Box(Modifier.art(300f, 1660f, 75f, 66f).background(Color.White, CircleShape))
                NoticeText(
                    text = stringResource(R.string.guess_give_up),
                    size = fs(48f),
                    color = Color(0xFFE8751A),
                    maxLines = 1,
                    modifier = Modifier.art(285f, 1655f, 285f, 76f)
                )
                if (!canAnswer) Box(Modifier.art(255f, 1648f, 330f, 96f).background(Color(0x88FFFFFF), RoundedCornerShape(u * 48f)))
                else Box(Modifier.art(255f, 1648f, 330f, 96f).pressable(pressedScale = 0.96f, onClick = onGiveUp))
            }
        }
    }
}
