package com.sualtikasifi.cizimhafiza.presentation.levelmap

import com.sualtikasifi.cizimhafiza.presentation.common.cachedPainterResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.presentation.common.LetteredText
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedStyle
import com.sualtikasifi.cizimhafiza.presentation.common.CurrentPositionGlow
import com.sualtikasifi.cizimhafiza.presentation.common.RaisedCard
import com.sualtikasifi.cizimhafiza.presentation.common.ScreenTopActions
import com.sualtikasifi.cizimhafiza.presentation.theme.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

// The artwork files are all this size (bg_world_N); the stop positions in LevelNodePositions are fractions of it.
private const val ArtWidth = 841f
private const val ArtHeight = 1870f

// How much taller than the window the artwork is drawn, so the map scrolls a little instead of being one fixed picture.
private const val ArtStretch = 1.1f

private val CoinSize = 58.dp

/** Seconds counted on the PLAY button before a level opens. */
private const val PlayCountdownSeconds = 3
private val RingSize = 80.dp

// How far up the artwork its bottom edge melts into the plain colour below it.
private val FadeHeight = 130.dp

@Composable
fun LevelMapScreen(
    worldId: Int,
    onLevelClick: (levelIndex: Int) -> Unit,
    onBack: () -> Unit,
    viewModel: LevelMapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val world = uiState.world
    val levels = uiState.levels

    // The stop whose panel is open: what the player tapped, else where they left off (the next level to play).
    var tapped by rememberSaveable { mutableStateOf<Int?>(null) }
    val current = tapped ?: levels.firstOrNull { it.isNext }?.levelIndex ?: levels.lastOrNull { it.unlocked }?.levelIndex
    val selected = levels.firstOrNull { it.levelIndex == current }

    // PLAY starts a 3-2-1 on the button itself; the level opens when it reaches the end. Another tap on the map cancels it.
    var startingLevel by remember { mutableStateOf<Int?>(null) }
    var countdown by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(startingLevel) {
        val level = startingLevel ?: return@LaunchedEffect
        for (second in PlayCountdownSeconds downTo 1) {
            countdown = second
            delay(1_000)
        }
        startingLevel = null
        countdown = null
        onLevelClick(level)
    }

    val scroll = rememberScrollState()
    val positions = LevelNodePositions[world?.id] ?: LevelNodePositions.getValue(1)
    val density = LocalDensity.current

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color(0xFF2F5D2B))) {
        val screenWidth = maxWidth
        // The panel's own height (its picture is 1080x350) plus the margin round it: the artwork carries on under it,
        // and the scroll can bring any stop above it.
        val panelHeight = (screenWidth - PanelSideMargin * 2) * (350f / 1080f)
        val artHeight = screenWidth * (ArtHeight / ArtWidth) * ArtStretch
        val mapHeight = artHeight + panelHeight + 72.dp
        val scale = artHeight.value / ArtHeight
        val edge = Color(LevelArtBottomColors[world?.id] ?: 0xFF2F5D2B)

        // Brings the open stop into the upper part of the window whenever it changes (and once the first layout
        // has told the scroll state how far it can go).
        LaunchedEffect(current, levels.isNotEmpty()) {
            val index = current ?: return@LaunchedEffect
            if (levels.isEmpty()) return@LaunchedEffect
            snapshotFlow { scroll.maxValue }.first { it > 0 }
            val nodeY = with(density) { (artHeight * (positions[index - 1].second / 100f)).toPx() }
            val viewport = with(density) { maxHeight.toPx() }
            scroll.animateScrollTo(
                (nodeY - viewport * 0.38f).toInt().coerceIn(0, scroll.maxValue),
                tween(700, easing = FastOutSlowInEasing)
            )
        }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(scroll)) {
            Box(modifier = Modifier.width(screenWidth).height(mapHeight)) {
                Image(
                    painter = cachedPainterResource(worldBackgroundRes(world?.id) ?: R.drawable.bg_world_1),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(artHeight)
                )
                // Below the picture: its own bottom colour, so the ground seems to carry on under the panel.
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(mapHeight - artHeight + FadeHeight)
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                (FadeHeight / (mapHeight - artHeight + FadeHeight)) to edge,
                                1f to edge
                            )
                        )
                )
                // A golden ring that travels from stop to stop, so moving on reads as walking along the path.
                current?.let { open ->
                    val (ox, oy) = positions[open - 1]
                    val ringX by animateDpAsState((screenWidth.value / 2f + ArtWidth * scale * (ox / 100f - 0.5f)).dp, tween(650, easing = FastOutSlowInEasing), label = "ring-x")
                    val ringY by animateDpAsState((artHeight.value * oy / 100f).dp, tween(650, easing = FastOutSlowInEasing), label = "ring-y")
                    val pulse by rememberInfiniteTransition(label = "ring").animateFloat(
                        initialValue = 0.9f, targetValue = 1.08f,
                        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                        label = "ring-pulse"
                    )
                    Box(
                        modifier = Modifier
                            .offset(x = ringX - RingSize / 2, y = ringY - RingSize / 2 + 6.dp)
                            .size(RingSize)
                            .graphicsLayer { scaleX = pulse; scaleY = pulse }
                            .border(5.dp, Color(0xFFFFE066), CircleShape)
                    )
                }
                levels.forEach { level ->
                    val (px, py) = positions[level.levelIndex - 1]
                    // Cropped to fill the box, the picture is as wide as its height says; the centre stays put.
                    val centreX = screenWidth.value / 2f + ArtWidth * scale * (px / 100f - 0.5f)
                    val centreY = artHeight.value * py / 100f
                    LevelCoin(
                        level = level,
                        isOpen = level.levelIndex == current,
                        onClick = { if (level.unlocked) { startingLevel = null; countdown = null; tapped = level.levelIndex } },
                        modifier = Modifier.offset(x = (centreX - CoinBox.value / 2f).dp, y = (centreY - CoinBox.value / 2f).dp)
                    )
                }
            }
        }

        // Which world this is, stated on the page.
        world?.let { w ->
            val cleared = levels.count { it.stars > 0 }
            ScreenTopActions(onBack = onBack, modifier = Modifier.align(Alignment.TopStart)) {
                RaisedCard(corner = 18.dp, raise = 4.dp) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(text = w.emoji, style = MaterialTheme.typography.titleMedium)
                        Column {
                            Text(text = stringResource(w.displayNameRes), style = MaterialTheme.typography.labelLarge, maxLines = 1)
                            Text(
                                text = stringResource(R.string.world_progress_format, cleared),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } ?: ScreenTopActions(onBack = onBack, modifier = Modifier.align(Alignment.TopStart))

        AnimatedVisibility(
            visible = selected != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            if (selected != null && world != null) {
                // Moving between stops slides the panel's content the way the ring travels: forwards from the right,
                // backwards from the left.
                AnimatedContent(
                    targetState = selected,
                    contentKey = { it.levelIndex },
                    transitionSpec = {
                        val dir = if (targetState.levelIndex >= initialState.levelIndex) 1 else -1
                        (slideInHorizontally(tween(380, easing = FastOutSlowInEasing)) { dir * it / 3 } + fadeIn(tween(300)))
                            .togetherWith(slideOutHorizontally(tween(380, easing = FastOutSlowInEasing)) { -dir * it / 3 } + fadeOut(tween(200)))
                    },
                    label = "level-panel"
                ) { shown ->
                    LevelPanel(
                        level = shown,
                        worldIconRes = world.iconRes,
                        countdown = countdown,
                        onPlay = { if (startingLevel == null) startingLevel = shown.levelIndex },
                        modifier = Modifier.navigationBarsPadding().padding(horizontal = PanelSideMargin, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

private val PanelSideMargin = 12.dp

/** The box a stop occupies — a little larger than the coin so the glow and the stars above it fit. */
private val CoinBox = 72.dp

/**
 * One stop on the painted path: a numbered wooden coin (orange when playable, blue and glowing for the next one to
 * play, grey with a padlock while locked), with the stars earned floating above it.
 */
@Composable
private fun LevelCoin(level: LevelNodeState, isOpen: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val rim = Color(0xFF7A4313)
    val pop by animateFloatAsState(if (isOpen) 1.14f else 1f, spring(dampingRatio = 0.45f, stiffness = 380f), label = "coin-pop")
    val face = when {
        !level.unlocked -> Brush.verticalGradient(listOf(Color(0xFF6C7686), Color(0xFF3A4250)))
        level.isNext -> Brush.verticalGradient(listOf(Color(0xFF52B3FF), Color(0xFF1F73D3)))
        else -> Brush.verticalGradient(listOf(Color(0xFFFFB347), Color(0xFFE9801D)))
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(CoinBox)
            .clickable(
                enabled = level.unlocked,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        if (level.isNext) CurrentPositionGlow(modifier = Modifier.matchParentSize())
        if (level.unlocked && (level.stars > 0 || level.isNext)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                modifier = Modifier.align(Alignment.TopCenter).offset(y = (-6).dp)
            ) {
                repeat(3) { i ->
                    Icon(
                        imageVector = if (i < level.stars) Icons.Filled.Star else Icons.Outlined.StarOutline,
                        contentDescription = null,
                        tint = if (i < level.stars) Color(0xFFFFD43B) else Color(0xFF7A4313),
                        modifier = Modifier.size(if (i == 1) 20.dp else 17.dp)
                    )
                }
            }
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(top = 12.dp)
                .size(CoinSize)
                .graphicsLayer { scaleX = pop; scaleY = pop }
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(face)
                .border(3.dp, if (level.unlocked) rim else Color(0xFFAEB9CA), CircleShape)
        ) {
            if (level.unlocked) {
                Text(
                    text = level.levelIndex.toString(),
                    style = PaintedStyle(
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        shadow = androidx.compose.ui.graphics.Shadow(Color(0xFF5A300C), androidx.compose.ui.geometry.Offset(0f, 3f), 3f)
                    )
                )
            } else {
                Padlock(modifier = Modifier.size(30.dp))
            }
        }
        if (!level.unlocked) {
            // The stage number sits on a small plate under the coin, so a locked stop still says which one it is.
            Text(
                text = level.levelIndex.toString(),
                style = PaintedStyle(color = Color(0xFFEAF0F7), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 2.dp)
                    .background(Brush.verticalGradient(listOf(Color(0xFF4A5362), Color(0xFF2B323D))), RoundedCornerShape(8.dp))
                    .border(1.5.dp, Color(0xFF9AA6B8), RoundedCornerShape(8.dp))
                    .padding(horizontal = 7.dp, vertical = 1.dp)
            )
        }
    }
}

/** A steel padlock with a keyhole, drawn rather than taken from the icon set so it can carry a highlight and a shadow. */
@Composable
private fun Padlock(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val bodyTop = h * 0.44f
        drawArc(
            color = Color(0xFFCBD3DF),
            startAngle = 180f, sweepAngle = 180f, useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.24f, h * 0.06f),
            size = androidx.compose.ui.geometry.Size(w * 0.52f, h * 0.76f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.13f)
        )
        val body = androidx.compose.ui.geometry.Rect(w * 0.12f, bodyTop, w * 0.88f, h * 0.96f)
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color(0xFFF1F4F9), Color(0xFFA9B4C4), Color(0xFF7D8999)), startY = body.top, endY = body.bottom),
            topLeft = body.topLeft, size = body.size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.14f)
        )
        drawRoundRect(
            color = Color(0xFF3B4452), topLeft = body.topLeft, size = body.size,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.14f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.05f)
        )
        val cx = w / 2f
        val cy = body.top + body.height * 0.42f
        drawCircle(Color(0xFF2B323D), radius = w * 0.085f, center = androidx.compose.ui.geometry.Offset(cx, cy))
        drawRoundRect(
            Color(0xFF2B323D),
            topLeft = androidx.compose.ui.geometry.Offset(cx - w * 0.035f, cy),
            size = androidx.compose.ui.geometry.Size(w * 0.07f, h * 0.17f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.03f)
        )
    }
}

/** The card that rises from the bottom when a stop is tapped: its number, the stars so far and the button that starts it. */
@Composable
private fun LevelPanel(level: LevelNodeState, worldIconRes: Int, countdown: Int?, onPlay: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().aspectRatio(1080f / 350f)) {
        Image(
            painter = painterResource(R.drawable.level_panel),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize().padding(start = 20.dp, end = 18.dp, top = 12.dp, bottom = 12.dp)
        ) {
            Image(
                painter = painterResource(worldIconRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(2.dp, Color(0xFF9C5A1B), RoundedCornerShape(14.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.level_panel_title, level.levelIndex),
                    style = PaintedStyle(color = Color(0xFF4A2A10), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold),
                    maxLines = 1
                )
                Text(
                    text = stringResource(R.string.level_panel_subtitle),
                    style = PaintedStyle(color = Color(0xFF5A321F), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, lineHeight = 15.sp),
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(3) { i ->
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = if (i < level.stars) Color(0xFFFFC107) else Color(0xFF8A6A4A),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            PlayButton(countdown = countdown, onClick = onPlay)
        }
    }
}

@Composable
private fun PlayButton(countdown: Int?, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .width(112.dp)
            .height(54.dp)
            .shadow(3.dp, shape)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF7BE042), Color(0xFF2FA524))))
            .border(3.dp, Color(0xFF1F7A1A), shape)
            .clickable(enabled = countdown == null, onClick = onClick)
    ) {
        // Each number lands big and settles, like the lobby's start count.
        val pop = remember(countdown) { Animatable(if (countdown != null) 1.6f else 1f) }
        LaunchedEffect(countdown) { if (countdown != null) pop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 300f)) }
        LetteredText(
            text = countdown?.toString() ?: stringResource(R.string.level_play),
            size = if (countdown != null) 30.sp else 22.sp,
            outline = Color(0xFF14540F),
            modifier = Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value }
        )
    }
}

/** The full-screen artwork of a world's level map. */
private fun worldBackgroundRes(worldId: Int?): Int? = when (worldId) {
    1 -> R.drawable.bg_world_1
    2 -> R.drawable.bg_world_2
    3 -> R.drawable.bg_world_3
    4 -> R.drawable.bg_world_4
    5 -> R.drawable.bg_world_5
    6 -> R.drawable.bg_world_6
    7 -> R.drawable.bg_world_7
    8 -> R.drawable.bg_world_8
    9 -> R.drawable.bg_world_9
    else -> null
}
