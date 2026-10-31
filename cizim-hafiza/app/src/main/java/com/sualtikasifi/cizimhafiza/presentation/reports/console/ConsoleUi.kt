package com.sualtikasifi.cizimhafiza.presentation.reports.console

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.abs

/**
 * The developer panel's own look: a quiet, neutral console instead of the game's painted paper, so numbers and
 * decisions read at a glance. A dark header band carries where you are; the work sits on a light sheet under it.
 * Plain system sans throughout (the game's display face is for players), one brand accent, status colors kept for
 * status only. Everything in the panel is built from these tokens and the components in this package
 * (ConsoleControls, ConsoleShell); [ConsoleTheme] points Material's own pieces (dialogs, switches, sliders) at them too.
 */
object Console {
    // the sheet
    val Bg = Color(0xFFF4F2EE)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceAlt = Color(0xFFFAF9F6)
    val Border = Color(0xFFE6E2DA)
    val BorderStrong = Color(0xFFD5CFC5)
    val Grid = Color(0xFFEEEBE5)
    val Ink = Color(0xFF1C1916)
    val InkSecondary = Color(0xFF5E574F)
    val InkMuted = Color(0xFF948B80)

    // brand and status
    val Accent = Color(0xFFEB6834)
    val AccentStrong = Color(0xFFD5561F)
    val AccentInk = Color(0xFFB4440F)
    val AccentSoft = Color(0xFFFDECE3)
    val Blue = Color(0xFF2A78D6)
    val BlueSoft = Color(0xFFE4EEFB)
    val Violet = Color(0xFF7457D9)
    val VioletSoft = Color(0xFFEEE9FC)
    val Teal = Color(0xFF138A7E)
    val TealSoft = Color(0xFFDDF3F0)
    val Good = Color(0xFF0CA30C)
    val GoodText = Color(0xFF0B7A0B)
    val GoodSoft = Color(0xFFE2F4E2)
    val Critical = Color(0xFFD03B3B)
    val CriticalSoft = Color(0xFFFBE6E6)
    val Warning = Color(0xFFFAB219)
    val WarningText = Color(0xFF8A5A00)
    val WarningSoft = Color(0xFFFEF3D9)
    val Dark = Color(0xFF221E1A)

    // the header band and the passcode screen
    val HeaderTop = Color(0xFF14110E)
    val HeaderBottom = Color(0xFF26201B)
    val HeaderInk = Color(0xFFF8F3EC)
    val HeaderMuted = Color(0xFFB5AA9C)
    val HeaderLine = Color(0x1FFFFFFF)
    val HeaderFill = Color(0x14FFFFFF)

    private val Sans = FontFamily.SansSerif
    val Hero = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, color = Ink, letterSpacing = (-0.4).sp)
    val Title = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Ink, lineHeight = 22.sp, letterSpacing = (-0.1).sp)
    val Subtitle = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Ink, lineHeight = 19.sp)
    val Body = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 14.sp, color = Ink, lineHeight = 20.sp)
    val Small = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 12.sp, color = InkSecondary, lineHeight = 16.sp)
    val Label = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = InkSecondary, letterSpacing = 0.2.sp)
    val Overline = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = InkMuted, letterSpacing = 0.9.sp)
    val Button = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.1.sp)
    val Value = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, color = Ink, letterSpacing = (-0.3).sp)
    val Tick = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 10.sp, color = InkMuted)

    /** Corner radii: controls, cards, sheets. */
    val ControlCorner = 10.dp
    val CardCorner = 14.dp
    val SheetCorner = 22.dp

    /** The sheet's side margin and the gap between blocks on it. */
    val Gutter = 16.dp
    val Gap = 10.dp

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

/**
 * Shrinks a touch under the finger and springs back: the one press response every tappable console surface shares,
 * so cards, tiles and buttons all feel like the same material.
 */
@Composable
fun Modifier.pressScale(interaction: MutableInteractionSource, pressedScale: Float = 0.975f): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
        label = "pressScale"
    )
    return this.graphicsLayer { scaleX = scale; scaleY = scale }
}

/** A white card with a hairline border: the one container every console panel uses. */
@Composable
fun ConsoleCard(
    modifier: Modifier = Modifier,
    padding: Dp = 14.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(Console.CardCorner)
    val interaction = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier.then(
            if (onClick != null) {
                Modifier.pressScale(interaction, 0.985f).clip(shape).clickable(interaction, ripple(color = Console.Accent), onClick = onClick)
            } else Modifier
        ),
        shape = shape,
        color = Console.Surface,
        border = BorderStroke(1.dp, Console.Border)
    ) {
        Column(modifier = Modifier.padding(padding), content = content)
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, subtitle: String? = null, trailing: @Composable (() -> Unit)? = null) {
    Row(modifier = modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = Console.Subtitle.copy(fontSize = 15.sp))
            if (subtitle != null) Text(subtitle, style = Console.Small)
        }
        trailing?.invoke()
    }
}

/** A small all-caps heading over a group of controls or tiles. */
@Composable
fun Overline(text: String, modifier: Modifier = Modifier, color: Color = Console.InkMuted) {
    Text(text.uppercase(Locale("tr")), style = Console.Overline.copy(color = color), modifier = modifier, maxLines = 1)
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
        Icon(if (up) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward, contentDescription = null, tint = fg, modifier = Modifier.size(11.dp))
        Spacer(Modifier.width(2.dp))
        Text(Console.percent(abs(change)), style = Console.Label.copy(color = fg, fontWeight = FontWeight.SemiBold, fontSize = 11.sp))
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
    ConsoleCard(modifier = modifier, padding = 12.dp, onClick = onClick) {
        Text(label, style = Console.Label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        Text(value, style = Console.Value, maxLines = 1)
        if (change != null || changeCaption != null) {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (change != null) DeltaBadge(change, upIsGood)
                if (changeCaption != null) {
                    if (change != null) Spacer(Modifier.width(6.dp))
                    Text(changeCaption, style = Console.Small.copy(fontSize = 11.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (trend != null && trend.size >= 2) {
            Spacer(Modifier.height(8.dp))
            Sparkline(trend, color = trendColor, modifier = Modifier.fillMaxWidth().height(24.dp))
        }
    }
}

/** A small rounded tag (counts, states). */
@Composable
fun Tag(text: String, fg: Color = Console.InkSecondary, bg: Color = Console.Grid, modifier: Modifier = Modifier) {
    Text(
        text,
        style = Console.Label.copy(color = fg, fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
        modifier = modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 8.dp, vertical = 2.dp),
        maxLines = 1
    )
}

/** A colored square behind an icon: what a tool is, at a glance. */
@Composable
fun IconChip(icon: ImageVector, tint: Color = Console.Accent, bg: Color = Console.AccentSoft, size: Dp = 32.dp) {
    Box(
        modifier = Modifier.size(size).clip(RoundedCornerShape(size * 0.3f)).background(bg),
        contentAlignment = Alignment.Center
    ) { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.56f)) }
}

/** One entry of the tools grid: icon, name, one-line purpose, optional count. Compact: two fit side by side. */
@Composable
fun ToolTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    badge: String? = null,
    tint: Color = Console.Accent,
    tintSoft: Color = Console.AccentSoft,
    onClick: () -> Unit
) {
    ConsoleCard(modifier = modifier, padding = 10.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxHeight()) {
            IconChip(icon, tint, tintSoft, 32.dp)
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = Console.Subtitle.copy(fontSize = 13.sp, lineHeight = 16.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = Console.Small.copy(fontSize = 11.sp, lineHeight = 14.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (badge != null) {
                Spacer(Modifier.width(4.dp))
                Tag(badge, fg = Color.White, bg = Console.Accent)
            }
        }
    }
}

/** A row in a grouped list: icon, title, a line under it, a value or count, and a chevron when it leads somewhere. */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = Console.InkSecondary,
    iconBg: Color = Console.Grid,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Console.ControlCorner))
            .then(if (onClick != null) Modifier.clickable(interaction, ripple(color = Console.Accent), onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            IconChip(icon, iconTint, iconBg, 30.dp)
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = Console.Subtitle.copy(fontSize = 13.5.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, style = Console.Small, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        trailing?.invoke()
        if (onClick != null) Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Console.InkMuted, modifier = Modifier.size(20.dp))
    }
}

data class ConsoleNavItem(val key: String, val label: String, val icon: ImageVector, val badge: Int = 0)

/**
 * The panel's bottom bar: four main places and the tools drawer. Docked to the bottom edge; the place you are on gets
 * a soft accent pill behind its icon that grows in when you arrive.
 */
@Composable
fun ConsoleNavBar(items: List<ConsoleNavItem>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Surface(color = Console.Surface, modifier = modifier) {
        Column {
            Box(Modifier.fillMaxWidth().height(1.dp).background(Console.Border))
            Row(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                items.forEach { item -> NavEntry(item, item.key == selected, Modifier.weight(1f)) { onSelect(item.key) } }
            }
        }
    }
}

@Composable
private fun NavEntry(item: ConsoleNavItem, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pill by animateFloatAsState(if (on) 1f else 0f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow), label = "navPill")
    val tint by animateColorAsState(if (on) Console.AccentInk else Console.InkMuted, tween(180), label = "navTint")
    val labelColor by animateColorAsState(if (on) Console.Ink else Console.InkMuted, tween(180), label = "navLabel")
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(interaction, indication = null, onClick = onClick)
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.pressScale(interaction, 0.9f)) {
            Box(
                Modifier
                    .size(width = 56.dp, height = 30.dp)
                    .graphicsLayer { scaleX = 0.5f + 0.5f * pill; alpha = pill }
                    .clip(RoundedCornerShape(50))
                    .background(Console.AccentSoft)
            )
            Icon(item.icon, contentDescription = item.label, tint = tint, modifier = Modifier.size(21.dp))
            if (item.badge > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(x = 12.dp, y = (-1).dp)
                        .height(16.dp)
                        .clip(CircleShape)
                        .background(Console.Critical)
                        .border(1.5.dp, Console.Surface, CircleShape)
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (item.badge > 99) "99+" else item.badge.toString(), style = Console.Tick.copy(color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold))
                }
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(item.label, style = Console.Label.copy(color = labelColor, fontSize = 11.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium), maxLines = 1)
    }
}

/**
 * Segmented choice (time ranges, modes). The white thumb slides to the option picked rather than jumping, so the eye
 * follows the change.
 */
@Composable
fun Segmented(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    if (options.isEmpty()) return
    val index = options.indexOfFirst { it.first == selected }
    BoxWithConstraints(
        modifier = modifier.clip(RoundedCornerShape(Console.ControlCorner)).background(Console.Grid).padding(3.dp)
    ) {
        val gap = 3.dp
        val cell = (maxWidth - gap * (options.size - 1)) / options.size
        val x by animateDpAsState(
            targetValue = (cell + gap) * index.coerceAtLeast(0),
            animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
            label = "segThumb"
        )
        if (index >= 0) {
            Box(
                Modifier
                    .offset(x = x)
                    .width(cell)
                    .height(32.dp)
                    .shadow(1.dp, RoundedCornerShape(8.dp), clip = false, ambientColor = Color(0x22000000), spotColor = Color(0x22000000))
                    .clip(RoundedCornerShape(8.dp))
                    .background(Console.Surface)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(gap), modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, (key, label) ->
                val on = i == index
                val color by animateColorAsState(if (on) Console.Ink else Console.InkSecondary, tween(160), label = "segText")
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelect(key) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        style = Console.Label.copy(color = color, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium, fontSize = 12.5.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                }
            }
        }
    }
}

/** A thin vertical rule, for splitting a row of figures. */
@Composable
fun VRule(modifier: Modifier = Modifier) {
    Box(modifier.width(1.dp).fillMaxHeight().background(Console.Border))
}
