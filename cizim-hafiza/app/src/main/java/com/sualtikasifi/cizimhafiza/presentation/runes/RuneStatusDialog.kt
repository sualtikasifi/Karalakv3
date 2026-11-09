package com.sualtikasifi.cizimhafiza.presentation.runes

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.RuneEffect
import com.sualtikasifi.cizimhafiza.domain.model.RuneEffects
import com.sualtikasifi.cizimhafiza.domain.model.RuneGates
import com.sualtikasifi.cizimhafiza.domain.model.RuneTree
import com.sualtikasifi.cizimhafiza.domain.model.RuneUnit
import com.sualtikasifi.cizimhafiza.presentation.common.LetteredText
import com.sualtikasifi.cizimhafiza.presentation.common.PrimaryButton
import com.sualtikasifi.cizimhafiza.presentation.theme.DisplayFont
import java.text.NumberFormat

/** Where the once-a-day / once-a-month abilities stand right now, read when the window opens. */
data class RuneAbilityState(
    val instantReady: Boolean,
    val bestFriendReady: Boolean,
    val insuranceReady: Boolean,
    val reserveWaiting: Boolean
)

private val Ink = Color(0xFF3A2416)
private val LabelColor = Color(0xFF9A5418)
private val Faded = Color(0xFF9A8670)
private val Good = Color(0xFF2E7D32)

/** The stat effects in the order the window lists them, with the label of each. */
private val statRows: List<Pair<RuneEffect, Int>> = listOf(
    RuneEffect.WORD to R.string.rune_stat_word,
    RuneEffect.WIN to R.string.rune_stat_win,
    RuneEffect.LOGIN to R.string.rune_stat_login,
    RuneEffect.DAILY to R.string.rune_stat_daily,
    RuneEffect.CHEST to R.string.rune_stat_chest,
    RuneEffect.CJOKER to R.string.rune_stat_cjoker,
    RuneEffect.DISC to R.string.rune_stat_disc,
    RuneEffect.DJ to R.string.rune_stat_dj,
    RuneEffect.XP to R.string.rune_stat_xp,
    RuneEffect.CTIME to R.string.rune_stat_ctime,
    RuneEffect.ADCD to R.string.rune_stat_adcd,
    RuneEffect.ROOM to R.string.rune_stat_room,
    RuneEffect.HOST to R.string.rune_stat_host
)

/** Effects that take something away (a price, a wait) rather than add: written "-%10". */
private val reductions = setOf(RuneEffect.DISC, RuneEffect.CTIME, RuneEffect.ADCD)

/**
 * "Rün Durumu": everything the rune map has given the player on one page — level and progress, every bonus as it
 * stands against its top, the abilities with today's state, and the rune-only rewards collected. Opened from the centre.
 */
@Composable
fun RuneStatusDialog(levels: Map<String, Int>, playerLevel: Int, abilities: RuneAbilityState, onDismiss: () -> Unit) {
    val numbers = NumberFormat.getIntegerInstance()
    // A slow golden breath around the window and a soft spring on the way in: the frame feels alive without moving anything
    // the player has to read.
    val breath by rememberInfiniteTransition(label = "runeStatusGlow").animateFloat(
        initialValue = 0.28f, targetValue = 0.62f,
        animationSpec = infiniteRepeatable(tween(2300, easing = LinearEasing), RepeatMode.Reverse), label = "glow"
    )
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = 340f)) }
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .graphicsLayer {
                    val e = enter.value
                    scaleX = 0.88f + 0.12f * e
                    scaleY = 0.88f + 0.12f * e
                    alpha = (e * 3f).coerceIn(0f, 1f)
                }
                .drawBehind {
                    val r = 34.dp.toPx()
                    val spread = 11.dp.toPx() * (0.6f + breath)
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            0f to Color(0xFFFFC94A).copy(alpha = breath),
                            1f to Color.Transparent,
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = maxOf(size.width, size.height) * 0.62f
                        ),
                        topLeft = Offset(-spread, -spread),
                        size = androidx.compose.ui.geometry.Size(size.width + spread * 2, size.height + spread * 2),
                        cornerRadius = CornerRadius(r + spread)
                    )
                }
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .sliced(R.drawable.rune_ui_card, srcCorner = 58, dstCorner = 26.dp)
                .padding(horizontal = 24.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LetteredText(text = stringResource(R.string.rune_status_title), size = 32.sp, title = true)
            Spacer(Modifier.size(6.dp))
            // A thin gold rule under the title, fading out at both ends.
            Box(
                Modifier.fillMaxWidth(0.7f).height(2.dp)
                    .background(Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFFD9A441), Color.Transparent)))
            )
            Spacer(Modifier.size(8.dp))
            val scroll = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 470.dp)
                    .fadeEdges(scroll)
                    .verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val runes = RuneTree.all.filter { it.id != RuneTree.ROOT_ID }
                SectionBar(stringResource(R.string.rune_status_general))
                var row = 0
                StatusRow(stringResource(R.string.rune_status_player_level), playerLevel.toString(), striped = row++ % 2 == 0)
                StatusRow(
                    stringResource(R.string.rune_status_rune_levels),
                    "${runes.sumOf { RuneTree.levelOf(levels, it.id) }} / ${runes.sumOf { it.maxLevel }}",
                    striped = row++ % 2 == 0,
                    progress = runes.sumOf { RuneTree.levelOf(levels, it.id) }.toFloat() / runes.sumOf { it.maxLevel }.coerceAtLeast(1)
                )
                StatusRow(
                    stringResource(R.string.rune_status_spent),
                    numbers.format(runes.sumOf { r -> (0 until RuneTree.levelOf(levels, r.id)).sumOf { RuneGates.price(r, it) ?: 0 } }),
                    striped = row++ % 2 == 0
                )
                (1..4).filter { ring -> RuneTree.all.any { it.ring == ring } }.forEach { ring ->
                    val label = if (ring == 4) stringResource(R.string.rune_status_ring_legend) else stringResource(R.string.rune_status_ring, ring)
                    if (RuneTree.ringOpen(levels, ring, playerLevel)) {
                        val p = RuneTree.ringProgress(levels, ring)
                        StatusRow(label, stringResource(R.string.rune_status_percent, (p * 100).toInt()), striped = row++ % 2 == 0, progress = p)
                    } else {
                        StatusRow(label, stringResource(R.string.rune_status_ring_locked, RuneGates.levelFor(ring)), dim = true, striped = row++ % 2 == 0)
                    }
                }

                SectionBar(stringResource(R.string.rune_status_bonuses))
                row = 0
                statRows.forEach { (effect, labelRes) ->
                    val now = RuneEffects.total(levels, effect)
                    val top = RuneEffects.capOf(effect) ?: RuneTree.all.filter { it.effect == effect }.sumOf { it.maxLevel * it.perLevel }
                    StatusRow(
                        stringResource(labelRes), statText(effect, now), extra = "/ " + statText(effect, top), dim = now == 0,
                        striped = row++ % 2 == 0, progress = if (top > 0) now.toFloat() / top else 0f
                    )
                }

                SectionBar(stringResource(R.string.rune_status_abilities))
                row = 0
                runes.filter { it.effect.unit == RuneUnit.FLAG && it.effect != RuneEffect.STYLE }.forEach { rune ->
                    val owned = RuneTree.levelOf(levels, rune.id) >= 1
                    val ready = when (rune.effect) {
                        RuneEffect.INSTANT -> abilities.instantReady
                        RuneEffect.BEST -> abilities.bestFriendReady
                        RuneEffect.INSURE -> abilities.insuranceReady
                        RuneEffect.RESERVE -> abilities.reserveWaiting
                        else -> true
                    }
                    val state = when {
                        !owned -> R.string.rune_status_locked
                        rune.effect == RuneEffect.INSTANT -> if (abilities.instantReady) R.string.rune_status_ready_today else R.string.rune_status_used_today
                        rune.effect == RuneEffect.BEST -> if (abilities.bestFriendReady) R.string.rune_status_ready_today else R.string.rune_status_used_today
                        rune.effect == RuneEffect.INSURE -> if (abilities.insuranceReady) R.string.rune_status_ready_month else R.string.rune_status_used_month
                        rune.effect == RuneEffect.RESERVE -> if (abilities.reserveWaiting) R.string.rune_status_reserve_full else R.string.rune_status_reserve_empty
                        else -> R.string.rune_status_on
                    }
                    StatusRow(
                        stringResource(rune.nameRes), stringResource(state), dim = !owned, striped = row++ % 2 == 0,
                        valueColor = if (owned && ready) Good else null
                    )
                }

                // The collection of rune rewards: only shown while some rune gives one.
                val style = runes.filter { it.effect == RuneEffect.STYLE }
                if (style.isNotEmpty()) {
                    SectionBar(stringResource(R.string.rune_status_collection))
                    row = 0
                    StatusRow(stringResource(R.string.rune_status_rewards), "${style.count { RuneTree.levelOf(levels, it.id) >= 1 }} / ${style.size}", striped = row++ % 2 == 0)
                    style.forEach { rune ->
                        val owned = RuneTree.levelOf(levels, rune.id) >= 1
                        StatusRow(
                            stringResource(rune.nameRes), stringResource(if (owned) R.string.rune_status_owned else R.string.rune_status_locked),
                            dim = !owned, striped = row++ % 2 == 0, valueColor = if (owned) Good else null
                        )
                    }
                }
            }
            Spacer(Modifier.size(14.dp))
            PrimaryButton(text = stringResource(R.string.close), onClick = onDismiss, modifier = Modifier.fillMaxWidth(), height = 50.dp)
        }
        }
    }
}

/** Lets the list melt into the parchment at its top and bottom edge when there is more to scroll, instead of cutting a line in half. */
private fun Modifier.fadeEdges(scroll: ScrollState): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val fade = 16.dp.toPx()
        if (scroll.value > 0) drawRect(
            Brush.verticalGradient(listOf(Color.Transparent, Color.Black), startY = 0f, endY = fade), size = androidx.compose.ui.geometry.Size(size.width, fade), blendMode = BlendMode.DstIn
        )
        if (scroll.value < scroll.maxValue) drawRect(
            Brush.verticalGradient(listOf(Color.Black, Color.Transparent), startY = size.height - fade, endY = size.height),
            topLeft = Offset(0f, size.height - fade), size = androidx.compose.ui.geometry.Size(size.width, fade), blendMode = BlendMode.DstIn
        )
    }

@Composable
private fun statText(effect: RuneEffect, value: Int): String = when {
    effect.unit == RuneUnit.COUNT -> stringResource(R.string.rune_status_count, value)
    effect in reductions -> stringResource(R.string.rune_status_minus, value)
    else -> stringResource(R.string.rune_tile_pct, value)
}

/** A section title on a gold band, like the class bar of a game's status sheet. */
@Composable
private fun SectionBar(text: String) {
    Spacer(Modifier.size(8.dp))
    Text(
        text = text,
        fontFamily = DisplayFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 16.sp,
        color = Ink,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFFEBCB8B), Color(0xFFDDB46C))))
            .padding(vertical = 6.dp)
    )
    Spacer(Modifier.size(3.dp))
}

/**
 * One line of the sheet: the label on the left in the warm colour, the value on the right, an optional faded top after it.
 * Every other line sits on a faint band so the eye can follow it across; [progress] draws a thin bar under the label.
 */
@Composable
private fun StatusRow(
    label: String, value: String, extra: String? = null, dim: Boolean = false,
    striped: Boolean = false, progress: Float? = null, valueColor: Color? = null
) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            .background(if (striped) Color(0x1F8A5A1E) else Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                fontFamily = DisplayFont,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (dim) Faded else LabelColor,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = value,
                fontFamily = DisplayFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = valueColor ?: if (dim) Faded else Ink,
                maxLines = 1
            )
            if (extra != null) {
                Text(
                    text = " $extra",
                    fontFamily = DisplayFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Faded,
                    maxLines = 1
                )
            }
        }
        if (progress != null) {
            Spacer(Modifier.size(3.dp))
            Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color(0x33523112))) {
                Box(
                    Modifier.fillMaxHeight().fillMaxWidth(progress.coerceIn(0f, 1f))
                        .background(Brush.horizontalGradient(listOf(Color(0xFFE8A33A), Color(0xFFF2C14E))))
                )
            }
        }
    }
}
