package com.sualtikasifi.cizimhafiza.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.sualtikasifi.cizimhafiza.domain.repository.ServerClock
import com.sualtikasifi.cizimhafiza.util.TurkeyTime
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import javax.inject.Inject

/**
 * Asks Firestore what time it is: writes a server timestamp to users/{uid}/private/loginClock and reads it back from
 * the server. The write is the owner's own document under private/ (see firestore.rules), so no new rule is needed.
 * One write and one read, and only on the days the reward has not been taken yet.
 */
class ServerClockImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ServerClock {

    override suspend fun todayEpochDay(): Long? = withTimeoutOrNull(TIMEOUT_MS) {
        runCatching {
            val uid = auth.currentUser?.uid ?: auth.signInAnonymously().await().user?.uid ?: return@runCatching null
            val ref = firestore.collection("users").document(uid).collection("private").document("loginClock")
            ref.set(mapOf("at" to FieldValue.serverTimestamp())).await()
            val millis = ref.get(Source.SERVER).await().getTimestamp("at")?.toDate()?.time ?: return@runCatching null
            Instant.ofEpochMilli(millis).atZone(TurkeyTime.zone).toLocalDate().toEpochDay()
        }.getOrNull()
    }

    private companion object {
        const val TIMEOUT_MS = 8_000L
    }
}
