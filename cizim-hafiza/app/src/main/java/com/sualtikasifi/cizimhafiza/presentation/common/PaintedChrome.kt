package com.sualtikasifi.cizimhafiza.presentation.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sualtikasifi.cizimhafiza.R

/**
 * A soft shade under the status bar, so the (always light) clock and battery icons read on whatever picture a
 * screen paints behind them. Drawn once over the whole navigation host.
 */
@Composable
fun StatusBarScrim(modifier: Modifier = Modifier) {
    val top = with(LocalDensity.current) { WindowInsets.statusBars.getTop(this).toDp() }
    if (top <= 0.dp) return
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(top + 14.dp)
            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.42f), Color.Black.copy(alpha = 0.18f), Color.Transparent)))
    )
}

/** The game's back button — the painted wooden arrow every screen uses, in the same spot on every screen. */
@Composable
fun PaintedBackButton(onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 54.dp) {
    WoodBackArrow(onClick = onClick, size = size, modifier = modifier.statusBarsPadding().padding(start = 14.dp, top = 12.dp))
}

/** Just the wooden arrow, for layouts that place it themselves (see ScreenTopActions). */
@Composable
fun WoodBackArrow(onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 54.dp) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, label = "back")
    Image(
        painter = painterResource(R.drawable.join_back),
        contentDescription = stringResource(R.string.cd_back),
        modifier = modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    )
}

/** A painted pill button: orange with white lettering ([primary]) or cream with brown lettering. */
@Composable
fun PaintedPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    enabled: Boolean = true,
    height: Dp = 52.dp,
    textSize: androidx.compose.ui.unit.TextUnit = 18.sp,
    danger: Boolean = false
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) 0.95f else 1f, label = "pill")
    NinePatch(
        res = if (primary) R.drawable.offline_pill_on else R.drawable.offline_pill_off,
        slicePx = 90,
        edge = height / 2,
        tint = when {
            !enabled -> androidx.compose.ui.graphics.ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(0f) })
            danger && primary -> androidx.compose.ui.graphics.ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix(hueShiftToRed))
            else -> null
        },
        modifier = modifier
            .height(height)
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) 1f else 0.7f }
            .clickable(enabled = enabled, interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        if (primary) {
            LetteredText(
                text = text,
                size = textSize,
                outline = if (danger) Color(0xFF7A1410) else Color(0xFF8A3A00),
                modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 14.dp)
            )
        } else {
            FitText(
                text = text,
                style = PaintedStyle(color = if (danger) Color(0xFFC62828) else InkBrown, fontSize = textSize, textAlign = TextAlign.Center),
                modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 14.dp)
            )
        }
    }
}

/** Shifts the orange pill towards a warning red for destructive actions. */
private val hueShiftToRed = floatArrayOf(
    1.05f, 0f, 0f, 0f, 0f,
    0f, 0.45f, 0f, 0f, 0f,
    0f, 0f, 0.45f, 0f, 0f,
    0f, 0f, 0f, 1f, 0f
)

/**
 * The game's window: a parchment panel in a wooden frame over a dimmed screen, with a lettered title, a body and
 * up to two painted buttons. Every confirmation and info window uses it, so none of them falls back to the stock
 * Android dialog look.
 */
@Composable
fun PaintedDialog(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissOnOutsideTap: Boolean = true,
    buttons: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { if (dismissOnOutsideTap) onDismiss() }
                )
                .padding(horizontal = 20.dp, vertical = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            NinePatch(
                res = R.drawable.league_card,
                slicePx = 100,
                edge = 24.dp,
                modifier = modifier
                    .widthIn(max = 460.dp)
                    .fillMaxWidth()
                    .heightIn(max = maxHeight)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    FitText(
                        text = title,
                        style = PaintedStyle(color = Color(0xFF3A2416), fontSize = 24.sp, textAlign = TextAlign.Center),
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    Column(
                        modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        content = content
                    )
                    if (buttons != null) {
                        Spacer(Modifier.height(16.dp))
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp), content = buttons)
                    }
                }
            }
        }
    }
}

/** Body text for a [PaintedDialog]. */
@Composable
fun PaintedDialogText(text: String, modifier: Modifier = Modifier) {
    androidx.compose.material3.Text(
        text = text,
        style = DescriptionStyle(16.sp, 22.sp),
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * A yes/no question in the game's window. [confirmText] is the main (orange, or red when [destructive]) button,
 * [dismissText] the quiet one; both sit side by side when they fit.
 */
@Composable
fun PaintedConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissText: String? = null,
    destructive: Boolean = false,
    onDismissButton: () -> Unit = onDismiss,
    extra: (@Composable ColumnScope.() -> Unit)? = null
) {
    PaintedDialog(
        title = title,
        onDismiss = onDismiss,
        buttons = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (dismissText != null) {
                    PaintedPillButton(text = dismissText, onClick = onDismissButton, primary = false, modifier = Modifier.weight(1f))
                }
                PaintedPillButton(text = confirmText, onClick = onConfirm, danger = destructive, modifier = Modifier.weight(1f))
            }
        }
    ) {
        PaintedDialogText(message)
        if (extra != null) {
            Spacer(Modifier.height(8.dp))
            extra()
        }
    }
}
