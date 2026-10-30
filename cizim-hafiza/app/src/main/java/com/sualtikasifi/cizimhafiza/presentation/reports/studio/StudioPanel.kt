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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.res.painterResource
import com.sualtikasifi.cizimhafiza.domain.model.AvatarFrame
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealDrawing
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
        item {
            Column {
                Text("Video dili", style = Console.Label)
                Text("Karalak'ın söylediği cümleler, ses ve gönderi metni bu dilde yazılır. Ekrandaki sabit yazılar şimdilik Türkçe.", style = Console.Small)
                Spacer(Modifier.height(6.dp))
                Segmented(
                    options = StudioLang.entries.map { it.name to "${it.label} · ${it.named}" },
                    selected = st.lang.name,
                    onSelect = { viewModel.setLang(StudioLang.valueOf(it)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
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
                LineEditor(script.hook, { vm.editLine(LineRef.MemoryHook, it) }, { vm.editEmotion(LineRef.MemoryHook, it) }) { vm.listen(script.hook) }
                script.picks.forEachIndexed { i, p ->
                    Text("Çizim ${i + 1} yorumu (boş bırakılabilir)", style = Console.Label, modifier = Modifier.padding(top = 8.dp))
                    val l = p.comment ?: SpokenLine("", MascotEmotion.Laughing)
                    LineEditor(l, { vm.editLine(LineRef.MemoryPickLine(i), it) }, { vm.editEmotion(LineRef.MemoryPickLine(i), it) }) { vm.listen(l) }
                }
                Text("Ters köşe", style = Console.Label, modifier = Modifier.padding(top = 8.dp))
                LineEditor(script.paradox, { vm.editLine(LineRef.MemoryParadox, it) }, { vm.editEmotion(LineRef.MemoryParadox, it) }) { vm.listen(script.paradox) }
                Text("Kapanış", style = Console.Label, modifier = Modifier.padding(top = 8.dp))
                LineEditor(script.outro, { vm.editLine(LineRef.MemoryOutro, it) }, { vm.editEmotion(LineRef.MemoryOutro, it) }) { vm.listen(script.outro) }
            }
        }
    }
}

/** "Gerçek Olsaydı": the latest drawings of players who allowed it, three of them, each turned real with ChatGPT and Gemini. */
private fun LazyListScope.realSections(st: StudioState, vm: StudioViewModel, context: Context) {
    val r = st.real
    val p = r.project
    val chosenIds = p?.reveals?.map { it.sampleId }.orEmpty()

    // the library comes first: finished pieces kept on this phone, to watch or to make into a new video
    item { Step("★", "Kütüphane", "${r.library.size} kayıt · ${megabytes(r.libraryBytes)}") }
    item {
        ConsoleCard(Modifier.fillMaxWidth()) {
            Text(
                "Ürettiğin görseller ve videolar telefonda otomatik saklanır (uygulamanın boyutunu büyütmez). " +
                    "Kancası, kapanışı ve sesleriyle birlikte; yani her kayıt hazır bir video (stok). Birini seçip yeniden video yapabilirsin.",
                style = Console.Small
            )
            Spacer(Modifier.height(8.dp))
            DarkButton(
                text = if (r.libraryShown) "Kütüphaneyi gizle" else "Kütüphaneyi aç (${r.library.size})",
                busy = false,
                enabled = true
            ) { vm.toggleLibrary() }
        }
    }
    if (r.libraryShown) {
        if (r.library.isEmpty()) item { Text("Henüz kayıt yok. Görsel ya da video eklediğin her çizim buraya düşer.", style = Console.Small) }
        r.library.chunked(3).forEach { row ->
            item(key = "lib-" + row.first().id) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { it ->
                        LibraryThumb(it, r.librarySelected.indexOf(it.id), vm, Modifier.weight(1f))
                    }
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
    }
    r.libraryView?.let { id -> r.library.find { it.id == id }?.let { item { LibraryViewer(it, vm) } } }

    item { Step("1", "Çizimleri seç", "${chosenIds.size}/5 seçili") }
    item {
        ConsoleCard(Modifier.fillMaxWidth()) {
            Text(
                "Havuzdaki en son 100 çizim. “Videolarda öne çık” izni veren oyuncunun adı ve seviyesi videoda görünür; " +
                    "izin vermeyen anonim kalır (yalnızca çerçevesi görünür, resmi hep varsayılan). Her video tek çizimle yapılır: birkaç çizim seçip " +
                    "hepsini hazırlayabilirsin (stok); video, “Başa al” ile en üste koyduğun çizimle yapılır.",
                style = Console.Small
            )
            Spacer(Modifier.height(8.dp))
            DarkButton(
                text = if (r.loadingRecent) "Okunuyor…" else if (r.recent.isEmpty()) "Son çizimleri yükle" else "Yenile",
                busy = r.loadingRecent,
                enabled = !r.loadingRecent
            ) { vm.refreshRecent() }
            r.recentError?.let { Text(it, style = Console.Small.copy(color = Console.Critical), modifier = Modifier.padding(top = 4.dp)) }
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

    if (p == null || p.reveals.isEmpty()) return
    item { Step("2", "Gerçeğe dönüştür", "${p.reveals.count { it.image != null }}/${p.reveals.size} görsel · ${p.reveals.count { it.clip != null }}/${p.reveals.size} video") }
    item {
        ConsoleCard(Modifier.fillMaxWidth()) {
            Text(
                "1) “ChatGPT'ye gönder”: çizim paylaşılır, istem panoya kopyalanır; ChatGPT'de yapıştır. " +
                    "2) Çıkan görseli kaydet, “Görsel ekle” ile seç. " +
                    "3) “Gemini'ye gönder”: görsel ve video istemi birlikte gider; Gemini'de Video'yu seçip gönder (sesli üretir). " +
                    "4) Çıkan videoyu “Video ekle” ile seç. " +
                    "Videoya en üstteki videolu çizimin Gemini videosu girer; kancası ve kapanışı aşağıda. " +
                    "Görseller videoya girmez: kaydırmalı gönderi olarak ayrı paylaşılır.",
                style = Console.Small
            )
            ActionButton(Icons.Filled.Share, "Görselleri paylaş (kaydırmalı) · ${p.reveals.count { it.image != null }}", Console.Surface, Console.Ink, Modifier.fillMaxWidth().padding(top = 8.dp)) {
                vm.shareCarousel()
            }
            Spacer(Modifier.height(8.dp))
            DarkButton(
                text = if (r.writing) "Asistan yazıyor…" else "Asistan kelimeleri, şekilleri ve video sahnesini yazsın",
                busy = r.writing,
                enabled = !r.writing
            ) { vm.describeReal() }
            Text("İsteğe bağlı: aynı alanları aşağıdan elle de doldurabilirsin. Açıklama ve etiketleri de yazar.", style = Console.Small, modifier = Modifier.padding(top = 4.dp))
            st.aiCost?.let { Text("Son yazımın maliyeti: " + OpenRouterClient.usd(it), style = Console.Small) }
            r.writeError?.let { Text(it, style = Console.Small.copy(color = Console.Critical)) }
        }
    }
    p.reveals.forEachIndexed { i, rv ->
        item(key = "rv-" + rv.sampleId) { RevealCard(i, rv, p, r.busy == rv.sampleId, r.busy != null, vm, context) }
    }

    val opening = p.opening
    item {
        val voiced = opening?.hook?.voice != null
        Step("3", "Kanca: videonun ilk saniyeleri", opening?.let { "≈ ${(it.hookMs + 999) / 1000} sn · " + if (voiced) "sesli" else "sessiz" })
    }
    item {
        LineCard(RealLine.Hook, p, st, vm) {
            Text(
                "Video, çizimin oyun ekranında baştan çizilmesiyle başlar: kelime üstte, süre köşede akar, altta oyunun jokerleri durur. " +
                    "Kanca ekrana yazılmaz, yalnızca Karalak söyler: seslendirip videoya ekle. Sonra video çizimin içinden açılır.",
                style = Console.Small
            )
        }
    }
    item {
        val l = opening?.outro ?: VoicedLine()
        Step("4", "Kapanış: Karalak'ın tepkisi", if (l.text.isBlank()) "yok" else "≈ ${(opening!!.outroMs + 999) / 1000} sn · " + if (l.voice != null) "sesli" else "sessiz")
    }
    item {
        LineCard(RealLine.Outro, p, st, vm) {
            Text(
                "Klibin son saniyelerinde, video oynamaya devam ederken Karalak klipte olana kısa bir tepki verip izleyiciyi yoruma çağırır; " +
                    "alt yazı olarak da görünür, klibin sesi o sırada kısılır. " +
                    "Asistan kliple ilgili sahne yazısını (yukarıdaki “Videodaki sahne”) ve kancayı da okur.",
                style = Console.Small
            )
        }
    }

    item { Step("5", "Arka plan müziği", opening?.music?.let { RealMusic.label(it) } ?: "yok") }
    item { MusicCard(opening, st.real.musicPlaying, vm) }

    item { Step("6", "Kartın yeri ve boyu") }
    item {
        ConsoleCard(Modifier.fillMaxWidth()) {
            Text("Karalak kartının videodaki yerini ve boyunu ayarla; aşağıdaki önizleme anında değişir.", style = Console.Small)
            Text("Kaydırıcıyla kabaca, − ve + ile hassas ayarla (0,01'lik adım; basılı tutunca hızlanır).", style = Console.Small)
            PreciseSlider("Boyut", r.card.scale, CardLayout.MIN_SCALE..CardLayout.MAX_SCALE) { vm.setCard(r.card.copy(scale = it)) }
            PreciseSlider("Sağa–sola", r.card.x, 0f..1f) { vm.setCard(r.card.copy(x = it)) }
            PreciseSlider("Yukarı–aşağı", r.card.y, 0f..1f) { vm.setCard(r.card.copy(y = it)) }
            SmallLink("Varsayılana dön") { vm.resetCard() }
        }
    }

    item {
        Step("7", "Tanıtım videosu: her videonun sonu", if (r.promo == null) "yok" else if (!r.promoOn) "kapalı" else String.format(Locale("tr"), "%.1f sn", r.promoMs / 1000f))
    }
    item { PromoCard(r, vm) }
}

/**
 * The app's promo video, made once by hand and kept on this phone: it closes every video, with its own sound, after
 * the drawing's video ends. It can be swapped, removed or left out of a video.
 */
@Composable
private fun PromoCard(r: RealUi, vm: StudioViewModel) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let { vm.importPromo(it) } }
    ConsoleCard(Modifier.fillMaxWidth()) {
        Text(
            "Uygulamayı tanıtan kısa videon her videonun sonuna, kendi sesiyle eklenir; bir kez seçmen yeterli, telefonda saklanır. " +
                "Dikey 9:16 en iyisi; başka oranda bir video kırpılmadan, arkası bulanık olarak yerleşir. En fazla ${RealTimeline.PROMO_MAX / 1000} saniyesi girer.",
            style = Console.Small
        )
        if (r.promo != null) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Videoların sonuna ekle", style = Console.Subtitle)
                    Text(String.format(Locale("tr"), "%.1f sn", r.promoMs / 1000f) + " · önizlemede en sonda görünür", style = Console.Small)
                }
                Switch(checked = r.promoOn, onCheckedChange = vm::setPromoOn, colors = SwitchDefaults.colors(checkedTrackColor = Console.Accent))
            }
        }
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
private fun MusicCard(reveal: RealReveal?, playing: String?, vm: StudioViewModel) {
    ConsoleCard(Modifier.fillMaxWidth()) {
        Text(
            "Uygulamanın müziklerinden biri videonun altında çalar. Karalak konuşurken kısılır, klip boyunca biraz alçalır, sonda yavaşça biter. " +
                "▶ ile önce dinleyebilirsin.",
            style = Console.Small
        )
        val chosen = reveal?.music
        val options = listOf<String?>(null) + RealMusic.TRACKS.map { it.first }
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
    val count = if (hook) RealCopy.HOOK_COUNT else RealCopy.OUTRO_COUNT
    val name = if (hook) "kanca" else "kapanış"
    ConsoleCard(Modifier.fillMaxWidth()) {
        intro()
        drawing?.let { Text("Videodaki çizim: ${it.word}" + if (it.seconds > 0) " · ${it.seconds} sn" else "", style = Console.Subtitle, modifier = Modifier.padding(top = 6.dp)) }
        Spacer(Modifier.height(8.dp))
        DarkButton(
            text = if (r.lineWriting == kind) "Asistan yazıyor…" else if (line.options.isEmpty()) "Asistan $count $name yazsın" else "$count yeni $name yazsın",
            busy = r.lineWriting == kind,
            enabled = r.lineWriting == null && opening != null
        ) { vm.writeLines(kind) }
        r.lineErrors[kind]?.let { Text(it, style = Console.Small.copy(color = Console.Critical), modifier = Modifier.padding(top = 4.dp)) }
        if (line.text.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(if (hook) "Videodaki kanca" else "Videodaki kapanış", style = Console.Label)
                    Text(line.text, style = Console.Subtitle)
                    Text(
                        if (line.voice != null) "Sesli · " + String.format(Locale("tr"), "%.1f sn", line.voiceMs / 1000f)
                        else if (hook) "Sessiz: kanca yazılmadığı için videoda duyulmaz; seslendirip “Videoya ekle”ye bas."
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
        if (!st.eleven.keySet) Text("Seslendirmek için aşağıdaki “Karalak'ın sesi” bölümüne ElevenLabs anahtarını gir.", style = Console.Small, modifier = Modifier.padding(top = 8.dp))
        var own by remember(kind) { mutableStateOf("") }
        Field(if (hook) "Kendi kancanı yaz" else "Kendi kapanışını yaz", own, { own = it.take(RealCopy.HOOK_CHARS) })
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
                Field(null, draft, { draft = it.take(RealCopy.HOOK_CHARS) }, lines = 2)
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
                val d = project.drawing(rv.sampleId)
                Text("Çizim ${index + 1} · ${d?.word.orEmpty()}", style = Console.Subtitle)
                Text(if (d?.named == true) "@${d.nickname} · Sv. ${d.level}" else "Anonim oyuncu", style = Console.Small)
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
        if (project.featured?.sampleId == rv.sampleId) Text("Videodaki çizim bu", style = Console.Label.copy(color = Console.Accent), modifier = Modifier.padding(top = 6.dp))
        else if (index > 0) SmallLink("Başa al (video bu çizimle yapılır)") { vm.moveRevealFirst(rv.sampleId) }
        PlayerEditor(project.drawing(rv.sampleId), vm, anyBusy)
        Field("Çizimin şekli (görsel istemine eklenir)", rv.describe, { vm.setRevealDescribe(rv.sampleId, it) }, lines = 3)
        Field("Videodaki sahne (video istemine eklenir): kim, nerede, ne yapıyor, hangi sesler? Ne kadar ayrıntılı yazarsan o kadar iyi.", rv.motion, { vm.setRevealMotion(rv.sampleId, it) }, lines = 7)

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

        Text("Video · Gemini", style = Console.Label, modifier = Modifier.padding(top = 10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
            ActionButton(Icons.Filled.Share, "Gemini'ye gönder", Console.Accent, Color.White, Modifier.weight(1.4f)) { vm.shareToGemini(rv.sampleId) }
            ActionButton(Icons.Filled.ContentCopy, "İstem", Console.Surface, Console.Ink, Modifier.weight(1f)) { vm.videoPrompt(rv.sampleId)?.let { copy(context, it) } }
        }
        if (rv.image == null) Text("Gemini'ye göndermek için önce görseli ekle.", style = Console.Small, modifier = Modifier.padding(top = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            ActionButton(Icons.Filled.Movie, if (rv.clip == null) "Video ekle" else "Videoyu değiştir", Console.Surface, Console.Ink, Modifier.weight(1f)) {
                if (!anyBusy) clipPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
            }
        }
        if (rv.clip != null) SmallLink("Videoyu kaldır", Console.Critical) { vm.removeClip(rv.sampleId) }
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
    Text("Kartta görünen oyuncu", style = Console.Label, modifier = Modifier.padding(top = 10.dp))
    Field("Kullanıcı adı (boş: Anonim oyuncu)", d.nickname, { vm.setPlayerName(d.id, it) })
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
                    vm.listen(st.script?.intro ?: SpokenLine("Merhaba, ben Karalak! Çizimlerini tahmin etmeye geldim.", MascotEmotion.Excited))
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

    // 6. preview
    item { Step("▶", "Önizleme", if (st.totalMs > 0) "≈ ${(st.totalMs + 999) / 1000} sn" else null) }
    item {
        ConsoleCard(Modifier.fillMaxWidth()) {
            if (st.preview == null && st.totalMs == 0L) {
                Text(
                    when (st.format) {
                        StudioFormat.Guess -> "Önizleme için en az bir çizim seç."
                        StudioFormat.RealLife -> "Önizleme için çizim seç ve Gemini videosunu ekle."
                        else -> "Önizleme için bir kelime seç."
                    },
                    style = Console.Small
                )
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
                        else if (st.format == StudioFormat.RealLife) "Kanca, video (kendi sesiyle, üstünde Karalak kartı) ve kapanış. Hedef 8–20 sn."
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
            val voiceMissing = st.voiceOn && st.voiceEngine == VoiceEngine.ElevenLabs && st.eleven.missing > 0
            BigButton("Videoyu oluştur", enabled = st.ready && !voiceMissing) { vm.render() }
            if (st.ready && voiceMissing) {
                Text("Önce Ses bölümünde “Seslendir”e bas: ${st.eleven.missing} cümle, ~${st.eleven.missingCredits} kredi.", style = Console.Small, modifier = Modifier.padding(top = 6.dp))
            }
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
                    ActionButton(Icons.Filled.Share, "Paylaş", Console.Accent, Color.White, Modifier.weight(1.3f)) { share(context, file, st.caption, st.hashtags, store = real) }
                    ActionButton(Icons.Filled.Download, "Galeriye", Console.Surface, Console.Ink, Modifier.weight(1f)) { vm.saveToGallery() }
                    ActionButton(Icons.Filled.ContentCopy, "Metin", Console.Surface, Console.Ink, Modifier.weight(1f)) { copy(context, postText(st.caption, st.hashtags, store = real)) }
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

/** Every line Karalak will say, editable, with its face and a button to hear it. */
private fun LazyListScope.scriptEditor(sc: GuessScript, st: StudioState, vm: StudioViewModel) {
    item {
        ConsoleCard(Modifier.fillMaxWidth()) {
            Text("Açılış", style = Console.Label)
            LineEditor(sc.intro, { vm.editLine(LineRef.Intro, it) }, { vm.editEmotion(LineRef.Intro, it) }) { vm.listen(sc.intro) }
            Alternatives(st.introAlts) { vm.pickAlt(LineRef.Intro, it) }
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
                        LineEditor(l, { vm.editLine(LineRef.Stage(r, k), it) }, { vm.editEmotion(LineRef.Stage(r, k), it) }) { vm.listen(l) }
                    }
                }
                Text("Sonuca tepkisi", style = Console.Label, modifier = Modifier.padding(top = 8.dp))
                LineEditor(round.reaction, { vm.editLine(LineRef.Reaction(r), it) }, { vm.editEmotion(LineRef.Reaction(r), it) }) { vm.listen(round.reaction) }
            }
        }
    }
    item {
        ConsoleCard(Modifier.fillMaxWidth()) {
            Text("Kapanış", style = Console.Label)
            LineEditor(sc.outro, { vm.editLine(LineRef.Outro, it) }, { vm.editEmotion(LineRef.Outro, it) }) { vm.listen(sc.outro) }
            Alternatives(st.outroAlts) { vm.pickAlt(LineRef.Outro, it) }
            Spacer(Modifier.height(8.dp))
            SmallLink(if (st.writing) "Yazılıyor…" else "Kararı değiştirdiysen: tepkileri ve kapanışı yeniden yazdır") { if (!st.writing) vm.rewriteFrame() }
        }
    }
}

/** The assistant's other openings or closings: a tap swaps one in, and the line it replaces joins the list. */
@Composable
private fun Alternatives(alts: List<SpokenLine>, onPick: (SpokenLine) -> Unit) {
    if (alts.isEmpty()) return
    Text("Başka seçenekler", style = Console.Label, modifier = Modifier.padding(top = 8.dp))
    alts.forEach { a ->
        Text(
            "↻ ${a.text}",
            style = Console.Small.copy(color = Console.Accent),
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onPick(a) }.padding(vertical = 6.dp, horizontal = 4.dp)
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
private fun Field(label: String?, value: String, onChange: (String) -> Unit, lines: Int = 1, secret: Boolean = false) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        label = label?.let { l -> @Composable { Text(l) } },
        singleLine = lines == 1, maxLines = lines.coerceAtLeast(1),
        visualTransformation = if (secret) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
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

private fun postText(caption: String, hashtags: String, store: Boolean = false): String {
    val text = listOf(caption.trim(), hashtags.trim()).filter { it.isNotEmpty() }.joinToString("\n\n")
    // the viewer is sent to the app's Google Play page, not the website
    return if (store) RealCopy.withStoreLink(text) else text
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
private fun share(context: Context, file: File, caption: String, hashtags: String, store: Boolean = false) {
    runCatching {
        val text = postText(caption, hashtags, store)
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
