package com.sualtikasifi.cizimhafiza.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.sualtikasifi.cizimhafiza.util.EconomyConfig
import com.sualtikasifi.cizimhafiza.util.NonFatal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Listens to the single `config/economy` document and copies its numbers into [EconomyConfig] (gold per word, win and
 * achievement gold, the daily challenge's gold, the ad rewards and their waits…), so the economy can be tuned without an
 * app update. Every field is optional and range-checked; a missing document leaves the built-in defaults.
 */
@Singleton
class EconomyConfigRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var listening = false

    @Synchronized
    fun startListening() {
        if (listening) return
        listening = true
        scope.launch {
            firestoreFlow<Map<String, Any?>?>("economyConfig") { emit, onError ->
                firestore.document("config/economy").addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        onError(error)
                        return@addSnapshotListener
                    }
                    emit(snapshot?.data)
                }
            }.catch { NonFatal.report("EconomyConfigRepository", "listener failed", it) }
                .collect { data -> EconomyConfig.apply(data) }
        }
    }
}
