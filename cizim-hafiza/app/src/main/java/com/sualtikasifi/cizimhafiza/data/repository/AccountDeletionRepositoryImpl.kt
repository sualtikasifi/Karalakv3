package com.sualtikasifi.cizimhafiza.data.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.sualtikasifi.cizimhafiza.data.local.AppDatabase
import com.sualtikasifi.cizimhafiza.domain.repository.AccountDeletionRepository
import com.sualtikasifi.cizimhafiza.util.NonFatal
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

/**
 * See AccountDeletionRepository for why this exists.
 *
 * Deletion happens in two places. First the account files deletionRequests/{uid}: the daily maintenance run
 * (functions/src/maintenance.ts, runProcessDeletions) then removes everything the account left anywhere — including
 * what no client is allowed to touch (the cloud save, moderation records, entries in other players' lists) — and the
 * sign-in itself. Filing it is the one step that must succeed: without it the request was not recorded.
 *
 * Then, so the player does not wait a day to disappear, this device removes everything the rules let it remove right
 * away. That part is best-effort: whatever it misses, the server run catches.
 */
@Singleton
class AccountDeletionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val database: AppDatabase
) : AccountDeletionRepository {

    override suspend fun deleteAccountAndData(): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("No signed-in user"))
        val uid = user.uid

        // 1) The request. Bounded, because the Task resolves on the server's acknowledgement and offline that never
        //    comes: a deletion that cannot be recorded is reported as failed, so the player knows to try again online.
        try {
            withTimeout(REQUEST_TIMEOUT_MS) {
                firestore.collection("deletionRequests").document(uid)
                    .set(mapOf("requestedAt" to FieldValue.serverTimestamp(), "platform" to "android"))
                    .await()
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }

        // 2) What this device can remove now. Failures are swallowed on purpose: the server run removes the rest.
        runCatching { deleteRemoteData(uid) }.onFailure { NonFatal.report(TAG, "client-side cleanup incomplete", it) }

        // 3) The sign-in. Firebase refuses to delete a user whose sign-in is old; that no longer needs the player to
        //    sign in again: the request is already filed and the server deletes the sign-in as well. Signing out here
        //    makes sure this device lets go of the account either way.
        try {
            user.delete().await()
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            auth.signOut()
        } catch (e: Exception) {
            NonFatal.report(TAG, "auth user not deleted on the device; the server run will", e)
            auth.signOut()
        }

        // 4) Local, unconditionally and last: by this point there is no account left for the local data to belong to.
        runCatching { database.clearAllTables() }
        runCatching {
            LOCAL_PREFS.forEach { name ->
                context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()
            }
        }
        return Result.success(Unit)
    }

    private suspend fun deleteRemoteData(uid: String) {
        val userDoc = firestore.collection("users").document(uid)

        // This account's entry in each friend's own list goes first, while the list naming them is still readable.
        runCatching {
            userDoc.collection("friends").get().await().documents.forEach { friend ->
                runCatching {
                    firestore.collection("users").document(friend.id).collection("friends").document(uid).delete().await()
                }
            }
        }
        USER_SUBCOLLECTIONS.forEach { name ->
            runCatching {
                userDoc.collection(name).get().await().documents.forEach { doc ->
                    runCatching { doc.reference.delete().await() }
                }
            }
        }
        runCatching {
            val code = userDoc.get().await().getString("friendCode")
            if (code != null) firestore.collection("friendCodes").document(code).delete().await()
        }
        // A name and the account's marker pointing at it are released together (see firestore.rules, usernameOwners).
        runCatching {
            val names = firestore.collection("usernames").whereEqualTo("uid", uid).get().await().documents
            val batch = firestore.batch()
            names.forEach { batch.delete(it.reference) }
            batch.delete(firestore.collection("usernameOwners").document(uid))
            batch.commit().await()
        }
        // Recorded rounds live in flat top-level collections, not under users/{uid}, and they carry this player's
        // nickname and drawings. Each round's drawings are a sibling document sharing its id.
        listOf("ghostRuns" to "ghostRunItems", "pendingRuns" to "pendingRunItems").forEach { (runs, items) ->
            runCatching {
                firestore.collection(runs).whereEqualTo("uid", uid).get().await().documents.forEach { run ->
                    runCatching { run.reference.delete().await() }
                    runCatching { firestore.collection(items).document(run.id).delete().await() }
                }
            }
        }
        listOf("pendingDailyChallenges", "bugReports").forEach { collection ->
            runCatching {
                firestore.collection(collection).whereEqualTo("uid", uid).get().await().documents.forEach { doc ->
                    runCatching { doc.reference.delete().await() }
                }
            }
        }
        listOf("challengerUid", "opponentUid").forEach { field ->
            runCatching {
                firestore.collection("duels").whereEqualTo(field, uid).get().await().documents.forEach { doc ->
                    runCatching { doc.reference.delete().await() }
                }
            }
        }
        runCatching { userDoc.delete().await() }
    }

    private companion object {
        const val TAG = "AccountDeletion"
        const val REQUEST_TIMEOUT_MS = 15_000L

        /** Everything under users/{uid} a client may delete. The cloud save (backup) is the server run's. */
        val USER_SUBCOLLECTIONS = listOf(
            "friends", "friendRequests", "invites", "blockedUsers", "inviteCooldowns", "private"
        )

        /** Every preference file the app writes. A new one belongs here too. */
        val LOCAL_PREFS = listOf(
            "cizim_hafiza_settings",
            "karalak_backup",
            "ad_manager_prefs",
            "karalak_daily_challenge",
            "duel_history",
            "friend_request_limit",
            "guess_layout",
            "one_time_flags",
            "admin_stats",
            "brand_splash",
            "config_doc_fetch",
            "console_ai",
            "voice_settings",
            com.sualtikasifi.cizimhafiza.util.LegalConsentStore.PREFS_NAME
        )
    }
}
