package com.sualtikasifi.cizimhafiza.data.repository

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

/**
 * Reads a document that never changes after it is written from the local cache first, and
 * only goes to the server when the cache does not have it.
 *
 * Firestore bills every server read, and a plain `get()` always asks the server even when the
 * SDK's persistent cache (see FirebaseModule) already holds the exact document. That is wasted
 * money and, for the large bot drawings, several megabytes of the player's mobile data. Only
 * use this for documents that are immutable once written (recorded rounds' drawings, the
 * sealed bot training set): a document that can change would be served stale.
 *
 * A cache miss, a document missing from the cache, or a cache error all fall through to the
 * normal server read, so behaviour is unchanged apart from doing less work.
 */
suspend fun DocumentReference.getCacheFirst(): DocumentSnapshot {
    val cached = runCatching { get(Source.CACHE).await() }.getOrNull()
    if (cached != null && cached.exists()) return cached
    return get().await()
}
