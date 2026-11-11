package com.sualtikasifi.cizimhafiza.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The small on-phone limits of the "Bil Bakalım" links, counted per day in Türkiye time (see [TurkeyTime]): how many
 * links this phone made today, and how many solved links already paid out today. Plain numbers in their own file,
 * nothing here is worth a sync.
 */
@Singleton
class GuessStore @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("guess_links", Context.MODE_PRIVATE)

    private fun day(): Long = TurkeyTime.today().toEpochDay()

    private fun countToday(key: String): Int = if (prefs.getLong("${key}_day", -1L) == day()) prefs.getInt(key, 0) else 0

    private fun addToday(key: String, n: Int) {
        prefs.edit().putLong("${key}_day", day()).putInt(key, countToday(key) + n).apply()
    }

    val linksLeftToday: Int get() = (MAX_LINKS_PER_DAY - countToday(KEY_LINKS)).coerceAtLeast(0)

    fun recordLinkCreated() = addToday(KEY_LINKS, 1)

    /** How many more solved results may be paid today; the amount to grant is [REWARD_GOLD] each. */
    val paidSolvesLeftToday: Int get() = (MAX_PAID_SOLVES_PER_DAY - countToday(KEY_PAID)).coerceAtLeast(0)

    fun recordPaidSolves(n: Int) = addToday(KEY_PAID, n)

    /**
     * A link the player installed the app from (read once from the Play Store's install referrer, see ReferralRewardClaimer):
     * the first screen opens on it. Taken once.
     */
    var pendingLinkId: String?
        get() = prefs.getString(KEY_PENDING, null)
        set(value) { prefs.edit().apply { if (value == null) remove(KEY_PENDING) else putString(KEY_PENDING, value) }.apply() }

    companion object {
        private const val KEY_PENDING = "pending_link"
        const val MAX_LINKS_PER_DAY = 20
        const val MAX_PAID_SOLVES_PER_DAY = 5
        /** Gold for each friend who guessed one of the player's drawings (up to [MAX_PAID_SOLVES_PER_DAY] a day). */
        const val REWARD_GOLD = 20
        private const val KEY_LINKS = "links"
        private const val KEY_PAID = "paid"
    }
}
