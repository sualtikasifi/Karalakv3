package com.sualtikasifi.cizimhafiza.util

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.Period
import javax.inject.Inject
import javax.inject.Singleton

/** A date of birth as the first-run prompt takes it. [month] and [day] are 0 for an answer given as a year only. */
data class BirthDate(val year: Int, val month: Int = 0, val day: Int = 0)

/**
 * The player's answers to the first-run legal prompt (see presentation/common/LegalConsentHost): their date of birth
 * (a neutral age screen — Play's Families policy for an app that may appeal to children) and which version of the terms
 * of use they accepted. Kept on this device; once the player links Google, the date is also saved on their account and
 * from then on the account's date is the one that counts (AccountAgeSync).
 *
 * What the age changes, all of it promised in the privacy policy (hosting/gizlilik):
 *  - under 13 ([childMode]): rooms, friends and duels stay closed (ChildGate); Quick Match only ever brings approved
 *    recordings and the AI; the league ranks them only once linked to Google, under the generated name and without a
 *    photo; no drawing is uploaded, Analytics collects nothing, and ads are child-directed (TFCD, rated G, never
 *    personalised);
 *  - under 16 ([underConsentAge]): ads are never personalised (TFUA, and UMP is told the player is under the age of consent).
 *
 * The full date is asked, so the age is exact and changes on the birthday. An answer given as a year only (the prompt
 * asked for just the year before) takes the young end of that year: someone born in 2013 is possibly 12 for all of 2026.
 */
@Singleton
class LegalConsentStore @Inject constructor(@ApplicationContext private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _birthDate = MutableStateFlow(
        BirthDate(prefs.getInt(KEY_BIRTH_YEAR, 0), prefs.getInt(KEY_BIRTH_MONTH, 0), prefs.getInt(KEY_BIRTH_DAY, 0))
    )
    private val _termsVersion = MutableStateFlow(prefs.getInt(KEY_TERMS_VERSION, 0))
    private val _childMode = MutableStateFlow(isChild(_birthDate.value))

    /** True until the player has given a date of birth and accepted the current terms. */
    val needsPrompt: Boolean
        get() = _birthDate.value.year == 0 || _termsVersion.value < TERMS_VERSION

    private val _needsPromptFlow = MutableStateFlow(needsPrompt)
    val needsPromptFlow: StateFlow<Boolean> = _needsPromptFlow.asStateFlow()

    /** The player may be under 13. */
    val childMode: StateFlow<Boolean> = _childMode.asStateFlow()

    /** The player may be under 16, the highest age of digital consent in the EU. */
    val underConsentAge: Boolean
        get() = ageOf(_birthDate.value)?.let { it < CONSENT_AGE } ?: false

    /** The date of birth this device goes by; its year is 0 before the question was answered. */
    val birthDate: BirthDate get() = _birthDate.value

    /**
     * Goes by the date saved on the player's Google account instead of the one this device was told (AccountAgeSync):
     * the account's answer wins, so a reinstall cannot change it. The accepted terms are left as they are.
     */
    fun adoptAccountDate(date: BirthDate) {
        if (!isValidBirthDate(date) || date == _birthDate.value) return
        save(date)
        _needsPromptFlow.value = needsPrompt
        applyAnalyticsCollection()
    }

    /** Records the answers. [date] has already been checked with [isValidBirthDate]. */
    fun accept(date: BirthDate) {
        prefs.edit().putInt(KEY_TERMS_VERSION, TERMS_VERSION).apply()
        _termsVersion.value = TERMS_VERSION
        save(date)
        _needsPromptFlow.value = needsPrompt
        applyAnalyticsCollection()
    }

    private fun save(date: BirthDate) {
        prefs.edit()
            .putInt(KEY_BIRTH_YEAR, date.year)
            .putInt(KEY_BIRTH_MONTH, date.month)
            .putInt(KEY_BIRTH_DAY, date.day)
            .apply()
        _birthDate.value = date
        _childMode.value = isChild(date)
    }

    /** Analytics is off for a child; on otherwise (Consent Mode still decides what it may store, see ConsentManager). */
    fun applyAnalyticsCollection() {
        runCatching { FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(!_childMode.value) }
    }

    private fun isChild(date: BirthDate): Boolean = ageOf(date)?.let { it < MIN_AGE } ?: false

    companion object {
        const val PREFS_NAME = "legal_consent"
        private const val KEY_BIRTH_YEAR = "birth_year"
        private const val KEY_BIRTH_MONTH = "birth_month"
        private const val KEY_BIRTH_DAY = "birth_day"
        private const val KEY_TERMS_VERSION = "terms_version"

        /** Raise when the terms of use change enough to be accepted again (hosting/kosullar). */
        const val TERMS_VERSION = 1
        const val MIN_AGE = 13
        const val CONSENT_AGE = 16
        private const val OLDEST_AGE = 110

        private fun today(): LocalDate = LocalDate.now(java.time.ZoneId.of("Europe/Istanbul"))

        /** The player's age today, or null before they answered. A year-only answer gives the youngest they can be. */
        fun ageOf(date: BirthDate, today: LocalDate = today()): Int? = when {
            date.year <= 0 -> null
            date.month == 0 || date.day == 0 -> today.year - date.year - 1
            else -> runCatching { Period.between(LocalDate.of(date.year, date.month, date.day), today).years }.getOrNull()
        }

        /** A real date (or a year only), not in the future and not more than [OLDEST_AGE] years ago. */
        fun isValidBirthDate(date: BirthDate, today: LocalDate = today()): Boolean {
            if (date.year !in (today.year - OLDEST_AGE)..today.year) return false
            if (date.month == 0 && date.day == 0) return true
            val day = runCatching { LocalDate.of(date.year, date.month, date.day) }.getOrNull() ?: return false
            return !day.isAfter(today)
        }
    }
}
