package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealMix
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealEnding
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealOpening
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealLine
import kotlinx.coroutines.launch
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Console
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleCard
import com.sualtikasifi.cizimhafiza.presentation.reports.console.OpenRouterClient
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Segmented
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.audio.ElevenLabs
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.CardLayout
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealProject
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealReveal
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealTimeline
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RecentDrawing
import java.util.Locale

// "Gerçek Olsaydı" screens of the studio panel: the steps of the wizard, the library and the settings tab.

/** "Gerçek Olsaydı": the latest drawings of players who allowed it, three of them, each turned real with ChatGPT and Gemini. */
/** The seven steps of "Gerçek Olsaydı", in the order the work goes (see [WizardStepInfo]). */
internal class WizardStepInfo(val title: String, val short: String)

internal val WIZARD_STEPS = listOf(
    WizardStepInfo("Çizimi seç", "Çizim"),
    WizardStepInfo("Görsel istemini hazırla", "İstem"),
    WizardStepInfo("Gerçeğe dönüştür ve oyuncuyu ayarla", "Görsel"),
    WizardStepInfo("Görseli yükle, senaryoyu seç, Gemini'ye gönder", "Senaryo"),
    WizardStepInfo("Seslendirmeler ve müzik", "Ses"),
    WizardStepInfo("Videoyu yükle, kartı yerleştir", "Video"),
    WizardStepInfo("Önizle ve oluştur", "Bitir")
)

internal val VOICE_STEPS = listOf("Kanca", "Anlatıcı", "Kapanış", "Müzik", "Bitiş sesi")

/** Whether step [n] (1..7) has what it is for: only for the marks on the stepper, the steps themselves can be visited in any order. */
internal fun stepDone(n: Int, st: StudioState): Boolean {
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

internal fun voiceDone(n: Int, f: RealReveal?): Boolean = when (n) {
    1 -> f?.hook?.voice != null
    2 -> f?.narration?.voice != null
    3 -> f?.outro?.voice != null
    4 -> f?.music != null
    else -> f?.endLine?.voice != null
}

internal fun LazyListScope.realSections(st: StudioState, vm: StudioViewModel, context: Context) {
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
internal fun LazyListScope.libraryTab(st: StudioState, vm: StudioViewModel) {
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
internal fun LazyListScope.settingsTab(st: StudioState, vm: StudioViewModel) {
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
internal fun LazyListScope.wizard(st: StudioState, vm: StudioViewModel, context: Context) {
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
internal fun LazyListScope.stepChoose(st: StudioState, vm: StudioViewModel) {
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
internal fun LazyListScope.stepPrompt(st: StudioState, vm: StudioViewModel, f: RealReveal, context: Context) {
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
internal fun LazyListScope.stepImage(st: StudioState, vm: StudioViewModel, f: RealReveal, p: RealProject, context: Context) {
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
internal fun LazyListScope.stepScenario(st: StudioState, vm: StudioViewModel, f: RealReveal, context: Context) {
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
internal fun LazyListScope.stepVoices(st: StudioState, vm: StudioViewModel, f: RealReveal, p: RealProject) {
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
            item {
                MusicCard(
                    f?.music, f?.musicVolume ?: RealMix.DEFAULT_VOLUME, f != null, r.musicOn, r.musicPlaying, r.customMusic, vm,
                    onPick = vm::setMusic, onVolume = vm::setMusicVolume, onAdd = { vm.addMusic(it) }
                )
            }
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
internal fun LazyListScope.stepVideo(st: StudioState, vm: StudioViewModel, f: RealReveal) {
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
internal fun LazyListScope.stepFinish(st: StudioState, vm: StudioViewModel, context: Context) {
    item { PreviewCard(st, vm) }
    renderItems(st, vm)
    resultItems(st, vm, context)
}
