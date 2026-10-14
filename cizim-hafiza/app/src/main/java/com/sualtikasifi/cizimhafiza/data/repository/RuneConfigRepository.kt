package com.sualtikasifi.cizimhafiza.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.sualtikasifi.cizimhafiza.domain.model.RuneGates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Listens to the single `config/runes` document and copies its numbers into [RuneGates], so the pace of the rune map
 * (when each ring opens, and what the later rings cost) can be tuned from the Firebase console without an app update.
 *
 * Every field is optional and kept within a sane range; a missing document or field leaves the built-in default.
 * Fields: ring2Level, ring3Level, ring4Level (player level), percentNeeded (0-100), priceFactor (0.25-4.0).
 */
@Singleton
class RuneConfigRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var listening = false

    @Synchronized
    fun startListening() {
        if (listening) return
        listening = true
        scope.launch {
            firestoreFlow<Map<String, Any?>?>("runeConfig") { emit, onError ->
                firestore.document("config/runes").addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        onError(error)
                        return@addSnapshotListener
                    }
                    emit(snapshot?.data)
                }
            }.catch { }.collect { data -> apply(data) }
        }
    }

    private fun apply(data: Map<String, Any?>?) {
        fun int(key: String, range: IntRange, fallback: Int): Int = (data?.get(key) as? Number)?.toInt()?.takeIf { it in range } ?: fallback
        RuneGates.ring2Level = int("ring2Level", 1..100, 15)
        RuneGates.ring3Level = int("ring3Level", 1..100, 30)
        RuneGates.ring4Level = int("ring4Level", 1..100, 40)
        RuneGates.percentNeeded = int("percentNeeded", 0..100, 70)
        RuneGates.priceFactor = (data?.get("priceFactor") as? Number)?.toFloat()?.takeIf { it in 0.25f..4.0f } ?: 1.0f
    }
}
