package com.sualtikasifi.cizimhafiza.util

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.sualtikasifi.cizimhafiza.domain.model.PlayerReportReason
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reports another player — their name or how they behave, not a drawing (that is DrawingReportRepository). One report
 * per player per reporter (playerReports/{reportedUid}__{reporterUid}); only the reviewer reads them, in the developer
 * panel, and acts on them by hand. Play's user-generated content policy asks for exactly this wherever strangers see
 * each other's names: the league table and the room lobby.
 */
@Singleton
class PlayerReporter @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    /** [context] says where the report came from ("league", "room 123456"), at most 32 characters. */
    suspend fun report(reportedUid: String, nickname: String, reason: PlayerReportReason, context: String): Result<Unit> = runCatching {
        val me = auth.currentUser?.uid ?: error("Not signed in")
        require(reportedUid.isNotBlank() && reportedUid != me) { "Nobody to report" }
        try {
            firestore.collection(COLLECTION).document("${reportedUid}__$me").set(
                mapOf(
                    "reporterUid" to me,
                    "reportedUid" to reportedUid,
                    "nickname" to nickname.take(40),
                    "reason" to reason.name,
                    "context" to context.take(32),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: FirebaseFirestoreException) {
            // A second report of the same player lands on the first one's document, which the rules never let anyone
            // overwrite. The first one is already with the reviewer, so for the player it is sent all the same.
            if (e.code != FirebaseFirestoreException.Code.PERMISSION_DENIED) throw e
        }
    }

    private companion object {
        const val COLLECTION = "playerReports"
    }
}
