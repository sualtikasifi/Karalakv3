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
 *
 * The offer is asked on a tap on "Hızlı Eşleş", once per [OFFER_GAP_MILLIS]: whatever the player answers (an ad watched or "not
 * now"), the next question waits eight hours, so nobody is pushed.
 */
object XpBoostRules {
    const val DURATION_MILLIS = 30 * 60_000L
    const val MAX_PER_DAY = 3
    const val MULTIPLIER = 2.0
    const val OFFER_GAP_MILLIS = 8 * 60 * 60_000L

    /** How many more may be taken today; [storedDay] is the day [storedCount] was counted on. */
    fun offersLeft(storedDay: Long, storedCount: Int, today: Long): Int =
        if (storedDay != today) MAX_PER_DAY else (MAX_PER_DAY - storedCount).coerceAtLeast(0)

    /**
     * Whether the question may be asked now: never asked yet, or the last time was eight hours ago or more. A last-asked time further
     * ahead than one gap (the phone's clock was set back) counts as long ago, so a wrong clock cannot lock the offer away for days.
     */
    fun offerDue(lastOfferAt: Long, now: Long): Boolean =
        lastOfferAt <= 0L || now - lastOfferAt >= OFFER_GAP_MILLIS || lastOfferAt - now > OFFER_GAP_MILLIS

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

    /** Whether the offer may be asked now (see [XpBoostRules.offerDue]). */
    fun offerDue(now: Long = System.currentTimeMillis()): Boolean = XpBoostRules.offerDue(prefs.getLong(KEY_LAST_OFFER, 0L), now)

    /** The question was asked now: the next one waits eight hours, whatever the answer. */
    fun markOffered(now: Long = System.currentTimeMillis()) { prefs.edit().putLong(KEY_LAST_OFFER, now).apply() }

    /** The ad the player agreed to could not be shown: the question was not really answered, so it may be asked again. */
    fun forgetOffer() { prefs.edit().remove(KEY_LAST_OFFER).apply() }

    /** An earned ad: 30 minutes from now, and one of today's taken. */
    @Synchronized
    fun grant(now: Long = System.currentTimeMillis(), today: Long = TurkeyTime.today().toEpochDay()) {
        val count = if (prefs.getLong(KEY_DAY, -1L) == today) prefs.getInt(KEY_COUNT, 0) + 1 else 1
        val end = now + XpBoostRules.DURATION_MILLIS
        prefs.edit().putLong(KEY_UNTIL, end).putLong(KEY_DAY, today).putInt(KEY_COUNT, count).apply()
        _until.value = end
    }

    /** A different account took over this phone: a boost the previous one earned must not run for the next one. */
    @Synchronized
    fun clear() {
        prefs.edit().clear().apply()
        _until.value = 0L
    }

    private companion object {
        const val KEY_UNTIL = "until"
        const val KEY_DAY = "day"
        const val KEY_COUNT = "count"
        const val KEY_LAST_OFFER = "lastOffer"
    }
}
