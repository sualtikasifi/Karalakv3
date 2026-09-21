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

private val Ink = Color(0xFF3A2416)
private val InkSoft = Color(0xFF7A5A44)
private val Parchment = Color(0xFFF6E7C6)


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
    pen: PenSkin,
    gold: Int,
    renameUsed: Boolean,
    onEditName: () -> Unit,
    onFrameClick: () -> Unit,
    onPenClick: () -> Unit,
    onRankClick: () -> Unit,
    onGoldClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .chunky(
                face = Brush.verticalGradient(listOf(Color(0xFFFFF6E3), Color(0xFFFCE6BF))),
                edge = Color(0xFFD9B57A),
                corner = 26.dp,
                lift = 5.dp,
                rim = Color(0xFFEBCB93)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LevelAvatar(
                level = progress.level,
                frame = frame,
                size = 64.dp,
                modifier = Modifier.clickable(onClick = onFrameClick).a11yButton(stringResource(R.string.avatar_frame_change_cd))
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = if (renameUsed) Modifier else Modifier.clickable(onClick = onEditName).a11yButton(stringResource(R.string.nickname_edit_title)),
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
                    if (!renameUsed) {
                        Box(
                            modifier = Modifier.size(22.dp).clip(CircleShape).background(Color(0xFFFF7A21)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.nickname_edit_title), tint = Color.White, modifier = Modifier.size(13.dp))
                        }
                    }
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

/** One of the three big ways to start a match: big art up front, small caption, saturated gradient, chunky edge. */
@Composable
internal fun GradientModeCard(
    imageRes: Int,
    label: String,
    top: Color,
    bottom: Color,
    edge: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "modePress")
    Column(
        modifier = modifier.fillMaxHeight()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .chunky(Brush.verticalGradient(listOf(top, bottom)), edge, corner = 24.dp, lift = 5.dp, rim = Color.White.copy(alpha = 0.35f))
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

/** The one-time username change. Warns that it can only be done once; validation lives in SettingsRepository. */
@Composable
internal fun NicknameEditDialog(current: String, onConfirm: (String) -> Boolean, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(current) }
    var error by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.nickname_edit_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.nickname_edit_warning),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))
                AppTextField(
                    value = text,
                    onValueChange = { if (it.length <= 16) { text = it; error = false } },
                    placeholder = stringResource(R.string.nickname_edit_hint)
                )
                if (error) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(stringResource(R.string.nickname_edit_invalid), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (onConfirm(text)) onDismiss() else error = true }) {
                Text(stringResource(R.string.nickname_edit_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.nickname_edit_cancel)) } }
    )
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

    if (infoOpen) {
        AlertDialog(
            onDismissRequest = { infoOpen = false },
            title = { Text(stringResource(R.string.home_chests_info_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.home_chests_info_body))
                    ChestTier.entries.forEach { tier ->
                        val minutes = (tier.unlockDurationMillis / 60_000L).toInt()
                        Text(
                            text = stringResource(R.string.home_chests_info_line, stringResource(tier.labelRes()), minutes, tier.durationHours()),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(
                                when (tier) {
                                    ChestTier.SILVER -> R.string.chest_loot_silver
                                    ChestTier.GOLD -> R.string.chest_loot_gold
                                    ChestTier.RARE -> R.string.chest_loot_rare
                                },
                                tier.goldReward.first, tier.goldReward.last
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(stringResource(R.string.home_chests_info_speedup), style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { TextButton(onClick = { infoOpen = false }) { Text(stringResource(R.string.home_chests_info_ok)) } }
        )
    }
}

@Composable
private fun HomeChestSlot(chest: Chest?, nowMillis: Long, onClick: () -> Unit, compact: Boolean, modifier: Modifier = Modifier) {
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
                text = stringResource(R.string.chest_slot_empty_title),
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
    Box(
        modifier = modifier
            .fillMaxHeight()
            .graphicsLayer { scaleX = pulse; scaleY = pulse }
            .clip(shape)
            .border(if (ready) 3.dp else 2.dp, tier.borderColor().copy(alpha = if (ready) 1f else 0.85f), shape)
            .clickable(onClick = onClick)
    ) {
        ChestBackdrop(tier = tier, modifier = Modifier.matchParentSize())
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 3.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.height(if (compact) 42.dp else 54.dp), contentAlignment = Alignment.Center) {
                ChestImage(tier = tier, width = if (compact) 48.dp else 62.dp)
            }
            Text(
                text = stringResource(tier.labelRes()),
                fontWeight = FontWeight.ExtraBold,
                color = tier.onBackdrop(),
                textAlign = TextAlign.Center,
                maxLines = if (compact) 1 else 2,
                softWrap = !compact,
                fontSize = if (compact) 9.5.sp else 11.sp,
                lineHeight = 12.sp,
                modifier = Modifier.weight(1f)
            )
            val pillColor = when {
                ready -> AppTheme.tokens.success
                unlocking -> Color(0xFF2B1A12)
                else -> tier.accent()
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(pillColor)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                if (unlocking) {
                    Icon(Icons.Filled.Schedule, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                }
                Text(
                    text = when {
                        ready -> stringResource(R.string.chests_open_button).uppercase(java.util.Locale.getDefault()) + "!"
                        // Down to the second: this is what the player watches tick.
                        unlocking -> formatCountdown(chest.remainingMillis(nowMillis))
                        else -> stringResource(R.string.chest_duration_hours, tier.durationHours())
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

/**
 * One-line text that is the same size everywhere (12sp) and only shrinks —
 * never wraps or overflows — when it would not fit, so a long pen name or a
 * seven-digit gold balance stays inside its chip.
 */
@Composable
private fun FitText(text: String, color: Color, maxSp: Float = 12f, minSp: Float = 6f, twoLines: Boolean = false) {
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    // A multi-word name breaks at the space nearest its middle, so "Kurşun Halka" becomes "Kurşun / Halka".
    val shown = if (twoLines && text.contains(' ')) {
        val mid = text.length / 2
        val cut = text.indices.filter { text[it] == ' ' }.minByOrNull { kotlin.math.abs(it - mid) }!!
        text.substring(0, cut).trim() + "\n" + text.substring(cut + 1).trim()
    } else text
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
            fontFamily = DisplayFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = sp.sp,
            lineHeight = (sp * 1.05f).sp,
            textAlign = TextAlign.Center,
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
    val animated = remember { Animatable(0f) }
    LaunchedEffect(fraction) { animated.animateTo(fraction.coerceIn(0f, 1f), tween(1100, easing = FastOutSlowInEasing)) }
    val infinite = rememberInfiniteTransition(label = "xpSheen")
    val sheen = infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = androidx.compose.animation.core.LinearEasing), RepeatMode.Restart),
        label = "sheen"
    )
    Box(modifier = modifier.height(20.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val h = size.height
            val rim = 2.dp.toPx()
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
