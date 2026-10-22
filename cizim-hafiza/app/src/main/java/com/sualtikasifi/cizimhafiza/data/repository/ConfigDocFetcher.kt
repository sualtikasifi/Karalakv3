package com.sualtikasifi.cizimhafiza.data.repository

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.sualtikasifi.cizimhafiza.util.NonFatal
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

/**
 * Reads a small tuning document (config/economy, config/runes) without keeping a live listener on it.
 *
 * A listener re-delivers the document to every open app each time it is edited, and attaches (and bills) a read each
 * time it is not resumable from the cache. These documents change a few times a month, so the app instead:
 *  1. applies the copy already on the device at once (so a restart never waits on the network), then
 *  2. asks the server again only when the last server read is older than [TTL_MILLIS].
 * A change made in the console therefore reaches players within a few hours instead of instantly, which is fine for
 * numbers like gold per word. (The 2x-XP event is NOT served this way: it has to start and stop at once, see
 * XpEventRepositoryImpl.)
 */
internal object ConfigDocFetcher {
    const val TTL_MILLIS = 6L * 60 * 60 * 1000
    private const val PREFS = "config_doc_fetch"

    /** Loads [path] now, then again every [ttlMillis] for as long as the app process lives. */
    suspend fun keepFresh(context: Context, firestore: FirebaseFirestore, path: String, ttlMillis: Long = TTL_MILLIS, apply: (Map<String, Any?>?) -> Unit) {
        while (true) {
            load(context, firestore, path, ttlMillis, apply)
            delay(ttlMillis)
        }
    }

    /** [ttlMillis] 0 forces a server read (the developer panel's "fetch now"). */
    suspend fun load(context: Context, firestore: FirebaseFirestore, path: String, ttlMillis: Long, apply: (Map<String, Any?>?) -> Unit) {
        val ref = firestore.document(path)
        runCatching { ref.get(Source.CACHE).await() }.getOrNull()?.takeIf { it.exists() }?.let { apply(it.data) }

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val last = prefs.getLong(path, 0L)
        // A stamp in the future (the clock was set back) counts as stale.
        if (ttlMillis > 0 && last in 1..now && now - last < ttlMillis) return

        runCatching { ref.get(Source.SERVER).await() }
            .onSuccess { snapshot ->
                apply(snapshot.data)
                prefs.edit().putLong(path, now).apply()
            }
            .onFailure { NonFatal.report("ConfigDocFetcher", "could not read $path", it) }
    }
}
