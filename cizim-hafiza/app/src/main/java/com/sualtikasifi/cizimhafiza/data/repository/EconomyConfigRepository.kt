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
            runCatching { ConfigDocFetcher.keepFresh(context, firestore, PATH) { EconomyConfig.apply(it) } }
                .onFailure { NonFatal.report("EconomyConfigRepository", "config read failed", it) }
        }
    }

    /** Reads the document from the server right now, whatever the last read was (the developer panel). */
    fun refreshNow() {
        scope.launch { ConfigDocFetcher.load(context, firestore, PATH, 0L) { EconomyConfig.apply(it) } }
    }

    private companion object {
        const val PATH = "config/economy"
    }
}
