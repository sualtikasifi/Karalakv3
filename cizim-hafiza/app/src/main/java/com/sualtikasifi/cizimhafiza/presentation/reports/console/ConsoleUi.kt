package com.sualtikasifi.cizimhafiza.presentation.reports.console

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.abs

/**
 * The developer panel's own look: a quiet, neutral console instead of the game's painted paper, so
 * numbers and decisions read at a glance. Plain system sans throughout (the game's display face is
 * for players), one brand accent, status colors kept for status only.
 */
object Console {
    val Bg = Color(0xFFF4F2EE)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceAlt = Color(0xFFFAF8F5)
    val Border = Color(0xFFE6E1D9)
    val Grid = Color(0xFFEDE9E2)
    val Ink = Color(0xFF1E1B18)
    val InkSecondary = Color(0xFF5E574F)
    val InkMuted = Color(0xFF8F877D)
    val Accent = Color(0xFFEB6834)
    val AccentSoft = Color(0xFFFDEBE2)
    val Blue = Color(0xFF2A78D6)
    val BlueSoft = Color(0xFFE3EEFB)
    val Good = Color(0xFF0CA30C)
    val GoodText = Color(0xFF0B7A0B)
    val GoodSoft = Color(0xFFE2F4E2)
    val Critical = Color(0xFFD03B3B)
    val CriticalSoft = Color(0xFFFBE5E5)
    val Warning = Color(0xFFFAB219)
    val Dark = Color(0xFF221E1A)

    private val Sans = FontFamily.SansSerif
    val Hero = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, color = Ink, letterSpacing = (-0.5).sp)
    val Title = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = Ink)
    val Subtitle = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Ink)
    val Body = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 14.sp, color = Ink, lineHeight = 20.sp)
    val Small = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 12.sp, color = InkSecondary, lineHeight = 16.sp)
    val Label = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = InkSecondary, letterSpacing = 0.2.sp)
    val Value = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, color = Ink)
    val Tick = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 10.sp, color = InkMuted)

    /** 1,284 / 12.9B / 4.2M — compact from ten thousand up, Turkish separators. */
    fun compact(n: Double): String {
        val a = abs(n)
        return when {
            a >= 1_000_000 -> String.format(Locale("tr"), "%.1fM", n / 1_000_000)
            a >= 10_000 -> String.format(Locale("tr"), "%.1fB", n / 1_000)
            a >= 100 || n == Math.floor(n) -> String.format(Locale("tr"), "%,.0f", n)
            else -> String.format(Locale("tr"), "%.1f", n)
        }
    }

    fun percent(fraction: Double): String = String.format(Locale("tr"), "%%%.0f", fraction * 100)
}

/** A white card with a hairline border: the one container every console panel uses. */
@Composable
fun ConsoleCard(
    modifier: Modifier = Modifier,
    padding: androidx.compose.ui.unit.Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.then(if (onClick != null) Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(16.dp),
        color = Console.Surface,
        border = BorderStroke(1.dp, Console.Border)
    ) {
        Column(modifier = Modifier.padding(padding), content = content)
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, subtitle: String? = null, trailing: @Composable (() -> Unit)? = null) {
    Row(modifier = modifier.fillMaxWidth().padding(top = 6.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = Console.Title)
            if (subtitle != null) Text(subtitle, style = Console.Small)
        }
        trailing?.invoke()
    }
}

/** Signed change against a named period; the color says whether the direction is good, the arrow says which way. */
@Composable
fun DeltaBadge(change: Double?, upIsGood: Boolean = true, modifier: Modifier = Modifier) {
    if (change == null || change.isNaN() || change.isInfinite()) {
        Text("—", style = Console.Small, modifier = modifier)
        return
    }
    val up = change >= 0
    val good = up == upIsGood
    val fg = if (abs(change) < 0.005) Console.InkSecondary else if (good) Console.GoodText else Console.Critical
    val bg = if (abs(change) < 0.005) Console.Grid else if (good) Console.GoodSoft else Console.CriticalSoft
    Row(
        modifier = modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(if (up) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward, contentDescription = null, tint = fg, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(2.dp))
        Text(Console.percent(abs(change)), style = Console.Label.copy(color = fg, fontWeight = FontWeight.SemiBold))
    }
}

/**
 * A stat tile: label, value, optional delta vs a named period, optional 12-ish point trend.
 * [change] is a fraction (0.12 = +12%).
 */
@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    change: Double? = null,
    changeCaption: String? = null,
    upIsGood: Boolean = true,
    trend: List<Float>? = null,
    trendColor: Color = Console.Accent,
    onClick: (() -> Unit)? = null
) {
    ConsoleCard(modifier = modifier, padding = 14.dp, onClick = onClick) {
        Text(label, style = Console.Label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        Text(value, style = Console.Value, maxLines = 1)
        if (change != null || changeCaption != null) {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (change != null) DeltaBadge(change, upIsGood)
                if (changeCaption != null) {
                    Spacer(Modifier.width(6.dp))
                    Text(changeCaption, style = Console.Small, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (trend != null && trend.size >= 2) {
            Spacer(Modifier.height(10.dp))
            Sparkline(trend, color = trendColor, modifier = Modifier.fillMaxWidth().height(28.dp))
        }
    }
}

/** A small rounded tag (counts, states). */
@Composable
fun Tag(text: String, fg: Color = Console.InkSecondary, bg: Color = Console.Grid, modifier: Modifier = Modifier) {
    Text(
        text,
        style = Console.Label.copy(color = fg, fontWeight = FontWeight.SemiBold),
        modifier = modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 8.dp, vertical = 3.dp),
        maxLines = 1
    )
}

/** One entry of the tools grid: icon, name, one-line purpose, optional count. */
@Composable
fun ToolTile(icon: ImageVector, title: String, subtitle: String, modifier: Modifier = Modifier, badge: String? = null, onClick: () -> Unit) {
    ConsoleCard(modifier = modifier, padding = 12.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Console.AccentSoft),
                contentAlignment = Alignment.Center
            ) { Icon(icon, contentDescription = null, tint = Console.Accent, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.weight(1f))
            if (badge != null) Tag(badge, fg = Color.White, bg = Console.Accent)
        }
        Spacer(Modifier.height(10.dp))
        Text(title, style = Console.Subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(subtitle, style = Console.Small, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

data class ConsoleNavItem(val key: String, val label: String, val icon: ImageVector, val badge: Int = 0)

/** The panel's bottom bar: four main places and the tools drawer. */
@Composable
fun ConsoleNavBar(items: List<ConsoleNavItem>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Surface(color = Console.Surface, border = BorderStroke(1.dp, Console.Border), shape = RoundedCornerShape(20.dp), modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceAround) {
            items.forEach { item ->
                val on = item.key == selected
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (on) Console.AccentSoft else Color.Transparent)
                        .clickable { onSelect(item.key) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box {
                        Icon(item.icon, contentDescription = item.label, tint = if (on) Console.Accent else Console.InkSecondary, modifier = Modifier.size(22.dp))
                        if (item.badge > 0) {
                            Box(
                                modifier = Modifier.align(Alignment.TopEnd).padding(start = 14.dp).size(16.dp).clip(CircleShape)
                                    .background(Console.Critical).border(1.5.dp, Console.Surface, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (item.badge > 99) "99+" else item.badge.toString(), style = Console.Tick.copy(color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(item.label, style = Console.Label.copy(color = if (on) Console.Accent else Console.InkSecondary, fontSize = 11.sp), maxLines = 1)
                }
            }
        }
    }
}

/** Segmented choice (time ranges, modes). */
@Composable
fun Segmented(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.clip(RoundedCornerShape(12.dp)).background(Console.Grid).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        options.forEach { (key, label) ->
            val on = key == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (on) Console.Surface else Color.Transparent)
                    .clickable { onSelect(key) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label, style = Console.Label.copy(color = if (on) Console.Ink else Console.InkSecondary, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium), maxLines = 1)
            }
        }
    }
}
