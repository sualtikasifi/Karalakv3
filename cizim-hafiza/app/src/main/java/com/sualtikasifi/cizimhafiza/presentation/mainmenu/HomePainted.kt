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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
private val InkSoft2 = Color(0xFF6B5446)

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
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val ux = maxWidth / ArtW
        val uy = maxHeight / ArtH
        val us = minOf(ux.value, uy.value)
        fun fs(art: Float) = (art * us).sp
        fun box(x0: Float, y0: Float, x1: Float, y1: Float): Modifier =
            Modifier.offset(ux * x0, uy * y0).size(ux * (x1 - x0), uy * (y1 - y0))
        val noRipple = remember { MutableInteractionSource() }

        Image(
            painter = painterResource(R.drawable.bg_home_scene),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )

        // ── Top chips ──
        Box(box(40f, 258f, 256f, 326f).clickable(interactionSource = noRipple, indication = null, onClick = onGoldClick)) {}
        Box(box(116f, 270f, 198f, 314f), contentAlignment = Alignment.Center) {
            LetteredText(NumberFormat.getIntegerInstance().format(gold), fs(31f), outline = Color(0xFF3A1E08), maxLines = 1)
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
        Box(box(350f, 266f, 462f, 320f), contentAlignment = Alignment.Center) {
            LetteredText(stringResource(pen.labelRes), fs(27f), outline = Color(0xFF3A1E08), maxLines = 2)
        }

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
                    style = PaintedStyle(color = InkSoft2, fontSize = fs(26f), fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Start),
                    maxLines = 1
                )
            }
        }
        Box(
            box(222f, 400f, 380f, 447f).clickable(interactionSource = noRipple, indication = null, onClick = onRankClick),
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
                Text(
                    text = stringResource(progress.tier.rank.nameRes),
                    style = PaintedStyle(color = Color.White, fontSize = fs(32f), textAlign = TextAlign.Center),
                    maxLines = 1
                )
            }
        }
        Box(box(430f, 410f, 780f, 446f), contentAlignment = Alignment.CenterEnd) {
            Text(
                text = if (progress.isMaxLevel) "MAX" else stringResource(R.string.home_xp_to_next, progress.xpToNextLevel, progress.level + 1),
                style = PaintedStyle(color = InkSoft2, fontSize = fs(30f), fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End),
                maxLines = 1
            )
        }
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
        Box(box(388f, 548f, 594f, 624f), contentAlignment = Alignment.Center) {
            LetteredText(stringResource(R.string.daily_challenge_title), fs(33f), outline = cardInk, maxLines = 2)
        }
        Box(box(256f, 628f, 360f, 678f), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .background(Brush.verticalGradient(listOf(Color(0xFFFFE9B0), Color(0xFFFFC04A))), RoundedCornerShape(50))
                    .border(2.dp, Color(0xFFE08A1B), RoundedCornerShape(50))
                    .padding(horizontal = (14f * us).dp, vertical = (3f * us).dp)
            ) {
                Text("🔥 ${multiplier}x", style = PaintedStyle(color = Color(0xFF8A3A00), fontSize = fs(32f), textAlign = TextAlign.Center), maxLines = 1)
            }
        }
        Box(box(394f, 628f, 594f, 676f), contentAlignment = Alignment.Center) {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                repeat(DailyChallenge.WORD_COUNT) { index ->
                    val flag = flags.getOrNull(index)
                    Box(
                        modifier = Modifier
                            .size((38f * us).dp)
                            .background(
                                when (flag) {
                                    true -> Color(0xFF2EA043)
                                    false -> Color(0xFFE53935)
                                    null -> Color.White.copy(alpha = 0.2f)
                                },
                                CircleShape
                            )
                            .border(2.dp, Color.White.copy(alpha = 0.9f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        when (flag) {
                            true -> Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size((26f * us).dp))
                            false -> Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size((26f * us).dp))
                            null -> Unit
                        }
                    }
                }
            }
        }
        if (available) {
            Box(
                box(264f, 688f, 596f, 744f)
                    .background(Brush.verticalGradient(listOf(Color(0xFFFFE27A), Color(0xFFFFB300))), RoundedCornerShape(50))
                    .border(2.dp, Color(0xFFB36B00), RoundedCornerShape(50))
                    .clickable(interactionSource = noRipple, indication = null, onClick = onDaily),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.daily_play_now).uppercase(androidx.compose.ui.text.intl.Locale.current.platformLocale) + " ▸",
                    style = PaintedStyle(color = Color(0xFF4A2600), fontSize = fs(34f), textAlign = TextAlign.Center),
                    maxLines = 1
                )
            }
        } else {
            Box(box(294f, 696f, 396f, 736f), contentAlignment = Alignment.CenterStart) {
                Text(
                    text = midnightCountdownText(),
                    style = PaintedStyle(color = Color.White, fontSize = fs(29f), textAlign = TextAlign.Start),
                    maxLines = 1
                )
            }
            Box(box(404f, 688f, 598f, 742f), contentAlignment = Alignment.Center) {
                Row(
                    modifier = Modifier
                        .background(Brush.verticalGradient(listOf(Color(0xFF5BD070), Color(0xFF2EA043))), RoundedCornerShape(50))
                        .border(2.dp, Color(0xFFB8F5CF), RoundedCornerShape(50))
                        .padding(horizontal = (18f * us).dp, vertical = (6f * us).dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size((30f * us).dp))
                    LetteredText(stringResource(R.string.daily_done_badge), fs(29f), outline = Color(0xFF14602A), maxLines = 1)
                }
            }
        }

        // Ad gold (left)
        val goldNow = rememberNowUntil(adGoldNextAt)
        val goldRemaining = adGoldNextAt - goldNow
        val goldReady = goldRemaining <= 0
        Box(box(34f, 538f, 244f, 754f).clickable(enabled = goldReady, interactionSource = noRipple, indication = null, onClick = onWatchGold)) {}
        Box(box(46f, 648f, 232f, 698f), contentAlignment = Alignment.Center) {
            LetteredText(stringResource(R.string.home_ad_gold_label), fs(36f), outline = Color(0xFF8A4E12), maxLines = 1)
        }
        Box(box(98f, 702f, 226f, 742f), contentAlignment = Alignment.Center) {
            Text(
                text = if (goldReady) stringResource(R.string.home_ad_watch) else "⏳ " + hms(goldRemaining / 1000),
                style = PaintedStyle(color = HomeInk, fontSize = fs(28f), textAlign = TextAlign.Center),
                maxLines = 1
            )
        }

        // Free chest ad (right)
        val midnight = remember(adChestAvailable) { com.sualtikasifi.cizimhafiza.util.TurkeyTime.nextMidnightMillis() }
        val chestNow = rememberNowUntil(if (adChestAvailable) 0L else midnight)
        val chestRemaining = midnight - chestNow
        Box(box(616f, 538f, 808f, 754f).clickable(enabled = adChestAvailable, interactionSource = noRipple, indication = null, onClick = onWatchChest)) {}
        Box(box(634f, 644f, 796f, 706f), contentAlignment = Alignment.Center) {
            LetteredText(stringResource(R.string.home_ad_chest_label), fs(27f), outline = Color(0xFF14549A), maxLines = 2)
        }
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
            Box(box(x0, y0, x1, y1).clickable(interactionSource = noRipple, indication = null, onClick = onClick).a11yButton(label)) {}
            Box(box(lx0, ly0, lx1, ly1), contentAlignment = Alignment.Center) {
                LetteredText(label, fs(35f), outline = Color(0xFF241408), maxLines = 2)
            }
            content()
        }
        Tile(30f, 776f, 288f, 1012f, 56f, 926f, 268f, 998f, stringResource(R.string.menu_play_online), onPlayOnline)
        Tile(294f, 776f, 552f, 1012f, 330f, 940f, 520f, 986f, stringResource(R.string.quick_match_title), onQuickMatch) {
            val boosted = xpEvent != null && xpEvent.endsAtMillis > System.currentTimeMillis()
            if (boosted && xpEvent != null) BoostBadge(xpEvent.multiplier, box(306f, 788f, 420f, 826f))
        }
        Tile(558f, 776f, 816f, 1012f, 590f, 926f, 780f, 998f, stringResource(R.string.menu_play), onPlay)
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
