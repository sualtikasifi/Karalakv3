package com.sualtikasifi.cizimhafiza.presentation.reports.studio

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
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Movie
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleCard
import com.sualtikasifi.cizimhafiza.presentation.reports.console.OpenRouterClient
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Segmented
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Tag
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.guess.GuessScript
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.guess.SpokenLine
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MascotEmotion
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealProject
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealReveal
import java.io.File
import java.util.Locale

/**
 * The video studio screen. Each format runs top to bottom — choose the drawings, let Karalak write
 * its lines, adjust any of them — and then the shared steps: the voice, a look at any moment of
 * the video, making it, sharing it.
 */
@Composable
fun StudioPanel(onBack: () -> Unit, viewModel: StudioViewModel = hiltViewModel()) {
    val st by viewModel.state.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(st.message) {
        st.message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show(); viewModel.clearMessage() }
    }
    LaunchedEffect(st.real.share) {
        st.real.share?.let { sh -> shareImage(context, sh.file, sh.prompt); viewModel.clearShare() }
    }
    st.real.confirmWord?.let { w ->
        AlertDialog(
            onDismissRequest = viewModel::cancelRealWord,
            title = { Text("Yeni proje başlasın mı?") },
            text = {
                Text("Şu anki “${st.real.project?.script?.word.orEmpty()}” projesindeki görseller ve videolar silinir, “${w.word}” için yeni proje başlar.")
            },
            confirmButton = { TextButton(onClick = { viewModel.selectRealWord(w, confirmed = true) }) { Text("Başlat", color = Console.Critical) } },
            dismissButton = { TextButton(onClick = viewModel::cancelRealWord) { Text("Vazgeç") } }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Araçlar", tint = Console.Ink) }
                Column(Modifier.weight(1f)) {
                    Text("Video Stüdyosu", style = Console.Title)
                    Text("Karalak'lı dikey videolar · 1080×1920 · sesli", style = Console.Small)
                }
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
        when (st.format) {
            StudioFormat.Guess -> { guessSections(st, viewModel); sharedSections(st, viewModel, context) }
            StudioFormat.Memory -> { memorySections(st, viewModel); sharedSections(st, viewModel, context) }
            StudioFormat.RealLife -> { realSections(st, viewModel, context); sharedSections(st, viewModel, context) }
        }
    }
}

private fun LazyListScope.guessSections(st: StudioState, vm: StudioViewModel) {
    // 1. drawings
    item { Step("1", "Çizimler", "${st.selected.size} seçili · en iyisi 3") }
    item {
        Segmented(
            options = listOf(GuessSource.Mine.name to "Benim çizimlerim", GuessSource.Players.name to "İzin veren oyuncular"),
            selected = st.source.name,
            onSelect = { vm.setSource(GuessSource.valueOf(it)) },
            modifier = Modifier.fillMaxWidth()
        )
    }
    if (st.source == GuessSource.Players) featuredPicker(st, vm)
    when {
        st.source == GuessSource.Mine && st.loading -> item { Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Console.Accent) } }
        st.source == GuessSource.Mine && st.drawings.isEmpty() -> item {
            ConsoleCard(Modifier.fillMaxWidth()) { Text("Bu telefonda kayıtlı çizim yok. Birkaç tur oyna, sonra buraya dön.", style = Console.Small) }
        }
        else -> st.pool.take(90).chunked(3).forEach { row ->
            item(key = "row-" + row.first().id) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { d -> Thumb(d, st.selected.indexOf(d.id), Modifier.weight(1f)) { vm.toggle(d) } }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }

    // 2. who is on the right-hand tile
    item { Step("2", "Oyuncu kartı") }
    if (st.source == GuessSource.Players) item {
        ConsoleCard(Modifier.fillMaxWidth()) {
            Text(st.player?.let { "${it.name} · Sv. ${it.level}" } ?: "Önce bir oyuncu seç", style = Console.Subtitle)
            Text("Oyuncu videolarda yer almayı kabul etti: adı, seviyesi ve çerçevesi görünür; fotoğrafı yerine dinozor avatarı.", style = Console.Small)
        }
    } else item {
        ConsoleCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(st.player?.let { "${it.name} · Sv. ${it.level}" } ?: "Yükleniyor…", style = Console.Subtitle)
                    Text(
                        if (st.showName) "Kendi adın, seviyen, çerçeven ve fotoğrafın görünür."
                        else "Ad yerine \"Oyuncu\", fotoğraf yerine Karalak görünür.",
                        style = Console.Small
                    )
                }
                Switch(checked = st.showName, onCheckedChange = vm::setShowName, colors = SwitchDefaults.colors(checkedTrackColor = Console.Accent))
            }
        }
    }

    // 3. Karalak's lines
    item { Step("3", "Karalak'ın senaryosu") }
    item {
        ConsoleCard(Modifier.fillMaxWidth()) {
            DarkButton(
                text = when {
                    st.writing -> st.writeProgress ?: "Yazılıyor…"
                    st.script == null -> "Karalak çizimlere baksın"
                    else -> "Baştan yazdır"
                },
                busy = st.writing,
                enabled = st.canWrite
            ) { vm.writeScript() }
            Text(
                "Karalak her çizimi üç anda görür (üçte biri, üçte ikisi, bitmiş) ve cevabı bilmeden tahmin eder. Tahminler gerçektir.",
                style = Console.Small, modifier = Modifier.padding(top = 6.dp)
            )
            st.aiCost?.let { Text("Bu yazımın maliyeti: " + OpenRouterClient.usd(it), style = Console.Small) }
            st.writeError?.let { Text(it, style = Console.Small.copy(color = Console.Critical), modifier = Modifier.padding(top = 4.dp)) }
        }
    }
    st.script?.let { sc -> scriptEditor(sc, st, vm) }
}

/** The players who turned on "Videolarda öne çık", to pick whose drawings Karalak looks at. */
private fun LazyListScope.featuredPicker(st: StudioState, vm: StudioViewModel) {
    item {
        ConsoleCard(Modifier.fillMaxWidth()) {
            Text(
                "Yalnızca Hesap ekranında “Karalak videolarında öne çık”ı açan oyuncular. Çizimler onaylanmış hızlı eşleş havuzundan gelir.",
                style = Console.Small
            )
            Spacer(Modifier.height(8.dp))
            DarkButton(
                text = if (st.featuredLoading) (st.featuredStatus ?: "Aranıyor…") else if (st.featured.isEmpty()) "Oyuncuları bul" else "Yeniden ara",
                busy = st.featuredLoading,
                enabled = !st.featuredLoading
            ) { vm.loadFeatured() }
            if (!st.featuredLoading) st.featuredStatus?.let { Text(it, style = Console.Small, modifier = Modifier.padding(top = 4.dp)) }
        }
    }
    st.featured.take(40).forEach { f ->
        item(key = "fp-" + f.player.uid) {
            WordRow(f.player.nickname, "Sv. ${f.player.level} · ${f.drawings.size} çizim", on = st.featuredUid == f.player.uid, upper = false) { vm.selectFeatured(f.player.uid) }
        }
    }
}

/** "Hafıza Tuzağı": the archive, a word, its drawings, and Karalak's lines. */
private fun LazyListScope.memorySections(st: StudioState, vm: StudioViewModel) {
    val m = st.memory
    item { Step("1", "Veri", "${m.roundsSeen} tur arşivde") }
    item {
        ConsoleCard(Modifier.fillMaxWidth()) {
            Text(
                "Panel, incelediğin her hızlı eşleş turunu ve günlük meydan okumayı bu telefonda isimsiz olarak sayar. " +
                    "Günlük denemeler onaylanınca sunucudan silinir; sayıları burada kalır.",
                style = Console.Small
            )
            Spacer(Modifier.height(8.dp))
            DarkButton(
                text = if (m.scanning) (m.scanStatus ?: "Okunuyor…") else "Havuzu ve bekleyenleri tara",
                busy = m.scanning,
                enabled = !m.scanning
            ) { vm.scanAll() }
            if (!m.scanning) m.scanStatus?.let { Text(it, style = Console.Small, modifier = Modifier.padding(top = 4.dp)) }
        }
    }

    item { Step("2", "Kelime", "en az 5 çizim") }
    if (m.words.isEmpty()) {
        item { ConsoleCard(Modifier.fillMaxWidth()) { Text("Henüz 5 kez çizilmiş kelime yok. Havuzu tara ya da kuyruğu incele.", style = Console.Small) } }
    } else {
        m.words.take(40).forEach { w ->
            item(key = "word-" + w.key) {
                val on = m.selected?.key == w.key
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (on) Console.AccentSoft else Console.Surface,
                    border = BorderStroke(if (on) 1.5.dp else 1.dp, if (on) Console.Accent else Console.Border),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { vm.selectWord(w) }
                ) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(w.word.uppercase(Locale("tr")), style = Console.Subtitle)
                            Text(
                                "${w.drawn} çizim · ${w.forgot} tanıyamadı · ≈ ${w.avgDelaySec} sn · ${w.samples} örnek" +
                                    if (w.daily > 0) " · ${w.daily} günlük" else "",
                                style = Console.Small
                            )
                        }
                        Tag(
                            "%${w.forgotPercent}",
                            fg = Color.White,
                            bg = if (w.drawn >= 50) Console.Critical else Console.InkMuted
                        )
                    }
                }
            }
        }
    }

    val script = m.script
    val stat = m.selected
    if (script != null && stat != null) {
        item {
            Step("3", "Çizimler", "${script.picks.size} seçili")
        }
        if (stat.drawn < 50) {
            item {
                Text(
                    "Bu kelime ${stat.drawn} kez çizilmiş. Videoda yüzde değil sayı gösterilir; yayın için 50 ve üzeri önerilir.",
                    style = Console.Small.copy(color = Console.Critical)
                )
            }
        }
        val forgotFirst = m.samples.sortedBy { it.correct }
        forgotFirst.take(48).chunked(3).forEach { row ->
            item(key = "ms-" + row.first().id) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { sample ->
                        val order = script.picks.indexOfFirst { it.sampleId == sample.id }
                        SampleThumb(sample, order, Modifier.weight(1f)) { vm.togglePick(sample) }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        item {
            ConsoleCard(Modifier.fillMaxWidth()) {
                DarkButton(
                    text = if (m.writing) "Karalak seçiyor…" else "Karalak seçsin ve yorumlasın",
                    busy = m.writing,
                    enabled = !m.writing
                ) { vm.writeMemory() }
                Text("En ilginç ve birbirinden farklı çizimleri seçer, her birine kısa bir cümle yazar. Sayıları değiştiremez.", style = Console.Small, modifier = Modifier.padding(top = 6.dp))
                m.writeError?.let { Text(it, style = Console.Small.copy(color = Console.Critical)) }
            }
        }
        item { Step("4", "Senaryo") }
        item {
            ConsoleCard(Modifier.fillMaxWidth()) {
                Text("Açılış (sayılar arşivden)", style = Console.Label)
                LineEditor(script.hook, { vm.editLine(LineRef.MemoryHook, it) }, { vm.editEmotion(LineRef.MemoryHook, it) }) { vm.listen(script.hook.text) }
                script.picks.forEachIndexed { i, p ->
                    Text("Çizim ${i + 1} yorumu (boş bırakılabilir)", style = Console.Label, modifier = Modifier.padding(top = 8.dp))
                    val l = p.comment ?: SpokenLine("", MascotEmotion.Laughing)
                    LineEditor(l, { vm.editLine(LineRef.MemoryPickLine(i), it) }, { vm.editEmotion(LineRef.MemoryPickLine(i), it) }) { vm.listen(l.text) }
                }
                Text("Ters köşe", style = Console.Label, modifier = Modifier.padding(top = 8.dp))
                LineEditor(script.paradox, { vm.editLine(LineRef.MemoryParadox, it) }, { vm.editEmotion(LineRef.MemoryParadox, it) }) { vm.listen(script.paradox.text) }
                Text("Kapanış", style = Console.Label, modifier = Modifier.padding(top = 8.dp))
                LineEditor(script.outro, { vm.editLine(LineRef.MemoryOutro, it) }, { vm.editEmotion(LineRef.MemoryOutro, it) }) { vm.listen(script.outro.text) }
            }
        }
    }
}

/** "Gerçek Olsaydı": a word, its drawings, Karalak's jokes, and the pictures and clips made elsewhere. */
private fun LazyListScope.realSections(st: StudioState, vm: StudioViewModel, context: Context) {
    val r = st.real
    item { Step("1", "Kelime", "en az 3 örnek çizim") }
    if (r.words.isEmpty()) {
        item {
            ConsoleCard(Modifier.fillMaxWidth()) {
                Text("Arşivde yeterli çizim yok. Çizimler, panelde incelediğin turlardan isimsiz olarak toplanır.", style = Console.Small)
                Spacer(Modifier.height(8.dp))
                DarkButton(
                    text = if (st.memory.scanning) (st.memory.scanStatus ?: "Okunuyor…") else "Havuzu ve bekleyenleri tara",
                    busy = st.memory.scanning,
                    enabled = !st.memory.scanning
                ) { vm.scanAll() }
            }
        }
    } else {
        r.words.take(30).forEach { w ->
            item(key = "rw-" + w.key) {
                WordRow(w.word, "${w.samples} örnek çizim · ${w.drawn} kez çizildi", on = r.project?.wordKey == w.key) { vm.selectRealWord(w) }
            }
        }
    }

    val p = r.project ?: return
    val sc = p.script
    val roastIds = sc.roasts.map { it.sampleId }
    val revealIds = sc.reveals.map { it.sampleId }.toSet()

    item { Step("2", "Çizimler", "${sc.roasts.size}/5 seçili · ${sc.reveals.size}/3 gerçeğe") }
    val pool = (p.drawings.map { it.id to it.strokes } + r.samples.map { it.id to it.strokes }).distinctBy { it.first }
    pool.take(48).chunked(3).forEach { row ->
        item(key = "rs-" + row.first().first) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (id, strokes) ->
                    RealThumb(strokes, roastIds.indexOf(id), id in revealIds, Modifier.weight(1f)) { vm.toggleRoast(id, strokes) }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
    item {
        ConsoleCard(Modifier.fillMaxWidth()) {
            DarkButton(
                text = if (r.writing) "Karalak bakıyor…" else "Karalak seçsin ve espri yapsın",
                busy = r.writing,
                enabled = !r.writing
            ) { vm.writeReal() }
            Text(
                "En komik ve birbirinden farklı 5 çizimi seçer, her birine espri yazar ve gerçeğe dönüşünce en eğlenceli olacak 3 tanesini işaretler. " +
                    "İstersen çizimleri elle seçip cümleleri kendin de yazabilirsin.",
                style = Console.Small, modifier = Modifier.padding(top = 6.dp)
            )
            st.aiCost?.let { Text("Son yazımın maliyeti: " + OpenRouterClient.usd(it), style = Console.Small) }
            r.writeError?.let { Text(it, style = Console.Small.copy(color = Console.Critical)) }
        }
    }

    item { Step("3", "Senaryo") }
    item {
        ConsoleCard(Modifier.fillMaxWidth()) {
            Text("Açılış", style = Console.Label)
            LineEditor(sc.hook, { vm.editLine(LineRef.RealHook, it) }, { vm.editEmotion(LineRef.RealHook, it) }) { vm.listen(sc.hook.text) }
            sc.roasts.forEachIndexed { i, roast ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
                    MiniDoodle(p.drawing(roast.sampleId)?.strokes.orEmpty())
                    Spacer(Modifier.width(8.dp))
                    Text("Çizim ${i + 1} esprisi", style = Console.Label, modifier = Modifier.weight(1f))
                    val on = roast.sampleId in revealIds
                    Tag(
                        if (on) "★ Gerçeğe" else "☆ Gerçeğe",
                        fg = if (on) Color.White else Console.Accent,
                        bg = if (on) Console.Accent else Console.AccentSoft,
                        modifier = Modifier.clip(RoundedCornerShape(50)).clickable { vm.toggleReveal(roast.sampleId) }
                    )
                }
                LineEditor(roast.line, { vm.editLine(LineRef.RealRoastLine(i), it) }, { vm.editEmotion(LineRef.RealRoastLine(i), it) }) { vm.listen(roast.line.text) }
            }
            Text("Geçiş (görseli olan dönüşüm varsa söylenir)", style = Console.Label, modifier = Modifier.padding(top = 10.dp))
            LineEditor(sc.bridge, { vm.editLine(LineRef.RealBridge, it) }, { vm.editEmotion(LineRef.RealBridge, it) }) { vm.listen(sc.bridge.text) }
        }
    }

    item { Step("4", "Gerçeğe dönüştür", "${sc.reveals.count { it.image != null }}/${sc.reveals.size} görsel") }
    item {
        ConsoleCard(Modifier.fillMaxWidth()) {
            Text(
                "1) “ChatGPT'ye gönder”: çizim paylaşılır, istem panoya kopyalanır; ChatGPT'de yapıştır. " +
                    "2) Çıkan görseli kaydet, “Görsel ekle” ile seç. " +
                    "3) İstersen görseli Gemini'ye ver, “Video istemi”ni yapıştır; videoyu “Video ekle” ile seç. " +
                    "Görseli olmayan dönüşüm videoya girmez.",
                style = Console.Small
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Videoların kendi sesi", style = Console.Subtitle)
                    Text("Gemini videosundaki doğal ses Karalak'ın sesinin altında çalar.", style = Console.Small)
                }
                Switch(checked = r.clipSound, onCheckedChange = vm::setClipSound, colors = SwitchDefaults.colors(checkedTrackColor = Console.Accent))
            }
            if (sc.reveals.isEmpty()) {
                Text("Senaryodaki bir çizimin yanındaki “☆ Gerçeğe”ye dokun.", style = Console.Small.copy(color = Console.Critical), modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
    sc.reveals.forEachIndexed { i, rv ->
        item(key = "rv-" + rv.sampleId) { RevealCard(i, rv, p, r.busy == rv.sampleId, r.busy != null, vm, context) }
    }

    item { Step("5", "Kapanış") }
    item {
        ConsoleCard(Modifier.fillMaxWidth()) {
            LineEditor(sc.outro, { vm.editLine(LineRef.RealOutro, it) }, { vm.editEmotion(LineRef.RealOutro, it) }) { vm.listen(sc.outro.text) }
        }
    }
}

/** One drawing to turn real: hand it to ChatGPT, bring the picture (and Gemini's clip) back, and Karalak's reaction. */
@Composable
private fun RevealCard(index: Int, rv: RealReveal, project: RealProject, busy: Boolean, anyBusy: Boolean, vm: StudioViewModel, context: Context) {
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let { vm.importImage(rv.sampleId, it) } }
    val clipPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let { vm.importClip(rv.sampleId, it) } }
    ConsoleCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(84.dp).clip(RoundedCornerShape(10.dp)).background(Color.White)) {
                StrokeCanvas(strokes = project.drawing(rv.sampleId)?.strokes.orEmpty(), modifier = Modifier.fillMaxSize().padding(4.dp), strokeWidthPx = 3f)
            }
            Spacer(Modifier.width(8.dp))
            Text("→", style = Console.Subtitle)
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(84.dp).clip(RoundedCornerShape(10.dp)).background(Console.Grid), contentAlignment = Alignment.Center) {
                val image = rv.image
                if (image != null) AsyncImage(model = vm.mediaFile(image), contentDescription = "Gerçek hâli", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else Text("görsel\nyok", style = Console.Small, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Dönüşüm ${index + 1}", style = Console.Subtitle)
                Text(
                    listOfNotNull(
                        if (rv.image != null) "görsel ✓" else "görsel bekleniyor",
                        rv.clip?.let { "video ${(rv.clipMs + 999) / 1000} sn ✓" }
                    ).joinToString(" · "),
                    style = Console.Small
                )
            }
            if (busy) CircularProgressIndicator(Modifier.size(18.dp), color = Console.Accent, strokeWidth = 2.dp)
        }
        Field("Çizimin şekli (İngilizce, isteme eklenir)", rv.describe, { vm.setRevealDescribe(rv.sampleId, it) }, lines = 2)

        Text("Görsel · ChatGPT", style = Console.Label, modifier = Modifier.padding(top = 10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
            ActionButton(Icons.Filled.Share, "ChatGPT'ye gönder", Console.Accent, Color.White, Modifier.weight(1.4f)) { vm.shareDoodle(rv.sampleId) }
            ActionButton(Icons.Filled.ContentCopy, "İstem", Console.Surface, Console.Ink, Modifier.weight(1f)) { vm.imagePrompt(rv.sampleId)?.let { copy(context, it) } }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            ActionButton(Icons.Filled.Download, if (rv.image == null) "Görsel ekle" else "Görseli değiştir", Console.Surface, Console.Ink, Modifier.weight(1.4f)) {
                if (!anyBusy) imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
            if (rv.image != null) ActionButton(Icons.Filled.Refresh, "Kaldır", Console.Surface, Console.Critical, Modifier.weight(1f)) { vm.removeImage(rv.sampleId) }
        }

        Text("Video · Gemini (isteğe bağlı)", style = Console.Label, modifier = Modifier.padding(top = 10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
            ActionButton(Icons.Filled.ContentCopy, "Video istemi", Console.Surface, Console.Ink, Modifier.weight(1f)) { copy(context, vm.videoPrompt()) }
            ActionButton(Icons.Filled.Movie, if (rv.clip == null) "Video ekle" else "Değiştir", Console.Surface, Console.Ink, Modifier.weight(1f)) {
                if (!anyBusy) clipPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
            }
        }
        if (rv.clip != null) SmallLink("Videoyu kaldır", Console.Critical) { vm.removeClip(rv.sampleId) }

        Text("Karalak'ın tepkisi", style = Console.Label, modifier = Modifier.padding(top = 10.dp))
        LineEditor(rv.line, { vm.editLine(LineRef.RealRevealLine(index), it) }, { vm.editEmotion(LineRef.RealRevealLine(index), it) }) { vm.listen(rv.line.text) }
        if (rv.image != null) {
            SmallLink(if (busy) "Karalak bakıyor…" else "Karalak gerçeğine baksın: tepkiyi görsele göre yazsın") { if (!anyBusy) vm.reactReal(rv.sampleId) }
        }
    }
}

@Composable
private fun WordRow(word: String, detail: String, on: Boolean, upper: Boolean = true, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (on) Console.AccentSoft else Console.Surface,
        border = BorderStroke(if (on) 1.5.dp else 1.dp, if (on) Console.Accent else Console.Border),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(if (upper) word.uppercase(Locale("tr")) else word, style = Console.Subtitle)
            Text(detail, style = Console.Small)
        }
    }
}

@Composable
private fun RealThumb(strokes: List<List<DrawingPoint>>, order: Int, star: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val on = order >= 0
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Console.Surface,
        border = BorderStroke(if (on) 2.dp else 1.dp, if (on) Console.Accent else Console.Border),
        modifier = modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
            StrokeCanvas(strokes = strokes, modifier = Modifier.fillMaxSize().padding(6.dp), strokeWidthPx = 4f)
            if (on) {
                Box(
                    Modifier.padding(5.dp).size(22.dp).clip(CircleShape).background(Console.Accent).align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center
                ) { Text("${order + 1}", style = Console.Label.copy(color = Color.White)) }
            }
            if (star) Text("★", style = Console.Subtitle.copy(color = Console.Accent), modifier = Modifier.padding(6.dp).align(Alignment.TopStart))
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
    // 4. voice
    item { Step("♪", "Ses") }
    item {
        ConsoleCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Karalak konuşsun", style = Console.Subtitle)
                    Text("Telefonun Türkçe sesi, ücretsiz. Kapalıyken yalnızca altyazı.", style = Console.Small)
                }
                Switch(checked = st.voiceOn, onCheckedChange = vm::setVoiceOn, colors = SwitchDefaults.colors(checkedTrackColor = Console.Accent))
            }
            if (st.voiceOn) {
                LabeledSlider("Ses inceliği", st.voicePitch, 0.8f..1.7f) { vm.setVoice(it, st.voiceRate) }
                LabeledSlider("Konuşma hızı", st.voiceRate, 0.8f..1.4f) { vm.setVoice(st.voicePitch, it) }
                SmallLink("Sesi dinle") { vm.listen(st.script?.intro?.text ?: "Merhaba, ben Karalak! Çizimlerini tahmin etmeye geldim.") }
            }
            st.voiceError?.let { Text(it, style = Console.Small.copy(color = Console.Critical)) }
        }
    }

    // 5. mascot artwork
    item { Step("☺", "Karalak'ın pozları") }
    item {
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

    // 6. preview
    item { Step("▶", "Önizleme", if (st.totalMs > 0) "≈ ${(st.totalMs + 999) / 1000} sn" else null) }
    item {
        ConsoleCard(Modifier.fillMaxWidth()) {
            if (st.preview == null && st.totalMs == 0L) {
                Text(if (st.format == StudioFormat.Guess) "Önizleme için en az bir çizim seç." else "Önizleme için bir kelime seç.", style = Console.Small)
            } else {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val img = st.preview
                    if (img != null) Image(img, contentDescription = "Önizleme", contentScale = ContentScale.Fit, modifier = Modifier.width(220.dp).aspectRatio(9f / 16f).clip(RoundedCornerShape(12.dp)))
                    else Box(Modifier.width(220.dp).aspectRatio(9f / 16f).background(Console.Grid, RoundedCornerShape(12.dp)))
                }
                if (st.totalMs > 0) {
                    Slider(
                        value = st.previewMs.toFloat().coerceIn(0f, st.totalMs.toFloat()),
                        onValueChange = { vm.setPreviewTime(it.toLong()) },
                        valueRange = 0f..st.totalMs.toFloat(),
                        colors = SliderDefaults.colors(thumbColor = Console.Accent, activeTrackColor = Console.Accent),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        if (st.format == StudioFormat.Guess && st.script == null) "Örnek cümlelerle gösteriliyor; Karalak yazınca kendi cümleleri gelir."
                        else "Sesli videoda süreler sesin gerçek uzunluğuna göre ayarlanır.",
                        style = Console.Small, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    // 7. make it
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
            BigButton("Videoyu oluştur", enabled = st.ready) { vm.render() }
            if (st.format == StudioFormat.Guess && st.script == null && st.selected.isNotEmpty()) {
                Text("Önce Karalak çizimlere baksın (3. adım).", style = Console.Small, modifier = Modifier.padding(top = 6.dp))
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

    // 8. result
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionButton(Icons.Filled.Share, "Paylaş", Console.Accent, Color.White, Modifier.weight(1.3f)) { share(context, file, st.caption, st.hashtags) }
                    ActionButton(Icons.Filled.Download, "Galeriye", Console.Surface, Console.Ink, Modifier.weight(1f)) { vm.saveToGallery() }
                    ActionButton(Icons.Filled.ContentCopy, "Metin", Console.Surface, Console.Ink, Modifier.weight(1f)) { copy(context, postText(st.caption, st.hashtags)) }
                }
                Field("Açıklama", st.caption, vm::setCaption, lines = 3)
                Field("Etiketler", st.hashtags, vm::setHashtags, lines = 2)
                Text(
                    "Paylaşırken açıklama panoya kopyalanır; TikTok/Instagram'da yapıştır. Arka plan müziğini orada, sesi kısık ekle.",
                    style = Console.Small, modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

/** Every line Karalak will say, editable, with its face and a button to hear it. */
private fun LazyListScope.scriptEditor(sc: GuessScript, st: StudioState, vm: StudioViewModel) {
    item {
        ConsoleCard(Modifier.fillMaxWidth()) {
            Text("Açılış", style = Console.Label)
            LineEditor(sc.intro, { vm.editLine(LineRef.Intro, it) }, { vm.editEmotion(LineRef.Intro, it) }) { vm.listen(sc.intro.text) }
        }
    }
    sc.rounds.forEachIndexed { r, round ->
        item(key = "round-$r") {
            ConsoleCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Çizim ${r + 1} · ${round.word.uppercase(Locale("tr"))}", style = Console.Subtitle)
                        Text("Karalak'ın son tahmini: “${round.finalGuess}”" + (round.playerAnswer?.let { " · çizenin cevabı: “$it”" } ?: ""), style = Console.Small)
                    }
                    Tag(
                        if (round.correct) "Bildi ✓" else "Bilemedi ✗",
                        fg = Color.White,
                        bg = if (round.correct) Console.Good else Console.Critical,
                        modifier = Modifier.clip(RoundedCornerShape(50)).clickable { vm.toggleVerdict(r) }
                    )
                }
                listOf("Üçte biri", "Üçte ikisi", "Bitince").forEachIndexed { k, label ->
                    round.stages.getOrNull(k)?.let { l ->
                        Text(label, style = Console.Label, modifier = Modifier.padding(top = 8.dp))
                        LineEditor(l, { vm.editLine(LineRef.Stage(r, k), it) }, { vm.editEmotion(LineRef.Stage(r, k), it) }) { vm.listen(l.text) }
                    }
                }
                Text("Sonuca tepkisi", style = Console.Label, modifier = Modifier.padding(top = 8.dp))
                LineEditor(round.reaction, { vm.editLine(LineRef.Reaction(r), it) }, { vm.editEmotion(LineRef.Reaction(r), it) }) { vm.listen(round.reaction.text) }
            }
        }
    }
    item {
        ConsoleCard(Modifier.fillMaxWidth()) {
            Text("Kapanış", style = Console.Label)
            LineEditor(sc.outro, { vm.editLine(LineRef.Outro, it) }, { vm.editEmotion(LineRef.Outro, it) }) { vm.listen(sc.outro.text) }
            Spacer(Modifier.height(8.dp))
            SmallLink(if (st.writing) "Yazılıyor…" else "Kararı değiştirdiysen: tepkileri ve kapanışı yeniden yazdır") { if (!st.writing) vm.rewriteFrame() }
        }
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

@Composable
private fun SampleThumb(sample: com.sualtikasifi.cizimhafiza.presentation.reports.studio.memory.MemorySample, order: Int, modifier: Modifier, onClick: () -> Unit) {
    val on = order >= 0
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Console.Surface,
        border = BorderStroke(if (on) 2.dp else 1.dp, if (on) Console.Accent else Console.Border),
        modifier = modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
            StrokeCanvas(strokes = sample.strokes, modifier = Modifier.fillMaxSize().padding(6.dp), strokeWidthPx = 4f)
            Box(
                Modifier.padding(5.dp).size(18.dp).clip(CircleShape).background(if (sample.correct) Console.Good else Console.Critical).align(Alignment.BottomStart),
                contentAlignment = Alignment.Center
            ) { Text(if (sample.correct) "✓" else "✗", style = Console.Label.copy(color = Color.White)) }
            if (on) {
                Box(
                    Modifier.padding(5.dp).size(22.dp).clip(CircleShape).background(Console.Accent).align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center
                ) { Text("${order + 1}", style = Console.Label.copy(color = Color.White)) }
            }
        }
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
private fun DarkButton(text: String, busy: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (enabled && !busy) Console.Dark else Console.InkMuted,
        modifier = Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(12.dp)).clickable(enabled = enabled && !busy, onClick = onClick)
    ) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            if (busy) CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
            else Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, style = Console.Subtitle.copy(color = Color.White), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun BigButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (enabled) Console.Accent else Console.InkMuted,
        modifier = Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(14.dp)).clickable(enabled = enabled, onClick = onClick)
    ) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Movie, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, style = Console.Subtitle.copy(color = Color.White))
        }
    }
}

@Composable
private fun Field(label: String?, value: String, onChange: (String) -> Unit, lines: Int = 1) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        label = label?.let { l -> @Composable { Text(l) } },
        singleLine = lines == 1, maxLines = lines.coerceAtLeast(1),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Console.Accent, unfocusedBorderColor = Console.Border,
            focusedContainerColor = Console.Surface, unfocusedContainerColor = Console.Surface,
            focusedTextColor = Console.Ink, unfocusedTextColor = Console.Ink, cursorColor = Console.Accent,
            focusedLabelColor = Console.Accent, unfocusedLabelColor = Console.InkSecondary
        ),
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
    )
}

@Composable
private fun ActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, bg: Color, fg: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp), color = bg,
        border = if (bg == Console.Surface) BorderStroke(1.dp, Console.Border) else null,
        modifier = modifier.height(46.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick)
    ) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(text, style = Console.Subtitle.copy(color = fg), maxLines = 1)
        }
    }
}

private fun postText(caption: String, hashtags: String) = listOf(caption.trim(), hashtags.trim()).filter { it.isNotEmpty() }.joinToString("\n\n")

private fun copy(context: Context, text: String) {
    runCatching {
        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Karalak", text))
        Toast.makeText(context, "Metin kopyalandı", Toast.LENGTH_SHORT).show()
    }
}

/** Hands a drawing to ChatGPT (or any app) as a picture, with the prompt on the clipboard to paste beside it. */
private fun shareImage(context: Context, file: File, prompt: String) {
    runCatching {
        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Karalak", prompt))
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, prompt)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "ChatGPT'ye gönder").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        Toast.makeText(context, "İstem panoya kopyalandı; ChatGPT'de yapıştır", Toast.LENGTH_LONG).show()
    }.onFailure { Toast.makeText(context, "Paylaşılamadı: ${it.message}", Toast.LENGTH_LONG).show() }
}

/** Hands the video to the system share sheet (Instagram, TikTok, WhatsApp…), with the post text on the clipboard. */
private fun share(context: Context, file: File, caption: String, hashtags: String) {
    runCatching {
        val text = postText(caption, hashtags)
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
