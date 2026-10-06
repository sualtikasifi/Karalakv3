package com.sualtikasifi.cizimhafiza.presentation.chests

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.Chest
import com.sualtikasifi.cizimhafiza.domain.model.ChestReward
import com.sualtikasifi.cizimhafiza.domain.model.ChestTier
import com.sualtikasifi.cizimhafiza.presentation.common.PrimaryButton
import com.sualtikasifi.cizimhafiza.presentation.common.icon
import com.sualtikasifi.cizimhafiza.presentation.common.shortRes
import com.sualtikasifi.cizimhafiza.presentation.common.tint
import com.sualtikasifi.cizimhafiza.presentation.common.artRes
import com.sualtikasifi.cizimhafiza.presentation.common.openVideoRes
import com.sualtikasifi.cizimhafiza.presentation.common.labelRes
import com.sualtikasifi.cizimhafiza.presentation.theme.DisplayFont
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** The colour a tier reads as everywhere a chest is drawn: card tint, timer pill, opening rays. */
fun ChestTier.accent(): Color = when (this) {
    ChestTier.SILVER -> Color(0xFFE08A2E)
    ChestTier.GOLD -> Color(0xFFE5A800)
    ChestTier.RARE -> Color(0xFF8E4FE0)
}

/** A much lighter partner of [accent] — the glow behind the chest and the fill of a slot card. */
fun ChestTier.glow(): Color = lerp(accent(), Color.White, 0.72f)

fun ChestTier.durationHours(): Int = (unlockDurationMillis / (60 * 60 * 1000L)).toInt()

/** 3:05:09, or 5:09 under an hour. */
fun formatCountdown(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, seconds)
    else String.format("%d:%02d", minutes, seconds)
}

fun Chest.remainingMillis(nowMillis: Long): Long =
    if (unlockStartedAtMillis == null) tier.unlockDurationMillis
    else (unlockStartedAtMillis + tier.unlockDurationMillis - nowMillis).coerceAtLeast(0)

fun Chest.unlockProgress(nowMillis: Long): Float =
    if (unlockStartedAtMillis == null) 0f
    else (1f - remainingMillis(nowMillis).toFloat() / tier.unlockDurationMillis).coerceIn(0f, 1f)

/** The chest illustration (transparent PNG), scaled to [width] keeping its own aspect. */
@Composable
fun ChestImage(tier: ChestTier, width: Dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(tier.artRes()),
        contentDescription = null,
        modifier = modifier.width(width)
    )
}


private class Particle(
    val angle: Float,
    val speed: Float,
    val size: Float,
    val spin: Float,
    val colorIndex: Int,
    val coin: Boolean
)

/**
 * Plays a tier's chest-opening video full-screen (cropped to fill, never
 * letterboxed — see openVideoRes) and calls [onEnded] once, the moment
 * playback reaches the end. No controls, no loop, no audio duplication
 * with the rest of the scene: the video carries its own sound.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun ChestOpeningVideo(tier: ChestTier, onEnded: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val latestOnEnded = androidx.compose.runtime.rememberUpdatedState(onEnded)
    val player = remember(tier) {
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            val uri = android.net.Uri.parse("android.resource://${context.packageName}/${tier.openVideoRes()}")
            setMediaItem(androidx.media3.common.MediaItem.fromUri(uri))
            prepare()
            playWhenReady = true
        }
    }
    // Every clip fades to white at its very end, and the tail is dead time:
    // hand over to the reward screen [VIDEO_TAIL_TRIM_MS] before the last frame
    // (or at the real end, whichever comes first) so opening feels quick.
    androidx.compose.runtime.LaunchedEffect(player) {
        var fired = false
        fun fire() {
            if (!fired) { fired = true; latestOnEnded.value() }
        }
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == androidx.media3.common.Player.STATE_ENDED) fire()
            }
        }
        player.addListener(listener)
        try {
            while (!fired) {
                val duration = player.duration
                if (duration != androidx.media3.common.C.TIME_UNSET && duration > VIDEO_TAIL_TRIM_MS * 2 &&
                    player.currentPosition >= duration - VIDEO_TAIL_TRIM_MS
                ) fire()
                delay(40)
            }
        } finally {
            player.removeListener(listener)
        }
    }
    androidx.compose.runtime.DisposableEffect(player) {
        onDispose { player.release() }
    }
    androidx.compose.ui.viewinterop.AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            androidx.media3.ui.PlayerView(ctx).apply {
                this.player = player
                useController = false
                resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }
        }
    )
}

/** How much of the opening video's white-out tail is skipped. */
private const val VIDEO_TAIL_TRIM_MS = 1_500L

/**
 * The full-screen "kasa açma" moment: a tier-specific hand-made video plays
 * the chest opening, then hands off to a bright reward screen — white like the
 * video's last frame — where the gold counts up on a card under soft rays and
 * confetti in the chest's own colour.
 *
 * The reward is already known (and already paid) when this starts — the
 * animation is presentation only, so backing out of the app mid-way can
 * never lose it.
 */
@Composable
fun ChestOpeningDialog(reward: ChestReward, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        ChestOpeningScene(reward = reward, onDismiss = onDismiss)
    }
}

private val RewardInk = Color(0xFF3A2416)
private val RewardGold = Color(0xFFC77F00)

@Composable
private fun ChestOpeningScene(reward: ChestReward, onDismiss: () -> Unit) {
    val tier = reward.tier
    val accent = tier.accent()
    val light = tier.glow()
    val palette = remember(tier) {
        listOf(accent, light, Color(0xFFFFC928), Color(0xFFFF7A59), Color(0xFF5BD6FF), Color(0xFF7BD88F))
    }
    val particles = remember {
        val rnd = Random(reward.gold * 31 + tier.ordinal)
        List(56) {
            Particle(
                angle = rnd.nextFloat() * (2f * PI.toFloat()),
                speed = 260f + rnd.nextFloat() * 620f,
                size = 6f + rnd.nextFloat() * 10f,
                spin = (rnd.nextFloat() - 0.5f) * 900f,
                colorIndex = rnd.nextInt(palette.size),
                coin = it % 4 == 0
            )
        }
    }

    val burst = remember { Animatable(0f) }
    val reveal = remember { Animatable(0f) }
    val fadeIn = remember { Animatable(0f) }
    val count = remember { Animatable(0f) }
    // 0 = the opening video is playing, 1 = it just ended and the reward is
    // fading in / counting up, 2 = settled, the "topla" button is live.
    var stage by remember { mutableIntStateOf(0) }
    val haptic = LocalHapticFeedback.current
    val sound = com.sualtikasifi.cizimhafiza.presentation.common.rememberSoundManager()

    LaunchedEffect(stage == 1) {
        if (stage != 1) return@LaunchedEffect
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        launch { fadeIn.animateTo(1f, tween(320)) }
        launch { burst.animateTo(1f, tween(1300, easing = LinearOutSlowInEasing)) }
        launch { reveal.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 220f)) }
        sound.playCoinShower()
        delay(250)
        count.animateTo(reward.gold.toFloat(), tween(900, easing = FastOutSlowInEasing))
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        stage = 2
    }

    val infinite = rememberInfiniteTransition(label = "rays")
    val rayAngle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(16_000, easing = LinearEasing), RepeatMode.Restart),
        label = "rayAngle"
    )

    val density = LocalDensity.current
    val hasExtras = reward.jokers.isNotEmpty() || reward.penDrop != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (stage == 0) Color.Black else Color.White),
        contentAlignment = Alignment.Center
    ) {
        // The chest actually opening — a tier-specific hand-made video,
        // full-screen. Everything below only starts once it ends.
        if (stage == 0) {
            ChestOpeningVideo(tier = tier, onEnded = { if (stage == 0) stage = 1 })
            // A tap anywhere skips straight to the reward (already paid — see the class comment).
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { if (stage == 0) stage = 1 }
            )
        }

        if (stage >= 1) {
            val a = fadeIn.value
            // Soft tint, rays and confetti — light enough to sit on white.
            Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = a }) {
                val center = Offset(size.width / 2f, size.height * 0.36f)
                val dp = density.density
                val glowRadius = 340f * dp
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(light.copy(alpha = 0.75f), light.copy(alpha = 0.22f), Color.Transparent),
                        center = center,
                        radius = glowRadius
                    ),
                    radius = glowRadius,
                    center = center
                )
                val rayCount = 16
                val rayLen = size.maxDimension
                withTransform({ rotate(rayAngle, center) }) {
                    for (i in 0 until rayCount) {
                        val ang = (i * 360f / rayCount) * (PI.toFloat() / 180f)
                        val half = (PI.toFloat() / rayCount) * 0.28f
                        val path = Path().apply {
                            moveTo(center.x, center.y)
                            lineTo(center.x + cos(ang - half) * rayLen, center.y + sin(ang - half) * rayLen)
                            lineTo(center.x + cos(ang + half) * rayLen, center.y + sin(ang + half) * rayLen)
                            close()
                        }
                        drawPath(
                            path = path,
                            brush = Brush.radialGradient(
                                colors = listOf(accent.copy(alpha = 0.16f), Color.Transparent),
                                center = center,
                                radius = rayLen * 0.7f
                            )
                        )
                    }
                }
                val b = burst.value
                drawCircle(
                    color = accent.copy(alpha = (1f - b).coerceIn(0f, 1f) * 0.55f),
                    radius = (50f + 560f * b) * dp,
                    center = center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = (14f * (1f - b) + 2f) * dp)
                )
                val t = b * 1.5f
                particles.forEach { p ->
                    val x = center.x + cos(p.angle) * p.speed * t * dp
                    val y = center.y + sin(p.angle) * p.speed * t * dp + 0.5f * 900f * t * t * dp
                    val alpha = (1f - b * b).coerceIn(0f, 1f)
                    val s = p.size * dp
                    val color = palette[p.colorIndex]
                    if (p.coin) {
                        drawCircle(Color(0xFFFFC928).copy(alpha = alpha), radius = s, center = Offset(x, y))
                        drawCircle(Color(0xFFFFF0A0).copy(alpha = alpha), radius = s * 0.6f, center = Offset(x, y))
                    } else {
                        rotate(p.spin * t, Offset(x, y)) {
                            drawRect(
                                color = color.copy(alpha = alpha),
                                topLeft = Offset(x - s / 2f, y - s / 2f),
                                size = Size(s, s * 1.7f)
                            )
                        }
                    }
                }
            }

            val r = reveal.value
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = fadeIn.value }
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.chests_reward_title),
                    fontFamily = DisplayFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp,
                    color = RewardInk,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer { scaleX = r; scaleY = r }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(tier.labelRes()),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = accent,
                    modifier = Modifier
                        .graphicsLayer { scaleX = r; scaleY = r }
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                        .background(accent.copy(alpha = 0.14f))
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                )
                Spacer(modifier = Modifier.height(18.dp))
                Column(
                    modifier = Modifier
                        .graphicsLayer { scaleX = 0.85f + 0.15f * r; scaleY = 0.85f + 0.15f * r }
                        .fillMaxWidth()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(28.dp))
                        .background(Color.White)
                        .border(2.5.dp, accent.copy(alpha = 0.55f), androidx.compose.foundation.shape.RoundedCornerShape(28.dp))
                        .padding(horizontal = 20.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(R.drawable.icon_gold_coin),
                        contentDescription = null,
                        modifier = Modifier
                            .size(if (hasExtras) 92.dp else 124.dp)
                            .graphicsLayer { rotationY = (1f - r.coerceIn(0f, 1f)) * 540f }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.chests_reward_gold, count.value.toInt()),
                        color = RewardGold,
                        fontFamily = DisplayFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = if (hasExtras) 40.sp else 48.sp,
                        textAlign = TextAlign.Center
                    )
                    if (stage >= 2 && hasExtras) {
                        Spacer(modifier = Modifier.height(16.dp))
                        androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            reward.jokers.forEach { (type, n) ->
                                androidx.compose.foundation.layout.Column(
                                    modifier = Modifier
                                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                                        .background(type.tint())
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    com.sualtikasifi.cizimhafiza.presentation.common.JokerArt(type, 40.dp)
                                    Text("×$n", color = Color.White, fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                                    Text(stringResource(type.shortRes()), color = Color.White.copy(alpha = 0.95f), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                }
                            }
                        }
                        reward.penDrop?.let { pen ->
                            Spacer(modifier = Modifier.height(12.dp))
                            val colors = pen.colors.map { Color(it) }
                            androidx.compose.foundation.layout.Row(
                                modifier = Modifier
                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                                    .background(RewardInk.copy(alpha = 0.07f))
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 46.dp, height = 12.dp)
                                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                                        .background(Brush.horizontalGradient(if (colors.size > 1) colors else listOf(colors.first(), colors.first())))
                                )
                                Text(
                                    text = stringResource(R.string.joker_pen_won) + " " + stringResource(pen.labelRes),
                                    color = RewardInk,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(22.dp))
                if (stage >= 2) {
                    PrimaryButton(
                        text = stringResource(R.string.chests_reward_button),
                        onClick = onDismiss,
                        modifier = Modifier.width(240.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(58.dp))
                }
            }
        }
    }
}
