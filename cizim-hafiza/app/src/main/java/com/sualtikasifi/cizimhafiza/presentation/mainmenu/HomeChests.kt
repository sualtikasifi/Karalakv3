package com.sualtikasifi.cizimhafiza.presentation.mainmenu

import androidx.annotation.StringRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import com.sualtikasifi.cizimhafiza.presentation.common.AppTextField
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.sualtikasifi.cizimhafiza.presentation.common.springIn
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.AvatarFrame
import com.sualtikasifi.cizimhafiza.domain.model.Chest
import com.sualtikasifi.cizimhafiza.domain.model.ChestSlots
import com.sualtikasifi.cizimhafiza.domain.model.ChestTier
import com.sualtikasifi.cizimhafiza.domain.model.LevelProgressState
import com.sualtikasifi.cizimhafiza.domain.model.PenSkin
import com.sualtikasifi.cizimhafiza.presentation.chests.ChestBackdrop
import com.sualtikasifi.cizimhafiza.presentation.chests.ChestImage
import com.sualtikasifi.cizimhafiza.presentation.chests.borderColor
import com.sualtikasifi.cizimhafiza.presentation.chests.formatCountdown
import com.sualtikasifi.cizimhafiza.presentation.common.drawOrbitSparks
import com.sualtikasifi.cizimhafiza.presentation.chests.onBackdrop
import com.sualtikasifi.cizimhafiza.presentation.chests.ChestsViewModel
import com.sualtikasifi.cizimhafiza.presentation.chests.accent
import com.sualtikasifi.cizimhafiza.presentation.chests.durationHours
import com.sualtikasifi.cizimhafiza.presentation.chests.glow
import com.sualtikasifi.cizimhafiza.presentation.chests.remainingMillis
import com.sualtikasifi.cizimhafiza.presentation.common.LevelAvatar
import com.sualtikasifi.cizimhafiza.presentation.common.a11yButton
import com.sualtikasifi.cizimhafiza.presentation.common.nameRes
import com.sualtikasifi.cizimhafiza.presentation.common.labelRes
import com.sualtikasifi.cizimhafiza.presentation.theme.AppTheme
import com.sualtikasifi.cizimhafiza.presentation.theme.DisplayFont
import java.text.NumberFormat

/**
 * "Kasalarım" on the home screen: the four chest slots with live timers.
 * Tapping anywhere goes to the full chests screen where they are unlocked
 * and opened. Reads its own [ChestsViewModel] — same repository state, so it
 * never disagrees with the chests screen.
 */
@Composable
internal fun HomeChestsSection(compact: Boolean = false, viewModel: ChestsViewModel = hiltViewModel()) {
    var selectedChestId by remember { mutableStateOf<String?>(null) }
    val slots by viewModel.chestSlots.collectAsState()
    val now by viewModel.nowMillis.collectAsState()
    var infoOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .chunky(
                face = Brush.verticalGradient(listOf(Color(0xFFFBEFD3), Parchment)),
                edge = Color(0xFFD3B27A),
                corner = 26.dp,
                lift = 5.dp,
                rim = Color(0xFFEBCB93)
            )
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChestImage(tier = ChestTier.GOLD, width = if (compact) 36.dp else 46.dp)
            Text(
                text = stringResource(R.string.menu_chests),
                fontFamily = DisplayFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                color = Ink,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(if ((0 until ChestSlots.SLOT_COUNT).all { slots.getOrNull(it) != null }) R.string.home_chests_full else R.string.home_chests_tagline),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = InkSoft,
                textAlign = TextAlign.End,
                maxLines = 2,
                modifier = Modifier.width(92.dp)
            )
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2E86D6))
                    .border(2.dp, Color.White, CircleShape)
                    .clickable { infoOpen = true }
                    .a11yButton(stringResource(R.string.home_chests_info_title)),
                contentAlignment = Alignment.Center
            ) {
                Text("i", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth().height(if (compact) 108.dp else 138.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (0 until ChestSlots.SLOT_COUNT).forEach { index ->
                HomeChestSlot(
                    chest = slots.getOrNull(index),
                    nowMillis = now,
                    onClick = { slots.getOrNull(index)?.let { selectedChestId = it.id } },
                    compact = compact,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    com.sualtikasifi.cizimhafiza.presentation.chests.ChestDetailHost(
        selectedId = selectedChestId,
        onDismiss = { selectedChestId = null },
        viewModel = viewModel
    )

    if (infoOpen) ChestInfoDialog(onDismiss = { infoOpen = false })
}

@Composable
internal fun HomeChestSlot(chest: Chest?, nowMillis: Long, onClick: () -> Unit, compact: Boolean, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(18.dp)
    if (chest == null) {
        val dash = InkSoft.copy(alpha = 0.5f)
        Column(
            modifier = modifier
                .fillMaxHeight()
                .clip(shape)
                .background(Color.White.copy(alpha = 0.35f))
                .drawBehind {
                    drawRoundRect(
                        color = dash,
                        cornerRadius = CornerRadius(18.dp.toPx()),
                        style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f)))
                    )
                }
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(dash.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text("+", fontSize = 26.sp, color = dash, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.chest_slot_empty_title).let { if (compact) it.replace(' ', '\n') else it },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = InkSoft,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
        return
    }

    val tier = chest.tier
    val ready = chest.isReady(nowMillis)
    val unlocking = chest.unlockStartedAtMillis != null && !ready
    val infinite = rememberInfiniteTransition(label = "homeSlot")
    val pulse by infinite.animateFloat(
        initialValue = 1f,
        targetValue = if (ready) 1.06f else 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val orbit = infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3400, easing = androidx.compose.animation.core.LinearEasing)),
        label = "orbit"
    )
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .graphicsLayer { scaleX = pulse; scaleY = pulse }
            .clip(shape)
            .border(if (ready) 3.dp else 2.dp, tier.borderColor().copy(alpha = if (ready) 1f else 0.85f), shape)
            .clickable(onClick = onClick)
    ) {
        // On the painted home the slot is as tall as its painted frame, which differs from phone to phone; every
        // part scales with it so the plate at the bottom can never be pushed out (it vanished on 16:9 screens).
        val k = if (compact) (maxHeight / 104.dp).coerceIn(0.6f, 1.3f) else 1f
        ChestBackdrop(tier = tier, modifier = Modifier.matchParentSize())
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 3.dp, vertical = 6.dp * k),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.height(if (compact) 44.dp * k else 54.dp), contentAlignment = Alignment.Center) {
                ChestImage(tier = tier, width = if (compact) 50.dp * k else 62.dp)
            }
            // The name gets a fixed band of its own (two lines tall) so it can
            // never reach into the timer below, whatever the font scale; the
            // timer sits on a dark plate of its own, which also guarantees it
            // reads on every chest colour.
            Box(
                modifier = Modifier.fillMaxWidth().height(if (compact) 16.dp * k else 26.dp),
                contentAlignment = Alignment.Center
            ) {
                com.sualtikasifi.cizimhafiza.presentation.common.FitText(
                    text = stringResource(tier.labelRes()),
                    style = com.sualtikasifi.cizimhafiza.presentation.common.PaintedStyle(
                        color = tier.onBackdrop(),
                        fontSize = if (compact) 11.sp * k else 11.sp,
                        textAlign = TextAlign.Center,
                        shadow = if (tier == ChestTier.RARE) androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = 0.55f), Offset(0f, 2f), 4f) else null
                    ),
                    maxLines = if (compact) 1 else 2,
                    minScale = 0.7f,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 2.dp)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            val plateBrush = when {
                ready -> Brush.verticalGradient(listOf(Color(0xFF5BE08A), Color(0xFF1E9E52)))
                unlocking -> Brush.verticalGradient(listOf(Color(0xFF4A2B14), Color(0xFF1F1008)))
                else -> Brush.verticalGradient(listOf(Color(0xF2352218), Color(0xF21A1108)))
            }
            val plateBorder = when {
                ready -> Color(0xFFB8F5CF)
                unlocking -> Color(0xFFFFC94D)
                else -> tier.accent().copy(alpha = 0.7f)
            }
            val sparkColors = remember(ready) {
                if (ready) listOf(Color(0xFFD9FFE6), Color.White) else listOf(Color(0xFFFFE08A), Color(0xFFFFB340), Color.White)
            }
            val plateShape = RoundedCornerShape(11.dp)
            // Inset from the card edge so the plate and its rim do not touch the chest border on either side.
            // The sparks circle the plate while a chest is counting down (gold) or waiting to be opened (green).
            Box(
                modifier = Modifier
                    .padding(horizontal = 7.dp)
                    .fillMaxWidth()
                    .height(if (compact) 24.dp * k else 24.dp)
                    .drawWithContent {
                        drawContent()
                        if (unlocking || ready) {
                            drawOrbitSparks(
                                progress = orbit.value,
                                count = if (ready) 4 else 6,
                                colors = sparkColors,
                                radius = 1.7.dp.toPx(),
                                outset = 1.dp.toPx()
                            )
                        }
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(plateShape)
                        .background(plateBrush)
                        // A glossy highlight over the top half, like a button cap.
                        .drawBehind {
                            drawRect(
                                Brush.verticalGradient(
                                    listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.04f)),
                                    endY = size.height * 0.55f
                                ),
                                size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.55f)
                            )
                        }
                        .border(1.5.dp, plateBorder, plateShape)
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // One centred label, no icon beside it: an icon on one side pulled the text off the plate's centre.
                    Text(
                        text = when {
                            ready -> stringResource(R.string.chests_open_button) + "!"
                            // Down to the second: this is what the player watches tick.
                            unlocking -> formatCountdown(chest.remainingMillis(nowMillis))
                            // Not started yet: just how long the chest takes (SS:DD), the same plate the countdown later uses.
                            else -> (tier.unlockDurationMillis / 60_000L).let { minutes -> "%02d:%02d".format(minutes / 60, minutes % 60) }
                        },
                        fontSize = if (compact) 12.sp * k else 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (unlocking) Color(0xFFFFE08A) else Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.fillMaxWidth(),
                        style = androidx.compose.ui.text.TextStyle(
                            shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = 0.45f), Offset(0f, 1.5f), 2f),
                            platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
                            lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(
                                alignment = androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,
                                trim = androidx.compose.ui.text.style.LineHeightStyle.Trim.None
                            )
                        )
                    )
                }
            }
        }
    }
}

/** Ticks once a second while [untilMillis] is still in the future; returns the current time. */
@Composable
internal fun rememberNowUntil(untilMillis: Long): Long {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(untilMillis) {
        now = System.currentTimeMillis()
        while (now < untilMillis) {
            kotlinx.coroutines.delay(1_000)
            now = System.currentTimeMillis()
        }
    }
    return now
}

internal fun hms(totalSeconds: Long): String {
    val s = totalSeconds.coerceAtLeast(0)
    return "%d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
}


/**
 * The painted "Kasalarım" panel: the picture already carries the frame, the empty slots and the info button; this places
 * the live parts over it. [box] turns a rectangle of the picture into a Modifier, [fs] a size in picture units into text.
 */
@Composable
internal fun HomeChestsPainted(
    box: (Float, Float, Float, Float) -> Modifier,
    fs: (Float) -> androidx.compose.ui.unit.TextUnit,
    viewModel: ChestsViewModel = hiltViewModel()
) {
    var selectedChestId by remember { mutableStateOf<String?>(null) }
    val slots by viewModel.chestSlots.collectAsState()
    val now by viewModel.nowMillis.collectAsState()
    var infoOpen by remember { mutableStateOf(false) }

    // Title and tagline share one baseline (the tagline is the small line that closes the same row), centred in the
    // panel's header band.
    val tagline = stringResource(if ((0 until ChestSlots.SLOT_COUNT).all { slots.getOrNull(it) != null }) R.string.home_chests_full else R.string.home_chests_tagline)
    Box(box(166f, 1428f, 728f, 1498f), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.menu_chests),
                style = com.sualtikasifi.cizimhafiza.presentation.common.PaintedStyle(
                    color = Color(0xFF2B1A10), fontSize = fs(42f), textAlign = TextAlign.Start
                ),
                maxLines = 1,
                modifier = Modifier.alignByBaseline()
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = tagline,
                style = com.sualtikasifi.cizimhafiza.presentation.common.PaintedStyle(
                    color = Color(0xFF4A3426), fontSize = fs(if (tagline.length > 26) 20f else 23f), fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End
                ),
                maxLines = 1,
                modifier = Modifier.alignByBaseline()
            )
        }
    }
    Box(
        box(731f, 1426f, 794f, 1490f)
            .clickable { infoOpen = true }
            .a11yButton(stringResource(R.string.home_chests_info_title))
    )
    val slotX = listOf(64f to 232f, 247f to 412f, 429f to 596f, 610f to 780f)
    (0 until ChestSlots.SLOT_COUNT).forEach { index ->
        val (x0, x1) = slotX[index]
        val chest = slots.getOrNull(index)
        if (chest != null) {
            Box(box(x0, 1494f, x1, 1674f).springIn(index = index, stepMs = 90, key = chest.id)) {
                HomeChestSlot(
                    chest = chest,
                    nowMillis = now,
                    onClick = { selectedChestId = chest.id },
                    compact = true,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            // One line, the same size in all four slots: a fixed-width box centred on each slot (the painted slots differ
            // by a few units, which used to make the label wrap in some and not in others).
            val cx = (x0 + x1) / 2f
            com.sualtikasifi.cizimhafiza.presentation.common.FitText(
                text = stringResource(R.string.chest_slot_empty_title),
                style = com.sualtikasifi.cizimhafiza.presentation.common.PaintedStyle(
                    color = Color(0xFF4A3426), fontSize = fs(25f), fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center
                ),
                maxLines = 1,
                minScale = 0.5f,
                modifier = box(cx - 70f, 1596f, cx + 70f, 1650f)
            )
        }
    }

    com.sualtikasifi.cizimhafiza.presentation.chests.ChestDetailHost(
        selectedId = selectedChestId,
        onDismiss = { selectedChestId = null },
        viewModel = viewModel
    )
    if (infoOpen) ChestInfoDialog(onDismiss = { infoOpen = false })
}
