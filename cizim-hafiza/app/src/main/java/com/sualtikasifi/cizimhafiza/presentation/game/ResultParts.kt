package com.sualtikasifi.cizimhafiza.presentation.game

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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.ResultItem
import com.sualtikasifi.cizimhafiza.presentation.common.DescriptionStyle
import com.sualtikasifi.cizimhafiza.presentation.common.FitText
import com.sualtikasifi.cizimhafiza.presentation.common.LetteredText
import com.sualtikasifi.cizimhafiza.presentation.common.NinePatch
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedStyle
import com.sualtikasifi.cizimhafiza.presentation.common.StrokeCanvas
import com.sualtikasifi.cizimhafiza.presentation.common.a11yButton
import com.sualtikasifi.cizimhafiza.presentation.common.cachedPainterResource
import com.sualtikasifi.cizimhafiza.presentation.common.pressable
import com.sualtikasifi.cizimhafiza.util.capitalizeForWordLanguage

// The result screen is laid out in the design's own units: 841 wide, everything else measured against that.
internal const val ResArtW = 841f

// rs_head (663 x 415 px): the back button, the mascot with the trophy and the empty sign, shown 720 units wide and
// centred; the XP plate (rs_xp) overlaps the wreath's lower edge.
private const val HeadShown = 690f
private const val HeadScale = HeadShown / 663f
// The sign sits a little left of the picture's middle; shifting the picture by this puts the sign on the screen's centre.
private const val HeadShift = 7f
private const val HeadTop = 0f
private const val PlateTop = 440f
private const val PlateH = 100f
// The XP plate is gone (the round's XP is the last stat card now), so the head is the picture alone.
internal const val HeadBlockH = HeadTop + 415f * HeadScale + 2f

internal val ResultInk = Color(0xFF3B2314)
internal val XpOrange = Color(0xFFF2541B)

/** The head of the result screen: the painted picture with the title lettered on its sign. */
@Composable
internal fun ResultHeader(title: String, onBack: () -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(ResArtW / HeadBlockH)
    ) {
        val u = maxWidth / ResArtW
        val fontScale0 = LocalDensity.current.fontScale
        fun fs(art: Float) = (art * u.value / fontScale0).sp
        fun box(x0: Float, y0: Float, x1: Float, y1: Float): Modifier =
            Modifier.offset(u * x0, u * y0).size(u * (x1 - x0), u * (y1 - y0))
        // A point of the head picture (px) in design units.
        fun hx(px: Float) = (ResArtW - HeadShown) / 2f + HeadShift + px * HeadScale
        fun hy(py: Float) = HeadTop + py * HeadScale

        Image(
            painter = cachedPainterResource(R.drawable.rs_head),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = box(hx(0f), hy(0f), hx(663f), hy(415f))
        )
        // The painted back button: leaves the same way the claim button does (the XP is already banked).
        Box(
            box(hx(2f), hy(18f), hx(118f), hy(136f))
                .pressable(pressedScale = 0.9f, onClick = onBack)
                .a11yButton(stringResource(R.string.cd_back))
        )
        LetteredText(
            text = title,
            size = fs(80f),
            fill = Color.White,
            outline = Color(0xFF4A2410),
            modifier = box(hx(95f), hy(268f), hx(555f), hy(350f)),
            minScale = 0.5f,
            title = true
        )
    }
}

/** A gold star with a darker rim, the mark the design puts in front of every XP amount. */
@Composable
private fun StarBadge(size: Dp) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFB86A00), modifier = Modifier.fillMaxSize())
        Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFC21A), modifier = Modifier.fillMaxSize(0.8f))
    }
}
/** What a stat card shows on its left: a painted disc or picture, or an icon in a coloured disc. */
internal sealed interface StatIcon {
    data class Art(val res: Int) : StatIcon
    data class Disc(val icon: ImageVector, val color: Color) : StatIcon
}

/** [headline]: one centred line over the whole card (the round's XP), no icon, no label. */
internal data class ResultStat(val icon: StatIcon, val label: String, val value: String, val valueColor: Color, val headline: Boolean = false)

/** The round's numbers as painted cards two to a row (an odd last one centred): hits, misses, stars, gold, bonuses. */
@Composable
internal fun ResultStatGrid(stats: List<ResultStat>, u: Dp, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(u * 8f)) {
        stats.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(u * 12f), modifier = Modifier.fillMaxWidth()) {
                if (pair.size == 1) Spacer(Modifier.weight(0.5f))
                pair.forEachIndexed { i, stat -> StatCard(stat, u, leafOnRight = pair.size == 1 || i == 1, modifier = Modifier.weight(1f)) }
                if (pair.size == 1) Spacer(Modifier.weight(0.5f))
            }
        }
    }
}

// rs_card_a has its leaf on the right edge, rs_card_b on the left: the outer edges of the grid carry them.
@Composable
private fun StatCard(stat: ResultStat, u: Dp, leafOnRight: Boolean, modifier: Modifier) {
    val fontScale0 = LocalDensity.current.fontScale
    fun fs(art: Float) = (art * u.value / fontScale0).sp
    Box(modifier = modifier.height(u * 108f)) {
        Image(
            painter = cachedPainterResource(if (leafOnRight) R.drawable.rs_card_a else R.drawable.rs_card_b),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )
        if (stat.headline) {
            FitText(
                text = stat.value,
                style = PaintedStyle(color = stat.valueColor, fontSize = fs(46f), textAlign = TextAlign.Center),
                maxLines = 1,
                minScale = 0.45f,
                modifier = Modifier.fillMaxSize().padding(top = u * 6f, start = u * 40f, end = u * 40f)
            )
            return@Box
        }
        Row(
            modifier = Modifier.fillMaxSize().padding(top = u * 6f, start = u * (if (leafOnRight) 22f else 36f), end = u * (if (leafOnRight) 46f else 22f)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(u * 6f)
        ) {
            Box(Modifier.size(u * 74f), contentAlignment = Alignment.Center) {
                when (val icon = stat.icon) {
                    is StatIcon.Art -> Image(painterResource(icon.res), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                    is StatIcon.Disc -> Box(
                        modifier = Modifier
                            .fillMaxSize(0.92f)
                            .shadow(2.dp, CircleShape)
                            .background(Brush.verticalGradient(listOf(androidx.compose.ui.graphics.lerp(icon.color, Color.White, 0.25f), icon.color)), CircleShape)
                            .border(1.5.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Icon(icon.icon, contentDescription = null, tint = Color.White, modifier = Modifier.fillMaxSize(0.62f)) }
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                FitText(
                    text = stat.label,
                    style = PaintedStyle(color = ResultInk, fontSize = fs(25f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                    maxLines = 1,
                    minScale = 0.6f,
                    modifier = Modifier.fillMaxWidth().offset(y = u * 8f)
                )
                FitText(
                    text = stat.value,
                    style = PaintedStyle(color = stat.valueColor, fontSize = fs(46f), textAlign = TextAlign.Center),
                    maxLines = 1,
                    minScale = 0.5f,
                    modifier = Modifier.fillMaxWidth().offset(y = -(u * 8f))
                )
            }
        }
    }
}

/** The ready-made stat cards for a round: always hits and misses, then whatever else it paid. */
@Composable
internal fun resultStats(state: GamePhase.Result, xp: Int, withXpCard: Boolean): List<ResultStat> {
    val green = Color(0xFF2EA043)
    val red = Color(0xFFE23B32)
    val orange = Color(0xFFE8650F)
    val list = mutableListOf(
        ResultStat(StatIcon.Art(R.drawable.rs_ic_check), stringResource(R.string.result_stat_correct), state.correctCount.toString(), green),
        ResultStat(StatIcon.Art(R.drawable.rs_ic_x), stringResource(R.string.result_stat_wrong), state.wrongCount.toString(), red)
    )
    // (The fastest-answer card used to follow here; it is gone to leave room for the drawings.)
    val stars = state.levelStars
    if (stars != null) {
        list += ResultStat(StatIcon.Art(R.drawable.rs_ic_star), stringResource(R.string.result_stat_stars), "★".repeat(stars) + "☆".repeat((3 - stars).coerceAtLeast(0)), orange)
    }
    if (state.goldFromAchievements > 0) {
        list += ResultStat(StatIcon.Art(R.drawable.icon_league_trophy), stringResource(R.string.result_stat_achievement), stringResource(R.string.result_gold_amount, state.goldFromAchievements), orange)
    }
    if (state.goldFromLevel > 0) {
        list += ResultStat(StatIcon.Art(R.drawable.rs_ic_crown), stringResource(R.string.result_stat_level_gold), stringResource(R.string.result_gold_amount, state.goldFromLevel), orange)
    }
    if (state.goldFromDaily > 0) {
        list += ResultStat(StatIcon.Art(R.drawable.daily_calendar_icon), stringResource(R.string.result_stat_daily_gold), stringResource(R.string.result_gold_amount, state.goldFromDaily), orange)
    }
    val quick = state.quickMatchDailyBonusApplied
    val mult = state.xpMultiplier
    val eventMult = if (state.xpEventMultiplierApplied) (mult / if (quick) 2 else 1).coerceAtLeast(2) else 0
    if (eventMult > 0) {
        list += ResultStat(StatIcon.Disc(Icons.Filled.Celebration, Color(0xFF8E44D6)), stringResource(R.string.result_stat_event), stringResource(R.string.result_stat_multiplier, eventMult), orange)
    }
    if (withXpCard) {
        list += ResultStat(StatIcon.Art(R.drawable.rs_ic_star), "", stringResource(R.string.result_xp_won, xp), XpOrange, headline = true)
    }
    return list
}
/**
 * The daily-challenge card: the ten answers as ticks and crosses, the streak and the XP it paid, and the green button
 * that shares exactly what is shown here.
 */
@Composable
internal fun ResultDailyCard(daily: DailyResultSummary, correctFlags: List<Boolean>, onShare: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .shadow(4.dp, shape)
            .background(Brush.verticalGradient(listOf(Color(0xFFFFF8E6), Color(0xFFFCEACB))), shape)
            .border(2.5.dp, Color(0xFFE8A13A), shape)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.daily_challenge_result_title),
            style = PaintedStyle(color = ResultInk, fontSize = 16.sp, textAlign = TextAlign.Center)
        )
        Spacer(Modifier.height(4.dp))
        val count = correctFlags.size.coerceAtLeast(com.sualtikasifi.cizimhafiza.domain.model.DailyChallenge.WORD_COUNT)
        BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            // As big as 24dp, smaller when that many would not fit side by side.
            val pip = (maxWidth / (count * 1.25f)).coerceAtMost(24.dp)
            com.sualtikasifi.cizimhafiza.presentation.common.DailyPips(
                flags = correctFlags,
                count = count,
                size = pip,
                emptyColor = Color(0x33795548),
                rimColor = Color(0xFFFFF6E6)
            )
        }
        Spacer(Modifier.height(5.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            DailyPill("🔥", daily.streak.toString(), stringResource(R.string.daily_streak_label), Color(0xFFE8650F), Modifier.weight(1f))
            DailyPill("⭐", stringResource(R.string.daily_challenge_xp_earned, daily.xpEarned), stringResource(R.string.daily_reward_won), Color(0xFFE8650F), Modifier.weight(1f))
        }
        if (daily.streakMultiplierIncreased) {
            Spacer(Modifier.height(3.dp))
            Text(
                text = stringResource(R.string.daily_challenge_streak_multiplier_increased, daily.streakMultiplier),
                style = PaintedStyle(color = Color(0xFFB5441A), fontSize = 12.sp, textAlign = TextAlign.Center)
            )
        }
        Spacer(Modifier.height(5.dp))
        GreenButton(text = stringResource(R.string.daily_challenge_share), onClick = onShare, modifier = Modifier.fillMaxWidth(0.8f), height = 38.dp)
    }
}

@Composable
private fun DailyPill(emoji: String, value: String, label: String, valueColor: Color, modifier: Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .background(Color.White, shape)
            .border(1.5.dp, Color(0xFFEBCB9A), shape)
            .padding(vertical = 3.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(emoji, fontSize = 15.sp)
            Text(value, style = PaintedStyle(color = valueColor, fontSize = 17.sp, textAlign = TextAlign.Center), maxLines = 1)
        }
        FitText(
            text = label,
            style = PaintedStyle(color = ResultInk, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
            maxLines = 1,
            minScale = 0.7f,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** The green "share" button of the design: a glossy pill with a share mark and white lettering. */
@Composable
internal fun GreenButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, height: Dp = 52.dp) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .height(height)
            .pressable(pressedScale = 0.95f, onClick = onClick)
            .shadow(4.dp, shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF7EDC5C), Color(0xFF34A83A), Color(0xFF248A2E))), shape)
            .border(2.5.dp, Color(0xFF1B6E25), shape)
            .a11yButton(text)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(height * 0.5f))
        Spacer(Modifier.width(10.dp))
        LetteredText(text, (height.value * 0.4f).sp, outline = Color(0xFF1B5E20), modifier = Modifier.weight(1f, fill = false))
    }
}

/**
 * Every drawing of the round on the painted board (rs_board, 714 x 300 px): the dog peeking over the frame, the wooden
 * plank with the title, and the paper below it with the sheets in rows of five, each with its number, its tick or cross,
 * the drawing and the word. Tapping a sheet opens the replay. [items] may still be empty (the opponent's drawings
 * loading): then [placeholders] empty sheets keep the board's shape.
 */
@Composable
internal fun ResultWordsBoard(
    title: String,
    items: List<ResultItem>,
    placeholders: Int,
    wordLanguage: String,
    onPreview: (ResultItem) -> Unit,
    onShareAll: (() -> Unit)?,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null
) {
    // The board is drawn in three slices at the picture's own scale: rows 0..170 (dog, plank, the paper's top), the one
    // paper row 170 stretched to whatever height the sheets need, and rows 171..300 (the rest of the paper, the frame).
    // The plank is at x 285..575, y 56..114; the paper at x 36..668, y 134..268.
    BoxWithConstraints(modifier = modifier) {
        val k = maxWidth / 714f
        val fontScale0 = LocalDensity.current.fontScale
        fun fs(px: Float) = (px * k.value / fontScale0).sp
        val total = if (items.isEmpty()) placeholders else items.size
        val perRow = 5
        val rows = ((total + perRow - 1) / perRow).coerceAtLeast(1)
        val gap = k * 8f
        val sheetW = (k * 632f - gap * (perRow - 1)) / perRow
        val gridH = sheetW / 0.95f * rows + gap * (rows - 1)
        val boardH = maxOf(k * 300f, k * 138f + gridH + k * 70f)
        Box(Modifier.fillMaxWidth().height(boardH)) {
            val board = androidx.compose.ui.graphics.ImageBitmap.imageResource(R.drawable.rs_board)
            androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
                val w = size.width.toInt()
                val kk = size.width / board.width
                val topH = (170 * kk).toInt()
                val botSrc = board.height - 171
                val botH = (botSrc * kk).toInt()
                val h = size.height.toInt()
                drawImage(board, androidx.compose.ui.unit.IntOffset(0, 0), androidx.compose.ui.unit.IntSize(board.width, 170),
                    dstOffset = androidx.compose.ui.unit.IntOffset(0, 0), dstSize = androidx.compose.ui.unit.IntSize(w, topH))
                drawImage(board, androidx.compose.ui.unit.IntOffset(0, 170), androidx.compose.ui.unit.IntSize(board.width, 1),
                    dstOffset = androidx.compose.ui.unit.IntOffset(0, topH), dstSize = androidx.compose.ui.unit.IntSize(w, (h - botH - topH).coerceAtLeast(0) + 1))
                drawImage(board, androidx.compose.ui.unit.IntOffset(0, 171), androidx.compose.ui.unit.IntSize(board.width, botSrc),
                    dstOffset = androidx.compose.ui.unit.IntOffset(0, h - botH), dstSize = androidx.compose.ui.unit.IntSize(w, botH))
            }
            // The title on the plank, with the share button at its right end.
            Box(
                modifier = Modifier.offset(k * 272f, k * 56f).size(k * 316f, k * 58f),
                contentAlignment = Alignment.Center
            ) {
                if (header != null) {
                    Box(Modifier.fillMaxWidth().padding(horizontal = 2.dp)) { header() }
                } else {
                    LetteredText(
                        title, fs(36f), maxLines = 1,
                        modifier = Modifier.fillMaxWidth().padding(start = k * 8f, end = if (onShareAll != null) k * 50f else k * 8f),
                        title = true
                    )
                }
                if (onShareAll != null && header == null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = k * 6f)
                            .size(k * 42f)
                            .background(Color(0x33FFFFFF), CircleShape)
                            .pressable(pressedScale = 0.88f, onClick = onShareAll)
                            .a11yButton(stringResource(R.string.share_all_drawings)),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Filled.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(k * 28f)) }
                }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(gap),
                modifier = Modifier.fillMaxWidth().padding(start = k * 40f, end = k * 42f, top = k * 139f)
            ) {
                (0 until total).chunked(perRow).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(gap), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { n ->
                            WordSheet(
                                number = n + 1,
                                item = items.getOrNull(n),
                                wordLanguage = wordLanguage,
                                onClick = onPreview,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}
@Composable
private fun WordSheet(number: Int, item: ResultItem?, wordLanguage: String, onClick: (ResultItem) -> Unit, modifier: Modifier) {
    val sheet = RoundedCornerShape(8.dp)
    Box(modifier = modifier.aspectRatio(0.95f)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 6.dp, end = 4.dp)
                .shadow(2.dp, sheet)
                .clip(sheet)
                .background(if (item == null) Color(0x99FFF3DA) else Color(0xFFFFF6E2))
                .border(1.dp, Color(0xFFD9BC8C), sheet)
                .then(
                    if (item != null) Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onClick(item) } else Modifier
                )
        ) {
            if (item != null) {
                // The notebook's binding dots down the left edge, as on the design's sheets.
                Column(
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 3.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) { repeat(5) { Box(Modifier.size(3.dp).background(Color(0xFFB59A7A), CircleShape)) } }
                Text(
                    text = number.toString(),
                    style = PaintedStyle(color = ResultInk, fontSize = 12.sp, textAlign = TextAlign.Start),
                    modifier = Modifier.padding(start = 5.dp, top = 2.dp)
                )
                Column(modifier = Modifier.fillMaxSize().padding(start = 8.dp, end = 3.dp, top = 12.dp, bottom = 2.dp)) {
                    StrokeCanvas(strokes = item.strokes, modifier = Modifier.weight(1f).fillMaxWidth())
                    FitText(
                        text = item.word.capitalizeForWordLanguage(wordLanguage),
                        style = PaintedStyle(color = ResultInk, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                        maxLines = 1,
                        minScale = 0.6f,
                        modifier = Modifier.fillMaxWidth().height(16.dp)
                    )
                }
            }
        }
        if (item != null) {
            ResultMark(correct = item.isCorrect, size = 20.dp, modifier = Modifier.align(Alignment.TopEnd))
        }
    }
}

/** The bottom claim buttons' inside: an icon, then one or two lines of white lettering. */
@Composable
internal fun RowScope.ClaimContent(icon: ImageVector, line1: String, line2: String? = null) {
    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
    Spacer(Modifier.width(8.dp))
    // Both buttons letter their main line in the same size and on the same line across the pair; the second
    // line (only the ad button has one) hangs below it without moving it.
    Box(modifier = Modifier.weight(1f, fill = false), contentAlignment = Alignment.Center) {
        LetteredText(line1, 18.sp, outline = Color(0xFF8A3A00), maxLines = 1, minScale = 0.7f)
        if (line2 != null) {
            Text(
                line2,
                style = DescriptionStyle(10.sp).copy(color = Color.White, fontWeight = FontWeight.Bold),
                maxLines = 1,
                modifier = Modifier.align(Alignment.BottomCenter).offset(y = 13.dp)
            )
        }
    }
}
