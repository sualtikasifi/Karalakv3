package com.sualtikasifi.cizimhafiza.presentation.reports

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.presentation.common.MAX_CODE_INPUT_LENGTH
import com.sualtikasifi.cizimhafiza.presentation.common.REPORTS_ACCESS_CODE
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Console
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleBackdrop
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleButton
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleTheme
import com.sualtikasifi.cizimhafiza.presentation.reports.console.HeaderIconButton
import com.sualtikasifi.cizimhafiza.presentation.reports.console.pressScale
import kotlinx.coroutines.launch

/**
 * The passcode in front of the developer panel.
 *
 * Reports carry other players' drawings and the uid of whoever was reported, which is nobody else's business — and
 * unlike Bot Eğitim this screen has to stay reachable in a shipping build, since reports arrive while the game is
 * live. The tap-the-version door that leads here (see SettingsScreen, DEVELOPER_REVEAL_TAPS) hides it; this stops
 * whoever stumbles through that door anyway, with its own code ([REPORTS_ACCESS_CODE]) rather than Bot Eğitim's, and
 * no title of its own — see the composable body for why.
 *
 * Deliberately NOT remembered between visits, unlike Bot Eğitim's gate: that one is unlocked once by someone about to
 * spend hours training, while this is opened for a minute now and then, and a phone that has been unlocked once
 * should not hand the inbox to whoever picks it up next.
 *
 * Wraps the screen rather than living inside it so [DrawingReportsViewModel] — and the Firestore read its init kicks
 * off — is never constructed for somebody who cannot get past this.
 *
 * The look is the console's lock screen: the panel's dark band over the whole screen, a lock, the digits typed so far
 * as dots, and a keypad of its own (the code is digits only, so the system keyboard would only cover half the screen
 * for nothing). A wrong code shakes the dots and buzzes.
 */
@Composable
fun DrawingReportsGate(onBack: () -> Unit, onBotNames: () -> Unit = {}) {
    // rememberSaveable, not remember: this composable's own NavBackStackEntry stays alive (just off-screen) while the
    // "Bot İsimleri" tool pushes BotNamesScreen on top of it, so a plain `remember` was torn down and recreated on the
    // way back — the passcode reappeared every time. Saved state survives exactly that round trip, and still resets
    // on a genuinely fresh visit.
    var unlocked by rememberSaveable { mutableStateOf(false) }

    if (unlocked) {
        DrawingReportsScreen(onBack = onBack, onBotNames = onBotNames)
        return
    }

    var entered by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    var attempts by remember { mutableIntStateOf(0) }
    val haptics = LocalHapticFeedback.current
    val shake = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val submit: () -> Unit = {
        if (REPORTS_ACCESS_CODE.isNotEmpty() && entered == REPORTS_ACCESS_CODE) {
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            unlocked = true
        } else {
            haptics.performHapticFeedback(HapticFeedbackType.Reject)
            wrong = true
            attempts++
            entered = ""
            scope.launch {
                shake.snapTo(0f)
                // a damped side-to-side: big, smaller, smaller, still
                for (x in listOf(18f, -14f, 10f, -6f, 3f, 0f)) shake.animateTo(x, tween(55))
            }
        }
    }
    val press: (Char) -> Unit = { c ->
        if (entered.length < MAX_CODE_INPUT_LENGTH) {
            haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
            entered += c
            wrong = false
        }
    }

    ConsoleTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            ConsoleBackdrop(Modifier.fillMaxSize(), artAlpha = 0.34f)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.weight(0.9f))
                // No "Geliştirici Paneli" heading here on purpose — this is the screen anyone who stumbles onto the
                // tap gesture lands on, and a title naming what it unlocks would give away the one thing the gate is
                // there to hide. The real name shows up on DrawingReportsScreen, once past this.
                LockBadge(open = false)
                Spacer(Modifier.height(18.dp))
                Text(
                    stringResource(R.string.reports_gate_label),
                    style = Console.Title.copy(color = Console.HeaderInk, fontSize = 17.sp)
                )
                Spacer(Modifier.height(18.dp))
                CodeDots(
                    count = entered.length,
                    wrong = wrong,
                    modifier = Modifier.offset { IntOffset(shake.value.dp.roundToPx(), 0) }
                )
                Box(Modifier.height(30.dp), contentAlignment = Alignment.Center) {
                    androidx.compose.animation.AnimatedVisibility(visible = wrong, enter = fadeIn(), exit = fadeOut()) {
                        Text(
                            stringResource(R.string.reports_gate_wrong) + if (attempts > 1) " · $attempts" else "",
                            style = Console.Label.copy(color = Color(0xFFFF8A80), fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
                Spacer(Modifier.weight(0.5f))
                Keypad(
                    onDigit = press,
                    onDelete = {
                        if (entered.isNotEmpty()) {
                            haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                            entered = entered.dropLast(1)
                        }
                    },
                    onClear = { entered = ""; wrong = false }
                )
                Spacer(Modifier.height(18.dp))
                ConsoleButton(
                    text = stringResource(R.string.reports_gate_enter),
                    onClick = submit,
                    enabled = REPORTS_ACCESS_CODE.isNotEmpty() && entered.isNotEmpty(),
                    icon = Icons.Filled.LockOpen,
                    height = 50.dp,
                    onDark = true,
                    modifier = Modifier.widthIn(max = 320.dp).fillMaxWidth()
                )
                Spacer(Modifier.height(20.dp))
            }
            // No title either — same reasoning as dropping the heading above.
            HeaderIconButton(
                Icons.AutoMirrored.Filled.ArrowBack, "Geri", onBack,
                modifier = Modifier.statusBarsPadding().padding(start = 12.dp, top = 11.dp)
            )
        }
    }
}

/** The lock in a glass ring, with a soft halo of the accent behind it. */
@Composable
private fun LockBadge(open: Boolean) {
    val pop = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow)) }
    Box(
        Modifier
            .size(84.dp)
            .graphicsLayer { scaleX = pop.value; scaleY = pop.value; alpha = ((pop.value - 0.6f) / 0.4f).coerceIn(0f, 1f) }
            .clip(CircleShape)
            .background(Console.Accent.copy(alpha = 0.16f))
            .border(1.dp, Console.Accent.copy(alpha = 0.35f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.size(58.dp).clip(CircleShape).background(Console.Accent),
            contentAlignment = Alignment.Center
        ) {
            Icon(if (open) Icons.Filled.LockOpen else Icons.Filled.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
        }
    }
}

/**
 * The digits typed so far, as dots that pop in one by one. How many there are to type is not shown: a row of empty
 * slots would give the code's length away. Red while a wrong code is on screen.
 */
@Composable
private fun CodeDots(count: Int, wrong: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier
            .widthIn(min = 180.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(50))
            .background(Console.HeaderFill)
            .border(1.dp, if (wrong) Color(0x99FF8A80) else Console.HeaderLine, RoundedCornerShape(50))
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        if (count == 0) {
            Text("Kodu tuşla", style = Console.Label.copy(color = Console.HeaderMuted.copy(alpha = 0.7f), fontSize = 13.sp))
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                repeat(count) { i -> androidx.compose.runtime.key(i) { PopDot(if (wrong) Color(0xFFFF8A80) else Console.HeaderInk) } }
            }
        }
    }
}

/** One typed digit: grows in with a little overshoot. */
@Composable
private fun PopDot(color: Color) {
    val scale = remember { Animatable(0f) }
    LaunchedEffect(Unit) { scale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium)) }
    Box(Modifier.size(12.dp).graphicsLayer { scaleX = scale.value; scaleY = scale.value }.clip(CircleShape).background(color))
}

/** 1–9, then clear, 0, delete: a phone's keypad, in glass keys that sink a little under the finger. */
@Composable
private fun Keypad(onDigit: (Char) -> Unit, onDelete: () -> Unit, onClear: () -> Unit) {
    val rows = listOf("123", "456", "789")
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                row.forEach { c -> Key(onClick = { onDigit(c) }) { Text(c.toString(), style = Console.Hero.copy(color = Console.HeaderInk, fontSize = 26.sp, fontWeight = FontWeight.Medium)) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Key(onClick = onClear, glass = false) { Text("Sil", style = Console.Label.copy(color = Console.HeaderMuted, fontSize = 13.sp)) }
            Key(onClick = { onDigit('0') }) { Text("0", style = Console.Hero.copy(color = Console.HeaderInk, fontSize = 26.sp, fontWeight = FontWeight.Medium)) }
            Key(onClick = onDelete, glass = false) {
                Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Son rakamı sil", tint = Console.HeaderMuted, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun Key(onClick: () -> Unit, glass: Boolean = true, content: @Composable () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier
            .pressScale(interaction, 0.9f)
            .size(68.dp)
            .clip(CircleShape)
            .then(if (glass) Modifier.background(Console.HeaderFill).border(1.dp, Console.HeaderLine, CircleShape) else Modifier)
            .clickable(interaction, ripple(color = Color.White), onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}
