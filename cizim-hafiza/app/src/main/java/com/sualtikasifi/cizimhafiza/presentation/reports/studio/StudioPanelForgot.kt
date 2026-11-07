package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotMode
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotSource
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotStyle
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotChest
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotCopy
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotPhase
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotScript
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotTimeline
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Console
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleCard
import com.sualtikasifi.cizimhafiza.presentation.reports.console.OpenRouterClient
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Segmented
import java.util.Locale

// "Ödüllü Karalak Düellosu" screens of the studio panel: the drawing, the code, the lines, the cue sheet.

/** Viewers drop off past this length (seconds): the panel warns when a "Çizen Bile Bilemedi" video runs over it. */
internal const val FORGOT_TARGET_SECONDS = 20

/** "Çizen Bile Bilemedi": a drawing its own drawer could not recognise, how the code is given, and Karalak's lines. */
internal fun LazyListScope.forgotSections(st: StudioState, vm: StudioViewModel) {
    val f = st.forgot
    // 1. the drawing: this phone's own, or the pool's; the ones its drawer got wrong first
    item { Step("1", "Çizim", if (f.drawingId != null) "seçili" else "birini seç") }
    item {
        Segmented(
            options = ForgotSource.entries.map { it.name to it.title },
            selected = f.source.name,
            onSelect = { vm.setForgotSource(ForgotSource.valueOf(it)) },
            modifier = Modifier.fillMaxWidth()
        )
    }
    item {
        Text(
            if (f.source == ForgotSource.Phone) "Bu telefonda kayıtlı çizimlerden biri; kendi çizimini tanıyamadığın turlar başta."
            else "Havuzdaki oyuncu turlarından biri; çizeni tanıyamayanlar başta. Çizenlerin adı videoda görünmez.",
            style = Console.Small
        )
    }
    val fromPool = f.source == ForgotSource.Pool
    val pool = if (fromPool) st.real.recent.map { it.toStudio() }.sortedBy { it.correct } else st.drawings.sortedBy { it.correct }
    when {
        fromPool && st.real.loadingRecent && pool.isEmpty() -> item { Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Console.Accent) } }
        fromPool && pool.isEmpty() -> item {
            ConsoleCard(Modifier.fillMaxWidth()) {
                Text(st.real.recentError ?: "Havuzda çizim bulunamadı.", style = Console.Small)
                ActionButton(Icons.Filled.Refresh, "Yeniden oku", Console.Surface, Console.Ink, Modifier.padding(top = 6.dp)) { vm.refreshRecent() }
            }
        }
        st.loading && !fromPool -> item { Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Console.Accent) } }
        pool.isEmpty() -> item { ConsoleCard(Modifier.fillMaxWidth()) { Text("Bu telefonda kayıtlı çizim yok. Birkaç tur oyna, sonra buraya dön.", style = Console.Small) } }
        else -> pool.take(f.shown).chunked(3).forEach { row ->
            item(key = "fg-" + row.first().id) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { d -> Thumb(d, if (f.drawingId == d.id) 0 else -1, Modifier.weight(1f)) { vm.selectForgot(d) } }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
    if (pool.size > f.shown) {
        item(key = "fg-more") {
            ActionButton(Icons.Filled.Refresh, "Daha fazla çizim göster (${pool.size - f.shown} kaldı)", Console.Surface, Console.Ink, Modifier.fillMaxWidth()) { vm.showMoreForgot() }
        }
    }
    val script = f.script ?: return
    item {
        ConsoleCard(Modifier.fillMaxWidth()) {
            Text("Doğru kelime: ${script.word.uppercase(Locale("tr"))}", style = Console.Subtitle)
        }
    }

    // 2. the code
    item { Step("2", "Kupon kodu") }
    item {
        ConsoleCard(Modifier.fillMaxWidth()) {
            Segmented(
                options = ForgotMode.entries.map { it.name to it.title },
                selected = f.mode.name,
                onSelect = { vm.setForgotMode(ForgotMode.valueOf(it)) },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                if (f.mode == ForgotMode.Word) "Kod, doğru cevabın kendisi. Videoda yalnızca harf sayısı kadar boş kutu görünür; bilen girer, bilmeyen yorumlardan öğrenir."
                else "Kod bitiş kartında yazılır. Modu değiştirince kod cümlesi o moda göre sıfırlanır.",
                style = Console.Small, modifier = Modifier.padding(top = 6.dp)
            )
            Text(
                if (script.code.isBlank()) "Bu kelime kod olamaz (4–24 harf): “Kod ekranda”yı seç." else "Kod: ${script.code}",
                style = Console.Subtitle.copy(color = if (script.code.isBlank()) Console.Critical else Console.Ink), modifier = Modifier.padding(top = 6.dp)
            )
            if (f.mode == ForgotMode.Shown) {
                ActionButton(Icons.Filled.Refresh, "Yeni kod", Console.Surface, Console.Ink, Modifier.padding(top = 6.dp)) { vm.newForgotCode() }
            }
            if (f.mode == ForgotMode.Word) {
                // a hint for the viewer: the code's first letter in its first box
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("İlk harfi göster", style = Console.Subtitle)
                        Text("Kodun ilk harfi ilk kutuda görünür; kelimeyi bulmayı kolaylaştırır.", style = Console.Small)
                    }
                    Switch(checked = script.showFirstLetter, onCheckedChange = vm::setForgotFirstLetter, colors = SwitchDefaults.colors(checkedTrackColor = Console.Accent))
                }
            }
            Text("Yazı dili", style = Console.Label, modifier = Modifier.padding(top = 10.dp))
            Segmented(
                options = listOf("Auto" to "Dönüşümlü") + ForgotStyle.entries.map { it.name to it.title },
                selected = f.styleChoice?.name ?: "Auto",
                onSelect = { vm.setForgotStyle(if (it == "Auto") null else ForgotStyle.valueOf(it)) },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                (if (f.styleChoice == null) "Bu video: ${script.style.title} (her video bitince sıradaki dile geçer). " else "") +
                    "Ekran yazıları ve Karalak'ın üslubu bu dile göre değişir; metinleri yeniden yazdırınca üslup uygulanır.",
                style = Console.Small, modifier = Modifier.padding(top = 6.dp)
            )
            Text("Ödül", style = Console.Label, modifier = Modifier.padding(top = 10.dp))
            Field("Altın miktarı", if (f.gold == 0) "" else f.gold.toString(), { vm.setForgotGold(it.digitsToInt()) }, numeric = true)
            Field("XP miktarı", if (f.xp == 0) "" else f.xp.toString(), { vm.setForgotXp(it.digitsToInt()) }, numeric = true)
            Text("Kasa (anında açılır)", style = Console.Label, modifier = Modifier.padding(top = 10.dp))
            Segmented(
                options = ForgotChest.entries.map { it.name to it.title },
                selected = f.chest.name,
                onSelect = { vm.setForgotChest(ForgotChest.valueOf(it)) },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                if (script.reward.isBlank()) "Ödül boş: altın, XP ya da kasa gir." else "Ödül: ${script.reward}",
                style = Console.Small.copy(color = if (script.reward.isBlank()) Console.Critical else Console.InkMuted), modifier = Modifier.padding(top = 4.dp)
            )
            Field("Kaç kişi kullanabilsin", if (f.maxUses == 0) "" else f.maxUses.toString(), { vm.setForgotUses(it.digitsToInt()) }, numeric = true)
            Text("Ne zamana kadar geçerli", style = Console.Label, modifier = Modifier.padding(top = 10.dp))
            ExpiryPicker(f.expiresAtMillis, vm::setForgotExpiry)
            Spacer(Modifier.height(10.dp))
            DarkButton(
                text = if (f.couponBusy) "Oluşturuluyor…" else "Kuponu oluştur",
                busy = f.couponBusy,
                enabled = !f.couponBusy && script.code.isNotBlank()
            ) { vm.createForgotCoupon() }
            Text("Videoyu paylaşmadan önce kuponu oluştur. Aynı kod varsa üzerine yazılmaz.", style = Console.Small, modifier = Modifier.padding(top = 4.dp))
            f.couponStatus?.let { Text(it, style = Console.Small.copy(color = if (it.startsWith("Kupon hazır")) Console.Ink else Console.Critical), modifier = Modifier.padding(top = 4.dp)) }
        }
    }
    // 3. Karalak's lines
    item { Step("3", "Karalak'ın senaryosu") }
    item {
        ConsoleCard(Modifier.fillMaxWidth()) {
            DarkButton(
                text = if (f.writing) "Karalak yazıyor…" else "Karalak çizime baksın",
                busy = f.writing,
                enabled = !f.writing
            ) { vm.writeForgot() }
            Text("Her tahmini, çizimin senin seçtiğin andaki hâline bakarak yazar; doğru kelimeyi söyleyen cümleler atılır. Model: ${com.sualtikasifi.cizimhafiza.presentation.reports.console.OpenRouterClient.FORGOT_MODEL} (OpenRouter).", style = Console.Small, modifier = Modifier.padding(top = 6.dp))
            st.aiCost?.let { Text("Bu yazımın maliyeti: " + OpenRouterClient.usd(it), style = Console.Small) }
            f.writeError?.let { Text(it, style = Console.Small.copy(color = Console.Critical), modifier = Modifier.padding(top = 4.dp)) }
        }
    }
    item { ForgotCueSheet(script, st.real.howtoMs) }
    item {
        ConsoleCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Açılış (kanca)", style = Console.Label, modifier = Modifier.weight(1f))
                ActionButton(
                    Icons.Filled.Refresh, if (f.hookWriting) "Yazıyor…" else "Yenile", Console.Surface, Console.Ink, Modifier
                ) { if (!f.hookWriting && !f.writing) vm.rewriteForgotHook() }
            }
            Text(
                "Sıradaki tür: ${ForgotCopy.HookKind.forTurn(f.hookTurn).title}. Yenile yalnızca açılış cümlesini değiştirir; öncekilerden farklı yazar.",
                style = Console.Small, modifier = Modifier.padding(bottom = 4.dp)
            )
            f.hookError?.let { Text(it, style = Console.Small.copy(color = Console.Critical), modifier = Modifier.padding(bottom = 4.dp)) }
            LineEditor(script.hook, { vm.editLine(LineRef.ForgotHook, it) }, { vm.editEmotion(LineRef.ForgotHook, it) }) { vm.listen(script.hook) }
            Text("Kaç tahmin", style = Console.Label, modifier = Modifier.padding(top = 8.dp))
            Segmented(
                options = (1..ForgotTimeline.MAX_GUESSES).map { it.toString() to it.toString() },
                selected = script.guesses.size.toString(),
                onSelect = { vm.setForgotGuessCount(it.toInt()) },
                modifier = Modifier.fillMaxWidth()
            )
            script.guesses.forEachIndexed { i, l ->
                Text("Tahmin ${i + 1}", style = Console.Label, modifier = Modifier.padding(top = 8.dp))
                StopScrubber(
                    strokes = st.forgotDrawing(script.drawingId)?.strokes.orEmpty(),
                    share = script.stops[i],
                    onChange = { vm.setForgotStop(i, it) },
                    onNudge = { vm.stepForgotStop(i, it) }
                )
                LineEditor(l, { vm.editLine(LineRef.ForgotGuess(i), it) }, { vm.editEmotion(LineRef.ForgotGuess(i), it) }) { vm.listen(l) }
            }
            Text("Geri sayımda izleyiciye soru", style = Console.Label, modifier = Modifier.padding(top = 8.dp))
            LineEditor(script.askLine, { vm.editLine(LineRef.ForgotAsk, it) }, { vm.editEmotion(LineRef.ForgotAsk, it) }) { vm.listen(script.askLine) }
            Text("Tepki ve kod anı (tek cümle)", style = Console.Label, modifier = Modifier.padding(top = 8.dp))
            LineEditor(script.codeLine, { vm.editLine(LineRef.ForgotCode, it) }, { vm.editEmotion(LineRef.ForgotCode, it) }) { vm.listen(script.codeLine) }
            Text("Kapanış", style = Console.Label, modifier = Modifier.padding(top = 8.dp))
            LineEditor(script.outro, { vm.editLine(LineRef.ForgotOutro, it) }, { vm.editEmotion(LineRef.ForgotOutro, it) }) { vm.listen(script.outro) }
        }
    }
}

/** The digits of what was typed in a number field as a number (nothing typed: 0), at most eight of them. */
internal fun String.digitsToInt(): Int = filter { it.isDigit() }.take(8).toIntOrNull() ?: 0

/**
 * Where a guess stops the drawing, picked by looking: the drawing as it is at [share] sits on the left, and dragging the bar
 * grows or shrinks it, so the scene Karalak reacts to is chosen from the picture, never from a number. − and + nudge it a
 * hundredth for the last bit.
 */
@Composable
internal fun StopScrubber(strokes: List<com.sualtikasifi.cizimhafiza.domain.model.DrawingStroke>, share: Float, onChange: (Float) -> Unit, onNudge: (Int) -> Unit) {
    // one picture per hundredth, made again only when the share moves to another
    val step = (share * 100f).let { kotlin.math.round(it).toInt() }
    val picture = remember(strokes, step) {
        if (strokes.isEmpty()) null
        else runCatching {
            val png = com.sualtikasifi.cizimhafiza.presentation.reports.studio.StrokeReplay.png(strokes, step / 100f, 288)
            android.graphics.BitmapFactory.decodeByteArray(png, 0, png.size)?.asImageBitmap()
        }.getOrNull()
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Box(Modifier.size(132.dp).clip(RoundedCornerShape(12.dp)).background(Color.White).border(1.dp, Console.Border, RoundedCornerShape(12.dp))) {
            picture?.let { Image(it, contentDescription = "Çizimin bu andaki hâli", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize()) }
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text("Çubuğu sürükle: soldaki çizim, Karalak'ın tahmin ettiği an", style = Console.Small)
            Slider(
                value = share,
                onValueChange = onChange,
                valueRange = ForgotTimeline.MIN_STOP..1f,
                colors = SliderDefaults.colors(thumbColor = Console.Accent, activeTrackColor = Console.Accent),
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                StepButton("−") { onNudge(-1) }
                Text("%$step", style = Console.Label, modifier = Modifier.padding(horizontal = 12.dp))
                StepButton("+") { onNudge(1) }
            }
        }
    }
}

/**
 * The video as it will be said, moment by moment: when each spoken line starts, what is on screen then and what Karalak says.
 * The same timeline the video is made from (voices at the pace the preview assumes, the real ones when the video is made),
 * so it answers "which line plays when" at a glance.
 */
@Composable
internal fun ForgotCueSheet(script: ForgotScript, howtoMs: Long) {
    val events = remember(script, howtoMs) { ForgotTimeline.build(script, howtoMs) { _, l -> StudioCopy.estimateMs(l.text) } }
    ConsoleCard(Modifier.fillMaxWidth()) {
        Text("Sahne akışı ve seslendirme", style = Console.Subtitle)
        Text("Hangi cümle, videonun hangi anında, ekranda ne varken söylenir. Süreler tahmin; gerçek ses uzunluğuna göre ayarlanır.", style = Console.Small)
        events.filter { it.line != null }.forEach { e ->
            val label = when (e.phase) {
                ForgotPhase.Hook -> "Açılış · çizim çizilmeye başlıyor"
                ForgotPhase.Guess -> "Tahmin ${e.look + 1} · çizimin %${ForgotCopy.percent(e.drawTo)}'i çizilmiş"
                ForgotPhase.Countdown -> "Geri sayım 3-2-1 · izleyiciye soru"
                ForgotPhase.Code -> "Kod ekranı · ödül ve nereye girileceği yazıyor"
                ForgotPhase.Outro -> "Bitiş kartı · nasıl oynanır videosu"
                else -> ""
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(String.format(Locale("tr"), "%.1f sn", e.startMs / 1000f), style = Console.Label, modifier = Modifier.width(62.dp))
                Column(Modifier.weight(1f)) {
                    Text(label, style = Console.Small)
                    Text("“${e.line?.text.orEmpty()}”", style = Console.Subtitle)
                }
            }
        }
        val total = ((events.lastOrNull()?.endMs ?: 0L) + 999) / 1000
        Text("≈ $total sn", style = Console.Label, modifier = Modifier.padding(top = 8.dp))
    }
}

/** The date and time the code stops working, shown as one button: a tap picks the date, then the time (device time zone). */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun ExpiryPicker(millis: Long, onPick: (Long) -> Unit) {
    val zone = java.time.ZoneId.systemDefault()
    val current = java.time.Instant.ofEpochMilli(millis).atZone(zone)
    var step by remember { mutableStateOf(0) }
    var date by remember { mutableStateOf(current.toLocalDate()) }
    val label = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").format(current)
    ActionButton(Icons.Filled.Event, label, Console.Surface, Console.Ink, Modifier.fillMaxWidth()) {
        date = current.toLocalDate()
        step = 1
    }
    if (step == 1) {
        val state = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { step = 0 },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { date = java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).toLocalDate() }
                    step = 2
                }) { Text("İleri: saat") }
            },
            dismissButton = { TextButton(onClick = { step = 0 }) { Text("Vazgeç") } }
        ) { androidx.compose.material3.DatePicker(state = state) }
    }
    if (step == 2) {
        val time = androidx.compose.material3.rememberTimePickerState(initialHour = current.hour, initialMinute = current.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { step = 0 },
            confirmButton = {
                TextButton(onClick = {
                    onPick(date.atTime(time.hour, time.minute).atZone(zone).toInstant().toEpochMilli())
                    step = 0
                }) { Text("Tamam") }
            },
            dismissButton = { TextButton(onClick = { step = 1 }) { Text("Geri") } },
            text = { androidx.compose.material3.TimePicker(state = time) }
        )
    }
}
