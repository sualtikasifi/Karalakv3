package com.sualtikasifi.cizimhafiza.util

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.sualtikasifi.cizimhafiza.domain.repository.FriendRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps this device's push token on the signed-in account (users/{uid}/private/device), so a duel or an invite sent to this
 * player has an address to go to.
 *
 * The token used to be written only from FirebaseMessagingService.onNewToken, which Firebase calls once, when the token is
 * first made. If the account was not there to take it (a different anonymous account signed in afterwards, or a Google account
 * was signed into later) it was never written again, and no push could ever reach that player. Nothing checked at launch.
 *
 * Now every sign-in, and every launch, looks at the account and the token: when this pair has not been saved yet, it is saved.
 * What was last saved is kept on the phone, so an ordinary launch costs no write. Not done while notifications are off in the
 * app, nor for a child account (friends and duels are closed to them; ChildGate).
 */
@Singleton
class PushTokenSync @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val friendRepository: FriendRepository,
    private val settingsRepository: SettingsRepository,
    private val legalConsentStore: LegalConsentStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs get() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private var started = false

    /** Safe to call repeatedly; only the first call subscribes. */
    fun start() {
        if (started) return
        started = true
        // Called at once with the account that is signed in now, and again whenever the account changes.
        auth.addAuthStateListener { fa ->
            val uid = fa.currentUser?.uid ?: return@addAuthStateListener
            scope.launch { runCatching { sync(uid) }.onFailure { Log.w(TAG, "Push token sync failed", it) } }
        }
    }

    private suspend fun sync(uid: String) {
        if (!settingsRepository.notificationsEnabled.value || legalConsentStore.childMode.value) return
        val token = FirebaseMessaging.getInstance().token.await()
        if (!needsSaving(prefs.getString(KEY_SAVED, null), uid, token)) return
        friendRepository.updateFcmToken(token)
        prefs.edit().putString(KEY_SAVED, savedKey(uid, token)).apply()
    }

    companion object {
        private const val TAG = "PushTokenSync"
        private const val PREFS = "push_token_sync"
        private const val KEY_SAVED = "saved"

        internal fun savedKey(uid: String, token: String) = "$uid:$token"

        /** True when the account/token pair is not the one that was last saved from this phone. */
        internal fun needsSaving(saved: String?, uid: String, token: String): Boolean = saved != savedKey(uid, token)
    }
}
