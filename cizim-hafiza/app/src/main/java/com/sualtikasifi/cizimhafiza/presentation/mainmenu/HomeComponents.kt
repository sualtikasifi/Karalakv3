package com.sualtikasifi.cizimhafiza.presentation.mainmenu

import androidx.annotation.StringRes
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
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

internal val Ink = Color(0xFF3A2416)
internal val InkSoft = Color(0xFF7A5A44)
internal val Parchment = Color(0xFFF6E7C6)


/** A raised, chunky panel: darker "edge" slab offset below the face, as in the rest of the game's controls. */
internal fun Modifier.chunky(face: Brush, edge: Color, corner: Dp, lift: Dp = 4.dp, rim: Color? = null): Modifier =
    this
        .padding(bottom = lift)
        .drawBehind {
            drawRoundRect(
                color = edge,
                topLeft = Offset(0f, lift.toPx()),
                size = size,
                cornerRadius = CornerRadius(corner.toPx())
            )
        }
        .clip(RoundedCornerShape(corner))
        .background(face)
        .then(if (rim != null) Modifier.border(2.dp, rim, RoundedCornerShape(corner)) else Modifier)


/**
 * The strip along the very top of the home screen: who the player is and
 * what they carry. Username (one-time editable) and rank are separate lines,
 * then gold, pen name and frame name. No purchasable currency, no league rank.
 */
@Composable
internal fun HomeProfileBar(
    nickname: String,
    progress: LevelProgressState,
    frame: AvatarFrame,
    photo: com.sualtikasifi.cizimhafiza.presentation.common.AvatarPhoto,
    pen: PenSkin,
    gold: Int,
    onFrameClick: () -> Unit,
    onPenClick: () -> Unit,
    onRankClick: () -> Unit,
    onGoldClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    com.sualtikasifi.cizimhafiza.presentation.common.NinePatch(
        res = R.drawable.home_profile,
        slicePx = 100,
        edge = 24.dp,
        modifier = modifier.fillMaxWidth()
    ) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LevelAvatar(
                level = progress.level,
                frame = frame,
                photo = photo,
                size = 64.dp,
                modifier = Modifier.clickable(onClick = onFrameClick).a11yButton(stringResource(R.string.avatar_frame_change_cd))
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = nickname,
                        fontFamily = DisplayFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = "• " + stringResource(R.string.home_level_inline, progress.level),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = InkSoft.copy(alpha = 0.85f),
                        maxLines = 1,
                        softWrap = false
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Brush.horizontalGradient(listOf(Color(0xFF7B5BE6), Color(0xFF5B3FC4))))
                            .clickable(onClick = onRankClick)
                            .padding(horizontal = 9.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = progress.tier.rank.emoji, fontSize = 12.sp)
                        Text(
                            text = stringResource(progress.tier.rank.nameRes),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = if (progress.isMaxLevel) "MAX" else stringResource(R.string.home_xp_to_next, progress.xpToNextLevel, progress.level + 1),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = InkSoft,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.height(5.dp))
                XpBar(
                    fraction = progress.progressFraction,
                    label = stringResource(R.string.home_xp_format, progress.xpIntoLevel, progress.xpForThisLevel),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Spacer(modifier = Modifier.height(9.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gold: a plain balance (no "+"), tapping it opens the store.
            ArtChip(
                spokenLabel = stringResource(R.string.a11y_gold_chip, NumberFormat.getIntegerInstance().format(gold)),
                res = R.drawable.gold_chip_icon,
                crop = floatArrayOf(0.061f, 0.182f, 0.940f, 0.802f),
                stretch = floatArrayOf(0.46f, 0.80f),
                value = NumberFormat.getIntegerInstance().format(gold),
                modifier = Modifier.weight(1f).clickable(onClick = onGoldClick)
            )
            ArtChip(
                spokenLabel = stringResource(R.string.a11y_pen_chip, stringResource(pen.labelRes)),
                res = R.drawable.pen_chip_icon,
                crop = floatArrayOf(0.112f, 0.102f, 0.888f, 0.826f),
                stretch = floatArrayOf(0.60f, 0.72f),
                value = stringResource(pen.labelRes),
                modifier = Modifier.weight(1f).clickable(onClick = onPenClick)
            )
            ArtChip(
                spokenLabel = stringResource(R.string.a11y_frame_chip, stringResource(frame.nameRes())),
                twoLines = true,
                res = R.drawable.frame_chip_icon,
                crop = floatArrayOf(0.085f, 0.107f, 0.915f, 0.810f),
                stretch = floatArrayOf(0.51f, 0.75f),
                value = stringResource(frame.nameRes()),
                modifier = Modifier.weight(1f).clickable(onClick = onFrameClick)
            )
        }
    }
    }
}

/** Image cropped to [crop] (left, top, right, bottom as fractions) and multiplied onto what is behind, so its white paper drops out. */
@Composable
internal fun CroppedArt(res: Int, crop: FloatArray, modifier: Modifier = Modifier, stretch: Boolean = false) {
    val bmp = ImageBitmap.imageResource(res)
    val sx = (bmp.width * crop[0]).toInt()
    val sy = (bmp.height * crop[1]).toInt()
    val sw = (bmp.width * (crop[2] - crop[0])).toInt()
    val sh = (bmp.height * (crop[3] - crop[1])).toInt()
    Canvas(modifier = if (stretch) modifier else modifier.aspectRatio(sw.toFloat() / sh)) {
        drawImage(
            image = bmp,
            srcOffset = IntOffset(sx, sy),
            srcSize = IntSize(sw, sh),
            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
            blendMode = BlendMode.Multiply,
            filterQuality = FilterQuality.High
        )
    }
}

/**
 * A pill from the supplied art drawn as a three-part strip: the left cap (icon)
 * and right cap keep their natural proportions, only the blank middle stretches.
 * That makes every chip exactly the same size whatever its source art, and the
 * text is centred in the blank middle.
 *
 * [crop] is (left, top, right, bottom) of the region to draw and [stretch] is
 * (start, end) of the blank band, both as fractions of the whole image.
 */
@Composable
private fun ArtChip(
    twoLines: Boolean = false,
    spokenLabel: String,
    res: Int,
    crop: FloatArray,
    stretch: FloatArray,
    value: String,
    modifier: Modifier = Modifier
) {
    val bmp = ImageBitmap.imageResource(res)
    val srcL = (bmp.width * crop[0]).toInt()
    val srcT = (bmp.height * crop[1]).toInt()
    val srcR = (bmp.width * crop[2]).toInt()
    val srcB = (bmp.height * crop[3]).toInt()
    val midL = (bmp.width * stretch[0]).toInt()
    val midR = (bmp.width * stretch[1]).toInt()
    BoxWithConstraints(modifier = modifier.height(42.dp).a11yButton(spokenLabel)) {
        val density = LocalDensity.current
        val hPx = with(density) { maxHeight.toPx() }
        val wPx = with(density) { maxWidth.toPx() }
        var scale = hPx / (srcB - srcT)
        var leftW = (midL - srcL) * scale
        var rightW = (srcR - midR) * scale
        // Very narrow chip: shrink both caps rather than let them overlap.
        if (leftW + rightW > wPx * 0.85f) {
            val k = wPx * 0.85f / (leftW + rightW)
            leftW *= k; rightW *= k
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Whole-pixel edges shared by neighbouring pieces, so no hairline gap or overlap shows at the joins.
            val total = size.width.roundToInt()
            val x1 = leftW.roundToInt()
            val x2 = (size.width - rightW).roundToInt()
            val h = size.height.roundToInt()
            fun piece(sx: Int, sw: Int, dx: Int, dw: Int) {
                drawImage(
                    image = bmp,
                    srcOffset = IntOffset(sx, srcT),
                    srcSize = IntSize(sw, srcB - srcT),
                    dstOffset = IntOffset(dx, 0),
                    dstSize = IntSize(dw.coerceAtLeast(1), h),
                    blendMode = BlendMode.Multiply,
                    filterQuality = FilterQuality.High
                )
            }
            piece(srcL, midL - srcL, 0, x1)
            piece(midL, midR - midL, x1, x2 - x1)
            piece(midR, srcR - midR, x2, total - x2)
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(start = with(density) { leftW.toDp() } - 2.dp, end = with(density) { rightW.toDp() } - 4.dp, top = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            FitText(text = value, color = Ink, maxSp = if (twoLines) 13f else 12f, twoLines = twoLines)
        }
    }
}

/** Wordmark, centred and compact. */
@Composable
internal fun HomeHero(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CroppedArt(
            res = R.drawable.karalak_logo_lettering,
            crop = floatArrayOf(0.13f, 0.19f, 0.87f, 0.81f),
            modifier = Modifier.width(210.dp)
        )
    }
}

/**
 * One of the three big ways to start a match: big art up front, small caption,
 * saturated gradient, chunky edge. While a 2x XP event is running ([boost]),
 * the card breathes a golden glow and wears a pulsing "2x XP" badge.
 */
@Composable
internal fun GradientModeCard(
    imageRes: Int,
    label: String,
    top: Color,
    bottom: Color,
    edge: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    boost: com.sualtikasifi.cizimhafiza.domain.repository.XpEvent? = null,
    /** A bright streak of light circling the card's edge, to draw the eye to it. */
    orbit: Boolean = false
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "modePress")
    val orbitAngle by rememberInfiniteTransition(label = "modeOrbit").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2800, easing = androidx.compose.animation.core.LinearEasing)),
        label = "modeOrbitAngle"
    )

    // The event can run out while the home screen is open: flip the effect off
    // at the moment it ends instead of waiting for a recomposition.
    var boostExpired by remember(boost?.endsAtMillis) { mutableStateOf(boost == null || System.currentTimeMillis() >= boost.endsAtMillis) }
    LaunchedEffect(boost?.endsAtMillis) {
        if (boost != null) {
            kotlinx.coroutines.delay((boost.endsAtMillis - System.currentTimeMillis()).coerceAtLeast(0L))
            boostExpired = true
        }
    }
    val boosted = boost != null && !boostExpired
    val glow = rememberInfiniteTransition(label = "modeBoost").animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(850, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "modeBoostGlow"
    )

    Box(modifier = modifier.fillMaxHeight().graphicsLayer { scaleX = scale; scaleY = scale }) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (boosted) Modifier.drawBehind {
                        val spread = 6.dp.toPx() * glow.value
                        drawRoundRect(
                            color = Color(0xFFFFD54A).copy(alpha = 0.55f * glow.value),
                            topLeft = androidx.compose.ui.geometry.Offset(-spread, -spread),
                            size = androidx.compose.ui.geometry.Size(size.width + spread * 2, size.height + spread * 2),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx() + spread)
                        )
                    } else Modifier
                )
                .chunky(Brush.verticalGradient(listOf(top, bottom)), edge, corner = 24.dp, lift = 5.dp, rim = Color.White.copy(alpha = 0.35f))
                .then(if (orbit) Modifier.drawWithContent { drawContent(); drawOrbit(orbitAngle, 24.dp.toPx()) } else Modifier)
                .clickable(interactionSource = interaction, indication = null, onClick = onClick)
                .padding(start = 2.dp, end = 2.dp, top = 4.dp, bottom = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(imageRes),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
            Box(modifier = Modifier.fillMaxWidth().height(26.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = label,
                    fontFamily = DisplayFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }
        if (boosted && boost != null) {
            BoostBadge(multiplier = boost.multiplier, modifier = Modifier.align(Alignment.TopEnd).padding(top = 3.dp, end = 3.dp))
        }
    }
}

/** The small "2x XP" pill on the boosted mode card: it pulses, and a star twinkles beside it. */
@Composable
private fun BoostBadge(multiplier: Int, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "boostBadge")
    val pulse = t.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(650, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "boostBadgePulse"
    )
    val twinkle = t.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(480), RepeatMode.Reverse),
        label = "boostBadgeTwinkle"
    )
    Row(
        modifier = modifier.graphicsLayer { scaleX = pulse.value; scaleY = pulse.value },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "✦", fontSize = 11.sp, color = Color(0xFFFFF3B0), modifier = Modifier.graphicsLayer { alpha = twinkle.value })
        Text(
            text = "${multiplier}x XP",
            fontFamily = DisplayFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 10.sp,
            color = Color(0xFF5A2E00),
            modifier = Modifier
                .background(Brush.verticalGradient(listOf(Color(0xFFFFE566), Color(0xFFFFB300))), RoundedCornerShape(50))
                .border(1.5.dp, Color(0xFFB36B00), RoundedCornerShape(50))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

/**
 * Full-width home banner for a running event: the event's own name, what it
 * does, and a live countdown. Hides itself the second the event ends.
 */
@Composable
internal fun XpEventBanner(event: com.sualtikasifi.cizimhafiza.domain.repository.XpEvent, modifier: Modifier = Modifier) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(event.endsAtMillis) {
        while (System.currentTimeMillis() < event.endsAtMillis) {
            now = System.currentTimeMillis()
            kotlinx.coroutines.delay(1_000)
        }
        now = System.currentTimeMillis()
    }
    val remaining = event.endsAtMillis - now
    if (remaining <= 0) return

    val shimmer = rememberInfiniteTransition(label = "xpBanner").animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(tween(2200, easing = androidx.compose.animation.core.LinearEasing)),
        label = "xpBannerShimmer"
    )
    val shape = RoundedCornerShape(18.dp)
    val name = event.label?.takeIf { it.isNotBlank() } ?: stringResource(R.string.xp_event_default_name)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(Color(0xFFFF8A00), Color(0xFFFFC21A), Color(0xFFFF8A00))))
            .drawWithContent {
                drawContent()
                val x = size.width * shimmer.value
                drawRect(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.35f), Color.Transparent),
                        startX = x - 60.dp.toPx(),
                        endX = x + 60.dp.toPx()
                    )
                )
            }
            .border(2.dp, Color(0xFFB36B00), shape)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text = "🎉", fontSize = 20.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontFamily = DisplayFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                lineHeight = 15.sp,
                color = Color(0xFF4A2600),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.xp_event_banner_sub, event.multiplier),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6B3A00)
            )
        }
        Text(
            text = formatCountdown(remaining),
            fontFamily = DisplayFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            color = Color(0xFFFFE08A),
            modifier = Modifier
                .background(Color(0xFF2A1808), RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 3.dp)
        )
    }
}

/**
 * One painted menu tile: the picture already carries its frame and its icon, the label is written on its lower third in
 * the game's lettering. A red dot or a count can ride on the corner; [boost] adds the "2x XP" pill of a running event.
 */
@Composable
internal fun PaintedTile(
    res: Int,
    aspect: Float,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showBadge: Boolean = false,
    badgeCount: Int = 0,
    boost: com.sualtikasifi.cizimhafiza.domain.repository.XpEvent? = null,
    labelSize: Float = 14f
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, label = "paintedTile")
    var boostExpired by remember(boost?.endsAtMillis) { mutableStateOf(boost == null || System.currentTimeMillis() >= boost.endsAtMillis) }
    LaunchedEffect(boost?.endsAtMillis) {
        if (boost != null) {
            kotlinx.coroutines.delay((boost.endsAtMillis - System.currentTimeMillis()).coerceAtLeast(0L))
            boostExpired = true
        }
    }
    val boosted = boost != null && !boostExpired
    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(aspect)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .a11yButton(label)
    ) {
        Image(painterResource(res), contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
        val tileWidth = maxWidth
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = maxHeight * 0.1f)
                .fillMaxWidth()
                .height(maxHeight * 0.27f)
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            val sizeSp = (tileWidth.value * labelSize / 120f).coerceIn(10f, 17f)
            com.sualtikasifi.cizimhafiza.presentation.common.LetteredText(
                text = label,
                size = sizeSp.sp,
                maxLines = 2
            )
        }
        if (showBadge) {
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 10.dp).size(14.dp).background(MaterialTheme.colorScheme.error, CircleShape).border(2.dp, Color.White, CircleShape))
        }
        if (badgeCount > 0) {
            Box(
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 6.dp).background(MaterialTheme.colorScheme.error, CircleShape).border(2.dp, Color.White, CircleShape).padding(horizontal = 7.dp, vertical = 1.dp)
            ) {
                Text(text = badgeCount.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onError)
            }
        }
        if (boosted && boost != null) {
            BoostBadge(multiplier = boost.multiplier, modifier = Modifier.align(Alignment.TopStart).padding(top = 8.dp, start = 10.dp))
        }
    }
}

/** A coloured menu tile matching [GradientModeCard]: icon disc over a one-line label, optional badge. */
@Composable
internal fun GradientTile(
    icon: ImageVector,
    label: String,
    top: Color,
    bottom: Color,
    edge: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showBadge: Boolean = false,
    badgeCount: Int = 0,
    compact: Boolean = false
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "tilePress")
    Box(modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .chunky(Brush.verticalGradient(listOf(top, bottom)), edge, corner = 20.dp, lift = 4.dp, rim = Color.White.copy(alpha = 0.3f))
                .clickable(interactionSource = interaction, indication = null, onClick = onClick)
                .padding(horizontal = 6.dp, vertical = if (compact) 5.dp else 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(if (compact) 32.dp else 38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.28f))
                    .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(if (compact) 19.dp else 23.dp))
            }
            Spacer(modifier = Modifier.height(3.dp))
            FitText(text = label, color = Color.White, maxSp = 13f, minSp = 8f)
        }
        if (showBadge) {
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(13.dp).background(MaterialTheme.colorScheme.error, CircleShape).border(2.dp, Color.White, CircleShape))
        }
        if (badgeCount > 0) {
            Box(
                modifier = Modifier.align(Alignment.TopEnd).padding(3.dp).background(MaterialTheme.colorScheme.error, CircleShape).border(2.dp, Color.White, CircleShape).padding(horizontal = 7.dp, vertical = 1.dp)
            ) {
                Text(text = badgeCount.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onError)
            }
        }
    }
}

/** Every pen, locked ones dimmed with the level that opens them; tapping an unlocked one equips it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PenPickerSheet(items: List<PenSkinUiItem>, onSelect: (PenSkin) -> Unit, onDismiss: () -> Unit) {
    com.sualtikasifi.cizimhafiza.presentation.common.AppWindowDialog(title = stringResource(R.string.pen_picker_title), onDismiss = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                gridItems(items, key = { it.skin.name }) { item ->
                    val colors = item.skin.colors.map { Color(it) }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(if (item.selected) 3.dp else 1.dp, if (item.selected) Color(0xFFFF7A21) else MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                            .clickable(enabled = item.unlocked) { onSelect(item.skin) }
                            .padding(vertical = 12.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Canvas(modifier = Modifier.fillMaxWidth().height(34.dp).alpha(if (item.unlocked) 1f else 0.35f)) {
                            val y = size.height / 2f
                            drawLine(
                                brush = if (colors.size > 1) Brush.horizontalGradient(colors) else Brush.horizontalGradient(listOf(colors.first(), colors.first())),
                                start = Offset(size.width * 0.12f, y + 6.dp.toPx()),
                                end = Offset(size.width * 0.88f, y - 6.dp.toPx()),
                                strokeWidth = 12.dp.toPx(),
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(item.skin.labelRes),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        if (!item.unlocked) {
                            Text(
                                text = when { item.skin.isStoreItem -> stringResource(R.string.store_in_store); item.skin.isLeagueReward -> stringResource(R.string.cosmetic_locked_league); else -> stringResource(R.string.avatar_frame_locked_level, item.skin.unlockLevel) },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * One-line text that is the same size everywhere (12sp) and only shrinks —
 * never wraps or overflows — when it would not fit, so a long pen name or a
 * seven-digit gold balance stays inside its chip.
 */
@Composable
internal fun FitText(text: String, color: Color, maxSp: Float = 12f, minSp: Float = 6f, twoLines: Boolean = false, textAlign: TextAlign = TextAlign.Center, style: androidx.compose.ui.text.TextStyle = androidx.compose.ui.text.TextStyle.Default) {
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    // A multi-word name breaks at the space nearest its middle, so "Kurşun Halka" becomes "Kurşun / Halka".
    val mid = text.length / 2
    val cut = if (twoLines) text.indices.filter { text[it] == ' ' }.minByOrNull { kotlin.math.abs(it - mid) } else null
    val shown = if (cut != null) text.substring(0, cut).trim() + "\n" + text.substring(cut + 1).trim() else text
    val lines = if (shown.contains('\n')) 2 else 1
    BoxWithConstraints {
        val base = androidx.compose.ui.text.TextStyle(
            fontFamily = DisplayFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = maxSp.sp
        )
        val natural = measurer.measure(text = shown, style = base, maxLines = lines, softWrap = false).size.width
        val available = constraints.maxWidth
        val sp = if (natural <= available || natural == 0) maxSp else (maxSp * available / natural).coerceAtLeast(minSp)
        Text(
            text = shown,
            style = style,
            fontFamily = DisplayFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = sp.sp,
            lineHeight = (sp * 1.05f).sp,
            textAlign = textAlign,
            color = color,
            maxLines = lines,
            softWrap = false
        )
    }
}

/**
 * The profile's XP bar: a dark carved well with a glossy orange fill that has
 * a light sheen sliding across it, the XP text written on the bar itself.
 */
@Composable
private fun XpBar(fraction: Float, label: String, modifier: Modifier = Modifier) {
    // Coming home from a result screen, the bar already stands at its old value (no fill-up from empty),
    // so the spark that lands on it is what visibly moves it.
    val animated = remember { Animatable(if (com.sualtikasifi.cizimhafiza.presentation.common.XpFlyBus.active.value != null) fraction.coerceIn(0f, 1f) else 0f) }
    LaunchedEffect(fraction) {
        val target = fraction.coerceIn(0f, 1f)
        // A level-up shows as the bar filling to the top, emptying, and filling again to its new value.
        if (target < animated.value - 0.01f) {
            animated.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
            animated.snapTo(0f)
        }
        animated.animateTo(target, tween(1100, easing = FastOutSlowInEasing))
    }
    // Landing glow: lit when the spark arrives, then fades.
    val glow = remember { Animatable(0f) }
    val arrivedId by com.sualtikasifi.cizimhafiza.presentation.common.XpFlyBus.arrivedId.collectAsState()
    LaunchedEffect(arrivedId) {
        if (arrivedId != 0L) {
            glow.snapTo(1f)
            glow.animateTo(0f, tween(1400))
        }
    }
    // Tell the spark where to land: the end of the bar's current fill, in window coordinates.
    var barRect by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    LaunchedEffect(barRect, fraction) {
        val r = barRect ?: return@LaunchedEffect
        com.sualtikasifi.cizimhafiza.presentation.common.XpFlyBus.target.value =
            Offset(r.left + r.width * fraction.coerceIn(0.06f, 1f), r.center.y)
    }
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { com.sualtikasifi.cizimhafiza.presentation.common.XpFlyBus.target.value = null }
    }
    val infinite = rememberInfiniteTransition(label = "xpSheen")
    val sheen = infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = androidx.compose.animation.core.LinearEasing), RepeatMode.Restart),
        label = "sheen"
    )
    Box(
        modifier = modifier.height(20.dp).onGloballyPositioned { barRect = it.boundsInWindow() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val h = size.height
            val rim = 2.dp.toPx()
            val g = glow.value
            if (g > 0f) {
                for (i in 1..4) {
                    drawRoundRect(
                        Color(0xFFFFC53D).copy(alpha = 0.30f * g / i),
                        topLeft = Offset(-i * 3.dp.toPx(), -i * 3.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(size.width + i * 6.dp.toPx(), h + i * 6.dp.toPx()),
                        cornerRadius = CornerRadius(h / 2f + i * 3.dp.toPx())
                    )
                }
            }
            drawRoundRect(Color(0xFF7A4A2A), cornerRadius = CornerRadius(h / 2f))
            val innerSize = androidx.compose.ui.geometry.Size(size.width - rim * 2, h - rim * 2)
            drawRoundRect(
                Brush.verticalGradient(listOf(Color(0xFF2E1B10), Color(0xFF4A2C1A))),
                topLeft = Offset(rim, rim), size = innerSize, cornerRadius = CornerRadius(innerSize.height / 2f)
            )
            val f = animated.value
            if (f > 0f) {
                val fillW = (innerSize.width * f).coerceAtLeast(innerSize.height)
                val fillRect = androidx.compose.ui.geometry.RoundRect(
                    left = rim, top = rim, right = rim + fillW, bottom = rim + innerSize.height,
                    cornerRadius = CornerRadius(innerSize.height / 2f)
                )
                val clip = androidx.compose.ui.graphics.Path().apply { addRoundRect(fillRect) }
                clipPath(clip) {
                    drawRect(
                        Brush.horizontalGradient(listOf(Color(0xFFFFC53D), Color(0xFFFF8A1F), Color(0xFFFF5A1A))),
                        topLeft = Offset(rim, rim), size = androidx.compose.ui.geometry.Size(fillW, innerSize.height)
                    )
                    // Top gloss.
                    drawRect(
                        Color.White.copy(alpha = 0.38f),
                        topLeft = Offset(rim, rim), size = androidx.compose.ui.geometry.Size(fillW, innerSize.height * 0.45f)
                    )
                    // Sliding sheen.
                    val bandX = rim + (fillW + 60.dp.toPx()) * sheen.value - 30.dp.toPx()
                    drawRect(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, Color.White.copy(alpha = 0.55f), Color.Transparent),
                            startX = bandX - 18.dp.toPx(), endX = bandX + 18.dp.toPx()
                        ),
                        topLeft = Offset(rim, rim), size = androidx.compose.ui.geometry.Size(fillW, innerSize.height)
                    )
                }
            }
        }
        Text(
            text = label,
            fontFamily = DisplayFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 10.sp,
            color = Color.White,
            style = androidx.compose.ui.text.TextStyle(shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = 0.7f), Offset(0f, 2f), 3f)),
            maxLines = 1
        )
    }
}
