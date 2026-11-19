package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Console
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleCard
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// "Kayıtlar": every finished video and story picture, to share from, mark as posted and delete.

private fun day(millis: Long): String = SimpleDateFormat("d MMM HH:mm", Locale("tr")).format(Date(millis))

internal fun LazyListScope.savedTab(st: StudioState, vm: StudioViewModel, context: Context) {
    item {
        var askAll by remember { mutableStateOf(false) }
        val posted = st.saved.count { it.shared }
        Column {
        Text(
            if (st.saved.isEmpty()) "Ürettiğin videolar ve Bil Bakalım görselleri burada birikir."
            else "${st.saved.size} içerik · ${st.saved.size - posted} paylaşılmayı bekliyor · $posted paylaşıldı",
            style = Console.Small
        )
        if (posted > 0) {
            SmallLink("Paylaşılanların hepsini sil ($posted)", Console.Critical) { askAll = true }
        }
        }
        if (askAll) AlertDialog(
            onDismissRequest = { askAll = false },
            title = { Text("Paylaşılanlar silinsin mi?") },
            text = { Text("Paylaşıldı diye işaretlenen $posted içerik ve dosyaları silinir. Bu geri alınamaz.") },
            confirmButton = { TextButton(onClick = { askAll = false; vm.deleteSharedSaved() }) { Text("Sil", color = Console.Critical) } },
            dismissButton = { TextButton(onClick = { askAll = false }) { Text("Vazgeç") } }
        )
    }
    items(st.saved.size, key = { st.saved[it].id }) { i -> SavedCard(st.saved[i], vm, context) }
}

@Composable
private fun SavedCard(m: SavedMedia, vm: StudioViewModel, context: Context) {
    val file = vm.savedFile(m)
    var ask by remember { mutableStateOf(false) }
    ConsoleCard(Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SavedThumb(file, m.isVideo, Modifier.width(78.dp).height(139.dp))
            Column(Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Tag(m.format)
                    Tag(if (m.isVideo) "Video" else "Görsel")
                    if (m.shared) Tag("Paylaşıldı", fg = Console.Good, bg = Console.GoodSoft)
                }
                Text(m.title, style = Console.Subtitle, maxLines = 2, modifier = Modifier.padding(top = 4.dp))
                if (m.detail.isNotBlank()) Text(m.detail, style = Console.Small, maxLines = 1)
                Text(
                    if (m.shared) "Yapıldı ${day(m.madeAt)} · paylaşıldı ${day(m.sharedAt)}" else "Yapıldı ${day(m.madeAt)}",
                    style = Console.Small
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionButton(Icons.Filled.Share, "Paylaş", Console.Accent, androidx.compose.ui.graphics.Color.White, Modifier.weight(1f)) {
                        if (file.exists()) {
                            if (m.isVideo) share(context, file, "", "") else shareImages(context, listOf(file), "")
                        } else android.widget.Toast.makeText(context, "Dosya bulunamadı", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    ActionButton(
                        Icons.Filled.Check, if (m.shared) "Geri al" else "Paylaşıldı",
                        if (m.shared) Console.Grid else Console.GoodSoft, if (m.shared) Console.Ink else Console.Good, Modifier.weight(1f)
                    ) { vm.toggleSavedShared(m.id) }
                }
                SmallLink("Sil", Console.Critical) { ask = true }
            }
        }
    }
    if (ask) AlertDialog(
        onDismissRequest = { ask = false },
        title = { Text("Silinsin mi?") },
        text = { Text("\"${m.title}\" ${if (m.isVideo) "videosu" else "görseli"} bu telefondan silinir. Bu geri alınamaz.") },
        confirmButton = { TextButton(onClick = { ask = false; vm.deleteSaved(m.id) }) { Text("Sil", color = Console.Critical) } },
        dismissButton = { TextButton(onClick = { ask = false }) { Text("Vazgeç") } }
    )
}

/** A small picture of the file: the image itself, or a video's first frame (read off the main thread). */
@Composable
private fun SavedThumb(file: File, video: Boolean, modifier: Modifier) {
    val bmp by produceState<Bitmap?>(null, file.path) {
        value = withContext(Dispatchers.IO) { runCatching { if (video) videoFrame(file) else imageSample(file) }.getOrNull() }
    }
    Box(modifier.clip(RoundedCornerShape(8.dp)).background(Console.Grid), contentAlignment = Alignment.Center) {
        bmp?.let { Image(it.asImageBitmap(), null, Modifier.matchParentSize(), contentScale = ContentScale.Crop) }
        if (video && bmp != null) Box(Modifier.size(28.dp).clip(RoundedCornerShape(50)).background(Console.Dark.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
            androidx.compose.material3.Icon(Icons.Filled.PlayArrow, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

private fun imageSample(file: File): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= 240) sample *= 2
    return BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
}

private fun videoFrame(file: File): Bitmap? {
    val r = MediaMetadataRetriever()
    return try { r.setDataSource(file.path); r.getFrameAtTime(500_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) } finally { runCatching { r.release() } }
}
