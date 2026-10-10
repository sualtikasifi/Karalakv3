package com.sualtikasifi.cizimhafiza.presentation.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import com.sualtikasifi.cizimhafiza.presentation.common.NinePatch
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
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedStyle
import com.sualtikasifi.cizimhafiza.presentation.common.StrokeCanvas
import com.sualtikasifi.cizimhafiza.presentation.common.a11yButton
import com.sualtikasifi.cizimhafiza.presentation.common.cachedPainterResource
import com.sualtikasifi.cizimhafiza.presentation.common.pressable
import com.sualtikasifi.cizimhafiza.util.capitalizeForWordLanguage

// res_head2 is the top of the result design: the mascot with the trophy, the "Oyun Bitti!" sign and the XP plate under
// it, on the workshop wall. 941 x 372 picture units.
private const val HeadW = 941f
private const val HeadH = 372f

internal val ResultInk = Color(0xFF3B2314)
private val CardBorder = Color(0xFFE9A23B)
private val XpOrange = Color(0xFFF2541B)

/**
 * The head of the result screen: the painted picture with the title lettered on its sign and the round's XP (counting up)
 * with the line that explains it on the plate under the sign. Its edges are kept sharp (no fade into the wall).
 */
@Composable
internal fun ResultHeader(title: String, xp: Int, explanation: String, onBack: () -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(HeadW / HeadH)

    ) {
        val u = maxWidth / HeadW
        val fontScale0 = LocalDensity.current.fontScale
        fun fs(art: Float) = (art * u.value / fontScale0).sp
        fun box(x0: Float, y0: Float, x1: Float, y1: Float): Modifier =
            Modifier.offset(u * x0, u * y0).size(u * (x1 - x0), u * (y1 - y0))

        // res_head3 is the painted head with its lower edge fading out, so the dog and the leaves melt into the
        // page instead of ending on a straight cut.
        Image(
            painter = cachedPainterResource(R.drawable.res_head3),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )
        // The painted back button: leaves the same way the claim button does (the XP is already banked).
        Box(
            box(25f, 35f, 128f, 128f)
                .pressable(pressedScale = 0.9f, onClick = onBack)
                .a11yButton(stringResource(R.string.cd_back))
        )
        LetteredText(
            text = title,
            size = fs(84f),
            fill = Color(0xFFFFD84D),
            outline = Color(0xFF4A2410),
            modifier = box(492f, 150f, 874f, 246f),
            minScale = 0.5f,
            title = true
        )
        Row(
            modifier = box(500f, 266f, 852f, 316f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StarBadge(u * 46f)
            Spacer(Modifier.width(u * 14f))
            LetteredText(
                text = stringResource(R.string.xp_gained_format, xp),
                size = fs(56f),
                fill = XpOrange,
                outline = null,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
        FitText(
            text = explanation,
            style = PaintedStyle(color = ResultInk, fontSize = fs(25f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
            maxLines = 1,
            minScale = 0.6f,
            modifier = box(500f, 314f, 852f, 348f)
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

/** What a stat card shows on its left: a picture from the game's art, or an icon in a coloured disc. */
internal sealed interface StatIcon {
    data class Art(val res: Int) : StatIcon
    data class Disc(val icon: ImageVector, val color: Color) : StatIcon
}

internal data class ResultStat(val icon: StatIcon, val label: String, val value: String, val valueColor: Color)

/** The round's numbers as cream cards two to a row (an odd last one centred): hits, misses, stars, gold, bonuses. */
@Composable
internal fun ResultStatGrid(stats: List<ResultStat>, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        stats.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                if (pair.size == 1) Spacer(Modifier.weight(0.5f))
                pair.forEach { StatCard(it, Modifier.weight(1f)) }
                if (pair.size == 1) Spacer(Modifier.weight(0.5f))
            }
        }
    }
}

@Composable
private fun StatCard(stat: ResultStat, modifier: Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .height(52.dp)
            .shadow(3.dp, shape)
            .background(Brush.verticalGradient(listOf(Color(0xFFFFFAEE), Color(0xFFFFEFCF))), shape)
            .border(2.dp, CardBorder, shape)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (val icon = stat.icon) {
            is StatIcon.Art -> Image(painterResource(icon.res), contentDescription = null, modifier = Modifier.size(38.dp))
            is StatIcon.Disc -> Box(
                modifier = Modifier
                    .size(34.dp)
                    .shadow(2.dp, CircleShape)
                    .background(Brush.verticalGradient(listOf(androidx.compose.ui.graphics.lerp(icon.color, Color.White, 0.25f), icon.color)), CircleShape)
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) { Icon(icon.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp)) }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            FitText(
                text = stat.label,
                style = PaintedStyle(color = ResultInk, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                maxLines = 1,
                minScale = 0.65f,
                modifier = Modifier.fillMaxWidth()
            )
            FitText(
                text = stat.value,
                style = PaintedStyle(color = stat.valueColor, fontSize = 20.sp, textAlign = TextAlign.Center),
                maxLines = 1,
                minScale = 0.6f,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** The ready-made stat cards for a round: always hits and misses, then whatever else it paid. */
@Composable
internal fun resultStats(state: GamePhase.Result): List<ResultStat> {
    val green = Color(0xFF2EA043)
    val red = Color(0xFFE23B32)
    val orange = Color(0xFFE8650F)
    val list = mutableListOf(
        ResultStat(StatIcon.Disc(Icons.Filled.Check, Color(0xFF34B24A)), stringResource(R.string.result_stat_correct), state.correctCount.toString(), green),
        ResultStat(StatIcon.Disc(Icons.Filled.Close, Color(0xFFE53935)), stringResource(R.string.result_stat_wrong), state.wrongCount.toString(), red)
    )
    val stars = state.levelStars
    if (stars != null) {
        list += ResultStat(StatIcon.Disc(Icons.Filled.Star, Color(0xFFF2A100)), stringResource(R.string.result_stat_stars), "★".repeat(stars) + "☆".repeat((3 - stars).coerceAtLeast(0)), orange)
    } else {
        state.fastestCorrectSeconds?.let {
            list += ResultStat(StatIcon.Disc(Icons.Filled.Bolt, Color(0xFFF2A100)), stringResource(R.string.result_stat_fastest), stringResource(R.string.result_stat_fastest_value, it), orange)
        }
    }
    if (state.goldFromAchievements > 0) {
        list += ResultStat(StatIcon.Art(R.drawable.icon_league_trophy), stringResource(R.string.result_stat_achievement), stringResource(R.string.result_gold_amount, state.goldFromAchievements), orange)
    }
    if (state.goldFromLevel > 0) {
        list += ResultStat(StatIcon.Disc(Icons.Filled.Star, Color(0xFFF2A100)), stringResource(R.string.result_stat_level_gold), stringResource(R.string.result_gold_amount, state.goldFromLevel), orange)
    }
    if (state.goldFromDaily > 0) {
        list += ResultStat(StatIcon.Art(R.drawable.daily_calendar_icon), stringResource(R.string.result_stat_daily_gold), stringResource(R.string.result_gold_amount, state.goldFromDaily), orange)
    }
    val quick = state.quickMatchDailyBonusApplied
    if (quick) {
        list += ResultStat(StatIcon.Disc(Icons.Filled.Bolt, Color(0xFFE8650F)), stringResource(R.string.result_stat_quick), stringResource(R.string.result_stat_multiplier, 2), orange)
    }
    val mult = state.xpMultiplier
    val eventMult = if (state.xpEventMultiplierApplied) (mult / if (quick) 2 else 1).coerceAtLeast(2) else 0
    if (eventMult > 0) {
        list += ResultStat(StatIcon.Disc(Icons.Filled.Celebration, Color(0xFF8E44D6)), stringResource(R.string.result_stat_event), stringResource(R.string.result_stat_multiplier, eventMult), orange)
    }
    return list
}

/**
 * The daily-challenge card: the ten answers as ticks and crosses, the streak and the XP it paid, and the green button
 * that shares exactly what is shown here.
 */
@Composable
internal fun ResultDailyCard(daily: DailyResultSummary, correctFlags: List<Boolean>, onShare: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .shadow(4.dp, shape)
            .background(Brush.verticalGradient(listOf(Color(0xFFFFF8E6), Color(0xFFFCEACB))), shape)
            .border(3.dp, Color(0xFFE8A13A), shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.daily_challenge_result_title),
            style = PaintedStyle(color = ResultInk, fontSize = 19.sp, textAlign = TextAlign.Center)
        )
        Spacer(Modifier.height(8.dp))
        val count = correctFlags.size.coerceAtLeast(com.sualtikasifi.cizimhafiza.domain.model.DailyChallenge.WORD_COUNT)
        BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            // As big as 32dp, smaller when that many would not fit side by side.
            val pip = (maxWidth / (count * 1.25f)).coerceAtMost(32.dp)
            com.sualtikasifi.cizimhafiza.presentation.common.DailyPips(
                flags = correctFlags,
                count = count,
                size = pip,
                emptyColor = Color(0x33795548),
                rimColor = Color(0xFFFFF6E6)
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            DailyPill("🔥", daily.streak.toString(), stringResource(R.string.daily_streak_label), Color(0xFFE8650F), Modifier.weight(1f))
            DailyPill("⭐", stringResource(R.string.daily_challenge_xp_earned, daily.xpEarned), stringResource(R.string.daily_reward_won), Color(0xFFE8650F), Modifier.weight(1f))
        }
        if (daily.streakMultiplierIncreased) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.daily_challenge_streak_multiplier_increased, daily.streakMultiplier),
                style = PaintedStyle(color = Color(0xFFB5441A), fontSize = 14.sp, textAlign = TextAlign.Center)
            )
        }
        Spacer(Modifier.height(10.dp))
        GreenButton(text = stringResource(R.string.daily_challenge_share), onClick = onShare, modifier = Modifier.fillMaxWidth(0.9f))
    }
}

@Composable
private fun DailyPill(emoji: String, value: String, label: String, valueColor: Color, modifier: Modifier) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .background(Color.White, shape)
            .border(1.5.dp, Color(0xFFEBCB9A), shape)
            .padding(vertical = 6.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(emoji, fontSize = 18.sp)
            Text(value, style = PaintedStyle(color = valueColor, fontSize = 20.sp, textAlign = TextAlign.Center), maxLines = 1)
        }
        FitText(
            text = label,
            style = PaintedStyle(color = ResultInk, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
            maxLines = 1,
            minScale = 0.7f,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** The green "share" button of the design: a glossy pill with a share mark and white lettering. */
@Composable
internal fun GreenButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .height(52.dp)
            .pressable(pressedScale = 0.95f, onClick = onClick)
            .shadow(4.dp, shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF7EDC5C), Color(0xFF34A83A), Color(0xFF248A2E))), shape)
            .border(2.5.dp, Color(0xFF1B6E25), shape)
            .a11yButton(text)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(10.dp))
        LetteredText(text, 21.sp, outline = Color(0xFF1B5E20), modifier = Modifier.weight(1f, fill = false))
    }
}

/**
 * Every drawing of the round on a wooden board: a plank with "Çizdiğin 10 Kelime" across its top and the sheets in rows
 * of five, each with its number, its tick or cross, the drawing and the word. Tapping a sheet opens the replay. [items]
 * may still be empty (the opponent's drawings loading): then [placeholders] empty sheets keep the board's shape.
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
    // The painted board (res_words_board, 1420 x 988 px): the dog and the plank on top, the paper below. Row 647 of
    // the picture is plain paper and frame, so that one row is what stretches when the sheets need more room; the
    // rest is drawn at the picture's own proportions. The title sits on the plank (x 466..1301, y 228..390), the
    // sheets on the paper (x 70..1350, y 420..925).
    BoxWithConstraints(modifier = modifier) {
        val u = maxWidth / 1420f
        val total = if (items.isEmpty()) placeholders else items.size
        val perRow = 5
        val rows = ((total + perRow - 1) / perRow).coerceAtLeast(1)
        val sheetW = (maxWidth - u * 140f - 6.dp * (perRow - 1)) / perRow
        val gridH = sheetW / 0.95f * rows + 10.dp * (rows - 1)
        // The paper as drawn holds 505 px; anything more stretches the band.
        val extra = (gridH + u * 40f - u * 505f).coerceAtLeast(0.dp)
        Box(Modifier.fillMaxWidth().height(u * 988f + extra)) {
            NinePatch(
                res = R.drawable.res_words_board,
                slicePx = 140,
                edge = u * 140f,
                sliceYPx = 647,
                edgeY = u * 647f,
                clampEdgeToHeight = false,
                modifier = Modifier.matchParentSize()
            )
            // The title on the plank, with the share button at its right end.
            Box(
                modifier = Modifier.offset(u * 466f, u * 236f).size(u * 835f, u * 146f),
                contentAlignment = Alignment.Center
            ) {
                if (header != null) {
                    Box(Modifier.fillMaxWidth().padding(horizontal = 6.dp)) { header() }
                } else {
                    LetteredText(
                        title, 19.sp, maxLines = 1,
                        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = if (onShareAll != null) 44.dp else 12.dp),
                        title = true
                    )
                }
                if (onShareAll != null && header == null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 14.dp)
                            .size(30.dp)
                            .background(Color(0x33FFFFFF), CircleShape)
                            .pressable(pressedScale = 0.88f, onClick = onShareAll)
                            .a11yButton(stringResource(R.string.share_all_drawings)),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Filled.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp)) }
                }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().padding(start = u * 70f, end = u * 70f, top = u * 440f)
            ) {
                (0 until total).chunked(perRow).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
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
                .shadow(3.dp, sheet)
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
