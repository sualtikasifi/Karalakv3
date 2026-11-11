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
import androidx.compose.material.icons.filled.CheckCircle
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
 * The video studio screen. A video is made screen by screen (see [StepFlow]): each screen holds one job, with "Geri" and "İleri"
 * fixed at the bottom, so nothing has to be scrolled to. Beside the steps are the library, the log of made videos and the
 * settings (the model, Karalak's voice, the poses), which are set once and left.
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

    Column(Modifier.fillMaxSize()) {
        // the format and the page on top; what each is made of is below, one screen at a time
        Segmented(
            options = StudioFormat.entries.map { it.name to it.title },
            selected = st.format.name,
            onSelect = { viewModel.setFormat(StudioFormat.valueOf(it)) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        val pages = buildList {
            add(StudioPage.Make.name to "Video")
            if (st.format == StudioFormat.RealLife) add(StudioPage.Library.name to "Kütüphane (${st.real.library.size})")
            add(StudioPage.Log.name to "Geçmiş")
            add(StudioPage.Settings.name to "Ayarlar")
        }
        // the library belongs to "Gerçek Olsaydı": on the other format its page falls back to the video
        val page = if (st.page == StudioPage.Library && st.format != StudioFormat.RealLife) StudioPage.Make else st.page
        Segmented(
            options = pages,
            selected = page.name,
            onSelect = { viewModel.setPage(StudioPage.valueOf(it)) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        when (page) {
            StudioPage.Make -> when (st.format) {
                StudioFormat.Forgot -> StepFlow(forgotFlow(st, viewModel, context), st.forgot.step - 1, { viewModel.setForgotStep(it + 1) })
                StudioFormat.RealLife -> StepFlow(realFlow(st, viewModel, context), st.real.step - 1, { viewModel.setRealStep(it + 1) })
            }
            StudioPage.Library -> PageList { libraryTab(st, viewModel) }
            StudioPage.Log -> PageList { trackingItems(st, viewModel) }
            StudioPage.Settings -> PageList {
                languageItems(st, viewModel)
                settingsTab(st, viewModel)
                mascotItems(st, viewModel)
            }
        }
    }
}

/** A page beside the steps (library, log, settings): an ordinary list that scrolls. */
@Composable
private fun PageList(content: LazyListScope.() -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 28.dp),
        content = content
    )
}

/** The language of the videos: what the assistant writes in and what the voice speaks. Chosen at the first step of each video. */
internal fun LazyListScope.languageItems(st: StudioState, vm: StudioViewModel) {
    item {
        Column {
            Text("Video dili · ${st.lang.named}", style = Console.Label)
            Text("Karalak'ın söylediği cümleler, ses ve gönderi metni bu dilde yazılır. Ekrandaki sabit yazılar şimdilik Türkçe.", style = Console.Small)
            Spacer(Modifier.height(6.dp))
            Segmented(
                options = StudioLang.entries.map { it.name to it.label },
                selected = st.lang.name,
                onSelect = { vm.setLang(StudioLang.valueOf(it)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * "Ödüllü Karalak Düellosu" screen by screen: 1 drawing · 2 how the code is given · 3 reward and coupon · 4 hook ·
 * 5 guesses · 6 closing · 7 voice and music · 8 preview · 9 make and share.
 */
internal fun forgotFlow(st: StudioState, vm: StudioViewModel, context: Context): List<FlowStep> {
    val f = st.forgot
    val script = f.script
    val chosen = script != null
    val steps = listOf<FlowStep>(
        FlowStep("Çizim", "Çizimi seç", chosen, if (chosen) null else "Devam etmek için bir çizim seç.", blocksNext = !chosen) {
            languageItems(st, vm)
            forgotStepDrawing(st, vm)
        },
        FlowStep("Kod", "Kod nasıl verilsin?", script?.code?.isNotBlank() == true) { forgotStepCode(st, vm) },
        FlowStep("Ödül", "Ödül ve kupon", f.couponStatus?.startsWith("Kupon hazır") == true) { forgotStepReward(st, vm) },
        FlowStep("Kanca", "Karalak yazsın: açılış cümlesi", script?.hook?.text?.isNotBlank() == true) { forgotStepHook(st, vm) },
        FlowStep("Tahminler", "Tahminler", script != null && script.guesses.isNotEmpty()) { forgotStepGuesses(st, vm) },
        FlowStep("Kapanış", "Soru, tepki ve kapanış", script?.outro?.text?.isNotBlank() == true) { forgotStepClosing(st, vm) },
        FlowStep("Ses", "Ses ve müzik", st.voiceOn && f.music != null) { forgotStepSound(st, vm) },
        FlowStep("Önizleme", "Önizle", false) { item { PreviewCard(st, vm) } },
        FlowStep("Üret", "Videoyu oluştur ve paylaş", st.video != null) {
            renderItems(st, vm)
            resultItems(st, vm, context)
        }
    )
    check(steps.size == FORGOT_STEPS) { "the Düello has $FORGOT_STEPS screens" }
    return steps
}

/** Whether Karalak speaks, with which engine, and what is still to be voiced (the Düello's voice screen). */
internal fun LazyListScope.voiceItems(st: StudioState, vm: StudioViewModel) {
    item {
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
                if (st.voiceEngine == VoiceEngine.ElevenLabs) {
                    // what is still to be voiced stays here; the key, the model and the voice are in the settings
                    ElevenVoicing(st, vm)
                    SmallLink("Ses ayarları (anahtar, model, ses)") { vm.setPage(StudioPage.Settings) }
                } else {
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
}

/** Karalak's poses for the Düello: how many are loaded, and a button to read them again. */
internal fun LazyListScope.mascotItems(st: StudioState, vm: StudioViewModel) {
    item { Step("☺", "Karalak'ın pozları (Düello)") }
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
                if (!real && st.forgot.draftId != null) {
                    ActionButton(
                        Icons.Filled.CheckCircle, if (st.forgot.done) "Tamamlandı · kaldır" else "Tamamlandı olarak işaretle",
                        if (st.forgot.done) Console.GoodSoft else Console.Surface, if (st.forgot.done) Console.GoodText else Console.Ink,
                        Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) { vm.toggleDoneForOpenDraft() }
                    Text(
                        if (st.forgot.done) "Bu videonun cümleleri örnek olarak kullanılıyor." else "Tamamlananların cümleleri, yapay zeka yeni cümle yazarken örnek olarak gösterilir.",
                        style = Console.Small, modifier = Modifier.padding(bottom = 4.dp)
                    )
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
