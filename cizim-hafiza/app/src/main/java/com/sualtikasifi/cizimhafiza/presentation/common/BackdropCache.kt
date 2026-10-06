package com.sualtikasifi.cizimhafiza.presentation.common

import android.content.Context
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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

    /** Decodes [ids] on a background thread, one after another, so they are waiting when their screens open. */
    fun preload(context: Context, ids: List<Int>) {
        val resources = context.applicationContext.resources
        scope.launch {
            ids.forEach { id -> runCatching { get(resources, id) } }
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
