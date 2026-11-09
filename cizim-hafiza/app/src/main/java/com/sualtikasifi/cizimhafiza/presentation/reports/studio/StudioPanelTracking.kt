package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Console
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleCard
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleIconButton
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Tag
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// The studio panel's two ledgers: what making videos has cost, and which videos were made and posted.

private fun day(millis: Long): String = SimpleDateFormat("d MMM HH:mm", Locale("tr")).format(Date(millis))

/** The spend card and the log of made videos, at the end of the panel for either format. */
internal fun LazyListScope.trackingItems(st: StudioState, vm: StudioViewModel) {
    item { Step("$", "Harcama") }
    item {
        // read once when the panel is first looked at, then on the refresh button
        LaunchedEffect(Unit) { if (st.spend.readAt == 0L) vm.refreshSpend() }
        SpendCard(st.spend, st.aiCost, vm)
    }
    item { Step("🗓", "Yayın günlüğü", if (st.publishLog.isEmpty()) null else "${st.publishLog.count { !it.published }} bekliyor") }
    item { PublishLogCard(st.publishLog, vm) }
}

@Composable
private fun SpendCard(spend: SpendUi, lastWrite: Double?, vm: StudioViewModel) {
    ConsoleCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (spend.readAt == 0L && spend.loading) "Okunuyor…" else "Anahtarların bildirdiği harcama",
                style = Console.Small, modifier = Modifier.weight(1f)
            )
            ConsoleIconButton(Icons.Filled.Refresh, "Yenile", { vm.refreshSpend() }, size = 32.dp)
        }
        Spacer(Modifier.height(8.dp))
        // the assistant: OpenRouter's own figures for the key
        Text("Asistan (OpenRouter)", style = Console.Label)
        if (spend.openRouterMonth != null) {
            Text(
                "Bu ay ${SpendText.dollars(spend.openRouterMonth)} · bugün ${SpendText.dollars(spend.openRouterToday ?: 0.0)} · toplam ${SpendText.dollars(spend.openRouterTotal ?: 0.0)}",
                style = Console.Subtitle
            )
            spend.openRouterLeft?.let { Text("Anahtarın limitinden kalan: ${SpendText.dollars(it)}", style = Console.Small) }
        } else Text("—", style = Console.Subtitle)
        lastWrite?.let { Text("Son cümle yazımı: ${SpendText.dollars(it)}", style = Console.Small) }
        Spacer(Modifier.height(10.dp))
        // the voice: ElevenLabs credits this month
        Text("Ses (ElevenLabs)", style = Console.Label)
        val used = spend.elevenUsed
        val limit = spend.elevenLimit
        if (used != null && limit != null) {
            val pct = SpendText.percent(used, limit)
            Text("Bu ay $used / $limit kredi" + (pct?.let { " (%$it)" } ?: ""), style = Console.Subtitle)
            if (pct != null) {
                Spacer(Modifier.height(4.dp))
                Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Console.Grid)) {
                    Box(Modifier.fillMaxWidth(pct / 100f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(if (pct >= 90) Console.Critical else Console.Accent))
                }
            }
        } else Text("—", style = Console.Subtitle)
        spend.note?.let { Text(it, style = Console.Small.copy(color = Console.InkMuted), modifier = Modifier.padding(top = 8.dp)) }
    }
}

@Composable
private fun PublishLogCard(log: List<PublishEntry>, vm: StudioViewModel) {
    ConsoleCard(Modifier.fillMaxWidth(), padding = 4.dp) {
        if (log.isEmpty()) {
            Text("Bir video oluşturunca burada görünür: hangi gün, hangi kodla, yayınlandı mı.", style = Console.Small, modifier = Modifier.padding(10.dp))
            return@ConsoleCard
        }
        log.take(SHOWN).forEachIndexed { i, e ->
            if (i > 0) Box(Modifier.fillMaxWidth().padding(horizontal = 10.dp).height(1.dp).background(Console.Grid))
            Row(Modifier.fillMaxWidth().clickable { vm.openLogEntry(e) }.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(e.label.ifBlank { "—" }, style = Console.Subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        Spacer(Modifier.width(6.dp))
                        Tag(e.format, fg = Console.InkSecondary, bg = Console.Grid)
                    }
                    Text(listOf(day(e.madeAt), e.detail).filter { it.isNotBlank() }.joinToString(" · "), style = Console.Small, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (e.code.isNotBlank() || e.reward.isNotBlank()) {
                        Text(
                            listOf(e.code.takeIf { it.isNotBlank() }, e.reward.takeIf { it.isNotBlank() }).filterNotNull().joinToString(" · "),
                            style = Console.Small.copy(color = Console.AccentInk, fontFamily = FontFamily.Monospace), maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (e.draft) {
                        // no video yet: the line is a draft that opens where it was left
                        Tag("taslak · devam et", fg = Console.AccentInk, bg = Console.AccentSoft)
                    } else {
                        Tag(
                            if (e.published) "yayında · ${day(e.publishedAt)}" else "bekliyor",
                            fg = if (e.published) Console.GoodText else Console.WarningText,
                            bg = if (e.published) Console.GoodSoft else Console.WarningSoft,
                            modifier = Modifier.clickable { vm.togglePublished(e.id) }
                        )
                        Tag(
                            if (e.done) "tamamlandı" else "tamamla",
                            fg = if (e.done) Console.GoodText else Console.InkMuted,
                            bg = if (e.done) Console.GoodSoft else Console.Grid,
                            modifier = Modifier.clickable { vm.toggleDone(e.id) }
                        )
                    }
                }
                ConsoleIconButton(Icons.Filled.Delete, "Günlükten sil", { vm.deletePublishEntry(e.id) }, size = 32.dp, tint = Console.Critical)
            }
        }
        if (log.size > SHOWN) Text("+${log.size - SHOWN} eski kayıt", style = Console.Small, modifier = Modifier.padding(10.dp))
        Text(
            "Satıra dokununca kaldığın yerden devam edersin (video bittiyse son hâlini görürsün). Bekliyor / yayında ve tamamla etiketlerine dokunarak durumu değiştir. " +
                "Tamamlandı olan Düello videolarının cümleleri, yapay zekaya örnek olarak gösterilir.",
            style = Console.Small.copy(color = Console.InkMuted), modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

private const val SHOWN = 15
