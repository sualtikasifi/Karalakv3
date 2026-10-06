package com.sualtikasifi.cizimhafiza.presentation.mainmenu

import com.sualtikasifi.cizimhafiza.presentation.common.cachedPainterResource
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import com.sualtikasifi.cizimhafiza.presentation.common.breathing
import com.sualtikasifi.cizimhafiza.presentation.common.glint
import com.sualtikasifi.cizimhafiza.presentation.common.pressable
import com.sualtikasifi.cizimhafiza.presentation.common.sceneIn
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.AvatarFrame
import com.sualtikasifi.cizimhafiza.domain.model.DailyChallenge
import com.sualtikasifi.cizimhafiza.domain.model.LevelProgressState
import com.sualtikasifi.cizimhafiza.domain.model.PenSkin
import com.sualtikasifi.cizimhafiza.presentation.common.FitText
import com.sualtikasifi.cizimhafiza.presentation.common.LetteredText
import com.sualtikasifi.cizimhafiza.presentation.common.LevelAvatar
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedStyle
import com.sualtikasifi.cizimhafiza.presentation.common.a11yButton
import com.sualtikasifi.cizimhafiza.presentation.common.nameRes
import com.sualtikasifi.cizimhafiza.domain.model.XpAwards
import java.text.NumberFormat
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.State
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

private const val ArtW = 841f
private const val ArtH = 1870f
private val HomeInk = Color(0xFF2B1A10)
/** Secondary lettering on the parchment: dark enough to read on the painted grain (the old #6B5446 washed out). */
private val HomeInkSoft = Color(0xFF4A3426)

private val GoldSparkles = listOf(
    com.sualtikasifi.cizimhafiza.presentation.common.SparkleSpot(0.20f, 0.18f, 0.075f),
    com.sualtikasifi.cizimhafiza.presentation.common.SparkleSpot(0.82f, 0.26f, 0.06f),
    com.sualtikasifi.cizimhafiza.presentation.common.SparkleSpot(0.12f, 0.62f, 0.05f),
    com.sualtikasifi.cizimhafiza.presentation.common.SparkleSpot(0.88f, 0.70f, 0.07f),
    com.sualtikasifi.cizimhafiza.presentation.common.SparkleSpot(0.52f, 0.06f, 0.055f)
)
private val ChestSparkles = listOf(
    com.sualtikasifi.cizimhafiza.presentation.common.SparkleSpot(0.16f, 0.22f, 0.075f),
    com.sualtikasifi.cizimhafiza.presentation.common.SparkleSpot(0.86f, 0.18f, 0.06f),
    com.sualtikasifi.cizimhafiza.presentation.common.SparkleSpot(0.90f, 0.72f, 0.05f),
    com.sualtikasifi.cizimhafiza.presentation.common.SparkleSpot(0.10f, 0.76f, 0.06f),
    com.sualtikasifi.cizimhafiza.presentation.common.SparkleSpot(0.50f, 0.04f, 0.055f)
)

/** Lettering size of the "Reklam izle" pill on both ad cards, so the two read as one button. */
private const val AdPillText = 27f

/**
 * The home screen as ONE painted scene (bg_home_scene): the picture carries every frame, tile and panel; this places the
 * live numbers, names and states over it. Positions are in picture units (841 x 1870): x scales with the width of the
 * screen and y with its height, so on any phone the writing sits exactly on its painted spot.
 */
@Composable
internal fun PaintedHome(
    nickname: String,
    progress: LevelProgressState,
    frame: AvatarFrame,
    photo: com.sualtikasifi.cizimhafiza.presentation.common.AvatarPhoto,
    pen: PenSkin,
    gold: Int,
    xpEvent: com.sualtikasifi.cizimhafiza.domain.repository.XpEvent?,
    dailyState: com.sualtikasifi.cizimhafiza.util.DailyChallengeState,
    adGoldNextAt: Long,
    adChestAvailable: Boolean,
    pendingFriendRequests: Int,
    hasUnseenAchievement: Boolean,
    accountNotLinked: Boolean,
    onFrameClick: () -> Unit,
    onPenClick: () -> Unit,
    onRankClick: () -> Unit,
    onGoldClick: () -> Unit,
    onWatchGold: () -> Unit,
    onWatchChest: () -> Unit,
    onDaily: () -> Unit,
    onPlayOnline: () -> Unit,
    onQuickMatch: () -> Unit,
    onPlay: () -> Unit,
    onLevels: () -> Unit,
    onFriends: () -> Unit,
    onAchievements: () -> Unit,
    onLeague: () -> Unit,
    onStore: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize().sceneIn()) {
        // ONE scale for both directions, so the picture is never stretched: on a phone shaped like it the picture fills
        // the screen exactly; on a wider or taller one it is centred and the spare strips show a blurred copy of it.
        val unit = minOf(maxWidth / ArtW, maxHeight / ArtH)
        val offX = (maxWidth - unit * ArtW) / 2
        val offY = (maxHeight - unit * ArtH) / 2
        val us = unit.value
        // Lettering on the scene follows the picture, not the system font-size setting (see FitLettered).
        val fontScale0 = LocalDensity.current.fontScale
        fun fs(art: Float) = (art * us / fontScale0).sp
        fun box(x0: Float, y0: Float, x1: Float, y1: Float): Modifier =
            Modifier.offset(offX + unit * x0, offY + unit * y0).size(unit * (x1 - x0), unit * (y1 - y0))
        val noRipple = remember { MutableInteractionSource() }

        @Composable
        fun FitLettered(text: String, baseArt: Float, outline: Color?, maxLines: Int, modifier: Modifier, fill: Color = Color.White) {
            // The lettering measures itself against the box it is laid in and shrinks to fit (see LetteredText).
            LetteredText(text, fs(baseArt), fill = fill, outline = outline, maxLines = maxLines, modifier = modifier, minScale = 0.62f)
        }

        val scene = cachedPainterResource(R.drawable.bg_home_scene)
        // The same picture, as pixels, for the press "sink" of the areas painted into it (see sunkenArt).
        val sceneContext = androidx.compose.ui.platform.LocalContext.current
        val sceneBitmap = remember {
            runCatching { com.sualtikasifi.cizimhafiza.presentation.common.BackdropCache.get(sceneContext.resources, R.drawable.bg_home_scene) }.getOrNull()
        }
        val pixelsPerArtX = (sceneBitmap?.width ?: 1) / ArtW
        val pixelsPerArtY = (sceneBitmap?.height ?: 1) / ArtH
        fun sunk(sink: SinkState, x0: Float, y0: Float, x1: Float, y1: Float, cornerArt: Float): Modifier =
            Modifier.sunkenArt(sink, sceneBitmap, x0 * pixelsPerArtX, y0 * pixelsPerArtY, (x1 - x0) * pixelsPerArtX, (y1 - y0) * pixelsPerArtY, (cornerArt * us).dp)
        /** A live piece (label, pill, number) laid in [x0]..[y1] over an area whose centre is ([cx],[cy]) dips with it. */
        fun follow(sink: SinkState, cx: Float, cy: Float, x0: Float, y0: Float, x1: Float, y1: Float): Modifier =
            Modifier.sinkWith(sink, (cx - x0) / (x1 - x0), (cy - y0) / (y1 - y0))
        if (offX > 1.dp || offY > 1.dp) {
            Image(scene, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().blur(20.dp))
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.18f)))
        }
        Image(scene, contentDescription = null, contentScale = ContentScale.FillBounds, modifier = box(0f, 0f, ArtW, ArtH))

        @Composable
        fun AdPill(rect: Modifier, text: String) {
            Row(
                rect
                    .background(Brush.verticalGradient(listOf(Color(0xFFFFF3D6), Color(0xFFFFD98A))), RoundedCornerShape(50))
                    .border(2.dp, Color(0xFFE08A1B), RoundedCornerShape(50))
                    .padding(horizontal = (6f * us).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size((26f * us).dp).background(Color(0xFFF58A1F), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size((20f * us).dp))
                }
                FitText(
                    text = text,
                    style = PaintedStyle(color = HomeInk, fontSize = fs(AdPillText), textAlign = TextAlign.Center),
                    minScale = 0.6f,
                    modifier = Modifier.weight(1f).padding(horizontal = (4f * us).dp)
                )
            }
        }

        // ── Top chips ──
        val goldChipSink = rememberSink(0.94f)
        Box(
            box(40f, 258f, 256f, 326f)
                .then(sunk(goldChipSink, 40f, 258f, 256f, 326f, 34f))
                .clickable(interactionSource = goldChipSink.source, indication = null, onClick = onGoldClick)
        ) {}
        Box(box(112f, 270f, 238f, 314f).then(follow(goldChipSink, 148f, 292f, 112f, 270f, 238f, 314f)), contentAlignment = Alignment.Center) {
            // Counts to the new total whenever gold changes (a reward, a purchase) instead of jumping.
            val goldShown by animateIntAsState(gold, tween(800, easing = FastOutSlowInEasing), label = "gold")
            LetteredText(NumberFormat.getIntegerInstance().format(goldShown), fs(31f), outline = Color(0xFF3A1E08), maxLines = 1)
        }
        val penChipSink = rememberSink(0.94f)
        Box(
            box(264f, 258f, 512f, 326f)
                .then(sunk(penChipSink, 264f, 258f, 512f, 326f, 34f))
                .clickable(interactionSource = penChipSink.source, indication = null, onClick = onPenClick)
                .a11yButton(stringResource(pen.labelRes))
        ) {}
        Box(
            box(282f, 262f, 344f, 322f)
                .then(follow(penChipSink, 388f, 292f, 282f, 262f, 344f, 322f))
                .padding(2.dp)
                .background(Brush.verticalGradient(listOf(Color(0xFFFFD66B), Color(0xFFF0A21A))), CircleShape)
                .border(2.dp, Color(0xFF8A4E12), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Create, contentDescription = null, tint = Color(0xFF5A2E0A), modifier = Modifier.size((30f * us).dp))
        }
        FitLettered(stringResource(pen.labelRes), 27f, Color(0xFF3A1E08), 1, box(350f, 266f, 484f, 320f).then(follow(penChipSink, 388f, 292f, 350f, 266f, 484f, 320f)))

        // ── Profile ──
        Box(
            box(66f, 358f, 204f, 500f).clickable(interactionSource = noRipple, indication = null, onClick = onFrameClick)
                .a11yButton(stringResource(R.string.avatar_frame_change_cd)),
            contentAlignment = Alignment.Center
        ) {
            LevelAvatar(level = progress.level, frame = frame, photo = photo, size = (132f * us).dp)
            // A small pencil badge on the photo's top-right corner: the picture is a button, and this says what for.
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (4f * us).dp, y = (-2f * us).dp)
                    .size((38f * us).dp)
                    .breathing(0.08f, 1300)
                    .background(Brush.verticalGradient(listOf(Color(0xFFFFB03D), Color(0xFFF26A1B))), CircleShape)
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Create, contentDescription = null, tint = Color.White, modifier = Modifier.size((22f * us).dp))
            }
        }
        Box(box(224f, 350f, 780f, 394f), contentAlignment = Alignment.CenterStart) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = nickname,
                    style = PaintedStyle(color = HomeInk, fontSize = fs(36f), textAlign = TextAlign.Start),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = "• " + stringResource(R.string.home_level_inline, progress.level),
                    style = PaintedStyle(color = HomeInkSoft, fontSize = fs(26f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Start),
                    maxLines = 1
                )
            }
        }
        Box(
            box(222f, 400f, 488f, 447f).clickable(interactionSource = noRipple, indication = null, onClick = onRankClick),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier
                    .background(Brush.horizontalGradient(listOf(Color(0xFF8A62F0), Color(0xFF5B3FC4))), RoundedCornerShape(50))
                    .padding(horizontal = (18f * us).dp, vertical = (4f * us).dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = progress.tier.rank.emoji, fontSize = fs(28f))
                FitText(
                    text = stringResource(progress.tier.rank.nameRes),
                    style = PaintedStyle(color = Color.White, fontSize = fs(31f), textAlign = TextAlign.Start),
                    minScale = 0.7f,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }
        FitText(
            text = if (progress.isMaxLevel) "MAX" else stringResource(R.string.home_xp_to_next, progress.xpToNextLevel, progress.level + 1),
            style = PaintedStyle(color = HomeInkSoft, fontSize = fs(29f), fontWeight = FontWeight.Bold, textAlign = TextAlign.End),
            contentAlignment = Alignment.CenterEnd,
            modifier = box(496f, 406f, 780f, 446f)
        )
        Box(box(222f, 452f, 772f, 492f)) {
            XpBar(
                fraction = progress.progressFraction,
                label = stringResource(R.string.home_xp_format, progress.xpIntoLevel, progress.xpForThisLevel),
                modifier = Modifier.fillMaxSize()
            )
        }

        // ── Cards ──
        val tintMatrix = remember {
            // Rotates the hue of the green card towards purple (the "ready to play" colour).
            val a = Math.toRadians(135.0)
            val c = Math.cos(a).toFloat(); val s = Math.sin(a).toFloat()
            ColorMatrix(floatArrayOf(
                0.213f + c * 0.787f - s * 0.213f, 0.715f - c * 0.715f - s * 0.715f, 0.072f - c * 0.072f + s * 0.928f, 0f, 0f,
                0.213f - c * 0.213f + s * 0.143f, 0.715f + c * 0.285f + s * 0.140f, 0.072f - c * 0.072f - s * 0.283f, 0f, 0f,
                0.213f - c * 0.213f - s * 0.787f, 0.715f - c * 0.715f + s * 0.715f, 0.072f + c * 0.928f + s * 0.072f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        val available = dailyState.isAvailableToday
        if (available) {
            Image(
                painter = painterResource(R.drawable.home_daily_card),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                colorFilter = ColorFilter.colorMatrix(tintMatrix),
                modifier = box(236f, 538f, 608f, 756f)
            )
        }
        val flags = dailyState.todayResult?.correctFlags.orEmpty()
        val multiplier = XpAwards.dailyStreakMultiplier(dailyState.streakIfCompletedToday)
        val cardInk = if (available) Color(0xFF2E1A66) else Color(0xFF0B4F2A)
        FitLettered(stringResource(R.string.daily_challenge_title), 30f, cardInk, 2, box(384f, 546f, 598f, 626f))
        Box(
            box(256f, 630f, 360f, 676f)
                .background(Brush.verticalGradient(listOf(Color(0xFFFFE9B0), Color(0xFFFFC04A))), RoundedCornerShape(50))
                .border(2.dp, Color(0xFFE08A1B), RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
        ) {
            FitLettered("🔥 ${multiplier}x", 30f, null, 1, Modifier.fillMaxSize().padding(horizontal = 4.dp), fill = Color(0xFF8A3A00))
        }
        Box(box(394f, 628f, 594f, 676f), contentAlignment = Alignment.Center) {
            com.sualtikasifi.cizimhafiza.presentation.common.DailyPips(
                flags = flags,
                count = DailyChallenge.WORD_COUNT,
                size = (33f * us).dp,
                gap = (7f * us).dp
            )
        }
        if (available) {
            Box(
                box(264f, 688f, 596f, 744f)
                    .breathing(0.03f, 1100)
                    .background(Brush.verticalGradient(listOf(Color(0xFFFFE27A), Color(0xFFFFB300))), RoundedCornerShape(50))
                    .border(2.dp, Color(0xFFB36B00), RoundedCornerShape(50))
                    .glint(2400, 0.55f, 40.dp)
                    .pressable(pressedScale = 0.95f, onClick = onDaily),
                contentAlignment = Alignment.Center
            ) {
                FitText(
                    text = stringResource(R.string.daily_play_now).uppercase(androidx.compose.ui.text.intl.Locale.current.platformLocale) + " ▸",
                    style = PaintedStyle(color = Color(0xFF4A2600), fontSize = fs(34f), textAlign = TextAlign.Center),
                    modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp)
                )
            }
        } else {
            // The countdown to tomorrow sits where the picture leaves room at the card's lower left.
            FitText(
                text = midnightCountdownText(),
                style = PaintedStyle(
                    color = Color.White, fontSize = fs(29f), textAlign = TextAlign.Center,
                    shadow = androidx.compose.ui.graphics.Shadow(Color(0x88000000), androidx.compose.ui.geometry.Offset(0f, 2f), 3f)
                ),
                minScale = 0.6f,
                modifier = box(262f, 692f, 404f, 740f)
            )
            Row(
                box(410f, 690f, 598f, 740f)
                    .background(Brush.verticalGradient(listOf(Color(0xFF5BD070), Color(0xFF2EA043))), RoundedCornerShape(50))
                    .border(2.dp, Color(0xFFB8F5CF), RoundedCornerShape(50))
                    .padding(horizontal = (12f * us).dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size((28f * us).dp))
                Spacer(Modifier.size((4f * us).dp))
                LetteredText(
                    stringResource(R.string.daily_done_badge), fs(28f), outline = Color(0xFF14602A), maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }

        // Ad gold (left)
        val goldNow = rememberNowUntil(adGoldNextAt)
        val goldRemaining = adGoldNextAt - goldNow
        val goldReady = goldRemaining <= 0
        val goldCardSink = rememberSink(0.95f)
        Box(
            box(34f, 538f, 230f, 754f)
                .then(sunk(goldCardSink, 34f, 538f, 230f, 754f, 26f))
                .clickable(enabled = goldReady, interactionSource = goldCardSink.source, indication = null, onClick = onWatchGold)
        ) {}
        FitLettered(stringResource(R.string.home_ad_gold_label), 36f, Color(0xFF8A4E12), 1, box(40f, 648f, 226f, 698f).then(follow(goldCardSink, 132f, 646f, 40f, 648f, 226f, 698f)))
        // The coins are painted into the scene, so they cannot move on their own: glitter and a passing glint over them
        // do the calling instead, while there is a reward to take.
        com.sualtikasifi.cizimhafiza.presentation.common.SparkleField(
            spots = GoldSparkles,
            active = goldReady,
            modifier = box(48f, 548f, 232f, 672f)
        )
        if (goldReady) {
            Box(box(70f, 566f, 214f, 650f).glint(periodMs = 2600, strength = 0.38f, corner = 40.dp))
        }
        AdPill(box(64f, 702f, 200f, 740f).then(follow(goldCardSink, 132f, 646f, 64f, 702f, 200f, 740f)), if (goldReady) stringResource(R.string.home_ad_watch) else "⏳ " + hms(goldRemaining / 1000))

        // Free chest ad (right)
        val midnight = remember(adChestAvailable) { com.sualtikasifi.cizimhafiza.util.TurkeyTime.nextMidnightMillis() }
        val chestNow = rememberNowUntil(if (adChestAvailable) 0L else midnight)
        val chestRemaining = midnight - chestNow
        val chestCardSink = rememberSink(0.95f)
        Box(
            box(616f, 538f, 808f, 754f)
                .then(sunk(chestCardSink, 616f, 538f, 808f, 754f, 26f))
                .clickable(enabled = adChestAvailable, interactionSource = chestCardSink.source, indication = null, onClick = onWatchChest)
        ) {}
        FitLettered(stringResource(R.string.home_ad_chest_label), 27f, Color(0xFF14549A), 2, box(634f, 640f, 796f, 706f).then(follow(chestCardSink, 712f, 646f, 634f, 640f, 796f, 706f)))
        // Same for the chest: it cannot move, so it glitters and catches the light while a free one is waiting.
        com.sualtikasifi.cizimhafiza.presentation.common.SparkleField(
            spots = ChestSparkles,
            active = adChestAvailable,
            color = Color(0xFFCFF0FF),
            modifier = box(626f, 544f, 800f, 650f)
        )
        if (adChestAvailable) {
            Box(box(660f, 560f, 776f, 640f).glint(periodMs = 2900, strength = 0.34f, corner = 30.dp))
        }
        // The very same pill as the gold card's: both are drawn here (the picture leaves them empty), same size, same
        // colours, same row, each centred in its card with the same gap to the card's frame.
        AdPill(box(646f, 702f, 782f, 740f).then(follow(chestCardSink, 712f, 646f, 646f, 702f, 782f, 740f)), if (adChestAvailable) stringResource(R.string.home_ad_watch) else "⏳ " + hms(chestRemaining / 1000))

        // ── Tiles ──
        @Composable
        fun Tile(x0: Float, y0: Float, x1: Float, y1: Float, lx0: Float, ly0: Float, lx1: Float, ly1: Float, label: String, onClick: () -> Unit, content: @Composable () -> Unit = {}) {
            val sink = rememberSink(0.92f)
            Box(
                box(x0, y0, x1, y1)
                    .then(sunk(sink, x0, y0, x1, y1, 26f))
                    .clickable(interactionSource = sink.source, indication = null, onClick = onClick)
                    .a11yButton(label)
            ) {}
            FitLettered(label, 31f, Color(0xFF241408), 2, box(lx0, ly0, lx1, ly1).then(follow(sink, (x0 + x1) / 2f, (y0 + y1) / 2f, lx0, ly0, lx1, ly1)))
            content()
        }
        Tile(30f, 776f, 288f, 1012f, 50f, 918f, 274f, 1002f, stringResource(R.string.menu_play_online), onPlayOnline)
        Tile(294f, 776f, 552f, 1012f, 330f, 940f, 520f, 986f, stringResource(R.string.quick_match_title), onQuickMatch) {
            val boosted = xpEvent != null && xpEvent.endsAtMillis > System.currentTimeMillis()
            if (boosted && xpEvent != null) BoostBadge(xpEvent.multiplier, box(306f, 788f, 420f, 826f))
        }
        Tile(558f, 776f, 816f, 1012f, 578f, 918f, 798f, 1002f, stringResource(R.string.menu_play), onPlay)
        Tile(30f, 1018f, 288f, 1204f, 50f, 1142f, 268f, 1188f, stringResource(R.string.menu_levels), onLevels)
        Tile(294f, 1018f, 552f, 1204f, 320f, 1142f, 526f, 1188f, stringResource(R.string.menu_friends), onFriends) {
            if (pendingFriendRequests > 0) CornerCount(box(484f, 1024f, 548f, 1072f), pendingFriendRequests)
        }
        Tile(558f, 1018f, 816f, 1204f, 584f, 1142f, 792f, 1188f, stringResource(R.string.menu_achievements), onAchievements) {
            if (hasUnseenAchievement) CornerDot(box(764f, 1028f, 804f, 1068f))
        }
        Tile(30f, 1210f, 288f, 1398f, 50f, 1332f, 268f, 1380f, stringResource(R.string.league_title), onLeague)
        Tile(294f, 1210f, 552f, 1398f, 340f, 1332f, 510f, 1380f, stringResource(R.string.store_title), onStore)
        Tile(558f, 1210f, 816f, 1398f, 590f, 1332f, 786f, 1380f, stringResource(R.string.menu_settings), onSettings) {
            if (accountNotLinked) CornerDot(box(766f, 1218f, 806f, 1258f))
        }

        // ── Chests ──
        HomeChestsPainted(box = { a, b, c, d -> box(a, b, c, d) }, fs = { fs(it) })

        // ── A running XP event, floated along the top ──
        xpEvent?.let { event ->
            Box(Modifier.statusBarsPadding().padding(horizontal = 16.dp).padding(top = 2.dp)) { XpEventBanner(event = event) }
        }
    }
}

@Composable
private fun CornerDot(modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(Modifier.size(14.dp).background(MaterialTheme.colorScheme.error, CircleShape).border(2.dp, Color.White, CircleShape))
    }
}

@Composable
private fun CornerCount(modifier: Modifier, count: Int) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(Modifier.background(MaterialTheme.colorScheme.error, CircleShape).border(2.dp, Color.White, CircleShape).padding(horizontal = 7.dp, vertical = 1.dp)) {
            Text(text = count.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onError)
        }
    }
}



/** Press state of an area painted into the scene: [scale] dips to [depth] while a finger is down and springs back. */
private class SinkState(val source: MutableInteractionSource, val scale: State<Float>)

@Composable
private fun rememberSink(depth: Float = 0.9f): SinkState {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale = animateFloatAsState(
        targetValue = if (pressed) depth else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "sink"
    )
    return SinkState(source, scale)
}

/**
 * The press "sink" for something that is PAINTED into the scene picture and so cannot scale itself: while pressed, the
 * area's own pixels are drawn again, shrunk, over a darker recess that hides the full-size original — the button
 * visibly caves in, like the room buttons do. [srcLeft]..[srcH] are the area's rectangle in the picture's own pixels.
 */
private fun Modifier.sunkenArt(
    sink: SinkState,
    scene: ImageBitmap?,
    srcLeft: Float, srcTop: Float, srcW: Float, srcH: Float,
    corner: Dp
): Modifier = drawWithContent {
    val k = sink.scale.value
    if (scene != null && k < 0.9995f) {
        val r = corner.toPx()
        val grow = 3.dp.toPx()
        val recessSize = Size(size.width + grow * 2, size.height + grow * 2)
        drawRoundRect(Color(0xFF6B2D0C), Offset(-grow, -grow), recessSize, CornerRadius(r + grow))
        drawRoundRect(
            Brush.verticalGradient(listOf(Color(0x77000000), Color.Transparent), startY = -grow, endY = size.height * 0.35f),
            Offset(-grow, -grow), recessSize, CornerRadius(r + grow)
        )
        scale(k, k, pivot = center) {
            clipPath(Path().apply { addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(r))) }) {
                drawImage(
                    scene,
                    srcOffset = IntOffset(srcLeft.roundToInt(), srcTop.roundToInt()),
                    srcSize = IntSize(srcW.roundToInt(), srcH.roundToInt()),
                    dstOffset = IntOffset.Zero,
                    dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                    filterQuality = FilterQuality.High
                )
            }
        }
    }
    drawContent()
}

/** Makes a live piece laid over a painted area (its label, its pill) dip together with it, about the area's centre. */
private fun Modifier.sinkWith(sink: SinkState, pivotX: Float, pivotY: Float): Modifier = graphicsLayer {
    val k = sink.scale.value
    scaleX = k
    scaleY = k
    transformOrigin = TransformOrigin(pivotX, pivotY)
}
