package com.sualtikasifi.cizimhafiza.util

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.sualtikasifi.cizimhafiza.domain.repository.AuthRepository
import kotlinx.coroutines.tasks.await
import java.text.Normalizer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

sealed interface UsernameClaimResult {
    data object Success : UsernameClaimResult
    data object Invalid : UsernameClaimResult
    data object Taken : UsernameClaimResult
    data object AlreadyLocked : UsernameClaimResult
    data object NetworkError : UsernameClaimResult
}

/**
 * Globally unique usernames in Firestore: usernames/{foldedName} → { uid, name, createdAt }.
 *
 * Life of a name:
 *  1. A new player is given "Karalak" + a number that nobody else has ([ensureUsername]) — no
 *     prompt, no friction on first launch.
 *  2. Before linking Google they may change it once ([change] with final = false).
 *  3. When they link Google they are asked to keep it or change it one last time, and then it is
 *     locked for good ([lockCurrent] / [change] with final = true).
 *
 * The document id is the case/diacritic-folded name and the rules forbid updating an existing
 * document, so two players can never own the same name. The lock and the change counter live on
 * users/{uid} (`usernameLocked`, `usernameChanges`), which is what lets a fresh install or another
 * device restore the same state. A claim made before this flow existed carries no such field and
 * is treated as already final.
 */
@Singleton
class UsernameRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository
) {
    private val usernames get() = firestore.collection("usernames")
    private val users get() = firestore.collection("users")

    /**
     * Makes sure this account has a username: restores the one the server already has, or hands out
     * a new one. Safe to call on every launch; does nothing once the name is locked, and does
     * nothing (retries next launch) while offline.
     */
    suspend fun ensureUsername() {
        if (settingsRepository.nicknameRenameUsed.value) return
        try {
            val uid = authRepository.ensureSignedIn()
            val owned = usernames.whereEqualTo("uid", uid).limit(1).get().await().documents.firstOrNull()
            val ownedName = owned?.getString("name")
            if (!ownedName.isNullOrBlank()) {
                val profile = users.document(uid).get().await()
                settingsRepository.setNickname(ownedName)
                settingsRepository.usernameChanges = (profile.getLong("usernameChanges") ?: 0L).toInt()
                // Only an explicit `false` means "still changeable"; older claims are final.
                if (profile.getBoolean("usernameLocked") != false) settingsRepository.lockUsername(ownedName)
                return
            }

            // A name the player typed in an older version gets first refusal.
            val legacy = settingsRepository.nickname.value.trim().replace(Regex("\\s+"), " ")
            if (settingsRepository.hasChosenNickname && isValid(legacy) && !legacy.startsWith("Karalak", ignoreCase = true) &&
                claimNew(uid, legacy)
            ) return

            repeat(12) { attempt ->
                // The range widens with every collision, so a busy namespace never runs dry.
                val upper = 1_000 * (attempt + 1) * 10
                if (claimNew(uid, "Karalak" + Random.nextInt(100, upper))) return
            }
        } catch (e: Exception) {
            Log.w(TAG, "ensureUsername failed", e)
        }
    }

    /** Reserves [name] for [uid] if it is free; on success stores it as the current, still-changeable name. */
    private suspend fun claimNew(uid: String, name: String): Boolean {
        val ref = usernames.document(keyOf(name))
        val taken = firestore.runTransaction { tx ->
            val existing = tx.get(ref)
            if (existing.exists() && existing.getString("uid") != uid) {
                true
            } else {
                if (!existing.exists()) {
                    tx.set(ref, mapOf("uid" to uid, "name" to name, "createdAt" to FieldValue.serverTimestamp()))
                }
                false
            }
        }.await()
        if (taken) return false
        settingsRepository.setNickname(name)
        settingsRepository.usernameChanges = 0
        publish(uid, name, changes = 0, locked = false)
        return true
    }

    /**
     * Renames the account. [final] = true is the one made when linking Google: it also locks the
     * name. Without it, only one change is allowed before that. The old name is released.
     */
    suspend fun change(rawName: String, final: Boolean): UsernameClaimResult {
        if (settingsRepository.nicknameRenameUsed.value) return UsernameClaimResult.AlreadyLocked
        if (!final && settingsRepository.usernameChanges >= 1) return UsernameClaimResult.AlreadyLocked
        val name = rawName.trim().replace(Regex("\\s+"), " ")
        if (!isValid(name)) return UsernameClaimResult.Invalid
        val current = settingsRepository.nickname.value.trim()
        if (name == current) return if (final) lockCurrent() else UsernameClaimResult.Success
        return try {
            val uid = authRepository.ensureSignedIn()
            val newRef = usernames.document(keyOf(name))
            val oldRef = usernames.document(keyOf(current))
            val taken = firestore.runTransaction { tx ->
                val newSnap = tx.get(newRef)
                val oldSnap = if (current.isNotEmpty() && oldRef.id != newRef.id) tx.get(oldRef) else null
                if (newSnap.exists() && newSnap.getString("uid") != uid) {
                    true
                } else {
                    if (!newSnap.exists()) {
                        tx.set(newRef, mapOf("uid" to uid, "name" to name, "createdAt" to FieldValue.serverTimestamp()))
                    }
                    if (oldSnap != null && oldSnap.exists() && oldSnap.getString("uid") == uid) tx.delete(oldRef)
                    false
                }
            }.await()
            if (taken) return UsernameClaimResult.Taken
            val changes = settingsRepository.usernameChanges + 1
            settingsRepository.setNickname(name)
            settingsRepository.usernameChanges = changes
            if (final) settingsRepository.lockUsername(name)
            publish(uid, name, changes = changes, locked = final)
            UsernameClaimResult.Success
        } catch (e: Exception) {
            Log.w(TAG, "change failed", e)
            UsernameClaimResult.NetworkError
        }
    }

    /** Keeps the current name and makes it permanent. */
    suspend fun lockCurrent(): UsernameClaimResult {
        val name = settingsRepository.nickname.value.trim()
        if (name.isEmpty()) return UsernameClaimResult.Invalid
        return try {
            val uid = authRepository.ensureSignedIn()
            users.document(uid).set(mapOf("usernameLocked" to true), SetOptions.merge()).await()
            settingsRepository.lockUsername(name)
            UsernameClaimResult.Success
        } catch (e: Exception) {
            Log.w(TAG, "lock failed", e)
            UsernameClaimResult.NetworkError
        }
    }

    private suspend fun publish(uid: String, name: String, changes: Int, locked: Boolean) {
        runCatching {
            users.document(uid).set(
                mapOf("nickname" to name, "username" to name, "usernameChanges" to changes, "usernameLocked" to locked),
                SetOptions.merge()
            ).await()
        }
        runCatching { authRepository.updateDisplayName(name) }
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
