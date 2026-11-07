package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotMode
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotStyle
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotChest
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotCopy
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotPhase
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotScript
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotTimeline
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.res.painterResource
import com.sualtikasifi.cizimhafiza.domain.model.AvatarFrame
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.PostLink
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.PostStats
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealDrawing
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealEnding
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealOpening
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.SocialPlatform
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.VideoPost
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealLine
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealMix
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealMusic
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.VoicedLine
import androidx.compose.runtime.Composable
import kotlinx.coroutines.launch
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.sualtikasifi.cizimhafiza.domain.model.DrawingPoint
import com.sualtikasifi.cizimhafiza.presentation.common.StrokeCanvas
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Console
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleButton
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleButtonStyle
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleTextField
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleCard
import com.sualtikasifi.cizimhafiza.presentation.reports.console.OpenRouterClient
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Segmented
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Tag
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.audio.ElevenLabs
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.audio.VoiceModel
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MascotEmotion
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.CardLayout
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.LibraryItem
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealCopy
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealProject
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealReveal
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealTimeline
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RecentDrawing
import java.io.File
import java.util.Locale

/**
 * The video studio screen. Each format runs top to bottom — choose the drawings, let Karalak write
 * its lines, adjust any of them — and then the shared steps: the voice, a look at any moment of
 * the video, making it, sharing it.
 */
@Composable
fun StudioPanel(viewModel: StudioViewModel = hiltViewModel()) {
    val st by viewModel.state.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(st.message) {
        st.message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show(); viewModel.clearMessage() }
    }
    LaunchedEffect(st.real.share) {
        st.real.share?.let { sh -> shareImage(context, sh.file, sh.prompt, sh.target); viewModel.clearShare() }
    }
    LaunchedEffect(st.real.carousel) {
        st.real.carousel?.let { files -> shareImages(context, files, postText(st.caption, st.hashtags)); viewModel.clearCarousel() }
    }
    st.eleven.confirm?.let { line ->
        AlertDialog(
            onDismissRequest = viewModel::cancelListen,
            title = { Text("ElevenLabs ile seslendirilsin mi?") },
            text = { Text("“${line.text.take(90)}”\nYaklaşık ${st.eleven.spec.credits(line)} kredi harcanır. Bir kez seslendirilen cümle tekrar ücretlendirilmez.") },
            confirmButton = { TextButton(onClick = viewModel::confirmListen) { Text("Seslendir") } },
            dismissButton = {
                Row {
                    TextButton(onClick = viewModel::listenFree) { Text("Telefon sesiyle") }
                    TextButton(onClick = viewModel::cancelListen) { Text("Vazgeç") }
                }
            }
        )
    }
    if (st.real.confirmReset) {
        AlertDialog(
            onDismissRequest = viewModel::cancelResetReal,
            title = { Text("Baştan başlansın mı?") },
            text = { Text("Seçilen çizimler videodan kalkar. Görselleri ve videoları kütüphanede durur; oradan silebilirsin.") },
            confirmButton = { TextButton(onClick = { viewModel.resetReal(confirmed = true) }) { Text("Temizle", color = Console.Critical) } },
            dismissButton = { TextButton(onClick = viewModel::cancelResetReal) { Text("Vazgeç") } }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Karalak'lı dikey videolar · 1080×2400 · sesli", style = Console.Label, modifier = Modifier.weight(1f))
            }
        }
        item {
            Segmented(
                options = StudioFormat.entries.map { it.name to it.title },
                selected = st.format.name,
                onSelect = { viewModel.setFormat(StudioFormat.valueOf(it)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Column {
                Text("Video dili · ${st.lang.named}", style = Console.Label)
                Text("Karalak'ın söylediği cümleler, ses ve gönderi metni bu dilde yazılır. Ekrandaki sabit yazılar şimdilik Türkçe.", style = Console.Small)
                Spacer(Modifier.height(6.dp))
                Segmented(
                    options = StudioLang.entries.map { it.name to it.label },
                    selected = st.lang.name,
                    onSelect = { viewModel.setLang(StudioLang.valueOf(it)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        when (st.format) {
            StudioFormat.Forgot -> { forgotSections(st, viewModel); sharedSections(st, viewModel, context) }
            StudioFormat.RealLife -> realSections(st, viewModel, context)
        }
    }
}

/** Viewers drop off past this length (seconds): the panel warns when a "Çizen Bile Bilemedi" video runs over it. */
private const val FORGOT_TARGET_SECONDS = 20

/** "Çizen Bile Bilemedi": a drawing its own drawer could not recognise, how the code is given, and Karalak's lines. */
private fun LazyListScope.forgotSections(st: StudioState, vm: StudioViewModel) {
    val f = st.forgot
    // 1. the drawing: this phone's own, the ones its drawer got wrong first
    item { Step("1", "Çizim", if (f.drawingId != null) "seçili" else "birini seç") }
    item {
        Text(
            "Bu telefonda kayıtlı çizimlerden biri; kendi çizimini tanıyamadığın turlar başta.",
            style = Console.Small
        )
    }
    val pool = st.drawings.sortedBy { it.correct }
    when {
        st.loading -> item { Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Console.Accent) } }
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
    item {
        ConsoleCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("“%${script.percent} tanıyamadı” göster", style = Console.Subtitle)
                    Text("Elle girilen bir değer: gerçek ölçüm değil. Yayında gerçeğe yakın tut.", style = Console.Small)
                }
                Switch(checked = script.showPercent, onCheckedChange = vm::setForgotShowPercent, colors = SwitchDefaults.colors(checkedTrackColor = Console.Accent))
            }
            if (script.showPercent) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    StepButton("−") { vm.stepForgotPercent(-1) }
                    Text("%${script.percent}", style = Console.Subtitle, modifier = Modifier.padding(horizontal = 16.dp))
                    StepButton("+") { vm.stepForgotPercent(1) }
                }
            }
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
            Text("Her tahmini, çizimin senin seçtiğin andaki hâline bakarak yazar; doğru kelimeyi söyleyen cümleler atılır. Model: ${st.analysisModel} (OpenRouter; Ayarlar sekmesinden değişir).", style = Console.Small, modifier = Modifier.padding(top = 6.dp))
            st.aiCost?.let { Text("Bu yazımın maliyeti: " + OpenRouterClient.usd(it), style = Console.Small) }
            f.writeError?.let { Text(it, style = Console.Small.copy(color = Console.Critical), modifier = Modifier.padding(top = 4.dp)) }
        }
    }
    item { ForgotCueSheet(script, st.real.howtoMs) }
    item {
        ConsoleCard(Modifier.fillMaxWidth()) {
            Text("Açılış", style = Console.Label)
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
                    strokes = st.drawings.find { it.id == script.drawingId }?.strokes.orEmpty(),
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
private fun String.digitsToInt(): Int = filter { it.isDigit() }.take(8).toIntOrNull() ?: 0

/**
 * Where a guess stops the drawing, picked by looking: the drawing as it is at [share] sits on the left, and dragging the bar
 * grows or shrinks it, so the scene Karalak reacts to is chosen from the picture, never from a number. − and + nudge it a
 * hundredth for the last bit.
 */
@Composable
private fun StopScrubber(strokes: List<com.sualtikasifi.cizimhafiza.domain.model.DrawingStroke>, share: Float, onChange: (Float) -> Unit, onNudge: (Int) -> Unit) {
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
private fun ForgotCueSheet(script: ForgotScript, howtoMs: Long) {
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
private fun ExpiryPicker(millis: Long, onPick: (Long) -> Unit) {
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

/** "Gerçek Olsaydı": the latest drawings of players who allowed it, three of them, each turned real with ChatGPT and Gemini. */
/** The seven steps of "Gerçek Olsaydı", in the order the work goes (see [WizardStepInfo]). */
private class WizardStepInfo(val title: String, val short: String)

private val WIZARD_STEPS = listOf(
    WizardStepInfo("Çizimi seç", "Çizim"),
    WizardStepInfo("Görsel istemini hazırla", "İstem"),
    WizardStepInfo("Gerçeğe dönüştür ve oyuncuyu ayarla", "Görsel"),
    WizardStepInfo("Görseli yükle, senaryoyu seç, Gemini'ye gönder", "Senaryo"),
    WizardStepInfo("Seslendirmeler ve müzik", "Ses"),
    WizardStepInfo("Videoyu yükle, kartı yerleştir", "Video"),
    WizardStepInfo("Önizle ve oluştur", "Bitir")
)

private val VOICE_STEPS = listOf("Kanca", "Anlatıcı", "Kapanış", "Müzik", "Bitiş sesi")

/** Whether step [n] (1..7) has what it is for: only for the marks on the stepper, the steps themselves can be visited in any order. */
private fun stepDone(n: Int, st: StudioState): Boolean {
    val p = st.real.project
    val f = p?.opening
    return when (n) {
        1 -> p != null && p.reveals.isNotEmpty()
        2 -> f != null && f.describe.isNotBlank()
        3 -> f?.image != null
        4 -> f != null && f.image != null && f.motion.isNotBlank()
        5 -> f != null && f.hook.text.isNotBlank()
        6 -> f?.clip != null
        else -> st.video != null
    }
}

private fun voiceDone(n: Int, f: RealReveal?): Boolean = when (n) {
    1 -> f?.hook?.voice != null
    2 -> f?.narration?.voice != null
    3 -> f?.outro?.voice != null
    4 -> f?.music != null
    else -> f?.endLine?.voice != null
}

private fun LazyListScope.realSections(st: StudioState, vm: StudioViewModel, context: Context) {
    val r = st.real
    item {
        Segmented(
            options = listOf(RealTab.Prepare.name to "Video hazırla", RealTab.Library.name to "Kütüphane (${r.library.size})", RealTab.Settings.name to "Ayarlar"),
            selected = r.tab.name,
            onSelect = { vm.setRealTab(RealTab.valueOf(it)) },
            modifier = Modifier.fillMaxWidth()
        )
    }
    when (r.tab) {
        RealTab.Library -> libraryTab(st, vm)
        RealTab.Settings -> settingsTab(st, vm)
        RealTab.Prepare -> wizard(st, vm, context)
    }
}

/** The finished pieces kept on this phone, to watch or to make into a new video. */
private fun LazyListScope.libraryTab(st: StudioState, vm: StudioViewModel) {
    val r = st.real
    item { Step("★", "Kütüphane", "${r.library.size} kayıt · ${megabytes(r.libraryBytes)}") }
    item {
        ConsoleCard(Modifier.fillMaxWidth()) {
            Text(
                "Ürettiğin görseller ve videolar telefonda otomatik saklanır (uygulamanın boyutunu büyütmez). " +
                    "Kancası, kapanışı ve sesleriyle birlikte; yani her kayıt hazır bir video (stok). Birini seçip yeniden video yapabilirsin.",
                style = Console.Small
            )
        }
    }
    if (r.library.isEmpty()) item { Text("Henüz kayıt yok. Görsel ya da video eklediğin her çizim buraya düşer.", style = Console.Small) }
    r.library.chunked(3).forEach { row ->
        item(key = "lib-" + row.first().id) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { it -> LibraryThumb(it, r.librarySelected.indexOf(it.id), vm, Modifier.weight(1f)) }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
    if (r.library.isNotEmpty()) item {
        ConsoleCard(Modifier.fillMaxWidth()) {
            Text("Projeye almak için kartın köşesindeki yuvarlağa dokun; video ilk seçtiğinle yapılır, diğerleri sırada bekler. Karta dokunursan izlersin.", style = Console.Small)
            Spacer(Modifier.height(8.dp))
            DarkButton(
                text = if (r.librarySelected.isEmpty()) "Video yapmak için kayıt seç" else if (r.librarySelected.size == 1) "Bu kayıtla video yap" else "${r.librarySelected.size} kaydı projeye al",
                busy = false,
                enabled = r.librarySelected.isNotEmpty()
            ) { vm.libraryMakeVideo() }
        }
    }
    r.libraryView?.let { id -> r.library.find { it.id == id }?.let { item { LibraryViewer(it, vm) } } }
    item { HookResults(r.library) }
}

/** What is set once and left: the model that writes, Karalak's voice (ElevenLabs). */
private fun LazyListScope.settingsTab(st: StudioState, vm: StudioViewModel) {
    item { Step("⚙", "Yazan model") }
    item {
        ConsoleCard(Modifier.fillMaxWidth()) {
            Text("Asistan çizimleri okuyup istemleri, sahneleri, kancayı, anlatıcıyı ve kapanışı bu modelle yazar (OpenRouter anahtarı Asistan ayarlarında).", style = Console.Small)
            Field("Model (OpenRouter kimliği)", st.analysisModel, { vm.setAnalysisModel(it) }, lines = 1)
            st.aiModel?.let { Text("Son yazımı yazan model: $it", style = Console.Small) }
            st.aiCost?.let { Text("Son yazımın maliyeti: " + OpenRouterClient.usd(it), style = Console.Small) }
        }
    }
    item { Step("♪", "Karalak'ın sesi (ElevenLabs)") }
    item {
        ConsoleCard(Modifier.fillMaxWidth()) {
            Text("Kanca, anlatıcı, kapanış ve bitiş görselinin cümleleri bu sesle seslendirilir. Ayarı değiştirirsen yeni seslendirmeler değişir; videoya eklenmiş ses değişmez.", style = Console.Small)
            ElevenSettings(st, vm)
            st.voiceError?.let { Text(it, style = Console.Small.copy(color = Console.Critical)) }
        }
    }
}

/** The seven steps, one at a time: the stepper on top, the step's own content, then back and forward. */
private fun LazyListScope.wizard(st: StudioState, vm: StudioViewModel, context: Context) {
    val r = st.real
    val p = r.project
    val hasDrawing = p != null && p.reveals.isNotEmpty()
    val step = if (hasDrawing) r.step.coerceIn(1, 7) else 1
    // the drawing being worked on: the first with a clip, or the first chosen while none has one yet
    val f = p?.opening
    item(key = "wz-head") {
        Column {
            Row(Modifier.fillMaxWidth()) {
                WIZARD_STEPS.forEachIndexed { i, info ->
                    val n = i + 1
                    val enabled = hasDrawing || n == 1
                    Column(
                        Modifier.weight(1f).clickable(enabled = enabled) { vm.setRealStep(n) },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val current = n == step
                        val done = stepDone(n, st)
                        Box(
                            Modifier.size(30.dp).clip(CircleShape).background(if (current) Console.Accent else if (done) Console.GoodSoft else Console.Grid),
                            contentAlignment = Alignment.Center
                        ) {
                            if (done && !current) Icon(Icons.Filled.Check, contentDescription = null, tint = Console.Good, modifier = Modifier.size(16.dp))
                            else Text("$n", style = Console.Label.copy(color = if (current) Color.White else if (enabled) Console.Ink else Console.InkSecondary))
                        }
                        Text(info.short, style = Console.Small.copy(color = if (current) Console.Accent else Console.InkSecondary), maxLines = 1)
                    }
                }
            }
            Text("Adım $step/7 · ${WIZARD_STEPS[step - 1].title}", style = Console.Subtitle, modifier = Modifier.padding(top = 8.dp))
        }
    }
    // the drawing the video is made of, when several are kept
    if (step >= 2 && p != null && p.reveals.size > 1 && f != null) item(key = "wz-switch") {
        Column {
            Text("Bu videonun çizimi", style = Console.Label)
            Segmented(
                options = p.reveals.map { rv -> rv.sampleId to (p.drawing(rv.sampleId)?.word?.ifBlank { null } ?: "çizim") },
                selected = f.sampleId,
                onSelect = { vm.moveRevealFirst(it) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
    when (step) {
        1 -> stepChoose(st, vm)
        2 -> if (f != null && p != null) stepPrompt(st, vm, f, context)
        3 -> if (f != null && p != null) stepImage(st, vm, f, p, context)
        4 -> if (f != null && p != null) stepScenario(st, vm, f, context)
        5 -> if (f != null && p != null) stepVoices(st, vm, f, p)
        6 -> if (f != null) stepVideo(st, vm, f)
        else -> stepFinish(st, vm, context)
    }
    // back and forward: the voices step has five of its own steps inside
    item(key = "wz-nav") {
        val voices = step == 5
        val vs = r.voiceStep.coerceIn(1, 5)
        val back = when { step == 1 -> null; voices && vs > 1 -> VOICE_STEPS[vs - 2]; else -> WIZARD_STEPS[step - 2].short }
        val next = when {
            step == 7 -> null
            voices && vs < 5 -> VOICE_STEPS[vs]
            else -> WIZARD_STEPS[step].short
        }
        val canNext = step != 1 || hasDrawing
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            if (back != null) ActionButton(Icons.AutoMirrored.Filled.ArrowBack, back, Console.Surface, Console.Ink, Modifier.weight(1f)) {
                if (voices && vs > 1) vm.setVoiceStep(vs - 1) else vm.setRealStep(step - 1)
            }
            if (next != null) Box(Modifier.weight(1.4f)) {
                DarkButton(text = next, busy = false, enabled = canNext) {
                    if (voices && vs < 5) vm.setVoiceStep(vs + 1) else vm.setRealStep(step + 1)
                }
            }
        }
        if (step == 1 && !hasDrawing) Text("Devam etmek için bir çizim seç.", style = Console.Small, modifier = Modifier.padding(top = 4.dp))
    }
}

/** Step 1: the latest drawings of the pool; the first one chosen is the video's. */
private fun LazyListScope.stepChoose(st: StudioState, vm: StudioViewModel) {
    val r = st.real
    val p = r.project
    val chosenIds = p?.reveals?.map { it.sampleId }.orEmpty()
    item {
        ConsoleCard(Modifier.fillMaxWidth()) {
            Text(
                "Havuzdaki en son 100 çizim. “Videolarda öne çık” izni veren oyuncunun adı ve seviyesi videoda görünür; " +
                    "izin vermeyen anonim kalır (yalnızca çerçevesi görünür, resmi hep varsayılan). Her video tek çizimle yapılır: ilk seçtiğin çizim videonun olur, " +
                    "diğerleri hazır bekler (stok). Sonraki adımlarda aralarında geçiş yapabilirsin.",
                style = Console.Small
            )
            Spacer(Modifier.height(8.dp))
            DarkButton(
                text = if (r.loadingRecent) "Okunuyor…" else if (r.recent.isEmpty()) "Son çizimleri yükle" else "Yenile",
                busy = r.loadingRecent,
                enabled = !r.loadingRecent
            ) { vm.refreshRecent() }
            r.recentError?.let { Text(it, style = Console.Small.copy(color = Console.Critical), modifier = Modifier.padding(top = 4.dp)) }
            Text("${chosenIds.size}/5 seçili", style = Console.Label, modifier = Modifier.padding(top = 6.dp))
            if (p != null && p.reveals.isNotEmpty()) SmallLink("Seçimi temizle (kütüphane durur)", Console.Critical) { vm.resetReal() }
        }
    }
    // chosen ones the latest list no longer holds stay at the top, so they can still be taken out
    val gone = p?.drawings?.filter { d -> d.id in chosenIds && r.recent.none { it.id == d.id } }
        ?.map { RecentDrawing(it.id, it.word, it.seconds, it.nickname, it.level, it.frameId, it.strokes) }.orEmpty()
    (gone + r.recent).chunked(3).forEach { row ->
        item(key = "rs-" + row.first().id) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { d ->
                    RealThumb(d.strokes, "${d.word} · " + if (d.nickname.isBlank()) "anonim" else "@${d.nickname}", chosenIds.indexOf(d.id), Modifier.weight(1f)) { vm.toggleReveal(d) }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Step 2: the assistant reads the drawing and writes the picture prompt for ChatGPT. */
private fun LazyListScope.stepPrompt(st: StudioState, vm: StudioViewModel, f: RealReveal, context: Context) {
    val r = st.real
    item {
        ConsoleCard(Modifier.fillMaxWidth()) {
            Text(
                "Asistan (${st.analysisModel}) çizimi okur: kelimeyi, şekli, malzemeyi, boyutu ve mekânı yazar; bunlardan ChatGPT'ye gidecek görsel istemi oluşur. " +
                    "Seçtiğin bütün çizimler için tek seferde yazar.",
                style = Console.Small
            )
            Spacer(Modifier.height(8.dp))
            DarkButton(
                text = if (r.writing) "Asistan hazırlıyor…" else if (f.describe.isBlank()) "Asistan görsel istemini hazırlasın" else "Yeniden hazırlat",
                busy = r.writing,
                enabled = !r.writing
            ) { vm.describeReal() }
            r.writeError?.let { Text(it, style = Console.Small.copy(color = Console.Critical), modifier = Modifier.padding(top = 4.dp)) }
            st.aiCost?.let { Text("Son yazımın maliyeti: " + OpenRouterClient.usd(it), style = Console.Small, modifier = Modifier.padding(top = 4.dp)) }
        }
    }
    if (f.describe.isNotBlank()) item {
        ConsoleCard(Modifier.fillMaxWidth()) {
            Text("ChatGPT görsel istemi", style = Console.Label)
            val prompt = vm.imagePrompt(f.sampleId).orEmpty()
            Text(prompt, style = Console.Small, modifier = Modifier.padding(top = 6.dp))
            ActionButton(Icons.Filled.ContentCopy, "İstemi kopyala", Console.Surface, Console.Ink, Modifier.fillMaxWidth().padding(top = 8.dp)) { if (prompt.isNotBlank()) copy(context, prompt) }
        }
    }
    item {
        var open by remember { mutableStateOf(false) }
        ConsoleCard(Modifier.fillMaxWidth()) {
            SmallLink(if (open) "Asistanın notlarını gizle" else "Asistanın notlarını düzenle (isteğe bağlı)") { open = !open }
            if (open) NotesEditor(f, vm)
        }
    }
}

/** Step 3: the doodle goes to ChatGPT; while it makes the picture, the player on the card is set up. */
private fun LazyListScope.stepImage(st: StudioState, vm: StudioViewModel, f: RealReveal, p: RealProject, context: Context) {
    val r = st.real
    item {
        ConsoleCard(Modifier.fillMaxWidth()) {
            Text(
                "“ChatGPT'ye gönder”: çizim paylaşılır, istem panoya kopyalanır; ChatGPT'de yapıştırıp gönder. " +
                    "ChatGPT görseli üretirken aşağıdan kartta görünecek oyuncuyu ayarla.",
                style = Console.Small
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                ActionButton(Icons.Filled.Share, "ChatGPT'ye gönder", Console.Accent, Color.White, Modifier.weight(1.4f)) { vm.shareDoodle(f.sampleId) }
                ActionButton(Icons.Filled.ContentCopy, "İstem", Console.Surface, Console.Ink, Modifier.weight(1f)) { vm.imagePrompt(f.sampleId)?.let { copy(context, it) } }
            }
            if (f.describe.isBlank()) Text("İstem boş görünüyorsa önce 2. adımda asistana hazırlat.", style = Console.Small.copy(color = Console.Critical), modifier = Modifier.padding(top = 6.dp))
        }
    }
    item {
        ConsoleCard(Modifier.fillMaxWidth()) {
            Text("Kartta görünen oyuncu", style = Console.Subtitle)
            Text("Kullanıcı adı, seviye, çerçeve, profil fotoğrafı ve çizim süresi. Seviye ve çerçeve kartta görünür.", style = Console.Small)
            PlayerEditor(p.drawing(f.sampleId), vm, r.busy != null)
        }
    }
    item { Text("Görsel hazır olunca ileri geç ve yükle.", style = Console.Small) }
}

/** Step 4: the picture comes in, the scene is chosen from ideas written by looking at it, and the video prompt goes to Gemini. */
private fun LazyListScope.stepScenario(st: StudioState, vm: StudioViewModel, f: RealReveal, context: Context) {
    val r = st.real
    val anyBusy = r.busy != null
    item {
        val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let { vm.importImage(f.sampleId, it) } }
        ConsoleCard(Modifier.fillMaxWidth()) {
            Text("ChatGPT'nin ürettiği görsel", style = Console.Label)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                Box(Modifier.size(110.dp).clip(RoundedCornerShape(10.dp)).background(Console.Grid), contentAlignment = Alignment.Center) {
                    val image = f.image
                    if (image != null) AsyncImage(model = vm.mediaFile(image), contentDescription = "Gerçek hâli", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    else Text("görsel\nyok", style = Console.Small, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    ActionButton(Icons.Filled.Download, if (f.image == null) "Görsel yükle" else "Görseli değiştir", Console.Accent, Color.White, Modifier.fillMaxWidth()) {
                        if (!anyBusy) imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                    if (f.image != null) SmallLink("Görseli kaldır", Console.Critical) { vm.removeImage(f.sampleId) }
                }
                if (r.busy == f.sampleId) CircularProgressIndicator(Modifier.size(18.dp), color = Console.Accent, strokeWidth = 2.dp)
            }
        }
    }
    item {
        ConsoleCard(Modifier.fillMaxWidth()) {
            Text("Video senaryosu", style = Console.Label)
            Text("Asistan görsele bakarak 3 komik sahne yazar; birini seçersin, video istemi ona göre kurulur.", style = Console.Small)
            ActionButton(
                Icons.Filled.Refresh,
                if (r.pitching == f.sampleId) "Fikirler yazılıyor…" else if (f.pitches.isEmpty()) "Fotoğrafa bakarak 3 senaryo yaz" else "Üç yeni senaryo yaz",
                Console.Surface, Console.Ink, Modifier.fillMaxWidth().padding(top = 6.dp)
            ) { if (!anyBusy && r.pitching == null) vm.writePitches(f.sampleId) }
            if (f.image == null) Text("Senaryolar için önce görseli yükle.", style = Console.Small, modifier = Modifier.padding(top = 4.dp))
            r.pitchError?.takeIf { r.pitching == null }?.let { Text(it, style = Console.Small.copy(color = Console.Critical), modifier = Modifier.padding(top = 4.dp)) }
            f.pitches.forEachIndexed { i, pitch ->
                Column(Modifier.fillMaxWidth().padding(top = 8.dp).background(Console.Surface, RoundedCornerShape(12.dp)).padding(12.dp)) {
                    Text("${i + 1}) " + pitch.joke.ifBlank { "Fikir" }, style = Console.Label)
                    Text(pitch.scene, style = Console.Small, modifier = Modifier.padding(top = 4.dp), maxLines = 8)
                    val chosen = pitch.scene.take(1800) == f.motion
                    SmallLink(if (chosen) "Seçili" else "Bu senaryoyu seç") { if (!chosen) vm.choosePitch(f.sampleId, i) }
                }
            }
            Field("Seçilen senaryo (istersen düzenle ya da kendin yaz)", f.motion, { vm.setRevealMotion(f.sampleId, it) }, lines = 7)
        }
    }
    item {
        ConsoleCard(Modifier.fillMaxWidth()) {
            Text("Gemini'ye gönder", style = Console.Label)
            Text("Görsel ve video istemi birlikte gider; Gemini'de Video'yu seçip gönder (sesli üretir). Üretirken sonraki adıma geçip seslendirmeleri hazırlayabilirsin.", style = Console.Small)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                ActionButton(Icons.Filled.Share, "Gemini'ye gönder", Console.Accent, Color.White, Modifier.weight(1.4f)) { vm.shareToGemini(f.sampleId) }
                ActionButton(Icons.Filled.ContentCopy, "İstem", Console.Surface, Console.Ink, Modifier.weight(1f)) { vm.videoPrompt(f.sampleId)?.let { copy(context, it) } }
            }
            if (f.image == null) Text("Göndermek için önce görseli yükle.", style = Console.Small, modifier = Modifier.padding(top = 4.dp))
        }
    }
    item {
        var open by remember { mutableStateOf(false) }
        ConsoleCard(Modifier.fillMaxWidth()) {
            SmallLink(if (open) "Video notlarını gizle" else "Video notlarını düzenle (isteğe bağlı)") { open = !open }
            if (open) {
                Field("Gerçek boyutu (ör. yaklaşık 15 cm, avuca sığar)", f.size, { vm.setRevealSize(f.sampleId, it) }, lines = 1)
                Field("Değişmeyenler (ör. sert plastik: bükülmez, esnemez)", f.fixed, { vm.setRevealFixed(f.sampleId, it) }, lines = 2)
            }
        }
    }
}

/** Step 5: Karalak's voice and the music, one at a time: the hook, the narrator, the closing, the music, the line over the end card. */
private fun LazyListScope.stepVoices(st: StudioState, vm: StudioViewModel, f: RealReveal, p: RealProject) {
    val r = st.real
    val vs = r.voiceStep.coerceIn(1, 5)
    item(key = "wz-voices") {
        Column {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(VOICE_STEPS.size) { i ->
                    val n = i + 1
                    val done = voiceDone(n, f)
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (n == vs) Console.Accent else if (done) Console.GoodSoft else Console.Grid,
                        modifier = Modifier.clickable { vm.setVoiceStep(n) }
                    ) {
                        Text(
                            (if (done && n != vs) "✓ " else "") + VOICE_STEPS[i],
                            style = Console.Small.copy(color = if (n == vs) Color.White else Console.Ink),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            if (vs != 4 && !st.eleven.keySet) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Seslendirmek için ElevenLabs anahtarı gerekir.", style = Console.Small.copy(color = Console.Critical), modifier = Modifier.weight(1f))
                    SmallLink("Ayarlar'a git") { vm.setRealTab(RealTab.Settings) }
                }
            }
        }
    }
    when (vs) {
        1 -> {
            item {
                LineCard(RealLine.Hook, p, st, vm) {
                    Text(
                        "Video, çizimin oyun ekranında baştan çizilmesiyle başlar. Kanca beyaz altyazı olarak çıkar ve Karalak söyler; " +
                            "kısa olsun (≈2 sn). Cümleyi seç, seslendir, “Videoya ekle”.",
                        style = Console.Small
                    )
                }
            }
            item {
                ConsoleCard(Modifier.fillMaxWidth()) {
                    Text("Açılış stili", style = Console.Label)
                    Segmented(
                        options = listOf(RealOpening.Classic.name to "Klasik", RealOpening.Teaser.name to "Önce gerçek hâl"),
                        selected = f.opening.name,
                        onSelect = { id -> vm.setOpening(f.sampleId, RealOpening.valueOf(id)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Klasik: oyun ekranında çizim baştan çizilir. Önce gerçek hâl: video klibin bir anıyla açılır ve çizimin içine çekilir; " +
                            "kapak karesi gerçek nesne olur.",
                        style = Console.Small, modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
        2 -> item {
            LineCard(RealLine.Narration, p, st, vm) {
                Text(
                    "Komedi video modeline bırakılmaz, burada yazılır: Karalak klibin üstünde ciddi bir belgesel spikeri gibi konuşur; " +
                        "tuhaf olanı ciddiyetle anlatması komik olur. Klip başladıktan yarım saniye sonra girer, altyazı olarak da görünür. " +
                        "Kısa tut (≈4 sn).",
                    style = Console.Small
                )
            }
        }
        3 -> item {
            LineCard(RealLine.Outro, p, st, vm) {
                Text(
                    "Klibin son saniyelerinde Karalak klipte olana kısa bir tepki verip izleyiciyi yoruma çağırır; altyazı olarak da görünür, klibin sesi o sırada kısılır.",
                    style = Console.Small
                )
            }
        }
        4 -> {
            item { MusicCard(f, r.musicOn, r.musicPlaying, r.customMusic, vm) }
        }
        else -> {
            item { EndingCard(r, vm) }
            if (r.ending == RealEnding.Card) item {
                LineCard(RealLine.End, p, st, vm) {
                    Text(
                        "Video, oyunun kapanış görseliyle biter. Aşağıdan bir cümle seç, seslendir, “Videoya ekle”: görsel, Karalak o cümleyi söylerken ekranda kalır. " +
                            "Seslendirmezsen ${RealTimeline.END_CARD_SILENT_MS / 1000} saniye sessiz durur.",
                        style = Console.Small
                    )
                }
            }
        }
    }
}

/** Step 6: the Gemini video comes in (cut where it is still right) and the card is placed, with a live preview. */
private fun LazyListScope.stepVideo(st: StudioState, vm: StudioViewModel, f: RealReveal) {
    val r = st.real
    val anyBusy = r.busy != null
    item {
        val clipPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let { vm.importClip(f.sampleId, it) } }
        ConsoleCard(Modifier.fillMaxWidth()) {
            Text("Gemini'nin ürettiği video", style = Console.Label)
            Text(if (f.clip != null) "Yüklü · ${(f.clipMs + 999) / 1000} sn kullanılıyor" else "Henüz yüklenmedi.", style = Console.Small)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                ActionButton(Icons.Filled.Movie, if (f.clip == null) "Video yükle" else "Videoyu değiştir", Console.Accent, Color.White, Modifier.weight(1.4f)) {
                    if (!anyBusy) clipPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                }
                if (f.clip != null) ActionButton(Icons.Filled.Delete, "Kaldır", Console.Surface, Console.Critical, Modifier.weight(1f)) { vm.removeClip(f.sampleId) }
            }
            if (r.busy == f.sampleId) CircularProgressIndicator(Modifier.size(18.dp).padding(top = 6.dp), color = Console.Accent, strokeWidth = 2.dp)
            if (f.clip != null) {
                // the part of the clip that is used: where the odd shape starts to drift back to an ordinary one, cut it there
                var clipText by remember(f.sampleId, f.clip) { mutableStateOf(String.format(Locale("tr"), "%.1f", f.clipMs / 1000.0)) }
                Field("Klibin kullanılacak kısmı, saniye (sonu bozuluyorsa kısalt)", clipText, { raw ->
                    val t = raw.filter { it.isDigit() || it == ',' || it == '.' }.take(4)
                    clipText = t
                    t.replace(',', '.').toDoubleOrNull()?.takeIf { it >= 1.0 }?.let { vm.setClipSeconds(f.sampleId, it) }
                })
            }
        }
    }
    item { PreviewCard(st, vm) }
    item {
        ConsoleCard(Modifier.fillMaxWidth()) {
            Text("Karalak kartının yeri ve boyu", style = Console.Label)
            Text("Kaydırıcıyla kabaca, − ve + ile hassas ayarla; yukarıdaki önizleme anında değişir.", style = Console.Small)
            PreciseSlider("Boyut", r.card.scale, CardLayout.MIN_SCALE..CardLayout.MAX_SCALE) { vm.setCard(r.card.copy(scale = it)) }
            PreciseSlider("Sağa–sola", r.card.x, 0f..1f) { vm.setCard(r.card.copy(x = it)) }
            PreciseSlider("Yukarı–aşağı", r.card.y, 0f..1f) { vm.setCard(r.card.copy(y = it)) }
            SmallLink("Varsayılana dön") { vm.resetCard() }
        }
    }
}

/** Step 7: watch it, make it, then share. */
private fun LazyListScope.stepFinish(st: StudioState, vm: StudioViewModel, context: Context) {
    item { PreviewCard(st, vm) }
    renderItems(st, vm)
    resultItems(st, vm, context)
}

/** The drawing's notes the assistant wrote, to correct by hand: they build the picture and video prompts. */
@Composable
private fun NotesEditor(rv: RealReveal, vm: StudioViewModel) {
    Field("Türü: nesne, canlı, yiyecek ya da bitki (nesnenin ve yiyeceğin gözü, yüzü, uzvu olmaz)", rv.kind, { vm.setRevealKind(rv.sampleId, it) }, lines = 1)
    Field("Çizimden gerçeğe eşleme (her satır: çizilen parça → gerçek parça)", rv.parts, { vm.setRevealParts(rv.sampleId, it) }, lines = 4)
    Field("Çizimin şekli (görsel istemine eklenir)", rv.describe, { vm.setRevealDescribe(rv.sampleId, it) }, lines = 3)
    Field("Malzeme (gerçekte neyden yapıldığı)", rv.material, { vm.setRevealMaterial(rv.sampleId, it) }, lines = 2)
    Field("Sahne (fotoğrafın çekildiği tek, gerçek mekân)", rv.scene, { vm.setRevealScene(rv.sampleId, it) }, lines = 2)
    Field("Gerçek boyutu (ör. yaklaşık 15 cm, avuca sığar)", rv.size, { vm.setRevealSize(rv.sampleId, it) }, lines = 1)
    Field("Boyutu belli eden şey (ör. yanında bir tükenmez kalem)", rv.scaleHint, { vm.setRevealScaleHint(rv.sampleId, it) }, lines = 1)
}

/**
 * What closes every video. The closing picture is the default: the game's own card (what it is, in three steps, and where
 * to get it) with a line of Karalak's over it, as long as that line takes. The long promo video is still here for when it
 * is wanted, and the video can end on Karalak's closing alone.
 */
@Composable
private fun EndingCard(r: RealUi, vm: StudioViewModel) {
    ConsoleCard(Modifier.fillMaxWidth()) {
        Segmented(
            options = listOf(RealEnding.Card.name to "Kapanış görseli", RealEnding.Promo.name to "Tanıtım videosu", RealEnding.Nothing.name to "Yok"),
            selected = r.ending.name,
            onSelect = { vm.setEnding(RealEnding.valueOf(it)) },
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            when (r.ending) {
                RealEnding.Card -> "Klibin ve Karalak'ın tepkisinin hemen ardından oyunu anlatan kapanış görseli: “Çiz, Hatırla, Tahmin Et!”, 3 adım, Google Play düğmesi. " +
                    "Görsel Türkçe ve her videoda aynıdır; ne kadar kalacağını aşağıdaki seslendirme belirler."
                RealEnding.Promo -> "Kendi tanıtım videon her videonun sonuna eklenir. Uzun bir son, izleyicinin videoyu bitirme ve baştan izleme oranını düşürür; kısa tut ya da karşılaştırmalı dene."
                RealEnding.Nothing -> "Video Karalak'ın kapanışıyla biter; çağrı yalnızca açıklamada olur."
            },
            style = Console.Small, modifier = Modifier.padding(top = 8.dp)
        )
        if (r.ending == RealEnding.Promo) PromoCard(r, vm)
        if (r.ending == RealEnding.Card && r.howto != null) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Telefonda oyun videosu", style = Console.Subtitle)
                    Text(
                        String.format(Locale("tr"), "Açık: bitiş görselindeki telefonun içinde oyunun nasıl oynandığını gösteren %.1f sn'lik video sessiz oynar (müzik devam eder). ", r.howtoMs / 1000f) +
                            "Görsel, videonun bitmesine kadar ekranda kalır; uygulamanın içinde gömülüdür, her videoda aynıdır. Kapalıyken düz bitiş görseli çıkar.",
                        style = Console.Small
                    )
                }
                Switch(checked = r.howtoOn, onCheckedChange = vm::setHowtoOn, colors = SwitchDefaults.colors(checkedTrackColor = Console.Accent))
            }
        }
        if (r.ending == RealEnding.Card) EndVoices(r, vm)
    }
}

/**
 * The ready-made closing lines' voices: paid for once, then kept for good (and carried by Android's backup), with a
 * backup file of their own for the one case that wipes them, uninstalling the app.
 */
@Composable
private fun EndVoices(r: RealUi, vm: StudioViewModel) {
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri -> uri?.let { vm.exportEndVoices(it) } }
    val load = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { vm.importEndVoices(it) } }
    Column(Modifier.padding(top = 10.dp)) {
        Text("Kapanış sesleri: ${r.endVoiced} / ${RealCopy.END_LINES.size} hazır · ${r.endKept} ses kalıcı", style = Console.Subtitle)
        Text(
            "Hazır cümleler bir kez seslendirilir, telefonda silinmeden durur ve her videoda ücretsiz seçilir. Uygulamayı silmeden önce Yedekle'ye bas; " +
                "yeniden yükleyince Yedekten yükle ile geri gelir (Google yedeği açıksa kendiliğinden de gelebilir).",
            style = Console.Small, modifier = Modifier.padding(top = 2.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            ActionButton(Icons.Filled.Download, "Yedekle", Console.Surface, Console.Ink, Modifier.weight(1f)) {
                save.launch("karalak_kapanis_sesleri.zip")
            }
            ActionButton(Icons.Filled.Refresh, "Yedekten yükle", Console.Surface, Console.Ink, Modifier.weight(1f)) {
                load.launch(arrayOf("application/zip", "application/octet-stream"))
            }
        }
    }
}

/**
 * The app's promo video, made once by hand and kept on this phone: when it is chosen as the video's ending it plays,
 * with its own sound, after the drawing's video ends. It can be swapped or removed.
 */
@Composable
private fun PromoCard(r: RealUi, vm: StudioViewModel) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let { vm.importPromo(it) } }
    Column(Modifier.padding(top = 8.dp)) {
        Text(
            "Bir kez seçmen yeterli, telefonda saklanır. Dikey 9:16 en iyisi; başka oranda bir video kırpılmadan, arkası bulanık olarak yerleşir. En fazla ${RealTimeline.PROMO_MAX / 1000} saniyesi girer.",
            style = Console.Small
        )
        if (r.promo != null) Text(String.format(Locale("tr"), "%.1f sn", r.promoMs / 1000f) + " · önizlemede en sonda görünür", style = Console.Subtitle, modifier = Modifier.padding(top = 6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            ActionButton(Icons.Filled.Movie, if (r.promoBusy) "Ekleniyor…" else if (r.promo == null) "Tanıtım videosu seç" else "Değiştir", Console.Surface, Console.Ink, Modifier.weight(1.4f)) {
                if (!r.promoBusy) picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
            }
            if (r.promo != null) ActionButton(Icons.Filled.Refresh, "Kaldır", Console.Surface, Console.Critical, Modifier.weight(1f)) { vm.removePromo() }
        }
    }
}

/**
 * The app's music under the video: none or one of its tracks, each to hear first, and how loud. Under Karalak's voice
 * it steps back on its own, a little under the clip, and it fades out at the end.
 */
@Composable
private fun MusicCard(reveal: RealReveal?, musicOn: Boolean, playing: String?, custom: List<String>, vm: StudioViewModel) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { vm.addMusic(it) } }
    ConsoleCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Müziği videoya göm", style = Console.Subtitle)
                Text(
                    if (musicOn) "Açıkken müzik videoya karışır." else "Kapalı: video müziksiz çıkar, dosya adı “_muziksiz” biter. Trend sesi TikTok/Instagram'da sen eklersin; kanca, kapanış ve klibin kendi sesi kalır.",
                    style = Console.Small
                )
            }
            Switch(checked = musicOn, onCheckedChange = vm::setMusicOn, colors = SwitchDefaults.colors(checkedTrackColor = Console.Accent))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Uygulamanın müziklerinden biri videonun altında çalar. Karalak konuşurken kısılır, klip boyunca biraz alçalır, sonda yavaşça biter. " +
                "▶ ile önce dinleyebilirsin.",
            style = Console.Small
        )
        val chosen = reveal?.music
        val options = listOf<String?>(null) + RealMusic.TRACKS.map { it.first } + custom
        options.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                row.forEach { name ->
                    val on = name == chosen
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (on) Console.AccentSoft else Console.Surface,
                        border = BorderStroke(if (on) 1.5.dp else 1.dp, if (on) Console.Accent else Console.Border),
                        modifier = Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(10.dp)).clickable(enabled = reveal != null) { vm.setMusic(name) }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 12.dp)) {
                            Text(name?.let { RealMusic.label(it) } ?: "Müzik yok", style = Console.Subtitle, modifier = Modifier.weight(1f))
                            if (name != null && name in custom) {
                                IconButton(onClick = { vm.removeMusic(name) }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Sil", tint = Console.Critical, modifier = Modifier.size(18.dp))
                                }
                            }
                            if (name != null) {
                                IconButton(onClick = { vm.listenMusic(name) }, modifier = Modifier.size(40.dp)) {
                                    Icon(if (playing == name) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = if (playing == name) "Durdur" else "Dinle", tint = Console.Ink)
                                }
                            }
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        ActionButton(Icons.Filled.AddCircle, "Kendi müziğini ekle", Console.Surface, Console.Ink, Modifier.fillMaxWidth().padding(top = 8.dp)) {
            picker.launch(arrayOf("audio/*"))
        }
        Text("Telefondaki bir müzik dosyası (en fazla 25 MB) uygulamaya kopyalanır ve bir kez eklemen yeter; listede kalır. Telifli şarkı sosyal medyada sesi kapatılmasına yol açabilir.", style = Console.Small, modifier = Modifier.padding(top = 4.dp))
        if (reveal != null && chosen != null) {
            LabeledSlider("Müzik sesi", reveal.musicVolume, 0f..RealMix.MAX_VOLUME) { vm.setMusicVolume(it) }
        }
    }
}

/**
 * One of Karalak's lines in the video of the project's [RealProject.opening] drawing: the assistant's ideas for it,
 * each to rewrite, voice and put into the video, the line now in the video, and one's own line to add. [intro] says
 * where the line goes.
 */
@Composable
private fun LineCard(kind: RealLine, p: RealProject, st: StudioState, vm: StudioViewModel, intro: @Composable () -> Unit) {
    val r = st.real
    val opening = p.opening
    val drawing = opening?.let { p.drawing(it.sampleId) }
    val line = opening?.line(kind) ?: VoicedLine()
    val hook = kind == RealLine.Hook
    val end = kind == RealLine.End
    val count = if (hook) RealCopy.HOOK_COUNT else RealCopy.OUTRO_COUNT
    val name = when (kind) { RealLine.Hook -> "kanca"; RealLine.Narration -> "anlatıcı"; else -> "kapanış" }
    ConsoleCard(Modifier.fillMaxWidth()) {
        intro()
        drawing?.let { Text("Videodaki çizim: ${it.word}" + if (it.seconds > 0) " · ${it.seconds} sn" else "", style = Console.Subtitle, modifier = Modifier.padding(top = 6.dp)) }
        Spacer(Modifier.height(8.dp))
        // the closing picture's lines are ready-made: there is nothing for the assistant to write
        if (!end) DarkButton(
            text = if (r.lineWriting == kind) "Asistan yazıyor…" else if (line.options.isEmpty()) "Asistan $count $name yazsın" else "$count yeni $name yazsın",
            busy = r.lineWriting == kind,
            enabled = r.lineWriting == null && opening != null
        ) { vm.writeLines(kind) }
        r.lineErrors[kind]?.let { Text(it, style = Console.Small.copy(color = Console.Critical), modifier = Modifier.padding(top = 4.dp)) }
        if (line.text.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(if (hook) "Videodaki kanca" else if (kind == RealLine.Narration) "Videodaki anlatıcı" else if (end) "Kapanış görselinin üstündeki cümle" else "Videodaki kapanış", style = Console.Label)
                    Text(line.text, style = Console.Subtitle)
                    Text(
                        if (line.voice != null) "Sesli · " + String.format(Locale("tr"), "%.1f sn", line.voiceMs / 1000f) + if (end) " · görsel ≈" + String.format(Locale("tr"), "%.1f sn", RealTimeline.endCardMs(line.voiceMs) / 1000f) + " kalır" else ""
                        else if (hook) "Sessiz: kanca yazılmadığı için videoda duyulmaz; seslendirip “Videoya ekle”ye bas."
                        else if (end) "Sessiz: görsel ${RealTimeline.END_CARD_SILENT_MS / 1000} saniye durur; seslendirip “Videoya ekle”ye basarsan Karalak söyler, görsel o kadar kalır."
                        else "Sessiz: yalnızca alt yazı; seslendirip “Videoya ekle”ye basarsan Karalak söyler.",
                        style = Console.Small
                    )
                }
                if (line.voice != null) IconButton(onClick = { vm.listenLine(kind, line.text) }) { Icon(Icons.Filled.PlayArrow, contentDescription = "Dinle", tint = Console.Ink) }
                SmallLink("Kaldır", Console.Critical) { vm.clearLine(kind) }
            }
        }
        if (line.options.isNotEmpty()) {
            Text(
                "Düzenle, seslendir, beğendiğini “Videoya ekle” ile sesiyle birlikte videoya koy. Seslendirmeden, sessiz koymak için yazısına dokun.",
                style = Console.Small, modifier = Modifier.padding(top = 10.dp)
            )
            line.options.forEach { o ->
                LineOption(
                    text = o,
                    inVideo = o == line.text,
                    voicedInVideo = o == line.text && line.voice != null,
                    voiced = o in r.lineVoiced[kind].orEmpty(),
                    voicing = r.lineVoicing == (kind to o),
                    canVoice = r.lineVoicing == null && st.eleven.keySet,
                    credits = st.eleven.spec.credits(RealCopy.spoken(kind, o)),
                    onChoose = { vm.chooseLine(kind, o) },
                    onEdit = { vm.editLineOption(kind, o, it) },
                    onVoice = { vm.voiceLine(kind, o) },
                    onListen = { vm.listenLine(kind, o) },
                    onAdd = { vm.addLineToVideo(kind, o) }
                )
            }
        }
        if (!st.eleven.keySet) Text("Seslendirmek için Ayarlar sekmesinde ElevenLabs anahtarını gir.", style = Console.Small, modifier = Modifier.padding(top = 8.dp))
        var own by remember(kind) { mutableStateOf("") }
        Field(if (hook) "Kendi kancanı yaz" else if (end) "Kendi cümleni yaz" else "Kendi kapanışını yaz", own, { own = it.take(RealCopy.maxChars(kind)) })
        if (own.isNotBlank()) SmallLink("Listeye ekle") { vm.addLineOption(kind, own); own = "" }
    }
}

/**
 * One idea for a line and what can be done with it: rewrite it, voice it in ElevenLabs, then put it into the video
 * with its voice ("Seslendir" turns into "Videoya ekle" once the voice is made). Tapping the words puts it in silent.
 * The line in the video is outlined in orange.
 */
@Composable
private fun LineOption(
    text: String, inVideo: Boolean, voicedInVideo: Boolean, voiced: Boolean, voicing: Boolean, canVoice: Boolean, credits: Int,
    onChoose: () -> Unit, onEdit: (String) -> Unit, onVoice: () -> Unit, onListen: () -> Unit, onAdd: () -> Unit
) {
    var editing by remember(text) { mutableStateOf(false) }
    var draft by remember(text) { mutableStateOf(text) }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (inVideo) Console.AccentSoft else Console.SurfaceAlt,
        border = BorderStroke(if (inVideo) 1.5.dp else 1.dp, if (inVideo) Console.Accent else Console.Border),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    ) {
        Column(Modifier.padding(10.dp)) {
            if (editing) {
                Field(null, draft, { draft = it.take(RealCopy.maxChars(RealLine.End)) }, lines = 2)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    ActionButton(Icons.Filled.Check, "Kaydet", Console.Accent, Color.White, Modifier.weight(1f)) { onEdit(draft); editing = false }
                    ActionButton(Icons.Filled.Close, "Vazgeç", Console.Surface, Console.Ink, Modifier.weight(1f)) { draft = text; editing = false }
                }
                if (voiced || voicedInVideo) Text("Değişen kanca yeniden seslendirilir.", style = Console.Small, modifier = Modifier.padding(top = 4.dp))
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).clickable(onClick = onChoose)) {
                    Text(text, style = Console.Body, modifier = Modifier.weight(1f).padding(vertical = 2.dp))
                    if (inVideo) Text(if (voicedInVideo) "Videoda · sesli" else "Videoda · sessiz", style = Console.Label.copy(color = Console.Accent), modifier = Modifier.padding(start = 6.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    ActionButton(Icons.Filled.Edit, "Düzenle", Console.Surface, Console.Ink, Modifier.weight(1f)) { draft = text; editing = true }
                    when {
                        voicing -> ActionButton(Icons.Filled.GraphicEq, "Seslendiriliyor…", Console.InkMuted, Color.White, Modifier.weight(1.3f)) {}
                        voicedInVideo -> ActionButton(Icons.Filled.Check, "Videoda", Console.GoodSoft, Console.GoodText, Modifier.weight(1.3f)) {}
                        voiced -> ActionButton(Icons.Filled.AddCircle, "Videoya ekle", Console.Accent, Color.White, Modifier.weight(1.3f), onAdd)
                        else -> ActionButton(Icons.AutoMirrored.Filled.VolumeUp, "Seslendir", if (canVoice) Console.Dark else Console.InkMuted, Color.White, Modifier.weight(1.3f)) {
                            if (canVoice) onVoice()
                        }
                    }
                    if (voiced || voicedInVideo) {
                        IconButton(onClick = onListen, modifier = Modifier.size(46.dp)) { Icon(Icons.Filled.PlayArrow, contentDescription = "Dinle", tint = Console.Ink) }
                    }
                }
                if (!voiced && !voicedInVideo && !voicing) Text("Seslendirmek ~$credits ElevenLabs kredisi; bir kez ödenir, telefonda saklanır.", style = Console.Small, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

private fun megabytes(bytes: Long): String = String.format(Locale.US, "%.1f MB", bytes / 1_048_576.0)

/** One kept piece: its picture (or the drawing), a play mark when it has a clip, and a round tick that adds it to the next video. */
@Composable
private fun LibraryThumb(item: LibraryItem, order: Int, vm: StudioViewModel, modifier: Modifier) {
    val on = order >= 0
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Console.Surface,
        border = BorderStroke(if (on) 2.dp else 1.dp, if (on) Console.Accent else Console.Border),
        modifier = modifier.clip(RoundedCornerShape(12.dp)).clickable { vm.openLibraryItem(item.id) }
    ) {
        Column {
            Box(Modifier.fillMaxWidth().aspectRatio(0.75f).background(Console.Grid)) {
                val image = item.reveal.image
                if (image != null) AsyncImage(model = vm.mediaFile(image), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else StrokeCanvas(strokes = item.drawing.strokes, modifier = Modifier.fillMaxSize().padding(8.dp).background(Color.White), strokeWidthPx = 3f)
                if (item.hasClip) Text("▶", style = Console.Subtitle.copy(color = Color.White), modifier = Modifier.align(Alignment.BottomStart).padding(8.dp))
                Box(
                    Modifier.padding(6.dp).size(30.dp).clip(CircleShape)
                        .background(if (on) Console.Accent else Color.White.copy(alpha = 0.85f))
                        .clickable { vm.toggleLibrarySelect(item.id) }
                        .align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center
                ) { Text(if (on) "${order + 1}" else "", style = Console.Label.copy(color = Color.White)) }
            }
            Text(
                item.drawing.word.ifBlank { "—" }, style = Console.Small, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp), textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Where this piece was posted and how it did. A posting is noted when the video is shared; its numbers are typed in a
 * day or two later from the platform's own analytics: views, the share still watching at 3 seconds, the share who
 * watched to the end, shares and installs.
 */
@Composable
private fun PostsSection(item: LibraryItem, vm: StudioViewModel) {
    Column(Modifier.fillMaxWidth().padding(top = 14.dp)) {
        Text("Paylaşımlar ve sonuçlar", style = Console.Subtitle)
        if (item.posts.isEmpty()) {
            Text("Bu kayıttan yapılan video paylaşıldığında burada görünür; sonuçları sen girersin.", style = Console.Small)
        }
        item.posts.forEach { post ->
            key(post.id) {
                Surface(
                    shape = RoundedCornerShape(10.dp), color = Console.Surface, border = BorderStroke(1.dp, Console.Border),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Column(Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                val day = java.text.SimpleDateFormat("d MMM HH:mm", Locale("tr")).format(java.util.Date(post.postedAt))
                                Text("${post.social.label} · $day · " + if (post.opening == RealOpening.Teaser.name) "önce gerçek hâl" else "klasik açılış", style = Console.Label)
                                if (post.hook.isNotBlank()) Text("“${post.hook}”", style = Console.Small)
                            }
                            SmallLink("Sil", Console.Critical) { vm.deletePost(item.id, post.id) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.weight(1f)) { NumberField("İzlenme", post.views) { v -> vm.updatePost(item.id, post.id) { it.copy(views = v) } } }
                            Box(Modifier.weight(1f)) { NumberField("3 sn tutma %", post.hold3s, max = 100) { v -> vm.updatePost(item.id, post.id) { it.copy(hold3s = v) } } }
                            Box(Modifier.weight(1f)) { NumberField("Bitiren %", post.finished, max = 100) { v -> vm.updatePost(item.id, post.id) { it.copy(finished = v) } } }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.weight(1f)) { NumberField("Paylaşım", post.shares) { v -> vm.updatePost(item.id, post.id) { it.copy(shares = v) } } }
                            Box(Modifier.weight(1f)) { NumberField("Kurulum", post.installs) { v -> vm.updatePost(item.id, post.id) { it.copy(installs = v) } } }
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

/** A whole number typed in (empty is 0), kept to [max] and to a sensible length. */
@Composable
private fun NumberField(label: String, value: Int, max: Int = 999_999_999, onChange: (Int) -> Unit) {
    Field(label, if (value == 0) "" else value.toString(), { raw -> onChange((raw.filter { it.isDigit() }.take(9).toLongOrNull() ?: 0L).coerceAtMost(max.toLong()).toInt()) })
}

/**
 * What the postings say about the hooks: for each, the share still watching at 3 seconds and the installs it brought,
 * over the posts with enough views to count. This is the list to write the next hooks from.
 */
@Composable
private fun HookResults(library: List<LibraryItem>) {
    val results = PostStats.bestHooks(library.flatMap { it.posts }).take(5)
    if (results.isEmpty()) return
    ConsoleCard(Modifier.fillMaxWidth()) {
        Text("Kanca sonuçları", style = Console.Subtitle)
        Text("${PostStats.MIN_VIEWS}+ izlenmeli paylaşımlardan, 3. saniyede izlemeye devam edenlere göre sıralı.", style = Console.Small)
        results.forEachIndexed { i, r ->
            Text("${i + 1}. “${r.hook}”", style = Console.Label, modifier = Modifier.padding(top = 8.dp))
            Text("3 sn tutma %${r.hold3s} · bitiren %${r.finished} · ${r.installs} kurulum · ${r.views} izlenme · ${r.posts} paylaşım", style = Console.Small)
        }
    }
}

/** A kept piece opened to watch: its clip playing, its picture below, and the way to delete it. */
@Composable
private fun LibraryViewer(item: LibraryItem, vm: StudioViewModel) {
    AlertDialog(
        onDismissRequest = { vm.openLibraryItem(null) },
        title = { Text(item.drawing.word.ifBlank { "Kayıt" }) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                item.reveal.clip?.let { clip ->
                    val file = vm.libraryFile(clip)
                    AndroidView(
                        factory = { ctx -> VideoView(ctx).apply { setOnPreparedListener { mp -> mp.isLooping = true; start() } } },
                        update = { v -> if (v.tag != file.path) { v.tag = file.path; v.setVideoURI(Uri.fromFile(file)) } },
                        modifier = Modifier.width(220.dp).aspectRatio(9f / 16f).clip(RoundedCornerShape(12.dp))
                    )
                }
                item.reveal.image?.let { image ->
                    Spacer(Modifier.height(8.dp))
                    AsyncImage(model = vm.mediaFile(image), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.width(220.dp).aspectRatio(9f / 16f).clip(RoundedCornerShape(12.dp)))
                }
                if (item.reveal.image == null && item.reveal.clip == null) {
                    Box(Modifier.size(200.dp).background(Color.White)) { StrokeCanvas(strokes = item.drawing.strokes, modifier = Modifier.fillMaxSize().padding(8.dp), strokeWidthPx = 4f) }
                }
                Text(
                    (if (item.drawing.named) "@${item.drawing.nickname}" else "Anonim oyuncu") + " · " + megabytes(vm.libraryBytesOf(item)),
                    style = Console.Small, modifier = Modifier.padding(top = 8.dp)
                )
                PostsSection(item, vm)
            }
        },
        confirmButton = { TextButton(onClick = { vm.openLibraryItem(null) }) { Text("Kapat") } },
        dismissButton = { TextButton(onClick = { vm.deleteLibraryItem(item.id) }) { Text("Sil", color = Console.Critical) } }
    )
}

/**
 * What the card says about the player, changeable by hand: the name (empty: "Anonim oyuncu"), the frame (any frame,
 * whatever the player's level) and the picture in the round slot (from the gallery, or the default).
 */
@Composable
private fun PlayerEditor(d: RealDrawing?, vm: StudioViewModel, anyBusy: Boolean) {
    if (d == null) return
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let { vm.importPlayerPhoto(d.id, it) } }
    Field("Kullanıcı adı (boş: Anonim oyuncu)", d.nickname, { vm.setPlayerName(d.id, it) })
    // the level shown on the card, by hand (the frame follows it while it is on "Oto")
    var levelText by remember(d.id) { mutableStateOf(d.level.takeIf { it > 0 }?.toString().orEmpty()) }
    Field("Seviye (kartta isimin yanında görünür; çerçeve “Oto” ise buna göre seçilir)", levelText, { raw ->
        val digits = raw.filter { it.isDigit() }.take(3)
        levelText = digits
        digits.toIntOrNull()?.takeIf { it >= 1 }?.let { vm.setPlayerLevel(d.id, it) }
    })
    // how long the drawing is said to have taken: by hand, since a quick drawing is more exciting than the game's own time
    var secondsText by remember(d.id) { mutableStateOf(d.seconds.takeIf { it > 0 }?.toString().orEmpty()) }
    Field("Çizim süresi, saniye (videoda geri sayım ve cümleler bunu kullanır)", secondsText, { raw ->
        val digits = raw.filter { it.isDigit() }.take(2)
        secondsText = digits
        digits.toIntOrNull()?.takeIf { it >= 1 }?.let { vm.setDrawingSeconds(d.id, it) }
    })
    Text("Çerçeve", style = Console.Small, modifier = Modifier.padding(top = 4.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
        item(key = "auto") {
            FrameChoice(selected = d.frameId.isBlank(), onClick = { vm.setPlayerFrame(d.id, null) }) { Text("Oto", style = Console.Small) }
        }
        items(AvatarFrame.entries.filter { !it.retired }, key = { it.name }) { f ->
            FrameChoice(selected = d.frameId == f.name, onClick = { vm.setPlayerFrame(d.id, f.name) }) {
                Image(painterResource(f.drawableRes), contentDescription = f.name, modifier = Modifier.fillMaxSize().padding(2.dp))
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
        ActionButton(Icons.Filled.Download, if (d.photo == null) "Profil fotoğrafı seç" else "Fotoğrafı değiştir", Console.Surface, Console.Ink, Modifier.weight(1.4f)) {
            if (!anyBusy) photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        if (d.photo != null) ActionButton(Icons.Filled.Refresh, "Varsayılan", Console.Surface, Console.Critical, Modifier.weight(1f)) { vm.clearPlayerPhoto(d.id) }
    }
}

@Composable
private fun FrameChoice(selected: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Console.AccentSoft else Console.Surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Console.Accent else Console.Border),
        modifier = Modifier.size(58.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick)
    ) { Box(contentAlignment = Alignment.Center) { content() } }
}

@Composable
private fun RealThumb(strokes: List<List<DrawingPoint>>, label: String, order: Int, modifier: Modifier, onClick: () -> Unit) {
    val on = order >= 0
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Console.Surface,
        border = BorderStroke(if (on) 2.dp else 1.dp, if (on) Console.Accent else Console.Border),
        modifier = modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick)
    ) {
        Column {
            Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
                StrokeCanvas(strokes = strokes, modifier = Modifier.fillMaxSize().padding(6.dp), strokeWidthPx = 4f)
                if (on) {
                    Box(
                        Modifier.padding(5.dp).size(22.dp).clip(CircleShape).background(Console.Accent).align(Alignment.TopEnd),
                        contentAlignment = Alignment.Center
                    ) { Text("${order + 1}", style = Console.Label.copy(color = Color.White)) }
                }
            }
            Text(
                label, style = Console.Small, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp), textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MiniDoodle(strokes: List<List<DrawingPoint>>) {
    Box(Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Color.White)) {
        StrokeCanvas(strokes = strokes, modifier = Modifier.fillMaxSize().padding(3.dp), strokeWidthPx = 2f)
    }
}

/** Voice, mascot artwork, preview, making the video and the result: the same for every format. */
private fun LazyListScope.sharedSections(st: StudioState, vm: StudioViewModel, context: Context) {
    val real = st.format == StudioFormat.RealLife
    // "Gerçek Olsaydı": only the hook is ever said aloud, and only in ElevenLabs; its settings are here
    if (real) item { Step("♪", "Karalak'ın sesi (ElevenLabs)") }
    if (real) item {
        ConsoleCard(Modifier.fillMaxWidth()) {
            Text("Kancalar bu sesle seslendirilir. Ayarı değiştirirsen kanca yeniden seslendirilir; videoya eklenmiş ses değişmez.", style = Console.Small)
            ElevenSettings(st, vm)
            st.voiceError?.let { Text(it, style = Console.Small.copy(color = Console.Critical)) }
        }
    }
    // 4. voice (the "Gerçek Olsaydı" video says only its hook: see above)
    if (!real) item { Step("♪", "Ses") }
    if (!real) item {
        ConsoleCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Karalak konuşsun", style = Console.Subtitle)
                    Text("Kapalıyken yalnızca altyazı.", style = Console.Small)
                }
                Switch(checked = st.voiceOn, onCheckedChange = vm::setVoiceOn, colors = SwitchDefaults.colors(checkedTrackColor = Console.Accent))
            }
            if (st.voiceOn) {
                Spacer(Modifier.height(8.dp))
                Segmented(
                    options = listOf(VoiceEngine.ElevenLabs.name to "ElevenLabs", VoiceEngine.Phone.name to "Telefon sesi"),
                    selected = st.voiceEngine.name,
                    onSelect = { vm.setVoiceEngine(VoiceEngine.valueOf(it)) },
                    modifier = Modifier.fillMaxWidth()
                )
                if (st.voiceEngine == VoiceEngine.ElevenLabs) ElevenSettings(st, vm)
                else {
                    LabeledSlider("Ses inceliği", st.voicePitch, 0.8f..1.7f) { vm.setVoice(it, st.voiceRate) }
                    LabeledSlider("Konuşma hızı", st.voiceRate, 0.8f..1.4f) { vm.setVoice(st.voicePitch, it) }
                }
                SmallLink(if (st.eleven.busy) "Seslendiriliyor…" else "Sesi dinle") {
                    vm.listen(st.forgot.script?.hook ?: SpokenLine("Merhaba, ben Karalak! Çizimlerini tahmin etmeye geldim.", MascotEmotion.Excited))
                }
            }
            st.voiceError?.let { Text(it, style = Console.Small.copy(color = Console.Critical)) }
        }
    }

    // 5. mascot artwork
    if (!real) item { Step("☺", "Karalak'ın pozları") }
    if (!real) item {
        ConsoleCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (st.mascotLoading) "Yükleniyor…" else "${st.mascotPoses} poz yüklü", style = Console.Subtitle)
                    Text(
                        if (st.mascotPoses == 0) "Pozlar karalak.com'a eklenene kadar büyüteçli Karalak konuşurken sallanır."
                        else "Duygu ve ağız hareketleri karalak.com'daki pozlardan gelir.",
                        style = Console.Small
                    )
                }
                IconButton(onClick = vm::reloadMascot) { Icon(Icons.Filled.Refresh, contentDescription = "Yenile", tint = Console.InkSecondary) }
            }
        }
    }

    previewItems(st, vm)
    renderItems(st, vm)
    resultItems(st, vm, context)
}

/** The preview's step and card, for the formats that show it as a section of their own. */
private fun LazyListScope.previewItems(st: StudioState, vm: StudioViewModel) {
    item { Step("▶", "Önizleme", if (st.totalMs > 0) "≈ ${(st.totalMs + 999) / 1000} sn" else null) }
    item { PreviewCard(st, vm) }
}

/** The preview frame with its time slider. */
@Composable
private fun PreviewCard(st: StudioState, vm: StudioViewModel) {
    ConsoleCard(Modifier.fillMaxWidth()) {
        if (st.preview == null && st.totalMs == 0L) {
            Text(
                when (st.format) {
                    StudioFormat.RealLife -> "Önizleme için çizim seç ve Gemini videosunu yükle."
                    StudioFormat.Forgot -> "Önizleme için kendi çizimini tanıyamadığın bir çizim seç."
                    else -> "Önizleme için bir kelime seç."
                },
                style = Console.Small
            )
        } else {
            val ratio = 9f / 20f
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val img = st.preview
                if (img != null) Image(img, contentDescription = "Önizleme", contentScale = ContentScale.Fit, modifier = Modifier.width(220.dp).aspectRatio(ratio).clip(RoundedCornerShape(12.dp)))
                else Box(Modifier.width(220.dp).aspectRatio(ratio).background(Console.Grid, RoundedCornerShape(12.dp)))
            }
            if (st.totalMs > 0) {
                Slider(
                    value = st.previewMs.toFloat().coerceIn(0f, st.totalMs.toFloat()),
                    onValueChange = { vm.setPreviewTime(it.toLong()) },
                    valueRange = 0f..st.totalMs.toFloat(),
                    colors = SliderDefaults.colors(thumbColor = Console.Accent, activeTrackColor = Console.Accent),
                    modifier = Modifier.fillMaxWidth()
                )
                val seconds = ((st.totalMs + 999) / 1000).toInt()
                // viewers drop off past ~20 s: "Çizen Bile Bilemedi" says so as soon as the video runs over
                val tooLong = st.format == StudioFormat.Forgot && seconds > FORGOT_TARGET_SECONDS
                Text(
                    when {
                        st.format == StudioFormat.RealLife -> "≈ $seconds sn · kanca, video (üstünde Karalak kartı), kapanış, tanıtım ve bitiş görseli."
                        st.format == StudioFormat.Forgot && tooLong -> "≈ $seconds sn · $FORGOT_TARGET_SECONDS saniyeyi geçti, izlenme oranı düşebilir. Tahmin sayısını ya da cümleleri kısalt. Sesli videoda süre sesin uzunluğuna göre ayarlanır."
                        st.format == StudioFormat.Forgot -> "≈ $seconds sn · hedef 15–$FORGOT_TARGET_SECONDS sn. Sesli videoda süre sesin uzunluğuna göre ayarlanır."
                        else -> "Sesli videoda süreler sesin gerçek uzunluğuna göre ayarlanır."
                    },
                    style = if (tooLong) Console.Small.copy(color = Console.Critical) else Console.Small,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/** The "Videoyu oluştur" button (or its progress) and the error when it failed. */
private fun LazyListScope.renderItems(st: StudioState, vm: StudioViewModel) {
    item {
        if (st.rendering) {
            ConsoleCard(Modifier.fillMaxWidth()) {
                Text("${st.renderStage} · %${(st.progress * 100).toInt()}", style = Console.Subtitle)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { st.progress }, color = Console.Accent, trackColor = Console.Grid, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                SmallLink("Vazgeç", Console.Critical) { vm.cancelRender() }
            }
        } else {
            val voiceMissing = st.voiceOn && st.voiceEngine == VoiceEngine.ElevenLabs && st.eleven.missing > 0
            BigButton("Videoyu oluştur", enabled = st.ready && !voiceMissing) { vm.render() }
            if (st.ready && voiceMissing) {
                Text("Önce Ses bölümünde “Seslendir”e bas: ${st.eleven.missing} cümle, ~${st.eleven.missingCredits} kredi.", style = Console.Small, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
    st.renderError?.let { err ->
        item {
            Surface(shape = RoundedCornerShape(12.dp), color = Console.CriticalSoft, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Video oluşturulamadı", style = Console.Subtitle.copy(color = Console.Critical))
                    Text(err, style = Console.Small)
                }
            }
        }
    }
}

/** The finished video: to watch, share (with where it goes), save, and its post text. */
private fun LazyListScope.resultItems(st: StudioState, vm: StudioViewModel, context: Context) {
    val real = st.format == StudioFormat.RealLife
    st.video?.let { file ->
        item { Step("✓", "Hazır") }
        item {
            ConsoleCard(Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    AndroidView(
                        factory = { ctx -> VideoView(ctx).apply { setOnPreparedListener { mp -> mp.isLooping = true; start() } } },
                        update = { v -> if (v.tag != file.path) { v.tag = file.path; v.setVideoURI(Uri.fromFile(file)) } },
                        modifier = Modifier.width(230.dp).aspectRatio(9f / 16f).clip(RoundedCornerShape(12.dp))
                    )
                }
                Spacer(Modifier.height(10.dp))
                // where it is going to be posted decides what the post text ends with (a link only where it can be tapped)
                val platform = if (real) st.real.platform else null
                if (real) {
                    Text("Nerede paylaşacaksın?", style = Console.Label)
                    Segmented(
                        options = SocialPlatform.entries.map { it.name to it.label },
                        selected = st.real.platform.name,
                        onSelect = { vm.setPlatform(SocialPlatform.valueOf(it)) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionButton(Icons.Filled.Share, "Paylaş", Console.Accent, Color.White, Modifier.weight(1.3f)) {
                        if (real) vm.logPost()
                        share(context, file, st.caption, st.hashtags, platform, st.lang)
                    }
                    ActionButton(Icons.Filled.Download, "Galeriye", Console.Surface, Console.Ink, Modifier.weight(1f)) { vm.saveToGallery() }
                    ActionButton(Icons.Filled.ContentCopy, "Metin", Console.Surface, Console.Ink, Modifier.weight(1f)) { copy(context, postText(st.caption, st.hashtags, platform, st.lang)) }
                }
                Field("Açıklama", st.caption, vm::setCaption, lines = 3)
                Field("Etiketler", st.hashtags, vm::setHashtags, lines = 2)
                Text(
                    "Paylaşırken açıklama panoya kopyalanır; TikTok/Instagram'da yapıştır. Arka plan müziğini orada, sesi kısık ekle.",
                    style = Console.Small, modifier = Modifier.padding(top = 6.dp)
                )
                if (real) {
                    Text(
                        "TikTok ve Instagram açıklamasında link tıklanmaz: açıklama “Link profilde” der, takip kodlu Play linkini profilin biyografisine koyarsın. " +
                            "YouTube Shorts'ta takip kodlu link açıklamada tıklanır. Google Play Console'da kurulumlar kaynağa göre (utm_source) görünür.",
                        style = Console.Small, modifier = Modifier.padding(top = 6.dp)
                    )
                    SmallLink("Profil linkini kopyala (${st.real.platform.label})") { copy(context, PostLink.url(st.real.platform)) }
                }
            }
        }
    }
}

/** ElevenLabs: the key (kept on this phone only), the voice, the model and how lively it speaks. */
@Composable
private fun ElevenSettings(st: StudioState, vm: StudioViewModel) {
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
private fun LineEditor(line: SpokenLine, onText: (String) -> Unit, onEmotion: (MascotEmotion) -> Unit, onListen: () -> Unit) {
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
private fun Step(number: String, title: String, trailing: String? = null) {
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
private fun Thumb(d: StudioDrawing, order: Int, modifier: Modifier, onClick: () -> Unit) {
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
private fun PreciseSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
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
private fun StepButton(symbol: String, onStep: () -> Unit) {
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
private fun LabeledSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
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
private fun SmallLink(text: String, color: Color = Console.Accent, onClick: () -> Unit) {
    Text(text, style = Console.Label.copy(color = color), modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick).padding(vertical = 6.dp, horizontal = 2.dp))
}

@Composable
private fun DarkButton(text: String, busy: Boolean, enabled: Boolean, onClick: () -> Unit) =
    ConsoleButton(text, onClick, Modifier.fillMaxWidth(), enabled = enabled, icon = Icons.Filled.AutoAwesome, height = 44.dp, style = ConsoleButtonStyle.Dark, loading = busy)

@Composable
private fun BigButton(text: String, enabled: Boolean, onClick: () -> Unit) =
    ConsoleButton(text, onClick, Modifier.fillMaxWidth(), enabled = enabled, icon = Icons.Filled.Movie, height = 50.dp)

@Composable
private fun Field(label: String?, value: String, onChange: (String) -> Unit, lines: Int = 1, secret: Boolean = false, numeric: Boolean = false) =
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
private fun ActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, bg: Color, fg: Color, modifier: Modifier, onClick: () -> Unit) {
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

private fun postText(caption: String, hashtags: String, platform: SocialPlatform? = null, lang: StudioLang = StudioLang.Tr): String {
    val text = listOf(caption.trim(), hashtags.trim()).filter { it.isNotEmpty() }.joinToString("\n\n")
    // the viewer is sent to the app's Google Play page: by the link itself where the platform makes it tappable, by "link in bio" elsewhere
    return if (platform != null) PostLink.withFooter(text, platform, lang) else text
}

private fun copy(context: Context, text: String) {
    runCatching {
        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Karalak", text))
        Toast.makeText(context, "Metin kopyalandı", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Hands a picture to ChatGPT or Gemini, with the prompt in the message and on the clipboard to paste beside it.
 * Gemini's app is tried directly first; the share sheet is the way out when it is not installed.
 */
private fun shareImage(context: Context, file: File, prompt: String, target: ShareTarget) {
    runCatching {
        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Karalak", prompt))
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, prompt)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val title = if (target == ShareTarget.Gemini) "Gemini'ye gönder" else "ChatGPT'ye gönder"
        var started = false
        if (target == ShareTarget.Gemini) {
            started = runCatching {
                context.startActivity(Intent(send).setPackage(GEMINI_PACKAGE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }.isSuccess
        }
        if (!started) context.startActivity(Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val hint = if (target == ShareTarget.Gemini) "İstem panoya kopyalandı; Gemini'de Video'yu seçip gönder" else "İstem panoya kopyalandı; ChatGPT'de yapıştır"
        Toast.makeText(context, hint, Toast.LENGTH_LONG).show()
    }.onFailure { Toast.makeText(context, "Paylaşılamadı: ${it.message}", Toast.LENGTH_LONG).show() }
}

private const val GEMINI_PACKAGE = "com.google.android.apps.bard"

/** Hands several pictures to the share sheet as one swipe-through post (Instagram, WhatsApp…), post text on the clipboard. */
private fun shareImages(context: Context, files: List<File>, text: String) {
    runCatching {
        if (text.isNotEmpty()) (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Karalak", text))
        val uris = ArrayList(files.map { FileProvider.getUriForFile(context, context.packageName + ".fileprovider", it) })
        val send = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            if (text.isNotEmpty()) putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Görselleri paylaş").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        if (text.isNotEmpty()) Toast.makeText(context, "Açıklama panoya kopyalandı", Toast.LENGTH_SHORT).show()
    }.onFailure { Toast.makeText(context, "Paylaşılamadı: ${it.message}", Toast.LENGTH_LONG).show() }
}

/** Hands the video to the system share sheet (Instagram, TikTok, WhatsApp…), with the post text on the clipboard. */
private fun share(context: Context, file: File, caption: String, hashtags: String, platform: SocialPlatform? = null, lang: StudioLang = StudioLang.Tr) {
    runCatching {
        val text = postText(caption, hashtags, platform, lang)
        if (text.isNotEmpty()) {
            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Karalak", text))
        }
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            if (text.isNotEmpty()) putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Videoyu paylaş").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        if (text.isNotEmpty()) Toast.makeText(context, "Açıklama panoya kopyalandı", Toast.LENGTH_SHORT).show()
    }.onFailure { Toast.makeText(context, "Paylaşılamadı: ${it.message}", Toast.LENGTH_LONG).show() }
}
