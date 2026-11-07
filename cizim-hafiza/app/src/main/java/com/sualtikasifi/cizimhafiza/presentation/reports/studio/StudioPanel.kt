package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import android.content.Context
import android.net.Uri
import android.widget.Toast
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.PostLink
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.SocialPlatform
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Console
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleCard
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Segmented
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.audio.ElevenLabs
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MascotEmotion

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

/** Voice, mascot artwork, preview, making the video and the result: the same for every format. */
internal fun LazyListScope.sharedSections(st: StudioState, vm: StudioViewModel, context: Context) {
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
    trackingItems(st, vm)
}

/** The preview's step and card, for the formats that show it as a section of their own. */
internal fun LazyListScope.previewItems(st: StudioState, vm: StudioViewModel) {
    item { Step("▶", "Önizleme", if (st.totalMs > 0) "≈ ${(st.totalMs + 999) / 1000} sn" else null) }
    item { PreviewCard(st, vm) }
}

/** The preview frame with its time slider. */
@Composable
internal fun PreviewCard(st: StudioState, vm: StudioViewModel) {
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
internal fun LazyListScope.renderItems(st: StudioState, vm: StudioViewModel) {
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
internal fun LazyListScope.resultItems(st: StudioState, vm: StudioViewModel, context: Context) {
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
