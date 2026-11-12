package com.sualtikasifi.cizimhafiza.ads

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The `config/ads` document: a way to switch an ad placement off, or change the interstitial limits, without a new app
 * version. Written from the Firebase console (reviewers only, see firestore.rules); every field is optional.
 *
 *  - `off`: list of placement names (the ones the call sites pass: "result_xp_x2", "interstitial_chest", ...).
 *  - `interstitialMinGapSec`, `interstitialPerHour`: override [AdGate]'s defaults.
 *
 * What was last read is kept on the phone, so a switch applies from the next start even before the document arrives, and a
 * missing document or no network leaves the built-in defaults.
 */
@Singleton
class AdRemoteConfig @Inject constructor(
    @ApplicationContext context: Context,
    private val firestore: dagger.Lazy<FirebaseFirestore>
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Volatile private var off: Set<String> = prefs.getStringSet(KEY_OFF, emptySet()) ?: emptySet()
    @Volatile private var minGapSec: Long = prefs.getLong(KEY_MIN_GAP, 0L)
    @Volatile private var perHour: Int = prefs.getInt(KEY_PER_HOUR, 0)
    @Volatile private var lastRefreshAt = 0L

    fun isOff(placement: String): Boolean = placement in off

    fun gateLimits(): AdGate.Limits = AdGate.Limits(
        minGapMillis = if (minGapSec > 0) minGapSec * 1000L else AdGate.DEFAULT_MIN_GAP_MILLIS,
        perHour = if (perHour > 0) perHour else AdGate.DEFAULT_PER_HOUR
    )

    /** Reads the document, at most once in [REFRESH_EVERY_MILLIS]. Fire and forget: a failure keeps what is stored. */
    fun refresh() {
        val now = System.currentTimeMillis()
        if (now - lastRefreshAt < REFRESH_EVERY_MILLIS) return
        lastRefreshAt = now
        runCatching {
            firestore.get().document("config/ads").get().addOnSuccessListener { doc ->
                val data = doc.data ?: emptyMap()
                val newOff = (data["off"] as? List<*>)?.filterIsInstance<String>()?.toSet() ?: emptySet()
                val newGap = (data["interstitialMinGapSec"] as? Number)?.toLong() ?: 0L
                val newPerHour = (data["interstitialPerHour"] as? Number)?.toInt() ?: 0
                off = newOff; minGapSec = newGap; perHour = newPerHour
                prefs.edit().putStringSet(KEY_OFF, newOff).putLong(KEY_MIN_GAP, newGap).putInt(KEY_PER_HOUR, newPerHour).apply()
            }
        }
    }

    private companion object {
        const val PREFS = "ad_remote_config"
        const val KEY_OFF = "off"
        const val KEY_MIN_GAP = "min_gap_sec"
        const val KEY_PER_HOUR = "per_hour"
        const val REFRESH_EVERY_MILLIS = 30 * 60_000L
    }
}
