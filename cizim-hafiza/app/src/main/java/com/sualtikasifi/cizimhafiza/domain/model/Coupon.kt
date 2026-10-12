package com.sualtikasifi.cizimhafiza.domain.model

/**
 * A kupon kodu: what it gives, how many people may use it and until when.
 *
 * Lives in Firestore (coupons/{CODE}), never in the app — a code stored in the APK would be readable by anyone.
 * [maxUses] is what makes "the first 10 people" work: a shared code with maxUses 10, or ten single-use codes
 * (maxUses 1) each sent to one winner. [expiresAtMillis] 0 means it never expires.
 */
data class Coupon(
    val code: String,
    val gold: Int = 0,
    val xp: Int = 0,
    val chest: ChestTier? = null,
    val maxUses: Int = 1,
    val usedCount: Int = 0,
    val expiresAtMillis: Long = 0L
) {
    val hasReward: Boolean get() = gold > 0 || xp > 0 || chest != null
}

sealed interface RedeemResult {
    data class Success(val coupon: Coupon) : RedeemResult
    data object Invalid : RedeemResult
    data object NotFound : RedeemResult
    data object Expired : RedeemResult
    data object Exhausted : RedeemResult
    data object AlreadyUsed : RedeemResult
    /** The code gives a chest and every chest slot is taken — nothing was used up, try again after opening one. */
    data object ChestSlotsFull : RedeemResult
    data object NetworkError : RedeemResult
}

object CouponCodes {
    const val MIN_LENGTH = 4
    const val MAX_LENGTH = 24
    const val MAX_GOLD = 100_000
    const val MAX_XP = 100_000

    // No 0/O/1/I/L: a code is read off a post or a message and typed by hand.
    private const val ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"

    /** Upper-cased, spaces dropped; null when it cannot possibly be a code (wrong length or characters). */
    fun normalize(raw: String): String? {
        val code = raw.trim().uppercase().replace(" ", "")
        if (code.length !in MIN_LENGTH..MAX_LENGTH) return null
        if (!code.all { it in 'A'..'Z' || it in '0'..'9' || it == '-' }) return null
        return code
    }

    /** A fresh code like KRLK-7H3QM2. */
    fun generate(random: kotlin.random.Random = kotlin.random.Random): String =
        "KRLK-" + (1..6).map { ALPHABET[random.nextInt(ALPHABET.length)] }.joinToString("")
}
