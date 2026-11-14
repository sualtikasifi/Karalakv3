package com.sualtikasifi.cizimhafiza.presentation.common

import android.content.Context
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Keeps the big full-screen pictures (a 1080 x 2400 backdrop is ~10 MB once decoded) in memory between screens.
 *
 * Without it every screen change decoded its backdrop on the main thread, in the middle of the slide animation —
 * the hitch at the start of nearly every transition. Now the likely next ones are decoded in the background while the
 * home screen sits idle (see [preload]), and a screen that comes back finds its picture already there. The cache is
 * bounded, so memory stays flat: the least recently used picture goes first.
 */
object BackdropCache {
    private val BUDGET_BYTES = minOf(128L * 1024 * 1024, Runtime.getRuntime().maxMemory() / 3).toInt()

    private val cache = object : LruCache<Int, ImageBitmap>(BUDGET_BYTES) {
        override fun sizeOf(key: Int, value: ImageBitmap): Int = value.width * value.height * 4
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun get(resources: Resources, id: Int): ImageBitmap {
        cache.get(id)?.let { return it }
        // One decode per picture at a time: when the screen asks for a backdrop the background preload is still decoding,
        // it waits for that result instead of decoding the same ~10 MB picture a second time in parallel.
        synchronized(lockFor(id)) {
            cache.get(id)?.let { return it }
            val decoded = BitmapFactory.decodeResource(resources, id, BitmapFactory.Options().apply { inScaled = false })
                ?.asImageBitmap() ?: throw IllegalStateException("Cannot decode drawable $id")
            cache.put(id, decoded)
            return decoded
        }
    }

    private val locks = HashMap<Int, Any>()
    private fun lockFor(id: Int): Any = synchronized(locks) { locks.getOrPut(id) { Any() } }

    /** Drops a picture that will not be needed again (the opening scene once the app is up), freeing its memory. */
    fun evict(id: Int) { cache.remove(id) }

    /**
     * Decodes [ids] on a background thread, one after another, so they are waiting when their screens open. [ids] go
     * most wanted first, and only as many as fit in the cache's free room are decoded: on a phone with a small memory
     * budget, preloading ten backdrops into room for six made each one push out an earlier one (the home scene among
     * them), so the work was thrown away and the screen being returned to had to decode its picture again.
     */
    fun preload(context: Context, ids: List<Int>) {
        val resources = context.applicationContext.resources
        scope.launch {
            for (id in ids) {
                if (cache.get(id) != null) continue
                val bytes = runCatching { decodedBytes(resources, id) }.getOrDefault(0)
                if (bytes <= 0 || cache.size() + bytes > cache.maxSize()) break
                runCatching { get(resources, id) }
            }
        }
    }

    /** How big [id] is once decoded, read from its header only. */
    private fun decodedBytes(resources: Resources, id: Int): Int {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true; inScaled = false }
        BitmapFactory.decodeResource(resources, id, o)
        return o.outWidth * o.outHeight * 4
    }

    /**
     * Gives memory back when the system asks (see CizimHafizaApp.onTrimMemory). In the background the pictures are
     * dropped entirely: a process holding ~100 MB of decoded backdrops is among the first Android kills, and with it
     * the game the player left for a moment. While on screen and memory runs low, half is kept.
     */
    fun trim(level: Int) {
        when {
            level >= android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> cache.evictAll()
            level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW &&
                level < android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> cache.trimToSize(cache.maxSize() / 2)
        }
    }
}

/** [painterResource] for a large backdrop: served from [BackdropCache] (decoded off the main thread when preloaded). */
@Composable
fun cachedPainterResource(id: Int): Painter {
    val resources = LocalContext.current.resources
    val bitmap = remember(id) { runCatching { BackdropCache.get(resources, id) }.getOrNull() }
    return if (bitmap != null) remember(bitmap) { BitmapPainter(bitmap) } else painterResource(id)
}

/**
 * The blurred copy of a painted scene that fills the bars left around it on a screen of another shape (an older 16:9
 * phone, a tablet, a foldable). [Modifier.blur] only works from Android 12: on Android 8 to 11 it is ignored and the
 * bars showed a sharp, cropped second copy of the scene. There the blur is made once instead, on a small copy of the
 * picture (an eighth of its size, softened by a few box blurs, about 40 KB), which the screen then stretches smoothly;
 * Android 12 and newer keep the real blur exactly as before.
 */
@Composable
fun BlurredSceneFill(id: Int, scene: Painter, radius: androidx.compose.ui.unit.Dp) {
    val fill = androidx.compose.ui.Modifier.fillMaxSize()
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        androidx.compose.foundation.Image(
            scene, contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = fill.blur(radius)
        )
    } else {
        val resources = LocalContext.current.resources
        val soft = remember(id) { runCatching { softCopy(resources, id) }.getOrNull() }
        if (soft != null) {
            androidx.compose.foundation.Image(
                remember(soft) { BitmapPainter(soft, filterQuality = androidx.compose.ui.graphics.FilterQuality.High) },
                contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = fill
            )
        } else {
            androidx.compose.foundation.Image(scene, contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = fill)
        }
    }
}

/** An eighth-size copy of [id], box-blurred three times each way (close to a gaussian), for [BlurredSceneFill]. */
private fun softCopy(resources: Resources, id: Int): ImageBitmap {
    val small = BitmapFactory.decodeResource(resources, id, BitmapFactory.Options().apply { inScaled = false; inSampleSize = 8 })
        ?: throw IllegalStateException("Cannot decode drawable $id")
    val w = small.width
    val h = small.height
    val px = IntArray(w * h)
    small.getPixels(px, 0, w, 0, 0, w, h)
    val tmp = IntArray(w * h)
    repeat(3) {
        boxBlur(px, tmp, w, h, radius = 3, horizontal = true)
        boxBlur(tmp, px, w, h, radius = 3, horizontal = false)
    }
    val out = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
    out.setPixels(px, 0, w, 0, 0, w, h)
    small.recycle()
    return out.asImageBitmap()
}

/** One pass of a box blur along rows or columns, the edges held (the way the real blur treats them). */
private fun boxBlur(src: IntArray, dst: IntArray, w: Int, h: Int, radius: Int, horizontal: Boolean) {
    val lines = if (horizontal) h else w
    val len = if (horizontal) w else h
    val span = 2 * radius + 1
    for (line in 0 until lines) {
        fun at(i: Int): Int {
            val k = i.coerceIn(0, len - 1)
            return if (horizontal) src[line * w + k] else src[k * w + line]
        }
        var a = 0; var r = 0; var g = 0; var b = 0
        for (i in -radius..radius) {
            val c = at(i)
            a += c ushr 24; r += (c shr 16) and 0xFF; g += (c shr 8) and 0xFF; b += c and 0xFF
        }
        for (i in 0 until len) {
            val o = if (horizontal) line * w + i else i * w + line
            dst[o] = ((a / span) shl 24) or ((r / span) shl 16) or ((g / span) shl 8) or (b / span)
            val add = at(i + radius + 1)
            val drop = at(i - radius)
            a += (add ushr 24) - (drop ushr 24)
            r += ((add shr 16) and 0xFF) - ((drop shr 16) and 0xFF)
            g += ((add shr 8) and 0xFF) - ((drop shr 8) and 0xFF)
            b += (add and 0xFF) - (drop and 0xFF)
        }
    }
}
