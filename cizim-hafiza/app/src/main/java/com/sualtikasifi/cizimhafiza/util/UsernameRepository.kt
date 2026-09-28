package com.sualtikasifi.cizimhafiza.util

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.sualtikasifi.cizimhafiza.domain.repository.AuthRepository
import com.sualtikasifi.cizimhafiza.domain.repository.FriendRepository
import kotlinx.coroutines.tasks.await
import java.text.Normalizer
import javax.inject.Inject
import javax.inject.Singleton

sealed interface UsernameClaimResult {
    data object Success : UsernameClaimResult
    data object Invalid : UsernameClaimResult
    data object Taken : UsernameClaimResult
    data object AlreadyLocked : UsernameClaimResult
    data object NetworkError : UsernameClaimResult
}

/**
 * Globally unique, one-time usernames stored in Firestore under
 * usernames/{normalizedName} → { uid, name, createdAt }.
 *
 * The document id is the case/diacritic-folded name, so "Ali", "ali" and
 * "ALİ" are one name. Claiming runs in a transaction and the rules forbid
 * updating an existing document, so two players can never end up owning the
 * same name. The claim is keyed by uid; Google sign-in links onto that same
 * uid (see AuthRepositoryImpl), which is what binds the account to the name,
 * and [syncFromServer] restores it on a fresh install or another device.
 */
@Singleton
class UsernameRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository,
    private val friendRepository: FriendRepository,
    private val settingsRepository: SettingsRepository
) {
    private val usernames get() = firestore.collection("usernames")
    private val users get() = firestore.collection("users")

    suspend fun claim(rawName: String): UsernameClaimResult {
        if (settingsRepository.nicknameRenameUsed.value) return UsernameClaimResult.AlreadyLocked
        val name = rawName.trim().replace(Regex("\\s+"), " ")
        if (!isValid(name)) return UsernameClaimResult.Invalid
        val key = keyOf(name)
        return try {
            val uid = authRepository.ensureSignedIn()
            val ref = usernames.document(key)
            val result = firestore.runTransaction { tx ->
                val existing = tx.get(ref)
                if (existing.exists()) {
                    if (existing.getString("uid") == uid) UsernameClaimResult.Success else UsernameClaimResult.Taken
                } else {
                    tx.set(ref, mapOf("uid" to uid, "name" to name, "createdAt" to FieldValue.serverTimestamp()))
                    UsernameClaimResult.Success
                }
            }.await()
            if (result == UsernameClaimResult.Success) {
                settingsRepository.lockUsername(name)
                runCatching { users.document(uid).set(mapOf("nickname" to name, "username" to name), SetOptions.merge()).await() }
                runCatching { authRepository.updateDisplayName(name) }
            }
            result
        } catch (e: Exception) {
            Log.w(TAG, "claim failed", e)
            UsernameClaimResult.NetworkError
        }
    }

    /**
     * Adopts the username this uid already owns, if any. Returns true once a
     * definitive answer is known (owned or provably not), false if offline.
     */
    suspend fun syncFromServer(): Boolean {
        if (settingsRepository.nicknameRenameUsed.value) return true
        return try {
            val uid = authRepository.ensureSignedIn()
            val snap = usernames.whereEqualTo("uid", uid).limit(1).get().await()
            val name = snap.documents.firstOrNull()?.getString("name")
            if (!name.isNullOrBlank()) settingsRepository.lockUsername(name)
            true
        } catch (e: Exception) {
            Log.w(TAG, "sync failed", e)
            false
        }
    }

    companion object {
        private const val TAG = "UsernameRepository"

        fun isValid(name: String): Boolean =
            name.length in 2..16 &&
                name.all { it.isLetterOrDigit() || it == '_' || it == '.' || it == '-' || it == ' ' } &&
                name.any { it.isLetterOrDigit() }

        fun keyOf(name: String): String =
            Normalizer.normalize(name.trim(), Normalizer.Form.NFD)
                .replace(Regex("\\p{M}+"), "")
                .replace('ı', 'i').replace('İ', 'i')
                .lowercase(java.util.Locale.ROOT)
                .replace(' ', '_')
    }
}
