package com.sualtikasifi.cizimhafiza.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.sualtikasifi.cizimhafiza.BuildConfig
import com.sualtikasifi.cizimhafiza.util.GameConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gathers advertising consent through Google's User Messaging Platform
 * before any ad is requested.
 *
 * This is not optional polish. AdMob's own policy requires a certified CMP
 * for traffic from the EEA, the UK and Switzerland, and serving personalised
 * ads there without a lawful basis is a GDPR breach — so an app that calls
 * `MobileAds.initialize` and starts requesting ads, as this one used to, is
 * non-compliant from the first European install. UMP also covers the
 * separate US state privacy signals AdMob applies through the same form.
 *
 * The flow is deliberately quiet for players outside a region that requires
 * a form: [ensureConsent] resolves immediately and no dialog is ever shown.
 *
 * **One-time console setup** (nothing here can substitute for it): in the
 * AdMob console open Privacy & messaging → GDPR (and → US state regulations),
 * create and publish a message for this app. Until a message is published,
 * `requestConsentInfoUpdate` reports that no form is available and the app
 * proceeds without one.
 */
@Singleton
class ConsentManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val legalConsentStore: com.sualtikasifi.cizimhafiza.util.LegalConsentStore
) {

    private val consentInformation: ConsentInformation by lazy {
        UserMessagingPlatform.getConsentInformation(context)
    }

    /**
     * True once UMP says ad requests are permitted. Checked before
     * initialising the ads SDK — see [AdManager.initializeIfEnabled].
     */
    val canRequestAds: Boolean
        get() = !GameConstants.ADMOB_ENABLED || consentInformation.canRequestAds()

    /**
     * Requests the current consent status and, if a form is required in this
     * player's jurisdiction, shows it. [onResolved] runs exactly once
     * afterwards — on success, on failure, and when no form is needed — so
     * the caller can go ahead and initialise ads either way. A consent
     * *failure* is not a reason to block the game: it only means no ad can
     * be requested, which [canRequestAds] already reflects.
     */
    fun ensureConsent(activity: Activity, onResolved: () -> Unit) {
        if (!GameConstants.ADMOB_ENABLED) {
            onResolved()
            return
        }

        val params = ConsentRequestParameters.Builder()
            // From the first-run year-of-birth question: below the age of digital consent no consent is asked
            // (a minor cannot give it) and ads are not personalised (see AdManager's request configuration).
            .setTagForUnderAgeOfConsent(legalConsentStore.underConsentAge)
            .apply {
                // In debug builds every device is treated as if it were in
                // the EEA, so the form can actually be exercised during
                // development instead of only ever appearing for European
                // users after release. Never enabled in a release build.
                if (BuildConfig.DEBUG) {
                    setConsentDebugSettings(
                        ConsentDebugSettings.Builder(context)
                            .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                            .build()
                    )
                }
            }
            .build()

        var settled = false
        val settle = {
            if (!settled) {
                settled = true
                onResolved()
            }
        }

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.d(TAG, "Consent form: ${formError.message}")
                    }
                    settle()
                }
            },
            { requestError ->
                // No network, or no published message yet. Ads simply stay
                // unavailable (canRequestAds reports false); the game itself
                // is unaffected.
                Log.d(TAG, "Consent info update failed: ${requestError.message}")
                settle()
            }
        )
    }

    /**
     * Google Analytics Consent Mode v2. The manifest starts every consent type as denied
     * (google_analytics_default_allow_*), so nothing is stored before this runs, right after the UMP flow resolves:
     *  - where no consent is required (outside the EEA, the UK and Switzerland), everything is granted;
     *  - where it is, each type follows the IAB TCF purposes the player agreed to in the UMP form — storage (purpose
     *    1) for analytics_storage and ad_storage, purposes 1 and 7 for ad_user_data, 3 and 4 for ad_personalization;
     *  - for a player who may be under 13, everything stays denied (Analytics collection itself is off too, see
     *    LegalConsentStore.applyAnalyticsCollection).
     * The SDK keeps the last values set, so a later launch that cannot reach UMP keeps the player's previous answer.
     */
    fun applyAnalyticsConsent() {
        runCatching {
            val status = consentInformation.consentStatus
            if (status == ConsentInformation.ConsentStatus.UNKNOWN && !legalConsentStore.childMode.value) return
            val child = legalConsentStore.childMode.value
            val required = status != ConsentInformation.ConsentStatus.NOT_REQUIRED
            // UMP writes the TCF string into the app's default preferences file, as the TCF spec requires.
            val purposes = context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
                .getString("IABTCF_PurposeConsents", "").orEmpty()
            fun purpose(n: Int) = !required || purposes.getOrNull(n - 1) == '1'
            fun state(granted: Boolean) =
                if (granted && !child) com.google.firebase.analytics.FirebaseAnalytics.ConsentStatus.GRANTED
                else com.google.firebase.analytics.FirebaseAnalytics.ConsentStatus.DENIED
            com.google.firebase.analytics.FirebaseAnalytics.getInstance(context).setConsent(
                mapOf(
                    com.google.firebase.analytics.FirebaseAnalytics.ConsentType.ANALYTICS_STORAGE to state(purpose(1)),
                    com.google.firebase.analytics.FirebaseAnalytics.ConsentType.AD_STORAGE to state(purpose(1)),
                    com.google.firebase.analytics.FirebaseAnalytics.ConsentType.AD_USER_DATA to state(purpose(1) && purpose(7)),
                    com.google.firebase.analytics.FirebaseAnalytics.ConsentType.AD_PERSONALIZATION to state(purpose(3) && purpose(4))
                )
            )
        }.onFailure { Log.d(TAG, "Analytics consent not applied: ${it.message}") }
    }

    /**
     * True when the player's jurisdiction lets them reopen the consent form
     * on demand — drives whether Settings shows a "Reklam tercihleri" row at
     * all, since offering it to someone who has no form to open would be
     * confusing.
     */
    val isPrivacyOptionsRequired: Boolean
        get() = consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    /** Reopens the consent form from Settings, so a choice can be changed later — itself a GDPR requirement. */
    fun showPrivacyOptions(activity: Activity, onDismissed: () -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            if (error != null) Log.d(TAG, "Privacy options form: ${error.message}")
            onDismissed()
        }
    }

    private companion object {
        const val TAG = "ConsentManager"
    }
}
