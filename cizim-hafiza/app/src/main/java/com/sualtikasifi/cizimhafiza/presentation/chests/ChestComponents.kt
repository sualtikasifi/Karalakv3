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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import com.sualtikasifi.cizimhafiza.presentation.common.pressable
import com.sualtikasifi.cizimhafiza.presentation.common.rememberFitScale
import com.sualtikasifi.cizimhafiza.presentation.common.scaledBy
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

/** How long this chest takes once started, with the chest-time rune taken off. */
val ChestTier.effectiveUnlockMillis: Long get() = com.sualtikasifi.cizimhafiza.domain.model.RuneLive.chestMillis(unlockDurationMillis)

/** "3 saat", "1 sa 48 dk" or "45 dk": the wait as it really is, runes included. */
@Composable
fun ChestTier.durationText(): String {
    val minutes = effectiveUnlockMillis / 60_000L
    val hours = (minutes / 60).toInt()
    val rest = (minutes % 60).toInt()
    return when {
        rest == 0 -> stringResource(if (hours == 1) R.string.chest_duration_hours_one else R.string.chest_duration_hours, hours)
        hours == 0 -> stringResource(R.string.chest_duration_minutes, rest)
        else -> stringResource(R.string.chest_duration_hm, hours, rest)
    }
}

/** 3:05:09, or 5:09 under an hour. */
fun formatCountdown(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    // Locale.ROOT: a countdown is digits and colons whatever the phone's language (some locales would write other digits).
    return if (hours > 0) String.format(java.util.Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    else String.format(java.util.Locale.ROOT, "%d:%02d", minutes, seconds)
}

fun Chest.remainingMillis(nowMillis: Long): Long =
    if (unlockStartedAtMillis == null) tier.effectiveUnlockMillis
    else (unlockStartedAtMillis + tier.unlockDurationMillis - nowMillis).coerceAtLeast(0)

fun Chest.unlockProgress(nowMillis: Long): Float =
    if (unlockStartedAtMillis == null) 0f
    else (1f - remainingMillis(nowMillis).toFloat() / tier.effectiveUnlockMillis).coerceIn(0f, 1f)

/** The chest illustration (transparent PNG), scaled to [width] keeping its own aspect. */
@Composable
fun ChestImage(tier: ChestTier, width: Dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(tier.artRes()),
        contentDescription = null,
        modifier = modifier.width(width)
    )
}



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
    // hand over to the reward screen [ChestTier.openVideoTailMs] before the last frame
    // (or at the real end, whichever comes first) so opening feels quick.
    val tailMs = tier.openVideoTailMs()
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
                if (duration != androidx.media3.common.C.TIME_UNSET && duration > tailMs * 2 &&
                    player.currentPosition >= duration - tailMs
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

/**
 * How much of the opening video's white-out tail is skipped. Every clip now ends right as it turns fully white, so
 * only a few frames are left to cut.
 */
private fun ChestTier.openVideoTailMs(): Long = 150L

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

@Composable
private fun ChestOpeningScene(reward: ChestReward, onDismiss: () -> Unit) {
    val tier = reward.tier
    val accent = tier.accent()
    val light = tier.glow()
    val loot = remember(tier) { fallingLoot(Random(reward.gold * 31 + tier.ordinal)) }
    // Jokers in their usual order, then the pen: the small cards under the gold.
    val extras: List<RewardExtra> = remember(reward) {
        reward.jokers.entries.filter { it.value > 0 }.sortedBy { it.key.ordinal }.map { RewardExtra.Joker(it.key, it.value) } +
            listOfNotNull(reward.penDrop?.let { RewardExtra.Pen(it) })
    }

    // The board comes in piece by piece once the video has ended: the sign drops, the frame opens, the ribbon
    // unrolls, the chest pops, the gold counts up, the cards follow one by one, the button last.
    val fadeIn = remember { Animatable(0f) }
    val sign = remember { Animatable(0f) }
    val board = remember { Animatable(0f) }
    val ribbon = remember { Animatable(0f) }
    val hero = remember { Animatable(0f) }
    val count = remember { Animatable(0f) }
    val cards = remember(extras.size) { List(extras.size) { Animatable(0f) } }
    val button = remember { Animatable(0f) }
    // 0 = the opening video is playing, 1 = it just ended and the board is coming in, 2 = settled, "Harika!" is live.
    var stage by remember { mutableIntStateOf(0) }
    val haptic = LocalHapticFeedback.current
    val sound = com.sualtikasifi.cizimhafiza.presentation.common.rememberSoundManager()
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(stage >= 1) {
        if (stage < 1) return@LaunchedEffect
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        sound.playChestOpenTail(context)
        sound.playCoinShower()
        launch { fadeIn.animateTo(1f, tween(320)) }
        launch { sign.animateTo(1f, spring(dampingRatio = 0.48f, stiffness = 240f)) }
        delay(140)
        launch { board.animateTo(1f, spring(dampingRatio = 0.68f, stiffness = 300f)) }
        delay(200)
        launch { ribbon.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = 380f)) }
        delay(120)
        launch { hero.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 300f)) }
        delay(180)
        count.animateTo(reward.gold.toFloat(), tween(900, easing = FastOutSlowInEasing))
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        cards.forEach { card ->
            launch { card.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 380f)) }
            delay(140)
        }
        button.animateTo(1f, tween(260))
        stage = 2
    }
    // A tap while the board is still coming in shows it all at once (the reward is already paid either way).
    fun showAll() {
        if (stage != 1) return
        stage = 2
        scope.launch {
            listOf(fadeIn, sign, board, ribbon, hero, button).forEach { it.snapTo(1f) }
            cards.forEach { it.snapTo(1f) }
            count.snapTo(reward.gold.toFloat())
        }
    }

    val infinite = rememberInfiniteTransition(label = "reward")
    val rayAngle by infinite.animateFloat(0f, 360f, infiniteRepeatable(tween(18_000, easing = LinearEasing), RepeatMode.Restart), label = "rays")
    val fall by infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(9_000, easing = LinearEasing), RepeatMode.Restart), label = "fall")
    val twinkle by infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(1_600, easing = LinearEasing), RepeatMode.Restart), label = "twinkle")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (stage == 0) Color.Black else Color.White)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { showAll() },
        contentAlignment = Alignment.Center
    ) {
        // The chest actually opening — a tier-specific hand-made video, full-screen. It ends on white, which is
        // where the board below begins. A tap skips it.
        if (stage == 0) {
            ChestOpeningVideo(tier = tier, onEnded = { if (stage == 0) stage = 1 })
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
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
                contentAlignment = Alignment.Center
            ) {
                // The board is laid out in the design's own 1080 × 1920 units, scaled to fit the screen.
                val u = minOf(maxWidth / BOARD_W, maxHeight / BOARD_H)
                val fontScale0 = LocalDensity.current.fontScale
                fun fs(art: Float) = (art * u.value / fontScale0).sp
                fun Modifier.at(x: Float, y: Float, w: Float, h: Float) = this.offset(u * x, u * y).size(u * w, u * h)
                val compact = extras.isEmpty()
                val heroY = if (compact) 960f else 915f
                val amountY = if (compact) 1185f else 1110f

                // Behind everything, full-screen: the light, the rays and the loot still falling from the video.
                Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = fadeIn.value }) {
                    val unit = u.toPx()
                    val center = Offset(size.width / 2f, size.height / 2f + (heroY - BOARD_H / 2f) * unit)
                    drawRewardBackdrop(center, unit, accent, light, rayAngle)
                    drawFallingLoot(loot, fall, unit)
                }

                Box(Modifier.size(u * BOARD_W, u * BOARD_H)) {
                    // The frame the rewards are laid out on.
                    Image(
                        painter = painterResource(R.drawable.tut_frame_mid),
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.at(60f, 510f, 960f, 1092f).graphicsLayer {
                            alpha = board.value.coerceIn(0f, 1f)
                            val s = 0.86f + 0.14f * board.value
                            scaleX = s; scaleY = s
                        }
                    )
                    // The mascot's sign drops in from above, swinging a little on its way.
                    Box(
                        Modifier.at(90f, 20f, 900f, 563f).graphicsLayer {
                            val t = sign.value
                            translationY = (1f - t) * -560f * u.toPx()
                            rotationZ = (1f - t) * -7f
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f)
                            alpha = t.coerceIn(0f, 1f)
                        }
                    ) {
                        Image(painterResource(R.drawable.rs_head), null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
                        val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
                        RewardLettering(
                            text = stringResource(R.string.chests_reward_title).uppercase(locale),
                            size = fs(92f),
                            top = Color(0xFFFFFAD7), bottom = Color(0xFFFFC032), outline = Color(0xFF552305),
                            modifier = Modifier.at(135f, 356f, 630f, 112f)
                        )
                    }
                    // The chest's ribbon: its name and its rarity in stars.
                    TierRibbon(
                        label = stringResource(tier.labelRes()),
                        stars = tier.stars(),
                        light = lerp(accent, Color.White, 0.25f),
                        dark = lerp(accent, Color.Black, 0.22f),
                        u = u,
                        fs = ::fs,
                        modifier = Modifier.at(191f, 666f, 698f, 124f).graphicsLayer {
                            scaleX = ribbon.value.coerceAtLeast(0f); alpha = ribbon.value.coerceIn(0f, 1f)
                        }
                    )
                    // The gold: an overflowing chest with sparkles round it, then the amount counting up.
                    Box(
                        Modifier.at(310f, heroY - 175f, 460f, 350f).graphicsLayer {
                            val s = hero.value.coerceAtLeast(0f); scaleX = s; scaleY = s
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(painterResource(R.drawable.rune_n_chest_gold), null, modifier = Modifier.size(u * 290f))
                        Canvas(Modifier.fillMaxSize()) { drawSparkles(twinkle, u.toPx()) }
                    }
                    Row(
                        Modifier.at(0f, amountY - 70f, BOARD_W, 140f).graphicsLayer { alpha = hero.value.coerceIn(0f, 1f) },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(painterResource(R.drawable.icon_gold_coin), null, modifier = Modifier.size(u * 100f))
                        Spacer(Modifier.width(u * 10f))
                        val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
                        val format = remember(locale) { java.text.NumberFormat.getIntegerInstance(locale) }
                        RewardLettering(
                            text = "+" + format.format(count.value.toInt()),
                            size = fs(120f),
                            top = Color(0xFFFFF6AA), bottom = Color(0xFFFFA514), outline = Color(0xFF5A2805),
                            modifier = Modifier.width(u * 560f),
                            align = Alignment.CenterStart
                        )
                    }
                    Text(
                        text = stringResource(R.string.chests_reward_gold_label).uppercase(androidx.compose.ui.platform.LocalConfiguration.current.locales[0]),
                        style = com.sualtikasifi.cizimhafiza.presentation.common.PaintedStyle(color = Color(0xFF965A14), fontSize = fs(40f), textAlign = TextAlign.Center),
                        modifier = Modifier.at(0f, amountY + 52f, BOARD_W, 56f).graphicsLayer { alpha = hero.value.coerceIn(0f, 1f) }
                    )
                    if (!compact) {
                        Canvas(Modifier.at(220f, 1222f, 640f, 20f).graphicsLayer { alpha = board.value.coerceIn(0f, 1f) }) {
                            drawLine(
                                color = Color(0xFFCDA064), start = Offset(0f, size.height / 2f), end = Offset(size.width, size.height / 2f),
                                strokeWidth = 4f * u.toPx(),
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(16f * u.toPx(), 14f * u.toPx()))
                            )
                            drawSparkle(Offset(size.width / 2f, size.height / 2f), 15f * u.toPx(), Color(0xFFE6AA3C))
                        }
                        val tile = if (extras.size > 3) 156f else 172f
                        val gap = 30f
                        val x0 = (BOARD_W - extras.size * tile - (extras.size - 1) * gap) / 2f
                        extras.forEachIndexed { i, extra ->
                            val x = x0 + i * (tile + gap)
                            val t = cards.getOrNull(i)?.value ?: 1f
                            Box(Modifier.at(x, 1262f, tile, tile + 64f).graphicsLayer {
                                val s = t.coerceAtLeast(0f); scaleX = s; scaleY = s; alpha = t.coerceIn(0f, 1f)
                            }) {
                                RewardCard(extra = extra, u = u, tile = tile, fs = ::fs)
                            }
                        }
                    }
                    // "Harika!" on the leafy orange button, once everything is in.
                    Box(
                        Modifier.at(210f, 1631f, 660f, 275f)
                            .graphicsLayer {
                                val t = button.value; alpha = t; val s = 0.8f + 0.2f * t; scaleX = s; scaleY = s
                            }
                            .then(
                                if (stage == 2) Modifier.pressable(pressedScale = 0.94f, onClick = onDismiss) else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(painterResource(R.drawable.rp_pill_orange), null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
                        RewardLettering(
                            text = stringResource(R.string.chests_reward_button),
                            size = fs(78f),
                            top = Color.White, bottom = Color(0xFFFFEBC8), outline = Color(0xFF823200),
                            modifier = Modifier.fillMaxWidth(0.62f).padding(bottom = u * 8f)
                        )
                    }
                }
            }
        }
    }
}

private const val BOARD_W = 1080f
private const val BOARD_H = 1920f

/** A chest's rarity, as the stars on its ribbon. */
private fun ChestTier.stars(): Int = when (this) {
    ChestTier.SILVER -> 1
    ChestTier.GOLD -> 2
    ChestTier.RARE -> 3
}

/** What a chest gave besides gold: a stack of one joker, or a store pen. */
private sealed interface RewardExtra {
    data class Joker(val type: com.sualtikasifi.cizimhafiza.domain.model.JokerType, val count: Int) : RewardExtra
    data class Pen(val skin: com.sualtikasifi.cizimhafiza.domain.model.PenSkin) : RewardExtra
}

/**
 * Game lettering for the reward board: a dark outline, a soft drop under it and a top-to-bottom gradient fill. Shrinks
 * (never wraps) to fit the box it is given.
 */
@Composable
private fun RewardLettering(
    text: String,
    size: androidx.compose.ui.unit.TextUnit,
    top: Color,
    bottom: Color,
    outline: Color,
    modifier: Modifier = Modifier,
    align: Alignment = Alignment.Center
) {
    BoxWithConstraints(modifier = modifier, contentAlignment = align) {
        val style = com.sualtikasifi.cizimhafiza.presentation.common.PaintedStyle(fontSize = size, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        val scale = rememberFitScale(text, style, 1, 0.5f)
        val base = style.scaledBy(scale)
        val stroke = with(LocalDensity.current) { base.fontSize.toPx() * 0.12f }
        val drop = with(LocalDensity.current) { (base.fontSize.toPx() * 0.07f).toDp() }
        Text(
            text,
            style = base.copy(color = outline.copy(alpha = 0.32f), drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(stroke * 1.4f, join = androidx.compose.ui.graphics.StrokeJoin.Round)),
            maxLines = 1, softWrap = false, modifier = Modifier.offset(y = drop)
        )
        Text(
            text,
            style = base.copy(color = outline, drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(stroke, join = androidx.compose.ui.graphics.StrokeJoin.Round)),
            maxLines = 1, softWrap = false
        )
        Text(text, style = base.copy(brush = Brush.verticalGradient(listOf(top, bottom))), maxLines = 1, softWrap = false)
    }
}

/** The tier's ribbon: a band with folded tails behind it, stitched edges, the rarity stars and the chest's name. */
@Composable
private fun TierRibbon(
    label: String,
    stars: Int,
    light: Color,
    dark: Color,
    u: Dp,
    fs: (Float) -> androidx.compose.ui.unit.TextUnit,
    modifier: Modifier
) {
    // Drawn in a 698 × 124 box: the band is the middle 540 × 88, the tails reach out beyond it.
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val k = size.width / 698f
            val bandL = 79f * k; val bandR = size.width - 79f * k; val bandT = 8f * k; val bandB = bandT + 88f * k
            val back = lerp(dark, Color.Black, 0.25f)
            val edge = lerp(dark, Color.Black, 0.55f)
            for (side in listOf(-1f, 1f)) {
                val inner = if (side < 0) bandL + 28f * k else bandR - 28f * k
                val outer = if (side < 0) 0f else size.width
                val notch = if (side < 0) 26f * k else size.width - 26f * k
                val tail = Path().apply {
                    moveTo(inner, bandT + 18f * k); lineTo(outer, bandT + 18f * k); lineTo(notch, (bandT + bandB) / 2f + 18f * k)
                    lineTo(outer, bandB + 18f * k); lineTo(inner, bandB + 18f * k); close()
                }
                drawPath(tail, back)
                drawPath(tail, edge, style = androidx.compose.ui.graphics.drawscope.Stroke(5f * k))
                val fold = Path().apply {
                    val x = if (side < 0) bandL else bandR
                    moveTo(inner, bandB); lineTo(x, bandB); lineTo(inner, bandB + 18f * k); close()
                }
                drawPath(fold, lerp(dark, Color.Black, 0.5f))
            }
            val r = androidx.compose.ui.geometry.CornerRadius(18f * k)
            drawRoundRect(Brush.verticalGradient(listOf(light, dark), startY = bandT, endY = bandB), Offset(bandL, bandT), Size(bandR - bandL, bandB - bandT), r)
            drawRoundRect(edge, Offset(bandL, bandT), Size(bandR - bandL, bandB - bandT), r, style = androidx.compose.ui.graphics.drawscope.Stroke(6f * k))
            drawLine(Color.White.copy(alpha = 0.45f), Offset(bandL + 22f * k, bandT + 14f * k), Offset(bandR - 22f * k, bandT + 14f * k), strokeWidth = 6f * k)
            val stitch = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f * k, 14f * k))
            val stitchColor = lerp(light, Color.White, 0.5f)
            drawLine(stitchColor, Offset(bandL + 26f * k, bandT + 26f * k), Offset(bandR - 26f * k, bandT + 26f * k), strokeWidth = 3f * k, pathEffect = stitch)
            drawLine(stitchColor, Offset(bandL + 26f * k, bandB - 16f * k), Offset(bandR - 26f * k, bandB - 16f * k), strokeWidth = 3f * k, pathEffect = stitch)
        }
        Row(
            Modifier.fillMaxWidth().padding(top = u * 8f, start = u * 104f, end = u * 104f).height(u * 88f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { i ->
                Image(
                    painterResource(R.drawable.mf_star), null,
                    modifier = Modifier.fillMaxHeight(0.52f).aspectRatio(1f).graphicsLayer { alpha = if (i < stars) 1f else 0.3f }
                )
            }
            Spacer(Modifier.width(u * 12f))
            RewardLettering(
                text = label,
                size = fs(46f),
                top = Color.White, bottom = Color(0xFFFFF0DC), outline = lerp(dark, Color.Black, 0.5f),
                modifier = Modifier.weight(1f, fill = false).fillMaxHeight()
            )
        }
    }
}

/** One extra as a card: the art on a cream-to-gold tile, its count on an orange tag, its name under it. */
@Composable
private fun RewardCard(extra: RewardExtra, u: Dp, tile: Float, fs: (Float) -> androidx.compose.ui.unit.TextUnit) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(u * 36f)
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .size(u * tile)
                .shadow(u * 10f, shape, ambientColor = Color(0xFF6E3C0A), spotColor = Color(0xFF6E3C0A))
                .background(Brush.verticalGradient(listOf(Color(0xFFFFF5CD), Color(0xFFFFD778))), shape)
                .border(u * 6f, Color(0xFF784610), shape)
                .padding(u * 9f)
                .border(u * 4f, Color(0xFFFFF5C8), androidx.compose.foundation.shape.RoundedCornerShape(u * 28f)),
            contentAlignment = Alignment.Center
        ) {
            when (extra) {
                is RewardExtra.Joker -> Image(painterResource(extra.type.artRes()), null, modifier = Modifier.fillMaxSize(0.84f))
                is RewardExtra.Pen -> com.sualtikasifi.cizimhafiza.presentation.common.PenSwoosh(extra.skin, Modifier.fillMaxWidth(0.86f).fillMaxHeight(0.5f))
            }
        }
        // the count tag, hanging off the lower right corner
        val tagW = tile * 0.5f; val tagH = tile * 0.24f
        Box(
            Modifier
                .offset(u * (tile - tagW + 16f), u * (tile - tagH + 14f))
                .size(u * tagW, u * tagH)
                .background(Color(0xFFFF781E), androidx.compose.foundation.shape.RoundedCornerShape(50))
                .border(u * 5f, Color(0xFF6E2800), androidx.compose.foundation.shape.RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
        ) {
            val n = when (extra) { is RewardExtra.Joker -> extra.count; is RewardExtra.Pen -> 1 }
            RewardLettering("×$n", fs(tagH * 0.62f), Color.White, Color(0xFFFFEBC8), Color(0xFF6E2800), Modifier.fillMaxSize(0.8f))
        }
        Text(
            text = when (extra) {
                is RewardExtra.Joker -> stringResource(extra.type.shortRes())
                is RewardExtra.Pen -> stringResource(extra.skin.labelRes)
            },
            style = com.sualtikasifi.cizimhafiza.presentation.common.PaintedStyle(color = Color(0xFF78410F), fontSize = fs(tile * 0.15f), textAlign = TextAlign.Center),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.offset(y = u * (tile + 18f)).width(u * (tile + 30f)).offset(x = u * -15f)
        )
    }
}

/** One coin, gem or crayon still falling from the video's chest, on its own slow loop. */
private class Loot(
    val x: Float,
    val start: Float,
    val speed: Float,
    val sway: Float,
    val spin: Float,
    val size: Float,
    val kind: Int,
    val color: Color
)

private fun fallingLoot(rnd: Random): List<Loot> {
    val gems = listOf(Color(0xFF46C878), Color(0xFFEB4664), Color(0xFF5096FF), Color(0xFFAA64F0))
    val crayons = listOf(Color(0xFFE63C3C), Color(0xFF3C78E6), Color(0xFFFAB41E), Color(0xFF965ADC))
    return List(34) {
        val kind = when (rnd.nextFloat()) { in 0f..0.5f -> 0; in 0.5f..0.8f -> 1; else -> 2 }
        Loot(
            x = rnd.nextFloat(),
            start = rnd.nextFloat(),
            speed = 0.8f + rnd.nextFloat() * 0.9f,
            sway = 0.01f + rnd.nextFloat() * 0.025f,
            spin = 1f + rnd.nextFloat() * 3f,
            size = 16f + rnd.nextFloat() * 12f,
            kind = kind,
            color = if (kind == 1) gems[rnd.nextInt(gems.size)] else crayons[rnd.nextInt(crayons.size)]
        )
    }
}

/** The white backdrop's warm light round the chest, the tier's slowly turning rays and a few soft light spots. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRewardBackdrop(center: Offset, unit: Float, accent: Color, light: Color, rayAngle: Float) {
    val warm = lerp(light, Color(0xFFFFECBE), 0.5f)
    drawCircle(Brush.radialGradient(listOf(warm.copy(alpha = 0.85f), warm.copy(alpha = 0.35f), Color.Transparent), center, 900f * unit), 900f * unit, center)
    val rayLen = size.maxDimension
    val rayColor = lerp(accent, Color(0xFFFFD26E), 0.45f)
    withTransform({ rotate(rayAngle, center) }) {
        for (i in 0 until 24) {
            val ang = (i * 15f) * (PI.toFloat() / 180f)
            val half = (PI.toFloat() / 24f) * 0.5f
            val path = Path().apply {
                moveTo(center.x, center.y)
                lineTo(center.x + cos(ang - half) * rayLen, center.y + sin(ang - half) * rayLen)
                lineTo(center.x + cos(ang + half) * rayLen, center.y + sin(ang + half) * rayLen)
                close()
            }
            drawPath(path, Brush.radialGradient(listOf(rayColor.copy(alpha = 0.30f), rayColor.copy(alpha = 0.12f), Color.Transparent), center, 1000f * unit))
        }
    }
    val spots = listOf(-0.36f to -0.42f, 0.38f to -0.30f, -0.42f to 0.12f, 0.40f to 0.22f, -0.20f to 0.46f, 0.26f to 0.52f, 0.05f to -0.55f)
    spots.forEachIndexed { i, (dx, dy) ->
        val c = Offset(size.width / 2f + dx * size.width, size.height / 2f + dy * size.height)
        val r = (26f + (i % 3) * 14f) * unit
        drawCircle(Brush.radialGradient(listOf(rayColor.copy(alpha = 0.32f), Color.Transparent), c, r), r, c)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFallingLoot(loot: List<Loot>, t: Float, unit: Float) {
    loot.forEach { p ->
        val phase = (p.start + t * p.speed) % 1f
        val y = (phase * 1.2f - 0.1f) * size.height
        val x = (p.x + sin((phase * p.spin + p.start) * 2f * PI.toFloat()) * p.sway) * size.width
        val s = p.size * unit
        val c = Offset(x, y)
        when (p.kind) {
            0 -> { // a coin turning over as it falls
                val squash = 0.35f + 0.65f * kotlin.math.abs(cos(phase * p.spin * 2f * PI.toFloat()))
                drawOval(Color(0xFFFFC428), Offset(x - s, y - s * squash), Size(s * 2f, s * 2f * squash))
                drawOval(Color(0xFFC88214), Offset(x - s, y - s * squash), Size(s * 2f, s * 2f * squash), style = androidx.compose.ui.graphics.drawscope.Stroke(3f * unit))
                drawOval(Color(0xFFFFE68C), Offset(x - s * 0.55f, y - s * 0.55f * squash), Size(s * 1.1f, s * 1.1f * squash))
            }
            1 -> { // a gem
                val gem = Path().apply { moveTo(x, y - s); lineTo(x + s * 0.8f, y - s * 0.2f); lineTo(x, y + s); lineTo(x - s * 0.8f, y - s * 0.2f); close() }
                drawPath(gem, p.color)
                drawPath(gem, Color.White, style = androidx.compose.ui.graphics.drawscope.Stroke(2.5f * unit))
            }
            else -> rotate(phase * 360f * p.spin, c) { // a crayon
                drawRoundRect(p.color, Offset(x - s * 1.1f, y - s * 0.28f), Size(s * 2.2f, s * 0.56f), androidx.compose.ui.geometry.CornerRadius(s * 0.2f))
            }
        }
    }
}

/** Four-point glints round the gold chest, each fading in and out on its own beat. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSparkles(t: Float, unit: Float) {
    val spots = listOf(Triple(0.13f, 0.18f, 24f), Triple(0.88f, 0.27f, 20f), Triple(0.80f, 0.82f, 14f), Triple(0.17f, 0.76f, 16f), Triple(0.54f, 0.04f, 18f))
    spots.forEachIndexed { i, (fx, fy, r) ->
        val a = (0.5f + 0.5f * sin((t + i * 0.21f) * 2f * PI.toFloat())).coerceIn(0f, 1f)
        val c = Offset(size.width * fx, size.height * fy)
        drawSparkle(c, r * unit * (0.7f + 0.3f * a), Color.White.copy(alpha = a))
        drawSparkle(c, r * unit * 0.5f * (0.7f + 0.3f * a), Color(0xFFFFE68C).copy(alpha = a))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSparkle(c: Offset, r: Float, color: Color) {
    val w = r * 0.22f
    drawPath(
        Path().apply {
            moveTo(c.x, c.y - r); lineTo(c.x + w, c.y - w); lineTo(c.x + r, c.y); lineTo(c.x + w, c.y + w)
            lineTo(c.x, c.y + r); lineTo(c.x - w, c.y + w); lineTo(c.x - r, c.y); lineTo(c.x - w, c.y - w); close()
        },
        color
    )
}
