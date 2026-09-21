package com.sualtikasifi.cizimhafiza.domain.model

/**
 * A consumable helper bought with gold (or dropped from chests) and spent
 * during a match. Persisted by [name] in SettingsRepository — never rename.
 */
enum class JokerType(val price: Int) {
    /** Reveals the first letter of the word being guessed. */
    FIRST_LETTER(120),

    /** Shows how many letters the word being guessed has. */
    LETTER_COUNT(80),

    /** Adds extra time to the current drawing turn. */
    EXTRA_TIME(150);

    companion object {
        /** Buying this many at once is discounted. */
        const val BULK_QUANTITY = 5
        const val BULK_DISCOUNT_PERCENT = 15
    }

    fun priceFor(quantity: Int): Int =
        if (quantity >= BULK_QUANTITY) (price * quantity * (100 - BULK_DISCOUNT_PERCENT)) / 100 else price * quantity
}
