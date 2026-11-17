package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Console
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleCard
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Segmented
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotSource

/**
 * "Bil Bakalım Story": pick a drawing, have the question and four answers written (the right one on a letter that changes from
 * story to story), and take the 9:20 picture to Instagram, where the maker adds the quiz sticker with the pasted text.
 */
internal fun LazyListScope.riddleTab(st: StudioState, vm: StudioViewModel, context: Context) {
    val r = st.riddle
    item {
        Segmented(
            options = ForgotSource.entries.map { it.name to it.title },
            selected = r.source.name,
            onSelect = { vm.setRiddleSource(ForgotSource.valueOf(it)) },
            modifier = Modifier.fillMaxWidth()
        )
    }
    val fromPool = r.source == ForgotSource.Pool
    val pool = if (fromPool) st.real.recent.map { it.toStudio() } else st.drawings
    when {
        fromPool && st.real.loadingRecent && pool.isEmpty() -> item { Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Console.Accent) } }
        fromPool && pool.isEmpty() -> item {
            ConsoleCard(Modifier.fillMaxWidth()) {
                Text(st.real.recentError ?: "Havuzda çizim bulunamadı.", style = Console.Small)
                ActionButton(Icons.Filled.Refresh, "Yeniden oku", Console.Surface, Console.Ink, Modifier.padding(top = 6.dp)) { vm.refreshRecent() }
            }
        }
        st.loading && !fromPool -> item { Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Console.Accent) } }
        pool.isEmpty() -> item { ConsoleCard(Modifier.fillMaxWidth()) { Text("Bu telefonda kayıtlı çizim yok. Birkaç tur oyna, sonra buraya dön.", style = Console.Small) } }
        else -> pool.take(r.shown).chunked(3).forEach { row ->
            item(key = "rd-" + row.first().id) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { d -> Thumb(d, if (r.drawingId == d.id) 0 else -1, Modifier.weight(1f)) { vm.selectRiddle(d) } }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
    if (pool.size > r.shown) {
        item(key = "rd-more") {
            ActionButton(Icons.Filled.Refresh, "Daha fazla çizim göster (${pool.size - r.shown} kaldı)", Console.Surface, Console.Ink, Modifier.fillMaxWidth()) { vm.showMoreRiddle() }
        }
    }
    val drawing = st.forgotDrawing(r.drawingId) ?: return

    // the question and the four answers
    item {
        ConsoleCard(Modifier.fillMaxWidth()) {
            Text("Soru ve şıklar · Doğru cevap: ${drawing.word.uppercase(java.util.Locale("tr"))}", style = Console.Label)
            if (r.options.isEmpty()) {
                Text("Çizime benzeyen 3 yanlış kelimeyle birlikte yazdırılır; doğru şık her seferinde farklı bir harfe gelir.", style = Console.Small)
            } else {
                Field("Soru", r.question, { vm.setRiddleQuestion(it) })
                r.options.forEachIndexed { i, text ->
                    val right = i == r.correct
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 2.dp)) {
                        Box(
                            Modifier.padding(top = 6.dp).size(30.dp).clip(CircleShape).background(if (right) Color(0xFF2E9B3E) else Console.Surface),
                            contentAlignment = Alignment.Center
                        ) { Text(RiddleStory.LETTERS[i].toString(), style = Console.Label.copy(color = if (right) Color.White else Console.Ink)) }
                        Box(Modifier.weight(1f)) { Field(null, text, { vm.setRiddleOption(i, it) }) }
                    }
                }
                Text("Doğru cevap: ${RiddleStory.LETTERS[r.correct]} şıkkı (yeşil). Instagram'da Test çıkartmasında bunu işaretle.", style = Console.Small.copy(color = Color(0xFF2E9B3E)), modifier = Modifier.padding(top = 6.dp))
            }
            r.error?.let { Text(it, style = Console.Small.copy(color = Console.Critical), modifier = Modifier.padding(top = 6.dp)) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                if (r.writing) CircularProgressIndicator(Modifier.size(22.dp), color = Console.Accent, strokeWidth = 2.dp)
                else ActionButton(
                    Icons.Filled.AutoAwesome, if (r.options.isEmpty()) "Soruyu ve şıkları yaz" else "Yeniden yaz",
                    Console.Accent, Color.White, Modifier.weight(1f)
                ) { vm.writeRiddle() }
                if (r.options.isNotEmpty()) ActionButton(
                    Icons.Filled.ContentCopy, "Kopyala", Console.Surface, Console.Ink, Modifier.weight(1f)
                ) { copy(context, RiddleStory.asText(r.question, r.options, r.correct)) }
            }
        }
    }

    // the story picture
    item {
        ConsoleCard(Modifier.fillMaxWidth()) {
            Text("Story görseli (1080 x 2400)", style = Console.Label)
            val file = r.image
            if (file == null) {
                Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Console.Accent) }
            } else {
                val bitmap = remember(file) {
                    runCatching { BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = 3 })?.asImageBitmap() }.getOrNull()
                }
                if (bitmap != null) {
                    Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                        Image(bitmap, contentDescription = "Story görseli", contentScale = ContentScale.Fit, modifier = Modifier.width(240.dp).aspectRatio(9f / 20f).clip(RoundedCornerShape(12.dp)))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
                    ActionButton(Icons.Filled.Share, "Paylaş / kaydet", Console.Accent, Color.White, Modifier.weight(1.4f)) {
                        shareImages(context, listOf(file), if (r.options.isNotEmpty()) RiddleStory.asText(r.question, r.options, r.correct) else "")
                    }
                    ActionButton(Icons.Filled.Refresh, "Yeniden çiz", Console.Surface, Console.Ink, Modifier.weight(1f)) { vm.selectRiddle(drawing) }
                }
            }
        }
    }
}
