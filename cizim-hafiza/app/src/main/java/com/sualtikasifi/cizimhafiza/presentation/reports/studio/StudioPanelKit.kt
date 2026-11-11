package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import kotlinx.coroutines.launch
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sualtikasifi.cizimhafiza.presentation.common.StrokeCanvas
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Console
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleButton
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleButtonStyle
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleTextField
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Segmented
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Tag
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.audio.ElevenLabs
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.audio.VoiceModel
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MascotEmotion
import java.util.Locale

// The small controls the studio panel is made of (steps, thumbnails, sliders, buttons, fields, the voice settings and the line editor).

/** ElevenLabs: the key (kept on this phone only), the voice, the model and how lively it speaks. */
@Composable
internal fun ElevenSettings(st: StudioState, vm: StudioViewModel) {
    val e = st.eleven
    var draft by remember { mutableStateOf("") }
    Spacer(Modifier.height(6.dp))
    if (e.keySet) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Anahtar kayıtlı · …${e.keyHint}", style = Console.Small, modifier = Modifier.weight(1f))
            SmallLink("Değiştir", Console.Critical) { vm.setElevenKey("") }
        }
    } else {
        Field("ElevenLabs API anahtarı", draft, { draft = it }, secret = true)
        Text("Yalnızca bu telefonda saklanır; uygulamaya ya da sunucuya gitmez.", style = Console.Small, modifier = Modifier.padding(top = 4.dp))
        SmallLink("Kaydet") { vm.setElevenKey(draft); draft = "" }
    }

    Text("Model", style = Console.Label, modifier = Modifier.padding(top = 8.dp))
    Segmented(
        options = VoiceModel.entries.map { it.name to it.label },
        selected = e.model.name,
        onSelect = { vm.setElevenModel(VoiceModel.valueOf(it)) },
        modifier = Modifier.fillMaxWidth()
    )
    if (e.model == VoiceModel.V3) {
        Text("Canlılık", style = Console.Label, modifier = Modifier.padding(top = 8.dp))
        Segmented(
            options = listOf("0.0" to "Yaratıcı", "0.5" to "Doğal", "1.0" to "Sabit"),
            selected = when { e.stability < 0.25f -> "0.0"; e.stability > 0.75f -> "1.0"; else -> "0.5" },
            onSelect = { vm.setElevenStability(it.toFloat()) },
            modifier = Modifier.fillMaxWidth()
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Duyguyla konuş", style = Console.Subtitle)
                Text("Karalak'ın yüz ifadesi sese de geçer: gülerken güler, şaşkınken şaşırır.", style = Console.Small)
            }
            Switch(checked = e.tags, onCheckedChange = vm::setElevenTags, colors = SwitchDefaults.colors(checkedTrackColor = Console.Accent))
        }
    } else {
        LabeledSlider("Kararlılık", e.stability, 0f..1f) { vm.setElevenStability(it) }
    }

    Text("Ses", style = Console.Label, modifier = Modifier.padding(top = 8.dp))
    val choices = (e.voices + ElevenLabs.PRESETS).distinctBy { it.id }
    choices.take(24).chunked(2).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
            row.forEach { v ->
                val on = v.id == e.voiceId
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (on) Console.AccentSoft else Console.Surface,
                    border = BorderStroke(if (on) 1.5.dp else 1.dp, if (on) Console.Accent else Console.Border),
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).clickable { vm.setElevenVoice(v.id) }
                ) {
                    Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text(v.name, style = Console.Subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(v.note, style = Console.Small, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
    Field("Ses ID (Voice Design ile yaptığın sesin ID'si)", e.voiceId, vm::setElevenVoice)
    e.status?.let { Text(it, style = Console.Small, modifier = Modifier.padding(top = 4.dp)) }

    ElevenVoicing(st, vm)
}

/** The one place a whole script is sent to ElevenLabs, and only on a tap: how many lines are voiced, and the button for the rest. */
@Composable
internal fun ElevenVoicing(st: StudioState, vm: StudioViewModel) {
    val e = st.eleven
    // the only place a whole script is sent to ElevenLabs, and only on a tap
    if (e.ready + e.missing > 0) {
        Text("Seslendirme: ${e.ready} cümle hazır · ${e.missing} eksik", style = Console.Subtitle, modifier = Modifier.padding(top = 10.dp))
        if (e.missing > 0) {
            Spacer(Modifier.height(6.dp))
            DarkButton(
                text = e.progress ?: "Seslendir (~${e.missingCredits} kredi)",
                busy = e.busy,
                enabled = e.keySet && !e.busy
            ) { vm.voiceAll() }
        }
        Text(
            "Kredi yalnızca bu düğmeye ya da bir cümlenin hoparlörüne (onay verince) basınca harcanır. Video oluşturmak kredi harcamaz; değişen cümle yalnızca kendisi yeniden seslendirilir.",
            style = Console.Small, modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
internal fun LineEditor(line: SpokenLine, onText: (String) -> Unit, onEmotion: (MascotEmotion) -> Unit, onListen: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box {
            Tag(line.emotion.label, fg = Console.Accent, bg = Console.AccentSoft, modifier = Modifier.clip(RoundedCornerShape(50)).clickable { open = true })
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                MascotEmotion.entries.forEach { e ->
                    DropdownMenuItem(text = { Text(e.label) }, onClick = { onEmotion(e); open = false })
                }
            }
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onListen) { Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Dinle", tint = Console.InkSecondary, modifier = Modifier.size(18.dp)) }
    }
    Field(null, line.text, onText, lines = 2)
}

@Composable
internal fun Step(number: String, title: String, trailing: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
        Box(Modifier.size(22.dp).clip(CircleShape).background(Console.Dark), contentAlignment = Alignment.Center) {
            Text(number, style = Console.Label.copy(color = Color.White))
        }
        Spacer(Modifier.width(8.dp))
        Text(title, style = Console.Subtitle, modifier = Modifier.weight(1f))
        trailing?.let { Text(it, style = Console.Small) }
    }
}

@Composable
internal fun Thumb(d: StudioDrawing, order: Int, modifier: Modifier, onClick: () -> Unit) {
    val on = order >= 0
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Console.Surface,
        border = BorderStroke(if (on) 2.dp else 1.dp, if (on) Console.Accent else Console.Border),
        modifier = modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick)
    ) {
        Column {
            Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
                StrokeCanvas(strokes = d.strokes, modifier = Modifier.fillMaxSize().padding(6.dp), strokeWidthPx = 4f)
                if (on) {
                    Box(
                        Modifier.padding(5.dp).size(22.dp).clip(CircleShape).background(Console.Accent).align(Alignment.TopEnd),
                        contentAlignment = Alignment.Center
                    ) { Text("${order + 1}", style = Console.Label.copy(color = Color.White)) }
                }
            }
            Text(d.word, style = Console.Label, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 6.dp))
        }
    }
}

/** A slider with a − and a + at its ends that move it by [STEP] a press (and keep going while held), for fine settings. */
@Composable
internal fun PreciseSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    val current by androidx.compose.runtime.rememberUpdatedState(value)
    val change by androidx.compose.runtime.rememberUpdatedState(onChange)
    fun nudge(direction: Int) {
        val next = ((current * 100f).let { kotlin.math.round(it) } + direction) / 100f
        change(next.coerceIn(range.start, range.endInclusive))
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
        Text(label, style = Console.Small, modifier = Modifier.width(78.dp))
        StepButton("−") { nudge(-1) }
        Slider(
            value = value, onValueChange = { change((kotlin.math.round(it * 100f)) / 100f) }, valueRange = range,
            colors = SliderDefaults.colors(thumbColor = Console.Accent, activeTrackColor = Console.Accent),
            modifier = Modifier.weight(1f)
        )
        StepButton("+") { nudge(1) }
        Text(String.format(Locale.US, "%.2f", value), style = Console.Small, modifier = Modifier.width(38.dp), textAlign = TextAlign.End)
    }
}

private const val STEP = 0.01f

/** A small square button that acts on a press and repeats while it is held: after a short wait, quicker and quicker. */
@Composable
internal fun StepButton(symbol: String, onStep: () -> Unit) {
    val step by androidx.compose.runtime.rememberUpdatedState(onStep)
    Surface(
        shape = RoundedCornerShape(10.dp), color = Console.Surface, border = BorderStroke(1.dp, Console.Border),
        modifier = Modifier.size(40.dp).pointerInput(Unit) {
            detectTapGestures(onPress = {
                step()
                kotlinx.coroutines.coroutineScope {
                    val repeat = launch {
                        kotlinx.coroutines.delay(400)
                        var wait = 110L
                        while (true) {
                            step()
                            kotlinx.coroutines.delay(wait)
                            wait = (wait * 9 / 10).coerceAtLeast(25L)
                        }
                    }
                    tryAwaitRelease()
                    repeat.cancel()
                }
            })
        }
    ) {
        Box(contentAlignment = Alignment.Center) { Text(symbol, style = Console.Title.copy(color = Console.Accent)) }
    }
}

@Composable
internal fun LabeledSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
        Text(label, style = Console.Small, modifier = Modifier.width(100.dp))
        Slider(
            value = value, onValueChange = onChange, valueRange = range,
            colors = SliderDefaults.colors(thumbColor = Console.Accent, activeTrackColor = Console.Accent),
            modifier = Modifier.weight(1f)
        )
        Text(String.format(Locale.US, "%.2f", value), style = Console.Small, modifier = Modifier.width(40.dp))
    }
}

@Composable
internal fun SmallLink(text: String, color: Color = Console.Accent, onClick: () -> Unit) {
    Text(text, style = Console.Label.copy(color = color), modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick).padding(vertical = 6.dp, horizontal = 2.dp))
}

@Composable
internal fun DarkButton(text: String, busy: Boolean, enabled: Boolean, onClick: () -> Unit) =
    ConsoleButton(text, onClick, Modifier.fillMaxWidth(), enabled = enabled, icon = Icons.Filled.AutoAwesome, height = 44.dp, style = ConsoleButtonStyle.Dark, loading = busy)

@Composable
internal fun BigButton(text: String, enabled: Boolean, onClick: () -> Unit) =
    ConsoleButton(text, onClick, Modifier.fillMaxWidth(), enabled = enabled, icon = Icons.Filled.Movie, height = 50.dp)

@Composable
internal fun Field(label: String?, value: String, onChange: (String) -> Unit, lines: Int = 1, secret: Boolean = false, numeric: Boolean = false) =
    ConsoleTextField(
        keyboardOptions = if (numeric) androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number) else androidx.compose.foundation.text.KeyboardOptions.Default,
        value = value,
        onValueChange = onChange,
        label = label,
        singleLine = lines == 1,
        minLines = if (lines > 1) 2 else 1,
        maxLines = lines.coerceAtLeast(1),
        visualTransformation = if (secret) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
    )

/** A button in one of the studio's rows; its colors name the console style it stands for. */
@Composable
internal fun ActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, bg: Color, fg: Color, modifier: Modifier, onClick: () -> Unit) {
    val style = when {
        bg == Console.Accent -> ConsoleButtonStyle.Primary
        bg == Console.Dark -> ConsoleButtonStyle.Dark
        bg == Console.GoodSoft -> ConsoleButtonStyle.Success
        bg == Console.InkMuted -> ConsoleButtonStyle.Dark
        fg == Console.Critical -> ConsoleButtonStyle.DangerSoft
        else -> ConsoleButtonStyle.Secondary
    }
    ConsoleButton(text, onClick, modifier, enabled = bg != Console.InkMuted || text.endsWith("…"), icon = icon, height = 42.dp, style = style, loading = bg == Console.InkMuted && text.endsWith("…"))
}
