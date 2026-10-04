package com.sualtikasifi.cizimhafiza.presentation.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedStyle
import com.sualtikasifi.cizimhafiza.domain.model.JokerType
import com.sualtikasifi.cizimhafiza.presentation.common.shortRes
import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sualtikasifi.cizimhafiza.R
import kotlinx.coroutines.delay
import com.sualtikasifi.cizimhafiza.domain.model.DrawingStroke
import com.sualtikasifi.cizimhafiza.domain.model.PenSkin
import com.sualtikasifi.cizimhafiza.presentation.common.CircularCountdown
import com.sualtikasifi.cizimhafiza.presentation.common.DrawTool
import com.sualtikasifi.cizimhafiza.presentation.common.DrawableCanvas
import com.sualtikasifi.cizimhafiza.presentation.common.EraserGlyph
import com.sualtikasifi.cizimhafiza.presentation.common.PrimaryButton
import com.sualtikasifi.cizimhafiza.presentation.common.GameTopBar
import com.sualtikasifi.cizimhafiza.presentation.common.RaisedCard
import com.sualtikasifi.cizimhafiza.presentation.common.RaisedIconButton
import com.sualtikasifi.cizimhafiza.presentation.common.SecondaryButton
import com.sualtikasifi.cizimhafiza.presentation.common.StatPill
import com.sualtikasifi.cizimhafiza.presentation.common.TintedBadge
import com.sualtikasifi.cizimhafiza.presentation.common.currentWordLanguage
import com.sualtikasifi.cizimhafiza.presentation.common.dotGridBackground
import com.sualtikasifi.cizimhafiza.presentation.common.hardEdge
import com.sualtikasifi.cizimhafiza.presentation.common.screenBackground
import com.sualtikasifi.cizimhafiza.presentation.theme.AppTheme
import com.sualtikasifi.cizimhafiza.presentation.theme.TimerWarning
import com.sualtikasifi.cizimhafiza.util.capitalizeForWordLanguage
import com.sualtikasifi.cizimhafiza.util.GameConstants

@Composable
fun DrawingScreen(
    state: GamePhase.Drawing,
    onStrokeFinished: (Int, DrawingStroke) -> Unit,
    onStrokeProgress: (Int, DrawingStroke) -> Unit,
    onClearCanvas: () -> Unit,
    onEraseStroke: (DrawingStroke) -> Unit,
    onUndoLastStroke: () -> Unit,
    onNextWord: () -> Unit,
    onBackClick: () -> Unit,
    onHintClick: () -> Unit = {},
    timeJokerCount: Int = 0,
    onTimeJoker: () -> Unit = {},
    musicEnabled: Boolean = true,
    onToggleMusic: () -> Unit = {},
    adUnavailable: Boolean = false,
    onAdUnavailableShown: () -> Unit = {},
    /** The player's chosen cosmetic pen (see domain.model.PenSkin); defaults keep the tutorial's call site unchanged. */
    penSkin: PenSkin = PenSkin.DEFAULT
) {
    val wordLanguage = currentWordLanguage()
    val backDescription = stringResource(R.string.cd_back)
    val timerColor = if (state.isWarning) TimerWarning else MaterialTheme.colorScheme.primary
    // Keyed on the word so every new turn starts on the pen again — finishing
    // one word with the eraser selected shouldn't leave the next word's blank
    // canvas in eraser mode, where the first strokes would silently do nothing.
    var tool by remember(state.word.id) { mutableStateOf(DrawTool.PEN) }

    // Guards against a double-tap firing two rewarded-ad loads for the same
    // click — resets per word, though once state.hintUsed flips true the
    // button is gone for the rest of the match anyway.
    var hintRequested by remember(state.word.id) { mutableStateOf(false) }
    // An ad that never loaded used to leave this button reading
    // "Yükleniyor…" and disabled until the word changed. Now the attempt
    // resolves: the button comes back, and a line says why nothing
    // happened.
    var adErrorShown by remember { mutableStateOf(false) }
    LaunchedEffect(adUnavailable) {
        if (adUnavailable) {
            hintRequested = false
            adErrorShown = true
            onAdUnavailableShown()
        }
    }
    if (adErrorShown) {
        LaunchedEffect(Unit) {
            delay(3_000)
            adErrorShown = false
        }
    }

    // Painted desk scene (bg_draw). Everything is placed in "art units": the picture is 841 wide, so one unit is
    // maxWidth / 841. Things at the top are measured from the top edge, things in the tray from the bottom edge, and
    // the canvas takes whatever height is left in between (the middle of the picture is only straight frame sides,
    // so it can stretch without anything looking squashed).
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val unit = maxWidth / ArtWidth
        fun a(v: Float): Dp = unit * v
        val screenHeight = maxHeight
        fun fromBottom(artY: Float): Dp = screenHeight - a(ArtHeight - artY)
        val f = (maxWidth.value / 411f).coerceIn(0.85f, 1.25f)
        // Phones with a tall status bar would hide the top of the header under it: slide the scene down by the excess.
        val topShift = with(LocalDensity.current) { (WindowInsets.statusBars.getTop(this).toDp() - 38.dp).coerceAtLeast(0.dp) }
        val ink = Color(0xFF3A2A22)
        val noRipple = remember { MutableInteractionSource() }

        ThreeSliceBackground(
            res = R.drawable.bg_draw,
            topSrc = 720,
            bottomSrc = 801,
            topOffset = topShift,
            modifier = Modifier.fillMaxSize()
        )

        Box(modifier = Modifier.fillMaxSize().offset(y = topShift)) {
        // --- Header: back (painted into the picture), clock + progress, music, countdown ring ---
        Box(
            modifier = Modifier
                .offset(a(28f), a(82f))
                .size(a(99f), a(96f))
                .clickable(interactionSource = noRipple, indication = null, onClick = onBackClick)
                .semantics { contentDescription = backDescription }
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.offset(a(152f), a(88f)).size(a(296f), a(82f))
        ) {
            val matchClock = state.matchSecondsRemaining?.let { formatMmSs(it) }
            val progress = "${state.wordNumber} / ${state.totalWords}"
            if (matchClock != null) {
                Icon(Icons.Filled.Timer, contentDescription = null, tint = Color(0xFFF26A1B), modifier = Modifier.size(18.dp * f))
                Spacer(Modifier.width(5.dp))
                Text(matchClock, style = PaintedStyle(color = ink, fontSize = 17.sp * f), maxLines = 1)
                Text("  ·  ", style = PaintedStyle(color = ink, fontSize = 17.sp * f), maxLines = 1)
            }
            Text(progress, style = PaintedStyle(color = ink, fontSize = 17.sp * f), maxLines = 1)
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset(a(593f), a(88f))
                .size(a(97f), a(82f))
                .clickable(interactionSource = noRipple, indication = null, onClick = onToggleMusic)
        ) {
            Icon(
                imageVector = if (musicEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                contentDescription = stringResource(if (musicEnabled) R.string.cd_music_on else R.string.cd_music_off),
                tint = ink,
                modifier = Modifier.size(26.dp * f)
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset(a(712f), a(82f))
                .size(a(98f))
                .background(Color.White, CircleShape)
                .border(1.dp, Color(0x33000000), CircleShape)
        ) {
            if (state.isUntimed) {
                Icon(Icons.Filled.SelfImprovement, contentDescription = null, tint = Color(0xFFF26A1B), modifier = Modifier.size(28.dp * f))
            } else {
                CircularCountdown(
                    secondsLeft = state.secondsLeft,
                    totalSeconds = state.totalSeconds,
                    ringColor = timerColor,
                    trackColor = Color(0xFFFFE3CC),
                    strokeWidth = 5.dp,
                    textStyle = PaintedStyle(color = timerColor, fontSize = 22.sp * f),
                    modifier = Modifier.fillMaxSize().padding(2.dp)
                )
            }
        }

        // --- The word, on the notepad sign ---
        val wordText = state.word.text.capitalizeForWordLanguage(wordLanguage)
        val wordSize = (a(425f).value * 0.97f / (wordText.length.coerceAtLeast(1) * 0.5f)).coerceIn(19f, 34f * f)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.offset(a(135f), a(236f)).size(a(425f), a(104f))
        ) {
            Text(
                text = wordText,
                style = PaintedStyle(color = ink, fontSize = wordSize.sp, textAlign = TextAlign.Center, lineHeight = (wordSize * 1.05f).sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .offset(a(235f), a(343f))
                .size(a(250f), a(8f))
                .background(Color(0xFFF58A1F), RoundedCornerShape(50))
        )

        }

        // --- The canvas ---
        val canvasHeight = (screenHeight - topShift - a(438f) - a(ArtHeight - 1386f)).coerceAtLeast(120.dp)
        key(state.word.id) {
            val wordId = state.word.id
            Box(
                modifier = Modifier
                    .offset(a(36f), a(438f) + topShift)
                    .size(a(770f), canvasHeight)
                    .clip(RoundedCornerShape(a(20f)))
                    .background(AppTheme.tokens.canvasPaper)
                    .dotGridBackground(dotColor = AppTheme.tokens.canvasGrid, spacing = 22.dp, radius = 1.2.dp)
            ) {
                DrawableCanvas(
                    liveStrokes = state.strokes,
                    onStrokeFinished = { onStrokeFinished(wordId, it) },
                    onStrokeProgress = { onStrokeProgress(wordId, it) },
                    tool = tool,
                    onEraseStroke = onEraseStroke,
                    penSkin = penSkin,
                    modifier = Modifier.fillMaxSize()
                )
                if (state.isWarning) {
                    Box(Modifier.fillMaxSize().border(3.dp, timerColor.copy(alpha = 0.55f), RoundedCornerShape(a(20f))))
                }
                if (!state.isUntimed) {
                    TimeJokerPill(
                        count = timeJokerCount,
                        enabled = !state.jokerTimeUsed,
                        onClick = onTimeJoker,
                        modifier = Modifier.align(Alignment.TopEnd).padding(top = a(8f), end = a(10f))
                    )
                }
            }
        }

        // --- Tray: pen, eraser, undo, clear, and the ad / next-word action ---
        val toolSize = a(98f)
        val toolTop = fromBottom(1433f)
        ToolButton(Modifier.offset(a(48f), toolTop), toolSize, selected = tool == DrawTool.PEN, onClick = { tool = DrawTool.PEN }, description = stringResource(R.string.tool_pen)) { tint, sz ->
            Icon(Icons.Filled.Create, contentDescription = null, tint = tint, modifier = Modifier.size(sz))
        }
        ToolButton(Modifier.offset(a(165f), toolTop), toolSize, selected = tool == DrawTool.ERASER, onClick = { tool = DrawTool.ERASER }, description = stringResource(R.string.tool_eraser)) { tint, sz ->
            EraserGlyph(tint = tint, size = sz)
        }
        ToolButton(Modifier.offset(a(290f), toolTop), toolSize, selected = false, enabled = state.strokes.isNotEmpty(), onClick = onUndoLastStroke, description = stringResource(R.string.tool_undo)) { tint, sz ->
            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, tint = tint, modifier = Modifier.size(sz))
        }
        ToolButton(Modifier.offset(a(418f), toolTop), toolSize, selected = false, enabled = state.strokes.isNotEmpty(), onClick = onClearCanvas, description = stringResource(R.string.clear_canvas)) { tint, sz ->
            Icon(Icons.Filled.Delete, contentDescription = null, tint = tint, modifier = Modifier.size(sz))
        }

        Box(
            contentAlignment = Alignment.CenterEnd,
            modifier = Modifier.offset(a(520f), fromBottom(1428f)).size(a(278f), a(104f))
        ) {
            if (state.isUntimed) {
                PrimaryButton(text = stringResource(R.string.next_word), onClick = onNextWord, height = 46.dp)
            } else if (!state.hintUsed && GameConstants.ADMOB_ENABLED) {
                // One rewarded-ad "+time" hint per whole match, not per word — separate budget from the guessing
                // screen's hint (see GameViewModel/OnlineGameViewModel.useDrawingHint). Hidden entirely while ads
                // are off: the offer is the ad.
                val label = stringResource(if (hintRequested) R.string.loading_hint else R.string.watch_ad_for_extra_time)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(a(230f), a(82f))
                        .shadow(2.dp, RoundedCornerShape(50))
                        .background(Color.White, RoundedCornerShape(50))
                        .border(2.dp, Color(0xFFF26A1B), RoundedCornerShape(50))
                        .clip(RoundedCornerShape(50))
                        .clickable(enabled = !hintRequested) {
                            if (!hintRequested) {
                                hintRequested = true
                                onHintClick()
                            }
                        }
                ) {
                    Text(label, style = PaintedStyle(color = Color(0xFFF26A1B), fontSize = 15.sp * f), maxLines = 1, modifier = Modifier.padding(horizontal = 8.dp))
                }
            }
        }
        if (adErrorShown) {
            TintedBadge(
                text = stringResource(R.string.ad_unavailable),
                container = MaterialTheme.colorScheme.errorContainer,
                content = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.offset(a(40f), fromBottom(1385f) - 34.dp)
            )
        }
    }
}

private const val ArtWidth = 841f
private const val ArtHeight = 1870f

/**
 * The scene picture cut into three bands: the top and bottom are drawn at their natural proportions (header, sign,
 * mascot / tray, desk props), and the middle — only the straight sides of the canvas frame — stretches to fill
 * whatever height the phone has.
 */
@Composable
private fun ThreeSliceBackground(res: Int, topSrc: Int, bottomSrc: Int, topOffset: Dp = 0.dp, modifier: Modifier = Modifier) {
    val img = ImageBitmap.imageResource(res)
    Box(
        modifier = modifier.drawBehind {
            val k = size.width / img.width
            val topDst = (topSrc * k).roundToInt()
            val bottomDst = (bottomSrc * k).roundToInt()
            val w = size.width.roundToInt()
            val h = size.height.roundToInt()
            val off = topOffset.toPx().roundToInt()
            val midDst = (h - off - topDst - bottomDst).coerceAtLeast(0)
            val midSrc = img.height - topSrc - bottomSrc
            fun slice(sy: Int, sh: Int, dy: Int, dh: Int) = drawImage(
                image = img,
                srcOffset = IntOffset(0, sy),
                srcSize = IntSize(img.width, sh),
                dstOffset = IntOffset(0, dy),
                dstSize = IntSize(w, dh),
                filterQuality = FilterQuality.Medium
            )
            if (off > 0) slice(0, 1, 0, off)
            slice(0, topSrc, off, topDst)
            if (midDst > 0) slice(topSrc, midSrc, off + topDst, midDst)
            slice(img.height - bottomSrc, bottomSrc, h - bottomDst, bottomDst)
        }
    )
}

/** A paper-cream square tool button; orange when it is the active tool. */
@Composable
private fun ToolButton(
    modifier: Modifier,
    size: Dp,
    selected: Boolean,
    onClick: () -> Unit,
    description: String,
    enabled: Boolean = true,
    icon: @Composable (tint: Color, size: Dp) -> Unit
) {
    val shape = RoundedCornerShape(size * 0.24f)
    val tint = if (selected) Color(0xFF4A2A10) else Color(0xFF3A2A22)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .alpha(if (enabled) 1f else 0.4f)
            .shadow(3.dp, shape)
            .background(
                if (selected) Brush.verticalGradient(listOf(Color(0xFFFFA24A), Color(0xFFF58A1F)))
                else Brush.verticalGradient(listOf(Color(0xFFFFFDF8), Color(0xFFF3EDE2))),
                shape
            )
            .border(1.5.dp, if (selected) Color(0xFFE5701A) else Color(0xFFB9AFA3), shape)
            .clip(shape)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = description }
    ) {
        icon(tint, size * 0.5f)
    }
}

/** The "+ time" joker as a painted orange pill with its remaining count. */
@Composable
private fun TimeJokerPill(count: Int, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val type = com.sualtikasifi.cizimhafiza.domain.model.JokerType.EXTRA_TIME
    val active = enabled && count > 0
    val orange = Color(0xFFF26A1B)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .alpha(if (active) 1f else 0.5f)
            .shadow(2.dp, RoundedCornerShape(50))
            .background(Brush.verticalGradient(listOf(Color(0xFFFF8A3A), orange)), RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            .clickable(enabled = active, onClick = onClick)
            .padding(start = 8.dp, end = 6.dp, top = 5.dp, bottom = 5.dp)
    ) {
        com.sualtikasifi.cizimhafiza.presentation.common.JokerArt(type, 26.dp)
        Text(
            text = stringResource(type.shortRes()),
            style = PaintedStyle(color = Color.White, fontSize = 15.sp),
            maxLines = 1
        )
        Box(
            modifier = Modifier.size(24.dp).background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(count.toString(), style = PaintedStyle(color = orange, fontSize = 13.sp, textAlign = TextAlign.Center), maxLines = 1)
        }
    }
}

private fun formatMmSs(totalSeconds: Int): String {
    val clamped = totalSeconds.coerceAtLeast(0)
    return "%d:%02d".format(clamped / 60, clamped % 60)
}
