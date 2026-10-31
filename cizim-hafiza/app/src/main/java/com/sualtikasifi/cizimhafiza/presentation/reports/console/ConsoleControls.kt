package com.sualtikasifi.cizimhafiza.presentation.reports.console

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** What a button is for: the one main action, a lesser one beside it, a quiet one, or one that destroys something. */
enum class ConsoleButtonStyle { Primary, Secondary, Tonal, Dark, Danger, DangerSoft, Ghost }

private class ButtonColors(val bg: Color, val fg: Color, val border: Color?)

private fun colorsOf(style: ConsoleButtonStyle): ButtonColors = when (style) {
    ConsoleButtonStyle.Primary -> ButtonColors(Console.Accent, Color.White, null)
    ConsoleButtonStyle.Secondary -> ButtonColors(Console.Surface, Console.Ink, Console.BorderStrong)
    ConsoleButtonStyle.Tonal -> ButtonColors(Console.AccentSoft, Console.AccentInk, null)
    ConsoleButtonStyle.Dark -> ButtonColors(Console.Dark, Color.White, null)
    ConsoleButtonStyle.Danger -> ButtonColors(Console.Critical, Color.White, null)
    ConsoleButtonStyle.DangerSoft -> ButtonColors(Console.CriticalSoft, Console.Critical, null)
    ConsoleButtonStyle.Ghost -> ButtonColors(Color.Transparent, Console.AccentInk, null)
}

/**
 * The console's button: flat, 44dp by default, a ripple and a small spring under the finger. Takes its arguments in
 * the same order as the game's PrimaryButton, so a panel moves over by changing only the name. [loading] swaps the
 * icon for a spinner and holds the button still.
 */
@Composable
fun ConsoleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    height: Dp = 44.dp,
    style: ConsoleButtonStyle = ConsoleButtonStyle.Primary,
    loading: Boolean = false
) {
    val interaction = remember { MutableInteractionSource() }
    val live = enabled && !loading
    val c = colorsOf(style)
    val bg by animateColorAsState(
        when {
            live -> c.bg
            style == ConsoleButtonStyle.Ghost -> Color.Transparent
            loading -> c.bg.copy(alpha = 0.75f)
            else -> Console.Grid
        },
        tween(160), label = "btnBg"
    )
    val fg by animateColorAsState(if (live || loading) c.fg else Console.InkMuted, tween(160), label = "btnFg")
    val h = height.coerceIn(30.dp, 52.dp)
    val shape = RoundedCornerShape(if (h >= 48.dp) 12.dp else Console.ControlCorner)
    Box(
        modifier = modifier
            .pressScale(interaction, 0.96f)
            .height(h)
            .clip(shape)
            .background(bg)
            .then(if (c.border != null && live) Modifier.border(1.dp, c.border, shape) else Modifier)
            .clickable(interaction, ripple(color = if (c.bg == Console.Surface || c.bg == Color.Transparent) Console.Accent else Color.White), enabled = live, onClick = onClick)
            .padding(horizontal = if (h < 38.dp) 12.dp else 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            val glyph = if (h < 38.dp) 16.dp else 18.dp
            if (loading) {
                CircularProgressIndicator(Modifier.size(glyph - 2.dp), color = fg, strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            } else if (icon != null) {
                Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(glyph))
                Spacer(Modifier.width(7.dp))
            }
            Text(
                text,
                style = Console.Button.copy(color = fg, fontSize = if (h < 38.dp) 13.sp else 14.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (trailingIcon != null) {
                Spacer(Modifier.width(6.dp))
                Icon(trailingIcon, contentDescription = null, tint = fg, modifier = Modifier.size(glyph))
            }
        }
    }
}

/** The lesser of two actions side by side: white with a border. Same argument order as the game's SecondaryButton. */
@Composable
fun ConsoleSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    height: Dp = 44.dp
) = ConsoleButton(text, onClick, modifier, enabled, icon, null, height, ConsoleButtonStyle.Secondary)

/** A square icon button: header actions, row actions. Quiet until pressed. */
@Composable
fun ConsoleIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    size: Dp = 40.dp,
    corner: Dp = Console.ControlCorner,
    tint: Color = Console.InkSecondary,
    bordered: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(corner)
    val bg by animateColorAsState(if (selected) Console.AccentSoft else Console.Surface, tween(160), label = "iconBtnBg")
    Box(
        modifier = modifier
            .pressScale(interaction, 0.9f)
            .size(size)
            .clip(shape)
            .background(bg)
            .then(if (bordered) Modifier.border(1.dp, if (selected) Console.Accent.copy(alpha = 0.5f) else Console.Border, shape) else Modifier)
            .clickable(interaction, ripple(color = Console.Accent), enabled = enabled, onClickLabel = contentDescription, onClick = onClick)
            .alpha(if (enabled) 1f else 0.4f),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = if (selected) Console.AccentInk else tint, modifier = Modifier.size(size * 0.48f))
    }
}

/**
 * A choice chip: amounts, durations, filters. A picked chip turns accent and a check slides in before its label.
 * Same arguments as the game's SelectableChip; the paddings it is given are kept within a compact height.
 */
@Composable
fun ConsoleChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 12.dp,
    verticalPadding: Dp = 7.dp,
    style: TextStyle? = null,
    maxLines: Int = 1,
    fillWidth: Boolean = false,
    accent: Color = Console.Accent,
    showCheck: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(50)
    val bg by animateColorAsState(if (selected) accent.copy(alpha = 0.12f).compositeOverWhite() else Console.Surface, tween(160), label = "chipBg")
    val border by animateColorAsState(if (selected) accent else Console.BorderStrong, tween(160), label = "chipBorder")
    val fg by animateColorAsState(if (selected) Console.Ink else Console.InkSecondary, tween(160), label = "chipFg")
    Row(
        modifier = modifier
            .pressScale(interaction, 0.94f)
            .clip(shape)
            .background(bg)
            .border(if (selected) 1.5.dp else 1.dp, border, shape)
            .clickable(interaction, ripple(color = accent), onClick = onClick)
            .defaultMinSize(minHeight = 32.dp)
            .padding(horizontal = horizontalPadding.coerceIn(8.dp, 14.dp), vertical = verticalPadding.coerceIn(4.dp, 8.dp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        AnimatedVisibility(
            visible = selected && showCheck,
            enter = fadeIn(tween(140)) + expandHorizontally(tween(180)),
            exit = fadeOut(tween(100)) + shrinkHorizontally(tween(160))
        ) {
            Row {
                Icon(Icons.Filled.Check, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
            }
        }
        Text(
            label,
            style = (style ?: Console.Label).copy(color = fg, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, fontSize = (style?.fontSize?.takeIf { it.value > 0 } ?: 13.sp)),
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (fillWidth) TextAlign.Center else null,
            modifier = if (fillWidth) Modifier.weight(1f, fill = false) else Modifier
        )
    }
}

private fun Color.compositeOverWhite(): Color = Color(
    red = red * alpha + (1f - alpha),
    green = green * alpha + (1f - alpha),
    blue = blue * alpha + (1f - alpha),
    alpha = 1f
)

/**
 * The console's text input: a label above (never floating into the border), a white field with a hairline that
 * turns accent with a soft ring while typing. Same arguments, in the same order, as the game's AppTextField.
 */
@Composable
fun ConsoleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    minLines: Int = 1,
    centered: Boolean = false,
    textStyle: TextStyle? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    focusRequester: FocusRequester? = null,
    corner: Dp = Console.ControlCorner,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    supporting: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(corner.coerceAtMost(14.dp))
    val border by animateColorAsState(if (focused) Console.Accent else Console.BorderStrong, tween(150), label = "fieldBorder")
    val ring by animateColorAsState(if (focused) Console.Accent.copy(alpha = 0.16f) else Color.Transparent, tween(150), label = "fieldRing")
    val resolved = (textStyle ?: Console.Body.copy(fontSize = 14.5.sp)).let { s ->
        // a caller's own style keeps its size and weight; color, face and alignment are the console's
        s.copy(color = Console.Ink, fontFamily = Console.Body.fontFamily, textAlign = if (centered) TextAlign.Center else TextAlign.Start)
    }
    Column(modifier = modifier.alpha(if (enabled) 1f else 0.55f)) {
        if (label != null) {
            Text(label, style = Console.Label, modifier = Modifier.padding(start = 2.dp, bottom = 5.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            textStyle = resolved,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interaction,
            visualTransformation = visualTransformation,
            cursorBrush = SolidColor(Console.Accent),
            modifier = Modifier
                .fillMaxWidth()
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .border(3.dp, ring, RoundedCornerShape(corner.coerceAtMost(14.dp) + 2.dp))
                .padding(2.dp)
                .clip(shape)
                .background(Console.Surface)
                .border(if (focused) 1.5.dp else 1.dp, border, shape)
                .defaultMinSize(minHeight = 42.dp),
            decorationBox = { field ->
                Row(
                    verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    if (leading != null) {
                        leading()
                        Spacer(Modifier.width(8.dp))
                    }
                    Box(Modifier.weight(1f), contentAlignment = if (centered) Alignment.Center else Alignment.CenterStart) {
                        if (value.isEmpty() && placeholder != null) {
                            Text(
                                placeholder,
                                style = resolved.copy(color = Console.InkMuted),
                                maxLines = if (singleLine) 1 else 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = if (centered) Modifier.fillMaxWidth() else Modifier
                            )
                        }
                        field()
                    }
                    if (trailing != null) {
                        Spacer(Modifier.width(8.dp))
                        trailing()
                    }
                }
            }
        )
        if (supporting != null) {
            Text(supporting, style = Console.Small.copy(fontSize = 11.sp), modifier = Modifier.padding(start = 2.dp, top = 4.dp))
        }
    }
}

/**
 * A plain white container with a hairline border, for rows that lay out their own content in a Box (the game's
 * RaisedCard's place in the panel). [border] tints the hairline, e.g. the picked row of a list.
 */
@Composable
fun ConsoleSurface(
    modifier: Modifier = Modifier,
    corner: Dp = Console.CardCorner,
    face: Color = Console.Surface,
    border: Color? = Console.Border,
    contentColor: Color = Console.Ink,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(corner.coerceAtMost(16.dp))
    val edge by animateColorAsState(border ?: Color.Transparent, tween(160), label = "surfaceBorder")
    Box(
        modifier = modifier
            .then(if (onClick != null) Modifier.pressScale(interaction, 0.985f) else Modifier)
            .clip(shape)
            .background(face)
            .border(if (border != null && border != Console.Border) 1.5.dp else 1.dp, edge, shape)
            .then(if (onClick != null) Modifier.clickable(interaction, ripple(color = Console.Accent), onClick = onClick) else Modifier)
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) { content() }
    }
}

enum class NoticeKind { Info, Success, Warning, Error }

/** A one-line (or few-line) message inside a panel: what happened, in the color of how it went. */
@Composable
fun ConsoleNotice(text: String, modifier: Modifier = Modifier, kind: NoticeKind = NoticeKind.Info) {
    val (fg, bg, icon) = when (kind) {
        NoticeKind.Info -> Triple(Console.Blue, Console.BlueSoft, Icons.Filled.Info)
        NoticeKind.Success -> Triple(Console.GoodText, Console.GoodSoft, Icons.Filled.CheckCircle)
        NoticeKind.Warning -> Triple(Console.WarningText, Console.WarningSoft, Icons.Filled.WarningAmber)
        NoticeKind.Error -> Triple(Console.Critical, Console.CriticalSoft, Icons.Filled.ErrorOutline)
    }
    Row(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(Console.ControlCorner)).background(bg).padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(9.dp))
        Text(text, style = Console.Small.copy(color = fg, fontWeight = FontWeight.Medium, fontSize = 12.5.sp), modifier = Modifier.weight(1f))
    }
}

/** Guesses how a free-form status line went from its words, for panels that only keep the text. */
fun noticeKindOf(message: String): NoticeKind {
    val m = message.lowercase(java.util.Locale("tr"))
    return when {
        listOf("yazılamadı", "okunamadı", "hata", "başarısız", "olmadı", "gönderilemedi", "yetki", "olmalı", "bulunamadı").any { it in m } -> NoticeKind.Error
        listOf("kaydedildi", "gönderildi", "yayınlandı", "oluşturuldu", "tamam", "silindi", "açıldı", "kapatıldı", "başladı", "planlandı", "uygulandı", "kilitlendi", "sıfırlandı").any { it in m } -> NoticeKind.Success
        else -> NoticeKind.Info
    }
}

/** Nothing to show: an icon, a line saying so, and what to do about it. */
@Composable
fun ConsoleEmpty(icon: ImageVector, title: String, modifier: Modifier = Modifier, body: String? = null, action: (@Composable () -> Unit)? = null) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(Console.Grid), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Console.InkMuted, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(title, style = Console.Subtitle.copy(fontSize = 15.sp), textAlign = TextAlign.Center)
        if (body != null) {
            Spacer(Modifier.height(4.dp))
            Text(body, style = Console.Small, textAlign = TextAlign.Center)
        }
        if (action != null) {
            Spacer(Modifier.height(14.dp))
            action()
        }
    }
}

/** Grey bars where rows are about to be, with a light sweeping across them: loading that shows the shape to come. */
@Composable
fun ConsoleSkeleton(modifier: Modifier = Modifier, rows: Int = 4, rowHeight: Dp = 72.dp) {
    val t = rememberInfiniteTransition(label = "skeleton")
    val x by t.animateFloat(-1f, 2f, infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Restart), label = "sweep")
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Console.Gap)) {
        repeat(rows) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(rowHeight)
                    .clip(RoundedCornerShape(Console.CardCorner))
                    .background(Console.Surface)
                    .border(1.dp, Console.Border, RoundedCornerShape(Console.CardCorner))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Bar(0.45f, x)
                    Bar(0.8f, x)
                    if (rowHeight > 64.dp) Bar(0.6f, x)
                }
            }
        }
    }
}

@Composable
private fun Bar(fraction: Float, sweep: Float) {
    Box(
        Modifier
            .fillMaxWidth(fraction)
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .drawWithContent {
                drawRect(Console.Grid)
                val w = size.width
                drawRect(
                    Brush.linearGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.75f), Color.Transparent),
                        start = Offset(w * (sweep - 0.4f), 0f),
                        end = Offset(w * sweep, 0f)
                    )
                )
            }
    )
}

/** A setting that is on or off: what it is, a line of why, and the switch. */
@Composable
fun ConsoleSwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier, subtitle: String? = null, enabled: Boolean = true) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Console.ControlCorner))
            .clickable(enabled = enabled) { onChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = Console.Subtitle.copy(fontSize = 13.5.sp))
            if (subtitle != null) Text(subtitle, style = Console.Small)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled, colors = consoleSwitchColors())
    }
}

@Composable
fun consoleSwitchColors() = SwitchDefaults.colors(
    checkedTrackColor = Console.Accent,
    checkedThumbColor = Color.White,
    checkedBorderColor = Console.Accent,
    uncheckedTrackColor = Console.Grid,
    uncheckedThumbColor = Console.InkMuted,
    uncheckedBorderColor = Console.BorderStrong
)

/** − count + : a small number nudged up and down. */
@Composable
fun ConsoleStepper(value: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier, min: Int = Int.MIN_VALUE, max: Int = Int.MAX_VALUE) {
    Row(
        modifier = modifier.clip(RoundedCornerShape(Console.ControlCorner)).border(1.dp, Console.BorderStrong, RoundedCornerShape(Console.ControlCorner)).background(Console.Surface),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepCell("−", value > min) { onChange(value - 1) }
        Box(Modifier.width(1.dp).height(20.dp).background(Console.Border))
        Text(value.toString(), style = Console.Subtitle, textAlign = TextAlign.Center, modifier = Modifier.width(40.dp))
        Box(Modifier.width(1.dp).height(20.dp).background(Console.Border))
        StepCell("+", value < max) { onChange(value + 1) }
    }
}

@Composable
private fun StepCell(symbol: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(36.dp).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(symbol, style = Console.Title.copy(color = if (enabled) Console.AccentInk else Console.InkMuted)) }
}

/** A card with a small heading over its content: the shape of every settings block in the panel. */
@Composable
fun ConsoleSection(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    ConsoleCard(modifier = modifier.fillMaxWidth(), padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = Console.Subtitle.copy(fontSize = 14.5.sp))
                if (subtitle != null) Text(subtitle, style = Console.Small)
            }
            trailing?.invoke()
        }
        Spacer(Modifier.height(10.dp))
        content()
    }
}
