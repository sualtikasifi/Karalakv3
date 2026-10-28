package com.sualtikasifi.cizimhafiza.util

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The player's answers to the first-run legal prompt (see presentation/common/LegalConsentHost): their year of birth
 * (a neutral age screen — Play's Families policy for an app that may appeal to children) and which version of the terms
 * of use they accepted. Kept on this device only; the birth year is never uploaded.
 *
 * What the age changes, all of it promised in the privacy policy (hosting/gizlilik):
 *  - under 13 ([childMode]): online and sharing features stay closed (ChildGate), no drawing is uploaded, no league score
 *    is published, Analytics collects nothing, and ads are child-directed (TFCD, rated G, never personalised);
 *  - under 16 ([underConsentAge]): ads are never personalised (TFUA, and UMP is told the player is under the age of consent).
 *
 * Only a year is asked, so the age is known to within a year. Both checks take the young end of that range: someone born
 * in 2013 is treated as possibly 12 for all of 2026.
 */
@Singleton
class LegalConsentStore @Inject constructor(@ApplicationContext private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _birthYear = MutableStateFlow(prefs.getInt(KEY_BIRTH_YEAR, 0))
    private val _termsVersion = MutableStateFlow(prefs.getInt(KEY_TERMS_VERSION, 0))
    private val _childMode = MutableStateFlow(isChild(_birthYear.value))

    /** True until the player has given a birth year and accepted the current terms. */
    val needsPrompt: Boolean
        get() = _birthYear.value == 0 || _termsVersion.value < TERMS_VERSION

    private val _needsPromptFlow = MutableStateFlow(needsPrompt)
    val needsPromptFlow: StateFlow<Boolean> = _needsPromptFlow.asStateFlow()

    /** The player may be under 13. */
    val childMode: StateFlow<Boolean> = _childMode.asStateFlow()

    /** The player may be under 16, the highest age of digital consent in the EU. */
    val underConsentAge: Boolean
        get() = youngestPossibleAge(_birthYear.value)?.let { it < CONSENT_AGE } ?: false

    /** Records the answers. [birthYear] has already been checked with [isValidBirthYear]. */
    fun accept(birthYear: Int) {
        prefs.edit()
            .putInt(KEY_BIRTH_YEAR, birthYear)
            .putInt(KEY_TERMS_VERSION, TERMS_VERSION)
            .apply()
        _birthYear.value = birthYear
        _termsVersion.value = TERMS_VERSION
        _childMode.value = isChild(birthYear)
        _needsPromptFlow.value = needsPrompt
        applyAnalyticsCollection()
    }

    /** Analytics is off for a child; on otherwise (Consent Mode still decides what it may store, see ConsentManager). */
    fun applyAnalyticsCollection() {
        runCatching { FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(!_childMode.value) }
    }

    private fun isChild(birthYear: Int): Boolean = youngestPossibleAge(birthYear)?.let { it < MIN_AGE } ?: false

    companion object {
        const val PREFS_NAME = "legal_consent"
        private const val KEY_BIRTH_YEAR = "birth_year"
        private const val KEY_TERMS_VERSION = "terms_version"

        /** Raise when the terms of use change enough to be accepted again (hosting/kosullar). */
        const val TERMS_VERSION = 1
        const val MIN_AGE = 13
        const val CONSENT_AGE = 16
        private const val OLDEST_AGE = 110

        private fun currentYear(): Int = java.time.Year.now(java.time.ZoneId.of("Europe/Istanbul")).value

        /** The youngest the player can be this year (their birthday may still be ahead), or null before they answered. */
        fun youngestPossibleAge(birthYear: Int, year: Int = currentYear()): Int? =
            if (birthYear <= 0) null else year - birthYear - 1

        fun isValidBirthYear(birthYear: Int, year: Int = currentYear()): Boolean =
            birthYear in (year - OLDEST_AGE)..year
    }
}
