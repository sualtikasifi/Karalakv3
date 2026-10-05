package com.sualtikasifi.cizimhafiza.presentation.mainmenu

import androidx.compose.foundation.Image
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
import com.sualtikasifi.cizimhafiza.presentation.common.pressFlash
import com.sualtikasifi.cizimhafiza.presentation.common.pressable
import com.sualtikasifi.cizimhafiza.presentation.common.sceneIn
import androidx.compose.ui.draw.blur
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

private const val ArtW = 841f
private const val ArtH = 1870f
private val HomeInk = Color(0xFF2B1A10)
/** Secondary lettering on the parchment: dark enough to read on the painted grain (the old #6B5446 washed out). */
private val HomeInkSoft = Color(0xFF4A3426)

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

        val scene = painterResource(R.drawable.bg_home_scene)
        if (offX > 1.dp || offY > 1.dp) {
            Image(scene, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().blur(20.dp))
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.18f)))
        }
        Image(scene, contentDescription = null, contentScale = ContentScale.FillBounds, modifier = box(0f, 0f, ArtW, ArtH))

        // ── Top chips ──
        Box(box(40f, 258f, 256f, 326f).clickable(interactionSource = noRipple, indication = null, onClick = onGoldClick)) {}
        Box(box(116f, 270f, 198f, 314f), contentAlignment = Alignment.Center) {
            // Counts to the new total whenever gold changes (a reward, a purchase) instead of jumping.
            val goldShown by animateIntAsState(gold, tween(800, easing = FastOutSlowInEasing), label = "gold")
            LetteredText(NumberFormat.getIntegerInstance().format(goldShown), fs(31f), outline = Color(0xFF3A1E08), maxLines = 1)
        }
        Box(box(264f, 258f, 512f, 326f).clickable(interactionSource = noRipple, indication = null, onClick = onPenClick).a11yButton(stringResource(pen.labelRes))) {}
        Box(
            box(282f, 262f, 344f, 322f)
                .padding(2.dp)
                .background(Brush.verticalGradient(listOf(Color(0xFFFFD66B), Color(0xFFF0A21A))), CircleShape)
                .border(2.dp, Color(0xFF8A4E12), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Create, contentDescription = null, tint = Color(0xFF5A2E0A), modifier = Modifier.size((30f * us).dp))
        }
        FitLettered(stringResource(pen.labelRes), 27f, Color(0xFF3A1E08), 1, box(350f, 266f, 484f, 320f))

        // ── Profile ──
        Box(
            box(66f, 358f, 204f, 500f).clickable(interactionSource = noRipple, indication = null, onClick = onFrameClick)
                .a11yButton(stringResource(R.string.avatar_frame_change_cd)),
            contentAlignment = Alignment.Center
        ) {
            LevelAvatar(level = progress.level, frame = frame, photo = photo, size = (132f * us).dp)
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
        Box(box(34f, 538f, 244f, 754f).clickable(enabled = goldReady, interactionSource = noRipple, indication = null, onClick = onWatchGold)) {}
        FitLettered(stringResource(R.string.home_ad_gold_label), 36f, Color(0xFF8A4E12), 1, box(40f, 648f, 238f, 698f))
        FitText(
            text = if (goldReady) stringResource(R.string.home_ad_watch) else "⏳ " + hms(goldRemaining / 1000),
            style = PaintedStyle(color = HomeInk, fontSize = fs(27f), textAlign = TextAlign.Center),
            minScale = 0.6f,
            modifier = box(94f, 702f, 230f, 742f)
        )

        // Free chest ad (right)
        val midnight = remember(adChestAvailable) { com.sualtikasifi.cizimhafiza.util.TurkeyTime.nextMidnightMillis() }
        val chestNow = rememberNowUntil(if (adChestAvailable) 0L else midnight)
        val chestRemaining = midnight - chestNow
        Box(box(616f, 538f, 808f, 754f).clickable(enabled = adChestAvailable, interactionSource = noRipple, indication = null, onClick = onWatchChest)) {}
        FitLettered(stringResource(R.string.home_ad_chest_label), 27f, Color(0xFF14549A), 2, box(634f, 640f, 796f, 706f))
        Box(
            box(654f, 708f, 792f, 746f)
                .background(Color(0xCC183A6E), RoundedCornerShape(50))
                .border(1.5.dp, Color(0x669CC4FF), RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (adChestAvailable) stringResource(R.string.home_ad_watch) else "⏳ " + hms(chestRemaining / 1000),
                style = PaintedStyle(color = Color.White, fontSize = fs(28f), textAlign = TextAlign.Center),
                maxLines = 1
            )
        }

        // ── Tiles ──
        @Composable
        fun Tile(x0: Float, y0: Float, x1: Float, y1: Float, lx0: Float, ly0: Float, lx1: Float, ly1: Float, label: String, onClick: () -> Unit, content: @Composable () -> Unit = {}) {
            Box(box(x0, y0, x1, y1).pressFlash(corner = 24.dp, onClick = onClick).a11yButton(label)) {}
            FitLettered(label, 31f, Color(0xFF241408), 2, box(lx0, ly0, lx1, ly1))
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

