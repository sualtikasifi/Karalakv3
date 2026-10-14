package com.sualtikasifi.cizimhafiza.presentation.runes

import androidx.compose.foundation.background
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
private val LabelColor = Color(0xFFB0641E)
private val Faded = Color(0xFF9A8670)
private val BarColor = Color(0xFFE2C48E)

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
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .sliced(R.drawable.rune_ui_card, srcCorner = 58, dstCorner = 26.dp)
                .padding(horizontal = 22.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LetteredText(text = stringResource(R.string.rune_status_title), size = 26.sp)
            Spacer(Modifier.size(10.dp))
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 470.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                val runes = RuneTree.all.filter { it.id != RuneTree.ROOT_ID }
                SectionBar(stringResource(R.string.rune_status_general))
                StatusRow(stringResource(R.string.rune_status_player_level), playerLevel.toString())
                StatusRow(
                    stringResource(R.string.rune_status_rune_levels),
                    "${runes.sumOf { RuneTree.levelOf(levels, it.id) }} / ${runes.sumOf { it.maxLevel }}"
                )
                StatusRow(
                    stringResource(R.string.rune_status_spent),
                    numbers.format(runes.sumOf { r -> (0 until RuneTree.levelOf(levels, r.id)).sumOf { RuneGates.price(r, it) ?: 0 } })
                )
                (1..4).forEach { ring ->
                    val label = if (ring == 4) stringResource(R.string.rune_status_ring_legend) else stringResource(R.string.rune_status_ring, ring)
                    if (RuneTree.ringOpen(levels, ring, playerLevel)) {
                        StatusRow(label, stringResource(R.string.rune_status_percent, (RuneTree.ringProgress(levels, ring) * 100).toInt()))
                    } else {
                        StatusRow(label, stringResource(R.string.rune_status_ring_locked, RuneGates.levelFor(ring)), dim = true)
                    }
                }

                SectionBar(stringResource(R.string.rune_status_bonuses))
                statRows.forEach { (effect, labelRes) ->
                    val now = RuneEffects.total(levels, effect)
                    val top = RuneEffects.capOf(effect) ?: RuneTree.all.filter { it.effect == effect }.sumOf { it.maxLevel * it.perLevel }
                    StatusRow(stringResource(labelRes), statText(effect, now), extra = "/ " + statText(effect, top), dim = now == 0)
                }

                SectionBar(stringResource(R.string.rune_status_abilities))
                runes.filter { it.effect.unit == RuneUnit.FLAG && it.effect != RuneEffect.STYLE }.forEach { rune ->
                    val owned = RuneTree.levelOf(levels, rune.id) >= 1
                    val state = when {
                        !owned -> R.string.rune_status_locked
                        rune.effect == RuneEffect.INSTANT -> if (abilities.instantReady) R.string.rune_status_ready_today else R.string.rune_status_used_today
                        rune.effect == RuneEffect.BEST -> if (abilities.bestFriendReady) R.string.rune_status_ready_today else R.string.rune_status_used_today
                        rune.effect == RuneEffect.INSURE -> if (abilities.insuranceReady) R.string.rune_status_ready_month else R.string.rune_status_used_month
                        rune.effect == RuneEffect.RESERVE -> if (abilities.reserveWaiting) R.string.rune_status_reserve_full else R.string.rune_status_reserve_empty
                        else -> R.string.rune_status_on
                    }
                    StatusRow(stringResource(rune.nameRes), stringResource(state), dim = !owned)
                }

                SectionBar(stringResource(R.string.rune_status_collection))
                val style = runes.filter { it.effect == RuneEffect.STYLE }
                StatusRow(stringResource(R.string.rune_status_rewards), "${style.count { RuneTree.levelOf(levels, it.id) >= 1 }} / ${style.size}")
                style.forEach { rune ->
                    val owned = RuneTree.levelOf(levels, rune.id) >= 1
                    StatusRow(stringResource(rune.nameRes), stringResource(if (owned) R.string.rune_status_owned else R.string.rune_status_locked), dim = !owned)
                }
            }
            Spacer(Modifier.size(14.dp))
            PrimaryButton(text = stringResource(R.string.close), onClick = onDismiss, modifier = Modifier.fillMaxWidth(), height = 50.dp)
        }
    }
}

@Composable
private fun statText(effect: RuneEffect, value: Int): String = when {
    effect.unit == RuneUnit.COUNT -> stringResource(R.string.rune_status_count, value)
    effect in reductions -> stringResource(R.string.rune_status_minus, value)
    else -> stringResource(R.string.rune_tile_pct, value)
}

/** A section title on a darker band, like the class bar of a game's status sheet. */
@Composable
private fun SectionBar(text: String) {
    Spacer(Modifier.size(6.dp))
    Text(
        text = text,
        fontFamily = DisplayFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 16.sp,
        color = Ink,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(BarColor).padding(vertical = 5.dp)
    )
    Spacer(Modifier.size(2.dp))
}

/** One line of the sheet: the label on the left in the warm colour, the value on the right, an optional faded top after it. */
@Composable
private fun StatusRow(label: String, value: String, extra: String? = null, dim: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            fontFamily = DisplayFont,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = if (dim) Faded else LabelColor,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontFamily = DisplayFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp,
            color = if (dim) Faded else Ink,
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
}
