package com.sualtikasifi.cizimhafiza.presentation.common

import android.content.Context
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Small pictures (profile photos, avatar frames) decoded ONCE, at about the size they are drawn at, off the main thread.
 *
 * [androidx.compose.ui.res.painterResource] decodes the whole picture on the main thread each time an item that shows it
 * enters the composition. In a lazy grid that is every picture that scrolls into view: a 512 x 512 WebP is a 1 MB bitmap
 * and a few milliseconds each, several per frame while the finger moves, and again every time the grid is scrolled back —
 * the stutter in the profile-picture window. Here they come from a bounded cache; one that is not there yet is decoded
 * on a background thread and drawn when it arrives, and [preload] fills the cache before anyone scrolls.
 */
object ArtCache {
    private val BUDGET_BYTES = minOf(40L * 1024 * 1024, Runtime.getRuntime().maxMemory() / 6).toInt()
    private val cache = object : LruCache<Long, ImageBitmap>(BUDGET_BYTES) {
        override fun sizeOf(key: Long, value: ImageBitmap): Int = value.width * value.height * 4
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private fun key(res: Int, maxPx: Int): Long = (res.toLong() shl 16) or maxPx.toLong()

    fun peek(res: Int, maxPx: Int): ImageBitmap? = cache.get(key(res, maxPx))

    suspend fun load(resources: Resources, res: Int, maxPx: Int): ImageBitmap? = withContext(Dispatchers.Default) {
        peek(res, maxPx) ?: decode(resources, res, maxPx)?.also { cache.put(key(res, maxPx), it) }
    }

    /** Decodes [resIds] one after another in the background, so they are waiting when a window opens. */
    fun preload(context: Context, resIds: List<Int>, maxPx: Int = AvatarArtPx) {
        val resources = context.applicationContext.resources
        scope.launch {
            for (id in resIds) {
                if (peek(id, maxPx) != null) continue
                runCatching { decode(resources, id, maxPx)?.let { cache.put(key(id, maxPx), it) } }
            }
        }
    }

    /** The largest power-of-two reduction that still leaves the picture at least [maxPx] on its short side. */
    private fun decode(resources: Resources, res: Int, maxPx: Int): ImageBitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true; inScaled = false }
        BitmapFactory.decodeResource(resources, res, bounds)
        var sample = 1
        val shortSide = minOf(bounds.outWidth, bounds.outHeight)
        while (shortSide / (sample * 2) >= maxPx) sample *= 2
        val options = BitmapFactory.Options().apply { inScaled = false; inSampleSize = sample }
        return BitmapFactory.decodeResource(resources, res, options)?.asImageBitmap()
    }

    /** Gives memory back when the system asks (see CizimHafizaApp.onTrimMemory). */
    fun trim(level: Int) {
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) cache.evictAll()
    }

    /** The size, in pixels, the profile pictures and frames are decoded at: a little over what the biggest cell shows. */
    const val AvatarArtPx = 320
}

/** The picture [res] at about [maxPx], from [ArtCache]; null for the instant before a background decode finishes. */
@Composable
fun rememberArtBitmap(res: Int, maxPx: Int = ArtCache.AvatarArtPx): ImageBitmap? {
    val resources = LocalContext.current.resources
    var bitmap by remember(res, maxPx) { mutableStateOf(ArtCache.peek(res, maxPx)) }
    if (bitmap == null) {
        LaunchedEffect(res, maxPx) { bitmap = ArtCache.load(resources, res, maxPx) }
    }
    return bitmap
}
