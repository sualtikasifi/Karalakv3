package com.sualtikasifi.cizimhafiza.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * "Try a pen for one game" (a rewarded ad in the store): the rules, pure so they can be tested.
 * At most [MAX_PER_DAY] trials a day, and a given pen once a day (Turkey's day, see TurkeyTime).
 */
object PenTrialRules {
    const val MAX_PER_DAY = 3
    /** An armed trial that was never played is forgotten after this, so a pen picked on Monday does not surprise anyone on Friday. */
    const val EXPIRES_MILLIS = 24 * 60 * 60_000L

    /** [usedToday] are the pens already tried on [storedDay]; [storedCount] counts those trials. */
    fun canTry(pen: String, storedDay: Long, storedCount: Int, usedToday: Set<String>, today: Long): Boolean {
        if (storedDay != today) return true
        return storedCount < MAX_PER_DAY && pen !in usedToday
    }

    fun isLive(armedAt: Long, now: Long): Boolean = armedAt in 1..now && now - armedAt < EXPIRES_MILLIS
}

/** The pen armed for the next game, and what was tried today. */
@Singleton
class PenTrialStore @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("pen_trial", Context.MODE_PRIVATE)
    private val _armed = MutableStateFlow(readArmed())

    /** Name of the pen to draw the next game with, or null. */
    val armed: StateFlow<String?> = _armed.asStateFlow()

    private fun readArmed(): String? {
        val pen = prefs.getString(KEY_PEN, null) ?: return null
        return pen.takeIf { PenTrialRules.isLive(prefs.getLong(KEY_ARMED_AT, 0L), System.currentTimeMillis()) }
    }

    private fun usedToday(today: Long): Set<String> =
        if (prefs.getLong(KEY_DAY, -1L) != today) emptySet() else prefs.getString(KEY_USED, "").orEmpty().split(',').filter { it.isNotBlank() }.toSet()

    fun canTry(pen: String, today: Long = TurkeyTime.today().toEpochDay()): Boolean =
        PenTrialRules.canTry(pen, prefs.getLong(KEY_DAY, -1L), prefs.getInt(KEY_COUNT, 0), usedToday(today), today)

    /** An earned ad: [pen] is the next game's pen, and counts as tried today. */
    @Synchronized
    fun arm(pen: String, now: Long = System.currentTimeMillis(), today: Long = TurkeyTime.today().toEpochDay()) {
        val used = usedToday(today) + pen
        prefs.edit().putString(KEY_PEN, pen).putLong(KEY_ARMED_AT, now).putLong(KEY_DAY, today).putInt(KEY_COUNT, used.size).putString(KEY_USED, used.joinToString(",")).apply()
        _armed.value = pen
    }

    /** A different account took over this phone: the previous one's armed pen and tried-today list are not the next one's. */
    @Synchronized
    fun clear() {
        prefs.edit().clear().apply()
        _armed.value = null
    }

    /** The game that used it is over: the pen goes back to the player's own. */
    @Synchronized
    fun consume() {
        if (_armed.value == null) return
        prefs.edit().remove(KEY_PEN).remove(KEY_ARMED_AT).apply()
        _armed.value = null
    }

    private companion object {
        const val KEY_PEN = "armed_pen"
        const val KEY_ARMED_AT = "armed_at"
        const val KEY_DAY = "day"
        const val KEY_COUNT = "count"
        const val KEY_USED = "used"
    }
}
