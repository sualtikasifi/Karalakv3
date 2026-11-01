package com.sualtikasifi.cizimhafiza.util

import android.content.Context

/**
 * "When did I last do X for Y", remembered across app launches.
 *
 * The Firestore repositories used to keep these in memory, so every cold start began with an empty map and re-read
 * every friend's profile from the server — one billed read per friend per launch, whatever the time windows said.
 * Backed by a small SharedPreferences file; writes are asynchronous and best effort (a lost one only means one more
 * read next time).
 */
class PersistedClock(context: Context, name: String) {
    private val prefs = context.getSharedPreferences("clock_$name", Context.MODE_PRIVATE)

    operator fun get(key: String): Long? = prefs.getLong(key, -1L).takeIf { it >= 0L }

    operator fun set(key: String, millis: Long) {
        prefs.edit().putLong(key, millis).apply()
    }

    /** Forgets everything (an account change must not inherit the previous player's clocks). */
    fun clear() {
        prefs.edit().clear().apply()
    }
}
