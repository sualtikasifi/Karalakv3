package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Console
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleButton
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleButtonStyle

/**
 * One screen of a video's making. [short] names it on the bar's "İleri" button and in the step list, [title] heads the screen,
 * [done] puts the tick beside it in that list, [hint] says under the title what this screen is waiting for. Nothing here forbids
 * going on: work often has to be done out of order (Gemini makes a clip while the voices are being set), so only a step that
 * cannot mean anything yet (no drawing chosen) holds the "İleri" button back, [blocksNext].
 */
internal class FlowStep(
    val short: String,
    val title: String,
    val done: Boolean = false,
    val hint: String? = null,
    val blocksNext: Boolean = false,
    val content: LazyListScope.() -> Unit
)

/**
 * A video made screen by screen instead of one long page: the position on top (tap it for the list of steps), one step's own
 * content in the middle, and "Geri" / "İleri" fixed at the bottom. A step that is longer than the screen scrolls inside the
 * middle, and starts at its top each time it is opened; the bar never moves.
 */
@Composable
internal fun StepFlow(steps: List<FlowStep>, index: Int, onIndex: (Int) -> Unit, modifier: Modifier = Modifier) {
    if (steps.isEmpty()) return
    val i = index.coerceIn(0, steps.lastIndex)
    val step = steps[i]
    var listOpen by remember { mutableStateOf(false) }
    Column(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { listOpen = true }.padding(bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Adım ${i + 1} / ${steps.size}", style = Console.Label, modifier = Modifier.weight(1f))
                Text("Tüm adımlar", style = Console.Label.copy(color = Console.Accent))
            }
            Text(step.title, style = Console.Subtitle, modifier = Modifier.padding(vertical = 2.dp))
            LinearProgressIndicator(
                progress = { (i + 1f) / steps.size },
                color = Console.Accent, trackColor = Console.Grid,
                modifier = Modifier.fillMaxWidth()
            )
        }
        // re-created for each step, so every step opens at its top
        key(i) {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp)
            ) { step.content(this) }
        }
        step.hint?.let { Text(it, style = Console.Small.copy(color = Console.Critical), modifier = Modifier.padding(top = 4.dp)) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            if (i > 0) ConsoleButton(
                "Geri", { onIndex(i - 1) }, Modifier.weight(1f),
                icon = Icons.AutoMirrored.Filled.ArrowBack, style = ConsoleButtonStyle.Secondary
            )
            if (i < steps.lastIndex) ConsoleButton(
                steps[i + 1].short, { onIndex(i + 1) }, Modifier.weight(1.6f),
                enabled = !step.blocksNext, trailingIcon = Icons.AutoMirrored.Filled.ArrowForward, style = ConsoleButtonStyle.Primary
            )
        }
    }
    if (listOpen) {
        AlertDialog(
            onDismissRequest = { listOpen = false },
            title = { Text("Adımlar") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    itemsIndexed(steps) { n, s ->
                        val current = n == i
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                .background(if (current) Console.AccentSoft else Color.Transparent)
                                .clickable { listOpen = false; onIndex(n) }
                                .padding(horizontal = 6.dp, vertical = 8.dp)
                        ) {
                            Box(
                                Modifier.size(24.dp).clip(CircleShape).background(if (current) Console.Accent else if (s.done) Console.GoodSoft else Console.Grid),
                                contentAlignment = Alignment.Center
                            ) {
                                if (s.done && !current) Icon(Icons.Filled.Check, contentDescription = null, tint = Console.Good, modifier = Modifier.size(14.dp))
                                else Text("${n + 1}", style = Console.Label.copy(color = if (current) Color.White else Console.Ink))
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(s.title, style = if (current) Console.Subtitle else Console.Small.copy(color = Console.Ink))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { listOpen = false }) { Text("Kapat") } }
        )
    }
}
