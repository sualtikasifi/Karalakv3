package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import android.net.Uri
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
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.res.painterResource
import com.sualtikasifi.cizimhafiza.domain.model.AvatarFrame
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.PostStats
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealDrawing
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealEnding
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealOpening
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealLine
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealMix
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealMusic
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.VoicedLine
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.AsyncImage
import com.sualtikasifi.cizimhafiza.domain.model.DrawingPoint
import com.sualtikasifi.cizimhafiza.presentation.common.StrokeCanvas
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Console
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleCard
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Segmented
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.audio.ElevenLabs
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.LibraryItem
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealCopy
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealProject
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealReveal
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealTimeline
import java.util.Locale

// The cards and small pieces "Gerçek Olsaydı" is built from: notes, ending, music, lines, the library and its viewer.

/** The drawing's notes the assistant wrote, to correct by hand: they build the picture and video prompts. */
@Composable
internal fun NotesEditor(rv: RealReveal, vm: StudioViewModel) {
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
internal fun EndingCard(r: RealUi, vm: StudioViewModel) {
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
internal fun EndVoices(r: RealUi, vm: StudioViewModel) {
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
internal fun PromoCard(r: RealUi, vm: StudioViewModel) {
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
internal fun MusicCard(reveal: RealReveal?, musicOn: Boolean, playing: String?, custom: List<String>, vm: StudioViewModel) {
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
internal fun LineCard(kind: RealLine, p: RealProject, st: StudioState, vm: StudioViewModel, intro: @Composable () -> Unit) {
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
internal fun LineOption(
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

internal fun megabytes(bytes: Long): String = String.format(Locale.US, "%.1f MB", bytes / 1_048_576.0)

/** One kept piece: its picture (or the drawing), a play mark when it has a clip, and a round tick that adds it to the next video. */
@Composable
internal fun LibraryThumb(item: LibraryItem, order: Int, vm: StudioViewModel, modifier: Modifier) {
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
internal fun PostsSection(item: LibraryItem, vm: StudioViewModel) {
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
internal fun NumberField(label: String, value: Int, max: Int = 999_999_999, onChange: (Int) -> Unit) {
    Field(label, if (value == 0) "" else value.toString(), { raw -> onChange((raw.filter { it.isDigit() }.take(9).toLongOrNull() ?: 0L).coerceAtMost(max.toLong()).toInt()) })
}

/**
 * What the postings say about the hooks: for each, the share still watching at 3 seconds and the installs it brought,
 * over the posts with enough views to count. This is the list to write the next hooks from.
 */
@Composable
internal fun HookResults(library: List<LibraryItem>) {
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
internal fun LibraryViewer(item: LibraryItem, vm: StudioViewModel) {
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
internal fun PlayerEditor(d: RealDrawing?, vm: StudioViewModel, anyBusy: Boolean) {
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
internal fun FrameChoice(selected: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Console.AccentSoft else Console.Surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Console.Accent else Console.Border),
        modifier = Modifier.size(58.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick)
    ) { Box(contentAlignment = Alignment.Center) { content() } }
}

@Composable
internal fun RealThumb(strokes: List<List<DrawingPoint>>, label: String, order: Int, modifier: Modifier, onClick: () -> Unit) {
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
internal fun MiniDoodle(strokes: List<List<DrawingPoint>>) {
    Box(Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Color.White)) {
        StrokeCanvas(strokes = strokes, modifier = Modifier.fillMaxSize().padding(3.dp), strokeWidthPx = 2f)
    }
}
