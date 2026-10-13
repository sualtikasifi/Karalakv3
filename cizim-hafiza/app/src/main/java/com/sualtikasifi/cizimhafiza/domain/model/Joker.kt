package com.sualtikasifi.cizimhafiza.domain.model

/**
 * A consumable helper bought with gold (or dropped from chests) and spent
 * during a match. Persisted by [name] in SettingsRepository — never rename.
 */
enum class JokerType(val price: Int) {
    /** Reveals the first letter of the word being guessed. */
    FIRST_LETTER(200),

    /** Shows how many letters the word being guessed has. */
    LETTER_COUNT(150),

    /** Adds extra time to the current drawing turn. */
    EXTRA_TIME(250);

    companion object {
        /** Buying this many at once is discounted. */
        const val BULK_QUANTITY = 5
        const val BULK_DISCOUNT_PERCENT = 15
    }

    fun priceFor(quantity: Int, extraDiscountPercent: Int = 0): Int {
        val base = if (quantity >= BULK_QUANTITY) (price * quantity * (100 - BULK_DISCOUNT_PERCENT)) / 100 else price * quantity
        return if (extraDiscountPercent > 0) base * (100 - extraDiscountPercent) / 100 else base
    }
}

/**
 * The joker offered free (for a rewarded ad) on a given calendar day: first
 * letter, then letter count, then extra drawing time, round and round. A pure
 * function of the day so every device agrees and nothing has to be stored to
 * know what "today's" joker is.
 */
object DailyJoker {
    private val ROTATION = listOf(JokerType.FIRST_LETTER, JokerType.LETTER_COUNT, JokerType.EXTRA_TIME)

    fun typeFor(epochDay: Long): JokerType = ROTATION[Math.floorMod(epochDay, ROTATION.size.toLong()).toInt()]
}
