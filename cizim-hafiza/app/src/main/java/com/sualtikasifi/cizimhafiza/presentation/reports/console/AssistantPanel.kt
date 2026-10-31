package com.sualtikasifi.cizimhafiza.presentation.reports.console

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The AI assistant tab: pick a job (event plan, social posts, growth plan, product ideas, store
 * listing, free question), tap once, get a ready-to-use answer to copy or share. Answers are kept
 * on the phone so a good plan is never lost.
 */
@Composable
fun AssistantPanel(pendingPlan: SpecialDay?, onPlanConsumed: () -> Unit, viewModel: AssistantViewModel = hiltViewModel()) {
    val st by viewModel.state.collectAsState()
    LaunchedEffect(pendingPlan) { pendingPlan?.let { viewModel.planFor(it); onPlanConsumed() } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Console.Gap),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (st.hasKey) "OpenRouter · ${st.model}" + st.usage?.let { " · bu ay ${OpenRouterClient.usd(it.monthly)}" }.orEmpty()
                    else "Kurulum gerekiyor: OpenRouter anahtarı",
                    style = Console.Label, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                )
                ConsoleIconButton(Icons.Filled.Settings, "Ayarlar", viewModel::toggleSettings, size = 32.dp)
            }
        }
        if (st.showSettings) item { SettingsCard(st, viewModel) }
        item {
            Text("Ne hazırlayayım?", style = Console.Subtitle, modifier = Modifier.padding(top = 4.dp))
        }
        AiTask.entries.chunked(2).forEach { pair ->
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { t ->
                        val on = st.task == t
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (on) Console.AccentSoft else Console.Surface,
                            border = BorderStroke(if (on) 1.5.dp else 1.dp, if (on) Console.Accent else Console.Border),
                            modifier = Modifier.weight(1f).height(86.dp).clip(RoundedCornerShape(14.dp)).clickable { viewModel.selectTask(t) }
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(t.title, style = Console.Subtitle.copy(color = if (on) Console.Accent else Console.Ink))
                                Spacer(Modifier.height(2.dp))
                                Text(t.blurb, style = Console.Small, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        item { TaskInputs(st, viewModel) }
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (st.running) Console.InkMuted else Console.Accent,
                modifier = Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(14.dp)).clickable(enabled = !st.running) { viewModel.run() }
            ) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    if (st.running) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Veriler okunuyor, hazırlanıyor…", style = Console.Subtitle.copy(color = Color.White))
                    } else {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Hazırla", style = Console.Subtitle.copy(color = Color.White))
                    }
                }
            }
        }
        st.error?.let { err ->
            item {
                Surface(shape = RoundedCornerShape(12.dp), color = Console.CriticalSoft, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Olmadı", style = Console.Subtitle.copy(color = Console.Critical))
                        Text(err, style = Console.Small)
                        Text("Anahtarı, OpenRouter bakiyeni ve model adını ayarlardan kontrol et.", style = Console.Small)
                    }
                }
            }
        }
        if (st.history.isNotEmpty()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Icon(Icons.Filled.History, contentDescription = null, tint = Console.InkSecondary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Kaydedilenler", style = Console.Subtitle, modifier = Modifier.weight(1f))
                    TextLink("Verileri tazele") { viewModel.refreshContext() }
                }
            }
            items(st.history, key = { it.at }) { saved ->
                ConsoleCard(Modifier.fillMaxWidth(), padding = 12.dp, onClick = { viewModel.openSaved(saved) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(saved.title, style = Console.Subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(SimpleDateFormat("d MMM HH:mm", Locale("tr")).format(Date(saved.at)) + saved.costUsd?.let { " · " + OpenRouterClient.usd(it) }.orEmpty() + " · " + saved.text.lineSequence().firstOrNull { it.isNotBlank() && !it.startsWith("#") }.orEmpty().take(70), style = Console.Small, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { viewModel.deleteSaved(saved) }) { Icon(Icons.Filled.Delete, contentDescription = "Sil", tint = Console.InkMuted, modifier = Modifier.size(18.dp)) }
                    }
                }
            }
        }
    }

    st.answer?.let { a -> AnswerSheet(st.answerTitle, a, onClose = viewModel::closeAnswer, onRegenerate = { viewModel.closeAnswer(); viewModel.run() }) }
}

@Composable
private fun TextLink(text: String, onClick: () -> Unit) {
    Text(text, style = Console.Label.copy(color = Console.Accent), modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick).padding(4.dp))
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Console.Accent, unfocusedBorderColor = Console.Border,
    focusedContainerColor = Console.Surface, unfocusedContainerColor = Console.Surface,
    focusedTextColor = Console.Ink, unfocusedTextColor = Console.Ink, cursorColor = Console.Accent,
    focusedLabelColor = Console.Accent, unfocusedLabelColor = Console.InkSecondary
)

@Composable
private fun TaskInputs(st: AssistantState, vm: AssistantViewModel) {
    when (st.task) {
        AiTask.Event -> Column {
            Text("Hangi gün için?", style = Console.Label)
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(SpecialDays.upcoming()) { d ->
                    val on = st.eventName == d.name
                    Tag(
                        "${d.name} · ${d.daysLeft()} gün",
                        fg = if (on) Color.White else Console.InkSecondary,
                        bg = if (on) Console.Accent else Console.Grid,
                        modifier = Modifier.clip(RoundedCornerShape(50)).clickable { vm.setEvent(d.name, d.date.toString()) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = st.eventName, onValueChange = { vm.setEvent(it, st.eventDate) },
                label = { Text("ya da kendi etkinliğin (ör. 100 bin indirme kutlaması)") },
                singleLine = true, colors = fieldColors(), modifier = Modifier.fillMaxWidth()
            )
        }
        AiTask.Social -> Column {
            Segmented(
                options = SocialPlatform.entries.map { it.name to it.label.substringBefore(" /").substringBefore(" (") },
                selected = st.platform.name,
                onSelect = { vm.setPlatform(SocialPlatform.valueOf(it)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = st.topic, onValueChange = vm::setTopic,
                label = { Text("Konu (boş bırakabilirsin)") },
                colors = fieldColors(), modifier = Modifier.fillMaxWidth(), maxLines = 3
            )
        }
        AiTask.Ask -> OutlinedTextField(
            value = st.question, onValueChange = vm::setQuestion,
            label = { Text("Sorun") }, colors = fieldColors(), modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 6
        )
        else -> Text(
            "Son 14 günün canlı verileri, son geri bildirimler ve yaklaşan özel günler otomatik eklenir.",
            style = Console.Small
        )
    }
}

@Composable
private fun SettingsCard(st: AssistantState, vm: AssistantViewModel) {
    val context = LocalContext.current
    var key by remember { mutableStateOf("") }
    var model by remember(st.model) { mutableStateOf(st.model) }
    var search by remember(st.webSearch) { mutableStateOf(st.webSearch) }
    ConsoleCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Key, contentDescription = null, tint = Console.Accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("OpenRouter anahtarı", style = Console.Subtitle)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "1) openrouter.ai'de hesap aç, Credits sayfasından bakiye yükle.\n2) Keys sayfasında \"Create Key\" de ve bir harcama limiti koy (ör. aylık 2 $).\n3) Anahtarı buraya yapıştır, kaydet.\nAnahtar sadece bu telefonda saklanır. Her cevabın maliyeti cevabın üstünde yazar.",
            style = Console.Small
        )
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.clip(RoundedCornerShape(8.dp)).clickable {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://openrouter.ai/settings/keys")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = Console.Accent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("openrouter.ai/settings/keys", style = Console.Label.copy(color = Console.Accent, fontWeight = FontWeight.SemiBold))
        }
        st.usage?.let { u ->
            Spacer(Modifier.height(8.dp))
            Surface(shape = RoundedCornerShape(10.dp), color = Console.Grid, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    UsageFigure("Bugün", OpenRouterClient.usd(u.daily), Modifier.weight(1f))
                    UsageFigure("Bu ay", OpenRouterClient.usd(u.monthly), Modifier.weight(1f))
                    UsageFigure("Kalan limit", u.limitRemaining?.let { OpenRouterClient.usd(it) } ?: "limitsiz", Modifier.weight(1f))
                    IconButton(onClick = vm::refreshUsage) { Icon(Icons.Filled.Refresh, contentDescription = "Tazele", tint = Console.InkSecondary, modifier = Modifier.size(18.dp)) }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = key, onValueChange = { key = it.trim() },
            label = { Text(if (st.hasKey) "Kayıtlı: ${st.keyPreview} (değiştirmek için yapıştır)" else "API anahtarı") },
            singleLine = true, visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = fieldColors(), modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = model, onValueChange = { model = it.trim() },
            label = { Text("Model") }, singleLine = true, colors = fieldColors(), modifier = Modifier.fillMaxWidth()
        )
        Text("Önerilen: ${OpenRouterClient.DEFAULT_MODEL}. Çalışmazsa sırayla ${OpenRouterClient.FALLBACK_MODELS.joinToString()} denenir.", style = Console.Small)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Web'de ara", style = Console.Subtitle)
                Text("Güncel trendler ve tarihler için. Cevap başına maliyeti birkaç kat artırır (Gemini'de arama başına yaklaşık 1,4 sent).", style = Console.Small)
            }
            Switch(checked = search, onCheckedChange = { search = it }, colors = SwitchDefaults.colors(checkedTrackColor = Console.Accent))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                shape = RoundedCornerShape(12.dp), color = Console.Accent,
                modifier = Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(12.dp)).clickable { vm.saveSettings(key, model, search); key = "" }
            ) { Box(contentAlignment = Alignment.Center) { Text("Kaydet", style = Console.Subtitle.copy(color = Color.White)) } }
            if (st.hasKey) {
                Surface(
                    shape = RoundedCornerShape(12.dp), color = Console.Surface, border = BorderStroke(1.dp, Console.Border),
                    modifier = Modifier.height(44.dp).clip(RoundedCornerShape(12.dp)).clickable { vm.forgetKey() }
                ) { Box(Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) { Text("Anahtarı sil", style = Console.Subtitle.copy(color = Console.Critical)) } }
            }
        }
    }
}

@Composable
private fun UsageFigure(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = Console.Small)
        Text(value, style = Console.Subtitle)
    }
}

@Composable
private fun AnswerSheet(title: String, a: AiAnswer, onClose: () -> Unit, onRegenerate: () -> Unit) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = Console.Bg, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().background(Console.Surface).padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Kapat", tint = Console.Ink) }
                    Column(Modifier.weight(1f)) {
                        Text(title, style = Console.Subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            listOfNotNull(
                                a.model.takeIf { it != AssistantViewModel.SAVED },
                                a.costUsd?.let { OpenRouterClient.usd(it) },
                                "web araması".takeIf { a.searched }
                            ).joinToString(" · ").ifBlank { "kayıtlı cevap" },
                            style = Console.Small, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { copy(context, a.text) }) { Icon(Icons.Filled.ContentCopy, contentDescription = "Kopyala", tint = Console.InkSecondary) }
                    IconButton(onClick = { share(context, title, a.text) }) { Icon(Icons.Filled.Share, contentDescription = "Paylaş", tint = Console.InkSecondary) }
                    if (a.model != AssistantViewModel.SAVED) IconButton(onClick = onRegenerate) { Icon(Icons.Filled.Refresh, contentDescription = "Yeniden", tint = Console.InkSecondary) }
                }
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
                    MarkdownText(a.text)
                    if (a.sources.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        Text("Kaynaklar", style = Console.Subtitle)
                        a.sources.take(10).forEach { (t, u) ->
                            Text(
                                "• $t", style = Console.Small.copy(color = Console.Blue),
                                modifier = Modifier.padding(vertical = 3.dp).clickable {
                                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }
}

private fun copy(context: Context, text: String) {
    runCatching {
        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Karalak", text))
        android.widget.Toast.makeText(context, "Kopyalandı", android.widget.Toast.LENGTH_SHORT).show()
    }
}

private fun share(context: Context, title: String, text: String) {
    runCatching {
        val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_SUBJECT, title); putExtra(Intent.EXTRA_TEXT, text) }
        context.startActivity(Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
