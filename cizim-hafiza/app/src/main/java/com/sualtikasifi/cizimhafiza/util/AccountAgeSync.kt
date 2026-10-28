package com.sualtikasifi.cizimhafiza.util

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.sualtikasifi.cizimhafiza.ads.AdManager
import com.sualtikasifi.cizimhafiza.ads.ConsentManager
import com.sualtikasifi.cizimhafiza.domain.repository.AuthRepository
import com.sualtikasifi.cizimhafiza.domain.repository.AuthState
import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Binds the player's date of birth to their Google account, so the age rules follow the account to a reinstall or a
 * new phone instead of being asked again from scratch.
 *
 * The first time an account is linked, the date this device was given is saved on it (users/{uid}/private/age). From
 * then on the account's date is the one that counts: a device that was told something else takes the account's date
 * (LegalConsentStore.adoptAccountDate), and the ads and Analytics settings follow at once. The rules keep the saved
 * date from being rewritten while it says the player may be under 18 (firestore.rules). A different Google account
 * starts again from whatever its device is told.
 *
 * Runs on every launch for a linked account, so an account linked before this existed gets its date saved too.
 */
@Singleton
class AccountAgeSync @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val authRepository: AuthRepository,
    private val legalConsentStore: LegalConsentStore,
    private val adManager: Lazy<AdManager>,
    private val consentManager: Lazy<ConsentManager>
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var started = false

    /** Safe to call repeatedly; only the first call subscribes. */
    fun start() {
        if (started) return
        started = true
        scope.launch {
            // Only once this device has an answer of its own (the first-run question), which an account without a
            // saved year then takes.
            combine(authRepository.authState, legalConsentStore.needsPromptFlow) { authState, needsPrompt ->
                if (authState is AuthState.Linked && !needsPrompt) auth.currentUser?.uid else null
            }
                .filterNotNull()
                .distinctUntilChanged()
                .collect { uid -> runCatching { sync(uid) }.onFailure { NonFatal.report(TAG, "Age not synced", it) } }
        }
    }

    private suspend fun sync(uid: String) {
        val ref = firestore.collection("users").document(uid).collection("private").document("age")
        val doc = ref.get().await()
        val local = legalConsentStore.birthDate
        val savedYear = doc.getLong("birthYear")?.toInt()
        if (savedYear == null) {
            if (local.year <= 0) return
            val fields = mutableMapOf<String, Any>("birthYear" to local.year.toLong(), "savedAt" to FieldValue.serverTimestamp())
            if (local.month > 0 && local.day > 0) {
                fields["birthMonth"] = local.month.toLong()
                fields["birthDay"] = local.day.toLong()
            }
            ref.set(fields).await()
            return
        }
        val saved = BirthDate(savedYear, doc.getLong("birthMonth")?.toInt() ?: 0, doc.getLong("birthDay")?.toInt() ?: 0)
        // A year-only answer saved earlier says no more than this device's full date of the same year.
        if (saved == local || (saved.month == 0 && saved.year == local.year)) return
        val wasChild = legalConsentStore.childMode.value
        val wasUnderConsentAge = legalConsentStore.underConsentAge
        legalConsentStore.adoptAccountDate(saved)
        if (legalConsentStore.childMode.value != wasChild || legalConsentStore.underConsentAge != wasUnderConsentAge) {
            consentManager.get().applyAnalyticsConsent()
            adManager.get().applyAudience()
        }
    }

    private companion object {
        const val TAG = "AccountAgeSync"
    }
}
