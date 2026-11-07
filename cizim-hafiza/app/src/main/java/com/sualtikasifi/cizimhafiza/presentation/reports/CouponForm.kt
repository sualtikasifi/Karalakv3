package com.sualtikasifi.cizimhafiza.presentation.reports

import com.sualtikasifi.cizimhafiza.domain.model.CouponCodes

/** The rules of the panel's "Yeni kupon" form, apart from the screen so they can be tested. */
object CouponForm {
    /** How often a generated code is drawn again when its name turns out to be taken already. */
    const val MAX_NAME_ATTEMPTS = 5

    /**
     * What is wrong with the form, as the line the panel shows, or null when it can be made. [maxUses] is what the form
     * would give (1 when every code is single-use), [customCode] only counts for the single shared code.
     */
    fun problem(gold: Int, xp: Int, chestTotal: Int, maxUses: Int, separateCodes: Int, customCode: String): String? = when {
        gold !in 0..CouponCodes.MAX_GOLD || xp !in 0..CouponCodes.MAX_XP -> "Altın ve XP 0 ile ${CouponCodes.MAX_GOLD} arasında olmalı."
        chestTotal > CouponCodes.MAX_CHESTS -> "En fazla ${CouponCodes.MAX_CHESTS} kasa verilebilir (her biri sırayla açılır)."
        gold == 0 && xp == 0 && chestTotal == 0 -> "En az bir ödül seç."
        maxUses < 1 -> "Kullanım hakkı en az 1 olmalı."
        separateCodes == 0 && customCode.isNotBlank() && CouponCodes.normalize(customCode) == null ->
            "Kod 4-24 karakter olmalı, sadece harf, rakam ve tire."
        else -> null
    }

    /** When a code made [now] stops working after [days] days; 0 (never) for no days. */
    fun expiresAt(days: Int, now: Long): Long = if (days > 0) now + days * 24L * 60 * 60 * 1000 else 0L

    /** The line for a name that is taken: a typed one has to be changed, a drawn one never reaches the screen. */
    fun takenMessage(code: String) = "“$code” kodu zaten var; başka bir kod yaz ya da boş bırak."
}
