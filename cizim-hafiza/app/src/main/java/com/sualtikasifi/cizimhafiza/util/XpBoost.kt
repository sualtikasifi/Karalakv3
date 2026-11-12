package com.sualtikasifi.cizimhafiza.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The rules of "30 minutes of double XP" (a rewarded ad on the home screen), pure so they can be tested.
 *
 * It does not stack with a running XP event: the XP read by every round is the larger of the two (see XpEventRepositoryImpl),
 * and the offer is not made while an event already gives 2x. At most [MAX_PER_DAY] a day (Turkey's day, see TurkeyTime).
 */
object XpBoostRules {
    const val DURATION_MILLIS = 30 * 60_000L
    const val MAX_PER_DAY = 3
    const val MULTIPLIER = 2.0

    /** How many more may be taken today; [storedDay] is the day [storedCount] was counted on. */
    fun offersLeft(storedDay: Long, storedCount: Int, today: Long): Int =
        if (storedDay != today) MAX_PER_DAY else (MAX_PER_DAY - storedCount).coerceAtLeast(0)

    /** Running now. A deadline further away than one boost can last (the phone's clock was set back) does not count. */
    fun isActive(until: Long, now: Long): Boolean = until > now && until - now <= DURATION_MILLIS

    fun multiplier(until: Long, now: Long): Double = if (isActive(until, now)) MULTIPLIER else 1.0
}

/** Where a taken boost is kept: when it ends (wall clock, so it runs while the app is closed) and how many were taken today. */
@Singleton
class XpBoostStore @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("xp_boost", Context.MODE_PRIVATE)
    private val _until = MutableStateFlow(prefs.getLong(KEY_UNTIL, 0L))
    val until: StateFlow<Long> = _until.asStateFlow()

    fun multiplierNow(now: Long = System.currentTimeMillis()): Double = XpBoostRules.multiplier(_until.value, now)

    fun offersLeft(today: Long = TurkeyTime.today().toEpochDay()): Int =
        XpBoostRules.offersLeft(prefs.getLong(KEY_DAY, -1L), prefs.getInt(KEY_COUNT, 0), today)

    /** An earned ad: 30 minutes from now, and one of today's taken. */
    @Synchronized
    fun grant(now: Long = System.currentTimeMillis(), today: Long = TurkeyTime.today().toEpochDay()) {
        val count = if (prefs.getLong(KEY_DAY, -1L) == today) prefs.getInt(KEY_COUNT, 0) + 1 else 1
        val end = now + XpBoostRules.DURATION_MILLIS
        prefs.edit().putLong(KEY_UNTIL, end).putLong(KEY_DAY, today).putInt(KEY_COUNT, count).apply()
        _until.value = end
    }

    private companion object {
        const val KEY_UNTIL = "until"
        const val KEY_DAY = "day"
        const val KEY_COUNT = "count"
    }
}
