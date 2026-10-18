package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.widget.VideoView
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.sualtikasifi.cizimhafiza.presentation.common.StrokeCanvas
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Console
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleCard
import com.sualtikasifi.cizimhafiza.presentation.reports.console.OpenRouterClient
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Tag
import java.io.File

/**
 * The video studio screen: pick a template, pick drawings, let the assistant write the words, look
 * at any moment of the video, make it, then share it or keep it in the gallery.
 */
@Composable
fun StudioPanel(onBack: () -> Unit, viewModel: StudioViewModel = hiltViewModel()) {
    val st by viewModel.state.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(st.message) {
        st.message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show(); viewModel.clearMessage() }
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
                    Text("Bu telefondaki kendi çizimlerinden dikey video · ücretsiz", style = Console.Small)
                }
            }
        }

        // 1. template
        item { Step("1", "Şablon") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StudioTemplate.entries.forEach { t ->
                    val on = st.template == t
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (on) Console.AccentSoft else Console.Surface,
                        border = BorderStroke(if (on) 1.5.dp else 1.dp, if (on) Console.Accent else Console.Border),
                        modifier = Modifier.weight(1f).height(96.dp).clip(RoundedCornerShape(14.dp)).clickable { viewModel.setTemplate(t) }
                    ) {
                        Column(Modifier.padding(10.dp)) {
                            Text(t.title, style = Console.Subtitle.copy(color = if (on) Console.Accent else Console.Ink))
                            Spacer(Modifier.height(2.dp))
                            Text(t.blurb, style = Console.Small, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        // 2. drawings
        item {
            Step(
                "2", "Çizimler",
                trailing = "${st.selected.size}/${st.template.maxCount} seçili" +
                    if (st.template.minCount > 1) " · en az ${st.template.minCount}" else ""
            )
        }
        when {
            st.loading -> item { Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Console.Accent) } }
            st.offered.isEmpty() -> item {
                ConsoleCard(Modifier.fillMaxWidth()) {
                    Text(
                        if (st.template == StudioTemplate.Versus) "Aynı kelimeyi iki kez çizdiğin bir tur bulunamadı. Biraz daha oyna ya da başka şablon seç."
                        else "Bu telefonda kayıtlı çizim yok. Birkaç tur oyna, sonra buraya dön.",
                        style = Console.Small
                    )
                }
            }
            else -> st.offered.take(90).chunked(3).forEach { row ->
                item(key = "row-" + row.first().id) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { d ->
                            val order = st.selected.indexOf(d.id)
                            Thumb(d, order, Modifier.weight(1f)) { viewModel.toggle(d) }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }

        // 3. words
        item { Step("3", "Yazılar") }
        item {
            ConsoleCard(Modifier.fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (st.aiRunning) Console.InkMuted else Console.Dark,
                    modifier = Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(12.dp)).clickable(enabled = !st.aiRunning) { viewModel.writeWithAi() }
                ) {
                    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        if (st.aiRunning) CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        else Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (st.aiRunning) "Yazıyor…" else "Yapay zekâ yazsın", style = Console.Subtitle.copy(color = Color.White))
                    }
                }
                st.aiCost?.let { Text("Son yazım: " + OpenRouterClient.usd(it), style = Console.Small, modifier = Modifier.padding(top = 4.dp)) }
                st.aiError?.let { Text(it, style = Console.Small.copy(color = Console.Critical), modifier = Modifier.padding(top = 4.dp)) }
                Spacer(Modifier.height(8.dp))
                Field("İlk ekran (kanca)", st.hook, viewModel::setHook, lines = 2)
                Field("Soru (çizim bitince)", st.ask, viewModel::setAsk)
                Field("Son ekran", st.outro, viewModel::setOutro)
                Text("Gönderi metni (videoya girmez, paylaşırken panoya kopyalanır)", style = Console.Label, modifier = Modifier.padding(top = 8.dp))
                Field("Açıklama", st.caption, viewModel::setCaption, lines = 3)
                Field("Etiketler", st.hashtags, viewModel::setHashtags, lines = 2)
            }
        }

        // 4. preview and make
        item { Step("4", "Önizleme", trailing = if (st.totalMs > 0) "Süre: ${(st.totalMs + 999) / 1000} sn" else null) }
        item {
            ConsoleCard(Modifier.fillMaxWidth()) {
                if (!st.ready) {
                    Text("Önizleme için ${st.template.minCount}–${st.template.maxCount} çizim seç.", style = Console.Small)
                } else {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        val img = st.preview
                        if (img != null) {
                            Image(img, contentDescription = "Önizleme", contentScale = ContentScale.Fit, modifier = Modifier.width(210.dp).aspectRatio(9f / 16f).clip(RoundedCornerShape(12.dp)))
                        } else {
                            Box(Modifier.width(210.dp).aspectRatio(9f / 16f).background(Console.Grid, RoundedCornerShape(12.dp)))
                        }
                    }
                    if (st.totalMs > 0) {
                        Slider(
                            value = st.previewMs.toFloat().coerceIn(0f, st.totalMs.toFloat()),
                            onValueChange = { viewModel.setPreviewTime(it.toLong()) },
                            valueRange = 0f..st.totalMs.toFloat(),
                            colors = SliderDefaults.colors(thumbColor = Console.Accent, activeTrackColor = Console.Accent),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Kaydırarak videonun her anına bak", style = Console.Small, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
        item {
            if (st.rendering) {
                ConsoleCard(Modifier.fillMaxWidth()) {
                    Text("Video oluşturuluyor… %${(st.progress * 100).toInt()}", style = Console.Subtitle)
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(progress = { st.progress }, color = Console.Accent, trackColor = Console.Grid, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Text("Vazgeç", style = Console.Label.copy(color = Console.Critical), modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { viewModel.cancelRender() }.padding(4.dp))
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (st.ready) Console.Accent else Console.InkMuted,
                    modifier = Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(14.dp)).clickable(enabled = st.ready) { viewModel.render() }
                ) {
                    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Movie, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Videoyu oluştur", style = Console.Subtitle.copy(color = Color.White))
                    }
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

        // 5. result
        st.video?.let { file ->
            item { Step("5", "Hazır") }
            item {
                ConsoleCard(Modifier.fillMaxWidth()) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        AndroidView(
                            factory = { ctx -> VideoView(ctx).apply { setOnPreparedListener { mp -> mp.isLooping = true; start() } } },
                            update = { v -> if (v.tag != file.path) { v.tag = file.path; v.setVideoURI(Uri.fromFile(file)) } },
                            modifier = Modifier.width(220.dp).aspectRatio(9f / 16f).clip(RoundedCornerShape(12.dp))
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionButton(Icons.Filled.Share, "Paylaş", Console.Accent, Color.White, Modifier.weight(1.3f)) {
                            share(context, file, st.caption, st.hashtags)
                        }
                        ActionButton(Icons.Filled.Download, "Galeriye", Console.Surface, Console.Ink, Modifier.weight(1f)) { viewModel.saveToGallery() }
                        ActionButton(Icons.Filled.ContentCopy, "Metin", Console.Surface, Console.Ink, Modifier.weight(1f)) {
                            copy(context, postText(st.caption, st.hashtags))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Paylaşırken açıklama ve etiketler panoya kopyalanır; TikTok/Instagram'da yapıştır. Müziği orada ekle: platformun trend sesleri videoyu daha çok kişiye gösterir.",
                        style = Console.Small
                    )
                }
            }
        }
    }
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
            Tag(d.word, modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 6.dp))
        }
    }
}

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit, lines: Int = 1) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        label = { Text(label) },
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
