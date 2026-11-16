package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Console
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealTimeline
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Cuts a clip by looking at it: the video itself on top, under it a strip of its frames with a handle at each end of the part that
 * is used. Dragging a handle shows the frame it stands on in the video above; "Oynat" plays just the chosen part.
 * [onCut] gets the start and end in milliseconds when a handle is let go (or nudged).
 */
@Composable
internal fun ClipTrimmer(file: File, fullMs: Long, startMs: Long, endMs: Long, onCut: (Long, Long) -> Unit) {
    val context = LocalContext.current
    val minLen = RealTimeline.CLIP_MIN
    val maxLen = RealTimeline.CLIP_MAX
    var s by remember(file, startMs) { mutableStateOf(startMs) }
    var e by remember(file, endMs) { mutableStateOf(endMs) }
    var playing by remember(file) { mutableStateOf(false) }

    val exo = remember(file) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(android.net.Uri.fromFile(file)))
            volume = 0f
            prepare()
            pause()
        }
    }
    DisposableEffect(exo) { onDispose { exo.release() } }
    val startNow by rememberUpdatedState(s)
    val endNow by rememberUpdatedState(e)
    LaunchedEffect(playing) {
        if (!playing) { exo.pause(); return@LaunchedEffect }
        exo.seekTo(startNow)
        exo.play()
        while (playing) {
            kotlinx.coroutines.delay(40)
            if (exo.currentPosition >= endNow) { exo.pause(); exo.seekTo(startNow); playing = false }
        }
    }
    fun showAt(ms: Long) { playing = false; exo.pause(); exo.seekTo(ms.coerceIn(0L, fullMs)) }

    // the strip of frames, read once off the main thread
    val frames = remember(file) { mutableStateListOf<Bitmap?>() }
    LaunchedEffect(file, fullMs) {
        val count = 10
        frames.clear(); repeat(count) { frames.add(null) }
        val list = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val r = MediaMetadataRetriever()
            try {
                r.setDataSource(file.absolutePath)
                List(count) { i ->
                    runCatching {
                        val bmp = r.getFrameAtTime((fullMs * (i + 0.5) / count * 1000).toLong(), MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        bmp?.let { Bitmap.createScaledBitmap(it, (it.width * 120f / it.height).roundToInt().coerceAtLeast(1), 120, true) }
                    }.getOrNull()
                }
            } finally { r.release() }
        }
        list.forEachIndexed { i, b -> if (i < frames.size) frames[i] = b }
    }

    fun sec(ms: Long) = String.format(Locale("tr"), "%.1f", ms / 1000.0)

    Column(Modifier.fillMaxWidth().padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(12.dp)).background(Color.Black), contentAlignment = Alignment.Center) {
            AndroidView(
                factory = { c -> PlayerView(c).apply { useController = false; resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT; this.player = exo } },
                modifier = Modifier.fillMaxSize()
            )
        }

        // the strip, the handles and the dimmed parts that are left out
        var widthPx by remember { mutableStateOf(1) }
        val density = LocalDensity.current
        val handleW = 16.dp
        val handlePx = with(density) { handleW.toPx() }
        val stripH = 64.dp
        fun pxOf(ms: Long) = if (fullMs > 0) ms.toFloat() / fullMs * widthPx else 0f
        val full = maxOf(fullMs, 1L)
        Box(
            Modifier.fillMaxWidth().height(stripH).clip(RoundedCornerShape(8.dp)).background(Console.Surface).onSizeChanged { widthPx = it.width.coerceAtLeast(1) }
        ) {
            Row(Modifier.fillMaxSize()) {
                frames.forEach { b ->
                    Box(Modifier.weight(1f).fillMaxHeight().background(Color(0xFF222222))) {
                        if (b != null) Image(b.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    }
                }
            }
            // left out: dimmed
            Box(Modifier.offset { IntOffset(0, 0) }.width(with(density) { pxOf(s).toDp() }).fillMaxHeight().background(Color(0xAA000000)))
            Box(Modifier.offset { IntOffset(pxOf(e).roundToInt(), 0) }.width(with(density) { (widthPx - pxOf(e)).coerceAtLeast(0f).toDp() }).fillMaxHeight().background(Color(0xAA000000)))
            // the used part's frame
            Box(
                Modifier.offset { IntOffset(pxOf(s).roundToInt(), 0) }
                    .width(with(density) { (pxOf(e) - pxOf(s)).coerceAtLeast(0f).toDp() }).fillMaxHeight()
                    .border(3.dp, Console.Accent)
            )
            // handles
            Box(
                Modifier.offset { IntOffset((pxOf(s) - handlePx / 2).roundToInt(), 0) }.width(handleW).fillMaxHeight()
                    .background(Console.Accent, RoundedCornerShape(6.dp))
                    .pointerInput(fullMs) {
                        detectHorizontalDragGestures(
                            onDragEnd = { onCut(startNow, endNow) },
                            onDragCancel = { onCut(startNow, endNow) }
                        ) { _, dx ->
                            val d = (dx / widthPx * full).toLong()
                            val ns = (startNow + d).coerceIn(maxOf(0L, endNow - maxLen), endNow - minLen)
                            s = ns; showAt(ns)
                        }
                    }
            )
            Box(
                Modifier.offset { IntOffset((pxOf(e) - handlePx / 2).roundToInt(), 0) }.width(handleW).fillMaxHeight()
                    .background(Console.Accent, RoundedCornerShape(6.dp))
                    .pointerInput(fullMs) {
                        detectHorizontalDragGestures(
                            onDragEnd = { onCut(startNow, endNow) },
                            onDragCancel = { onCut(startNow, endNow) }
                        ) { _, dx ->
                            val d = (dx / widthPx * full).toLong()
                            val ne = (endNow + d).coerceIn(startNow + minLen, minOf(fullMs, startNow + maxLen))
                            e = ne; showAt(ne)
                        }
                    }
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ActionButton(
                if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (playing) "Durdur" else "Seçili kısmı oynat",
                Console.Accent, Color.White, Modifier.weight(1f)
            ) { playing = !playing }
            Text("${sec(s)} – ${sec(e)} sn (${sec(e - s)} sn)", style = Console.Small, modifier = Modifier.weight(1f))
        }
        // fine tuning, a tenth of a second at a time
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Başlangıç", style = Console.Small)
            SmallLink("−0,1") { val ns = (s - 100).coerceIn(maxOf(0L, e - maxLen), e - minLen); s = ns; showAt(ns); onCut(ns, e) }
            SmallLink("+0,1") { val ns = (s + 100).coerceIn(maxOf(0L, e - maxLen), e - minLen); s = ns; showAt(ns); onCut(ns, e) }
            Text("Bitiş", style = Console.Small)
            SmallLink("−0,1") { val ne = (e - 100).coerceIn(s + minLen, minOf(fullMs, s + maxLen)); e = ne; showAt(ne); onCut(s, ne) }
            SmallLink("+0,1") { val ne = (e + 100).coerceIn(s + minLen, minOf(fullMs, s + maxLen)); e = ne; showAt(ne); onCut(s, ne) }
        }
        Text("Tutamakları sürükle: video o anı gösterir. En az ${minLen / 1000} sn, en çok ${maxLen / 1000} sn kullanılır; videonun tamamı ${sec(fullMs)} sn.", style = Console.Small)
    }
}
