package com.sualtikasifi.cizimhafiza.util

import com.google.firebase.auth.FirebaseAuth
import com.sualtikasifi.cizimhafiza.domain.repository.AuthRepository
import com.sualtikasifi.cizimhafiza.domain.repository.FriendRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the player's "Videolarda öne çık" choice on their public profile (users/{uid}.featureConsent), where the
 * developer panel's video studio looks for players who agreed.
 *
 * Follows the account as well as the switch: after signing into another account the choice is written to that
 * account's profile, not left on the old one. A profile that never said yes is never written to — no field means no —
 * so the switch costs nothing for the players who leave it off. A failed write is retried at the next launch, when the
 * flows below emit again.
 */
@Singleton
class FeatureConsentPublisher @Inject constructor(
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
    private val friendRepository: FriendRepository,
    private val auth: FirebaseAuth
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var started = false

    /** Safe to call repeatedly; only the first call subscribes. */
    fun start() {
        if (started) return
        started = true
        scope.launch {
            combine(authRepository.authState, settingsRepository.featureConsent) { _, consent -> consent }
                .collect { consent -> publish(consent) }
        }
    }

    private suspend fun publish(consent: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        if (!needsWrite(uid, consent, settingsRepository.publishedFeatureConsent)) return
        runCatching { friendRepository.publishFeatureConsent(consent) }
            .onSuccess { settingsRepository.publishedFeatureConsent = signature(uid, consent) }
            .onFailure { NonFatal.report(TAG, "Feature consent not published", it) }
    }

    internal companion object {
        private const val TAG = "FeatureConsentPublisher"

        fun signature(uid: String, consent: Boolean) = "$uid|$consent"

        /** A yes not yet on this profile, or a no on a profile this device once said yes on; nothing else is written. */
        fun needsWrite(uid: String, consent: Boolean, published: String?): Boolean = when {
            published == signature(uid, consent) -> false
            consent -> true
            else -> published == signature(uid, true)
        }
    }
}
