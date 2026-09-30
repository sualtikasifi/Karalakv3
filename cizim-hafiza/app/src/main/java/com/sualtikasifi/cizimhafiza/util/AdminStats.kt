package com.sualtikasifi.cizimhafiza.util

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Daily counters for the developer panel's "İstatistik" tab: ad requests / payouts / estimated
 * revenue per placement, app opens, finished games, and how many different players opened the
 * app each day.
 *
 * One document per UTC day (`adminStats/{yyyy-MM-dd}`), each counter a FieldValue.increment, so
 * every device adds to the same numbers. Events are held in memory and written together — at most
 * once every [FLUSH_INTERVAL_MILLIS], when [FLUSH_AT_EVENTS] pile up, or when the app leaves the
 * screen — so an ad-heavy session costs a couple of writes, not one per ad. A counter lost with a
 * killed process is an accepted error; these are trends, not accounting.
 *
 * Failing here is silent by design: statistics must never affect the game.
 */
@Singleton
class AdminStats @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: Lazy<FirebaseFirestore>,
    private val auth: Lazy<FirebaseAuth>
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs by lazy { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    private val pending = HashMap<String, Long>()
    private var pendingEvents = 0
    private var lastFlushAt = System.currentTimeMillis()

    /** Flushes when the app goes to the background; call once from Application.onCreate. */
    fun install(app: Application) {
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityStopped(activity: Activity) = flush()
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }

    fun record(key: String, amount: Long = 1L) {
        if (amount == 0L) return
        val flushNow = synchronized(pending) {
            pending[key] = (pending[key] ?: 0L) + amount
            pendingEvents++
            pendingEvents >= FLUSH_AT_EVENTS || System.currentTimeMillis() - lastFlushAt >= FLUSH_INTERVAL_MILLIS
        }
        if (flushNow) flush()
    }

    /** Counts this player once for today; the day's document is the distinct-player list. */
    fun markActiveToday() {
        val day = today()
        if (prefs.getString(KEY_LAST_ACTIVE_DAY, null) == day) return
        scope.launch {
            runCatching {
                val uid = auth.get().currentUser?.uid ?: return@launch
                firestore.get().collection(COLLECTION).document(day)
                    .collection(USERS).document(uid)
                    .set(mapOf("at" to FieldValue.serverTimestamp())).await()
                prefs.edit().putString(KEY_LAST_ACTIVE_DAY, day).apply()
            }
        }
    }

    fun flush() {
        val batch = synchronized(pending) {
            if (pending.isEmpty()) return
            val copy = HashMap(pending)
            pending.clear()
            pendingEvents = 0
            lastFlushAt = System.currentTimeMillis()
            copy
        }
        val day = today()
        scope.launch {
            runCatching {
                if (auth.get().currentUser == null) error("not signed in yet")
                firestore.get().collection(COLLECTION).document(day).set(
                    mapOf(
                        "day" to day,
                        "counts" to batch.mapValues { FieldValue.increment(it.value) }
                    ),
                    SetOptions.merge()
                ).await()
            }.onFailure {
                // Put the numbers back so the next flush retries them.
                synchronized(pending) { batch.forEach { (k, v) -> pending[k] = (pending[k] ?: 0L) + v } }
            }
        }
    }

    companion object {
        const val COLLECTION = "adminStats"
        const val USERS = "users"

        private const val PREFS_NAME = "admin_stats"
        private const val KEY_LAST_ACTIVE_DAY = "last_active_day"
        private const val FLUSH_AT_EVENTS = 25
        private const val FLUSH_INTERVAL_MILLIS = 10 * 60 * 1000L

        fun today(): String = LocalDate.now(ZoneOffset.UTC).toString()

        /** Counter names are `ad__<placement>__<event>`; split on the double underscore. */
        fun adKey(placement: String, event: String) = "ad__${placement}__$event"
    }
}
