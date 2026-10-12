package com.sualtikasifi.cizimhafiza.domain.repository

import com.sualtikasifi.cizimhafiza.domain.model.Coupon
import com.sualtikasifi.cizimhafiza.domain.model.RedeemResult

interface CouponRepository {
    /**
     * Uses a code. The server-side transaction checks that it exists, has not expired, has uses left and has not
     * been used by this account yet, and only then counts the use. [freeChestSlots] is asked first so a code that
     * gives chests is not used up when there is nowhere to put them.
     */
    suspend fun redeem(rawCode: String, freeChestSlots: Int): RedeemResult

    /** Developer panel only (the rules allow nobody else): creates or replaces a code. */
    suspend fun create(coupon: Coupon): Result<Unit>

    /** Developer panel only: the newest codes with how often each has been used. */
    suspend fun recent(limit: Int): Result<List<Coupon>>
}
