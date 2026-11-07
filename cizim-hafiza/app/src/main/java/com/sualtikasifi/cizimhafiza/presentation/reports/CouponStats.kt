package com.sualtikasifi.cizimhafiza.presentation.reports

import com.sualtikasifi.cizimhafiza.domain.model.Coupon
import java.time.Instant
import java.time.ZoneOffset

/** What the coupon panel works out from codes and their users, apart from the screen so it can be tested. */
object CouponStats {
    /** One campaign's codes added up. */
    data class SourceRow(val source: String, val codes: Int, val used: Int, val capacity: Int)

    /**
     * The UTC days (yyyy-MM-dd) from the one a code was made on up to today, the latest [max] of them: the days whose list of
     * new players is looked at to see how many of a code's users were new. Empty for a code with no known creation time.
     */
    fun days(createdAtMillis: Long, nowMillis: Long, max: Int = 14): List<String> {
        if (createdAtMillis <= 0L || nowMillis < createdAtMillis) return emptyList()
        val from = Instant.ofEpochMilli(createdAtMillis).atZone(ZoneOffset.UTC).toLocalDate()
        val to = Instant.ofEpochMilli(nowMillis).atZone(ZoneOffset.UTC).toLocalDate()
        return generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }.map { it.toString() }.toList().takeLast(max)
    }

    /** How many of [redeemers] are among [newUids] (the players who first appeared on the days looked at). */
    fun newAmong(redeemers: Set<String>, newUids: Set<String>): Int = redeemers.count { it in newUids }

    /** The labelled codes added up by label, the most used first: how well each video's code did. */
    fun bySource(coupons: List<Coupon>): List<SourceRow> = coupons
        .filter { it.source.isNotBlank() }
        .groupBy { it.source }
        .map { (source, list) -> SourceRow(source, list.size, list.sumOf { it.usedCount }, list.sumOf { it.maxUses }) }
        .sortedByDescending { it.used }

    /**
     * The line to paste under the video when a code is over, or null while it still works: either every use is taken or its
     * time has passed. Says how many used it and that a new one is coming.
     */
    fun closingComment(c: Coupon, now: Long): String? {
        val expired = c.expiresAtMillis in 1..now
        val full = c.maxUses > 0 && c.usedCount >= c.maxUses
        return when {
            full -> "✅ ${c.code} kodu doldu: ilk ${c.maxUses} kişi kullandı. Bir sonraki videoda yeni bir kod var, takipte kal! 🎁"
            expired -> "⌛ ${c.code} kodunun süresi bitti (${c.usedCount}/${c.maxUses} kişi kullandı). Yeni kod için takipte kal! 🎁"
            else -> null
        }
    }
}
