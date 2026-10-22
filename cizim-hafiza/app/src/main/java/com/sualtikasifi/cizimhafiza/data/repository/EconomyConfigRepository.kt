package com.sualtikasifi.cizimhafiza.data.repository

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import com.sualtikasifi.cizimhafiza.util.EconomyConfig
import com.sualtikasifi.cizimhafiza.util.NonFatal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads the single `config/economy` document (see [ConfigDocFetcher]: cached copy at once, server again every few hours) and copies its numbers into [EconomyConfig] (gold per word, win and
 * achievement gold, the daily challenge's gold, the ad rewards and their waits…), so the economy can be tuned without an
 * app update. Every field is optional and range-checked; a missing document leaves the built-in defaults.
 */
@Singleton
class EconomyConfigRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var listening = false

    @Synchronized
    fun startListening() {
        if (listening) return
        listening = true
        scope.launch {
            // Every half hour rather than every few hours: the same document carries the forced-update switch
            // (minVersionCode, see UpdateGate), which has to reach phones soon after it is set.
            runCatching { ConfigDocFetcher.keepFresh(context, firestore, PATH, REFRESH_MILLIS) { apply(it) } }
                .onFailure { NonFatal.report("EconomyConfigRepository", "config read failed", it) }
        }
    }

    /** Reads the document from the server right now, whatever the last read was (the developer panel). */
    fun refreshNow() {
        scope.launch { ConfigDocFetcher.load(context, firestore, PATH, 0L) { apply(it) } }
    }

    private fun apply(data: Map<String, Any?>?) {
        EconomyConfig.apply(data)
        com.sualtikasifi.cizimhafiza.util.UpdateGate.apply(data)
    }

    private companion object {
        const val PATH = "config/economy"
        const val REFRESH_MILLIS = 30L * 60 * 1000
    }
}
